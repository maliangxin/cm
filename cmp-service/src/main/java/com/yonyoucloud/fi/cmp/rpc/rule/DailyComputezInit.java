package com.yonyoucloud.fi.cmp.rpc.rule;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

@Slf4j
public class DailyComputezInit {

    private static String DAILYCOMPUTEZINITMAPPER = "com.yonyoucloud.fi.cmp.mapper.DailyComputezInitMapper.";

    /**
     * 模拟日结
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param auditstatus
     * @param issettle
     * @param endDate 基准日期
     * @return
     * @throws Exception
     */
    public static Map<String, SettlementDetail> imitateDailyComputeInit(String accentity,
                                                                    String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date endDate)
            throws Exception {
        Map<String, SettlementDetail> settlementDetailMap = new HashMap<String, SettlementDetail>();
        try {
            // 这块只是查一下么 做什么用？
                QueryBaseDocUtils.queryOrgPeriodBeginDate(accentity);// 会计期间启用日
        } catch (Exception e) {
//                throw new BizException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180832","未能获取到资金组织对应的会计期间！"));
            List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{accentity}));
            Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101819"),
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101819"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050004", "未能获取到资金组织对应的会计期间！") );
        }
        getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, endDate, settlementDetailMap);
        log.error("DailyComputezInit settlementDetailMap:"+settlementDetailMap);
            Map param = getDailyComputezInitMapperParam( accentity,  currency,  cashaccount,
                     bankaccount, auditstatus, issettle, endDate, true);
            List<Map<String, Object>>  listJournalCount = SqlHelper.selectList(DAILYCOMPUTEZINITMAPPER + "getJournalListForCount", param);
            log.error("DailyComputezInit listJournalCount:"+listJournalCount);
            for (Map<String, Object> mapJ : listJournalCount) {
                Journal journal = new Journal();
                journal.init(mapJ);
                String key = journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
                key = key.replace("null", "");
                SettlementDetail settlementDetail = settlementDetailMap.get(key);
                if (settlementDetail != null) {
                    //统计余额
                    //  昨日原币余额 = (昨日原币余额+借方原币金额) - 贷方原币金额
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(
                                BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(),
                                        new BigDecimal(journal.get("debitoriSum").toString())),new BigDecimal(journal.get("creditoriSum").toString())));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(
                                BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(),
                                        new BigDecimal(journal.get("debitnatSum").toString())),new BigDecimal(journal.get("creditnatSum").toString())));
                    settlementDetail.setTodayorimoney(BigDecimalUtils.safeSubtract(
                                BigDecimalUtils.safeAddObj(settlementDetail.getTodayorimoney(),
                                        new BigDecimal(journal.get("debitoriSum").toString())),new BigDecimal(journal.get("creditoriSum").toString())));
                    settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeSubtract(
                                BigDecimalUtils.safeAddObj(settlementDetail.getTodaylocalmoney(),
                                        new BigDecimal(journal.get("debitnatSum").toString())),new BigDecimal(journal.get("creditnatSum").toString())));

                } else {
                    settlementDetail = new SettlementDetail();
                    settlementDetail.setAccentity(journal.getAccentity());
                    settlementDetail.setBankaccount(journal.getBankaccount());
                    settlementDetail.setCashaccount(journal.getCashaccount());
                    settlementDetail.setAccountdate(endDate);
                    settlementDetail.setCurrency(journal.getCurrency());
                    settlementDetail.setTenant(AppContext.getTenantId());
                    if(endDate == null){
                        //如果日期为空，则为最大值，都是昨日，昨日与余额一致
                        settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(journal.getDebitoriSum(),journal.getCreditoriSum()));
                        settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(journal.getDebitnatSum(),journal.getCreditnatSum()));
                        settlementDetail.setTodayorimoney(BigDecimalUtils.safeSubtract(
                                BigDecimalUtils.safeAddObj(settlementDetail.getTodayorimoney(),
                                        new BigDecimal(journal.get("debitoriSum").toString())),new BigDecimal(journal.get("creditoriSum").toString())));
                        settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeSubtract(
                                BigDecimalUtils.safeAddObj(settlementDetail.getTodaylocalmoney(),
                                        new BigDecimal(journal.get("debitnatSum").toString())),new BigDecimal(journal.get("creditnatSum").toString())));
                    }
                    settlementDetailMap.put(key, settlementDetail);
                }
            }

        return settlementDetailMap;
    }

    public static Map<String, SettlementDetail> imitateDailyComputeInitNew(String accentity,
                                                                           ArrayList<String> currency, ArrayList<String> cashaccount, ArrayList<String> bankaccount, String auditstatus, String issettle, Date endDate)
            throws Exception {
        Map<String, SettlementDetail> settlementDetailMap = new HashMap<>();
        try {
            // 这块只是查一下么 做什么用？
            QueryBaseDocUtils.queryOrgPeriodBeginDate(accentity);// 会计期间启用日
        } catch (Exception e) {
//            throw new BizException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180832","未能获取到资金组织对应的会计期间！"));
            List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{accentity}));
            Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101819"),
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F4707EC04380008", "未能获取到组织[%]现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织[%]现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
        }
        getSettleMentDetailFromInitDataNew(accentity, currency, cashaccount, bankaccount, endDate, settlementDetailMap);

        log.error("DailyComputezInit settlementDetailMap:"+settlementDetailMap);
        Map param = getDailyComputezInitMapperParamNew( accentity,  currency,  cashaccount,
                bankaccount, auditstatus, issettle, endDate, true);
        // todo 银行账户现金账户分批
        List<Map<String, Object>>  listJournalCount = SqlHelper.selectList(DAILYCOMPUTEZINITMAPPER + "getJournalListForCountNew", param);
        log.error("DailyComputezInit listJournalCount:"+listJournalCount);
        if (CollectionUtils.isNotEmpty(listJournalCount)) {
            for (Map<String, Object> mapJ : listJournalCount) {
                Journal journal = new Journal();
                journal.init(mapJ);
                String key = journal.get("accentity_raw")+ journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
                key = key.replace("null", "");
                SettlementDetail settlementDetail = settlementDetailMap.get(key);
                if (settlementDetail != null) {
                    //统计余额
                    //  昨日原币余额 = (昨日原币余额+借方原币金额) - 贷方原币金额
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(
                            BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(),
                                    new BigDecimal(journal.get("debitoriSum").toString())),new BigDecimal(journal.get("creditoriSum").toString())));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(
                            BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(),
                                    new BigDecimal(journal.get("debitnatSum").toString())),new BigDecimal(journal.get("creditnatSum").toString())));
                    settlementDetail.setTodayorimoney(BigDecimalUtils.safeSubtract(
                            BigDecimalUtils.safeAddObj(settlementDetail.getTodayorimoney(),
                                    new BigDecimal(journal.get("debitoriSum").toString())),new BigDecimal(journal.get("creditoriSum").toString())));
                    settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeSubtract(
                            BigDecimalUtils.safeAddObj(settlementDetail.getTodaylocalmoney(),
                                    new BigDecimal(journal.get("debitnatSum").toString())),new BigDecimal(journal.get("creditnatSum").toString())));

                } else {
                    settlementDetail = new SettlementDetail();
                    settlementDetail.setAccentity(journal.get("accentity_raw"));
                    settlementDetail.setBankaccount(journal.getBankaccount());
                    settlementDetail.setCashaccount(journal.getCashaccount());
                    settlementDetail.setAccountdate(endDate);
                    settlementDetail.setCurrency(journal.getCurrency());
                    settlementDetail.setTenant(AppContext.getTenantId());
                    settlementDetailMap.put(key, settlementDetail);
                }
            }
        }
        return settlementDetailMap;
    }

    /**
     * 从期初开始计算模拟日结数据
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param endDate
     * @param settlementDetailMap
     * @throws Exception
     */
    private static void getSettleMentDetailFromInitData(String accentity, String currency, String cashaccount, String bankaccount, Date endDate, Map<String, SettlementDetail> settlementDetailMap) throws Exception {
        QueryConditionGroup initDataCondition = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        QuerySchema initDataQuerySchema = QuerySchema.create().addSelect("*");
        initDataQuerySchema.addCondition(initDataCondition);
        List<Map<String, Object>> initDataQuery = MetaDaoHelper.query(InitData.ENTITY_NAME, initDataQuerySchema);
        for (Map<String, Object> initmap : initDataQuery) {
            InitData initData = new InitData();
            initData.init(initmap);
            String key = initData.getAccentity() + initData.getBankaccount() + initData.getCashaccount() + initData.getCurrency();
            key = key.replace("null", "");
            SettlementDetail settlementDetail = new SettlementDetail();
            settlementDetail.setAccentity(initData.getAccentity());
            settlementDetail.setBankaccount(initData.getBankaccount());
            settlementDetail.setCashaccount(initData.getCashaccount());
            settlementDetail.setAccountdate(endDate);
            settlementDetail.setCurrency(initData.getCurrency());
            settlementDetail.setTenant(AppContext.getTenantId());
            // 统计今日
            settlementDetail.setTodaycreditorimoneysum(BigDecimal.ZERO);//今日贷方原币合计
            settlementDetail.setTodaycreditlocalmoneysum(BigDecimal.ZERO);//今日贷方本币合计
            settlementDetail.setTodaydebitorimoneysum(BigDecimal.ZERO);//今日借方原币合计
            settlementDetail.setTodaydebitlocalmoneysum(BigDecimal.ZERO);//今日贷方原币合计
            settlementDetail.setTodaydebitnum(BigDecimal.ZERO);//借方笔数
            settlementDetail.setTodaycreditnum(BigDecimal.ZERO);//贷方笔数
            settlementDetail.setYesterdayorimoney(initData.getCoinitloribalance());//昨日原币余额
            settlementDetail.setYesterdaylocalmoney(initData.getCoinitlocalbalance());//昨日本币余额
            settlementDetail.setTodayorimoney(initData.getCoinitloribalance());//今日原币余额
            settlementDetail.setTodaylocalmoney(initData.getCoinitlocalbalance());//今日本币余额
            settlementDetailMap.put(key, settlementDetail);
        }
    }


    /**
     * 从期初开始计算模拟日结数据new
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param endDate
     * @param settlementDetailMap
     * @throws Exception
     */
    private static void getSettleMentDetailFromInitDataNew(String accentity, ArrayList<String> currency, ArrayList<String> cashaccount, ArrayList<String> bankaccount, Date endDate, Map<String, SettlementDetail> settlementDetailMap) throws Exception {
        Map initParam = getInitListMapperParam(accentity, currency, cashaccount, bankaccount, endDate, true);
        List<Map<String, Object>> initDataQuery = SqlHelper.selectList(DAILYCOMPUTEZINITMAPPER + "getInitList", initParam);
        log.error("查询到的期初汇总数据initDataQuery{}", initDataQuery);
        for (Map<String, Object> initmap : initDataQuery) {
            InitData initData = new InitData();
            initData.init(initmap);
            String key = initData.get("accentity_raw") + initData.getBankaccount() + initData.getCashaccount() + initData.getCurrency();
            key = key.replace("null", "");
            SettlementDetail settlementDetail = new SettlementDetail();
            settlementDetail.setAccentity(initData.get("accentity_raw"));
            settlementDetail.setBankaccount(initData.getBankaccount());
            settlementDetail.setCashaccount(initData.getCashaccount());
            settlementDetail.setAccountdate(endDate);
            settlementDetail.setCurrency(initData.getCurrency());
            settlementDetail.setTenant(AppContext.getTenantId());
            // 统计今日
            settlementDetail.setTodaycreditorimoneysum(BigDecimal.ZERO);//今日贷方原币合计
            settlementDetail.setTodaycreditlocalmoneysum(BigDecimal.ZERO);//今日贷方本币合计
            settlementDetail.setTodaydebitorimoneysum(BigDecimal.ZERO);//今日借方原币合计
            settlementDetail.setTodaydebitlocalmoneysum(BigDecimal.ZERO);//今日贷方原币合计
            settlementDetail.setTodaydebitnum(BigDecimal.ZERO);//借方笔数
            settlementDetail.setTodaycreditnum(BigDecimal.ZERO);//贷方笔数
            settlementDetail.setYesterdayorimoney(initData.getCoinitloribalance());//昨日原币余额
            settlementDetail.setYesterdaylocalmoney(initData.getCoinitlocalbalance());//昨日本币余额
            settlementDetail.setTodayorimoney(initData.getCoinitloribalance());//今日原币余额
            settlementDetail.setTodaylocalmoney(initData.getCoinitlocalbalance());//今日本币余额
            settlementDetailMap.put(key, settlementDetail);
        }
    }

    /**
     * 查询条件封装
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @return
     */
    private static QueryConditionGroup getQueryConditionGroup(String accentity, String currency, String cashaccount,
                                                              String bankaccount) {
        QueryConditionGroup condition = new QueryConditionGroup();
        if (!StringUtils.isEmpty(accentity)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity)));
        }
        if (!StringUtils.isEmpty(cashaccount) && !StringUtils.isEmpty(bankaccount)) {
            condition.addCondition(QueryConditionGroup.or(QueryCondition.name("cashaccount").in(cashaccount.split(",")),
                    QueryCondition.name("bankaccount").in(bankaccount.split(","))));
        } else if (!StringUtils.isEmpty(cashaccount)) {
            condition.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("cashaccount").in(cashaccount.split(","))));
        } else if (!StringUtils.isEmpty(bankaccount)) {
            condition.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("bankaccount").in(bankaccount.split(","))));
        }
        if (!StringUtils.isEmpty(currency) ) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").in(currency.split(","))));
        }
        return condition;
    }

    private static QueryConditionGroup getQueryConditionGroupNew(String accentity, ArrayList<String> currency, ArrayList<String> cashaccount, ArrayList<String> bankaccount) {
        QueryConditionGroup condition = new QueryConditionGroup();
        if (!StringUtils.isEmpty(accentity)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentityRaw").eq(accentity)));
        }
        if (CollectionUtils.isNotEmpty(cashaccount)) {
            condition.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("cashaccount").in(cashaccount)));
        } else if (CollectionUtils.isNotEmpty(bankaccount)) {
            condition.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("bankaccount").in(bankaccount)));
        }
        if (CollectionUtils.isNotEmpty(currency) ) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").in(currency)));
        }
        return condition;
    }

    public static Map getDailyComputezInitMapperParam(String accentity, String currency, String cashaccount,
                                                              String bankaccount,String auditstatus,String settlestatus,Date endDate,Boolean isCount) {
        Map param = new HashMap();
        param.put("ytenantId",AppContext.getYTenantId());
        param.put("useAccentity",accentity);
        if(currency!=null && !"".equals(currency)){
            param.put("currencys",new ArrayList<String>(Arrays.asList(currency.split(","))));
        }
        if(cashaccount!=null && !"".equals(cashaccount)){
            param.put("cashaccounts",new ArrayList<String>(Arrays.asList(cashaccount.split(","))));
        }
        if(bankaccount!=null && !"".equals(bankaccount)){
            param.put("bankaccounts",new ArrayList<String>(Arrays.asList(bankaccount.split(","))));
        }
        param.put("auditstatus",auditstatus);
        param.put("settlestatus",settlestatus);
        if(isCount){
            param.put("endDate",endDate);
        }
        return param;
    }

    public static Map getDailyComputezInitMapperParamNew(String accentity, ArrayList<String> currency,ArrayList<String> cashaccount, ArrayList<String> bankaccount,String auditstatus,String settlestatus,Date endDate,Boolean isCount) throws ParseException {
        Map param = new HashMap();
        param.put("ytenantId",AppContext.getYTenantId());
        param.put("useAccentity",accentity);
        if (CollectionUtils.isNotEmpty(cashaccount)) {
            param.put("cashaccounts",cashaccount);
        }
        if (CollectionUtils.isNotEmpty(bankaccount)) {
            param.put("bankaccounts",bankaccount);
        }
        if(CollectionUtils.isNotEmpty(currency)){
            param.put("currencys",currency);
        }
        param.put("auditstatus",auditstatus);
        param.put("settlestatus",settlestatus);
        if(isCount){
            param.put("endDate", DateUtils.dateFormat(endDate, DateUtils.dateFormat(endDate, DateUtils.DATE_PATTERN)));
        }
        return param;
    }


    public static Map getInitListMapperParam(String accentity, ArrayList<String> currency,ArrayList<String> cashaccount, ArrayList<String> bankaccount,Date endDate,Boolean isCount) throws ParseException {
        Map param = new HashMap();
        param.put("ytenantId",AppContext.getYTenantId());
        param.put("accentityRaw",accentity);
        if (CollectionUtils.isNotEmpty(cashaccount)) {
            param.put("cashaccounts",cashaccount);
        }
        if (CollectionUtils.isNotEmpty(bankaccount)) {
            param.put("bankaccounts",bankaccount);
        }
        if(CollectionUtils.isNotEmpty(currency)){
            param.put("currencys",currency);
        }
        param.put("endDate", DateUtils.dateFormat(endDate, DateUtils.dateFormat(endDate, DateUtils.DATE_PATTERN)));
        return param;
    }
}
