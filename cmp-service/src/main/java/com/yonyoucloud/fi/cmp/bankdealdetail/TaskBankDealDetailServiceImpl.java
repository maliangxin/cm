package com.yonyoucloud.fi.cmp.bankdealdetail;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.constant.CtmExceptionConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.task.until.TaskParamHandle;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;
import cn.hutool.core.thread.BlockPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskBankDealDetailServiceImpl implements TaskBankDealDetailService {

    @Autowired
    private BankDealDetailService bankDealDetailService;
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;
    @Autowired
    BankAccountSettingService bankaccountSettingService;
    @Autowired
    private HttpsService httpsService;
    ///调度任务线程池 不同的任务在大数据量下 需要不同的线程池参数 及配置
    private ExecutorService taskExecutor;

    public Map<String ,List<EnterpriseBankAcctVO>> bankTradeDetailQueryAccounts(CtmJSONObject param) throws Exception {
        CtmJSONObject queryBankAccountVosParams = TaskParamHandle.buildQueryBankAccountVosParams(param);
        queryBankAccountVosParams.put(ICmpConstant.IS_DISPATCH_TASK_CMP, param.get(ICmpConstant.IS_DISPATCH_TASK_CMP));
        //根据参数查询全量账户
        List<EnterpriseBankAcctVO> bankAccounts = EnterpriseBankQueryService.getEnterpriseBankAccountVos(queryBankAccountVosParams);
        //通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
        Map<String ,List<EnterpriseBankAcctVO>> bankAccountsGroup= bankDealDetailService.getBankAcctVOsGroupByTask(bankAccounts);
        return bankAccountsGroup;
    }


    @Override
    public ThreadResult bankTradeDetailAsyncProcess(CtmJSONObject param, Boolean isHistory, String logId, Map<String ,List<EnterpriseBankAcctVO>> bankAccountsGroup) throws Exception {
        List<EnterpriseBankAcctVO> innerAccounts = bankAccountsGroup.get("innerAccounts");
        List<EnterpriseBankAcctVO> checkSuccess = bankAccountsGroup.get("checkSuccess");
        CtmJSONObject queryBankAccountVosParams = TaskParamHandle.buildQueryBankAccountVosParams(param);//直联账户
        //添加时间参数 获取每天的时间集合
        List<String>getBetweenDate = TaskParamHandle.getBetweenDate(param, isHistory);


        checkSuccess = DirectmethodCheckUtils.getAccountByParamMapOfEnterpriseBankAcctVOs(param,checkSuccess);
        //保证返回的list的size正确
        bankAccountsGroup.put("checkSuccess", checkSuccess);


        ThreadResult threadResult = new ThreadResult();
        //结算中心 内部账户
        if(!innerAccounts.isEmpty()){
            ExecutorService executorServiceInner = null;
            try {
                //构建线程池
                executorServiceInner = buildThreadPoolForTaskBankDeail(queryBankAccountVosParams.getInteger("corepoolsize"));
                //拉取内部账户
                excuteInnerQueryTransactionDetail(param, innerAccounts,getBetweenDate, executorServiceInner, logId);
            }finally {
                if(executorServiceInner !=null){
                    executorServiceInner.shutdown();
                }
            }
        }
        if(!checkSuccess.isEmpty()){
            ExecutorService executorServiceHttp = null;
            try {
                //构建线程池
                executorServiceHttp = buildThreadPoolForTaskBankDeail(queryBankAccountVosParams.getInteger("corepoolsize"));
                List<CtmJSONObject> queryBankTradeDetailList = buildQueryBankTradeDetailMsg(checkSuccess, getBetweenDate);
                //拉取直联账户
                threadResult = excuteHttpQueryTransactionDetail(queryBankTradeDetailList,getBetweenDate, executorServiceHttp, logId);
            }finally {
                if(executorServiceHttp != null){
                    executorServiceHttp.shutdown();
                }
            }
        }
        return threadResult;
    }

    private List<CtmJSONObject> buildQueryBankTradeDetailMsg(List<EnterpriseBankAcctVO> checkSuccess, List<String> getBetweenDate) throws Exception {
        //将直连账户和币种、日期拼接
        List<CtmJSONObject> checkSuccessList = new ArrayList<>();
        String channel= bankConnectionAdapterContext.getChanPayCustomChanel();
        for(EnterpriseBankAcctVO evo:checkSuccess){
            Date enableDate = BankAccountUtil.getEnableDate(evo.getAccount());
            String customNo = bankaccountSettingService.getCustomNoByBankAccountId(evo.getId());
            for(BankAcctCurrencyVO currencyVO : evo.getCurrencyList()){
                for (String date : getBetweenDate) {
                    DateTime parseDate = DateUtil.parseDate(date);
                    //早于启用日期的不拉取
                    if (enableDate != null && parseDate.isBefore(enableDate)) {
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
                    map.put("customNo", customNo);
                    map.put("lineNumber", evo.getLineNumber());
                    map.put("channel", channel);
                    checkSuccessList.add(map);
                }
            }
        }
        return checkSuccessList;
    }

    ///**
    // * 测试分支没有调用的地方
    // * 之前的方法，现在分开了：
    // * bankTradeDetailQueryAccounts(CtmJSONObject param)查询数据；
    // * bankTradeDetailAsyncProcess(CtmJSONObject param, Boolean isHistory, String logId, List<EnterpriseBankAcctVO> innerAccounts, List<EnterpriseBankAcctVO> checkSuccess)
    // * 为之前的处理逻辑
    // * @param param
    // * @param isHistory
    // * @param logId
    // * @throws Exception
    // */
    //@Override
    //public void bankTradeDetailAsyncProcess(CtmJSONObject param, Boolean isHistory, String logId) throws Exception {
    //    CtmJSONObject queryBankAccountVosParams = TaskParamHandle.buildQueryBankAccountVosParams(param);
    //    queryBankAccountVosParams.put(ICmpConstant.IS_DISPATCH_TASK_CMP, param.get(ICmpConstant.IS_DISPATCH_TASK_CMP));
    //    //根据参数查询全量账户
    //    List<EnterpriseBankAcctVO> bankAccounts = EnterpriseBankQueryService.getEnterpriseBankAccountVos(queryBankAccountVosParams);
    //    //通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
    //    Map<String ,List<EnterpriseBankAcctVO>> bankAccountsGroup= bankDealDetailService.getBankAcctVOsGroupByTask(bankAccounts);
    //    //直联账户
    //    List<EnterpriseBankAcctVO> checkSuccess = bankAccountsGroup.get("checkSuccess");
    //    //添加时间参数 获取每天的时间集合
    //    List<String>getBetweenDate = TaskParamHandle.getBetweenDate(param, isHistory);
    //    //结算中心 内部账户
    //    List<EnterpriseBankAcctVO> innerAccounts = bankAccountsGroup.get("innerAccounts");
    //
    //    if(!innerAccounts.isEmpty()){
    //        ExecutorService executorServiceInner = null;
    //        try {
    //            //构建线程池
    //            executorServiceInner = buildThreadPoolForTaskBankDeail(queryBankAccountVosParams.getInteger("corepoolsize"));
    //            //拉取内部账户
    //            excuteInnerQueryTransactionDetail( innerAccounts,getBetweenDate, executorServiceInner, logId);
    //        }finally {
    //            if(executorServiceInner !=null){
    //                executorServiceInner.shutdown();
    //            }
    //        }
    //
    //    }
    //    if(!checkSuccess.isEmpty()){
    //        ExecutorService executorServiceHttp = null;
    //        try {
    //            //构建线程池
    //            executorServiceHttp = buildThreadPoolForTaskBankDeail(queryBankAccountVosParams.getInteger("corepoolsize"));
    //            //拉取直联账户
    //            excuteHttpQueryTransactionDetail( checkSuccess,getBetweenDate, executorServiceHttp, logId);
    //        }finally {
    //            if(executorServiceHttp != null){
    //                executorServiceHttp.shutdown();
    //            }
    //        }
    //
    //    }
    //
    //    TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, null,
    //            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180272", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
    //
    //}

    private void excuteInnerQueryTransactionDetail(CtmJSONObject param,List<EnterpriseBankAcctVO> innerAccounts,List<String>getBetweenDate,ExecutorService executorService, String logId)throws Exception{
        List<CtmJSONObject> queryInnerParamList = new ArrayList<>();
        //组装拉取内部账户的参数
        if(innerAccounts.size() > 0){
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : innerAccounts) {
                for(String date : getBetweenDate){
                    CtmJSONObject queryParam = new CtmJSONObject();
                    queryParam.put("accEntity", enterpriseBankAcctVO.getOrgid());
                    queryParam.put("accountId", enterpriseBankAcctVO.getId());
                    queryParam.put("startDate", date);
                    queryParam.put("endDate", date);
                    queryParam.put("insertCount",0);
                    queryParam.put("account",enterpriseBankAcctVO.getAccount());
                    queryInnerParamList.add(queryParam);
                }
            }
        }
        //执行查询内部账户
        ThreadPoolUtil.executeByBatch(executorService,queryInnerParamList,10,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400594", "查内部交易明细调度任务") /* "查内部交易明细调度任务" */,(int fromIndex, int toIndex)->{
            for(int t = fromIndex ; t < toIndex; t++){
                CtmJSONObject innerObj = queryInnerParamList.get(t);
                String accountInfo = innerObj.get("account") + "|" + innerObj.get("startDate");
                String lockKey = accountInfo + ICmpConstant.QUERYTRANSDETAILKEY;
                try {
                    CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                        CtmJSONObject paramNow = new CtmJSONObject(innerObj);
                        if (lockstatus == LockStatus.GETLOCK_FAIL) {
                            //加锁失败
                            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId,
                                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400595", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */,accountInfo), TaskUtils.UPDATE_TASK_LOG_URL);
                            return;
                        }
                        //加锁成功
                        bankDealDetailService.queryInnerAccountDetails(paramNow);
                    });
                } catch (Exception e) {
                    log.error("查询内部账户交易明细失败：",e);
                    TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, YQLUtils.getErrorMsqWithAccount(e, accountInfo), TaskUtils.UPDATE_TASK_LOG_URL);
                }
            }
            return null;
        });
    }

    private ThreadResult excuteHttpQueryTransactionDetail(List<CtmJSONObject> checkSuccessList, List<String>getBetweenDate, ExecutorService executorService, String logId) throws Exception{
        if (checkSuccessList.size() == 0) {
            ThreadResult threadResult = new ThreadResult();
            threadResult.getErrorReturnList().add(CtmExceptionConstant.DISPATCH_ACCOUNT_NOT_EXIST);
            return threadResult;
        }
        //执行查询直联账户
        ThreadResult threadResult = ThreadPoolUtil.executeByBatchCollectResults(executorService,checkSuccessList,10,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400596", "查银行交易明细调度任务") /* "查银行交易明细调度任务" */,null, true,(int fromIndex, int toIndex)->{
            ThreadResult subThreadResult = new ThreadResult();
            for(int t = fromIndex ; t < toIndex; t++){
                CtmJSONObject httpObj = checkSuccessList.get(t);
                String accountInfo = httpObj.get("acct_no") + "|" + httpObj.get("startDate") + "|" + httpObj.get("currencyCode");
                try {
                    String lockKey = accountInfo + ICmpConstant.QUERYTRANSDETAILKEY;
                    CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                        CtmJSONObject paramNow = new CtmJSONObject(httpObj);
                        if (lockstatus == LockStatus.GETLOCK_FAIL) {
                            ////加锁失败
                            //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS, logId,
                            //        String.format("[%s]:系统正在对此账户拉取中",accountInfo),
                            //        TaskUtils.UPDATE_TASK_LOG_URL);
                            subThreadResult.getSucessReturnList().add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400595", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */,accountInfo));
                            return;
                        }
                        //加锁成功
                        String noDataMsg = batchQueryTransactionDetail(paramNow);
                        if (noDataMsg != null) {
                            String noDataSuccessMessage = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400599", "[%s]:此账户返回[%s]") /* "[%s]:此账户返回[%s]" */,accountInfo, noDataMsg);
                            subThreadResult.getSucessReturnList().add(noDataSuccessMessage);
                        }
                    });
                } catch (Exception e) {
                    log.error("查询直连账户交易明细失败：", e);
                    //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_FAILURE, logId,
                    //        YQLUtils.getErrorMsqWithAccount(e, accountInfo),
                    //        TaskUtils.UPDATE_TASK_LOG_URL);
                    subThreadResult.getErrorReturnList().add(YQLUtils.getErrorMsqWithAccount(e, accountInfo));
                }
            }
            return subThreadResult;
        });
        return threadResult;
    }


    private String batchQueryTransactionDetail(CtmJSONObject params) throws Exception {
        String customNo = params.getString("customNo");
        CtmJSONObject queryMsg = new CtmJSONObject();
        CtmJSONObject result = new  CtmJSONObject();
        try {
            params.put("customNo", customNo);
            params.put("signature", "");
            params.put("begNum",1);
            queryMsg = bankDealDetailService.buildQueryTransactionDetailMsg(params);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            log.error("=======================交易明细task参数=========================>" + CtmJSONObject.toJSONString(queryMsg));
            result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL, requestData, bankConnectionAdapterContext.getChanPayUri());
            CtmJSONObject logData = new CtmJSONObject();
            String acct_no = params.getString("acct_no");
            String acct_name = params.getString("acct_name");
            logData.put("uri",bankConnectionAdapterContext.getChanPayUri());
            logData.put("params", "acctName:"+acct_name+", account:"+acct_no);
            logData.put("queryMsg", queryMsg);
            logData.put("result", result);
            //调度任务不写业务日志，否则写太多
            //ctmcmpBusinessLogService.saveBusinessLog(logData, acct_name, acct_no, IServicecodeConstant.BANKRECONCILIATION,"银行账户交易明细查询", "银行账户交易明细查询");
            log.error("=======================交易明细task结果===========================>" + CtmJSONObject.toJSONString(result));
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String service_resp_code = responseHead.getString("service_resp_code");
                if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL,service_resp_code)) {
                    CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                    Map<String, Object> enterpriseInfo = new HashMap<>();
                    enterpriseInfo.put("startDate", params.get("startDate"));
                    enterpriseInfo.put("endDate", params.get("endDate"));
                    enterpriseInfo.put("lineNumber",params.get("lineNumber"));
                    enterpriseInfo.put("accEntityId", params.getString("accEntityId"));
                    enterpriseInfo.put("accountId", params.get("accountId"));
                    enterpriseInfo.put("account", params.get("acct_no"));
                    enterpriseInfo.put("banktype", params.get("banktype"));
                    enterpriseInfo.put("startDate", params.get("startDate"));
                    enterpriseInfo.put("endDate", params.get("endDate"));

                    bankDealDetailService.insertTransactionDetail(enterpriseInfo, responseBody);
                    String nextPage = responseBody.getString("next_page");
                    while("1".equals(nextPage)){
                        int begNum = params.getInteger("begNum");
                        params.put("begNum", begNum + ITransCodeConstant.QUERY_NUMBER_50);
                        params.put("queryExtend", responseBody.get("query_extend"));
                        queryMsg = bankDealDetailService.buildQueryTransactionDetailMsg(params);
                        signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryMsg.toString());
                        requestData = new ArrayList<>();
                        requestData.add(new BasicNameValuePair("reqData", queryMsg.toString()));
                        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
                        log.error("===================>交易明细task参数存在下一页begNum:"+params.get("begNum") + queryMsg);
                        result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL, requestData, bankConnectionAdapterContext.getChanPayUri());
                        CtmJSONObject forLogData = new CtmJSONObject();
                        forLogData.put("uri",bankConnectionAdapterContext.getChanPayUri());
                        forLogData.put("params", "acctName:"+acct_name+", account:"+acct_no);
                        forLogData.put("queryMsg", queryMsg);
                        forLogData.put("result", result);
                        ctmcmpBusinessLogService.saveBusinessLog(forLogData, acct_name, acct_no, IServicecodeConstant.BANKRECONCILIATION,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400597", "银行账户交易明细查询") /* "银行账户交易明细查询" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400597", "银行账户交易明细查询") /* "银行账户交易明细查询" */);
                        log.error("===================>交易明细task结果在下一页" + result);
                        if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL,service_resp_code)) {
                            responseBody = result.getJSONObject("data").getJSONObject("response_body");
                            bankDealDetailService.insertTransactionDetail(enterpriseInfo, responseBody);
                            nextPage = responseBody.getString("next_page");
                            params.put("queryExtend", responseBody.get("query_extend"));
                        }else{
                            nextPage = "0";
                        }
                    }
                    params.put("queryExtend","");
                    params.put("nextPage", false);
//                    if ("1".equals(nextPage)) {
//                        int begNum = params.getInteger("begNum")==null?1:params.getInteger("begNum");
//                        params.put("begNum", begNum + ITransCodeConstant.QUERY_NUMBER_50);
//                        params.put("queryExtend", responseBody.get("query_extend"));
//                        params.put("nextPage", true);
//                        batchQueryTransactionDetail(params);
//                    }else{
//                        params.put("begNum", 1);
//                        params.put("queryExtend", "");
//                    }
                }else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
                    String noDataMsg = YQLUtils.getYQLNoDataMsq(responseHead);
                    return noDataMsg;
                    //没有数据不报错
                } else {
                    //调度任务时，抛出银企联异常给上层收集后展示
                    throw new CtmException(YQLUtils.getYQLErrorMsq(responseHead));
                }
            } else {
                //调度任务时，抛出银企联请求异常给上层收集后展示
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400598", "请求银企联报错：请求参数 = %s,响应参数 = %s") /* "请求银企联报错：请求参数 = %s,响应参数 = %s" */,queryMsg, result));
            }
        } catch (Exception e) {
            log.error(String.format("batchQueryTransactionDetailTask-error，请求参数 = %s,响应参数 = %s,报错信息 = %s,报错账号 = %s", queryMsg, result, e.getMessage(), params.getString("acct_no")), e);
            throw new Exception(e.getMessage());
        }
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg.put("nextPage", false);
        return null;
    }

    private ExecutorService buildThreadPoolForTaskBankDeail(Integer corePoolSize){
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.bankdetailTask.thread.param","8,128,1000,cmp-bankdetailTask-async-");
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

}
