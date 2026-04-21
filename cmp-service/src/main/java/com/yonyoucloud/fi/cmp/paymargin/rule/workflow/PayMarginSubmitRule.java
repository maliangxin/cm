package com.yonyoucloud.fi.cmp.paymargin.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetPaymarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 支付保证金提交规则
 * *
 *
 * @author xuxbo
 * @date 2023/8/3 15:15
 */
@Component
public class PayMarginSubmitRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetPaymarginManagerService cmpBudgetPaymarginManagerService;

    @Autowired
    @Qualifier("stwbPayMarginServiceImpl")
    private StwbBillService stwbBillService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            PayMargin payMargin = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId(), null);
            if (ObjectUtils.isEmpty(payMargin)) {
                return result;
            }

           /* if(!payMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue())) {
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(payMargin);
                stwbBillService.pushBill(currentBillList, true);// 冗余数据调用资金结算校验
            }*/

            short verifystate = bizObject.get(ICmpConstant.VERIFY_STATE);
            if (verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.COMPLETED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100757"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B0002F", "单据已提交，不能进行重复提交!") /* "单据已提交，不能进行重复提交!" */);
            }
            if (verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100758"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00140", "单据已终止流程，不能进行提交！") /* "单据已终止流程，不能进行提交！" */);
            }

            if (!payMargin.getIsWfControlled()) {
                // 预算
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
                    cmpBudgetPaymarginManagerService.executeSubmit(payMargin);
                    //刷新pubts
                    PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId(), null);
                    bizObject.setPubts(payMarginNew.getPubts());
                }
                // 未启动审批流，单据直接审批通过
                result = BillBiz.executeRule(ICmpConstant.AUDIT, billContext, paramMap);
                result.setCancel(true);
            } else {
                // 预算
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
                    cmpBudgetPaymarginManagerService.executeSubmit(payMargin);
                    //刷新pubts
                    PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId(), null);
                    bizObject.setPubts(payMarginNew.getPubts());
                    PayMargin payMarginObj = (PayMargin) bizObject;
                    payMarginObj.setIsOccupyBudget(payMarginNew.getIsOccupyBudget());
                }
            }
        }
        return result;
    }




}
