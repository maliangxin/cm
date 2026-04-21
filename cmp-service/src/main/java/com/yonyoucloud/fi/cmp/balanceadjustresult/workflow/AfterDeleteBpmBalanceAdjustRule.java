package com.yonyoucloud.fi.cmp.balanceadjustresult.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.cmpentity.BalanceAuditStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * @description: 余额调节表驳回到制单或终止前规则
 * "deleteAll":删除流程实例或撤回流程到初始状态
 * "withdraw":(终审)撤销审核
 * "REJECTTOSTART":驳回制单
 * "WITHDRAWREJECTTOSTART":撤销驳回制单
 * "ACTIVITI_DELETED":终止
 * @author: zhoulyu@yonyou.com
 * @date: 2025/03/18 15:49
 */

@Component("afterDeleteBpmBalanceAdjustRule")
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AfterDeleteBpmBalanceAdjustRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        //String deleteReason = billContext.getDeleteReason();
        List<BizObject> bills = getBills(billContext, map);
        String fullName = BalanceAdjustResult.ENTITY_NAME;
        for (BizObject bizobject : bills) {
            BizObject currentBill = MetaDaoHelper.findById(fullName, bizobject.getId(), ICmpConstant.CONSTANT_TWO);
            currentBill.set("auditDate", null);
            currentBill.set("auditTime", null);
            currentBill.setEntityStatus(EntityStatus.Update);
            if (BooleanUtils.b(currentBill.get(ICmpConstant.IS_WFCONTROLLED))) {
                if (currentBill.get("verifystate") != null) {
                    if (VerifyState.INIT_NEW_OPEN.getValue() == currentBill.getShort("verifystate")) {
                        currentBill.set("auditstatus", BalanceAuditStatus.Incomplete.getValue());
                    } else if (VerifyState.SUBMITED.getValue() == currentBill.getShort("verifystate")) {
                        currentBill.set("auditstatus", BalanceAuditStatus.SUBMITED.getValue());
                    } else if (VerifyState.COMPLETED.getValue() == currentBill.getShort("verifystate")) {
                        currentBill.set("auditstatus", BalanceAuditStatus.Complete.getValue());
                    }
                }
                currentBill.setEntityStatus(EntityStatus.Update);
            }
        }
        return new RuleExecuteResult();
    }
}



