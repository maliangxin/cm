package com.yonyoucloud.fi.cmp.bankreceipt.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.BlockPolicy;
import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.log.model.BusinessObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.log.util.BusiObjectBuildUtil;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.task.until.TaskParamHandle;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
//@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
@RequiredArgsConstructor
public class TaskBankReceiptServiceImpl implements TaskBankReceiptService {
    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;
    @Autowired
    private BankreconciliationService bankreconciliationService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Resource
    private IApplicationService appService;

    String errorListKey = "errorList";
    String sucessListKey = "sucessList";
    // 流水关联回单是否检查回单文件已下载
    static String receiptRelateCheckFile;

    static {
        receiptRelateCheckFile = AppContext.getEnvConfig("receipt.relate.check.file", "false");
    }

    @Override
    public Map<String, List<EnterpriseBankAcctVO>> bankReceipt(CtmJSONObject param) throws Exception {
        //构建查询银行账户的参数
        CtmJSONObject queryBankAccountVosParams = TaskParamHandle.buildQueryBankAccountVosParams(param);
        queryBankAccountVosParams.put(ICmpConstant.IS_DISPATCH_TASK_CMP, param.get(ICmpConstant.IS_DISPATCH_TASK_CMP));
        //根据参数查询全量账户
        BankDealDetailService bankDealDetailService = CtmAppContext.getBean(BankDealDetailService.class);
        List<EnterpriseBankAcctVO> bankAccounts = EnterpriseBankQueryService.getEnterpriseBankAccountVos(queryBankAccountVosParams);
        //通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
        Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = bankDealDetailService.getBankAcctVOsGroupByTask(bankAccounts);
//        //直联账户
//        List<EnterpriseBankAcctVO> checkSuccess = bankAccountsGroup.get("checkSuccess");
//        //内部账户
//        List<EnterpriseBankAcctVO> innerAccounts = bankAccountsGroup.get("innerAccounts");
        return bankAccountsGroup;
    }

    /**
     * 之前的 bankReceiptAsyncProcess(CtmJSONObject param) 保留了，现在这里是
     *
     * @param param
     * @param bankAccountsGroup
     * @throws Exception
     */
    @Override
    public ThreadResult bankReceiptAsyncProcess(CtmJSONObject param, Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup) throws Exception {

        //直联账户
        List<EnterpriseBankAcctVO> checkSuccess = bankAccountsGroup.get("checkSuccess");
        //内部账户
        List<EnterpriseBankAcctVO> innerAccounts = bankAccountsGroup.get("innerAccounts");

        //构建查询银行账户的参数
        CtmJSONObject queryBankAccountVosParams = TaskParamHandle.buildQueryBankAccountVosParams(param);
        //添加时间参数 获取每天的时间集合
        List<String> getBetweenDate = TaskParamHandle.getBetweenDate(param, null);
        //构建线程池
        ExecutorService executorService = buildThreadPoolForTaskBankReceipt(queryBankAccountVosParams.getInteger("corepoolsize"));
        //拉取直联账户回单
        String logId = param.getString("logId");
        ThreadResult threadResult = excuteHttpQueryTransactionDetail(checkSuccess, getBetweenDate, executorService, logId);
        if (innerAccounts != null && !innerAccounts.isEmpty()) {
            List<String> accountIdList = innerAccounts.stream().map(EnterpriseBankAcctVO::getId).collect(Collectors.toList());
            BankReceiptService bankReceiptService = CtmAppContext.getBean(BankReceiptService.class);
            List<String> accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeys(ICmpConstant.QUERYBANKRECEIPTKEY, innerAccounts);
            try {
                CtmLockTool.executeInOneServiceExclusivelyBatchLock(accountInfoLocks, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        ////加锁失败
                        //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, logId,
                        //        String.format("[%s]:系统正在对此账户拉取中",accountInfoLocks),
                        //        TaskUtils.UPDATE_TASK_LOG_URL);
                        threadResult.getSucessReturnList().add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004CD", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */, accountInfoLocks));
                        return;
                    }
                    //加锁成功
                    bankReceiptService.stctInsertReceiptByinnerAccounts(param, accountIdList, null);
                });
            } catch (Exception e) {
                log.error("拉取银行交易电子回单失败：", e);
                threadResult.getErrorReturnList().add(e.getMessage());
                //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE, logId,
                //        String.format("[%s]:此账户操作发生异常",accountInfoLocks) + "[Failure Reason]" + e.getMessage(),
                //        TaskUtils.UPDATE_TASK_LOG_URL);
            }
        }
        return threadResult;
    }


    ///**
    // * 测试环境没有调用
    // * 之前的bankReceiptAsyncProcess，拆分出了查询账户和拉取来构建线程池
    // *
    // * @param param
    // * @throws Exception
    // */
    //@Override
    //public void bankReceiptAsyncProcess(CtmJSONObject param) throws Exception {
    //    //构建查询银行账户的参数
    //    CtmJSONObject queryBankAccountVosParams = TaskParamHandle.buildQueryBankAccountVosParams(param);
    //    queryBankAccountVosParams.put(ICmpConstant.IS_DISPATCH_TASK_CMP, param.get(ICmpConstant.IS_DISPATCH_TASK_CMP));
    //    BankDealDetailService bankDealDetailService = CtmAppContext.getBean(BankDealDetailService.class);
    //    //根据参数查询全量账户
    //    List<EnterpriseBankAcctVO> bankAccounts = EnterpriseBankQueryService.getEnterpriseBankAccountVos(queryBankAccountVosParams);
    //    //通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
    //    Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = bankDealDetailService.getBankAcctVOsGroupByTask(bankAccounts);
    //    //直联账户
    //    List<EnterpriseBankAcctVO> checkSuccess = bankAccountsGroup.get("checkSuccess");
    //    //添加时间参数 获取每天的时间集合
    //    List<String> getBetweenDate = TaskParamHandle.getBetweenDate(param, true);
    //    //构建线程池
    //    ExecutorService executorService = buildThreadPoolForTaskBankReceipt(queryBankAccountVosParams.getInteger("corepoolsize"));
    //    //拉取直联账户回单
    //    String logId = param.getString("logId");
    //    ThreadResult threadResult = excuteHttpQueryTransactionDetail(checkSuccess, getBetweenDate, executorService, logId);
    //    //内部账户
    //    List<EnterpriseBankAcctVO> innerAccounts = bankAccountsGroup.get("innerAccounts");
    //    if (innerAccounts != null && innerAccounts.size() > 0) {
    //        List<String> accountIdList = innerAccounts.stream().map(EnterpriseBankAcctVO::getId).collect(Collectors.toList());
    //        BankReceiptService bankReceiptService = CtmAppContext.getBean(BankReceiptService.class);
    //        bankReceiptService.stctInsertReceiptByinnerAccounts(param, accountIdList, null);
    //    }
    //}

    private ThreadResult excuteHttpQueryTransactionDetail(List<EnterpriseBankAcctVO> checkSuccess, List<String> getBetweenDate, ExecutorService executorService, String logId) throws Exception {
        //将直连账户和币种、日期拼接
        String channel = bankConnectionAdapterContext.getChanPayCustomChanel();
        List<CtmJSONObject> checkSuccessList = new ArrayList<>();
        BankDealDetailService bankDealDetailService = CtmAppContext.getBean(BankDealDetailService.class);
        for (EnterpriseBankAcctVO evo : checkSuccess) {
            Date enableDate = BankAccountUtil.getEnableDate(evo.getAccount());
            for (BankAcctCurrencyVO currencyVO : evo.getCurrencyList()) {
                for (String date : getBetweenDate) {
                    DateTime parseDate = DateUtil.parseDate(date);
                    //早于启用日期的不拉取
                    if (parseDate.isBefore(enableDate)) {
                        continue;
                    }
                    CtmJSONObject map = new CtmJSONObject();
                    List<BankAcctCurrencyVO> getCurrencyList = new ArrayList<>();
                    getCurrencyList.add(currencyVO);
                    map.put("currencyCode", bankDealDetailService.queryCurrencyCode(getCurrencyList).get(currencyVO.getCurrency()));
                    map.put("accountId", evo.getId());
                    map.put("accEntityId", evo.getOrgid());
                    map.put("banktype", evo.getBank());
                    map.put("acct_name", evo.getAcctName());
                    map.put("acct_no", evo.getAccount());
                    map.put("startDate", date);
                    map.put("endDate", date);
                    map.put("channel", channel);
                    checkSuccessList.add(map);
                }
            }
        }
        if (checkSuccessList.size() == 0) {
            ThreadResult threadResult = new ThreadResult();
            threadResult.getErrorReturnList().add(CtmExceptionConstant.DISPATCH_ACCOUNT_NOT_EXIST);
            return threadResult;
        }
        AtomicInteger atomicInteger = new AtomicInteger();
        String semphareCount = AppContext.getEnvConfig("cmp.bankReceiptTask.seamphore", "60");
        String batchCountStr = AppContext.getEnvConfig("cmp.bankReceiptTask.batchcount", "10");
        int batchcount = Integer.parseInt(batchCountStr);
        Semaphore semaphore = new Semaphore(Integer.parseInt(semphareCount));
        //执行查询直联账户
        //拿返回值，等待所有线程结果返回
        ThreadResult threadResult = ThreadPoolUtil.executeByBatchCollectResults(executorService, checkSuccessList, batchcount, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004CE", "查银行交易回单调度任务") /* "查银行交易回单调度任务" */, semaphore, true, (int fromIndex, int toIndex) -> {
            ThreadResult subThreadResult = new ThreadResult();
            for (int t = fromIndex; t < toIndex; t++) {
                CtmJSONObject httpObj = checkSuccessList.get(t);
                String accountInfo = getAccountInfo(httpObj.get("accountId"),httpObj.get("startDate"), httpObj.get("currencyCode"));
                String accountTipInfo = httpObj.get("acct_no") + "|" + httpObj.get("startDate") + "|" + httpObj.get("endDate") + "|" + httpObj.get("currencyCode");
                try {
                    String lockKey = accountInfo + ICmpConstant.QUERYBANKRECEIPTKEY;
                    CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                        CtmJSONObject paramNow = new CtmJSONObject(httpObj);
                        if (lockstatus == LockStatus.GETLOCK_FAIL) {
                            ////加锁失败
                            //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, logId,
                            //        String.format("[%s]:系统正在对此账户拉取中", accountInfo),
                            //        TaskUtils.UPDATE_TASK_LOG_URL);
                            subThreadResult.getSucessReturnList().add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004CD", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */, accountInfo));
                            return;
                        }
                        //加锁成功
                        String noDataMsg = buildAndQueryTask(paramNow, atomicInteger);
                        if (noDataMsg != null) {
                            String noDataSuccessMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D3", "[%s]:此账户返回[%s]") /* "[%s]:此账户返回[%s]" */, accountTipInfo, noDataMsg);
                            subThreadResult.getSucessReturnList().add(noDataSuccessMessage);
                        }
                        paramNow = null;
                    });
                } catch (Exception e) {
                    log.error("查询直连账户交易回单失败：" + e.getMessage(), e);
                    String errorMessage = YQLUtils.getErrorMsqWithAccount(e, accountTipInfo);
                    subThreadResult.getErrorReturnList().add(errorMessage);
                    //throw new Exception(errorMessage, e);
                    //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE, logId,
                    //        YQLUtils.getErrorMsqWithAccount(e, accountInfo),
                    //        TaskUtils.UPDATE_TASK_LOG_URL);
                }
            }
            //收集线程内的错误信息和成功信息返回
            return subThreadResult;
        });
        return threadResult;
    }

    public static  String getAccountInfo(Object accountId, Object startDate, Object currencyCode) {
        return accountId + "|" + startDate + "|" + currencyCode;
    }

    /**
     * 构建电子回单查询参数 并进行查询
     *
     * @throws Exception
     * @paramparam
     */
    public String buildAndQueryTask(CtmJSONObject param, AtomicInteger atomicInteger) throws Exception {
        String customNo = null;
        CtmJSONObject queryMsg = new CtmJSONObject();
        CtmJSONObject result = new CtmJSONObject();
        try {
            BankReceiptService bankReceiptService = CtmAppContext.getBean(BankReceiptService.class);
            Map<String, Object> accountSeting = bankReceiptService.getBankAccountSetting(param.getString("accountId"));
            if (accountSeting != null && accountSeting.get("customNo") != null) {
                customNo = accountSeting.get("customNo").toString();
            } else {
                return null;
            }
            param.put("customNo", customNo);
            param.put("begNum", 1);
            queryMsg = buildReceiptQueryTask(param);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            int currHttpNum = atomicInteger.getAndIncrement();
            long start = System.currentTimeMillis();
            log.error("======回单拉取请求参数=======>{}.第{}个请求", queryMsg.toString(), currHttpNum);
            result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, requestData, bankConnectionAdapterContext.getChanPayUri());
            log.error("=======回单拉取result======>{}.第{}个请求，耗时{}毫秒", CtmJSONObject.toJSONString(result), currHttpNum, (System.currentTimeMillis() - start));
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String service_resp_code = responseHead.getString("service_resp_code");
                if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL, service_resp_code)) {
                    CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                    Map<String, Object> enterpriseInfo = new HashMap<>();
                    enterpriseInfo.put("accEntityId", param.getString("accEntityId"));
                    enterpriseInfo.put("accountId", param.getString("accountId"));
                    enterpriseInfo.put("customNo", customNo);
                    bankReceiptService.insertReceiptDetail(enterpriseInfo, responseBody, null);
                    String nextPage = responseBody.getString("next_page");
                    while ("1".equals(nextPage)) {
                        int begNum = param.getInteger("begNum");
                        param.put("begNum", begNum + ITransCodeConstant.QUERY_NUMBER_50);
                        param.put("queryExtend", responseBody.get("query_extend"));
                        queryMsg = buildReceiptQueryTask(param);
                        signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
                        requestData = new ArrayList<>();
                        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
                        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
                        log.error("===================>回单拉取请求参数存在下一页begNum:" + param.get("begNum") + queryMsg);
                        result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL, requestData, bankConnectionAdapterContext.getChanPayUri());
                        log.error("===================>回单拉取请求结果存在下一页" + result);
                        if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL, service_resp_code)) {
                            responseBody = result.getJSONObject("data").getJSONObject("response_body");
                            bankReceiptService.insertReceiptDetail(enterpriseInfo, responseBody, null);
                            nextPage = responseBody.getString("next_page");
                            param.put("queryExtend", responseBody.get("query_extend"));
                        } else {
                            nextPage = "0";
                        }
                    }
                    param.put("begNum", 1);
                    param.put("queryExtend", "");
                    param.put("query_extend", "");
                    param.put("nextPage", false);
//                    if ("1".equals(nextPage)) {
//                        int begNum = param.getInteger("begNum")==null?1:param.getInteger("begNum");
//                        param.put("begNum", begNum + responseBody.getInteger("back_num"));
//                        param.put("queryExtend", responseBody.get("query_extend"));
//                        param.put("nextPage", true);
//                        buildAndQueryTask(param);
//                    }else {
//                        param.put("begNum", 1);
//                        param.put("queryExtend","");
//                        param.put("query_extend","");
//                        param.put("nextPage", false);
//                    }
                } else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
                    String noDataMsg = YQLUtils.getYQLNoDataMsq(responseHead);
                    return noDataMsg;
                    //没有数据不报错
                } else {
                    //调度任务时，抛出银企联异常给上层收集后展示
                    throw new CtmException(YQLUtils.getYQLErrorMsq(responseHead));
                }
            } else {
                //调度任务时，抛出银企联请求异常给上层收集后展示
                throw new CtmException(YQLUtils.getYQLErrorMsqOfNetWork(result.getString("message")));
            }
        } catch (Exception e) {
            log.error(String.format("buildAndQueryTask-error，请求参数 = %s,响应参数 = %s,报错信息 = %s", queryMsg, result, e.getMessage()), e);
            throw new Exception(e.getMessage(), e);
        }
        return null;

    }

    private CtmJSONObject buildReceiptQueryTask(CtmJSONObject params) throws Exception {
        String requestseqno = TaskParamHandle.buildRequestSeqNo(params.get("customNo").toString());
        CtmJSONObject requestHead = buildRequloadestHead(ITransCodeConstant.QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL,
                null,
                params.get("customNo").toString(),
                requestseqno,
                params.getString("channel"));
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("acct_name", params.getString("acct_name"));
        requestBody.put("acct_no", params.getString("acct_no"));
        String startDate = params.getString("startDate").replaceAll("-", "").substring(0, 8);
        String endDate = params.getString("endDate").replaceAll("-", "").substring(0, 8);
        requestBody.put("beg_date", startDate);
        requestBody.put("end_date", endDate);
        requestBody.put("tran_status", "00");
        requestBody.put("curr_code", params.get("currencyCode"));
        requestBody.put("beg_num", params.get("begNum") != null ? params.get("begNum") : 1);
        requestBody.put("query_num", ITransCodeConstant.QUERY_NUMBER_50);
        requestBody.put("query_extend", params.get("queryExtend"));
        CtmJSONObject queryMsg = new CtmJSONObject();
        queryMsg.put("request_head", requestHead);
        queryMsg.put("request_body", requestBody);
        return queryMsg;
    }

    private CtmJSONObject buildRequloadestHead(String transCode, String oper, String customNo, String requestseqno, String channel) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", channel);
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper_sign", null);
        requestHead.put("tran_code", transCode);
        requestHead.put("oper", oper);
        return requestHead;
    }

    private ExecutorService buildThreadPoolForTaskBankReceipt(Integer corePoolSize) {
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.bankReceiptTask.thread.param","8,32,1000,cmp-bankReceiptTask-async-");
        String[] threadParamArray = threadParam.split(",");
        if (corePoolSize == null) {
            corePoolSize = Integer.parseInt(threadParamArray[0]);;
        }
        int maxPoolSize = Integer.parseInt(threadParamArray[1]);
        int queueSize = Integer.parseInt(threadParamArray[2]);
        String threadNamePrefix = threadParamArray[3];
        ExecutorService executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
        return executorService;
    }

    private ExecutorService buildThreadPoolForTaskBankReceiptForTask(String name, Integer corePoolSize) {
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.receiptTaskDown.thread.param","8,32,1000,YmsPool_CmpReceiptTaskDown");
        String[] threadParamArray = threadParam.split(",");
        if (corePoolSize == null) {
            corePoolSize = Integer.parseInt(threadParamArray[0]);;
        }
        int maxPoolSize = Integer.parseInt(threadParamArray[1]);
        int queueSize = Integer.parseInt(threadParamArray[2]);
        String threadNamePrefix;
        if (StringUtils.isEmpty(name)) {
            threadNamePrefix = threadParamArray[3];
        } else {
            threadNamePrefix = name;
        }
        ExecutorService executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
        return executorService;
    }

    /**
     * *批量更新银行对账单和回单关联关系
     *
     * @param callableList
     * @throws Exception
     */
//    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
//    public void updateBankreceiptAssociationstatus(List<Map<String, Object>> callableList) throws Exception {
//        List<String> bankReconciliationIds = new ArrayList<>();
//        for (Map<String, Object> map : callableList) {
//            List<BankElectronicReceipt> receipts = (List<BankElectronicReceipt>) map.get("receipts");
//            // 查询出的银行回单根据交易流水号查询银行对账单的数据
//            for (BankElectronicReceipt bankElectronicReceipt : receipts) {
//                QuerySchema querySchema = QuerySchema.create().addSelect("id");
//                QueryConditionGroup queryCondition = new QueryConditionGroup();
//                queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("bank_seq_no").eq(bankElectronicReceipt.getBankseqno())));
//                queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(bankElectronicReceipt.getAccentity())));
//                queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("dc_flag").eq(bankElectronicReceipt.getDc_flag().getValue())));
//                queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_amt").eq(bankElectronicReceipt.getTran_amt())));
//                queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(bankElectronicReceipt.getEnterpriseBankAccount())));
//                queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("receiptassociation").eq(ReceiptassociationStatus.NoAssociated.getValue())));
//                if (bankReconciliationIds != null && bankReconciliationIds.size() > 0) {
//                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("id").not_in(bankReconciliationIds)));
//                }
//                querySchema.addCondition(queryCondition);
//                List<BankReconciliation> detailList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
//                if (null != detailList && detailList.size() > 0) {
//                    bankReconciliationIds.add(detailList.get(0).getId().toString());
//                    bankElectronicReceipt.setAssociationstatus(ReceiptassociationStatus.AutomaticAssociated.getValue());
//                    EntityTool.setUpdateStatus(bankElectronicReceipt);
//                    bankElectronicReceipt.setBankreconciliationid(detailList.get(0).getId().toString());
//                    MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, bankElectronicReceipt);
//                    //关联成功且交易回单有回单文件 发事件
//                    if (StringUtils.isNotEmpty(bankElectronicReceipt.getExtendss())) {
//                        bankreconciliationService.sendEventOfFileid(detailList.get(0).getId(), bankElectronicReceipt.getExtendss());
//                    }
//                }
//            }
//            if (bankReconciliationIds != null && bankReconciliationIds.size() > 0) {
//                Map<String, Object> bankReconparam = new HashMap<>();
//                bankReconparam.put("ids", bankReconciliationIds);
//                bankReconparam.put("ytenant_id", InvocationInfoProxy.getTenantid());
//                SqlHelper.update("com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper.updateBankReconciliationReceiptassociation", bankReconparam);
//            }
//        }
//    }
    @Override
    public void matchBankreceiptAndBankReconciliation(List<Map<String, Object>> callableList) throws Exception {
        String[] queryBankReconciliationFields = {ICmpConstant.ID, BankReconciliation.BANK_SEQ_NO, BankReconciliation.DC_FLAG, BankReconciliation.TRAN_AMT, BankReconciliation.BANKACCOUNT, BankReconciliation.RECEIPTASSOCIATION, BankReconciliation.OTHER_CHECKNO, BankReconciliation.OTHER_CHECKFLAG};
        String[] queryBankElectronicReceiptFields = {ICmpConstant.ID, BankElectronicReceipt.EXTENDSS};
        List<String> bankReconciliationIds = new ArrayList<>();
        List<BankElectronicReceipt> updateBankElectronicReceipts = new ArrayList<>();
        Map<Long, BankElectronicReceipt> sendRelateEventMap = new HashMap<>();
        Map<Long, BankElectronicReceipt> reconciliationIdAndBankElectronicReceiptMap = new HashMap<>();
        List<String> matchedBankReconciliationId = new ArrayList<>();
        for (Map<String, Object> map : callableList) {
            List<BankElectronicReceipt> receipts = (List<BankElectronicReceipt>) map.get("receipts");
            // 查询出的银行回单根据交易流水号查询银行对账单的数据
            long s = System.currentTimeMillis();
            for (BankElectronicReceipt bankElectronicReceipt : receipts) {
                List<BankReconciliation> detailList = new ArrayList<>();
                String detailReceiptRelationCode = bankElectronicReceipt.getDetailReceiptRelationCode();
                //优先用关联码查询
                if (StringUtils.isNotEmpty(detailReceiptRelationCode)) {
                    QuerySchema querySchema = QuerySchema.create().addSelect(queryBankReconciliationFields);
                    QueryConditionGroup queryCondition = new QueryConditionGroup();
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.DETAIL_RECEIPT_RELATION_CODE).eq(detailReceiptRelationCode)));
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankReconciliation.RECEIPTASSOCIATION).eq(ReceiptassociationStatus.NoAssociated.getValue())));
                    querySchema.addCondition(queryCondition);
                    List<BizObject> list = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema);
                    //List<BizObject> list = CmpMetaDaoHelper.queryColByOneEqualCondition(BankReconciliation.ENTITY_NAME, ICmpConstant.DETAIL_RECEIPT_RELATION_CODE, detailReceiptRelationCode, queryBankReconciliationFields);
                    if (list != null && list.size() > 1) {
                        log.error("bankreconciliation detail_receipt_relation_code is not unique!");
                        CtmJSONObject result = new CtmJSONObject();
                        result.put("errorMsg", "bankreconciliation detail_receipt_relation_code is not unique!");
                        result.put("detail_receipt_relation_code", detailReceiptRelationCode);
                        BusinessObject businessObject = BusiObjectBuildUtil.build(IServicecodeConstant.BANKRECEIPTMATCH,
                                BankElectronicReceipt.ENTITY_NAME, OperCodeTypes.update,
                                IMsgConstant.BANK_ELEC_RECEIPT, IMsgConstant.BANK_ELEC_RECEIPT, result);
                        IBusinessLogService businessLogService = AppContext.getBean(IBusinessLogService.class);
                        businessLogService.saveBusinessLog(businessObject);
                    } else if (list != null && list.size() == 1) {
                        BankReconciliation bankReconciliation = new BankReconciliation();
                        bankReconciliation.init(list.get(0));
                        detailList.add(bankReconciliation);
                    }
                }
                //关联码查询匹配不到或者没有关联码时，用要素匹配
                if (CollectionUtil.isEmpty(detailList)) {
                    QuerySchema querySchema = QuerySchema.create().addSelect(queryBankReconciliationFields);
                    QueryConditionGroup queryCondition = new QueryConditionGroup();
                    //流水回单关联四要素[流水号、收付方向、交易金额、银行账号]
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("bank_seq_no").eq(bankElectronicReceipt.getBankseqno())));
                    //CZFW-411237 【20250110迭代上线】【现金管理-回单】银行交易回单，在当前已经适配账户共享的情况下，回单和流水，通过调度任务自动关联的时候，关联维度中，应该去掉‘资金组织’维度，原因详见附件
                    //queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(bankElectronicReceipt.getAccentity())));
                    //收付方向允许为空后，为空时，认为是异常数据，不做匹配
                    if (bankElectronicReceipt.getDc_flag() == null) {
                        continue;
                    }
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("dc_flag").eq(bankElectronicReceipt.getDc_flag().getValue())));

                    //交易日期匹配。可能切日，一般都是1天。我们可以设置为2天，往前找2天就行。不用往后找。比如回单日期是20251023，查找流水日期就是20251021、20251022、20251023
                    Date endDate = bankElectronicReceipt.getTranDate();
                    Date startDate = DateUtils.beforeDay(endDate, 2);
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankReconciliation.TRAN_DATE).between(startDate, endDate)));

                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_amt").eq(bankElectronicReceipt.getTran_amt())));
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(bankElectronicReceipt.getEnterpriseBankAccount())));
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("receiptassociation").eq(ReceiptassociationStatus.NoAssociated.getValue())));
                    if (bankReconciliationIds != null && bankReconciliationIds.size() > 0) {
                        queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("id").not_in(bankReconciliationIds)));
                    }
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankReconciliation.RECEIPTASSOCIATION).eq(ReceiptassociationStatus.NoAssociated.getValue())));
                    querySchema.addCondition(queryCondition);
                    detailList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                }
                if (null != detailList && detailList.size() > 0) {
                    String detailId = detailList.get(0).getId().toString();
                    //防止多个回单匹配到同一个流水
                    if (!matchedBankReconciliationId.contains(detailId)) {
                        bankReconciliationIds.add(detailId);
                        bankElectronicReceipt.setAssociationstatus(ReceiptassociationStatus.AutomaticAssociated.getValue());
                        EntityTool.setUpdateStatus(bankElectronicReceipt);
                        bankElectronicReceipt.setBankreconciliationid(detailId);
                        updateBankElectronicReceipts.add(bankElectronicReceipt);
                        if (StringUtils.isNotEmpty(bankElectronicReceipt.getExtendss())) {
                            sendRelateEventMap.put(detailList.get(0).getId(), bankElectronicReceipt);
                        }
                        reconciliationIdAndBankElectronicReceiptMap.put(detailList.get(0).getId(), bankElectronicReceipt);
                        //凭证关联银行电子回单功能；电子回单下载过文件且银行对账单和总账凭证已勾对，要发送关联事件
                        if (bankElectronicReceipt.getIsdown() && detailList.get(0).getOther_checkflag()) {
                            bankreconciliationService.handleBankReceiptCorrEvent(bankElectronicReceipt, detailList.get(0));
                        }
                        matchedBankReconciliationId.add(detailId);
                    }
                }
            }
            log.error("【交易回单关联】银行账号{},包含{}条回单明细,匹配银行流水共耗时{}s", map.get("enterpriseBankAccount"), CollectionUtils.isEmpty(receipts) ? "0" : receipts.size(), (System.currentTimeMillis() - s) / 1000.0);
        }
        //更新回单表、流水表、发事件
        TaskBankReceiptService bankReceiptService = AppContext.getBean(TaskBankReceiptService.class);
        bankReceiptService.updateBankreceiptAssociationstatus(updateBankElectronicReceipts, sendRelateEventMap, reconciliationIdAndBankElectronicReceiptMap, bankReconciliationIds, null);
    }


    @Override
    public Map<BankReconciliation, BankElectronicReceipt> matchBankReconciliationAndBankreceipt(List<BankReconciliation> bankReconciliationList) throws Exception {
        HashMap<BankReconciliation, BankElectronicReceipt> matchResult = new HashMap<BankReconciliation, BankElectronicReceipt>();
        List<BankReconciliation> originBankReconciliationList = new ArrayList<BankReconciliation>(bankReconciliationList);
        try {
            String[] queryBankElectronicReceiptFields = {ICmpConstant.ID, BankElectronicReceipt.EXTENDSS, BankElectronicReceipt.ISDOWN};
            List<String> bankReconciliationIds = new ArrayList<>();
            List<String> bankElectronicReceiptIds = new ArrayList<>();
            List<BankElectronicReceipt> updateBankElectronicReceipts = new ArrayList<>();
            Map<Long, BankElectronicReceipt> sendRelateEventMap = new HashMap<>();
            Map<Long, BankElectronicReceipt> reconciliationIdAndBankElectronicReceiptMap = new HashMap<>();
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                long s = System.currentTimeMillis();
                List<BankElectronicReceipt> matchedBankElectronicReceiptList = new ArrayList<>();
                BankElectronicReceipt matchedBankElectronicReceipt = null;
                String detailReceiptRelationCode = bankReconciliation.getDetailReceiptRelationCode();
                //优先用关联码查询
                if (StringUtils.isNotEmpty(detailReceiptRelationCode)) {
                    QuerySchema querySchema = QuerySchema.create().addSelect(queryBankElectronicReceiptFields);
                    QueryConditionGroup queryCondition = new QueryConditionGroup();
                    // 增加参数：没有下载回单文件的不进行关联
                    if (receiptRelateCheckFile.equals("true")) {
                        queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.EXTENDSS).is_not_null()));
                    }
                    // [关联码]
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.DETAIL_RECEIPT_RELATION_CODE).eq(detailReceiptRelationCode)));
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.ASSOCIATIONSTATUS).eq(ReceiptassociationStatus.NoAssociated.getValue())));
                    querySchema.addCondition(queryCondition);
                    matchedBankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, querySchema, null);
/*                    List<BizObject> matchedBankElectronicReceiptListMap = CmpMetaDaoHelper.queryColByOneEqualCondition(BankElectronicReceipt.ENTITY_NAME, ICmpConstant.DETAIL_RECEIPT_RELATION_CODE,detailReceiptRelationCode, queryBankElectronicReceiptFields);
                    matchedBankElectronicReceiptList = matchedBankElectronicReceiptListMap.stream().map(bankElectronicReceiptMap -> {
                        BankElectronicReceipt bankElectronicReceiptDTO = new BankElectronicReceipt();
                        bankElectronicReceiptDTO.init(bankElectronicReceiptMap);
                        return bankElectronicReceiptDTO;
                    }).collect(Collectors.toList());*/
                }
                //关联码查询匹配不到或者没有关联码时，用要素匹配
                if (CollectionUtil.isEmpty(matchedBankElectronicReceiptList)) {
                    QuerySchema querySchema = QuerySchema.create().addSelect(queryBankElectronicReceiptFields);
                    QueryConditionGroup queryCondition = new QueryConditionGroup();
                    //流水回单关联四要素[流水号、收付方向、交易金额、银行账号]
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.BANKSEQNO).eq(bankReconciliation.getBank_seq_no())));
                    //CZFW-411237 【20250110迭代上线】【现金管理-回单】银行交易回单，在当前已经适配账户共享的情况下，回单和流水，通过调度任务自动关联的时候，关联维度中，应该去掉‘资金组织’维度，原因详见附件
                    //queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(bankElectronicReceipt.getAccentity())));
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.DC_FLAG).eq(bankReconciliation.getDc_flag().getValue())));
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.TRAN_AMT).eq(bankReconciliation.getTran_amt())));
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.ENTERPRISE_BANK_ACCOUNT).eq(bankReconciliation.getBankaccount())));
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.ASSOCIATIONSTATUS).eq(ReceiptassociationStatus.NoAssociated.getValue())));
                    //交易日期匹配。可能切日，一般都是1天。我们可以设置为2天。回单找流水时，往前找2天就行。不用往后找。比如回单日期是20251023，查找流水日期就是20251021、20251022、20251023
                    //流水找回单反之
                    Date startDate = bankReconciliation.getTran_date();
                    Date  endDate = DateUtils.afterDay(startDate, 2);
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.TRAN_DATE).between(startDate, endDate)));

                    if (bankElectronicReceiptIds != null && bankElectronicReceiptIds.size() > 0) {
                        queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("id").not_in(bankElectronicReceiptIds)));
                    }
                    // 增加参数：没有下载回单文件的不进行关联
                    if (receiptRelateCheckFile.equals("true")) {
                        queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.EXTENDSS).is_not_null()));
                    }
                    queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.ASSOCIATIONSTATUS).eq(ReceiptassociationStatus.NoAssociated.getValue())));
                    querySchema.addCondition(queryCondition);
                    matchedBankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, querySchema, null);
                }
                if (matchedBankElectronicReceiptList != null && matchedBankElectronicReceiptList.size() > 1) {
                    log.error("matchedBankElectronicReceiptList is not unique!");
                    CtmJSONObject result = new CtmJSONObject();
                    result.put("errorMsg", "matchedBankElectronicReceiptList is not unique!");
                    result.put(BankElectronicReceipt.BANKSEQNO, bankReconciliation.getBank_seq_no());
                    result.put(BankElectronicReceipt.DC_FLAG, bankReconciliation.getDc_flag().getValue());
                    result.put(BankElectronicReceipt.TRAN_AMT, bankReconciliation.getTran_amt());
                    result.put(BankElectronicReceipt.ENTERPRISE_BANK_ACCOUNT, bankReconciliation.getBankaccount());
                    result.put("detail_receipt_relation_code", detailReceiptRelationCode);
                    BusinessObject businessObject = BusiObjectBuildUtil.build(IServicecodeConstant.BANKRECEIPTMATCH,
                            BankElectronicReceipt.ENTITY_NAME, OperCodeTypes.update,
                            IMsgConstant.BANK_ELEC_RECEIPT, IMsgConstant.BANK_ELEC_RECEIPT, result);
                    IBusinessLogService businessLogService = AppContext.getBean(IBusinessLogService.class);
                    businessLogService.saveBusinessLog(businessObject);
                    LogUtil.saveErrorBusinessLog(result.toString(), "流水匹配回单失败", bankReconciliation.getBank_seq_no());
                } else if (null != matchedBankElectronicReceiptList && matchedBankElectronicReceiptList.size() == 1) {
                    matchedBankElectronicReceipt = matchedBankElectronicReceiptList.get(0);
                    bankElectronicReceiptIds.add(matchedBankElectronicReceipt.getId().toString());
                    bankReconciliationIds.add(bankReconciliation.getId().toString());
                    matchedBankElectronicReceipt.setAssociationstatus(ReceiptassociationStatus.AutomaticAssociated.getValue());
                    //需要修改原流水的字段，否则智能流水后面会覆盖回去
                    bankReconciliation.setReceiptassociation(ReceiptassociationStatus.AutomaticAssociated.getValue());
                    bankReconciliation.setReceiptId(matchedBankElectronicReceipt.getId().toString());
                    EntityTool.setUpdateStatus(matchedBankElectronicReceipt);
                    matchedBankElectronicReceipt.setBankreconciliationid(bankReconciliation.getId().toString());
                    updateBankElectronicReceipts.add(matchedBankElectronicReceipt);
                    if (StringUtils.isNotEmpty(matchedBankElectronicReceipt.getExtendss())) {
                        sendRelateEventMap.put(bankReconciliation.getId(), matchedBankElectronicReceipt);
                    }
                    reconciliationIdAndBankElectronicReceiptMap.put(bankReconciliation.getId(), matchedBankElectronicReceipt);
                    matchResult.put(bankReconciliation, matchedBankElectronicReceipt);
                }
                log.error("【交易回单关联】银行账号{},包含{}条流水明细,匹配银行回单共耗时{}s", bankReconciliation.getBankaccount(), CollectionUtils.isEmpty(bankReconciliationList) ? "0" : bankReconciliationList.size(), (System.currentTimeMillis() - s) / 1000.0);
            }
            ////更新回单表、流水表、发事件
            //TaskBankReceiptService bankReceiptService = AppContext.getBean(TaskBankReceiptService.class);
            //bankReceiptService.updateBankreceiptAssociationstatus(updateBankElectronicReceipts, sendRelateEventMap, reconciliationIdAndBankElectronicReceiptMap, bankReconciliationIds, bankReconciliationList);
        }catch (Exception e){
            String errorMsg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004CF", "回单匹配失败！") /* "回单匹配失败！" */ + e.getMessage();
            log.error(errorMsg, e);
            //恢复数据，不抛异常，防止阻断主流程
            bankReconciliationList = originBankReconciliationList;
            matchResult = new HashMap<BankReconciliation, BankElectronicReceipt>();
        }
        return matchResult;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void updateBankreceiptAssociationstatus(List<BankElectronicReceipt> updateBankElectronicReceipts,
                                                   Map<Long, BankElectronicReceipt> sendRelateEventMap, Map<Long, BankElectronicReceipt> reconciliationIdAndBankElectronicReceiptMap, List<String> bankReconciliationIds, List<BankReconciliation> bankReconciliationList) throws Exception {
        try {
            if (CollectionUtils.isEmpty(updateBankElectronicReceipts)) {
                return;
            }
            //step1:回写回单与流水关联状态
            MetaDaoHelper.update(BankElectronicReceipt.ENTITY_NAME, updateBankElectronicReceipts);
            List<BankReconciliation> updateBankReconciliationList = new ArrayList<>();
            for(BankElectronicReceipt bankElectronicReceipt : updateBankElectronicReceipts){
                BankReconciliation bankReconciliation = new BankReconciliation();
                bankReconciliation.setId(Long.valueOf(bankElectronicReceipt.getBankreconciliationid()));
                bankReconciliation.setReceiptassociation(ReceiptassociationStatus.AutomaticAssociated.getValue());
                //保存前规则调用时，拿不到id，保存后规则会再赋值一次
                bankReconciliation.setReceiptId(bankElectronicReceipt.getId().toString());
                updateBankReconciliationList.add(bankReconciliation);
            }
            try {
                CommonSaveUtils.updateBankReconciliation4ReceiptassociationStatus(updateBankReconciliationList);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D0", "更新对账单失败") /* "更新对账单失败" */, e);
            }
            //step3:关联成功且交易回单有回单文件 发事件
            for (Map.Entry<Long, BankElectronicReceipt> entry : sendRelateEventMap.entrySet()) {
                Long bankreconcilionId = entry.getKey();
                BankElectronicReceipt bankElectronicReceipt = entry.getValue();
                String fileId = bankElectronicReceipt.getExtendss();
                LogUtil.saveBankelereceiptSendFileEventlogById(bankreconcilionId.toString(), fileId, 20); // 修改这里
                bankreconciliationService.sendEventOfFileid(bankreconcilionId, fileId);
            }
        } catch (Exception e) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D4", "更新回单关联状态失败") /* "更新回单关联状态失败" */, e);
        } finally {
            //step3:关联成功且交易回单有回单文件 发事件
            for (Map.Entry<Long, BankElectronicReceipt> entry : sendRelateEventMap.entrySet()) {
                Long bankreconcilionId = entry.getKey();
                BankElectronicReceipt bankElectronicReceipt = entry.getValue();
                String fileId = bankElectronicReceipt.getExtendss();
                LogUtil.saveBankelereceiptSendFileEventlogById(bankreconcilionId.toString(), fileId, 21); // 修改这里
                bankreconciliationService.sendEventOfFileid(bankreconcilionId, fileId);
            }
        }
    }


    /**
     * 银行交易回单文件下载调度任务
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public ThreadResult bankTradeDetailElectronTask(CtmJSONObject param) throws Exception {
        ThreadResult threadResult = new ThreadResult();
        try {
            //获取需要下载回单的数据
            HashMap<String, String> querydate = TaskUtils.queryDateProcess(param, "yyyy-MM-dd");
            //QuerySchema schema = QuerySchema.create().addSelect("id , extendss , custno ,receiptno ,bill_extend ,enterpriseBankAccount , signStatus ");
            //发回单关联事件时，需要getIsdown getBankreconciliationid getReceiptno
            QuerySchema schema = QuerySchema.create().addSelect("id , extendss , custno ,receiptno ,bill_extend ,enterpriseBankAccount , signStatus, isdown, bankreconciliationid, receiptno");
            schema.addSelect(YQLUtils.YQL_UNIQUE_NO);
            QueryConditionGroup conditionGroup = new QueryConditionGroup();
            Date yesterday = DateUtils.getBeforeDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            if (querydate.isEmpty()) {
                conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").egt(dateFormat.format(yesterday))));
                conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").elt(dateFormat.format(today))));
            } else if (querydate.containsKey(TaskUtils.TASK_NO_DATA)) {
                threadResult.getErrorReturnList().add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D2", "请求的交易日期时间段为空") /* "请求的交易日期时间段为空" */);
                return threadResult;
            } else {
                String begDate = querydate.get(TaskUtils.TASK_START_DATE);
                String endDate = querydate.get(TaskUtils.TASK_END_DATE);
                conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").egt(begDate)));
                //结束日期大于当前日期
                if (LocalDate.parse(endDate).isBefore(LocalDate.now())) {
                    conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").elt(endDate)));
                } else {
                    conditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("tranDate").elt(dateFormat.format(today))));
                }
            }
            //所属组织改为使用组织，转化为账户条件，和原账户条件取交集
            List<String> accounts = TaskUtils.getAccountsByAccEntitys(param);
            if (CollectionUtil.isNotEmpty(accounts)) {
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(accounts));
            }

            //String accentitys = (String) (Optional.ofNullable(param.get("accentity")).orElse(""));
            String banktypes = (String) (Optional.ofNullable(param.get("banktype")).orElse(""));
            String currencys = (String) (Optional.ofNullable(param.get("currency")).orElse(""));
            String accountIds = (String) (Optional.ofNullable(param.get("accountId")).orElse(""));
            //String[] accentityArr = null;
            //if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accentitys)) {
            //    accentityArr = accentitys.split(";");
            //}
            //if (accentityArr != null && accentityArr.length > 0) {
            //    conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentityArr));
            //}
            String[] banktypeArr;
            if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(banktypes)) {
                banktypeArr = banktypes.split(";");
                if (banktypeArr != null && banktypeArr.length > 0) {
                    conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount.bankNumber.bank").in(banktypeArr));
                }
            }
            String[] currencyArr = null;
            if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(currencys)) {
                currencyArr = currencys.split(";");
                if (currencyArr != null && currencyArr.length > 0) {
                    conditionGroup.appendCondition(QueryCondition.name("currency").in(currencyArr));
                }
            }
            String[] accountIdArr = null;
            if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accountIds)) {
                accountIdArr = accountIds.split(";");
                if (accountIdArr != null && accountIdArr.length > 0) {
                    conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(accountIdArr));
                }
            }

            conditionGroup.addCondition(QueryCondition.name("extendss").is_null());
            conditionGroup.addCondition(QueryCondition.name("custno").is_not_null());
            schema.addCondition(conditionGroup);
            //查询最近的数据
            schema.addOrderBy(new QueryOrderby("tranDate", "desc"));
            //最多查pageSize条
            int pageSize = Integer.parseInt(CmpYmsConfigEnum.bankReceiptDownloadMaxsize.getFinalValue());
            schema.addPager(0, pageSize);
            List<BankElectronicReceipt> receiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, schema, null);
            if (receiptList.size() >= pageSize) {
                threadResult.getErrorReturnList().add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D1", "调度任务每次会下载最近的%s条数据，当前需要下载的数据为%s条，可能需要再次下载") /* "调度任务每次会下载最近的%s条数据，当前需要下载的数据为%s条，可能需要再次下载" */, pageSize, receiptList.size()));
            }
            if (receiptList != null && receiptList.size() > 0) {
                //按相同账户10个一组 进行分组 执行分组后 每组数据都是10条 且账户相同
                BankReceiptService bankReceiptService = CtmAppContext.getBean(BankReceiptService.class);
                Map<String, List<BankElectronicReceipt>> mapAccountReceipt = new HashMap<>();
                for (BankElectronicReceipt receipt : receiptList) {
                    if (mapAccountReceipt.containsKey(receipt.getEnterpriseBankAccount())) {
                        mapAccountReceipt.get(receipt.getEnterpriseBankAccount()).add(receipt);
                    } else {
                        List<BankElectronicReceipt> receiptListThis = new ArrayList<>();
                        receiptListThis.add(receipt);
                        mapAccountReceipt.put(receipt.getEnterpriseBankAccount(), receiptListThis);
                    }
                }
                List<List<List<BankElectronicReceipt>>> excuteReceiptList = new ArrayList<>();
                for (List<BankElectronicReceipt> receipts : mapAccountReceipt.values()) {
                    excuteReceiptList.add(groupReceiptData(receipts, 10));
                }
                //构造一个内部是List<BankElectronicReceipt>的结婚 且每个List<BankElectronicReceipt>账户相同 数量<=10
                List<List<BankElectronicReceipt>> excuteReceiptGroupList = new ArrayList<>();
                for (List<List<BankElectronicReceipt>> groupList : excuteReceiptList) {
                    for (List<BankElectronicReceipt> excute : groupList) {
                        excuteReceiptGroupList.add(excute);
                    }
                }
                //构建线程池
                ExecutorService executorService = buildThreadPoolForTaskBankReceiptForTask("YmsPool_CmpReceiptTaskDown", param.getInteger("corepoolsize"));
                //下载文件 并更新状态
                threadResult = bankReceiptService.downloadReceiptFileByAccount(excuteReceiptGroupList, executorService);
            }
        } catch (Exception e) {
            log.error("银行回单文件拉取更新单据信息异常" + e, e);
            threadResult.getErrorReturnList().add(e.getMessage());
        }
        return threadResult;
    }


    private List<List<BankElectronicReceipt>> groupReceiptData(List<BankElectronicReceipt> sourceList, int groupNum) {
        List<List<BankElectronicReceipt>> targetList = new ArrayList<>();
        int size = sourceList.size();
        int remainder = size % groupNum;
        int sum = size / groupNum;
        for (int i = 0; i < sum; i++) {
            List<BankElectronicReceipt> subList;
            subList = sourceList.subList(i * groupNum, (i + 1) * groupNum);
            targetList.add(subList);
        }
        if (remainder > 0) {
            List<BankElectronicReceipt> subList;
            subList = sourceList.subList(size - remainder, size);
            targetList.add(subList);
        }
        return targetList;
    }

}