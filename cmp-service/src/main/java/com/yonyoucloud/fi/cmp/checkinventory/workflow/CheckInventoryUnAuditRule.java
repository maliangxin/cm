package com.yonyoucloud.fi.cmp.checkinventory.workflow;

import cn.hutool.core.date.DateUtil;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.util.Lists;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory_b;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CheckResultStatus;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <h1>弃审前规则</h1>
 */
@Slf4j
@Component
public class CheckInventoryUnAuditRule extends AbstractCommonRule {

    @Autowired
    CheckStatusService checkStatusService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            log.info("CheckInventoryUnAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            CheckInventory currentBill = MetaDaoHelper.findById(CheckInventory.ENTITY_NAME, bizobject.getId());
            log.info("CheckInventoryUnAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100698"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180383", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            //  盘点日期降序查
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
            queryConditionGroup.addCondition(QueryCondition.name("accentity").eq(currentBill.getAccentity()));
            querySchema.addCondition(queryConditionGroup);
            querySchema.addOrderBy(new QueryOrderby("inventorydate", "desc"));
            Date inventorydate = (Date) MetaDaoHelper.query(CheckInventory.ENTITY_NAME, querySchema).get(0).get("inventorydate");
            int compare = DateUtil.compare(currentBill.getInventorydate(), inventorydate);
            //   已经存在大于本次盘点日期的盘点单据，不允许撤回
            if (compare < 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101733"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_185FB29205380004", "已经存在大于本次盘点日期的盘点单据，不允许撤回") /* "已经存在大于本次盘点日期的盘点单据，不允许撤回" */);
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101734"),MessageUtils.getMessage("P_YS_FI_AR_0000059097") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101735"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_AR_0000059097","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
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
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101736"),MessageUtils.getMessage("P_YS_FI_AR_0000058988") /* "审核日期不能早于单据日期" */);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101737"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_AR_0000058988","审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            List<CheckInventory_b> checkInventory_bList = currentBill.get("CheckInventory_b");
            for (CheckInventory_b checkInventoryB : checkInventory_bList) {
                if (Short.parseShort(checkInventoryB.getCheckresult()) != CheckResultStatus.InventoryProfit.getValue()) {
                    if (null != checkInventoryB.get("checkid")) {
                        String[] checkids = checkInventoryB.get("checkid").toString().split(",");
                        List<String> checkIdList = Lists.newArrayList(checkids);
                        List<Map<String, Object>> checkStockList = MetaDaoHelper.queryByIds(CheckStock.ENTITY_NAME, "*", checkIdList);
                        /*if (checkStockList.size() > 0) {
                            long count = checkStockList.stream().filter(e -> Integer.parseInt(e.get("checkBillStatus").toString()) != 1).count();
                            //   校验支票状态均为已入库状态
                            if (count > 0) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101738"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_185FB22804800006", "存在已使用/处置的支票，不允许撤回") *//* "存在已使用/处置的支票，不允许撤回" *//*);
                            }
                        }*/
                        handleStatusUpdate(checkStockList);
                    }
                }
            }
            // 已审核
            bizobject.set("auditstatus", AuditStatus.Incomplete.getValue());
//            bizobject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
            bizobject.set("voucherstatus", VoucherStatus.Empty.getValue());
            bizobject.set("voucherNo", null);
            bizobject.set("voucherPeriod", null);
            bizobject.set("voucherId", null);
            bizobject.set("auditorId", null);
            bizobject.set("auditor", null);
            bizobject.set("auditDate", null);
            bizobject.set("auditTime", null);
            bizobject.set("settleSuccessTime", null);
        }
        return new RuleExecuteResult();
    }

    private void handleStatusUpdate(List<Map<String, Object>> checkStockList) throws Exception {
        if (checkStockList.isEmpty()) {
            return;
        }
        List<CheckStock> blist = new ArrayList<>();
        List<Map<String, Object>> oldBlist = new ArrayList<>();
        checkStockList.forEach(sub -> {
            CheckStock checkStock = new CheckStock();
            checkStock.init(sub);
            //盘点单撤回时，如果所盘点的支票的最新支票状态为已入库，撤回时，支票状态变为盘点中，否则支票状态不变
            if (CmpCheckStatus.InStock.getValue().equals(checkStock.getCheckBillStatus())) {
                EntityTool.setUpdateStatus(checkStock);
                checkStock.setCheckBillStatus(CmpCheckStatus.Inventory.getValue());
                blist.add(checkStock);
                oldBlist.add(sub);
            }
        });
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, blist);
        checkStatusService.recordCheckStatusByMap(oldBlist, CmpCheckStatus.Inventory.getValue());
    }

}
