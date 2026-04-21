package com.yonyoucloud.fi.cmp.checkinventory.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.model.LoginUser;
import com.yonyou.workbench.util.Lists;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory_b;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CheckResultStatus;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支票盘点，保存后规则
 */
@Slf4j
@Component("checkInventoryAfterSaveRule")
public class CheckInventoryAfterSaveRule extends AbstractCommonRule {
    @Autowired
    CheckStatusService checkStatusService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bill = getBills(billContext, paramMap).get(0);
        QuerySchema querySchema = new QuerySchema().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("mainid").eq(bill.getId()));
        querySchema.addCondition(queryConditionGroup);
        List<CheckInventory_b> listb = MetaDaoHelper.queryObject(CheckInventory_b.ENTITY_NAME, querySchema, null);
        if (listb == null || listb.size() < 1) {
            return new RuleExecuteResult();
        }
        handleCalCheckInventory(listb, bill);
        for (CheckInventory_b checkInventoryB : listb) {
            if (Short.parseShort(checkInventoryB.getCheckresult()) != CheckResultStatus.InventoryProfit.getValue()) {
                if (null != checkInventoryB.get("checkid")) {
                    String[] checkids = checkInventoryB.get("checkid").toString().split(",");
                    List<String> checkIdList = Lists.newArrayList(checkids);
                    handleStatusUpdate(MetaDaoHelper.queryByIds(CheckStock.ENTITY_NAME, "*", checkIdList), bill.getId());
                }
            }
        }
        return new RuleExecuteResult();
    }

    private void handleStatusUpdate(List<Map<String, Object>> checkStockList, Long mainid) throws Exception {
        if (checkStockList.isEmpty()) {
            return;
        }
        //  保存后更新支票工作台的支票状态为盘点中
        List<CheckStock> blist = new ArrayList<>();
        List<Map<String, Object>> oldBlist = new ArrayList<>();
        //补录盘点单时，支票状态不等于“已入库”的支票，盘点单保存时，支票状态不更新，只有支票状态=已入库的支票更新支票状态为“盘点中”
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
        checkStatusService.recordCheckStatusByInventory(oldBlist, CmpCheckStatus.Inventory.getValue(), mainid);
    }

    private void handleCalCheckInventory(List<CheckInventory_b> checkInventoryBList, BizObject bill) throws Exception {
        Map<String, List<CheckInventory_b>> map = checkInventoryBList.stream().collect(Collectors.groupingBy(item -> item.getCheckresult()));
        //台账
        int checkquantity = checkInventoryBList.stream().mapToInt(t -> Math.toIntExact(t.getCheckquantity())).sum();
        //实物
        int materialquantity = checkInventoryBList.stream().mapToInt(t -> Math.toIntExact(t.getMaterialquantity())).sum();
        int inventoryProfit = 0;
        int inventoryLoss = 0;
        //盘盈
        if (map.get(String.valueOf(CheckResultStatus.InventoryProfit.getValue())) != null && map.get(String.valueOf(CheckResultStatus.InventoryProfit.getValue())).size() > 0) {
            inventoryProfit = map.get(String.valueOf(CheckResultStatus.InventoryProfit.getValue())).stream().mapToInt(t -> Math.toIntExact(t.getDiffquantity())).sum();
        }
        if (map.get(String.valueOf(CheckResultStatus.InventoryLoss.getValue())) != null && map.get(String.valueOf(CheckResultStatus.InventoryLoss.getValue())).size() > 0) {
            //盘亏
            inventoryLoss = map.get(String.valueOf(CheckResultStatus.InventoryLoss.getValue())).stream().mapToInt(t -> Math.toIntExact(t.getDiffquantity())).sum();
        }
            //相符
        int toConform = checkquantity-inventoryLoss;
        StringBuffer sb = new StringBuffer();
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CTM-CMP-MD_1808A9F004D00829", "台账数量(张)") /* "台账数量(张)" */);
        sb.append(checkquantity);
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F", "张") /* "张" */);
        sb.append(",");
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CTM-CMP-MD_1808A9F004D00175", "实物数量(张)") /* "实物数量(张)" */);
        sb.append(materialquantity);
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F", "张") /* "张" */);
        sb.append(",");
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108E6604B81B9B", "盘盈") /* "盘盈" */);
        sb.append(inventoryProfit);
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F", "张") /* "张" */);
        sb.append(",");
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108E6604B81B95", "盘亏") /* "盘亏" */);
        sb.append(inventoryLoss);
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F", "张") /* "张" */);
        sb.append(",");
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108E6604B81B9F", "相符") /* "相符" */);
        sb.append(toConform);
        sb.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418015F", "张") /* "张" */);
        CheckInventory checkInventory = new CheckInventory();
        checkInventory.setId(bill.getId());
        checkInventory.setModifyDate(new Date());
        checkInventory.setModifyTime(new Date());
        checkInventory.setCheckresult(sb.toString());
        LoginUser loginUser = AppContext.getCurrentUser();
        checkInventory.setModifier(loginUser.getName());
        checkInventory.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(CheckInventory.ENTITY_NAME, checkInventory);
    }
}
