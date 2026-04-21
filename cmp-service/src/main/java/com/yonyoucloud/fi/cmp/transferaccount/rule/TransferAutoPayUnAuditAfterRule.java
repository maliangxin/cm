package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.transferaccount.service.TransferAccountService;
import com.yonyoucloud.fi.cmp.transferaccount.util.AssertUtil;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 银行对账单、认领单业务处理生成的同名账户划转，可以提交撤回、审批撤回，和是否传资金结算无关。 *
 */
@Component
@Slf4j
public class TransferAutoPayUnAuditAfterRule extends AbstractCommonRule {

    @Autowired
    private TransferAccountService transferAccountService;

    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    private CheckStatusService checkStatusService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            AssertUtil.isNotNull(bizobject, () -> new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400779", "获取审批上下文对象失败....") /* "获取审批上下文对象失败...." */));
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizobject.getId());
            //1。数据库是否推送结算
            if (transferAccount.getIsSettlement() == null || !transferAccount.getIsSettlement()) {
                //不推送结算 结算状态为 已结算补单 并且 支付状态为 已支付补单 弃审自动取消线下支付
                if (transferAccount.getSettlestatus().getValue() == SettleStatus.SettledRep.getValue() &&
                        transferAccount.getPaystatus().getValue() == PayStatus.SupplPaid.getValue()) {
                    log.info("线下支付自动取消....");
                    //为了能够取消线下支付 需要把支付状态改成支付成功
                    transferAccount.setPaystatus(PayStatus.OfflinePay);
                    this.cancelOffLinePay(CtmJSONObject.toJSON(transferAccount));
                    //再加个 查询
                    transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizobject.getId());
                    //为了后续流程  还需要再把支付状态 和结算状态改回来
                    transferAccount.setPaystatus(PayStatus.SupplPaid);
                    transferAccount.setSettlestatus(SettleStatus.SettledRep);
                    //清空结算成功时间，结算金额
                    transferAccount.setSettledate(null);
                    transferAccount.setSettleSuccessAmount(null);
                    transferAccount.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
                    // 更新支票状态并清空值
                    if (transferAccount.get("checkid") != null) {
                        Long checkId = transferAccount.get("checkid");
                        CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                        // 不推结算，转账单弃审时，清空金额，出票日期，出票人员
                        checkStock.setCheckBillStatus(CmpCheckStatus.Billing.getValue());
                        checkStock.setAmount(null);
                        checkStock.setDrawerDate(null);
                        checkStock.setDrawerName(null);
                        checkStock.setPayeeName(null);
                        checkStock.setEntityStatus(EntityStatus.Update);
                        checkStatusService.recordCheckStatusByCheckId(checkStock.getId(),checkStock.getCheckBillStatus());
                        MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                    }
                }
            } else {
                if (transferAccount.getSettlestatus().getValue() == SettleStatus.SettledRep.getValue() || transferAccount.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()) {
                    //如果生成了凭证则删除
                    //modified by lichaor 20260107 和亮亮沟通，去掉状态判断，要不可能因为voucherSatus不对的问题，导致没有调用删除事项的接口
                    //if (transferAccount.getVoucherstatus() == VoucherStatus.Created || transferAccount.getVoucherstatus() == VoucherStatus.POSTING || transferAccount.getVoucherstatus() == VoucherStatus.POST_SUCCESS) {
                        transferAccount.set("entityName", TransferAccount.ENTITY_NAME);
                        transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
                        CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(transferAccount);
                        if (!deleteResult.getBoolean("dealSucceed")) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100348"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008F","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
                        }
                    //}
                    // 发接口删除凭证后，清空转账单的凭证号和凭证期间
                    transferAccount.setVoucherNo(null);
                    transferAccount.setVoucherPeriod(null);
                    transferAccount.setVoucherId(null);
                    transferAccount.setVoucherstatus(VoucherStatus.Empty.getValue());
                    //清空结算成功时间，结算金额
                    transferAccount.setSettledate(null);
                    transferAccount.setSettleSuccessAmount(null);
                    transferAccount.setCollectid(null);
                    transferAccount.setPaymentid(null);
                    transferAccount.setIsSettleAgain(false);
                    if (transferAccount.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()) {
                        // 只有结算成功的撤回需要改为待结算，已结算补单的不改
                        transferAccount.setSettlestatus(SettleStatus.noSettlement);
                    }
                    transferAccount.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
                }
            }
        }
        //3。end
        return new RuleExecuteResult();
    }

    /**
     * 取消线下支付
     *
     * @param ctmJSONObject
     */
    private void cancelOffLinePay(CtmJSONObject ctmJSONObject) {
        CtmJSONObject param = new CtmJSONObject();
        try {
            CtmJSONArray rows = new CtmJSONArray();
            rows.add(ctmJSONObject);
            param.put("rows", rows);
            transferAccountService.cancelOffLinePay(param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101647"),e.getMessage());
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("rows"));
        }
    }
}
