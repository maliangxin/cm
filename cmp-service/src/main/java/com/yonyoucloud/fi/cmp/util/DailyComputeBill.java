package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.BizException;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;

import java.math.BigDecimal;
import java.util.*;

/**
 * 不返回没有币种的数据
 */
@Slf4j
public class DailyComputeBill {

    public static Map<String, SettlementDetail> imitateDailyCompute(String accentity, String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date startDate) throws Exception {
        return doImitateDailyCompute(accentity, currency, cashaccount, bankaccount, auditstatus, issettle, startDate, startDate);
    }

    /**
     * 通过sumsql 计算期初
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param auditstatus
     * @param issettle
     * @param startbillDate
     * @param startdzDate
     * @return
     * @throws Exception
     */
    public static Map<String, SettlementDetail> doImitateDailyComputeBySumSql(String accentity, String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date startbillDate,Date startdzDate) throws Exception {
        Map<String, SettlementDetail> settlementDetailMap = new HashMap<String, SettlementDetail>();
        //当按日期作为计算依据时 走此种方式
        if(startbillDate!=null && startdzDate == null){
            getSettlementDetailMapFotBillDate(accentity,currency,cashaccount,bankaccount,auditstatus,issettle,startbillDate,startdzDate,settlementDetailMap);
        }else if(startbillDate==null && startdzDate != null){
            /**
             * 当按登账日记作为计算依据时 这里由于历史数据原因 使用日结明细作为期初计算依据 分三种情况
             * 1.不包含未结算：所有单据已审批
             * 2.包含未结算，不包含未审批：已审批 未结算
             * 3.包含未结算，包含未审批：直接统计所有单据
             */
            //1.不包含未结算：所有单据已审批 已结算
            if((auditstatus!=null && "2".equals(auditstatus))&&(issettle!=null &&  "2".equals(issettle))){
                getSettlementDetailMapFotDzDateIsAuIsSe(accentity,currency,cashaccount,bankaccount,auditstatus,issettle,startbillDate,startdzDate,settlementDetailMap);
            }
            //2.包含未结算，不包含未审批：已审批 未结算 TODO 由于时间紧迫 这种情况先按原来的逻辑 后续添加
            else if((auditstatus!=null && "2".equals(auditstatus))&&(issettle==null ||  "1".equals(issettle))){
                getSettlementDetailMapFotBillDate(accentity,currency,cashaccount,bankaccount,auditstatus,issettle,startbillDate,startdzDate,settlementDetailMap);
            }else if((auditstatus==null || "1".equals(auditstatus))&&(issettle==null ||  "1".equals(issettle))){
                //3.包含未结算，包含未审批 全包含还走源逻辑从头推算
                getSettlementDetailMapFotBillDate(accentity,currency,cashaccount,bankaccount,auditstatus,issettle,startbillDate,startdzDate,settlementDetailMap);
            }
        }else{
            getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, null, settlementDetailMap);
        }
        return settlementDetailMap;
    }

    /**
     * 当按日期作为计算依据时 走此种方式
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param auditstatus
     * @param issettle
     * @param startbillDate
     * @param startdzDate
     * @param settlementDetailMap
     * @throws Exception
     */
    public static void getSettlementDetailMapFotBillDate(String accentity, String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date startbillDate,Date startdzDate,Map<String, SettlementDetail> settlementDetailMap ) throws Exception {
        BigDecimal initSumOri = BigDecimal.ZERO;
        BigDecimal sumNatbalance = BigDecimal.ZERO;
        //查出期初数据
        InitData initData = new InitData();
        QueryConditionGroup initDataCondition = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        QuerySchema initDataQuerySchema = QuerySchema.create().addSelect("accentity,currency,cashaccount,bankaccount,coinitloribalance,coinitlocalbalance");
        initDataQuerySchema.addCondition(initDataCondition);
        List<Map<String, Object>> initDataQueryList = MetaDaoHelper.query(InitData.ENTITY_NAME, initDataQuerySchema);
        if (CollectionUtils.isEmpty(initDataQueryList)) {
            initData.setCoinitloribalance(BigDecimal.ZERO);
            initData.setCoinitlocalbalance(BigDecimal.ZERO);
        } else {
            initData.init(initDataQueryList.get(0));
        }
        //根据条件查询日记账数据 借贷之和
        QueryConditionGroup conditionJournal = getQueryConditionGroupForJournal(accentity, currency, cashaccount, bankaccount);
        if(auditstatus!=null && "2".equals(auditstatus)){
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("auditstatus").eq(1)));
        }
        if(issettle!=null &&  "2".equals(issettle)){
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("settlestatus").eq(2)));
        }
        if(startbillDate!=null){
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").lt(startbillDate)));
        }
        if(startdzDate!=null){
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").lt(startdzDate)));
        }
        conditionJournal.addCondition(QueryCondition.name("initflag").eq(0));
        QuerySchema querySchemaSum = QuerySchema.create().addSelect("sum(debitnatSum) as debitnatSum,sum(debitoriSum) as debitoriSum,sum(creditnatSum) as creditnatSum,sum(creditoriSum) as creditoriSum");
        querySchemaSum.addCondition(conditionJournal);
        List<Map<String, Object>> journalSumList = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaSum);
        //如果当前没数据 说明传入日期小于当前条件下的业务发生日期 需要从期初开始计算模拟日结数据
        if(journalSumList!=null && journalSumList.size()>0){
            Journal journalSum = new Journal();
            journalSum.init(journalSumList.get(0));
            //结合期初数据计算
            initSumOri = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(initData.getCoinitloribalance(),journalSum.getDebitoriSum()),journalSum.getCreditoriSum());
            sumNatbalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(initData.getCoinitlocalbalance(),journalSum.getDebitnatSum()),journalSum.getCreditnatSum());
            //组装数据返回
            buildReSettlementDetailMap(settlementDetailMap,accentity,currency,cashaccount,bankaccount,initSumOri,sumNatbalance);
        }else {
            getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, null, settlementDetailMap);
        }
    }

    /**
     * 在已审批 已结算的条件下计算期初余额
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param auditstatus
     * @param issettle
     * @param startbillDate
     * @param startdzDate
     * @param settlementDetailMap
     * @throws Exception
     */
    public static void getSettlementDetailMapFotDzDateIsAuIsSe(String accentity, String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date startbillDate,Date startdzDate,Map<String, SettlementDetail> settlementDetailMap ) throws Exception {
        BigDecimal initSumOri = BigDecimal.ZERO;
        BigDecimal sumNatbalance = BigDecimal.ZERO;
        QueryConditionGroup conditionMin = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        List<Map<String, Object>> queryMin = null;
        //先查询当前开始日期的前一日是否有结算明细 若有直接用 若无进行后续计算
        conditionMin.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(DateUtils.dateAdd(startdzDate,-1,false))));
        QuerySchema querySchemaMin = QuerySchema.create().addSelect("accountdate,todayorimoney,todaylocalmoney");
        querySchemaMin.addCondition(conditionMin);
        queryMin = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchemaMin);
        if(!queryMin.isEmpty()){
            initSumOri = new BigDecimal(queryMin.get(0).get("todayorimoney").toString());
            sumNatbalance = new BigDecimal(queryMin.get(0).get("todaylocalmoney").toString());
            buildReSettlementDetailMap(settlementDetailMap,accentity,currency,cashaccount,bankaccount,initSumOri,sumNatbalance);
        }else{
            //查询当前能取到最大日期的日结明细
            QuerySchema querySchemaSettlement = QuerySchema.create().addSelect("accountdate,todayorimoney,todaylocalmoney");
            querySchemaSettlement.addCondition(getQueryConditionGroup(accentity, currency, cashaccount, bankaccount));
            querySchemaSettlement.addOrderBy(new QueryOrderby("accountdate", "desc"));
            querySchemaSettlement.addPager(0,1);
            Map<String,Object> map = MetaDaoHelper.queryOne(SettlementDetail.ENTITY_NAME,querySchemaSettlement);
            /**
             * 如果无数据，说明从未日结过 这里需要从头推算
             * 如果当前的最大日结日期比页面上选择的开始日期还大，说明开始日期选的太小了这时候还没有启用过日结 期初肯定需要从头推算
             */
            if(map == null){
                getSettlementDetailMapFotBillDate(accentity,currency,cashaccount,bankaccount,auditstatus,issettle,startbillDate,startdzDate,settlementDetailMap);
            }else if(map!=null && DateUtils.dateParse(map.get("accountdate").toString(),null).compareTo(startdzDate)>= 0){
                //如果当前的最大日结日期比页面上选择的开始日期还大，说明开始日期选的太小了这时候还没有启用过日结 期初肯定需要从头推算
                getSettlementDetailMapFotBillDate(accentity,currency,cashaccount,bankaccount,auditstatus,issettle,startbillDate,startdzDate,settlementDetailMap);
            }else{
                //如果有数据，则后续要查询当前日结日期 到开始日期之前的日记账 进行期初重算
                QuerySchema initDataQuerySchema = QuerySchema.create().addSelect("sum(debitnatSum) as debitnatSum,sum(debitoriSum) as debitoriSum,sum(creditnatSum) as creditnatSum,sum(creditoriSum) as creditoriSum");
                QueryConditionGroup conditionJournal = getQueryConditionGroupForJournal(accentity, currency, cashaccount, bankaccount);
                conditionJournal.addCondition(QueryCondition.name("initflag").eq(0));
                conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("auditstatus").eq(1)));
                conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("settlestatus").eq(2)));
                //大于当前最大结账日期 小于选择的登账日期
                conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").gt(DateUtils.dateParse(map.get("accountdate").toString(),null))));
                conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").lt(startdzDate)));
                initDataQuerySchema.addCondition(conditionJournal);
                List<Map<String, Object>> query = MetaDaoHelper.query(Journal.ENTITY_NAME, initDataQuerySchema);
                if(query!=null && query.size()>0){
                    Journal journalSum = new Journal();
                    journalSum.init(query.get(0));
                    //结合期初数据计算
                    initSumOri = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(new BigDecimal(map.get("todayorimoney").toString()),journalSum.getDebitoriSum()),journalSum.getCreditoriSum());
                    sumNatbalance = BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAdd(new BigDecimal(map.get("todaylocalmoney").toString()),journalSum.getDebitnatSum()),journalSum.getCreditnatSum());
                }else{
                    initSumOri = new BigDecimal(map.get("todayorimoney").toString());
                    sumNatbalance = new BigDecimal(map.get("todaylocalmoney").toString());
                }
                //组装数据返回
                buildReSettlementDetailMap(settlementDetailMap,accentity,currency,cashaccount,bankaccount,initSumOri,sumNatbalance);
            }
        }
    }
    /**
     * 根据条件构建返回的settlementDetailMap 日记账期初信息
     * @param settlementDetailMap
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     */
    public static void buildReSettlementDetailMap(Map<String, SettlementDetail> settlementDetailMap, String accentity, String currency, String cashaccount, String bankaccount, BigDecimal initSumOri, BigDecimal sumNatbalance){
        String key = accentity + bankaccount + cashaccount + currency;
        key = key.replace("null", "");
        SettlementDetail settlementDetail = new SettlementDetail();
        settlementDetail.setAccentity(accentity);
        settlementDetail.setAccentityRaw(CmpMetaDaoHelper.getAccentityRaw(accentity));
        settlementDetail.setCurrency(currency);
        settlementDetail.setYesterdayorimoney(initSumOri);
        settlementDetail.setYesterdaylocalmoney(sumNatbalance);
        settlementDetail.setTodayorimoney(initSumOri);
        settlementDetail.setTodaylocalmoney(sumNatbalance);
        settlementDetailMap.put(key, settlementDetail);
    }

    public static Map<String, SettlementDetail> getSettlementDetailBySumSql(String accentity, String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date dzDate, Date vouchdate) throws Exception {
        Map<String, SettlementDetail> settlementDetailMap = new HashMap<String, SettlementDetail>();
        //查询会计期间启用日期
        Date accountingDate = null;
        //会计期间校验
        getAccountingDate(accentity,  currency,  cashaccount,  bankaccount, accountingDate);
        //先判断登是否已结算
        Boolean isDzDate = true;
        if (null!= issettle && "1".equals(issettle)) {//包含未结算 说明是单据日期
            isDzDate = false;
        }
//        List<Map<String, Object>> queryMin = null;
        //计算期初金额
        getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, null, settlementDetailMap);
        countSettlementInit(accentity, currency, cashaccount, bankaccount, dzDate,vouchdate, settlementDetailMap,auditstatus,issettle);
        return settlementDetailMap;
    }


    private static void countSettlementInit(String accentity,String currency,String cashaccount,String bankaccount,Date dzDate,Date vouchdate, Map<String, SettlementDetail> settlementDetailMap,String auditstatus, String issettle) throws Exception {
        QuerySchema queryJournalSum = QuerySchema.create().addSelect("sum(creditoriSum) as creditoriSum,sum(debitoriSum) as debitoriSum,sum(creditnatSum) as creditnatSum,sum(debitnatSum) as debitnatSum,bankaccount,cashaccount,currency,accentity");
        QueryConditionGroup conditionJournal = getQueryConditionGroupForJournal(accentity, currency, cashaccount, bankaccount);
        conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("initflag").eq(0)));
        //审批状态
        if (null!= auditstatus && "2".equals(auditstatus)) {//不包含未审核
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("auditstatus").eq(AuditStatus.Complete.getValue())));//1为已审核
        }
        //结算状态
        if (null!= issettle && "2".equals(issettle)) {//不包含未结算
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("settlestatus").eq(SettleStatus.alreadySettled.getValue())));
        }
        //若为已结算 必有登账日期 故而这里不用拼接 结算的条件
        if(!Objects.isNull(dzDate)){
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").lt(dzDate)));
        }else{
            conditionJournal.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").lt(vouchdate)));
        }
        queryJournalSum.addCondition(conditionJournal);
        queryJournalSum.addGroupBy("bankaccount,cashaccount,currency,accentity");
        List<Map<String, Object>> journalSumList = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournalSum);
        if (journalSumList!=null && journalSumList.size()>0){
            for(Map<String, Object> journalMap : journalSumList){
                Journal journal = new Journal();
                journal.init(journalMap);
                String key = (journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency()).replace("null", "");
                if(settlementDetailMap.get(key)!=null){
                    SettlementDetail settlementDetail = settlementDetailMap.get(key);
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(settlementDetail.getYesterdayorimoney(), journal.getCreditoriSum()), journal.getDebitoriSum()));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(settlementDetail.getYesterdaylocalmoney(), journal.getCreditnatSum()), journal.getDebitnatSum()));
                } else {
                    //有发生额，但是没有期初，则默认为0，并追加发生额
                    SettlementDetail settlementDetail = new SettlementDetail();
                    settlementDetail.setAccentity(accentity);
                    settlementDetail.setAccentityRaw(CmpMetaDaoHelper.getAccentityRaw(accentity));
                    settlementDetail.setBankaccount(bankaccount);
                    settlementDetail.setCashaccount(cashaccount);
                    settlementDetail.setAccountdate(null);
                    settlementDetail.setCurrency(currency);
                    settlementDetail.setTenant(AppContext.getTenantId());
                    settlementDetail.setTodaycreditorimoneysum(BigDecimal.ZERO);
                    settlementDetail.setTodaycreditlocalmoneysum(BigDecimal.ZERO);
                    settlementDetail.setTodaydebitorimoneysum(BigDecimal.ZERO);
                    settlementDetail.setTodaydebitlocalmoneysum(BigDecimal.ZERO);
                    settlementDetail.setTodaydebitnum(BigDecimal.ZERO);
                    settlementDetail.setTodaycreditnum(BigDecimal.ZERO);
                    settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getCreditoriSum()), journal.getDebitoriSum()));
                    settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(BigDecimal.ZERO, journal.getCreditnatSum()), journal.getDebitnatSum()));
                    settlementDetail.setTodayorimoney(BigDecimal.ZERO);
                    settlementDetail.setTodaylocalmoney(BigDecimal.ZERO);
                }
            }
        }
    }

    /**
     * 根据相关条件查询日结明细
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param startDate
     * @return
     * @throws Exception
     */
    private static List<Map<String, Object>> getSettleMentDetail(String accentity,String currency,String cashaccount,String bankaccount,Date startDate) throws Exception {
        List<Map<String, Object>> queryMin = null;
        QueryConditionGroup conditionMin = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        conditionMin.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(startDate)));
        QuerySchema querySchemaMin = QuerySchema.create().addSelect("*");
        querySchemaMin.addCondition(conditionMin);
        queryMin = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchemaMin);
        return queryMin;
    }

    /**
     * 模拟日结
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param auditstatus
     * @param issettle
     * @param startDate 基准日期
     * @return
     *  1.会计期间启用日：有会计期间启用日且小于传入日期，查询最近日结；否则查期初
     *  2.根据不同条件初始化SettlementDetail
     *  3.有了结算明细初始化后：a)条件：有结算时间且包含未结算；需要统计最近日结之前未结算金额做修正；b)无结算时间：从头推算
     *  c)查询：endDate-结算日期，时间段内数据；d)没有传startDate：查询大于结算日期的数据
     *  4.讲3中数据做金额修正
     * @return
     * @throws Exception
     */
    public static Map<String, SettlementDetail> doImitateDailyCompute(String accentity, String currency, String cashaccount, String bankaccount, String auditstatus, String issettle, Date startDate, Date endDate) throws Exception {
        //1.会计期间启用日
        Map<String, SettlementDetail> settlementDetailMap = new HashMap<String, SettlementDetail>();
        //日结明细数据
        List<Map<String, Object>> queryMin = null;
        QueryConditionGroup conditionMin = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        if(startDate != null){
            //查询当前日期的日结明细数据，如果有startDate即为当前查询的最大结账日
            conditionMin.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(startDate)));
            QuerySchema querySchemaMin = QuerySchema.create().addSelect("*");
            querySchemaMin.addCondition(conditionMin);
            queryMin = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchemaMin);
            //会计期间启用日
            Date minDateUnSettlement = null;
            //根据传入日期查询日结明细数据，同时尝试获取会计期间启用日
            minDateUnSettlement = getAccountingDate(accentity,  currency,  cashaccount,  bankaccount, minDateUnSettlement);
            //最大日结日期
            Date maxDateSettlement = null;
            //2. 如果获取到会计期间启用日并且比需要查询开始日期晚
            if((minDateUnSettlement != null && minDateUnSettlement.compareTo(startDate) >= 0)){
                getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap);
            }else if(CollectionUtils.isEmpty(queryMin)){
                getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap);
            }else{
                maxDateSettlement = getSettlementDetailMap(accentity, currency, cashaccount, bankaccount, startDate,queryMin,maxDateSettlement,settlementDetailMap);
            }
            if(maxDateSettlement!=null){
                //3.下面部分主要进行数据查询与金额修正   （日结时不允许存在未审批的单据；日结是连续的；换句话说前面没有未审批数据不用考虑是否包含未审批的条件 ---新版不成立）
                if (null!= issettle && "1".equals(issettle)) {//根据单据日期查询结算时间maxDateSettlement，前未结算数据，并进行金额修正  报表要求包含未结算issettle=1时
                    beforeSettlement( accentity, currency, cashaccount, bankaccount, startDate, maxDateSettlement, settlementDetailMap, auditstatus, issettle);
                }
                if (DateUtils.dateToStr(startDate).equals(DateUtils.dateToStr(maxDateSettlement))) {
                    return settlementDetailMap;
                }
            }
            //逻辑至此，有最大日结时间的数据 无论页面查询包含结算还是不包含结算，最大日结前金额修正已完成
            List<Map<String, Object>> queryNew = new ArrayList<Map<String, Object>>();
            //接下来处理：从未日结；最大结账日<日期<startDate；没有startDate
            queryCorrect( accentity, currency, cashaccount, bankaccount, startDate,maxDateSettlement, auditstatus, issettle, queryNew);
            correctionAmount( startDate, queryNew, settlementDetailMap);
            return settlementDetailMap;
        }else{
            getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap);
            return settlementDetailMap;
        }
    }

    /**
     * 金额修正
     * @param startDate
     * @param queryNew
     * @param settlementDetailMap
     */
    private static void correctionAmount(Date startDate,List<Map<String, Object>> queryNew,Map<String, SettlementDetail> settlementDetailMap){
        if (!CollectionUtils.isEmpty(queryNew)) {
            for (Map<String, Object> mapJ : queryNew) {
                Journal journal = new Journal();
                journal.init(mapJ);
                String key = journal.getParentAccentity() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
                key = key.replace("null", "");
                SettlementDetail settlementDetail = settlementDetailMap.get(key);
                if (settlementDetail != null) {
                    if (DateUtils.dateToStr(startDate).equals(DateUtils.dateToStr(journal.getVouchdate() == null ? journal.getDzdate() : journal.getVouchdate()))) {
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
                    } else {// 统计昨日
                        settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), journal.getDebitoriSum()), journal.getCreditoriSum()));//昨日原币余额 = (昨日原币余额 + 借方原币金额) - 贷方原币金额
                        settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), journal.getDebitnatSum()), journal.getCreditnatSum()));//昨日本币余额 = (昨日本币余额 + 借方本币金额) - 贷方本币金额
                    }

                    //统计余额
                    settlementDetail.setTodayorimoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), settlementDetail.getTodaydebitorimoneysum()), settlementDetail.getTodaycreditorimoneysum()));//今日原币余额 = (昨日原币余额 + 今日借方原币合计) - 今日贷方原币合计
                    settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), settlementDetail.getTodaydebitlocalmoneysum()), settlementDetail.getTodaycreditlocalmoneysum()));//今日本币余额 = (昨日本币余额 + 今日借方本币合计) - 今日贷方本币合计
                } else {
                    if (StringUtils.isEmpty(journal.getCurrency())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101151"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807C5","日记账单据没有币种：") /* "日记账单据没有币种：" */ + journal.getId() + ";" + journal.getBillno());
                    }
                    settlementDetail = new SettlementDetail();
                    settlementDetail.setAccentity(journal.getAccentity());
                    settlementDetail.setBankaccount(journal.getBankaccount());
                    settlementDetail.setCashaccount(journal.getCashaccount());
                    settlementDetail.setAccountdate(startDate);
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
                    // 统计今日
                    if (DateUtils.dateToStr(startDate).equals(DateUtils.dateToStr(journal.getVouchdate() == null ? journal.getDzdate() : journal.getVouchdate()))) {
                        settlementDetail.setTodaycreditorimoneysum(journal.getCreditoriSum());
                        settlementDetail.setTodaycreditlocalmoneysum(journal.getCreditnatSum());
                        settlementDetail.setTodaydebitorimoneysum(journal.getDebitoriSum());
                        settlementDetail.setTodaydebitlocalmoneysum(journal.getDebitnatSum());
                    } else { // 统计昨日
                        settlementDetail.setYesterdayorimoney(BigDecimalUtils.safeSubtract(journal.getDebitoriSum(), journal.getCreditoriSum()));//昨日原币余额 = 借方原币金额 - 贷方原币金额
                        settlementDetail.setYesterdaylocalmoney(BigDecimalUtils.safeSubtract(journal.getDebitnatSum(), journal.getCreditnatSum()));//昨日本币余额 = 借方本币金额 - 贷方本币金额
                    }

                    settlementDetail.setTodayorimoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdayorimoney(), settlementDetail.getTodaydebitorimoneysum()), settlementDetail.getTodaycreditorimoneysum()));//今日原币余额 = (昨日原币余额 + 今日借方原币合计) - 今日贷方原币合计
                    settlementDetail.setTodaylocalmoney(BigDecimalUtils.safeSubtract(BigDecimalUtils.safeAddObj(settlementDetail.getYesterdaylocalmoney(), settlementDetail.getTodaydebitlocalmoneysum()), settlementDetail.getTodaycreditlocalmoneysum()));//今日本币余额 = (昨日本币余额 + 今日借方本币合计) - 今日贷方本币合计
                    settlementDetailMap.put(key, settlementDetail);
                }
            }
        }
    }

    /**
     *
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param startDate
     * @param maxDateSettlement
     * @param auditstatus
     * @param issettle
     * @param queryNew
     * @throws Exception
     */
    private static void queryCorrect(String accentity, String currency, String cashaccount, String bankaccount,Date startDate,
                                     Date maxDateSettlement,String auditstatus, String issettle,List<Map<String, Object>> queryNew)throws Exception{
        QueryConditionGroup conditionJournal_L = getQueryConditionGroupForJournal(accentity, currency, cashaccount, bankaccount);
        if(maxDateSettlement == null){//到startDate之前从未日结过
            conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").elt(startDate)));
        }else{
            //条件：最大结账日<日期<startDate的不是期初的所有数据（已日结的包含未结算的，未日结的包含未审批未结算）
            conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").gt(maxDateSettlement)));
            conditionJournal_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").lt(startDate)));
        }
        conditionJournal_L.addCondition(QueryCondition.name("initflag").eq(0));
        QuerySchema querySchemaJournal_L = QuerySchema.create().addSelect("*");
        querySchemaJournal_L.addCondition(conditionJournal_L);
        querySchemaJournal_L.addOrderBy(new QueryOrderby("id", "asc"));
        List<Map<String, Object>> queryJournal_L = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchemaJournal_L);

        // 包含未审批，包含未结算逻辑处理，余额重算（当前存储的是最大结账日的日结明细，如果最大结账日不是endDate需要余额进行重算）
        if (!CollectionUtils.isEmpty(queryJournal_L)) {
            for (Map<String, Object> rebuildMap : queryJournal_L) {
                if (rebuildMap.get("auditstatus") != null && (Integer) rebuildMap.get("auditstatus") == 2) {// 单据审批状态是未审批
                    if (null != auditstatus && "2".equals(auditstatus)) {//不包含未审批单据--->将未审批的单据+借-贷  // 报表选择不包含未审批
                        continue;
                    }
                }
                if (rebuildMap.get("settlestatus") != null && (Integer) rebuildMap.get("settlestatus") == 1) {//单据为结算单据
                    if (null != issettle && "2".equals(issettle)) {// 报表选择不包含未结算
                        continue;
                    }
                }
                queryNew.add(rebuildMap);
            }
        }
    }

    /**
     * 根据单据日期查询结算时间maxDateSettlement，前未结算数据，并进行金额修正
     * 举例：查询范围为6.1-6.30 日结日期为5.25时日余额==昨日余额（此前已将当日余额赋值给昨日余额）
     * 日结日期为6.1时安照正常查询出数据处理（即：昨日余额与今日余额不一定相等）
     * 产品设计如下修正的数据范围：（个人认为下面思路不全，代码中做了判断）
     * ①已保存 未审批     日记账日期<=8-31；
     * ②已审批 未结算     日记账日期<=8-31；
     * ③已结算                日记账日期<=8-31，登账日期>8-31；
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param startDate
     * @param maxDateSettlement
     * @param settlementDetailMap
     * @throws Exception
     */
    private static void beforeSettlement(String accentity, String currency, String cashaccount, String bankaccount,Date startDate,
                                         Date maxDateSettlement,Map<String, SettlementDetail> settlementDetailMap, String auditstatus, String issettle)throws Exception{
        QuerySchema queryJournalForXZ_L = QuerySchema.create().addSelect("*");//查询vouchdate<=maxDateSettlement且(dzdate>maxDateSettlement 或者 dzdate是null)
        QuerySchema queryJournalForXZ = QuerySchema.create().addSelect("*");
        QuerySchema queryJournalForXZ_td = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionJournalXZ_L = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        QueryConditionGroup conditionJournalXZ = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        QueryConditionGroup conditionJournalXZ_td = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        conditionJournalXZ_L.addCondition(QueryCondition.name("initflag").eq(0));
        conditionJournalXZ.addCondition(QueryCondition.name("initflag").eq(0));
        conditionJournalXZ_td.addCondition(QueryCondition.name("initflag").eq(0));
        if (null!= auditstatus && "2".equals(auditstatus)) {//不包含未审核
            conditionJournalXZ_L.addCondition(QueryCondition.name("auditstatus").eq(1));//1为已审核
            conditionJournalXZ_td.addCondition(QueryCondition.name("auditstatus").eq(1));//1为已审核
        }
        conditionJournalXZ_L.addCondition(QueryCondition.name("settlestatus").eq(1));//未结算  日结的金额不会包含未结算的数据，需要对包含的数据进行修正
        conditionJournalXZ.addCondition(QueryCondition.name("settlestatus").eq(2));//已结算
        List<Map<String, Object>> queryJournalXZ_td = new ArrayList<>();
        if( maxDateSettlement.compareTo(startDate) == 0){//---举例：查6.1号到6.30号数据 6月1号为最大日结 此时SettlementDetail实际取值为5月31号
            conditionJournalXZ_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").lt(maxDateSettlement)));// vouchdate<2021-06-01
//            conditionJournalXZ_td.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").eq(maxDateSettlement)));// vouchdate<2021-06-01
            conditionJournalXZ.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").lt(maxDateSettlement)));// vouchdate<2021-06-01
            conditionJournalXZ.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").egt(maxDateSettlement)));// dzdate>=2021-06-01
        }else{//---举例：查6.1号到6.30号数据 5月25号为最大日结  此时SettlementDetail实际取值为5月25号
            conditionJournalXZ_L.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").elt(maxDateSettlement)));//vouchdate<=2021-05-25
            conditionJournalXZ_td.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").eq(maxDateSettlement)));//vouchdate<=2021-05-25
            conditionJournalXZ.addCondition(QueryConditionGroup.and(QueryCondition.name("vouchdate").elt(maxDateSettlement)));//vouchdate<=2021-05-25
            conditionJournalXZ.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").gt(maxDateSettlement)));//dzdate>2021-05-25
            queryJournalForXZ_td.addCondition(conditionJournalXZ_td);
            queryJournalXZ_td = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournalForXZ_td);
        }
        queryJournalForXZ_L.addCondition(conditionJournalXZ_L);
        queryJournalForXZ.addCondition(conditionJournalXZ);
        List<Map<String, Object>> queryJournalXZ_L = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournalForXZ_L);
        List<Map<String, Object>> queryJournalXZ = MetaDaoHelper.query(Journal.ENTITY_NAME, queryJournalForXZ);
        if (!CollectionUtils.isEmpty(queryJournalXZ_L) || !CollectionUtils.isEmpty(queryJournalXZ)) {
            List<Map<String, Object>> listMap = new ArrayList<>();
            if(!CollectionUtils.isEmpty(queryJournalXZ)){
                listMap.addAll(queryJournalXZ);
            }
            if(!CollectionUtils.isEmpty(queryJournalXZ_L)){
                listMap.addAll(queryJournalXZ_L);
            }
            if(!CollectionUtils.isEmpty(queryJournalXZ_td)){
                listMap.addAll(queryJournalXZ_td);
            }
            for (Map<String, Object> mapJ : listMap) {
                Journal journal = new Journal();
                journal.init(mapJ);
                String key = journal.getAccentity() + journal.getBankaccount() + journal.getCashaccount() + journal.getCurrency();
                key = key.replace("null", "");
                SettlementDetail settlementDetail = settlementDetailMap.get(key);
                if (settlementDetail != null) {
                    // 修正原则：反向  昨日+借-贷
                    // 修正今日
                    if (DateUtils.dateToStr(startDate).equals(DateUtils.dateToStr(journal.getVouchdate()==null?journal.getDzdate():journal.getVouchdate()))) {
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
    }

    /**
     * 根据不同条件生成settlementDetailMap
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param startDate
     * @param queryMin
     * @param maxDateSettlement
     * @param settlementDetailMap
     * @throws Exception
     */
    private static Date getSettlementDetailMap(String accentity, String currency, String cashaccount, String bankaccount, Date startDate, List<Map<String, Object>> queryMin, Date maxDateSettlement, Map<String, SettlementDetail> settlementDetailMap) throws Exception {
        QuerySchema querySchemaSettlement = QuerySchema.create().addSelect("max(settlementdate)");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity), QueryCondition.name("settleflag").eq(1));//查询日结表全部为结算状态的最新一条数据
        querySchemaSettlement.addCondition(group);
        Map<String,Object> map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchemaSettlement);
        if (org.apache.commons.collections4.MapUtils.isNotEmpty(map)) {
            maxDateSettlement = (Date) map.get("max");
            if((maxDateSettlement != null && maxDateSettlement.compareTo(startDate) > 0)){//最大结算日期在查询开始日期之后
                if(queryMin != null && queryMin.size() > 0){
                    maxDateSettlement = startDate;
                }else{//结算详情表没有数据  现实可能是结算bug存在这种情况(上面已经判断 理论上不存在此情况)
                    getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap);
                    return maxDateSettlement;
                }
            }
            //查询最大结账日的日结明细
            QueryConditionGroup conditionMinMax = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
            conditionMinMax.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(maxDateSettlement)));
            QuerySchema querySchemaMax = QuerySchema.create().addSelect("*");
            querySchemaMax.addCondition(conditionMinMax);
            List<Map<String, Object>> queryMax = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchemaMax);
            if(CollectionUtils.isNotEmpty(queryMax)){
                for(Map<String,Object> mapM : queryMax){
                    SettlementDetail settlementDetail = new SettlementDetail();
                    settlementDetail.init(mapM);
                    if (DateUtils.dateToStr(startDate).equals(DateUtils.dateToStr(maxDateSettlement))) {

                    }else{
                        settlementDetail.setTodaycreditorimoneysum(BigDecimal.ZERO);
                        settlementDetail.setTodaycreditlocalmoneysum(BigDecimal.ZERO);
                        settlementDetail.setTodaydebitorimoneysum(BigDecimal.ZERO);
                        settlementDetail.setTodaydebitlocalmoneysum(BigDecimal.ZERO);
                        settlementDetail.setTodaydebitnum(BigDecimal.ZERO);
                        settlementDetail.setTodaycreditnum(BigDecimal.ZERO);
                    }
                    if(startDate.compareTo(maxDateSettlement) > 0){
                        //如果最大结账日在endDate之前将日结最大日结明细数据中的今日设置给昨日---举例：查6.1号到6.30号数据 5月25号为最大日结
                        settlementDetail.setYesterdayorimoney(settlementDetail.getTodayorimoney());
                        settlementDetail.setYesterdaylocalmoney(settlementDetail.getTodaylocalmoney());
                    }
                    String key = settlementDetail.getAccentity() + settlementDetail.getBankaccount() + settlementDetail.getCashaccount() + settlementDetail.getCurrency();
                    settlementDetailMap.put(key.replace("null",""),settlementDetail);
                }
            }else{
                maxDateSettlement = null;
                getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap);
            }
        } else {
            getSettleMentDetailFromInitData(accentity, currency, cashaccount, bankaccount, startDate, settlementDetailMap);
        }
        return maxDateSettlement;
    }

    /**
     * 根据传入日期查询日结明细数据，同时尝试获取会计期间启用日
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @param minDateUnSettlement
     * @throws Exception
     */
    private static Date getAccountingDate(String accentity, String currency, String cashaccount, String bankaccount,
                                          Date minDateUnSettlement) throws Exception{
        if(minDateUnSettlement == null){
            try {
                minDateUnSettlement = DailyComputeUtils.queryOrgPeriodBeginDate(accentity);// 会计期间启用日
            } catch (Exception e) {
                List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{accentity}));
                Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101152"),
                        String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
            }
        }
        return minDateUnSettlement;
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
        if (!StringUtils.isEmpty(accentity)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity)));
        }
        if (!StringUtils.isEmpty(currency) ) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").in((Object) currency.split(","))));
        }
        return condition;
    }

    /**
     * 查询条件封装（用于查询日记账）
     * @param accentity
     * @param currency
     * @param cashaccount
     * @param bankaccount
     * @return
     */
    private static QueryConditionGroup getQueryConditionGroupForJournal(String accentity, String currency, String cashaccount,
                                                              String bankaccount) {
        QueryConditionGroup condition = new QueryConditionGroup();
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
        if (!StringUtils.isEmpty(accentity)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity)));
        }
        if (!StringUtils.isEmpty(currency) ) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").in((Object) currency.split(","))));
        }
        return condition;
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
    public static void  getSettleMentDetailFromInitData(String accentity, String currency, String cashaccount, String bankaccount, Date endDate, Map<String, SettlementDetail> settlementDetailMap) throws Exception {
        QueryConditionGroup initDataCondition = getQueryConditionGroup(accentity, currency, cashaccount, bankaccount);
        QuerySchema initDataQuerySchema = QuerySchema.create().addSelect("*");
        initDataQuerySchema.addCondition(initDataCondition);
        List<Map<String, Object>> initDataQuery = MetaDaoHelper.query(InitData.ENTITY_NAME, initDataQuerySchema);
        //没有设置期初的不追加0值
        if(!CollectionUtils.isEmpty(initDataQuery)){
            for (Map<String, Object> initmap : initDataQuery) {
                InitData initData = new InitData();
                initData.init(initmap);
                if (StringUtils.isEmpty(initData.getCurrency())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101153"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807C4","期初余额单据没有币种：") /* "期初余额单据没有币种：" */ + initData.getId());
                }
                String key = initData.getAccentity() + initData.getBankaccount() + initData.getCashaccount() + initData.getCurrency();
                key = key.replace("null", "");
                SettlementDetail settlementDetail = new SettlementDetail();
                settlementDetail.setAccentity(initData.getAccentity());
                //给日结明细的核算会计主体赋值 如果期初没有则进行查询后赋值
                if(initData.getAccentityRaw()!=null){
                    settlementDetail.setAccentityRaw(initData.getAccentityRaw());
                }else{
                    settlementDetail.setAccentityRaw(CmpMetaDaoHelper.getAccentityRaw(initData.getAccentity()));
                }
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
    }


}
