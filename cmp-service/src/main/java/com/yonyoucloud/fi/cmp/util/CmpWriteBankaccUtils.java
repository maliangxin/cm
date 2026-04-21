package com.yonyoucloud.fi.cmp.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyoucloud.fi.cmp.cmpentity.RpType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.bd.period.Period;
import com.yonyoucloud.fi.cmp.cmpentity.MoneyForm;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonServiceImpl;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IMultilangConstant;
import com.yonyoucloud.fi.cmp.enums.UpgradeSignEnum;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.initdata.service.InitDataService;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import lombok.NonNull;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName CmpWriteBankaccUtils
 * @Description 现金管理写账工具类
 * 本类用于业务单据登记日记账，删除日记账，同时修改企业方账面余额所用
 * 提供接口方法：1、登账 addAccountBook  2、删帐 delAccountBook  3、检查账户是否透支 checkAccOverDraft
 * @Author majfd
 * @Date 2021/09/10 14:18
 * @Version 1.0
 **/
@Slf4j
@Service
public class CmpWriteBankaccUtils {

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    SettlementService settlementService;

    //更新账户余额表的实时账面余额处理
    private static final String INITDATAMAPPER = "com.yonyoucloud.fi.cmp.mapper.InitDataMapper.updateInitDataAccount";

    private static final @NonNull Cache<String, InitData> initDataCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    /**
     * 登帐
     * 增加账；账面实时余额为加上当前借方金额 减去贷方金额
     * @param journal 日记账实体
     * @throws Exception
     */
    public void addAccountBook(Journal journal) throws Exception {
        if (null == journal){
            return;
        }
        String accid = getAccId(journal);
        if (StringUtils.isEmpty(accid)) {
            return;
        }
		//计算日记账余额
        InitData initdata = updateBalanceByAccount(journal, accid, journal.getCurrency());
        JournalService journalService = CtmAppContext.getBean(JournalService.class);
        //查询当前账户的所属组织 进行赋值操作
        journalService.setParentAccentityForJournal(journal);
        //查询当前资金组织所对应的核算会计主体 进行赋值操作
        journal.setAccentityRaw(AccentityUtil.getFinOrgDTOByAccentityId(journal.getAccentity()).getId());
        //登账时 进行新版日结检查校验 校验当前账户的所属组织是否已经日结
        settlementService.checkSettleForAcctParentAccentity(journal.getAccentity(),journal.getBankaccount(),journal.getVouchdate(),EntityStatus.Insert);
        //登账时如果有dzdate 那么对dztime进行赋值
        journal.setDztime(journal.getDzdate());
        //该日记账余额可能不对，业务逻辑中不使用
        journal.setNatbalance(initdata.getCobooklocalbalance());
        journal.setOribalance(initdata.getCobookoribalance());
        journal.setEntityStatus(EntityStatus.Insert);
        journal.setId(ymsOidGenerator.nextId());
        journalService.addOthertitle(journal);
        addJournalRptype(journal);
        CmpMetaDaoHelper.insert(Journal.ENTITY_NAME, journal);
        //进行kafka异步推送 新增日记账
//        GetCmpData.sendJournalData("add",null,journal);
    }

    /**
     * 添加日记账收付类型
     * @param journal
     */
    private void addJournalRptype(Journal journal) {
        // 借方金额大于0 收入
        if (journal.getDebitoriSum() != null && journal.getDebitoriSum().compareTo(BigDecimal.ZERO) > 0) {
            journal.setRptype(RpType.ReceiveBill);
        } else if (journal.getCreditoriSum() != null && journal.getCreditoriSum().compareTo(BigDecimal.ZERO) > 0) {
            // 贷方金额大于0 支出
            journal.setRptype(RpType.PayBill);
        }
    }


    /**
     * 登帐 (资金结算登账逻辑使用，只更新期初余额)
     * 增加账；账面实时余额为加上当前借方金额 减去贷方金额
     * @param journal 日记账实体
     * @throws Exception
     */
    public void addAccountBookSTWB(Journal journal) throws Exception {
        if (null == journal){
            return;
        }
        String accid = getAccId(journal);
        if (StringUtils.isEmpty(accid)) {
            return;
        }
        updateBalanceByAccount(journal, accid, journal.getCurrency());
    }

    /**
     * 增加账面时调用，修改时重新占用，新增单据时增加帐面余额
     * @param journal 日记账实体
     * @param accid 账户id
     * @param currency 账户所属币种id
     * @return
     * @throws Exception
     */
	private InitData updateBalanceByAccount(Journal journal, String accid, String currency) throws Exception {
        InitData initdata = getInitDataByAccid(journal.getAccentity(),accid, currency);
        if (ValueUtils.isNotEmpty(initdata)) {
            Map<String, Object> objectMap = getInitDataVoByAdd(journal, initdata);
            SqlHelper.update(INITDATAMAPPER, objectMap);
            //日记账中余额字段赋值使用
            initdata.setCobookoribalance(BigDecimalUtils.safeAdd(initdata.getCobookoribalance(), journal.getDebitoriSum()));
            initdata.setCobooklocalbalance(BigDecimalUtils.safeAdd(initdata.getCobooklocalbalance(), journal.getDebitnatSum()));
            initdata.setCobookoribalance(BigDecimalUtils.safeSubtract(initdata.getCobookoribalance(), journal.getCreditoriSum()));
            initdata.setCobooklocalbalance(BigDecimalUtils.safeSubtract(initdata.getCobooklocalbalance(), journal.getCreditnatSum()));
        } else {
            // 增加redis 去重 accentity  + accid + currency
            String initDataKey = "createinitdata:" + journal.getAccentity() + accid + currency;
            try (YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(initDataKey)) {
                if (null == ymsLock) {
                    return new InitData();
                }
            }
            initdata = createInitDataVO(journal);
            initdata.setEntityStatus(EntityStatus.Insert);
            initdata.setId(ymsOidGenerator.nextId());
            CmpMetaDaoHelper.insert(InitData.ENTITY_NAME, initdata);
        }
        return initdata;
	}

    /**
     * 登账时更新实时账面余额
     * 正向：加借减贷
     * @param journal 日记账实体
     * @param initdata 期初余额实体
     * @return
     */
	private static Map<String, Object> getInitDataVoByAdd(Journal journal, InitData initdata) {
        Map<String, Object> map = new HashMap<>(8);
        map.put("id", initdata.getId());
        map.put("ytenant_id", InvocationInfoProxy.getTenantid());
        if (Direction.Debit.getValue() == journal.getDirection().getValue()) {
            map.put(IBussinessConstant.ORI_SUM_L, journal.getDebitoriSum());
            map.put(IBussinessConstant.LOCAL_SUM, journal.getDebitnatSum());
        } else {
            map.put(IBussinessConstant.ORI_SUM_L, BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getCreditoriSum()));
            map.put(IBussinessConstant.LOCAL_SUM, BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getCreditnatSum()));
        }
        //20240731补充 汇兑损益特殊 保存时直接登实占帐
//        changeInitSettledBalance(journal,map,true);
        return map;
    }

    /**
     * 删账时更新实时账面余额
     * 逆向：加贷减借
     * @param journal 日记账实体
     * @param initdata 期初余额实体
     * @return
     */
    private static Map<String, Object> getInitDataVoByDel(Journal journal, InitData initdata) {
        Map<String, Object> map = new HashMap<>(8);
        map.put("id", initdata.getId());
        map.put("ytenant_id", InvocationInfoProxy.getTenantid());
        if (Direction.Debit.getValue() == journal.getDirection().getValue()) {
            map.put(IBussinessConstant.ORI_SUM_L, BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getDebitoriSum()));
            map.put("localsum", BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getDebitnatSum()));
        } else {
            map.put(IBussinessConstant.ORI_SUM_L, journal.getCreditoriSum());
            map.put("localsum", journal.getCreditnatSum());
        }
//        changeInitSettledBalance(journal,map,false);
        return map;
    }

    //处理登账、删账时候 账户期初的实占余额(这里仅针对汇兑损益 此节点保存即实占)
//    private static void changeInitSettledBalance(Journal journal,Map<String, Object> map,Boolean isAdd){
//        if(journal.getSettlestatus().getValue() != SettleStatus.alreadySettled.getValue()){
//            return;
//        }
//        if(isAdd){//登账
//            //已经结算成功的 更新实占余额
//            if (Direction.Debit.getValue() == journal.getDirection().getValue()) {
//                map.put(IBussinessConstant.ORISETTLED_SUM, journal.getDebitoriSum());
//                map.put(IBussinessConstant.NATSETTLED_SUM, journal.getDebitnatSum());
//            } else {
//                map.put(IBussinessConstant.ORISETTLED_SUM, BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getCreditoriSum()));
//                map.put(IBussinessConstant.ORISETTLED_SUM, BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getCreditnatSum()));
//            }
//        }else{//删账
//            if (Direction.Debit.getValue() == journal.getDirection().getValue()) {
//                map.put(IBussinessConstant.ORISETTLED_SUM, BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getDebitoriSum()));
//                map.put(IBussinessConstant.NATSETTLED_SUM, BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getDebitnatSum()));
//            } else {
//                map.put(IBussinessConstant.ORISETTLED_SUM, journal.getCreditoriSum());
//                map.put(IBussinessConstant.NATSETTLED_SUM, journal.getCreditnatSum());
//            }
//        }
//    }

    /**
     * 删除账
     * 实时账面余额计算：加上当前贷方金额 减去借方金额
     * @param billid 来源单据id
     * @param check 校验是否已勾兑
     * @throws Exception
     */
    public static void delAccountBook(String billid, Boolean  check) throws Exception {
        if (StringUtils.isEmpty(billid)) {
            return;
        }
        List<Map<String, Object>> journalListold = MetaDaoHelper.query(Journal.ENTITY_NAME,
                QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("srcbillitemid").eq(billid)));
        if (ValueUtils.isNotEmpty(journalListold)) {
            for (Map<String, Object> oldJournal : journalListold){
                Journal journal = new Journal();
                journal.init(oldJournal);
                if (check && journal.getCheckflag()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101108"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180076","该单据已勾对，不能进行删除！") /* "该单据已勾对，不能进行删除！" */);
                }
                updateAccountByJournal(journal);
            }
		}
    }

    /**
     * 删除账，默认校验银行日记账是否已勾兑，若已勾兑，抛出异常
     * 实时账面余额计算：加上当前贷方金额 减去借方金额
     * @param billid 来源单据id
     * @throws Exception
     */
    public static void delAccountBook(String billid) throws Exception {
        delAccountBook(billid, true);
    }

    /**
     * 删除账；实时账面余额加上当前贷方金额 减去借方金额
     *
     * @param journal 日记账实体
     * @throws Exception
     */

    public static void delAccountBookByJournal(Journal journal) throws Exception {
        if (ValueUtils.isNotEmpty(journal)) {
			updateAccountByJournal(journal);
		}
    }

    /**
     * 删除账面时调用，修改账面实时余额原占用，删除单据日记账
     * @param journal 日记账实体
     * @throws Exception
     */
	private static void updateAccountByJournal(Journal journal) throws Exception {
        //日记账已勾对的情况下，不允许删除
        if (journal.getCheckflag() !=null && journal.getCheckflag() && StringUtils.isNotEmpty(journal.getCheckno())) {
            throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1FE5834C04B0000A", "银行日记账已和银行流水完成勾对，不可以删除，勾对号为：%s") /* "银行日记账已和银行流水完成勾对，不可以删除，勾对号为：%s" */, journal.getCheckno()));
        }
		String accid = getAccId(journal);
        if (StringUtils.isEmpty(accid)) {
            return;
        }
		InitData initData = getInitDataByAccid(journal.getAccentity(),accid, journal.getCurrency());
		if (ValueUtils.isNotEmpty(initData)) {
			Map<String, Object> initDataVoByDel = getInitDataVoByDel(journal, initData);
			SqlHelper.update(INITDATAMAPPER, initDataVoByDel);
		}
		MetaDaoHelper.delete(Journal.ENTITY_NAME, journal);
	}


    /**
     * 通过账户ID和币种ID查询账户期初余额实体，无需区别是现金还是银行
     * 用户更新账户所属币种的实时账面余额
     * @param accid 账户id
     * @param currency 币种id
     * @return
     * @throws Exception
     */
    public static InitData queryInitDatabyAccid(String accentity,String accid, String currency) throws Exception {
        if (StringUtils.isEmpty(accid) || StringUtils.isEmpty(currency)) {
            return null;
        }
        QuerySchema queryInitDataSchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and
                (QueryCondition.name("currency").eq(currency),QueryCondition.name("accentity").eq(accentity));
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.or);
        conditionGroup.addCondition(QueryCondition.name("bankaccount").eq(accid));
        conditionGroup.addCondition(QueryCondition.name("cashaccount").eq(accid));
        group.addCondition(conditionGroup);
        queryInitDataSchema.addCondition(group);
        List<Map<String, Object>> initDataList = MetaDaoHelper.query(InitData.ENTITY_NAME, queryInitDataSchema);
        if (!CollectionUtils.isEmpty(initDataList)) {
            InitData initData = new InitData();
            initData.init(initDataList.get(0));
            return initData;
        }
        return null;
    }

    public static InitData getInitDataByAccid(String accentity,String accid, String currency) throws Exception {
        if (StringUtils.isEmpty(accid) || StringUtils.isEmpty(currency)) {
            return null;
        }
        Object tenantId = AppContext.getTenantId();
        String cacheKey = accentity + accid + currency + tenantId;
        InitData result = initDataCache.getIfPresent(cacheKey);
        if (result != null) {
            return result;
        }
        InitData initData = queryInitDatabyAccid(accentity,accid, currency);
        if (initData != null) {
            result = initData;
        } else {
            result = null;
        }
        if (result != null) {
            initDataCache.put(cacheKey, result);
        }
        return result;
    }

    /**
     * 取日记账账户id
     * @param journal 日记账实体
     * @return
     */
    public static String getAccId(Journal journal) {
        String accid = "";
        if (ValueUtils.isNotEmpty(journal.getBankaccount())) {
            accid = journal.getBankaccount();
        } else if (ValueUtils.isNotEmpty(journal.getCashaccount())) {
            accid = journal.getCashaccount();
        }
        return accid;
    }

    /**
     * 根据日记账实体，初始化账户所属币种的期初余额数据
     * @param journal 日记账实体
     * @return
     * @throws Exception
     */
    private InitData createInitDataVO(Journal journal) throws Exception {
        InitData initData = new InitData();
        initData.setAccentity(journal.getAccentity());
        initData.setAccentityRaw(journal.getAccentityRaw());
        if(journal.getParentAccentity()==null){
            JournalService journalService = CtmAppContext.getBean(JournalService.class);
            journalService.setParentAccentityForJournal(journal);
        }
        initData.setParentAccentity(journal.getParentAccentity());
        initData.setUpgradesign(UpgradeSignEnum.ADDNEW.getValue());
        String currencyOrg = AccentityUtil.getNatCurrencyIdByAccentityId(journal.getAccentity());
        initData.setPeriod(null);
        initData.setAccountdate(journal.getDzdate()!=null?journal.getDzdate():journal.getVouchdate());
        if (journal.getBankaccount() != null) {
            initData.setMoneyform(MoneyForm.bankaccount);
        } else if (journal.getCashaccount() != null) {
            initData.setMoneyform(MoneyForm.cashstock);
        }
        initData.setBankaccount(journal.getBankaccount());
        initData.setBankaccountno(journal.getBankaccountno());
        initData.setCashaccount(journal.getCashaccount());
        initData.setCashaccountno(journal.getCashaccountno());
        initData.setCurrency(journal.getCurrency());
        initData.setNatCurrency(currencyOrg);
        initData.setExchangerate(journal.getExchangerate());
        // 默认乘
        initData.setExchRateOps((short)1);
        initData.setBankinitlocalbalance(BigDecimal.ZERO);
        initData.setCoinitlocalbalance(BigDecimal.ZERO);
        initData.setCobooklocalbalance(BigDecimal.ZERO);
        initData.setBankinitoribalance(BigDecimal.ZERO);
        initData.setCoinitloribalance(BigDecimal.ZERO);
        initData.setCobookoribalance(BigDecimal.ZERO);
        initData.setCobookoribalance(BigDecimalUtils.safeAdd(initData.getCobookoribalance(), journal.getDebitoriSum()));
        initData.setCobooklocalbalance(BigDecimalUtils.safeAdd(initData.getCobooklocalbalance(), journal.getDebitnatSum()));
        initData.setCobookoribalance(BigDecimalUtils.safeSubtract(initData.getCobookoribalance(), journal.getCreditoriSum()));
        initData.setCobooklocalbalance(BigDecimalUtils.safeSubtract(initData.getCobooklocalbalance(), journal.getCreditnatSum()));
        // 获取汇率类型和汇率
        ExchangeRateTypeVO defaultExchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(journal.getAccentity(), true);
        if (defaultExchangeRateType != null && defaultExchangeRateType.getId() != null) {
            initData.setExchangeRateType(defaultExchangeRateType.getId());
        }
        // 不等于用户自定义汇率再寻汇
        if (defaultExchangeRateType != null && !defaultExchangeRateType.getCode().equals("02")) {
            try {
                // 获取汇率和汇率折算方式
                CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(initData.getCurrency(), initData.getNatCurrency(), initData.getAccountdate(), defaultExchangeRateType.getId());
                if (cmpExchangeRateVO.getExchangeRate().compareTo(BigDecimal.ZERO) != 0) {
                    // 汇率
                    initData.setExchangerate(cmpExchangeRateVO.getExchangeRate());
                    // 汇率折算方式
                    initData.setExchRateOps(cmpExchangeRateVO.getExchangeRateOps());
                }
            } catch (Exception e) {
                log.error("账户期初不存在创建期初获取汇率异常:{}", e.getMessage(), e);
            }
        }
        initData.setDirection(Direction.Debit);
        initData.setBankdirection(Direction.Credit);//银行方余额方向，默认为贷
        initData.setQzbz(true);
        initData.setCreateDate(new Date());
        initData.setCreateTime(new Date());
        initData.setCreator(AppContext.getCurrentUser().getName());
        InitDataService initDateService = CtmAppContext.getBean(InitDataService.class);
        Map<String, Period> periodMap = initDateService.queryListFinanceOrg(Collections.singletonList(journal.getAccentity()));
        // 企业银行账户
        if (StringUtils.isNotEmpty(initData.getBankaccount())) {
            EnterpriseBankQueryService enterpriseBankQueryService = CtmAppContext.getBean(EnterpriseBankQueryService.class);
            EnterpriseBankAcctVOWithRange bankAcctVO = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(initData.getBankaccount());
            initDateService.initAccountDate(periodMap, journal.getAccentity(), initData, bankAcctVO);
        } else {
            // 企业现金账户
            initDateService.initCashAccountDate(periodMap, journal.getAccentity(), initData, QueryBaseDocUtils.queryCashBankAccountById(initData.getCashaccount()));
        }
        return initData;
    }

    ///**
    // * 查询会计主体默认汇率类型
    // * @param orgid 会计主体id
    // * @return
    // * @throws Exception
    // */
    //public static Map<String, Object> getDefaultExchangeRateType(String orgid) throws Exception {
    //    // 适配财务公共去除业务账簿修改
    //    return QueryBaseDocUtils.queryExchangeRateTypeByOrgId(orgid);/* 暂不修改 静态方法 可实现*/
    //}

    /**
     * 校验账户是否透支
     * 账户添加透支控制时，在业务单据结算时校验账户实时余额是否已透支，添加提示控制
     * @param accId 账户id
     * @param currency 币种id
     */
    public static boolean checkAccOverDraft(String accentity,String accId, String currency) throws Exception {
        InitData initData = queryInitDatabyAccid(accentity,accId, currency);
        if (initData != null) {
            BigDecimal balance = initData.getCobookoribalance();
            Short overDraft = initData.getOverdraftCtrl(); //账户透支控制项
            if (overDraft != 1) {
                if (balance != null && balance.compareTo(BigDecimal.ZERO) < 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
