package com.yonyoucloud.fi.cmp.checkmanage.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.lang.BooleanUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>支票处置提交前规则</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-06-13 09:07
 */
@Component
public class CheckManageSubmitBeforeRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            short auditstatus = Short.parseShort(bizObject.get("auditstatus").toString());

            String code = bizObject.get("code");
            if (auditstatus == VerifyState.SUBMITED.getValue() || auditstatus == VerifyState.COMPLETED.getValue()  || auditstatus == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100423"),code + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18905AFC0450000C","单据状态不是已保存/已驳回状态，不允许提交！") /* "单据状态不是已保存/已驳回状态，不允许提交！" */);
            }
            CheckManage checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bizObject.getId(), 2);
            if (!BooleanUtils.b((Boolean)bizObject.get("isWfControlled"))) {
                // 单据未配置审批流，提交后单据审批状态变更为审批通过
                bizObject.put("auditstatus", VerifyState.COMPLETED.getValue());
                result = BillBiz.executeRule("audit", billContext, paramMap);
                result.setCancel(true);
            }
        }
        return result;
    }
}
