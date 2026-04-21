package com.yonyoucloud.fi.cmp.checkStockApply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 支票入库，删除前规则
 */
@Component("checkStockApplyBeforDeleteRule")
public class CheckStockApplyBeforDeleteRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bill : bills) {
            if (AuditStatus.Complete.getValue() == (Short) bill.get("auditstatus")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100951"),bill.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805B3","已审批，不能进行删除！") /* "已审批，不能进行删除！" */);
            }
            short verifystate = Short.parseShort(bill.get("verifystate").toString());
            if (verifystate == VerifyState.COMPLETED.getValue() || verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100952"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187AF70A05600008", "单据状态不是初始开立态/驳回到制单状态，不允许删除") /* "单据状态不是初始开立态/驳回到制单状态，不允许删除" */);
            }
        }
        return new RuleExecuteResult();
    }

}
