package com.yonyoucloud.fi.cmp.util;

import cn.tca.TopBasicCrypto.cms.bc.BcRSAKeyTransRecipientInfoGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.cmpentity.DirectionJD;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 日结临时使用
 */
@Slf4j
public class DailySettleCompute {
    private static String DAILYCOMPUTEZINITMAPPER = "com.yonyoucloud.fi.cmp.mapper.DailyComputezInitMapper.";
    /**
     * 模拟日结重载方法
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param auditstatus
     * @param issettle
     * @param endDate
     * @return
     * @throws Exception
     */
    public static Map<String, SettlementDetail> imitateDailyCompute(String accentity, String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date endDate,boolean isSettle) throws Exception {
        return doImitateDailyCompute(accentity, currency, cashaccount, bankaccount, auditstatus, issettle, endDate, endDate, isSettle);
    }

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
    public static Map<String, SettlementDetail> doImitateDailyCompute(String accentity, String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date startDate, Date endDate,boolean isSettle) throws Exception {
        Map<String, SettlementDetail> settlementDetailMap = new HashMap();
        QueryConditionGroup conditionMin = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        List<Map<String, Object>> queryMin = null;
        if(startDate != null){
            //查询当前日期的日结明细数据，如果有startDate即为当前查询的最大结账日
            if(isSettle){
                Date  currDate = DateUtils.dateAdd(startDate,-1,false);
//                Date  currDate = DateUtils.preDay(startDate);
                conditionMin.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(currDate)));//TODO 应替换为开始日期
            }else{
                conditionMin.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(startDate)));//TODO 应替换为开始日期
            }
            QuerySchema querySchemaMin = QuerySchema.create().addSelect("*");
            querySchemaMin.addCondition(conditionMin);
            queryMin = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchemaMin);
        }
        Date minDateUnSettlement = null;
        if(minDateUnSettlement == null){
            try {
                // 会计期间启用日
                minDateUnSettlement = DailyComputeUtils.queryOrgPeriodBeginDate(accentity);
            } catch (Exception e) {
                List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{accentity}));
                Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100740"),
                        String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
            }
        }
        //多币种统计银行账户+币种信息
        List<String> currencyKeyList = new ArrayList<>();
        if(StringUtils.isEmpty(bankaccount) || StringUtils.isEmpty(currency)){
            currencyKeyList = getCurrencyKey(accentity);
        }
        //多币种
        Date maxDateSettlement = null;
        //期初
        if(minDateUnSettlement != null && startDate != null && minDateUnSettlement.compareTo(startDate) >= 0){
            // 会计期间启用日大于查询开始日期，直接取期初数据
            getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap,null);//TODO 应替换为开始日期
        }else if(queryMin == null || queryMin.size()==0){
            //当前日期没有日结明细数据，从期初开始推(当前资金组织有已日结数据，新建银行账户的业务单据日结，出现该问题)
            getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap,null);//TODO 应替换为开始日期
        }else{
            //先查询出来最大已结账日   period   2019-01-06    2019-10-10
            //先查询出来最大已结账日，如果大于startDate 则将startDate作为最大结账日
            QuerySchema querySchemaSettlement = QuerySchema.create().addSelect("max(settlementdate)");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity), QueryCondition.name("settleflag").eq(1));
            querySchemaSettlement.addCondition(group);
            Map<String,Object> map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchemaSettlement);
            if (MapUtils.isNotEmpty(map)) {
                maxDateSettlement = (Date) map.get("max");
                if(queryMin != null && queryMin.size() > 0){
                    if(isSettle) {
                        maxDateSettlement = DateUtils.dateAdd(startDate, -1, false);
                    }else{
                        maxDateSettlement = startDate;
                    }
                }
                //查询最大结账日的日结明细
                QueryConditionGroup conditionMinMax = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
                conditionMinMax.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(maxDateSettlement)));
                QuerySchema querySchemaMax = QuerySchema.create().addSelect("*");
                querySchemaMax.addCondition(conditionMinMax);
                List<Map<String, Object>> queryMax = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchemaMax);
                if(CollectionUtils.isNotEmpty(queryMax)){
                    //多币种深拷贝资金组织下银行账户+币种
                    List<String> newList = new ArrayList<>();
                    if(CollectionUtils.isNotEmpty(currencyKeyList)){
                        String[] stringArray = new String[currencyKeyList.size()];
                        currencyKeyList.toArray(stringArray);
                        CollectionUtils.addAll(newList, stringArray);
                    }
                    //多币种-----
                    for(Map<String,Object> mapM : queryMax){
                        SettlementDetail settlementDetail = new SettlementDetail();
                        settlementDetail.init(mapM);
//                        settlementDetail.setAccountdate(endDate);
                        if (startDate != null && DateUtils.dateToStr(startDate).equals(DateUtils.dateToStr(maxDateSettlement))) {

                        }else{
                            settlementDetail.setTodaycreditorimoneysum(BigDecimal.ZERO);
                            settlementDetail.setTodaycreditlocalmoneysum(BigDecimal.ZERO);
                            settlementDetail.setTodaydebitorimoneysum(BigDecimal.ZERO);
                            settlementDetail.setTodaydebitlocalmoneysum(BigDecimal.ZERO);
                            settlementDetail.setTodaydebitnum(BigDecimal.ZERO);
                            settlementDetail.setTodaycreditnum(BigDecimal.ZERO);
                        }
                        if(startDate != null && startDate.compareTo(maxDateSettlement) > 0){
                            //如果最大结账日在startDate之前将日结最大日结明细数据中的今日设置给昨日
                            settlementDetail.setYesterdayorimoney(settlementDetail.getTodayorimoney());
                            settlementDetail.setYesterdaylocalmoney(settlementDetail.getTodaylocalmoney());
                        }
                        String key = settlementDetail.getAccentity() + settlementDetail.getBankaccount() + settlementDetail.getCashaccount() + settlementDetail.getCurrency();
                        key = key.replace("null","");
                        settlementDetailMap.put(key,settlementDetail);
                        if(CollectionUtils.isNotEmpty(newList) && newList.contains(key)){
                            newList.remove(key.replace("null",""));
                        }
                    }
                    if(CollectionUtils.isNotEmpty(newList)){
                        getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap,newList);
                    }
                }else{
                    getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap,null);
                }
            } else {
                getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap,null);
            }
        }
        //逻辑已变：日结的金额不会包含未结算的数据，需要对包含的数据进行修正
        //把未结算的记录加进来进行基准点金额修正
        if (null!= issettle && "1".equals(issettle)&&maxDateSettlement!=null) {
            QuerySchema queryJournalForXZ = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionJournalXZ = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
            //查询小于等于最大结账日期的不是期初且未结算日记账数据
            conditionJournalXZ.addCondition(QueryCondition.name("settlestatus").eq(1));
            conditionJournalXZ.addCondition(QueryCondition.name("initflag").eq(0));
            QuerySchema queryJournalForXZ_L = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionJournalXZ_L = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
            conditionJournalXZ_L.addCondition(QueryCondition.name("settlestatus").eq(1));
            conditionJournalXZ_L.addCondition(QueryCondition.name("initflag").eq(0));

            if (null!= auditstatus && "2".equals(auditstatus)) {//不包含未审核
                conditionJournalXZ.addCondition(QueryCondition.name("auditstatus").eq(1));//1为已审核
                conditionJournalXZ_L.addCondition(QueryCondition.name("auditstatus").eq(1));
            }

            if(maxDateSettlement != null){
                conditionJournalXZ_L.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").is_null()));
                conditionJournalXZ.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").elt(maxDateSettlement)));
                conditionJournalXZ_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").elt(maxDateSettlement)));
            }else{
                //最大日结为空走的是期初数据
                if (startDate!=null){
                    conditionJournalXZ_L.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").is_null()));
                    conditionJournalXZ.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").elt(startDate)));
                    conditionJournalXZ_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").elt(startDate)));
                }
            }
            queryJournalForXZ.addCondition(conditionJournalXZ);
            queryJournalForXZ_L.addCondition(conditionJournalXZ_L);
            List<Map<String, Object>> queryJournalXZ = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournalForXZ);
            List<Map<String, Object>> queryJournalXZ_L = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournalForXZ_L);
            if (queryJournalXZ!=null&&queryJournalXZ_L!=null&&queryJournalXZ_L.size()>0){
                queryJournalXZ.addAll(queryJournalXZ_L);
            }
            for (Map<String, Object> mapJ : queryJournalXZ) {
                Journal journal = new Journal();
                journal.init(mapJ);
                String key = journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
                key = key.replace("null", "");
                SettlementDetail settlementDetail = settlementDetailMap.get(key);
                if (settlementDetail != null) {
                    // 修正原则：反向  昨日+借-贷
                    // 修正今日
                    Date journalDate = journal.getDzdate()==null?journal.getVouchdate():journal.getDzdate();
                    if (endDate != null && (startDate.equals(journalDate) || endDate.equals(journalDate) || (startDate.before(journalDate) && endDate.after(journalDate)))) {
                        //设置今日贷方金额  今日贷-日记账的贷
                        settlementDetail.setTodaycreditorimoneysum(BigDecimalUtils.safeAdd(settlementDetail.getTodaycreditorimoneysum(), journal.getCreditoriSum()));
                        settlementDetail.setTodaycreditlocalmoneysum(BigDecimalUtils.safeAdd(settlementDetail.getTodaycreditlocalmoneysum(), journal.getCreditnatSum()));
                        settlementDetail.setTodaydebitorimoneysum(BigDecimalUtils.safeAdd(settlementDetail.getTodaydebitorimoneysum(), journal.getDebitoriSum()));
                        settlementDetail.setTodaydebitlocalmoneysum(BigDecimalUtils.safeAdd(settlementDetail.getTodaydebitlocalmoneysum(), journal.getDebitnatSum()));
                        if (journal.getDirection().getValue() == 1) {
                            settlementDetail.setTodaydebitnum(BigDecimalUtils.safeAdd(settlementDetail.getTodaydebitnum(), BigDecimal.ONE));
                        }
                        if (journal.getDirection().getValue() == 2) {
                            settlementDetail.setTodaycreditnum(BigDecimalUtils.safeAdd(settlementDetail.getTodaycreditnum(), BigDecimal.ONE));
                        }
                    } else {
                        //修正昨日  昨日原币+借-贷
                        settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(settlementDetail.getYesterdayorimoney(), journal.getCreditoriSum()), journal.getDebitoriSum()));
                        settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(settlementDetail.getYesterdaylocalmoney(), journal.getCreditnatSum()), journal.getDebitnatSum()));
                    }
                    // 修正今日
                    settlementDetail.setTodayorimoney(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(settlementDetail.getTodayorimoney(), journal.getCreditoriSum()),journal.getDebitoriSum()));
                    settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(settlementDetail.getTodaylocalmoney(), journal.getCreditnatSum()),journal.getDebitnatSum()));
                }
            }
        }
        // 统计未日结的借方金额与贷方金额
        QueryConditionGroup conditionJournal = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        QueryConditionGroup conditionJournal_L = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        if(maxDateSettlement == null){//到endDate之前从未日结过，
            if(endDate != null){
                conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").is_null()));
                conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").elt(startDate)));
                conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").elt(startDate)));
            }
        }else{
            if(endDate != null){
                conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").is_null()));
                //条件：最大结账日<日期<=endDate的不是期初的所有数据（已日结的包含未结算的，未日结的包含未审批未结算）
                conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").between(DateUtils.dateAddDays(maxDateSettlement,1), startDate)));
                conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").between(DateUtils.dateAddDays(maxDateSettlement,1), startDate)));
            }else{//查询大于最大日结日期的数据
                conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").is_null()));
                conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").egt(DateUtils.dateAddDays(maxDateSettlement,1))));
                conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").egt(DateUtils.dateAddDays(maxDateSettlement,1))));
            }
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
        if (queryJournal!=null&&queryJournal_L!=null&&queryJournal_L.size()>0){
            queryJournal.addAll(queryJournal_L);
        }
        // 数据加工
        List<Map<String, Object>> queryNew = new ArrayList<Map<String, Object>>();
        BigDecimal sumOribalance = BigDecimal.ZERO;
        BigDecimal sumNatbalance = BigDecimal.ZERO;
        // 包含未审批，包含未结算逻辑处理，余额重算（当前存储的是最大结账日的日结明细，如果最大结账日不是endDate需要余额进行重算）
        for (Map<String, Object> rebuildMap : queryJournal) {
            // 审批状态是未审批
            if (rebuildMap.get("auditstatus") != null && ("2").equals(rebuildMap.get("auditstatus").toString())) {// 单据审批状态是未审批
                // 包含未审批
                if (null != auditstatus && "2".equals(auditstatus)) {//不包含未审批单据--->将未审批的单据+借-贷   // 报表选择不包含未审批
                    sumOribalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(sumOribalance, (BigDecimal) rebuildMap.get("debitoriSum"), (BigDecimal) rebuildMap.get("creditoriSum")));
                    sumNatbalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(sumNatbalance, (BigDecimal) rebuildMap.get("debitnatSum"), (BigDecimal) rebuildMap.get("creditnatSum")));
                    continue;
                }
            }
            if (rebuildMap.get("settlestatus") != null && (Integer) rebuildMap.get("settlestatus") == 1) {//单据为结算单据
                // 包含未结算
                if (null != issettle && "2".equals(issettle)) {// 报表选择不包含未结算
                    sumOribalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(sumOribalance, (BigDecimal) rebuildMap.get("debitoriSum"), (BigDecimal) rebuildMap.get("creditoriSum")));
                    sumNatbalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(sumNatbalance, (BigDecimal) rebuildMap.get("debitnatSum"), (BigDecimal) rebuildMap.get("creditnatSum")));
                    continue;
                }
            }
            // 重算包含未审批，包含未结算后面的金额
            if (sumOribalance.compareTo(BigDecimal.ZERO) > 0) {
                rebuildMap.put("oribalance", BigDecimalUtils.safeSubtract((BigDecimal) rebuildMap.get("oribalance"), sumOribalance));
                rebuildMap.put("natbalance", BigDecimalUtils.safeSubtract((BigDecimal) rebuildMap.get("natbalance"), sumNatbalance));
            }
            queryNew.add(rebuildMap);
        }
        // 替换数据加工后的结果集
        queryJournal = queryNew;
        for (Map<String, Object> mapJ : queryJournal) {
            Journal journal = new Journal();
            journal.init(mapJ);
            String key = journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
            key = key.replace("null", "");
            SettlementDetail settlementDetail = settlementDetailMap.get(key);
            if (settlementDetail != null) {
                Date journalDate = journal.getDzdate()==null?journal.getVouchdate():journal.getDzdate();
                if (endDate != null && (startDate.equals(journalDate) || endDate.equals(journalDate) || (startDate.before(journalDate) && endDate.after(journalDate)))) {
                    settlementDetail.setTodaycreditorimoneysum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaycreditorimoneysum(), journal.getCreditoriSum()));
                    settlementDetail.setTodaycreditlocalmoneysum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaycreditlocalmoneysum(), journal.getCreditnatSum()));
                    settlementDetail.setTodaydebitorimoneysum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaydebitorimoneysum(), journal.getDebitoriSum()));
                    settlementDetail.setTodaydebitlocalmoneysum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaydebitlocalmoneysum(), journal.getDebitnatSum()));
                    if (journal.getDirection().getValue() == 1) {
                        settlementDetail.setTodaydebitnum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaydebitnum(), BigDecimal.ONE));
                    }
                    if (journal.getDirection().getValue() == 2) {
                        settlementDetail.setTodaycreditnum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaycreditnum(), BigDecimal.ONE));
                    }
                }
                // 统计昨日
                if (endDate != null && !(startDate.equals(journalDate) || endDate.equals(journalDate) || (startDate.before(journalDate) && endDate.after(journalDate)))) {
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), journal.getDebitoriSum()), journal.getCreditoriSum()));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), journal.getDebitnatSum()), journal.getCreditnatSum()));
                }
                //统计余额
                if(endDate == null){
                    //如果日期为空，则为最大值，都是昨日，昨日与余额一致
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), journal.getDebitoriSum()), journal.getCreditoriSum()));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), journal.getDebitnatSum()), journal.getCreditnatSum()));
                    settlementDetail.setTodayorimoney(settlementDetail.getYesterdayorimoney());
                    settlementDetail.setTodaylocalmoney(settlementDetail.getYesterdaylocalmoney());
                }else{
                    settlementDetail.setTodayorimoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), settlementDetail.getTodaydebitorimoneysum()), settlementDetail.getTodaycreditorimoneysum()));
                    settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), settlementDetail.getTodaydebitlocalmoneysum()), settlementDetail.getTodaycreditlocalmoneysum()));
                }
            } else {
                settlementDetail = new SettlementDetail();
                settlementDetail.setAccentity(journal.getAccentity());
                settlementDetail.setBankaccount(journal.getBankaccount());
                settlementDetail.setCashaccount(journal.getCashaccount());
                settlementDetail.setAccountdate(startDate);
                settlementDetail.setCurrency(journal.getCurrency());
                settlementDetail.setTenant(AppContext.getTenantId());
                // 统计今日
                Date journalDate = journal.getDzdate()==null?journal.getVouchdate():journal.getDzdate();
                if (endDate != null && (startDate.equals(journalDate) || endDate.equals(journalDate) || (startDate.before(journalDate) && endDate.after(journalDate)))) {
                    settlementDetail.setTodaycreditorimoneysum(journal.getCreditoriSum());
                    settlementDetail.setTodaycreditlocalmoneysum(journal.getCreditnatSum());
                    settlementDetail.setTodaydebitorimoneysum(journal.getDebitoriSum());
                    settlementDetail.setTodaydebitlocalmoneysum(journal.getDebitnatSum());
                }
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
                // 统计昨日
                if (endDate != null && (startDate.equals(journalDate) || endDate.equals(journalDate) || (startDate.before(journalDate) && endDate.after(journalDate)))) {
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(journal.getDebitoriSum(), journal.getCreditoriSum()));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(journal.getDebitnatSum(), journal.getCreditnatSum()));
                }
                if(endDate == null){
                    //如果日期为空，则为最大值，都是昨日，昨日与余额一致
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(journal.getDebitoriSum(),journal.getCreditoriSum()));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(journal.getDebitnatSum(),journal.getCreditnatSum()));
                    settlementDetail.setTodayorimoney(settlementDetail.getYesterdayorimoney());
                    settlementDetail.setTodaylocalmoney(settlementDetail.getYesterdaylocalmoney());
                }else{
                    settlementDetail.setTodayorimoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), settlementDetail.getTodaydebitorimoneysum()), settlementDetail.getTodaycreditorimoneysum()));
                    settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), settlementDetail.getTodaydebitlocalmoneysum()), settlementDetail.getTodaycreditlocalmoneysum()));
                }
                settlementDetailMap.put(key, settlementDetail);
            }
        }
        return settlementDetailMap;
    }


    public static Map getDailyComputezInitMapperParam(String accentity, String currency, String cashaccount,
                                                      String bankaccount,String auditstatus,String settlestatus,
                                                      Date currDate) {
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
        param.put("maxDateSettlement",currDate);

        return param;
    }
    /**
     * 模拟日结
     * @param accentity
     * @param auditstatus
     * @param issettle
     * @return
     * @throws Exception
     */
    public static Map<String, SettlementDetail> imitateDailyComputeNew(String accentity, String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date currDate, Boolean isUpdate) throws Exception {
        Date minDateUnSettlement = null;
        if (minDateUnSettlement == null) {
            try {
                // 会计期间启用日
                minDateUnSettlement = DailyComputeUtils.queryOrgPeriodBeginDate(accentity);
            } catch (Exception e) {
                if(!isUpdate){
                    List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{accentity}));
                    Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100740"),
                            String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
                }else
                    log.error(accentity+"未能获取到资金组织对应的会计期间", e);
//                throw new BizException("02P_YS_FI_CM_0000026291", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180289", "未能获取到资金组织对应的会计期间！") /* "未能获取到资金组织对应的会计期间！" */);
            }
        }
        Map<String, SettlementDetail> settlementDetailMap = new HashMap();
        //期初
        getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, currDate, settlementDetailMap, null);

        Map param = getDailyComputezInitMapperParam(accentity, currency, cashaccount, bankaccount, auditstatus, issettle, currDate);
        //小于等于当天的合计日记账数据
        List<Map<String, Object>> listJournalCurrdateCount = SqlHelper.selectList(DAILYCOMPUTEZINITMAPPER +
                "getSumForCurrentday", param);
        //小于当天的合计日记账数据 用于累计昨日数据
        List<Map<String, Object>> listJournalYesterdayCount = SqlHelper.selectList(DAILYCOMPUTEZINITMAPPER +
                "getSumForYesterday", param);
        Map<String, Map<String, Object>> yesterdayMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(listJournalYesterdayCount)) {
            for (Map<String, Object> item : listJournalYesterdayCount) {
                Journal journal = new Journal();
                journal.init(item);
                String key = journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
                yesterdayMap.put(key.replace("null", ""), item);
            }
        }
        //listJournalCurrdateCount为空  且 listJournalYesterdayCount也为空 说明一直没有发生额 此时使用期初数据
        if(listJournalCurrdateCount.isEmpty() && listJournalYesterdayCount.isEmpty()){
            return settlementDetailMap;
        } else if (listJournalCurrdateCount.isEmpty() && !listJournalYesterdayCount.isEmpty()){
            //listJournalCurrdateCount为空 且listJournalYesterdayCount不为空 说明当天没有发生额 此时使用昨日数据
            reComputeSettlementdetailForJournal(listJournalYesterdayCount,settlementDetailMap,yesterdayMap,currDate,false);
        } else if(!listJournalCurrdateCount.isEmpty() && listJournalYesterdayCount.isEmpty()){
            //listJournalCurrdateCount 不为空 说明当天有发生额 循环计算当日发生额
            reComputeSettlementdetailForJournal(listJournalCurrdateCount,settlementDetailMap,yesterdayMap,currDate,true);
        }else if(!listJournalCurrdateCount.isEmpty() && !listJournalYesterdayCount.isEmpty()){
            // 如果今日、昨日都有余额数据 先假定今日无数据 再假定昨日无数据 顺序不能变
            reComputeSettlementdetailForJournal(listJournalYesterdayCount,settlementDetailMap,yesterdayMap,currDate,false);
            reComputeSettlementdetailForJournal(listJournalCurrdateCount,settlementDetailMap,yesterdayMap,currDate,true);
        }
        return settlementDetailMap;
    }

    /**
     * 根据昨日(今日)的日记账数据信息 重算日结明细相关余额信息
     * @param listJournalCount
     * @param settlementDetailMap
     * @param yesterdayMap
     * @param currDate
     * @param isTodayData 判断当前发生额数据是当日 还是 昨日(这里的昨日指 从业务开始直至昨日的发生额),如果发生额是其中一个 默认另一个为空
     */
    private static void reComputeSettlementdetailForJournal(List<Map<String, Object>> listJournalCount,Map<String, SettlementDetail> settlementDetailMap,
                                                            Map<String, Map<String, Object>> yesterdayMap,Date currDate,Boolean isTodayData){
        if(isTodayData){
            for(Map<String, Object> mapJournalCount : listJournalCount){
                Journal journal = new Journal();
                journal.init(mapJournalCount);
                String key = journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
                key = key.replace("null", "");
                SettlementDetail settlementDetail = settlementDetailMap.get(key);
                Map<String, Object> sumYesterday = yesterdayMap.get(key);
                if(settlementDetail!=null){
                    //今日借贷方合计
                    settlementDetail.setTodaycreditorimoneysum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaycreditorimoneysum(), journal.getCreditoriSum()));
                    settlementDetail.setTodaycreditlocalmoneysum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaycreditlocalmoneysum(), journal.getCreditnatSum()));
                    settlementDetail.setTodaydebitorimoneysum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaydebitorimoneysum(), journal.getDebitoriSum()));
                    settlementDetail.setTodaydebitlocalmoneysum(BigDecimalUtils.safeAddObj(settlementDetail.getTodaydebitlocalmoneysum(), journal.getDebitnatSum()));
                    /**
                     * 统计昨日 如果同时存在昨日发生额与今日发生额(二者可能都存在的情况) 那么由于先计算了昨日 所以昨日不为空 则用昨日
                     * 如果昨日确实为空(说明二者不是都存在) 则默认期数据为期初数据
                     * 无论上述哪种情况 由于先计算昨日 所以昨日数据为昨日数据 这里不需要额外处理 仅留注释
                     */

                    //统计今日余额 = 昨日余额(期初金额) + 今日借方发生额 - 今日贷方发生额
                    settlementDetail.setTodayorimoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), settlementDetail.getTodaydebitorimoneysum()), settlementDetail.getTodaycreditorimoneysum()));
                    settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), settlementDetail.getTodaydebitlocalmoneysum()), settlementDetail.getTodaycreditlocalmoneysum()));
                }else{
                    buildSettlementDetailForNOInitDataAndHaveJournal(journal,settlementDetailMap,currDate,sumYesterday,key,isTodayData);
                }
            }
        }else{
            //如果当前数据为昨日 则说明 当日发生额为空 那么昨日数据为当日数据
            for(Map<String, Object> mapJournalCount : listJournalCount){
                Journal journal = new Journal();
                journal.init(mapJournalCount);
                String key = journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
                key = key.replace("null", "");
                SettlementDetail settlementDetail = settlementDetailMap.get(key);
                Map<String, Object> sumYesterday = yesterdayMap.get(key);
                if(settlementDetail!=null){
                    //今日借贷方合计 当日发生额为空 应为0
                    settlementDetail.setTodaycreditorimoneysum(BigDecimal.ZERO);
                    settlementDetail.setTodaycreditlocalmoneysum(BigDecimal.ZERO);
                    settlementDetail.setTodaydebitorimoneysum(BigDecimal.ZERO);
                    settlementDetail.setTodaydebitlocalmoneysum(BigDecimal.ZERO);
                    // 统计昨日
                    if (sumYesterday != null && !sumYesterday.isEmpty()) {
                        //期初余额+08-29 含之前所有(借方余额合计-贷方余额合计)
                        settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), BigDecimalUtils.safeSubtract(new BigDecimal(sumYesterday.get("debitoriSum")!=null?sumYesterday.get("debitoriSum").toString():"0"),
                                new BigDecimal(sumYesterday.get("creditoriSum")!=null?sumYesterday.get("creditoriSum").toString():"0"))));
                        settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), BigDecimalUtils.safeSubtract(new BigDecimal(sumYesterday.get("debitnatSum")!=null?sumYesterday.get("debitnatSum").toString():"0"),
                                new BigDecimal(sumYesterday.get("creditnatSum")!=null?sumYesterday.get("creditnatSum").toString():"0"))));
                    }
                    //统计今日余额 因为今日发生额为空 所以今日余额=昨日余额
                    settlementDetail.setTodayorimoney(settlementDetail.getYesterdayorimoney());
                    settlementDetail.setTodaylocalmoney(settlementDetail.getYesterdaylocalmoney());
                }else{
                    buildSettlementDetailForNOInitDataAndHaveJournal(journal,settlementDetailMap,currDate,sumYesterday,key,isTodayData);
                }
            }
        }
    }

    /**
     * 特数据情况 如果账户期初中没有当前账户信息 但是日记账中有此账户发生信息 则需要新增一个SettlementDetail 并加入settlementDetailMap中
     * @param journal
     * @param settlementDetailMap
     * @param isTodayData 判断当前发生额数据是当日 还是 昨日(这里的昨日指 从业务开始直至昨日的发生额)
     */
    private static void buildSettlementDetailForNOInitDataAndHaveJournal(Journal journal,Map<String, SettlementDetail> settlementDetailMap,Date currDate,Map<String, Object> sumYesterday,String key
                                                                         ,Boolean isTodayData){
        SettlementDetail settlementDetail = new SettlementDetail();
        if(settlementDetailMap.get(key)!=null){
            settlementDetail = settlementDetailMap.get(key);
        }else{
            settlementDetail.setAccentity(journal.getAccentity());
            settlementDetail.setBankaccount(journal.getBankaccount());
            settlementDetail.setCashaccount(journal.getCashaccount());
            settlementDetail.setAccountdate(currDate);
            settlementDetail.setCurrency(journal.getCurrency());
            settlementDetail.setTenant(AppContext.getTenantId());
        }
        if(isTodayData){
            //如果当前数据为今日 则说明从业务开始直至昨日的发生额为0
            /**
             * 统计昨日 如果同时存在昨日发生额与今日发生额(二者可能都存在的情况) 那么由于先计算了昨日 所以昨日不为空 则用昨日
             * 如果昨日确实为空(说明二者不是都存在) 则默认期初为0
             */
            settlementDetail.setYesterdayorimoney(settlementDetail.getYesterdayorimoney()!=null?settlementDetail.getYesterdayorimoney():BigDecimal.ZERO);
            settlementDetail.setYesterdaylocalmoney(settlementDetail.getYesterdaylocalmoney()!=null?settlementDetail.getYesterdaylocalmoney():BigDecimal.ZERO);
            //今日借贷方合计
            settlementDetail.setTodaycreditorimoneysum(journal.getCreditoriSum());
            settlementDetail.setTodaycreditlocalmoneysum(journal.getCreditnatSum());
            settlementDetail.setTodaydebitorimoneysum(journal.getDebitoriSum());
            settlementDetail.setTodaydebitlocalmoneysum(journal.getDebitnatSum());
            //统计今日余额 = 昨日余额(期初金额) + 今日借方发生额 - 今日贷方发生额
            settlementDetail.setTodayorimoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), settlementDetail.getTodaydebitorimoneysum()), settlementDetail.getTodaycreditorimoneysum()));
            settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), settlementDetail.getTodaydebitlocalmoneysum()), settlementDetail.getTodaycreditlocalmoneysum()));
        }else{
            //如果当前数据为昨日 则说明 当日发生额为空 那么昨日数据为当日数据
            // 统计昨日
            if (sumYesterday != null && !sumYesterday.isEmpty()) {
                settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), BigDecimalUtils.safeSubtract(new BigDecimal(sumYesterday.get("debitoriSum")!=null?sumYesterday.get("debitoriSum").toString():"0")),
                        new BigDecimal(sumYesterday.get("creditoriSum")!=null?sumYesterday.get("creditoriSum").toString():"0")));
                settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), BigDecimalUtils.safeSubtract(new BigDecimal(sumYesterday.get("debitnatSum")!=null?sumYesterday.get("debitnatSum").toString():"0")),
                        new BigDecimal(sumYesterday.get("creditnatSum")!=null?sumYesterday.get("creditnatSum").toString():"0")));
            }else{
                settlementDetail.setYesterdayorimoney(BigDecimal.ZERO);
                settlementDetail.setYesterdaylocalmoney(BigDecimal.ZERO);
            }
            // 统计今日
            //今日借贷方合计 当日发生额为空 应为0
            settlementDetail.setTodaycreditorimoneysum(BigDecimal.ZERO);
            settlementDetail.setTodaycreditlocalmoneysum(BigDecimal.ZERO);
            settlementDetail.setTodaydebitorimoneysum(BigDecimal.ZERO);
            settlementDetail.setTodaydebitlocalmoneysum(BigDecimal.ZERO);
            //统计今日余额 因为今日发生额为空 所以今日余额=昨日余额
            settlementDetail.setTodayorimoney(settlementDetail.getYesterdayorimoney());
            settlementDetail.setTodaylocalmoney(settlementDetail.getYesterdaylocalmoney());
        }
        settlementDetailMap.put(key, settlementDetail);
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
    private static void getSettleMentDetailFromInitData(String accentity, String currency, String cashaccount, String bankaccount, Date endDate, Map<String, SettlementDetail> settlementDetailMap,List<String> newList) throws Exception {
        QueryConditionGroup initDataCondition = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        QuerySchema initDataQuerySchema = QuerySchema.create().addSelect("*");
        initDataQuerySchema.addCondition(initDataCondition);
        List<Map<String, Object>> initDataQuery = MetaDaoHelper.query(InitData.ENTITY_NAME, initDataQuerySchema);
        for (Map<String, Object> initmap : initDataQuery) {
            InitData initData = new InitData();
            initData.init(initmap);
            String key = initData.getAccentity() + initData.getBankaccount() + initData.getCashaccount() + initData.getCurrency();
            key = key.replace("null", "");
            //多币种处理
            if(!CollectionUtils.isEmpty(newList) && !newList.contains(key)){
                continue;
            }
            //多币种处理
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
    public static QueryConditionGroup getQueryConditionGroup(String accentity, String currency, String cashaccount,
                                                             String bankaccount) {
        QueryConditionGroup condition = new QueryConditionGroup();
        if (!StringUtils.isEmpty(accentity)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity)));
        }
        if (!StringUtils.isEmpty(cashaccount) && !StringUtils.isEmpty(bankaccount)) {
            condition.addCondition(QueryConditionGroup.or(QueryCondition.name("cashaccount").in((Object) cashaccount.split(",")),
                    QueryCondition.name("bankaccount").in((Object) bankaccount.split(","))));
        } else if (!StringUtils.isEmpty(cashaccount)) {
            condition.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("cashaccount").in((Object) cashaccount.split(","))));
        } else if (!StringUtils.isEmpty(bankaccount)) {
            condition.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("bankaccount").in((Object) bankaccount.split(","))));
        }
        if (!StringUtils.isEmpty(currency) ) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").in((Object) currency.split(","))));
        }
        return condition;
    }

    /**
     * 获取资金组织下银行账户+币种
     * @param accentity
     * @return
     * @throws Exception
     */
    public static List<String> getCurrencyKey(String accentity) throws Exception {
        QueryConditionGroup initDataCondition = getQueryConditionGroup(accentity, null, null, null);
        QuerySchema initDataQuerySchema = QuerySchema.create().addSelect("*");
        initDataQuerySchema.addCondition(initDataCondition);
        List<Map<String, Object>> initDataQueryList = MetaDaoHelper.query(InitData.ENTITY_NAME, initDataQuerySchema);
        List<String> currencyKey = new ArrayList<>();
        if(!CollectionUtils.isEmpty(initDataQueryList)){
            for (Map<String, Object> initDataQuery : initDataQueryList) {
                InitData initData = new InitData();
                initData.init(initDataQuery);
                String key = initData.getAccentity() + initData.getBankaccount() + initData.getCashaccount() + initData.getCurrency();
                key = key.replace("null", "");
                if(!currencyKey.contains(key)){
                    currencyKey.add(key);
                }
            }
        }
        return currencyKey;
    }

}
