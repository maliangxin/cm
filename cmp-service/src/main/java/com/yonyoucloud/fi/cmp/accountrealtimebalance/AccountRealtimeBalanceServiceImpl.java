package com.yonyoucloud.fi.cmp.accountrealtimebalance;

import cn.hutool.core.thread.BlockPolicy;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.dao.AccountRealtimeBalanceDAO;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.enums.BalanceFlag;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.Constant.ThreadConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.stct.api.openapi.IAccountOpenApiService;
import com.yonyoucloud.fi.stct.api.openapi.common.dto.Result;
import com.yonyoucloud.fi.stct.api.openapi.response.AccountQueryVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.message.BasicNameValuePair;
import lombok.NonNull;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CURRENCY;

/**
 * @ClassName AccountRealtimeBalanceServiceImpl
 * @Description 账户实时余额接口实现
 * @Author yangjn
 * @Date 2021/8/25 16:23
 * @Version 1.0
 **/
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class AccountRealtimeBalanceServiceImpl implements AccountRealtimeBalanceService {


    public static final String INNER_ACCOUNTS = "innerAccounts";
    public static final String NET_ACCOUNTS_TO_HTTP = "netAccountsToHttp";
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AccountRealtimeBalanceServiceImpl.class);
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;

    private  AtomicInteger cardinalNumber = new AtomicInteger(0);
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    private HttpsService httpsService;
    @Autowired
    private AccountRealtimeBalanceDAO accountRealtimeBalanceDAO;

    /**
     * 缓存币种主键和编码
     */
    private static final @NonNull Cache<String, String> currencyCodeCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();
    @Resource
    private CtmThreadPoolExecutor ctmThreadPoolExecutor;

    @Autowired
    private CurrencyQueryService currencyQueryService;

    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;


    private boolean queryAccountBalanceForHttp(List<EnterpriseBankAcctVO> enterpriseBankAcctList, CtmJSONObject params, String uid, int listSize, Set<String> failAccountSet) throws Exception {
        String message = null;
        CtmJSONObject queryBalanceMsg = new CtmJSONObject();
        CtmJSONObject result = new CtmJSONObject();
        int allDbCount = 0;
        String errorInfo = null;
        try{

            queryBalanceMsg = buildQueryBalanceMsg(params, enterpriseBankAcctList);
            log.error("实时余额请求参数==========================》：" + queryBalanceMsg.toString());
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryBalanceMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", queryBalanceMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_BALANCE, requestData, bankConnectionAdapterContext.getChanPayUri());
            log.error("实时余额响应参数==========================》：" + result.toString());
            CtmJSONObject logData = new CtmJSONObject();
            logData.put(IMsgConstant.BILL_DATA, params);
            logData.put(IMsgConstant.ACCBAL_REQUEST, queryBalanceMsg);
            logData.put(IMsgConstant.ACCBAL_RESPONSE, result);
            ctmcmpBusinessLogService.saveBusinessLog(logData, params.getString("customNo"), params.getString("customNo"), IServicecodeConstant.RETIBALIST, IMsgConstant.QUERY_ACCREALTIMEBAL, IMsgConstant.QUERY_ACCREALTIMEBAL);
            /**
             * 不拼接与银企连通信是否成功的信息，因为如果连接错误会直接throw error
             * 如果可以连接通信，但数据有误。会使用户理解有歧义(为什么提示成功了，但还是报错?)
             */
            //        message.append(result.getString("message"));
            //银企联返回结果解析
            if (result.isEmpty()) {
                errorInfo = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007BF", "银企联返回result为空") /* "银企联返回result为空" */;
                ProcessUtil.addMessage(uid, errorInfo);
                ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, enterpriseBankAcctList);
            }else {
                if (result.getInteger("code") == 1) {
                    CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                    String service_resp_code = responseHead.getString("service_resp_code");
                    if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_BALANCE, service_resp_code)) {
                        CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                        allDbCount = insertAccountBalanceData(enterpriseBankAcctList.get(0).getOrgid(), enterpriseBankAcctList, responseHead, responseBody, uid, failAccountSet, queryBalanceMsg);
                    } else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
                        ProcessUtil.addYQLNodataMessage(uid, YQLUtils.getYQLErrorMsqForManual(params, responseHead));
                    } else {
                        //无论是否成功 都把信息提示错出来
                        CtmJSONObject requestBody = (CtmJSONObject) queryBalanceMsg.get("request_body");
                        CtmJSONArray records = (CtmJSONArray) requestBody.get("record");
                        //StringBuilder errorInfoBuilder = new StringBuilder();
                        records.stream().forEach(record -> {
                            String recordErrorInfo = YQLUtils.getYQLErrorMsqForManual((CtmJSONObject) record, responseHead);
                            ProcessUtil.addMessage(uid, recordErrorInfo);

                            //errorInfoBuilder.append(recordErrorInfo + "\n");
                        });
                        ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, enterpriseBankAcctList);

                        //errorInfo = errorInfoBuilder.toString();
                    }
                } else {
                    errorInfo = YQLUtils.getYQLErrorMsqOfNetWork(result.getString("message"));
                    ProcessUtil.addMessage(uid, errorInfo);
                    ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, enterpriseBankAcctList);
                }
            }
            return true;
        }catch (Exception e) {
            log.error("queryAccountBalanceForHttp错误，请求参数 = {},响应参数 = {},报错信息={}", queryBalanceMsg.toString(), result, e);
            ArrayList<String> accountNoList = (ArrayList<String>) enterpriseBankAcctList.stream().map(t->t.getAcctName()).collect(Collectors.toList());
            errorInfo = YQLUtils.getErrorMsqWithAccount(e, accountNoList.toString());
            ProcessUtil.addMessage(uid, errorInfo);
            ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, enterpriseBankAcctList);
            return false;
        }finally {
            //只有存在报错提示信息时 才调用此方法 一旦存在message 则插件会认为当前失败数量+1
            //if(errorInfo != null){
            //    ProcessUtil.addYQLErrorMessage(uid, errorInfo);
            //}
            //else{
            //    ProcessUtil.refreshProcess(uid,true);
            //}
        }
    }

//    private ExecutorService getTaskExecutor() {
//
//        if (null != this.taskExecutor) {
//            return this.taskExecutor;
//        } else {
//            String coreSize = AppContext.getEnvConfig("cmp.balance.core.poolSize",String.valueOf(Runtime.getRuntime().availableProcessors()*2));
//            String maxSize = AppContext.getEnvConfig("cmp.balance.max.poolSize","200");
//            String queueLength = AppContext.getEnvConfig("cmp.balance.queueCapacity","10");
//            ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
//            threadPoolTaskExecutor.setCorePoolSize(Integer.parseInt(coreSize));
//            threadPoolTaskExecutor.setMaxPoolSize(Integer.parseInt(maxSize));
//            threadPoolTaskExecutor.setQueueCapacity(Integer.parseInt(queueLength));
//            threadPoolTaskExecutor.setKeepAliveSeconds(120);
//            threadPoolTaskExecutor.setThreadNamePrefix("cmp-account-realtime-balance-async-");
//            threadPoolTaskExecutor.setRejectedExecutionHandler(new BlockPolicy());
//
//            threadPoolTaskExecutor.initialize();
//            this.taskExecutor = YmsContextWrappers.wrap(threadPoolTaskExecutor.getThreadPoolExecutor());
//            return this.taskExecutor;
//        }
//    }

    ///**
    // *@Author tongyd
    // *@Description 插入企业银行账户实时余额数据，不用message，供调度任务使用
    // *@Date 2019/5/31 15:43
    // *@Param [accEntity, bankAccounts, responseBody]
    // *@Return void
    // **/
    //@Override
    //public int insertAccountBalanceData(String accEntity, List<EnterpriseBankAcctVO> bankAccounts, CtmJSONObject responseBody) throws Exception {
    //    String message = "";
    //    int allDbCount = insertAccountBalanceDataCollectMessage(accEntity, bankAccounts, responseBody, message);
    //    return allDbCount;

        //List<AccountRealtimeBalance> balances = new ArrayList<>();
        //int totalNum = responseBody.getInteger("tot_num");
        //int allDbCount = 0;
        //String currencyCode = "CNY";
        //if (totalNum < 1) {
        //    return allDbCount;
        //} else {
        //    CtmJSONArray records = new CtmJSONArray();
        //    if(totalNum == 1){
        //        records.add(responseBody.getJSONObject("record"));
        //    }else{
        //        records = responseBody.getJSONArray("record");
        //    }
        //    //账号 + 币种id作为key
        //    Map<String,CtmJSONObject> mapRecord = new HashMap<>();
        //    for (int i = 0; i < records.size(); i++) {
        //        CtmJSONObject record = records.getJSONObject(i);
        //        if (!record.containsKey("acct_bal") || !record.containsKey("acct_no")) {
        //            continue;
        //        }
        //        String currency = currencyQueryService.getCurrencyByCode(record.getString("curr_code")!=null?record.getString("curr_code"):currencyCode);
        //        String key = record.getString("acct_no") + currency;
        //        mapRecord.put(key, record);
        //    }
        //    for(EnterpriseBankAcctVO acctVo : bankAccounts){
        //        for(BankAcctCurrencyVO currVo:acctVo.getCurrencyList()){
        //            CtmJSONObject recordNow = mapRecord.get(acctVo.getAccount()+currVo.getCurrency());
        //            if(recordNow!=null){
        //                balances.add(buildRealBalanceVo(recordNow, acctVo, currVo));
        //            }
        //        }
        //    }
        //}
        //if(balances.size()>0){
        //    // start wangdengk 20230713 如果数据库中已经存在直接更新数据库中的余额信息 不存在再插入
        //    List<AccountRealtimeBalance> existBalances = queryExistRealBalanceData(balances);
        //    allDbCount = executeRealGroupData(existBalances, balances);
        //    // end wangdengk 20230713 如果数据库中已经存在直接更新数据库中的余额信息 不存在再插入
        //}
        //return allDbCount;
    //}

    /**
     *@Author tongyd
     *@Description 插入企业银行账户实时余额数据，使用message，供手动拉取使用
     *@Date 2019/5/31 15:43
     *@Param [accEntity, bankAccounts, responseBody]
     *@Return void
     **/
    @Override
    public int insertAccountBalanceData(String accEntity, List<EnterpriseBankAcctVO> bankAccounts, CtmJSONObject responseHead, CtmJSONObject responseBody, String uid, Set<String> failAccountSet, CtmJSONObject queryBalanceMsg) throws Exception {
        int allDbCount = 0;
        int requsetNum = queryBalanceMsg.getJSONObject("request_body").getJSONArray("record").size();
        StringBuilder errorMessage = new StringBuilder();
        //统一手动和调度任务插入数据的入口
        List<AccountRealtimeBalance> balances = new ArrayList<>();
        Integer responseNum = responseBody.getInteger("tot_num");
        if (responseNum == null) {
            errorMessage.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B6", "返回的总记录数【tot_num】不能为空！") /* "返回的总记录数【tot_num】不能为空！" */ + YQLUtils.CONTACT_YQL_TIP);
            return allDbCount;
        }
        String currencyCode = "CNY";
        if (responseNum != requsetNum) {
            //临时从表头中获取失败信息
            String serviceRespDesc = responseHead.getString("service_resp_desc");
            errorMessage.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007BC", "返回的记录数【%s】和请求的记录数【%s】不一致！报错信息为：%s") /* "返回的记录数【%s】和请求的记录数【%s】不一致！报错信息为：%s" */ + YQLUtils.CONTACT_YQL_TIP, responseNum, requsetNum, serviceRespDesc) + "\n");
        }
        if (responseNum < 1) {
            return allDbCount;
        } else {
            CtmJSONArray records = new CtmJSONArray();
            if(responseNum == 1){
                records.add(responseBody.getJSONObject("record"));
            }else{
                records = responseBody.getJSONArray("record");
            }
            //账号 + 币种id作为key
            Map<String,CtmJSONObject> returnCurrencyRecordMap = new HashMap<>();
            Table<String, String, CtmJSONObject> returnAcctCurrency_Record_Table = HashBasedTable.create();
            for (int i = 0; i < records.size(); i++) {
                CtmJSONObject record = records.getJSONObject(i);
                if (!record.containsKey("acct_bal")) {
                    errorMessage.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007C1", "账号【%s】返回的余额不能为空！") /* "账号【%s】返回的余额不能为空！" */ + YQLUtils.CONTACT_YQL_TIP, record.get("acct_name")) + "\n");
                    continue;
                }
                if (!record.containsKey("acct_no")) {
                    errorMessage.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B5", "账号【%s】返回的账号编码不能为空！") /* "账号【%s】返回的账号编码不能为空！" */ + YQLUtils.CONTACT_YQL_TIP, record.get("acct_name")) + "\n");
                    continue;
                }
                String recordCurrency = currencyQueryService.getCurrencyByCode(record.getString("curr_code")!=null?record.getString("curr_code"):currencyCode);
                String key = record.getString("acct_no") + recordCurrency;
                returnCurrencyRecordMap.put(key, record);
                returnAcctCurrency_Record_Table.put(record.getString("acct_no"), recordCurrency, record);
            }
            for(EnterpriseBankAcctVO acctVo : bankAccounts){
                List<BankAcctCurrencyVO> enableCurrencyVOList = getEnableCurrencyVOList(acctVo.getCurrencyList());
                if (enableCurrencyVOList.size() == 0) {
                    errorMessage.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007BB", "账号【%s】无启用状态的币种【%s】，请检查【企业资金账户】设置！") /* "账号【%s】无启用状态的币种【%s】，请检查【企业资金账户】设置！" */, acctVo.getAccount(), acctVo.getCurrencyList()) + "\n");
                    ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, bankAccounts);
                    //throw new CtmException(String.format("账号【%s】无启用状态的币种【%s】，请检查【企业资金账户】设置！", acctVo.getAccount(), acctVo.getCurrencyList()));
                }
                List<String> enableCurrencyCode = currencyQueryService.queryByCurrencyList(enableCurrencyVOList).values().stream().collect(Collectors.toList());
                for(BankAcctCurrencyVO enableCurrVo: enableCurrencyVOList){
                    CtmJSONObject recordNow = returnAcctCurrency_Record_Table.get(acctVo.getAccount(), enableCurrVo.getCurrency());
                    Collection<CtmJSONObject> accountRecords = returnAcctCurrency_Record_Table.row(acctVo.getAccount()).values();
                    List<Object> returnCurrCodeList = accountRecords.stream().map(r -> r.get("curr_code")).collect(Collectors.toList());
                    if (recordNow == null) {
                        EnterpriseBankQueryService.remainSubCurrency(enableCurrVo.getCurrency(), acctVo);
                        List<EnterpriseBankAcctVO> remainSubCurrencyAcctVo = new ArrayList<>();
                        remainSubCurrencyAcctVo.add(acctVo);
                        ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, remainSubCurrencyAcctVo);
                        if (returnCurrCodeList.isEmpty()) {
                            errorMessage.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221476FC05600005", "银企联未返回账号【%s】的币种【%s】的数据！") /* "银企联未返回账号【%s】的币种【%s】的数据！" */ + YQLUtils.CONTACT_YQL_TIP, acctVo.getAccount(), returnCurrCodeList)+ "\n");
                        } else {
                            errorMessage.append(String.format( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221476FC05600006", "银企联返回账号【%s】的币种【%s】与请求币种【%s】不一致！") /* "银企联返回账号【%s】的币种【%s】与请求币种【%s】不一致！" */ + YQLUtils.CONTACT_YQL_TIP, acctVo.getAccount(), returnCurrCodeList, enableCurrencyCode)+ "\n");
                        }
                        //throw new CtmException(String.format("银企联返回账号【%s】的币种【%s】与请求币种【%s】不一致，请联系银企联排查！", acctVo.getAccount(), returnCurrCodeList, enableCurrencyCode));
                    } else {
                        balances.add(buildRealBalanceVo(recordNow, acctVo, enableCurrVo));
                    }
                }
            }
        }
        int totalPullCount = balances.size();
        //成功拉取的数目
        ProcessUtil.totalPullCountAdd(uid,totalPullCount);
        if(totalPullCount >0){
            // start wangdengk 20230713 如果数据库中已经存在直接更新数据库中的余额信息 不存在再插入
            List<AccountRealtimeBalance> existBalances = queryExistRealBalanceData(balances);
            allDbCount = executeRealGroupData(existBalances, balances);
            ProcessUtil.newAddCountAdd(uid,allDbCount);
            // end wangdengk 20230713 如果数据库中已经存在直接更新数据库中的余额信息 不存在再插入
        }
        if (errorMessage.length() > 0) {
            //todo 抛异常的话，已经入库的数据会被回滚；后面有空改成收集错误信息，最后统一回写调度任务
            //手动拉取时，先返回给前端
            ProcessUtil.addMessage(uid,errorMessage.toString());
            //throw new CtmException(errorMessage.toString());
        }
        return allDbCount;
    }


    @Override
    public List<AccountRealtimeBalance> getAccountBalanceData(String accEntity, List<EnterpriseBankAcctVO> bankAccounts, CtmJSONObject responseBody) throws Exception {
        List<AccountRealtimeBalance> balances = new ArrayList<>();
        int totalNum = responseBody.getInteger("tot_num");
        String currencyCode = "CNY";
        if (totalNum < 1) {
            return balances;
        } else {
            CtmJSONArray records = new CtmJSONArray();
            if(totalNum == 1){
                records.add(responseBody.getJSONObject("record"));
            }else{
                records = responseBody.getJSONArray("record");
            }
            //账号 + 币种id作为key
            Map<String,CtmJSONObject> mapRecord = new HashMap<>();
            for (int i = 0; i < records.size(); i++) {
                CtmJSONObject record = records.getJSONObject(i);
                if (!record.containsKey("acct_bal") || !record.containsKey("acct_no")) {
                    continue;
                }
                String currency = currencyQueryService.getCurrencyByCode(record.getString("curr_code")!=null?record.getString("curr_code"):currencyCode);
                String key = record.getString("acct_no") + currency;
                mapRecord.put(key, record);
            }
            for(EnterpriseBankAcctVO acctVo : bankAccounts){
                for(BankAcctCurrencyVO currVo:acctVo.getCurrencyList()){
                    CtmJSONObject recordNow = mapRecord.get(acctVo.getAccount()+currVo.getCurrency());
                    if(recordNow!=null){
                        balances.add(buildRealBalanceVo(recordNow, acctVo, currVo));
                    }
                }
            }
        }
        return balances;
    }

    @Override
    public List<AccountRealtimeBalance> queryTraceabilityBalance(List<String> enterpriseBankAccounts, List<String> accentitys, String currency, List<String> currencyList,String startDate, String endDate) {
        List<AccountRealtimeBalance> list = accountRealtimeBalanceDAO.queryTraceabilityBalance(enterpriseBankAccounts,accentitys,currency,currencyList,startDate,endDate);
        if(CollectionUtils.isEmpty(list)){
            return new ArrayList<>();
        }
        return list;
    }


    private AccountRealtimeBalance buildRealBalanceVo(CtmJSONObject record,EnterpriseBankAcctVO bankAccount,BankAcctCurrencyVO currVo) throws Exception {
        AccountRealtimeBalance balance = new AccountRealtimeBalance();
        balance.setTenant(AppContext.getTenantId());
        //String accNo = record.getString("acct_no");
        //start wangdengk 20230713 此处增加业务逻辑 取银企账户的使用组织给会计主体赋值
        balance.setAccentity(bankAccount.getOrgid());
        //end wangdengk 20230713 此处增加业务逻辑 取银企账户的使用组织给会计主体赋值
        balance.setEnterpriseBankAccount(bankAccount.getId());
        if (bankAccount.getBank() != null) {
            balance.setBanktype((String) bankAccount.getBank());
        }
        balance.setCurrency(currVo.getCurrency());
        balance.setCashflag(record.getString("cash_flag"));
        balance.setYesterbal(!record.containsKey("yester_bal") ? BigDecimal.ZERO : record.getBigDecimal("yester_bal"));
        balance.setAcctbal(!record.containsKey("acct_bal") ? BigDecimal.ZERO : record.getBigDecimal("acct_bal"));
        BigDecimal acctBal = balance.getAcctbal();
        if (acctBal.compareTo(BigDecimal.ZERO) > 0) {
            balance.setDepositbalance(acctBal.abs());
            balance.setOverdraftbalance(BigDecimal.ZERO);
        } else if (acctBal.compareTo(BigDecimal.ZERO) < 0) {
            balance.setDepositbalance(BigDecimal.ZERO);
            balance.setOverdraftbalance(acctBal.abs());
        } else {
            balance.setDepositbalance(BigDecimal.ZERO);
            balance.setOverdraftbalance(BigDecimal.ZERO);
        }
        balance.setAvlbal(!record.containsKey("avl_bal") ? BigDecimal.ZERO : record.getBigDecimal("avl_bal"));
        balance.setFrzbal(!record.containsKey("frz_bal") ? BigDecimal.ZERO : record.getBigDecimal("frz_bal"));
        balance.setBalancedate(DateUtils.formatBalanceDate(new Date()));//实时余额新增余额日期字段 默认为系统当天
        //国际化相关字段
        balance.setProj_name(record.getString("proj_name"));
        balance.setProj_name(record.getString("sub_name"));
        balance.setProj_name(record.getString("budget_source"));
        balance.setFlag(BalanceFlag.AutoPull.getCode());
//        balance.setDatasource(BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode());
        balance.setDatasource(BalanceAccountDataSourceEnum.CURRENTDAY_BAL.getCode());

        balance.setRegular_amt(BigDecimal.ZERO);
        CtmJSONArray sub_records = record.getJSONArray("sub_record");
        if (sub_records !=null && sub_records.size()> 0) {
            BigDecimal regular_amt = new BigDecimal(0);
            for (int j = 0; j < sub_records.size(); j++) {
                CtmJSONObject sub_record = sub_records.getJSONObject(j);
                BigDecimal bal_amt = !sub_record.containsKey("bal_amt") ? BigDecimal.ZERO : sub_record.getBigDecimal("bal_amt");
                //定期金额
                regular_amt = regular_amt.add(bal_amt);
            }
            balance.setRegular_amt(regular_amt);
        }
        balance.setTotal_amt(BigDecimalUtils.safeAdd(balance.getAcctbal(),balance.getRegular_amt()));
        balance.setEntityStatus(EntityStatus.Insert);
        balance.setId(ymsOidGenerator.nextId());
        return balance;
    }

    private void queryAccountBalanceExecute(CtmJSONObject params,CtmJSONObject responseMsg,List<List<EnterpriseBankAcctVO>> lists,int batchcount,int totalTask,String uid) throws Exception{
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.balance.thread.param","8,32,1000,cmp-account-realtime-balance-async-");
        String[] threadParamArray = threadParam.split(",");
        int corePoolSize = Integer.parseInt(threadParamArray[0]);
        int maxPoolSize = Integer.parseInt(threadParamArray[1]);
        int queueSize = Integer.parseInt(threadParamArray[2]);
        String threadNamePrefix = threadParamArray[3];
        Set<String> failAccountSet = ConcurrentHashMap.newKeySet();

        ExecutorService executorService = null ;
        String channel = bankConnectionAdapterContext.getChanPayCustomChanel();
        params.put("channel",channel);
        try{
            executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                    .setDaemon(false)
                    .setRejectHandler(new BlockPolicy())
                    .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);

            ThreadPoolUtil.executeByBatch(executorService,lists,batchcount,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007BD", "实时余额查询") /* "实时余额查询" */,(int fromIndex,int toIndex)->{
                for(int t = fromIndex ; t < toIndex; t++){
                    List<EnterpriseBankAcctVO> enterpriseBankAcctVOList = lists.get(t);
                    CtmJSONObject ctmJSONObject = new CtmJSONObject(params);
                    // 加锁的账号信息
                    String accountInfos = enterpriseBankAcctVOList.stream().map(EnterpriseBankAcctVO::getAccount).reduce((s1,s2) -> s1 + ", " + s2).orElse("");
                    //实时余额，手动和调度任务用一个锁
                    List<String> accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeys(ICmpConstant.QUERYREALBALANCE_AND_TASK_BALANCE_SUPPLEMENT_COMBINE_LOCK,enterpriseBankAcctVOList);
                    // 加锁信息：账号+行为
                    try {
                        CtmLockTool.executeInOneServiceExclusivelyBatchLock( accountInfoLocks,60*60*2L, TimeUnit.SECONDS,(int lockstatus)->{
                        if(lockstatus == LockStatus.GETLOCK_FAIL){
                            //加锁失败添加报错信息 刷新进度+1
                            ProcessUtil.addMessage(uid,String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007BE", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */,accountInfos));
                            ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, enterpriseBankAcctVOList);
                            return ;
                        }
                        queryAccountBalanceForHttp(enterpriseBankAcctVOList, ctmJSONObject, uid, lists.size(), failAccountSet);
                        });
                    } catch (Exception e) {
                        //没用，走不到，异常被吞了
                        log.error("queryAccountBalanceExecute",e);
                        //加锁失败添加报错信息 刷新进度+1
                        ProcessUtil.addMessage(uid,String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B7", "[%s]:此账户操作发生异常:[%s]") /* "[%s]:此账户操作发生异常:[%s]" */,accountInfos,e.getMessage()));
                        ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, enterpriseBankAcctVOList);
                    }
                }
                return null;
            });
        }catch (Exception e ){
            //一般走不到，大部分代码的异常被吞了
            log.error(e.getMessage(),e);
            throw e;
        }finally {
            ////最后需要将进度置为100%
            //ProcessUtil.completedResetCount(uid);
            if(executorService != null){
                executorService.shutdown();
            }
        }
    }

    /**
     * 将集合拆分为多个集合
     * @param sourceList
     * @param groupNum
     * @return
     */
    @Override
    public List<List<EnterpriseBankAcctVO>> groupData(List<EnterpriseBankAcctVO> sourceList, int groupNum) {
        //todo 不按账户分组？？？
        //先按会计主体分组
        Map<String, List<EnterpriseBankAcctVO>> entityMap = new HashMap<>();
        for(EnterpriseBankAcctVO enterpriseBankAcctVO : sourceList){
            if(entityMap.containsKey(enterpriseBankAcctVO.getOrgid())){
                entityMap.get(enterpriseBankAcctVO.getOrgid()).add(enterpriseBankAcctVO);
            }else {
                //这里用线程安全的list 供后续操作
                CopyOnWriteArrayList<EnterpriseBankAcctVO> enterpriseBankAcctVOS = new CopyOnWriteArrayList<>();
                enterpriseBankAcctVOS.add(enterpriseBankAcctVO);
                entityMap.put(enterpriseBankAcctVO.getOrgid(),enterpriseBankAcctVOS);
            }
        }
        List<List<EnterpriseBankAcctVO>> targetList = new CopyOnWriteArrayList<>();
        //循环每个会计主体 若有会计主体下的账户数目大于10 则分组，保证每组的会计主体一致
        Set<String> orgidSet = entityMap.keySet();
        for(String orgid : orgidSet){
            List<EnterpriseBankAcctVO> listNow = entityMap.get(orgid);
            if(listNow.size()>groupNum){
                int size = listNow.size();
                int remainder = size % groupNum;
                int sum = size / groupNum;
                for (int i = 0; i<sum; i++) {
                    List<EnterpriseBankAcctVO> subList;
                    subList = listNow.subList(i * groupNum, (i + 1) * groupNum);
                    targetList.add(subList);
                }
                if (remainder > 0) {
                    List<EnterpriseBankAcctVO> subList;
                    subList = listNow.subList(size - remainder, size);
                    targetList.add(subList);
                }
            }else{
                targetList.add(listNow);
            }
        }
        return targetList;
    }

    /**
     * 如果前端没有选择币种的话，需要将查询到的银行账户按照子表币种拆成多个账户给银企联发送请求
     * @param currency
     * @param netAccountsToHttp
     * @return
     */
    @NotNull
    @Override
    public  List<EnterpriseBankAcctVO> getEnterpriseBankAcctVOS(String currency, List<EnterpriseBankAcctVO> netAccountsToHttp) {
        List<EnterpriseBankAcctVO> accountsOne2One = new ArrayList<>();
        if(org.apache.commons.lang3.StringUtils.isEmpty(currency)){
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : netAccountsToHttp) {
                if(enterpriseBankAcctVO.getCurrencyList().size()>1){
                    for (BankAcctCurrencyVO bankAcctCurrencyVO: enterpriseBankAcctVO.getCurrencyList()) {
                        EnterpriseBankAcctVO bankAcctVO = new EnterpriseBankAcctVO();
                        bankAcctVO.setOrgid(enterpriseBankAcctVO.getOrgid());
                        bankAcctVO.setAccount(enterpriseBankAcctVO.getAccount());
                        bankAcctVO.setAcctName(enterpriseBankAcctVO.getAcctName());
                        bankAcctVO.setId(enterpriseBankAcctVO.getId());
                        bankAcctVO.setBank(enterpriseBankAcctVO.getBank());
                        List<BankAcctCurrencyVO> currencyList = new ArrayList<>();
                        currencyList.add(bankAcctCurrencyVO);
                        bankAcctVO.setCurrencyList(currencyList);
                        accountsOne2One.add(bankAcctVO);
                    }
                }else{
                    accountsOne2One.add(enterpriseBankAcctVO);
                }
            }
        }
        return accountsOne2One;
    }



    /**
     * 查询账户实时余额直连查询
     * @param params
     * @return
     * @throws Exception
     *
    @Override
    public CtmJSONObject queryAccountBalance(CtmJSONObject params) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        responseMsg = queryAccountBalanceExecute(params,responseMsg);
        return responseMsg;
    }

    /**
     * 银行账户实时余额查询，查询企业银行账户和内部户
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryAccountBalanceUnNeedUkey(CtmJSONObject params) throws Exception {
        // 调用公共逻辑处理方法
        AccountBalanceQueryContext context = prepareAccountBalanceQuery(params);
        if (context.listSize == 0) {
            ProcessUtil.completed(context.uid, true);
            return context.responseMsg;
        }
        // 异步执行
        ExecutorService executorServiceAccept =
                ThreadPoolBuilder.buildThreadPoolByYmsParam(ThreadConstant.CMP_REALTIMEBAL_PULL_MANU_ACCEPT);
        Future<?> future = executorServiceAccept.submit(() -> {
            try {
                executeAccountBalanceQuery(context);
            } catch (Exception e) {
                log.error("查询银行账户实时余额失败：", e);
                ProcessUtil.addMessage(context.uid, e.getMessage());
                ProcessUtil.failAccountNumAdd(context.uid, context.listSize);
            }
            ProcessUtil.completed(context.uid, true);
        });
        return context.responseMsg;
    }

    /**
     * 银行账户实时余额查询，查询企业银行账户和内部户（异步版本）
     * @param params
     * @return Future<CtmJSONObject>
     * @throws Exception
     */
    @Override
    public Future<CtmJSONObject> queryAccountBalanceUnNeedUkeyAsync(CtmJSONObject params) throws Exception {
        // 调用公共逻辑处理方法
        AccountBalanceQueryContext context = prepareAccountBalanceQuery(params);
        if (context.listSize == 0) {
            ProcessUtil.completed(context.uid, true);
            // 返回已完成的Future
            CompletableFuture<CtmJSONObject> future = new CompletableFuture<>();
            future.complete(context.responseMsg);
            return future;
        }
        // 异步执行
        Future<?> future = executorServicePool.getThreadPoolExecutor().submit(() -> {
            try {
                executeAccountBalanceQuery(context);
            } catch (Exception e) {
                log.error("查询银行账户实时余额失败：", e);
                ProcessUtil.addMessage(context.uid, e.getMessage());
                ProcessUtil.failAccountNumAdd(context.uid, context.listSize);
            }
            ProcessUtil.completed(context.uid, true);
        });
        // 返回Future对象
        return (Future<CtmJSONObject>) future;
    }

    /**
     * 准备账户余额查询的上下文信息
     * @param params
     * @return
     * @throws Exception
     */
    private AccountBalanceQueryContext prepareAccountBalanceQuery(CtmJSONObject params) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        CtmJSONObject queryParam = new CtmJSONObject();
        Set<String> failAccountSet = ConcurrentHashMap.newKeySet();

        // 查询账户信息
        String banktypeParam = params.getString("banktype");
        List<String> banktypeList = new ArrayList<>();
        if (banktypeParam != null) {
            banktypeList = Arrays.asList(banktypeParam.split(","));
            // 判断前段数据是否为多选
            if (banktypeParam.contains("[")) {
                banktypeList = params.getObject("banktype", List.class);
            }
        }

        if (!StringUtils.isEmpty(banktypeParam)) {
            // 判断查询条件 是不在列表中
            Map<String, String> compareLogicMap = params.getObject("compareLogic", Map.class);
            if (compareLogicMap != null && compareLogicMap.get("banktype") != null) {
                if ("nin".equals(compareLogicMap.get("banktype"))) {
                    // 根据不在列表中条件  查询银行类别
                    QuerySchema schema = QuerySchema.create().addSelect("*");
                    QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                    conditionGroup.appendCondition(QueryCondition.name("id").not_in(banktypeList));
                    schema.addCondition(conditionGroup);
                    List<Map<String, Object>> bankList = MetaDaoHelper.query("bd.bank.BankVO", schema, "ucfbasedoc");
                    List<String> banktypeListNew = new ArrayList<>();
                    if (bankList != null && bankList.size() > 0) {
                        bankList.forEach(map -> {
                            banktypeListNew.add(map.get("id").toString());
                        });
                    }
                    queryParam.put("banktypeList", banktypeListNew);
                } else {
                    queryParam.put("banktypeList", banktypeList);
                }
            } else {
                queryParam.put("banktypeList", banktypeList);
            }
        }

        String currencys = params.getString("currency");
        List<String> currencyList;
        if (!StringUtils.isEmpty(currencys)) {
            currencyList = Arrays.asList(currencys.split(","));
            // 判断前段数据是否为多选
            if (currencys.contains("[")) {
                currencyList = params.getObject("currency", List.class);
            }
            queryParam.put("currencyList", currencyList);
        }

        queryParam.putAll(params);
        // 查询账户信息
        List<EnterpriseBankAcctVO> bankAccounts = queryEnterpriseBankAccountByCondition(queryParam);

        // 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、客户号等信息 并返回
        Map<String, List<EnterpriseBankAcctVO>> bankAcctVOsGroupMap = enterpriseBankQueryService.getBankAcctVOsGroup(bankAccounts);
        // 先查看是否有内部账户 这里存错内部账户信息、内部账户id
        List<EnterpriseBankAcctVO> innerAccounts = bankAcctVOsGroupMap.get(enterpriseBankQueryService.INNER_ACCOUNTS);
        List<List<EnterpriseBankAcctVO>> innerAccountsQuery = groupData(innerAccounts, 10);
        // 构建直联账户信息
        List<EnterpriseBankAcctVO> netAccountsToHttp = bankAcctVOsGroupMap.get(enterpriseBankQueryService.CHECK_SUCCESS);
        // 构建进度条信息
        String uid = params.getString("uid");
        int yqlAccountNum = netAccountsToHttp.stream().mapToInt(account -> account.getCurrencyList() == null ? 0 : account.getCurrencyList().size()).sum();
        int innerAccountNum = innerAccounts.stream().mapToInt(account -> account.getCurrencyList() == null ? 0 : account.getCurrencyList().size()).sum();
        ProcessUtil.initProcessWithAccountNum(uid, yqlAccountNum + innerAccountNum);
        // 币种
        String currency = params.getString("currency");
        // 账户实时余额查询，每次查询上送账号最大数为10个
        List<EnterpriseBankAcctVO> accountsOne2One = getEnterpriseBankAcctVOS(currency, netAccountsToHttp);
        List<List<EnterpriseBankAcctVO>> enterpriseBankAcctList = groupData(org.apache.commons.lang3.StringUtils.isEmpty(currency) ? accountsOne2One : netAccountsToHttp, 10);
        int listSize = innerAccountsQuery.size() + enterpriseBankAcctList.size();

        CtmJSONObject logData = new CtmJSONObject();
        logData.put(IMsgConstant.BILL_DATA, params);
        logData.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B9", "账户实时余额查询账户请求") /* "账户实时余额查询账户请求" */, queryParam);
        logData.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007BA", "账户实时余额查询账户返回") /* "账户实时余额查询账户返回" */, bankAcctVOsGroupMap);
        ctmcmpBusinessLogService.saveBusinessLog(logData, "", "", IServicecodeConstant.RETIBALIST, IMsgConstant.QUERY_ACCREALTIMEBAL, IMsgConstant.QUERY_ACCREALTIMEBAL);

        // 创建上下文对象
        AccountBalanceQueryContext context = new AccountBalanceQueryContext();
        context.responseMsg = responseMsg;
        context.params = params;
        context.innerAccounts = innerAccounts;
        context.innerAccountsQuery = innerAccountsQuery;
        context.netAccountsToHttp = netAccountsToHttp;
        context.enterpriseBankAcctList = enterpriseBankAcctList;
        context.uid = uid;
        context.listSize = listSize;
        context.failAccountSet = failAccountSet;

        return context;
    }

    /**
     * 执行账户实时余额查询的核心逻辑
     * @param context
     * @throws Exception
     */
    private void executeAccountBalanceQuery(AccountBalanceQueryContext context) throws Exception {
        try {
            // 查询内部账户余额
            if (CollectionUtils.isNotEmpty(context.innerAccounts)) {
                excuteQueryInner(context.innerAccountsQuery, context.uid, context.params);
            }
            // 查询直联账户
            // 构建params 用于组装请求参数
            context.params.put("customNo", YQLUtils.getCustomNoFromAccounts(context.netAccountsToHttp));
            context.params.put("operator", null);
            context.params.put("signature", null);
            int batchcount = Integer.parseInt(AppContext.getEnvConfig("cmp.balance.batchcount", "5"));
            int totalTask = (context.listSize % batchcount == 0 ? context.listSize / batchcount : (context.listSize / batchcount) + 1);
            queryAccountBalanceExecute(context.params, context.responseMsg, context.enterpriseBankAcctList, batchcount, totalTask, context.uid);
        } catch (Exception e) {
            log.error("查询银行账户实时余额失败：", e);
            ProcessUtil.addMessage(context.uid, e.getMessage());
            ProcessUtil.failAccountNumAdd(context.uid, context.listSize);
            throw e;
        }
    }

    /**
     * 账户实时余额查询上下文类
     */
    private static class AccountBalanceQueryContext {
        CtmJSONObject responseMsg;
        CtmJSONObject params;
        List<EnterpriseBankAcctVO> innerAccounts;
        List<List<EnterpriseBankAcctVO>> innerAccountsQuery;
        List<EnterpriseBankAcctVO> netAccountsToHttp;
        List<List<EnterpriseBankAcctVO>> enterpriseBankAcctList;
        String uid;
        int listSize;
        Set<String> failAccountSet;
    }

    private void excuteQueryInner(List<List<EnterpriseBankAcctVO>> innerAccountsQuery,String uid,CtmJSONObject params) throws Exception {
        params.put("uid",uid);
        for(List<EnterpriseBankAcctVO> innerlist: innerAccountsQuery){
            List innerAcctId = new ArrayList();
            for(EnterpriseBankAcctVO inner : innerlist){
                innerAcctId.add(inner.getId());
            }
            // 加锁的账号信息
            String accountInfos = innerlist.stream().map(EnterpriseBankAcctVO::getAccount).reduce((s1,s2) -> s1 + ", " + s2).orElse("");
            List<String> accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeys(ICmpConstant.QUERYREALBALANCEKEY, innerlist);
            try {
                CtmLockTool.executeInOneServiceExclusivelyBatchLock(accountInfoLocks, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        //加锁失败添加报错信息 刷新进度+1
                        ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007BE", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */, accountInfos));
                        ProcessUtil.failAccountNumAdd(uid, innerlist.size());
                        return;
                    }
                    //若有则查询内部账户相关信息
                    queryAccountBalance(params, innerAcctId, innerlist, uid);
                });
            } catch (Exception e) {
                //异常被吞了，走不到
                log.error("queryAccountBalanceExecute",e);
                ProcessUtil.failAccountNumAdd(uid, innerlist.size());
                ////加锁失败添加报错信息 刷新进度+1
                //ProcessUtil.addMessage(uid,String.format("[%s]:此账户操作发生异常:[%s]",accountInfos,e.getMessage()));
            }
        }
    }


    @Override
    public CtmJSONObject queryAccountBalance(CtmJSONObject params, List<String> accounts, List<EnterpriseBankAcctVO> bankAccounts, String uid) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        try{
            if(params.getString("tenant_id")!=null && !params.getString("tenant_id").equals(AppContext.getTenantId().toString())){
                //由于有多线程 这个判断说明线程中获得的和线程外获得的不同 此时终止操作
                log.error("===========查询内部账户余额，业务错误信息 tenant_id获取错误，线程外id："+params.getString("tenant_id") + "线程内id："+ AppContext.getTenantId().toString());
                ProcessUtil.failAccountNumAdd(uid, bankAccounts.size());
                return responseMsg;
            }
            AccountQueryVo accountQueryVo = RemoteDubbo.get(IAccountOpenApiService.class,IDomainConstant.MDD_DOMAIN_STCT).
                    queryAccountBalance(null, accounts, AppContext.getYTenantId().toString());
            Result result = accountQueryVo.getResult();
            log.error("===========查询内部账户余额============结果:{}", CtmJSONObject.toJSONString(result));
            if(1 == result.getCode().intValue()){//成功
                Object resultLines = result.getData();
                //跟新相关余额信息
                int allDbCount = 0;
                boolean isEmpty = false;
                if (resultLines != null) {
                    // wangdengk 20230718 后期删除
                    CtmJSONArray resultLines2 = CtmJSONArray.parseArray(CtmJSONObject.toJSONString(resultLines));
                    if (CollectionUtils.isEmpty(resultLines2)) {
                        isEmpty = true;
                    }
                    ProcessUtil.totalPullCountAdd(uid, resultLines2.size());
                    // wangdengk 20230718 后期删除
                    allDbCount = insertAccountBalanceDataInner(resultLines2, bankAccounts);
                    ProcessUtil.newAddCountAdd(uid, allDbCount);

                } else {
                    isEmpty = true;
                }
                if (isEmpty) {
                    ProcessUtil.addInnerNodataMessage(uid, accounts, null, null, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007B8", "请检查[内部账户当日余额查询]节点是否有数据") /* "请检查[内部账户当日余额查询]节点是否有数据" */);
                }
//                if(params.getString("uid")!=null){
////                    ProcessUtil.refreshProcess(params.getString("uid"),true);
//                    ProcessUtil.refreshProcess(params.getString("uid"), true, allDbCount);
//                }
            }else{
                log.error("===========查询内部账户余额，业务错误信息==========");
                log.error(result.getData().toString());
                //拼接内部账户信息 后续用于提示
                StringBuffer accountMessage = new StringBuffer();
                for(EnterpriseBankAcctVO vo : bankAccounts){
                    accountMessage.append(vo.getAccount()+",");
                }
                if(params.getString("uid")!=null){
                    ProcessUtil.failAccountNumAdd(uid, bankAccounts.size());
                    ProcessUtil.addMessage(params.getString("uid"),accountMessage.append(result.getData().toString()).toString());
                }
            }
        }catch(Exception e){
            log.error(e.getMessage());
            ArrayList<String> accountNoList = (ArrayList<String>) bankAccounts.stream().map(t->t.getAcctName()).collect(Collectors.toList());
            //加锁失败添加报错信息 刷新进度+1
            ProcessUtil.addMessage(params.getString("uid"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007C2", "账户[%s]操作发生异常:[%s]") /* "账户[%s]操作发生异常:[%s]" */,accountNoList.toString(),e.getMessage()));
            ProcessUtil.failAccountNumAdd(uid, bankAccounts.size());
        }
        return responseMsg;
    }

    private int insertAccountBalanceDataInner(CtmJSONArray records, List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
        //用于存储需要新增的余额信息
        List<AccountRealtimeBalance> balances = new ArrayList<>();
        //用于存储接口内部真正返回的账户id
        List<String> accIds = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            CtmJSONObject record = records.getJSONObject(i);
            if (!record.containsKey("acctbal")) {
                return 0;
            }
            accIds.add(record.getString("enterpriseBankAccount"));
            AccountRealtimeBalance balance = new AccountRealtimeBalance();
            balance.setTenant(AppContext.getTenantId());
            balance.setAccentity(record.getString(IBussinessConstant.ACCENTITY));
//            balance.setBusiaccbook(accBookTypeId);没有业务账簿
            Map<String, Object> bankAccount2 = QueryBaseDocUtils.queryEnterpriseBankAccountById(record.getString("enterpriseBankAccount"));
            balance.setBanktype(bankAccount2.get("bank").toString());
            balance.setEnterpriseBankAccount(record.getString("enterpriseBankAccount"));
            //循环账户数组 找出与返回数据匹配的信息
            Iterator<EnterpriseBankAcctVO> iterator = bankAccounts.iterator();
            while (iterator.hasNext()) {
                EnterpriseBankAcctVO account = iterator.next();
                if (account.getId().equals(record.getString("enterpriseBankAccount"))) {
                    balance.setBanktype((String)account.getBank());
                    break;
                }
            }
            balance.setCurrency(record.getString("currency"));
            balance.setCashflag(record.getString("cashflag"));
            balance.setYesterbal(!record.containsKey("yesterbal") ? BigDecimal.ZERO : record.getBigDecimal("yesterbal"));
            balance.setAcctbal(!record.containsKey("acctbal") ? BigDecimal.ZERO : record.getBigDecimal("acctbal"));
            balance.setAvlbal(!record.containsKey("avlbal") ? BigDecimal.ZERO : record.getBigDecimal("avlbal"));
            balance.setFrzbal(!record.containsKey("frzbal") ? BigDecimal.ZERO : record.getBigDecimal("frzbal"));
            balance.setRegular_amt(!record.containsKey("regular_amt") ? BigDecimal.ZERO : record.getBigDecimal("regular_amt"));
            //加入合计金额
            balance.setTotal_amt(BigDecimalUtils.safeAdd(balance.getAcctbal(),balance.getRegular_amt()));
            balance.setBalancedate(DateUtils.formatBalanceDate(new Date()));//实时余额新增余额日期字段 默认为系统当天
            balance.setFlag(BalanceFlag.AutoPull.getCode());
            balance.setDatasource(BalanceAccountDataSourceEnum.CURRENTDAY_BAL.getCode());
            balance.setEntityStatus(EntityStatus.Insert);
            balance.setId(ymsOidGenerator.nextId());
            balances.add(balance);
        }
        //如果是自当任务调用 会有多个符合条件的会计主体 此时params中没有accEntity的相关信息 ，只根据账户id进行后续操作
        // start wangdengk 20230713 如果数据库中已经存在直接更新数据库中的余额信息 不存在再插入
        List<AccountRealtimeBalance> existBalances = queryExistRealBalanceData(balances);
        return executeRealGroupData(existBalances, balances);
        // end wangdengk 20230713 如果数据库中已经存在直接更新数据库中的余额信息 不存在再插入
    }
    /**
     *
     * 根据返回的结果查询 已经存在的当天的余额
     * @param balances
     * @return
     */
    public List<AccountRealtimeBalance> queryExistRealBalanceData(List<AccountRealtimeBalance> balances) throws Exception {
        Set<String> orgIds = new HashSet<String>(); // 会计主体集合
        Set<String> accIds = new HashSet<String>(); // 银行账号集合
        Set<String> curIds = new HashSet<String>(); // 币种id集合
        balances.forEach(currBalance -> {
            orgIds.add(currBalance.getAccentity());
            accIds.add(currBalance.getEnterpriseBankAccount());
            curIds.add(currBalance.getCurrency());
        });
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).in(orgIds));
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(accIds));
        conditionGroup.appendCondition(QueryCondition.name("currency").in(curIds));
        conditionGroup.appendCondition(QueryCondition.name("balancedate").in(LocalDateUtil.getNowDateString()));
        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema.addCondition(conditionGroup);
        List<AccountRealtimeBalance>  existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        return existBalances;
    }
    /**
     *
     * 对结果分组 分别执行更新 插入操作
     * @param existBalances
     * @param balances
     * @return
     */

    public int executeRealGroupData(List<AccountRealtimeBalance> existBalances, List<AccountRealtimeBalance> balances) throws Exception {
        List<AccountRealtimeBalance> firstInsertBalances = new ArrayList<>();// 定义首次新增数据集合
        List<AccountRealtimeBalance> insertBalances = new ArrayList<>();// 定义新增数据集合
        List<AccountRealtimeBalance> updateBalances = new ArrayList<>();// 定义更新数据集合
        HashMap<String,AccountRealtimeBalance> groupMap = new HashMap<>();
        if(balances.isEmpty()){ // 如果返回的余额没有值 直接返回
            return 0;
        }
        if(!existBalances.isEmpty()){ // 对已存在的数据遍历根据会计主体+银行账户+币种分组
            existBalances.forEach(existBalance -> {
                StringBuilder keyBuilder = new StringBuilder();
                keyBuilder.append(existBalance.getAccentity()).append("#");
                keyBuilder.append(existBalance.getEnterpriseBankAccount()).append("#");
                keyBuilder.append(existBalance.getCurrency());
                groupMap.put(keyBuilder.toString(),existBalance);
            });
        }
        balances.forEach(currBalance -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(currBalance.getAccentity()).append("#");
            keyBuilder.append(currBalance.getEnterpriseBankAccount()).append("#");
            keyBuilder.append(currBalance.getCurrency());
            if(groupMap.containsKey(keyBuilder.toString())){
                if(StringUtils.isEmpty(currBalance.getFirst_flag())){ // 如果firstFlag为空再更新
                    AccountRealtimeBalance updateBalance =   groupMap.get(keyBuilder.toString());
                    // TODO 使用currBalance 更新updateBalance
                    updateSourceRealBalanceData(updateBalance,currBalance);
                    updateBalances.add(updateBalance);
                }
            }else{
                currBalance.setCreateDate(new Date());
                currBalance.setCreateTime(new Date());
                currBalance.setCreator(AppContext.getCurrentUser().getName());
                currBalance.setCreatorId(AppContext.getCurrentUser().getId());
                insertBalances.add(currBalance);
                AccountRealtimeBalance coppy = (AccountRealtimeBalance) currBalance.clone();
                // 添加创建时间、创建日期
                coppy.setCreateDate(new Date());
                coppy.setCreateTime(new Date());
                coppy.setCreator(AppContext.getCurrentUser().getName());
                coppy.setCreatorId(AppContext.getCurrentUser().getId());
                coppy.setFirst_flag(BalanceFlag.AutoPull.getCode());
                coppy.setId(ymsOidGenerator.nextId());
                firstInsertBalances.add(coppy);
            }
        });
        if(!firstInsertBalances.isEmpty()){
            CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, firstInsertBalances);
        }
        if(!insertBalances.isEmpty()){
            CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, insertBalances);
        }
        if(!updateBalances.isEmpty()){
            MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, updateBalances);
        }
        return insertBalances.size();
    }
    /**
     *
     * 更新旧的实时余额关键字段
     * @param sourceData
     * @param targetData
     * @return
     */
    private void updateSourceRealBalanceData(AccountRealtimeBalance sourceData, AccountRealtimeBalance targetData) {
        sourceData.setCashflag(targetData.getCashflag());
        sourceData.setYesterbal(targetData.getYesterbal());
        sourceData.setAcctbal(targetData.getAcctbal());
        sourceData.setAvlbal(targetData.getAvlbal());
        sourceData.setFrzbal(targetData.getFrzbal());
        sourceData.setModifyDate(new Date());
        sourceData.setModifyTime(new Date());
        //实时余额国际化相关字段
        sourceData.setProj_name(targetData.getProj_name());
        sourceData.setSub_name(targetData.getSub_name());
        sourceData.setBudget_source(targetData.getBudget_source());
        sourceData.setFlag(targetData.getFlag());
        sourceData.setDatasource(BalanceAccountDataSourceEnum.CURRENTDAY_BAL.getCode());
        sourceData.setEntityStatus(EntityStatus.Update);
    }

    /**
     * 根据前端条件查询相应的银行账户
     * @return
     * @throws Exception
     */
    @Override
    public List<EnterpriseBankAcctVO> queryEnterpriseBankAccountByCondition(CtmJSONObject params) throws Exception {

        EnterpriseParams enterpriseParams = new EnterpriseParams();
        // 过滤停用账户
        List<Integer> enables = new ArrayList<>();
        enables.add(1);
        enterpriseParams.setEnables(enables);
        enterpriseParams.setCurrencyEnable(1);
        // start wangdengk CZFW-145775 兼容会计主体从默认业务单元中获取到 传到后台为字符串
        String accEntityParam =  params.getString("accEntity");
        List<String> accentitys = new ArrayList<>();
        List<String> accounts = new ArrayList<>();
        if(accEntityParam!=null){
            accentitys = Arrays.asList(accEntityParam.split(","));
            //判断前段数据是否为多选
            if(accEntityParam.contains("[")){
                accentitys = params.getObject("accEntity", List.class);
            }
        }
        // end wangdengk CZFW-145775 兼容会计主体从默认业务单元中获取到 传到后台为字符串
        //String accEntity = null;
        if(accentitys != null && !accentitys.isEmpty()){
            //accEntity = accentitys.get(0);
            // 根据所选组织查询 有权限的账户
            EnterpriseParams newEnterpriseParams = new EnterpriseParams();
            newEnterpriseParams.setOrgidList(accentitys);
            List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllEnableWithRange(newEnterpriseParams);
            //组织下没有账户时，直接返回空列表；否则用id为空去查，会查到所有的账户
            if (enterpriseBankAcctVOS == null || enterpriseBankAcctVOS.isEmpty()) {
                return new ArrayList<EnterpriseBankAcctVO>();
            }
            for(EnterpriseBankAcctVOWithRange enterpriseBankAcctVO : enterpriseBankAcctVOS){
                accounts.add(enterpriseBankAcctVO.getId());
            }
        }
        // 银行账户不能为空
        List<String> enterBankAccs = params.getObject("enterpriseBankAccount", List.class);
        if(enterBankAccs != null && !enterBankAccs.isEmpty()){
            enterpriseParams.setIdList(enterBankAccs);
        }else {
            enterpriseParams.setIdList(accounts);
        }
        //币种
        List<String> currencyList = params.getObject("currencyList", List.class);
        if(currencyList != null && !currencyList.isEmpty()){
            enterpriseParams.setCurrencyIDList(currencyList);
        }

        // 银行类别
        String banktypeListParam = params.getString("banktypeList");
        List<String> banktypeList = new ArrayList<>();
        if(banktypeListParam!=null){
            banktypeList = Arrays.asList(banktypeListParam.split(","));
            //判断前段数据是否为多选
            if(banktypeListParam.contains("[")){
                banktypeList = params.getObject("banktypeList", List.class);
            }
        }
        List<EnterpriseBankAcctVO> bankAccounts = new ArrayList<>();
        if(banktypeList != null && banktypeList.size() > 0){
            if(banktypeList.size() == 1){
                enterpriseParams.setBank(banktypeList.get(0));
                bankAccounts.addAll(enterpriseBankQueryService.queryAllEnable(enterpriseParams));
            }else {
                for(String bank : banktypeList){
                    enterpriseParams.setBank(bank);
                    bankAccounts.addAll(enterpriseBankQueryService.queryAllEnable(enterpriseParams));
                }
            }
        }else {
            bankAccounts.addAll(enterpriseBankQueryService.queryAllEnable(enterpriseParams));
        }
        // 根据银行账户Id和币种进行过滤
        return filterBankAccountByIdAndCurrencyId(bankAccounts, currencyList);
    }

    /**
     * 根据币种和企业银行账户过滤企业银行账户
     * @param bankAccounts 未去重的企业银行账户集合
     * @param currencyList 前端传入的币种集合
     * @return
     */
    private List<EnterpriseBankAcctVO>  filterBankAccountByIdAndCurrencyId(List<EnterpriseBankAcctVO> bankAccounts, List<String> currencyList) {
        List<EnterpriseBankAcctVO> retAccounts = new ArrayList<>();
        if (CollectionUtils.isEmpty(bankAccounts)) {
            return retAccounts;
        }

        Set<String> idSet = new HashSet<>();
        for (EnterpriseBankAcctVO vo : bankAccounts) {
            if (idSet.contains(vo.getId())) {
                continue;
            }
            idSet.add(vo.getId());
            // 根据前端传入的币种进行过滤
            boolean filterFlag = false;
            if (CollectionUtils.isNotEmpty(currencyList)) {
                List<BankAcctCurrencyVO> filtercurrencyList = new ArrayList<>();
                for (BankAcctCurrencyVO bankAcctCurrencyVO : vo.getCurrencyList()) {
                    if (!currencyList.contains(bankAcctCurrencyVO.getCurrency())) {
                        continue;
                    }
                    // 保证每个银行账户都有币种
                    filterFlag = true;
                    filtercurrencyList.add(bankAcctCurrencyVO);
                }
                vo.setCurrencyList(filtercurrencyList);
                if (filterFlag) {
                    retAccounts.add(vo);
                }
            } else {
                retAccounts.add(vo);
            }
        }
        return retAccounts;
    }

    /**
     * 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
     * @param bankAccounts
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getBankAcctVOsGroup(List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
        //存储网银类型的账户信息
        List<EnterpriseBankAcctVO> netAccounts = new ArrayList<>();
        List<String> netAccountsId = new ArrayList<>();
        //真正请求查询的网易账户信息
        List<EnterpriseBankAcctVO> netAccountsToHttp = new ArrayList<>();
        //先查看是否有内部账户 这里存错内部账户信息、内部账户id
        List<EnterpriseBankAcctVO> innerAccounts = new ArrayList<>();
        for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
            if (bankAccount.getAcctopentype() != null && bankAccount.getAcctopentype().equals(1)) {
                innerAccounts.add(bankAccount);
            } else {//添加非内部账户的信息
                netAccounts.add(bankAccount);
                netAccountsId.add(bankAccount.getId());
            }
        }
        //一个租户只有一个客户号 本系统不做区分
        String customNo = "";
        //如果非内部账户
        if(netAccountsId.size()>0){
            QuerySchema schema = QuerySchema.create().addSelect("accentity,enterpriseBankAccount,customNo");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(netAccountsId));
            conditionGroup.appendCondition(QueryCondition.name("openFlag").eq(true));
            conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
            conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> listBankAccountSetting = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
            if (listBankAccountSetting != null && listBankAccountSetting.size() >0) {
                customNo = listBankAccountSetting.get(0).get("customNo").toString();
                // 过滤掉没有启用银企联的银行账号
                List<String> bankAccountSettingList = new ArrayList<>();
                for (Map<String, Object> bankAccountSetting : listBankAccountSetting) {
                    bankAccountSettingList.add(bankAccountSetting.get("enterpriseBankAccount").toString());
                }
                for (int i = 0; i < netAccounts.size(); i++) {
                    if (bankAccountSettingList.contains(netAccounts.get(i).getId())) {
                        netAccountsToHttp.add(netAccounts.get(i));
                    }
                }
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(INNER_ACCOUNTS,innerAccounts);
        resultMap.put(NET_ACCOUNTS_TO_HTTP,netAccountsToHttp);
        resultMap.put("customNo", customNo);
        return resultMap;
    }

    /**
     * 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
     * @param bankAccounts
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getBankAcctVOsGroupByTask(List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
        //存储网银类型的账户信息
        List<EnterpriseBankAcctVO> netAccounts = new ArrayList<>();
        List<String> netAccountsId = new ArrayList<>();
        //真正请求查询的网易账户信息
        List<EnterpriseBankAcctVO> netAccountsToHttp = new ArrayList<>();
        //先查看是否有内部账户 这里存错内部账户信息、内部账户id
        List<EnterpriseBankAcctVO> innerAccounts = new ArrayList<>();
        for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
            if (bankAccount.getAcctopentype() != null && bankAccount.getAcctopentype().equals(1)) {
                innerAccounts.add(bankAccount);
            } else {//添加非内部账户的信息
                netAccounts.add(bankAccount);
                netAccountsId.add(bankAccount.getId());
            }
        }
        //一个租户只有一个客户号 本系统不做区分
        String customNo = "";
        //如果非内部账户
        if(netAccountsId.size()>0){
            QuerySchema schema = QuerySchema.create().addSelect("accentity,enterpriseBankAccount,customNo");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(netAccountsId));
            conditionGroup.appendCondition(QueryCondition.name("openFlag").eq(true));
            conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
            conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
            //20240430 新增故事 银企联账户的启用日期 需要和当前操作日期比较 如果启用日期小于当亲日期 则过滤掉(无启用日期的账户不过滤)
            QueryConditionGroup groupOr = QueryConditionGroup.or(
                    QueryCondition.name("enableDate").is_null(),
                    QueryCondition.name("enableDate").elt(DateUtils.getCurrentDate(null))
            );
            QueryConditionGroup groupAll  = QueryConditionGroup.and(
                    groupOr,
                    conditionGroup
            );
            schema.addCondition(groupAll);
            List<Map<String, Object>> listBankAccountSetting = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
            if (listBankAccountSetting != null && listBankAccountSetting.size() >0) {
                customNo = listBankAccountSetting.get(0).get("customNo").toString();
                // 过滤掉没有启用银企联的银行账号
                List<String> bankAccountSettingList = new ArrayList<>();
                for (Map<String, Object> bankAccountSetting : listBankAccountSetting) {
                    bankAccountSettingList.add(bankAccountSetting.get("enterpriseBankAccount").toString());
                }
                for (int i = 0; i < netAccounts.size(); i++) {
                    if (bankAccountSettingList.contains(netAccounts.get(i).getId())) {
                        netAccountsToHttp.add(netAccounts.get(i));
                    }
                }
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(INNER_ACCOUNTS,innerAccounts);
        resultMap.put(NET_ACCOUNTS_TO_HTTP,netAccountsToHttp);
        resultMap.put("customNo", customNo);
        return resultMap;
    }

    /*
     * @Description 构建请求流水号
     * @Date 2019/9/12
     * @Param [customNo]
     * @return java.lang.String
     **/
    public String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

    /**
     * 直连账户获取实时余额接口
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject queryRealbalanceBalanceNew(CtmJSONObject params) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        String enterpriseBankAccount = params.getString("enterpriseBankAccount");
        String accentity = params.getString("accentity");//会计主体
        String curr_code = params.getString("currencyCode");//币种编码
        if (org.apache.commons.lang3.StringUtils.isBlank(accentity) || org.apache.commons.lang3.StringUtils.isBlank(enterpriseBankAccount)) {
            responseMsg.put("code", "00001");
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806E2","accentity或者enterpriseBankAccount为空") /* "accentity或者enterpriseBankAccount为空" */);
            return responseMsg;
        }
        List<Map<String, Object>> bankAccountSettinges = queryBankAccountSetting(true, enterpriseBankAccount,
                accentity);// 查询银行信息
        log.info("bankAccountSettinges info");
        if (!CollectionUtils.isEmpty(bankAccountSettinges)) {
            Map<String, Object> bankAccountSetting = bankAccountSettinges.get(0);
            if (null == bankAccountSetting.get("bankId") || bankAccountSetting.get("bankId").toString().equals("")) {// 判断银行账户是否存在
                responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.MessageUtils
                        .getMessage("P_YS_FI_CM_0000026013") /* "会计主体无有效的企业银行账户" */);
                responseMsg.put("code", "00002");
                return responseMsg;
            }
            if (null == bankAccountSetting.get("customNo")
                    || bankAccountSetting.get("customNo").toString().equals("")) {// 判断银行customNo是否存在
                responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.MessageUtils
                        .getMessage("P_YS_FI_CM_0000026099") /* "无法获取企业银行账户相关联的银企联客户号，请在银企联账户设置功能节点维护" */);
                responseMsg.put("code", "00002");
                return responseMsg;
            }
            CtmJSONArray record = new CtmJSONArray();
            CtmJSONObject recordDate = new CtmJSONObject();
            recordDate.put("acct_no", bankAccountSetting.get("account"));
            recordDate.put("acct_name", bankAccountSetting.get("acctName"));
            if (!org.apache.commons.lang3.StringUtils.isBlank(curr_code)) {
                recordDate.put("curr_code", curr_code);
            }
            record.add(recordDate);
            params.put("customNo", bankAccountSetting.get("customNo"));
            CtmJSONObject queryBalanceMsg = buildQueryBalanceMsgTask(params, record);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryBalanceMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", queryBalanceMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            CtmJSONObject result = HttpsUtils.doHttpsPostNew(ITransCodeConstant.QUERY_ACCOUNT_BALANCE, requestData,
                    bankConnectionAdapterContext.getChanPayUri());
            log.error("实时余额请求返回 无ukey==========================》：" + result);
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String service_resp_code = responseHead.getString("service_resp_code");
                if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_ACCOUNT_BALANCE,service_resp_code)) {
                    CtmJSONObject responseData = result.getJSONObject("data");
                    CtmJSONObject responseBody = responseData.getJSONObject("response_body");
                    manufacturingResults(responseBody, enterpriseBankAccount, curr_code);
                    responseData.put("response_body", responseBody);
                    result.put("data", responseData);
                }
            }
            return result;
        } else {
            responseMsg.put("code", "00003");
            responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CB","没有有效的银行账户信息") /* "没有有效的银行账户信息" */);
        }
        return responseMsg;
    }

    /**
     * 给商业汇票做币种转换
     *
     * @param responseBody
     * @throws Exception
     */
    private void manufacturingResults(CtmJSONObject responseBody, String enterpriseBankAccount ,String curr_code) throws Exception {
        CtmJSONArray records = new CtmJSONArray();
        int totalNum = responseBody.getInteger("tot_num");
        if (totalNum < 1) {
            return;
        } else if (totalNum == 1) {
            CtmJSONObject record = responseBody.getJSONObject("record");
            records.add(record);
            responseBody.put("record", records);
            if (!record.containsKey("acct_bal")) {
                return;
            }
            modifyRecord(record, enterpriseBankAccount, curr_code);
        } else {
            CtmJSONArray recordes = responseBody.getJSONArray("record");
            for (int i = 0; i < recordes.size(); i++) {
                CtmJSONObject record = recordes.getJSONObject(i);
                if (!record.containsKey("acct_bal")) {
                    continue;
                }
                modifyRecord(record, enterpriseBankAccount, curr_code);
            }
        }
    }


    /**
     *@Author tongyd
     *@Description 构建查询企业银行账户实时余额查询报文
     *@Date 2019/5/31 14:15
     *@Param [params, bankAccounts]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    public CtmJSONObject buildQueryBalanceMsg(CtmJSONObject params, List<EnterpriseBankAcctVO> bankAccounts) throws Exception {
        CtmJSONObject requestHead = buildRequestHeadNew(ITransCodeConstant.QUERY_ACCOUNT_BALANCE,
                params.getString("operator"),
                params.getString("customNo"),
                params.getString("requestseqno"),
                params.getString("signature"),
                params.getString("channel"));
        CtmJSONObject requestBody = new CtmJSONObject();
        CtmJSONArray record = new CtmJSONArray();
        for (EnterpriseBankAcctVO bankAccount : bankAccounts) {
            List<BankAcctCurrencyVO> currencyList = bankAccount.getCurrencyList();
            //筛选启用币种
            List<BankAcctCurrencyVO> enableCurrencyVOList = getEnableCurrencyVOList(currencyList);
            if (CollectionUtils.isEmpty(enableCurrencyVOList)) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007BB", "账号【%s】无启用状态的币种【%s】，请检查【企业资金账户】设置！") /* "账号【%s】无启用状态的币种【%s】，请检查【企业资金账户】设置！" */, bankAccount.getAccount(), currencyList));
            }else {
                HashMap<String,String> currencyMap = queryCurrencyCode(enableCurrencyVOList);
                for (BankAcctCurrencyVO currencyVO : enableCurrencyVOList) {
                    CtmJSONObject recordDate = new CtmJSONObject();
                    recordDate.put("acct_no", bankAccount.getAccount());
                    recordDate.put("acct_name", bankAccount.getAcctName());
                    recordDate.put("curr_code", currencyMap.get(currencyVO.getCurrency()));// 这里取到的是币种编码
                    record.add(recordDate);
                }
            }
        }
        requestBody.put("record", record);
        CtmJSONObject placeOrderMsg = new CtmJSONObject();
        placeOrderMsg.put("request_head", requestHead);
        placeOrderMsg.put("request_body", requestBody);
        return placeOrderMsg;
    }

    private static @NotNull List<BankAcctCurrencyVO> getEnableCurrencyVOList(List<BankAcctCurrencyVO> currencyList) {
        if (CollectionUtils.isEmpty(currencyList)) {
            return new ArrayList<BankAcctCurrencyVO>();
        }
        return currencyList.stream().filter(currencyVO -> currencyVO.getEnable() == 1).collect(Collectors.toList());
    }

    /**
     * 根据币种主键查询对应的编码
     * 如果缓存中存在的话取缓存 没有的话查询
     * @param currencyList
     * @return
     * @throws Exception
     */
    public HashMap<String, String> queryCurrencyCode(List<BankAcctCurrencyVO> currencyList) throws Exception {
        HashMap<String, String> currencyMap = new HashMap<>();
        List<String> currencyIds = currencyList.stream().map(e -> e.getCurrency()).collect(Collectors.toList());
        List<String> ids = new ArrayList<>();
        for (String currency : currencyIds) {
            String currencyCode = currencyCodeCache.getIfPresent(currency);
            if(org.apache.commons.lang3.StringUtils.isEmpty(currencyCode)){
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
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id,code");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        conditionGroup.appendCondition(QueryCondition.name("dr").eq(0));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        //全系统通用币种code 故这里不拼接租户id
        Map<String, String> map = query.stream().collect(Collectors.toMap(e -> (String)e.get("id"), e -> (String)e.get("code")));
        for (Map.Entry<String,String> entry : map.entrySet()) {
            currencyCodeCache.put(entry.getKey(), entry.getValue());
        }
        currencyMap.putAll(map);
        return currencyMap;
    }

    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建请求报文头
     * @Date 2019/8/19
     * @Param [transCode, customNo, requestseqno, signature]
     **/
    private CtmJSONObject buildRequestHeadNew(String transCode, String operator, String customNo, String requestseqno, String signature,String channel) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", buildRequestSeqNo(customNo));
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", channel);
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", operator);
        requestHead.put("oper_sign", signature);
        requestHead.put("tran_code", transCode);
        return requestHead;
    }

    /**
     * 具体替换
     *
     * @param record
     * @param enterpriseBankAccount
     * @return
     * @throws Exception
     */
    private void modifyRecord(CtmJSONObject record, String enterpriseBankAccount ,String curr_code) throws Exception {
        String currencyCode = record.getString("curr_code");
        if (org.apache.commons.lang3.StringUtils.isBlank(currencyCode)) {
            currencyCode = curr_code;
        }
        String currency = currencyQueryService.getCurrencyByCode(currencyCode);
        if (null != currency) {
            record.put(CURRENCY, currency);
            record.put("enterpriseBankAccount", enterpriseBankAccount);
        } else {
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101412"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001EA", "根据币种编码未获取到币种！") /* "根据币种编码未获取到币种！" */ + "currencyCode  :" + currencyCode);
            throw new CtmException(ErrorMsgUtil.getCurrencyCodeWrongMsg(currencyCode));
        }
    }

    public List<Map<String, Object>> queryBankAccountSetting(boolean isContain, String enterpriseBankAccount,
                                                             String accentity) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(
                " id,tenant,accentity,openFlag,enterpriseBankAccount,enterpriseBankAccount.acctName as acctName,enterpriseBankAccount.name as name,"
                        + "enterpriseBankAccount.account as account, enterpriseBankAccount.bankNumber.bank as bank,"
                        + "customNo,enterpriseBankAccount.id as bankId,enterpriseBankAccount.enable as enable");// 判断银行账户表是否为空故多差一个id
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        if (isContain) {
            if (!org.apache.commons.lang3.StringUtils.isBlank(enterpriseBankAccount)) {
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
            }else {
                // 账户共享之后，有银行账户的情况下，不用在拼接会计主体
                conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
            }
        }
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
        return list;
    }

    /**
     * 给商业汇票做币种转换
     *
     * @param responseBody
     * @throws Exception
     */
    private void manufacturingResultsOld(CtmJSONObject responseBody, String enterpriseBankAccount ,String curr_code) throws Exception {
        CtmJSONArray records = new CtmJSONArray();
        int totalNum = responseBody.getInteger("tot_num");
        if (totalNum < 1) {
            return;
        } else if (totalNum == 1) {
            CtmJSONObject record = responseBody.getJSONObject("record");
            records.add(record);
            responseBody.put("record", records);
            if (!record.containsKey("acct_bal")) {
                return;
            }
            modifyRecordOld(record, enterpriseBankAccount, curr_code);
        } else {
            CtmJSONArray recordes = responseBody.getJSONArray("record");
            for (int i = 0; i < recordes.size(); i++) {
                CtmJSONObject record = recordes.getJSONObject(i);
                if (!record.containsKey("acct_bal")) {
                    continue;
                }
                modifyRecordOld(record, enterpriseBankAccount, curr_code);
            }
        }
    }

    private void modifyRecordOld(CtmJSONObject record, String enterpriseBankAccount ,String curr_code) throws Exception {
        String currencyCode = record.getString("curr_code");
        if (org.apache.commons.lang3.StringUtils.isBlank(currencyCode)) {
            currencyCode = curr_code;
        }
        String currency = currencyQueryService.getCurrencyByCode(currencyCode);
        if (null != currency) {
            record.put(CURRENCY, currency);
            record.put("enterpriseBankAccount", enterpriseBankAccount);
        } else {
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101412"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001EA", "根据币种编码未获取到币种！") /* "根据币种编码未获取到币种！" */ + "currencyCode  :" + currencyCode);
            throw new CtmException(ErrorMsgUtil.getCurrencyCodeWrongMsg(currencyCode));
        }
    }

    /**
     * @Author tongyd
     *
     * @Description 构建查询企业银行账户实时余额查询报文
     *
     * @Date 2019/5/31 14:15
     *
     * @Param [params, bankAccounts]
     *
     * @Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    private CtmJSONObject buildQueryBalanceMsgTask(Map<String, Object> params, CtmJSONArray record) throws Exception {

        String requestseqno = DigitalSignatureUtils.buildRequestNum(params.get("customNo").toString());
        CtmJSONObject requestHead = buildRequestHeadTask(ITransCodeConstant.QUERY_ACCOUNT_BALANCE, params.get("customNo").toString(),
                requestseqno, params);
        CtmJSONObject requestBody = new CtmJSONObject();

        requestBody.put("record", record);
        CtmJSONObject placeOrderMsg = new CtmJSONObject();
        placeOrderMsg.put("request_head", requestHead);
        placeOrderMsg.put("request_body", requestBody);
        return placeOrderMsg;
    }




    /**
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @Author tongyd
     * @Description 构建请求报文头
     * @Date 2019/8/19
     * @Param [transCode, customNo, requestseqno, signature]
     **/
    private CtmJSONObject buildRequestHeadTask(String transCode, String customNo, String requestseqno, Map<String, Object> params) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", requestseqno);
        requestHead.put("cust_no", customNo);
        requestHead.put("cust_chnl", bankConnectionAdapterContext.getChanPayCustomChanel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("tran_code", transCode);
        //国际化专属 外部调用接口 添加curr_code字段
        if (params.get("curr_code") != null) {
            requestHead.put("curr_code", params.get("curr_code"));
        }
        return requestHead;
    }

}
