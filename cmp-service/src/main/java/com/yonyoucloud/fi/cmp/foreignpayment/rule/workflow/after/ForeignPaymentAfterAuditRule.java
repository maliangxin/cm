package com.yonyoucloud.fi.cmp.foreignpayment.rule.workflow.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetForeignpaymentManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 外汇付款 审核后规则
 * *
 *
 * @author xuxbo
 * @date 2023/8/3 14:31
 */

@Slf4j
@Component
public class ForeignPaymentAfterAuditRule extends AbstractCommonRule {

    @Autowired
    @Qualifier("stwbForeignPaymentBillServiceImpl")
    private StwbBillService stwbBillService;

    @Autowired
    private CmpBudgetForeignpaymentManagerService cmpBudgetForeignpaymentManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {
            String id = bizObject.getId().toString();
            try {
                log.info("ForeignPaymentAfterAuditRule bizObject, id = {}, pubTs = {}", bizObject.getId(), bizObject.getPubts());
                ForeignPayment currentBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizObject.getId());
                log.info("ForeignPaymentAfterAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                //推送资金结算
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(currentBill);
                stwbBillService.pushBill(currentBillList, false);// 推送资金结算
                // 修改结算状态为结算中
//                if (currentBill.getSettlestatus().equals(FundSettleStatus.WaitSettle.getValue())){
//                    currentBill.setSettlestatus(FundSettleStatus.SettleProssing.getValue());
//                }
//
//                currentBill.setEntityStatus(EntityStatus.Update);
//                MetaDaoHelper.update(ForeignPayment.ENTITY_NAME, currentBill);
                if (currentBill.getSettlestatus() != null && currentBill.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue() && currentBill.getSettlesuccesstime() != null) {
                    boolean implement = cmpBudgetForeignpaymentManagerService.implement(currentBill);
                    if (implement) {
                        cmpBudgetForeignpaymentManagerService.updateOccupyBudget(currentBill, OccupyBudget.ActualSuccess.getValue());
                    }
                }
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100567"),e.getMessage());
            } finally {
                JedisLockUtils.unlockRuleWithOutTrace(map);
            }

        }
        return new RuleExecuteResult();
    }
}
