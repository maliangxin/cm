package com.yonyoucloud.fi.cmp.paymargin.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetPaymarginManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.marginworkbench.service.MarginWorkbenchService;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.BillAction;
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
public class PayMarginAfterUnAuditRule extends AbstractCommonRule {

    @Autowired
    @Qualifier("stwbPayMarginServiceImpl")
    StwbBillService stwbBillService;

    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    MarginWorkbenchService marginWorkbenchService;

    @Autowired
    private PayMarginService payMarginService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CmpBudgetPaymarginManagerService cmpBudgetPaymarginManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            String id = bizobject.getId().toString();
            try {
                log.info("PayMarginAfterUnAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
                PayMargin currentBill = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizobject.getId());
                log.info("PayMarginAfterUnAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                Boolean settlestatuschange = false;
                if (currentBill.getSettleflag() == 1) {
                    //删除待结算数据:直接删除待结算
                    // todo 两个待结算数据
                    List<BizObject> currentBillList = new ArrayList<>();
                    currentBillList.add(currentBill);
                    stwbBillService.deleteBill(currentBillList);
                    //根据结算状态判断是否删除凭证
                    Short settlestatus = currentBill.getSettlestatus();
                    if (ObjectUtils.isNotEmpty(settlestatus) && (settlestatus == FundSettleStatus.SettleSuccess.getValue() || settlestatus == FundSettleStatus.SettlementSupplement.getValue())
                        && (currentBill.getVoucherstatus() == VoucherStatus.POST_SUCCESS.getValue() || currentBill.getVoucherstatus() == VoucherStatus.Created.getValue())) {
                        //如果是已结算：删除凭证
                        CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(bizobject);
                        if (!deleteResult.getBoolean("dealSucceed")) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100348"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008F","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
                        }
                    }
                    // 设置推送次数为第一次
                    currentBill.set(ICmpConstant.PUSHTIMES, ICmpConstant.FIRST);
                    // 修改结算状态为待结算
                    if (!currentBill.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue()) && !currentBill.getSettlestatus().equals(FundSettleStatus.WaitSettle.getValue())){
                        currentBill.set(ICmpConstant.SETTLE_STATUS, FundSettleStatus.WaitSettle.getValue());
                        settlestatuschange = true;
                    }

                }
                // 新增：不推送结算时，审批撤回后应删除对应的事项分录、凭证
                else {
                    // 删除凭证
                    CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(bizobject);
                    if (!deleteResult.getBoolean("dealSucceed")) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100348"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008F","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
                    }
                }

                CtmJSONObject params = new CtmJSONObject();
                // 弃审调用工作台更新接口
                params.put(ICmpConstant.ACTION, ICmpConstant.UN_AUDIT);
                //保证金原始业务号
                params.put(ICmpConstant.MARGINBUSINESSNO, currentBill.getMarginbusinessno());
                params.put(ICmpConstant.MARGINAMOUNT, currentBill.getMarginamount());
                params.put(ICmpConstant.NATMARGINAMOUNT, currentBill.getNatmarginamount());
                params.put(ICmpConstant.TRADETYPE, currentBill.getTradetype());
                if (ObjectUtils.isNotEmpty(currentBill.getConversionamount())) {
                    params.put(ICmpConstant.CONVERSIONAMOUNT, currentBill.getConversionamount());
                    params.put(ICmpConstant.NATCONVERSIONAMOUNT, currentBill.getNatconversionamount());
                }
                params.put(ICmpConstant.SETTLEFLAG, currentBill.getSettleflag());
                params.put(ICmpConstant.SRC_ITEM, currentBill.getSrcitem());
                //上传结算状态
                params.put(ICmpConstant.SETTLE_STATUS, currentBill.getSettlestatus());
                //上传结算状态是否变更
                params.put(ICmpConstant.SETTLESTATUSCHANGE, settlestatuschange);
                params.put(ICmpConstant.PAYMARGIN, currentBill);
                marginWorkbenchService.payMarginWorkbenchUpdate(params);

                currentBill.set(ICmpConstant.VOUCHER_STATUS, VoucherStatus.Empty.getValue());
                currentBill.set(ICmpConstant.VOUCHERNO, null);
                currentBill.set(ICmpConstant.VOUCHERPERIOD, null);
                currentBill.set(ICmpConstant.VOUCHERID, null);
                currentBill.set(ICmpConstant.VOUCHERSTATUS_ORIGINAL, currentBill.getVoucherstatus());
                currentBill.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, currentBill);
                //撤回提交后，需要释放实占。重新预占 且结算状态应置为待结算、并清空结算成功时间，重新预占
                if (cmpBudgetManagerService.isCanStart(IBillNumConstant.CMP_PAYMARGIN)) {
                    executeUnAudit(currentBill);
                }
                payMarginService.changeSettleFlagAfterUnAudit(currentBill);
                //刷新pubts
                PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, currentBill.getId(), null);
                bizobject.setPubts(payMarginNew.getPubts());
            }catch (Exception e){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102641"),e.getMessage());
            }finally {
                JedisLockUtils.unlockRuleWithOutTrace(map);
            }

        }
        return new RuleExecuteResult();
    }

    /**
     * 删除实占，重新预占
     * @param payMargin
     * @throws Exception
     */
    private void executeUnAudit(PayMargin payMargin) throws Exception {
        PayMargin payMarginNew = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, payMargin.getId());
        Short budgeted = payMarginNew.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return;
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            ResultBudget resultBudget = cmpBudgetPaymarginManagerService.gcExecuteTrueUnAudit(payMargin, payMarginNew, IBillNumConstant.CMP_PAYMARGIN, BillAction.CANCEL_SUBMIT);
            if (resultBudget.isSuccess()) {
                //重新预占
                log.error("重新预占.....");
                //且结算状态应置为待结算、并清空结算成功时间
                ResultBudget budgetResult = cmpBudgetPaymarginManagerService.budget(payMargin, payMargin, IBillNumConstant.CMP_PAYMARGIN, BillAction.SUBMIT);
                if (budgetResult.isSuccess()) {//可能是没有匹配上规则，也可能是没有配置规则
                    payMargin.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                    PayMargin update = new PayMargin();
                    update.setId(payMargin.getId());
                    update.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                    update.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
                } else {
                    payMargin.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    PayMargin update = new PayMargin();
                    update.setId(payMargin.getId());
                    update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    update.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(PayMargin.ENTITY_NAME, update);
                }
            } else {
                log.error("释放实占失败,resultBudget:{}", resultBudget);
            }
        }
    }
}
