package com.yonyoucloud.fi.cmp.paymargin.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetPaymarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * "deleteAll":删除流程实例或撤回流程到初始状态
 * "withdraw":(终审)撤销审核
 * "REJECTTOSTART":驳回制单
 * "WITHDRAWREJECTTOSTART":撤销驳回制单
 * "ACTIVITI_DELETED":终止
 * *
 */
@Component("payMarginAfterChangeVerifystateRule")
@Slf4j
@RequiredArgsConstructor
public class PayMarginAfterChangeVerifystateRule extends AbstractCommonRule {
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetPaymarginManagerService cmpBudgetPaymarginManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        String deleteReason = billContext.getDeleteReason();
        List<BizObject> bills = getBills(billContext, map);
        String fullName = billContext.getFullname();
        for (BizObject bizobject : bills) {
            PayMargin payMargin = (PayMargin) MetaDaoHelper.findById(fullName, bizobject.getId(), ICmpConstant.CONSTANT_TWO);
            //驳回/撤回到发起人/审批终止
            //是否占预算为预占成功时，删除预占
            if (BooleanUtils.b(payMargin.get(ICmpConstant.IS_WFCONTROLLED)) && ("REJECTTOSTART".equals(deleteReason) || "ACTIVITI_DELETED".equals(deleteReason))) {
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
                    if (payMargin.getIsOccupyBudget() != null && OccupyBudget.PreSuccess.getValue() == payMargin.getIsOccupyBudget()) {
                        cmpBudgetPaymarginManagerService.executeSubmitDelete(payMargin);
                        //刷新pubts
                        PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizobject.getId(), null);
                        bizobject.setPubts(payMarginNew.getPubts());
                    }
                }
            } else if (BooleanUtils.b(payMargin.get(ICmpConstant.IS_WFCONTROLLED)) && ("WITHDRAWREJECTTOSTART".equals(deleteReason))) {
                log.error("WITHDRAWREJECTTOSTART........");
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
                    cmpBudgetPaymarginManagerService.executeSubmit(payMargin);
                    //刷新pubts
                    PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizobject.getId(), null);
                    bizobject.setPubts(payMarginNew.getPubts());
                }
            } else if (BooleanUtils.b(payMargin.get(ICmpConstant.IS_WFCONTROLLED)) && ("withdraw".equals(deleteReason))) {
                log.error("withdraw........");
            }
        }
        return new RuleExecuteResult();
    }
}
