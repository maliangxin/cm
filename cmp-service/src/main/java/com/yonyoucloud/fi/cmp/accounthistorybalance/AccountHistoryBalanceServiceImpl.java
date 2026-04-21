package com.yonyoucloud.fi.cmp.accounthistorybalance;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.precision.CheckPrecisionVo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSetting;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cashhttp.CashHttpBankEnterpriseLinkVo;
import com.yonyoucloud.fi.cmp.cmpentity.BalanceContrast;
import com.yonyoucloud.fi.cmp.cmpentity.MoneyForm;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.BalanceAccountDataSourceEnum;
import com.yonyoucloud.fi.cmp.enums.BalanceFlag;
import com.yonyoucloud.fi.cmp.https.service.HttpsService;
import com.yonyoucloud.fi.cmp.https.utils.HttpServiceInforamtionUtils;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.Constant.ThreadConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.stct.api.openapi.balancehistory.StctBalanceHistoryApiService;
import com.yonyoucloud.fi.stct.api.openapi.balancehistory.dto.BalanceHistoryParamDTO;
import com.yonyoucloud.fi.stct.api.openapi.balancehistory.vo.BalanceHistoryVO;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.biz.base.BizContext;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class AccountHistoryBalanceServiceImpl implements AccountHistoryBalanceService {


    @Autowired
    CurrencyQueryService currencyQueryService;
    //TODO 需要进行调整
    static long locktime = 20L;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    private static String YONSUITE_AUTOTASK = "Yonsuite_AutoTask";
    private static String AUTO_PAY_IDEN = "Y";
    private static final Cache<String, Map<String, Object>> currencyCache = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).concurrencyLevel(4).maximumSize(1000).softValues().build();
    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    StctBalanceHistoryApiService stctBalanceHistoryApiService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BankAccountSettingService bankAccountSettingService;
    static int batchcount = 10;

    @Override
    public CtmJSONObject queryAccountBalance(CtmJSONObject params, List<String> accounts, List<Map<String, Object>> bankAccounts) throws Exception {
        return null;
    }

    @Autowired
    public IBusinessLogService businessLogService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    BankDealDetailService bankDealDetailService;
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;
    @Autowired
    private HttpsService httpsService;

    /**
     * 历史余额入库
     *
     * @param balance
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject saveAccountBalance(AccountRealtimeBalance balance) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        //对账号直连属性做判断，如果为直连账号则报错提示 否则继续执行
        String enterpriseBankAccount = balance.getEnterpriseBankAccount();
        Map<String, Object> bankAccount2 = QueryBaseDocUtils.queryEnterpriseBankAccountById(enterpriseBankAccount);
        balance.setBanktype(bankAccount2.get("bank").toString());
        //余额日期必须是系统当前时间之前
        Date balancedate = balance.getBalancedate();
        balance.setFlag(BalanceFlag.Manually.getCode());
        //int res = date1.compareTo(date2)，相等则返回0，date1大返回1，否则返回-1。
        Date now = DateUtils.getNowDateShort2();
        if (balancedate.compareTo(now) >= 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100445"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418027D", "录入的银行账户余额日期必须在系统当前日期之前!") /* "录入的银行账户余额日期必须在系统当前日期之前!" */);
        }
        BigDecimal acctbal = balance.getAcctbal();
        BigDecimal avlbal = balance.getAvlbal();
        BigDecimal frzbal = balance.getFrzbal();
        if (BigDecimal.ZERO.compareTo(acctbal) > 0 || BigDecimal.ZERO.compareTo(avlbal) > 0 || BigDecimal.ZERO.compareTo(frzbal) > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100446"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180281", "录入金额值不得为负数") /* "录入金额值不得为负数" */);
        }
        //账户余额=可用金额+冻结金额
        BigDecimal total = BigDecimalUtils.safeAdd(avlbal, frzbal);
        if (BigDecimal.ZERO.compareTo(BigDecimalUtils.safeSubtract(acctbal, total)) != 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100447"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180285", "录入数据的账户余额需要等于可用金额与冻结金额合计值") /* "录入数据的账户余额需要等于可用金额与冻结金额合计值" */);
        }
        //通过账号，币种，余额日期，会计主体四个属性字段判断是新增还是修改
        balance.setEntityStatus(EntityStatus.Insert);
        balance.setId(ymsOidGenerator.nextId());

        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //如果是自当任务调用 会有多个符合条件的会计主体 此时params中没有accEntity的相关信息 ，只根据账户id进行后续操作
        if (balance.getAccentity() != null) {
            conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(balance.getAccentity()));
        }
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        conditionGroup.appendCondition(QueryCondition.name("currency").eq(balance.getCurrency()));
        conditionGroup.appendCondition(QueryCondition.name("balancedate").eq(balancedate));
        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema.addCondition(conditionGroup);
        List<AccountRealtimeBalance> existBalances = null;
        existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        if (existBalances != null && existBalances.size() > 0) {
            MetaDaoHelper.delete(AccountRealtimeBalance.ENTITY_NAME, existBalances);
        }
        CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, balance);
        return responseMsg;
    }

    /**
     * 内部户历史余额同步
     *
     * @param
     * @throws Exception
     */
    @Override
    public void syncHistoryInnerAccountBalance(Date startDate, Date endDate, List<EnterpriseBankAcctVO> innerBankAccounts, String uid) throws Exception {
        for (EnterpriseBankAcctVO enterpriseBankAcctVO : innerBankAccounts) {
            Set<String> deleteAccountIds = new LinkedHashSet<>();
            Set<AccountRealtimeBalance> saveBalances = new LinkedHashSet<>();
            BalanceHistoryParamDTO dto = new BalanceHistoryParamDTO();
            dto.setSourceAccountId(enterpriseBankAcctVO.getId());
            dto.setStartDate(startDate);
            dto.setEndDate(endDate);
            List<String> deleteInnerAccountIds = new ArrayList<>();
            List<String> deleteInnerCurrencyIds = new ArrayList<>();
            // 加锁的账号信息
            String accountInfo = enterpriseBankAcctVO.getId() + "|" + startDate;
            // 加锁信息：账号+行为
            String lockKey = accountInfo + ICmpConstant.QUERYHISBALANCEKEY;
            try {
                CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        //加锁失败添加报错信息 刷新进度+1
                        ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400727", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */, accountInfo));
                        ProcessUtil.failAccountNumAddOne(uid);
                        return;
                    }
                    // 内部户历史余额同步处理
                    queryInnerAccountHistoryBalance(uid, enterpriseBankAcctVO, dto, deleteInnerAccountIds, deleteInnerCurrencyIds, deleteAccountIds, saveBalances);
                });
            } catch (Exception e) {
                log.error("syncHistoryInnerAccountBalance", e);
                ProcessUtil.failAccountNumAddOne(uid);
                //加锁失败添加报错信息 刷新进度+1
                ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400728", "[%s]:此账户操作发生异常") /* "[%s]:此账户操作发生异常" */, accountInfo));
            }
            // 删除&新增操作
            deleteAndSave(deleteAccountIds, saveBalances);
        }
    }

    /**
     * 内部户历史余额同步处理
     */
    private void queryInnerAccountHistoryBalance(String uid, EnterpriseBankAcctVO enterpriseBankAcctVO, BalanceHistoryParamDTO dto, List<String> deleteInnerAccountIds, List<String> deleteInnerCurrencyIds, Set<String> deleteAccountIds, Set<AccountRealtimeBalance> saveBalances) throws Exception {
        log.error("===========查询内部账户余额============参数:{}", dto);
        List<BalanceHistoryVO> list = stctBalanceHistoryApiService.queryBalanceHistoryList(dto);
        log.error("===========查询内部账户余额============结果:{}", list);
        if (list.isEmpty()) {
            ProcessUtil.addInnerNodataMessage(uid,  Arrays.asList(enterpriseBankAcctVO.getId()), dto.getStartDate(), dto.getEndDate(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540072D", "请检查[内部账户历史余额查询]节点是否有数据") /* "请检查[内部账户历史余额查询]节点是否有数据" */);
            return;
        }
        ProcessUtil.totalPullCountAdd(uid, list.size());
        int addNum = 0;
        for (BalanceHistoryVO balanceHistoryVO : list) {
            // 根据余额四要素，删除重复历史余额数据
            deleteInnerAccountIds.add(enterpriseBankAcctVO.getId());
            deleteInnerCurrencyIds.add(balanceHistoryVO.getCurrency());

            AccountRealtimeBalance balance = new AccountRealtimeBalance();
            balance.setAccentity(enterpriseBankAcctVO.getOrgid());
            balance.setBalancedate(balanceHistoryVO.getSettlementDate());
            balance.setTenant(AppContext.getTenantId());
            // 账户余额
            balance.setAcctbal(balanceHistoryVO.getCurrentBalance() == null ? BigDecimal.ZERO : balanceHistoryVO.getCurrentBalance());
            if (balanceHistoryVO.getAvailableBalance().compareTo(BigDecimal.ZERO) > 0) {
                balance.setDepositbalance(balanceHistoryVO.getAvailableBalance().abs());
                balance.setOverdraftbalance(BigDecimal.ZERO);
                balance.setAvlbal(balanceHistoryVO.getAvailableBalance().abs());
            } else if (balanceHistoryVO.getAvailableBalance().compareTo(BigDecimal.ZERO) < 0) {
                balance.setDepositbalance(BigDecimal.ZERO);
                balance.setOverdraftbalance(balanceHistoryVO.getAvailableBalance().abs());
                balance.setAvlbal(balanceHistoryVO.getAvailableBalance().abs());
            } else {
                balance.setDepositbalance(BigDecimal.ZERO);
                balance.setOverdraftbalance(BigDecimal.ZERO);
                balance.setAvlbal(BigDecimal.ZERO);
            }
            // 冻结金额&可用余额
            balance.setFrzbal(BigDecimal.ZERO);
            //balance.setAvlbal(balance.getAcctbal() == null ? BigDecimal.ZERO : balance.getAcctbal());
            balance.setFlag(BalanceFlag.AutoPull.getCode());
            balance.setEntityStatus(EntityStatus.Insert);
            balance.setCreateTime(new Date());
            balance.setCreateDate(DateUtils.getNowDateShort2());
            balance.setCreator(AppContext.getCurrentUser().getName());//新增人名称
            balance.setCreatorId(AppContext.getCurrentUser().getId());//新增人id
            balance.setId(ymsOidGenerator.nextId());
            balance.setDatasource(BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode());
            balance.setEnterpriseBankAccount(enterpriseBankAcctVO.getId());
            balance.setBanktype(enterpriseBankAcctVO.getBank());
            balance.setCurrency(balanceHistoryVO.getCurrency());
            // 添加创建时间、创建日期
            balance.setCreateDate(new Date());
            balance.setCreateTime(new Date());
            // 内部账户暂不涉及冻结金额，无需记录第一次拉取
            balance.setFirst_flag("0");
            balance.setRegular_amt(BigDecimal.ZERO);
            //加入合计金额
            balance.setTotal_amt(BigDecimalUtils.safeAdd(balance.getAcctbal(), balance.getRegular_amt()));
            // 历史余额删除后新增判断逻辑
            boolean isNewAdd = befroeDeleteAndSave(balance, deleteAccountIds, saveBalances, addNum);
            if (isNewAdd) {
                ProcessUtil.newAddCountAdd(uid, 1);
            }
//                insertList.add(balance);
        }
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//            deleteList.addAll(deleteAccountBalanceList(enterpriseBankAcctVO.getOrgid(), deleteInnerCurrencyIds, deleteInnerAccountIds, sdf.format(startDate),sdf.format(endDate)));
//            ProcessUtil.refreshProcess(uid, true, saveBalances.size());
    }

    @Override
    public CtmJSONObject confirmAccountBalance(List<AccountRealtimeBalance> billList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (billList == null || billList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105065"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00180", "请选择单据！") /* "请选择单据！" */);
        }
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        List<AccountRealtimeBalance> successList = new ArrayList<>();
        //如果是批量操作，检查小于当前余额日期的历史余额是否是否勾选
        Map<String, AccountRealtimeBalance> accountDateMap = new HashMap<>();
        if (billList.size() > 1) {
            billList.sort((a, b) -> a.getBalancedate().compareTo(b.getBalancedate()));
            for (AccountRealtimeBalance accountRealtimeBalance : billList) {
                if (accountRealtimeBalance.getBalancecontrast() == null ||
                        accountRealtimeBalance.getBalancecontrast() == BalanceContrast.Equal.getValue()) {
                    accountDateMap.put(accountRealtimeBalance.getEnterpriseBankAccount() + "" + new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate()), accountRealtimeBalance);
                }
            }
        }
        for (AccountRealtimeBalance accountRealtimeBalance : billList) {
            //加锁
            String lockkey = ICmpConstant.MY_BILL_CLAIM_LIST + accountRealtimeBalance.get("id");
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(lockkey);
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100448"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00185", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
            }
            try {
                Map<String, Object> bankAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(accountRealtimeBalance.getEnterpriseBankAccount());
                if (bankAccount == null) {
                    failed.put(accountRealtimeBalance.getId().toString(), accountRealtimeBalance.getId().toString());
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00188", "银行账户不合法！请检查银行账户:") /* "银行账户不合法！请检查银行账户:" */ + accountRealtimeBalance.getEnterpriseBankAccount() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00187", "是否存在！") /* "是否存在！" */);
                    i++;
                    continue;
                }
                //如果已确认（即已确认=“是”），则提示“账户XXX余额日期XXX的余额已确认！”
                if (accountRealtimeBalance.getIsconfirm() != null && accountRealtimeBalance.getIsconfirm()) {
//                throw new CtmException("银行账户："+accountRealtimeBalance.getEnterpriseBankAccount()+"余额日期："+accountRealtimeBalance.getBalancedate()+"的余额已确认！");
                    failed.put(accountRealtimeBalance.getId().toString(), accountRealtimeBalance.getId().toString());
//                    messages.add(MessageUtils.getMessage("P_YS_CTM_TMSP-FE_1617722556232499332") /* "银行账户：" */ + bankAccount.get("name") + MessageUtils.getMessage("P_YS_FI_YYFI-UI_0001142326") /* "余额日期" */ +
//                            new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate()) + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129006") /* "的余额已确认！" */);
                    messages.add(String.format(MessageUtils.getMessage("P_YS_CTM_CM-BE_1732554906396000259") /* "银行账户：[%s]余额日期：[%s]的余额已确认！" */, bankAccount.get("name"), new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate())));
                    i++;
                    continue;
                } else {
                    //已确认=“否”且余额检查结果=“不相等”），则提示“账户XXX余额日期余额检查结果不相等，不允许进行余额确认，请核对后进行确认！”
                    if (accountRealtimeBalance.getBalancecontrast() != null &&
                            accountRealtimeBalance.getBalancecontrast() == BalanceContrast.Unequal.getValue()) {
//                    throw new CtmException("银行账户："+accountRealtimeBalance.getEnterpriseBankAccount()+"余额日期："
//                            +accountRealtimeBalance.getBalancedate()+"余额检查结果不相等，不允许进行余额确认，请核对后进行确认！");
                        failed.put(accountRealtimeBalance.getId().toString(), accountRealtimeBalance.getId().toString());
                        messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018C", "银行账户：") /* "银行账户：" */ + bankAccount.get("name") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018B", "余额日期") /* "余额日期" */
                                + new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018F", "余额检查结果不相等，不允许进行余额确认，请核对后进行确认！") /* "余额检查结果不相等，不允许进行余额确认，请核对后进行确认！" */);
                        i++;
                        continue;
                    }
                }

                if (getCheckAccountBanlanceFlag(accountRealtimeBalance.getAccentity())) {
                    //检查历史余额与银行对账单余额是否相等
                    if (BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode() != accountRealtimeBalance.getDatasource()) {
                        QuerySchema querySchema = new QuerySchema().addSelect("id,accentity,bankaccount,currency,tran_date,tran_amt,dc_flag,tran_time,acct_bal");
                        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
                        queryConditionGroup.addCondition(QueryCondition.name("tran_date").eq(accountRealtimeBalance.getBalancedate())); // 等于
                        queryConditionGroup.addCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.getCurrency()));
                        queryConditionGroup.addCondition(QueryCondition.name("accentity").eq(accountRealtimeBalance.getAccentity()));
                        queryConditionGroup.addCondition(QueryCondition.name("bankaccount").eq(accountRealtimeBalance.getEnterpriseBankAccount()));
                        querySchema.addCondition(queryConditionGroup);
                        //querySchema.addOrderBy("tran_time");
                        querySchema.addOrderBy(new QueryOrderby("tran_time", "desc"));
                        querySchema.addOrderBy(new QueryOrderby("bank_seq_no", "desc"));
                        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                        if (null != bankReconciliations && bankReconciliations.size() > 0) {
                            int size = bankReconciliations.size();
                            if (bankReconciliations.get(0).getTran_time() != null && bankReconciliations.get(0).getAcct_bal() != null) {
                                if (!accountRealtimeBalance.getAcctbal().equals(bankReconciliations.get(0).getAcct_bal())) {
                                    failed.put(accountRealtimeBalance.getId().toString(), accountRealtimeBalance.getId().toString());
                                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_186795080468000F", "账户[%s]余额，与当日银行对账单[%s]的余额[%s]不相等，不允许进行余额确认，请核对后进行确认！") /* "账户[%s]余额，与当日银行对账单[%s]的余额[%s]不相等，不允许进行余额确认，请核对后进行确认！" */,
                                            bankAccount.get("name"),
                                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(bankReconciliations.get(0).getTran_time()),
                                            dealMoneyDigit(accountRealtimeBalance, bankReconciliations.get(0).getAcct_bal())));
                                    i++;
                                    continue;
                                }
                            }
                        }
                    }
                }

                //检查小于当前余额日期的历史余额是否有未确认数据
                QuerySchema schema = QuerySchema.create().addSelect(" id,enterpriseBankAccount,currency,balancedate");//
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.getAccentity()));
                conditionGroup.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.getCurrency()));
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
                conditionGroup.appendCondition(QueryCondition.name("balancedate").between(null, accountRealtimeBalance.getBalancedate()));
                conditionGroup.appendCondition(QueryCondition.name("isconfirm").eq(Boolean.FALSE));
                conditionGroup.appendCondition(QueryCondition.name("id").not_eq(accountRealtimeBalance.getId()));
                conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
                schema.addCondition(conditionGroup);
                schema.addOrderBy("balancedate");
                List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
                if (existBalances != null && existBalances.size() > 0) {
                    StringBuilder dateStr = new StringBuilder("");
                    if (billList.size() == 1) {
                        for (int index = 0; index < existBalances.size(); index++) {
                            if (dateStr.length() > 0) {
                                dateStr = dateStr.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00182", "、") /* "、" */ + new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                            } else {
                                dateStr = dateStr.append(new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                            }
                        }
//                messages.add(MessageUtils.getMessage("P_YS_CTM_TMSP-FE_1617722556232499332") /* "银行账户：" */+bankAccount.get("name")+MessageUtils.getMessage("P_YS_FI_YYFI-UI_0001142326") /* "余额日期" */
//                        +dateStr+MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129042") /* "的余额未确认，请按余额日期顺序进行确认！" */);
//                    messages.add(String.format(MessageUtils.getMessage("P_YS_CTM_CM-BE_1727540660929560596") /* "银行账户：[%s]余额日期：[%s]的余额未确认，请按余额日期顺序进行确认！" */, bankAccount.get("name"), dateStr));
//                    i++;
//                    continue;
                    } else {
//                    StringBuilder dateStr = new StringBuilder("");
                        for (int index = 0; index < existBalances.size(); index++) {
                            String key = existBalances.get(index).getEnterpriseBankAccount() + "" + new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate());
                            if (!accountDateMap.containsKey(key)) {
                                if (dateStr.length() > 0) {
                                    dateStr = dateStr.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00182", "、") /* "、" */ + new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                                } else {
                                    dateStr = dateStr.append(new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                                }
                            } else {
                                if (failed.containsKey(existBalances.get(index).getId().toString())) {
                                    if (dateStr.length() > 0) {
                                        dateStr = dateStr.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00182", "、") /* "、" */ + new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                                    } else {
                                        dateStr = dateStr.append(new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                                    }
                                }
                            }
                        }
                    }
                    if (dateStr.length() > 0) {
                        //如果存在不包含则报错
                        failed.put(accountRealtimeBalance.getId().toString(), accountRealtimeBalance.getId().toString());
                        messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018A", "银行账户：[%s]余额日期：[%s]的余额未确认，请按余额日期顺序进行确认！") /* "银行账户：[%s]余额日期：[%s]的余额未确认，请按余额日期顺序进行确认！" */, bankAccount.get("name"), dateStr));
                        i++;
                        continue;
                    }
                }
                //检查账户余额连续性
                /**
                 * 1，判断是否是初始数据，初始数据可成功确认(当前余额日期之前没有历史余额记录)
                 * 2，非初始数据，获取最新已确认数据，获取时间
                 * 3，计算最新已确认数据时间与当前时间间隔天数
                 * 4，获取最新已确认数据时间与当前时间之间世界余额条数
                 * 5，条数不相等 获取缺失日期
                 */
                //1，判断是否是初始数据
                QuerySchema schema2 = QuerySchema.create().addSelect(" id,enterpriseBankAccount,currency,balancedate");//
                QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup2.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.getAccentity()));
                conditionGroup2.appendCondition(QueryCondition.name(IBussinessConstant.CURRENCY).eq(accountRealtimeBalance.getCurrency()));
                conditionGroup2.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
                conditionGroup2.appendCondition(QueryCondition.name("balancedate").between(null, accountRealtimeBalance.getBalancedate()));
                conditionGroup2.appendCondition(QueryCondition.name("id").not_eq(accountRealtimeBalance.getId()));
                conditionGroup2.appendCondition(QueryCondition.name("first_flag").eq("0"));
                schema2.addCondition(conditionGroup2);
                List<AccountRealtimeBalance> existBalances2 = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
                if (existBalances2 == null || existBalances2.size() < 1) {
                    accountRealtimeBalance.setIsconfirm(Boolean.TRUE);
                    accountRealtimeBalance.setBalanceconfirmerid(AppContext.getUserId());
                    accountRealtimeBalance.setBalanceconfirmtime(new Date());
                    EntityTool.setUpdateStatus(accountRealtimeBalance);
//                MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, billList);
                    successList.add(accountRealtimeBalance);
                    continue;
                }
                //2，不是初始数据，检查账户余额连续性
                conditionGroup2.appendCondition(QueryCondition.name("isconfirm").eq(Boolean.TRUE));
                schema2.addOrderBy("balancedate");
                existBalances2 = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
                if (existBalances2 != null && existBalances2.size() > 0) {
                    AccountRealtimeBalance lastConfirmAccount = existBalances2.get(existBalances2.size() - 1);
                    //获取间隔日期天数和间隔日期列表
                    List<String> betweenDates = getBetweenDate(lastConfirmAccount.getBalancedate().toString(), new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate()));
                    int days = daysBetween(lastConfirmAccount.getBalancedate(), accountRealtimeBalance.getBalancedate());
                    if (days > 0 && betweenDates.size() > 0) {
                        //获取间隔余额历史
                        QuerySchema schema3 = QuerySchema.create().addSelect(" id,enterpriseBankAccount,currency,balancedate");//
                        QueryConditionGroup conditionGroup3 = new QueryConditionGroup(ConditionOperator.and);
                        conditionGroup3.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.getAccentity()));
                        conditionGroup3.appendCondition(QueryCondition.name(IBussinessConstant.CURRENCY).eq(accountRealtimeBalance.getCurrency()));
                        conditionGroup3.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
                        conditionGroup3.appendCondition(QueryCondition.name("balancedate").between(lastConfirmAccount.getBalancedate(), accountRealtimeBalance.getBalancedate()));
                        conditionGroup3.appendCondition(QueryCondition.name("id").not_eq(accountRealtimeBalance.getId()));
                        conditionGroup3.appendCondition(QueryCondition.name("id").not_eq(lastConfirmAccount.getId()));
                        conditionGroup3.appendCondition(QueryCondition.name("first_flag").eq("0"));
                        schema3.addCondition(conditionGroup3);
                        // 上次确认和本次确认之间得余额历史记录
                        List<AccountRealtimeBalance> existBalances3 = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema3, null);
                        if (existBalances3 != null && existBalances3.size() > 0) {
                            if (existBalances3.size() != days) {
                                for (AccountRealtimeBalance accountRealtimeBalance1 : existBalances3) {
                                    String removeDay = DateUtils.convertToStr(accountRealtimeBalance1.getBalancedate(), "yyyy-MM-dd");
                                    if (betweenDates.contains(removeDay)) {
                                        betweenDates.remove(removeDay);
                                    }
                                }
                                StringBuilder dateStr = new StringBuilder("");
                                for (int index = 0; index < betweenDates.size(); index++) {
                                    if (index != betweenDates.size() - 1) {
                                        dateStr = dateStr.append(betweenDates.get(index) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00182", "、") /* "、" */);
                                    } else {
                                        dateStr = dateStr.append(betweenDates.get(index));
                                    }
                                }
                                if (dateStr.length() > 0) {
                                    failed.put(accountRealtimeBalance.getId().toString(), accountRealtimeBalance.getId().toString());
                                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018C", "银行账户：") /* "银行账户：" */ + bankAccount.get("name") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018B", "余额日期") /* "余额日期" */
                                            + dateStr + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018D", "余额缺失，请补充后进行确认！") /* "余额缺失，请补充后进行确认！" */);
                                    i++;
                                    continue;
                                }
                            }
                        } else {
                            StringBuilder dateStr = new StringBuilder("");
                            for (int index = 0; index < betweenDates.size(); index++) {
                                if (index != betweenDates.size() - 1) {
                                    dateStr = dateStr.append(betweenDates.get(index) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00182", "、") /* "、" */);
                                } else {
                                    dateStr = dateStr.append(betweenDates.get(index));
                                }
                            }
                            failed.put(accountRealtimeBalance.getId().toString(), accountRealtimeBalance.getId().toString());
                            messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018C", "银行账户：") /* "银行账户：" */ + bankAccount.get("name") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018B", "余额日期") /* "余额日期" */
                                    + dateStr + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018D", "余额缺失，请补充后进行确认！") /* "余额缺失，请补充后进行确认！" */);
                            i++;
                            continue;
                        }
                    }

                }
                accountRealtimeBalance.setIsconfirm(Boolean.TRUE);
                accountRealtimeBalance.setBalanceconfirmerid(AppContext.getUserId());
                accountRealtimeBalance.setBalanceconfirmtime(new Date());
                EntityTool.setUpdateStatus(accountRealtimeBalance);
//            MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, billList);
                successList.add(accountRealtimeBalance);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105066"), e.getMessage());
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, successList);
//        String message = com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026301") /* "共：" */ + billList.size() + com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026150") /* "张单据；" */ + (billList.size() - i) + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385128976") /* "张余额确认成功；" */ + i + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385128982") /* "张余额确认失败！" */;
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00183", "共：[%s]张单据；[%s]张余额确认成功；[%s]张余额确认失败！") /* "共：[%s]张单据；[%s]张余额确认成功；[%s]张余额确认失败！" */, billList.size(), (billList.size() - i), i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", billList.size());
        result.put("sucessCount", billList.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject cancelConfirmAccountBalance(List<AccountRealtimeBalance> billList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (billList == null || billList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105067"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00180", "请选择单据！") /* "请选择单据！" */);
        }
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        //如果是批量操作，检查小于当前余额日期的历史余额是否是否勾选
        Map<String, AccountRealtimeBalance> accountDateMap = new HashMap<>();
        if (billList.size() > 1) {
            for (AccountRealtimeBalance accountRealtimeBalance : billList) {
                accountDateMap.put(accountRealtimeBalance.getEnterpriseBankAccount() + "" + new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate()), accountRealtimeBalance);
            }
        }
        for (AccountRealtimeBalance accountRealtimeBalance : billList) {
            //加锁
            String lockkey = ICmpConstant.MY_BILL_CLAIM_LIST + accountRealtimeBalance.get("id");
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTraceByTime(lockkey, 5);
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100448"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00185", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
            }
            try {
                Map<String, Object> bankAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(accountRealtimeBalance.getEnterpriseBankAccount());
                if (accountRealtimeBalance.getIsconfirm() != null && !accountRealtimeBalance.getIsconfirm()) {
                    //                throw new CtmException("银行账户："+accountRealtimeBalance.getEnterpriseBankAccount()+"余额日期："+accountRealtimeBalance.getBalancedate()+"余额未确认，不允许取消确认！");
                    failed.put(accountRealtimeBalance.getId().toString(), accountRealtimeBalance.getId().toString());
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018C", "银行账户：") /* "银行账户：" */ + bankAccount.get("name") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0018B", "余额日期") /* "余额日期" */
                            + new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00190", "余额未确认，不允许取消确认！") /* "余额未确认，不允许取消确认！" */);
                    i++;
                    continue;
                }
                //检查大于当前余额日期的历史余额是否有已经确认数据
                QuerySchema schema = QuerySchema.create().addSelect(" id,enterpriseBankAccount,currency,balancedate");//
                QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.getAccentity()));
                conditionGroup.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.getCurrency()));
                conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
                conditionGroup.appendCondition(QueryCondition.name("balancedate").between(accountRealtimeBalance.getBalancedate(), null));
                conditionGroup.appendCondition(QueryCondition.name("isconfirm").eq(Boolean.TRUE));
                conditionGroup.appendCondition(QueryCondition.name("id").not_eq(accountRealtimeBalance.getId()));
                conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
                schema.addCondition(conditionGroup);
                schema.addOrderBy("balancedate");
                List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
                if (existBalances != null && existBalances.size() > 0) {
                    StringBuilder dateStr = new StringBuilder("");
                    if (billList.size() == 1) {
                        for (int index = 0; index < existBalances.size(); index++) {
                            if (dateStr.length() > 0) {
                                dateStr = dateStr.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00182", "、") /* "、" */ + new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                            } else {
                                dateStr = dateStr.append(new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                            }
                        }
                        //                messages.add(MessageUtils.getMessage("P_YS_CTM_TMSP-FE_1617722556232499332") /* "银行账户：" */+bankAccount.get("name")+MessageUtils.getMessage("P_YS_FI_YYFI-UI_0001142326") /* "余额日期" */
                        //                        +dateStr+MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385129045") /* "的余额未取消确认，不允许取消确认！" */);
                        //                    messages.add(String.format(MessageUtils.getMessage("P_YS_CTM_CM-BE_1727540660929560590") /* "银行账户：[%s]余额日期：[%s]的余额未取消确认，不允许取消确认！" */, bankAccount.get("name"), dateStr));
                        //                    i++;
                        //                    continue;
                    } else {
                        //                    StringBuilder dateStr = new StringBuilder("");
                        for (int index = 0; index < existBalances.size(); index++) {
                            String key = existBalances.get(index).getEnterpriseBankAccount() + "" + new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate());
                            if (!accountDateMap.containsKey(key)) {
                                if (dateStr.length() > 0) {
                                    dateStr = dateStr.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00182", "、") /* "、" */ + new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                                } else {
                                    dateStr = dateStr.append(new SimpleDateFormat("yyyy-MM-dd").format(existBalances.get(index).getBalancedate()));
                                }
                            }
                        }
                    }
                    if (dateStr.length() > 0) {
                        //如果存在不包含则报错
                        messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00189", "银行账户：[%s]余额日期：[%s]的余额未取消确认，不允许取消确认！") /* "银行账户：[%s]余额日期：[%s]的余额未取消确认，不允许取消确认！" */, bankAccount.get("name"), dateStr));
                        i++;
                        continue;
                    }
                }
                accountRealtimeBalance.setIsconfirm(Boolean.FALSE);
                accountRealtimeBalance.setBalanceconfirmerid(null);
                accountRealtimeBalance.setBalanceconfirmtime(null);
                EntityTool.setUpdateStatus(accountRealtimeBalance);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105068"), e.getMessage());
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
        }
        MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, billList);
//        String message = com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026301") /* "共：" */ + billList.size() + com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026150") /* "张单据；" */ + (billList.size() - i) + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385128976") /* "张余额确认成功；" */ + i + MessageUtils.getMessage("P_YS_CTM_CM-BE_1723110711385128982") /* "张余额确认失败！" */;
        String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00183", "共：[%s]张单据；[%s]张余额确认成功；[%s]张余额确认失败！") /* "共：[%s]张单据；[%s]张余额确认成功；[%s]张余额确认失败！" */, billList.size(), (billList.size() - i), i);
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", billList.size());
        result.put("sucessCount", billList.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    /**
     * 据余额四要素，删除重复历史余额数据
     *
     * @param accEntity
     * @param deleteCurrencyIds
     * @param deleteCurrencyIds
     * @param startDate
     * @param endDate
     * @throws Exception
     */
    @Override
    public List<AccountRealtimeBalance> deleteAccountBalanceList(String accEntity, List<String> deleteCurrencyIds, List<String> enterpriseBankAccountIds, String startDate, String endDate) throws Exception {
        List<AccountRealtimeBalance> existBalances = new ArrayList<>();
        if (!deleteCurrencyIds.isEmpty() && !enterpriseBankAccountIds.isEmpty()) {
            QuerySchema schema = QuerySchema.create().addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accEntity));
            conditionGroup.appendCondition(QueryCondition.name("currency").in(deleteCurrencyIds));
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccountIds));
            conditionGroup.appendCondition(QueryCondition.name("balancedate").egt(startDate));
            conditionGroup.appendCondition(QueryCondition.name("balancedate").elt(endDate));
            conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
            schema.addCondition(conditionGroup);
            existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
            if (existBalances != null && existBalances.size() > 0) {
                existBalances.stream().forEach(existBalance -> existBalance.setEntityStatus(EntityStatus.Delete));
            }
        }
        return existBalances;
    }

    @Override
    public CtmJSONObject syncHistoryAccountBalance(CtmJSONObject balance) throws Exception {
        //获取时间维度
        List betweendates = (List) balance.get("betweendate");
        boolean isStartDateEmpty = betweendates != null && Strings.isNullOrEmpty(betweendates.get(0).toString());
        boolean isEndDateEmpty = betweendates != null && Strings.isNullOrEmpty(betweendates.get(1).toString());
        //校验余额日期
        if (betweendates == null || isStartDateEmpty || isEndDateEmpty) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100449"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00192", "请输入余额日期！") /* "请输入余额日期！" */);
        }
        Date startDate = DateUtils.dateParse(betweendates.get(0).toString(), "yyyy-MM-dd");
        Date endDate = DateUtils.dateParse(betweendates.get(1).toString(), "yyyy-MM-dd");
        Date nowDate = DateUtils.getNowDateShort2();
        if (endDate.compareTo(nowDate) >= 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100450"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00181", "账户历史同步余额日期必须在系统当前日期之前！") /* "账户历史同步余额日期必须在系统当前日期之前！" */);
        }
        // 根据条件查询结算中心内部户历史余额
        Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = getBankAccountsGroupForBalance(balance);
        //内部账户
        List<EnterpriseBankAcctVO> innerBankAccounts = bankAccountsGroup.get("innerAccounts");
        //直联账户
        List<EnterpriseBankAcctVO> httpBankAccounts = bankAccountsGroup.get("checkSuccess");
        //获取直连账户查询信息组装
        List<CashHttpBankEnterpriseLinkVo> httpList = querHttpAccount(httpBankAccounts, balance, false);
        //异步进度球相关信息
        String uid = balance.getString("uid");
        int yqlAccountNum = httpBankAccounts.stream().mapToInt(account -> account.getCurrencyList() == null ? 0 : account.getCurrencyList().size()).sum();
        int innerAccountNum = innerBankAccounts.stream().mapToInt(account -> account.getCurrencyList() == null ? 0 : account.getCurrencyList().size()).sum();
        int size = yqlAccountNum + innerAccountNum;
        //初始化进度条
        ProcessUtil.initProcessWithAccountNum(uid, size);
        CtmJSONObject logData = new CtmJSONObject();
        logData.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540072B", "账户历史余额查询账户请求") /* "账户历史余额查询账户请求" */, balance);
        logData.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540072E", "账户历史余额查询账户返回") /* "账户历史余额查询账户返回" */, bankAccountsGroup);
        ctmcmpBusinessLogService.saveBusinessLog(logData, "", "", IServicecodeConstant.ACCHISBAL, IMsgConstant.QUERY_ACCHISBAL, IMsgConstant.QUERY_ACCHISBAL);
        ExecutorService executorService =
                ThreadPoolBuilder.buildThreadPoolByYmsParam(ThreadConstant.CMP_HISTIMEBAL_PULL_MANU_ACCEPT);
        executorService.submit(() -> {
            try {
                if (CollectionUtils.isNotEmpty(innerBankAccounts)) {
                    //查询内部账户历史余额
                    syncHistoryInnerAccountBalance(startDate, endDate, innerBankAccounts, uid);
                }
                if (CollectionUtils.isNotEmpty(httpList)) {
                    //查询直联账户历史余额
                    doSyncHistoryAccountBalance(httpList, uid);
                }
            } catch (Exception e) {
                log.error("同步银行账户历史余额失败：", e);
                ProcessUtil.addMessage(uid, e.getMessage());
                ProcessUtil.failAccountNumAdd(uid,size);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100451"), e.getMessage());
            } finally {
                //存在两个账户都不存在的情况 最后需要将进度置为100%
                ProcessUtil.completedResetCount(uid, true);
            }
        });
        CtmJSONObject result = new CtmJSONObject();
        return result;
    }

    /**
     * 根据条件查询 内部账户和直联账户的分组
     *
     * @param balance
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, List<EnterpriseBankAcctVO>> getBankAccountsGroupForBalance(CtmJSONObject balance) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        //会计主体
        String accEntity = balance.getString(IBussinessConstant.ACCENTITY);
        List<String> accentitys = new ArrayList<>();
        List<String> accounts = new ArrayList<>();
        if (StringUtils.isNotEmpty(accEntity)) {
            if (accEntity.contains(",")) {
                accentitys = Arrays.asList(accEntity.split(","));
            } else if (accEntity.contains(";")) {//调度任务 传进来的信息是分号区分
                accentitys = Arrays.asList(accEntity.split(";"));
            } else
                //说明参数只选了一个
                accentitys.add(accEntity);

            //判断前段数据是否为多选
            if (accEntity.contains("[")) {
                accentitys = balance.getObject(IBussinessConstant.ACCENTITY, List.class);
            }
            if (accentitys != null && !accentitys.isEmpty()) {
                // 根据所选组织查询 有权限的账户
                EnterpriseParams newEnterpriseParams = new EnterpriseParams();
                newEnterpriseParams.setOrgidList(accentitys);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllEnableWithRange(newEnterpriseParams);
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVO : enterpriseBankAcctVOS) {
                    accounts.add(enterpriseBankAcctVO.getId());
                }
            }
        }
        //银行类被
        String bankType = balance.getString("banktype");
        List<String> bankTypes = new ArrayList<>();
        if (StringUtils.isNotEmpty(bankType)) {
            if (bankType != null && bankType.contains("[")) {
                bankTypes = balance.getObject("banktype", List.class);
            } else if (bankType.contains(",")) {
                bankTypes = Arrays.asList(bankType.split(","));
            } else if (bankType.contains(";")) {//调度任务 传进来的信息是分号区分
                bankTypes = Arrays.asList(bankType.split(";"));
            } else
                //说明参数只选了一个
                bankTypes.add(bankType);
        }

        //银行账户
        String enterpriseBankAccount = balance.getString("enterpriseBankAccount");
        List<String> enterpriseBankAccounts = new ArrayList<>();
        if (enterpriseBankAccount != null && enterpriseBankAccount.contains("[")) {
            enterpriseBankAccounts = balance.getObject("enterpriseBankAccount", List.class);
        } else if (StringUtils.isNotEmpty(enterpriseBankAccount)) {
            enterpriseBankAccounts.add(enterpriseBankAccount);
        }
        if (StringUtils.isNotEmpty(enterpriseBankAccount)) {
            enterpriseParams.setIdList(enterpriseBankAccounts);
        } else {
            enterpriseParams.setIdList(accounts);
        }
        //币种
        String currency = balance.getString("currency");
        List<String> currencyids = new ArrayList<>();
        if (StringUtils.isNotEmpty(currency)) {
            if (currency != null && currency.contains("[")) {
                currencyids = balance.getObject("currency", List.class);
            } else if (currency.contains(",")) {//前端 传进来的信息是逗号区分
                currencyids = Arrays.asList(currency.split(","));
            } else if (currency.contains(";")) {//调度任务 传进来的信息是分号区分
                currencyids = Arrays.asList(currency.split(";"));
            } else {//说明参数只选了一个
                currencyids.add(currency);
            }
        }
        if (StringUtils.isNotEmpty(currency)) {
            enterpriseParams.setCurrencyIDList(currencyids);
        }
        //根据条件查询 企业银行账户
        List<EnterpriseBankAcctVO> bankAccounts = new ArrayList<>();
        /**
         * 由于账户提供的接口参数 银行类别为string，所以当传入多个银行类别时候需要循环查询
         */
        if (CollectionUtils.isNotEmpty(bankTypes)) {
            for (String bank : bankTypes) {
                enterpriseParams.setBank(bank);
                bankAccounts.addAll(enterpriseBankQueryService.queryAllEnable(enterpriseParams));
            }
        } else
            bankAccounts.addAll(enterpriseBankQueryService.queryAllEnable(enterpriseParams));

        Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = EnterpriseBankQueryService.getBankAcctVOsGroup(bankAccounts);

        return bankAccountsGroup;
    }

    /**
     * 根据条件查询 内部账户和直联账户的分组
     *
     * @param balance
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, List<EnterpriseBankAcctVO>> getBankAccountsGroupForBalanceByTask(CtmJSONObject balance) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        //会计主体
        String accEntity = balance.getString(IBussinessConstant.ACCENTITY);
        List<String> accentitys = new ArrayList<>();
        List<String> accounts = new ArrayList<>();
        if (!StringUtils.isEmpty(accEntity)) {
            if (accEntity.contains(",")) {
                accentitys = Arrays.asList(accEntity.split(","));
            } else if (accEntity.contains(";")) {//调度任务 传进来的信息是分号区分
                accentitys = Arrays.asList(accEntity.split(";"));
            } else
                //说明参数只选了一个
                accentitys.add(accEntity);

            //判断前段数据是否为多选
            if (accEntity.contains("[")) {
                accentitys = balance.getObject(IBussinessConstant.ACCENTITY, List.class);
            }
            if (accentitys != null && !accentitys.isEmpty()) {
                // 根据所选组织查询 有权限的账户
                EnterpriseParams newEnterpriseParams = new EnterpriseParams();
                newEnterpriseParams.setOrgidList(accentitys);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllEnableWithRange(newEnterpriseParams);
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVO : enterpriseBankAcctVOS) {
                    accounts.add(enterpriseBankAcctVO.getId());
                }
            }
        }
        //银行类被
        String bankType = balance.getString("banktype");
        List<String> bankTypes = new ArrayList<>();
        if (StringUtils.isNotEmpty(bankType)) {
            if (bankType != null && bankType.contains("[")) {
                bankTypes = balance.getObject("banktype", List.class);
            } else if (bankType.contains(",")) {
                bankTypes = Arrays.asList(bankType.split(","));
            } else if (bankType.contains(";")) {//调度任务 传进来的信息是分号区分
                bankTypes = Arrays.asList(bankType.split(";"));
            } else
                //说明参数只选了一个
                bankTypes.add(bankType);
        }

        //银行账户
        //String enterpriseBankAccount = balance.getString("bankaccount");
        //调度任务参数改名了,兼容旧代码；没有此参数时，返回null(日常看参数没值时，返回空字符串)
        String enterpriseBankAccount = balance.getString("enterpriseBankAccount") == null ? balance.getString("bankaccount") : balance.getString("enterpriseBankAccount");
        List<String> enterpriseBankAccounts = new ArrayList<>();
        if (enterpriseBankAccount != null && enterpriseBankAccount.contains("[")) {
            enterpriseBankAccounts = balance.getObject("enterpriseBankAccount", List.class);
        } else if (StringUtils.isNotEmpty(enterpriseBankAccount) && enterpriseBankAccount.contains(";")) {//调度任务
            // 传进来的信息是分号区分
            enterpriseBankAccounts = Arrays.asList(enterpriseBankAccount.split(";"));
        } else if (StringUtils.isNotEmpty(enterpriseBankAccount)) {
            enterpriseBankAccounts.add(enterpriseBankAccount);
        }
        if (StringUtils.isNotEmpty(enterpriseBankAccount)) {
            enterpriseParams.setIdList(enterpriseBankAccounts);
        } else {
            enterpriseParams.setIdList(accounts);
        }
        //币种
        String currency = balance.getString("currency");
        List<String> currencyids = new ArrayList<>();
        if (StringUtils.isNotEmpty(currency)) {
            if (currency != null && currency.contains("[")) {
                currencyids = balance.getObject("currency", List.class);
            } else if (currency.contains(",")) {//前端 传进来的信息是逗号区分
                currencyids = Arrays.asList(currency.split(","));
            } else if (currency.contains(";")) {//调度任务 传进来的信息是分号区分
                currencyids = Arrays.asList(currency.split(";"));
            } else {//说明参数只选了一个
                currencyids.add(currency);
            }
        }
        if (StringUtils.isNotEmpty(currency)) {
            enterpriseParams.setCurrencyIDList(currencyids);
        }
        //根据条件查询 企业银行账户
        List<EnterpriseBankAcctVO> bankAccounts = new ArrayList<>();
        /**
         * 由于账户提供的接口参数 银行类别为string，所以当传入多个银行类别时候需要循环查询
         */
        if (CollectionUtils.isNotEmpty(bankTypes)) {
            for (String bank : bankTypes) {
                enterpriseParams.setBank(bank);
                bankAccounts.addAll(enterpriseBankQueryService.queryAllEnable(enterpriseParams));
            }
        } else
            bankAccounts.addAll(enterpriseBankQueryService.queryAllEnable(enterpriseParams));

        Map<String, List<EnterpriseBankAcctVO>> bankAccountsGroup = bankDealDetailService.getBankAcctVOsGroupByTask(bankAccounts);

        return bankAccountsGroup;
    }


    /**
     * 查询直联账户
     */
    @Override
    public List<CashHttpBankEnterpriseLinkVo> querHttpAccount(List<EnterpriseBankAcctVO> httpBankAccounts, CtmJSONObject balance, boolean isTask) throws Exception {
        List betweendates = (List) balance.get("betweendate");
        String startDate = betweendates.get(0).toString().replace("-", "");
        String endate = betweendates.get(1).toString().replace("-", "");
        String customNo = null;
        String channel = null;
        //实际请求的数据
        List<CashHttpBankEnterpriseLinkVo> httpList = new ArrayList<>();
        if (httpBankAccounts != null && httpBankAccounts.size() > 0) {
            channel = bankConnectionAdapterContext.getChanPayCustomChanel();
            customNo = bankAccountSettingService.getCustomNoByBankAccountId(httpBankAccounts.get(0).getId());
            for (EnterpriseBankAcctVO bankAccount : httpBankAccounts) {
                if (isTask) {
                    StringBuilder changedStartDate = new StringBuilder(startDate);
                    if (TaskUtils.changeStartDateByEnableDateAndCheckIfSkip(bankAccount, changedStartDate, endate)) {
                        continue;
                    }
                    startDate = changedStartDate.toString();
                }
                //此时前端只选择了会计主体 没有选币种
                //停用的币种也会返回，需要筛选下
                List<BankAcctCurrencyVO> currencyVOs = bankAccount.getCurrencyList().stream().filter(currencyVO -> currencyVO.getEnable() == 1).collect(Collectors.toList());
                HashMap<String, String> currencyMap = currencyQueryService.queryByCurrencyList(currencyVOs);
                //存在选择账户后给了个错误币种的情况 所以这里判空
                if (!currencyMap.values().isEmpty()) {
                    for (BankAcctCurrencyVO currencyVO : currencyVOs) {
                        CashHttpBankEnterpriseLinkVo httpmap = new CashHttpBankEnterpriseLinkVo();
                        httpmap.setAccentity(bankAccount.getOrgid());
                        httpmap.setEnterpriseBankAccount(bankAccount.getId());
                        httpmap.setAcct_no(bankAccount.getAccount());
                        httpmap.setAcct_name(bankAccount.getAcctName());
                        httpmap.setBank(bankAccount.getBank());
                        httpmap.setCurr_code(currencyMap.get(currencyVO.getCurrency()));
                        httpmap.setOperator(balance.getString("operator"));
                        httpmap.setSignature(balance.getString("signature"));
                        httpmap.setBeg_date(startDate);
                        httpmap.setEnd_date(endate);
                        httpmap.setCustomNo(customNo);
                        httpmap.setChannel(channel);
                        httpList.add(httpmap);
                    }
                } else {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540072C", "账号【%s】无启用状态的”币种“，请检查【企业资金账户】设置！") /* "账号【%s】无启用状态的”币种“，请检查【企业资金账户】设置！" */, bankAccount.getAccount()));
                }
            }
        }
        return httpList;
    }


    /**
     * 账户历史余额同步
     *
     * @param httpList
     * @param uid
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject doSyncHistoryAccountBalance(List<CashHttpBankEnterpriseLinkVo> httpList, String uid) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        //构建线程池
        ExecutorService executorService = null;
        Set<String> failAccountSet = ConcurrentHashMap.newKeySet();
        try {
            int maxSize = Integer.parseInt(AppContext.getEnvConfig("cmp.hisBalance.max.poolSize", "10"));
            int queueLength = Integer.parseInt(AppContext.getEnvConfig("cmp.hisBalance.queueCapacity", "2000"));
            String threadName = "cmp-hisBalance-async-";
            int coreSize = 10;
            executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                    .setDaemon(false)
                    .setRejectHandler(new BlockPolicy())
                    .builder(coreSize, maxSize, queueLength, threadName);
            ThreadPoolUtil.executeByBatch(executorService, httpList, batchcount, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540072A", "查历史余额") /* "查历史余额" */, (int fromIndex, int toIndex) -> {
                for (int t = fromIndex; t < toIndex; t++) {
                    CashHttpBankEnterpriseLinkVo httpAcct = httpList.get(t);
                    // 加锁的账号信息
                    String acctId = httpAcct.getEnterpriseBankAccount();
                    String currCode = httpAcct.getCurr_code();
                    String accountInfo = httpAcct.getAcct_no() +"|"+ httpAcct.getBeg_date() + "|"+ currCode;
                    // 加锁信息：账号+行为
                    String lockKey = accountInfo + ICmpConstant.QUERYHISBALANCEKEY;
                    String accountId_currencyCode_key = acctId + "|" + currCode;
                    try {
                        CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                            if (lockstatus == LockStatus.GETLOCK_FAIL) {
                                //加锁失败添加报错信息 并把进度刷新
                                ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                                ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400727", "[%s]:系统正在对此账户拉取中") /* "[%s]:系统正在对此账户拉取中" */, accountInfo));
                                return;
                            }
                            // 加锁成功，直连查询历史余额数据并进行验重和入库
                            excuteHttpHisBalance(httpAcct, uid, failAccountSet);
                        });
                    } catch (Exception e) {
                        log.error("doSyncHistoryAccountBalance", e);
                        ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                        ProcessUtil.addMessage(uid, String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400728", "[%s]:此账户操作发生异常") /* "[%s]:此账户操作发生异常" */, accountInfo));
                    }
                }
                return null;
            });
        } catch (Exception e) {
            ProcessUtil.failAccountNumAdd(uid, httpList.size());
            ProcessUtil.addMessage(uid, e.getMessage());
            log.error("doSyncHistoryAccountBalance", e);
        }
        return responseMsg;
    }

    public void excuteHttpHisBalance(CashHttpBankEnterpriseLinkVo httpAcct, String uid, Set<String> failAccountSet) throws Exception {
        CtmJSONObject queryBalanceMsg = new CtmJSONObject();
        CtmJSONObject result = new CtmJSONObject();
        String accountId = httpAcct.getEnterpriseBankAccount();
        String errorInfo = null;
        String accountId_currencyCode_key = accountId + "|" + httpAcct.getCurr_code();
        try {
            queryBalanceMsg = buildQueryHistoryBalanceMsg(httpAcct);//组装查询参数
            log.error("历史余额请求参数==========================》：" + queryBalanceMsg);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(queryBalanceMsg.toString());
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", queryBalanceMsg.toString()));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            result = HttpsUtils.doHttpsPostNew(QUERY_HIS_ACCOUNT_BALANCE, requestData, bankConnectionAdapterContext.getChanPayUri());
            log.error("历史余额响应参数==========================》：" + result);
            CtmJSONObject logData = new CtmJSONObject();
            logData.put(IMsgConstant.BILL_DATA, httpAcct);
            logData.put(IMsgConstant.ACCBAL_REQUEST, queryBalanceMsg);
            logData.put(IMsgConstant.ACCBAL_RESPONSE, result);
            ctmcmpBusinessLogService.saveBusinessLog(logData, httpAcct.getAcct_no(), httpAcct.getBeg_date(), IServicecodeConstant.ACCHISBAL, IMsgConstant.ACCHISBAL, IMsgConstant.QUERY_ACCHISBAL);
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String service_resp_code = responseHead.getString("service_resp_code");
                CtmJSONObject requestBody = (CtmJSONObject) queryBalanceMsg.get("request_body");
                if (HttpServiceInforamtionUtils.httpSuccessByRespCode(ITransCodeConstant.QUERY_HIS_ACCOUNT_BALANCE, service_resp_code)) {
                    CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                    //保存账户历史余额
                    insertAccountHistoryBalanceData(httpAcct, responseBody, uid);
                } else if (ITransCodeConstant.NO_DATAS_ERVICE_RESP_CODE.equals(service_resp_code)) {
                    // 解决多线程下的并发问题
                    ProcessUtil.dealYQLNodataMessage(requestBody, uid, failAccountSet, accountId_currencyCode_key, responseHead);
                } else {
                    String serviceRespDesc = responseHead.getString("service_resp_desc");
                    errorInfo = YQLUtils.getYQLErrorMsqForManual(requestBody, responseHead);
                    ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                }
            } else {
                ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
                errorInfo = YQLUtils.getYQLErrorMsqOfNetWork(result.getString("message"));
            }
        } catch (Exception e) {
            log.error(String.format("excuteHttpHisBalance错误，请求参数 = %s,响应参数 = %s,报错信息 = %s,对应账号 = %s", queryBalanceMsg, result, e.getMessage(), httpAcct.getAcct_no()), e);
            ProcessUtil.refreshFailAccountProcess(uid, failAccountSet, accountId_currencyCode_key);
            errorInfo = YQLUtils.getErrorMsqWithAccount(e, httpAcct.getAcct_no());
        } finally {
            if (errorInfo != null) {
                ProcessUtil.addYQLErrorMessage(uid, errorInfo);
            }
            //else {//要么成功 要么失败 不能一直成功
            //    ProcessUtil.refreshProcess(uid, true);
            //}
        }
    }

    /**
     * 历史余额入库
     *
     * @param bankAccount
     * @param responseBody
     * @param uid
     * @throws Exception
     */
    public int insertAccountHistoryBalanceData(CashHttpBankEnterpriseLinkVo bankAccount, CtmJSONObject responseBody, String uid) throws Exception {
        CtmJSONArray records = new CtmJSONArray();
        int addNum = 0;
        Date endDate = DateUtils.dateParse(bankAccount.getEnd_date(), "yyyyMMdd");
        int backNum = responseBody.getInteger("back_num");
        ProcessUtil.totalPullCountAdd(uid, backNum);
        if (backNum == 1) {
            CtmJSONObject jsonData = responseBody.getJSONObject("record");
            records.add(jsonData);
        } else if (backNum > 1) {
            records = responseBody.getJSONArray("record");
        }
        if (CollectionUtils.isEmpty(records)) {
            return 0;
        }
        Set<String> deleteAccountIds = new LinkedHashSet<>();
//        List<String> deleteCurrencyIds = new ArrayList<>();
        Set<AccountRealtimeBalance> saveBalances = new LinkedHashSet<>();
        for (int i = 0; i < records.size(); i++) {
            CtmJSONObject record = records.getJSONObject(i);
            AccountRealtimeBalance balance = new AccountRealtimeBalance();
            String _tran_date = record.getString("tran_date");
            if (_tran_date == null || "".equals(_tran_date)) {
                balance.setBalancedate(endDate);
            } else
                balance.setBalancedate(DateUtils.dateParse(_tran_date, "yyyyMMdd"));
            balance.setTenant(AppContext.getTenantId());
            balance.setAccentity(bankAccount.getAccentity());
            String currencyCode = record.getString("curr_code");
            String requsetCurrCode = bankAccount.getCurr_code();
            if (StringUtils.isNotEmpty(currencyCode)) {
                CurrencyTenantDTO currency = baseRefRpcService.getCurrencyByCode(currencyCode);
                if (null == currency) {
                    //ProcessUtil.failAccountNumAddOne(uid);
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400726", "银企联返回币种[%s]不存在，请检查币种档案！") /* "银企联返回币种[%s]不存在，请检查币种档案！" */, currencyCode));
                    //currency = baseRefRpcService.getCurrencyByCode("CNY");
                    //balance.setCurrency(currency.getId());
                } else {
                    if (currencyCode.equals(requsetCurrCode)) {
                        balance.setCurrency(currency.getId());
                    } else {
                        //ProcessUtil.failAccountNumAddOne(uid);
                        throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400729", "请求币种[%s]和银企联返回币种[%s]不一致，请联系银企联老师处理！") /* "请求币种[%s]和银企联返回币种[%s]不一致，请联系银企联老师处理！" */, requsetCurrCode, currencyCode));
                    }
                }
            } else {
                //throw new CtmException(String.format("银企联返回币种为空，请联系银企联老师处理！"));
                //若银企联返回报文中没有指定币种 这里默认人民币
                CurrencyTenantDTO currency = baseRefRpcService.getCurrencyByCode("CNY");
                balance.setCurrency(currency.getId());
            }
            if (bankAccount.getBank() != null) {
                balance.setBanktype(bankAccount.getBank());
            }
            balance.setBanktype(bankAccount.getBank());
            balance.setEnterpriseBankAccount(bankAccount.getEnterpriseBankAccount());
            BigDecimal acctBal = !record.containsKey("acct_bal") ? BigDecimal.ZERO : record.getBigDecimal("acct_bal");
            balance.setAcctbal(acctBal);
            if (record.containsKey("avl_bal") && record.getBigDecimal("avl_bal") != null) {
                balance.setAvlbal(record.getBigDecimal("avl_bal"));
            }
            if (record.containsKey("frz_bal") && record.getBigDecimal("frz_bal") != null) {
                balance.setFrzbal(record.getBigDecimal("frz_bal"));
            }
            // 处理冻结金额方法
            //todo
            dealFrzbal(record, balance);
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
            balance.setFlag(BalanceFlag.AutoPull.getCode());
            balance.setEntityStatus(EntityStatus.Insert);
            balance.setCreateTime(new Date());
            balance.setCreateDate(DateUtils.getNowDateShort2());
            balance.setCreator(AppContext.getCurrentUser().getName());//新增人名称
            balance.setCreatorId(AppContext.getCurrentUser().getId());//新增人id
            balance.setId(ymsOidGenerator.nextId());
            balance.setDatasource(BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode());
            balance.setRegular_amt(BigDecimal.ZERO);
            CtmJSONArray sub_records = record.getJSONArray("sub_record");
            if (sub_records != null && sub_records.size() > 0) {
                BigDecimal regular_amt = BigDecimal.ZERO;
                for (int j = 0; j < sub_records.size(); j++) {
                    CtmJSONObject sub_record = sub_records.getJSONObject(j);
                    BigDecimal bal_amt = !sub_record.containsKey("bal_amt") ? BigDecimal.ZERO : sub_record.getBigDecimal("bal_amt");
                    //定期金额
                    regular_amt = regular_amt.add(bal_amt);
                }
                balance.setRegular_amt(regular_amt);
            }
            balance.setTotal_amt(BigDecimalUtils.safeAdd(balance.getAcctbal(), balance.getRegular_amt()));
            // 历史余额删除后新增判断逻辑
            boolean isNewAdd = befroeDeleteAndSave(balance, deleteAccountIds, saveBalances, addNum);
            if (isNewAdd) {
                ProcessUtil.newAddCountAdd(uid, 1);
            }
        }
        // 删除&新增操作
        deleteAndSave(deleteAccountIds, saveBalances);
        return saveBalances.size();
    }

    private void deleteAndSave(Set<String> deleteAccountIds, Set<AccountRealtimeBalance> saveBalances) throws Exception {
        // 需要删除的历史余额数据
        if (!deleteAccountIds.isEmpty()) {
            MetaDaoHelper.batchDelete(AccountRealtimeBalance.ENTITY_NAME, Lists.newArrayList(new SimpleCondition("id", ConditionOperator.in, deleteAccountIds)));
        }
        // 需要新增的历史余额数据
        if (!saveBalances.isEmpty()) {
            List<AccountRealtimeBalance> accountRealtimeBalanceList = new ArrayList<>(saveBalances);
            CmpMetaDaoHelper.batchInsert(BizContext.getMetaRepository().entity(AccountRealtimeBalance.ENTITY_NAME), accountRealtimeBalanceList);
        }
    }

    /**
     * 历史余额删除后新增逻辑
     *
     * @param accountRealtimeBalance
     * @param deleteAccountIds
     * @param saveBalances
     * @param addNum
     */
    private boolean befroeDeleteAndSave(AccountRealtimeBalance accountRealtimeBalance, Set<String> deleteAccountIds, Set<AccountRealtimeBalance> saveBalances, int addNum) throws Exception {
        Boolean isNewAdd = false;
        // 1,根据会计主体、银行账号、币种、余额日期、first_flag
        QuerySchema schema = QuerySchema.create().addSelect("id,isconfirm,acctbal,balancecontrast,balancecheckinstruction");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.getAccentity()));
        conditionGroup.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.getCurrency()));
        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.getEnterpriseBankAccount()));
        conditionGroup.appendCondition(QueryCondition.name("balancedate").eq(accountRealtimeBalance.getBalancedate()));
        conditionGroup.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema.addCondition(conditionGroup);
        List<AccountRealtimeBalance> realtimeBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        log.error("=================根据会计主体、银行账号、币种、余额日期、first_flag查询历史余额==========" + realtimeBalances);
        if (realtimeBalances.isEmpty()) {
            // 不存在重复历史余额，直接入库
            saveBalances.add(accountRealtimeBalance);
            isNewAdd = true;
            //addNum++;
        } else {
            for (AccountRealtimeBalance oldBalance : realtimeBalances) {
                // 历史余额已确认直接跳过，不删不增
                if (oldBalance.getIsconfirm() != null && !oldBalance.getIsconfirm()) {
                    // 历史余额未确认判断余额是否相等
                    if (oldBalance.getAcctbal() != null && oldBalance.getAcctbal().compareTo(accountRealtimeBalance.getAcctbal()) == 0) {
                        // 余额相等 取数据库中对应余额检查结果,更新至银企联返回数据
                        accountRealtimeBalance.setBalancecontrast(oldBalance.getBalancecontrast());
                        accountRealtimeBalance.setBalancecheckinstruction(oldBalance.getBalancecheckinstruction());
                    } else {
                        // 余额不相等且远余额检查结果有值更新银企联返回数据的余额检查结果为不相等
                        if (oldBalance.getBalancecontrast() != null) {
                            accountRealtimeBalance.setBalancecontrast(BalanceContrast.Unequal.getValue());
                        }
                    }
                    deleteAccountIds.add(oldBalance.getId().toString());
                    saveBalances.add(accountRealtimeBalance);
                }
            }
        }
        log.error("=================历史余额删除列表==========" + deleteAccountIds);
        log.error("=================历史余额新增列表==========" + deleteAccountIds);
        return isNewAdd;
    }

    /**
     * 处理冻结金额
     *
     * @param record
     * @param balance
     * @throws Exception
     */
    private void dealFrzbal(CtmJSONObject record, AccountRealtimeBalance balance) throws Exception {
        /**
         * 1,判断冻结金额是否存在
         * 2,不存在则查询当日实时余额中的冻结金额
         * 3，不存在实时余额走原有逻辑，存在实时余额，根据实时余额得冻结金额赋值，并计算可用余额
         */
        BigDecimal acctBal = !record.containsKey("acct_bal") ? BigDecimal.ZERO : record.getBigDecimal("acct_bal");
        if (record.getBigDecimal("frz_bal") == null || BigDecimal.ZERO.equals(record.getBigDecimal("frz_bal"))) {
            //查询实时余额
            Date nextDay = DateUtils.dateAdd(balance.getBalancedate(), 1, false);
            nextDay = DateUtils.formatBalanceDate(nextDay);
            QuerySchema schema2 = QuerySchema.create().addSelect("id,frzbal,avlbal");
            QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup2.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(balance.getAccentity()));
            conditionGroup2.appendCondition(QueryCondition.name("currency").eq(balance.getCurrency()));
            conditionGroup2.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(balance.getEnterpriseBankAccount()));
            conditionGroup2.appendCondition(QueryCondition.name("balancedate").eq(nextDay));
            conditionGroup2.appendCondition(QueryCondition.name("first_flag").eq("1"));
            schema2.addCondition(conditionGroup2);
            List<AccountRealtimeBalance> realtimeBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
            if (realtimeBalances != null && realtimeBalances.size() > 0) {
                //balance.setAvlbal(acctBal.subtract(realtimeBalances.get(0).getFrzbal()));
                balance.setAvlbal(!record.containsKey("avl_bal") ? acctBal.subtract(realtimeBalances.get(0).getFrzbal()) : record.getBigDecimal("avl_bal"));
                balance.setFrzbal(realtimeBalances.get(0).getFrzbal());
            } else {
                balance.setAvlbal(!record.containsKey("avl_bal") ? acctBal : record.getBigDecimal("avl_bal"));
                balance.setFrzbal(!record.containsKey("frz_bal") ? BigDecimal.ZERO : record.getBigDecimal("frz_bal"));
            }
        } else {
            balance.setFrzbal(record.getBigDecimal("frz_bal"));
            balance.setAvlbal(!record.containsKey("avl_bal") ? acctBal.subtract(record.getBigDecimal("frz_bal")) : record.getBigDecimal("avl_bal"));
        }
    }

    /**
     * 组装接口查询请求数据
     *
     * @param bankAccount
     * @return
     */
    @Override
    public CtmJSONObject buildQueryHistoryBalanceMsg(CashHttpBankEnterpriseLinkVo bankAccount) throws Exception {
        CtmJSONObject requestHead = buildRequestHead(QUERY_HIS_ACCOUNT_BALANCE, bankAccount, bankAccount.getOperator(), bankAccount.getSignature(), true);
        CtmJSONObject requestBody = new CtmJSONObject();
        requestBody.put("acct_no", bankAccount.getAcct_no());
        requestBody.put("acct_name", bankAccount.getAcct_name());
        requestBody.put("beg_date", bankAccount.getBeg_date());
        requestBody.put("end_date", bankAccount.getEnd_date());
        requestBody.put("curr_code", bankAccount.getCurr_code());
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
    private CtmJSONObject buildRequestHead(String transCode, CashHttpBankEnterpriseLinkVo bankAccount, String operator, String signature, boolean fAutoPay) {
        CtmJSONObject requestHead = new CtmJSONObject();
        requestHead.put("version", "1.0.0");
        requestHead.put("request_seq_no", UUID.randomUUID().toString().replace("-", ""));
        requestHead.put("cust_no", bankAccount.getCustomNo());
        requestHead.put("cust_chnl", bankAccount.getChannel());
        LocalDateTime dateTime = LocalDateTime.now();
        requestHead.put("request_date", DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD).format(dateTime));
        requestHead.put("request_time", DateTimeFormatter.ofPattern(DateUtils.HHMMSS).format(dateTime));
        requestHead.put("oper", operator);
        requestHead.put("oper_sign", signature);
        requestHead.put("tran_code", transCode);
        if (fAutoPay) {
            requestHead.put(YONSUITE_AUTOTASK, AUTO_PAY_IDEN);
        }
        return requestHead;
    }

    /**
     * 查询符合条件的直连账户信息
     *
     * @param enterpriseBankAccount
     * @param accentity
     * @return
     * @throws Exception
     */
    public List<Map<String, Object>> queryBankAccountSetting(String enterpriseBankAccount, String accentity, String currency) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(
                " id,tenant,accentity,openFlag,enterpriseBankAccount,enterpriseBankAccount.acctName as acctName,enterpriseBankAccount.name as name,"
                        + "enterpriseBankAccount.account as account, enterpriseBankAccount.bankNumber.bank as bank,"
                        + "customNo,enterpriseBankAccount.id as bankId,enterpriseBankAccount.enable as enable, enterpriseBankAccount.currencyList.currency as currency");// 判断银行账户表是否为空故多差一个id

        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        if (!StringUtils.isBlank(enterpriseBankAccount)) {
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        } else {
            conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity));
        }
        if (StringUtils.isNotEmpty(currency)) {
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount.currencyList.currency").eq(currency));
        }
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        conditionGroup.appendCondition(QueryCondition.name("accStatus").eq("0"));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
    }

    /**
     * 查询直连或非直连账户信息
     *
     * @return
     * @throws Exception
     */
    public List<BankAccountSetting> queryBankAccountSettingOpenFlag(String openFlag) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id,enterpriseBankAccount");// 判断银行账户表是否为空故多差一个id
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq(openFlag));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
    }


    /**
     * 查询符合条件的直连账户信息
     *
     * @param enterpriseBankAccount
     * @param accentity
     * @return
     * @throws Exception
     */
    public List<Map<String, Object>> queryBankAccountSetting(String enterpriseBankAccount, String accentity) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect(" id,tenant,accentity,openFlag,enterpriseBankAccount,enterpriseBankAccount.acctName as acctName,enterpriseBankAccount.name as name," + "enterpriseBankAccount.account as account, enterpriseBankAccount.bankNumber.bank as bank," + "customNo,enterpriseBankAccount.id as bankId,enterpriseBankAccount.enable as enable");// 判断银行账户表是否为空故多差一个id
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("openFlag").eq("1"));
        if (!StringUtils.isBlank(enterpriseBankAccount)) {
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAccount));
        } else {
            conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity));
        }
        conditionGroup.appendCondition(QueryCondition.name("customNo").is_not_null());
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.query(BankAccountSetting.ENTITY_NAME, schema);
    }

    /**
     * 获取两个日期间的天数，精确到年月日
     *
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

    /**
     * 获取两个日期之间的所有日期
     *
     * @param startTime
     * @param endTime
     * @return
     */
    private List<String> getBetweenDate(String startTime, String endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 声明保存日期集合
        List<String> list = new ArrayList<String>();
        try {
            // 转化成日期类型
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);

            //用Calendar 进行日期比较判断
            Calendar calendar = Calendar.getInstance();
            while (startDate.getTime() < endDate.getTime()) {
                // 设置日期
                calendar.setTime(startDate);
                //把日期增加一天
                calendar.add(Calendar.DATE, 1);
                // 获取增加后的日期
                startDate = calendar.getTime();
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
     * @param preDays 调度任务设置的参数
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject checkAccountBalance(Integer preDays) throws Exception {
        /**
         * 余额检查步骤：
         * 1，获取历史余额中所有银行账户
         * 2，根据银行账户分组检查历史余额
         */
        //1，获取所有银行账户
        QuerySchema schema = QuerySchema.create().addSelect("accentity,enterpriseBankAccount,currency");//
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        List<BankAccountSetting> bankAccountSettings = queryBankAccountSettingOpenFlag("0");
        if (bankAccountSettings.size() == 0) {
            return null;
        }
        List<String> enterpriseBankAccounts = new ArrayList<>();

        for (Map bankAccountSetting : bankAccountSettings) {
            enterpriseBankAccounts.add(bankAccountSetting.get("enterpriseBankAccount").toString());
        }

        conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").in(enterpriseBankAccounts));
        schema.addCondition(conditionGroup);
        schema.distinct();
        List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
        int batch = Integer.parseInt(AppContext.getEnvConfig("balance.check.batch", "100"));
        if (existBalances != null && existBalances.size() > 0) {
            //计算任务数
            int total = existBalances.size() % 100 == 0 ? existBalances.size() / 100 : (existBalances.size() / 100) + 1;
            int size = existBalances.size();
            CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
            List<Callable<Object>> callables = new ArrayList<>();
            for (int i = 1; i <= total; i++) {
                int finalI = i;
                callables.add(() -> {
                    List<AccountRealtimeBalance> taskAccountRealtimeBalanceList = (finalI == total ? existBalances.subList((finalI - 1) * batch, (finalI - 1) * batch + size % batch) : existBalances.subList((finalI - 1) * batch, batch * finalI));
                    for (AccountRealtimeBalance accountRealtimeBalance : taskAccountRealtimeBalanceList) {
                        //1,余额检查是否缺失，缺失补齐
                        try {
                            log.error("------------------------------步骤一开始处理银行账号:" + accountRealtimeBalance.getEnterpriseBankAccount() + "--------------------------------");
                            checkDeletion(accountRealtimeBalance, preDays + 1);
                            log.error("------------------------------步骤一银行账号:" + accountRealtimeBalance.getEnterpriseBankAccount() + "处理完成--------------------------------");
                        } catch (Exception e) {
                            log.error(accountRealtimeBalance.getEnterpriseBankAccount() + "余额补齐异常！");
                        }
                    }
                    return null;
                });
            }
            //2，检查历史余额是否相等，回写结果
            try {
                log.error("------------------------------步骤二，检查历史余额是否相等，回写结果--------------------------------");
                for (int k = preDays; k > 0; k--) {
                    acctbalCompare(k);
                }
                log.error("------------------------------步骤二,处理完成-------------------------------");

            } catch (Exception e) {
                log.error("余额检查异常！");
            }
            ctmThreadPoolExecutor.getThreadPoolExecutor().invokeAll(callables);
        }
        log.error("------------------------------定时任务完成-------------------------------");

        return null;
    }

    //检查历史余额缺失，补齐
    private void checkDeletion(AccountRealtimeBalance accountRealtimeBalance, int preDays) throws Exception {

        //根据银行账号查询所有历史余额
        QuerySchema schema2 = QuerySchema.create().addSelect(" * ");//
        QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup2.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
        conditionGroup2.appendCondition(QueryCondition.name("first_flag").eq("0"));
        schema2.addCondition(conditionGroup2);
        schema2.addOrderBy("balancedate");
        List<AccountRealtimeBalance> allBalanceOfAccount = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
        // 将当前银行账户历史余额存入
        Map<Object, BizObject> accountRealtimeBalanceMap = new HashMap<>();
        for (AccountRealtimeBalance accountRealtimeBalance2 : allBalanceOfAccount) {
            accountRealtimeBalanceMap.put(DateUtils.convertToStr(accountRealtimeBalance2.getBalancedate(), "yyyy-MM-dd"), accountRealtimeBalance2);
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
        Date startDate = DateUtils.formatBalanceDate(-preDays);
        Date lastDay = DateUtils.formatBalanceDate(-1);
        ;
        //查询最新确认日期
        conditionGroup2.appendCondition(QueryCondition.name("isconfirm").eq(Boolean.TRUE));
        List<AccountRealtimeBalance> confirmedBalanceList = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema2, null);
        Date confirmDate = null;
        if (confirmedBalanceList != null && confirmedBalanceList.size() > 0) {
            confirmDate = confirmedBalanceList.get(confirmedBalanceList.size() - 1).getBalancedate();
            //比较初始日期和最新确认日期
            if (!confirmDate.before(startDate)) {
                startDate = confirmDate;
            }
        }
        supplementBalance(startDate, lastDay, accountRealtimeBalance, accountRealtimeBalanceMap);
    }

    //补充两日期之间缺失的历史余额
    public void supplementBalance(Date startDate, Date lastDay, AccountRealtimeBalance accountRealtimeBalance, Map<Object, BizObject> accountRealtimeBalanceMap) throws Exception {
        //校验银行账户是否开通银企联服务
        Boolean openFlag = getOpenFlag(accountRealtimeBalance);
        if (openFlag) {
            return;
        }
        //检查是否有缺失余额记录
        //获取间隔日期天数和间隔日期列表；初始日期和昨日
        List<String> betweenDate = getBetweenDate(new SimpleDateFormat("yyyy-MM-dd").format(startDate), new SimpleDateFormat("yyyy-MM-dd").format(lastDay));
        Set<String> betweenDateSet = new TreeSet<>();
        List<String> betweenDates = new ArrayList<>();
        betweenDateSet.add(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
        betweenDateSet.addAll(betweenDate);
        betweenDateSet.add(lastDay.toString());
        betweenDates.addAll(betweenDateSet);
        //TODO 考虑大数据量问题，内存是否能支撑住  多线程处理，事务分离成小事务。账户
        //TODO 国机测试环境实验一下 达梦库  加日志  优先
        //TODO 达梦数据库下时间格式的问题原因- mdd708/710升级之后出现的  元数据数据库格式
        int days = daysBetween(startDate, lastDay) + 1;// 9.1 -9.30 = 30
        if (days > 0 && betweenDates.size() > 0) {
            //获取间隔余额历史
            QuerySchema schema3 = QuerySchema.create().addSelect(" id,enterpriseBankAccount,currency,balancedate");//
            QueryConditionGroup conditionGroup3 = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup3.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
            conditionGroup3.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.get("currency")));
            conditionGroup3.appendCondition(QueryCondition.name("balancedate").between(startDate, lastDay));
            conditionGroup3.appendCondition(QueryCondition.name("first_flag").eq("0"));
            schema3.addCondition(conditionGroup3);
            schema3.addOrderBy("balancedate");
            // 上次两日期之间得余额历史记录
            List<AccountRealtimeBalance> existBalances3 = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema3, null);// 0
            if (existBalances3 != null && existBalances3.size() > 0) {
                if (existBalances3.size() != days) {
                    for (AccountRealtimeBalance accountRealtimeBalance1 : existBalances3) {
                        String removeDay = DateUtils.convertToStr(accountRealtimeBalance1.getBalancedate(), "yyyy-MM-dd");
                        if (betweenDates.contains(removeDay)) {
                            betweenDates.remove(removeDay);
                        }
                    }
                    for (int index = 0; index < betweenDates.size(); index++) {
                        // 补缺失余额
                        //判断是否开启银企联服务
                        supplementaryBalance(openFlag, accountRealtimeBalance, betweenDates, index, accountRealtimeBalanceMap);
                    }
                }
            } else {
                for (int index = 0; index < betweenDates.size(); index++) {
                    //判断是否开启银企联服务
                    supplementaryBalance(openFlag, accountRealtimeBalance, betweenDates, index, accountRealtimeBalanceMap);
                }
            }
        }
    }

    //补充余额
    private void supplementaryBalance(Boolean openFlag, AccountRealtimeBalance accountRealtimeBalance, List<String> betweenDates, int index, Map<Object, BizObject> accountRealtimeBalanceMap) throws Exception {
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
            Date nowDate = DateUtils.formatBalanceDate(new SimpleDateFormat("yyyy-MM-dd").parse(betweenDates.get(index)));
            String preDate = DateUtils.formatBalanceDate(DateUtils.dateAdd(nowDate, -1, Boolean.FALSE)).toString();
            // 历史余额中是否存在前一天历史余额
            String balanceKey = preDate + accountRealtimeBalance.getCurrency();
            Boolean isContains = accountRealtimeBalanceMap.containsKey(balanceKey);
            Map<String, Object> bankAccount = QueryBaseDocUtils.queryEnterpriseBankAccountById(accountRealtimeBalance.getEnterpriseBankAccount());
            if (isContains) {
                AccountRealtimeBalance insertVO = new AccountRealtimeBalance();
                insertVO.setAccentity(accountRealtimeBalance.getAccentity());
                insertVO.setEnterpriseBankAccount(accountRealtimeBalance.getEnterpriseBankAccount());
                insertVO.setBanktype(Objects.toString(bankAccount.get("bank"), null));
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
                //计算当日余额
                insertVO.setAcctbal(preDayAcctbal.add(todaySZ));
                //冻结金额
                insertVO.setFrzbal(preAccountBalance.getFrzbal() == null ? BigDecimal.ZERO : preAccountBalance.getFrzbal());
                // 可用余额 = 账户余额-冻结金额
                //insertVO.setAvlbal(preDayAcctbal.add(todaySZ));//当日银行对账单收支
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
                // 审计信息
                insertVO.setCreator(AppContext.getCurrentUser().getName());
                insertVO.setCreatorId(AppContext.getCurrentUser().getId());
                insertVO.setCreateDate(new Date());
                insertVO.setCreateTime(new Date());
                CmpMetaDaoHelper.insert(AccountRealtimeBalance.ENTITY_NAME, insertVO);
                accountRealtimeBalanceMap.put(betweenDates.get(index) + accountRealtimeBalance.getCurrency(), insertVO);
            } else {
                //历史余额中不存在前一日余额，查询最近日期的历史余额，补充最近日期到昨日缺失的数据
                QuerySchema schema4 = QuerySchema.create().addSelect(" id,enterpriseBankAccount,currency,balancedate");//
                QueryConditionGroup conditionGroup4 = new QueryConditionGroup(ConditionOperator.and);
                conditionGroup4.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.getAccentity()));
                conditionGroup4.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
                conditionGroup4.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.get("currency")));
                conditionGroup4.appendCondition(QueryCondition.name("balancedate").between(null, DateUtils.formatBalanceDate(DateUtils.dateAdd(nowDate, -1, Boolean.FALSE))));
                conditionGroup4.appendCondition(QueryCondition.name("first_flag").eq("0"));
                schema4.addCondition(conditionGroup4);
                schema4.addOrderBy("balancedate");
                List<AccountRealtimeBalance> existBalances4 = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema4, null);
                if (existBalances4 != null && existBalances4.size() > 0) {
                    //当前银行账户最近历史余额记录
                    AccountRealtimeBalance preAccountBalance = existBalances4.get(existBalances4.size() - 1);
                    supplementBalance(preAccountBalance.getBalancedate(), nowDate, accountRealtimeBalance, accountRealtimeBalanceMap);
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
        if (settingList != null && settingList.size() > 0) {
            //开通银企联服务
            openFlag = Boolean.TRUE;
        }
        return openFlag;
    }

    //获取银行对账单某一天的净收支
    private BigDecimal getBankAccountBalanceByDay(AccountRealtimeBalance accountRealtimeBalance) throws Exception {
        //获取当日净支付额，查询银行对账单
        QuerySchema schema = QuerySchema.create().addSelect(" * ");//
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accountRealtimeBalance.get("accentity")));
        conditionGroup.appendCondition(QueryCondition.name("currency").eq(accountRealtimeBalance.getCurrency()));
        conditionGroup.appendCondition(QueryCondition.name("bankaccount").eq(accountRealtimeBalance.get("enterpriseBankAccount")));
        conditionGroup.appendCondition(QueryCondition.name("tran_date").between(accountRealtimeBalance.get("balancedate"), accountRealtimeBalance.get("balancedate")));
        //期初数据不参与余额弥补，只用于对账
        conditionGroup.appendCondition(QueryCondition.name(BankReconciliation.INITFLAG).eq(false));
        schema.addCondition(conditionGroup);
        List<BankReconciliation> existBankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        //计算当日净收支额
        BigDecimal nowDayBankMoney = new BigDecimal(0);
        if (existBankReconciliationList != null && existBankReconciliationList.size() > 0) {
            // +贷的合计-借的合计
            for (BankReconciliation bankReconciliation : existBankReconciliationList) {
                //贷
                if (bankReconciliation.getDc_flag() != null && Direction.Credit.equals(bankReconciliation.getDc_flag())) {
                    nowDayBankMoney = nowDayBankMoney.add(bankReconciliation.getTran_amt());
                } else if (bankReconciliation.getDc_flag() != null && Direction.Debit.equals(bankReconciliation.getDc_flag())) {
                    //借
                    nowDayBankMoney = nowDayBankMoney.subtract(bankReconciliation.getTran_amt());
                }
            }
        }
        return nowDayBankMoney;
    }

    /**
     * 具体逻辑
     *
     * @param i
     * @return
     * @throws Exception
     */
    private void acctbalCompare(int i) throws Exception {
        //定义相等
        AtomicInteger failedCount = new AtomicInteger(0);
        //定义不相等
        AtomicInteger successCount = new AtomicInteger(0);
        //前天历史余额键值对
        Map<String, BigDecimal> beforeYesBalancesMap = new HashMap<>();
        Date beforeYesDate = DateUtils.strToDate(DateUtils.dateToStr(DateUtils.dateAddDays(DateUtils.getNow(), ~i)));
        //前天银行历史余额账户map1(银行账户+会计主体+币种,余额)
        List<AccountRealtimeBalance> beforeYesBalances = getRealtimeBalances(beforeYesDate);
        Date yesterdayDate = DateUtils.strToDate(DateUtils.dateToStr(DateUtils.dateAddDays(DateUtils.getNow(), ~i + 1)));
        //昨天历史余额
        List<AccountRealtimeBalance> yesterdayBalances = getRealtimeBalances(yesterdayDate);
        if (!CollectionUtils.isEmpty(beforeYesBalances)) {
            if (!CollectionUtils.isEmpty(yesterdayBalances)) {
                //解析前天历史余额信息
                getBeforeYes(beforeYesBalances, beforeYesBalancesMap);
                //昨天银行历史余额账户map2(银行账户+会计主体+币种,余额)
                Map<String, BigDecimal> yesterdayMap = new HashMap<>();
                Map<String, Long> idMap = new HashMap<>();
                //解析昨天历史余额信息
                analysisYesBalances(yesterdayBalances, yesterdayMap, idMap);
                //查询银行对账单map3(银行账户+会计主体+币种,计算余额)计算得到数据  前日余额+贷-借
                Map<String, BigDecimal> bankMap = new HashMap<>();
                //昨天银行对账单
                List<BankReconciliation> yesterdayBankReconciliationes = getBankReconciliationes(null, yesterdayDate);
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
                EntityTool.setUpdateStatus(yesterdayBalances);
                MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, yesterdayBalances);
            }
        }
        //查询银行账户期初表 获得总银行账户数
       // List<Map<String, Object>> initDataCount = getInitDataCount();
//        2023-03-29日，银行账户余额经过比对计算，XX个账户余额相等，XX个账户余额不等，XX个账户本日无余额
    }

    /**
     * 比较前天与昨天金额 并回写数据库
     *
     * @param yesterdayBalances
     * @param beforeYesBalancesMap
     * @param bankMap
     * @param idMap
     * @param successCount
     * @param failedCount
     * @throws Exception
     */
    private void compareResult(List<AccountRealtimeBalance> yesterdayBalances, Map<String, BigDecimal> beforeYesBalancesMap, Map<String, BigDecimal> bankMap, Map<String, Long> idMap
            , AtomicInteger successCount, AtomicInteger failedCount) throws Exception {
        if (!CollectionUtils.isEmpty(yesterdayBalances)) {
            List<AccountRealtimeBalance> accountRealtimeBalanceList = new ArrayList<>();
            yesterdayBalances.stream().forEach(e -> {
                if (e.getIsconfirm() == null || !e.getIsconfirm()) {
                    String keyb = e.getAccentity() + e.getCurrency() + e.getEnterpriseBankAccount();
                    AccountRealtimeBalance accountRealtimeBalance = new AccountRealtimeBalance();
                    accountRealtimeBalance.setId(idMap.get(keyb));
                    Boolean checkAccountFlag = Boolean.FALSE;
                    try {
                        checkAccountFlag = getCheckAccountBanlanceFlag(e.getAccentity());
                    } catch (Exception ex) {
                        log.error("检查历史余额与银行对账单余额是否相等，查询银行账户信息失败！");
                    }
                    //检查历史余额与银行对账单余额是否相等
                    if (checkAccountFlag) {
                        if (e.getDatasource() != null && BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode() != e.getDatasource()) {
                            QuerySchema querySchema = new QuerySchema().addSelect("id,accentity,bankaccount,currency,tran_date,tran_amt,dc_flag");
                            QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
                            queryConditionGroup.addCondition(QueryCondition.name("tran_date").eq(e.getBalancedate())); // 等于
                            //queryConditionGroup.addCondition(QueryCondition.name("accentity").eq(e.getAccentity()));
                            querySchema.addCondition(queryConditionGroup);
                            querySchema.addOrderBy("tran_time");
                            List<BankReconciliation> existBalances = null;
                            try {
                                existBalances = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
                            } catch (Exception ex) {
                                log.error("检查历史余额与银行对账单余额是否相等，查询银行对账单失败！");
                            }
                            if (null != existBalances && existBalances.size() > 0) {
                                int size = existBalances.size();
                                if (existBalances.get(size - 1).getTran_time() != null && existBalances.get(size - 1).getAcct_bal() != null) {
                                    //设置不相等
                                    accountRealtimeBalance.setBalancecontrast(BalanceContrast.Unequal.getValue());
//                                    accountRealtimeBalance.setBalancecheckinstruction(String.format("账户[%s]余额，与当日银行对账单[%s]的余额[%s]不相等，不允许进行余额确认，请核对后进行确认！",
//                                            bankAccount.get("name"), existBalances.get(size - 1).getTran_time(), existBalances.get(size - 1).getAcct_bal()));
                                    try {
                                        accountRealtimeBalance.setBalancecheckinstruction(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F537005B00009", "余额[%s]与当日银行对账单中[%s]的余额[%s]不相等，差额[%s];") /* "余额[%s]与当日银行对账单中[%s]的余额[%s]不相等，差额[%s];" */,
                                                dealMoneyDigit(accountRealtimeBalance, e.getAcctbal()),
                                                new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(existBalances.get(size - 1).getTran_time()),
                                                dealMoneyDigit(accountRealtimeBalance, existBalances.get(size - 1).getAcct_bal()),
                                                dealMoneyDigit(accountRealtimeBalance, e.getAcctbal().subtract(existBalances.get(size - 1).getAcct_bal()).abs())));
                                    } catch (Exception ex) {
                                        accountRealtimeBalance.setBalancecheckinstruction(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F537005B00009", "余额[%s]与当日银行对账单中[%s]的余额[%s]不相等，差额[%s];") /* "余额[%s]与当日银行对账单中[%s]的余额[%s]不相等，差额[%s];" */,
                                                accountRealtimeBalance, e.getAcctbal(),
                                                new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(existBalances.get(size - 1).getTran_time()),
                                                existBalances.get(size - 1).getAcct_bal(),
                                                e.getAcctbal().subtract(existBalances.get(size - 1).getAcct_bal()).abs()));
                                        log.error("1249检查历史余额与银行对账单余额是否相等，提示信息金额精度格式化失败！");
                                        ;
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
                                    List<BankReconciliation> bankReconciliations = getBankReconciliationes(e.getEnterpriseBankAccount(), e.getBalancedate());
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
                                    StringBuilder balancecheckinstruction = new StringBuilder("");
                                    if (StringUtils.isNotEmpty(accountRealtimeBalance.getBalancecheckinstruction())) {
                                        balancecheckinstruction = balancecheckinstruction.append(accountRealtimeBalance.getBalancecheckinstruction());
                                    }
                                    if (balancecheckinstruction.length() > 0) {
                                        accountRealtimeBalance.setBalancecheckinstruction(balancecheckinstruction + ";" + String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00186", "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]") /* "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]" */,
                                                dealMoneyDigit(e, beforeYesBalancesMap.get(keyb)),
                                                dealMoneyDigit(e, creMoney),
                                                dealMoneyDigit(e, dreMoney),
                                                dealMoneyDigit(e, bankMap.get(keyb)),
                                                dealMoneyDigit(e, e.getAcctbal()),
                                                dealMoneyDigit(e, e.getAcctbal().subtract(bankMap.get(keyb)).abs())));
                                    } else {
                                        accountRealtimeBalance.setBalancecheckinstruction(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00186", "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]") /* "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]" */,
                                                dealMoneyDigit(e, beforeYesBalancesMap.get(keyb)),
                                                dealMoneyDigit(e, creMoney),
                                                dealMoneyDigit(e, dreMoney),
                                                dealMoneyDigit(e, bankMap.get(keyb)),
                                                dealMoneyDigit(e, e.getAcctbal()),
                                                dealMoneyDigit(e, e.getAcctbal().subtract(bankMap.get(keyb)).abs())));
                                    }
                                } catch (Exception ex) {
                                    log.error(ex.getMessage());
                                }

                                failedCount.incrementAndGet();
                            } else {
                                //设置相等
                                accountRealtimeBalance.setBalancecheckinstruction(null);
                                accountRealtimeBalance.setBalancecontrast(BalanceContrast.Equal.getValue());
                                successCount.incrementAndGet();
                            }
                        } else {
                            if (beforeYesBalancesMap.get(keyb).equals(e.getAcctbal())) {
                                //设置相等
                                accountRealtimeBalance.setBalancecheckinstruction(null);
                                accountRealtimeBalance.setBalancecontrast(BalanceContrast.Equal.getValue());
                                successCount.incrementAndGet();
                            } else {
                                try {
                                    List<BankReconciliation> bankReconciliations = getBankReconciliationes(e.getEnterpriseBankAccount(), e.getBalancedate());
                                    BigDecimal creMoney = getBankReconciliationesMoney(bankReconciliations, 1);
                                    BigDecimal dreMoney = getBankReconciliationesMoney(bankReconciliations, 2);
                                    //计算余额余额
                                    BigDecimal calculateBalance = dealMoneyDigit(e, beforeYesBalancesMap.get(keyb).add(creMoney).subtract(dreMoney));
                                    if (calculateBalance.equals(e.getAcctbal())) {
                                        accountRealtimeBalance.setBalancecheckinstruction(null);
                                        accountRealtimeBalance.setBalancecontrast(BalanceContrast.Equal.getValue());
                                        successCount.incrementAndGet();
                                    } else {
                                        accountRealtimeBalance.setBalancecontrast(BalanceContrast.Unequal.getValue());
                                        accountRealtimeBalance.setBalancecheckinstruction(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00186", "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]") /* "昨日余额[%s]，当日收入[%s]，支出[%s]，计算余额[%s]与当日余额[%s]不相等，差额[%s]" */,
                                                dealMoneyDigit(e, beforeYesBalancesMap.get(keyb)),
                                                dealMoneyDigit(e, creMoney),
                                                dealMoneyDigit(e, dreMoney),
                                                dealMoneyDigit(e, beforeYesBalancesMap.get(keyb)),
                                                dealMoneyDigit(e, e.getAcctbal()),
                                                dealMoneyDigit(e, beforeYesBalancesMap.get(keyb).subtract(e.getAcctbal()).abs())));
                                        failedCount.incrementAndGet();
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
            });
            if (!CollectionUtils.isEmpty(accountRealtimeBalanceList)) {
                EntityTool.setUpdateStatus(accountRealtimeBalanceList);
                MetaDaoHelper.update(AccountRealtimeBalance.ENTITY_NAME, accountRealtimeBalanceList);
            }
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
                String keyr = e.getAccentity() + e.getCurrency() + e.getBankaccount();
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

    /**
     * 查询银行对账单收入和支出
     *
     * @param existBalances
     * @param flag          1 收入，2 支出
     * @return
     * @throws Exception
     */
    private BigDecimal getBankReconciliationesMoney(List<BankReconciliation> existBalances, int flag) throws Exception {
        BigDecimal targetMoney = BigDecimal.ZERO;
        for (BankReconciliation existBalance : existBalances) {
            if (flag == 1) {
                //贷
                if (existBalance.getDc_flag() != null && Direction.Credit.equals(existBalance.getDc_flag())) {
                    targetMoney = targetMoney.add(existBalance.getTran_amt());
                }
            } else if (flag == 2) {
                if (existBalance.getDc_flag() != null && Direction.Debit.equals(existBalance.getDc_flag())) {
                    targetMoney = targetMoney.add(existBalance.getTran_amt());
                }
            }
        }
        return targetMoney;
    }

    //精度处理
    private BigDecimal dealMoneyDigit(AccountRealtimeBalance accountRealtimeBalance, BigDecimal targetMoney) throws Exception {
        //精度处理
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(accountRealtimeBalance.get("currency"));
        Integer moneydigit = currencyDTO.getMoneydigit();
        RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(accountRealtimeBalance.get("currency"), 1);
        CheckPrecisionVo checkPrecisionVo = new CheckPrecisionVo();
        checkPrecisionVo.setPrecisionId(accountRealtimeBalance.get("currency"));
        return targetMoney.setScale(moneydigit, moneyRound);
    }

    /**
     * 获取租户下银行账户期初个数
     *
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getInitDataCount() throws Exception {
        QuerySchema queryInitDataCount = QuerySchema.create().addSelect("count(id)");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("qzbz").eq(true)); // 是否期初
        queryConditionGroup.addCondition(QueryCondition.name("moneyform").eq(MoneyForm.bankaccount.getValue()));
        queryInitDataCount.addCondition(queryConditionGroup);
        return MetaDaoHelper.query(InitData.ENTITY_NAME, queryInitDataCount);
    }

    /**
     * 查询历史余额数据
     *
     * @param startDate
     * @return
     * @throws Exception
     */
    private List<AccountRealtimeBalance> getRealtimeBalances(Date startDate) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("id,accentity,enterpriseBankAccount,currency,balancedate,acctbal,isconfirm");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("balancedate").eq(startDate)); // 等于
        queryConditionGroup.addCondition(QueryCondition.name("first_flag").eq("0"));
        querySchema.addCondition(queryConditionGroup);
        List<AccountRealtimeBalance> existBalances = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, querySchema, null);
        return existBalances;
    }

    /**
     * 查询银行对账单
     *
     * @param startDate
     * @return
     * @throws Exception
     */
    private List<BankReconciliation> getBankReconciliationes(String bankaccount, Date startDate) throws Exception {
        QuerySchema querySchema = new QuerySchema().addSelect("id,accentity,bankaccount,currency,tran_date,tran_amt,dc_flag");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
        queryConditionGroup.addCondition(QueryCondition.name("tran_date").eq(startDate)); // 等于
        if (StringUtils.isNotEmpty(bankaccount)) {
            queryConditionGroup.addCondition(QueryCondition.name("bankaccount").eq(bankaccount));
        }
        querySchema.addCondition(queryConditionGroup);
        List<BankReconciliation> existBalances = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return existBalances;
    }

    //银行账户是否开通银企联服务
    private Boolean getCheckAccountBanlanceFlag(String accentity) throws Exception {
        //Boolean openFlag = Boolean.FALSE;
        //根据会计主体查询配置的现金参数-是否进行历史余额与对账单余额比对参数
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity));
        querySchema1.addCondition(group1);
        List<AutoConfig> configList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (configList == null || configList.size() == 0) {
            return Boolean.FALSE;
        } else {
            return configList.get(0).getCheckaccountbanlance();
        }
    }


    /**
     * 历史数据升级
     *
     * @param schema
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject updateHistory(String schema) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        Map<String, Object> schemaParams = new HashMap<>();
        schemaParams.put("schema", schema);
        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.UpdateBankTypeMapper.updateRealtimebalanceBankType", schemaParams);
        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.UpdateBankTypeMapper.updateBankreconciliationBankType", schemaParams);
        return responseMsg;
    }

    /**
     * 历史数据升级2（银行账户交易明细，银行账户交易回单）
     *
     * @param schema
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject updateHistoryV2(String schema) throws Exception {
        CtmJSONObject responseMsg = new CtmJSONObject();
        Map<String, Object> schemaParams = new HashMap<>();
        schemaParams.put("schema", schema);
        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.UpdateBankTypeMapper.updateBankElectronicBankType", schemaParams);
        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.UpdateBankTypeMapper.updateBankDealDetailBankType", schemaParams);
        return responseMsg;
    }
}
