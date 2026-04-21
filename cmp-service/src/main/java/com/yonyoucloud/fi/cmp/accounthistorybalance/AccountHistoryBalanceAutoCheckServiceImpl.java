package com.yonyoucloud.fi.cmp.accounthistorybalance;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.enums.BalanceFlag;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.paymentbill.service.redis.BankBalanceRedisUtil;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.stct.api.openapi.balancehistory.StctBalanceHistoryApiService;
import cn.hutool.core.thread.BlockPolicy;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountHistoryBalanceAutoCheckServiceImpl implements AccountHistoryBalanceAutoCheckService{
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    StctBalanceHistoryApiService stctBalanceHistoryApiService;
    @Autowired
    BankBalanceRedisUtil bankBalanceRedisUtil;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;


    final Map<String, String> BANK_MAP = new ConcurrentHashMap<>();
    final Map<String, CurrencyTenantDTO> CURRENCY_MAP = new ConcurrentHashMap<>();

    static ExecutorService executorService;
    static{
        // 线程参数 “8,32,1000,cmp-balance-compare-async-” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength, 线程前缀：threadNamePrefix
        String threadParam = AppContext.getEnvConfig("cmp.balance.check.thread.param","8,32,1000,cmp-balance-check-async-");
        String[] threadParamArray = threadParam.split(",");
        int corePoolSize = Integer.parseInt(threadParamArray[0]);
        int maxPoolSize = Integer.parseInt(threadParamArray[1]);
        int queueSize = Integer.parseInt(threadParamArray[2]);
        String threadNamePrefix = threadParamArray[3];
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setDaemon(false)
                .setRejectHandler(new BlockPolicy())
                .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);
    }

    /**
     * @param paramMap 调度任务设置的参数
     * @param logId
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject checkAccountBalance(Map<String, Object> paramMap, String logId) throws Exception {

        String accentitys = (String) (Optional.ofNullable(paramMap.get("accentity")).orElse(""));
        String banktypes = (String) (Optional.ofNullable(paramMap.get("banktype")).orElse(""));
        String currency = (String) (Optional.ofNullable(paramMap.get("currency")).orElse(""));

        long start = System.currentTimeMillis();
        /**
         * 余额弥补步骤：
         * 1，根据会计主体、币种、银行类别，查询历史余额中所有银行账户
         * 2，过滤停用账户
         * 2，根据银行账户分组弥补历史余额
         */
        //1, 根据会计主体、币种、银行类别，查询历史余额中所有银行账户
        QuerySchema schema = QuerySchema.create().addSelect("accentity,enterpriseBankAccount,currency");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").is_not_null());
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accentitys)) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(Arrays.asList(accentitys.split(";"))));
        }
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(banktypes)) {
            conditionGroup.appendCondition(QueryCondition.name("banktype").in(Arrays.asList(banktypes.split(";"))));
        }
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(currency)) {
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        }
        schema.addCondition(conditionGroup);
        schema.distinct();
        List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        if(existBalances != null && existBalances.size() > 0){
            // 2,启用的银行账户
            List<String> enableAccounts = getEnableAccounts(existBalances);

            enableAccounts = DirectmethodCheckUtils.getAccountsByParamMapOfAccountIDList(paramMap, enableAccounts);

            List<String> finalEnableAccounts = getFinalEnableAccounts(paramMap, enableAccounts);
            if (CollectionUtils.isEmpty(finalEnableAccounts)) {
                TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + "没有需要弥补的历史余额数据存在", TaskUtils.UPDATE_TASK_LOG_URL);
            }
            //和实时余额加一个锁
            List<String> accountInfoLocks = BatchLockGetKeysUtils.batchLockCombineKeys(ICmpConstant.QUERYREALBALANCE_AND_TASK_BALANCE_SUPPLEMENT_COMBINE_LOCK, finalEnableAccounts);
            CtmLockTool.executeInOneServiceExclusivelyBatchLock(accountInfoLocks,60*60*2L, TimeUnit.SECONDS,(int lockStatus)->{
                if(lockStatus == LockStatus.GETLOCK_FAIL){
                    TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747","该数据正在处理，请稍后重试！") /* "执行失败" */, TaskUtils.UPDATE_TASK_LOG_URL);
                    return;
                }
                exeSupplementBalance(paramMap, existBalances, finalEnableAccounts, logId);
            });
        }else{
            TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + "没有需要弥补的历史余额数据存在", TaskUtils.UPDATE_TASK_LOG_URL);
        }
        log.error("------------------------------定时任务完成共耗时:"+(System.currentTimeMillis()-start)/1000+"s");
        return null;
    }

    private static @NotNull List<String> getFinalEnableAccounts(Map<String, Object> paramMap, List<String> enableAccounts) throws Exception {
        List<String> finalEnableAccounts = enableAccounts;
        if (CollectionUtils.isEmpty(enableAccounts)) {
            return enableAccounts;
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("enterpriseBankAccount");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").in(enableAccounts));
        group.appendCondition(QueryCondition.name("openFlag").eq("1"));
        querySchema.addCondition(group);
        List<Map<String, Object>> settings = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, querySchema);
        //开通银企联账户
        List<String> openflagAccounts = new ArrayList<>();
        //非直联账户
        List<String> notopenflagAccounts = new ArrayList<>();
        if (settings != null && settings.size() > 0) {
            for (Map<String, Object> map : settings) {
                openflagAccounts.add(map.get("enterpriseBankAccount").toString());
            }
        }
        notopenflagAccounts = enableAccounts.stream()
                .filter(s -> !openflagAccounts.contains(s))
                .collect(Collectors.toList());
        String containDirectAccount = (String) (Optional.ofNullable(paramMap.get("containDirectAccount")).orElse(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E5", "否") /* "否" */));
        log.error("==============删除检查账户【补充调整】的余额============所有启用账户："+ JSONObject.toJSONString(enableAccounts));
        if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E4", "是") /* "是" */.equals(containDirectAccount)) {
            finalEnableAccounts = openflagAccounts;
        } else {
            finalEnableAccounts = notopenflagAccounts;
        }
        return finalEnableAccounts;
    }

    private void exeSupplementBalance(Map<String, Object> paramMap, List<AccountRealtimeBalance> existBalances, List<String> enableAccounts, String logId) throws Exception {
        // 检查账户id集合，用于删除补充调整数据使用
        List<AccountRealtimeBalance> enableAccountRealtimeBalances = new ArrayList<>();
        for(AccountRealtimeBalance accountRealtimeBalance : existBalances){
            if(enableAccounts.contains(accountRealtimeBalance.getEnterpriseBankAccount())){
                enableAccountRealtimeBalances.add(accountRealtimeBalance);
            }
        }
        List<AccountRealtimeBalance> enableRealtimeBalances = removeDisableCurrencyData(enableAccountRealtimeBalances);
        log.error("========历史余额检查调度任务==============步骤一开始==========================");
        log.error("========历史余额检查调度任务======删除检查账户【补充调整】的余额============");
        // 获取删除条件
        Map<String, Object> params = getDeleteParams(paramMap);
        String noInitDataErrorMsg = removeNoInitData(logId, params, enableRealtimeBalances, enableAccounts);
        // 由于没有停用日期概念，只删除启用账户 补充调整的历史余额数据
        log.error("==============删除检查账户【补充调整】的余额============删除条件："+ JSONObject.toJSONString(params));
        // 判断是否包含直联账户，是 删除当日余额生成数据，否  不删除
        if(CollectionUtils.isEmpty(enableAccounts)){
            // 银行账户为空时，不删除任何数据
        } else {
            // 包含直联账户 参数
            String containDirectAccount = (String) (Optional.ofNullable(paramMap.get("containDirectAccount")).orElse(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E5", "否") /* "否" */));
            log.error("==============删除检查账户【补充调整】的余额============所有启用账户："+ JSONObject.toJSONString(enableAccounts));
            if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E4", "是") /* "是" */.equals(containDirectAccount)) {
                //删除直联账户的流水计算数据
                params.put("accounts", enableAccounts);
                log.error("==============删除检查账户【补充调整】的余额============直联账户：" + JSONObject.toJSONString(enableAccounts));
            } else {
                //删除非直联账户的流水计算数据
                log.error("==============删除检查账户【补充调整】的余额============非直联账户：" + JSONObject.toJSONString(enableAccounts));
            }
            //删除账户的流水计算数据
            params.put("accounts", enableAccounts);
            if (!enableAccounts.isEmpty()) {
                //改到线程内删除，否则开启全局事务后，此时还不提交，对线程内不可见
                //SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.BankAccountBalanceMapper.deleteByCheckTaskContainZhiLian",
                //        params);
            }
            //只弥补直联/非直联数据
            enableRealtimeBalances = enableRealtimeBalances.stream()
                    .filter(s -> enableAccounts.contains(s.getEnterpriseBankAccount()))
                    .collect(Collectors.toList());
        }
        log.error("==============删除检查账户【补充调整】的余额============删除结束=====补充余额开始=========");
        paramMap.put(START_DATE, params.get(START_DATE));
        paramMap.put(END_DATE, params.get(END_DATE));
        String errorResult = checkAccountBalanceInThreadPool(enableRealtimeBalances, paramMap);
        //不阻断流程，之前的多线程里有get，会阻塞等任务执行完后，最后回写错误信息；之前不再回写调度任务，收集错误信息之后统一回写
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isNotEmpty(noInitDataErrorMsg) || com.yonyoucloud.fi.cmp.util.StringUtils.isNotEmpty(errorResult)) {
            TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + noInitDataErrorMsg + "\n" + errorResult, TaskUtils.UPDATE_TASK_LOG_URL);
        } else {
            TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        }
        log.error("======================步骤一结束==========================");
    }

    private String removeNoInitData(String logId, Map<String, Object> params, List<AccountRealtimeBalance> enableRealtimeBalances, List<String> enableAccounts) throws Exception {
        if (CollectionUtils.isEmpty(enableRealtimeBalances)) {
            TaskUtils.updateTaskLog((Map<String,String>)params.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */ + "[没有需要操作的数据]", TaskUtils.UPDATE_TASK_LOG_URL);
        }
        List<String> allAccountsId = enableRealtimeBalances.stream().map(accountRealtimeBalance -> {
            return accountRealtimeBalance.getEnterpriseBankAccount();
        }).collect(Collectors.toList());
        String errorMsg = "";
        Date endDate = (Date) params.get(END_DATE);
        //没有id和日期，需要重新查询
        //根据银行账号查询所有历史余额
        QuerySchema schema2 = QuerySchema.create().addSelect("enterpriseBankAccount");
        schema2.distinct();
        QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup2.appendCondition(QueryCondition.name("enterpriseBankAccount").in(allAccountsId));
        conditionGroup2.appendCondition(QueryCondition.name("first_flag").eq("0"));
        conditionGroup2.appendCondition(QueryCondition.name("balancedate").lt(endDate));
        schema2.addCondition(conditionGroup2);
        List<AccountRealtimeBalance> hasInitBalance = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
        List<String> hasInitAccountsId = hasInitBalance.stream().map(accountRealtimeBalance -> accountRealtimeBalance.getEnterpriseBankAccount()).collect(Collectors.toList());
        List<String> noInitAccountsId = allAccountsId.stream().filter(accountId -> !hasInitAccountsId.contains(accountId)).collect(Collectors.toList());

        List<EnterpriseBankAcctVO> VoList = QueryBaseDocUtils.queryEnterpriseBankAccountVOListByIds(noInitAccountsId);
        List<String> noInitDataAccountAccounts = VoList.stream().map(EnterpriseBankAcctVO::getAccount).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(noInitAccountsId)) {
            enableAccounts.removeAll(noInitAccountsId);
            Iterator<AccountRealtimeBalance> iterator = enableRealtimeBalances.iterator();
            while (iterator.hasNext()) {
                AccountRealtimeBalance balance = iterator.next();
                if (balance != null && noInitAccountsId.contains(balance.getEnterpriseBankAccount().toString())) {
                    iterator.remove();
                }
            }
            errorMsg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E8", "账户%s弥补余额失败，根据弥补日期范围未找到期初余额，请先维护日期%s之前的余额！") /* "账户%s弥补余额失败，根据弥补日期范围未找到期初余额，请先维护日期%s之前的余额！" */, noInitDataAccountAccounts.toString(), DateUtils.formatDate(endDate));
        }
        return errorMsg;
    }


    // 获取条件范围内启用的银行账户
    private List<String> getEnableAccounts(List<AccountRealtimeBalance> existBalances) throws Exception {
        /**
         * 1,获取到历史余额中所有银行账户id集合
         * 2，查询全部启用账户
         * 3，遍历出历史余额中所有启用的银行账户
         */
        // 过滤停用账户
        List<String> accounts = new ArrayList<>();
        // 启用银行账户
        List<String> enableAccounts = new ArrayList<>();
        for(AccountRealtimeBalance accountRealtimeBalance :existBalances){
            accounts.add(accountRealtimeBalance.getEnterpriseBankAccount());
        }
       /* EnterpriseParams enterpriseParams = new EnterpriseParams();
        // 过滤非内部账户 CZFW-234750
        enterpriseParams.setAcctopentype(0);
        List<Integer> enables = new ArrayList<>();
        enables.add(1);
        enterpriseParams.setEnables(enables);*/
        // 查询全部 启用状态银行账号
        //List<EnterpriseBankAcctVO> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAll(enterpriseParams);
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("id,orgid,code,name,account,bank,bankNumber,lineNumber,enable,acctType,tenant,acctName,acctopentype");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("enable").eq(1));
        conditionGroup.appendCondition(QueryCondition.name("acctopentype").not_eq(1));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> enterpriseBankAcctVOS = MetaDaoHelper.query(billContext, schema);
        for(Map<String, Object> enterpriseBankAcctVO : enterpriseBankAcctVOS){
            // 只检查存在历史余额的银行账户
            if(accounts.contains(enterpriseBankAcctVO.get("id").toString())){
                enableAccounts.add(enterpriseBankAcctVO.get("id").toString());
            }
        }
        return enableAccounts;
    }

    private Map<String, Object> getDeleteParams(Map<String, Object> paramMap) throws ParseException {
        Boolean useStartDate = (Boolean)paramMap.get("useStartDate");
        String checkRangeStr = (String) (Optional.ofNullable(paramMap.get("CheckRange")).orElse(""));
        Integer preDays = Integer.valueOf(com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(checkRangeStr)? "1" : checkRangeStr);
        String currency = (String) (Optional.ofNullable(paramMap.get("currency")).orElse(""));
        // 弥补当日余额参数
        String supplementTody = (String) (Optional.ofNullable(paramMap.get("supplementTody")).orElse(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E5", "否") /* "否" */));
        Map<String, Object> params = new HashMap<>();
        Date nowDate = DateUtils.formatBalanceDate(new Date());
        if(useStartDate){
            String startdate = (String) (Optional.ofNullable(paramMap.get(START_DATE)).orElse(""));
            String enddateStr = (String) (Optional.ofNullable(paramMap.get(END_DATE)).orElse(""));
            Date endDate = DateUtils.strToDate(enddateStr);
            params.put(START_DATE, DateUtils.strToDate(startdate));
            if(nowDate.equals(endDate)){
                if(!com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E4", "是") /* "是" */.equals(supplementTody)){
                    endDate = DateUtils.dateAdd(nowDate, -1, Boolean.FALSE);
                }
            }
            params.put(END_DATE, endDate);
        }else {
            Date startDate = DateUtils.dateAdd(nowDate, -preDays, Boolean.FALSE);
            Date endDate = DateUtils.dateAdd(nowDate, -1, Boolean.FALSE);
            if(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E4", "是") /* "是" */.equals(supplementTody)){
                endDate = nowDate;
            }
            params.put(START_DATE, startDate);
            params.put(END_DATE, endDate);
        }
        if(StringUtils.isNotEmpty(currency)){
            params.put("currency",currency);
        }
        return params;
    }

    /**
     * 账号检测校验放到线程池执行
     * */
    private String checkAccountBalanceInThreadPool(List<AccountRealtimeBalance> lists,Map<String, Object> paramMap){
        log.error("========检查历史余额缺失补齐checkAccountBalanceInThreadPool=======进入========");
        String logId = (String) paramMap.get("logId");
        try{
            int  batchcount = Integer.parseInt(AppContext.getEnvConfig("cmp.balance.check.batch","100"));
            List<Object> results = ThreadPoolUtil.executeByBatch(executorService,lists,batchcount,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E7", "【调度任务】历史余额自动检测") /* "【调度任务】历史余额自动检测" */,(int fromIndex, int toIndex)->{
                List errorMsgList = new ArrayList<>();
                for(int t = fromIndex ; t < toIndex; t++){
                    List<AccountRealtimeBalance> taskAccountRealtimeBalanceList = lists.subList(fromIndex,toIndex);
                    for(AccountRealtimeBalance accountRealtimeBalance : taskAccountRealtimeBalanceList) {
                        //1,余额检查是否缺失，缺失补齐
                        try{
                            checkDeletion(accountRealtimeBalance,paramMap);
                        }catch (Exception e){
                            String errorMsg = accountRealtimeBalance.getEnterpriseBankAccount() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E9", "余额补齐异常！") /* "余额补齐异常！" */ + e.getMessage();
                            log.error(errorMsg, e);
                            errorMsgList.add(errorMsg);
                        }
                    }
                }
                //只返回错误信息
                return errorMsgList;
            },false);
            if (results.stream().anyMatch(obj -> obj instanceof Collection ? CollectionUtils.isNotEmpty((Collection<?>) obj) : obj != null)) {
                log.error("========检查历史余额缺失补齐checkAccountBalanceInThreadPool=======失败========" + results);
                return results.toString();
            }
        }catch (Exception e ){
            log.error(e.getMessage(),e);
            return e.getMessage();
        }
        //正常返回，没有报错
        String emptyStr = "";
        return emptyStr;
    }

    //检查历史余额缺失，补齐
    private void checkDeletionForDate(AccountRealtimeBalance accountRealtimeBalance, Map<String, Object> paramMap) throws Exception{
        log.error("========检查历史余额缺失补齐checkDeletionForDate=======进入========");
        String startdate = (String) (Optional.ofNullable(paramMap.get(START_DATE)).orElse(""));
        String enddate = (String) (Optional.ofNullable(paramMap.get(END_DATE)).orElse(""));
        log.error("========检查历史余额缺失补齐================入参：startDate:"+startdate+"==lastDay:"+enddate);
        //根据银行账号查询所有历史余额
        QuerySchema schema2 = QuerySchema.create().addSelect(" * ");
        QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup2.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
        conditionGroup2.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema2.addCondition(conditionGroup2);
        schema2.addOrderBy("balancedate");
        List<AccountRealtimeBalance> allBalanceOfAccount = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
        // 将当前银行账户历史余额存入
        Map<Object, BizObject> accountRealtimeBalanceMap = new HashMap<>();
        for(AccountRealtimeBalance accountRealtimeBalance2 : allBalanceOfAccount){
//            accountRealtimeBalanceMap.put(DateUtils.convertToStr(accountRealtimeBalance2.getBalancedate(),"yyyy-MM-dd"), accountRealtimeBalance2);
            // 余额日期+币种作为key，多币种账户的情况
            String balanceKey = DateUtils.convertToStr(accountRealtimeBalance2.getBalancedate(),"yyyy-MM-dd")+accountRealtimeBalance2.getCurrency();
            accountRealtimeBalanceMap.put(balanceKey, accountRealtimeBalance2);
        }
        //2，检查历史余额完整性
        /**
         * 1，根据preDays计算初始日期
         * 2，查询最新确认日期
         * 3，比较初始日期和最新确认日期
         * 4，检查是否有缺失余额
         */
        //根据preDays计算初始日期
        //检查初始日期
        Date startDate = DateUtils.strToDate(startdate);
        Date lastDay = DateUtils.strToDate(enddate);
        Date nowDate = DateUtils.formatBalanceDate(new Date());
        Date preDate = DateUtils.dateAdd(nowDate, -1, Boolean.FALSE);
        if(lastDay.after(preDate)){
            lastDay = preDate;
        }
        //查询最新确认日期
        conditionGroup2.appendCondition(QueryCondition.name("isconfirm").eq(Boolean.TRUE));
        List<AccountRealtimeBalance> confirmedBalanceList = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
        Date confirmDate = null;
        if(confirmedBalanceList != null && confirmedBalanceList.size() > 0){
            confirmDate = confirmedBalanceList.get(confirmedBalanceList.size()-1).getBalancedate();
            //比较初始日期和最新确认日期
            if(!confirmDate.before(startDate)){
                startDate = confirmDate;
            }
        }
        log.error("========检查历史余额缺失补齐================startDate:"+startDate+"==lastDay:"+lastDay);
        supplementBalance(DateUtils.formatBalanceDate(startDate),DateUtils.formatBalanceDate(lastDay),accountRealtimeBalance,accountRealtimeBalanceMap,paramMap);
        log.error("========检查历史余额缺失补齐================startDate:"+startDate+"==lastDay:"+lastDay+"====结束========");
    }

    //检查历史余额缺失，补齐
    private void checkDeletion(AccountRealtimeBalance accountRealtimeBalance, Map<String, Object> paramMap) throws Exception{
        Map<String, Object> params = new HashMap<>();
        params.put(START_DATE, paramMap.get(START_DATE));
        params.put(END_DATE, paramMap.get(END_DATE));
        params.put("currency",accountRealtimeBalance.getCurrency());
        List<String> enableAccounts = Arrays.asList(accountRealtimeBalance.getEnterpriseBankAccount());
        params.put("accounts", enableAccounts);
        //改到线程内删除，否则开启全局事务后，删除对线程内不可见
        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.BankAccountBalanceMapper.deleteByCheckTaskContainZhiLian",
                params);

        //根据银行账号查询所有历史余额
        QuerySchema schema2 = QuerySchema.create().addSelect(" * ");
        QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup2.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
        conditionGroup2.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema2.addCondition(conditionGroup2);
        schema2.addOrderBy("balancedate");
        List<AccountRealtimeBalance> allBalanceOfAccount = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
        // 将当前银行账户历史余额存入
        Map<Object, BizObject> accountRealtimeBalanceMap = new HashMap<>();
        for(AccountRealtimeBalance accountRealtimeBalance2 : allBalanceOfAccount){
            String balanceKey = DateUtils.convertToStr(accountRealtimeBalance2.getBalancedate(),"yyyy-MM-dd")+accountRealtimeBalance2.getCurrency();
            accountRealtimeBalanceMap.put(balanceKey, accountRealtimeBalance2);
        }
        //2，检查历史余额完整性
        /**
         * 1，根据preDays计算初始日期
         * 2，查询最新确认日期
         * 3，比较初始日期和最新确认日期
         * 4，检查是否有缺失余额
         */
        //根据preDays计算初始日期
        //检查初始日期
//        Date startDate = DateUtils.formatBalanceDate(-preDays);
//        Date lastDay = DateUtils.formatBalanceDate(-1);;
        Date startDate = (Date)paramMap.get(START_DATE);
        Date lastDay = (Date)paramMap.get(END_DATE);

        //查询最新确认日期
        conditionGroup2.appendCondition(QueryCondition.name("isconfirm").eq(Boolean.TRUE));
        List<AccountRealtimeBalance> confirmedBalanceList = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
        Date confirmDate = null;
        if(confirmedBalanceList != null && confirmedBalanceList.size() > 0){
            confirmDate = confirmedBalanceList.get(confirmedBalanceList.size()-1).getBalancedate();
            //比较初始日期和最新确认日期
            if(!confirmDate.before(startDate)){
                startDate = confirmDate;
            }
        }
        log.error("===============checkDeletion========startDate:"+startDate+"==lastDay:"+lastDay+"==============");
        supplementBalance(startDate,lastDay,accountRealtimeBalance,accountRealtimeBalanceMap,paramMap);
        log.error("===============supplementBalance========end==============");
    }

    //补充两日期之间缺失的历史余额
    public void supplementBalance(Date startDate,Date lastDay,AccountRealtimeBalance accountRealtimeBalance, Map<Object, BizObject> accountRealtimeBalanceMap, Map<String, Object> paramMap) throws Exception {
        log.error("========supplementBalance================开始1=========");
        //银行账户是否开通银企联服务标识
        Boolean openFlag;
        // 包含直联账户 参数
        String containDirectAccount = (String) (Optional.ofNullable(paramMap.get("containDirectAccount")).orElse(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E5", "否") /* "否" */));
        if(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E4", "是") /* "是" */.equals(containDirectAccount)){
            // 如果参数设置为包含直联账户，则所有账户默认为未开启银企联
            openFlag = Boolean.FALSE;
        }else {
            // 如果参数设置为不包含直联账户，则根据账户是否开通银企联判断是否进行弥补
            openFlag = getOpenFlag(accountRealtimeBalance);
            if(openFlag){
                return;
            }
        }
        log.error("========supplementaryBalance================开始2=========");
        //检查是否有缺失余额记录
        //获取间隔日期天数和间隔日期列表；初始日期和昨日
        List<String> betweenDate = getBetweenDate(new SimpleDateFormat("yyyy-MM-dd").format(startDate),new SimpleDateFormat("yyyy-MM-dd").format(lastDay));
        Set<String> betweenDateSet = new TreeSet<>();
        List<String> betweenDates = new ArrayList<>();
        betweenDateSet.add(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
        betweenDateSet.addAll(betweenDate);
        betweenDateSet.add(new SimpleDateFormat("yyyy-MM-dd").format(lastDay));
        betweenDates.addAll(betweenDateSet);
        int days = daysBetween(startDate, lastDay)+1;// 9.1 -9.30 = 30
        if(days > 0 && betweenDates.size() > 0) {
            //获取间隔余额历史
            QuerySchema schema3 = QuerySchema.create().addSelect(" id,enterpriseBankAccount,balancedate");//
            QueryConditionGroup conditionGroup3 = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup3.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
            conditionGroup3.appendCondition(QueryCondition.name("balancedate").between(startDate, lastDay));
            conditionGroup3.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.get("currency")));
            conditionGroup3.appendCondition(QueryCondition.name("first_flag").eq("0"));
            schema3.addCondition(conditionGroup3);
            schema3.addOrderBy("balancedate");
            // 上次两日期之间得余额历史记录
            List<AccountRealtimeBalance> existBalances3 = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema3, null);// 0
            if (existBalances3 != null && existBalances3.size() > 0) {
                if (existBalances3.size() != days) {
                    for (AccountRealtimeBalance accountRealtimeBalance1 : existBalances3) {
                        String removeDay = DateUtils.convertToStr(accountRealtimeBalance1.getBalancedate(),"yyyy-MM-dd");
                        if (betweenDates.contains(removeDay)) {
                            betweenDates.remove(removeDay);
                        }
                    }
                    // 补缺失余额
                    //判断是否开启银企联服务
                    log.error("========supplementaryBalance================参数:"+JSONObject.toJSONString(betweenDates));
                    supplementaryBalance(openFlag,accountRealtimeBalance,betweenDates,accountRealtimeBalanceMap,paramMap);
                }
            } else {
                //判断是否开启银企联服务
                log.error("========supplementaryBalance================openFlag========="+openFlag);
                supplementaryBalance(openFlag,accountRealtimeBalance,betweenDates,accountRealtimeBalanceMap,paramMap);
            }
        }
    }

    //补充余额
    private void supplementaryBalance(Boolean openFlag
            ,AccountRealtimeBalance accountRealtimeBalance
            ,List<String> betweenDates
            , Map<Object, BizObject> accountRealtimeBalanceMap
            , Map<String, Object> paramMap) throws Exception {
        // 根据参数判断是否弥补银企联账户
        if (openFlag) {
            //拉取对应历史余额
//            CtmJSONObject jsonObject = new CtmJSONObject();
//            jsonObject.put("accentity", accountRealtimeBalance.getAccentity());
//            jsonObject.put("enterpriseBankAccount", accountRealtimeBalance.getEnterpriseBankAccount());
//            ArrayList list = new ArrayList();
//            list.add(betweenDates.get(index));
//            list.add(betweenDates.get(index));
//            jsonObject.put("betweendate", list);
//            syncHistoryAccountBalance(jsonObject);
        } else {
            List<AccountRealtimeBalance> accountRealtimeBalanceList = new ArrayList<>();
            for(int index = 0; index < betweenDates.size();index ++){
                Date nowDate = DateUtils.formatBalanceDate(new SimpleDateFormat("yyyy-MM-dd").parse(betweenDates.get(index)));
                String preDate = DateUtils.formatBalanceDate(DateUtils.dateAdd(nowDate, -1, Boolean.FALSE)).toString();
                String balanceKey = preDate+accountRealtimeBalance.getCurrency();
                // 历史余额中是否存在前一天历史余额
                log.error("========supplementaryBalance================balanceKey:"+balanceKey);
                Boolean isContains = accountRealtimeBalanceMap.containsKey(balanceKey);
                String enterpriseBankAccount = accountRealtimeBalance.getEnterpriseBankAccount();
                if(!BANK_MAP.containsKey(enterpriseBankAccount)){
                    Map<String, Object> bankAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(accountRealtimeBalance.getEnterpriseBankAccount());
                    if(bankAccount.get("bank")!=null){
                        BANK_MAP.put(enterpriseBankAccount,bankAccount.get("bank").toString());
                    }else{
                        BANK_MAP.put(enterpriseBankAccount,StringUtils.EMPTY);
                    }
                }
                if (isContains) {
                    AccountRealtimeBalance insertVO = new AccountRealtimeBalance();
                    insertVO.setAccentity(accountRealtimeBalance.getAccentity());
                    insertVO.setEnterpriseBankAccount(enterpriseBankAccount);
                    insertVO.setBanktype(BANK_MAP.get(enterpriseBankAccount));
                    insertVO.setCurrency(accountRealtimeBalance.getCurrency());
                    //手动补充调整
                    insertVO.setDatasource(BalanceAccountDataSourceEnum.SUPPLEMENTARY_ADJUSTMENTS.getCode());
                    insertVO.setFlag(BalanceFlag.Manually.getCode());
                    insertVO.setBalancedate(nowDate);
                    //当日收入 支出
                    BigDecimal todaySZ = getBankAccountBalanceByDay(insertVO);
                    //历史余额中存在前一日余额
                    AccountRealtimeBalance preAccountBalance = (AccountRealtimeBalance) accountRealtimeBalanceMap.get(balanceKey);
                    BigDecimal preDayAcctbal = preAccountBalance.get("acctbal");
                    //TODO preDayAcctbal添加空校验
//                    if(preDayAcctbal  == null){
//                        抛出具体的错误提示
//                    }
                    //计算当日余额
                    insertVO.setAcctbal(preDayAcctbal.add(todaySZ));
                    //冻结金额
                    insertVO.setFrzbal(preAccountBalance.getFrzbal() == null ? BigDecimal.ZERO : preAccountBalance.getFrzbal());
                    // 可用余额 = 账户余额-冻结金额
                    insertVO.setAvlbal(insertVO.getAcctbal().subtract(insertVO.getFrzbal()));

                    //设置存款余额 setDepositbalance 和 透支余额 setOverdraftbalance
                    if (insertVO.getAcctbal().compareTo(BigDecimal.ZERO) > 0) {
                        insertVO.setDepositbalance(insertVO.getAcctbal().abs());
                        insertVO.setOverdraftbalance(BigDecimal.ZERO);
                    } else if (insertVO.getAcctbal().compareTo(BigDecimal.ZERO) < 0) {
                        insertVO.setDepositbalance(BigDecimal.ZERO);
                        insertVO.setOverdraftbalance(insertVO.getAcctbal().abs());
                    } else {
                        insertVO.setDepositbalance(BigDecimal.ZERO);
                        insertVO.setOverdraftbalance(BigDecimal.ZERO);
                    }
                    insertVO.setEntityStatus(EntityStatus.Insert);
                    insertVO.setId(ymsOidGenerator.nextId());
                    insertVO.setCreateDate(new Date());
                    insertVO.setCreateTime(new Date());
                    insertVO.setCreatorId(Long.valueOf("2591339873333504"));
                    insertVO.setCreator(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006E6", "默认用户") /* "默认用户" */);
                    accountRealtimeBalanceList.add(insertVO);
                    accountRealtimeBalanceMap.put(betweenDates.get(index)+accountRealtimeBalance.getCurrency(), insertVO);
                } else {
                    //历史余额中不存在前一日余额，查询最近日期的历史余额，补充最近日期到昨日缺失的数据
                    QuerySchema schema4 = QuerySchema.create().addSelect(" id,enterpriseBankAccount,balancedate");//
                    QueryConditionGroup conditionGroup4 = new QueryConditionGroup(ConditionOperator.and);
                    conditionGroup4.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.getAccentity()));
                    conditionGroup4.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
                    conditionGroup4.appendCondition(QueryCondition.name("balancedate").between(null, DateUtils.formatBalanceDate(DateUtils.dateAdd(nowDate, -1, Boolean.FALSE))));
                    conditionGroup4.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.get("currency")));
                    conditionGroup4.appendCondition(QueryCondition.name("first_flag").eq("0"));
                    schema4.addCondition(conditionGroup4);
                    //schema4.addOrderBy("balancedate");
                    //默认升序，找的是最小的日期，即最早的日期；应该用降序，找一条就行
                    schema4.addOrderBy(new QueryOrderby("balancedate", "desc"));
                    schema4.setLimitCount(1);
                    List<AccountRealtimeBalance> existBalances4 = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema4, null);
                    log.error("========supplementaryBalance================balanceKey:"+JSONObject.toJSONString(existBalances4));
                    if (existBalances4 != null && existBalances4.size() > 0) {
                        //当前银行账户最近历史余额记录
                        AccountRealtimeBalance preAccountBalance = existBalances4.get(0);
                        log.error("========supplementaryBalance========490========参数:"+preAccountBalance.getBalancedate(),nowDate);
                        supplementBalance(preAccountBalance.getBalancedate(),nowDate,accountRealtimeBalance,accountRealtimeBalanceMap,paramMap);
                    }
                }
            }

            if(accountRealtimeBalanceList.size()>0){
                long s = System.currentTimeMillis();
                 CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, accountRealtimeBalanceList);
                //insertBatch(accountRealtimeBalanceList);
                log.error("======================补充入库"+accountRealtimeBalanceList.size()+"条,耗时"+(System.currentTimeMillis()-s)+"ms");
            }

        }
    }

    /**
     * 去掉停用或没有的币种，不做弥补
     * */
    private List<AccountRealtimeBalance> removeDisableCurrencyData(List<AccountRealtimeBalance> enableAccountRealtimeBalances) throws Exception {
        List<AccountRealtimeBalance> enableRealtimeBalances = new ArrayList<>(enableAccountRealtimeBalances.size());
        List<String> idList = enableAccountRealtimeBalances.stream().map(AccountRealtimeBalance::getEnterpriseBankAccount).collect(Collectors.toList());
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setIdList(idList);
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOs = EnterpriseBankQueryService.queryAll(enterpriseParams);
        Map<String, AccountRealtimeBalance> enableAccountRealtimeBalanceOMap = enableAccountRealtimeBalances.stream().collect(Collectors.toMap(k -> k.getEnterpriseBankAccount() + "_" + k.getCurrency(), v -> v));
        for(EnterpriseBankAcctVO enterpriseBankAcctVO : enterpriseBankAcctVOs){
            for (BankAcctCurrencyVO bankAcctCurrencyVO : enterpriseBankAcctVO.getCurrencyList()) {
                if (bankAcctCurrencyVO.getEnable() == EnterpriseBankQueryService.BANKACCTCURRENCYVO_ENABLE) {
                    AccountRealtimeBalance enableCurrencyRealtimeBalance = enableAccountRealtimeBalanceOMap.get(enterpriseBankAcctVO.getId() + "_" + bankAcctCurrencyVO.getCurrency());
                    // 增加空值判断，避免空指针异常
                    if (enableCurrencyRealtimeBalance != null) {
                        enableRealtimeBalances.add(enableCurrencyRealtimeBalance);
                    }
                } else {
                    log.error("========supplementaryBalance========skipCurrency:{} skipAccount:{}", bankAcctCurrencyVO.getCurrencyName(), enterpriseBankAcctVO.getAccount());
                }
            }
        }
        return enableRealtimeBalances;
    }

    /**
     * 高效率执行批量插入
     * */
    @Transactional(propagation = Propagation.NOT_SUPPORTED,rollbackFor = RuntimeException.class)
    public void insertBatch(List<AccountRealtimeBalance> accountRealtimeBalanceList){
        if (!CollectionUtils.isEmpty(accountRealtimeBalanceList)) {
            SqlSessionTemplate sqlSessionTemplate = AppContext.getSqlSession();
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
                connection = DataSourceUtils.getConnection(dataSource);
                connection.setAutoCommit(false);
                String insertSql = "insert into cmp_bankaccount_realtimebalance(accentity,enterpriseBankAccount,bankType," +
                        "currency,datasource,flag," +
                        "balancedate,acctbal,frzbal," +
                        "avlbal,id,tenant_id,ytenant_id,create_time,create_date,creator,creatorId,depositbalance,overdraftbalance)" +
                        "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                preparedStatement = connection.prepareStatement(insertSql);
                int size = accountRealtimeBalanceList.size();
                String orgid = null;
                String bankType = null;
                if (size > 0) {
                    //当前账户的所属组织
                    String enterpriseBankAccount = accountRealtimeBalanceList.get(0).getEnterpriseBankAccount();
                    Optional<EnterpriseBankAcctVO> enterpriseBankAcctVOOptional = QueryBaseDocUtils.queryEnterpriseBankAccountVOById(enterpriseBankAccount);
                    if (enterpriseBankAcctVOOptional.isPresent()) {
                        orgid = enterpriseBankAcctVOOptional.get().getOrgid();
                        bankType = enterpriseBankAcctVOOptional.get().getBank();
                    }
                }
                int task = 0;
                for (int i = 0; i < size; i++) {
                    AccountRealtimeBalance accountRealtimeBalance = accountRealtimeBalanceList.get(i);
                    String enterpriseBankAccount = accountRealtimeBalance.getEnterpriseBankAccount();
                    //String accentity = accountRealtimeBalance.getAccentity();
                    //改为取当前账户的所属组织
                    String accentity = orgid;
                    //String bankType = accountRealtimeBalance.getBanktype();
                    String currency = accountRealtimeBalance.getCurrency();
                    Short datasource = accountRealtimeBalance.getDatasource();
                    String flag = accountRealtimeBalance.getFlag();
                    Date balancedate = accountRealtimeBalance.getBalancedate();
                    BigDecimal acctbal = accountRealtimeBalance.getAcctbal();
                    BigDecimal frzbal = accountRealtimeBalance.getFrzbal();
                    BigDecimal avlbal = accountRealtimeBalance.getAvlbal();
                    Long id = accountRealtimeBalance.getId();
                    Date createDate = accountRealtimeBalance.getCreateDate();
                    String creator = accountRealtimeBalance.getCreator();
                    Long creatorId = accountRealtimeBalance.getCreatorId();
                    BigDecimal depositbalance = accountRealtimeBalance.getDepositbalance();
                    BigDecimal overdraftbalance = accountRealtimeBalance.getOverdraftbalance();

                    preparedStatement.setString(1, accentity);
                    preparedStatement.setString(2, enterpriseBankAccount);
                    preparedStatement.setString(3, bankType);
                    preparedStatement.setString(4, currency);
                    preparedStatement.setShort(5, datasource);
                    preparedStatement.setString(6, flag);
                    preparedStatement.setDate(7,new java.sql.Date(balancedate.getTime()));
                    preparedStatement.setBigDecimal(8,acctbal);
                    preparedStatement.setBigDecimal(9,frzbal);
                    preparedStatement.setBigDecimal(10,avlbal);
                    preparedStatement.setLong(11, id);
                    preparedStatement.setLong(12, AppContext.getTenantId());
                    preparedStatement.setString(13, AppContext.getYTenantId());
                    preparedStatement.setTimestamp(14, new Timestamp(System.currentTimeMillis()));
                    preparedStatement.setDate(15, new java.sql.Date(createDate.getTime()));
                    preparedStatement.setString(16, creator);
                    preparedStatement.setLong(17, creatorId);
                    preparedStatement.setBigDecimal(18,depositbalance);
                    preparedStatement.setBigDecimal(19,overdraftbalance);
                    //添加批量sql
                    preparedStatement.addBatch();
                    //每400条执行一次，防止内存堆栈溢出
                    if (i > 0 && i % 200 == 0) {
                        log.error("第" + task + "批执行插入任务");
                        preparedStatement.executeBatch();
                        preparedStatement.clearBatch();
                        task++;
                    }
                }
                //最后执行剩余不足200条的
                preparedStatement.executeBatch();
                //执行完手动提交事务
                connection.commit();
                //在把自动提交事务打开
                connection.setAutoCommit(true);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                try{
                    if (null != preparedStatement) {
                        preparedStatement.close();
                    }
                    if (null != connection) {
                        connection.close();
                    }
                }catch (SQLException e){
                    log.error(e.getMessage(),e);
                }
            }
        }
    }


    //银行账户是否开通银企联服务
    private Boolean getOpenFlag(AccountRealtimeBalance accountRealtimeBalance) throws Exception {
        Boolean openFlag = Boolean.FALSE;
        //查询银企联账号 cmp_bankaccountsetting，若此会计主体下有账户开通了银企联 则认为该租户开通了银企联 需要走后续逻辑，如果没记录则直接返回flase不校验
        QuerySchema querySettingSchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup bankAccountGroup = new QueryConditionGroup(ConditionOperator.and);
        bankAccountGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.getEnterpriseBankAccount())));
        bankAccountGroup.addCondition(QueryConditionGroup.and(QueryCondition.name("openFlag").eq(1)));
        querySettingSchema.addCondition(bankAccountGroup);
        List<AutoConfig> settingList = MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, querySettingSchema);
        if(settingList != null && settingList.size() > 0){
            //开通银企联服务
            openFlag = Boolean.TRUE;
        }
        return openFlag;
    }

    //获取银行对账单某一天的净收支
    private BigDecimal getBankAccountBalanceByDay(AccountRealtimeBalance accountRealtimeBalance) throws Exception {
        //获取当日净支付额，查询银行对账单
        QuerySchema schema = QuerySchema.create().addSelect("accentity,serialdealtype,dc_flag,tran_amt");//
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //账户共享后，不区分组织，因为余额的所属组织映射到银行对账单的使用组织[都使用accentity]，但是使用组织可能改为手动选择，会对应不上
        //conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.get("accentity")));
        conditionGroup.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.getCurrency()));
        conditionGroup.appendCondition(QueryCondition.name("bankaccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
        conditionGroup.appendCondition(QueryCondition.name("tran_date").between(accountRealtimeBalance.get("balancedate"), accountRealtimeBalance.get("balancedate")));
        //期初数据不参与余额弥补，只用于对账
        conditionGroup.appendCondition(QueryCondition.name(BankReconciliation.INITFLAG).eq(false));
        schema.addCondition(conditionGroup);
        List<BankReconciliation> existBankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        BankreconciliationUtils.checkAndFilterData(existBankReconciliationList, BankreconciliationScheduleEnum.ACCOUNTHISTORYBALANCECHECK);
        //计算当日净收支额
        BigDecimal nowDayBankMoney = new BigDecimal(0);
        if(existBankReconciliationList != null && existBankReconciliationList.size() > 0){
            // +贷的合计-借的合计
            for(BankReconciliation bankReconciliation : existBankReconciliationList){
                //贷
                if(bankReconciliation.getDc_flag() != null && Direction.Credit.equals(bankReconciliation.getDc_flag())){
                    nowDayBankMoney = nowDayBankMoney.add(bankReconciliation.getTran_amt());
                }else if(bankReconciliation.getDc_flag() != null && Direction.Debit.equals(bankReconciliation.getDc_flag())){
                    //借
                    nowDayBankMoney = nowDayBankMoney.subtract(bankReconciliation.getTran_amt());
                }
            }
        }
        return nowDayBankMoney;
    }

    /**
     * 获取两个日期之间的所有日期
     * @param startTime
     * @param endTime
     * @return
     */
    private List<String> getBetweenDate(String startTime, String endTime){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 声明保存日期集合
        List<String> list = new ArrayList<String>();
        try {
            // 转化成日期类型
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);

            //用Calendar 进行日期比较判断
            Calendar calendar = Calendar.getInstance();
            while (startDate.getTime()<endDate.getTime()){
                // 设置日期
                calendar.setTime(startDate);
                //把日期增加一天
                calendar.add(Calendar.DATE, 1);
                // 获取增加后的日期
                startDate=calendar.getTime();
                // 把日期添加到集合
                list.add(sdf.format(startDate));
            }
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        list.remove(endTime);
        return list;
    }

    /**
     * 获取两个日期间的天数，精确到年月日
     * @param start
     * @param end
     * @return
     */
    private int daysBetween(Date start, Date end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            start = sdf.parse(sdf.format(start));
            end = sdf.parse(sdf.format(end));
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        long startTime = start.getTime();
        long endTime = end.getTime();
        long betweenDays = (endTime - startTime) / (1000 * 3600 * 24);
        return Integer.parseInt(String.valueOf(betweenDays));
    }

}
