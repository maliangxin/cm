package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
@Slf4j
@Component
public class TransferUnAuditRule extends AbstractCommonRule {

    @Autowired
    private JournalService journalService;

    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    private AutoConfigService autoConfigService;

    @Autowired
    private CheckStatusService checkStatusService;

    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        // 1。查询现金参数 是否推送结算 是
        Boolean checkFundTransfer = autoConfigService.getCheckFundTransfer();
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);

            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100060"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CF","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }

            BizObject currentBill = MetaDaoHelper.findById(bizobject.getEntityName(), bizobject.getId());

            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100061"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CA","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }

            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100062"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CE","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            String message = "";
            Short auditStatus = Short.parseShort(currentBill.get("auditstatus").toString());
            if (auditStatus != null && auditStatus.equals(AuditStatus.Incomplete.getValue())) {
                message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003B","单据[%s]未审批不能撤回") /* "单据[%s]未审批不能撤回" */,currentBill.get(ICmpConstant.CODE).toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102646"),message);
            }
            if(currentBill.get("settlestatus") != null && 3 == currentBill.getShort("settlestatus")){
                bizobject.set("settlestatus", SettleStatus.noSettlement.getValue());
            }
            EventSource eventSource =EventSource.find(currentBill.get(IBillConst.SRCITEM));
            EventType eventType = EventType.find(currentBill.get(IBillConst.BILLTYPE));
            if (EventSource.Drftchase.equals(eventSource) || EventType.SignNote.equals(eventType)) {
                message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00038","商业汇票传入的单据[%s]不能撤回") /* "商业汇票传入的单据[%s]不能撤回" */,currentBill.get(ICmpConstant.CODE).toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102647"),message);
            }
            Short payStatus = currentBill.getShort("paystatus");
            if (payStatus.compareTo(PayStatus.NoPay.getValue()) != 0
                    && payStatus.compareTo(PayStatus.PreFail.getValue()) != 0
                    && payStatus.compareTo(PayStatus.Fail.getValue()) != 0
                    && payStatus.compareTo(PayStatus.SupplPaid.getValue()) != 0) {
                message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003A","单据[%s]支付状态不能撤回") /* "单据[%s]支付状态不能撤回" */,currentBill.get(ICmpConstant.CODE).toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102648"),message);
            }

            Boolean check = journalService.checkJournal(currentBill.getId());
            if (check) {
                message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00039","单据[%s]已勾兑不能撤回") /* "单据[%s]已勾兑不能撤回" */,currentBill.get(ICmpConstant.CODE).toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102649"),message);
            }

            bizobject.set("auditstatus", AuditStatus.Incomplete.getValue());
            // 撤回改成待支付-排除流水生单场景 已支付补单撤回状态保持不变-已支付补单和已结算补单才能删除凭证-TransferAutoPayUnAuditAfterRule
            if (payStatus.compareTo(PayStatus.SupplPaid.getValue()) != 0) {
            bizobject.set("paystatus", PayStatus.NoPay.getValue());
            }
            bizobject.set("auditorId", null);
            bizobject.set("auditor", null);
            bizobject.set("auditDate", null);
            bizobject.set("auditTime", null);
            // 转账单支持取消结算，已经生成凭证的转账单可以弃审，需要删除凭证信息
            bizobject.set("voucherNo", null);
            // 传结算的时候，弃审需要清空收款流水的关联信息
            if (checkFundTransfer) {
                if(bizobject.get("billtype") != null && bizobject.getShort("billtype") != EventType.CashMark.getValue() && bizobject.getShort("billtype") != EventType.BillClaim.getValue()) {
                    bizobject.set("associationStatusCollect", false);
                    bizobject.set("collectbankbill", null);// 收款_银行对账单ID
                    bizobject.set("collectbillclaim", null);// 收款_认领单ID
                }
            }

            // 更新支票状态并清空值
            if (currentBill.get("checkid") != null) {
                Long checkId = currentBill.get("checkid");
                CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);

                // 不推结算，转账单弃审时，清空金额，出票日期，出票人员
                if (!checkFundTransfer) {
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
            //第三方转账
            boolean thirdPartyTransfer = Short.parseShort(currentBill.get("srcitem").toString()) == EventSource.ThreePartyReconciliation.getValue();
            if(thirdPartyTransfer){
                journalService.updateJournalThird(bizobject,false);
            }else{
                journalService.updateJournal(bizobject);
            }

            if(thirdPartyTransfer && null != currentBill.getShort("paystatus") && currentBill.getShort("paystatus").compareTo(PayStatus.SupplPaid.getValue()) == 0){
                CtmJSONObject jsonObject = new CtmJSONObject();
                jsonObject.put("id",bizobject.getId());
                jsonObject.put("billnum","cm_transfer_account");
                boolean checked = cmpVoucherService.isChecked(jsonObject);
                if (checked) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102650"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C8","单据【") /* "单据【" */ + currentBill.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C9","】凭证已勾对，不能取消线下支付") /* "】凭证已勾对，不能取消线下支付" */);
                }
                bizobject.set("voucherNo",null);
                bizobject.set("settleuser",null);
                bizobject.set("settledate",null);
                bizobject.set("voucherId",null);
                bizobject.set("voucherPeriod",null);
                bizobject.set("voucherstatus",VoucherStatus.Empty.getValue());

                currentBill.set("settleuser",null);
                currentBill.set("settledate",null);
                currentBill.set("voucherNo",null);
                currentBill.set("voucherId",null);
                currentBill.set("voucherPeriod",null);
                currentBill.set("_entityName", TransferAccount.ENTITY_NAME);
                CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(currentBill);
                if (!deleteResult.getBoolean("dealSucceed")) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102650"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C8","单据【") /* "单据【" */ + currentBill.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CC","】删除凭证失败：") /* "】删除凭证失败：" */ + deleteResult.get("message"));
                }
            }
            TransferAccount account = (TransferAccount) bizobject;
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizobject.getId(), null);
            if (!transferAccount.getIsWfControlled()) {//未开启审批流直接释放
                boolean releaseBudget = releaseBudget(transferAccount);
                if (releaseBudget) {
                    account.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    cmpBudgetTransferAccountManagerService.updateOccupyBudget(account, OccupyBudget.UnOccupy.getValue());
                }
            } else {//开启审批流释放重新预占
                Short occupyBudget = budgetAfterUnAudit(transferAccount);
                if (occupyBudget != null) {
                    account.setIsOccupyBudget(occupyBudget);
                    cmpBudgetTransferAccountManagerService.updateOccupyBudget(account, occupyBudget);
                }
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 是否占预算为实占成功时，删除实占；
     * 是否占预算为预占成功时，删除预占；
     *
     * @param transferAccount
     * @throws Exception
     */
    private boolean releaseBudget(TransferAccount transferAccount) throws Exception {
        Short budgeted = transferAccount.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return false;
        } else if (OccupyBudget.PreSuccess.getValue() == budgeted) {//是否占预算为预占成功时，删除预占；
            return cmpBudgetTransferAccountManagerService.releaseBudget(transferAccount);
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            return cmpBudgetTransferAccountManagerService.releaseImplement(transferAccount);
        }
        return false;
    }

    /**
     * 如果是预占就跳过，如果是实占，删除实占，重新预占
     *
     * @param transferAccount
     * @throws Exception
     */
    private Short budgetAfterUnAudit(TransferAccount transferAccount) throws Exception {
        Short budgeted = transferAccount.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return null;
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            boolean releaseImplement = cmpBudgetTransferAccountManagerService.releaseImplement(transferAccount);
            if (releaseImplement) {
                //重新预占
                log.error("重新预占.....");
                //且结算状态应置为待结算、并清空结算成功时间
                transferAccount.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                boolean budget = cmpBudgetTransferAccountManagerService.budget(transferAccount);
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
