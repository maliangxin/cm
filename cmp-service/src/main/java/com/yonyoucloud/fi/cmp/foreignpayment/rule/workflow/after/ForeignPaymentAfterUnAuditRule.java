package com.yonyoucloud.fi.cmp.foreignpayment.rule.workflow.after;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetForeignpaymentManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 支付保证金弃审后置规则*
 * @author xuxbo
 * @date 2023/8/3 15:24
 */

@Slf4j
@Component
public class ForeignPaymentAfterUnAuditRule extends AbstractCommonRule {

    @Autowired
    @Qualifier("stwbForeignPaymentBillServiceImpl")
    StwbBillService stwbBillService;

    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    private CmpBudgetForeignpaymentManagerService cmpBudgetForeignpaymentManagerService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            String id = bizobject.getId().toString();
            try {
                log.info("ForeignPaymentAfterUnAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
                ForeignPayment currentBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizobject.getId());
                log.info("ForeignPaymentAfterUnAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                //删除待结算数据:直接删除待结算
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(currentBill);
                stwbBillService.deleteBill(currentBillList);
                //根据结算状态判断是否删除凭证
                Short settlestatus = currentBill.getSettlestatus();
                if (ObjectUtils.isNotEmpty(settlestatus) && (settlestatus == FundSettleStatus.SettleSuccess.getValue() || settlestatus == FundSettleStatus.SettleProssing.getValue() ||settlestatus == FundSettleStatus.SettlementSupplement.getValue())
                        && (currentBill.getVoucherstatus() == VoucherStatus.POST_SUCCESS.getValue() || currentBill.getVoucherstatus() == VoucherStatus.Created.getValue())) {
                    //如果是已结算：删除凭证
                    CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(bizobject);
                    if (!deleteResult.getBoolean("dealSucceed")) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100348"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008F","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
                    }
                }
                // 修改结算状态为待结算
                if (!currentBill.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue())){
                    currentBill.set(ICmpConstant.SETTLE_STATUS, FundSettleStatus.WaitSettle.getValue());
                }
                currentBill.setTransNumber(null);
                currentBill.setSettlesuccesstime(null);
                currentBill.setSettlesuccessSum(null);
                currentBill.setSettleExchangeRate(null);
                currentBill.setSettleExchangeRateOps(null);
                currentBill.setSettleExchangeRateType(null);
                currentBill.setSettleAmount(null);
                currentBill.setSettleerrorSum(null);



                currentBill.set(ICmpConstant.VOUCHER_STATUS, VoucherStatus.Empty.getValue());
                currentBill.set(ICmpConstant.VOUCHERNO, null);
                currentBill.set(ICmpConstant.VOUCHERPERIOD, null);
                currentBill.set(ICmpConstant.VOUCHERID, null);
                currentBill.set(ICmpConstant.VOUCHERSTATUS_ORIGINAL, currentBill.getVoucherstatus());
                ForeignPayment oldBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, currentBill.getId());
                if (!currentBill.getIsWfControlled()) {//未开启审批流直接释放
                    boolean releaseBudget = releaseBudget(oldBill);
                    if (releaseBudget) {
                        currentBill.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    }
                } else {//开启审批流释放重新预占
                    Short occupyBudget = budgetAfterUnAudit(oldBill);
                    if (occupyBudget != null) {
                        currentBill.setIsOccupyBudget(occupyBudget);
                    }
                }
                currentBill.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ForeignPayment.ENTITY_NAME, currentBill);
            }catch (Exception e){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102195"),e.getMessage());
            }finally {
                JedisLockUtils.unlockRuleWithOutTrace(map);
            }

        }
        return new RuleExecuteResult();
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     *
     * @param foreignPayment
     * @throws Exception
     */
    private boolean releaseBudget(ForeignPayment foreignPayment) throws Exception {
        Short budgeted = foreignPayment.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return false;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            return cmpBudgetForeignpaymentManagerService.releaseBudget(foreignPayment);
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            return cmpBudgetForeignpaymentManagerService.releaseImplement(foreignPayment);
        }
        return false;
    }

    /**
     * 如果是预占就跳过，如果是实占，删除实占，重新预占
     * @param foreignPayment
     * @throws Exception
     */
    private Short budgetAfterUnAudit(ForeignPayment foreignPayment) throws Exception {
        Short budgeted = foreignPayment.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return null;
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            boolean releaseImplement = cmpBudgetForeignpaymentManagerService.releaseImplement(foreignPayment);
            if (releaseImplement) {
                //重新预占
                log.error("重新预占.....");
                //且结算状态应置为待结算、并清空结算成功时间
                foreignPayment.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                boolean budget = cmpBudgetForeignpaymentManagerService.budget(foreignPayment);
                if (budget) {//可能是没有匹配上规则，也可能是没有配置规则
                    return OccupyBudget.PreSuccess.getValue();
                } else {
                    return OccupyBudget.UnOccupy.getValue();
                }
            } else {
                log.error("释放实占失败,releaseImplement:{}", releaseImplement);
            }
        }
        return null;
    }
}
