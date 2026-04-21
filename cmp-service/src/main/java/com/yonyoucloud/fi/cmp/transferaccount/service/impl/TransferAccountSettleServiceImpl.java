package com.yonyoucloud.fi.cmp.transferaccount.service.impl;

import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.checkStock.service.enums.CashType;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.transferaccount.service.TransferAccountSettleService;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.SettleCheckUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class TransferAccountSettleServiceImpl implements TransferAccountSettleService {

    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private SettlementService settlementService;
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    private JournalService journalService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private CheckStatusService checkStatusService;
    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;
    @Override
    public void OfflinePay(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("row");
        long userId = AppContext.getCurrentUser().getId();
        List<Object> ids = new ArrayList<>();
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            try {
                if (!CacheUtils.lockRowData(id)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180562","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180564","】被锁定不能线下支付") /* "】被锁定不能线下支付" */);
                }
                TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, id,3);
                if (transferAccount == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100556"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001D8", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                }
                Short payStatus = rowData.getShort("paystatus");
                if (payStatus.compareTo(PayStatus.NoPay.getValue()) != 0 && payStatus.compareTo(PayStatus.PreFail.getValue()) != 0 && payStatus.compareTo(PayStatus.Fail.getValue()) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180562","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180567","】的支付状态不能线下支付") /* "】的支付状态不能线下支付" */);
                }
                if(null != transferAccount.get("isSettlement")){
                    String isSettlement = transferAccount.get("isSettlement").toString();
                    if (ICmpConstant.CONSTANT_ONE.toString().equals(isSettlement)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100557"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_182D596C05880000","已推往资金结算处理的转账单，不允许在转账工作台进行结算处理") /*"已推往资金结算处理的转账单，不允许在转账工作台进行结算处理"*/);
                    }
                }

                //校验该账户是否允许透支
                String payBankAccount = rowData.getString("payBankAccount");
                String payCashAccount = rowData.getString("payCashAccount");
                String currency = rowData.getString("currency");
                if (payBankAccount != null) {
                    if (!CmpWriteBankaccUtils.checkAccOverDraft(transferAccount.getAccentity(),payBankAccount, currency)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100558"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418056B","账户余额不足") /* "账户余额不足" */);
                    }
                }
                if (payCashAccount != null) {
                    if (!CmpWriteBankaccUtils.checkAccOverDraft(transferAccount.getAccentity(),payCashAccount, currency)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100558"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418056B","账户余额不足") /* "账户余额不足" */);
                    }
                }
                Short auditStatus = rowData.getShort("auditstatus");
                if (auditStatus.compareTo(AuditStatus.Complete.getValue()) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100559"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00043","单据【%s】未审批不能线下支付") /* "单据【%s】未审批不能线下支付" */,rowData.get("code").toString()));
                }

                //校验结算日期是否已日结
                Date maxSettleDate = null;
                if(maxSettleDateMaps.containsKey(transferAccount.getAccentity())){
                    maxSettleDate = maxSettleDateMaps.get(transferAccount.getAccentity());
                }else{
                    maxSettleDate = settlementService.getMaxSettleDate(transferAccount.getAccentity());
                    maxSettleDateMaps.put(transferAccount.getAccentity(), maxSettleDate);
                }
                if (SettleCheckUtil.checkDailySettlement(maxSettleDate, false)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100560"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180569","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
                }
                transferAccount.setEntityStatus(EntityStatus.Update);
                transferAccount.setPubts(rowData.getDate("pubts"));
                transferAccount.setPaystatus(PayStatus.OfflinePay);
                transferAccount.setSettleuser(userId);
                if(BillInfoUtils.getBusinessDate() != null) {
                    transferAccount.setSettledate(BillInfoUtils.getBusinessDate());
                }else {
                    transferAccount.setSettledate(AppContext.getLoginDate());
                }
                transferAccount.setSettlestatus(SettleStatus.alreadySettled);
                if (transferAccount.getSettleSuccessAmount() == null && transferAccount.getOriSum() != null) {
                    transferAccount.setSettleSuccessAmount(transferAccount.getOriSum());
                }
                boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
                if (implement) {
                    transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                }
                transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
                CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(transferAccount);
                if (!generateResult.getBoolean("dealSucceed")) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180562","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418056E","】发送会计平台失败：") /* "】发送会计平台失败：" */ + generateResult.get("message"));
                }
                // 线下支付，更新支票状态
                if (rowData.getLong("checkid") != null ) {
                    Long checkId = rowData.getLong("checkid");
                    CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                    // 更新支票状态为“已兑付”
                    checkStock.setCheckBillStatus(CmpCheckStatus.Cashed.getValue());
                    checkStock.setCashDate(rowData.getDate("vouchdate"));
                    checkStock.setCashPerson(AppContext.getCurrentUser().getName());//兑付人
                    checkStock.setCashType(CashType.Business.getIndex());//兑付方式
                    checkStock.setCheckpurpose(CheckPurpose.VirtualToBank.getValue());
                    checkStock.setEntityStatus(EntityStatus.Update);
                    checkStatusService.recordCheckStatusByCheckStock(checkStock.getId(), checkStock);
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                }
                journalService.updateJournal(transferAccount);
                MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
                rowData.put("voucherstatus", transferAccount.getVoucherstatus().getValue());
                rowData.put("paystatus", PayStatus.OfflinePay.getValue());
                rowData.put("settlestatus", SettleStatus.alreadySettled.getValue());
                ids.add(id);
                ctmcmpBusinessLogService.saveBusinessLog(transferAccount, transferAccount.getCode(), "", IServicecodeConstant.TRANSFERACCOUNT, IMsgConstant.TRANSFER_ACCOUNT, IMsgConstant.OFFLINE_PAY);
            } finally {
                CacheUtils.unlockRowData(id);
            }
        }
        if (ids.size() > 0) {
            cmCommonService.refreshPubTs(TransferAccount.ENTITY_NAME, ids, rows);
        }
    }


    @Override
    public void cancelOfflinePay(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("row");
        List<TransferAccount> transferAccounts = new ArrayList(rows.size());
        List<Journal> journalList = new ArrayList();
        List<Object> ids = new ArrayList<>();
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            try {
                if (!CacheUtils.lockRowData(id)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180562","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180566","】被锁定不能取消线下支付") /* "】被锁定不能取消线下支付" */);
                }
                TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, id);
                Short payStatus = rowData.getShort("paystatus");
                if (payStatus.compareTo(PayStatus.OfflinePay.getValue()) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180562","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180568","】的支付状态不能取消线下支付") /* "】的支付状态不能取消线下支付" */);
                }
                //日结检查校验，效率优化，减少数据库访问
                Date maxSettleDate;
                if(maxSettleDateMaps.containsKey(transferAccount.getAccentity())){
                    maxSettleDate = maxSettleDateMaps.get(transferAccount.getAccentity());
                }else{
                    maxSettleDate = settlementService.getMaxSettleDate(transferAccount.getAccentity());
                    maxSettleDateMaps.put(transferAccount.getAccentity(), maxSettleDate);
                }
                if (SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, transferAccount.getSettledate())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180562","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418056C","】已日结不能取消线下支付") /* "】已日结不能取消线下支付" */);
                }
                Boolean journal = journalService.checkJournal(id);
                if (journal) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180562","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418056D","】已勾对不能取消线下支付") /* "】已勾对不能取消线下支付" */);
                }
                Boolean match = journalService.matchJournal(id);
                if (match) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100561"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1818559404280026", "单据【%s】已经匹配关联，不能取消线下支付") /* "单据【%s】已经匹配关联，不能取消线下支付" */,rowData.get("code")));
                }
                CtmJSONObject jsonObject = new CtmJSONObject();
                jsonObject.put("id",id);
                jsonObject.put("billnum","cm_transfer_account");
                boolean checked = cmpVoucherService.isChecked(jsonObject);
                if (checked) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180562","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180565","】凭证已勾对，不能取消线下支付") /* "】凭证已勾对，不能取消线下支付" */);
                }
                transferAccount.setEntityStatus(EntityStatus.Update);
                transferAccount.setPubts(rowData.getDate("pubts"));
                transferAccount.setPaystatus(PayStatus.NoPay);
                transferAccount.setSettleuser(null);
                transferAccount.setSettledate(null);
                transferAccount.setVoucherNo(null);
                transferAccount.setVoucherId(null);
                transferAccount.setVoucherPeriod(null);
                transferAccount.setSettleSuccessAmount(null);
                transferAccount.setSettlestatus(SettleStatus.noSettlement);
                transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
                TransferAccount tA = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, transferAccount.getId());
                boolean releaseImplement = cmpBudgetTransferAccountManagerService.releaseImplement(tA);
                if (releaseImplement) {
                    transferAccount.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    tA.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                    boolean budget = cmpBudgetTransferAccountManagerService.budget(tA);
                    if (budget) {
                        transferAccount.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                    }
                }
                CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(transferAccount);
                if (!deleteResult.getBoolean("dealSucceed")) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100555"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180562","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418056A","】删除凭证失败：") /* "】删除凭证失败：" */ + deleteResult.get("message"));
                }
                // 转账单取消结算，更新支票状态
                if (rowData.getLong("checkid") != null) {
                    Long checkId = rowData.getLong("checkid");
                    CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                    // 更新支票状态为“已开票”
                    checkStock.setCheckBillStatus(CmpCheckStatus.BillOver.getValue());
                    checkStock.setCashDate(null);
                    checkStock.setCashType(null);//兑付方式
                    checkStock.setCashPerson(null);//兑付人
                    checkStock.setCheckpurpose(null);
                    checkStock.setEntityStatus(EntityStatus.Update);
                    checkStatusService.recordCheckStatusByCheckId(checkStock.getId(),checkStock.getCheckBillStatus());
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                }
                journalList.addAll(journalService.updateJournalByBill(transferAccount));
                rowData.put("paystatus", PayStatus.NoPay.getValue());
                rowData.put("settlestatus", SettleStatus.noSettlement.getValue());
                rowData.put("voucherstatus", transferAccount.getVoucherstatus().getValue());
                transferAccounts.add(transferAccount);
                ids.add(id);
                ctmcmpBusinessLogService.saveBusinessLog(transferAccount, transferAccount.getCode(), "", IServicecodeConstant.TRANSFERACCOUNT, IMsgConstant.TRANSFER_ACCOUNT, IMsgConstant.CANCEL_OFFLINE_PAY);
            } finally {
                CacheUtils.unlockRowData(id);
            }
        }
        if(!CollectionUtils.isEmpty(journalList)){
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
        if (transferAccounts.size() > 0) {
            MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccounts);
            cmCommonService.refreshPubTs(TransferAccount.ENTITY_NAME, ids, rows);
        }
    }

}
