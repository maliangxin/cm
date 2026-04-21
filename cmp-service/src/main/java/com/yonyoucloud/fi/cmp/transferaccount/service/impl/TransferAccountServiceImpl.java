package com.yonyoucloud.fi.cmp.transferaccount.service.impl;

import com.google.common.collect.Maps;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.bizdoc.service.settlemethod.ISettleMethodQueryService;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.budget.*;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.checkStock.service.enums.CashType;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.BillMapEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.transferaccount.service.TransferAccountService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * @ClassName TransferAccountServiceImpl
 * @Desc 转账工作台服务实现类
 * @Author tongyd
 * @Date 2019/9/9
 * @Version 1.0
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class TransferAccountServiceImpl implements TransferAccountService {

    @Autowired
    private CmpVoucherService cmpVoucherService;

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private JournalService journalService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Autowired
    ISettleMethodQueryService settleMethodQueryService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    TransTypeQueryService transTypeQueryService;

    @Autowired
    private CheckStatusService checkStatusService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;

    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;

    @Override
    public CtmJSONObject audit(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("rows");
        List<TransferAccount> transferAccounts = new ArrayList<>(rows.size());
        List<Object> ids = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        CtmJSONObject failed = new CtmJSONObject();
        int failedCount = 0;
        Date date = BillInfoUtils.getBusinessDate();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            if (!CacheUtils.lockRowData(id)) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039E","】被锁定不能审核") /* "】被锁定不能审核" */);
                failedCount++;
                continue;
            }
            Short auditStatus = rowData.getShort("auditstatus");
            if (auditStatus.compareTo(AuditStatus.Complete.getValue()) == 0) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039F","】已审核不能审核") /* "】已审核不能审核" */);
                failedCount++;
                continue;
            }

            if(date != null && rowData.getDate("vouchdate") != null&& date.compareTo(rowData.getDate("vouchdate")) < 0){
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A1","】审批日期小于单据日期，不能审批！") /* "】审批日期小于单据日期，不能审批！" */);
                failedCount++;
                continue;
            }
            Short payStatus = rowData.getShort("paystatus");
            if (payStatus.compareTo(PayStatus.NoPay.getValue()) != 0
                    && payStatus.compareTo(PayStatus.PreFail.getValue()) != 0
                    && payStatus.compareTo(PayStatus.Fail.getValue()) != 0) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A8","】的支付状态不能审批") /* "】的支付状态不能审批" */);
                failedCount++;
                continue;
            }
            TransferAccount transferAccount = new TransferAccount();
            transferAccount.setEntityStatus(EntityStatus.Update);
            transferAccount.setId(id);
            transferAccount.setAuditstatus(AuditStatus.Complete);
            transferAccount.setAuditorId(AppContext.getCurrentUser().getId());
            transferAccount.setAuditor(AppContext.getCurrentUser().getName());
            transferAccount.setAuditTime(new Date());
            transferAccount.setAuditDate(BillInfoUtils.getBusinessDate());
            rowData.put("auditstatus", AuditStatus.Complete.getValue());
            transferAccounts.add(transferAccount);
            ids.add(id);
        }
        if (transferAccounts.size() > 0) {
            MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccounts);
            cmCommonService.refreshPubTs(TransferAccount.ENTITY_NAME, ids, rows);
        }
        param.put("rows", rows);
        if (rows.size() == 1) {
            if (messages.size() > 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102414"),messages.get(0));
            } else {
                param.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180395","审核成功") /* "审核成功" */);
            }
        }
        param.put("messages", messages);
        param.put("count", rows.size());
        param.put("sucessCount", rows.size() - failedCount);
        param.put("failCount", failedCount);
        if(failed.size() > 0) {
            param.put(ICmpConstant.FAILED, failed);
        }
        return param;
    }

    /**
     * @param param
     * @return java.lang.String
     * @Author tongyd
     * @Description 弃审
     * @Date 2019/10/18
     * @Param [param]
     */
    @Override
    public CtmJSONObject unAudit(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("rows");
        List<TransferAccount> transferAccounts = new ArrayList<>(rows.size());
        List<Object> ids = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        CtmJSONObject failed = new CtmJSONObject();
        int failedCount = 0;
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            if (!CacheUtils.lockRowData(id)) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A5","】被锁定不能弃审") /* "】被锁定不能弃审" */);
                failedCount++;
                continue;
            }
            Short auditStatus = rowData.getShort("auditstatus");
            if (auditStatus.compareTo(AuditStatus.Incomplete.getValue()) == 0) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A9","】未审核不能弃审") /* "】未审核不能弃审" */);
                failedCount++;
                continue;
            }
            Short payStatus = rowData.getShort("paystatus");
            if (payStatus.compareTo(PayStatus.NoPay.getValue()) != 0
                    && payStatus.compareTo(PayStatus.PreFail.getValue()) != 0
                    && payStatus.compareTo(PayStatus.Fail.getValue()) != 0) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803AA","】支付状态不能弃审") /* "】支付状态不能弃审" */);
                failedCount++;
                continue;
            }
            Boolean settlement = settlementService.checkDailySettlement(rowData.getString(IBussinessConstant.ACCENTITY), rowData.getDate("vouchdate"));
            if (settlement) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803AD","】已日结不能弃审") /* "】已日结不能弃审" */);
                failedCount++;
                continue;
            }
            Boolean check = journalService.checkJournal(rowData.getLong("id"));
            if (check) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180396","】已勾对不能弃审") /* "】已勾对不能弃审" */);
                failedCount++;
                continue;
            }
            Boolean matchJournal = journalService.matchJournal(rowData.getLong("id"));
            if (matchJournal) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180399","单据已经关联匹配银行交易回单，不允许弃审") /* "单据已经关联匹配银行交易回单，不允许弃审" */);
                failedCount++;
                continue;
            }
            TransferAccount transferAccount = new TransferAccount();
            transferAccount.setEntityStatus(EntityStatus.Update);
            transferAccount.setId(id);
            transferAccount.setAuditstatus(AuditStatus.Incomplete);
            transferAccount.setAuditorId(null);
            transferAccount.setAuditor(null);
            transferAccount.setAuditTime(null);
            transferAccount.setAuditDate(null);
            rowData.put("auditstatus", AuditStatus.Incomplete.getValue());
            transferAccounts.add(transferAccount);
            ids.add(id);
        }
        if (transferAccounts.size() > 0) {
            MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccounts);
            cmCommonService.refreshPubTs(TransferAccount.ENTITY_NAME, ids, rows);
        }
        param.put("rows", rows);
        if (rows.size() == 1) {
            if (messages.size() >0) {
                throw  new CtmException(messages.get(0));
            } else {
                param.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A4","弃审成功") /* "弃审成功" */);
            }
        }
        param.put("messages", messages);
        param.put("count", rows.size());
        param.put("sucessCount", rows.size() - failedCount);
        param.put("failCount", failedCount);
        if(failed.size() > 0) {
            param.put("failed", failed);
        }
        return param;
    }

    @Override
    public CtmJSONObject offLinePay(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("rows");
        long userId = AppContext.getCurrentUser().getId();
        List<Object> ids = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        CtmJSONObject failed = new CtmJSONObject();
        int failedCount = 0;
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();

        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            if (!CacheUtils.lockRowData(id)) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180398","】被锁定不能线下支付") /* "】被锁定不能线下支付" */);
                failedCount++;
                continue;
            }
            //校验该账户是否允许透支
            String payBankAccount = rowData.getString("payBankAccount");
            String payCashAccount = rowData.getString("payCashAccount");
            String currency = rowData.getString("currency");
            if(payBankAccount!=null){
                if(!CmpWriteBankaccUtils.checkAccOverDraft(rowData.getString(IBussinessConstant.ACCENTITY),payBankAccount, currency)){
                    failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039C","账户余额不足") /* "账户余额不足" */);
                    if (rows.size() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102415"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039C","账户余额不足") /* "账户余额不足" */);
                    }
                    failedCount++;
                    continue;
                }
            }
            if(payCashAccount!=null){
                if(!CmpWriteBankaccUtils.checkAccOverDraft(rowData.getString(IBussinessConstant.ACCENTITY),payCashAccount, currency)){
                    failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039C","账户余额不足") /* "账户余额不足" */);
                    if (rows.size() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102415"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039C","账户余额不足") /* "账户余额不足" */);
                    }
                    failedCount++;
                    continue;
                }
            }
            Short auditStatus = rowData.getShort("auditstatus");
            if (auditStatus.compareTo(AuditStatus.Complete.getValue()) != 0) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A6","】未审核不能线下支付") /* "】未审核不能线下支付" */);
                failedCount++;
                continue;
            }
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, id,3);
            if (transferAccount == null) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00158", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102416"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00158", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, rowData.getString("code")));
                }
                failedCount++;
                continue;
            }

            Short payStatus = rowData.getShort("paystatus");
            if (payStatus.compareTo(PayStatus.NoPay.getValue()) != 0
                    && payStatus.compareTo(PayStatus.PreFail.getValue()) != 0
                    && payStatus.compareTo(PayStatus.Fail.getValue()) != 0) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803AE","】的支付状态不能线下支付") /* "】的支付状态不能线下支付" */);
                failedCount++;
                continue;
            }
            //校验结算日期是否已日结
            Date maxSettleDate = null;
            if(maxSettleDateMaps.containsKey(transferAccount.getAccentity())){
                maxSettleDate = maxSettleDateMaps.get(transferAccount.getAccentity());
            }else{
                maxSettleDate = settlementService.getMaxSettleDate(transferAccount.getAccentity());
                maxSettleDateMaps.put(transferAccount.getAccentity(), maxSettleDate);
            }
            if(SettleCheckUtil.checkDailySettlement(maxSettleDate, false)){
                failed.put(id.toString(), id.toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039A","单据【") /* "单据【" */
                        /* "单据【" */ + transferAccount.getCode() + String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039B","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102417"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039B","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
                }
                failedCount++;
                continue;
            }
            transferAccount.setEntityStatus(EntityStatus.Update);
            transferAccount.setPubts(rowData.getDate("pubts"));
            transferAccount.setPaystatus(PayStatus.OfflinePay);
            transferAccount.setSettleuser(userId);
            if (transferAccount.getSettleSuccessAmount() == null && transferAccount.getOriSum() != null) {
                transferAccount.setSettleSuccessAmount(transferAccount.getOriSum());
            }
            //如果是已结算补单，则不更新结算状态，银行流水认领生单同名账户划转时，结算状态是：已结算补单zxl
            if (transferAccount.getSettlestatus().getValue() != SettleStatus.SettledRep.getValue()) {
                if(BillInfoUtils.getBusinessDate() != null) {
                    transferAccount.setSettledate(BillInfoUtils.getBusinessDate());
                }else {
                    transferAccount.setSettledate(AppContext.getLoginDate());
                }
                transferAccount.setSettlestatus(SettleStatus.alreadySettled);
                boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
                if (implement) {
                    transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                }
            }
            transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
            CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(transferAccount);
            if (!generateResult.getBoolean("dealSucceed")) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A3","】发送会计平台失败：") /* "】发送会计平台失败：" */ + generateResult.get("message"));
                failedCount++;
                continue;
            }
            // 线下支付，更新支票状态
            if (rowData.getLong("checkid") != null ) {
                Long checkId = rowData.getLong("checkid");
                CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                // 更新支票状态为“已兑付”
                checkStock.setCheckBillStatus(CmpCheckStatus.Cashed.getValue());
                checkStock.setCashPerson(AppContext.getCurrentUser().getName());//兑付人
                checkStock.setCashType(CashType.Business.getIndex());//兑付方式
                checkStock.setCashDate(rowData.getDate("vouchdate"));
                checkStock.setCheckpurpose(CheckPurpose.VirtualToBank.getValue());
                checkStock.setEntityStatus(EntityStatus.Update);
                checkStatusService.recordCheckStatusByCheckStock(checkStock.getId(), checkStock);
                MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
            }
            journalService.updateJournal(transferAccount);
            MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
            rowData.put("voucherstatus", transferAccount.getVoucherstatus().getValue());
            rowData.put("paystatus", PayStatus.OfflinePay.getValue());
            //如果是已结算补单，则不更新结算状态，银行流水认领生单同名账户划转时，结算状态是：已结算补单zxl
            if (transferAccount.getSettlestatus().getValue() != SettleStatus.SettledRep.getValue()) {
                rowData.put("settlestatus", SettleStatus.alreadySettled.getValue());
            }else {
                rowData.put("settlestatus", SettleStatus.SettledRep.getValue());
            }

            ids.add(id);
            ctmcmpBusinessLogService.saveBusinessLog(transferAccount, transferAccount.getCode(), "", IServicecodeConstant.TRANSFERACCOUNT, IMsgConstant.TRANSFER_ACCOUNT, IMsgConstant.OFFLINE_PAY);
        }
        if (ids.size() > 0) {
            cmCommonService.refreshPubTs(TransferAccount.ENTITY_NAME, ids, rows);
        }
        param.put("rows", rows);
        if (rows.size() == 1) {
            if (messages.size() > 0) {
                throw  new CtmException(messages.get(0));
            } else {
                param.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B1","线下支付成功") /* "线下支付成功" */);
            }
        }
        param.put("messages", messages);
        param.put("count", rows.size());
        param.put("sucessCount", rows.size() - failedCount);
        param.put("failCount", failedCount);
        if(failed.size() > 0) {
            param.put("failed", failed);
        }
        return param;
    }

    @Override
    public CtmJSONObject cancelOffLinePay(CtmJSONObject param) throws Exception {
        CtmJSONArray rows = param.getJSONArray("rows");
        List<TransferAccount> transferAccounts = new ArrayList<>(rows.size());
        List<Journal> journalList = new ArrayList<Journal>();
        List<Object> ids = new ArrayList<>();
        int failedCount = 0;
        List<String> messages = new ArrayList<>();
        CtmJSONObject failed = new CtmJSONObject();
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();

        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            if (!CacheUtils.lockRowData(id)) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A2","】被锁定不能取消线下支付") /* "】被锁定不能取消线下支付" */);
                failedCount++;
                continue;
            }
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, id);
            Short payStatus = rowData.getShort("paystatus");
            if (payStatus.compareTo(PayStatus.OfflinePay.getValue()) != 0) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A7","】的支付状态不能取消线下支付") /* "】的支付状态不能取消线下支付" */);
                failedCount++;
                continue;
            }
            //日结检查校验，效率优化，减少数据库访问
            Date maxSettleDate = null;
            if(maxSettleDateMaps.containsKey(transferAccount.getAccentity())){
                maxSettleDate = maxSettleDateMaps.get(transferAccount.getAccentity());
            }else{
                maxSettleDate = settlementService.getMaxSettleDate(transferAccount.getAccentity());
                maxSettleDateMaps.put(transferAccount.getAccentity(), maxSettleDate);
            }
            if(SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, transferAccount.getSettledate())){
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803AC","】已日结不能取消线下支付") /* "】已日结不能取消线下支付" */);
                failedCount++;
                continue;
            }
            Boolean journal = journalService.checkJournal(id);
            if (journal) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803AF","】已勾对不能取消线下支付") /* "】已勾对不能取消线下支付" */);
                failedCount++;
                continue;
            }
            Boolean match = journalService.matchJournal(id);
            if (match) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add( String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181855940428002C", "单据【%s】已经匹配关联，不能取消线下支付") /* "单据【%s】已经匹配关联，不能取消线下支付" */,rowData.get("code")) );
                failedCount++;
                continue;
            }
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("id",id);
            jsonObject.put("billnum","cm_transfer_account");
            boolean checked = cmpVoucherService.isChecked(jsonObject);
            if (checked){
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418039D","】凭证已勾对，不能取消线下支付") /* "】凭证已勾对，不能取消线下支付" */);
                failedCount++;
                continue;
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
            //如果是已结算补单，则不更新结算状态，银行流水认领生单同名账户划转时，结算状态是：已结算补单zxl
            if (transferAccount.getSettlestatus().getValue() != 8) {
                transferAccount.setSettlestatus(SettleStatus.noSettlement);
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
            }
            transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
            CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(transferAccount);
            if (!deleteResult.getBoolean("dealSucceed")) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180397","单据【") /* "单据【" */ + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803A0","】删除凭证失败：") /* "】删除凭证失败：" */ + deleteResult.get("message"));
                failedCount++;
                continue;
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
            //如果是已结算补单，则不更新结算状态，银行流水认领生单同名账户划转时，结算状态是：已结算补单zxl
            if (transferAccount.getSettlestatus().getValue() == 8) {
                rowData.put("settlestatus", SettleStatus.SettledRep.getValue());
            }else {
                rowData.put("settlestatus", SettleStatus.noSettlement.getValue());
            }
            rowData.put("paystatus", PayStatus.NoPay.getValue());
            rowData.put("voucherstatus", transferAccount.getVoucherstatus().getValue());
            transferAccounts.add(transferAccount);
            ids.add(id);
            ctmcmpBusinessLogService.saveBusinessLog(transferAccount, transferAccount.getCode(), "", IServicecodeConstant.TRANSFERACCOUNT, IMsgConstant.TRANSFER_ACCOUNT, IMsgConstant.CANCEL_OFFLINE_PAY);
        }
        if(!CollectionUtils.isEmpty(journalList)){
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
        if (transferAccounts.size() > 0) {
            MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccounts);
            cmCommonService.refreshPubTs(TransferAccount.ENTITY_NAME, ids, rows);
        }
        param.put("rows", rows);
        if (rows.size() == 1) {
            if (messages.size() >0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102418"),messages.get(0));
            } else {
                param.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B0","取消线下支付成功") /* "取消线下支付成功" */);
            }
        }
        param.put("messages", messages);
        param.put("count", rows.size());
        param.put("sucessCount", rows.size() - failedCount);
        param.put("failCount", failedCount);
        if(failed.size() > 0) {
            param.put("failed", failed);
        }
        return param;
    }

    @Override
    public CtmJSONObject findByCode(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setCode(param.getString("code"));
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        List<SettleMethodModel> list = settleMethodQueryService.querySettleMethods(settleMethodQueryParam);
        result.put(ICmpConstant.MSG, ResultMessage.success());
        result.put("data", list.get(0));
        return result;
    }

    @Override
    public String queryExchangeRateRateTypeByCode(CtmJSONObject param) throws Exception {
        Map<String, Object> rateType = Maps.newHashMap();
        Map<String, Object> defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(param.getString("accentity"));
        if (defaultExchangeRateType!=null&&defaultExchangeRateType.get("id")!=null){
            rateType.put("id",defaultExchangeRateType.get("id"));
            rateType.put("name",defaultExchangeRateType.get("name"));
            rateType.put("digit",defaultExchangeRateType.get("digit"));
        }
        return ResultMessage.data(rateType);
    }

    @Override
    public String querySwiftCode(CtmJSONObject param) throws Exception {
        Map<String, Object> resultMap = Maps.newHashMap();
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(param.getString("bankAccount"));
        if (enterpriseBankAcctVO != null) {
            String bankNumber = enterpriseBankAcctVO.getBankNumber();
            BankdotVO bankdotVO = enterpriseBankQueryService.querybankNumberlinenumberById(bankNumber);
            if (bankdotVO != null) {
                resultMap.put("swiftCode",bankdotVO.getSwiftCode());
            }
        }
        return ResultMessage.data(resultMap);
    }

    @Override
    public CtmJSONObject checkAddTransType(CtmJSONObject params) throws Exception {
        String serviceCode = params.getString("serviceCode");
        String addType = params.getString("addType");
        String tradeType = serviceCode.split("_")[0];
        BdTransType bdTransType = transTypeQueryService.findById(tradeType);
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        boolean addTransTypeFlag = false;
        if ("yhzz".equals(jsonObject.get("transferType_zz"))) {
            // 银行转账
            if (!"0".equals(addType)) {
                addTransTypeFlag = true;
            }
        } else if ("jcxj".equals(jsonObject.get("transferType_zz"))) {
            // 缴存现金
            if (!"1".equals(addType)) {
                addTransTypeFlag = true;
            }
        } else if ("tqxj".equals(jsonObject.get("transferType_zz"))) {
            // 提取现金
            if (!"2".equals(addType)) {
                addTransTypeFlag = true;
            }
        } else if ("xjhz".equals(jsonObject.get("transferType_zz"))) {
            // 现金互转
            if (!"3".equals(addType)) {
                addTransTypeFlag = true;
            }
        } else if ("dsfzz".equals(jsonObject.get("transferType_zz"))) {
            // 第三方转账
            if (!"4".equals(addType)) {
                addTransTypeFlag = true;
            }
        }else if ("sbqbcz".equals(jsonObject.get("transferType_zz"))) {
            // 数币钱包充值
            if (!"5".equals(addType)) {
                addTransTypeFlag = true;
            }
        } else if ("sbqbtx".equals(jsonObject.get("transferType_zz"))) {
            // 数币钱包提现
            if (!"6".equals(addType)) {
                addTransTypeFlag = true;
            }
        } else if ("sbqbhz".equals(jsonObject.get("transferType_zz"))) {
            // 数币钱包互转
            if (!"7".equals(addType)) {
                addTransTypeFlag = true;
            }
        }
        params.put("addTransTypeFlag", addTransTypeFlag);
        return params;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.TRANSFERACCOUNT)) {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        }
        String billnum = cmpBudgetVO.getBillno();

        String entityname = null;
        BillMapEnum enumByBillNum = BillMapEnum.getEnumByBillNum(billnum);
        if (enumByBillNum != null) {
            entityname = enumByBillNum.getEntityName();
        }
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(entityname)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AE", "请求参数缺失") /* "请求参数缺失" */));
        }

        //TODO 变更单据返回来源单据信息，ids 为便跟单据自己信息
        List<String> ids = cmpBudgetVO.getIds();
        List<BizObject> bizObjects = new ArrayList<>();
        if (ValueUtils.isNotEmptyObj(ids)) {
            bizObjects = queryBizObjsWarpParentInfo(ids);
        } else if (ValueUtils.isNotEmptyObj(cmpBudgetVO.getBizObj())) {
            BizObject bizObject = CtmJSONObject.parseObject(cmpBudgetVO.getBizObj(), BizObject.class);
            bizObjects.add(bizObject);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AE", "请求参数缺失") /* "请求参数缺失" */));
        }
        //变更单据
        String changeBillno = cmpBudgetVO.getChangeBillno();
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(changeBillno)) {
            if (CollectionUtils.isEmpty(bizObjects)) {
                CtmJSONObject resultBack = new CtmJSONObject();
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000D0", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AF", "变更金额小于原金额,不需要校验!") /* "变更金额小于原金额,不需要校验!" */));
                return ResultMessage.data(resultBack);
            }
            //变更单据获取（融资登记单据类型）
            billnum = changeBillno;
        } else {
            //非变更单据 自己单据
            if (CollectionUtils.isEmpty(bizObjects)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007AE", "请求参数缺失") /* "请求参数缺失" */));
            }
        }
        return cmpBudgetCommonManagerService.budgetCheckNew(bizObjects, billnum, BudgetUtils.SUBMIT);
    }

    public List<BizObject> queryBizObjsWarpParentInfo(List<String> ids) throws Exception {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, schema, null);
    }

}
