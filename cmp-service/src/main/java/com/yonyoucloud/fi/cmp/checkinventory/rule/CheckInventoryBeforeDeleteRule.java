package com.yonyoucloud.fi.cmp.checkinventory.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.util.Lists;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory_b;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CheckResultStatus;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/*
 *
 * 支票盘点删除前规则
 */
@Slf4j
@Component("checkInventoryBeforeDeleteRule")
public class CheckInventoryBeforeDeleteRule extends AbstractCommonRule {
    @Autowired
    CheckStatusService checkStatusService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bill : bills) {
            CheckInventory checkInventory = MetaDaoHelper.findById(CheckInventory.ENTITY_NAME, bill.getId(), 3);
            if (null == checkInventory) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101070"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801FE", "单据不存在 id:") /* "单据不存在 id:" */ + bill.getId());
            }
            Date currentPubts = bill.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(checkInventory.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101071"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801FF", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            short verifystate = Short.parseShort(bill.get("verifystate").toString());
            if (verifystate == VerifyState.COMPLETED.getValue() || verifystate == VerifyState.SUBMITED.getValue() || verifystate == VerifyState.TERMINATED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100952"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187AF70A05600008", "单据状态不是初始开立态/驳回到制单状态，不允许删除") /* "单据状态不是初始开立态/驳回到制单状态，不允许删除" */);
            }
            QuerySchema schema = new QuerySchema().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
            queryConditionGroup.addCondition(QueryCondition.name("mainid").eq(checkInventory.getId()));
            schema.addCondition(queryConditionGroup);
            List<CheckInventory_b> checkInventory_bList = MetaDaoHelper.queryObject(CheckInventory_b.ENTITY_NAME, schema, null);
            log.error("盘点单子表数据,checkInventory_bList{}", CtmJSONObject.toJSONString(checkInventory_bList));
            for (CheckInventory_b checkInventoryB : checkInventory_bList) {
                if (Short.parseShort(checkInventoryB.getCheckresult()) != CheckResultStatus.InventoryProfit.getValue()) {
                    if (null != checkInventoryB.get("checkid")) {
                        String[] checkids = checkInventoryB.get("checkid").toString().split(",");
                        List<String> checkIdList = Lists.newArrayList(checkids);
                        log.error("=============handleStatusUpdate方法开始===================");
                        handleStatusUpdate(MetaDaoHelper.queryByIds(CheckStock.ENTITY_NAME, "*", checkIdList));
                        log.error("=============handleStatusUpdate方法结束===================");
                    }
                }
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
            //变为盘点中的支票，恢复为已入库，没有变为盘点中的支票，状态不变
            log.error("=============更新盘点状态为已入库===================");
            if (CmpCheckStatus.Inventory.getValue().equals(checkStock.getCheckBillStatus())) {
                EntityTool.setUpdateStatus(checkStock);
                checkStock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
                checkStock.setIsInventory(null);
                blist.add(checkStock);
                oldBlist.add(sub);
            }
        });
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, blist);
        log.error("=============插入状态表数据开始===================");
        checkStatusService.recordCheckStatusByMap(oldBlist,CmpCheckStatus.InStock.getValue());
        log.error("=============插入状态表数据结束===================");
    }
}
