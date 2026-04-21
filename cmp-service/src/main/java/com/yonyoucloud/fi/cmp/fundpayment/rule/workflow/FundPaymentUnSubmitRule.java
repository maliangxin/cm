package com.yonyoucloud.fi.cmp.fundpayment.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatusConverter;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanService;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundCommonCancelSettleServiceImpl;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.migrade.CmpNewFiPreCheckService;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.util.BillAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h1>资金付款单撤回规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-13 10:16
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class FundPaymentUnSubmitRule extends AbstractCommonRule {

    private final CmCommonService<Object> commonService;

    private final CmpBudgetManagerService cmpBudgetManagerService;

    private final CmpNewFiPreCheckService cmpNewFiPreCheckService;

    private final FundBillAdaptationFundPlanService fundBillAdaptationFundPlanService;

    @Autowired
    private IFundCommonService fundCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();

        for (BizObject bizObject : bills) {
            FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId());
            if (fundPayment == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100698"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180383","单据不存在 id:") /* "单据不存在 id:" */ + bizObject.getId());
            }
            short verifystate = Short.parseShort(fundPayment.get("verifystate").toString());
            if (verifystate == VerifyState.INIT_NEW_OPEN.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101484"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180776","单据未提交，不能进行撤回！") /* "单据未提交，不能进行撤回！" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101485"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180777","单据已终止流程，不能进行撤回！") /* "单据已终止流程，不能进行撤回！" */);
            }

            // 先去掉这个校验，老给客户修数据，累了，试运行几周试试；会引发的问题是：提交单据立即撤回，导致单据为保存态，但是生成了事项分录
            if(fundPayment.getVoucherstatus()!=null){
                short voucherstatus = fundPayment.getVoucherstatus().getValue();
                if (voucherstatus == VoucherStatus.POSTING.getValue()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100747"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180775","过账中的单据，不能进行撤回！") /* "过账中的单据，不能进行撤回！" */);
                }
            }

            //解决场景问题：需要传结算，且结算完成后生成事项的，如果已经生成了结算单，并且事项事件还没回来把主表的事项状态更新成过帐中，会产生单子保存态却生成了事项，或者单子删除了，事项还在的问题
            /*boolean isSettleSuccessToPost = AppContext.getBean(FundCommonCancelSettleServiceImpl.class).isSettleSuccessToPost(fundPayment.getAccentity());
            if(fundPayment.getSettleflag() == 1 && isSettleSuccessToPost && fundPayment.getVoucherstatus().getValue() ==  VoucherStatus.TO_BE_POST.getValue()){
                List<FundPayment_b> fundPayment_bList = fundPayment.get("FundPayment_b");
                List<FundPayment_b> notSettlementSupplement_bList = fundPayment_bList.stream().filter(detail->detail.getFundSettlestatus() != FundSettleStatus.Refund && detail.getFundSettlestatus() != FundSettleStatus.SettlementSupplement).collect(Collectors.toList());
                if(CollectionUtils.isEmpty(notSettlementSupplement_bList)){
                    List<String> detailIdList = fundPayment_bList.stream().map(FundPayment_b::getId).map(Object::toString).collect(Collectors.toList());
                    fundCommonService.checkHasSettlementBill(detailIdList);
                }
            }*/

            //begin yangjn 20240130 老架构收付款工作台升级数据 如果来源单据已经结算完成 则资金收付不能逆操作
            if(fundPayment.getMigradeid()!=null){
                cmpNewFiPreCheckService.checkUpgradeDataBack(PayBill.ENTITY_NAME,fundPayment.getMigradeid());
            }
            //end yangjn 20240130

            Map<String, Object> autoConfigMap = commonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY));
            if (!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")) {
                // 走影像
                BillBiz.executeRule("shareUnSubmit", billContext, paramMap);
            }
            if (null == fundPayment.getIsWfControlled() || !fundPayment.getIsWfControlled()) {
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule("unaudit", billContext, paramMap);
                result.setCancel(true);
            }

            if (cmpBudgetManagerService.isCanStart(IBillNumConstant.FUND_PAYMENT)) {
                fundPaymentExecuteSubmitDelete(fundPayment,bizObject);
            }
            // 资金计划额度释放
            fundBillAdaptationFundPlanService.fundPaymentUnSubmitReleaseFundPlan(fundPayment);
            //把rule中用的数据赋值给上下文
            copySubFieidValue(fundPayment, (FundPayment) bizObject,new String[]{"isOccupyBudget","isToPushCspl"});
        }
        return result;
    }

    /**
     * 更新预算占用状态
     * @param fundPayment
     * @throws Exception
     */
    private void fundPaymentExecuteSubmitDelete(FundPayment fundPayment,BizObject bizObject) throws Exception {
        List<FundPayment_b> list = fundPayment.FundPayment_b();
        List<FundPayment_b> relList = new ArrayList();
        List<FundPayment_b> updateList = new ArrayList();
        for (FundPayment_b fundPayment_b : list) {
            Short budgeted = fundPayment_b.getIsOccupyBudget();
            // 已经释放仍要释放，直接跳过不执行了
            if (budgeted == null || ((budgeted==OccupyBudget.UnOccupy.getValue()))) {
                continue;
            }
            if(fundPayment_b.getIsOccupyBudget() != null && fundPayment_b.getIsOccupyBudget() == OccupyBudget.UnOccupy.getValue()){
                continue;
            }
            relList.add(fundPayment_b);
        }
        if (relList.size() > 0) {
            ResultBudget resultBudget = cmpBudgetManagerService.gcExecuteBatchUnSubmit(fundPayment, relList, IBillNumConstant.FUND_PAYMENT, BillAction.CANCEL_SUBMIT);
            if(resultBudget.isSuccess()){
                List<BizObject> subList = bizObject.get("FundPayment_b");
                HashMap<String, Short> contextMap = new HashMap<>();
                if(subList != null && subList.size() > 0){
                    for (BizObject bizObject1 : subList) {
                        contextMap.put(bizObject1.getId().toString(), bizObject1.get(ICmpConstant.SETTLE_STATUS));
                    }
                }
                relList.stream().forEach(fundPayment_b -> {
                    // 更新预算占用状态的数据重新构建
                    FundPayment_b fundPaymentB = new FundPayment_b();
                    fundPaymentB.setId(fundPayment_b.getId());
                    fundPaymentB.setEntityStatus(EntityStatus.Update);
                    fundPaymentB.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    updateList.add(fundPaymentB);
                    //同时参数数据，用于更新上下文的数据
                    fundPayment_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                });
            }
            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, updateList);
        }
    }

    private void copySubFieidValue(FundPayment original, FundPayment target,String[] fields) throws Exception {
        if(original == null || target==null || fields == null || fields.length == 0 ){
            return;
        }
        List<FundPayment_b> oriSubList = original.FundPayment_b();
        if(oriSubList == null || oriSubList.size() == 0){
            return;
        }
        List<FundPayment_b> tarSubList = target.FundPayment_b();
        if(tarSubList == null || tarSubList.size() == 0){
            return;
        }
        HashMap<String, FundPayment_b> oriSubMap = new HashMap<>();
        for (int i = 0; i < oriSubList.size(); i++) {
            oriSubMap.put(oriSubList.get(i).getId().toString(), oriSubList.get(i));
        }
        HashMap<String, FundPayment_b> tarSubMap = new HashMap<>();
        for (int i = 0; i < tarSubList.size(); i++) {
            tarSubMap.put(tarSubList.get(i).getId().toString(), tarSubList.get(i));
        }
        oriSubMap.keySet().forEach(key -> {
            FundPayment_b oriSub = oriSubMap.get(key);
            FundPayment_b tarSub = tarSubMap.get(key);
            if(tarSub == null){
                return;
            }
            copyFieidValue(oriSub, tarSub, fields);
        });
    }

    private void copyFieidValue(FundPayment_b original, FundPayment_b target,String[] fields) {
        if(original == null || target==null || fields == null || fields.length == 0){
            return;
        }
        for(String field : fields){
            Object value = original.get(field);
            if(value != null){
                target.set(field, value);
            }
        }
    }
}
