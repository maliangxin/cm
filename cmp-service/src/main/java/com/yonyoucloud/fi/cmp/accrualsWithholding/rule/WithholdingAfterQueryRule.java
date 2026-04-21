package com.yonyoucloud.fi.cmp.accrualsWithholding.rule;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.model.LoginUser;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.cmpentity.DailySettlementControl;
import com.yonyoucloud.fi.cmp.cmpentity.WithholdingRuleStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IFieldConstant;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.withholding.InterestRateSettingHistory;
import com.yonyoucloud.fi.cmp.withholding.WithholdingRuleSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author shangxd
 * @date 2023/4/17 10:42
 * @describe
 */
@Slf4j
@Component("withholdingAfterQueryRule")
@RequiredArgsConstructor
public class WithholdingAfterQueryRule extends AbstractCommonRule {

    private final BaseRefRpcService baseRefRpcService;
    @Autowired
    private FundsOrgQueryServiceComponent fundsOrgQueryService;

//    //private final OrgRpcService orgRpcService;

    private final CmCommonService commonService;
    private final EnterpriseBankQueryService enterpriseBankQueryService;
    private final CtmThreadPoolExecutor ctmThreadPoolExecutor;

    private static final int PAGE_SIZE = 10; // 128

    private static final Cache<String, EnterpriseBankAcctVO> enterpriseBankAcctVOCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        RuleExecuteResult ruleResult = new RuleExecuteResult();
//        BillDataDto billDataDto = (BillDataDto) paramMap.get("param");
//        List<BizObject> paramList  = (List<BizObject>)billDataDto.getData();
//        BizObject prarm = paramList.get(0);
//        //分页相关信息
//        int pageIndex = billDataDto.getQuerySchema().pager()!=null?billDataDto.getQuerySchema().pager().pageIndex():0;
//        int pageSize = billDataDto.getQuerySchema().pager()!=null?billDataDto.getQuerySchema().pager().pageSize():0;
        //用于组装查询条件
        Map<String, Object> param = new HashMap<>();
        //获取返回页面的page
        Pager pager = (Pager) paramMap.get(ICmpConstant.RETURN);
        //页码
        int pageIndex = pager.getPageIndex();
        //每页条数
        int pageSize = pager.getPageSize();
        //组装参数
        buildParam(paramMap, param);
        //过滤区预提日期-------结束日期=等于查询条件选择的预提日期-1
        Date filterDate = DateUtils.dateParse(param.get(ICmpConstant.ACCRUEDENDDATE).toString(), null);
        //结束日期
        Date endDate = DateUtils.dateAdd(filterDate, -1, false);

        //查询会计主体日结最大日期与预提日期 比较  如果小于预提日期  则所有日结控制为是的数据过滤掉
        Map<String, Object> checkSettleMap = checkSettle(param.get(ICmpConstant.ACCENTITY).toString());
        if(null != checkSettleMap){
            Date settleDate = DateUtils.dateParse(checkSettleMap.get("settlementdate").toString(),null);
            if(settleDate.compareTo(endDate) < 0){
                //查询数据不查询日结控制规则为先日结后预提的数据
                param.put("accruaAfterSettlement",DailySettlementControl.OutOfControl.getValue());
            }
        }else{
            param.put("accruaAfterSettlement",DailySettlementControl.OutOfControl.getValue());
        }

        //查询会计主体
        String accentity = param.get(ICmpConstant.ACCENTITY).toString();
        FundsOrgDTO fundsOrgDTO = fundsOrgQueryService.getById(accentity);
        Map<String, Object> accEntityes = getAccentity(accentity);
        // 汇率类型
        ExchangeRateTypeVO defaultExchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(accentity, true);

        //查询预提规则设置表
        List<Map<String, Object>> withholdingListTemp = getWithholdingRuleSettingList(endDate, param, pageIndex, pageSize);
        //分页前变量
        List<AccrualsWithholding> withholdingTotalList = new ArrayList<>(withholdingListTemp.size());
        if(withholdingListTemp.size() <= PAGE_SIZE){
            for (Map<String, Object> bizObject : withholdingListTemp){
                AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
                accrualsWithholding.init(bizObject);
                if((accrualsWithholding.getLastInterestAccruedDate()!= null || accrualsWithholding.getLastInterestSettlementDate() != null) &&
                        (endDate.compareTo( DepositinterestWithholdingUtil.getMaxDate(accrualsWithholding.getLastInterestAccruedDate(),
                                accrualsWithholding.getLastInterestSettlementDate(), null)) > 0)){
                    if(isFilterate( accrualsWithholding)){
                        withholdingTotalList.add(accrualsWithholding);
                    }
                }
            }
        }else{
            withholdingTotalList = mulThreadIsFilterate( withholdingListTemp, endDate);
        }
        ResultList<AccrualsWithholding> page = PageUtils.page(pageIndex, pageSize, withholdingTotalList, true);
        //返回页面数据
        List<AccrualsWithholding> withholdingList = page.getList();
        //如果银行账户的日结控制规则为先日结后预提，还需要加上过滤条件，本次预提日期<该银行账户日记账的最后一次日结日期（没有日结数据过滤掉）
        if (!CollectionUtils.isEmpty(withholdingList)) {
            for (AccrualsWithholding accrualsWithholding : withholdingList) {
                //查询银行账户利率设置变更历史表获取利率等信息
                //id,accentity,bankType,bankaccount,currency,lastInterestAccruedDate,lastInterestSettlementDate," +
                //                "dailySettlementControl
                List<Map<String, Object>> interestRateSettingHistoryList = getInterestRateSettingHistoryList( endDate,  accrualsWithholding.getId());

//                id,startDate ,endDate ,interestRate , overdraftRate
                Map<String, Object> interestCalculationMap = interestRateSettingHistoryList.get(0);
                accrualsWithholding.setAccruedStartDate((Date) interestCalculationMap.get("startDate"));
                accrualsWithholding.setAccruedStartDate((Date) interestCalculationMap.get("endDate"));
                accrualsWithholding.setCurrentDepositRate(new BigDecimal(interestCalculationMap.get("interestRate").toString()) );
                accrualsWithholding.setCurrentOverdraftRate(new BigDecimal(interestCalculationMap.get("overdraftRate").toString()));

                Date lastInterestSettlementOrAccruedDate = DepositinterestWithholdingUtil.getMaxDate(accrualsWithholding.getLastInterestAccruedDate(),
                        accrualsWithholding.getLastInterestSettlementDate(), null);
                accrualsWithholding.setLastInterestSettlementOrAccruedDate(lastInterestSettlementOrAccruedDate);
                //等于max（上次结息结束日+1、上次预提结束日+1）
                accrualsWithholding.setAccruedStartDate(DateUtils.dateAdd(lastInterestSettlementOrAccruedDate, 1, false));
                accrualsWithholding.setAccruedEndDate(endDate);
                //设置资金组织
                accrualsWithholding.set(ICmpConstant.ACCENTITY_NAME,fundsOrgDTO.getName());
                //会计主体 币种 本币 汇率类型 汇率
                setInfo(accrualsWithholding,accEntityes,defaultExchangeRateType);
            }
        }

        pager.setRecordList(withholdingList);
        //当前页码
        pager.setPageIndex(pageIndex);
        //总条数
        pager.setRecordCount(page.getTotal());
        //每页条数
        pager.setPageSize(pageSize);
        ruleResult.setData(pager);
        return ruleResult;
    }

    private List<AccrualsWithholding> mulThreadIsFilterate(List<Map<String, Object>> withholdingListTemp,Date endDate) throws Exception {
        List<AccrualsWithholding> resultList = Lists.newArrayList();
        //获取线程池
        ExecutorService threadPoolExecutor = ctmThreadPoolExecutor.getThreadPoolExecutor();
        //String locale = InvocationInfoProxy.getLocale();
        List<Future<List<AccrualsWithholding>>> futures = Lists.partition(withholdingListTemp, 10)
                .stream()
                .map(subList -> threadPoolExecutor.submit(() -> {
                    return isFilterateMul(subList,endDate);
                }))
                .collect(Collectors.toList());
        // 封装返回值
        for (int i = 0; i < futures.size(); i++) {
            List<AccrualsWithholding>  accrualsWithholdingList =futures.get(i).get();
            resultList.addAll(accrualsWithholdingList);
        }
        return resultList;
    }

    /**
     * 多线程处理
     * @param withholdingListTemp
     * @param endDate
     * @return
     * @throws Exception
     */
    private List<AccrualsWithholding> isFilterateMul(List<Map<String, Object>> withholdingListTemp,Date endDate ) throws Exception {
        List<AccrualsWithholding> listTotal = Lists.newArrayList();
        for (Map<String, Object> bizObject : withholdingListTemp){
            AccrualsWithholding accrualsWithholding = new AccrualsWithholding();
            accrualsWithholding.init(bizObject);
            if((accrualsWithholding.getLastInterestAccruedDate()!= null || accrualsWithholding.getLastInterestSettlementDate() != null) &&
                    (endDate.compareTo( DepositinterestWithholdingUtil.getMaxDate(accrualsWithholding.getLastInterestAccruedDate(),
                            accrualsWithholding.getLastInterestSettlementDate(), null)) > 0)){
                if(isFilterate( accrualsWithholding)){
                    listTotal.add(accrualsWithholding);
                }
            }
        }
        return listTotal;
    }

    /**
     * 判断企业银行账户是否异常
     * @param accrualsWithholding
     * @return
     * @throws Exception
     */
    private boolean isFilterate(AccrualsWithholding accrualsWithholding) throws Exception {
        String currency = accrualsWithholding.getCurrency();
        if (currency == null) {
            log.error("================WithholdingAfterQueryRule filterateError currency is null:" + accrualsWithholding.getBankaccount());
            return false;
        }
        EnterpriseBankAcctVO enterpriseBankAccount = getEnterpriseBankAccount(accrualsWithholding.getCurrency(),accrualsWithholding.getBankaccount());
        if(null == enterpriseBankAccount){
            log.error("================WithholdingAfterQueryRule filterateError enterpriseBankAccount is null currency:"+ accrualsWithholding.getCurrency()+";bankaccount:"+accrualsWithholding.getBankaccount());
            return false;
        }
        accrualsWithholding.set(ICmpConstant.BANKACCOUNT_NAME,enterpriseBankAccount.getName());
        accrualsWithholding.set(ICmpConstant.BANKACCOUNT_ACCOUNT,enterpriseBankAccount.getAccount());//	银行账号
        accrualsWithholding.set(ICmpConstant.BANKACCOUNT_ACCTNAME,enterpriseBankAccount.getAcctName());//	开户名
        accrualsWithholding.set(ICmpConstant.BANKACCOUNT_CODE,enterpriseBankAccount.getCode());//	账户编码
        accrualsWithholding.set(ICmpConstant.BANKTYPE_NAME,enterpriseBankAccount.getBankName());//	银行类别名称

        return true;
    }

    /**
     * 查询会计主体
     * @param accentity
     * @return
     * @throws Exception
     */
    private Map<String, Object> getAccentity(String accentity) throws Exception {
        //会计主体
        List<Map<String, Object>> accEntityes = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accentity);
        if(CollectionUtils.isEmpty(accEntityes)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102021"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050041", "未查询到对应的资金组织") /* "未查询到对应的资金组织" */);
        }else{
            return accEntityes.get(0);
        }
    }

    /**
     * 根据id查询企业银行账户
     * @param currency
     * @param bankAccount
     * @return
     * @throws Exception
     */
    private EnterpriseBankAcctVO getEnterpriseBankAccount(String currency, String bankAccount) throws Exception {
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccount);
        if (null != enterpriseBankAcctVO ) {
            Integer enterpriseBankAccountFlag = enterpriseBankAcctVO.getEnable();
            if (enterpriseBankAccountFlag != null  && (2 == enterpriseBankAccountFlag || 0 == enterpriseBankAccountFlag)) {// enable = 2 停用     0 是未启用    1是启用
                enterpriseBankAcctVO = null;
            }
        }
//        EnterpriseBankAcctVO enterpriseBankAcctVO = null;
//        String enterpriseBankKey = currency.concat(InvocationInfoProxy.getTenantid()).concat(bankAccount);
//        if (null != enterpriseBankAcctVOCache.getIfPresent(enterpriseBankKey)) {
//            enterpriseBankAcctVO = enterpriseBankAcctVOCache.getIfPresent(enterpriseBankKey);
//        } else {
//            enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccount);
//            if (null != enterpriseBankAcctVO ) {
//                Integer enterpriseBankAccountFlag = enterpriseBankAcctVO.getEnable();
//                if (enterpriseBankAccountFlag != null  && (2 == enterpriseBankAccountFlag || 0 == enterpriseBankAccountFlag)) {// enable = 2 停用     0 是未启用    1是启用
//                    enterpriseBankAcctVO = null;
//                } else {
//                    enterpriseBankAcctVOCache.put(enterpriseBankKey, enterpriseBankAcctVO);
//                }
//            }
//        }
        return enterpriseBankAcctVO;
    }

    /**
     * 检查当前日期是否日结
     *
     * @param accentity
     * @throws Exception
     */
    private Map<String, Object> checkSettle(String accentity) throws Exception {

        QuerySchema querySchema = QuerySchema.create().addSelect("max(settlementdate) as settlementdate");
        //查询最大结账日
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                QueryCondition.name("settleflag").eq(1));
        querySchema.addCondition(group);
        return MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);

//        QuerySchema querySchema = QuerySchema.create();
//        querySchema.addSelect("*");
//        QueryConditionGroup conditionGroup = new QueryConditionGroup();
//        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.ACCENTITY).eq(accentity));
//        QueryOrderby order = new QueryOrderby("accountdate", "desc");
//        querySchema.addOrderBy(order);
//        querySchema.addCondition(conditionGroup);
//        querySchema.setLimitCount(1);
//        List<Map<String, Object>> settlementDetailList = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchema);
//        if (CollectionUtils.isNotEmpty(settlementDetailList)) {
//            return settlementDetailList.get(0);
//        } else {
//            return null;
//        }
    }

    /**
     * 会计主体 币种 本币 汇率类型 汇率
     *
     * @param accrualsWithholding
     * @throws Exception
     */
    private void setInfo(AccrualsWithholding accrualsWithholding, Map<String, Object> accEntityes, ExchangeRateTypeVO defaultExchangeRateType ) throws Exception {
        //币种精度
        CurrencyTenantDTO currencyDTO = findCurrency(accrualsWithholding.getCurrency());
        accrualsWithholding.set("currency_name", currencyDTO.getName());
        accrualsWithholding.set("currency_priceDigit", currencyDTO.getPricedigit());
        accrualsWithholding.set("currency_moneyDigit", currencyDTO.getMoneydigit());
        accrualsWithholding.set("currency_moneyRount", currencyDTO.getMoneyrount());
        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accrualsWithholding.getAccentity());
        accrualsWithholding.setNatCurrency(finOrgDTO.getCurrency());
        CurrencyTenantDTO currencyNatDTO = findNatCurrency(accrualsWithholding.getNatCurrency());
        if(null == currencyNatDTO){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100681"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D1","本币币种不能为空！") /* "本币币种不能为空！" */);
        }
        accrualsWithholding.set("natCurrency_name", currencyNatDTO.getName());
        accrualsWithholding.set("natCurrency_priceDigit", currencyNatDTO.getPricedigit());
        accrualsWithholding.set("natCurrency_moneyDigit", currencyNatDTO.getMoneydigit());
        accrualsWithholding.set("natCurrency_moneyRount", currencyNatDTO.getMoneyrount());

        //会计主体com.yonyoucloud.fi.cmp.constant.IFieldConstant.ACCENTITYRAW_NAME
        accrualsWithholding.set(IFieldConstant.ACCENTITYRAW_NAME,accEntityes.get(ICmpConstant.NAME));
        // 汇率类型
        if (StringUtils.isNotEmpty(defaultExchangeRateType.getId())) {
            accrualsWithholding.setExchangeRateType(defaultExchangeRateType.getId());
            accrualsWithholding.set("exchangeRateType_digit", defaultExchangeRateType.getDigit());
            accrualsWithholding.set("exchangeRateType_name", defaultExchangeRateType.getName());
        }
        String currency = accrualsWithholding.getCurrency();
        String natCurrency = accrualsWithholding.getNatCurrency();
        // 汇率（取汇率表中报价日期小于等于单据日期的值）
        if (currency.equals(natCurrency)) {
            accrualsWithholding.setExchangerate(new BigDecimal("1"));
            accrualsWithholding.setExchangerateOps((short) 1);
        } else {
            CmpExchangeRateVO exchangeRateWithMode = CmpExchangeRateUtils.getNewExchangeRateWithMode(currency, natCurrency, accrualsWithholding.getAccruedEndDate(), accrualsWithholding.getExchangeRateType(), accrualsWithholding.get("exchangeRateType_digit"));
            if (!BigDecimal.ZERO.equals(exchangeRateWithMode.getExchangeRate())) {
                accrualsWithholding.setExchangerate(exchangeRateWithMode.getExchangeRate().setScale(accrualsWithholding.get("exchangeRateType_digit"), RoundingMode.HALF_UP));
                accrualsWithholding.setExchangerateOps(exchangeRateWithMode.getExchangeRateOps());
            }
        }

    }

    /**
     * 查询币种
     *
     * @param currency
     * @return
     * @throws Exception
     */
    private CurrencyTenantDTO findCurrency(String currency) throws Exception {
        CurrencyTenantDTO currencyDTO = null;
        String locale = InvocationInfoProxy.getLocale();
        String currencyKey = currency.concat(InvocationInfoProxy.getTenantid()).concat("DEPOSITINTERESTWITHHOLDINGAFTERQUERYRULE").concat(locale);
        if (null != AppContext.cache().getObject(currencyKey)) {
            currencyDTO = AppContext.cache().getObject(currencyKey);
        } else {
            currencyDTO = baseRefRpcService.queryCurrencyById(currency);
            if (null != currencyDTO) {
                AppContext.cache().setObject(currencyKey, currencyDTO);
            }
        }
        return currencyDTO;
    }

    /**
     * 查询本币币种
     *
     * @param natCurrency
     * @return
     * @throws Exception
     */
    private CurrencyTenantDTO findNatCurrency(String natCurrency) throws Exception {
        CurrencyTenantDTO currencyNatDTO = null;
        String locale = InvocationInfoProxy.getLocale();
        String currencyKey = natCurrency.concat(InvocationInfoProxy.getTenantid()).concat("DEPOSITINTERESTWITHHOLDINGAFTERQUERYRULENAT").concat(locale);
        if (null != AppContext.cache().getObject(currencyKey)) {
            currencyNatDTO = AppContext.cache().getObject(currencyKey);
        } else {
            currencyNatDTO = baseRefRpcService.queryCurrencyById(natCurrency);
            if (null != currencyNatDTO) {
                AppContext.cache().setObject(currencyKey, currencyNatDTO);
            }
        }
        return currencyNatDTO;
    }

    /**
     * 查询预提规则利率测算表
     *
     * @param
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getInterestRateSettingHistoryList(Date endDate, Long mainId) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id,startDate ,endDate ,interestRate , overdraftRate");
//        QueryConditionGroup conditionGroup = getQueryConditionGroup(endDate, param);
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("startDate").elt(endDate));
        conditionGroup.appendCondition(QueryCondition.name("mainid").eq(mainId));
        schema.addCondition(conditionGroup);
        schema.addOrderBy(new QueryOrderby("endDate", "desc"));
        schema.addPager(0,1);
        return MetaDaoHelper.query(InterestRateSettingHistory.ENTITY_NAME, schema, null);
    }

    /**
     * 查询预提规则设置表
     *
     * @param
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getWithholdingRuleSettingList(Date endDate, Map<String, Object> param, int pageIndex, int pageSize) throws Exception {
//        QuerySchema schema = QuerySchema.create().addSelect("id,accentity,bankType,bankaccount,currency,lastInterestAccruedDate,lastInterestSettlementDate," +
//                "dailySettlementControl,InterestRateSettingHistory.startDate as accruedStartDate,InterestRateSettingHistory.endDate as accruedEndDate," +
//                "InterestRateSettingHistory.interestRate as currentDepositRate, InterestRateSettingHistory.overdraftRate as currentOverdraftRate");
        QuerySchema schema = QuerySchema.create().addSelect("id,accentity,bankType,bankaccount,currency,lastInterestAccruedDate,lastInterestSettlementDate," +
                "dailySettlementControl,accountPurpose,bankNumber");
        QueryConditionGroup conditionGroup = getQueryConditionGroup(endDate, param);
//        conditionGroup.appendCondition(QueryCondition.name("InterestRateSettingHistory.endDate").lt(endDate));
        schema.addCondition(conditionGroup);
        schema.addOrderBy(new QueryOrderby("accruedStartDate", "asc"));
//        schema.addOrderBy(new QueryOrderby("InterestRateSettingHistory.endDate", "desc"));
//        schema.addPager(pageIndex, pageSize);
//        schema.setLimitCount(1);
        return MetaDaoHelper.query(WithholdingRuleSetting.ENTITY_NAME, schema, null);
    }

    /**
     * 查询条件抽取
     *
     * @param endDate
     * @param param
     * @return
     */
    private QueryConditionGroup getQueryConditionGroup(Date endDate, Map<String, Object> param) {
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        //规则状态
        conditionGroup.appendCondition(QueryCondition.name("ruleStatus").eq(WithholdingRuleStatus.Enable.getValue()));

//        conditionGroup.appendCondition(QueryCondition.name("InterestRateSettingHistory.isNew").eq(1));
        if (!Objects.isNull(param.get(ICmpConstant.ACCENTITY))) {
            conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.ACCENTITY).eq(param.get(ICmpConstant.ACCENTITY)));
        }
        if (!Objects.isNull(param.get(ICmpConstant.ACCRUAAFTERSETTLEMENT))) {
            conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.DAILYSETTLEMENTCONTROL).eq(param.get(ICmpConstant.ACCRUAAFTERSETTLEMENT)));
        }
        if (!Objects.isNull(param.get(ICmpConstant.BANKTYPE))) {
            conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.BANKTYPE).in(param.get(ICmpConstant.BANKTYPE)));
        }
        if (!Objects.isNull(param.get(ICmpConstant.CURRENCY))) {
            conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.CURRENCY).eq(param.get(ICmpConstant.CURRENCY)));
        }
        if (!Objects.isNull(param.get(ICmpConstant.BANKACCOUNT))) {
            conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.BANKACCOUNT).in(param.get(ICmpConstant.BANKACCOUNT)));
        }
        return conditionGroup;
    }

    /**
     * 获取过滤条件数据
     *
     * @param paramMap
     * @param param
     * @throws Exception
     */
    private void buildParam(Map<String, Object> paramMap, Map<String, Object> param) throws Exception {
        LoginUser currentUser = AppContext.getCurrentUser();
        Long tenantId = currentUser.getTenant();
        param.put(ICmpConstant.TENANTID, tenantId);
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        //获取查询条件
        FilterVO filterVO = bill.getCondition();
        if (null == filterVO) {
            filterVO = new FilterVO();
        }
        FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
        if (commonVOs != null) {
            //遍历查询条件
            for (FilterCommonVO vo : commonVOs) {
                //会计主体
                if (ICmpConstant.ACCENTITY.equals(vo.getItemName())) {
                    param.put(ICmpConstant.ACCENTITY, vo.getValue1());
                }
                //预提日
                if (ICmpConstant.ACCRUEDENDDATE.equals(vo.getItemName())) {
                    param.put(ICmpConstant.ACCRUEDENDDATE, vo.getValue1());
                }
                //银行类别
                if (ICmpConstant.BANKTYPE.equals(vo.getItemName())) {
                    param.put(ICmpConstant.BANKTYPE, vo.getValue1());
                }
                //币种
                if (ICmpConstant.CURRENCY.equals(vo.getItemName())) {
                    param.put(ICmpConstant.CURRENCY, vo.getValue1());
                }
                //银行账号
                if (ICmpConstant.BANKACCOUNT.equals(vo.getItemName())) {
                    param.put(ICmpConstant.BANKACCOUNT, vo.getValue1());
                }
            }
        }
    }

}
