package com.yonyoucloud.fi.cmp.billclaim.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.RecheckStatus;
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
 * @description: 我的认领驳回到制单或终止前规则
 * "deleteAll":删除流程实例或撤回流程到初始状态
 * "withdraw":(终审)撤销审核
 * "REJECTTOSTART":驳回制单
 * "WITHDRAWREJECTTOSTART":撤销驳回制单
 * "ACTIVITI_DELETED":终止
 * @author: zhoulyu@yonyou.com
 * @date: 2024/06/18 15:49
 */

@Component("beforeDeleteBpmBillClaimRule")
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BeforeDeleteBpmBillClaimRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        String deleteReason = billContext.getDeleteReason();
        List<BizObject> bills = getBills(billContext, map);
        String fullName = billContext.getFullname();
        for (BizObject bizobject : bills) {
            BizObject currentBill = MetaDaoHelper.findById(fullName, bizobject.getId(), ICmpConstant.CONSTANT_TWO);
            currentBill.set("auditDate", null);
            currentBill.set("auditTime", null);
            currentBill.setEntityStatus(EntityStatus.Update);
            if (BooleanUtils.b(currentBill.get(ICmpConstant.IS_WFCONTROLLED))) {
                if ("ACTIVITI_DELETED".equals(deleteReason)) {
                    // 终止
                    currentBill.set("recheckstatus", RecheckStatus.Terminated.getValue());
                    MetaDaoHelper.update(fullName, currentBill);
                }
                if ("REJECTTOSTART".equals(deleteReason)) {
                    // 驳回制单
                    currentBill.set("recheckstatus", RecheckStatus.Rejected.getValue());
                    MetaDaoHelper.update(fullName, currentBill);
                }
                if ("WITHDRAWREJECTTOSTART".equals(deleteReason)) {
                    // 撤销驳回到制单
                    currentBill.set("recheckstatus", RecheckStatus.Submited.getValue());
                    MetaDaoHelper.update(fullName, currentBill);
                }
                if ("deleteAll".equalsIgnoreCase(deleteReason)) {
                    // 删除流程实例或撤回流程到初始状态
                    currentBill.set("recheckstatus", RecheckStatus.Saved.getValue());
                    currentBill.set("verifystate", null);
                    MetaDaoHelper.update(fullName, currentBill);
                }
            }
        }
        return new RuleExecuteResult();
    }
}



