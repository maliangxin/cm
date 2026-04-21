package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.BizException;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;

import java.math.BigDecimal;
import java.util.*;

public class DailyComputeInit {

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
    public static Map<String, SettlementDetail> imitateDailyCompute(String accentity,
                                                                    String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date endDate)
            throws Exception {
        Map<String, SettlementDetail> settlementDetailMap = new HashMap<String, SettlementDetail>();
        QueryConditionGroup conditionMin = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);

        Date minDateUnSettlement = null;
        if(minDateUnSettlement == null){
            try {
                minDateUnSettlement = DailyComputeUtils.queryOrgPeriodBeginDate(accentity);// 会计期间启用日
            } catch (Exception e) {
                List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{accentity}));
                Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100741"),
                        String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
            }
        }
        Date maxDateSettlement = null;
        //期初
        if(minDateUnSettlement != null && endDate != null && minDateUnSettlement.compareTo(endDate) >= 0){
            getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, endDate, settlementDetailMap);
        }else{
            //先查询出来最大已结账日   period   2019-01-06
            QuerySchema querySchemaSettlement = QuerySchema.create().addSelect("max(settlementdate)");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                    QueryCondition.name("settleflag").eq(true));
            querySchemaSettlement.addCondition(group);
            Map<String,Object> map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchemaSettlement);
            if (MapUtils.isNotEmpty(map)) {
                maxDateSettlement = (Date) map.get("max");
                QueryConditionGroup conditionMinMax = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
                conditionMinMax.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(maxDateSettlement)));
                QuerySchema querySchemaMax = QuerySchema.create().addSelect("*");
                querySchemaMax.addCondition(conditionMinMax);
                List<Map<String, Object>> queryMax = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchemaMax);
                if(CollectionUtils.isNotEmpty(queryMax)){
                    for(Map<String,Object> mapM : queryMax){
                        SettlementDetail settlementDetail = new SettlementDetail();
                        settlementDetail.init(mapM);
                        settlementDetail.setTodaycreditorimoneysum(BigDecimal.ZERO);
                        settlementDetail.setTodaycreditlocalmoneysum(BigDecimal.ZERO);
                        settlementDetail.setTodaydebitorimoneysum(BigDecimal.ZERO);
                        settlementDetail.setTodaydebitlocalmoneysum(BigDecimal.ZERO);
                        settlementDetail.setTodaydebitnum(BigDecimal.ZERO);
                        settlementDetail.setTodaycreditnum(BigDecimal.ZERO);
                        String key = settlementDetail.getAccentity() + settlementDetail.getBankaccount() + settlementDetail.getCashaccount();
                        settlementDetailMap.put(key.replace("null",""),settlementDetail);
                    }
                }else{
                    getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, endDate, settlementDetailMap);
                }
            } else {
                getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, endDate, settlementDetailMap);
            }
        }

        if(maxDateSettlement != null){
            QuerySchema queryJournalForXZ = QuerySchema.create().addSelect("*");
            QuerySchema queryJournalForXZ_L = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionJournalXZ = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
            QueryConditionGroup conditionJournalXZ_L = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
            //查询未结算数据
            conditionJournalXZ.addCondition(QueryCondition.name("settlestatus").eq(1));
            conditionJournalXZ.addCondition(QueryCondition.name("initflag").eq(0));
            conditionJournalXZ.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").elt(maxDateSettlement)));

            conditionJournalXZ_L.addCondition(QueryCondition.name("settlestatus").eq(1));
            conditionJournalXZ_L.addCondition(QueryCondition.name("initflag").eq(0));
            conditionJournalXZ_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").elt(maxDateSettlement)));
            conditionJournalXZ_L.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").is_null()));
            queryJournalForXZ.addCondition(conditionJournalXZ);
            queryJournalForXZ_L.addCondition(conditionJournalXZ_L);
            List<Map<String, Object>> queryJournalXZ = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournalForXZ);
            List<Map<String, Object>> queryJournalXZ_L = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournalForXZ_L);
            if (queryJournalXZ!=null&&queryJournalXZ_L!=null){
                queryJournalXZ.addAll(queryJournalXZ_L);
            }
            for (Map<String, Object> mapJ : queryJournalXZ) {
                Journal journal = new Journal();
                journal.init(mapJ);
                String key = journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount();
                key = key.replace("null", "");
                SettlementDetail settlementDetail = settlementDetailMap.get(key);
                if (settlementDetail != null) {
                    // 修正原则：反向  昨日+借-贷
                    // 修正今日
                    if (endDate != null && (DateUtils.dateToStr(endDate).equals(DateUtils.dateToStr(journal.getDzdate()==null?journal.getVouchdate():journal.getDzdate())))) {
                        //设置今日贷方金额  今日贷-日记账的贷
                        settlementDetail.setTodaycreditorimoneysum(BigDecimalUtils
                                .safeAdd(settlementDetail.getTodaycreditorimoneysum(),journal.getCreditoriSum()));
                        settlementDetail.setTodaycreditlocalmoneysum(BigDecimalUtils
                                .safeAdd(settlementDetail.getTodaycreditlocalmoneysum(),journal.getCreditnatSum()));
                        settlementDetail.setTodaydebitorimoneysum(BigDecimalUtils
                                .safeAdd(settlementDetail.getTodaydebitorimoneysum(),journal.getDebitoriSum()));
                        settlementDetail.setTodaydebitlocalmoneysum(BigDecimalUtils
                                .safeAdd(settlementDetail.getTodaydebitlocalmoneysum(),journal.getDebitnatSum()));
                        if (journal.getDirection().getValue() == 1) {
                            settlementDetail.setTodaydebitnum(
                                    BigDecimalUtils.safeAdd(settlementDetail.getTodaydebitnum(), BigDecimal.ONE));
                        }
                        if (journal.getDirection().getValue() == 2) {
                            settlementDetail.setTodaycreditnum(
                                    BigDecimalUtils.safeAdd(settlementDetail.getTodaycreditnum(), BigDecimal.ONE));
                        }
                    } else {
                        //修正昨日  昨日原币+借-贷
                        settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeAdd(BigDecimalUtils
                                        .safeSubtract(settlementDetail.getYesterdayorimoney(), journal.getCreditoriSum()),
                                journal.getDebitoriSum()));
                        settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeAdd(BigDecimalUtils
                                        .safeSubtract(settlementDetail.getYesterdaylocalmoney(), journal.getCreditnatSum()),
                                journal.getDebitnatSum()));
                    }
                    // 修正今日
                    settlementDetail.setTodayorimoney(BigDecimalUtils.safeAdd(
                            BigDecimalUtils.safeSubtract(settlementDetail.getTodayorimoney(),
                                    journal.getCreditoriSum()),journal.getDebitoriSum()));
                    settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeAdd(
                            BigDecimalUtils.safeSubtract(settlementDetail.getTodaylocalmoney(),
                                    journal.getCreditnatSum()),journal.getDebitnatSum()));
                }
            }
        }

        // 统计未日结的借方金额与贷方金额
        QueryConditionGroup conditionJournal = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        QueryConditionGroup conditionJournal_L = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        if(maxDateSettlement == null){

        }else{
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").egt(DateUtils.dateAddDays(maxDateSettlement,1))));
            conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").egt(DateUtils.dateAddDays(maxDateSettlement,1))));
            conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").is_null()));
        }
        conditionJournal.addCondition(QueryCondition.name("initflag").eq(0));
        QuerySchema querySchemaJournal = QuerySchema.create().addSelect("*");
        querySchemaJournal.addCondition(conditionJournal);
        querySchemaJournal.addOrderBy(new QueryOrderby("id", "asc"));
        List<Map<String, Object>> queryJournal = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaJournal);


        conditionJournal_L.addCondition(QueryCondition.name("initflag").eq(0));
        QuerySchema querySchemaJournal_L = QuerySchema.create().addSelect("*");
        querySchemaJournal_L.addCondition(conditionJournal_L);
        querySchemaJournal_L.addOrderBy(new QueryOrderby("id", "asc"));
        List<Map<String, Object>> queryJournal_L = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaJournal_L);
        if (queryJournal!=null&&queryJournal_L!=null){
            queryJournal.addAll(queryJournal_L);
        }
        // 数据加工
        List<Map<String, Object>> queryNew = new ArrayList<Map<String, Object>>();
        BigDecimal sumOribalance = BigDecimal.ZERO;
        BigDecimal sumNatbalance = BigDecimal.ZERO;
        // 包含未审批，包含未结算逻辑处理，余额重算
        for (Map<String, Object> rebuildMap : queryJournal) {
            // 审批状态是未审批
            if (rebuildMap.get("auditstatus") != null && (Integer) rebuildMap.get("auditstatus") == 2) {
                // 包含未审批
                if (null != auditstatus && "2".equals(auditstatus)) {
                    sumOribalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(sumOribalance,
                            (BigDecimal) rebuildMap.get("debitoriSum"), (BigDecimal) rebuildMap.get("creditoriSum")));
                    sumNatbalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(sumNatbalance,
                            (BigDecimal) rebuildMap.get("debitnatSum"), (BigDecimal) rebuildMap.get("creditnatSum")));
                    continue;
                }
            }
            if (rebuildMap.get("settlestatus") != null && (Integer) rebuildMap.get("settlestatus") == 1) {
                // 包含未结算
                if (null != issettle && "2".equals(issettle)) {
                    sumOribalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(sumOribalance,
                            (BigDecimal) rebuildMap.get("debitoriSum"), (BigDecimal) rebuildMap.get("creditoriSum")));
                    sumNatbalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(sumNatbalance,
                            (BigDecimal) rebuildMap.get("debitnatSum"), (BigDecimal) rebuildMap.get("creditnatSum")));
                    continue;
                }
            }
            // 重算包含未审批，包含未结算后面的金额
            if (sumOribalance.compareTo(BigDecimal.ZERO) > 0) {
                rebuildMap.put("oribalance",
                        BigDecimalUtils.safeSubtract((BigDecimal) rebuildMap.get("oribalance"), sumOribalance));
                rebuildMap.put("natbalance",
                        BigDecimalUtils.safeSubtract((BigDecimal) rebuildMap.get("natbalance"), sumNatbalance));
            }
            queryNew.add(rebuildMap);
        }
        // 替换数据加工后的结果集
        queryJournal = queryNew;
        for (Map<String, Object> mapJ : queryJournal) {
            Journal journal = new Journal();
            journal.init(mapJ);
            String key = journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount();
            key = key.replace("null", "");
            SettlementDetail settlementDetail = settlementDetailMap.get(key);
            if (settlementDetail != null) {
                // 统计今日
                if (endDate != null && DateUtils.dateToStr(endDate).equals(DateUtils.dateToStr(journal.getDzdate()))) {
                    settlementDetail.setTodaycreditorimoneysum(BigDecimalUtils
                            .safeAddObj(settlementDetail.getTodaycreditorimoneysum(), journal.getCreditoriSum()));
                    settlementDetail.setTodaycreditlocalmoneysum(BigDecimalUtils
                            .safeAddObj(settlementDetail.getTodaycreditlocalmoneysum(), journal.getCreditnatSum()));
                    settlementDetail.setTodaydebitorimoneysum(BigDecimalUtils
                            .safeAddObj(settlementDetail.getTodaydebitorimoneysum(), journal.getDebitoriSum()));
                    settlementDetail.setTodaydebitlocalmoneysum(BigDecimalUtils
                            .safeAddObj(settlementDetail.getTodaydebitlocalmoneysum(), journal.getDebitnatSum()));
                    if (journal.getDirection().getValue() == 1) {
                        settlementDetail.setTodaydebitnum(
                                BigDecimalUtils.safeAddObj(settlementDetail.getTodaydebitnum(), BigDecimal.ONE));
                    }
                    if (journal.getDirection().getValue() == 2) {
                        settlementDetail.setTodaycreditnum(
                                BigDecimalUtils.safeAddObj(settlementDetail.getTodaycreditnum(), BigDecimal.ONE));
                    }
                }
                //统计余额
                if(endDate == null){
                    //如果日期为空，则为最大值，都是昨日，昨日与余额一致
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(
                            BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(),
                                    journal.getDebitoriSum()),journal.getCreditoriSum()));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(
                            BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(),
                                    journal.getDebitnatSum()),journal.getCreditnatSum()));
                    settlementDetail.setTodayorimoney(settlementDetail.getYesterdayorimoney());
                    settlementDetail.setTodaylocalmoney(settlementDetail.getYesterdaylocalmoney());
                }
            } else {
                settlementDetail = new SettlementDetail();
                settlementDetail.setAccentity(journal.getAccentity());
                settlementDetail.setBankaccount(journal.getBankaccount());
                settlementDetail.setCashaccount(journal.getCashaccount());
                settlementDetail.setAccountdate(endDate);
                settlementDetail.setCurrency(journal.getCurrency());
                settlementDetail.setTenant(AppContext.getTenantId());
                if (journal.getDirection().getValue() == 1) {
                    settlementDetail.setTodaydebitnum(BigDecimal.ONE);
                } else {
                    settlementDetail.setTodaydebitnum(BigDecimal.ZERO);
                }
                if (journal.getDirection().getValue() == 2) {
                    settlementDetail.setTodaycreditnum(BigDecimal.ONE);
                } else {
                    settlementDetail.setTodaycreditnum(BigDecimal.ZERO);
                }
                if(endDate == null){
                    //如果日期为空，则为最大值，都是昨日，昨日与余额一致
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(journal.getDebitoriSum(),journal.getCreditoriSum()));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(journal.getDebitnatSum(),journal.getCreditnatSum()));
                    settlementDetail.setTodayorimoney(settlementDetail.getYesterdayorimoney());
                    settlementDetail.setTodaylocalmoney(settlementDetail.getYesterdaylocalmoney());
                }
                settlementDetailMap.put(key, settlementDetail);
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
            String key = initData.getAccentity() + initData.getBankaccount() + initData.getCashaccount();
            key = key.replace("null", "");
            SettlementDetail settlementDetail = new SettlementDetail();
            settlementDetail.setAccentity(initData.getAccentity());
            settlementDetail.setBankaccount(initData.getBankaccount());
            settlementDetail.setCashaccount(initData.getCashaccount());
            settlementDetail.setAccountdate(endDate);
            settlementDetail.setCurrency(initData.getCurrency());
            settlementDetail.setTenant(AppContext.getTenantId());
            // 统计今日
            settlementDetail.setTodaycreditorimoneysum(BigDecimal.ZERO);
            settlementDetail.setTodaycreditlocalmoneysum(BigDecimal.ZERO);
            settlementDetail.setTodaydebitorimoneysum(BigDecimal.ZERO);
            settlementDetail.setTodaydebitlocalmoneysum(BigDecimal.ZERO);
            settlementDetail.setTodaydebitnum(BigDecimal.ZERO);
            settlementDetail.setTodaycreditnum(BigDecimal.ZERO);
            settlementDetail.setYesterdayorimoney(initData.getCoinitloribalance());
            settlementDetail.setYesterdaylocalmoney(initData.getCoinitlocalbalance());
            settlementDetail.setTodayorimoney(initData.getCoinitloribalance());
            settlementDetail.setTodaylocalmoney(initData.getCoinitlocalbalance());
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
        if (!StringUtils.isEmpty(accentity)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity)));
        }
        if (!StringUtils.isEmpty(currency) ) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").in(currency.split(","))));
        }
        return condition;
    }

}
