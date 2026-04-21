package com.yonyoucloud.fi.cmp.fundexpense.rule.deleteBpm;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cmpentity.ExpenseAuditStatus;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.BizException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * desc:驳回和撤销驳回规则
 * author:wangqiangac
 * date:2024/5/21 15:55
 */
    @Component("fundexpenseDeleteBpmAfterRule")
public class FundexpenseDeleteBpmAfterRule extends AbstractCommonRule {
    /**
     * 驳回到开始环节
     */
    private static final String ACTION_REJECT = "REJECTTOSTART";

    /**
     * 撤销驳回
     */
    private static final String ACTION_WITHDRAW_REJECT = "WITHDRAWREJECTTOSTART";

    /**
     * 终止
     */
    private static final String ACTION_TERMINATED = "ACTIVITI_DELETED";

    /**
     * 撤回流程，反提交，删除流程
     */
    private static final String ACTION_DELETEALL = "deleteAll";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {

        List<BizObject> bills = getBills(billContext, map);
        if (CollectionUtils.isNotEmpty(bills)) {
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            fundexpense = MetaDaoHelper.findById(Fundexpense.ENTITY_NAME, fundexpense.getId());
            short auditStatus = fundexpense.getAuditstatus();
            if (ACTION_REJECT.equalsIgnoreCase(billContext.getDeleteReason())) {
                //驳回
                if (ExpenseAuditStatus.approval.getValue() != auditStatus) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102041"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_DRFT-BE_17FE1DB204180799","状态%s，不可驳回！") /* "状态%s，不可驳回！" */, ExpenseAuditStatus.getName(auditStatus)));
                }
                fundexpense.setAuditstatus(ExpenseAuditStatus.retured.getValue());
            } else if (ACTION_WITHDRAW_REJECT.equalsIgnoreCase(billContext.getDeleteReason())) {
                //撤销驳回
                if (ExpenseAuditStatus.retured.getValue() != auditStatus) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102042"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_DRFT-BE_17FE1DB204180798","状态%s，不可撤销驳回！") /* "状态%s，不可撤销驳回！" */, ExpenseAuditStatus.getName(auditStatus)));
                }
                fundexpense.setAuditstatus(ExpenseAuditStatus.approval.getValue());
            } else if (ACTION_TERMINATED.equalsIgnoreCase(billContext.getDeleteReason())){
                //终止
                fundexpense.setAuditstatus(ExpenseAuditStatus.STOPPED.getValue());
            } else if (ACTION_DELETEALL.equalsIgnoreCase(billContext.getDeleteReason())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102043"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080095", "不支持该操作") /* "不支持该操作" */);
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102043"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080095", "不支持该操作") /* "不支持该操作" */);
            }
            fundexpense.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(Fundexpense.ENTITY_NAME, fundexpense);
        }
        return new RuleExecuteResult();
    }
}
