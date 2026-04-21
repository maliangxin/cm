package com.yonyoucloud.fi.cmp.cashinventory.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cashinventory.CashInventory;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @since 2022-01-17
 */
@Slf4j
@Component
public class InventoryAuditRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizobject : bills) {
            log.info("InventoryAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            CashInventory cashInventory = MetaDaoHelper.findById(CashInventory.ENTITY_NAME,bizobject.getId());
            log.info("InventoryAuditRule currentBill, id = {}, pubTs = {}", cashInventory.getId(), cashInventory.getPubts());
            if (null == cashInventory) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102243"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180510","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            Date cashInventoryPubts = bizobject.getPubts();
            if (cashInventoryPubts != null) {
                if (!cashInventoryPubts.equals(cashInventory.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102244"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180512","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            Date date = BillInfoUtils.getBusinessDate();
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (currentDate.compareTo(date) < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102245"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180511","审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            // 已审核
            bizobject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
            bizobject.set("auditorId", AppContext.getCurrentUser().getId());
            bizobject.set("auditor", AppContext.getCurrentUser().getName());
            bizobject.set("auditDate", new Date());
            bizobject.set("auditTime", new Date());
        }
        return new RuleExecuteResult();
    }

}
