package com.yonyoucloud.fi.cmp.bankenterprise;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.business.SystemCodeUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ClassName InternetBankPayServiceImpl
 * @Desc 网银支付服务实现
 * @Author tongyd
 * @Date 2019/9/11
 * @Version 1.0
 */
@Service
@Slf4j
public class InternetBankPayServiceImpl implements InternetBankPayService {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    private CtmSignatureService digitalSignatureService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;
    @Autowired
    BankConnectionAdapterContext bankConnectionContext;
    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private HttpsService httpsService;

    @Autowired
    private JournalService journalService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;

    @Autowired
    BankConnectionAdapterContext bankConnectionAdapterContext;

    private AtomicInteger cardinalNumber = new AtomicInteger(0);

    private static final Map<String,String> tradeTypeMap =  new HashMap<>();
    static {
        tradeTypeMap.put("WCZ", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400814", "数币钱包充值") /* "数币钱包充值" */);
        tradeTypeMap.put("WTX", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400815", "数币钱包提现") /* "数币钱包提现" */);
        tradeTypeMap.put("WHZ", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400816", "数币钱包互转") /* "数币钱包互转" */);
    }

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public String batchPayPreOrder(CtmJSONObject param) throws Exception {
        CtmJSONArray row = param.getJSONArray("row");
        try {
            // 付款结算方式非直联不允许网银预下单
            // checkPayMethodMode(row);
            // 数币的交易类型不允许网银预下单
            checkPayOrderOfGg(row);
            checkPubTs(row);
            CtmJSONObject lockResult = CacheUtils.lockBill(row);
            if (!lockResult.getBoolean("dealSucceed")) {
                return ResultMessage.error(lockResult.getString("message"));
            }
            CtmJSONObject verifyResult = iTrusVerifySignatureByRowData(row);
            if (!verifyResult.getBoolean("dealSucceed")) {
                return ResultMessage.error(verifyResult.getString("message"));
            }
            if (!bankAccountSettingService.getOpenFlagByBankAccountId(param.getString("payBankAccount"))) {
                return ResultMessage.error(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F44AA05B0000E", "银行账户【%s】没有直联支付权限，网银预下单失败，请前往【银企联账号】检查银行账户直联权限。") /* "银行账户【%s】没有直联支付权限，网银预下单失败，请前往【银企联账号】检查银行账户直联权限。" */, param.get("payBankAccountName")));
            }
            String customNo = bankAccountSettingService.getCustomNoByBankAccountId(param.getString("payBankAccount"));
//            String customNo = bankAccountSettingService.getCustomNoAndCheckUKeyByBankAccountId(param.getBoolean("isNeedCheckUkey"), param.getString("payBankAccount"), param.getString("customNo"));
            String requestseqno = DigitalSignatureUtils.buildRequestNum(customNo);
            param.put("requestseqno", requestseqno);
            param.put("customNo", customNo);
            CtmJSONObject postMsg = buildBatchPayPreOrderMsg(param, row);
            log.error("转账工作台网银预下单推送报文：" + postMsg.toString());
            CtmJSONObject postResult = httpsService.doHttpsPost(ITransCodeConstant.BATCH_PAY_PRE_ORDER, postMsg, bankConnectionAdapterContext.getChanPayUri(), true);
            log.error("转账工作台网银预下单接收报文：" + postResult.toString());
            CtmJSONObject logData = new CtmJSONObject();
            logData.put(IMsgConstant.BILL_DATA,row);
            logData.put(IMsgConstant.BANK_PLACEORDER_REQUEST,postMsg);
            logData.put(IMsgConstant.BANK_PLACEORDER_RESPONSE,postResult);
            ctmcmpBusinessLogService.saveBusinessLog(logData, row.getJSONObject(0).getString("code"), "", IServicecodeConstant.TRANSFERACCOUNT, IMsgConstant.TRANSFER_ACCOUNT, IMsgConstant.BANK_PREORDER);
            analysisBatchPayPreOrderRespData(postResult, row);
            cmCommonService.refreshPubTs(TransferAccount.ENTITY_NAME, param.getObject("ids", List.class), row);
            param.put("row", row);
            param.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00083", "网银预下单成功") /* "网银预下单成功" */);
            return ResultMessage.data(param);
        } catch (Exception e) {
            return ResultMessage.error(e.getMessage());
        }
    }

    /**
     * 校验付款结算方式
     * @param row
     * @return
     */
    private void checkPayMethodMode(CtmJSONArray row) {
        if (row == null || row.isEmpty()) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400817", "传入的数据为空，请检查") /* "传入的数据为空，请检查" */);
        }
        for (int i = 0; i < row.size(); i++) {
            CtmJSONObject param = row.getJSONObject(i);
            if (param.get("settlemode") == null) {
                log.error("checkPayMethodMode settlemode is null,param:{}", CtmJSONObject.toJSONString(param));
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400817", "传入的数据为空，请检查") /* "传入的数据为空，请检查" */);
            }
            // 查询结算方式
            SettleMethodModel settleModeMap = baseRefRpcService.querySettleMethodsById(param.get("settlemode").toString());
            if (settleModeMap == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100495"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180469","未查询到对应的结算方式，保存失败！") /* "未查询到对应的结算方式，保存失败！" */);
            }
            Integer directConnection = settleModeMap.getDirectConnection();
            // 0 非直联 1 直联
            if(directConnection == null || directConnection == 0){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B7BBCC04700006", "结算方式是否直联未设置，网银预下单失败，请选择其它直联结算方式，或者请前往【结算方式】检查是否直联设置。" /* "结算方式是否直联未设置，网银预下单失败，请选择其它直联结算方式，或者请前往【结算方式】检查是否直联设置。" */));
            }
        }
    }

    /**
     * 网银预下单和支付的数币交易类型校验
     * @param row
     */
    private void checkPayOrderOfGg(CtmJSONArray row) {
        if (row == null || row.isEmpty()) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400817", "传入的数据为空，请检查") /* "传入的数据为空，请检查" */);
        }
        Set<String> tradeTypeCodeSet = tradeTypeMap.keySet();
        String tradeTypeCode = "";
        for (int i = 0; i < row.size(); i++) {
            CtmJSONObject rowData = row.getJSONObject(i);
            tradeTypeCode = rowData.getString("tradetype_code");
            if (tradeTypeCodeSet.contains(tradeTypeCode)) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400813", "%s,不支持直联支付") /* "%s,不支持直联支付" */, tradeTypeMap.get(rowData.getString("tradetype_code"))));
            }
        }
    }

    @Override
    public String preOrderTransactionConfirm(CtmJSONObject param) throws Exception {
        CtmJSONArray row = param.getJSONArray("row");
        // 付款结算方式非直联不允许网银预下单
        // checkPayMethodMode(row);
        // 数币交易类型不允许网银支付
        checkPayOrderOfGg(row);
        CtmJSONObject lockResult = CacheUtils.lockBill(row);
        if (!lockResult.getBoolean("dealSucceed")) {
            return ResultMessage.error(lockResult.getString("message"));
        }
        CtmJSONObject verifyResult = iTrusVerifySignatureByRowData(row);
        if (!verifyResult.getBoolean("dealSucceed")) {
            return ResultMessage.error(verifyResult.getString("message"));
        }
        verifyResult = verifyInitDataByRowData(row);
        if (!verifyResult.getBoolean("dealSucceed")) {
            return ResultMessage.error(verifyResult.getString("message"));
        }
        List<TransferAccount> transferAccounts = queryTaByPreOrderCode(param);
        String customNo = bankAccountSettingService.getCustomNoAndCheckByBankAccountId(transferAccounts.get(0).getPayBankAccount(), param.get("customNo"));
        String requestseqno = DigitalSignatureUtils.buildRequestNum(customNo);
        param.put("requestseqno", requestseqno);
        param.put("customNo", customNo);
        CtmJSONObject postMsg = buildTransactionConfirmMsg(param, transferAccounts);
        //启动独立事务跟新单据
        int count = cmCommonService.updateTransferAccountsForPayStatus(transferAccounts.get(0));
        //查看是否更新成功 若成功说单据状态正确
        if (count == 0) {
            //若count为0 则当前单据状态有误 不能执行后续操作
            String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418026F","单据[%s]支付状态有误，请在下载支付状态后，重新进行操作") /* "单据[%s]支付状态有误，请在下载支付状态后，重新进行操作" */, transferAccounts.get(0).getCode());
            return ResultMessage.error(message);
        }
        log.error("转账工作台网银支付推送报文："+ transferAccounts.get(0).getCode() + postMsg.toString());
        CtmJSONObject postResult = httpsService.doHttpsPost(ITransCodeConstant.PRE_ORDER_TRANSACTION_CONFIRM, postMsg, bankConnectionAdapterContext.getChanPayUri(), null);
        log.error("转账工作台网银支付接收报文：" + transferAccounts.get(0).getCode() + postResult.toString());
        CtmJSONObject logData = new CtmJSONObject();
        logData.put(IMsgConstant.BILL_DATA,row);
        logData.put(IMsgConstant.CONFIRM_PLACEORDER_REQUEST,postMsg);
        logData.put(IMsgConstant.CONFIRM_PLACEORDER_RESPONSE,postResult);
        ctmcmpBusinessLogService.saveBusinessLog(logData, row.getJSONObject(0).getString("code"), "", IServicecodeConstant.TRANSFERACCOUNT, IMsgConstant.TRANSFER_ACCOUNT, IMsgConstant.BANK_PAY);
        analysisTransactionConfirmRespData(postResult, transferAccounts, row);
        cmCommonService.refreshPubTs(TransferAccount.ENTITY_NAME, param.getObject("ids", List.class), row);
        param.put("row", row);
        param.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00082", "网银支付成功") /* "网银支付成功" */);
        return ResultMessage.data(param);
    }

    @Override
    public String batchPayDetailStatusQuery(CtmJSONObject param) throws Exception {
        CtmJSONArray row = param.getJSONArray("row");
        CtmJSONObject rowData = null;
        String customNo = bankAccountSettingService.getCustomNoByBankAccountId(row.getJSONObject(0).getString("payBankAccount"));
        String requestseqno = DigitalSignatureUtils.buildRequestNum(customNo);
        param.put("requestseqno", requestseqno);
        param.put("customNo", customNo);
        CtmJSONObject postMsg = buildPayStatusQueryMsg(param);
        log.error("转账工作台查询支付状态推送报文：" + postMsg.toString());
        CtmJSONObject postResult = httpsService.doHttpsPost(ITransCodeConstant.QUERY_BATCH_DETAIL_PAY_STATUS, postMsg, bankConnectionAdapterContext.getChanPayUri(), true);
        log.error("转账工作台查询支付状态接收报文：" + postResult.toString());
        if (!"0000".equals(postResult.getString("code"))) {
            return ResultMessage.error(postResult.getString("message"));
        }
        CtmJSONObject responseHead = postResult.getJSONObject("data").getJSONObject("response_head");
        String serviceStatus = responseHead.getString("service_status");
        if (!("00").equals(serviceStatus)) {
            return ResultMessage.error(responseHead.getString("service_resp_code") + responseHead.getString("service_resp_desc"));
        }
        analysisPayStatusQueryRespData(postResult.getJSONObject("data").getJSONObject("response_body"));
        List<TransferAccount> transferAccounts = queryTaById(param.getObject("ids", List.class));
        param.put("message", postResult.getString("message"));
        for (TransferAccount transferAccount : transferAccounts) {
            for (int i = 0; i < row.size(); i++) {
                if (transferAccount.getId().toString().equals(row.getJSONObject(i).getString("id"))) {
                    rowData = row.getJSONObject(i);
                }
            }
            rowData.put("paystatus", transferAccount.getPaystatus().getValue());
            rowData.put("paymessage", transferAccount.getPaymessage());
            rowData.put("settlestatus", transferAccount.getSettlestatus().getValue());
            rowData.put("voucherstatus", transferAccount.getVoucherstatus().getValue());
            rowData.put("bankseqno", transferAccount.getBankseqno());
            rowData.put("pubts", transferAccount.getPubts());
        }
        param.put("row", row);
        return ResultMessage.data(param);
    }

    /**
     * @Author tongyd
     * @Description 解析批量支付明细状态查询返回报文
     * @Date 2019/10/25
     * @Param [responseBody]
     * @return void
     **/
    public void analysisPayStatusQueryRespData(CtmJSONObject responseBody) throws Exception {
        List<TransferAccount> transferAccounts = new ArrayList<>();
        int totalNum = responseBody.getInteger("tot_num");
        TransferAccount transferAccount = null;
        if (totalNum == 1) {
            transferAccount = updatePayStatusByRecordData(responseBody.getJSONObject("record"));
            if (transferAccount != null) {
                transferAccounts.add(transferAccount);
            }
        } else if (totalNum > 1) {
            CtmJSONArray record = responseBody.getJSONArray("record");
            for (int j = 0; j < record.size(); j++) {
                transferAccount = updatePayStatusByRecordData(record.getJSONObject(j));
                if (transferAccount != null) {
                    transferAccounts.add(transferAccount);
                }
            }
        }
        if (transferAccounts.size() == 0) {
            return;
        }
        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccounts);
        Iterator<TransferAccount> iterator = transferAccounts.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getPaystatus() != PayStatus.Success) {
                iterator.remove();
            }
        }
        if (transferAccounts.size() > 0) {
            SendBizMessageUtils.sendTsBizMessage(transferAccounts, "cm_transfer_account_list", "paysucceed");
        }
    }

    /**
     * @Author tongyd
     * @Description 根据查询明细更新支付状态
     * @Date 2019/10/25
     * @Param [recordData]
     * @return com.yonyoucloud.fi.cm.transferaccount.TransferAccount
     **/
    private TransferAccount updatePayStatusByRecordData(CtmJSONObject recordData) throws Exception {
        String payStatusStr = recordData.getString("pay_status");
        String bankCheckNo = recordData.getString("bank_check_code");
        PayStatus payStatus = null;
        if ("00".equals(payStatusStr)) {
            payStatus = PayStatus.Success;
        } else if ("01".equals(payStatusStr)) {
            payStatus = PayStatus.Fail;
        } else {
            return null;
        }
        StringBuilder payMessage = new StringBuilder();
        if (recordData.containsKey("bank_resp_code")) {
            payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0000026016", "【") /* "【" */);
            payMessage.append(recordData.get("bank_resp_code"));
            payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0000026019", "】") /* "】" */);
        }
        if (recordData.containsKey("bank_resp_desc")) {
            payMessage.append(recordData.get("bank_resp_desc"));
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name("transeqno").eq(recordData.getString("tran_seq_no")));
        querySchema.addCondition(queryConditionGroup);
        List<TransferAccount> transferAccounts = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchema, null);
        if (transferAccounts.size() < 1) {
            return null;
        }
        TransferAccount transferAccount = transferAccounts.get(0);
        if(transferAccount.getPaystatus() == PayStatus.Success){
            return null;
        }
        transferAccount.setEntityStatus(EntityStatus.Update);
        transferAccount.setPaystatus(payStatus);
        if (payMessage.length() > 0) {
            transferAccount.setPaymessage(payMessage.toString());
        }
        if (recordData.containsKey("bank_resp_desc")) {
            transferAccount.setBankseqno(recordData.getString("bank_seq_no"));
        }
        if (recordData.containsKey("bank_check_code")) {
            transferAccount.setBankcheckcode(recordData.getString("bank_check_code"));
        }
        if (payStatus == PayStatus.Success) {
            transferAccount.setSettleuser(AppContext.getCurrentUser().getId());
            transferAccount.setSettledate(new Date());
            transferAccount.setSettlestatus(SettleStatus.alreadySettled);
            if (transferAccount.getSettleSuccessAmount() == null && transferAccount.getOriSum() != null) {
                transferAccount.setSettleSuccessAmount(transferAccount.getOriSum());
            }
            boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
            if (implement) {
                transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
            }
            transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
            transferAccount.setBankcheckno(bankCheckNo);
            cmpVoucherService.generateVoucherWithPay(transferAccount);

            QuerySchema queryJournalSchema = QuerySchema.create().addSelect("id,pubts,dzdate");
            queryJournalSchema.appendQueryCondition(QueryCondition.name("srcbillitemid").eq(transferAccount.getId()));
            List<Journal> journals = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, queryJournalSchema, null);
            Date currentDate = new Date();
            for (Journal journal : journals) {
                journal.setDzdate(currentDate);
                journal.setDztime(DateUtils.setTimeToCurrent(currentDate));
                journal.setBankcheckno(bankCheckNo);
                journal.setAuditstatus(AuditStatus.Complete);
                journal.setSettlestatus(SettleStatus.alreadySettled);
            }
            EntityTool.setUpdateStatus(journals);
            MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
        }
        return transferAccount;
    }

    /**
     * @Author tongyd
     * @Description 构建批量支付明细状态查询报文
     * @Date 2019/10/9
     * @Param [param]
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    public CtmJSONObject buildPayStatusQueryMsg(CtmJSONObject param) {
        CtmJSONObject requestHead = buildRequestHead(ITransCodeConstant.QUERY_BATCH_DETAIL_PAY_STATUS, param);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("request_seq_no", param.getString("oldRequestSeqNo"));
        CtmJSONObject batchDetailPayStatusMsg = new CtmJSONObject();
        batchDetailPayStatusMsg.put("request_head", requestHead);
        batchDetailPayStatusMsg.put("request_body", requestBody);
        return batchDetailPayStatusMsg;
    }

    /**
     * @Author tongyd
     * @Description 解析交易确认返回报文
     * @Date 2019/10/24
     * @Param [postResult, transferAccounts, row]
     * @return void
     **/
    private void analysisTransactionConfirmRespData(CtmJSONObject postResult, List<TransferAccount> transferAccounts, CtmJSONArray row) throws Exception {
        StringBuilder payMessage = new StringBuilder();
        PayStatus payStatus = null;
        if ("0000".equals(postResult.getString("code"))) {
            CtmJSONObject responseHead = postResult.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            if (("00").equals(serviceStatus)) {
                CtmJSONObject responseBody = postResult.getJSONObject("data").getJSONObject("response_body");
                if ("00".equals(responseBody.get("bat_status"))) {
                    payStatus = PayStatus.Success;
                } else if ("02".equals(responseBody.get("bat_status"))) {
                    payStatus = PayStatus.Fail;
                } else {
                    payStatus = PayStatus.Paying;
                }
                if (responseBody.containsKey("bank_resp_code")) {
                    payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180268","【") /* "【" */);
                    payMessage.append(responseBody.get("bank_resp_code"));
                    payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180269","】") /* "】" */);
                }
                if (responseBody.containsKey("bank_resp_desc")) {
                    payMessage.append(responseBody.get("bank_resp_desc"));
                }
            } else {
                payStatus = PayStatus.Paying;
                if (responseHead.containsKey("service_resp_code")) {
                    payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180268","【") /* "【" */);
                    payMessage.append(responseHead.getString("service_resp_code"));
                    payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180269","】") /* "】" */);
                }
                if (responseHead.containsKey("service_resp_desc")) {
                    payMessage.append(responseHead.getString("service_resp_desc"));
                }
            }
        } else {
            payStatus = PayStatus.PayUnknown;
            payMessage.append(postResult.get("message"));
        }
        Date currentDate = new Date();
        long userId = AppContext.getCurrentUser().getId();
        for (TransferAccount transferAccount : transferAccounts) {
            CtmJSONObject rowData = null;
            for (int i = 0; i < row.size(); i++) {
                if (row.getJSONObject(i).getString("id").equals(transferAccount.getId().toString())) {
                    rowData = row.getJSONObject(i);
                    break;
                }
            }
            transferAccount.setEntityStatus(EntityStatus.Update);
            transferAccount.setPaystatus(payStatus);
            transferAccount.setPaymessage(payMessage.toString());
            if (payStatus == PayStatus.Success) {
                transferAccount.setSettleuser(userId);
                transferAccount.setSettledate(currentDate);
                transferAccount.setSettlestatus(SettleStatus.alreadySettled);
                transferAccount.set("_entityName", TransferAccount.ENTITY_NAME);
                boolean implement = cmpBudgetTransferAccountManagerService.implement(transferAccount);
                if (implement) {
                    transferAccount.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                }
                cmpVoucherService.generateVoucherWithResult(transferAccount);
                journalService.updateJournal(transferAccount);
                if (rowData != null) {
                    rowData.put("settlestatus", SettleStatus.alreadySettled.getValue());
                    rowData.put("voucherstatus", transferAccount.getVoucherstatus().getValue());
                }
            }
            if (rowData != null) {
                rowData.put("paystatus", payStatus.getValue());
                rowData.put("paymessage", payMessage.toString());
            }
        }
        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccounts);
        transferAccounts.removeIf(strings -> strings.getPaystatus() != PayStatus.Success);
        if (transferAccounts.size() > 0) {
            SendBizMessageUtils.sendTsBizMessage(transferAccounts, "cm_transfer_account_list", "paysucceed");
        }
    }

    /**
     * @Author tongyd
     * @Description 根据ID查询转账单
     * @Date 2019/10/25
     * @Param [ids]
     * @return java.util.List<com.yonyoucloud.fi.cm.transferaccount.TransferAccount>
     **/
    private List<TransferAccount> queryTaById(List<Object> ids) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, schema, null);
    }

    /**
     * @Author tongyd
     * @Description 根据预下单编码查询转账单
     * @Date 2019/10/24
     * @Param [param]
     * @return java.util.List<com.yonyoucloud.fi.cm.transferaccount.TransferAccount>
     **/
    private List<TransferAccount> queryTaByPreOrderCode(CtmJSONObject param) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("porderid").eq(param.get("porderId")));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, schema, null);
    }

    /**
     * @Author tongyd
     * @Description 构建预下单交易确认报文
     * @Date 2019/10/9
     * @Param [param, transferAccounts]
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    private CtmJSONObject buildTransactionConfirmMsg(CtmJSONObject param, List<TransferAccount> transferAccounts) {
        CtmJSONObject requestHead = buildRequestHead(ITransCodeConstant.PRE_ORDER_TRANSACTION_CONFIRM, param);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("porder_id", param.get("porderId"));
        requestBody.put("acct_name", param.get("acctName"));
        requestBody.put("acct_no", param.get("account"));
        requestBody.put("tran_tot_num", transferAccounts.size());
        BigDecimal totalMoney = BigDecimal.ZERO;
        for (TransferAccount transferAccount : transferAccounts) {
            totalMoney = totalMoney.add(transferAccount.getOriSum());
        }
        requestBody.put("tran_tot_amt", totalMoney.setScale(2,BigDecimal.ROUND_HALF_UP));
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /**
     * @Author tongyd
     * @Description 解析批量支付预下单反馈数据
     * @Date 2019/10/24
     * @Param [postResult, row]
     * @return void
     **/
    private void analysisBatchPayPreOrderRespData(CtmJSONObject postResult, CtmJSONArray row) throws Exception {
        StringBuilder payMessage = new StringBuilder();
        PayStatus payStatus = null;
        String porderId = null;
        if ("0000".equals(postResult.getString("code"))) {
            CtmJSONObject responseHead = postResult.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            if (("00").equals(serviceStatus)) {
                CtmJSONObject responseBody = postResult.getJSONObject("data").getJSONObject("response_body");
                String porderStatus = responseBody.getString("porder_status");
                if ("00".equals(porderStatus)) {
                    payStatus = PayStatus.PreSuccess;
                } else {
                    payStatus = PayStatus.PreFail;
                }
                porderId = responseBody.getString("porder_id");
                if (responseBody.containsKey("risk_resp_code")) {
                    payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180268","【") /* "【" */);
                    payMessage.append(responseBody.getString("risk_resp_code"));
                    payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180269","】") /* "】" */);
                }
                if (responseBody.containsKey("risk_resp_data")) {
                    payMessage.append(responseBody.getString("risk_resp_data"));
                }
            } else {
                payStatus = PayStatus.PreFail;
                if (responseHead.containsKey("service_resp_code")) {
                    payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180268","【") /* "【" */);
                    payMessage.append(responseHead.getString("service_resp_code"));
                    payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180269","】") /* "】" */);
                }
                if (responseHead.containsKey("service_resp_desc")) {
                    payMessage.append(responseHead.getString("service_resp_desc"));
                }
            }
        } else {
            payStatus = PayStatus.PayUnknown;
            payMessage.append(postResult.get("message"));
        }
        List<TransferAccount> transferAccounts = new ArrayList<>();
        for (int i = 0; i < row.size(); i++) {
            CtmJSONObject rowData = row.getJSONObject(i);
            TransferAccount transferAccount = new TransferAccount();
            transferAccount.setEntityStatus(EntityStatus.Update);
            transferAccount.setId(rowData.getLong("id"));
            transferAccount.setPubts(rowData.getDate("pubts"));
            transferAccount.set("batno", rowData.get("batno"));
            transferAccount.set("requestseqno", rowData.get("requestseqno"));
            transferAccount.set("transeqno", rowData.get("transeqno"));
            rowData.put("porderid", porderId);
            transferAccount.set("porderid", porderId);
            rowData.put("paymessage", payMessage.toString());
            transferAccount.set("paymessage", payMessage.toString());
            rowData.put("paystatus", payStatus.getValue());
            transferAccount.set("paystatus", payStatus.getValue());
            transferAccounts.add(transferAccount);
        }
        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccounts);
    }

    /**
     * @Author tongyd
     * @Description 构建请求报文头
     * @Date 2019/9/26
     * @Param [transCode, param]
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    private CtmJSONObject buildRequestHead(String transCode, CtmJSONObject param) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", param.get("requestseqno"));
        requestHead.put("cust_no", param.get("customNo"));
        requestHead.put("cust_chnl", bankConnectionAdapterContext.getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", param.get("operator"));
        requestHead.put("oper_sign", param.get("signature"));
        requestHead.put("tran_code", transCode);
        requestHead.put("Yonsuite_AutoTask", "Y");
        return requestHead;
    }

    /*
     * @Author tongyd
     * @Description 构建批量支付预下单报文
     * @Date 2019/9/23
     * @Param [param, row]
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    private CtmJSONObject buildBatchPayPreOrderMsg(CtmJSONObject param, CtmJSONArray row) throws Exception {
        String requestseqno = param.getString("requestseqno");
        CtmJSONObject requestHead = buildRequestHead(ITransCodeConstant.BATCH_PAY_PRE_ORDER, param);
        CtmJSONObject requestBody = new CtmJSONObject();
        String batNo = UUID.randomUUID().toString().replaceAll("-", "");
        requestBody.put("bat_no", batNo); // 批次号
        requestBody.put("acct_no", param.get("payBankAccount_account"));
        requestBody.put("acct_name", param.get("payBankAccount_acctName"));
        requestBody.put("tot_num", row.size());
        //这个是前端计算的批量支付的总金额,前端计算不准确。不再使用
        // requestBody.put("tran_amt", param.getBigDecimal("tranAmt").setScale(2,BigDecimal.ROUND_HALF_UP));
        requestBody.put("curr_code", param.get("currencyCode"));
        requestBody.put("send_url", AppContext.getEnvConfig("servername")
                + "/cm/bankEnterprise/updateBatchPayStatus");
        requestBody.put("porder_validate", "8");
        requestBody.put("other_bank_flag", param.getString("otherBankFlag"));
        String backParam = "yht_tenantid:" + InvocationInfoProxy.getTenantid()
                + ",yht_userid:" + AppContext.getCurrentUser().getYhtUserId();
        CtmJSONArray record = new CtmJSONArray();
        BigDecimal tranAmtTotal = BigDecimal.ZERO;
        for (int i = 0; i < row.size(); i++) {
            CtmJSONObject rowData = row.getJSONObject(i);
            tranAmtTotal = tranAmtTotal.add(rowData.getBigDecimal(IBussinessConstant.ORI_SUM));
            CtmJSONObject recordData = new CtmJSONObject();
            //设置批次号
            rowData.put("batno", batNo);
            //设置请求流水号
            rowData.put("requestseqno", requestseqno);
            //设置交易流水号
            String tranSeqNo = buildTranSeqNo(param.getString("customNo"));
            rowData.put("transeqno", tranSeqNo);
            recordData.put("tran_seq_no", tranSeqNo);

            Map<String, Object> recAccInfo = getRecAccInfo(rowData);
            recordData.put("to_bank_type", recAccInfo.get("to_bank_type"));
            recordData.put("to_acct_no", recAccInfo.get("to_acct_no"));
            recordData.put("to_acct_name", recAccInfo.get("to_acct_name"));
            recordData.put("to_bank_no", recAccInfo.get("to_bank_no"));
            recordData.put("to_brch_no", recAccInfo.get("to_brch_no"));
            recordData.put("to_brch_name", recAccInfo.get("to_brch_name"));
            recordData.put("to_acct_type", recAccInfo.get("to_acct_type"));
            recordData.put("to_addr", "");
            recordData.put("to_city_code", "");
            recordData.put("user_name", "");
            recordData.put("tran_amt", rowData.getBigDecimal(IBussinessConstant.ORI_SUM).setScale(2,BigDecimal.ROUND_HALF_UP));
            recordData.put("use_desc", rowData.getString("purpose")); //用途
            recordData.put("remark", rowData.getString("description")); //附言
            recordData.put("phone_no", "");
            recordData.put("sms_string", "");
            recordData.put("email", "");
            recordData.put("mail_desc", "");
            recordData.put("back_para", backParam);
            record.add(recordData);
        }
        requestBody.put("tran_amt", tranAmtTotal.setScale(2,BigDecimal.ROUND_HALF_UP));
        requestBody.put("record", record);
        CtmJSONObject postMsg = new CtmJSONObject();
        postMsg.put("request_head", requestHead);
        postMsg.put("request_body", requestBody);
        return postMsg;
    }

    /*
     * @Author tongyd
     * @Description 获取收款账户信息
     * @Date 2019/9/23
     * @Param [rowData]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    private Map<String, Object> getRecAccInfo(CtmJSONObject rowData) throws Exception {
        Map<String, Object> recAccInfo = new HashMap<>();
        recAccInfo.put("to_acct_no", rowData.get("recBankAccount_account"));
        recAccInfo.put("to_acct_name", rowData.get("recBankAccount_acctName"));
        recAccInfo.put("to_acct_type", "02");
        Object payLineNumber = null;
        Object recLineNumber = null;
        //其他金融机构开户  没有开户行 recBankAccount_bankNumber 为空  需要特殊处理
        BankdotVO recDepositBank = null;
        if (ObjectUtils.isNotEmpty(rowData.get("recBankAccount_bankNumber"))) {
            recDepositBank =baseRefRpcService.queryBankdotVOByBanddotId(rowData.get("recBankAccount_bankNumber").toString());
        }
        if (recDepositBank!=null) {
            if (recDepositBank.getLinenumber() != null){
                recLineNumber = recDepositBank.getLinenumber();
            }
            recAccInfo.put("to_brch_name", recDepositBank.getName());
        } else {
            EnterpriseBankAcctVO recBankAccount = baseRefRpcService.queryEnterpriseBankAccountById(rowData.getString("recBankAccount"));
            recLineNumber = recBankAccount.getLineNumber();
        }
        recAccInfo.put("to_bank_no", recLineNumber);
        recAccInfo.put("to_brch_no", recLineNumber);
        //其他金融机构开户  没有开户行 recBankAccount_bankNumber 为空  需要特殊处理
        BankdotVO payDepositBank = null;
        if (ObjectUtils.isNotEmpty(rowData.get("payBankAccount_bankNumber"))) {
            payDepositBank =baseRefRpcService.queryBankdotVOByBanddotId(rowData.get("payBankAccount_bankNumber").toString());
        }
        if (payDepositBank!=null && payDepositBank.getLinenumber() != null) {
            payLineNumber = payDepositBank.getLinenumber();
        } else {
            EnterpriseBankAcctVO payBankAccount = baseRefRpcService.queryEnterpriseBankAccountById(rowData.getString("payBankAccount"));
            payLineNumber = payBankAccount.getLineNumber();
        }
        if (rowData.get("tobanktype") != null) {
            recAccInfo.put("to_bank_type", rowData.get("tobanktype"));
        } else {
            if (payLineNumber != null && recLineNumber != null) {
                String toBankType = SystemCodeUtil.getToBankType(payLineNumber, recLineNumber);
                recAccInfo.put("to_bank_type", toBankType);
            } else {
                recAccInfo.put("to_bank_type", "01");
            }
        }
        return recAccInfo;
    }

    /**
     * @Author tongyd
     * @Description 构建交易流水号
     * @Date 2019/8/19
     * @Param [customNo]
     * @return java.lang.String
     **/
    private String buildTranSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("T");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }
    /*
     *@Author
     *@Description 校验时间戳    转账单预下单
     *@Date 2020/7/6 14:55
     *@Param [rows]
     *@Return void
     **/
    public void checkPubTs(CtmJSONArray rows) throws Exception {
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, row.getLong("id"));
            if (row.getDate("pubts").compareTo(transferAccount.getPubts()) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102302"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418026A","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
            }
        }
    }

    /**
     * 校验账户余额
     * @param rowData
     * @return
     * @throws Exception
     */
    private CtmJSONObject verifyInitDataByRowData(CtmJSONArray rowData) throws Exception {
        CtmJSONObject verifyResult = new CtmJSONObject();
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        for (int i = 0; i < rowData.size(); i++) {
            CtmJSONObject data = rowData.getJSONObject(i);
            //校验该账户是否允许透支
            String payBankAccount = data.getString("payBankAccount");
            String payCashAccount = data.getString("payCashAccount");
            String accentity = data.get(IBussinessConstant.ACCENTITY).toString();
            BigDecimal oriSum = data.getBigDecimal(IBussinessConstant.ORI_SUM);
            String currency = data.getString("currency");
            if (payBankAccount != null && oriSum != null) {
                if (!CmpWriteBankaccUtils.checkAccOverDraft(accentity,payBankAccount, currency)) {
                    verifyResult.put("dealSucceed", false);
                    verifyResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418026C","账户余额不足") /* "账户余额不足" */);
                    return verifyResult;
                }
            }
            if (payCashAccount != null && oriSum != null) {
                if (!CmpWriteBankaccUtils.checkAccOverDraft(accentity,payCashAccount, currency)) {
                    verifyResult.put("dealSucceed", false);
                    verifyResult.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418026C","账户余额不足") /* "账户余额不足" */);
                    return verifyResult;
                }
            }
            //校验结算日期是否已日结
            Date maxSettleDate = null;
            if (maxSettleDateMaps.containsKey(accentity)) {
                maxSettleDate = maxSettleDateMaps.get(accentity);
            } else {
                maxSettleDate = settlementService.getMaxSettleDate(accentity);
                maxSettleDateMaps.put(accentity, maxSettleDate);
            }
            if (SettleCheckUtil.checkDailySettlement(maxSettleDate, true)) {
                verifyResult.put("dealSucceed", false);
                verifyResult.put("message", String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180270","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
                return verifyResult;
            }
        }
        verifyResult.put("dealSucceed", true);
        return verifyResult;
    }

    public CtmJSONObject iTrusVerifySignatureByRowData(CtmJSONArray rowData) throws Exception {
        CtmJSONObject verifyResult = new CtmJSONObject();
        for (int i = 0; i < rowData.size(); i++) {
            CtmJSONObject data = rowData.getJSONObject(i);
            if (StringUtils.isEmpty(data.getString("signature"))) {
                continue;
            }
            EnterpriseBankAcctVO bankAcc = baseRefRpcService.queryEnterpriseBankAccountById(data.get("recBankAccount").toString());
            String originalMsg = data.getBigDecimal(IBussinessConstant.ORI_SUM).stripTrailingZeros().toPlainString() + bankAcc.getAccount() + bankAcc.getAcctName();
            if (!digitalSignatureService.iTrusVerifySignature(originalMsg, data.getString("signature"))) {
                verifyResult.put("dealSucceed", false);
                verifyResult.put("message",
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418026D","单据【") /* "单据【" */ + rowData.getJSONObject(i)
                                .get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418026E","】验签失败") /* "】验签失败" */);
                return verifyResult;
            }
        }
        verifyResult.put("dealSucceed", true);
        return verifyResult;
    }

}
