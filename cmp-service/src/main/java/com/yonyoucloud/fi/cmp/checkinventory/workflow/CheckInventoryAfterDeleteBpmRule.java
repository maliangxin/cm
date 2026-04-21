package com.yonyoucloud.fi.cmp.checkinventory.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.util.Lists;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory_b;
import com.yonyoucloud.fi.cmp.checkinventory.service.CheckInventoryService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CheckResultStatus;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审批流终止规则
 */
@Component("checkInventoryAfterDeleteBpmRule")
@Slf4j
public class CheckInventoryAfterDeleteBpmRule extends AbstractCommonRule {

    @Autowired
    private CheckInventoryService checkInventoryService;
    @Autowired
    CheckStatusService checkStatusService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        String deleteReason = billContext.getDeleteReason();
        BizObject bizObject = getBills(billContext, map).get(0);
        CheckInventory checkInventory = MetaDaoHelper.findById(CheckInventory.ENTITY_NAME, bizObject.get("id"), 3);
        if (checkInventory.getIsWfControlled()) {
            switch (deleteReason) {
                case "REJECTTOSTART": {
                    List<CheckInventory_b> billbs = checkInventory.CheckInventory_b();
                    if (!billbs.isEmpty()) {
                        for (CheckInventory_b checkInventoryB : billbs) {
                            if (Short.parseShort(checkInventoryB.getCheckresult()) != CheckResultStatus.InventoryProfit.getValue()) {
                                if (null != checkInventoryB.get("checkid")) {
                                    String[] checkids = checkInventoryB.get("checkid").toString().split(",");
                                    List<String> checkIdList = Lists.newArrayList(checkids);
                                    List<Map<String, Object>> checkStockList = MetaDaoHelper.queryByIds(CheckStock.ENTITY_NAME, "*", checkIdList);
                                    if (checkStockList.size() > 0) {
                                        handleStatusUpdate(checkStockList);
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case "WITHDRAWREJECTTOSTART":
                    //撤回
                    break;
                case "ACTIVITI_DELETED": {
                    List<CheckInventory_b> billbs = checkInventory.CheckInventory_b();
                    if (!billbs.isEmpty()) {
                        checkInventoryService.afterSaveBillToCmp(billbs, checkInventory.getAccentity());
                    }
                    break;
                }
                default:
                    break;
            }
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
            if (CmpCheckStatus.InStock.getValue().equals(checkStock.getCheckBillStatus())) {
                EntityTool.setUpdateStatus(checkStock);
                checkStock.setCheckBillStatus(CmpCheckStatus.Inventory.getValue());
                blist.add(checkStock);
                oldBlist.add(sub);
            }
        });
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, blist);
        checkStatusService.recordCheckStatusByMap(oldBlist,CmpCheckStatus.Inventory.getValue());
    }
}



