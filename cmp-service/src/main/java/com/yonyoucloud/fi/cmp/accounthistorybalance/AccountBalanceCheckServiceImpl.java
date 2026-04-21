package com.yonyoucloud.fi.cmp.accounthistorybalance;

import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.precision.CheckPrecisionVo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationScheduleEnum;
import com.yonyoucloud.fi.cmp.cmpentity.BalanceContrast;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.stct.api.openapi.balancehistory.StctBalanceHistoryApiService;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AccountBalanceCheckServiceImpl implements AccountBalanceCheckService{
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    StctBalanceHistoryApiService stctBalanceHistoryApiService;
    @Autowired
    public IBusinessLogService businessLogService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    final Map<String, CurrencyTenantDTO> CURRENCY_MAP = new ConcurrentHashMap<>();
    @Override
    public CtmJSONObject balanceCheck(Map<String, Object> paramMap) throws Exception {
        Boolean useStartDate = (Boolean)paramMap.get("useStartDate");
        Object checkRange = paramMap.get("CheckRange");
        Integer preDays = 1;
        if (checkRange != null) {
            if (checkRange instanceof String) {
                if (checkRange.toString().length() > 0) {
                    preDays = Integer.valueOf(checkRange.toString());
                }
            } else if (checkRange instanceof Integer) {
                preDays = (Integer) checkRange;
            } else {
                log.error("CheckRange参数类型错误，给默认值1");
            }
        }

        long step1end = System.currentTimeMillis();
        // 检查历史余额是否相等，回写结果
        if(useStartDate){
            int beginDays = 0;
            int endDays = 0;
            String startdateStr = (String) (Optional.ofNullable(paramMap.get("startDate")).orElse(""));
            String enddateStr = (String) (Optional.ofNullable(paramMap.get("endDate")).orElse(""));
            //Date startDate = DateUtils.strToDate(startdateStr);
            Date endDay = DateUtils.strToDate(enddateStr);
            // 今天和昨天
            Date nowDate = DateUtils.formatBalanceDate(new Date());
            Date preDate = DateUtils.dateAdd(nowDate, -1, Boolean.FALSE);
            // 如果结束日期晚于今天，结束日期取昨天
            if(endDay.after(nowDate)){
                endDay = preDate;
                enddateStr = DateUtils.dateToStr(endDay);
            }
            // 校验最早天数
            beginDays = DateUtils.dateBetween(startdateStr, DateUtils.dateToStr(nowDate));
            endDays = DateUtils.dateBetween(enddateStr, DateUtils.dateToStr(nowDate));
            try{
                log.error("======================步骤一开始==========================");
                for (int i = beginDays; i >= endDays; i--) {
                    long s1 = System.currentTimeMillis();
                    acctbalCompare(i,paramMap);
                    log.error("======================步骤一：校验历史余额是否相等并回写结果,第{}天,耗时{}ms",i,(System.currentTimeMillis()-s1));
                }
                log.error("======================步骤一结束,耗时"+(System.currentTimeMillis()-step1end)/1000+"s");
            }catch (Exception e){
                log.error("余额检查异常！", e);
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败")+":"+e.getMessage(), e);
                
            }
        }else {
            try{
                log.error("======================步骤一开始==========================");
                for (int i = preDays; i > 0; i--) {
                    long s1 = System.currentTimeMillis();
                    acctbalCompare(i,paramMap);
                    log.error("======================步骤一：校验历史余额是否相等并回写结果,第{}天,耗时{}ms",i,(System.currentTimeMillis()-s1));
                }
                log.error("======================步骤一结束,耗时"+(System.currentTimeMillis()-step1end)/1000+"s");
            }catch (Exception e){
                log.error("余额检查异常！", e);
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败")+":"+e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * 具体逻辑
     *
     * @param i
     * @return
     * @throws Exception
     */
    private void acctbalCompare(int i, Map<String, Object> paramMap) throws Exception {
        //定义相等
        AtomicInteger failedCount = new AtomicInteger(0);
        //定义不相等
        AtomicInteger successCount = new AtomicInteger(0);
        //前天历史余额键值对
        Map<String, BigDecimal> beforeYesBalancesMap = new ConcurrentHashMap<>();
        Date beforeYesDate = DateUtils.strToDate(DateUtils.dateToStr(DateUtils.dateAddDays(DateUtils.getNow(), ~i)));
        //前天银行历史余额账户map1(银行账户+会计主体+币种,余额)
        List<AccountRealtimeBalance> beforeYesBalances = getRealtimeBalances(beforeYesDate,paramMap);
        Date yesterdayDate = DateUtils.strToDate(DateUtils.dateToStr(DateUtils.dateAddDays(DateUtils.getNow(), ~i + 1)));
        //昨天历史余额
        List<AccountRealtimeBalance> yesterdayBalances = getRealtimeBalances(yesterdayDate,paramMap);
        if (!CollectionUtils.isEmpty(beforeYesBalances)) {
            if (!CollectionUtils.isEmpty(yesterdayBalances)) {
                //解析前天历史余额信息
                getBeforeYes(beforeYesBalances, beforeYesBalancesMap);
                //昨天银行历史余额账户map2(银行账户+会计主体+币种,余额)
                Map<String, BigDecimal> yesterdayMap = new ConcurrentHashMap<>();
                Map<String, Long> idMap = new ConcurrentHashMap<>();
                //解析昨天历史余额信息
                analysisYesBalances(yesterdayBalances, yesterdayMap, idMap);
                //查询银行对账单map3(银行账户+会计主体+币种,计算余额)计算得到数据  前日余额+贷-借
                Map<String, BigDecimal> bankMap = new ConcurrentHashMap<>();
                //昨天银行对账单
                List<BankReconciliation> yesterdayBankReconciliationes = getBankReconciliationes(null,yesterdayDate,null);
                BankreconciliationUtils.checkAndFilterData(yesterdayBankReconciliationes, BankreconciliationScheduleEnum.ACCOUNTBALANCECHECK);
                analysisBank(yesterdayBankReconciliationes, beforeYesBalancesMap, yesterdayMap, bankMap);
                //比较map3与map1是否相等 //根据id，回写计算余额与是否相等
                compareResult(yesterdayBalances, beforeYesBalancesMap, bankMap, idMap, successCount, failedCount);
            }
        } else {
            //前天无历史余额，即初始数据，初始数据检查结果为空
            if (!CollectionUtils.isEmpty(yesterdayBalances)) {
                yesterdayBalances.stream().forEach(e -> {
                    //设置不相等
                    e.setBalancecontrast(null);
                    e.setBalancecheckinstruction(null);
                    failedCount.incrementAndGet();
                });
                CmpMetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, yesterdayBalances);
                //updateBatch(yesterdayBalances);
            }
        }
    }
    /**
     * 查询历史余额数据
     *
     * @param startDate
     * @return
     * @throws Exception
     */
    private List<AccountRealtimeBalance> getRealtimeBalances(Date startDate, Map<String, Object> paramMap) throws Exception {
        String accentitys = (String) (Optional.ofNullable(paramMap.get("accentity")).orElse(""));
        String banktypes = (String) (Optional.ofNullable(paramMap.get("banktype")).orElse(""));
        String currency = (String) (Optional.ofNullable(paramMap.get("currency")).orElse(""));

        QuerySchema querySchema = new QuerySchema().addSelect("id,accentity,enterpriseBankAccount,currency,balancedate,acctbal,isconfirm,datasource");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("balancedate").eq(startDate)); // 等于
        queryConditionGroup.addCondition(QueryCondition.name("first_flag").eq("0"));
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(accentitys)) {
            queryConditionGroup.appendCondition(QueryCondition.name("accentity").in(Arrays.asList(accentitys.split(";"))));
        }
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(banktypes)) {
            queryConditionGroup.appendCondition(QueryCondition.name("banktype").in(Arrays.asList(banktypes.split(";"))));
        }
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(currency)) {
            queryConditionGroup.appendCondition(QueryCondition.name("currency").eq(currency));
        }
        querySchema.addCondition(queryConditionGroup);
        List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, querySchema, null);
        return existBalances;
    }

    /**
     * 比较前天与昨天金额 并回写数据库
     *
     * @param yesterdayBalances 昨日余额
     * @param beforeYesBalancesMap 前日余额
     * @param bankMap 昨日计算余额
     * @param idMap
     * @param successCount
     * @param failedCount
     * @throws Exception
     */
    private void  compareResult(List<AccountRealtimeBalance> yesterdayBalances, Map<String, BigDecimal> beforeYesBalancesMap, Map<String, BigDecimal> bankMap, Map<String, Long> idMap
            , AtomicInteger successCount, AtomicInteger failedCount) throws Exception {
        if (!CollectionUtils.isEmpty(yesterdayBalances)) {
            List<AccountRealtimeBalance> accountRealtimeBalanceList = new CopyOnWriteArrayList<>();
            long s = System.currentTimeMillis();
            if(yesterdayBalances != null && yesterdayBalances.size() > 0){
                compareResultInThreadPool(yesterdayBalances,accountRealtimeBalanceList,beforeYesBalancesMap,bankMap,idMap,successCount,failedCount);
            }
            Long start = System.currentTimeMillis();
            if (!CollectionUtils.isEmpty(accountRealtimeBalanceList)) {
                log.error("------------------------------compareResult处理完所有银行账号,耗时{}ms，开始批量更新",(System.currentTimeMillis()-s));
                //updateBatch(accountRealtimeBalanceList);
                CmpMetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, accountRealtimeBalanceList);
//                EntityTool.setUpdateStatus(accountRealtimeBalanceList);
//                MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, accountRealtimeBalanceList);
//                SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.UpdateCheckFlagMapper.updateRealtimeBalanceCheckFlag", accountRealtimeBalanceList);
                Long end = System.currentTimeMillis();
                log.error("------------------------------批量更新{}条,耗时{}ms",accountRealtimeBalanceList.size(),(end-start));
            }
        }
    }
    private void compareResultInThreadPool(List<AccountRealtimeBalance> lists,List<AccountRealtimeBalance> accountRealtimeBalanceList,Map<String, BigDecimal> beforeYesBalancesMap, Map<String, BigDecimal> bankMap, Map<String, Long> idMap
            , AtomicInteger successCount, AtomicInteger failedCount){
        int  batchcount = Integer.parseInt(AppContext.getEnvConfig("cmp.balance.compare.batch","200"));
        // 线程参数 “8,32,2000” 核心线程数：corePoolSize,最大线程数：maxPoolSize,队列数：queueLength
        String threadParam = AppContext.getEnvConfig("cmp.balance.compare.thread.param","8,32,1000,cmp-balance-compare-async-");
        try{
            String[] threadParamArray = threadParam.split(",");
            int corePoolSize = Integer.parseInt(threadParamArray[0]);
            int maxPoolSize = Integer.parseInt(threadParamArray[1]);
            int queueSize = Integer.parseInt(threadParamArray[2]);
            String threadNamePrefix = threadParamArray[3];
            ExecutorService executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                    .setDaemon(false)
                    .setRejectHandler(new BlockPolicy())
                    .builder(corePoolSize, maxPoolSize, queueSize,threadNamePrefix);

            ThreadPoolUtil.executeByBatch(executorService,lists,batchcount,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007C4", "【调度任务】历史余额自动检测-与昨天余额对比") /* "【调度任务】历史余额自动检测-与昨天余额对比" */,(int fromIndex, int toIndex)->{
                List<AccountRealtimeBalance> taskAccountRealtimeBalanceList = lists.subList(fromIndex,toIndex);
                for(AccountRealtimeBalance accountRealtimeBalance:taskAccountRealtimeBalanceList){
                    asyncCompareOneByOne(accountRealtimeBalance,accountRealtimeBalanceList,beforeYesBalancesMap,bankMap,idMap,successCount,failedCount);
                }
                return null;
            });
        }catch (Exception e ){
            log.error(e.getMessage(),e);
        }
    }
    private void asyncCompareOneByOne(AccountRealtimeBalance e,List<AccountRealtimeBalance> accountRealtimeBalanceList,Map<String, BigDecimal> beforeYesBalancesMap, Map<String, BigDecimal> bankMap, Map<String, Long> idMap, AtomicInteger successCount, AtomicInteger failedCount){
        if(e.getIsconfirm() == null || !e.getIsconfirm()) {
            String keyb = e.getAccentity() + e.getCurrency() + e.getEnterpriseBankAccount();
            AccountRealtimeBalance accountRealtimeBalance = new AccountRealtimeBalance();
            accountRealtimeBalance.setId(idMap.get(keyb));
            accountRealtimeBalance.setCurrency(e.getCurrency());
            Boolean checkAccountFlag = Boolean.FALSE;
            try {
                checkAccountFlag = getCheckAccountBanlanceFlag(e.getAccentity());
            } catch (Exception ex) {
                log.error("检查历史余额与银行对账单余额是否相等，查询银行账户信息失败！");
            }
            //检查历史余额与银行对账单余额是否相等
            if(checkAccountFlag) {
                if (e.getDatasource() != null && BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode() != e.getDatasource()) {
                    QuerySchema querySchema = new QuerySchema().addSelect("id,accentity,serialdealtype,bankaccount,currency,tran_date,tran_time,tran_amt,dc_flag,acct_bal");
                    QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
                    queryConditionGroup.addCondition(QueryCondition.name("tran_date").eq(e.getBalancedate())); // 等于
                    //queryConditionGroup.addCondition(QueryCondition.name("accentity").eq(e.getAccentity()));
                    queryConditionGroup.addCondition(QueryCondition.name("bankaccount").eq(e.getEnterpriseBankAccount()));
                    queryConditionGroup.addCondition(QueryCondition.name("currency").eq(e.getCurrency()));
                    querySchema.addCondition(queryConditionGroup);
                    querySchema.addOrderBy("tran_time");
                    List<BankReconciliation> existBalances = null;
                    try {
                        // long s1 = System.currentTimeMillis();
                        existBalances = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                        BankreconciliationUtils.checkAndFilterData(existBalances, BankreconciliationScheduleEnum.ACCOUNTBALANCECHECK);
                        // log.error("金额比较子任务正处理，tran_date={},accentity={}查询耗时{}ms",e.getBalancedate(),e.getAccentity(),(System.currentTimeMillis()-s1));
                    } catch (Exception ex) {
                        log.error("检查历史余额与银行对账单余额是否相等，查询银行对账单失败！");
                    }
                    if (null != existBalances && existBalances.size() > 0) {
                        int size = existBalances.size();
                        BankReconciliation bankReconciliation = existBalances.get(size - 1);
                        if ((bankReconciliation.getTran_time() != null && bankReconciliation.getAcct_bal() != null)
                                && bankReconciliation.getAcct_bal().compareTo(e.getAcctbal()) != 0) {
                            //设置不相等
                            accountRealtimeBalance.setBalancecontrast(BalanceContrast.Unequal.getValue());
//                                    accountRealtimeBalance.setBalancecheckinstruction(String.format("账户[%s]余额，与当日银行对账单[%s]的余额[%s]不相等，不允许进行余额确认，请核对后进行确认！",
//                                            bankAccount.get("name"), bankReconciliation.getTran_time(), bankReconciliation.getAcct_bal()));
                            try {
                                accountRealtimeBalance.setBalancecheckinstruction(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F537005B00009", "余额[%s]与当日银行对账单中[%s]的余额[%s]不相等，差额[%s];") /* "余额[%s]与当日银行对账单中[%s]的余额[%s]不相等，差额[%s];" */,
                                        dealMoneyDigit(accountRealtimeBalance,e.getAcctbal()),
                                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(bankReconciliation.getTran_time()),
                                        dealMoneyDigit(accountRealtimeBalance,bankReconciliation.getAcct_bal()),
                                        dealMoneyDigit(accountRealtimeBalance,e.getAcctbal().subtract(bankReconciliation.getAcct_bal()).abs())));
                            } catch (Exception ex) {
                                accountRealtimeBalance.setBalancecheckinstruction(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F537005B00009", "余额[%s]与当日银行对账单中[%s]的余额[%s]不相等，差额[%s];") /* "余额[%s]与当日银行对账单中[%s]的余额[%s]不相等，差额[%s];" */,
                                        accountRealtimeBalance,e.getAcctbal(),
                                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(bankReconciliation.getTran_time()),
                                        bankReconciliation.getAcct_bal(),
                                        e.getAcctbal().subtract(bankReconciliation.getAcct_bal()).abs()));
                                log.error("1249检查历史余额与银行对账单余额是否相等，提示信息金额精度格式化失败！");;
                            }
                        }
                    }
                }
            }
            //前天历史余额有该账户  昨天余额=前天余额+/-昨天银行对账单
            if (beforeYesBalancesMap.containsKey(keyb)) {
//                        AccountRealtimeBalance accountRealtimeBalance = new AccountRealtimeBalance();
//                        accountRealtimeBalance.setId(idMap.get(keyb));
                accountRealtimeBalanceList.add(accountRealtimeBalance);
                //昨天计算余额
                if (bankMap.containsKey(keyb)) {
                    accountRealtimeBalance.setAcctbalcount(bankMap.get(keyb));
                    if (e.getAcctbal().compareTo(bankMap.get(keyb)) != 0) {
                        //设置不相等
                        accountRealtimeBalance.setBalancecontrast(BalanceContrast.Unequal.getValue());
                        //昨日余额XXX，当日收入XXX，支出XXX，计算余额XXX与当日余额XXX不相等，差额XXX！
                        try {
                            List<BankReconciliation> bankReconciliations = getBankReconciliationes(e.getEnterpriseBankAccount(),e.getBalancedate(),e.getCurrency());
                            BankreconciliationUtils.checkAndFilterData(bankReconciliations, BankreconciliationScheduleEnum.ACCOUNTBALANCECHECK);
                            BigDecimal creMoney = getBankReconciliationesMoney(bankReconciliations, 1);
                            BigDecimal dreMoney = getBankReconciliationesMoney(bankReconciliations, 2);
                            //昨日余额
////                                    accountRealtimeBalance.setBalancecheckinstruction(MessageUtils.getMessage("P_YS_PF_GZTTMP_0000076392") /* "昨日余额" */ + yestodayBalance + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129039") /* "，当日收入" */
////                                            + dealMoneyDigit(e, creMoney) + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129027") /* "，支出" */ + dealMoneyDigit(e, dreMoney)
////                                            + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129009") /* "，计算余额" */ + dealMoneyDigit(e, e.getAcctbal()) + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385128985") /* "与当日余额" */ + dealMoneyDigit(e, e.getAcctbal()) + MessageUtils.getMessage("P_YS_SD_SDMBF_0000141575") /* "不相等" */ + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129003") /* ",差额：" */ +
////                                            dealMoneyDigit(e, e.getAcctbal().subtract(bankMap.get(keyb)).abs()));
//                                    accountRealtimeBalance.setBalancecheckinstruction(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00186", "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]") /* "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]" */,
//                                            dealMoneyDigit(e, beforeYesBalancesMap.get(keyb)),
//                                            dealMoneyDigit(e, creMoney),
//                                            dealMoneyDigit(e, dreMoney),
//                                            dealMoneyDigit(e, bankMap.get(keyb)),
//                                            dealMoneyDigit(e, e.getAcctbal()),
//                                            dealMoneyDigit(e, e.getAcctbal().subtract(bankMap.get(keyb)).abs())));
                            //todo  bankMap带组织维度，现在应该去掉，以后再改，先重新计算下
                            BigDecimal calMoney = beforeYesBalancesMap.get(keyb).add(creMoney).subtract(dreMoney);
                            //处理精度，否则相同数字不同精度比较结果为不同
                            calMoney = dealMoneyDigit(e, calMoney);
                            e.setAcctbal(dealMoneyDigit(e, e.getAcctbal()));
                            if (e.getAcctbal().compareTo(calMoney) == 0) {
                                accountRealtimeBalance.setBalancecontrast(BalanceContrast.Equal.getValue());
                            }else{
                                StringBuilder balancecheckinstruction = new StringBuilder("");
                                if(StringUtils.isNotEmpty(accountRealtimeBalance.getBalancecheckinstruction())){
                                    balancecheckinstruction = balancecheckinstruction.append(accountRealtimeBalance.getBalancecheckinstruction());
                                }
                                if(balancecheckinstruction.length() > 0){
                                    accountRealtimeBalance.setBalancecheckinstruction(balancecheckinstruction+String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00186", "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]") /* "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]" */,
                                            dealMoneyDigit(e, beforeYesBalancesMap.get(keyb)),
                                            dealMoneyDigit(e, creMoney),
                                            dealMoneyDigit(e, dreMoney),
                                            dealMoneyDigit(e, calMoney),
                                            dealMoneyDigit(e, e.getAcctbal()),
                                            dealMoneyDigit(e, e.getAcctbal().subtract(calMoney))));
                                }else {
                                    accountRealtimeBalance.setBalancecheckinstruction(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00186", "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]") /* "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]" */,
                                            dealMoneyDigit(e, beforeYesBalancesMap.get(keyb)),
                                            dealMoneyDigit(e, creMoney),
                                            dealMoneyDigit(e, dreMoney),
                                            dealMoneyDigit(e, calMoney),
                                            dealMoneyDigit(e, e.getAcctbal()),
                                            dealMoneyDigit(e, e.getAcctbal().subtract(calMoney))));
                                }
                            }
                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                        }

                        failedCount.incrementAndGet();
                    } else {
                        if(StringUtils.isEmpty(accountRealtimeBalance.getBalancecheckinstruction())) {
                            //设置相等
                            accountRealtimeBalance.setBalancecheckinstruction(null);
                            accountRealtimeBalance.setBalancecontrast(BalanceContrast.Equal.getValue());
                            successCount.incrementAndGet();
                        }
                    }
                } else {
                    if (beforeYesBalancesMap.get(keyb).equals(e.getAcctbal())) {
                        if(StringUtils.isEmpty(accountRealtimeBalance.getBalancecheckinstruction())) {
                            //设置相等
                            accountRealtimeBalance.setBalancecheckinstruction(null);
                            accountRealtimeBalance.setBalancecontrast(BalanceContrast.Equal.getValue());
                            successCount.incrementAndGet();
                        }
                    } else {
                        try {
                            List<BankReconciliation> bankReconciliations = getBankReconciliationes(e.getEnterpriseBankAccount(),e.getBalancedate(),e.getCurrency());
                            BankreconciliationUtils.checkAndFilterData(bankReconciliations, BankreconciliationScheduleEnum.ACCOUNTBALANCECHECK);
                            BigDecimal creMoney = getBankReconciliationesMoney(bankReconciliations,1);
                            BigDecimal dreMoney = getBankReconciliationesMoney(bankReconciliations,2);
                            //计算余额余额
                            BigDecimal calculateBalance = dealMoneyDigit(e, beforeYesBalancesMap.get(keyb).add(creMoney).subtract(dreMoney));
                            //处理精度，否则相同数字不同精度比较结果为不同
                            e.setAcctbal(dealMoneyDigit(e, e.getAcctbal()));
                            if(calculateBalance.equals(e.getAcctbal())){
                                if(StringUtils.isEmpty(accountRealtimeBalance.getBalancecheckinstruction())) {
                                    accountRealtimeBalance.setBalancecheckinstruction(null);
                                    accountRealtimeBalance.setBalancecontrast(BalanceContrast.Equal.getValue());
                                    successCount.incrementAndGet();
                                }
                            }else {
                                accountRealtimeBalance.setBalancecontrast(BalanceContrast.Unequal.getValue());
                                StringBuilder balancecheckinstruction = new StringBuilder("");
                                if(StringUtils.isNotEmpty(accountRealtimeBalance.getBalancecheckinstruction())){
                                    balancecheckinstruction = balancecheckinstruction.append(accountRealtimeBalance.getBalancecheckinstruction());
                                }
                                if(balancecheckinstruction.length() > 0){
                                    accountRealtimeBalance.setBalancecheckinstruction(balancecheckinstruction+String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00186", "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]") /* "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]" */,
                                            dealMoneyDigit(e, beforeYesBalancesMap.get(keyb)),
                                            dealMoneyDigit(e,creMoney),
                                            dealMoneyDigit(e,dreMoney),
                                            dealMoneyDigit(e, calculateBalance),
                                            dealMoneyDigit(e, e.getAcctbal()),
                                            dealMoneyDigit(e, calculateBalance.subtract(e.getAcctbal()))));
                                    failedCount.incrementAndGet();
                                }else {
                                    accountRealtimeBalance.setBalancecheckinstruction(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00186", "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]") /* "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]" */,
                                            dealMoneyDigit(e, beforeYesBalancesMap.get(keyb)),
                                            dealMoneyDigit(e,creMoney),
                                            dealMoneyDigit(e,dreMoney),
                                            dealMoneyDigit(e, calculateBalance),
                                            dealMoneyDigit(e, e.getAcctbal()),
                                            dealMoneyDigit(e, calculateBalance.subtract(e.getAcctbal()))));
                                    failedCount.incrementAndGet();
                                }

                            }
                        } catch (Exception ex) {
                            log.error("余额计算失败");
                        }
                        failedCount.incrementAndGet();
                    }
                }
            } else {
                //银行账户第一条数据相等
//                        AccountRealtimeBalance accountRealtimeBalance = new AccountRealtimeBalance();
//                        accountRealtimeBalance.setId(idMap.get(keyb));
                accountRealtimeBalance.setBalancecheckinstruction(null);
                accountRealtimeBalance.setBalancecontrast(null);
                accountRealtimeBalanceList.add(accountRealtimeBalance);
                successCount.incrementAndGet();
//                        failedCount.incrementAndGet();
            }
        }
    }
    /**
     * 查询银行对账单收入和支出
     *
     * @param existBalances
     * @param flag 1 收入，2 支出
     * @return
     * @throws Exception
     */
    private BigDecimal getBankReconciliationesMoney(List<BankReconciliation> existBalances ,int flag) throws Exception {
        BigDecimal targetMoney = BigDecimal.ZERO;
        for (BankReconciliation existBalance : existBalances) {
            if(flag == 1){
                //贷
                if(existBalance.getDc_flag() != null && Direction.Credit.equals(existBalance.getDc_flag())){
                    targetMoney = targetMoney.add(existBalance.getTran_amt());
                }
            }else if(flag == 2){
                if(existBalance.getDc_flag() != null && Direction.Debit.equals(existBalance.getDc_flag())){
                    targetMoney = targetMoney.add(existBalance.getTran_amt());
                }
            }
        }
        return targetMoney;
    }
    //精度处理
    private BigDecimal dealMoneyDigit(AccountRealtimeBalance accountRealtimeBalance, BigDecimal targetMoney) throws Exception {
        //long s = System.currentTimeMillis();
        //精度处理
        CurrencyTenantDTO currencyDTO;
        if(CURRENCY_MAP.containsKey(accountRealtimeBalance.get("currency"))){
            currencyDTO = CURRENCY_MAP.get(accountRealtimeBalance.get("currency"));
        }else {
            currencyDTO = baseRefRpcService.queryCurrencyById(accountRealtimeBalance.get("currency"));
            CURRENCY_MAP.put(accountRealtimeBalance.get("currency"), currencyDTO);
        }
        Integer moneydigit = currencyDTO.getMoneydigit();
        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(accountRealtimeBalance.get("currency"), 1);
        CheckPrecisionVo checkPrecisionVo = new CheckPrecisionVo();
        checkPrecisionVo.setPrecisionId(accountRealtimeBalance.get("currency"));
        BigDecimal decimal =  targetMoney.setScale(moneydigit, moneyRound);
        return decimal;
    }
    //银行账户是否开通银企联服务
    private Boolean getCheckAccountBanlanceFlag(String accentity) throws Exception {
        //Boolean openFlag = Boolean.FALSE;
        //根据会计主体查询配置的现金参数-是否进行历史余额与对账单余额比对参数
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity));
        querySchema1.addCondition(group1);
        List<AutoConfig> configList= MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME,querySchema1,null);
        if (configList == null || configList.size() == 0){
            return Boolean.FALSE;
        }else {
            return configList.get(0).getCheckaccountbanlance();
        }
    }
    /**
     * 解析昨天银行对账单信息并计算账户昨天真实余额
     *
     * @param yesterdayBankReconciliationes
     * @param beforeYesBalancesMap
     * @param yesterdayMap
     * @param bankMap
     */
    private void analysisBank(List<BankReconciliation> yesterdayBankReconciliationes, Map<String, BigDecimal> beforeYesBalancesMap,
                              Map<String, BigDecimal> yesterdayMap, Map<String, BigDecimal> bankMap) {
        if (!CollectionUtils.isEmpty(yesterdayBankReconciliationes)) {
            yesterdayBankReconciliationes.stream().forEach(e -> {
                String keyr = e.getOrgid() + e.getCurrency() + e.getBankaccount();
                //前天历史余额必须有该账户  否则 不相等 无需处理  并且 昨天历史余额必须有该账户
                if (beforeYesBalancesMap.containsKey(keyr) && yesterdayMap.containsKey(keyr)) {
                    BigDecimal tranAmt = e.getTran_amt() == null ? BigDecimal.ZERO : e.getTran_amt();
                    if (!bankMap.containsKey(keyr)) {
                        BigDecimal balanceValue = beforeYesBalancesMap.get(keyr) == null ? BigDecimal.ZERO : beforeYesBalancesMap.get(keyr);
                        if (e.getDc_flag().getValue() == Direction.Debit.getValue()) {
                            tranAmt = balanceValue.subtract(e.getTran_amt());
                        } else if (e.getDc_flag().getValue() == Direction.Credit.getValue()) {
                            tranAmt = balanceValue.add(e.getTran_amt());
                        }
                        bankMap.put(keyr, tranAmt);
                    } else {
                        if (e.getDc_flag().getValue() == Direction.Debit.getValue()) {
                            tranAmt = bankMap.get(keyr).subtract(e.getTran_amt());
                        } else if (e.getDc_flag().getValue() == Direction.Credit.getValue()) {
                            tranAmt = bankMap.get(keyr).add(e.getTran_amt());
                        }
                        bankMap.put(keyr, tranAmt);
                    }
                }
            });
        } else {
            bankMap.putAll(beforeYesBalancesMap);
        }
    }
    /**
     * 查询银行对账单
     *
     * @param startDate
     * @return
     * @throws Exception
     */
    private List<BankReconciliation> getBankReconciliationes(String bankaccount,Date startDate,String currency) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("id,orgid,accentity,serialdealtype,bankaccount,currency,tran_date,tran_amt,dc_flag");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("tran_date").eq(startDate)); // 等于
        if(StringUtils.isNotEmpty(bankaccount)){
            queryConditionGroup.addCondition(QueryCondition.name("bankaccount").eq(bankaccount));
        }
        if(StringUtils.isNotEmpty(currency)){
            queryConditionGroup.addCondition(QueryCondition.name("currency").eq(currency));
        }
        queryConditionGroup.appendCondition(QueryCondition.name(BankReconciliation.INITFLAG).eq(false));
        querySchema.addCondition(queryConditionGroup);
        List<BankReconciliation> existBalances = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return existBalances;
    }
    /**
     * 解析前天历史余额信息
     *
     * @param beforeYesBalances
     * @param startMap
     */
    private void getBeforeYes(List<AccountRealtimeBalance> beforeYesBalances, Map<String, BigDecimal> startMap) {
        beforeYesBalances.stream().forEach(e -> {
            String key = e.getAccentity() + e.getCurrency() + e.getEnterpriseBankAccount();
            if (!startMap.containsKey(key)) {
                startMap.put(key, e.getAcctbal());
            }
        });
    }
    /**
     * 解析昨天历史余额信息
     *
     * @param yesterdayBalances
     * @param yesterdayMap
     * @param idMap
     */
    private void analysisYesBalances(List<AccountRealtimeBalance> yesterdayBalances, Map<String, BigDecimal> yesterdayMap, Map<String, Long> idMap) {
        yesterdayBalances.stream().forEach(e -> {
            String key = e.getAccentity() + e.getCurrency() + e.getEnterpriseBankAccount();
            if (!yesterdayMap.containsKey(key)) {
                yesterdayMap.put(key, e.getAcctbal());
                idMap.put(key, e.getId());
            }
        });
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED,rollbackFor = RuntimeException.class)
    public void updateBatch(List<AccountRealtimeBalance> accountRealtimeBalanceList){

        if (!CollectionUtils.isEmpty(accountRealtimeBalanceList)) {
            SqlSessionTemplate sqlSessionTemplate = AppContext.getSqlSession();
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            try {
                DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
                connection = DataSourceUtils.getConnection(dataSource);
                connection.setAutoCommit(false);
                String updateSql = "update cmp_bankaccount_realtimebalance set balancecheckinstruction = ?,balancecontrast=? where id=?";
                preparedStatement = connection.prepareStatement(updateSql);
                int size = accountRealtimeBalanceList.size();
                int task = 0;
                for (int i = 0; i < size; i++) {
                    AccountRealtimeBalance accountRealtimeBalance = accountRealtimeBalanceList.get(i);
                    String balancecheckinstruction = accountRealtimeBalance.getBalancecheckinstruction();
                    Short balancecontrast = accountRealtimeBalance.getBalancecontrast();
                    Long id = accountRealtimeBalance.getId();
                    //log.error("第" + i + "条，sql参数为：balancecheckinstruction={}，balancecontrast={},id={}", balancecheckinstruction, balancecontrast, id);
                    preparedStatement.setString(1, balancecheckinstruction);
                    if (balancecontrast == null) {
                        preparedStatement.setNull(2, Types.INTEGER);
                    }else{
                        preparedStatement.setShort(2, balancecontrast);
                    }
                    preparedStatement.setLong(3, id);
                    //添加批量sql
                    preparedStatement.addBatch();
                    //每400条执行一次，防止内存堆栈溢出
                    if (i > 0 && i % 400 == 0) {
                        //log.error("批量更新拆批，第" + task + "批执行");
                        preparedStatement.executeBatch();
                        preparedStatement.clearBatch();
                        task++;
                    }
                }
                //最后执行剩余不足400条的
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
}
