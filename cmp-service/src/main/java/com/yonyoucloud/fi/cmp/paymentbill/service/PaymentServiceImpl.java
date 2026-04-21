package com.yonyoucloud.fi.cmp.paymentbill.service;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.CustomerRpcService;
import com.yonyoucloud.fi.basecom.utils.FIContext;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalanceService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.bankreconciliation.RefundAutoCheckRuleService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.paybill.PaymentBillSettleBO;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.util.business.SystemCodeUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * @ClassName PaymentServiceImpl
 * @Description 付款工作台支付相关操作实现类
 * @Author tongyd
 * @Date 2019/4/28 16:23
 * @Version 1.0
 **/
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    CustomerRpcService customerRpcService;

    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    private JournalService journalService;

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private PaymentServiceUtil paymentServiceUtil;

    @Autowired
    private PaymentSettleService paymentSettleService;

    private AtomicInteger cardinalNumber = new AtomicInteger(0);

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private CurrencyQueryService currencyQueryService;
    @Resource
    private CtmThreadPoolExecutor ctmThreadPoolExecutor;

    @Autowired
    YTSPayBillSettleServiceImpl ytsPayBillSettleServiceImpl;
    @Autowired
    private BankDealDetailService bankDealDetailService;
    @Autowired
    private AccountRealtimeBalanceService accountRealtimeBalanceService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private CtmSignatureService signatureService;
    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;

    //单据智能分类service
    @Resource
    private BillSmartClassifyService billSmartClassifyService;

    //退票辨识规则service
    @Resource
    private RefundAutoCheckRuleService refundAutoCheckRuleService;
    @Autowired
    VendorQueryService vendorQueryService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    //结算方式
    Map<Long, Map<String, Object>> settlemodeMaps = new HashMap<Long, Map<String, Object>>();
    //企业银行账号查询一页默认返回值
    private static int pageSIze = 4999;
    private static final @NonNull Cache<String, Map<String, Object>> currencyCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();
    /**
     * 缓存银行账户的对应币种（多币种时不适用）
     * 缓存规则：账户单币种时缓存单一币种；多币种时缓存默认币种；若多币种时无默认 则取人民币
     */
    private static final @NonNull Cache<String, Map<String, Object>> accountCurrencyCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    /**
     * 缓存币种主键和编码
     */
    private static final @NonNull Cache<String, String> currencyCodeCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    @Override
    public CtmJSONObject internetBankPlaceOrder(CtmJSONObject params) throws Exception {
        CtmJSONArray rows = params.getJSONArray("rows");
        Object code = null;
        if (rows.getJSONObject(0) != null) {
            code = rows.getJSONObject(0).get("code");
        }
        //单据加pk锁，在外层controller中解锁
        CtmJSONObject responseData = new CtmJSONObject();
        checkPubTs(rows);
        List<Object> payBillIds = params.getObject("ids", List.class);
        List<PayBill> payBills = getPayBillById(payBillIds);
        // 业务校验改为后端校验
        checkPayBill(payBills);
        verifySignature(payBills);
        Boolean isBatchPay = payBills.get(0).getIsBatchPay();
        if (isBatchPay == null || (isBatchPay != null && !isBatchPay)) {//如果是新单据 走单笔预下单(由于arap没有更新代码 这里默认先走新的)
            params = this.internetBankPlaceOrderSingle(params, payBills);
            return params;
        } else {
            CtmJSONObject placeOrderMsg = buildPlaceOrderMsg(params, payBills);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            log.error("批量支付预下单参数：" + placeOrderMsg.toString());      // 添加日志 20201130
            requestData.add(new BasicNameValuePair("reqData", placeOrderMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            log.info("调用uri:" + bankConnectionAdapterContext.getChanPayUri());
            CtmJSONObject result = HttpsUtils.doHttpsPost(ITransCodeConstant.BATCH_PLACE_ORDER, requestData, bankConnectionAdapterContext.getChanPayUri());
            log.error("批量支付预下单结果：" + result.toString());
            CtmJSONObject responseFailedData = getResponseFailedData(result);
            if (responseFailedData != null) {
                return responseFailedData;
            }
            updatePlaceOrderResult(result.getJSONObject("data"), payBills);
            params.put("dealSucceed", true);
            params.put("rows", updatePlaceOrderRows(payBillIds, rows));
            for (PayBill payBill : payBills) {
                ctmcmpBusinessLogService.saveBusinessLog(payBill, payBill.getCode(), "", IServicecodeConstant.PAYMENTBILL, IMsgConstant.PAYMENT_BILL, IMsgConstant.BANK_PREORDER);
            }
            return params;
        }
    }

    private void checkPayBill(List<PayBill> payBills) throws Exception {
        for (PayBill payBill : payBills) {
            if (payBill.getSettlemode() == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102174"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806DF","结算方式不能为空！") /* "结算方式不能为空！" */);
            }
            Map<String, Object> settlementWay = QueryBaseDocUtils.querySettlementWayById(payBill.getSettlemode());
            if (MapUtils.isEmpty(settlementWay) || !settlementWay.get("serviceAttr").equals(0)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102175"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E3","单据结算方式的业务属性不为银行业务") /* "单据结算方式的业务属性不为银行业务" */);
            }
        }
    }

    /**
     * 取消预下单
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject internetBankPlaceOrderCancel(CtmJSONObject params) throws Exception {
        log.info("开始预下单取消流程");
        CtmJSONObject row = params.getJSONObject("row");
        //单据加pk锁，在外层controller中解锁
        CtmJSONObject responseData = new CtmJSONObject();
        Long rowid = row.getLong("id");
        List payBillIds = new ArrayList();
        payBillIds.add(rowid);
        //校验pubTs
        PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, row.getLong("id"));
        if (payBill.getPubts().compareTo(row.getDate("pubts")) != 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102176"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C3","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
        }
        //判断支付状态是否为预下单成功
        if (PayStatus.PreSuccess.getValue() != row.getShort("paystatus")) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102177"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C5","支付状态不符合要求") /* "支付状态不符合要求" */);
        }
        payBill.setPaystatus(PayStatus.NoPay);
        EntityTool.setUpdateStatus(payBill);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, payBill);
        ctmcmpBusinessLogService.saveBusinessLog(payBill, payBill.getCode(), "", IServicecodeConstant.PAYMENTBILL, IMsgConstant.PAYMENT_BILL, IMsgConstant.CANCEL_PREORDER);
        params.put("dealSucceed", true);
        return params;
    }

    @Override
    public CtmJSONObject confirmPlaceOrder(CtmJSONObject params) throws Exception {
        CtmJSONArray rows = params.getJSONArray("rows");
        Object code = null;
        if (rows.getJSONObject(0) != null) {
            code = rows.getJSONObject(0).get("code");
        }
        List<PayBill> payBills = getPayBillByPorderId(params.getString("porderId"),
                rows.getJSONObject(0).getLong("id"));
        //校验逻辑处理
        checkPubTs(rows);
        verifyInitData(payBills);
        verifySignature(payBills);

        CtmJSONObject confirmPlaceOrderMsg = buildConfirmPlaceOrderMsg(params,
                payBills);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(confirmPlaceOrderMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", confirmPlaceOrderMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        //启动独立事务跟新单据
        int count = cmCommonService.updatePayForPayStatus(payBills.get(0));
        //查看是否更新成功 若成功说单据状态正确
        if (count == 0) {
            //若count为0 则当前单据状态有误 不能执行后续操作
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102178"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D4","单据[%s]支付状态有误，请在下载支付状态后，重新进行操作") /* "单据[%s]支付状态有误，请在下载支付状态后，重新进行操作" */, payBills.get(0).getCode()));
        }
        log.error("付款工作台网银支付参数：" + code + confirmPlaceOrderMsg.toString());
        CtmJSONObject result = HttpsUtils.doHttpsPost(ITransCodeConstant.PRE_ORDER_TRANSACTION_CONFIRM, requestData, bankConnectionAdapterContext.getChanPayUri());
        log.error("付款工作台网银支付结果：" + code + result.toString());
        CtmJSONObject logData = new CtmJSONObject();
        logData.put(IMsgConstant.BILL_DATA, payBills.get(0));
        logData.put(IMsgConstant.CONFIRM_PLACEORDER_REQUEST, confirmPlaceOrderMsg);
        logData.put(IMsgConstant.CONFIRM_PLACEORDER_RESPONSE, result);
        ctmcmpBusinessLogService.saveBusinessLog(logData, payBills.get(0).getCode(), "", IServicecodeConstant.PAYMENTBILL, IMsgConstant.PAYMENT_BILL, IMsgConstant.BANK_PREORDER);
        CtmJSONObject responseFailedData = getResponseFailedData(result);
        if (responseFailedData != null) {
            return responseFailedData;
        }
        updateConfirmPayResult(result.getJSONObject("data"), payBills);
        List<Object> payBillIds = params.getObject("ids", List.class);
        params.put("dealSucceed", true);
        params.put("rows", updateConfirmPayRows(payBillIds, rows));
        return params;
    }


    @Override
    public String updateBatchPayStatus(String requestData) throws Exception {
        String requestStr = URLDecoder.decode(requestData, "UTF-8");
        log.info("批量支付支付结果推送【11B10R】畅捷支付推送报文：", requestStr);
        String[] reqDataAndReqSignData = requestStr.split("&");
        String reqData = reqDataAndReqSignData[0].split("=")[1];
        String reqSignData = reqDataAndReqSignData[1].split("=")[1];
        if (!bankConnectionAdapterContext.chanPayVerifySignature(reqData, reqSignData)) {
            return HttpsUtils.buildCommonResponseMsg("01", "000002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806DD","批量支付支付结果推送失败：签名校验失败") /* "批量支付支付结果推送失败：签名校验失败" */, "");
        }
        CtmJSONObject requestBody = CtmJSONObject.parseObject(reqData).getJSONObject("request_body");
        int totalNum = requestBody.getInteger("tot_num");
        if (totalNum == 1) {
            CtmJSONObject recordData = requestBody.getJSONObject("record");
            String backPara = recordData.getString("back_para");
            String[] backParas = backPara.split(",");
            String yht_tenantid = backParas[0].split(":")[1];
            String yht_userid = backParas[1].split(":")[1];
            FIContext.setCurrentYhtUser(yht_tenantid, yht_userid);
            updatePayStatusByRecordData(recordData);
        } else if (totalNum > 1) {
            CtmJSONArray record = requestBody.getJSONArray("record");
            for (int i = 0; i < record.size(); i++) {
                CtmJSONObject recordData = record.getJSONObject(i);
                if (i == 0) {
                    CtmJSONObject backPara = recordData.getJSONObject("back_para");
                    FIContext.setCurrentYhtUser(backPara.getString("yht_tenantid"),
                            backPara.getString("yht_userid"));
                }
                updatePayStatusByRecordData(recordData);
            }
        }
        return HttpsUtils.buildCommonResponseMsg("00", "000000", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E9","批量支付支付结果推送成功") /* "批量支付支付结果推送成功" */, "");
    }

    @Override
    public CtmJSONObject queryBatchDetailPayStatus(CtmJSONArray params) throws Exception {
        CtmJSONArray rowsForIsBatchPay = params.getJSONObject(0).getJSONArray("rows");
        CtmJSONObject rowForIsBatchPay = rowsForIsBatchPay.getJSONObject(0);
        PayBill payBillForIsBatchPay = MetaDaoHelper.findById(PayBill.ENTITY_NAME, rowForIsBatchPay.getLong("id"));
        Boolean isBatchPay = payBillForIsBatchPay.getIsBatchPay();
        if (isBatchPay == null || (isBatchPay != null && !isBatchPay)) {//如果是新单据 走单笔查询支付状态(由于arap没有更新代码 这里默认先走新的)
            CtmJSONObject responseData = new CtmJSONObject();
            responseData = this.querySingleDetailPayStatus(params);
            return responseData;
        }
        StringBuilder message = new StringBuilder();
        Set<String> msg = new HashSet<>();
        for (int i = 0; i < params.size(); i++) {
            CtmJSONObject param = params.getJSONObject(i);
            CtmJSONObject batchDetailPayStatusMsg = buildBatchDetailPayStatusMsg(param);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(batchDetailPayStatusMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", batchDetailPayStatusMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            log.error("批量支付畅捷支付推送报文：" + batchDetailPayStatusMsg.toString());      // 添加日志 20201130
            CtmJSONObject result = HttpsUtils.doHttpsPost(ITransCodeConstant.QUERY_BATCH_DETAIL_PAY_STATUS, requestData, bankConnectionAdapterContext.getChanPayUri());
            log.error("批量支付畅捷支付结果报文：" + result.toString());
            message.append(result.getString("message"));
            msg.add(result.getString("message"));
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String serviceStatus = responseHead.getString("service_status");
                if (("00").equals(serviceStatus)) {
                    CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                    int totalNum = responseBody.getInteger("tot_num");
                    if (totalNum == 1) {
                        log.error("========================支付结果：" + responseBody.getJSONObject("record").getString("pay_status"));
                        updatePayStatusByRecordData(responseBody.getJSONObject("record"));
                    } else if (totalNum > 1) {
                        CtmJSONArray record = responseBody.getJSONArray("record");
                        for (int j = 0; j < record.size(); j++) {
                            updatePayStatusByRecordData(record.getJSONObject(j));
                        }
                    }
                } else {
                    String serviceRespCode = responseHead.getString("service_resp_code");
                    String serviceRespDesc = responseHead.getString("service_resp_desc");
                    message.append(serviceRespCode);
                    message.append(serviceRespDesc);
                    msg.add(serviceRespCode + "|" + serviceRespDesc);
                }
            }
        }
        CtmJSONObject responseData = new CtmJSONObject();

        responseData.put("message", msg.iterator().next());
        CtmJSONArray responseRows = new CtmJSONArray();
        for (int i = 0; i < params.size(); i++) {
            CtmJSONArray rows = params.getJSONObject(i).getJSONArray("rows");
            for (int j = 0; j < rows.size(); j++) {
                CtmJSONObject row = rows.getJSONObject(j);
                PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, row.getLong("id"));
                row.put("paystatus", payBill.getPaystatus().getValue());
                row.put("bankseqno", payBill.getBankseqno());
                row.put("pubts", payBill.getPubts());
                responseRows.add(row);
            }
        }
        responseData.put("rows", responseRows);
        responseData.put("rowNum", params.getJSONObject(0).get("rowNum"));
        return responseData;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public CtmJSONObject offLinePay(CtmJSONObject params) throws Exception {
        CtmJSONArray rows = params.getJSONArray("rows");
        long userId = AppContext.getCurrentUser().getId();
        List<String> messages = new ArrayList<>();
        Date date = BillInfoUtils.getBusinessDate();//业务日期
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        //每次请求清空缓存
        settlemodeMaps = new HashMap<>();
        List<PaymentBillSettleBO> results = new ArrayList<>(1);//PaymentBillSettleBO记录响应结果
        List<Future<PaymentBillSettleBO>> futures = new ArrayList<>(rows.size());
        List<YmsLock> ymsLockList = new ArrayList<>();
        if (rows.size() == 1) {
            PaymentBillSettleBO processResult = paymentSettleService.processOffLinePayItem(rows.getJSONObject(0), false, date, userId, maxSettleDateMaps,ymsLockList);
            results.add(processResult);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                CtmJSONObject row = rows.getJSONObject(i);
                Future<PaymentBillSettleBO> task = null;
                try {
                    task = ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                        try {
                            return paymentSettleService.processOffLinePayItem(row, true, date, userId, maxSettleDateMaps,ymsLockList);
                        } catch (Exception e) {
                            log.error("exception when batch process offLinePay", e);
                            return PaymentBillSettleBO.builder()
                                    .failedId(row.getLong("id").toString())
                                    .message(e.getMessage())
                                    .build();
                        }
                    });
                } catch (Exception e) {
                    log.error("exception when process offLinePay", e);
                }
                futures.add(task);
            }
            futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("future get result failed offLinePay", e);
                    return null;
                }
            }).filter(Objects::nonNull).forEach(results::add);
        }

        List<PayBill> updateBills = new ArrayList<>(rows.size());
        Map<String, String> failed = new HashMap<>();
        List<Journal> journalList = new ArrayList<>();
        AtomicInteger failedCount = new AtomicInteger(0);
        results.forEach(processResult -> {
            if (!StringUtils.isBlank(processResult.getMessage())) {
                messages.add(processResult.getMessage());
            }
            if (processResult.getFailedId() != null) {
                failedCount.incrementAndGet();
                failed.put(processResult.getFailedId(), processResult.getFailedId());
            }
            journalList.addAll(processResult.getJournalList());
            if (null != processResult.getUpdateBill()) {
                updateBills.add(processResult.getUpdateBill());
            }
        });

        if (!CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
        if (!CollectionUtils.isEmpty(updateBills)) {
            SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.PaybillMapper.batchUpdatePaybill", updateBills);
        }

        // 2020/11/16 sungc : 线下支付， 由付款申请单生成的付款单，当结算完毕后，同步数据【已支付金额】
        updatePaymentPullPayApplyBillData(updateBills);

        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            row.put("pubts", cmCommonService.getPubTsById(PayBill.ENTITY_NAME, row.getLong("id")));
        }
        StringBuilder message = new StringBuilder();
        if (rows.size() == 1) {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C6","该单据线下支付成功") /* "该单据线下支付成功" */);
        } else {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BC","共：") /* "共：" */);
            message.append(rows.size());
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BE","张单据；") /* "张单据；" */);
            message.append(rows.size());
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C7","张线下支付成功；") /* "张线下支付成功；" */);
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C8","0张线下支付失败！") /* "0张线下支付失败！" */);
        }
        CtmJSONObject responseData = new CtmJSONObject();
        responseData.put("message", message);
        responseData.put("rows", rows);
        responseData.put("msgs", messages);
        responseData.put("messages", messages);
        responseData.put("count", rows.size());
        responseData.put("sucessCount", rows.size() - failedCount.intValue());
        responseData.put("failCount", failedCount.intValue());
        responseData.put("ymsLockList", ymsLockList);
        if (failed.size() > 0) {
            responseData.put("failed", failed);
        }
        return responseData;
    }

    @Override
    public CtmJSONObject cancelOffLinePay(CtmJSONObject params) throws Exception {
        List<String> messages = new ArrayList<>();
        CtmJSONArray rows = params.getJSONArray("rows");
        //东亚说批量查询，查询不到子表数据，下面用到子表数据，所以注释掉不使用
//        List<Object> ids = new ArrayList<Object>();
//        Map<Long,PayBill> payBillMap = new HashMap<Long,PayBill>();
//        for (int i = 0; i < rows.size(); i++) {
//            CtmJSONObject rowData = rows.getJSONObject(i);
//            Long id = rowData.getLong("id");
//            ids.add(id);
//        }
//        List<PayBill> payBillList = getPayBillById(ids);
//        for (PayBill paybill:payBillList) {
//            payBillMap.put(paybill.getId(),paybill);
//        }
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        List<PaymentBillSettleBO> results = new ArrayList<>(1);
        List<Future<PaymentBillSettleBO>> futures = new ArrayList<>(rows.size());
        if (rows.size() == 1) {
            PaymentBillSettleBO processResult = paymentSettleService.processCancelOffLinePayItem(rows.getJSONObject(0), false, maxSettleDateMaps);
            results.add(processResult);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                CtmJSONObject row = rows.getJSONObject(i);
                Future<PaymentBillSettleBO> task = null;
                try {
                    task = ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                        try {
                            return paymentSettleService.processCancelOffLinePayItem(row, true, maxSettleDateMaps);
                        } catch (Exception e) {
                            log.error("exception when batch process payment CancelOffLinePay", e);
                            return PaymentBillSettleBO.builder()
                                    .failedId(row.getLong("id").toString())
                                    .message(e.getMessage())
                                    .build();
                        }
                    });
                } catch (Exception e) {
                    log.error("exception when process executorServicePool payment CancelOffLinePay", e);
                }
                futures.add(task);
            }
            futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("future get result failed CancelOffLinePay", e);
                    return null;
                }
            }).filter(Objects::nonNull).forEach(results::add);
        }

        List<PayBill> updateBills = new ArrayList<>(rows.size());
        Map<String, String> failed = new HashMap<>();
        List<Journal> journalList = new ArrayList<>();
        AtomicInteger failedCount = new AtomicInteger(0);
        results.forEach(processResult -> {
            if (!StringUtils.isBlank(processResult.getMessage())) {
                messages.add(processResult.getMessage());
            }
            if (processResult.getFailedId() != null) {
                failedCount.incrementAndGet();
                failed.put(processResult.getFailedId(), processResult.getFailedId());
            }
            journalList.addAll(processResult.getJournalList());
            if (null != processResult.getUpdateBill()) {
                updateBills.add(processResult.getUpdateBill());
            }
        });

        if (!CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
        if (!CollectionUtils.isEmpty(updateBills)) {
            EntityTool.setUpdateStatus(updateBills);
            MetaDaoHelper.update(PayBill.ENTITY_NAME, updateBills);
        }

        //  2020/11/16  sungc: 取消线下支付，数据恢复
        cancelOffLinPayRollbackPayApplyBillData(updateBills);

        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            row.put("pubts", cmCommonService.getPubTsById(PayBill.ENTITY_NAME, row.getLong("id")));
        }
        StringBuilder message = new StringBuilder();
        if (rows.size() == 1) {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806B8","该单据取消线下支付成功") /* "该单据取消线下支付成功" */);
        } else {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BC","共：") /* "共：" */);
            message.append(rows.size());
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BE","张单据；") /* "张单据；" */);
            message.append(rows.size() - failedCount.intValue());
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C0","张取消线下支付成功；") /* "张取消线下支付成功；" */);
            message.append(failedCount);
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C2","张取消线下支付失败！") /* "张取消线下支付失败！" */);
        }
        CtmJSONObject responseData = new CtmJSONObject();
        responseData.put("message", message);
        responseData.put("msgs", messages);
        responseData.put("rows", rows);
        responseData.put("messages", messages);
        responseData.put("count", rows.size());
        responseData.put("sucessCount", rows.size() - failedCount.intValue());
        responseData.put("failCount", failedCount);
        if (failed.size() > 0) {
            responseData.put("failed", failed);
        }
        return responseData;
    }

    private void cancelOffLinPayRollbackPayApplyBillData(List<PayBill> updateBills) throws Exception{
        for (PayBill bill : updateBills) {
            if (EventType.PayApplyBill.getValue() == bill.getBilltype().getValue()
                    || (EventSource.Manual.getValue() == bill.getSrcitem().getValue() && CMP_PAY_APPLICATION.equals(bill.getSrcflag()))) {
                log.error("cancel offLinePay pay bill back write Pay Apply Bill, 1. tenant_id = {},payStatus = {},billType = {}, srcItem = {},srcFlag = {}, payBill = {}",
                        InvocationInfoProxy.getTenantid(),
                        bill.getPaystatus().getValue(),
                        bill.getBilltype().getValue(),
                        bill.getSrcitem().getValue(),
                        bill.getSrcflag(),
                        bill);
                QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
                querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name(MAINID).eq(bill.getId())));
                try {
                    List<Map<String, Object>> mapList = MetaDaoHelper.query(PayBillb.ENTITY_NAME, querySchemaJ);
                    for (Map<String, Object> e : mapList) {
                            Long id = Long.valueOf(e.get(SRCBILLITEMID).toString());
                            BigDecimal oriSum = (BigDecimal) e.get(ORISUM);
                            log.error("cancel offLinePay pay bill back write Pay Apply Bill, 2. tenant_id = {}, payBillId = {}, payBillBId = {}, src_bill_item_id = {}",
                                    InvocationInfoProxy.getTenantid(), bill.getId(), e.get("id"), e.get(SRCBILLITEMID));
                            // 更新子表已付金额和未付金额
                            PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, id);
                            log.error("cancel offLinePay pay bill back write Pay Apply Bill, 3. tenant_id = {}, payApplicationBill_b = {}",
                                    InvocationInfoProxy.getTenantid(), payApplicationBill_b);
                            if (null != payApplicationBill_b) {
                                payApplicationBill_b.setPaidAmount(BigDecimalUtils.safeSubtract(payApplicationBill_b.getPaidAmount(), oriSum));
                                payApplicationBill_b.setUnpaidAmount(BigDecimalUtils.safeAdd(payApplicationBill_b.getUnpaidAmount(), oriSum));
                                EntityTool.setUpdateStatus(payApplicationBill_b);
                                MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, payApplicationBill_b);
                                // 更新主表已付金额总数和未付金额总数
                                Long mainid = payApplicationBill_b.getMainid();
                                PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, mainid);
                                if (oriSum.equals(payApplicationBill.getPaidAmountSum())) {
                                    payApplicationBill.setPaidAmountSum(new BigDecimal(0));
                                } else {
                                    payApplicationBill.setPaidAmountSum(BigDecimalUtils.safeSubtract(payApplicationBill.getPaidAmountSum(), oriSum));
                                }
                                payApplicationBill.setUnpaidAmountSum(BigDecimalUtils.safeAdd(payApplicationBill.getUnpaidAmountSum(), oriSum));
                                EntityTool.setUpdateStatus(payApplicationBill);
                                if (new BigDecimal(0).equals(payApplicationBill.getUnpaidAmountSum()) || null == payApplicationBill.getUnpaidAmountSum()) {
                                    payApplicationBill.setPayBillStatus(PayBillStatus.PaymentCompleted);
                                } else if (new BigDecimal(0).equals(payApplicationBill.getPaidAmountSum()) || null == payApplicationBill.getPaidAmountSum()) {
                                    payApplicationBill.setPayBillStatus(PayBillStatus.PendingPayment);
                                } else if (!payApplicationBill.getUnpaidAmountSum().equals(payApplicationBill.getPaidAmountSum())
                                        && !new BigDecimal(0).equals(payApplicationBill.getUnpaidAmountSum())
                                        && !new BigDecimal(0).equals(payApplicationBill.getPaidAmountSum())) {
                                    payApplicationBill.setPayBillStatus(PayBillStatus.PartialPayment);
                                } else if (new BigDecimal(0).equals(payApplicationBill.getPaymentApplyAmountSum()) || null == payApplicationBill.getPaymentApplyAmountSum()) {
                                    payApplicationBill.setPayBillStatus(PayBillStatus.PendingApproval);
                                }
                                MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBill);
                            }
                    }
                } catch (Exception e) {
                    log.error("cancel offLinePay pay bill back write Pay Apply Bill fail, 4. tenant_id = {}, code={}, errorMsg={}",
                            InvocationInfoProxy.getTenantid(), bill.getCode(), e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102179"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C2", "回写付款申请工作台金额失败！") /* "回写付款申请工作台金额失败！" */);
                }
            }
        }
    }

    @Override
    public String getChanPayChanelNo() {
        return bankConnectionAdapterContext.getChanPayCustomChanel();
    }

    @Override
    public List<Map<String, Object>> queryBankAccountSetting(String accentitys, String banktypes, String currencys) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(
                " id,tenant,accentity,openFlag,enterpriseBankAccount,enterpriseBankAccount.acctName as acctName,enterpriseBankAccount.name as name,"
                        + "enterpriseBankAccount.account as account, enterpriseBankAccount.bankNumber.bank as bank,"
                        + "customNo,enterpriseBankAccount.id as bankId,enterpriseBankAccount.enable as enable");// 判断银行账户表是否为空故多差一个id
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        String[] accentityArr = null;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accentitys)) {
            accentityArr = accentitys.split(";");
        }
        if (accentityArr != null && accentityArr.length >0){
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentityArr));
        }
        String[] banktypeArr;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(banktypes)) {
            banktypeArr = banktypes.split(";");
            if(banktypeArr != null && banktypeArr.length >0){
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount.bankNumber.bank").in(banktypeArr));
            }
        }
        String[] currencyArr = null;
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(currencys)) {
            currencyArr = currencys.split(";");
            if(currencyArr != null && currencyArr.length >0){
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount.currencyList.currency").in(currencyArr));
            }
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        return list;
    }

    @Override
    public CtmJSONObject paymentAudit(CtmJSONObject param) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103015"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2575C04E00004", "在财务新架构环境下，不允许审批付款单。") /* "在财务新架构环境下，不允许审批付款单。" */);
        }
        CtmJSONArray rows = param.getJSONArray("rows");
        List<String> messages = new ArrayList<>();
        int failedCount = 0;
        CtmJSONObject failed = new CtmJSONObject();
        Date date = BillInfoUtils.getBusinessDate();
        List<Object> ids = new ArrayList<Object>();
        Map<Long, PayBill> payBillMap = new HashMap<Long, PayBill>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            ids.add(id);
        }
        List<PayBill> payBillList = getPayBillById(ids);
        for (PayBill paybill : payBillList) {
            payBillMap.put(paybill.getId(), paybill);
        }
        List<PayBill> updateBills = new ArrayList<>(rows.size());
        List<Journal> journalList = new ArrayList<Journal>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            PayBill payBill = payBillMap.get(row.getLong("id"));
            if (!ValueUtils.isNotEmptyObj(payBill)) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D9","单据不存在 id:") /* "单据不存在 id:" */ + rows.getJSONObject(i).getLong("id"));
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102180"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D9","单据不存在 id:") /* "单据不存在 id:" */ + rows.getJSONObject(i).getLong("id"));
                }
                failedCount++;
                continue;
            }
            PayStatus payStatus = payBill.getPaystatus();
            if (date != null && payBill.getVouchdate() != null && date.compareTo(payBill.getVouchdate()) < 0) {
                failed.put(payBill.getId().toString(), payBill.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + MessageUtils.getMessage(
                        "P_YS_FI_CM_0001066023") /* "】审批日期小于单据日期，不能审批！" */);
                if (rows.size() == 1) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + MessageUtils.getMessage(
                                    "P_YS_FI_CM_0001066023") /* "】审批日期小于单据日期，不能审批！" */);
                }
                i++;
                continue;
            }

            if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.PreFail && payStatus != PayStatus.Fail) {
                failed.put(row.getLong("id").toString(), row.getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + MessageUtils.getMessage(
                        "P_YS_FI_CM_0001038117") /* "】支付状态不能进行审批！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102181"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E4","该单据支付状态不能进行审批！") /* "该单据支付状态不能进行审批！" */);
                }
                failedCount++;
                continue;
            }
            AuditStatus auditStatus = payBill.getAuditstatus();
            if (auditStatus != null && auditStatus == AuditStatus.Complete) {
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + MessageUtils.getMessage(
                        "P_YS_FI_CM_0001008433") /* "】已审批，不能进行重复审批！" */);
                failed.put(row.getLong("id").toString(), row.getLong("id").toString());
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102182"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806EA","该单据已审批，不能进行重复审批！") /* "该单据已审批，不能进行重复审批！" */);
                }
                failedCount++;
                continue;
            }
            EventSource eventSource = payBill.getSrcitem();
            if (eventSource != null && eventSource != EventSource.Cmpchase) {
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + MessageUtils.getMessage(
                        "P_YS_FI_CM_0001008425") /* "】不是现金自制单据，不能进行审批！" */);
                failed.put(row.getLong("id").toString(), row.getLong("id").toString());
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102183"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BF","该单据不是现金自制单据，不能进行审批！") /* "该单据不是现金自制单据，不能进行审批！" */);
                }
                failedCount++;
                continue;
            }
            if (row.getDate("pubts").compareTo(payBill.getPubts()) != 0) {
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C4","】数据无效，请刷新后重试") /* "】数据无效，请刷新后重试" */);
                failed.put(row.getLong("id").toString(), row.getLong("id").toString());
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102176"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C3","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
                }
                failedCount++;
                continue;
            }
            payBill.setAuditstatus(AuditStatus.Complete);
            payBill.setAuditorId(AppContext.getCurrentUser().getId());
            payBill.setAuditor(AppContext.getCurrentUser().getName());
            payBill.setAuditTime(new Date());
            payBill.setAuditDate(BillInfoUtils.getBusinessDate());
            journalList.addAll(journalService.updateJournalByBill(payBill));
            updateBills.add(payBill);
            row.put("auditstatus", payBill.getAuditstatus().getValue());
        }
        if (!CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
        EntityTool.setUpdateStatus(updateBills);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, updateBills);
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            row.put("pubts", cmCommonService.getPubTsById(PayBill.ENTITY_NAME, row.getLong("id")));
        }
        StringBuilder message = new StringBuilder();
        if (rows.size() == 1) {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CE","审批成功!") /* "审批成功!" */);
        } else {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BC","共：") /* "共：" */);
            message.append(rows.size());
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BE","张单据；") /* "张单据；" */);
            message.append(rows.size() - failedCount);
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D1","张审批通过；") /* "张审批通过；" */);
            message.append(failedCount);
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D2","张审批未通过！") /* "张审批未通过！" */);
        }
        CtmJSONObject responseData = new CtmJSONObject();
        responseData.put("message", message);
        responseData.put("rows", rows);
        responseData.put("msgs", messages);
        responseData.put("messages", messages);
        responseData.put("count", rows.size());
        responseData.put("sucessCount", rows.size() - failedCount);
        responseData.put("failCount", failedCount);
        if (failed.size() > 0) {
            responseData.put("failed", failed);
        }
        // 当付款单拉取的是付款申请时，单据状态改为待付款。
        paymentServiceUtil.auditPayBillPullPayApplyBillUpdatePayBillStatus(updateBills, "audit");
        return responseData;
    }

    @Override
    public CtmJSONObject paymentUnAudit(CtmJSONObject param) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103017"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2583004E00003", "在财务新架构环境下，不允许弃审付款单。") /* "在财务新架构环境下，不允许弃审付款单。" */);
        }
        CtmJSONArray rows = param.getJSONArray("rows");
        List<PayBill> updateBills = new ArrayList<>(rows.size());
        List<Journal> journalList = new ArrayList<Journal>();
        List<String> messages = new ArrayList<>();
        CtmJSONObject failed = new CtmJSONObject();
        int failcount = 0;
        List<Object> ids = new ArrayList<Object>();
        Map<Long, PayBill> payBillMap = new HashMap<Long, PayBill>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject rowData = rows.getJSONObject(i);
            Long id = rowData.getLong("id");
            ids.add(id);
        }
        List<PayBill> payBillList = getPayBillById(ids);
        for (PayBill paybill : payBillList) {
            payBillMap.put(paybill.getId(), paybill);
        }
        List<YmsLock> ymsLockList = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(rows.getJSONObject(i).getLong("id").toString());
            if (null == ymsLock) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + rows.getJSONObject(i).getString("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806DA","】已锁定，请勿操作！") /* "】已锁定，请勿操作！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101740"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + rows.getJSONObject(i).getString("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806DB","】已锁定，请勿操作") /* "】已锁定，请勿操作" */);
                }
                failcount++;
                continue;
            }
            ymsLockList.add(ymsLock);
            PayBill payBill = payBillMap.get(rows.getJSONObject(i).getLong("id"));
            if (!ValueUtils.isNotEmptyObj(payBill)) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D9","单据不存在 id:") /* "单据不存在 id:" */ + rows.getJSONObject(i).getLong("id"));
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102180"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D9","单据不存在 id:") /* "单据不存在 id:" */ + rows.getJSONObject(i).getLong("id"));
                }
                failcount++;
                continue;
            }
            PayStatus payStatus = payBill.getPaystatus();
            if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.Fail && payStatus != PayStatus.PreFail) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E0","】支付状态不能进行取消审批！") /* "】支付状态不能进行取消审批！" */);
                if (rows.size() == 1) {

                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101740"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E0","】支付状态不能进行取消审批！") /* "】支付状态不能进行取消审批！" */);
                }
                failcount++;
                continue;
            }
            AuditStatus auditStatus = payBill.getAuditstatus();
            if (auditStatus != null && auditStatus.getValue() == AuditStatus.Incomplete.getValue()) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E8","】未审批，不能进行取消审批！") /* "】未审批，不能进行取消审批！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101740"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E8","】未审批，不能进行取消审批！") /* "】未审批，不能进行取消审批！" */);
                }
                failcount++;
                continue;
            }
            EventSource eventSource = payBill.getSrcitem();
            if (eventSource != null && eventSource != EventSource.Cmpchase) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806B9","】不是现金自制单据，不能进行取消审批！") /* "】不是现金自制单据，不能进行取消审批！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101740"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806B9","】不是现金自制单据，不能进行取消审批！") /* "】不是现金自制单据，不能进行取消审批！" */);

                }
                failcount++;
                continue;
            }
            // begin 日结逻辑控制调整 majfd 21/06/07
            //已日结后不能取消审批
//            QuerySchema querySchema = QuerySchema.create().addSelect("1");
//            querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("settleflag").eq(true),
//                    QueryCondition.name("settlementdate").eq(payBill.get("vouchdate")),
//                    QueryCondition.name("accentity").eq(payBill.get("accentity"))));
//            List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME, querySchema);
//            if (ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0) {
//                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
//                messages.add(MessageUtils.getMessage("P_YS_FI_CM_0000026209") /* "单据【" */ + payBill.getCode() +   MessageUtils.getMessage("P_YS_FI_CM_0000153345") /* "】已日结，不能取消审批！" */);
//                if (rows.size()==1){
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102184"),MessageUtils.getMessage("P_YS_FI_CM_0000026209") /* "单据【" */ + payBill.getCode() + MessageUtils.getMessage("P_YS_FI_CM_0000153345") /* "】已日结，不能取消审批！" */);
//
//                }
//                failcount++;
//                continue;
//            }
            // end
            //勾对完成后不能取消审批
            if (journalService.checkJournal(payBill.getId())) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CC","】已勾对，不能取消审批！") /* "】已勾对，不能取消审批！" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101740"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BA","单据【") /* "单据【" */ + payBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CC","】已勾对，不能取消审批！") /* "】已勾对，不能取消审批！" */);

                }
                failcount++;
                continue;
            }
            Boolean matchJournal = journalService.matchJournal(rows.getJSONObject(i).getLong("id"));
            if (matchJournal) {
                failed.put(rows.getJSONObject(i).getLong("id").toString(), rows.getJSONObject(i).getLong("id").toString());
                messages.add(rows.getJSONObject(i).get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CF","单据已经关联匹配银行交易回单，不能取消审批") /* "单据已经关联匹配银行交易回单，不能取消审批" */);
                if (rows.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102185"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CF","单据已经关联匹配银行交易回单，不能取消审批") /* "单据已经关联匹配银行交易回单，不能取消审批" */);
                }
                failcount++;
                continue;
            }
            payBill.setAuditstatus(AuditStatus.Incomplete);
            payBill.setAuditorId(null);
            payBill.setAuditor(null);
            payBill.setAuditTime(null);
            payBill.setAuditDate(null);
            rows.getJSONObject(i).put("auditstatus", AuditStatus.Incomplete.getValue());
            updateBills.add(payBill);
            journalList.addAll(journalService.updateJournalByBill(payBill));
        }
        if (!CollectionUtils.isEmpty(journalList)) {
            MetaDaoHelper.update(Journal.ENTITY_NAME, journalList);
        }
        EntityTool.setUpdateStatus(updateBills);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, updateBills);
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            row.put("pubts", cmCommonService.getPubTsById(PayBill.ENTITY_NAME, row.getLong("id")));
        }
        StringBuilder message = new StringBuilder();
        if (rows.size() == 1) {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D5","取消审批成功!") /* "取消审批成功!" */);
        } else {
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BC","共：") /* "共：" */);
            message.append(rows.size());
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806BE","张单据；") /* "张单据；" */);
            message.append(rows.size() - failcount);
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D7","张取消审批成功；") /* "张取消审批成功；" */);
            message.append(failcount);
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D8","张取消审批失败！") /* "张取消审批失败！" */);
        }
        CtmJSONObject responseData = new CtmJSONObject();
        responseData.put("message", message);
        responseData.put("msgs", messages);
        responseData.put("rows", rows);
        responseData.put("messages", messages);
        responseData.put("count", rows.size());
        responseData.put("sucessCount", rows.size() - failcount);
        responseData.put("failCount", failcount);
        responseData.put("ymsLockList", ymsLockList);
        if (failed.size() > 0) {
            responseData.put("failed", failed);
        }
        // 当付款单拉取的是付款申请时，单据状态改为已审核。
        auditPayBillPullPayApplyBillUpdatePayBillStatus(updateBills, "unaudit");
        return responseData;
    }

    private void auditPayBillPullPayApplyBillUpdatePayBillStatus(List<PayBill> updateBills, String flag) {
        for (PayBill payBill : updateBills) {
            if (ValueUtils.isNotEmptyObj(payBill.getBilltype()) && payBill.getBilltype().getValue() == (short) 59) {
                QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
                querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("mainid").eq(payBill.getId())));
                try {
                    List<Map<String, Object>> mapList = MetaDaoHelper.query(PayBillb.ENTITY_NAME, querySchemaJ);
                    if (!ValueUtils.isNotEmptyObj(mapList)) {
                        continue;
                    }
                    Map<String, Object> map = mapList.get(0);
                    try {
                        Set<Object> srcbillitemid = new HashSet<>(2);
                        srcbillitemid.add(map.get("srcbillitemid"));
                        // 更新子表预占金额
                        cmCommonService.updateStatePayApplyBill(flag, srcbillitemid);
                    } catch (Exception exception) {
                        log.error("调整付款申请审批状态失败!:" + exception.getMessage());
                    }
                } catch (Exception e) {
                    log.error("调整付款申请审批状态失败:" + e.getMessage());
                }
            }
        }
    }


    @Override
    public List<PayBill> queryAggvoByIds(Long[] ids) throws Exception {
        List<Map<String, Object>> payBillListQuery = MetaDaoHelper.queryByIds("cmp.paybill.PayBill", "*", ids);
        List<Map<String, Object>> payBListQuery = MetaDaoHelper.query("cmp.paybill.PayBillb", QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("mainid").in(ids)));
        Map<Long, List<PayBillb>> payBMapList = new HashMap<Long, List<PayBillb>>();
        List<PayBillb> pbyBList = null;
        for (Map<String, Object> map : payBListQuery) {
            PayBillb pbyBill_b = new PayBillb();
            pbyBill_b.init(map);
            if (payBMapList.get(map.get("mainid")) == null) {
                pbyBList = new ArrayList<PayBillb>();
            } else {
                pbyBList = payBMapList.get(map.get("mainid"));
            }
            pbyBList.add(pbyBill_b);
            payBMapList.put((Long) map.get("mainid"), pbyBList);
        }
        List<PayBill> payBillList = new ArrayList<PayBill>();
        for (Map<String, Object> map : payBillListQuery) {
            PayBill payBill = new PayBill();
            payBill.init(map);
            payBillList.add(payBill);
            payBill.setPayBillb(payBMapList.get(payBill.getId()));
        }
        return payBillList;
    }

    private CtmJSONObject getResponseFailedData(CtmJSONObject result) {
        if (result.getInteger("code") == 1) {
            return null;
        }
        CtmJSONObject responseData = new CtmJSONObject();
        responseData.put("dealSucceed", false);
        responseData.put("failureMessage", result.get("message"));
        return responseData;
    }

    /*
     *@Author tongyd
     *@Description 校验时间戳
     *@Date 2019/7/23 8:40
     *@Param [rows]
     *@Return void
     **/
    private void checkPubTs(CtmJSONArray rows) throws Exception {
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, row.getLong("id"));
            if (row.getDate("pubts").compareTo(payBill.getPubts()) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102176"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C3","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
            }
        }
    }

    /**
     * @Author tongyd
     * @Description 获取收款账户信息
     * @Date 2019/6/27 19:09
     * @Param [payBill, payAccount]
     * @Return java.util.Map<java.lang.String, java.lang.Object>
     **/
    private Map<String, Object> getRecAccInfo(PayBill payBill, EnterpriseBankAcctVO payAccount) throws Exception {
        Map<String, Object> recAccInfo = new HashMap<>();
        CaObject caObject = payBill.getCaobject();
        //开户行ID
        Object depositBankId = null;
        //收款行联行号
        Object recLineNumber = null;
        if (caObject == CaObject.Supplier) {
            if (null == payBill.getSupplierbankaccount()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102186"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D6","供应商银行账户为空") /* "供应商银行账户为空" */);
            }
            Map<String, Object> condition = new HashMap<>();
            condition.put("id", payBill.getSupplierbankaccount());
            List<VendorBankVO> supplierAccounts = vendorQueryService.getVendorBanksByCondition(condition);
            if (supplierAccounts != null && supplierAccounts.size() > 0) {
                VendorBankVO vendorBankVO = supplierAccounts.get(0);
                recAccInfo.put("to_acct_no", vendorBankVO.getAccount());
                recAccInfo.put("to_acct_name", vendorBankVO.getAccountname());
                if (vendorBankVO.getAccountType() == null) {
                    recAccInfo.put("to_acct_type", "02");
                } else if (1 == vendorBankVO.getAccountType()) {
                    recAccInfo.put("to_acct_type", "01");
                } else if (0 == vendorBankVO.getAccountType()) {
                    recAccInfo.put("to_acct_type", "02");
                }
                depositBankId = vendorBankVO.getOpenaccountbank();
                recLineNumber = vendorBankVO.getCorrespondentcode();
            }
        } else if (caObject == CaObject.Customer) {
            if (null == payBill.getCustomerbankaccount()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102187"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806DC","客户银行账户为空") /* "客户银行账户为空" */);
            }
            List<Long> ids = new ArrayList<Long>();
            ids.add(payBill.getCustomerbankaccount());
            AgentFinancialDTO customerAccount = customerRpcService.queryAccEntityById(ids).get(0);
            recAccInfo.put("to_acct_no", customerAccount.getBankAccount());
            recAccInfo.put("to_acct_name", customerAccount.getBankAccountName());
            if (customerAccount.getAccountType() == null) {
                recAccInfo.put("to_acct_type", "02");
            } else if (customerAccount.getAccountType().equals(1)) {
                recAccInfo.put("to_acct_type", "01");
            } else if (customerAccount.getAccountType().equals(0)) {
                recAccInfo.put("to_acct_type", "02");
            }
            depositBankId = customerAccount.getOpenBank();
            recLineNumber = customerAccount.getJointLineNo();
        } else if (caObject == CaObject.Employee) {
            if (null == payBill.getStaffBankAccount()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102188"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E1","员工银行账户为空") /* "员工银行账户为空" */);
            }
            Map<String, Object> employeeAccount = QueryBaseDocUtils.queryStaffBankAccountById(payBill.getStaffBankAccount());
            Map<String, Object> employee = QueryBaseDocUtils.queryStaffById(payBill.getEmployee());
            recAccInfo.put("to_acct_no", employeeAccount.get("account"));
            recAccInfo.put("to_acct_name", employee.get("name"));
            recAccInfo.put("to_acct_type", "01");
            depositBankId = employeeAccount.get("bankname");
        } else if (caObject == CaObject.Other) {
            recAccInfo.put("to_acct_no", payBill.getRetailerAccountNo());
            recAccInfo.put("to_acct_name", payBill.getRetailerAccountName());
            RetailerAccountType accountType = payBill.getRetailerAccountType();
            if (accountType == RetailerAccountType.forPrivate) {
                recAccInfo.put("to_acct_type", "01");
            } else if (accountType == RetailerAccountType.toPublic) {
                recAccInfo.put("to_acct_type", "02");
            } else if (accountType == RetailerAccountType.bankAccount) {
                recAccInfo.put("to_acct_type", "03");
            }
            recLineNumber = payBill.getRetailerLineNumber();
            recAccInfo.put("to_bank_no", recLineNumber);
            recAccInfo.put("to_brch_no", recLineNumber);
            if (payBill.getRetailerBankName() != null && !" ".equals(payBill.getRetailerBankName())) {
                recAccInfo.put("to_brch_name", payBill.getRetailerBankName());
            } else {
                Map<String, Object> queryCond = new HashMap<>(1);
                queryCond.put("linenumber", recLineNumber);
                List<Map<String, Object>> depositBank = QueryBaseDocUtils.queryDepositBankByCondition(queryCond);
                if (depositBank.size() > 0) {
                    recAccInfo.put("to_brch_name", depositBank.get(0).get("name"));
                } else {
                    recAccInfo.put("to_brch_name", "");
                }
            }
        }
        // 联行号算法修改，根据银行账户的开户行，获取银行网点档案中的联行号，获取不到时，取银行账户档案中的联行号
        if (depositBankId != null) {
            Map<String, Object> depositBank = QueryBaseDocUtils.queryDepositBankById(depositBankId);
            if (depositBank != null) {
                if (depositBank.get("linenumber") != null) {
                    recLineNumber = depositBank.get("linenumber");
                }
                recAccInfo.put("to_bank_no", recLineNumber);
                recAccInfo.put("to_brch_no", recLineNumber);
                recAccInfo.put("to_brch_name", depositBank.get("name"));
            }
        }
        Object payLineNumber = null;
        Map<String, Object> payDepositBank = QueryBaseDocUtils.queryDepositBankById(payAccount.getBankNumber());
        if (payDepositBank != null && payDepositBank.get("linenumber") != null) {
            payLineNumber = payDepositBank.get("linenumber");
        } else {
            payLineNumber = payAccount.getLineNumber();
        }
        if (payBill.getTobanktype() != null) {
            recAccInfo.put("to_bank_type", payBill.getTobanktype());
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
     * @return com.yonyou.yonbip.ctm.json.CtmJSONArray
     * @Author tongyd
     * @Description 支付确认更新前段行数据
     * @Date 2019/8/13
     * @Param [payBillIds, rows]
     **/
    private CtmJSONArray updateConfirmPayRows(List<Object> payBillIds, CtmJSONArray rows) throws Exception {
        List<PayBill> payBills = getPayBillById(payBillIds);
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            Iterator<PayBill> iterator = payBills.iterator();
            while (iterator.hasNext()) {
                PayBill payBill = iterator.next();
                if (payBill.getId().toString().equals(row.get("id").toString())) {
                    row.put("paystatus", payBill.getPaystatus().getValue());
                    row.put("paymessage", payBill.getPaymessage());
                    row.put("pubts", payBill.getPubts());
                    iterator.remove();
                    break;
                }
            }
        }
        return rows;
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONArray
     * @Author tongyd
     * @Description 更新预下单前端行数据
     * @Date 2019/8/13
     * @Param [payBillIds, rows]
     **/
    private CtmJSONArray updatePlaceOrderRows(List<Object> payBillIds, CtmJSONArray rows) throws Exception {
        List<PayBill> payBills = getPayBillById(payBillIds);
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject row = rows.getJSONObject(i);
            Iterator<PayBill> iterator = payBills.iterator();
            while (iterator.hasNext()) {
                PayBill payBill = iterator.next();
                if (payBill.getId().toString().equals(row.get("id").toString())) {
                    row.put("batno", payBill.getBatno());
                    row.put("requestseqno", payBill.getRequestseqno());
                    row.put("transeqno", payBill.getTranseqno());
                    row.put("paystatus", payBill.getPaystatus().getValue());
                    row.put("settlestatus", payBill.getSettlestatus().getValue());
                    row.put("porderid", payBill.getPorderid());
                    row.put("paymessage", payBill.getPaymessage());
                    row.put("pubts", payBill.getPubts());
                    iterator.remove();
                    break;
                }
            }
        }
        return rows;
    }

    /*
     *@Author tongyd
     *@Description 校验数据签名
     *@Date 2019/6/18 11:23
     *@Param [payBills]
     *@Return void
     **/
    private void verifySignature(List<PayBill> payBills) throws Exception {
        for (PayBill bill : payBills) {
            if (StringUtils.isBlank(bill.getSignature())) {
                continue;
            }
            String originalMsg = DigitalSignatureUtils.getOriginalMsg(bill);
            if (!signatureService.iTrusVerifySignature(originalMsg,
                    bill.getSignature())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102189"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E6","单据[%s]数据签名验证失败，请检查该单据制单后是否有修改收方银行账号、收方开户名档案字段，如有修改请弃审单据，进行修改保存操作！") /* "单据[%s]数据签名验证失败，请检查该单据制单后是否有修改收方银行账号、收方开户名档案字段，如有修改请弃审单据，进行修改保存操作！" */, bill.getCode()));
            }
        }
    }

    /**
     * 校验账户余额
     *
     * @param payBills
     * @throws Exception
     */
    private void verifyInitData(List<PayBill> payBills) throws Exception {
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        for (PayBill bill : payBills) {
            String enterprisebankaccount = bill.getEnterprisebankaccount();
            String cashaccount = bill.getCashaccount();
            if (enterprisebankaccount != null) {
                if (!CmpWriteBankaccUtils.checkAccOverDraft(bill.getAccentity(),enterprisebankaccount, bill.getCurrency())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102190"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C1","账户余额不足") /* "账户余额不足" */);
                }
            }
            if (cashaccount != null) {
                if (!CmpWriteBankaccUtils.checkAccOverDraft(bill.getAccentity(),cashaccount, bill.getCurrency())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102190"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C1","账户余额不足") /* "账户余额不足" */);
                }
            }
            //校验结算日期是否已日结
            Date maxSettleDate = null;
            if (maxSettleDateMaps.containsKey(bill.getAccentity())) {
                maxSettleDate = maxSettleDateMaps.get(bill.getAccentity());
            } else {
                maxSettleDate = settlementService.getMaxSettleDate(bill.getAccentity());
                maxSettleDateMaps.put(bill.getAccentity(), maxSettleDate);
            }
            if (SettleCheckUtil.checkDailySettlement(maxSettleDate, true)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102191"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806C9","当前日结日期为[%s]，结算日期不能小于等于日结日期！") /* "当前日结日期为[%s]，结算日期不能小于等于日结日期！" */, maxSettleDate));
            }
        }
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建批量支付明细状态查询报文
     * @Date 2019/7/31
     * @Param [param]
     **/
    private CtmJSONObject buildBatchDetailPayStatusMsg(CtmJSONObject param) {
        CtmJSONObject requestHead = buildRequestHead(ITransCodeConstant.QUERY_BATCH_DETAIL_PAY_STATUS,
                param.getString("operator"),
                param.getString("customNo"),
                param.getString("requestseqno"),
                param.getString("signature"));
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("request_seq_no", param.getString("oldRequestSeqNo"));
        //requestBody.put("tran_seq_no", "");
        CtmJSONObject batchDetailPayStatusMsg = new CtmJSONObject();
        batchDetailPayStatusMsg.put("request_head", requestHead);
        batchDetailPayStatusMsg.put("request_body", requestBody);
        return batchDetailPayStatusMsg;
    }

    /*
     *@Author tongyd
     *@Description 根据畅捷支付回推数据更新支付状态
     *@Date 2019/6/13 14:58
     *@Param [recordData]
     *@Return void
     **/
    private void updatePayStatusByRecordData(CtmJSONObject recordData) throws Exception {
        String bankCheckNo = recordData.getString("bank_check_code");
        String payStatusStr = recordData.getString("pay_status");
        PayStatus payStatus = null;
        if ("00".equals(payStatusStr)) {
            payStatus = PayStatus.Success;
        } else if ("01".equals(payStatusStr)) {
            payStatus = PayStatus.Fail;
        } else {
            return;
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("transeqno")
                .in(recordData.getString("tran_seq_no")));
        querySchema.addCondition(queryConditionGroup);
        List<PayBill> bills = MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, querySchema, null);
        if (bills.size() < 1) {
            return;
        }
        PayBill payBill = bills.get(0);
        //重新查询表头表体数据
        payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, payBill.getId(), 3);
        payBill.setPaystatus(payStatus);
        payBill.setBankseqno(recordData.getString("bank_seq_no"));
        boolean flag = false;
        //begin yangjn 2021/03/02  在查询支付状态操作时。若支付失败,如果有银行响应信息需要添加
        StringBuilder payMessage = new StringBuilder();
        String bankCode = recordData.getString("bank_resp_code");
        if (StringUtils.isNotEmpty(bankCode) || "null".equals(bankCode)) {
            payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA","【") /* "【" */);
            payMessage.append(bankCode);
            payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D0","】") /* "】" */);
        }
        String bankDesc = recordData.getString("bank_resp_desc");
        if (StringUtils.isNotEmpty(bankDesc) || "null".equals(bankDesc)) {
            payMessage.append(bankDesc);
        }
        if (payMessage.length() > 0) {
            payBill.setPaymessage(payMessage.toString());
        }
        //end
        if (payStatus == PayStatus.Success) {
            payBill.setBankcheckno(bankCheckNo);
            payBill.setSettlestatus(SettleStatus.alreadySettled);
            //修改原代码逻辑，网银支付成功时，也赋值登账日期，凭证制单日期可配登账日期
            payBill.setDzdate(new Date());
            journalService.updateJournal(payBill);
            payBill.set("_entityName", PayBill.ENTITY_NAME);
            cmpVoucherService.generateVoucherWithPay(payBill);
            SendBizMessageUtils.sendBizMessage(payBill, "cmp_paymentlist", "paysucceed");
        } else if (payStatus == PayStatus.Fail) {
            payBill.setSettlestatus(SettleStatus.noSettlement);
            journalService.updateJournal(payBill);
        }

        payCallback(payBill);
        EntityTool.setUpdateStatus(payBill);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, payBill);
        // 2020/11/16 sungc: 网银支付，由付款申请单生成的付款单，当结算完毕后，同步数据【已支付金额】
        if (payStatus == PayStatus.Success) {
            updatePaymentPullPayApplyBillData(Collections.singletonList(payBill));
            try {
                String systemCode = SystemCodeUtil.getSystemCode(payBill);
                if (payBill.getSrcitem() != null) {
                    //单据来源为应收应付或者来源类型为应收应付的调用结算规则
                    if (payBill.getSrcitem().getValue() == com.yonyoucloud.fi.cmp.cmpentity.EventSource.Manual.getValue() || "fiar".equals(systemCode) || "fiap".equals(systemCode)) {
                        // 调用应收结算规则
                        BillContext billContext = new BillContext();
                        billContext.setBillnum("cmp_payment");
                        Map<String, Object> paramMap = new HashMap<>();
                        paramMap.put("paystatus", payBill.getPaystatus().getValue());
                        paramMap.put("paydate", payBill.getPaydate());
                        paramMap.put("settlemode", payBill.getSettlemode());
                        paramMap.put("enterprisebankaccount", payBill.getEnterprisebankaccount());
                        paramMap.put("cashaccount", payBill.getCashaccount());
                        paramMap.put("srcbillid", payBill.getSrcbillid());
                        paramMap.put("settlestatus", payBill.getSettlestatus().getValue());
                        paramMap.put("settledate", payBill.getSettledate());
                        billContext.setAction("arapSettle");
                        BillBiz.executeRule("arapSettle", billContext, paramMap);
                    }
                }
            } catch (Exception e) {
                log.error("##   #####   单据结算同步应收失败,执行回滚   ##  ######");
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102192"),e.getMessage());
            }
        }
    }

    private void updatePaymentPullPayApplyBillData(List<PayBill> payBills) throws Exception{
        for (PayBill payBill : payBills) {
            log.error("offLinePay pay bill back write Pay Apply Bill, 1. tenant_id = {},payStatus = {},billType = {}, srcItem = {},srcFlag = {}, payBill = {}",
                    InvocationInfoProxy.getTenantid(),
                    payBill.getPaystatus().getValue(),
                    payBill.getBilltype().getValue(),
                    payBill.getSrcitem().getValue(),
                    payBill.getSrcflag(),
                    payBill);
            boolean isPaySuccess = payBill.getPaystatus().getValue() == PayStatus.OfflinePay.getValue() || payBill.getPaystatus().getValue() == PayStatus.Success.getValue();
            boolean isManual = EventSource.Manual.getValue() == payBill.getSrcitem().getValue() && CMP_PAY_APPLICATION.equals(payBill.getSrcflag());
            if (isPaySuccess && (EventType.PayApplyBill.getValue() == payBill.getBilltype().getValue()
                    || isManual)) {
                QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
                querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name(MAINID).eq(payBill.getId())));
                try {
                    List<Map<String, Object>> mapList = MetaDaoHelper.query(PayBillb.ENTITY_NAME, querySchemaJ);
                    for (Map<String, Object> e : mapList) {
                        Long id = Long.valueOf(e.get(SRCBILLITEMID).toString());
                        BigDecimal oriSum = (BigDecimal) e.get(ORISUM);
                        log.error("offLinePay pay bill back write Pay Apply Bill, 2. tenant_id = {}, payBillId = {}, payBillBId = {}, src_bill_item_id = {}",
                                InvocationInfoProxy.getTenantid(), payBill.getId(), e.get("id"), e.get(SRCBILLITEMID));
                        // 更新子表已付金额和未付金额
                        PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, id);
                        log.error("offLinePay pay bill back write Pay Apply Bill, 3. tenant_id = {}, payApplicationBill_b = {}",
                                InvocationInfoProxy.getTenantid(), payApplicationBill_b);
                        if (null != payApplicationBill_b) {
                            if (null == payApplicationBill_b.getPaidAmount()) {
                                payApplicationBill_b.setPaidAmount(oriSum);
                            } else {
                                payApplicationBill_b.setPaidAmount(BigDecimalUtils.safeAdd(payApplicationBill_b.getPaidAmount(), oriSum));
                            }
                            payApplicationBill_b.setUnpaidAmount(BigDecimalUtils.safeSubtract(payApplicationBill_b.getUnpaidAmount(), oriSum));
                            EntityTool.setUpdateStatus(payApplicationBill_b);
                            MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, payApplicationBill_b);
                            // 更新主表已付金额总数和未付金额总数
                            Long mainid = payApplicationBill_b.getMainid();
                            PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, mainid);
                            if (null == payApplicationBill.getPaidAmountSum()) {
                                payApplicationBill.setPaidAmountSum(oriSum);
                            } else {
                                payApplicationBill.setPaidAmountSum(BigDecimalUtils.safeAdd(payApplicationBill.getPaidAmountSum(), oriSum));
                            }
                            payApplicationBill.setUnpaidAmountSum(BigDecimalUtils.safeSubtract(payApplicationBill.getUnpaidAmountSum(), oriSum));
                            EntityTool.setUpdateStatus(payApplicationBill);
                            if (payApplicationBill.getPaidAmountSum().equals(payApplicationBill.getPaymentApplyAmountSum())) {
                                payApplicationBill.setPayBillStatus(PayBillStatus.PaymentCompleted);
                            } else {
                                payApplicationBill.setPayBillStatus(PayBillStatus.PartialPayment);
                            }
                            MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBill);
                        }
                    }
                } catch (Exception e) {
                    log.error("offLinePay pay bill back write Pay Apply Bill fail! 4. tenant_id = {}, errorMsg={} ",
                            InvocationInfoProxy.getTenantid(), e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102179"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C2", "回写付款申请工作台金额失败！") /* "回写付款申请工作台金额失败！" */);
                }
            }
        }
    }

    /**
     * 支付回调外部系统
     *
     * @param payBill
     */
    private void payCallback(PayBill payBill) throws Exception {
        if (payBill.getCallback() == null) {
            return;
        }
        if (ValueUtils.isNotEmpty(payBill.getSrcflag()) && "Sifang".equals(payBill.getSrcflag())) {
            if (payBill.getPaystatus() == PayStatus.Success || payBill.getPaystatus() == PayStatus.Paying) {
                CtmJSONObject params = new CtmJSONObject();
                params.put("id", payBill.getId() + "");
                params.put("srcbillid", payBill.getSrcbillid());
                params.put("paystatus", String.valueOf(payBill.getPaystatus().getValue()));
                if (payBill.getPaydate() != null) {
                    String payTime = DateUtils.dateFormat(payBill.getPaydate(), "yyyy-MM-dd HH:mm:ss");
                    params.put("paytime", payTime);
                }
                String url = ReceiptDetail_ADDR_REQSEQ;         //也要改
                if (payBill.getTranseqno() != null) {    // 修改银行交易流水号为请求流水号 先不改
                    url = url + "?bankseq=" + payBill.getTranseqno() + "&bankid=" + payBill.getEnterprisebankaccount();
                }
                params.put("url", url);
                SifangHttpUtils.SifangCallBack(payBill.getCallback(), params);
            }
        } else {
            //回调外部系统（例：费用）
            CtmJSONObject paramsCallback = new CtmJSONObject();
            Long id = payBill.getId();
            payBill.setId(payBill.getSrcbillid());
            paramsCallback.put("pk", payBill.getSrcbillid());
            paramsCallback.put("paystatus", String.valueOf(payBill.getPaystatus().getValue()));
            paramsCallback.put("headvo",CtmJSONObject.toJSONString(payBill));
            String callbackUrl = payBill.getCallback() + "&token=" + InvocationInfoProxy.getYhtAccessToken();
            String responseStr = HttpTookit.doPostWithJson(callbackUrl, CtmJSONObject.toJSONString(paramsCallback), null);
            payBill.setId(id);
            CtmJSONObject result = CtmJSONObject.parseObject(responseStr);
            if (!"0".equals(result.getString("code"))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102193"),ValueUtils.isNotEmptyObj(result.getString("msg"))
                        ? result.getString("msg") : result.getString("message"));
            }
        }
    }

    /**
     * @return java.util.List<com.yonyoucloud.fi.arap.paybill.PayBill>
     * @Author tongyd
     * @Description 根据ID获取付款单
     * @Date 2019/7/24
     * @Param [ids]
     **/
    private List<PayBill> getPayBillById(List<Object> ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        querySchema.addCondition(queryConditionGroup);

      /*  QueryConditionGroup detailConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        detailConditionGroup.appendCondition(QueryCondition.name("mainid").in(ids));
        QuerySchema detailSchema = QuerySchema.create().name("PayBillb").addSelect("*").addCondition(detailConditionGroup);

        querySchema.addCompositionSchema(detailSchema);*/
        return MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, querySchema, null);
    }

    /*
     *@Author tongyd
     *@Description 更新支付单的网银支付结果
     *@Date 2019/5/14 20:55
     *@Param [responseMsg, payBills]
     *@Return void
     **/
    private void updateConfirmPayResult(CtmJSONObject responseMsg, List<PayBill> payBills) throws Exception {
        StringBuilder payMessage = new StringBuilder();
        PayStatus payStatus = null;
        CtmJSONObject responseHead = responseMsg.getJSONObject("response_head");
        String serviceStatus = responseHead.getString("service_status");
        if (("00").equals(serviceStatus)) {
            payStatus = PayStatus.Paying;
            CtmJSONObject responseBody = responseMsg.getJSONObject("response_body");
            if ("01".equals(responseBody.getString("pay_status"))) {
                payStatus = PayStatus.Fail;
            }
            String bankCode = responseBody.getString("bank_resp_code");
            if (StringUtils.isNotEmpty(bankCode) || "null".equals(bankCode)) {
                payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA","【") /* "【" */);
                payMessage.append(bankCode);
                payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D0","】") /* "】" */);
            }
            String bankDesc = responseBody.getString("bank_resp_desc");
            if (StringUtils.isNotEmpty(bankDesc) || "null".equals(bankDesc)) {
                payMessage.append(bankDesc);
            }
        } /*else if(("05").equals(serviceStatus)){//对于同一张预下单，第二次以后的支付网银返回明确标识码
            return ;
        }*/ else {
            //无论支付是否成功都改为 支付中 需要客户自己拉取支付状态并确认 防止重复支付
            payStatus = PayStatus.Paying;
            String respCode = responseHead.getString("service_resp_code");
            if (StringUtils.isNotEmpty(respCode) || "null".equals(respCode)) {
                payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA","【") /* "【" */);
                payMessage.append(respCode);
                payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D0","】") /* "】" */);
            }
            String respDesc = responseHead.getString("service_resp_desc");
            if (StringUtils.isNotEmpty(respDesc) || "null".equals(respDesc)) {
                payMessage.append(respDesc);
            }
        }
        long userId = AppContext.getCurrentUser().getId();
        for (PayBill payBill : payBills) {
            payBill.setPayman(userId);
            payBill.setPaydate(new Date());
            payBill.setPaystatus(payStatus);
            payBill.setPaymessage(payMessage.toString());
            payCallback(payBill);                               //更新支付状态---四方修改
        }
        EntityTool.setUpdateStatus(payBills);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, payBills);
    }

    /*
     *@Author tongyd
     *@Description 构建预下单交易确认报文
     *@Date 2019/5/14 16:48
     *@Param [params]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    private CtmJSONObject buildConfirmPlaceOrderMsg(CtmJSONObject params, List<PayBill> payBills) throws Exception {
        Map<String, Object> payBankAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(params.getString("enterpriseBankAccount"));
        String customNoAndCheckByBankAccountId = bankAccountSettingService.getCustomNoAndCheckByBankAccountId(params.getString("enterpriseBankAccount"), params.getString("customNo"));
        CtmJSONObject requestHead = buildRequestHead(ITransCodeConstant.PRE_ORDER_TRANSACTION_CONFIRM,
                params.getString("operator"),
                params.getString("customNo"),
                params.getString("requestseqno"),
                params.getString("signature"));
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("porder_id", params.getString("porderId"));
        requestBody.put("acct_name", payBankAccount.get("acctName"));
        requestBody.put("acct_no", payBankAccount.get("account"));
        requestBody.put("tran_tot_num", payBills.size());
        requestBody.put("tran_tot_amt", getTotalPayMoney(payBills).setScale(2, BigDecimal.ROUND_HALF_UP));
        CtmJSONObject confirmPlaceOrderMsg = new CtmJSONObject();
        confirmPlaceOrderMsg.put("request_head", requestHead);
        confirmPlaceOrderMsg.put("request_body", requestBody);
        return confirmPlaceOrderMsg;
    }

    /*
     *@Author tongyd
     *@Description 根据批次支付编码查询支付单
     *@Date 2019/5/14 16:00
     *@Param [porderId]
     *@Return java.util.List<com.yonyoucloud.fi.arap.paybill.PayBill>
     **/
    private List<PayBill> getPayBillByPorderId(String porderId, Long id) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("porderid").eq(porderId),
                QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, schema, null);
    }

    /**
     * @return void
     * @Author tongyd
     * @Description 更新单据预下单结果
     * @Date 2019/8/8
     * @Param [responseMsg, payBills]
     **/
    private void updatePlaceOrderResult(CtmJSONObject responseMsg, List<PayBill> payBills) throws Exception {
        StringBuilder payMessage = new StringBuilder();
        PayStatus payStatus = null;
        String porderId = null;
        CtmJSONObject responseHead = responseMsg.getJSONObject("response_head");
        String serviceStatus = responseHead.getString("service_status");
        if (("00").equals(serviceStatus)) {
            CtmJSONObject responseBody = responseMsg.getJSONObject("response_body");
            String porderStatus = responseBody.getString("porder_status");
            if ("00".equals(porderStatus)) {
                payStatus = PayStatus.PreSuccess;
            } else {
                payStatus = PayStatus.PreFail;
            }
            porderId = responseBody.getString("porder_id");
            String riskCode = responseBody.getString("risk_resp_code");
            if (StringUtils.isNotEmpty(riskCode) || "null".equals(riskCode)) {
                payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA","【") /* "【" */);
                payMessage.append(riskCode);
                payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D0","】") /* "】" */);
            }
            String riskData = responseBody.getString("risk_resp_data");
            if (StringUtils.isNotEmpty(riskData) || "null".equals(riskData)) {
                payMessage.append(riskData);
            }
        } else {
            payStatus = PayStatus.PreFail;
            String respCode = responseHead.getString("service_resp_code");
            if (StringUtils.isNotEmpty(respCode) || "null".equals(respCode)) {
                payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA","【") /* "【" */);
                payMessage.append(respCode);
                payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806D0","】") /* "】" */);
            }
            String respDesc = responseHead.getString("service_resp_desc");
            if (StringUtils.isNotEmpty(respDesc) || "null".equals(respDesc)) {
                payMessage.append(respDesc);
            }
        }

        for (PayBill payBill : payBills) {
            payBill.setPorderid(porderId);
            payBill.setPaymessage(payMessage.toString());
            payBill.setPaystatus(payStatus);
            //payBill.setSettlestatus(SettleStatus.alreadySettled);
            //journalService.updateJournal(payBill);
        }
        EntityTool.setUpdateStatus(payBills);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, payBills);
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建请求报文头
     * @Date 2019/8/19
     * @Param [transCode, customNo, requestseqno, signature]
     **/
    private CtmJSONObject buildRequestHead(String transCode, String operator, String customNo, String requestseqno, String signature) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", bankConnectionAdapterContext.getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", operator);
        requestHead.put("oper_sign", signature);
        requestHead.put("tran_code", transCode);
        requestHead.put("Yonsuite_AutoTask", "Y");
        return requestHead;
    }

    /**
     * @return java.lang.String
     * @Author tongyd
     * @Description 构建交易流水号
     * @Date 2019/8/19
     * @Param [customNo]
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
     *@Author tongyd
     *@Description 构建批量预下单的报文
     *@Date 2019/5/7 14:09
     *@Param [placeOrderData,payBills]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    private CtmJSONObject buildPlaceOrderMsg(CtmJSONObject placeOrderData, List<PayBill> payBills) throws Exception {
        EnterpriseBankAcctVO payAccount = baseRefRpcService.queryEnterpriseBankAccountById(placeOrderData.getString("enterpriseBankAccount"));
        if (!getOpenFlag(placeOrderData.getString("enterpriseBankAccount"))) {
            throw new CtmException(
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CA","【") /* "【" */ + payAccount.getName() + MessageUtils.getMessage(
                            "P_YS_FI_CM_0000026026") /* "】的企业银行账户没有开通银企联，无法进行网银预下单" */);
        }
        String requestseqno = placeOrderData.getString("requestseqno");   // TODO 构建请求流水号
        CtmJSONObject requestHead = buildRequestHead(ITransCodeConstant.BATCH_PLACE_ORDER,
                placeOrderData.getString("operator"),
                placeOrderData.getString("customNo"),
                requestseqno,
                placeOrderData.getString("signature"));
        CtmJSONObject requestBody = new CtmJSONObject();
        String batNo = UUID.randomUUID().toString().replaceAll("-", "");
        requestBody.put("bat_no", batNo); // 批次号
        requestBody.put("acct_no", payAccount.getAccount());
        requestBody.put("acct_name", payAccount.getAcctName());
        requestBody.put("tot_num", payBills.size());
        requestBody.put("tran_amt", getTotalPayMoney(payBills).setScale(2, BigDecimal.ROUND_HALF_UP));
        List<Map<String, Object>> currency = QueryBaseDocUtils.queryCurrencyById(placeOrderData.get("currency"));
        if (currency.size() > 0) {
            if ("RMB".equals(currency.get(0).get("code"))) {
                requestBody.put("curr_code", "CNY");
            } else {
                requestBody.put("curr_code", currency.get(0).get("code"));
            }
        }
        requestBody.put("send_url", AppContext.getEnvConfig("servername")
                + "/authapi/payment/updateBatchPayStatus");
        requestBody.put("porder_validate", "8");
        requestBody.put("other_bank_flag", placeOrderData.getString("otherBankFlag"));
        String backParam = "yht_tenantid:" + InvocationInfoProxy.getTenantid()
                + ",yht_userid:" + AppContext.getCurrentUser().getYhtUserId();
        CtmJSONArray record = new CtmJSONArray();
        for (PayBill payBill : payBills) {
            CtmJSONObject billRecord = new CtmJSONObject();
            //设置批次号
            payBill.setBatno(batNo);
            //设置请求流水号
            payBill.setRequestseqno(requestseqno);
            //设置交易流水号
            String tranSeqNo = buildTranSeqNo(placeOrderData.getString("customNo"));
            payBill.setTranseqno(tranSeqNo);
            billRecord.put("tran_seq_no", tranSeqNo);
            Map<String, Object> recAccInfo = getRecAccInfo(payBill, payAccount);
            billRecord.put("to_bank_type", recAccInfo.get("to_bank_type"));//跨行标识
            billRecord.put("to_acct_no", recAccInfo.get("to_acct_no"));//收方账号
            billRecord.put("to_acct_name", recAccInfo.get("to_acct_name"));//收方户名
            billRecord.put("to_bank_no", recAccInfo.get("to_bank_no"));//收方开户行联行号   跨行必输 对私账户且为银联卡此字段可为空
            billRecord.put("to_brch_no", recAccInfo.get("to_brch_no"));//收方开户行号 跨行必输，与 to_bank_no 字段相同。对私账户且为银联卡此字段可为空
            billRecord.put("to_brch_name", recAccInfo.get("to_brch_name"));//收方开户行名  通过开户行行号获取收方开户行行名 跨行必输 对私账户且为银联卡此字段可为空
            billRecord.put("to_acct_type", recAccInfo.get("to_acct_type")); //收方类型   01：对私；02：对公；03：银行内部户
            billRecord.put("to_addr", "");
            billRecord.put("to_city_code", "");
            billRecord.put("user_name", "");//用户说明
            billRecord.put("tran_amt", payBill.getOriSum().setScale(2, BigDecimal.ROUND_HALF_UP));
            billRecord.put("use_desc", payBill.getDescription());//用途字段，取付款单的备注字段
            billRecord.put("remark", "");//附言
            billRecord.put("phone_no", "");
            billRecord.put("sms_string", "");
            billRecord.put("email", "");
            billRecord.put("mail_desc", "");
            billRecord.put("back_para", backParam);
            record.add(billRecord);
        }
        requestBody.put("record", record);
        CtmJSONObject placeOrderMsg = new CtmJSONObject();
        placeOrderMsg.put("request_head", requestHead);
        placeOrderMsg.put("request_body", requestBody);
        return placeOrderMsg;
    }

    /*
     *@Author tongyd
     *@Description 获取企业银行账户是否开通银企联
     *@Date 2019/6/4 11:29
     *@Param [enterpriseBankAccount]
     *@Return boolean
     **/
    private boolean getOpenFlag(Object enterpriseBankAccount) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("openFlag,empower");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(
                QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        schema.addCondition(conditionGroup);
        Map<String, Object> setting = MetaDaoHelper.queryOne(BankAccountSetting.ENTITY_NAME, schema);
        if ((setting != null && setting.size() > 0) && EmpowerConstand.EMPOWER_QUERYANDPAY.equals(setting.get("empower"))) {
            return (boolean) setting.get("openFlag");
        }
        return false;
    }

    /*
     * @Author tongyd
     * @Description 构建请求流水号
     * @Date 2019/9/12
     * @Param [customNo]
     * @return java.lang.String
     **/
    @Override
    public String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

    /*
     *@Author tongyd
     *@Description 获取付款总金额
     *@Date 2019/5/8 13:18
     *@Param [payBills]
     *@Return java.math.BigDecimal
     **/
    private BigDecimal getTotalPayMoney(List<PayBill> payBills) {
        BigDecimal totalMoney = BigDecimal.ZERO;
        for (PayBill payBill : payBills) {
            totalMoney = totalMoney.add(payBill.getOriSum());
        }
        return totalMoney;
    }

    /**
     * 四方查询定时支付状态
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryBatchDetailPayStatusBySifang(CtmJSONArray params) throws Exception {
        StringBuilder message = new StringBuilder();
        Set<String> msg = new HashSet<>();
        for (int i = 0; i < params.size(); i++) {
            CtmJSONObject param = params.getJSONObject(i);
            if (param.get("isBatchPay") == null || (boolean) param.get("isBatchPay") == false) {//若无值或为false 走单笔
                log.error("==================定时拉取支付状态：单笔开始========");
                //定时任务不需要返回值 只需写日志就可以了
                CtmJSONArray array = new CtmJSONArray();
                array.add(param);
                this.querySingleDetailPayStatus(array);
                log.error("==================定时拉取支付状态：单笔结束========");
            } else {
                CtmJSONObject batchDetailPayStatusMsg = buildBatchDetailPayStatusMsgBySifang(param);
                String signMsg = bankConnectionAdapterContext.chanPaySignMessage(batchDetailPayStatusMsg.toString());
                List<BasicNameValuePair> requestData = new ArrayList<>();
                requestData.add(new BasicNameValuePair("reqData", batchDetailPayStatusMsg.toString()));
                requestData.add(new BasicNameValuePair("reqSignData", signMsg));
                log.error("==================定时拉取支付状态参数========:" + batchDetailPayStatusMsg.toString());
                CtmJSONObject result = HttpsUtils.doHttpsPost(ITransCodeConstant.QUERY_BATCH_DETAIL_PAY_STATUS, requestData, bankConnectionAdapterContext.getChanPayUri());
                log.error("==================定时拉取支付状态结果========:" + result.toString());
                message.append(result.getString("message"));
                msg.add(result.getString("message"));
                if (result.getInteger("code") == 1) {
                    CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                    String serviceStatus = responseHead.getString("service_status");
                    if (("00").equals(serviceStatus)) {
                        CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                        int totalNum = responseBody.getInteger("tot_num");
                        if (totalNum == 1) {
                            log.error("========================支付结果：" + responseBody.getJSONObject("record").getString("pay_status"));
                            updatePayStatusByRecordData(responseBody.getJSONObject("record"));
                        } else if (totalNum > 1) {
                            CtmJSONArray record = responseBody.getJSONArray("record");
                            for (int j = 0; j < record.size(); j++) {
                                updatePayStatusByRecordData(record.getJSONObject(j));
                            }
                        }
                    } else {
                        String serviceRespCode = responseHead.getString("service_resp_code");
                        String serviceRespDesc = responseHead.getString("service_resp_desc");
                        message.append(serviceRespCode);
                        message.append(serviceRespDesc);
                        msg.add(serviceRespCode + "|" + serviceRespDesc);
                    }
                }
            }
        }
        CtmJSONObject responseData = new CtmJSONObject();
        if (msg.size() > 0) {
            responseData.put("message", msg.iterator().next());
        }
        return responseData;
    }

    private CtmJSONObject buildBatchDetailPayStatusMsgBySifang(CtmJSONObject param) {
        CtmJSONObject requestHead = buildRequestHead(ITransCodeConstant.QUERY_BATCH_DETAIL_PAY_STATUS,
                param.getString("operator"),
                param.getString("customNo"),
                buildRequestSeqNo(param.getString("customNo")),
                param.getString("signature"));

        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("request_seq_no", param.getString("requestseqno"));
        //requestBody.put("tran_seq_no", "");
        CtmJSONObject batchDetailPayStatusMsg = new CtmJSONObject();
        batchDetailPayStatusMsg.put("request_head", requestHead);
        batchDetailPayStatusMsg.put("request_body", requestBody);
        return batchDetailPayStatusMsg;
    }

    /**
     * 租户下有多个客户，每个客户有自己的客户号；调用银企联根据不同客户号做区分
     *
     * @param bankAccountSettinges
     * @return
     */
    @Override
    public Map<String, List<Map<String, Object>>> customList(List<Map<String, Object>> bankAccountSettinges) {
        Map<String, List<Map<String, Object>>> resultMap = new HashMap<>();
        int num = bankAccountSettinges.size();
        for (int i = 0; i < num; i++) {
            Map<String, Object> bankAccountSetting = bankAccountSettinges.get(i);
            if (null == bankAccountSetting.get("bankId") || bankAccountSetting.get("bankId").toString().equals("")) {// 判断银行账户是否存在
                continue;
            } else {
                Integer enable = (Integer) bankAccountSetting.get("enable");
                if (1 != enable) {
                    continue;
                }
                String customNo = bankAccountSetting.get("customNo").toString();
                if (resultMap.containsKey(customNo)) {
                    resultMap.get(customNo).add(bankAccountSetting);
                } else {
                    List<Map<String, Object>> newBankAccountSetting = new ArrayList<>();
                    newBankAccountSetting.add(bankAccountSetting);
                    resultMap.put(customNo, newBankAccountSetting);
                }
            }
        }
        return resultMap;
    }

    @Override
    public CtmJSONObject internetBankPlaceOrderSingle(CtmJSONObject params, List<PayBill> payBills) throws Exception {
        CtmJSONArray rows = params.getJSONArray("rows");
        List<Object> payBillIds = params.getObject("ids", List.class);
        CtmJSONObject placeOrderMsg = buildPlaceOrderMsgSingle(params, payBills.get(0));
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderMsg.toString());
        List<BasicNameValuePair> requestData = new ArrayList<>();
        log.error("单笔支付预下单：" + placeOrderMsg.toString());
        requestData.add(new BasicNameValuePair("reqData", placeOrderMsg.toString()));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = HttpsUtils.doHttpsPost(ITransCodeConstant.SINGLE_PLACE_ORDER, requestData, bankConnectionAdapterContext.getChanPayUri());
        log.error("单笔支付预下单结果：" + result.toString());
        CtmJSONObject logData = new CtmJSONObject();
        logData.put(IMsgConstant.BILL_DATA, payBills.get(0));
        logData.put(IMsgConstant.BANK_PLACEORDER_REQUEST, placeOrderMsg);
        logData.put(IMsgConstant.BANK_PLACEORDER_RESPONSE, result);
        ctmcmpBusinessLogService.saveBusinessLog(logData, payBills.get(0).getCode(), "", IServicecodeConstant.PAYMENTBILL, IMsgConstant.PAYMENT_BILL, IMsgConstant.BANK_PREORDER);
        CtmJSONObject responseFailedData = getResponseFailedData(result);
        if (responseFailedData != null) {
            return responseFailedData;
        }
        //应答报文这里与文档不符 先不修改 看测试结果
        updatePlaceOrderResult(result.getJSONObject("data"), payBills);
        params.put("dealSucceed", true);
        params.put("rows", updatePlaceOrderRows(payBillIds, rows));
        return params;
    }

    /**
     * 构建单笔网银预下单的报文
     *
     * @param placeOrderData
     * @param payBill
     * @return
     * @throws Exception
     */
    private CtmJSONObject buildPlaceOrderMsgSingle(CtmJSONObject placeOrderData, PayBill payBill) throws Exception {
        EnterpriseBankAcctVO payAccount = baseRefRpcService.queryEnterpriseBankAccountById(placeOrderData.getString("enterpriseBankAccount"));
        if (!getOpenFlag(placeOrderData.getString("enterpriseBankAccount"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101723"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F44AA05B0000E", "银行账户【%s】没有直联支付权限，网银预下单失败，请前往【银企联账号】检查银行账户直联权限。") /* "银行账户【%s】没有直联支付权限，网银预下单失败，请前往【银企联账号】检查银行账户直联权限。" */, payAccount.getName()));
        }
        String customNoAndCheckByBankAccountId = bankAccountSettingService.getCustomNoAndCheckByBankAccountId(placeOrderData.getString("enterpriseBankAccount"), placeOrderData.getString("customNo"));
        String requestseqno = placeOrderData.getString("requestseqno");   // TODO 构建请求流水号
        //构建请求头 (银企连请求通用)
        CtmJSONObject requestHead = buildRequestHead(ITransCodeConstant.SINGLE_PLACE_ORDER,
                placeOrderData.getString("operator"),
                placeOrderData.getString("customNo"),
                requestseqno,
                placeOrderData.getString("signature"));
        //批次号
        String batNo = UUID.randomUUID().toString().replaceAll("-", "");
        payBill.setBatno(batNo);
        //设置请求流水号
        payBill.setRequestseqno(requestseqno);

        //构建单笔预下单请求体
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("bat_no", batNo); // 批次号
        requestBody.put("acct_no", payAccount.getAccount());
        requestBody.put("acct_name", payAccount.getAcctName());
        //设置交易流水号
        String tranSeqNo = buildTranSeqNo(placeOrderData.getString("customNo"));
        payBill.setTranseqno(tranSeqNo);
        requestBody.put("tran_seq_no", tranSeqNo);
        Map<String, Object> recAccInfo = getRecAccInfo(payBill, payAccount);
        requestBody.put("to_bank_type", recAccInfo.get("to_bank_type"));
        requestBody.put("to_acct_no", recAccInfo.get("to_acct_no"));
        requestBody.put("to_acct_name", recAccInfo.get("to_acct_name"));
        requestBody.put("to_bank_no", recAccInfo.get("to_bank_no"));
        requestBody.put("to_brch_no", recAccInfo.get("to_brch_no"));
        requestBody.put("to_brch_name", recAccInfo.get("to_brch_name"));
        requestBody.put("to_acct_type", recAccInfo.get("to_acct_type"));
        requestBody.put("to_addr", "");
        requestBody.put("to_city_code", "");
        List<Map<String, Object>> currency = QueryBaseDocUtils.queryCurrencyById(placeOrderData.get("currency"));
        if (currency.size() > 0) {
            if ("RMB".equals(currency.get(0).get("code"))) {
                requestBody.put("curr_code", "CNY");
            } else {
                requestBody.put("curr_code", currency.get(0).get("code"));
            }
        }
        requestBody.put("tran_amt", payBill.getOriSum().setScale(2, BigDecimal.ROUND_HALF_UP));
        requestBody.put("use_desc", payBill.getDescription());
        requestBody.put("remark", payBill.getDescription());
        requestBody.put("phone_no", "");
        requestBody.put("sms_string", "");
        requestBody.put("email", "");
        requestBody.put("mail_desc", "");
        String backParam = "yht_tenantid:" + InvocationInfoProxy.getTenantid()
                + ",yht_userid:" + AppContext.getCurrentUser().getYhtUserId();
        requestBody.put("back_para", backParam);
        requestBody.put("back_url", AppContext.getEnvConfig("servername") + "/authapi/payment/updateBatchPayStatus");
        requestBody.put("porder_validate", "8");
//        other_bank_flag  不知道是什么文档里没有 这里不拼接

        CtmJSONObject placeOrderMsg = new CtmJSONObject();
        placeOrderMsg.put("request_head", requestHead);
        placeOrderMsg.put("request_body", requestBody);
        return placeOrderMsg;
    }

    @Override
    public CtmJSONObject querySingleDetailPayStatus(CtmJSONArray params) throws Exception {
        Set<String> msg = new HashSet<>();
        for (int i = 0; i < params.size(); i++) {
            CtmJSONObject param = params.getJSONObject(i);
            CtmJSONObject singleDetailPayStatusMsg = buildSingleDetailPayStatusMsg(param);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(singleDetailPayStatusMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", singleDetailPayStatusMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            log.error("单笔支付状态查询请求:" + singleDetailPayStatusMsg.toString());
            CtmJSONObject result = HttpsUtils.doHttpsPost(ITransCodeConstant.QUERY_SINGLE_DETAIL_PAY_STATUS, requestData, bankConnectionAdapterContext.getChanPayUri());
            log.error("单笔支付状态查询返回:" + result.toString());
            msg.add(result.getString("message"));
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String serviceStatus = responseHead.getString("service_status");
                if (("00").equals(serviceStatus)) {
                    CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                    log.error("========================单笔支付结果：" + responseBody.getString("pay_status"));
                    //这里用交易流水号 确认唯一单据 如果没有oldRequestSeqNo 说明走的是定时任务 这时取requestseqno即可
                    String oldRequestSeqNo = param.getString("oldRequestSeqNo") != null ? param.getString("oldRequestSeqNo") : param.getString("requestseqno");
                    CtmJSONObject row = new CtmJSONObject();
                    if (param.get("isTiming") != null && (boolean) param.get("isTiming")) {
                        row = null;
                    } else {
                        row = param.getJSONArray("rows").getJSONObject(0);
                    }
                    updatePayStatusByRecordDataSingle(responseBody, oldRequestSeqNo, row);
                } else {
                    String serviceRespCode = responseHead.getString("service_resp_code");
                    String serviceRespDesc = responseHead.getString("service_resp_desc");
                    //如果serviceStatus不为00 说明本交易银行没有处理成功
                    //而上面result.getString("message")拼入的是此次请求的成功与否 会与后面拼接的信息产生歧义 故如果返回了银行交易信息这里现将msg clear
                    if (serviceRespCode != null || serviceRespDesc != null) {
                        msg.clear();
                        msg.add(serviceRespCode + "|" + serviceRespDesc);
                    }
                }
            }
        }
        CtmJSONObject responseData = new CtmJSONObject();

        responseData.put("message", msg.iterator().next());
        CtmJSONArray responseRows = new CtmJSONArray();
        for (int i = 0; i < params.size(); i++) {
            CtmJSONArray rows = params.getJSONObject(i).getJSONArray("rows");
            //定时任务 这里无法获取rows 需要判断
            if (rows != null) {
                for (int j = 0; j < rows.size(); j++) {
                    CtmJSONObject row = rows.getJSONObject(j);
                    PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, row.getLong("id"));
                    row.put("paystatus", payBill.getPaystatus().getValue());
                    row.put("bankseqno", payBill.getBankseqno());
                    row.put("pubts", payBill.getPubts());
                    responseRows.add(row);
                }
            }
        }
        responseData.put("rows", responseRows);
        responseData.put("rowNum", params.getJSONObject(0).get("rowNum"));
        return responseData;
    }

    /**
     * 构建单笔支付明细状态查询报文
     *
     * @param param
     * @return
     */
    private CtmJSONObject buildSingleDetailPayStatusMsg(CtmJSONObject param) {
        CtmJSONObject requestHead = new CtmJSONObject();
        CtmJSONObject requestBody = new CtmJSONObject();
        //这里判断是否是定时任务
        if (param.get("isTiming") != null && (boolean) param.get("isTiming") == true) {
            requestHead = buildRequestHead(ITransCodeConstant.QUERY_SINGLE_DETAIL_PAY_STATUS,
                    param.getString("operator"),
                    param.getString("customNo"),
                    buildRequestSeqNo(param.getString("customNo")),
                    param.getString("signature"));
            requestBody.put("tran_seq_no", param.getString("transeqno"));
        } else {
            requestHead = buildRequestHead(ITransCodeConstant.QUERY_SINGLE_DETAIL_PAY_STATUS,
                    param.getString("operator"),
                    param.getString("customNo"),
                    param.getString("requestseqno"),
                    param.getString("signature"));
            CtmJSONObject rowForIsBatchPay = param.getJSONArray("rows").getJSONObject(0);
            requestBody.put("tran_seq_no", rowForIsBatchPay.get("transeqno").toString());
        }
        CtmJSONObject batchDetailPayStatusMsg = new CtmJSONObject();
        batchDetailPayStatusMsg.put("request_head", requestHead);
        batchDetailPayStatusMsg.put("request_body", requestBody);
        return batchDetailPayStatusMsg;
    }


    /**
     * 根据畅捷支付回推数据更新支付状态 单笔
     *
     * @param recordData
     * @throws Exception
     */
    private void updatePayStatusByRecordDataSingle(CtmJSONObject recordData, String oldRequestSeqNo, CtmJSONObject row) throws Exception {
        String payStatusStr = recordData.getString("pay_status");
        PayStatus payStatus = null;
        if ("00".equals(payStatusStr)) {
            payStatus = PayStatus.Success;
        } else if ("01".equals(payStatusStr)) {
            payStatus = PayStatus.Fail;
        } else {
            return;
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("requestseqno").in(oldRequestSeqNo));
        querySchema.addCondition(queryConditionGroup);
        //这里只有主表数据 所以后面要再次查询
        List<PayBill> bills = MetaDaoHelper.queryObject(PayBill.ENTITY_NAME, querySchema, null);
        if (bills.size() < 1) {
            return;
        }
        PayBill payBill = bills.get(0);
        //重新查询表头表体数据
        payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, payBill.getId(), 3);
        //如果当前单据状态为支付成功 则不更新
        if (payBill.getPaystatus() == PayStatus.Success) {
            return;
        }
        payBill.setPaystatus(payStatus);
        payBill.setBankseqno(recordData.getString("bank_seq_no"));
        boolean flag = false;
        //begin yangjn 2021/03/02  在查询支付状态操作时。若支付失败,如果有银行响应信息需要添加
        StringBuilder payMessage = new StringBuilder();
        String bankCode = recordData.getString("bank_resp_code");
        if (StringUtils.isNotEmpty(bankCode) || "null".equals(bankCode)) {
            payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001EC", "【") /* "【" */);
            payMessage.append(bankCode);
            payMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001ED", "】") /* "】" */);
        }
        String bankDesc = recordData.getString("bank_resp_desc");
        if (StringUtils.isNotEmpty(bankDesc) || "null".equals(bankDesc)) {
            payMessage.append(bankDesc);
        }
        if (payMessage.length() > 0) {
            payBill.setPaymessage(payMessage.toString());
        }
        //end
        if (payStatus == PayStatus.Success) {
            payBill.setSettlestatus(SettleStatus.alreadySettled);
            //修改原代码逻辑，网银支付成功时，也赋值登账日期，凭证制单日期可配登账日期
            payBill.setDzdate(new Date());
            journalService.updateJournal(payBill);
            payBill.set("_entityName", PayBill.ENTITY_NAME);
            cmpVoucherService.generateVoucherWithPay(payBill);
            if (null == row) {
                SendBizMessageUtils.sendBizMessage(payBill, "cmp_paymentlist", "paysucceed");
            } else {
                SendBizMessageUtils.sendBizMessage(row, "cmp_paymentlist", "paysucceed");
            }
        } else if (payStatus == PayStatus.Fail) {

            payBill.setSettlestatus(SettleStatus.noSettlement);
            journalService.updateJournal(payBill);
        }

        payCallback(payBill);
        EntityTool.setUpdateStatus(payBill);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, payBill);
        // 2020/11/16 sungc: 网银支付，由付款申请单生成的付款单，当结算完毕后，同步数据【已支付金额】
        if (payStatus == PayStatus.Success) {
            updatePaymentPullPayApplyBillData(Collections.singletonList(payBill));
            try {
                String systemCode = SystemCodeUtil.getSystemCode(payBill);
                if (payBill.getSrcitem() != null) {
                    //单据来源为应收应付或者来源类型为应收应付的调用结算规则
                    if (payBill.getSrcitem().getValue() == com.yonyoucloud.fi.cmp.cmpentity.EventSource.Manual.getValue() || "fiar".equals(systemCode) || "fiap".equals(systemCode)) {
                        // 调用应收结算规则
                        BillContext billContext = new BillContext();
                        billContext.setBillnum("cmp_payment");
                        Map<String, Object> paramMap = new HashMap<>();
                        paramMap.put("paystatus", payBill.getPaystatus().getValue());
                        paramMap.put("paydate", payBill.getPaydate());
                        paramMap.put("settlemode", payBill.getSettlemode());
                        paramMap.put("enterprisebankaccount", payBill.getEnterprisebankaccount());
                        paramMap.put("cashaccount", payBill.getCashaccount());
                        paramMap.put("srcbillid", payBill.getSrcbillid());
                        paramMap.put("settlestatus", payBill.getSettlestatus().getValue());
                        paramMap.put("settledate", payBill.getSettledate());
                        billContext.setAction("arapSettle");
                        BillBiz.executeRule("arapSettle", billContext, paramMap);
                    }
                }
            } catch (Exception e) {
                log.error("##   #####   单据结算同步应收失败,执行回滚   ##  ######");
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102194"),e.getMessage());
            }
        }
    }

    /**
     * 根据币种主键查询对应的编码
     * 如果缓存中存在的话取缓存 没有的话查询
     * @param currencyList
     * @return
     * @throws Exception
     *  * @deprecated (use trace com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService queryByCurrencyList instead)
     */
  @Deprecated
    public HashMap<String, String> queryCurrencyCode(List<BankAcctCurrencyVO> currencyList) throws Exception {
        HashMap<String, String> currencyMap = new HashMap<>();
        List<String> currencyIds = currencyList.stream().map(e -> e.getCurrency()).collect(Collectors.toList());
        List<String> ids = new ArrayList<>();
        for (String currency : currencyIds) {
            String currencyCode = currencyCodeCache.getIfPresent(currency);
            if(StringUtils.isEmpty(currencyCode)){
                ids.add(currency);
            }else{
                currencyMap.put(currency,currencyCode);
            }
        }
        if(ids.size() == 0){
            return currencyMap;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.currencytenant.CurrencyTenantVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC); /* 暂不修改 须在BaseRefRpcService中添加通过参数查询币种的方式*/
        CurrencyBdParams currencyBdParams = new CurrencyBdParams();
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,code");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
//        conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getYTenantId()));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        Map<String, String> map = query.stream().collect(Collectors.toMap(e -> (String)e.get("id"), e -> (String)e.get("code")));
        for (Map.Entry<String,String> entry : map.entrySet()) {
            currencyCodeCache.put(entry.getKey(), entry.getValue());
        }
        currencyMap.putAll(map);
        return currencyMap;
    }

    @Override
    public Map<String, Object> getCurrencyByCode(String currencyCode) throws Exception {
        Object tenantId = AppContext.getTenantId();
        String cacheKey = currencyCode + ":" + tenantId;
        Map<String, Object> result = currencyCache.getIfPresent(cacheKey);
        if (result != null) {
            return result;
        }

        BillContext billContext = new BillContext();
        billContext.setFullname("bd.currencytenant.CurrencyTenantVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC); /* 暂不修改 须在BaseRefRpcService中添加通过参数查询币种的方式*/
        CurrencyBdParams currencyBdParams = new CurrencyBdParams();
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").eq(currencyCode));
//        conditionGroup.appendCondition(QueryCondition.name("tenant").eq(AppContext.getYTenantId()));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (ValueUtils.isEmpty(query)) {
            return null;
        }
        result = query.get(0);
        if (result != null) {
            currencyCache.put(cacheKey, result);
        }
        return result;
    }

    /**
     * 根据银行账户获取币种
     *
     * @param bankAcc
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getCurrencyByAccount(String bankAcc) throws Exception {
        Object tenantId = AppContext.getTenantId();
        String cacheKey = bankAcc + ":" + tenantId;
        Map<String, Object> result = accountCurrencyCache.getIfPresent(cacheKey);
        if (result != null) {
            return result;
        }
        //账户单币种时取单一币种；多币种时取默认币种；若多币种时无默认 则取人民币
        List<Map<String, Object>> currArray = QueryBaseDocUtils.queryBankAcctCurrByBankacc(bankAcc);/* 暂不修改 已登记*/
        if (currArray != null && currArray.size() == 1) {
            result = currArray.get(0);
        } else if (currArray != null && currArray.size() > 1) {
            result = QueryBaseDocUtils.queryBankAcctDefaultCurrByBankacc(bankAcc);/* 暂不修改 已登记*/
        } else if (result == null) {
            result = getCurrencyByCode("CNY");
        }
        if (result != null) {
            accountCurrencyCache.put(cacheKey, result);
        }
        return result;
    }

}
