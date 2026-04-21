package com.yonyoucloud.fi.cmp.checkStockApply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.checkstockapply.CheckStockApply;
import com.yonyoucloud.fi.cmp.enums.YesOrNoEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpLock;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 票据入库弃审规则
 */
@Slf4j
@Component
public class CheckStockApplyUnAuditRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            log.info("CheckStockUnAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            CheckStockApply currentBill = MetaDaoHelper.findById(CheckStockApply.ENTITY_NAME, bizobject.getId());
            log.info("CheckStockUnAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100718"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D0","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100719"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805D1","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }

            List<String> ids = new ArrayList<>();
            ids.add(bizobject.getId().toString());
            List<CheckStockApply> checkStorkApplyRows = getCheckStorkApplyById(ids);
            abandonCheckStocks(checkStorkApplyRows);
            bizobject.set("auditstatus", AuditStatus.Incomplete.getValue());
        }

        return new RuleExecuteResult();
    }

    /**
     * 根据id查询“支票入库”单据最新状态
     *
     * @param ids
     * @return
     * @throws Exception
     */
    private List<CheckStockApply> getCheckStorkApplyById(List<String> ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(CheckStockApply.ENTITY_NAME, querySchema, null);
    }

    /**
     * 撤回校验
     *
     * @param checkStorkApplyRows
     * @throws Exception
     */
    private void abandonCheckStocks(List<CheckStockApply> checkStorkApplyRows) throws Exception {
        List<Long> ids = new ArrayList<>();
        checkStorkApplyRows.forEach(apply -> {
            ids.add(apply.getId());
        });
        List<CheckStock> cpmCheckStocks = getCpmCheckStocks(ids);
        for (CheckStock stock : cpmCheckStocks) {
            if (!(CmpCheckStatus.InStock.getValue().equals(stock.getCheckBillStatus()) && CmpLock.NO.getValue().equals(stock.getIsLock()))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100720"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C1", "存在已使用/处置的支票，不允许撤回") /* "存在已使用/处置的支票，不允许撤回" */);
            }
            if (stock.getOccupy() != null && stock.getOccupy() == YesOrNoEnum.YES.getValue()) {
                // 若支票编号已被预占，则提示失败
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2290728804B00000", "支票编号已被预占，不能撤回！"));
            }
        }
        List<CheckStockApply> updateRow = getUpdateApplyList(checkStorkApplyRows, AuditStatus.Incomplete);
        for (int i = 0; i < cpmCheckStocks.size(); i++) {
            Long id = cpmCheckStocks.get(i).getId();
            MetaDaoHelper.deleteByObjectId(CheckStock.ENTITY_NAME, id);
        }
//        MetaDaoHelper.update(CheckStockApply.ENTITY_NAME, updateRow);
    }

    /**
     * 根据主表id获取“支票工作台”单据
     *
     * @param ids
     * @return
     * @throws Exception
     */
    private List<CheckStock> getCpmCheckStocks(List<Long> ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("mainid").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);
    }

    /**
     * 支票入库，审核弃审，批量修改审批状态
     * @param checkStorkApplyRows
     * @param status
     * @return
     */
    private List<CheckStockApply> getUpdateApplyList(List<CheckStockApply> checkStorkApplyRows, AuditStatus status) {
        if (null == checkStorkApplyRows || checkStorkApplyRows.size() < 0) {
            return null;
        }
        List<CheckStockApply> updateList = new ArrayList<>();
        for (CheckStockApply apply : checkStorkApplyRows) {
            CheckStockApply update = new CheckStockApply();
            update.setEntityStatus(EntityStatus.Update);
            update.setId(apply.getId());
            update.setAuditstatus(status.getValue());
            // 审核、弃审、设置审批人，审批时间
            if (AuditStatus.Complete == status) {// 审批
                update.setAuditorId(AppContext.getCurrentUser().getId());
                update.setAuditor(AppContext.getCurrentUser().getName());
                update.setAuditDate(BillInfoUtils.getBusinessDate());
                update.setAuditTime(BillInfoUtils.getBusinessDate());
            } else {
                update.setAuditorId(null);
                update.setAuditor(null);
                update.setAuditDate(null);
                update.setAuditTime(null);
            }
            updateList.add(update);
        }
        return updateList;
    }
}
