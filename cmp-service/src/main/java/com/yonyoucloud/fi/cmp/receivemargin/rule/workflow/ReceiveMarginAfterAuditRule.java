package com.yonyoucloud.fi.cmp.receivemargin.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetReceivemarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.SettleFlagEnum;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.receivemargin.service.ReceiveMarginService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 收到保证金审核后规则
 * *
 *
 * @author xuxbo
 * @date 2023/8/3 14:31
 */

@Slf4j
@Component
public class ReceiveMarginAfterAuditRule extends AbstractCommonRule {
    @Autowired
    private ReceiveMarginService receiveMarginService;
    @Autowired
    @Qualifier("stwbReceiveMarginServiceImpl")
    private StwbBillService stwbBillService;
    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetReceivemarginManagerService cmpBudgetReceivemarginManagerService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {
            String id = bizObject.getId().toString();
            try {
                log.info("ReceiveMarginAfterAuditRule bizObject, id = {}, pubTs = {}", bizObject.getId(), bizObject.getPubts());
                ReceiveMargin currentBill = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId());
                log.info("ReceiveMarginAfterAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                //如果结算标识是需要结算：推送结算
                if (currentBill.getSettleflag() == 1) {
                    if (currentBill.getOurassociationstatus() != null && currentBill.getOurassociationstatus() == 1) {
                        // 本方关联状态为已关联时，设置本方传结算字段
                        currentBill.set("ourcheckno", currentBill.getOurcheckno());         // 本方财资统一对账码
                        currentBill.set("ourbankbillid", currentBill.getOurbankbillid());   // 本方流水ID
                        currentBill.set("ourbillclaimid", currentBill.getOurbillclaimid()); // 本方认领单ID
                    }
                    //推送资金结算
                    List<BizObject> currentBillList = new ArrayList<>();
                    currentBillList.add(currentBill);
                    stwbBillService.pushBill(currentBillList, false);// 推送资金结算
                    currentBill.set(ICmpConstant.AUDIT_STATUS, AuditStatus.Complete.getValue());
                }
            } catch (Exception e) {
                log.error("catch Exception", e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102084"),e.getMessage());
            } finally {
                JedisLockUtils.unlockRuleWithOutTrace(map);
            }
            ReceiveMargin currentBill = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId());
            //结算状态为已结算补单/空，是否占预算为预占成功时，删除预占，进行实占；
            currentBill.put("_entityName","cmp.receivemargin.ReceiveMargin");
            if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_RECEIVEMARGIN)) {
                receiveMarginExecuteAudit(currentBill);
            }
            receiveMarginService.changeSettleFlagAfterAudit(currentBill);
            //刷新pubts
            ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId(), null);
            bizObject.setPubts(receiveMarginNew.getPubts());
        }
        return new RuleExecuteResult();
    }

    /**
     * 实占预算
     *
     * @param receiveMargin
     * @throws Exception
     */
    public void receiveMarginExecuteAudit(ReceiveMargin receiveMargin) throws Exception {
        ReceiveMargin receiveMarginNew = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, receiveMargin.getId(), 2);
        Short settleflag = receiveMarginNew.getSettleflag();
        ReceiveMargin update = new ReceiveMargin();
        //结算状态为已结算补单/空时 且 是否结算 = 否 时 ，如果是否结算 = 是，这个时候需要回调更新结算成功时间时才实占
        if (settleflag == SettleFlagEnum.NO.getValue() && (receiveMarginNew.getSettlestatus() == null || receiveMarginNew.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue())) {
            //实占预算
            //占用成功后更新是否占预算为实占成功
            if (cmpBudgetReceivemarginManagerService.budgetSuccess(receiveMarginNew, true,true)) {
                update.setId(receiveMargin.getId());
                update.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                update.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, update);
            }
        }
    }
}
