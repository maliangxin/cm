package com.yonyoucloud.fi.cmp.checkmanage.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 支票处置驳回和撤销驳回以及终止规则
 *
 * @Author: 闫小凯
 * @ClassName: CheckManageDeleteBpmAfterRule
 * @Date: 2023/06/19 9:00
 **/
@Component("checkManageDeleteBpmBeforeRule")
public class CheckManageDeleteBpmBeforeRule extends AbstractCommonRule {
    /**
     * 驳回
     */
    private static final String ACTION_REJECT = "REJECTTOSTART";

    /**
     * 撤销驳回
     */
    private static final String ACTION_WITHDRAW_REJECT = "WITHDRAWREJECTTOSTART";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (CollectionUtils.isNotEmpty(bills)) {
            BizObject bill = bills.get(0);
            CheckManage checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bill.getId(), 2);
            short auditStatus = checkManage.getAuditstatus();
            if (ACTION_REJECT.equalsIgnoreCase(billContext.getDeleteReason())) {
                if (VerifyState.SUBMITED.getValue() != auditStatus) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101132"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18999E1805B8002A","单据状态不是审批中，不可驳回！") /* "单据状态不是审批中，不可驳回！" */);
                }
            } else if (ACTION_WITHDRAW_REJECT.equalsIgnoreCase(billContext.getDeleteReason())) {
                //撤销驳回
                if (VerifyState.REJECTED_TO_MAKEBILL.getValue() != auditStatus) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101133"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18999E1805B80029","单据状态不是驳回到制单，不可撤销驳回！") /* "单据状态不是驳回到制单，不可撤销驳回！" */);
                }
            }

        }
        return new RuleExecuteResult();
    }
}
