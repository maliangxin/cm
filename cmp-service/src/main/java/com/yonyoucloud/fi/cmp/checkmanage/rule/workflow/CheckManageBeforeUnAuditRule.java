package com.yonyoucloud.fi.cmp.checkmanage.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>支票处置审批之后撤回后规则</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-06-19 17:55
 */
@Slf4j
@Component("checkManageBeforeUnAuditRule")
public class CheckManageBeforeUnAuditRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        /*for (BizObject bizObject : bills) {
            CheckManage checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, bizObject.getId(), 2);
        }*/
        return new RuleExecuteResult();
    }

}
