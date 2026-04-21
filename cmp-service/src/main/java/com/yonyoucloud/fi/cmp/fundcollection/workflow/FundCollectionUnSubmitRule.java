package com.yonyoucloud.fi.cmp.fundcollection.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundCommonCancelSettleServiceImpl;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.migrade.CmpNewFiPreCheckService;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 资金收款单撤回规则
 *
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@Component
@Slf4j
public class FundCollectionUnSubmitRule extends AbstractCommonRule {

    @Autowired
    IFundCommonService fundCommonService;

    @Resource
    CmCommonService<Object> commonService;

    @Autowired
    CmpNewFiPreCheckService cmpNewFiPreCheckService;

    @Resource
    private FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bills.get(0).getId(), 2);
            if (fundCollection == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100698"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180383","单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            short verifystate = Short.parseShort(fundCollection.get("verifystate").toString());
            if (verifystate == VerifyState.INIT_NEW_OPEN.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102373"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418060D","单据未提交，不能进行撤回！") /* "单据未提交，不能进行撤回！" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102265"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418060E","单据已终止流程，不能进行撤回！") /* "单据已终止流程，不能进行撤回！" */);
            }

            // 先去掉这个校验，老给客户修数据，累了，试运行几周试试；会引发的问题是：提交单据立即撤回，导致单据为保存态，但是生成了事项分录
            short voucherstatus = Short.parseShort(fundCollection.get("voucherstatus").toString());
            if (voucherstatus == VoucherStatus.POSTING.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102374"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418060C","过账中的单据，不能进行撤回！") /* "过账中的单据，不能进行撤回！" */);
            }

            //解决场景问题：需要传结算，且结算完成后生成事项的，如果已经生成了结算单，并且事项事件还没回来把主表的事项状态更新成过帐中，会产生单子保存态却生成了事项，或者单子删除了，事项还在的问题
          /*  boolean isSettleSuccessToPost = AppContext.getBean(FundCommonCancelSettleServiceImpl.class).isSettleSuccessToPost(fundCollection.getAccentity());
            if(fundCollection.getSettleflag() == 1 && isSettleSuccessToPost && fundCollection.getVoucherstatus().getValue() ==  VoucherStatus.TO_BE_POST.getValue()) {
                List<FundCollection_b> fundCollection_bList = fundCollection.get("FundCollection_b");
                List<FundCollection_b> notSettlementSupplement_bList = fundCollection_bList.stream().filter(detail->detail.getFundSettlestatus() != FundSettleStatus.Refund && detail.getFundSettlestatus() != FundSettleStatus.SettlementSupplement).collect(Collectors.toList());
                if(CollectionUtils.isEmpty(notSettlementSupplement_bList)){
                    List<String> detailIdList = fundCollection_bList.stream().map(FundCollection_b::getId).map(Object::toString).collect(Collectors.toList());
                    fundCommonService.checkHasSettlementBill(detailIdList);
                }
            }*/


            //begin yangjn 20240130 老架构收付款工作台升级数据 如果来源单据已经结算完成 则资金收付不能逆操作
            if(fundCollection.getMigradeid()!=null){
                cmpNewFiPreCheckService.checkUpgradeDataBack(ReceiveBill.ENTITY_NAME,fundCollection.getMigradeid());
            }
            //end yangjn 20240130


            Map<String, Object> autoConfigMap = commonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY)) ;
            if(!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")){
                //走影像
                BillBiz.executeRule("shareUnSubmit", billContext, paramMap);
            }
            if(null != fundCollection && (null == fundCollection.getIsWfControlled() || !fundCollection.getIsWfControlled()) ){
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule("unaudit", billContext, paramMap);
                result.setCancel(true);
                // 资金计划额度释放
                fundBillAdaptationFundPlanService.fundCollectionUnSubmitReleaseFundPlan(fundCollection);

            } else {
                // 资金计划额度释放
                fundBillAdaptationFundPlanService.fundCollectionUnSubmitReleaseFundPlan(fundCollection);
            }
        }

        return result;
    }

}
