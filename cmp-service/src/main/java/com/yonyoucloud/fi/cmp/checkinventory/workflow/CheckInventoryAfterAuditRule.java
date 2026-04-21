package com.yonyoucloud.fi.cmp.checkinventory.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory_b;
import com.yonyoucloud.fi.cmp.checkinventory.service.CheckInventoryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 支票盘点审核后规则
 */
@Slf4j
@Component
public class CheckInventoryAfterAuditRule extends AbstractCommonRule {
    @Autowired
    private CheckInventoryService checkInventoryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            try {
                CheckInventory checkInventory = MetaDaoHelper.findById(CheckInventory.ENTITY_NAME, bizobject.getId(),2);
                List<CheckInventory_b> billbs = checkInventory.CheckInventory_b();
                if (!billbs.isEmpty()) {
                    checkInventoryService.afterSaveBillToCmp(billbs,checkInventory.getAccentity());
                }
                // 已审核
                bizobject.putAll(checkInventory);
//                bizobject.set("verifystate", VerifyState.COMPLETED.getValue());
                bizobject.set("auditorId", AppContext.getCurrentUser().getId());
                bizobject.set("auditor", AppContext.getCurrentUser().getName());
                bizobject.set("auditDate", new Date());
                bizobject.set("auditTime", new Date());
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100394"),e.getMessage());
            }
        }
        return new RuleExecuteResult();
    }
}
