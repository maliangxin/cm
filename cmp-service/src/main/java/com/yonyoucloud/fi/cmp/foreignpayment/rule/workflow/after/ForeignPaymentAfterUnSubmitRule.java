package com.yonyoucloud.fi.cmp.foreignpayment.rule.workflow.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetForeignpaymentManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.BillAction;
import lombok.RequiredArgsConstructor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component("foreignPaymentAfterUnSubmitRule")
public class ForeignPaymentAfterUnSubmitRule extends AbstractCommonRule {

    @Autowired
    private CmpBudgetForeignpaymentManagerService cmpBudgetForeignpaymentManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            ForeignPayment foreignPayment = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizObject.getId(), null);
            releaseBudget(foreignPayment);
        }
        return result;
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     *
     * @param foreignPayment
     * @throws Exception
     */
    private void releaseBudget(ForeignPayment foreignPayment) throws Exception {
        Short budgeted = foreignPayment.getIsOccupyBudget();
        ForeignPayment update = new ForeignPayment();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            boolean releaseBudget = cmpBudgetForeignpaymentManagerService.releaseBudget(foreignPayment);
            if (releaseBudget) {
                cmpBudgetForeignpaymentManagerService.updateOccupyBudget(foreignPayment, OccupyBudget.UnOccupy.getValue());
            }
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            boolean releaseImplement = cmpBudgetForeignpaymentManagerService.releaseImplement(foreignPayment);
            if (releaseImplement) {
                cmpBudgetForeignpaymentManagerService.updateOccupyBudget(foreignPayment, OccupyBudget.UnOccupy.getValue());
            }
        }
    }
}
