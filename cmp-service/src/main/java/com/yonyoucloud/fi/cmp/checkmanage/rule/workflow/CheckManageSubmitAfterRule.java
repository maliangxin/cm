package com.yonyoucloud.fi.cmp.checkmanage.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>支票处置提交后规则</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-05-31 09:07
 */
@Component
public class CheckManageSubmitAfterRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        for (BizObject bizObject : bills) {
            CheckManage checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bizObject.getId(), 2);
            // 能走到提交后规则，说明单据配置了审批流，提交后单据审批状态变更为审批中。
            checkManage.setAuditstatus(VerifyState.SUBMITED.getValue());
            EntityTool.setUpdateStatus(checkManage);
            MetaDaoHelper.update(CheckManage.ENTITY_NAME, checkManage);
        }
        return result;
    }
}
