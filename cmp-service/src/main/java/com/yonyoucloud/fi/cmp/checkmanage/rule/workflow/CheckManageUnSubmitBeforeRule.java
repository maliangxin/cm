package com.yonyoucloud.fi.cmp.checkmanage.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <h1>支票处置撤回前规则</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-05-31 09:28
 */
@Component
public class CheckManageUnSubmitBeforeRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        ArrayList<CheckStock> stockArrayList = new ArrayList<>();
        for (BizObject bizObject : bills) {
            CheckManage checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bizObject.getId(), 2);
            short verifystate = Short.parseShort(checkManage.get("verifystate").toString());
            String code = bizObject.get("code");
            if (verifystate == VerifyState.INIT_NEW_OPEN.getValue() || verifystate == VerifyState.TERMINATED.getValue()
                    || verifystate == VerifyState.REJECTED_TO_MAKEBILL.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102375"),code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450000D","单据状态不是审批中/审批通过状态，不允许撤回！") /* "单据状态不是审批中/审批通过状态，不允许撤回！" */);
            }
            if (checkManage.getGenerateType() != 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102376"),code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450000E","单据状态不是手工录入单据，不允许撤回！") /* "单据状态不是手工录入单据，不允许撤回！" */);
            }
            if (!BooleanUtils.b((Boolean)bizObject.get("isWfControlled"))) {
                // 未启用审批流，撤回时
                // 未启动审批流，单据直接撤回
                result = BillBiz.executeRule("unaudit", billContext, paramMap);
                result.setCancel(true);
            }
        }
        return result;
    }
}
