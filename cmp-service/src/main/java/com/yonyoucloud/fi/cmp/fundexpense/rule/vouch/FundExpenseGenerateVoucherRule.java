package com.yonyoucloud.fi.cmp.fundexpense.rule.vouch;

import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>资金收付款单生成凭证</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-31 10:35
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FundExpenseGenerateVoucherRule extends AbstractCommonRule {

    private final CmpVoucherService cmpVoucherService;
    private final CmCommonService commonService;
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;
    @Resource
    private ICmpSendEventService cmpSendEventService;
    @Resource
    private IApplicationService appService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        Map<String, Object> autoConfig = commonService.queryAutoConfigByAccentity((String) bills.get(0).get("accentity"));
        boolean isSettleSuccessToPost;
        if(autoConfig != null ){
            // 资金收付款单如果在现金参数里：是否结算成功时做账为是时，则先不过账，最后统一过账
            if(autoConfig.get("isSettleSuccessToPost") != null){
                isSettleSuccessToPost = (Boolean) autoConfig.get("isSettleSuccessToPost");
            }else{
                Map<String, Object> autoConfigTenant = commonService.queryAutoConfigTenant();
                isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
            }
        }else{
            Map<String, Object> autoConfigTenant = commonService.queryAutoConfigTenant();
            isSettleSuccessToPost = autoConfigTenant != null && autoConfigTenant.get("isSettleSuccessToPost") != null && (Boolean) autoConfigTenant.get("isSettleSuccessToPost");
        }

        if (isSettleSuccessToPost){
            return new RuleExecuteResult();
        }
        for (BizObject bizObject : bills) {
            BizObject commonFund = null;
            commonFund = MetaDaoHelper.findById(Fundexpense.ENTITY_NAME, bizObject.getId(), 3);
            commonFund.put("_entityName", Fundexpense.ENTITY_NAME);
            boolean isWfControlled = ValueUtils.isNotEmptyObj(commonFund.get(ICmpConstant.IS_WFCONTROLLED)) && commonFund.getBoolean(ICmpConstant.IS_WFCONTROLLED);
            commonFund.set("isWfControlled", isWfControlled);
            EntityTool.setUpdateStatus(commonFund);
            // 新架构推送事件中心
            if (CmpCommonUtil.getNewFiFlag()) {
                boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
                if (!enableEVNT) {
                    log.error("客户环境未安装事项中台服务");
                    commonFund.set("voucherstatus", VoucherStatus.NONCreate.getValue());
                } else {
                    sendMessage2Event(commonFund,billContext);
                    commonFund.set("voucherstatus", VoucherStatus.POSTING.getValue());
                }
            }else{
                CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResultTry(commonFund);
                if (generateResult.getInteger("code") == 0 && !generateResult.getBoolean("dealSucceed")) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101567"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C2",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540054B", "发送会计平台失败：") /* "发送会计平台失败：" */) /* "发送会计平台失败：" */ + generateResult.get("message"));
                }
                assert commonFund != null;
                if (generateResult.getInteger("code") == 1 && !generateResult.getBoolean("genVoucher")) {
                    commonFund.put("voucherstatus", VoucherStatus.NONCreate.getValue());
                    bills.get(0).put("voucherstatus", VoucherStatus.NONCreate.getValue());
                }
                if (generateResult.getInteger("code") == 1 && generateResult.getBoolean("genVoucher")) {
                    commonFund.put("voucherstatus", VoucherStatus.Received.getValue());
                    bills.get(0).put("voucherstatus", VoucherStatus.Received.getValue());
                }
            }
            if (Fundexpense.ENTITY_NAME.equals(billContext.getFullname())) {
                MetaDaoHelper.update(Fundexpense.ENTITY_NAME, commonFund);
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 校验总账期间
     * @param bizObject
     * @return
     * @throws Exception
     */
    boolean checkOrgPeriodBeginDateGL(BizObject bizObject) throws Exception{
        Date enabledBeginData = cmpVoucherService.queryOrgPeriodBeginDateGL(bizObject.get(IBussinessConstant.ACCENTITY));
        if (enabledBeginData == null) {
            return false;
        }
        if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
            return false;
        }
        return true;
    }

    /**
     * 新架构下资金收付款单发送事件
     * @param bizObject
     */
    void sendMessage2Event(BizObject bizObject,BillContext billContext){
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @SneakyThrows
            @Override
            public void afterCommit() {
                executorServicePool.getThreadPoolExecutor().submit(()->{
                    try {
                        BizObject commonFund = null;
                        if (FundCollection.ENTITY_NAME.equals(billContext.getFullname())) {
                            commonFund = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizObject.getId(), 3);
                            commonFund.set("_entityName", FundCollection.ENTITY_NAME);
                        } else if (FundPayment.ENTITY_NAME.equals(billContext.getFullname())) {
                            commonFund = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId(), 3);
                            commonFund.set("_entityName", FundPayment.ENTITY_NAME);
                        }
                        CtmJSONObject billClue = new CtmJSONObject();
                        billClue.put("classifier", null);
                        billClue.put("srcBusiId", commonFund.getId().toString());
                        cmpSendEventService.sendSimpleEvent(commonFund, billClue);
                        commonFund.set("voucherstatus", VoucherStatus.POSTING.getValue());
                        if (FundCollection.ENTITY_NAME.equals(billContext.getFullname())) {
                            MetaDaoHelper.update(FundCollection.ENTITY_NAME, commonFund);
                        }else if (FundPayment.ENTITY_NAME.equals(billContext.getFullname())) {
                            MetaDaoHelper.update(FundPayment.ENTITY_NAME, commonFund);
                        }
                    } catch (Exception e) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100354"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805CC",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540054C", "生成凭证失败！请检查数据！") /* "生成凭证失败！请检查数据！" */));
                    }
                });
            }
        });
    }

}
