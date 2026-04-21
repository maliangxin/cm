package com.yonyoucloud.fi.cmp.bankvouchercheck.service.impl;

import com.yonyou.diwork.common.util.BeanPropertyCopyUtil;
import com.yonyou.iuap.yms.cache.YMSRedisTemplate;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.cmp.balanceadjust.service.impl.BalanceBatchCommonService;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.bankautocheckconfig.BankAutoCheckConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoCheckConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankBillSmartCheckService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.bankvouchercheck.service.BankVoucherCheckService;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoQueryVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankVoucherInfoQueryVO;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.ReconciliationProcessItem;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBankReconciliationSettingRpcServiceImpl;
import com.yonyoucloud.fi.cmp.enums.BankReconciliationActions;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.reconciliation.ReconciliationMatchRecord;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import cn.hutool.core.thread.BlockPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.schema.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description: 银企对账工作台后台接口具体实现类
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/14 9:56
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankVoucherCheckServiceImpl implements BankVoucherCheckService {
    //线程池
    static ExecutorService executorService = null;

    @Autowired
    BankreconciliationService bankreconciliationService;
    static {
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setQueueSize(100)
                .setDaemon(false)
                .setMaximumPoolSize(100)
                .setRejectHandler(new BlockPolicy())
                .builder(10, 60,1000,"cmp-bankvouchercheck-autoreconciliation-async-");
    }

    //用来存储进度条
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, ReconciliationProcessItem>> progressMap = new ConcurrentHashMap<>();

    private YMSRedisTemplate redisTemplate = AppContext.getBean(YMSRedisTemplate.class);

    @Autowired
    private BankBillSmartCheckService bankBillSmartCheckService;
    @Autowired
    CtmCmpBankReconciliationSettingRpcServiceImpl cmpBankReconciliationSettingRpcService;
    @Autowired
    private BankAutoCheckConfigService bankAutoCheckConfigService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private BalanceBatchCommonService balanceBatchCommonService;
    @Autowired
    private CmpCheckService cmpCheckService;

    @Override
    public List<String> getReconciliationSchemeAccentityList(Short reconciliationDataSource) throws Exception {
        QuerySchema mainSchema = QuerySchema.create().addSelect("mainid.accentity as accentity").distinct();
        //启用状态
        List<Object> statusList = new ArrayList<>();
        //查询授权使用组织有权限的或者所属组织有权限的
        Set<String> orgs ;
        if (ReconciliationDataSource.BankJournal.getValue() == reconciliationDataSource){
            orgs = BillInfoUtils.getOrgPermissions("cmp_bankjournalcheck_workbench");
        }else {
            orgs = BillInfoUtils.getOrgPermissions("cmp_bankvourhcercheck_workbench");
        }
        if (orgs.size() == 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105055"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_220F3C2205600005", "对账工作台缺少已授权的主组织权限！") /* "对账工作台缺少已授权的主组织权限！" */);
        }
        //启用的
        statusList.add(EnableStatus.Enabled.getValue());
        QueryConditionGroup mainGroup = QueryConditionGroup.and(
                QueryCondition.name("mainid.accentity").in(orgs),
                QueryCondition.name("mainid.enableStatus").in(statusList),
                QueryCondition.name("mainid.reconciliationdatasource").eq(reconciliationDataSource)
        );
        //需求CM202400683，银行账户增加对应的数据权限
        String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
        if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
            mainGroup.appendCondition(QueryCondition.name("bankaccount").in(Arrays.asList(bankAccountPermissions)));
        }
        mainSchema.addCondition(mainGroup);
        List<Map<String, Object>> bankReconciliationSettings = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, mainSchema);
        List<String> list = new ArrayList<>();
        if (bankReconciliationSettings != null) {
            for (Map<String, Object> map : bankReconciliationSettings) {
                if (map.get("accentity") != null){
                    list.add(map.get("accentity").toString());
                }
            }
        }
        return list;
    }

    @Override
    public List<CtmJSONObject> getAccentityListInfo(Short reconciliationDataSource) throws Exception {
        QuerySchema mainSchema = QuerySchema.create().distinct().addSelect("mainid.accentity as accentity,mainid.accentity.name as accentity_name,mainid.accentity.code as accentity_code");
        //查询授权使用组织有权限的或者所属组织有权限的
        Set<String> orgs ;
        if (ReconciliationDataSource.BankJournal.getValue() == reconciliationDataSource){
            orgs = BillInfoUtils.getOrgPermissions("cmp_bankjournalcheck_workbench");
        }else {
            orgs = BillInfoUtils.getOrgPermissions("cmp_bankvourhcercheck_workbench");
        }
        //启用状态
        List<Object> statusList = new ArrayList<>();
        //启用的
        statusList.add(EnableStatus.Enabled.getValue());
        QueryConditionGroup mainGroup = QueryConditionGroup.and(
                QueryCondition.name("mainid.accentity").in(orgs),
                QueryCondition.name("mainid.enableStatus").in(statusList),
                QueryCondition.name("mainid.reconciliationdatasource").eq(reconciliationDataSource)
        );
        //需求CM202400683，银行账户增加对应的数据权限
        String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
        if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
            mainGroup.appendCondition(QueryCondition.name("bankaccount").in(Arrays.asList(bankAccountPermissions)));
        }
        mainSchema.addCondition(mainGroup);
        List<Map<String, Object>> bankReconciliationSettings = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, mainSchema);
        List<CtmJSONObject> list = new ArrayList<>();
        Set<String> accentitySets = new HashSet<>();
        if (bankReconciliationSettings != null) {
            for (Map<String, Object> map : bankReconciliationSettings) {
                if (map.get("accentity") != null){
                    if(!accentitySets.contains(map.get("accentity").toString())){
                        CtmJSONObject ctmJSONObject = new CtmJSONObject();
                        ctmJSONObject.put("accentity",map.get("accentity").toString());
                        ctmJSONObject.put("accentity_code",map.get("accentity_code").toString());
                        ctmJSONObject.put("accentity_name",map.get("accentity_name").toString());
                        list.add(ctmJSONObject);
                        accentitySets.add(map.get("accentity").toString());
                    }
                }
            }
        }
        // 按照 accentity_code 正序排列
        Collections.sort(list, new Comparator<CtmJSONObject>() {
            @Override
            public int compare(CtmJSONObject o1, CtmJSONObject o2) {
                return o1.getString("accentity_code").compareTo(o2.getString("accentity_code"));
            }
        });
        return list;
    }

    @Override
    public List<BankAccountInfoVO> queryBankAccountInfo(BankAccountInfoQueryVO bankAccountInfoQueryVO) throws Exception {
        List<BankAccountInfoVO> bankAccountInfoVOList = new ArrayList<>();
        //若未传递对账组织，则取全部有权限，且已启用数据源为凭证的对账组织
        if (bankAccountInfoQueryVO.getAccentityList().size() == 0) {
            List<String> accentityList = getReconciliationSchemeAccentityList(bankAccountInfoQueryVO.getReconciliationDataSource());
            if (accentityList.size() == 0) {
                return bankAccountInfoVOList;
            }
            bankAccountInfoQueryVO.setAccentityList(accentityList);
        }
        //获取对账方案，银行账户+币种+对账组织维度数据
        QuerySchema schema = QuerySchema.create().distinct().addSelect("mainid as reconciliationScheme,mainid.bankreconciliationschemename as reconciliationScheme_name," +
                "bankaccount,bankaccount.name as bankaccount_name,bankaccount.account as bankaccount_account,bankaccount.bank as banktype,bankaccount.bank.name as banktype_name, " +
                "currency,currency.name as currency_name,currency.moneyDigit as currency_moneyDigit, mainid.accentity as accentity,mainid.accentity.name as accentity_name,mainid.accentity.code as accentity_code");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("mainid.accentity").in(bankAccountInfoQueryVO.getAccentityList())
        );
        //启用状态
        List<Object> statusList = new ArrayList<>();
        //启用的
        statusList.add(EnableStatus.Enabled.getValue());
        group.appendCondition(
                QueryCondition.name("enableStatus_b").in(statusList),
                QueryCondition.name("mainid.enableStatus").in(statusList),
                QueryCondition.name("mainid.reconciliationdatasource").eq(bankAccountInfoQueryVO.getReconciliationDataSource())
        );
        //银行账户
        if (bankAccountInfoQueryVO.getBankAccountList().size() > 0){
            group.appendCondition(QueryCondition.name("bankaccount").in(bankAccountInfoQueryVO.getBankAccountList()));
        }
        //币种
        if (bankAccountInfoQueryVO.getCurrencyList().size() > 0) {
            group.appendCondition(QueryCondition.name("currency").in(bankAccountInfoQueryVO.getCurrencyList()));
        }
        //银行类别
        if (bankAccountInfoQueryVO.getBanktypeList().size() > 0) {
            group.appendCondition(QueryCondition.name("bankaccount.bankNumber.bank").in(bankAccountInfoQueryVO.getBanktypeList()));
        }
        //对账方案
        if (bankAccountInfoQueryVO.getReconciliationSchemeList().size() > 0) {
            group.appendCondition(QueryCondition.name("mainid").in(bankAccountInfoQueryVO.getReconciliationSchemeList()));
        }
        //需求CM202400683，银行账户增加对应的数据权限
        String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
        if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
            group.appendCondition(QueryCondition.name("bankaccount").in(Arrays.asList(bankAccountPermissions)));
        }
        schema.addCondition(group);
        List<Map<String, Object>> bankReconciliationSetting_bs = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        for (Map<String, Object> map : bankReconciliationSetting_bs) {
            BankAccountInfoVO bankAccountInfoVO = new BankAccountInfoVO();
            // 使用 Optional 避免空指针异常
            bankAccountInfoVO.setAccentity(Optional.ofNullable(map.get("accentity")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setAccentity_name(Optional.ofNullable(map.get("accentity_name")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setAccentity_code(Optional.ofNullable(map.get("accentity_code")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setCurrency(Optional.ofNullable(map.get("currency")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setCurrency_name(Optional.ofNullable(map.get("currency_name")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setCurrency_moneyDigit(Optional.ofNullable(map.get("currency_moneyDigit")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setReconciliationScheme(Optional.ofNullable(map.get("reconciliationScheme")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setReconciliationScheme_name(Optional.ofNullable(map.get("reconciliationScheme_name")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBankaccount(Optional.ofNullable(map.get("bankaccount")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBankaccount_name(Optional.ofNullable(map.get("bankaccount_name")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBankaccount_account(Optional.ofNullable(map.get("bankaccount_account")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBanktype(Optional.ofNullable(map.get("banktype")).map(Object::toString).orElse(null));
            bankAccountInfoVO.setBanktype_name(Optional.ofNullable(map.get("banktype_name")).map(Object::toString).orElse(null));
            bankAccountInfoVOList.add(bankAccountInfoVO);
        }

        return bankAccountInfoVOList;
    }

    @Override
    public List<Journal> getVoucherByBankAccountInfo(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception {
        List<LinkedHashMap> journalMaps = new ArrayList<>();
        CtmJSONObject argsJson = new CtmJSONObject();
        //分页信息
        CtmJSONObject pageInfo = new CtmJSONObject();
        pageInfo.put("pageIndex",0);
        pageInfo.put("pageSize",1000);
        argsJson.put("page",pageInfo);

        //过滤条件
        CtmJSONObject conditionJson = new CtmJSONObject();
        CtmJSONArray ctmJSONArray = new CtmJSONArray();
        //业务日期
        LinkedHashMap<String,String> makeTimeMap = new LinkedHashMap<>();
        makeTimeMap.put("itemName","makeTime");
        if (bankVoucherInfoQueryVO.getBusinessStartDate() != null){
            makeTimeMap.put("value1",bankVoucherInfoQueryVO.getBusinessStartDate());
        }
        makeTimeMap.put("value2", bankVoucherInfoQueryVO.getCheckEndDate());
        ctmJSONArray.add(makeTimeMap);

        //会计主体
        LinkedHashMap<String,String> accentityMap = new LinkedHashMap<>();
        accentityMap.put("itemName","accentity");
        accentityMap.put("value1",bankVoucherInfoQueryVO.getAccentity());
        ctmJSONArray.add(accentityMap);

        //银行账户
        LinkedHashMap<String,String> bankaccountMap = new LinkedHashMap<>();
        bankaccountMap.put("itemName","bankaccount");
        bankaccountMap.put("value1",bankVoucherInfoQueryVO.getBankaccount());
        ctmJSONArray.add(bankaccountMap);

        //币种
        LinkedHashMap<String,String> currencyMap = new LinkedHashMap<>();
        currencyMap.put("itemName","currency");
        currencyMap.put("value1",bankVoucherInfoQueryVO.getCurrency());
        ctmJSONArray.add(currencyMap);

        //是否勾对
        LinkedHashMap<String,String> checkflagMap = new LinkedHashMap<>();
        checkflagMap.put("itemName","checkflag");
        checkflagMap.put("value1","false");
        ctmJSONArray.add(checkflagMap);

        //是否封存
        LinkedHashMap<String,String> sealflagMap = new LinkedHashMap<>();
        sealflagMap.put("itemName","sealflag");
        sealflagMap.put("value1","false");
        ctmJSONArray.add(sealflagMap);

        //对账方案id
        LinkedHashMap<String,String> bankreconciliationschemeMap = new LinkedHashMap<>();
        bankreconciliationschemeMap.put("itemName","bankreconciliationscheme");
        bankreconciliationschemeMap.put("value1",bankVoucherInfoQueryVO.getReconciliationScheme());
        ctmJSONArray.add(bankreconciliationschemeMap);
        conditionJson.put("commonVOs",ctmJSONArray);
        conditionJson.put("reconciliationdatasourceid",bankVoucherInfoQueryVO.getReconciliationScheme());

        argsJson.put("condition",conditionJson);
        argsJson.put("billnum","cmp_check");

        //查询对账方案下使用组织的账簿
        PlanParam planParam = new PlanParam(null,null,bankVoucherInfoQueryVO.getReconciliationScheme());
        List<BankReconciliationSettingVO> infoList = cmpBankReconciliationSettingRpcService.findUseOrg(planParam);

        //按照账簿下查询
        Set<String> bookids = new HashSet<>();
        for (BankReconciliationSettingVO settingVO : infoList){
            //需要已启用，且银行账户+币种相同
            if (settingVO.getEnableStatus() == EnableStatus.Enabled.getValue()
                    && settingVO.getBankAccount().equals(bankVoucherInfoQueryVO.getBankaccount())
                    && settingVO.getCurrency().equals(bankVoucherInfoQueryVO.getCurrency())){
                bookids.add(settingVO.getAccBook());
            }
        }
        //账簿id集合回写，回写到bankVoucherInfoQueryVO中
        bankVoucherInfoQueryVO.setAccbookids(bookids);
        //调用总账 cash/list2接口查询凭证数据
        for(String accbookid : bookids){
            conditionJson.put("accbookId",accbookid);
            argsJson.put("condition",conditionJson);
            Pager page = bankBillSmartCheckService.reqVoucheList2(CtmJSONObject.toJSONString(argsJson));
            if(page!=null){
                Integer pageCount = page.getPageCount();
                journalMaps.addAll(page.getRecordList());
                if(pageCount!=1){
                    for (int i=2;i<=pageCount;i++){
                        pageInfo.put("pageIndex",i);
                        argsJson.put("page",pageInfo);
                        page = bankBillSmartCheckService.reqVoucheList2(CtmJSONObject.toJSONString(argsJson));
                        if(page!=null){
                            journalMaps.addAll(page.getRecordList());
                        }
                    }
                }
            }
        }

        //封装成现金的日记账格式数据
        List<Journal> journals = new ArrayList<>();
        for (LinkedHashMap j:journalMaps){
            Journal journal = new Journal();
            journal.init(j);
            if (j.get("bankverifycode") != null){
                journal.setBankcheckno(j.get("bankverifycode").toString());
            }
            journals.add(journal);
        }
        return journals;
    }

    @Override
    public List<Journal> getJournalByBankAccountInfo(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception {
        List<QueryConditionGroup> groupList = new ArrayList<>();
        QueryConditionGroup bankAndCurrency = QueryConditionGroup.and(
                QueryCondition.name("bankaccount").eq(bankVoucherInfoQueryVO.getBankaccount()),
                QueryCondition.name("currency").eq(bankVoucherInfoQueryVO.getCurrency())
        );
        groupList.add(bankAndCurrency);
        QueryConditionGroup bankGroup = QueryConditionGroup.or(groupList.toArray(new ConditionExpression[0]));

        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("checkflag").eq(0),//勾兑标识，未勾兑
                QueryCondition.name("billtype").not_eq(EventType.ExchangeBill.getValue()),//不查汇兑损益生成的日记账
                QueryCondition.name(ICmpConstant.SETTLE_STATUS).eq(SettleStatus.alreadySettled.getValue()) //已结算的银行日记账
        );
        group.appendCondition(bankGroup);
        //对账方案数据
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,bankVoucherInfoQueryVO.getReconciliationScheme());
        if (bankReconciliationSetting == null){
            return new ArrayList<>();
        }
        //非期初数据，限定日期大于启用日期
        QueryConditionGroup group1 = QueryConditionGroup.and(
                QueryCondition.name("initflag").eq(0),
                QueryCondition.name("dzdate").egt(bankReconciliationSetting.getEnableDate()),
                QueryCondition.name("dzdate").elt(bankVoucherInfoQueryVO.getCheckEndDate())
        );
        group.appendCondition(QueryCondition.name("dzdate").elt(bankVoucherInfoQueryVO.getCheckEndDate()));
        if (bankVoucherInfoQueryVO.getBusinessStartDate() != null ){
            group1.appendCondition(QueryCondition.name("dzdate").egt(bankVoucherInfoQueryVO.getBusinessStartDate()));
            group.appendCondition(QueryCondition.name("dzdate").egt(bankVoucherInfoQueryVO.getBusinessStartDate()));
        }
        //期初数据
        QueryConditionGroup group2 = QueryConditionGroup.and(
                QueryCondition.name("initflag").eq(1),
                QueryCondition.name("bankreconciliationscheme").eq(bankReconciliationSetting.getId())
        );
        PlanParam planParam = new PlanParam(null,null,bankReconciliationSetting.getString("id"));
        //现金适配资金组织用cmpCheckService去查询;银行日记账适配账户共享时，要传递对应的账户使用组织
        List<BankReconciliationSettingVO> infoList = cmpCheckService.findUseOrg(planParam);
        Set<String> orgids = new HashSet<>();
        for (BankReconciliationSettingVO settingVO : infoList){
            if(settingVO.getEnableStatus() == EnableStatus.Enabled.getValue()
                    && bankVoucherInfoQueryVO.getBankaccount().equals(settingVO.getBankAccount())
                    && bankVoucherInfoQueryVO.getCurrency().equals(settingVO.getCurrency())){
                orgids.add(settingVO.getUseOrg());
            }
        }
        if (!orgids.isEmpty()) {
            if (orgids.size() == 1) {
                group.appendCondition(QueryCondition.name("accentity").eq(orgids.iterator().next()));
            } else {
                group.appendCondition(QueryCondition.name("accentity").in(orgids));
            }
        }
        group.appendCondition(QueryConditionGroup.or(group1,group2));
        querySchema.addCondition(group);
        return MetaDaoHelper.queryObject(Journal.ENTITY_NAME, querySchema, null);
    }

    @Override
    public List<BankReconciliation> getBankReconciliationByBankAccountInfo(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception {
        List<QueryConditionGroup> groupList = new ArrayList<>();
        QueryConditionGroup bankAndCurrency = QueryConditionGroup.and(
                QueryCondition.name("bankaccount").eq(bankVoucherInfoQueryVO.getBankaccount()),
                QueryCondition.name("currency").eq(bankVoucherInfoQueryVO.getCurrency())
        );
        groupList.add(bankAndCurrency);
        QueryConditionGroup bankGroup = QueryConditionGroup.or(groupList.toArray(new ConditionExpression[0]));

        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(bankGroup);
        //日记账和凭证分别设置勾对状态
        if (bankVoucherInfoQueryVO.getReconciliationDataSource() == ReconciliationDataSource.Voucher.getValue()){
        group.appendCondition(QueryCondition.name("other_checkflag").eq(0)); //总账未勾兑
        }else {
            group.appendCondition(QueryCondition.name("checkflag").eq(0)); //日记账未勾对
        }

        //增加无需处理标识
        if (!BankreconciliationUtils.isNoProcess(bankVoucherInfoQueryVO.getAccentity())) {
            //组织参数 无需处理的流水，是否参与银企对账、银行账户余额弥 为否，需要把无需处理的银行流水过滤掉
            QueryConditionGroup g1 = QueryConditionGroup.and(QueryCondition.name("serialdealtype").not_eq(5));
            QueryConditionGroup g2 = QueryConditionGroup.and(QueryCondition.name("serialdealtype").is_null());
            group.appendCondition(QueryConditionGroup.or(g1,g2));
        }

        //对账方案数据
        BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,bankVoucherInfoQueryVO.getReconciliationScheme());
        if (bankReconciliationSetting == null){
            return new ArrayList<>();
        }
        //非期初数据，限定日期大于启用日期
        QueryConditionGroup group1 = QueryConditionGroup.and(
                QueryCondition.name("initflag").eq(0),
                QueryCondition.name("dzdate").egt(bankReconciliationSetting.getEnableDate()),
                QueryCondition.name("dzdate").elt(bankVoucherInfoQueryVO.getCheckEndDate())
        );
        group.appendCondition(QueryCondition.name("dzdate").elt(bankVoucherInfoQueryVO.getCheckEndDate()));
        if (bankVoucherInfoQueryVO.getTranStartDate() != null ){
            group1.appendCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").egt(bankVoucherInfoQueryVO.getTranStartDate())));
            group.appendCondition(QueryCondition.name("dzdate").egt(bankVoucherInfoQueryVO.getTranStartDate()));
        }
        //期初数据
        QueryConditionGroup group2 = QueryConditionGroup.and(
                QueryCondition.name("initflag").eq(1),
                QueryCondition.name("bankreconciliationscheme").eq(bankReconciliationSetting.getId())
        );

        group.appendCondition(QueryConditionGroup.or(group1,group2));
        querySchema.addCondition(group);
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        return bankReconciliationList;
        // 流水正在处理中的需要过滤
//        return bankreconciliationService.filterBankReconciliationByLockKey(bankReconciliationList, BankReconciliationActions.AutoReconciliation);
    }

    @Override
    public BalanceAdjustResult getBalanceStatus(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception {
        //根据查询条件的对账截止日与系统日期比较，取较早的日期作为查询余额调节表的日期，判断其日期是否已经生成余额调节表；
        SimpleDateFormat dateFormatWithoutTime = new SimpleDateFormat("yyyy-MM-dd");
        Date checkEndDate = dateFormatWithoutTime.parse(bankVoucherInfoQueryVO.getCheckEndDate());
        if (checkEndDate ==null || checkEndDate.compareTo(new Date()) > 0){
            checkEndDate = new Date();
        }
        //查询该对账方案下+银行账户+截止日期是否存在余额调节表数据
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("currency").eq(bankVoucherInfoQueryVO.getCurrency()),//币种
                QueryCondition.name("bankaccount").eq(bankVoucherInfoQueryVO.getBankaccount()),//银行账号
                QueryCondition.name("bankreconciliationscheme").eq(bankVoucherInfoQueryVO.getReconciliationScheme()),//对账方案id
                QueryCondition.name("dzdate").eq(dateFormatWithoutTime.format(checkEndDate))
        );
        querySchema.addCondition(group);
        List<BalanceAdjustResult> checkList =  MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, querySchema, null);
        //为空则未生成余额调节表
        if(CollectionUtils.isEmpty(checkList)){
            return null;
        }else {
            return checkList.get(0);
        }
    }

    @Override
    public CtmJSONObject quickReconciliation(CtmJSONObject params) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //后台对账批次号
        String reconciliationSeqNo = params.getString("reconciliationSeqNo");
        if (StringUtils.isEmpty(reconciliationSeqNo)){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EAFFB6E05980003", "对账批次号reconciliationSeqNo不能为空") /* "对账批次号reconciliationSeqNo不能为空" */);
        }
        //初始化快速对账查询银行账户信息参数
        BankAccountInfoQueryVO bankAccountInfoQueryVO = initQuickReconciliationParams(params);
        //目前只有凭证对账设置了快速对账
        bankAccountInfoQueryVO.setReconciliationDataSource(ReconciliationDataSource.Voucher.getValue());
        //查询银企对账设置中的 账户+币种+对账组织等信息
        List<BankAccountInfoVO> bankAccountInfoVOList = this.queryBankAccountInfo(bankAccountInfoQueryVO);

        //封装给前端进度条的信息
        ConcurrentHashMap<String, ReconciliationProcessItem> taskProgressMap = new ConcurrentHashMap<>();
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger totalCount = new AtomicInteger(bankAccountInfoVOList.size());
        String checkStartDate = DateUtils.parseDateToStr(new Date(),"yyyy-MM-dd HH:mm:ss");
        //先初始化统计数据
        String businessStartDate = StringUtils.isNotEmpty(bankAccountInfoQueryVO.getBusinessStartDate()) ? DateUtils.dateFormatWithoutTime(bankAccountInfoQueryVO.getBusinessStartDate()) : null ;
        String tranStartDate = StringUtils.isNotEmpty(bankAccountInfoQueryVO.getTranStartDate()) ? DateUtils.dateFormatWithoutTime(bankAccountInfoQueryVO.getTranStartDate()) : null ;
        for (BankAccountInfoVO bankAccountInfoVO : bankAccountInfoVOList){
            initProcessItem(taskProgressMap,bankAccountInfoVO,DateUtils.dateFormatWithoutTime(bankAccountInfoQueryVO.getCheckEndDate()),businessStartDate,tranStartDate);
        }
        redisTemplate.opsForValue().set(reconciliationSeqNo, taskProgressMap);
        //todo 大数量对账，待优化；线程池；加锁实现
        ExecutorService taskExecutor = null;
        try {
            taskExecutor  = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"bankautocheck-quickReconciliation-threadpool");
            taskExecutor.submit(() -> {
                try {
                    ThreadPoolUtil.executeByBatch(executorService, bankAccountInfoVOList, 1, "银企对账工作台-快速对账流程", (int fromIndex, int toIndex) -> {//@notranslate
                        String builder = "";
                        for (int t = fromIndex; t < toIndex; t++) {
                            BankAccountInfoVO bankAccountInfoVO = bankAccountInfoVOList.get(t);
                            //统计step1:获取进度条统计参数
                            ReconciliationProcessItem progressItem = taskProgressMap.get(bankAccountInfoVO.getAccentity());
                            progressItem.setCheckStartDate(checkStartDate);
                            try {
                                balanceBatchCommonService.executeInOneServiceLock((int lockStatus)->{
                                    //获取锁失败抛出异常
                                    if (lockStatus == 0){
                                        throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EAFFB6E05980005", "当前银行账户：%s,币种：%s 正在对账，不能发起本次对账，请执行完成后再操作") /* "当前银行账户：%s,币种：%s 正在对账，不能发起本次对账，请执行完成后再操作" */,bankAccountInfoVO.getBankaccount_name(),bankAccountInfoVO.getCurrency_name()));
                                    }
                                    BankVoucherInfoQueryVO bankVoucherInfoQueryVO = initBillVoucherInfoQueryVO(bankAccountInfoVO, bankAccountInfoQueryVO);
                                    // 查询未勾对的的凭证数据
                                    List<Journal> voucherList = this.getVoucherByBankAccountInfo(bankVoucherInfoQueryVO);
                                    // 查询未勾对的银行流水数据
                                    List<BankReconciliation> bankReconciliationList = this.getBankReconciliationByBankAccountInfo(bankVoucherInfoQueryVO);
                                    // 自动对账优化，查询对应自动对账设置
                                    BankAutoCheckConfig bankAutoCheckConfig = bankAutoCheckConfigService.queryConfigInfo(null);
                                    if (bankAutoCheckConfig == null){ //自动对账设置为空时，默认按关键要素匹配
                                        bankAutoCheckConfig = new BankAutoCheckConfig();
                                        bankAutoCheckConfig.setKeyElementMatchFlag((short)1);
                                    }
                                    // 调用自动对账接口完成对账
                                    CtmJSONObject ctmJSONObject= bankBillSmartCheckService.handleAutoCheckAutomaticRules(bankReconciliationList, voucherList, Long.parseLong(bankVoucherInfoQueryVO.getReconciliationScheme()), 1,bankAutoCheckConfig);
                                    //统计当前勾对成功笔数
                                    BigDecimal vouchCheckedNum = ctmJSONObject.getBigDecimal("vouchCheckedNum");
                                    BigDecimal bankCheckedNum = ctmJSONObject.getBigDecimal("bankCheckedNum");
                                    progressItem.setVouchCheckedNum(BigDecimalUtils.safeAdd(progressItem.getVouchCheckedNum(), vouchCheckedNum));
                                    progressItem.setBankCheckedNum(BigDecimalUtils.safeAdd(progressItem.getBankCheckedNum(), bankCheckedNum));
                                    //对符状态
                                    progressItem.setReconciliationStatus(ctmJSONObject.getInteger("reconciliationStatus"));
                                    //统计step2 设置当前银行账户任务为已完成
                                    handleCompleteProcessItem(progressItem,bankAccountInfoVO,bankAccountInfoVOList,bankVoucherInfoQueryVO.getAccbookids());
                                    redisTemplate.opsForValue().set(reconciliationSeqNo, taskProgressMap);
                                }).apply(bankAccountInfoVO.getBankaccount(), bankAccountInfoVO.getCurrency());

                            } catch (Exception e) {
                                log.error("Error during quick reconciliation for bankAccountInfoVO: {}", bankAccountInfoVO, e);
                                progressItem.setStatus(3); // 3: error
                                progressItem.setErrmsg(e.getMessage());
                                //记录明细异常
                                Iterator<String> iterator = progressItem.getDetails().iterator();
                                CtmJSONObject removeDetail = null;
                                //20260115,适配自动对账结果展示改动,修改成账户+币种维度
                                String detailId= bankAccountInfoVO.getBankaccount() + "#" +bankAccountInfoVO.getCurrency();
                                while (iterator.hasNext()) {
                                    String detail = iterator.next();
                                    CtmJSONObject temp = CtmJSONObject.parseObject(detail);
                                    if (detailId.equals(temp.getString("id"))) {
                                        removeDetail = temp;
                                        iterator.remove(); // 使用迭代器的remove方法移除元素
                                    }
                                    if (removeDetail != null){
                                        removeDetail.put("status",3);//3，对账异常
                                        removeDetail.put("errorMsg",e.getMessage());
                                        progressItem.getDetails().add(removeDetail.toString());
                                    }
                                }
                                redisTemplate.opsForValue().set(reconciliationSeqNo, taskProgressMap);
                            } finally {
                                completedCount.incrementAndGet();
                                //记录对账日志
                                CtmJSONObject logParams = new CtmJSONObject();
                                logParams.put("reconciliationSeqNo",reconciliationSeqNo);
                                logParams.put("bankaccountinfo",bankAccountInfoVO);
                                logParams.put("checkResult",progressItem);
                                ctmcmpBusinessLogService.saveBusinessLog(logParams, reconciliationSeqNo, "银企对账工作台-快速对账", IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH, "银企对账工作台", "按银行账户+币种分组对账完成");//@notranslate
                            }
                        }
                        redisTemplate.opsForValue().set(reconciliationSeqNo, taskProgressMap);
                        return builder;
                    }, false);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }finally {
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }

        result.put("code", 200);
        result.put("reconciliationSeqNo",reconciliationSeqNo);
        result.put("accentityCount",bankAccountInfoVOList.size());
        result.put("msg", "快速对账任务已启动，reconciliationSeqNo: " + reconciliationSeqNo + "任务总数量：" + totalCount);//@notranslate
        return result;
    }

    @Override
    public CtmJSONObject autoReconciliation(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //后台对账批次号
        String reconciliationSeqNo = param.getString("reconciliationSeqNo");
        if (StringUtils.isEmpty(reconciliationSeqNo)){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EAFFB6E05980003", "对账批次号reconciliationSeqNo不能为空") /* "对账批次号reconciliationSeqNo不能为空" */);
        }
        //前端参数转换
        List<Map<String, String>> infoList = convertJsonArrayToListOfMaps(param);

        //默认凭证数据源；20250513新增银行日记账数据源逻辑
        short reconciliationDataSource = ReconciliationDataSource.Voucher.getValue();
        if ("2".equals(param.getString("reconciliationDataSource"))){
            reconciliationDataSource = ReconciliationDataSource.BankJournal.getValue();
        }

        //封装给前端进度条的信息
        ConcurrentHashMap<String, ReconciliationProcessItem> taskProgressMap = new ConcurrentHashMap<>();
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger totalCount = new AtomicInteger(infoList.size());
        String checkStartDate = DateUtils.parseDateToStr(new Date(),"yyyy-MM-dd HH:mm:ss");
        //先初始化统计数据
        for (Map<String, String> tempProcess : infoList){
            List<Map<String, String>> tempList = new ArrayList<>();
            tempList.add(tempProcess);
            BankAccountInfoVO bankAccountInfoVO = initBankAccountInfo(tempList).get(0);
            String businessDate = tempProcess.get("business_date") != null ? tempProcess.get("business_date") : null;
            String tranDate = tempProcess.get("tran_date") != null ? tempProcess.get("tran_date") : null;
            initProcessItem(taskProgressMap,bankAccountInfoVO,tempProcess.get("check_end_date") + "",businessDate,tranDate);
        }
        redisTemplate.opsForValue().set(reconciliationSeqNo, taskProgressMap);
        //启用线程池
        ExecutorService taskExecutor = null;
        short finalReconciliationDataSource = reconciliationDataSource;
        try {
            taskExecutor  = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"bankautocheck-autoReconciliation-threadpool");
            taskExecutor.submit(() -> {
                try {
                    //batchcount 必须为1，避免异常影响其他线程
                    ThreadPoolUtil.executeByBatch(executorService, infoList, 1, "银企对账工作台-自动对账流程", (int fromIndex, int toIndex) -> {//@notranslate
                        String builder = "";
                        for (int t = fromIndex; t < toIndex; t++) {
                            Map<String, String> bankvourchercheckWorkbench = infoList.get(t);
                            List<Map<String, String>> tempList = new ArrayList<>();
                            tempList.add(bankvourchercheckWorkbench);
                            BankAccountInfoVO bankAccountInfoVO = initBankAccountInfo(tempList).get(0);
                            //统计step1:获取进度条统计参数
                            ReconciliationProcessItem progressItem = taskProgressMap.get(bankAccountInfoVO.getAccentity());
                            progressItem.setCheckStartDate(checkStartDate);
                            try {
                                balanceBatchCommonService.executeInOneServiceLock((int lockStatus)->{
                                    //获取锁失败抛出异常
                                    if (lockStatus == 0){
                                        throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EAFFB6E05980005", "当前银行账户：%s,币种：%s 正在对账，不能发起本次对账，请执行完成后再操作") /* "当前银行账户：%s,币种：%s 正在对账，不能发起本次对账，请执行完成后再操作" */,bankAccountInfoVO.getBankaccount_name(),bankAccountInfoVO.getCurrency_name()));
                                    }
                                    BankVoucherInfoQueryVO bankVoucherInfoQueryVO = initBillVoucherInfoQueryVO(bankAccountInfoVO, bankvourchercheckWorkbench);
                                    bankVoucherInfoQueryVO.setReconciliationDataSource(finalReconciliationDataSource);
                                    //查询关联的凭证数据,银行日记账数据
                                    List<Journal> voucherList = new ArrayList<>();
                                    if (finalReconciliationDataSource == ReconciliationDataSource.Voucher.getValue()){
                                        voucherList = this.getVoucherByBankAccountInfo(bankVoucherInfoQueryVO);
                                    }
                                    if (finalReconciliationDataSource == ReconciliationDataSource.BankJournal.getValue()){
                                        voucherList = this.getJournalByBankAccountInfo(bankVoucherInfoQueryVO);
                                    }
                                    // 查询未勾对的银行流水数据
                                    List<BankReconciliation> bankReconciliationList = this.getBankReconciliationByBankAccountInfo(bankVoucherInfoQueryVO);
                                    // 自动对账优化，查询对应自动对账设置
                                    BankAutoCheckConfig bankAutoCheckConfig = bankAutoCheckConfigService.queryConfigInfo(null);
                                    if (bankAutoCheckConfig == null){ //自动对账设置为空时，默认按关键要素匹配
                                        bankAutoCheckConfig = new BankAutoCheckConfig();
                                        bankAutoCheckConfig.setKeyElementMatchFlag((short)1);
                                    }
                                    // 调用自动对账接口完成对账
                                    CtmJSONObject ctmJSONObject = bankBillSmartCheckService.handleAutoCheckAutomaticRules(bankReconciliationList, voucherList, Long.parseLong(bankVoucherInfoQueryVO.getReconciliationScheme()), Integer.valueOf(finalReconciliationDataSource + ""), bankAutoCheckConfig);
                                    //统计当前勾对成功笔数
                                    BigDecimal vouchCheckedNum = ctmJSONObject.getBigDecimal("vouchCheckedNum");
                                    BigDecimal bankCheckedNum = ctmJSONObject.getBigDecimal("bankCheckedNum");
                                    progressItem.setVouchCheckedNum(BigDecimalUtils.safeAdd(progressItem.getVouchCheckedNum(), vouchCheckedNum));
                                    progressItem.setBankCheckedNum(BigDecimalUtils.safeAdd(progressItem.getBankCheckedNum(), bankCheckedNum));
                                    //账户对符状态
                                    progressItem.setReconciliationStatus(ctmJSONObject.getInteger("reconciliationStatus"));
                                    //统计step2 设置当前银行账户任务为已完成
                                    handleCompleteProcessItem(progressItem,bankAccountInfoVO,initBankAccountInfo(infoList),bankVoucherInfoQueryVO.getAccbookids());
                                    redisTemplate.opsForValue().set(reconciliationSeqNo, taskProgressMap);
                                }).apply(bankAccountInfoVO.getBankaccount(), bankAccountInfoVO.getCurrency());

                            } catch (Exception e) {
                                log.error("Error during quick reconciliation for bankAccountInfoVO: {}", bankAccountInfoVO, e);
                                progressItem.setStatus(3); // 3: error
                                progressItem.setErrmsg(e.getMessage());
                                //记录明细异常
                                Iterator<String> iterator = progressItem.getDetails().iterator();
                                CtmJSONObject removeDetail = null;
                                //20260115,适配自动对账结果展示改动,修改成账户+币种维度
                                String detailId= bankAccountInfoVO.getBankaccount() + "#" +bankAccountInfoVO.getCurrency();
                                while (iterator.hasNext()) {
                                    String detail = iterator.next();
                                    CtmJSONObject temp = CtmJSONObject.parseObject(detail);
                                    if (detailId.equals(temp.getString("id"))) {
                                        removeDetail = temp;
                                        iterator.remove(); // 使用迭代器的remove方法移除元素
                                    }
                                    if (removeDetail != null){
                                        removeDetail.put("status",3);//3，对账异常
                                        removeDetail.put("errorMsg",e.getMessage());
                                        progressItem.getDetails().add(removeDetail.toString());
                                    }
                                }
                                redisTemplate.opsForValue().set(reconciliationSeqNo, taskProgressMap);
                            } finally {
                                completedCount.incrementAndGet();
                                //记录对账日志
                                CtmJSONObject logParams = new CtmJSONObject();
                                logParams.put("reconciliationSeqNo",reconciliationSeqNo);
                                logParams.put("bankaccountinfo",bankAccountInfoVO);
                                logParams.put("checkResult",progressItem);
                                ctmcmpBusinessLogService.saveBusinessLog(logParams,reconciliationSeqNo, "银企对账工作台-自动对账", IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH, "银企对账工作台", "按银行账户+币种分组对账完成");//@notranslate
                            }
                        }
                        redisTemplate.opsForValue().set(reconciliationSeqNo, taskProgressMap);
                        return builder;
                    }, false);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }finally {
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }

        result.put("code", 200);
        result.put("reconciliationSeqNo",reconciliationSeqNo);
        result.put("accentityCount",infoList.size());
        result.put("msg", "自动对账任务已启动，reconciliationSeqNo: " + reconciliationSeqNo + "任务总数量：" + totalCount);//@notranslate
        return result;
    }

    private HashMap getProcess(String reconciliationSeqNo){
        String cacheString = (String) redisTemplate.opsForValue().get(reconciliationSeqNo);
        HashMap<String, ReconciliationProcessItem> taskProgressMap = CtmJSONObject.parseObject(cacheString,HashMap.class);
        return taskProgressMap;
    }

    @Override
    public CtmJSONObject getReconciliationProgress(String reconciliationSeqNo) {
        CtmJSONObject result = new CtmJSONObject();
        if (redisTemplate.opsForValue().get(reconciliationSeqNo) == null) {
            result.put("code", 404);
            result.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EAFFB6E05980004", "任务未找到") /* "任务未找到" */);
            return result;
        }
        ConcurrentHashMap<String, ReconciliationProcessItem> taskProgressMap = (ConcurrentHashMap<String, ReconciliationProcessItem>) redisTemplate.opsForValue().get(reconciliationSeqNo);

        List<ReconciliationProcessItem> progressList = new ArrayList<>(taskProgressMap.values());
        Set<String> uniqueBankAccounts = new HashSet<>();
        BigDecimal totalVouchCheckedNum = BigDecimal.ZERO;
        BigDecimal totalBankCheckedNum = BigDecimal.ZERO;
        boolean isContainError = false;
        for (ReconciliationProcessItem item : progressList) {
            uniqueBankAccounts.addAll(item.getBankaccounList());
            totalVouchCheckedNum = BigDecimalUtils.safeAdd(totalVouchCheckedNum, item.getVouchCheckedNum());
            totalBankCheckedNum = BigDecimalUtils.safeAdd(totalBankCheckedNum, item.getBankCheckedNum());
            if (item.getStatus() == 3){
                isContainError = true;
            }
        }

        result.put("code", 200);
        result.put("msg", "success");
        result.put("isContainError",isContainError);
        result.put("accentityCount", progressList.stream().map(ReconciliationProcessItem::getTitle).distinct().count());
        result.put("bankCount", uniqueBankAccounts.size()); // 去重后的银行账户数量
        result.put("date", progressList.get(0) != null ? progressList.get(0).getDate() : ""); // 对账截止日期，
        result.put("checkStartDate", progressList.get(0) != null ? progressList.get(0).getCheckStartDate() : "");//对账开始时间
        result.put("totalVouchCheckedNum", totalVouchCheckedNum);
        result.put("totalBankCheckedNum", totalBankCheckedNum);
        Collections.sort(progressList, (o1, o2) -> o1.getAccentity_code().compareTo(o2.getAccentity_code()));
        // 对 progressList 中每个元素的 details 进行排序
        for (ReconciliationProcessItem item : progressList) {
            // 如果 item.getDetails() 返回 Set 类型
            Set<String> detailsSet = item.getDetails();
            List<String> detailsList = new ArrayList<>(detailsSet);
            Collections.sort(detailsList, (detail1, detail2) -> {
                CtmJSONObject json1 = CtmJSONObject.parseObject(detail1);
                CtmJSONObject json2 = CtmJSONObject.parseObject(detail2);
                String bankAccountAccount1 = json1.getString("bankaccount_account");
                String bankAccountAccount2 = json2.getString("bankaccount_account");
                // 处理可能的 null 值情况
                if (bankAccountAccount1 == null && bankAccountAccount2 == null) {
                    return 0;
                }
                if (bankAccountAccount1 == null) {
                    return 1;
                }
                if (bankAccountAccount2 == null) {
                    return -1;
                }
                return bankAccountAccount1.compareTo(bankAccountAccount2);
            });
            // 使用 LinkedHashSet 保持顺序
            Set<String> orderedDetailsSet = new LinkedHashSet<>(detailsList);
            item.setDetails(orderedDetailsSet);
        }
        result.put("progress", progressList);

        return result;
    }

    @Override
    public List<ReconciliationMatchRecord> queryReconciliationMatchRecord(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup mainGroup = QueryConditionGroup.and(
                QueryCondition.name("bankaccount").eq(bankVoucherInfoQueryVO.getBankaccount()),
                QueryCondition.name("currency").eq(bankVoucherInfoQueryVO.getCurrency()),
                QueryCondition.name("accentity").eq(bankVoucherInfoQueryVO.getAccentity()),
                QueryCondition.name("reconciliationScheme").eq(bankVoucherInfoQueryVO.getReconciliationScheme())
        );
        QueryConditionGroup journalGroup = QueryConditionGroup.and(QueryCondition.name("accountingDate").elt(bankVoucherInfoQueryVO.getCheckEndDate()));
        QueryConditionGroup bankGroup = QueryConditionGroup.and(QueryCondition.name("tranDate").elt(bankVoucherInfoQueryVO.getCheckEndDate()));
        if (bankVoucherInfoQueryVO.getTranStartDate() != null ){
            journalGroup.appendCondition(QueryConditionGroup.and(QueryCondition.name("accountingDate").egt(bankVoucherInfoQueryVO.getTranStartDate())));
            bankGroup.appendCondition(QueryCondition.name("tranDate").egt(bankVoucherInfoQueryVO.getTranStartDate()));
        }
        //日记账和凭证分别设置数据源
        if (bankVoucherInfoQueryVO.getReconciliationDataSource() == ReconciliationDataSource.Voucher.getValue()){
            journalGroup.appendCondition(QueryCondition.name("dataSource").eq(ReconciliationMatchDataSource.Voucher.getValue())); //总账凭证
        }else {
            journalGroup.appendCondition(QueryCondition.name("dataSource").eq(ReconciliationMatchDataSource.Journal.getValue())); //日记账
        }
        bankGroup.appendCondition(QueryCondition.name("dataSource").eq(ReconciliationMatchDataSource.BankReconciliation.getValue()));
        mainGroup.appendCondition(QueryConditionGroup.or(journalGroup,bankGroup));
        querySchema.addCondition(mainGroup);
        return MetaDaoHelper.queryObject(ReconciliationMatchRecord.ENTITY_NAME, querySchema, null);
    }

    private List<Map<String, String>> convertJsonArrayToListOfMaps(CtmJSONObject param) {
        List<Map<String, String>> listOfMaps = new ArrayList<>();
        // 获取 JSON 数组
        CtmJSONArray dataArray = param.getJSONArray("data");
        if (dataArray != null) {
            // 遍历 JSON 数组
            for (int i = 0; i < dataArray.size(); i++) {
                CtmJSONObject jsonObject = dataArray.getJSONObject(i);
                Map<String, String> map = new HashMap<>();

                // 遍历 JSON 对象的键值对
                for (String key : jsonObject.keySet()) {
                    Object value = jsonObject.get(key);
                    if (value != null) {
                        map.put(key, value.toString());
                    } else {
                        map.put(key, null); // 或者你可以选择跳过空值
                    }
                }

                listOfMaps.add(map);
            }
        }

        return listOfMaps;
    }

    /**
     * 初始化快速对账查询银行账户信息参数
     * @param params 前端接口json参数
     * @return 查询参数
     * @throws Exception
     */
    private BankAccountInfoQueryVO initQuickReconciliationParams(CtmJSONObject params) throws Exception {
        BankAccountInfoQueryVO bankAccountInfoQueryVO = new BankAccountInfoQueryVO();
        //对账组织
        List<String> accentityList = new ArrayList<>();
        String accentity = params.getString("accentityList");
        if (ObjectUtils.isNotEmpty(accentity)) {
            accentityList = params.getJSONArray("accentityList").toJavaList(String.class);
        }else {
            //为空则取全部的对账组织
            accentityList = getReconciliationSchemeAccentityList(ReconciliationDataSource.Voucher.getValue());
        }
        bankAccountInfoQueryVO.setAccentityList(accentityList);
        //对账截止日期
        String checkEndDate = params.getString("checkEndDate");
        if(ObjectUtils.isNotEmpty(checkEndDate)){
            bankAccountInfoQueryVO.setCheckEndDate(checkEndDate);
        }
        //银行账户
        List<String> bankaccountList = new ArrayList<>();
        String bankaccounts = params.getString("bankaccountList");
        if (ObjectUtils.isNotEmpty(bankaccounts)) {
            bankaccountList = JSONBuilderUtils.stringToBeanList(bankaccounts, String.class);
        }
        bankAccountInfoQueryVO.setBankAccountList(bankaccountList);
        //币种
        List<String> currencyList = new ArrayList<>();
        String currency = params.getString("currencyList");
        if (ObjectUtils.isNotEmpty(currency)) {
            currencyList = JSONBuilderUtils.stringToBeanList(currency, String.class);
        }
        bankAccountInfoQueryVO.setCurrencyList(currencyList);
        //银行类别
        List<String> banktypeList = new ArrayList<>();
        String banktype = params.getString("banktypeList");
        if (ObjectUtils.isNotEmpty(banktype)) {
            banktypeList = JSONBuilderUtils.stringToBeanList(banktype, String.class);
        }
        bankAccountInfoQueryVO.setBanktypeList(banktypeList);
        //对账方案
        List<String> reconciliationSchemeList = new ArrayList<>();
        String reconciliationScheme = params.getString("reconciliationSchemeList");
        if (ObjectUtils.isNotEmpty(reconciliationScheme)) {
            reconciliationSchemeList = JSONBuilderUtils.stringToBeanList(reconciliationScheme, String.class);
        }
        bankAccountInfoQueryVO.setReconciliationSchemeList(reconciliationSchemeList);
        //业务日期
        String businessStartDate = params.getString("businessDate");
        if(ObjectUtils.isNotEmpty(businessStartDate)){
            bankAccountInfoQueryVO.setBusinessStartDate(businessStartDate);
        }
        //交易日期
        String tranStartDate = params.getString("tranDate");
        if(ObjectUtils.isNotEmpty(tranStartDate)){
            bankAccountInfoQueryVO.setTranStartDate(tranStartDate);
        }

        return bankAccountInfoQueryVO;
    }

    /**
     * 前端传递信息拼装
     * @param infoList
     * @return
     */
    private List<BankAccountInfoVO> initBankAccountInfo(List<Map<String, String>> infoList){
        List<BankAccountInfoVO> bankAccountInfoVOList = new ArrayList<>();
        for (Map<String, String> bankvourchercheckWorkbench : infoList){
            BankAccountInfoVO bankAccountInfoVO = new BankAccountInfoVO();
            bankAccountInfoVO.setAccentity(bankvourchercheckWorkbench.get("accentity"));
            bankAccountInfoVO.setAccentity_name(bankvourchercheckWorkbench.get("accentity_name"));
            bankAccountInfoVO.setAccentity_code(bankvourchercheckWorkbench.get("accentity_code"));
            bankAccountInfoVO.setBankaccount(bankvourchercheckWorkbench.get("bankaccount"));
            bankAccountInfoVO.setBankaccount_name(bankvourchercheckWorkbench.get("bankaccount_name"));
            bankAccountInfoVO.setBankaccount_account(bankvourchercheckWorkbench.get("bankaccount_account"));
            bankAccountInfoVO.setCurrency(bankvourchercheckWorkbench.get("currency"));
            bankAccountInfoVO.setCurrency_name(bankvourchercheckWorkbench.get("currency_name"));
            bankAccountInfoVO.setReconciliationScheme(bankvourchercheckWorkbench.get("reconciliationScheme"));
            bankAccountInfoVO.setReconciliationScheme_name(bankvourchercheckWorkbench.get("reconciliationScheme_name"));
            bankAccountInfoVOList.add(bankAccountInfoVO);
        }
        return bankAccountInfoVOList;
    }

    /**
     * 初始化凭证和银行流水查询入参
     * @param bankAccountInfoVO 银行账户币种对账组织信息
     * @param infoQueryVO 前端页面查询过滤区参数
     * @return 凭证和银行流水查询入参
     */
    private BankVoucherInfoQueryVO initBillVoucherInfoQueryVO(BankAccountInfoVO bankAccountInfoVO,BankAccountInfoQueryVO infoQueryVO){
        BankVoucherInfoQueryVO bankVoucherInfoQueryVO = new BankVoucherInfoQueryVO();
        BeanPropertyCopyUtil.copyProperties(bankAccountInfoVO,bankVoucherInfoQueryVO);
        BeanPropertyCopyUtil.copyProperties(infoQueryVO,bankVoucherInfoQueryVO);
        return bankVoucherInfoQueryVO;
    }

    /**
     * 初始化凭证和银行流水查询入参
     * @param bankAccountInfoVO 银行账户币种对账组织信息
     * @param workbench 对账概览的行数据
     * @return 凭证和银行流水查询入参
     */
    private BankVoucherInfoQueryVO initBillVoucherInfoQueryVO(BankAccountInfoVO bankAccountInfoVO,Map<String, String> workbench){
        BankVoucherInfoQueryVO bankVoucherInfoQueryVO = new BankVoucherInfoQueryVO();
        BeanPropertyCopyUtil.copyProperties(bankAccountInfoVO,bankVoucherInfoQueryVO);
        bankVoucherInfoQueryVO.setCheckEndDate(workbench.get("check_end_date"));
        if (workbench.get("tran_date") != null){
            bankVoucherInfoQueryVO.setTranStartDate(workbench.get("tran_date"));
        }
        if (workbench.get("business_date") != null){
            bankVoucherInfoQueryVO.setBusinessStartDate(workbench.get("business_date"));
        }
        return bankVoucherInfoQueryVO;
    }

    /**
     * 设置对账进度条参数
     * @param bankAccountInfoVO 账户信息
     * @param checkEndDate 登账截止日期
     * @return 进度条参数
     */
    private ReconciliationProcessItem initProcessItem(ConcurrentHashMap<String, ReconciliationProcessItem> taskProgressMap,BankAccountInfoVO bankAccountInfoVO,String checkEndDate,String businessDate,String tranDate){
        String accentity = bankAccountInfoVO.getAccentity();
        //统计的值调整为会计主体
        String key = accentity;
        String title = bankAccountInfoVO.getAccentity_name(); // 假设使用对账组织名称作为标题
        //20260115,适配自动对账结果展示改动,修改成账户+币种维度
        String detailId= bankAccountInfoVO.getBankaccount() + "#" +bankAccountInfoVO.getCurrency();

        ReconciliationProcessItem progressItem = taskProgressMap.computeIfAbsent(key, k -> {
            ReconciliationProcessItem item = new ReconciliationProcessItem();
            item.setKey(key);
            item.setAccentity_code(bankAccountInfoVO.getAccentity_code());
            item.setTitle(title);
            item.setAccentity(accentity);
            item.setDate(checkEndDate);
            item.setStatus(1); // 1: 进行中
            item.setReconciliationStatus(1);//默认设置已对符
            CtmJSONObject detailJson = new CtmJSONObject();
            detailJson.put("id",detailId);
            detailJson.put("accentity",accentity);
            detailJson.put("accentity_name",bankAccountInfoVO.getAccentity_name());
            detailJson.put("bankaccount",bankAccountInfoVO.getBankaccount());
            detailJson.put("bankaccount_name",bankAccountInfoVO.getBankaccount_name());
            detailJson.put("bankaccount_account",bankAccountInfoVO.getBankaccount_account());
            detailJson.put("currency",bankAccountInfoVO.getCurrency());
            detailJson.put("currency_name",bankAccountInfoVO.getCurrency_name());
            detailJson.put("check_end_date",checkEndDate);
            if (StringUtils.isNotEmpty(businessDate)){
                detailJson.put("business_date",businessDate);
            }
            if (StringUtils.isNotEmpty(tranDate)){
                detailJson.put("tran_date",tranDate);
            }
            detailJson.put("reconciliationScheme",bankAccountInfoVO.getReconciliationScheme());
            detailJson.put("reconciliationScheme_name",bankAccountInfoVO.getReconciliationScheme_name());
            detailJson.put("status",1);//进行中
            detailJson.put("name",String.format("%s【%s】",bankAccountInfoVO.getBankaccount_name(),bankAccountInfoVO.getBankaccount_account()));//@notranslate
            // 添加银行账户名称到details
            item.getDetails().add(detailJson.toString());
            return item;
        });
        progressItem.getBankaccounList().add(detailId);
        progressItem.getBankaccoutInfoList().add(accentity + "#" + bankAccountInfoVO.getBankaccount() + "#" + bankAccountInfoVO.getCurrency());
        //判断是否已有该账户信息
        boolean isContain = false;
        for(String d : progressItem.getDetails()){
            CtmJSONObject c = CtmJSONObject.parseObject(d);
            if (c.getString("id").equals(detailId)){
                isContain = true;
            }
        }
        if (!isContain){
            CtmJSONObject detailJson = new CtmJSONObject();
            detailJson.put("id",detailId);
            detailJson.put("bankaccount",bankAccountInfoVO.getBankaccount());
            detailJson.put("bankaccount_name",bankAccountInfoVO.getBankaccount_name());
            detailJson.put("bankaccount_account",bankAccountInfoVO.getBankaccount_account());
            detailJson.put("accentity",accentity);
            detailJson.put("accentity_name",bankAccountInfoVO.getAccentity_name());
            detailJson.put("status",1);//进行中
            detailJson.put("currency",bankAccountInfoVO.getCurrency());
            detailJson.put("currency_name",bankAccountInfoVO.getCurrency_name());
            detailJson.put("check_end_date",checkEndDate);
            if (StringUtils.isNotEmpty(businessDate)){
                detailJson.put("business_date",businessDate);
            }
            if (StringUtils.isNotEmpty(tranDate)){
                detailJson.put("tran_date",tranDate);
            }
            detailJson.put("reconciliationScheme",bankAccountInfoVO.getReconciliationScheme());
            detailJson.put("reconciliationScheme_name",bankAccountInfoVO.getReconciliationScheme_name());
            detailJson.put("name",String.format("%s【%s】",bankAccountInfoVO.getBankaccount_name(),bankAccountInfoVO.getBankaccount_account()));//@notranslate
            // 添加银行账户名称到details
            progressItem.getDetails().add(detailJson.toString());
        }
        return progressItem;
    }

    /**
     * 更新当前账户进度为已完成
     * @param progressItem 进度信息
     * @param bankAccountInfoVO 账户信息
     * @param bankAccountInfoVOList 账户信息合集
     */
    private void handleCompleteProcessItem(ReconciliationProcessItem progressItem,BankAccountInfoVO bankAccountInfoVO,List<BankAccountInfoVO> bankAccountInfoVOList,Set<String> accbookids){
        try {
            synchronized (progressItem.getDetails()) {
                Iterator<String> iterator = progressItem.getDetails().iterator();
                CtmJSONObject removeDetail = null;
                //20260115,适配自动对账结果展示改动,修改成账户+币种维度
                String detailId= bankAccountInfoVO.getBankaccount() + "#" +bankAccountInfoVO.getCurrency();
                while (iterator.hasNext()) {
                    String detail = iterator.next();
                    CtmJSONObject temp = CtmJSONObject.parseObject(detail);
                    if (detailId.equals(temp.getString("id"))) {
                        removeDetail = temp;
                        iterator.remove(); // 使用迭代器的remove方法移除元素
                    }
                }
                if (removeDetail != null) {
                    removeDetail.put("status", 2); // 2：已完成
                    removeDetail.put("reconciliationStatus", progressItem.getReconciliationStatus());
                    if(accbookids != null && accbookids.size() > 0){
                        removeDetail.put("accbookids",accbookids);
                    }
                    progressItem.getDetails().add(removeDetail.toString());
                }
                // 检查是否所有银行账户和币种组合都已完成
                if (progressItem.getDetails().size() == bankAccountInfoVOList.stream()
                        .filter(vo -> vo.getAccentity().equals(bankAccountInfoVO.getAccentity()))
                        .map(vo -> vo.getBankaccount() + "#" + vo.getCurrency())  // 账户+币种组合
                        .distinct()
                        .count()) {
                    progressItem.setStatus(2); // 2: 已完成
                }
            }
        }catch (Exception e){
            log.error("更新当前账户对账进度数据异常",e);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054004D8", "更新当前账户对账进度数据异常：") /* "更新当前账户对账进度数据异常：" */ + e.getMessage());
        }
    }
}
