package com.yonyoucloud.fi.cmp.workbench.flowbench.service.impl;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.BdRequestParams;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.basedoc.service.itf.IExchangeRateTypeService;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.count.BankreconciliationCountServiceImpl;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.count.ClaimCenterCountServiceImpl;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.count.MybillclaimCountServiceImpl;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.event.utils.DateEventUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.PageUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.weekday.DateUtil;
import com.yonyoucloud.fi.cmp.workbench.flowbench.FlowWorkbench;
import com.yonyoucloud.fi.cmp.workbench.flowbench.dto.req.FlowBenchVO;
import com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp.*;
import com.yonyoucloud.fi.cmp.workbench.flowbench.service.IFlowBenchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryJoin;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 流水工作台实现类
 * 所属组织 企业银行账号节点 账号所属组织
 * 适用组织 企业银行账号 账号适用范围
 */
@Service
@Slf4j
public class FlowBenchServiceImpl implements IFlowBenchService {
    private static final String ORG_ENTITYNAME = "aa.baseorg.OrgMV";
    private static final String CURRENCY_FULL_NAME = "bd.enterprise.BankAcctCurrencyVO";

    @Autowired
    private YmsOidGenerator ymsOidGenerator;
    @Autowired
    private IExchangeRateTypeService exchangeRateTypeService;
    @Autowired
    private OrgDataPermissionService orgDataPermissionService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;
    /**
     * 我的认领中心
     */
    @Autowired
    private MybillclaimCountServiceImpl mybillclaimCountService;
    /**
     * 银行对账单
     */
    @Autowired
    private BankreconciliationCountServiceImpl bankreconciliationCountServiceImpl;
    /**
     * 到账认领中心
     */
    @Autowired
    private ClaimCenterCountServiceImpl claimCenterCountService;


    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    @Override
    @Transactional
    public FlowBenchVO saveView(FlowBenchVO vo) throws Exception {
        FlowWorkbench flowWorkbench = new FlowWorkbench();
        flowWorkbench.setViewName(vo.getViewName());
        if (StringUtils.isNotEmpty(vo.getAccentity())) {
            flowWorkbench.setAccentity(vo.getAccentity());
        } else {
            flowWorkbench.setAccentity(null);
        }
        flowWorkbench.setStartDate(DateEventUtils.parseDateStr(vo.getDateRange()[0].substring(0, 10)));
        flowWorkbench.setEndDate(DateEventUtils.parseDateStr(vo.getDateRange()[1].substring(0, 10)));
        if (StringUtils.isNotEmpty(vo.getAccountNo())) {
            flowWorkbench.setAccountNo(vo.getAccountNo());
        } else {
            flowWorkbench.setAccountNo(null);
        }
        if (StringUtils.isNotEmpty(vo.getAccountCurrency())) {
            flowWorkbench.setAccountCurrency(vo.getAccountCurrency());
        } else {
            flowWorkbench.setAccountCurrency(null);
        }
        flowWorkbench.setWarningDays(vo.getWarningDays() == null ? 7 : vo.getWarningDays());
        flowWorkbench.setCurrency(vo.getCurrency());
        flowWorkbench.setExchangeRateType(vo.getExchangeRateType());
        flowWorkbench.setCurrencyUnit(Short.parseShort(vo.getCurrencyUnit()));
        if (vo.getIsDefault() != null && vo.getIsDefault() == 1) {
            flowWorkbench.setIsDefault((short) 1);
        } else {
            flowWorkbench.setIsDefault((short) 0);
        }
        flowWorkbench.setIorder(vo.getIorder() == null ? 999 : vo.getIorder());
        flowWorkbench.setIsPreset((short) (vo.getIsPreset() == null ? 0 : 1));
        try {
            if (vo.getId() != null) {
                boolean flag = existsById(vo.getId());
                flowWorkbench.setId(vo.getId());
                flowWorkbench.setUserId(InvocationInfoProxy.getUserid());
                if (flag) {
                    flowWorkbench.setModifyDate(new Date());
                    flowWorkbench.setModifyTime(new Date());
                    flowWorkbench.setModifier(InvocationInfoProxy.getUsername());
                    flowWorkbench.setModifierId(Long.parseLong(InvocationInfoProxy.getIdentityId()));
                    flowWorkbench.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(FlowWorkbench.ENTITY_NAME, flowWorkbench);
                } else {
                    flowWorkbench.setCreateDate(new Date());
                    flowWorkbench.setCreateTime(new Date());
                    flowWorkbench.setCreator(InvocationInfoProxy.getUsername());
                    flowWorkbench.setCreatorId(Long.parseLong(InvocationInfoProxy.getIdentityId()));
                    flowWorkbench.setEntityStatus(EntityStatus.Insert);
                    CmpMetaDaoHelper.insert(FlowWorkbench.ENTITY_NAME, flowWorkbench);
                }

            } else {
                vo.setId(ymsOidGenerator.nextId());
                flowWorkbench.setId(vo.getId());
                flowWorkbench.setUserId(InvocationInfoProxy.getUserid());
                flowWorkbench.setCreateDate(new Date());
                flowWorkbench.setCreateTime(new Date());
                flowWorkbench.setCreator(InvocationInfoProxy.getUsername());
                flowWorkbench.setCreatorId(Long.parseLong(InvocationInfoProxy.getIdentityId()));
                flowWorkbench.setEntityStatus(EntityStatus.Insert);
                CmpMetaDaoHelper.insert(FlowWorkbench.ENTITY_NAME, flowWorkbench);
            }
            return this.selectById(vo.getId());
        } catch (Exception e) {
            log.error("新增/修改失败 ", e);
        }
        return null;
    }

    @Override
    @Transactional
    public void batchDeleteView(List<Long> ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id,isDefault")
                .appendQueryCondition(
                        QueryCondition.name("id").in(ids),
                        QueryCondition.name("userId").eq(InvocationInfoProxy.getUserid()));
        List<BizObject> bizObjects = MetaDaoHelper.queryObject(FlowWorkbench.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(bizObjects)) {
            log.error("根据id：{}未查询到数据", ids);
            return;
        }
        bizObjects.forEach(item -> item.setEntityStatus(EntityStatus.Delete));
        MetaDaoHelper.delete(FlowWorkbench.ENTITY_NAME, bizObjects);
    }

    @Override
    public void initData() throws Exception {
        //将iorder=1 认为预置数据
        QuerySchema querySchema = QuerySchema.create().addSelect("id")
                .appendQueryCondition(
                        QueryCondition.name("isPreset").eq(1),
                        QueryCondition.name("userId").eq(InvocationInfoProxy.getUserid())
                );
        List<BizObject> list = MetaDaoHelper.queryObject(FlowWorkbench.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(list)) {
            FlowBenchVO vo = new FlowBenchVO();
            vo.setViewName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540079F", "默认视图") /* "默认视图" */);
            Date startDate = DateUtils.dateAddDays(new Date(), -30);
            vo.setDateRange(new String[]{DateUtils.formatDate(startDate), DateUtils.formatDate(new Date())});
            vo.setWarningDays(7); //默认预警天数
            vo.setCurrency(this.queryCurrencyId()); //币种 默认为人民币
            vo.setExchangeRateType(this.queryDefaltExchangetype()); // 默认基准汇率
            vo.setCurrencyUnit("2"); //金额单位 默认为万元
            vo.setIsDefault(1);
            vo.setIorder(1);
            vo.setIsPreset(1);
            this.saveView(vo);
        }
    }

    @Override
    public List<Map<String, Object>> queryUnImportAccount(FlowBenchVO flowBenchVO) throws Exception {
        //未接入/导入账户数
        Set<String> accountSet = new HashSet<>();
        List<Map<String, Object>> accountMapList = new ArrayList<>();
        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());
        if (benchVO == null) {
            return accountMapList;
        }
        String[] dataRange = benchVO.getDateRange();
        Date startDate = DateUtils.parseDate(dataRange[0]);
        Date endDate = DateUtils.parseDate(dataRange[1]);
        //银行账户期初节点 ficmp0008 , cmp_initdatayhlist ,cmp.initdata.InitData,cmp_initdata(主) cmp_initdata_b(子表)
        fillOrgAndAccount(benchVO, IServicecodeConstant.BANKRECEIPTMATCH, false);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return accountMapList;
        }
        Set<String> orgs = new HashSet<>();
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = new ArrayList<>();
        if (StringUtils.isNotEmpty(benchVO.getAccentity())) {
            orgs = Arrays.stream(benchVO.getAccentity().split(",")).collect(Collectors.toSet());
        } else {
            orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.BANKRECEIPTMATCH);
        }
        if (CollectionUtils.isEmpty(orgs)) {
            return accountMapList;
        }
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            List<String> accountIds = accountNoList.stream().filter(item -> item.containsKey("id") && item.get("id") != null).map(item -> (String) item.get("id")).collect(Collectors.toList());

            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setIdList(accountIds);
            enterpriseParams.setOrgidList(new ArrayList<>(orgs));
            List<EnterpriseBankAcctVOWithRange> bankAcctVOWithRanges = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
            if (CollectionUtils.isEmpty(bankAcctVOWithRanges)) {
                return accountMapList;
            }
            enterpriseBankAcctVOS = bankAcctVOWithRanges;
        } else {
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setOrgidList(new ArrayList<>(orgs));
            enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
            if (CollectionUtils.isEmpty(enterpriseBankAcctVOS)) {
                return accountMapList;
            }
        }

        List<Date> everyDays = everyDateOfDates(startDate, endDate, true);
        if (CollectionUtils.isEmpty(everyDays)) {
            return accountMapList;
        }
        Set<String> accountIds = enterpriseBankAcctVOS.stream().map(item -> item.getId()).collect(Collectors.toSet());
        QuerySchema balanceSchema = QuerySchema.create().addSelect("id,balancedate,isconfirm,enterpriseBankAccount,currency")
                .appendQueryCondition(
                        QueryCondition.name("enterpriseBankAccount").in(accountIds),
                        QueryCondition.name("balancedate").between(startDate, endDate)
                );
        //银行账户历史余额 ficmp0031,cmp_hisbalist, cmp.accountrealtimebalance.AccountRealtimeBalance,cmp_bankaccount_realtimebalance
        List<AccountRealtimeBalance> balanceList = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, balanceSchema, null);

        QuerySchema dealSchema = QuerySchema.create().addSelect("id,enterpriseBankAccount,currency,tranDate")
                .appendQueryCondition(
                        QueryCondition.name("enterpriseBankAccount").in(accountIds),
                        QueryCondition.name("tranDate").between(startDate, endDate)
                );
        List<BankDealDetail> dealList = MetaDaoHelper.queryObject(BankDealDetail.ENTITY_NAME, dealSchema, null);
        Map<String, List<BankDealDetail>> dealListMap = dealList.stream().collect(Collectors.groupingBy(item -> item.getEnterpriseBankAccount() + "_" + item.getCurrency() + "_" + DateEventUtils.getDate(item.getTranDate())));


        if (CollectionUtils.isEmpty(balanceList)) {
            for (Date date : everyDays) {
                //流水缺失 计数(银行账号)
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS) {
                    if (CollectionUtils.isEmpty(enterpriseBankAcctVOWithRange.getCurrencyList())) {
                        continue;
                    }
                    List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVOWithRange.getCurrencyList().stream().filter(item -> item.getCurrencyEnable() != null && item.getCurrencyEnable() == 1).collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(currencyList)) {
                        continue;
                    }
                    for (BankAcctCurrencyVO currencyVO : currencyList) {
                        balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);

                    }
                }
            }
        } else {
            Map<String, List<AccountRealtimeBalance>> balanceByAccountMap = balanceList.stream().collect(Collectors.groupingBy(AccountRealtimeBalance::getEnterpriseBankAccount));


            for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS) {
                List<AccountRealtimeBalance> balanceByAccountList = balanceByAccountMap.get(enterpriseBankAcctVOWithRange.getId());

                if (CollectionUtils.isEmpty(enterpriseBankAcctVOWithRange.getCurrencyList())) {
                    continue;
                }
                //币种保留enable=1的数据;
                List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVOWithRange.getCurrencyList().stream().filter(item -> item.getCurrencyEnable() != null && item.getCurrencyEnable() == 1).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(currencyList)) {
                    continue;
                }
                for (BankAcctCurrencyVO currencyVO : currencyList) {
                    if (CollectionUtils.isNotEmpty(balanceByAccountList)) {
                        Map<String, List<AccountRealtimeBalance>> balanceByCurrencyMap = balanceByAccountList.stream().collect(Collectors.groupingBy(AccountRealtimeBalance::getCurrency));
                        if (balanceByCurrencyMap.containsKey(currencyVO.getCurrency())) {
                            List<AccountRealtimeBalance> balanceByCurrencyList = balanceByCurrencyMap.get(currencyVO.getCurrency());
                            //流水缺失/不等检查
                            Map<Date, AccountRealtimeBalance> balanceDataByAccountMap = new HashMap<>();
                            if (CollectionUtils.isNotEmpty(balanceByCurrencyList)) {
                                balanceDataByAccountMap = balanceByCurrencyList.stream().collect(Collectors.toMap(item -> item.getBalancedate(), Function.identity(), (k1, k2) -> k1));
                            }
                            for (Date date : everyDays) {
                                if (balanceDataByAccountMap.containsKey(date)) {
                                    AccountRealtimeBalance balance = balanceDataByAccountMap.get(date);
                                    if (balance.getIsconfirm()) {
                                        continue;
                                    }
                                    //不等检查余额不等 余额对比结果=不相等
                                    if (!balance.getIsconfirm()) {
                                        //流水接入/导入提醒
                                        //银行交易流水 serviceCode ficmp0018  billnum cmp_dllist uri cmp.bankdealdetail.BankDealDetail  tablename cmp_bankdealdetail
                                        balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);
                                    }
                                } else {
                                    balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);
                                }
                            }
                        } else {
                            for (Date date : everyDays) {
                                balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);
                            }
                        }
                    } else {
                        for (Date date : everyDays) {
                            balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);
                        }
                    }
                }
            }
        }
        return accountMapList;
    }

    @Override
    public List<Map<String, Object>> queryBalanceListData(FlowBenchVO flowBenchVO, int type) throws Exception {
        Set<String> balanceUnEqualAccountSet = new HashSet<>();
        Set<String> balanceMissingAccountSet = new HashSet<>();

        //不符记录(缺失+不等)
        List<Map<String, Object>> balanceUnMatchAccounts = new ArrayList<>();
        //缺失记录
        List<Map<String, Object>> balanceMissings = new ArrayList<>();

        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());
        if (benchVO == null) {
            return balanceUnMatchAccounts;
        }
        String[] dataRange = benchVO.getDateRange();
        Date startDate = DateUtils.parseDate(dataRange[0]);
        Date endDate = DateUtils.parseDate(dataRange[1]);
        //银行账户期初节点 ficmp0008 , cmp_initdatayhlist ,cmp.initdata.InitData,cmp_initdata(主) cmp_initdata_b(子表)
        fillOrgAndAccount(benchVO, IServicecodeConstant.BANKRECEIPTMATCH, false);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return balanceUnMatchAccounts;
        }
        Set<String> orgs = new HashSet<>();
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = new ArrayList<>();
        if (StringUtils.isNotEmpty(benchVO.getAccentity())) {
            orgs = Arrays.stream(benchVO.getAccentity().split(",")).collect(Collectors.toSet());
        } else {
            orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.BANKRECEIPTMATCH);
        }
        if (CollectionUtils.isEmpty(orgs)) {
            return balanceUnMatchAccounts;
        }
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            List<String> accountIds = accountNoList.stream().filter(item -> item.containsKey("id") && item.get("id") != null).map(item -> (String) item.get("id")).collect(Collectors.toList());

            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setIdList(accountIds);
            enterpriseParams.setOrgidList(new ArrayList<>(orgs));
            List<EnterpriseBankAcctVOWithRange> bankAcctVOWithRanges = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
            if (CollectionUtils.isEmpty(bankAcctVOWithRanges)) {
                return balanceUnMatchAccounts;
            }
            enterpriseBankAcctVOS = bankAcctVOWithRanges;
        } else {
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setOrgidList(new ArrayList<>(orgs));
            enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
            if (CollectionUtils.isEmpty(enterpriseBankAcctVOS)) {
                return balanceUnMatchAccounts;
            }
        }

        List<Date> everyDays = everyDateOfDates(startDate, endDate, true);
        if (CollectionUtils.isEmpty(everyDays)) {
            return balanceUnMatchAccounts;
        }
        Set<String> accountIds = enterpriseBankAcctVOS.stream().map(item -> item.getId()).collect(Collectors.toSet());
        QuerySchema balanceSchema = QuerySchema.create().addSelect("id,balancedate,isconfirm,enterpriseBankAccount,currency")
                .appendQueryCondition(
                        QueryCondition.name("enterpriseBankAccount").in(accountIds),
                        QueryCondition.name("balancedate").between(startDate, endDate)
                );
        //银行账户历史余额 ficmp0031,cmp_hisbalist, cmp.accountrealtimebalance.AccountRealtimeBalance,cmp_bankaccount_realtimebalance
        List<AccountRealtimeBalance> balanceList = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, balanceSchema, null);

        QuerySchema dealSchema = QuerySchema.create().addSelect("id,enterpriseBankAccount,currency,tranDate")
                .appendQueryCondition(
                        QueryCondition.name("enterpriseBankAccount").in(accountIds),
                        QueryCondition.name("tranDate").between(startDate, endDate)
                );
        List<BankDealDetail> dealList = MetaDaoHelper.queryObject(BankDealDetail.ENTITY_NAME, dealSchema, null);
        Map<String, List<BankDealDetail>> dealListMap = dealList.stream().collect(Collectors.groupingBy(item -> item.getEnterpriseBankAccount() + "_" + item.getCurrency() + "_" + DateEventUtils.getDate(item.getTranDate())));


        if (CollectionUtils.isEmpty(balanceList)) {
            for (Date date : everyDays) {
                //流水缺失 计数(银行账号)
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS) {
                    if (CollectionUtils.isEmpty(enterpriseBankAcctVOWithRange.getCurrencyList())) {
                        continue;
                    }
                    List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVOWithRange.getCurrencyList().stream().filter(item -> item.getCurrencyEnable() != null && item.getCurrencyEnable() == 1).collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(currencyList)) {
                        continue;
                    }
                    for (BankAcctCurrencyVO currencyVO : currencyList) {
                        balanceMissingInfo(enterpriseBankAcctVOWithRange, currencyVO, date, balanceMissingAccountSet, balanceMissings, balanceUnMatchAccounts);

                    }
                }
            }
        } else {
            Map<String, List<AccountRealtimeBalance>> balanceByAccountMap = balanceList.stream().collect(Collectors.groupingBy(AccountRealtimeBalance::getEnterpriseBankAccount));


            for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS) {
                List<AccountRealtimeBalance> balanceByAccountList = balanceByAccountMap.get(enterpriseBankAcctVOWithRange.getId());

                if (CollectionUtils.isEmpty(enterpriseBankAcctVOWithRange.getCurrencyList())) {
                    continue;
                }
                //币种保留enable=1的数据;
                List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVOWithRange.getCurrencyList().stream().filter(item -> item.getCurrencyEnable() != null && item.getCurrencyEnable() == 1).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(currencyList)) {
                    continue;
                }
                for (BankAcctCurrencyVO currencyVO : currencyList) {
                    if (CollectionUtils.isNotEmpty(balanceByAccountList)) {
                        Map<String, List<AccountRealtimeBalance>> balanceByCurrencyMap = balanceByAccountList.stream().collect(Collectors.groupingBy(AccountRealtimeBalance::getCurrency));
                        if (balanceByCurrencyMap.containsKey(currencyVO.getCurrency())) {
                            List<AccountRealtimeBalance> balanceByCurrencyList = balanceByCurrencyMap.get(currencyVO.getCurrency());
                            //流水缺失/不等检查
                            Map<Date, AccountRealtimeBalance> balanceDataByAccountMap = new HashMap<>();
                            if (CollectionUtils.isNotEmpty(balanceByCurrencyList)) {
                                balanceDataByAccountMap = balanceByCurrencyList.stream().collect(Collectors.toMap(item -> item.getBalancedate(), Function.identity(), (k1, k2) -> k1));
                            }
                            for (Date date : everyDays) {
                                if (balanceDataByAccountMap.containsKey(date)) {
                                    AccountRealtimeBalance balance = balanceDataByAccountMap.get(date);
                                    if (balance.getIsconfirm()) {
                                        continue;
                                    }
                                    //不等检查余额不等 余额对比结果=不相等
                                    if (!balance.getIsconfirm()) {
                                        if (balance.getBalancecontrast() != null && balance.getBalancecontrast() == 0) {
                                            //计数(银行账号+余额日期)
                                            Map<String, Object> tmp = new HashMap<>();
                                            tmp.put("code", enterpriseBankAcctVOWithRange.getCode());
                                            tmp.put("name", enterpriseBankAcctVOWithRange.getName());
                                            tmp.put("orgId", enterpriseBankAcctVOWithRange.getOrgid());
                                            tmp.put("orgName", enterpriseBankAcctVOWithRange.getOrgidName());
                                            tmp.put("enterpriseBankAccount", enterpriseBankAcctVOWithRange.getId());
                                            tmp.put("account", enterpriseBankAcctVOWithRange.getAccount());
                                            tmp.put("accountName", enterpriseBankAcctVOWithRange.getName());
                                            tmp.put("currency", currencyVO.getCurrency());
                                            tmp.put("currencyName", currencyVO.getCurrencyName());
                                            tmp.put("date", date);
                                            tmp.put("checkResult", balance.get("balancecheckinstruction"));
                                            balanceUnMatchAccounts.add(tmp);
                                        }
                                    }
                                } else {
                                    balanceMissingInfo(enterpriseBankAcctVOWithRange, currencyVO, date, balanceMissingAccountSet, balanceMissings, balanceUnMatchAccounts);
                                }
                            }
                        } else {
                            for (Date date : everyDays) {
                                balanceMissingInfo(enterpriseBankAcctVOWithRange, currencyVO, date, balanceMissingAccountSet, balanceMissings, balanceUnMatchAccounts);
                            }
                        }
                    } else {
                        for (Date date : everyDays) {
                            balanceMissingInfo(enterpriseBankAcctVOWithRange, currencyVO, date, balanceMissingAccountSet, balanceMissings, balanceUnMatchAccounts);
                        }
                    }
                }
            }
        }
        if (type == 1) {
            return balanceUnMatchAccounts;
        } else {
            return balanceMissings;
        }
    }


    @Override
    public List<FlowBenchVO> list() {
        List<FlowBenchVO> result = new ArrayList<>();
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM + " ,currency.name,exchangeRateType.name")
                .appendQueryCondition(QueryCondition.name("userId").eq(InvocationInfoProxy.getUserid()))
                .addOrderBy("isDefault desc,iorder asc");
        try {
            List<BizObject> workbenches = MetaDaoHelper.queryObject(FlowWorkbench.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(workbenches)) {
                List<String> accentityList = new ArrayList<>();
                List<String> accountCurrencyList = new ArrayList<>();
                for (BizObject bizObject : workbenches) {
                    if (bizObject.containsKey("accentity")) {
                        accentityList.addAll(Arrays.asList(bizObject.getString("accentity").split(",")));
                    }
                    if (bizObject.containsKey("accountCurrency")) {
                        accountCurrencyList.addAll(Arrays.asList(bizObject.getString("accountCurrency").split(",")));
                    }
                }
                Map<String, BizObject> accountMap = new HashMap();
                if (CollectionUtils.isNotEmpty(accentityList)) {
                    QuerySchema accentitySchema = QuerySchema.create().addSelect("id,name").appendQueryCondition(QueryCondition.name("id").in(accentityList));
                    List<BizObject> accentitys = MetaDaoHelper.queryObject(ORG_ENTITYNAME, accentitySchema, "ucf-org-center");
                    accountMap = accentitys.stream().collect(Collectors.toMap(item -> item.getString("id"), item -> item));
                }
                Map<String, BizObject> accountNoMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(accountCurrencyList)) {
                    //根据企业银行币种的id查询对应的币种和账号信息
                    QuerySchema accountNoSchema = QuerySchema.create().addSelect("id,currency,bankacct.id,bankacct.code,bankacct.name")
                            .appendQueryCondition(QueryCondition.name("id").in(accountCurrencyList));
                    List<BizObject> accountNos = MetaDaoHelper.queryObject(CURRENCY_FULL_NAME, accountNoSchema, "ucfbasedoc");
                    accountNoMap = accountNos.stream().collect(Collectors.toMap(item -> item.getString("id"), item -> item));
                }
                Map<String, BizObject> finalAccountMap = accountMap;
                Map<String, BizObject> finalAccountNoMap = accountNoMap;
                result = workbenches.stream().map(item -> this.buildByEntity(item, finalAccountMap, finalAccountNoMap)).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("查询失败！", e);
        }
        return result;
    }

    @Override
    public boolean existsById(Long id) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id").appendQueryCondition(QueryCondition.name("id").eq(id));
        List<BizObject> workbenches = MetaDaoHelper.queryObject(FlowWorkbench.ENTITY_NAME, querySchema, null);
        return CollectionUtils.isNotEmpty(workbenches);
    }

    @Override
    public FlowBenchVO queryDefault(String userid) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id").appendQueryCondition(
                QueryCondition.name("isPreset").eq(1),
                QueryCondition.name("userId").eq(userid)
        );
        List<BizObject> workbenches = MetaDaoHelper.queryObject(FlowWorkbench.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(workbenches)) {
            Long id = workbenches.get(0).getLong("id");
            return this.selectById(id);
        }
        return null;
    }


    @Override
    public FlowBenchVO selectById(Long id) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM + " ,currency.name,exchangeRateType.name")
                .appendQueryCondition(QueryCondition.name("id").eq(id));
        List<BizObject> workbenches = MetaDaoHelper.queryObject(FlowWorkbench.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(workbenches)) {
            log.error("根据id:{}未查询到流水工作台视图", id);
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101681"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C8752C405F00006","流水工作台视图不存在！"));
            return null;
        }
        BizObject bizObject = workbenches.get(0);
        Map<String, BizObject> accentityMap = new HashMap<>();
        if (bizObject.get("accentity") != null) {
            String[] accentity = bizObject.getString("accentity").split(",");
            QuerySchema accentitySchema = QuerySchema.create().addSelect("id,name").appendQueryCondition(
                    QueryCondition.name("id").in(accentity)
            );
            List<BizObject> accentityList = MetaDaoHelper.queryObject(ORG_ENTITYNAME, accentitySchema, "ucf-org-center");
            accentityMap = accentityList.stream().collect(Collectors.toMap(item -> item.getString("id"), item -> item));
        }

        Map<String, BizObject> accountNoMap = new HashMap<>();
        if (bizObject.get("accountCurrency") != null) {
            List<String> accountCurrencyList = Arrays.asList(bizObject.getString("accountCurrency").split(","));
            QuerySchema accountNoSchema = QuerySchema.create().addSelect("id,currency,bankacct.id,bankacct.code,bankacct.name")
                    .appendQueryCondition(QueryCondition.name("id").in(accountCurrencyList));
            List<BizObject> accountNos = MetaDaoHelper.queryObject(CURRENCY_FULL_NAME, accountNoSchema, "ucfbasedoc");
            accountNoMap = accountNos.stream().collect(Collectors.toMap(item -> item.getString("id"), item -> item));
        }
        return this.buildByEntity(workbenches.get(0), accentityMap, accountNoMap);
    }


    /**
     * 银行账号参照ucfbasedoc.bd_enterprisebankacct 元数据uri bd.enterprise.OrgFinBankacctVO tablename iuap_apdoc_basedoc.org_fin_bankacct
     * 银行流水处理 ficmp0006(银行流水认领)
     * 银行对账单 cmp.bankreconciliation.BankReconciliation/ billnum cmp_bankreconciliationlist /table cmp_bankreconciliation
     * 到账认领中心  cmp_billclaimcenterlist
     * 到账认领中心 cmp.bankreconciliation.BankReconciliation/ billnum cmp_billclaimcenterlist/
     * <p>
     * 退票状态 com.yonyoucloud.fi.cmp.cmpentity.RefundStatus
     * 业务关联状态枚举 com.yonyoucloud.fi.cmp.cmpentity.BillProcessFlag
     * 流水处理状态 processstatus com.yonyoucloud.fi.cmp.intelligentdealdetail.dealdetailconst.DealDetailEnumConst.DealDetailProcessStatusEnum
     * 参考 com.yonyoucloud.fi.cmp.bankreconciliation.service.BankreconciliationServiceImpl#getCount(com.yonyou.yonbip.ctm.json.CtmJSONObject)
     *
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    @Override
    public FlowTodoVO queryFlowTodo(FlowBenchVO flowBenchVO) throws Exception {
        FlowTodoVO vo = new FlowTodoVO();
        vo.setOrgConfirmNum(0L); // 待确认使用组织（账户使用组织为空）
        vo.setRefundConfirmNm(0L); //疑似退票待确认
        vo.setAutoHandleConfirmNum(0L); //自动处理待确认
        vo.setTodoProcessingNum(0L); //待处理
        vo.setProcessingNum(0L); //发布处理中
        vo.setAutorelationConfirming(0L);
        vo.setAutocreatebillConfirming(0L);

        vo.setCommonOrgConfirmNum(0L);
        vo.setCommonTodoClaimNum(0L);
        vo.setMyTodoClaimNum(0L);

        vo.setMyPendSubmitClaimNum(0L);//我的待提交
        vo.setMyPendApprovalClaimNum(0L); //我的待审批
        vo.setMyPendDoClaimNum(0L); //我的待处理
        vo.setMyPendAuditClaim(0L); //我的待审认领

        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());
        if (benchVO == null) {
            return vo;
        }
        CompletableFuture<Void> bankTodoFuture = CompletableFuture.runAsync(() -> {
            try {
                FlowBenchVO bankTodoBenchVO = cloneFlowBenchVO(benchVO);
                handleBankTodo(vo, bankTodoBenchVO);
            } catch (Exception e) {
                log.error("handleBankTodo 执行异常", e);
            }
        }, executorServicePool.getThreadPoolExecutor());

        CompletableFuture<Void> claimFuture = CompletableFuture.runAsync(() -> {
            try {
                FlowBenchVO claimBenchVO = cloneFlowBenchVO(benchVO);
                handleClaim(vo, claimBenchVO);
            } catch (Exception e) {
                log.error("handleClaim 执行异常", e);
            }
        }, executorServicePool.getThreadPoolExecutor());

        CompletableFuture<Void> myClaimFuture = CompletableFuture.runAsync(() -> {
            try {
                FlowBenchVO myClaimBenchVO = cloneFlowBenchVO(benchVO);
                handleMyClaim(vo, myClaimBenchVO);
            } catch (Exception e) {
                log.error("handleMyClaim 执行异常", e);
            }
        }, executorServicePool.getThreadPoolExecutor());

        CompletableFuture.allOf(bankTodoFuture, claimFuture, myClaimFuture).join();

//        handleBankTodo(vo, benchVO);
//        handleClaim(vo, benchVO);
//        handleMyClaim(vo, benchVO);
        return vo;
    }


    /**
     * 克隆 FlowBenchVO 对象，避免多线程并发修改问题
     * @param source 源对象
     * @return 克隆后的对象
     */
    private FlowBenchVO cloneFlowBenchVO(FlowBenchVO source) {
        FlowBenchVO target = new FlowBenchVO();
        target.setId(source.getId());
        target.setViewName(source.getViewName());
        target.setAccentity(source.getAccentity());
        target.setDateRange(source.getDateRange());
        target.setAccountNo(source.getAccountNo());
        target.setAccountCurrency(source.getAccountCurrency());
        target.setWarningDays(source.getWarningDays());
        target.setCurrency(source.getCurrency());
        target.setExchangeRateType(source.getExchangeRateType());
        target.setCurrencyUnit(source.getCurrencyUnit());
        target.setIsDefault(source.getIsDefault());
        target.setIorder(source.getIorder());
        target.setIsPreset(source.getIsPreset());

        if (source.getAccentitySet() != null) {
            target.setAccentitySet(new HashSet<>(source.getAccentitySet()));
        }
        if (source.getBankAccountSet() != null) {
            target.setBankAccountSet(new HashSet<>(source.getBankAccountSet()));
        }
        if (source.getAccountNoList() != null) {
            target.setAccountNoList(new ArrayList<>(source.getAccountNoList()));
        }

        return target;
    }


    /**
     * 待办-银行流水处理
     *
     * @param startDate
     * @param endDate
     * @param vo
     * @param benchVO
     * @throws Exception
     */
    private void handleBankTodo(FlowTodoVO vo, FlowBenchVO benchVO) throws Exception {
        fillOrgAndAccount(benchVO, IServicecodeConstant.CMPBANKRECONCILIATION);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return;
        }
        String[] dataRange = benchVO.getDateRange();
        Date startDate = DateUtils.parseDate(dataRange[0].substring(0, 10));
        Date endDate = DateUtils.parseDate(dataRange[1].substring(0, 10));
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        QueryConditionGroup conditionGroup3 = new QueryConditionGroup(ConditionOperator.or);
        conditionGroup3.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        if (StringUtils.isEmpty(benchVO.getAccentity())) {
            conditionGroup3.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
        }
        conditionGroup.addCondition(conditionGroup3);
        conditionGroup.appendCondition(
                QueryCondition.name("tran_date").between(startDate, endDate), //交易日期
                QueryCondition.name("initflag").eq("0")
        );
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
        }
        //银行账号考虑多币种场景
        long confirmingCount = bankreconciliationCountServiceImpl.getBankRecConfirmingCount(conditionGroup);

//        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
//            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
//            if (CollectionUtils.isNotEmpty(accountNoList)) {
//                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
//                conditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
//                //暂时先去掉币种
//                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
//            }
//        } else {
//            conditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
//        }

        conditionGroup.addCondition(QueryCondition.name("accentity").is_not_null());
        long publishingCount = bankreconciliationCountServiceImpl.getBankRecPublishingCount(conditionGroup);
        long todoProcessingNum = bankreconciliationCountServiceImpl.getBankRecConfirmedCount(conditionGroup);
        vo.setOrgConfirmNum(confirmingCount); // 待确认使用组织（账户使用组织为空）
        vo.setTodoProcessingNum(todoProcessingNum); //待处理
        vo.setProcessingNum(publishingCount); //发布处理中

        long refundConfirmNm = refundConfirmNm(startDate, endDate, benchVO);
        vo.setRefundConfirmNm(refundConfirmNm); //退票确认

        long autorelationConfirming = autorelationConfirming(startDate, endDate, benchVO);
        vo.setAutorelationConfirming(autorelationConfirming);

        long autocreatebillConfirming = autocreatebillConfirming(startDate, endDate, benchVO);
        vo.setAutocreatebillConfirming(autocreatebillConfirming);
    }

    public long refundConfirmNm(Date startDate, Date endDate, FlowBenchVO benchVO) throws Exception {
        //退票确认 service_code cmp_BankReconciliation_checkRefund billno cmp_BankReconciliation_checkRefund uri cmp.bankreconciliation.BankReconciliation table  cmp_bankreconciliation
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //银行账号考虑多币种场景
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                queryConditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            queryConditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
        }
        queryConditionGroup.addCondition(QueryCondition.name("initflag").eq(0));
        queryConditionGroup.addCondition(QueryCondition.name("refundstatus").eq(1));
        queryConditionGroup.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        if (StringUtils.isNotEmpty(benchVO.getAccentity())) {
            queryConditionGroup.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
        }
        queryConditionGroup.addCondition(QueryCondition.name("tran_date").between(startDate, endDate));
        return PageUtils.queryCount(queryConditionGroup, BankReconciliation.ENTITY_NAME);
    }

    public long autorelationConfirming(Date startDate, Date endDate, FlowBenchVO benchVO) throws Exception {
        //自动关联待确认 service_code ficmp0036 billno cmp_BankReconciliation_isSure_list uri cmp.bankreconciliation.BankReconciliation,cmp.bankreconciliation.BankReconciliationbusrelation_b table cmp_bankreconciliation,cmp_bankreconciliation_bus_relation_b
        //select * from  cmp_bankreconciliation  t1 inner join cmp_bankreconciliation_bus_relation_b  t2 on t1.id = t2.bankreconciliation where (t2.relationtype=0 and t2.relationstatus=1) and t1.ytenant_id='0000LQYJ6YUWDH50R30000'
        QuerySchema querySchema = new QuerySchema();
        querySchema.addSelect("count(id)");
        querySchema.fullname(BankReconciliation.ENTITY_NAME);
        QueryJoin queryJoin = new QueryJoin("sub", "sub.bankreconciliation=id", "inner alone");
        queryJoin.joinEntity(BankReconciliationbusrelation_b.ENTITY_NAME);
        querySchema.addJoin(queryJoin);
        querySchema.appendQueryCondition(
                QueryCondition.name("sub.relationtype").eq(0),
                QueryCondition.name("sub.relationstatus").eq(1),
                QueryCondition.name("initflag").eq(0),
                QueryCondition.name("accentity").is_not_null(),
                QueryCondition.name("tran_date").between(startDate, endDate)
        );
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        QueryConditionGroup orgConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        orgConditionGroup.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        if (StringUtils.isEmpty(benchVO.getAccentity())) {
            orgConditionGroup.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
        }
        queryConditionGroup.addCondition(orgConditionGroup);
        //银行账号考虑多币种场景
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                queryConditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            queryConditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
        }
        querySchema.appendQueryCondition(queryConditionGroup);
        Map<String, Object> countMap = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME, querySchema);
        return Long.parseLong(countMap.get("count").toString());
    }

    public long autocreatebillConfirming(Date startDate, Date endDate, FlowBenchVO benchVO) throws Exception {
        //自动生单待确认 ficmp0038 billno cmp_auto_push_bill_confirm_list uri cmp.bankreconciliation.BankReconciliation,cmp.bankreconciliation.BankReconciliationbusrelation_b table cmp_bankreconciliation,cmp_bankreconciliation_bus_relation_b
        //select * from  cmp_bankreconciliation  t1 where (t1.isautocreatebill=1 and t1.relationstatus=1) and t1.accentity='1899660702299193354'
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //银行账号考虑多币种场景
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
        }
        QueryConditionGroup orgConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        orgConditionGroup.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        if (StringUtils.isEmpty(benchVO.getAccentity())) {
            orgConditionGroup.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
        }
        conditionGroup.addCondition(orgConditionGroup);
        conditionGroup.addCondition(QueryCondition.name("isautocreatebill").eq(true));
        conditionGroup.addCondition(QueryCondition.name("relationstatus").eq(1));
        conditionGroup.addCondition(QueryCondition.name("relationstatus").eq(1));
        conditionGroup.addCondition(QueryCondition.name("initflag").eq(0));
        conditionGroup.addCondition(QueryCondition.name("accentity").is_not_null());
        conditionGroup.addCondition(QueryCondition.name("tran_date").between(startDate, endDate));
        return PageUtils.queryCount(conditionGroup, BankReconciliation.ENTITY_NAME);
    }

    /**
     * 到账认领 cmp_billclaimcenterlist，cmp_billclaimcenterlist ，cmp.bankreconciliation.BankReconciliation，cmp_bankreconciliation
     *
     * @param vo
     * @param benchVO
     * @throws Exception
     */
    private void handleClaim(FlowTodoVO vo, FlowBenchVO benchVO) throws Exception {
        fillOrgAndAccount(benchVO, IServicecodeConstant.BILLCLAIMCARD);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return;
        }
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        if (StringUtils.isNotEmpty(benchVO.getAccentity())) {
            ctmJSONObject.put("accentity", JSONBuilderUtils.beanToString(benchVO.getAccentity().split(",")));
        }
        if (CollectionUtils.isNotEmpty(benchVO.getAccentitySet())) {
            ctmJSONObject.put("accentity", benchVO.getAccentitySet());
        }

        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                ctmJSONObject.put("bankaccount", JSONBuilderUtils.beanToString(accounts));
                //暂时先去掉币种
                //ctmJSONObject.put("currency", accountNoList.get(0).get("currencyId"));
            }
        }
        if(benchVO.getDateRange() != null){
            ctmJSONObject.put("startDate",benchVO.getDateRange()[0]);
            ctmJSONObject.put("endDate",benchVO.getDateRange()[1]);
        }
        HashMap<String, Object> claimCenterCountServiceCount = claimCenterCountService.getCount(ctmJSONObject);
        if (claimCenterCountServiceCount != null && !claimCenterCountServiceCount.isEmpty()) {
            vo.setCommonOrgConfirmNum(claimCenterCountServiceCount.get("commonconfirmed") == null ? 0L : Long.parseLong(claimCenterCountServiceCount.get("commonconfirmed").toString()));
            vo.setCommonTodoClaimNum(claimCenterCountServiceCount.get("commonprocessed") == null ? 0L : Long.parseLong(claimCenterCountServiceCount.get("commonprocessed").toString()));
            vo.setMyTodoClaimNum(claimCenterCountServiceCount.get("myprocessed") == null ? 0L : Long.parseLong(claimCenterCountServiceCount.get("myprocessed").toString()));
        }

    }

    /**
     * 我的认领 ficmp0034，cmp_mybillclaimlist，cmp.billclaim.BillClaim,cmp_billclaim(主) cmp_billclaim_item（子）
     * IServicecodeConstant.CMPBANKRECONCILIATION
     *
     * @param vo
     * @param benchVO
     * @throws Exception
     */
    private void handleMyClaim(FlowTodoVO vo, FlowBenchVO benchVO) throws Exception {
        fillOrgAndAccount(benchVO, IServicecodeConstant.BILLCLAIMCARD);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return;
        }
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        if (StringUtils.isNotEmpty(benchVO.getAccentity())) {
            ctmJSONObject.put("accentity", JSONBuilderUtils.beanToString(benchVO.getAccentity().split(",")));
        }
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                ctmJSONObject.put("bankaccount", JSONBuilderUtils.beanToString(accounts));
                //暂时先去掉币种
                //ctmJSONObject.put("currency", accountNoList.get(0).get("currencyId"));
            }
        }

        HashMap<String, Object> mybillclaimCountServiceCount = mybillclaimCountService.getCount(ctmJSONObject);
        //我的待审 本期去掉
//        TaskService taskService = processService.bpmRestServices().getTaskService();
//        TaskQueryParam taskQueryParam = new TaskQueryParam();
//        taskQueryParam.setAssignee(InvocationInfoProxy.getUserid());
//        taskQueryParam.setReturnProcessInstance(true);
//        taskQueryParam.setCategoryId("CM.cmp_billclaimcard");
//        JsonNode jsonNode = (JsonNode) taskService.queryTasksToDo(InvocationInfoProxy.getUserid(), taskQueryParam);
//        List<String> ids = new ArrayList<>();
//        if (jsonNode.get("data") != null && jsonNode.get("total") != null && jsonNode.get("total").longValue() > 0) {
//            ArrayNode arrayNode = (ArrayNode) jsonNode.get("data");
//            for (JsonNode node : arrayNode) {
//                JsonNode processInstanceNode = node.get("processInstance");
//                ids.add(processInstanceNode.get("businessKey").asText().replace("cmp_billclaimcard_", ""));
//            }
//        }

//        List<String> allIds = billClaimList.stream().map(item -> item.getString("id")).collect(Collectors.toList());
//        long myPendAuditClaim = ids.stream().filter(id -> allIds.contains(id)).count();
        if (mybillclaimCountServiceCount != null && !mybillclaimCountServiceCount.isEmpty()) {
            vo.setMyPendSubmitClaimNum((Long) mybillclaimCountServiceCount.getOrDefault("init", 0L));//我的待提交
            vo.setMyPendApprovalClaimNum((Long) mybillclaimCountServiceCount.getOrDefault("submited", 0L)); //我的待审批
            vo.setMyPendDoClaimNum((Long) mybillclaimCountServiceCount.getOrDefault("pending", 0L)); //我的待处理
            vo.setMyPendAuditClaim(0L); //我的待审认领
        }

    }

    /**
     * 银行流水处理 ficmp0006(银行流水认领)
     *
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    @Override
    public List<FlowMonthDataVO> queryMonthCalc(FlowBenchVO flowBenchVO) throws Exception {
        List<FlowMonthDataVO> list = new ArrayList<>();
        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());

        //
        Date startDate = getMonthStartDay(flowBenchVO);
        Date endDate = getMonthEndDay(flowBenchVO);
        List<Date> everyDays = everyDateOfDates(startDate, endDate);
        if (CollectionUtils.isEmpty(everyDays)) {
            return list;
        }
        if (benchVO == null) {
            return this.buildMonthCalcDefaultData(everyDays);
        }

        fillOrgAndAccount(benchVO, IServicecodeConstant.CMPBANKRECONCILIATION);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return this.buildMonthCalcDefaultData(everyDays);
        }

        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        QueryConditionGroup orgConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        orgConditionGroup.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        if (StringUtils.isEmpty(benchVO.getAccentity())) {
            orgConditionGroup.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
        }
        conditionGroup.addCondition(orgConditionGroup);
        //银行账号考虑多币种场景
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
        }
        conditionGroup.addCondition(QueryCondition.name("accentity").is_not_null());
        conditionGroup.addCondition(QueryCondition.name("initflag").eq(0));
        QuerySchema countSchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(
                QueryCondition.name("tran_date").between(startDate, endDate)
        );
        countSchema.appendQueryCondition(conditionGroup);
        //银行流水认领
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, countSchema, null);

        Map<Date, List<BankReconciliation>> map = bankReconciliationList.stream().collect(Collectors.groupingBy(BankReconciliation::getTran_date));
        for (Date date : everyDays) {
            FlowMonthDataVO vo = new FlowMonthDataVO();
            //小于当前日期 添加数据，小于当前日期 不检查
            if (date.compareTo(DateUtils.getNow()) > 0) {
                vo.setFlag(true);

            } else {
                vo.setBillNum(0L);
                vo.setUnfinishNum(0L);
                vo.setFinishNum(0L);
                vo.setProcessedBillNum(0L);

                vo.setDays(DateUtils.dateToStr(new Date(date.getTime())));
//                QueryConditionGroup tmp = new QueryConditionGroup(ConditionOperator.and);
//                tmp.addCondition(conditionGroup);
//                tmp.appendCondition(QueryCondition.name("tran_date").eq(date));
//                long unfinish = bankreconciliationCountServiceImpl.getBankRecConfirmedCount(tmp);
//                long handle = bankreconciliationCountServiceImpl.getBankRecPublishingCount(tmp);
//                long finish = bankreconciliationCountServiceImpl.getBankRecEndedCount(tmp);
                vo.setFlag(true);
                long unfinish = 0L;
                long handle = 0L;
                long finish = 0L;

//            String key = DateUtils.dateFormat(date, "yyyy-MM-dd");
                if (map.containsKey(date)) {
                    List<BankReconciliation> bankReconciliations = map.get(date);
                    if (CollectionUtils.isNotEmpty(bankReconciliations)) {
                        vo.setBillNum((long) bankReconciliations.size());
                    }
                    unfinish = bankReconciliations.stream().filter(item -> item.getIspublish() == false && item.getSerialdealendstate() == 0).count();
                    handle = bankReconciliations.stream().filter(item -> item.getIspublish() == true && item.getSerialdealendstate() == 0).count();
                    finish = bankReconciliations.stream().filter(item -> item.getSerialdealendstate() == 1).count();
                    if (unfinish + handle > 0) {
                        vo.setFlag(false);
                    }
                } else {
                    vo.setBillNum(0L);
                }
                vo.setUnfinishNum(unfinish + handle);
                vo.setFinishNum(finish);
                vo.setProcessedBillNum(handle);
            }

            list.add(vo);
        }

//        for (Map.Entry<Date, List<BankReconciliation>> entry : map.entrySet()) {
//            Date key = entry.getKey();
//            String tranDate = DateUtils.dateFormat(key, "yyyy-MM-dd");
//            List<BankReconciliation> bankReconciliations = entry.getValue();
//            if (CollectionUtils.isEmpty(bankReconciliations)) {
//                continue;
//            }
//            // 初始是空的，后面辨识后需要赋值,走辨识规则链的时候，会给每条银行对账单这个字段赋值
//            //未处理流水笔数 流水处理完结状态=未完结 && 是否发布=否  确认下这个字段的状态
//            long unfinish = bankReconciliations.stream().filter(item -> (item.getSerialdealendstate() == null || item.getSerialdealendstate() == 0) && item.getIspublish().equals(false)).count();
//            //发布处理中流水笔数 流水处理完结状态=未完结 是否发布=是
//            long handle = bankReconciliations.stream().filter(item -> (item.getSerialdealendstate() == null || item.getSerialdealendstate() == 0) && item.getIspublish().equals(true)).count();
//            //已处理流水笔数 流水处理完结状态=已完结
//            long finish = bankReconciliations.stream().filter(item -> item.getSerialdealendstate() != null && item.getSerialdealendstate() == 1).count();
//            for (FlowMonthDataVO vo : list) {
//                if (vo.getDays().equals(tranDate)) {
//                    if (unfinish + handle == 0) {
//                        vo.setFlag(true);
//                    } else {
//                        vo.setFlag(false);
//                    }
//                    vo.setBillNum((long) bankReconciliations.size());
//                    vo.setUnfinishNum(unfinish);
//                    vo.setFinishNum(finish);
//                    vo.setProcessedBillNum(handle);
//                }
//            }
//        }
        return list;
    }

    private List<FlowMonthDataVO> buildMonthCalcDefaultData(List<Date> everyDays) {
        List<FlowMonthDataVO> list = new ArrayList<>();
        for (Date date : everyDays) {
            FlowMonthDataVO vo = new FlowMonthDataVO();
            if (date.compareTo(DateUtils.getNow()) > 0) {
                vo.setFlag(true);
            } else {
                vo.setFlag(true);
                vo.setBillNum(0L);
                vo.setUnfinishNum(0L);
                vo.setFinishNum(0L);
                vo.setProcessedBillNum(0L);
            }
            list.add(vo);
        }
        return list;
    }

    /**
     * 银行流水处理 ficmp0006(银行流水认领)
     * 银行交易回单 ficmp0042 cmp_bankelectronicreceiptlist cmp.bankelectronicreceipt.BankElectronicReceipt
     *
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    @Override
    public FlowDayDataVO queryDayCalc(FlowBenchVO flowBenchVO) throws Exception {
        FlowDayDataVO vo = new FlowDayDataVO();
        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());
        if (benchVO == null) {
            vo.setBillNum(0L);
            vo.setUnrelationreceiptsBillNum(0L);
            vo.setProcessedBillNum(0L);
            vo.setUnprocessedBillNum(0L);
            vo.setProcessingBillNum(0L);

            vo.setReceiptsBillNum(0L);
            vo.setReceiptsUnrelationBillNum(0L);
            vo.setReceiptsUnDownloadBillNum(0L);

            return vo;
        }
        //银行流水认领
        fillOrgAndAccount(benchVO, IServicecodeConstant.CMPBANKRECONCILIATION);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            vo.setBillNum(0L);
            vo.setUnrelationreceiptsBillNum(0L);
            vo.setProcessedBillNum(0L);
            vo.setUnprocessedBillNum(0L);
            vo.setProcessingBillNum(0L);
        } else {
            bankDayData(benchVO, flowBenchVO.getChooseDay(), flowBenchVO.getType(), vo);
        }

        //银行回单
        fillOrgAndAccount(benchVO, IServicecodeConstant.BANKRECEIPTMATCH);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            vo.setReceiptsBillNum(0L);
            vo.setReceiptsUnrelationBillNum(0L);
            vo.setReceiptsUnDownloadBillNum(0L);
        } else {
            receiptDayCalc(benchVO, flowBenchVO.getChooseDay(), flowBenchVO.getType(), vo);
        }
        return vo;
    }

    private void bankDayData(FlowBenchVO benchVO, Date chooseDay, String type, FlowDayDataVO vo) throws Exception {
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //银行账号考虑多币种场景
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
        }
        QueryConditionGroup orgConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        orgConditionGroup.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        if (StringUtils.isEmpty(benchVO.getAccentity())) {
            orgConditionGroup.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
        }
        conditionGroup.addCondition(orgConditionGroup);
        conditionGroup.appendCondition(
                QueryCondition.name("initflag").eq(0),
                QueryCondition.name("accentity").is_not_null(),
                QueryCondition.name("tran_date").eq(chooseDay)
        );
        if (type != null && !"3".equals(type)) {
            //付款1  对应 借 1/收款2 对应 贷 2 //com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction
            conditionGroup.appendCondition(QueryCondition.name("dc_flag").eq(type));
        }
        QuerySchema countSchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(
                conditionGroup
        );

        //银行流水认领
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, countSchema, null);
        if (CollectionUtils.isEmpty(bankReconciliationList)) {
            vo.setBillNum(0L);
            vo.setUnrelationreceiptsBillNum(0L);
        } else {
            // 明细未关联回单 回单关联状态=未关联
            long unrelationreceiptsBillNum = bankReconciliationList.stream().filter(item -> item.getReceiptassociation() != null && item.getReceiptassociation() == 4).count();
            vo.setBillNum((long) bankReconciliationList.size());
            vo.setUnrelationreceiptsBillNum(unrelationreceiptsBillNum);
        }

        long finish = bankreconciliationCountServiceImpl.getBankRecEndedCount(conditionGroup);
        vo.setProcessedBillNum(finish);
        long unfinish = bankreconciliationCountServiceImpl.getBankRecConfirmedCount(conditionGroup);
        vo.setUnprocessedBillNum(unfinish);
        long handle = bankreconciliationCountServiceImpl.getBankRecPublishingCount(conditionGroup);
        vo.setProcessingBillNum(handle);
    }

    public void receiptDayCalc(FlowBenchVO benchVO, Date chooseDay, String type, FlowDayDataVO vo) throws Exception {
        QueryConditionGroup conditionGroup4 = new QueryConditionGroup(ConditionOperator.and);
        //银行账号考虑多币种场景
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup4.addCondition(QueryCondition.name("enterpriseBankAccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup4.addCondition(QueryCondition.name("enterpriseBankAccount").in(benchVO.getBankAccountSet()));
        }
        QuerySchema receiptSchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM)
                .appendQueryCondition(
                        //QueryCondition.name("accentity").in(benchVO.getAccentitySet()),
                        QueryCondition.name("tranDate").eq(chooseDay));
        receiptSchema.appendQueryCondition(conditionGroup4);
        //com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction
        if (StringUtils.isNotEmpty(type) && !"3".equals(type)) {
            //付款1  对应 借 1/收款2 对应 贷 2
            receiptSchema.appendQueryCondition(QueryCondition.name("dc_flag").eq(type));
        }

        List<BankElectronicReceipt> receiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, receiptSchema, null);
        if (CollectionUtils.isEmpty(receiptList)) {
            vo.setReceiptsBillNum(0L);
            vo.setReceiptsUnrelationBillNum(0L);
            vo.setReceiptsUnDownloadBillNum(0L);
        } else {
            //回单未关联明细笔数 对账单关联状态=未关联
            long receiptsUnrelationBillNum = receiptList.stream().filter(item -> item.getAssociationstatus() != null && item.getAssociationstatus() == 4).count();
            //未下载回单文件笔数 是否已下载回单未否
            long receiptsUnDownloadBillNum = receiptList.stream().filter(item -> item.getIsdown() != null && !item.getIsdown()).count();

            vo.setReceiptsBillNum((long) receiptList.size());
            vo.setReceiptsUnrelationBillNum(receiptsUnrelationBillNum);
            vo.setReceiptsUnDownloadBillNum(receiptsUnDownloadBillNum);
        }
    }

    /**
     * 银行交易回单 ficmp0042 cmp_bankelectronicreceiptlist cmp.bankelectronicreceipt.BankElectronicReceipt
     * 对账单关联状态 associationstatus
     * 回单处理标识 billprocessflag com.yonyoucloud.fi.cmp.cmpentity.BillProcessFlag
     * 回单关联状态  receiptassociation  com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus
     * 是否已下载回单
     * 银行账户期初节点 ficmp0008 , cmp_initdatayhlist ,cmp.initdata.InitData,cmp_initdata(主) cmp_initdata_b(子表)
     *
     * @param flowBenchVO
     * @return
     */
    @Override
    public FlowReceiptVO queryReceipt(FlowBenchVO flowBenchVO) throws Exception {
        FlowReceiptVO vo = new FlowReceiptVO();
        vo.setBillNum(0L);
        vo.setUnrelationReceiptBillNum(0L);
        vo.setReceiptsBillNum(0L);
        vo.setReceiptsUnrelationBillNum(0L);
        vo.setReceiptsUnDownloadBillNum(0L);


        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());
        if (benchVO == null) {
            return vo;
        }
        String[] dataRange = benchVO.getDateRange();
        Date startDate = DateUtils.parseDate(dataRange[0]);
        Date endDate = DateUtils.parseDate(dataRange[1]);
        fillOrgAndAccount(benchVO, IServicecodeConstant.CMPBANKRECONCILIATION);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return vo;
        }
        Set<String> bankOrg = benchVO.getAccentitySet();
        Set<String> bankAccount = benchVO.getBankAccountSet();
        //银行账号考虑多币种场景
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        }
//        else {
//            conditionGroup.addCondition(QueryCondition.name("bankaccount").in(bankAccount));
//        }
        QueryConditionGroup orgConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        orgConditionGroup.addCondition(QueryCondition.name("accentity").in(bankOrg));
        if (StringUtils.isEmpty(benchVO.getAccentity())) {
            orgConditionGroup.addCondition(QueryCondition.name("orgid").in(bankOrg));
        }
        conditionGroup.addCondition(orgConditionGroup);
        //银行流水处理
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(
                QueryCondition.name("tran_date").between(startDate, endDate),
                QueryCondition.name("initflag").eq(0),
                QueryCondition.name("accentity").is_not_null()
        );
        querySchema.appendQueryCondition(conditionGroup);
        if (flowBenchVO.getCheckDirection() != null && flowBenchVO.getCheckDirection() == 1) {
            querySchema.appendQueryCondition(QueryCondition.name("dc_flag").eq(Direction.Debit.getValue()));
        } else if (flowBenchVO.getCheckDirection() != null && flowBenchVO.getCheckDirection() == 2) {
            querySchema.appendQueryCondition(QueryCondition.name("dc_flag").eq(Direction.Credit.getValue()));
        }
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(bankReconciliationList)) {
            vo.setBillNum(0L);
            vo.setUnrelationReceiptBillNum(0L);
        } else {
            long unrelationReceiptCount = bankReconciliationList.stream().filter(item -> item.getReceiptassociation() != null && item.getReceiptassociation() == 4).count();
            //
            vo.setBillNum((long) bankReconciliationList.size());
            //
            vo.setUnrelationReceiptBillNum(unrelationReceiptCount);
        }


        fillOrgAndAccount(benchVO, IServicecodeConstant.CMPBANKRECONCILIATION, false);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return vo;
        }
        Set<String> receiptOrg = benchVO.getAccentitySet();
        Set<String> receiptAccount = benchVO.getBankAccountSet();
        //银行账号考虑多币种场景
        QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup2.addCondition(QueryCondition.name("enterpriseBankAccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup2.addCondition(QueryCondition.name("enterpriseBankAccount").in(receiptAccount));
        }
        //银行交易回单 com.yonyoucloud.fi.cmp.bankreceipt.rule.BeforeQueryBankReceiptRule.execute只使用组织信息查询企业银行账号,不将字段添加到拼接查询条件
        QuerySchema receiptSchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(
                QueryCondition.name("tranDate").between(startDate, endDate)
        );
        receiptSchema.appendQueryCondition(conditionGroup2);
        if (flowBenchVO.getCheckDirection() != null && flowBenchVO.getCheckDirection() == 1) {
            receiptSchema.appendQueryCondition(QueryCondition.name("dc_flag").eq(Direction.Debit.getValue()));
        } else if (flowBenchVO.getCheckDirection() != null && flowBenchVO.getCheckDirection() == 2) {
            receiptSchema.appendQueryCondition(QueryCondition.name("dc_flag").eq(Direction.Credit.getValue()));
        }
        List<BankElectronicReceipt> receiptlist = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, receiptSchema, null);
        if (CollectionUtils.isEmpty(receiptlist)) {
            vo.setReceiptsBillNum(0L);
            vo.setReceiptsUnrelationBillNum(0L);
            vo.setReceiptsUnDownloadBillNum(0L);
        } else {
            //com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus
            long receiptsUnrelationCount = receiptlist.stream().filter(item -> item.getAssociationstatus() != null && item.getAssociationstatus() == 4).count();
            long receiptsUnDownloadCount = receiptlist.stream().filter(item -> item.getIsdown() != null && !item.getIsdown()).count();
            vo.setReceiptsBillNum((long) receiptlist.size());
            vo.setReceiptsUnrelationBillNum(receiptsUnrelationCount);
            vo.setReceiptsUnDownloadBillNum(receiptsUnDownloadCount);
        }

        //右侧折线图
        Date before7Date = DateUtils.dateAddDays(endDate, -6);
        if (DateUtils.dateBetweenIncludeToday(startDate, endDate) >= 7) {
            FlowReceiptDetailVO detailVO = buildListData(bankReconciliationList, receiptlist, before7Date, endDate, flowBenchVO.getCheckDirection());
            vo.setDetail(detailVO);
        } else {

            querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(
                    QueryCondition.name("tran_date").between(startDate, endDate),
                    QueryCondition.name("initflag").eq(0),
                    QueryCondition.name("accentity").is_not_null()
            );
            querySchema.appendQueryCondition(orgConditionGroup);
            if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
                List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
                if (CollectionUtils.isNotEmpty(accountNoList)) {
                    List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                    querySchema.appendQueryCondition(QueryCondition.name("bankaccount").in(accounts));
                    //暂时先去掉币种
                    //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
                }
            } else {
                querySchema.appendQueryCondition(QueryCondition.name("bankaccount").in(bankAccount));
            }
            bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);

            //银行交易回单 com.yonyoucloud.fi.cmp.bankreceipt.rule.BeforeQueryBankReceiptRule.execute只使用组织信息查询企业银行账号,不将字段添加到拼接查询条件
            receiptSchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(
                    QueryCondition.name("tranDate").between(startDate, endDate)
            );
            if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
                List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
                if (CollectionUtils.isNotEmpty(accountNoList)) {
                    List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                    receiptSchema.appendQueryCondition(QueryCondition.name("enterpriseBankAccount").in(accounts));
                    //暂时先去掉币种
                    //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
                }
            } else {
                receiptSchema.appendQueryCondition(QueryCondition.name("enterpriseBankAccount").in(receiptAccount));
            }
//            receiptSchema.addCondition(conditionGroup6);
            receiptlist = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, receiptSchema, null);
            FlowReceiptDetailVO detailVO = buildListData(bankReconciliationList, receiptlist, before7Date, endDate, flowBenchVO.getCheckDirection());
            vo.setDetail(detailVO);
        }

        return vo;
    }

    /**
     * @param bankReconciliationList
     * @param receiptlist
     * @param startDate
     * @param endDate
     */
    private FlowReceiptDetailVO buildListData(List<BankReconciliation> bankReconciliationList, List<BankElectronicReceipt> receiptlist, Date startDate, Date endDate, Integer checkDirection) throws Exception{
        FlowReceiptDetailVO flowReceiptDetailVO = new FlowReceiptDetailVO();
        List<String> dates = everyDayOfDates(startDate, endDate);
        //
        if (checkDirection != null && checkDirection == 1) {
            //借
            bankReconciliationList = bankReconciliationList.stream().filter(item -> item.getDc_flag() == Direction.Debit).collect(Collectors.toList());
            receiptlist = receiptlist.stream().filter(item -> item.getDc_flag() == Direction.Debit).collect(Collectors.toList());
        } else if (checkDirection != null && checkDirection == 2) {
            //贷
            bankReconciliationList = bankReconciliationList.stream().filter(item -> item.getDc_flag() == Direction.Credit).collect(Collectors.toList());
            receiptlist = receiptlist.stream().filter(item -> item.getDc_flag() == Direction.Credit).collect(Collectors.toList());
        }
        //总计
        List<Long> billList = new ArrayList<>();
        List<Long> unrelationReceiptList = new ArrayList<>();
        Map<String, List<BankReconciliation>> bankMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(bankReconciliationList)) {
            bankMap = bankReconciliationList.stream().filter(item -> item.getTran_date() != null).collect(Collectors.groupingBy(item -> DateUtils.dateToStr(new Date(item.getTran_date().getTime()))));
        }
        if (CollectionUtils.isNotEmpty(dates)) {
            for (String date : dates) {
                if (bankMap != null && !bankMap.isEmpty()) {
                    if (bankMap.containsKey(date)) {
                        List<BankReconciliation> bankList = bankMap.get(date);
                        //流水笔数
                        billList.add((long) bankList.size());
                        //未关联回单笔数 receiptassociation=4
                        long unrelationReceipt = bankList.stream().filter(item -> item.getReceiptassociation() != null && item.getReceiptassociation() == 4).count();
                        unrelationReceiptList.add(unrelationReceipt);
                    } else {
                        billList.add(0L);
                        unrelationReceiptList.add(0L);
                    }
                } else {
                    billList.add(0L);
                    unrelationReceiptList.add(0L);
                }
            }
        }
        flowReceiptDetailVO.setBillList(billList);
        flowReceiptDetailVO.setUnrelationReceiptList(unrelationReceiptList);

        List<Long> receiptsList = new ArrayList<>();
        List<Long> unrelationBillList = new ArrayList<>();
        List<Long> undownReceiptList = new ArrayList<>();
        Map<String, List<BankElectronicReceipt>> receiptMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(receiptlist)) {
            receiptMap = receiptlist.stream().filter(item -> item.getTranDate() != null).collect(Collectors.groupingBy(item -> DateUtils.dateToStr(new Date(item.getTranDate().getTime()))));
        }
        for (String date : dates) {
            if (receiptMap != null && !receiptMap.isEmpty()) {
                if (receiptMap.containsKey(date)) {
                    List<BankElectronicReceipt> receiptList = receiptMap.get(date);
                    //流水笔数
                    receiptsList.add((long) receiptList.size());
                    //未关联明细笔数 com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus
                    long unrelationBill = receiptList.stream().filter(item -> item.getAssociationstatus() != null && item.getAssociationstatus() == 4).count();
                    //未下载回单笔数
                    long undownReceipt = receiptList.stream().filter(item -> item.getIsdown() != null && !item.getIsdown()).count();

                    unrelationBillList.add(unrelationBill);
                    undownReceiptList.add(undownReceipt);
                } else {
                    receiptsList.add(0L);
                    unrelationBillList.add(0L);
                    undownReceiptList.add(0L);
                }
            } else {
                receiptsList.add(0L);
                unrelationBillList.add(0L);
                undownReceiptList.add(0L);
            }
        }
        flowReceiptDetailVO.setReceiptsList(receiptsList);
        flowReceiptDetailVO.setUnrelationBillList(unrelationBillList);
        flowReceiptDetailVO.setUndownReceiptList(undownReceiptList);

        return flowReceiptDetailVO;
    }

    /**
     * 汇率 exchanagerate_u8c，bd_newexchangeratelist，ucfbasedoc，bd.exchangeRate.ExchangeRateVO,bd_exchangerate
     * sourceCurrencyId 源币种，targetCurrencyId 币种 ，exchangeRate 汇率 quotationDate报价日期 enable 启用状态
     * 企业银行账户 bd.enterprise.OrgFinBankacctVO
     * 企业银行币种 bd.enterprise.BankAcctCurrencyVO
     * 银行流水处理 cmp.bankreconciliation.BankReconciliation   bankaccount 银行账户，currency 币种 tran_amt交易金额 tran_date 交易日期
     *
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    @Override
    public List<FlowBankStatementVO> queryFlowTop(FlowBenchVO flowBenchVO) throws Exception {
        String formatStr = "0.00";
        List<FlowBankStatementVO> res = new ArrayList<>();

        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());
        //银行账户期初节点 ficmp0008 , cmp_initdatayhlist ,cmp.initdata.InitData,cmp_initdata(主) cmp_initdata_b(子表)
        if (benchVO == null) {
            return res;
        }
        String[] dataRange = benchVO.getDateRange();
        Date startDate = DateUtils.parseDate(dataRange[0]);
        Date endDate = DateUtils.parseDate(dataRange[1]);
        fillOrgAndAccount(benchVO, IServicecodeConstant.BANKRECEIPTMATCH);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return res;
        }
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        QueryConditionGroup conditionGroup3 = new QueryConditionGroup(ConditionOperator.or);
        conditionGroup3.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        if (StringUtils.isEmpty(benchVO.getAccentity())) {
            conditionGroup3.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
        }
        conditionGroup.addCondition(conditionGroup3);

        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
        }
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM + ",banktype.name")
                .appendQueryCondition(
                        QueryCondition.name("tran_date").between(startDate, endDate),
                        QueryCondition.name("initflag").eq(0),
                        QueryCondition.name("accentity").is_not_null()
                );
        querySchema.appendQueryCondition(conditionGroup);
        if (flowBenchVO.getCheckDirection() != null && flowBenchVO.getCheckDirection() == 1) {
            querySchema.appendQueryCondition(QueryCondition.name("dc_flag").eq(1));
        } else if (flowBenchVO.getCheckDirection() != null && flowBenchVO.getCheckDirection() == 2) {
            querySchema.appendQueryCondition(QueryCondition.name("dc_flag").eq(2));
        }
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(bankReconciliationList)) {
            return res;
        }
        if (StringUtils.isNotEmpty(flowBenchVO.getType()) && "1".equals(flowBenchVO.getType())) {
            //汇率逻辑
            for (BankReconciliation item : bankReconciliationList) {
                Double calRate = CurrencyUtil.getCurrencyRateNew(null, benchVO.getExchangeRateType(), item.getCurrency(), benchVO.getCurrency(), new Date(), 6);
                //如果币种相同或者汇率中存在数据
                if (calRate != null && calRate != 0.0d) {
                    BigDecimal amount = BigDecimal.ZERO;
                    if (item.getDc_flag() == Direction.Debit) {
                        amount = item.getDebitamount() == null ? item.getTran_amt() : item.getDebitamount();
                    } else {
                        amount = item.getCreditamount() == null ? item.getTran_amt() : item.getCreditamount();
                    }
                    BigDecimal tranAmt = amount == null ? BigDecimal.ZERO : amount.multiply(BigDecimal.valueOf(calRate));
                    if ("2".equals(benchVO.getCurrencyUnit())) {
                        tranAmt = tranAmt.divide(new BigDecimal("10000"));
                    } else if ("3".equals(benchVO.getCurrencyUnit())) {
                        tranAmt = tranAmt.divide(new BigDecimal("100000000"));
                    }
                    item.setTran_amt(tranAmt);
                } else {
                    item.setTran_amt(BigDecimal.ZERO);
                }
            }
            BigDecimal totalAmount = bankReconciliationList.stream().map(item -> {
                return item.getTran_amt();
            }).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, BigDecimal> sumByCategory = bankReconciliationList.stream()
                    .filter(item -> item.containsKey("banktype_name") && item.get("banktype_name") != null)
                    .collect(Collectors.groupingBy(item -> item.getString("banktype_name"),
                            Collectors.reducing(BigDecimal.ZERO, BankReconciliation::getTran_amt, BigDecimal::add)));
            // Convert the map to a list of entries for sorting
            List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(sumByCategory.entrySet());

            List<Map.Entry<String, BigDecimal>> list = entries.stream().sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).limit(5).collect(Collectors.toList());
            int i = 1;
            for (Map.Entry<String, BigDecimal> entry : list) {
                FlowBankStatementVO vo = new FlowBankStatementVO();
                vo.setSn(i++);
                vo.setBankType(entry.getKey());
                vo.setAmount(entry.getValue());
                if (totalAmount != BigDecimal.ZERO) {
                    vo.setAmountPercent(entry.getValue().divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue());
                } else {
                    vo.setAmountPercent(100.0);
                }

                res.add(vo);
            }
        } else {
            Map<String, Long> countByCategory = bankReconciliationList.stream()
                    .filter(item -> item.containsKey("banktype_name") && item.get("banktype_name") != null)
                    .collect(Collectors.groupingBy(item -> item.getString("banktype_name"), Collectors.counting()));
            List<Map.Entry<String, Long>> entries = new ArrayList<>(countByCategory.entrySet());
            int total = bankReconciliationList.size();


            List<Map.Entry<String, Long>> list = entries.stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(5).collect(Collectors.toList());
            int i = 1;
            for (Map.Entry<String, Long> entry : list) {
                FlowBankStatementVO vo = new FlowBankStatementVO();
                vo.setSn(i++);
                vo.setBankType(entry.getKey());
                vo.setNum(entry.getValue());
                vo.setNumPercent(new DecimalFormat(formatStr).format(entry.getValue().doubleValue() * 100 / total));
                res.add(vo);
            }
        }

        return res;
    }

    /**
     * 银行交易流水 serviceCode ficmp0018  billnum cmp_dllist uri cmp.bankdealdetail.BankDealDetail  tablename cmp_bankdealdetail
     * 余额对比结果 balancecontrast com.yonyoucloud.fi.cmp.cmpentity.BalanceContrast
     * //bd.enterprise.OrgFinBankacctVO, 所属组织 enterpriseBankAccount.orgid.name,账户名称 enterpriseBankAccount.name,银行账号enterpriseBankAccount.account
     * //bd.adminOrg.FinanceOrgVO
     * // bd.enterprise.BankAcctCurrencyVO 币种  currency.name
     *
     * @param flowBenchVO
     * @return
     */
    @Override
    public FlowWarningVO queryFlowWarning(FlowBenchVO flowBenchVO) throws Exception {
        FlowWarningVO vo = new FlowWarningVO();
        vo.setBillInfo(true);
        vo.setBalanceCheck(true);
        vo.setOverDayInfo(true);
        vo.setBillNum(0L);
        vo.setOverDays(flowBenchVO.getWarningDays());
        vo.setRiskNum(0);

        Set<String> accountSet = new HashSet<>();
        Set<String> balanceUnEqualAccountSet = new HashSet<>();
        Set<String> balanceMissingAccountSet = new HashSet<>();
        //未接入/导入账户数
        List<Map<String, Object>> accountMapList = new ArrayList<>();
        //不符记录(缺失+不等)
        List<Map<String, Object>> balanceUnMatchAccounts = new ArrayList<>();
        //缺失记录
        List<Map<String, Object>> balanceMissings = new ArrayList<>();

        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());
        if (benchVO == null) {
            return vo;
        }
        String[] dataRange = benchVO.getDateRange();
        Date startDate = DateUtils.parseDate(dataRange[0]);
        Date endDate = DateUtils.parseDate(dataRange[1]);
        //银行账户期初节点 ficmp0008 , cmp_initdatayhlist ,cmp.initdata.InitData,cmp_initdata(主) cmp_initdata_b(子表)
        fillOrgAndAccount(benchVO, IServicecodeConstant.BANKRECEIPTMATCH, false);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return vo;
        }
        Set<String> orgs = new HashSet<>();
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = new ArrayList<>();
        if (StringUtils.isNotEmpty(benchVO.getAccentity())) {
            orgs = Arrays.stream(benchVO.getAccentity().split(",")).collect(Collectors.toSet());
        } else {
            orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.BANKRECEIPTMATCH);
        }
        if (CollectionUtils.isEmpty(orgs)) {
            return vo;
        }
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            List<String> accountIds = accountNoList.stream().filter(item -> item.containsKey("id") && item.get("id") != null).map(item -> (String) item.get("id")).collect(Collectors.toList());

            try {
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setIdList(accountIds);
                enterpriseParams.setOrgidList(new ArrayList<>(orgs));
                enterpriseParams.setPageSize(5000);
                List<EnterpriseBankAcctVOWithRange> bankAcctVOWithRanges = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
                if (CollectionUtils.isEmpty(bankAcctVOWithRanges)) {
                    return vo;
                }
                enterpriseBankAcctVOS = bankAcctVOWithRanges;
            } catch (Exception e) {
                log.error("查询企业银行账号异常,", e);
                return vo;
            }

        } else {
            try {
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setOrgidList(new ArrayList<>(orgs));
                enterpriseParams.setPageSize(5000);
                enterpriseBankAcctVOS = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
                if (CollectionUtils.isEmpty(enterpriseBankAcctVOS)) {
                    return vo;
                }
            } catch (Exception e) {
                log.error("查询企业银行账号异常,", e);
                return vo;
            }
        }

        List<Date> everyDays = everyDateOfDates(startDate, endDate, true);
        if (CollectionUtils.isEmpty(everyDays)) {
            return vo;
        }
        Set<String> accountIds = enterpriseBankAcctVOS.stream().map(item -> item.getId()).collect(Collectors.toSet());
        QuerySchema balanceSchema = QuerySchema.create().addSelect("id,balancedate,isconfirm,enterpriseBankAccount,currency")
                .appendQueryCondition(
                        QueryCondition.name("enterpriseBankAccount").in(accountIds),
                        QueryCondition.name("balancedate").between(startDate, endDate)
                );
        //银行账户历史余额 ficmp0031,cmp_hisbalist, cmp.accountrealtimebalance.AccountRealtimeBalance,cmp_bankaccount_realtimebalance
        List<AccountRealtimeBalance> balanceList = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, balanceSchema, null);

        QuerySchema dealSchema = QuerySchema.create().addSelect("id,enterpriseBankAccount,currency,tranDate")
                .appendQueryCondition(
                        QueryCondition.name("enterpriseBankAccount").in(accountIds),
                        QueryCondition.name("tranDate").between(startDate, endDate)
                );
        List<BankDealDetail> dealList = MetaDaoHelper.queryObject(BankDealDetail.ENTITY_NAME, dealSchema, null);
        Map<String, List<BankDealDetail>> dealListMap = dealList.stream().collect(Collectors.groupingBy(item -> item.getEnterpriseBankAccount() + "_" + item.getCurrency() + "_" + DateEventUtils.getDate(item.getTranDate())));


        if (CollectionUtils.isEmpty(balanceList)) {
            for (Date date : everyDays) {
                //流水缺失 计数(银行账号)
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS) {
                    if (CollectionUtils.isEmpty(enterpriseBankAcctVOWithRange.getCurrencyList())) {
                        continue;
                    }
                    List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVOWithRange.getCurrencyList().stream().filter(item -> item.getCurrencyEnable() != null && item.getCurrencyEnable() == 1).collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(currencyList)) {
                        continue;
                    }
                    for (BankAcctCurrencyVO currencyVO : currencyList) {
                        balanceMissingInfo(enterpriseBankAcctVOWithRange, currencyVO, date, balanceMissingAccountSet, balanceMissings, balanceUnMatchAccounts);
                        balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);

                    }
                }
            }
        } else {
            Map<String, List<AccountRealtimeBalance>> balanceByAccountMap = balanceList.stream().collect(Collectors.groupingBy(AccountRealtimeBalance::getEnterpriseBankAccount));


            for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS) {
                List<AccountRealtimeBalance> balanceByAccountList = balanceByAccountMap.get(enterpriseBankAcctVOWithRange.getId());

                if (CollectionUtils.isEmpty(enterpriseBankAcctVOWithRange.getCurrencyList())) {
                    continue;
                }
                //币种保留enable=1的数据;
                List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVOWithRange.getCurrencyList().stream().filter(item -> item.getCurrencyEnable() != null && item.getCurrencyEnable() == 1).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(currencyList)) {
                    continue;
                }
                for (BankAcctCurrencyVO currencyVO : currencyList) {
                    if (CollectionUtils.isNotEmpty(balanceByAccountList)) {
                        Map<String, List<AccountRealtimeBalance>> balanceByCurrencyMap = balanceByAccountList.stream().collect(Collectors.groupingBy(AccountRealtimeBalance::getCurrency));
                        if (balanceByCurrencyMap.containsKey(currencyVO.getCurrency())) {
                            List<AccountRealtimeBalance> balanceByCurrencyList = balanceByCurrencyMap.get(currencyVO.getCurrency());
                            //流水缺失/不等检查
                            Map<Date, AccountRealtimeBalance> balanceDataByAccountMap = new HashMap<>();
                            if (CollectionUtils.isNotEmpty(balanceByCurrencyList)) {
                                balanceDataByAccountMap = balanceByCurrencyList.stream().collect(Collectors.toMap(item -> item.getBalancedate(), Function.identity(), (k1, k2) -> k1));
                            }
                            for (Date date : everyDays) {
                                if (balanceDataByAccountMap.containsKey(date)) {
                                    AccountRealtimeBalance balance = balanceDataByAccountMap.get(date);
                                    if (balance.getIsconfirm()) {
                                        continue;
                                    }
                                    //不等检查余额不等 余额对比结果=不相等
                                    if (!balance.getIsconfirm()) {
                                        if (balance.getBalancecontrast() != null && balance.getBalancecontrast() == 0) {
                                            //计数(银行账号+余额日期)
                                            balanceUnEqualAccountSet.add(enterpriseBankAcctVOWithRange.getId() + "," + currencyVO.getCurrency() + "," + date);
                                            Map<String, Object> tmp = new HashMap<>();
                                            tmp.put("code", enterpriseBankAcctVOWithRange.getCode());
                                            tmp.put("name", enterpriseBankAcctVOWithRange.getName());
                                            tmp.put("orgId", enterpriseBankAcctVOWithRange.getOrgid());
                                            tmp.put("orgName", enterpriseBankAcctVOWithRange.getOrgidName());
                                            tmp.put("enterpriseBankAccount", enterpriseBankAcctVOWithRange.getId());
                                            tmp.put("account", enterpriseBankAcctVOWithRange.getAccount());
                                            tmp.put("accountName", enterpriseBankAcctVOWithRange.getName());
                                            tmp.put("currency", currencyVO.getCurrency());
                                            tmp.put("currencyName", currencyVO.getCurrencyName());
                                            tmp.put("date", date);
                                            tmp.put("checkResult", balance.get("balancecheckinstruction"));
                                            balanceUnMatchAccounts.add(tmp);
                                        }
                                        //流水接入/导入提醒
                                        //银行交易流水 serviceCode ficmp0018  billnum cmp_dllist uri cmp.bankdealdetail.BankDealDetail  tablename cmp_bankdealdetail
                                        balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);
                                    }
                                } else {
                                    balanceMissingInfo(enterpriseBankAcctVOWithRange, currencyVO, date, balanceMissingAccountSet, balanceMissings, balanceUnMatchAccounts);
                                    balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);
                                }
                            }
                        } else {
                            for (Date date : everyDays) {
                                balanceMissingInfo(enterpriseBankAcctVOWithRange, currencyVO, date, balanceMissingAccountSet, balanceMissings, balanceUnMatchAccounts);
                                balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);
                            }
                        }
                    } else {
                        for (Date date : everyDays) {
                            balanceMissingInfo(enterpriseBankAcctVOWithRange, currencyVO, date, balanceMissingAccountSet, balanceMissings, balanceUnMatchAccounts);
                            balanceInfo(enterpriseBankAcctVOWithRange, currencyVO, date, dealListMap, accountSet, accountMapList);
                        }
                    }
                }
            }
        }

        if (accountSet.size() == 0) {
            vo.setBillInfo(true);
        } else {
            vo.setBillInfo(false);
            vo.setAccountNum(accountSet.size());
            vo.setAccountList(accountSet.stream().collect(Collectors.toList()));
            vo.setBankAccountList(accountMapList);
        }
        //余额检查
        int balanceAccountTotal = balanceMissingAccountSet.size() + balanceUnEqualAccountSet.size();
        if (balanceAccountTotal == 0) {
            vo.setBalanceCheck(true);
        } else {
            vo.setBalanceCheck(false);
            vo.setBalanceUnMatchNum(balanceAccountTotal);
            vo.setBalanceMissingNum(balanceMissingAccountSet.size());
            vo.setBalanceUnEqualNum(balanceUnEqualAccountSet.size());
            vo.setBalanceMissingAccountMapList(balanceMissings);
            vo.setBalanceUnMatchNumAccountMapList(balanceUnMatchAccounts);
        }
        //超期预警
        int warnningDays = benchVO.getWarningDays() == null ? 7 : benchVO.getWarningDays();
        Date warnningTranDate = DateUtils.dateAddDays(new Date(), -warnningDays);
        if (warnningTranDate.compareTo(startDate) < 0) {
            vo.setOverDayInfo(true);
            vo.setBillNum(0L);
            vo.setOverDays(warnningDays);
        } else {
            //
            fillOrgAndAccount(benchVO, IServicecodeConstant.CMPBANKRECONCILIATION);
            if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
                vo.setOverDayInfo(true);
                vo.setBillNum(0L);
                vo.setOverDays(warnningDays);
            } else {
                QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
                //银行账号考虑多币种场景
                if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
                    List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
                    if (CollectionUtils.isNotEmpty(accountNoList)) {
                        List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                        conditionGroup2.addCondition(QueryCondition.name("bankaccount").in(accounts));
                        //暂时先去掉币种
                        //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
                    }
                } else {
                    conditionGroup2.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
                }
                QueryConditionGroup orgConditionGroup = new QueryConditionGroup(ConditionOperator.or);
                orgConditionGroup.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
                if (StringUtils.isEmpty(benchVO.getAccentity())) {
                    orgConditionGroup.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
                }
                conditionGroup2.addCondition(
                        QueryCondition.name("initflag").eq(0),
                        QueryCondition.name("accentity").is_not_null(),
                        QueryCondition.name("tran_date").between(startDate, endDate),
                        QueryCondition.name("tran_date").lt(warnningTranDate)
                );
                conditionGroup2.addCondition(QueryCondition.name(ICmpConstant.SERIAL_DEAL_END_STATE).not_eq(SerialdealendState.END.getValue()));
                long billNum = PageUtils.queryCount(conditionGroup2, BankReconciliation.ENTITY_NAME);

                if (billNum > 0) {
                    vo.setOverDayInfo(false);
                    vo.setBillNum(billNum);
                    vo.setOverDays(warnningDays);
                } else {
                    vo.setOverDayInfo(true);
                    vo.setBillNum(0L);
                    vo.setOverDays(warnningDays);
                }
            }
        }
        //风险项数
        int riskNum = 0;
        if (vo.getBillInfo() == false) {
            riskNum++;
        }
        if (vo.getBalanceCheck() == false) {
            riskNum++;
        }
        if (vo.getOverDayInfo() == false) {
            riskNum++;
        }
        vo.setRiskNum(riskNum);
        return vo;
    }

    private void balanceInfo(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange, BankAcctCurrencyVO currencyVO, Date date, Map<String, List<BankDealDetail>> dealListMap, Set<String> accountSet, List<Map<String, Object>> accountMapList) throws Exception {
        //流水接入/导入提醒
        //银行交易流水 serviceCode ficmp0018  billnum cmp_dllist uri cmp.bankdealdetail.BankDealDetail  tablename cmp_bankdealdetail
        String key = enterpriseBankAcctVOWithRange.getId() + "_" + currencyVO.getCurrency() + "_" + DateEventUtils.getDate(date);
        if (dealListMap == null || !dealListMap.containsKey(key)) {
            accountSet.add(enterpriseBankAcctVOWithRange.getId() + "," + currencyVO.getCurrency());
            Map<String, Object> importInfoMap = new HashMap<>();
            importInfoMap.put("code", enterpriseBankAcctVOWithRange.getCode());
            importInfoMap.put("name", enterpriseBankAcctVOWithRange.getName());
            importInfoMap.put("orgId", enterpriseBankAcctVOWithRange.getOrgid());
            importInfoMap.put("orgName", enterpriseBankAcctVOWithRange.getOrgidName());
            importInfoMap.put("enterpriseBankAccount", enterpriseBankAcctVOWithRange.getId());
            importInfoMap.put("account", enterpriseBankAcctVOWithRange.getAccount());
            importInfoMap.put("accountName", enterpriseBankAcctVOWithRange.getName());
            importInfoMap.put("currency", currencyVO.getCurrency());
            importInfoMap.put("currencyName", currencyVO.getCurrencyName());
            importInfoMap.put("date", date);
            accountMapList.add(importInfoMap);
        }
//        QuerySchema dealSchema = QuerySchema.create().addSelect("id")
//                .appendQueryCondition(
//                        QueryCondition.name("enterpriseBankAccount").eq(enterpriseBankAcctVOWithRange.getAccount()),
//                        QueryCondition.name("currency").eq(currencyVO.getCurrency()),
//                        QueryCondition.name("tranDate").eq(date)
//                );
//        List<BankDealDetail> dealList = MetaDaoHelper.queryObject(BankDealDetail.ENTITY_NAME, dealSchema, null);
//        //按日判断交易明细是否有值，对应日期交易明细数据为空的银行账户
//        if (CollectionUtils.isEmpty(dealList)) {
//            //计数(银行账号+币种)
//            accountSet.add(enterpriseBankAcctVOWithRange.getId() + "," + currencyVO.getCurrency());
//            Map<String, Object> importInfoMap = new HashMap<>();
//            importInfoMap.put("code", enterpriseBankAcctVOWithRange.getCode());
//            importInfoMap.put("name", enterpriseBankAcctVOWithRange.getName());
//            importInfoMap.put("orgId", enterpriseBankAcctVOWithRange.getOrgid());
//            importInfoMap.put("orgName", enterpriseBankAcctVOWithRange.getOrgidName());
//            importInfoMap.put("enterpriseBankAccount", enterpriseBankAcctVOWithRange.getId());
//            importInfoMap.put("account", enterpriseBankAcctVOWithRange.getAccount());
//            importInfoMap.put("accountName", enterpriseBankAcctVOWithRange.getName());
//            importInfoMap.put("currency", currencyVO.getCurrency());
//            importInfoMap.put("currencyName", currencyVO.getCurrencyName());
//            importInfoMap.put("date", date);
//            accountMapList.add(importInfoMap);
//        }
    }

    public void balanceMissingInfo(EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange, BankAcctCurrencyVO currencyVO, Date date, Set<String> balanceMissingAccountSet, List<Map<String, Object>> balanceMissings, List<Map<String, Object>> balanceUnMatchAccounts) {
        //流水缺失 计数(银行账号)
        balanceMissingAccountSet.add(enterpriseBankAcctVOWithRange.getId() + "," + currencyVO.getCurrency() + "," + date);
        Map<String, Object> tmp = new HashMap<>();
        tmp.put("code", enterpriseBankAcctVOWithRange.getCode());
        tmp.put("name", enterpriseBankAcctVOWithRange.getName());
        tmp.put("orgId", enterpriseBankAcctVOWithRange.getOrgid());
        tmp.put("orgName", enterpriseBankAcctVOWithRange.getOrgidName());
        tmp.put("enterpriseBankAccount", enterpriseBankAcctVOWithRange.getId());
        tmp.put("account", enterpriseBankAcctVOWithRange.getAccount());
        tmp.put("accountName", enterpriseBankAcctVOWithRange.getName());
        tmp.put("currency", currencyVO.getCurrency());
        tmp.put("currencyName", currencyVO.getCurrencyName());
        tmp.put("date", date);
        tmp.put("checkResult", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540079E", "余额缺失") /* "余额缺失" */);
        balanceMissings.add(tmp);
        balanceUnMatchAccounts.add(tmp);
    }

    /**
     * 流水处理速率
     *
     * @param flowBenchVO
     * @return
     */
    @Override
    public FlowProcessCheckVO processCheck(FlowBenchVO flowBenchVO) throws Exception {
        String formatStr = "0.00";
        FlowProcessCheckVO vo = new FlowProcessCheckVO();
        vo.setFlowPercent("100");
        vo.setUnProcessFlowNum(0L);
        vo.setProcessedFlowNum(0L);

        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());
        if (benchVO == null) {
            return vo;
        }
        String[] dataRange = benchVO.getDateRange();
        Date startDate = DateUtils.parseDate(dataRange[0]);
        Date endDate = DateUtils.parseDate(dataRange[1]);

        fillOrgAndAccount(benchVO, IServicecodeConstant.CMPBANKRECONCILIATION);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return vo;
        }
//        CtmJSONObject ctmJSONObject = new CtmJSONObject();
//        if (StringUtils.isNotEmpty(benchVO.getAccentity())) {
//            ctmJSONObject.put("accentity", JSONBuilderUtils.beanToString(benchVO.getAccentity().split(",")));
//        }
//        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
//            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
//            if (CollectionUtils.isNotEmpty(accountNoList)) {
//                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
//                ctmJSONObject.put("bankaccount", JSONBuilderUtils.beanToString(accounts));
//                ctmJSONObject.put("currency", accountNoList.get(0).get("currencyId"));
//            }
//
//        }
//        ctmJSONObject.put("startDate", flowBenchVO.getDateRange()[0]);
//        ctmJSONObject.put("endDate", flowBenchVO.getDateRange()[1]);
//        ctmJSONObject.put("billNo", "cmp_bankreconciliationlist");
//        ctmJSONObject.put("published_type", "1");
//        HashMap<String, Object> serviceImplCount = bankreconciliationCountServiceImpl.getCount(ctmJSONObject);
//        if (serviceImplCount != null && !serviceImplCount.isEmpty()) {
//            long ended = serviceImplCount.get("ended") == null ? 0 : Long.parseLong(serviceImplCount.get("ended").toString());
//            long all = serviceImplCount.get("all") == null ? 0 : Long.parseLong(serviceImplCount.get("all").toString());
//            if (all != 0L) {
//                double percent = (double) ended / all;
//                vo.setFlowPercent(new DecimalFormat(formatStr).format(percent * 100));
//                vo.setUnProcessFlowNum(all - ended);
//                vo.setProcessedFlowNum(ended);
//            }
//        }
        QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
        //银行账号考虑多币种场景
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup2.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup2.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
        }
        QueryConditionGroup orgConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        orgConditionGroup.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        if (StringUtils.isEmpty(benchVO.getAccentity())) {
            orgConditionGroup.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
        }
        conditionGroup2.addCondition(
                QueryCondition.name("initflag").eq(0),
                QueryCondition.name("accentity").is_not_null(),
                QueryCondition.name("tran_date").between(startDate, endDate)
        );
        long billNum = PageUtils.queryCount(conditionGroup2, BankReconciliation.ENTITY_NAME);
        long finishNum = bankreconciliationCountServiceImpl.getBankRecEndedCount(conditionGroup2);
        if (billNum != 0L) {
            double percent = (double) finishNum / billNum;
            vo.setFlowPercent(new DecimalFormat(formatStr).format(percent * 100));
            vo.setUnProcessFlowNum(billNum - finishNum);
            vo.setProcessedFlowNum(finishNum);
        }
        return vo;
    }

    /**
     * 这一期写死
     *
     * @param flowBenchVO
     * @return
     */
    @Override
    public FlowUnionPayMonitorVO queryUnionpayMonitor(FlowBenchVO flowBenchVO) {
        FlowUnionPayMonitorVO vo = new FlowUnionPayMonitorVO();
        vo.setTotalNum(8L);
        vo.setNormalNum(7L);
        vo.setExceptNum(1L);
        vo.setNormalList(Arrays.asList(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A2", "交通银行") /* "交通银行" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A0", "工商银行") /* "工商银行" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A3", "建设银行") /* "建设银行" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A1", "农业银行") /* "农业银行" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A4", "招商银行") /* "招商银行" */));
        vo.setExceptList(Collections.singletonList(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A5", "汇丰银行") /* "汇丰银行" */));
        return vo;
    }

    /**
     * 银行账户余额 bam_account_latest_balance,cmp_retibalist,cmp.accountrealtimebalance.AccountRealtimeBalance, cmp_bankaccount_realtimebalance
     *
     * @param flowBenchVO
     * @return
     * @throws Exception
     */
    @Override
    public FlowRpaMonitorVO queryRpaMonitor(FlowBenchVO flowBenchVO) throws Exception {
        FlowRpaMonitorVO vo = new FlowRpaMonitorVO();
        vo.setDownloadFlowNum(0L);
        vo.setKeyIdentifyNum(0L);
        vo.setRefundIdentifyNum(0L);
        vo.setAutoRelationBillNum(0L);
        vo.setAutoGenerateBillNum(0L);
        vo.setAutoRelationVoucherNum(0L);
        vo.setAutoPublishFlowNum(0L);
        vo.setReceiptDetailNum(0L);
        vo.setReceiptFileNum(0L);
        vo.setDownloadBalanceNum(0L);
        vo.setAccountNum(0L);


        FlowBenchVO benchVO = this.selectById(flowBenchVO.getId());
        if (benchVO == null) {
            return vo;
        }
        String[] dataRange = benchVO.getDateRange();
        Date startDate = DateUtils.parseDate(dataRange[0]);
        Date endDate = DateUtils.parseDate(dataRange[1]);
        //银行流水
        fillOrgAndAccount(benchVO, IServicecodeConstant.BANKRECONCILIATION);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return vo;
        }
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //银行账号考虑多币种场景
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
        }
        QueryConditionGroup orgConditionGroup = new QueryConditionGroup(ConditionOperator.or);
        orgConditionGroup.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        if (StringUtils.isEmpty(benchVO.getAccentity())) {
            orgConditionGroup.addCondition(QueryCondition.name("orgid").in(benchVO.getAccentitySet()));
        }
        conditionGroup.addCondition(orgConditionGroup);
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(
                QueryCondition.name("tran_date").between(startDate, endDate),
                QueryCondition.name("accentity").is_not_null(),
                QueryCondition.name("initflag").eq(0)
        );
        querySchema.appendQueryCondition(conditionGroup);
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if (!CollectionUtils.isEmpty(bankReconciliationList)) {
            //流水下载笔数 数据来源=银企联下载
            Long downloadFlowNum = bankReconciliationList.stream().filter(item -> item.getDataOrigin() != null && item.getDataOrigin() == DateOrigin.DownFromYQL).count();

            //退票 辨识笔数 退票自动辨识=是
            Long refundIdentifyNum = bankReconciliationList.stream().filter(item -> item.getRefundauto() != null && item.getRefundauto()).count();
            //收付单据自动关联笔数 主表-收付单据自动关联=是
            Long autoRelationBillNum = bankReconciliationList.stream().filter(item -> item.getAutoassociation() != null && item.getAutoassociation()).count();
            //收付单据自动生单笔数 主表-收付单据自动生单=是
            Long autoGenerateBillNum = bankReconciliationList.stream().filter(item -> item.getIsautocreatebill() != null && item.getIsautocreatebill()).count();
            //业务凭据自动关联笔数 主表-业务凭据自动关联=是 ??
            Long autoRelationVoucherNum = bankReconciliationList.stream().filter(item -> item.getIsautocreatebill() != null && item.getIsautocreatebill()).count();
            //流水自动发布笔数 主表-流水自动发布=是
            Long autoPublishFlowNum = bankReconciliationList.stream().filter(item -> item.getSerialauto() != null && item.getSerialauto()).count();

            vo.setDownloadFlowNum(downloadFlowNum);
            //关键信息辨识笔数= 流水笔数
            vo.setKeyIdentifyNum((long) bankReconciliationList.size());
            vo.setRefundIdentifyNum(refundIdentifyNum);
            vo.setAutoRelationBillNum(autoRelationBillNum);
            vo.setAutoGenerateBillNum(autoGenerateBillNum);
            vo.setAutoRelationVoucherNum(autoRelationVoucherNum);
            vo.setAutoPublishFlowNum(autoPublishFlowNum);
        }
        // 银行交易回单
        fillOrgAndAccount(benchVO, IServicecodeConstant.BANKRECEIPTMATCH);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return vo;
        }
        //银行账号考虑多币种场景
        QueryConditionGroup conditionGroup2 = new QueryConditionGroup(ConditionOperator.and);
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup2.addCondition(QueryCondition.name("enterpriseBankAccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup2.addCondition(QueryCondition.name("enterpriseBankAccount").in(benchVO.getBankAccountSet()));
        }
        QuerySchema receiptSchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(
                QueryCondition.name("tranDate").between(startDate, endDate)
        );
        receiptSchema.appendQueryCondition(conditionGroup2);
        List<BankElectronicReceipt> receiptlist = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, receiptSchema, null);
        if (!CollectionUtils.isEmpty(receiptlist)) {
            //回单明细下载笔数 数据来源=银企联下载
            Long receiptDetailNum = receiptlist.stream().filter(item -> item.getDataOrigin() != null && item.getDataOrigin() == DateOrigin.DownFromYQL).count();
            //回单文件下载笔数 数据来源=银企联下载 && 是否已下载回单=是
            Long receiptFileNum = receiptlist.stream().filter(item -> item.getDataOrigin() != null && item.getDataOrigin() == DateOrigin.DownFromYQL && item.getIsdown() != null && item.getIsdown()).count();
            vo.setReceiptDetailNum(receiptDetailNum);
            vo.setReceiptFileNum(receiptFileNum);
        }

        vo.setDownloadBalanceNum(this.downloadBalanceNum(startDate, endDate, benchVO));
        vo.setAccountNum(this.accountNum(startDate, endDate, benchVO));
        return vo;
    }

    private Long accountNum(Date startDate, Date endDate, FlowBenchVO benchVO) throws Exception {
        String serviceCode = IServicecodeConstant.CMPBANKRECONCILIATION;
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = new ArrayList<>();
        Set<String> orgs = null;
        if (StringUtils.isNotEmpty(benchVO.getAccentity())) {
            orgs = Arrays.stream(benchVO.getAccentity().split(",")).collect(Collectors.toSet());
        } else {
            orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(serviceCode);
        }
        if (CollectionUtils.isEmpty(orgs)) {
            log.error("根据serviceCode:{}+userId:{}查询出来的组织信息为空", serviceCode, InvocationInfoProxy.getUserid());
            return 0L;
        } else {
            benchVO.setAccentitySet(orgs);
            Set<String> accounts = new HashSet<>();
            if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
                Set<String> accountSet = benchVO.getBankAccountSet();
                if (CollectionUtils.isEmpty(accountSet)) {
                    return 0L;
                }
                try {
                    EnterpriseParams enterpriseParams = new EnterpriseParams();
                    enterpriseParams.setIdList(new ArrayList<>(accountSet));
                    enterpriseParams.setOrgidList(new ArrayList<>(orgs));
                    enterpriseParams.setPageSize(5000);
                    enterpriseBankAcctVOS = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
                }catch (Exception e){
                    log.error("查询企业银行账号异常,", e);
                    return 0L;
                }
            } else {
                try {
                    EnterpriseParams enterpriseParams = new EnterpriseParams();
                    enterpriseParams.setOrgidList(new ArrayList<>(orgs));
                    enterpriseParams.setPageSize(5000);
                    enterpriseBankAcctVOS = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeByCondition(enterpriseParams);
                }catch (Exception e){
                    log.error("查询企业银行账号异常,", e);
                    return 0L;
                }
            }
        }
        if (CollectionUtils.isEmpty(enterpriseBankAcctVOS)) {
            return 0L;
        }
        int i = 0;
        for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange : enterpriseBankAcctVOS) {
            List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVOWithRange.getCurrencyList();
            if (CollectionUtils.isNotEmpty(currencyList)) {
                for (BankAcctCurrencyVO bankAcctCurrencyVO : currencyList) {
                    if (bankAcctCurrencyVO.getCurrencyEnable() != null && bankAcctCurrencyVO.getCurrencyEnable() == 1) {
                        i++;
                    }
                }
            }
        }
        return (long) i;

//        fillOrgAndAccount(benchVO, IServicecodeConstant.BANKRECONCILIATION);
//        if(CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())){
//            return 0L;
//        }
//        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
//        QueryConditionGroup conditionGroup4 = new QueryConditionGroup(ConditionOperator.and);
//
//        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
//            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
//            if (CollectionUtils.isNotEmpty(accountNoList)) {
//                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
//                conditionGroup.addCondition(QueryCondition.name("bankaccount").in(accounts));
//                //暂时先去掉币种
//                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
//            }
//        } else {
//            conditionGroup.addCondition(QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()));
//        }
//        QuerySchema initSchema = QuerySchema.create().addSelect("bankaccount,currency,count(1)").appendQueryCondition(
////                QueryCondition.name("accentity").in(benchVO.getAccentitySet()),
////                QueryCondition.name("bankaccount").in(benchVO.getBankAccountSet()),
//                QueryCondition.name("accountdate").elt(endDate)
//        ).addGroupBy("bankaccount,currency");
//        initSchema.appendQueryCondition(conditionGroup4);
//        // 余额检查-账户数
//        List<InitData> initDataList = MetaDaoHelper.queryObject(InitData.ENTITY_NAME, initSchema, null);
//        if (CollectionUtils.isNotEmpty(initDataList)) {
//            return (long) initDataList.size();
//        }
//        return 0L;
    }

    private Long downloadBalanceNum(Date startDate, Date endDate, FlowBenchVO benchVO) throws Exception {
        // 银行账户余额(实时/历史) bam_account_latest_balance
        fillOrgAndAccount(benchVO, IServicecodeConstant.CMPBANKRECONCILIATION, false);
        if (CollectionUtils.isEmpty(benchVO.getAccentitySet()) || CollectionUtils.isEmpty(benchVO.getBankAccountSet())) {
            return 0L;
        }
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            List<Map<String, Object>> accountNoList = benchVO.getAccountNoList();
            if (CollectionUtils.isNotEmpty(accountNoList)) {
                List<String> accounts = accountNoList.stream().map(item -> (String) item.get("id")).collect(Collectors.toList());
                conditionGroup.addCondition(QueryCondition.name("enterpriseBankAccount").in(accounts));
                //暂时先去掉币种
                //conditionGroup.addCondition(QueryCondition.name("currency").eq(accountNoList.get(0).get("currencyId")));
            }
        } else {
            conditionGroup.addCondition(QueryCondition.name("enterpriseBankAccount").in(benchVO.getBankAccountSet()));
        }

        //com.yonyoucloud.fi.cmp.accounthistorybalance.rule.BeforeQueryHistoryBalanceRule.execute
//        QueryConditionGroup datasourceConditionGroup = new QueryConditionGroup(ConditionOperator.or);
//        QueryConditionGroup queryConditionGroup3 = new QueryConditionGroup(ConditionOperator.and);
//        queryConditionGroup3.addCondition(QueryCondition.name("datasource").eq(BalanceAccountDataSourceEnum.CURRENTDAY_BAL.getCode()));
        conditionGroup.addCondition(QueryCondition.name("first_flag").eq(0));
        conditionGroup.addCondition(QueryCondition.name("datasource").in(Arrays.asList("3", "6")));

//        datasourceConditionGroup.addCondition(queryConditionGroup3);
//        datasourceConditionGroup.addCondition(QueryCondition.name("datasource").eq(BalanceAccountDataSourceEnum.BANK_ENTERPRISE_DOWNLOAD.getCode()));
//        conditionGroup.addCondition(datasourceConditionGroup);

//        conditionGroup.addCondition(QueryCondition.name("accentity").in(benchVO.getAccentitySet()));
        conditionGroup.addCondition(QueryCondition.name("balancedate").between(startDate, endDate));
        //余额下载笔数
        return PageUtils.queryCount(conditionGroup, AccountRealtimeBalance.ENTITY_NAME);
    }

    /**
     * 查询用户授权组织
     *
     * @param benchVO
     * @param serviceCode 服务编码
     * @throws Exception
     */
    private void fillOrgAndAccount(FlowBenchVO benchVO, String serviceCode) throws Exception {
        fillOrgAndAccount(benchVO, serviceCode, true);
    }

    private void fillOrgAndAccount(FlowBenchVO benchVO, String serviceCode, boolean withStop) throws Exception {
        Set<String> orgs = null;
        if (StringUtils.isNotEmpty(benchVO.getAccentity())) {
            orgs = Arrays.stream(benchVO.getAccentity().split(",")).collect(Collectors.toSet());
        } else {
            orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(serviceCode);
        }
        if (CollectionUtils.isEmpty(orgs)) {
            log.error("根据serviceCode:{}+userId:{}查询出来的组织信息为空", serviceCode, InvocationInfoProxy.getUserid());
//            throw new CtmException("查询组织信息失败！");
            benchVO.setAccentitySet(Collections.EMPTY_SET);
            benchVO.setBankAccountSet(Collections.EMPTY_SET);
        } else {
            benchVO.setAccentitySet(orgs);
            Set<String> accounts = new HashSet<>();
            if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
                if (CollectionUtils.isNotEmpty(benchVO.getAccountNoList())) {
                    accounts = benchVO.getAccountNoList().stream().filter(item -> item.get("id") != null).map(item -> (String) item.get("id")).collect(Collectors.toSet());
                }
                List<String> accountIds = new ArrayList<>();
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setIdList(new ArrayList<>(accounts));
                enterpriseParams.setOrgidList(new ArrayList<>(orgs));
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
                if (CollectionUtils.isNotEmpty(enterpriseBankAcctVOS)) {
                    accounts = enterpriseBankAcctVOS.stream().map(item -> item.getId()).collect(Collectors.toSet());
                }
            } else {
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setOrgidList(new ArrayList<>(orgs));
                if (withStop) {
                    enterpriseParams.setEnables(Arrays.asList(1, 2));
                }
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllWithRange(enterpriseParams);
                if (CollectionUtils.isNotEmpty(enterpriseBankAcctVOS)) {
                    accounts = enterpriseBankAcctVOS.stream().map(item -> item.getId()).collect(Collectors.toSet());
                }
            }
            if (CollectionUtils.isEmpty(accounts)) {
                log.error("查询出来的企业银行账号信息为空");
                benchVO.setBankAccountSet(Collections.EMPTY_SET);
            } else {
                benchVO.setBankAccountSet(accounts);
            }
        }
    }

    /**
     * 账号+币种拼接条件
     *
     * @param benchVO
     * @param accountField
     * @param currencyField
     * @return
     * @throws Exception
     */
    private QueryConditionGroup addConditionGroupByOrgAndAccount(FlowBenchVO benchVO, String accountField, String currencyField) throws Exception {
        if (StringUtils.isNotEmpty(benchVO.getAccountCurrency())) {
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            List<String> accountCurrencyList = Arrays.stream(benchVO.getAccountCurrency().split(",")).collect(Collectors.toList());
            QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.or);

            //根据企业银行币种的id查询对应的币种和账号信息
            QuerySchema accountNoSchema = QuerySchema.create().addSelect("id,currency,bankacct.id,bankacct.code,bankacct.name")
                    .appendQueryCondition(QueryCondition.name("id").in(accountCurrencyList));
            List<BizObject> accountNos = MetaDaoHelper.queryObject("bd.enterprise.BankAcctCurrencyVO", accountNoSchema, "ucfbasedoc");
            if (CollectionUtils.isNotEmpty(accountNos)) {
                accountNos.forEach(bizobject -> {
                    QueryConditionGroup tmpGroup = new QueryConditionGroup(ConditionOperator.and);
                    tmpGroup.addCondition(QueryCondition.name(accountField).eq(bizobject.getString("accountNoList")));
                    tmpGroup.addCondition(QueryCondition.name(currencyField).eq(bizobject.getString("currencyId")));
                    conditionGroup1.addCondition(tmpGroup);
                });
            }
            conditionGroup.addCondition(conditionGroup1);
            return conditionGroup;
        }
        return null;
    }

    /**
     * 根据币种编号查询id
     *
     * @return
     */
    private String queryCurrencyId() throws Exception {
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").eq("CNY"));
        schema.addCondition(conditionGroup);
        List<BizObject> currencyId = MetaDaoHelper.queryObject("bd.currencytenant.CurrencyTenantVO", schema, "ucfbasedoc");
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(currencyId)) {
            return currencyId.get(0).get("id").toString();
        }
        return null;
    }

    /**
     * 默认汇率类型 基准汇率
     *
     * @return
     * @throws Exception
     */
    private String queryDefaltExchangetype() throws Exception {
        BdRequestParams bdRequestParams = new BdRequestParams();
        bdRequestParams.setCode("01");
        ExchangeRateTypeVO exchangeRateTypeVO = exchangeRateTypeService.queryByCondition(bdRequestParams);
        if (exchangeRateTypeVO != null) {
            return exchangeRateTypeVO.getId();
        }
        return null;
    }

    /**
     * 企业银行账号信息 bd.enterprise.OrgFinBankacctVO
     * orgid 所属组织
     * orgGroupId 适用组织
     *
     * @param orgs
     * @return
     */
    private List<String> queryBankAccount(Set<String> orgs) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("account").distinct().appendQueryCondition(
                QueryCondition.name("orgGroupId").in(orgs)
        );
        List<BizObject> list = MetaDaoHelper.queryObject("bd.enterprise.OrgFinBankacctVO", querySchema, "ucfbasedoc");
        if (CollectionUtils.isNotEmpty(list)) {
            return list.stream().map(item -> item.getString("account")).collect(Collectors.toList());
        }
        return null;
    }


    private Date getMonthEndDay(FlowBenchVO flowBenchVO) throws Exception {
        if (StringUtils.isNotEmpty(flowBenchVO.getMonth())) {
            Date date = DateUtils.strToDate(flowBenchVO.getMonth() + "-01");
            return DateUtils.maxDateOfMonth(new Date(date.getTime()));
        } else {
            return DateUtils.maxDateOfMonth(new Date());
        }
    }

    private Date getMonthStartDay(FlowBenchVO flowBenchVO) throws Exception {
        if (flowBenchVO != null && StringUtils.isNotEmpty(flowBenchVO.getMonth())) {
            Date date = DateUtils.strToDate(flowBenchVO.getMonth() + "-01");
            return DateUtils.minDateOfMonth(new Date(date.getTime()));
        } else {
            return DateUtils.minDateOfMonth(new Date());
        }
    }

    private List<String> everyDayOfDates(Date startDate, Date endDate)throws Exception {
        return everyDayOfDates(startDate, endDate, false);
    }

    private List<String> everyDayOfDates(Date startDate, Date endDate, boolean isBeforeNow) throws Exception{
        List<String> dates = new ArrayList<>();
        if (isBeforeNow) {
            if (startDate.compareTo(new Date()) > 0) {
                return dates;
            }
            if (endDate.compareTo(new Date()) > 0) {
                endDate = new Date();
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDate localStartDate = LocalDate.parse(DateUtils.formatDate(new Date(startDate.getTime())),formatter);
        LocalDate localEndDate = LocalDate.parse(DateUtils.formatDate(new Date(endDate.getTime())),formatter);

        while (!localStartDate.isAfter(localEndDate)) {
            dates.add(localStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            localStartDate = localStartDate.plusDays(1);
        }
        return dates;
    }

    private List<Date> everyDateOfDates(Date startDate, Date endDate) throws Exception {
        return this.everyDateOfDates(startDate, endDate, false);
    }

    /**
     * @param startDate
     * @param endDate
     * @param isBeforeNow 判断开始时间和结束时间是否大于当前时间
     * @return
     */
    private List<Date> everyDateOfDates(Date startDate, Date endDate, boolean isBeforeNow) throws Exception{
        List<Date> dates = new ArrayList<>();
        if (isBeforeNow) {
            if (startDate.compareTo(new Date()) > 0) {
                return dates;
            }
            if (endDate.compareTo(new Date()) > 0) {
                endDate = new Date();
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDate localStartDate = LocalDate.parse(DateUtils.formatDate(startDate),formatter);
        LocalDate localEndDate = LocalDate.parse(DateUtils.formatDate(endDate),formatter);

        while (!localStartDate.isAfter(localEndDate)) {
            Date resultDate = DateUtil.localDateTime2Date(localStartDate.atStartOfDay());
            dates.add(resultDate);
            localStartDate = localStartDate.plusDays(1);
        }
        return dates;
    }

    private FlowBenchVO buildByEntity(BizObject item, Map<String, BizObject> accentityMap, Map<String, BizObject> accountCurrencyMap) {
        FlowBenchVO vo = new FlowBenchVO();
        vo.setId(item.getId());
        vo.setViewName(item.containsKey("viewName") ? item.get("viewName") : null);
        vo.setIsDefault(item.containsKey("isDefault") && item.get("isDefault") != null ? item.getInteger("isDefault") : 0);
        try {
            if (vo.getIsDefault() == 1) {
                Date startDate = DateUtils.dateAddDays(new Date(), -30);
                vo.setDateRange(new String[]{DateEventUtils.getDate(startDate), DateEventUtils.getDate(new Date())});
                //默认视图多语处理
                vo.setViewName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CTMFC-BE_1A20E4BC0568225B","默认视图"));
            } else {
                if (item.containsKey("startDate") && item.containsKey("startDate")) {
                    vo.setDateRange(new String[]{DateEventUtils.getDate(item.get("startDate")), DateEventUtils.getDate(item.get("endDate"))});
                }
            }
        } catch (Exception e) {
            log.error("日期转换异常", e);
            Date startDate = DateUtils.dateAddDays(new Date(), -30);
            vo.setDateRange(new String[]{DateEventUtils.getDate(startDate), DateEventUtils.getDate(new Date())});
        }
        vo.setAccentity(item.containsKey("accentity") ? item.get("accentity") : null);
        if (StringUtils.isNotEmpty(vo.getAccentity())) {
            vo.setAccentityList(Arrays.stream(vo.getAccentity().split(",")).map(accentity -> {
                Map<String, Object> map = new HashMap<>();
                BizObject bizObject = accentityMap.get(accentity);
                map.put("id", accentity);
                map.put("name", bizObject != null && bizObject.containsKey("name") ? bizObject.getString("name") : null);
                return map;
            }).collect(Collectors.toList()));
        }
        vo.setAccountCurrency(item.containsKey("accountCurrency") ? item.get("accountCurrency") : null);
        if (StringUtils.isNotEmpty(vo.getAccountCurrency())) {
            List<Map<String, Object>> accountList = new ArrayList<>();
            List<String> accountNos = new ArrayList<>();
            for (String account : vo.getAccountCurrency().split(",")) {
                Map<String, Object> map = new HashMap<>();
                BizObject bizObject = accountCurrencyMap.get(account);
                map.put("id", bizObject != null && bizObject.containsKey("bankacct_id") ? bizObject.getString("bankacct_id") : null);
                if (bizObject != null && bizObject.containsKey("bankacct_id")) {
                    accountNos.add(bizObject.getString("bankacct_id"));
                }
                map.put("name", bizObject != null && bizObject.containsKey("bankacct_name") ? bizObject.getString("bankacct_name") : null);
                //企业银行币种id
                map.put("currency", bizObject != null && bizObject.containsKey("id") ? bizObject.getString("id") : null);
                //币种id
                map.put("currencyId", bizObject != null && bizObject.containsKey("currency") ? bizObject.getString("currency") : null);
                accountList.add(map);
            }
            vo.setAccountNo(String.join(",", accountNos));
            vo.setAccountNoList(accountList);
        }
        vo.setWarningDays(item.containsKey("warningDays") ? item.get("warningDays") : null);
        vo.setCurrency(item.containsKey("currency") ? item.get("currency") : null);
        vo.setCurrencyName(item.containsKey("currency_name") ? item.get("currency_name") : null);
        if (StringUtils.isNotEmpty(vo.getCurrency())) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", vo.getCurrency());
            map.put("name", vo.getCurrencyName());
            vo.setCurrencyMap(map);
        }
        vo.setExchangeRateType(item.containsKey("exchangeRateType") ? item.get("exchangeRateType") : null);
        vo.setExchangeRateTypeName(item.containsKey("exchangeRateType_name") ? item.get("exchangeRateType_name") : null);
        if (StringUtils.isNotEmpty(vo.getExchangeRateType())) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", vo.getExchangeRateType());
            map.put("name", vo.getExchangeRateTypeName());
            vo.setExchangeRateTypeMap(map);
        }
        vo.setCurrencyUnit(item.containsKey("currencyUnit") ? item.getString("currencyUnit") : null);
        vo.setIorder(item.containsKey("iorder") ? item.get("iorder") : null);

        return vo;
    }

}
