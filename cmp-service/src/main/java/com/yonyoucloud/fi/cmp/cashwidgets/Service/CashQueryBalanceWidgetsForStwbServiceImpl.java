package com.yonyoucloud.fi.cmp.cashwidgets.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.ExchangeRate;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.cashwidgets.CashQueryBalanceWidgetsForStwbService;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.vo.cashwidgets.CashQueryBalanceWidgetsForStwbVo;
import com.yonyoucloud.fi.cmp.vo.cashwidgets.SettleWorkbenchBaseRequest;
import org.apache.commons.collections4.CollectionUtils;
import lombok.NonNull;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CashQueryBalanceWidgetsForStwbServiceImpl implements CashQueryBalanceWidgetsForStwbService {

    @Autowired
    EnterpriseBankQueryService enterpriseQueryService;
    @Autowired
    CurrencyQueryService currencyQueryService;
    @Autowired
    BaseRefRpcService baseRefRpcService;

    //汇率缓存
    private static final @NonNull Cache<String, BigDecimal> exchangerateCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .softValues()
            .build();

    private static final String ACCTBAL = "acctbal";
    private static final String AVLBAL = "avlbal";

    @Override
    public CashQueryBalanceWidgetsForStwbVo cashQueryBalanceWidgetsForStwb(SettleWorkbenchBaseRequest settleWorkbenchBaseRequest) throws Exception {
        //公共使用参数
        String convertCurrency = settleWorkbenchBaseRequest.getPk_currency();//折算币种
        if(convertCurrency==null){//默认人民币
            convertCurrency = currencyQueryService.getCurrencyByCode("CNY");
        }
        //查询折算币种相关信息(取精度)
        CurrencyTenantDTO convertCurrencyDTO = baseRefRpcService.queryCurrencyById(convertCurrency);
        Short moneyDigit = Short.valueOf(convertCurrencyDTO.getMoneydigit().toString());//金额进度

        String convertRatetype = settleWorkbenchBaseRequest.getPk_ratetype();//折算汇率类型
        if(convertRatetype == null){
            return null;
        }
        CashQueryBalanceWidgetsForStwbVo resultVo = new CashQueryBalanceWidgetsForStwbVo();
        //查询符合权限条件的会计主体id集合
        List<String> orgPermissionsList = new ArrayList<>();
        orgPermissionsList.addAll(BillInfoUtils.getOrgPermissionsByAuth("cmp_fundpaymentlist"));
        List<String> intersection = orgPermissionsList;
        //查询当前会计主体集合下所有 有使用权限的账户(不传查询所有账户)
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        if(settleWorkbenchBaseRequest.getPk_entity()!=null && settleWorkbenchBaseRequest.getPk_entity().length>0){
            String[] pk_orgids = settleWorkbenchBaseRequest.getPk_entity();
            List<String> orgList = Arrays.asList(pk_orgids);
            //取交集
            intersection = orgList.stream()
                    .filter(orgPermissionsList::contains)
                    .collect(Collectors.toList());
        }
        enterpriseParams.setOrgidList(intersection);
        //商业银行账户
        List<String> bankAcctList = new ArrayList<>();
        //财务公司账户
        List<String> financeAcctList = new ArrayList<>();
        //结算中心户
        List<String> settlementAcctList = new ArrayList<>();

        List<EnterpriseBankAcctVO> acctList = enterpriseQueryService.queryAll(enterpriseParams);
        //循环账户 对账户分类
        for(EnterpriseBankAcctVO acctVo:acctList){
            if(acctVo.getAcctopentype().equals(AcctopenTypeEnum.BankAccount.getValue()) && ("0").equals(acctVo.getAccountNature())){
                bankAcctList.add(acctVo.getId());
            }
            if(acctVo.getAcctopentype().equals(AcctopenTypeEnum.FinancialCompany.getValue()) && ("0").equals(acctVo.getAccountNature())){
                financeAcctList.add(acctVo.getId());
            }
            //账户性质：活期 、 账户类型：一般
            if(acctVo.getAcctopentype().equals(AcctopenTypeEnum.SettlementCenter.getValue()) && ("0").equals(acctVo.getAccountNature()) && ("1").equals(acctVo.getAcctType())){
                settlementAcctList.add(acctVo.getId());
            }
        }

        //分别查询三种账户的账户实时余额合计(先分组统计 再根据币种计算)
        String tenantId = AppContext.getTenantId().toString();
        //今天和昨天
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.DATE_PATTERN);
        Date today = sdf.parse(sdf.format(new Date()));
        Date yestoday = DateUtils.dateAdd(today, -1, Boolean.FALSE);
        //查询商业银行余额-今日
        List<Map<String, Object>> bankAcctSumTodayList= queryRealtimeBalanceSumList(bankAcctList,today);
        Map<String,BigDecimal>  bankAcctTodayMap = computeBalance(bankAcctSumTodayList,today, convertRatetype, convertCurrency,tenantId);
        resultVo.setBankDeposit(bankAcctTodayMap.get(ACCTBAL));//存款余额
        resultVo.setAvaBankDeposit(bankAcctTodayMap.get(AVLBAL));//可用余额
        //查询商业银行余额-昨日
        List<Map<String, Object>> bankAcctSumYesTodayList= queryRealtimeBalanceSumList(bankAcctList,yestoday);
        Map<String,BigDecimal>  bankAcctYesTodayMap = computeBalance(bankAcctSumYesTodayList,yestoday, convertRatetype, convertCurrency,tenantId);
        BigDecimal bankAcctbalYesToday = bankAcctYesTodayMap.get(ACCTBAL);//存款余额
        BigDecimal bankAvlbalYesToday = bankAcctYesTodayMap.get(AVLBAL);//可用余额

        //查询财务公司银行余额-今日
        List<Map<String, Object>> financeAcctSumTodayList= queryRealtimeBalanceSumList(financeAcctList,today);
        Map<String,BigDecimal>  financeAcctTodayMap = computeBalance(financeAcctSumTodayList,today, convertRatetype, convertCurrency,tenantId);
        resultVo.setCompanyDeposit(financeAcctTodayMap.get(ACCTBAL));//存款余额
        resultVo.setAvaCompanyDeposit(financeAcctTodayMap.get(AVLBAL));//可用余额
        //查询财务公司银行余额-昨日
        List<Map<String, Object>> financeAcctSumYesTodayList= queryRealtimeBalanceSumList(financeAcctList,yestoday);
        Map<String,BigDecimal>  financeAcctYesTodayMap = computeBalance(financeAcctSumYesTodayList,yestoday, convertRatetype, convertCurrency,tenantId);
        BigDecimal financeAcctbalYesToday = financeAcctYesTodayMap.get(ACCTBAL);//存款余额
        BigDecimal financeAvlbalYesToday = financeAcctYesTodayMap.get(AVLBAL);//可用余额

        //查询结算中心银行余额-今日
        List<Map<String, Object>> settlementAcctSumTodayList= queryRealtimeBalanceSumList(settlementAcctList,today);
        Map<String,BigDecimal>  settlementAcctTodayMap = computeBalance(settlementAcctSumTodayList,today, convertRatetype, convertCurrency,tenantId);
        resultVo.setSettleCenterDeposit(settlementAcctTodayMap.get(ACCTBAL));//存款余额
        resultVo.setAvaSettleCenterDeposit(settlementAcctTodayMap.get(AVLBAL));//可用余额
        //查询结算中心银行余额-昨日
        List<Map<String, Object>> settlementAcctSumYesTodayList= queryRealtimeBalanceSumList(settlementAcctList,yestoday);
        Map<String,BigDecimal>  settlementAcctYesTodayMap = computeBalance(settlementAcctSumYesTodayList,yestoday, convertRatetype, convertCurrency,tenantId);
        BigDecimal settlementAcctbalYesToday = settlementAcctYesTodayMap.get(ACCTBAL);//存款余额
        BigDecimal settlementAvlbalYesToday = settlementAcctYesTodayMap.get(AVLBAL);//可用余额

        //今日余额合计
        resultVo.setDeposit(resultVo.getBankDeposit().add(resultVo.getCompanyDeposit()).add(resultVo.getSettleCenterDeposit()));
        //今日可用余额合计
        resultVo.setAvaDeposit(resultVo.getAvaBankDeposit().add(resultVo.getAvaCompanyDeposit()).add(resultVo.getAvaSettleCenterDeposit()));
        //昨日余额合计
        BigDecimal yesBalSum = bankAcctbalYesToday.add(financeAcctbalYesToday).add(settlementAcctbalYesToday);
        //昨日可用余额合计
        BigDecimal yesAvaBalSum = bankAvlbalYesToday.add(financeAvlbalYesToday).add(settlementAvlbalYesToday);
        //提升比例计算(今日-昨日 / 昨日)
        if(yesBalSum.compareTo(BigDecimal.ZERO) == 0){
            resultVo.setDepositQoq(new BigDecimal(0));
        }else{
            resultVo.setDepositQoq(new BigDecimal(String.valueOf(resultVo.getDeposit().subtract(yesBalSum).divide(yesBalSum,moneyDigit+2,BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)))));
        }

        if(yesAvaBalSum.compareTo(BigDecimal.ZERO) == 0){
            resultVo.setAvaDepositQoq(new BigDecimal(0));
        }else{
            resultVo.setAvaDepositQoq(new BigDecimal(String.valueOf(resultVo.getAvaDeposit().subtract(yesAvaBalSum).divide(yesAvaBalSum,moneyDigit+2,BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)))));
        }
        //上述计算完成后 统一处理精度 和 单位转化(万元 亿元)
        increaseMoneyDigit(resultVo,moneyDigit,settleWorkbenchBaseRequest.getAmountunit());
        return resultVo;
    }

    /**
     * 计算余额
     * @param bankAcctSumList  账户余额集合
     * @param date             当前余额日期(用于汇率查询)
     * @param convertRatetype  汇率类型
     * @param convertCurrency  折算币种
     * @return
     */
    private Map<String,BigDecimal> computeBalance(List<Map<String, Object>> bankAcctSumList,Date date,String convertRatetype,String convertCurrency,String ytenantid) throws Exception {
        BigDecimal bankAcctbalToday = BigDecimal.ZERO;//存款余额
        BigDecimal bankAvlbalSumToday = BigDecimal.ZERO;//可用余额
        if(bankAcctSumList!=null){
            for(Map<String, Object> map:bankAcctSumList){
                if(convertCurrency.equals(map.get("currency").toString())){//若余额币种和折算币种一致 直接加合
                    bankAcctbalToday = bankAcctbalToday.add(map.get("acctbal")!=null?new BigDecimal(map.get("acctbal").toString()):BigDecimal.ZERO);
                    bankAvlbalSumToday = bankAvlbalSumToday.add(map.get("avlbal")!=null?new BigDecimal(map.get("avlbal").toString()):BigDecimal.ZERO);
                }else{//若余额币种和折算币种不是一致 需要转化

                    //查询当前币种 与 目的币种的汇率 先从缓存中取
                    String key = map.get("currency").toString()+convertCurrency+date.toString()+ytenantid;
                    BigDecimal exchangerate = null;
                    if(exchangerate==null){
                        ExchangeRate exchangeRateVo = baseRefRpcService.queryRateByExchangeType(map.get("currency").toString(),convertCurrency,  date, convertRatetype);
                        exchangerate = new BigDecimal(0);
                        if(exchangeRateVo!=null && exchangeRateVo.getExchangerate()!=null){
                            exchangerate = BigDecimal.valueOf(exchangeRateVo.getExchangerate());
                        }
//                        exchangerateCache.put(key,exchangerate);
                    }
                    bankAcctbalToday = bankAcctbalToday.add
                            ((map.get("acctbal")!=null?new BigDecimal(map.get("acctbal").toString()):BigDecimal.ZERO).multiply(exchangerate));
                    bankAvlbalSumToday = bankAvlbalSumToday.add
                            ((map.get("avlbal")!=null?new BigDecimal(map.get("avlbal").toString()):BigDecimal.ZERO).multiply(exchangerate));
                }
            }
        }
        Map<String,BigDecimal> bankAcctBalance = new HashMap<>();
        bankAcctBalance.put(ACCTBAL,bankAcctbalToday);
        bankAcctBalance.put(AVLBAL,bankAvlbalSumToday);
        return bankAcctBalance;
    }

    private List<Map<String, Object>> queryRealtimeBalanceSumList(List<String> acctList,Date date) throws Exception {
        if(CollectionUtils.isEmpty(acctList)){
            return null;
        }
        QuerySchema querySchemaRealtimeBalance = QuerySchema.create().addSelect
                ("sum(acctbal) as acctbal,sum(avlbal) as avlbal,currency,enterpriseBankAccount");
        QueryConditionGroup condition = buildBalanceSumQueryCondition(acctList, date);
        querySchemaRealtimeBalance.addCondition(condition);
        querySchemaRealtimeBalance.addGroupBy("currency,enterpriseBankAccount");
        List<Map<String, Object>> realtimeBalanceSumList = MetaDaoHelper.query(AccountRealtimeBalance.ENTITY_NAME, querySchemaRealtimeBalance);
        return realtimeBalanceSumList;
    }

    private QueryConditionGroup buildBalanceSumQueryCondition(List<String> acctList,Date date){
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").in(acctList)));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("balancedate").eq(date)));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("first_flag").eq(0)));
        return condition;
    }

    private void increaseMoneyDigit(CashQueryBalanceWidgetsForStwbVo resultVo,Short moneyDigit,String amountunit /* 金额单位 元、万元、亿元 */) throws IllegalAccessException {
        BigDecimal unit = new BigDecimal(1);
        if(amountunit.equals("2")){//万元
            unit = new BigDecimal(10000);
        }else if(amountunit.equals("3")){//亿元
            unit = new BigDecimal(100000000);
        }
        Field[] fields = CashQueryBalanceWidgetsForStwbVo.class.getDeclaredFields();
        List<Field> bigDecimalFields = new ArrayList<>();

        for (Field field : fields) {
            if (field.getType().equals(BigDecimal.class)) {
                bigDecimalFields.add(field);
            }
        }
        for (Field field : bigDecimalFields) {
            field.setAccessible(true);
            BigDecimal value = (BigDecimal) field.get(resultVo);
            //进度确认为2
            BigDecimal newValue = BigDecimal.ZERO;
            if("depositQoq".equals(field.getName()) || "avaDepositQoq".equals(field.getName())){
                newValue = value.setScale(2, BigDecimal.ROUND_HALF_UP);
            }else{
                newValue = value.divide(unit, 2, BigDecimal.ROUND_HALF_UP);
            }
            field.set(resultVo, newValue);
        }
    }

}
