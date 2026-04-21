package com.yonyoucloud.fi.cmp.settlement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.settlementcheckresult.SettlementCheckResult;
import com.yonyoucloud.fi.cmp.settlementcheckresult.SettlementCheckResultb;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.BizException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Created by sz on 2019/4/20 0020.
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class SettlementServiceImpl implements SettlementService {
    private static final String DETAILPREX = "com.yonyoucloud.fi.cmp.mapper.UserMasterorgMapper";
    private static final String SETTLEMENT = "com.yonyoucloud.fi.cmp.mapper.SettleMapper";
    private static final String BILLNUM = "billnum";
    private static final String BILLNUM_VALUE = "cmp_settlementlist";
    private static final String USER_ID = "user_id";
    private static final String TENANT_ID = "tenant_id";
    private static final String Y_TENANT_ID = "ytenant_id";
    private static final String DAILYSETTLECHECKOUT = "dailysettleCheckout";
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private BaseRefRpcService baseRefRpcService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    private static String SETTLEMENTMAPPER = "com.yonyoucloud.fi.cmp.mapper.Settlement.settleCheckForSettle";

    /**
     * 获取选中会计主体下的会计期间
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    @Override
    public CtmJSONObject getSettlementPeriod(String accentity, String period) throws Exception {
        //获取最小未日结日
        CtmJSONObject result = new CtmJSONObject();
        String periodDay = getUnSettleMinDay(accentity, period);
        //插入userMasterOrg表数据
        Map<String,Object> param = new HashMap<String, Object>();
        param.put(BILLNUM,BILLNUM_VALUE);
        param.put(USER_ID,AppContext.getCurrentUser().getId().toString());
        param.put(Y_TENANT_ID,InvocationInfoProxy.getTenantid());
        SqlHelper.delete(DETAILPREX+".delUserMasterorg",param);
        param.put("org",accentity);
        param.put(TENANT_ID,AppContext.getTenantId().toString());
        param.put("id",ymsOidGenerator.nextId());
        SqlHelper.insert(DETAILPREX+".addUserMasterorg",param);
        result.put("periodDay",periodDay);//会计日期 格式 : "2018-10-01"
        List<Map<String, Object>> periodData = new ArrayList<Map<String, Object>>();
        QueryConditionGroup qcg ;
        QuerySchema settleSchema ;
        Map<String ,Object> periodmap = getPeriodByAccentity(accentity);
        Date beginDate = (Date) periodmap.get("begindate");
        Date endDate = (Date) periodmap.get("lastPeriodDate");
        String periodMin = DateUtils.dateToStr(beginDate);
        String periodMax = DateUtils.dateToStr(endDate);
        result.put("periodMin",periodMin);// 格式 : "2018-10-01"
        result.put("periodMax",periodMax);// 格式 : "2018-10-01"
        //查日结明细数据
        qcg = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity),
                QueryCondition.name("settlementdate").between(beginDate,endDate));
        settleSchema = QuerySchema.create().addSelect("accentity, accentity.name,periodcode, settlementdate,settleflag")
                .addCondition(qcg).addOrderBy("settlementdate asc " );
        List<Map<String, Object>>  settmentList = MetaDaoHelper.query(Settlement.ENTITY_NAME,settleSchema);
        String periodcode = "";
        String   dateState = "B";//"A","B"
        for(Map<String, Object> map:settmentList){
            Map<String, Object> temp = new HashMap<String, Object>();
            if(!periodcode.equals((String) map.get("periodcode"))){//&&periodcode.equals((String) map.get("periodcode")
                if("A".equals(dateState)){
                    dateState = "B";
                }else {
                    dateState = "A";
                }
            }
            temp.put("dateState",dateState);
            periodcode = (String) map.get("periodcode");
            temp.put("settlementdate",new SimpleDateFormat("yyyy-MM-dd").format(map.get("settlementdate")));
            temp.put("accentity_name",map.get("accentity_name"));
            temp.put(IBussinessConstant.ACCENTITY,map.get(IBussinessConstant.ACCENTITY));
            temp.put("settleflag",(boolean)map.get("settleflag")?"1":"0");
            periodData.add(temp);
        }
        result.put("periodData",periodData);
        return result;
    }

    public String getUnSettleMinDay(String accentity,String period) throws  Exception{
        String unSettleMinDay = "";
        if(!"".equals(accentity)){
            //查询当前会计主体下，数据库是否有数据--数据初始化
            initSettlementDataByLock(accentity, period);
            QuerySchema querySchema = QuerySchema.create().addSelect("min(settlementdate)");
            QueryConditionGroup group;
            //查询最小未结账日
            group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                    QueryCondition.name("settleflag").eq(0));
            querySchema.addCondition(group);
            Map<String,Object> map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);
            if(null!= map){
                Date minDateUnSettlement = (Date) map.get("min");
                unSettleMinDay = DateUtils.dateToStr(minDateUnSettlement);
            }else{
                //没查到没有数据，说明库里的数据全部已日结  返回最大日结日期
                querySchema = QuerySchema.create().addSelect("max(settlementdate)");
                //查询最大结账日
                group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                        QueryCondition.name("settleflag").eq(1));
                querySchema.addCondition(group);
                map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);
                if(map==null || map.get("max")==null){
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DF55D9A05980000","日结算数据正在初始化，请进行结账检查后稍等。") /* "日结算数据正在初始化，请进行结账检查后稍等。" */);
                }
                Date maxDateSettlement = (Date) map.get("max");
                unSettleMinDay =  DateUtils.dateToStr(maxDateSettlement);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101090"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418040B","请先选择会计主体！") /* "请先选择会计主体！" */);
        }
        return  unSettleMinDay;

    }

    /**
     * 加锁初始化日结数据
     * @param accentity 资金组织
     * @param period 期间
     */
    private void initSettlementDataByLock(String accentity, String period) {
        if(!"".equals(period)){
            return;
        }
        //检查组织锁是否存在
        String locKey = "initSettlementDataByLock:" + accentity;
        YmsLock ymsLock = JedisLockUtils.lockRjWithOutTrace(locKey);
        if (ymsLock == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101098"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CF", "该组织下正在日结，请稍后再试！") /* "该组织下正在日结，请稍后再试！" */);
        }
        try {
            initSettlementDate(accentity);
        } catch (Exception e) {
            log.error("initSettlementDataByLock error {}", e.getMessage(), e);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540058C", "服务异常，请稍后重试！") /* "服务异常，请稍后重试！" */);
        } finally {
            JedisLockUtils.unlockRjWithOutTrace(ymsLock);
        }
    }

    /**
     * 初始化数据
     * @param accentity
     * @throws Exception
     */
    public void initSettlementDate(String accentity) throws Exception {
        Map<String ,Object> periodmap;
        try {
            periodmap = getPeriodByAccentity(accentity);
        } catch (Exception e) {
            List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Arrays.asList(new String[]{accentity}));
            Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101091"),
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101092"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180418","未能获取到会计主体对应的会计期间！") /* "未能获取到会计主体对应的会计期间！" */);
        }
        Date endDate = (Date) periodmap.get("lastPeriodDate");
        Date beginDate;
        //有数据，查询数据中的最大日期
        QuerySchema querySchema = QuerySchema.create().addSelect("min(settlementdate), max(settlementdate)");
        QueryConditionGroup group;
        group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity));
        querySchema.addCondition(group);
        Map<String,Object> map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);
        if(null == map || map.isEmpty() || map.get("min") == null){
            beginDate = (Date) periodmap.get("begindate");
        }else{
            Date minDate = (Date) map.get("min");
            if(minDate.compareTo((Date) periodmap.get("begindate")) != 0){
                //日结主表初始化数据不等于业务期初期间
                //删除全部旧数据，重新初始化
                MetaDaoHelper.batchDelete(Settlement.ENTITY_NAME, Lists.newArrayList(new SimpleCondition(IBussinessConstant.ACCENTITY, ConditionOperator.eq, accentity)));
                beginDate = (Date) periodmap.get("begindate");
            }else{
                //有数据，查询数据中的最大日期
                Date maxDate = (Date) map.get("max");
                beginDate = DateUtils.dateAddDays(maxDate,1);
            }
        }
        List<Settlement> settlementList = initSettlementByPeriod(beginDate,endDate,accentity);
        if (CollectionUtils.isNotEmpty(settlementList)) {
            CmpMetaDaoHelper.insert(Settlement.ENTITY_NAME,settlementList);
        }
    }

    public List<Settlement> initSettlementByPeriod(Date beginDate,Date endDate,String accentity)throws  Exception{
        Date begindateTemp = null ;
        Date endDateTemp = null ;
        List<Settlement> settlementList = new ArrayList<Settlement>();
        Map<String, Object> accBody = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accentity).get(0);
        String accperiodschemeId = accBody.get("periodschema").toString();
        QuerySchema periodSchema = QuerySchema.create().addSelect("id,code,begindate,enddate").appendQueryCondition(
                QueryCondition.name("accperiodscheme").eq(accperiodschemeId),
                QueryCondition.name("begindate").elt(endDate),QueryCondition.name("enddate").egt(beginDate));
        periodSchema.addOrderBy(new QueryOrderby("begindate", "asc"));
        List<Map<String, Object>> periodList = MetaDaoHelper.query("bd.period.Period", periodSchema, ISchemaConstant.MDD_SCHEMA_FINBD);
        if(periodList.size()>0){
            for(int i =0; i<periodList.size();i++){
                Map period =  periodList.get(i);
                Long periodId = (Long) period.get("id");
                String code =   period.get("code").toString();
                if(i==0){
                   begindateTemp = beginDate;
                }else{
                    begindateTemp = (Date) period.get("begindate");
                }
                if(i==periodList.size()-1){
                	endDateTemp = endDate;
                }else{
                	endDateTemp = (Date)period.get("enddate");
                }
	            int dayNum = DateUtils.dateBetweenIncludeToday(begindateTemp,endDateTemp);
	            for(int j=0;j<dayNum;j++){
	                Settlement settlement = new Settlement();
	                settlement.setAccentity(accentity);
	                settlement.setPeriod(periodId);
	                settlement.setPeriodcode(code);
	                settlement.setSettleflag(false);
	                settlement.setSettlementdate(begindateTemp);
                    settlement.setEntityStatus(EntityStatus.Insert);
                    settlement.setId(ymsOidGenerator.nextId());
	                settlementList.add(settlement);
	                begindateTemp = DateUtils.dateAddDays(begindateTemp,1);
	            }
            }
        }
        return settlementList;
    }


    @Override
    public Map<String,Object>  getPeriodByAccentity(String accentity) throws Exception{
//        Long accBookTypeId =  FINBDApiUtil.getFI4BDService().getAccBookTypeByAccBody(accentity);//根据会计主体获取业务账簿id
        Map<String ,Object> lastPeriod = QueryBaseDocUtils.getLastPeriodByAccentity(accentity);
//        String periodt  =  FINBDApiUtil.getFI4BDService().getPeriodByModule(accBookTypeId, "CM");
//        Map<String ,Object> periodmap = FINBDApiUtil.getFI4BDAccPeriodService().getPeriodByParam(null,periodt,null,
//                String.valueOf(accBookTypeId),new String[]{"id","code","begindate","enddate"});
        Date periodt = QueryBaseDocUtils.queryOrgPeriodBeginDate(accentity);
        //用会计主体才能查到，查的是期间方案
        Map<String ,Object> periodmap = QueryBaseDocUtils.queryPeriodByAccbodyAndDate(accentity, periodt);
        periodmap.put("lastPeriodDate",lastPeriod.get("enddate"));
        return  periodmap;
    }

    @Override
    public void checkSettleForAcctParentAccentity(String accentity,String bankacctId,Date date,EntityStatus entityStatus) throws Exception {
        if (bankacctId != null && date != null) {
            String accentityName = "";
            //通过当前账户 查询所属组织
            EnterpriseBankAcctVOWithRange acctVo = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bankacctId);
            if(acctVo == null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101093"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800E3", "账户未找到,请检查账户！") /* "账户未找到,请检查账户！" */);
            }
            List<OrgRangeVO> orgRangeVOList = acctVo.getAccountApplyRange();
            for(OrgRangeVO orgRangeVO:orgRangeVOList){
               if(orgRangeVO.getRangeOrgId().equals(accentity)){
                   accentityName = orgRangeVO.getRangeOrgIdName();
               }
            }
            //查询所属组织的最大日结日期
            Date maxDate = getMaxSettleDate(accentity);
            //若存在最大日结日期 逐个与传入日期比较 若有一个不符合 则提示
            if (maxDate != null) {
                maxDate = DateUtils.getLastTimeForThisDate(maxDate);
                if (maxDate.compareTo(date) >= 0) {
                    String action = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F740F6A05600007", "已做日结处理，不允许登记银行日记账");
                    if(entityStatus == EntityStatus.Update){
                        action = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F74108204600007", "已做日结处理，不允许更新银行日记账");
                    }else if(entityStatus == EntityStatus.Delete){
                        action = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F7410FA05600007", "已做日结处理，不允许删除银行日记账");
                    }

                    String account = acctVo.getAccount();
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_18108D8404B80ED8", "银行账户")
                            + account + "，" +//@notranslate
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-UI_1AA1B4D205C801B7", "授权使用组织") + ":"
                            + accentityName +"，"+action);//@notranslate

                }
            }
        }else{//bankacctId为空说明为现金账
            //查询所属组织的最大日结日期
            Date maxDate = getMaxSettleDate(accentity);
            //若存在最大日结日期 逐个与传入日期比较 若有一个不符合 则提示
            if (maxDate != null) {
                maxDate = DateUtils.getLastTimeForThisDate(maxDate);
                if (maxDate.compareTo(date) >= 0) {
                    String action = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400589", "已做日结处理，不允许登记现金日记账") /* "已做日结处理，不允许登记现金日记账" */;
                    if(entityStatus == EntityStatus.Update){
                        action = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540058A", "已做日结处理，不允许更新现金日记账") /* "已做日结处理，不允许更新现金日记账" */;
                    }else if(entityStatus == EntityStatus.Delete){
                        action = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540058B", "已做日结处理，不允许删除现金日记账") /* "已做日结处理，不允许删除现金日记账" */;
                    }
                    throw new CtmException(action);
                }
            }

        }
    }

    /**
     * 获取日结检查项
     * @return   checkItemList
     * @throws Exception
     */
    @Override
    public List<Map<String,Object>> getCheckItem() throws Exception {
        List<Map<String,Object>> checkItemList = new ArrayList<Map<String,Object>>();
        Map<String,Object>  checkItem = new HashMap<String, Object>();
        String locale = InvocationInfoProxy.getLocale();
        log.info("local = {}, {}", locale, AppContext.getUserLocale());
        checkItem.put("checkresult", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180413","未检查") /* "未检查" */);
        checkItem.put("name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE2AAE05A80001","单据审核检查") /* "单据审核检查" */);//com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153426")
        checkItem.put("id", "1");
        checkItemList.add(checkItem);
        checkItem = new HashMap<String, Object>();
        checkItem.put("checkresult", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180413","未检查") /* "未检查" */);
        checkItem.put("name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE2AAE05A80002","单据结算检查") /* "单据结算检查" */);//com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153427")
        checkItem.put("id", "2");
        checkItemList.add(checkItem);
        checkItem = new HashMap<String, Object>();
        checkItem.put("checkresult", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180413","未检查") /* "未检查" */);
        checkItem.put("name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE2AAE05A80000","月末汇兑损益计算检查") /* "月末汇兑损益计算检查" */);//com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153428")
        checkItem.put("id", "3");
        checkItemList.add(checkItem);
        return checkItemList;
    }


    /**
     * 获取默认会计主体
     */
    @Override
    public  Map<String ,Object>  getDefaultAccentity() throws  Exception{
        String accentity = "";
        String accentity_name ="";
        Map<String,Object> map = new HashMap();
        if(FIDubboUtils.isSingleOrg()){
            BizObject singleOrg = FIDubboUtils.getSingleOrg();
            if(singleOrg != null){
                accentity = singleOrg.get("id");
                accentity_name = singleOrg.get("name");
            }
        }else{
            //如果设置了默认会计主体 则赋值
            String userId  = InvocationInfoProxy.getDefaultOrg();
            if(userId != null && !"".equals(userId)){
                QuerySchema querySchema = QuerySchema.create().addSelect("code,name,id");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(userId));
                querySchema.addCondition(group);
                List<Map<String, Object>> defaultAccentity = MetaDaoHelper.query("aa.baseorg.OrgMV", querySchema, ISchemaConstant.MDD_SCHEMA_ORGCENTER);
                if(defaultAccentity!=null && defaultAccentity.size()>0){
                    accentity = defaultAccentity.get(0).get("id").toString();
                    accentity_name = defaultAccentity.get(0).get("name").toString();
                }
            }else{
                    Map<String,Object> param = new HashMap();
                    param.put(BILLNUM,BILLNUM_VALUE);
                    param.put(USER_ID,AppContext.getCurrentUser().getId().toString());
                    param.put(Y_TENANT_ID,InvocationInfoProxy.getTenantid());
                    Map<String,Object> maptemp = SqlHelper.selectOne(DETAILPREX+".queryUserMasterorg",param);
                    if(null!= maptemp){
                        accentity = String.valueOf(maptemp.get("org"));
                        Set<String> ids = AuthUtil.getOrgPermissions("ficmp0013");
                        Set<String> dataPermsIds = AuthUtil.getOrgDataPermissions("CM");
                        Set<String> idsUnion = new HashSet();
                        if(ids != null && dataPermsIds == null) {
                            idsUnion.addAll(ids);
                        }else if(dataPermsIds != null && ids == null) {
                            idsUnion.addAll(dataPermsIds);
                        }else if(ids != null&&dataPermsIds != null){
                            idsUnion.addAll(ids);
                            idsUnion.retainAll(dataPermsIds);
                        }
                        if(!idsUnion.contains(accentity)) {
                            return  new HashMap();
                        }
                        BillContext billContext = new BillContext();
                        billContext.setFullname("aa.baseorg.OrgMV");
                        billContext.setDomain(IDomainConstant.MDD_DOMAIN_ORGCENTER);
                        QueryConditionGroup groupOrg = QueryConditionGroup.and(QueryCondition.name("id").eq(accentity),QueryCondition.name("stopstatus").eq(0));
                        List<Map<String, Object>> dataOrgList = MetaDaoHelper.queryAll(billContext,"id,code,name,tenant",groupOrg,null);
                        if(dataOrgList.size()>0){
                            accentity_name =String.valueOf( dataOrgList.get(0).get("name"));
                        }else{
                            return  new HashMap();
                        }
                    }else{
                        return  new HashMap();
                    }
                }
            }
        map.put(IBussinessConstant.ACCENTITY,accentity);
        map.put("accentity_name",accentity_name);
        return map;
    }

//    private Set<String> getDataPermsByServiceCode(String authId) throws Exception {
//        QuerySchema queryschema =  new QuerySchema().addSelect("subId");
//        queryschema.appendQueryCondition(QueryCondition.name("code").eq(authId));
//        List<Map<String,Object>> subResutl = MetaDaoHelper.query("sys.auth.Auth", queryschema);
//        if(subResutl == null || subResutl.isEmpty())
//            return null;
//        Set<String> ids = AuthUtil.getOrgDataPermissions((String)subResutl.get(0).get("subId"));
//        return ids;
//    }

    /**
     * 获取日结检查结果
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    @Override
    public CtmJSONObject settleCheck(String accentity, String period, boolean isAuto, String settleFlag) throws Exception{
        CtmJSONObject json = new CtmJSONObject();
        String  resultFlag = "pass";
        Date    minErrorDate = null;
        List<Map<String, Object>>  resultList = new ArrayList<Map<String, Object>>();
        if(!"".equals(accentity)){
            if(!"".equals(period)){
                //获取日结检查项
                List<Map<String,Object>> checkItemList  = getCheckItem();
                //获取最小未结账日
                String minUnSettleDay = getUnSettleMinDay(accentity,period.substring(0,7));
                Date minUnsetDay = DateUtils.strToDate(minUnSettleDay);
                //获取现金参数--存在未审核单据是否允许日结
                Map<String, Object> checkDailySettlementMap = queryAutoConfigByAccentity(accentity);
                boolean checkDailySettlement = false;
                if(null != checkDailySettlementMap && null != checkDailySettlementMap.get("checkDailySettlement")){
                    checkDailySettlement = (boolean) checkDailySettlementMap.get("checkDailySettlement");
                }
                List<String> resultFlagList = new ArrayList<>();
                for (Map<String,Object> checkItem :checkItemList) {
                    String id = (String)checkItem.get("id");
                    String  name =(String) checkItem.get("name");
                    switch (id){
                        case "1":
                            Map<String,Object> checkAudit = SettleCheckUtil.checkAudit(accentity,period,name,minUnsetDay,checkDailySettlement);
                            resultList.add(checkAudit);
                            String checkFlag = (String) checkAudit.get("checkResult");
                            if(!checkDailySettlement && "error".equals(checkFlag)){
                                resultFlag = checkFlag;
                            }
                            resultFlagList.add(resultFlag);
                            break;
                        case "2":
                            Map<String,Object> checkSettle = SettleCheckUtil.checkSettle(accentity,period,name,minUnsetDay);
                            resultList.add(checkSettle);
                            checkFlag = (String) checkSettle.get("checkResult");
                            if("warning".equals(checkFlag)){
                                resultFlag = checkFlag;
                            }
                            resultFlagList.add(resultFlag);
                            break;
                        case "3":
                            Map<String,Object> checkExchangeGainsAndLosses = SettleCheckUtil.checkExchangeGainsAndLosses(accentity,period,name,isAuto);
                            resultList.add(checkExchangeGainsAndLosses);
                            checkFlag = (String) checkExchangeGainsAndLosses.get("checkResult");
                            if("warning".equals(checkFlag)){
                                resultFlag = checkFlag;
                            }
                            resultFlagList.add(resultFlag);
                            break;
                        default:
                            continue;
                    }
                }

//                for (Map<String,Object> checkItem :checkItemList) {
//                    String id = (String)checkItem.get("id");
//                    String  name =(String) checkItem.get("name");
//                    String salaryFlag = "pass";
//                    Map<String, Object> stringObjectMapSalary = new HashMap<String, Object>();
//                    switch (id){
//                        case "1":
//                            //收款单审核：
//                            checkResult = SettleCheckUtil.checkAuditStatus(ReceiveBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.ReceiveBill,minErrorDate);
//                            String checkFlag = (String) checkResult.get("checkResult");
//                            if("error".equals(checkFlag)){
//                                resultFlag = "error";
//                            }
//                            minErrorDate = (Date) checkResult.get("minErrorDate");
//                        	resultList.add(checkResult);
//                            break;
//                        case "2":
//                            //收款单结算：
//                            checkResultList = new ArrayList<Map<String, Object>>();
//                            result = new HashMap<String,Object>();
//                            Map<String, Object> map = SettleCheckUtil.checkSettleStatus(ReceiveBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.ReceiveBill, "pass");
//                            checkResultList.add(map);
//                            result.put("checkName",name);
//                            if("pass".equals(resultFlag)){
//                                resultFlag = (String) map.get("flag");
//                            }
//                            result.put("checkResult",map.get("flag"));
//                            result.put("checkdetail",checkResultList);
//                            resultList.add(result);
//                            break;
//                        case "3":
//                            //付款单审核
//                            checkResult = SettleCheckUtil.checkAuditStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent,minErrorDate);
//                            checkFlag = (String) checkResult.get("checkResult");
//                            if("error".equals(checkFlag)){
//                                resultFlag = "error";
//                            }
//                            minErrorDate = (Date) checkResult.get("minErrorDate");
//                            resultList.add(checkResult);
//                            break;
//                        case "4":
//                            checkResultList = new ArrayList<Map<String, Object>>();
//                            result = new HashMap<String,Object>();
//                            String flag ="pass";
//                            //付款单结算：
//                            Map<String, Object> stringObjectMap = SettleCheckUtil.checkSettleStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent,"pass");
//                            checkResultList.add(stringObjectMap);
//                            if("pass".equals(resultFlag)){
//                                resultFlag = (String) stringObjectMap.get("flag");
//                            }
//                            flag = (String) stringObjectMap.get("flag");
//
//                            //2、支付失败
//                            Map<String, Object> stringObjectMap1 = SettleCheckUtil.checkPayFailStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent,(String)stringObjectMap.get("flag"));
//                            checkResultList.add(stringObjectMap1);
//                            if("pass".equals(resultFlag)){
//                                resultFlag = (String) stringObjectMap1.get("flag");
//                            }
//                            if(!"pass".equals((String) stringObjectMap1.get("flag"))){
//                                flag = (String) stringObjectMap1.get("flag");
//                            }
//
//                            //3、支付不明
//                            Map<String, Object> stringObjectMap2 = SettleCheckUtil.checkPayUnknownStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent,(String)stringObjectMap1.get("flag"));
//                            checkResultList.add(stringObjectMap2);
//                            if("pass".equals(resultFlag)){
//                                resultFlag = (String) stringObjectMap2.get("flag");
//                            }
//                            if(!"pass".equals((String) stringObjectMap2.get("flag"))){
//                                flag = (String) stringObjectMap1.get("flag");
//                            }
//
//                            //4、预下单失败
//                            Map<String, Object> stringObjectMap3 = SettleCheckUtil.checkPreFailStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent,(String)stringObjectMap2.get("flag"));
//                            checkResultList.add(stringObjectMap3);
//                            if("pass".equals(resultFlag)){
//                                resultFlag = (String) stringObjectMap3.get("flag");
//                            }
//                            if(!"pass".equals((String) stringObjectMap3.get("flag"))){
//                                flag = (String) stringObjectMap1.get("flag");
//                            }
//
//                            //5、预下单成功
//                            Map<String, Object> stringObjectMap4 = SettleCheckUtil.checkPreSuccessStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent,(String)stringObjectMap3.get("flag"));
//                            checkResultList.add(stringObjectMap4);
//                            if("pass".equals(resultFlag)){
//                                resultFlag = (String) stringObjectMap4.get("flag");
//                            }
//                            if(!"pass".equals((String) stringObjectMap4.get("flag"))){
//                                flag = (String) stringObjectMap1.get("flag");
//                            }
//
//                            //6、支付中
//                            Map<String, Object> stringObjectMap5 = SettleCheckUtil.checkPayingStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent,(String)stringObjectMap4.get("flag"));
//                            checkResultList.add(stringObjectMap5);
//                            if("pass".equals(resultFlag)){
//                                resultFlag = (String) stringObjectMap5.get("flag");
//                            }
//                            if(!"pass".equals((String) stringObjectMap5.get("flag"))){
//                                flag = (String) stringObjectMap1.get("flag");
//                            }
//
//                            //7、日记账中银行账号与现金账号都为空
//                            Map<String, Object> stringObjectMap6 = SettleCheckUtil.checkAccountNOStatus(PayBill.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.PayMent,(String)stringObjectMap5.get("flag"),minErrorDate);
//                            checkResultList.add(stringObjectMap6);
//                            if("error".equals((String) stringObjectMap6.get("flag"))){
//                                resultFlag = "error";
//                                flag = "error";
//                            }
//                            minErrorDate = (Date) stringObjectMap6.get("minErrorDate");
//                            result.put("checkName",name);
//                            result.put("checkResult",flag);
//                            result.put("checkdetail",checkResultList);
//                            resultList.add(result);
//                            break;
//                        case "5":
//                            //转账单审核：
//                            checkResult = SettleCheckUtil.checkAuditStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount,minErrorDate);
//                            checkFlag = (String) checkResult.get("checkResult");
//                            if("error".equals(checkFlag)){
//                                resultFlag = "error";
//                            }
//                            minErrorDate = (Date) checkResult.get("minErrorDate");
//                            resultList.add(checkResult);
//                            break;
//                       case "6":
//                    	    checkResultList = new ArrayList<Map<String, Object>>();
//                        	result = new HashMap<String,Object>();
//                           	//转账单结算：
//                           String taFlag = "pass";
//                           Map<String, Object> stringObjectMap7 = SettleCheckUtil.checkSettleStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount,"pass");
//                           checkResultList.add(stringObjectMap7);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMap7.get("flag");
//                           }
//                           taFlag = (String) stringObjectMap7.get("flag");
//
//                       		//2、转账单支付失败
//                           Map<String, Object> stringObjectMap8 = SettleCheckUtil.checkPayFailStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount, (String) stringObjectMap7.get("flag"));
//                           checkResultList.add(stringObjectMap8);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMap8.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMap8.get("flag"))){
//                               taFlag = (String) stringObjectMap8.get("flag");
//                           }
//
//                       		//3、转账单支付不明
//                           Map<String, Object> stringObjectMap9 = SettleCheckUtil.checkPayUnknownStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount, (String) stringObjectMap8.get("flag"));
//                           checkResultList.add(stringObjectMap9);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMap9.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMap9.get("flag"))){
//                               taFlag = (String) stringObjectMap9.get("flag");
//                           }
//
//                       		//4、转账单预下单失败
//                           Map<String, Object> stringObjectMap10 = SettleCheckUtil.checkPreFailStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount,(String)stringObjectMap9.get("flag"));
//                    	   checkResultList.add(stringObjectMap10);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMap10.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMap10.get("flag"))){
//                               taFlag = (String) stringObjectMap10.get("flag");
//                           }
//
//                       		//5、转账单预下单成功
//                           Map<String, Object> stringObjectMap11 = SettleCheckUtil.checkPreSuccessStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount,(String)stringObjectMap10.get("flag"));
//                    	   checkResultList.add(stringObjectMap11);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMap11.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMap11.get("flag"))){
//                               taFlag = (String) stringObjectMap11.get("flag");
//                           }
//
//                       		//6、转账单支付中
//                           Map<String, Object> stringObjectMap12 = SettleCheckUtil.checkPayingStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount,(String)stringObjectMap11.get("flag"));
//                    	   checkResultList.add(stringObjectMap12);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMap12.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMap12.get("flag"))){
//                               taFlag = (String) stringObjectMap12.get("flag");
//                           }
//                    	   result.put("checkName",name);
//                       	   result.put("checkResult",taFlag);
//                    	   result.put("checkdetail",checkResultList);
//                       	   resultList.add(result);
//
//                       		//7、转账单日记账中银行账号与现金账号都为空
//                    	   //resultList.add(SettleCheckUtil.checkAccountNOStatus(TransferAccount.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.TransferAccount));
//                           break;
//                        case "7":
//                            //外币兑换单审核：
//                            checkResult = SettleCheckUtil.checkAuditStatus(CurrencyExchange.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.CurrencyExchangeBill,minErrorDate);
//                            checkFlag = (String) checkResult.get("checkResult");
//                            if("error".equals(checkFlag)){
//                                resultFlag = "error";
//                            }
//                            minErrorDate = (Date) checkResult.get("minErrorDate");
//                            resultList.add(checkResult);
//                            break;
//                        case "8":
//                            //外币兑换单结算：
//                            checkResultList = new ArrayList<Map<String, Object>>();
//                            result = new HashMap<String,Object>();
//                            map = SettleCheckUtil.checkSettleStatus(CurrencyExchange.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.CurrencyExchangeBill, "pass");
//                            checkResultList.add(map);
//                            result.put("checkName",name);
//                            if("pass".equals(resultFlag)){
//                                resultFlag = (String) map.get("flag");
//                            }
//                            result.put("checkResult",map.get("flag"));
//                            result.put("checkdetail",checkResultList);
//                            resultList.add(result);
//                            break;
//                        case "9":
//                            //薪资支付审核：
//                            checkResult = SettleCheckUtil.checkAuditStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment,minErrorDate);
//                            checkFlag = (String) checkResult.get("checkResult");
//                            if("error".equals(checkFlag)){
//                                resultFlag = "error";
//                            }
//                            minErrorDate = (Date) checkResult.get("minErrorDate");
//                            resultList.add(checkResult);
//                            break;
//                       case "10":
//                    	    checkResultList = new ArrayList<Map<String, Object>>();
//                        	result = new HashMap<String,Object>();
//                           	//薪资支付结算：
//                           stringObjectMapSalary = SettleCheckUtil.checkSettleStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment,"pass");
//                           checkResultList.add(stringObjectMapSalary);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//                           salaryFlag = (String) stringObjectMapSalary.get("flag");
//
//                       		//2、薪资支付失败
//                           stringObjectMapSalary = SettleCheckUtil.checkPayFailStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment, (String) stringObjectMapSalary.get("flag"));
//                           checkResultList.add(stringObjectMapSalary);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMapSalary.get("flag"))){
//                               salaryFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//
//                       		//3、薪资支付不明
//                           stringObjectMapSalary = SettleCheckUtil.checkPayUnknownStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment, (String) stringObjectMapSalary.get("flag"));
//                           checkResultList.add(stringObjectMapSalary);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMapSalary.get("flag"))){
//                               salaryFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//
//                       		//4、薪资预下单失败
//                           stringObjectMapSalary = SettleCheckUtil.checkPreFailStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment,(String)stringObjectMapSalary.get("flag"));
//                    	   checkResultList.add(stringObjectMapSalary);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMapSalary.get("flag"))){
//                               salaryFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//
//                       		//5、薪资预下单成功
//                           stringObjectMapSalary = SettleCheckUtil.checkPreSuccessStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment,(String)stringObjectMapSalary.get("flag"));
//                    	   checkResultList.add(stringObjectMapSalary);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMapSalary.get("flag"))){
//                               salaryFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//
//                       		//6、薪资支付中
//                           stringObjectMapSalary = SettleCheckUtil.checkPayingStatus(Salarypay.ENTITY_NAME, accentity, period, name, minUnsetDay, EventType.SalaryPayment,(String)stringObjectMapSalary.get("flag"));
//                    	   checkResultList.add(stringObjectMapSalary);
//                           if("pass".equals(resultFlag)){
//                               resultFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//                           if(!"pass".equals((String) stringObjectMapSalary.get("flag"))){
//                               salaryFlag = (String) stringObjectMapSalary.get("flag");
//                           }
//                    	   result.put("checkName",name);
//                       	   result.put("checkResult",salaryFlag);
//                    	   result.put("checkdetail",checkResultList);
//                       	   resultList.add(result);
//                           break;
//                        default:
//                            continue;
//                    }
//                }//for循环结束
                json.put("data",resultList);
                if(resultFlagList.contains("error")){
                    resultFlag = "error";
                }else{
                    resultFlag = "pass";
                }
                json.put("checkrResult",resultFlag);
                json.put("minErrorDate",minErrorDate);
            }else{
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101096"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180416","请选择日结检查日期！") /* "请选择日结检查日期！" */);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101090"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418040B","请先选择会计主体！") /* "请先选择会计主体！" */);
        }
        //保存检查结果
        this.saveResultCtmJSONObject(json, accentity, period, settleFlag);
        return json;
    }



    /**
     * 保存手工日结检查结果 zxl
     *
     * @param json
     * @param accentity  会计主体
     * @param settleFlag 是否结账
     */
    public void saveResultCtmJSONObject(CtmJSONObject json, String accentity, String period, String settleFlag) throws Exception {
        CtmJSONArray dataArr = json.getJSONArray("data");
        Boolean isSave = false;
        for (int i = 0; i < dataArr.size(); i++) {
            CtmJSONObject data = dataArr.getJSONObject(i);
            String checkResult = data.getString(ISettlementConstant.CHECKRESULT);
            //异常和警告进行保存
            if (org.apache.commons.lang3.StringUtils.equals(ISettlementConstant.CHECKRESULT_WARNING, checkResult) ||
                    org.apache.commons.lang3.StringUtils.equals(ISettlementConstant.CHECKRESULT_ERROR, checkResult)) {
                isSave = true;
                break;
            }
        }
        if (!isSave && !org.apache.commons.lang3.StringUtils.equals("Y", settleFlag)) {
            log.info("saveResultCtmJSONObject = {}, {}", accentity + accentity, "不保存");
            //删除检查结果
            this.deleteCheckResult(accentity, period);
            return;
        }
        List<SettlementCheckResult> list = new ArrayList<>();
        for (int i = 0; i < dataArr.size(); i++) {
            CtmJSONObject checkDetailObj = dataArr.getJSONObject(i);
            CtmJSONArray checkdetailArr = checkDetailObj.getJSONArray("checkdetail");
            if (checkdetailArr == null || checkdetailArr.size() == 0) {
                //保存检查结果
                SettlementCheckResult settlementCheckResult = new SettlementCheckResult();
                //最终检查结果
                settlementCheckResult.setCheckrResult(json.getString(ISettlementConstant.CHECKRRESULT));
                settlementCheckResult.setId(ymsOidGenerator.nextId());
                settlementCheckResult.setEntityStatus(EntityStatus.Insert);
                settlementCheckResult.setAccentity(accentity);
                settlementCheckResult.setPeriod(period);
                settlementCheckResult.setSettleFlag(settleFlag);
                settlementCheckResult.setCheckName(checkDetailObj.getString(ISettlementConstant.CHECKNAME));
                settlementCheckResult.setCheckResult(checkDetailObj.getString(ISettlementConstant.CHECKRESULT));
                settlementCheckResult.setMessageAdjustment(checkDetailObj.getString(ISettlementConstant.MESSAGEADJUSTMENT));
                list.add(settlementCheckResult);
                continue;
            }
            for (int j = 0; j < checkdetailArr.size(); j++) {
                CtmJSONObject detailObj = checkdetailArr.getJSONObject(j);
                //保存检查结果
                SettlementCheckResult settlementCheckResult = new SettlementCheckResult();
                //最终检查结果
                settlementCheckResult.setCheckrResult(json.getString(ISettlementConstant.CHECKRRESULT));
                Long id = ymsOidGenerator.nextId();
                settlementCheckResult.setId(id);
                settlementCheckResult.setEntityStatus(EntityStatus.Insert);
                settlementCheckResult.setAccentity(accentity);
                settlementCheckResult.setPeriod(period);
                settlementCheckResult.setSettleFlag(settleFlag);
                settlementCheckResult.setCheckName(checkDetailObj.getString(ISettlementConstant.CHECKNAME));
                settlementCheckResult.setCheckResult(checkDetailObj.getString(ISettlementConstant.CHECKRESULT));
                settlementCheckResult.setMessageAdjustment(checkDetailObj.getString(ISettlementConstant.MESSAGEADJUSTMENT));
                settlementCheckResult.setCheckResultDetail(detailObj.getString(ISettlementConstant.CHECKRESULT));//checkdetail
                settlementCheckResult.setMessageLocale(detailObj.getString(ISettlementConstant.MESSAGELOCALE));
                settlementCheckResult.setCheckRule(detailObj.getString(ISettlementConstant.CHECKRULE));
                settlementCheckResult.setMessage(detailObj.getString(ISettlementConstant.MESSAGE));
                CtmJSONArray detailArr = detailObj.getJSONArray("detail");
                list.add(settlementCheckResult);
                if (detailArr == null || detailArr.size() == 0) {
                    continue;
                }
                List<SettlementCheckResultb> detailList = new ArrayList<>();
                for (int l = 0; l < detailArr.size(); l++) {
                    //保存检查结果明细
                    CtmJSONObject detail = detailArr.getJSONObject(l);
                    SettlementCheckResultb checkResultb = new SettlementCheckResultb();
                    checkResultb.setMainid(id);
                    checkResultb.setId(ymsOidGenerator.nextId());
                    checkResultb.setEntityStatus(EntityStatus.Insert);
                    checkResultb.setType(detail.getString(ISettlementConstant.TYPE));
                    checkResultb.setOrderno(detail.getString(ISettlementConstant.ORDERNO));
                    checkResultb.setDate(detail.getString(ISettlementConstant.DATE));
                    checkResultb.setErrorMessage(detail.getString(ISettlementConstant.ERRORMESSAGE));
                    detailList.add(checkResultb);
                }
                settlementCheckResult.setSettlementCheckResultb(detailList);

            }
        }
        this.deleteCheckResult(accentity, period);
        CmpMetaDaoHelper.insert(SettlementCheckResult.ENTITY_NAME, list);

    }

    /**
     * 删除手工日结检查结果
     *
     * @param accentity 会计主体
     * @param period    期间
     * @throws Exception
     */
    private void deleteCheckResult(String accentity, String period) throws Exception {
        //在执行日结的情况下 如果日结校验有告警 最多或执行 3次删除+插入的操作 此时MetaDaoHelper.delete存在pubts校验问题 这里尝试改为mapper删除
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
        conditionGroup.appendCondition(QueryCondition.name("period").eq(period));
        schema.addCondition(conditionGroup);
        List<SettlementCheckResult> checkResults = MetaDaoHelper.queryObject(SettlementCheckResult.ENTITY_NAME, schema, null);
        List<Long> checkResultIds = new ArrayList<>();
        Map<String, Object> param = new HashMap<>();
        if (checkResults != null && checkResults.size() > 0) {
            for (SettlementCheckResult checkResult : checkResults) {
                checkResultIds.add(Long.valueOf(checkResult.getId().toString()));
//                QueryConditionGroup condition = new QueryConditionGroup();
//                condition.addCondition(QueryConditionGroup.and(
//                        QueryCondition.name("mainid").eq(checkResult.getId())));
//                QuerySchema querySchemaMin = QuerySchema.create().addSelect("*");
//                querySchemaMin.addCondition(condition);
//                List<SettlementCheckResultb> subList = MetaDaoHelper.queryObject(SettlementCheckResultb.ENTITY_NAME, querySchemaMin, null);
//                if (subList != null && subList.size() > 0) {
//                    MetaDaoHelper.delete(SettlementCheckResultb.ENTITY_NAME, subList);
//                }
            }
            param.put("ytenant_id",AppContext.getYTenantId());
            param.put("ids",checkResultIds);
            SqlHelper.delete(SETTLEMENT+".delSettlementCheckResult",param);
            SqlHelper.delete(SETTLEMENT+".delSettlementCheckResultb",param);
//            MetaDaoHelper.delete(SettlementCheckResult.ENTITY_NAME, checkResults);

        }
    }

    /**
     * 查询手工日结检查结果
     *
     * @param accentity 会计主体
     * @param period    期间
     * @throws Exception
     */
    public CtmJSONObject queryCheckResult(String accentity, String period) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(accentity));
        conditionGroup.appendCondition(QueryCondition.name("period").eq(period));
        schema.addCondition(conditionGroup);
        List<SettlementCheckResult> checkResults = MetaDaoHelper.queryObject(SettlementCheckResult.ENTITY_NAME, schema, null);
        if (checkResults != null && checkResults.size() > 0) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            Map<String, List<SettlementCheckResult>> map = checkResults.stream().collect(Collectors.groupingBy(SettlementCheckResult::getCheckName));
            List<Map<String, Object>> data = new ArrayList<>();
            for (Map.Entry<String, List<SettlementCheckResult>> itemMap : map.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                List<Map<String, Object>> checkdetail = new ArrayList<>();
                List<SettlementCheckResult> value = itemMap.getValue();
                value.stream().forEach(ite -> {
                    Map<String, Object> map1 = new HashMap<>();
                    List<Map<String, Object>> detailList = new ArrayList<>();
                    //  List<SettlementCheckResultb> subList = ite.getBizObjects("SettlementCheckResultb", SettlementCheckResultb.class);
                    QueryConditionGroup condition = new QueryConditionGroup();
                    condition.addCondition(QueryConditionGroup.and(
                            QueryCondition.name("mainid").eq(ite.getId())));
                    QuerySchema querySchemaMin = QuerySchema.create().addSelect("*");
                    querySchemaMin.addCondition(condition);
                    try {
                        List<SettlementCheckResultb> subList = MetaDaoHelper.queryObject(SettlementCheckResultb.ENTITY_NAME, querySchemaMin,
                                null);
                        if (subList != null && subList.size() > 0) {
                            subList.stream().forEach(ite1 -> {
                                Map<String, Object> map2 = new HashMap<>();
                                map2.put(ISettlementConstant.TYPE, ite1.getType());
                                map2.put(ISettlementConstant.ORDERNO, ite1.getOrderno());
                                map2.put(ISettlementConstant.DATE, ite1.getDate());
                                map2.put(ISettlementConstant.ERRORMESSAGE, ite1.getErrorMessage());
                                detailList.add(map2);
                            });
                        }
                    } catch (Exception e) {
                        log.error("queryCheckResult{}", e.getMessage(), e);
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101097"),e.getMessage());
                    }

                    map1.put(ISettlementConstant.MESSAGELOCALE, ite.getMessageLocale());
                    map1.put(ISettlementConstant.CHECKRULE, ite.getCheckRule());
                    map1.put("detail", detailList);
                    map1.put(ISettlementConstant.MESSAGE, ite.getMessage());
                    map1.put(ISettlementConstant.CHECKRESULT, ite.getCheckResultDetail());
                    checkdetail.add(map1);
                });
                item.put("checkdetail", checkdetail);
                item.put(ISettlementConstant.CHECKNAME, value.get(0).getCheckName());
                item.put(ISettlementConstant.CHECKRESULT, value.get(0).getCheckResult());
                item.put(ISettlementConstant.MESSAGEADJUSTMENT, value.get(0).getMessageAdjustment());
                data.add(item);
                jsonObject.put("data", data);
                jsonObject.put(ISettlementConstant.CHECKRRESULT, value.get(0).getCheckrResult());
            }
            return jsonObject;
        } else {
            return null;
        }

    }

    /**
     * 日结
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    @Override
    public CtmJSONObject settle(String accentity, String period, boolean isAuto)throws Exception{
    	  //检查组织锁是否存在
        YmsLock ymsLock = JedisLockUtils.lockRjWithOutTrace(accentity);
        if (ymsLock == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101098"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CF","该组织下正在日结，请稍后再试！") /* "该组织下正在日结，请稍后再试！" */);
        }
        try {
            CtmJSONObject  json = new CtmJSONObject();
            json.put("settleResult","error");
            json.put("date","");
            if(!"".equals(accentity)){
                if(!"".equals(period)){
                    QuerySchema querySchema;
                    QueryConditionGroup group;
                    Map<String,Object> map;
                    //查询当前传入的period （2019-01-06格式） 是否符合结账条件
                    map =querySettlement(accentity,DateUtils.dateParse(period,"yyyy-MM-dd"));
                    if (MapUtils.isEmpty(map)) {
                        throw new BizException("00P_YS_FI_CM_0000153360&&&" + period, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180410","当前会计主体无该日期对应的结账日--") /* "当前会计主体无该日期对应的结账日--" */ + period);
                    }
                    conformSettle(map);
                    //先查询出来最小未结账日   period   2019-01-06
                    querySchema = QuerySchema.create().addSelect("min(settlementdate)");
                    group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                            QueryCondition.name("settleflag").eq(0));
                    querySchema.addCondition(group);
                    map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);
                    Map<String, Object> checkDailySettlementMap = queryAutoConfigByAccentity(accentity);
                    boolean checkDailySettlement = false;
                    if(null != checkDailySettlementMap && null != checkDailySettlementMap.get("checkDailySettlement")){
                        checkDailySettlement = (boolean) checkDailySettlementMap.get("checkDailySettlement");
                    }
                    if(null!= map){
                        Date minDateUnSettlement = (Date) map.get("min");
                        Date endDate = null;
                        //先进行日结检查操作
                        CtmJSONObject result = settleCheck(accentity,period,isAuto,"N");
                        String flag = (String)result.get("checkrResult");
                        if(!"error".equals(flag)){
                            endDate = DateUtils.dateParse(period,"yyyy-MM-dd");
                        }else{
                            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180411","当前日期有异常单据未处理，不允许日结") /* "当前日期有异常单据未处理，不允许日结" */);
                        }
                        //获取最小结账日到period之间的天数  获取两个日期（不含时分秒）相差的天数，不包含今天
                        int dayNum = DateUtils.dateBetweenIncludeToday(minDateUnSettlement,endDate);
                        for(int i =0;i<dayNum;i++){
                            Date  currDate = DateUtils.dateAdd(minDateUnSettlement,i,false);
                           /* if(DateUtils.dateCompare(currDate,new Date())==1){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101099"),"日结日期不能超过当前系统日期！");
                            }*/
                            String currDateString = DateUtils.dateToStr(currDate);
                            //判断当天的日结状态  检查结果，是否小于现在的时间
                            map =querySettlement(accentity,currDate);
                            if((boolean)map.get("settleflag")){
                                throw new BizException("01" + currDateString + "&&&P_YS_FI_CM_0000026266", currDateString + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418040D","已经日结,不能再次日结！") /* "已经日结,不能再次日结！" */);
                            }
                            Settlement settlement = new Settlement();
                            settlement.setId(map.get("id"));
                            settlement.setAccentity(accentity);
                            //进行日结操作
                            settleDetail(settlement,currDate);
                        }
                        json.put("settleResult","success");
                    }else {
                        //没查到没有数据，说明库里的数据 当月全部已日结
                        throw new BizException("02P_YS_FI_CM_0000026310", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418041A","没有可日结的日期！") /* "没有可日结的日期！" */);
                    }
                } else {
                    throw new BizException("02P_YS_FI_CM_0000026154", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180409","请选择日结日期！") /* "请选择日结日期！" */);
                }
            } else {
                throw new BizException("02P_YS_FI_CM_0000026184", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418040B","请先选择会计主体！") /* "请先选择会计主体！" */);
            }
            return json;
        } catch (Exception e){
            log.error(e.getMessage(), e);
             throw e;
        } finally {
            //释放组织锁
            JedisLockUtils.unlockRjWithOutTrace(ymsLock);
        }

    }


    /**
     * 取消日结
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    @Override
    public CtmJSONObject unsettle(String accentity, String period) throws Exception{
        CtmJSONObject  json = new CtmJSONObject();
        json.put("settleResult","error");
        if("".equals(accentity)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101090"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418040B","请先选择会计主体！") /* "请先选择会计主体！" */);
        }
        if("".equals(period)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101100"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180409","请选择日结日期！") /* "请选择日结日期！" */);
        }
        String token = InvocationInfoProxy.getYhtAccessToken();
        log.info("token:======================"+token);
//        Period periodvo = FINBDApiUtil.getFI4BDAccPeriodService().getPeriodVOByDate(DateUtils.strToDate(period),String.valueOf(accBookTypeId));//会计期间
        Map<String,Object> periodvo = QueryBaseDocUtils.queryPeriodByAccbodyAndDate(accentity,DateUtils.strToDate(period));
        // 查询总账的启用期间，未启用则不用判断当期总账是否未结账
//        IFI4BDService fi4bdSvr = FINBDApiUtil.getFI4BDService();
//        String glStartPeriod = fi4bdSvr.getPeriodByModuleWithOutException(accBookTypeId, FIModelUtil.GL);
        List<Map<String, Object>> gl = QueryBaseDocUtils.queryOrgBpOrgConfVO(accentity, ISystemCodeConstant.ORG_MODULE_GL);
        String glStartPeriod = null;
        if(!CollectionUtils.isEmpty(gl) && gl.get(0)!=null){
            glStartPeriod = gl.get(0).get("begindate").toString();
        }
        if(!StringUtils.isBlank(glStartPeriod)){
            Map<String, String> perioddata =getMinPeriod(accentity);
            String minPeriod = perioddata.get("accperiod");// 最小已结账期间
            String startPeriod = perioddata.get("startperiod");// 总账启用的开始时间，如果为空则未启用
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            if (!StringUtils.isBlank(startPeriod)) {
                Date glPeriods = sdf.parse(startPeriod);
                Date periodAcc = sdf.parse(periodvo.get("code").toString());
                if (!periodAcc.before(glPeriods)) {
                    // 取消时判断总账期间是否结账，如已结账则不能取消结账，否则可以取消结账。取消结账后结账状态置为未结账
                    if (StringUtils.isNotBlank(minPeriod)) {
                        Date minPeriodAcc = sdf.parse(minPeriod);
                        if (periodAcc.before(minPeriodAcc)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101101"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001F0", "总账已经在该会计期间结账，不能进行反结账！") /* "总账已经在该会计期间结账，不能进行反结账！" */);
                        }
                    }
                }
            }
        }
        //查询最大已结账日
        QuerySchema querySchema;
        QueryConditionGroup group;
        Map<String,Object> map;
        querySchema = QuerySchema.create().addSelect("max(settlementdate)");
        group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                QueryCondition.name("settleflag").eq(1));
        querySchema.addCondition(group);
        map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);
        if(null!= map){
            Date startDate = DateUtils.dateParse(period,"yyyy-MM-dd");
            Date endDate = (Date) map.get("max");
            Map<String,Object> param = new HashMap<String,Object>();
            param.put(IBussinessConstant.ACCENTITY,accentity);
            param.put("tenant_id",AppContext.getTenantId());
            param.put("unsettleman",AppContext.getCurrentUser().getId());
            param.put("unsettledate",new Date());
            param.put("startDate",startDate);
            param.put("endDate",endDate);
            int num = SqlHelper.update(SETTLEMENT+".batchUpdateSettle",param);
            log.error("num1:",num);
            num = SqlHelper.delete(SETTLEMENT+".batchUpdateSettleDetail",param);
            log.error("num2:",num);
            //获取startDate到endDate之间的天数
           /* int dayNum = DateUtils.dateBetweenIncludeToday(startDate,endDate);
            for(int i =0;i<dayNum;i++){
                Date  currDate = DateUtils.dateAdd(startDate,i,false);
                if(DateUtils.dateCompare(currDate,new Date())==1){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101102"),"取消日结日期不能超过当前系统日期！");
                }
                map =querySettlement(accentity,currDate);
                Settlement settlement = new Settlement();
                settlement.setAccentity(accentity);
                settlement.setId(map.get("id"));
                //取消日结的具体操作
                unSettleDetail(settlement,currDate);
            }*/
            json.put("settleResult","success");
        }else{
            //说明全部未日结
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101103"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180412","全部未日结，无需取消日结！") /* "全部未日结，无需取消日结！" */);
        }
        return json;

    }

    /**
     * 根据会计主体和时间查询 日结对象
     * @param accentity
     * @param currDate
     * @return
     * @throws Exception
     */
    public Map<String ,Object>   querySettlement(String accentity,Date currDate) throws  Exception{
        QuerySchema querySchema = QuerySchema.create().addSelect("id,settlementdate,settleflag");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                QueryCondition.name("settlementdate").eq(currDate));
        querySchema.addCondition(group);
        return MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);
    }


    /**
     * 日结条件判断
     * @param map
     * @throws Exception
     */
    public  void  conformSettle(Map<String ,Object> map ) throws Exception {
        String currDateString = new SimpleDateFormat("yyyy-MM-dd").format(map.get("settlementdate"));
        if((boolean)map.get("settleflag")){
            throw new BizException("01"+currDateString+"&&&P_YS_FI_CM_0000026266",currDateString+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001F1", "已经日结,不能再次日结！") /* "已经日结,不能再次日结！" */);
        }
        if(DateUtils.dateCompare((Date)map.get("settlementdate"),new Date())>1){
            throw new BizException("02P_YS_FI_CM_0000026182", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418040F","日结日期不能超过当前系统日期！") /* "日结日期不能超过当前系统日期！" */);
        }
    }


    /**
     * 日结具体操作
     * @param settlement
     * @param currDate
     * @throws Exception
     */
    public   void settleDetail(Settlement settlement,Date currDate) throws  Exception{
        settlement.setSettleflag(true);
        settlement.setSettleman(AppContext.getCurrentUser().getId());
        settlement.setSettledate(new Date());
        List<SettlementDetail> settlementDetailList = new ArrayList<SettlementDetail>();
        String accentity = settlement.getAccentity();
        Map<String, SettlementDetail> settlementDetailMap = DailySettleCompute.imitateDailyComputeNew(accentity,null,null,null,
                "2","2",currDate,false);
        for(SettlementDetail settlementDetail:settlementDetailMap.values()){
            settlementDetail.setId(ymsOidGenerator.nextId());
            settlementDetail.setAccountdate(currDate);
            settlementDetail.setEntityStatus(EntityStatus.Insert);
            settlementDetailList.add(settlementDetail);
        }
        CmpMetaDaoHelper.insert(SettlementDetail.ENTITY_NAME,settlementDetailList);
        EntityTool.setUpdateStatus(settlement);
        MetaDaoHelper.update(Settlement.ENTITY_NAME,settlement);

    }


    public  void unSettleDetail (Settlement settlement,Date currDate) throws  Exception{
        settlement.setSettleflag(false);
        settlement.setUnsettleman(AppContext.getCurrentUser().getId());
        settlement.setUnsettledate(new Date());
        List<SimpleCondition> scList = new ArrayList<SimpleCondition>();
        scList.add(new SimpleCondition(IBussinessConstant.ACCENTITY, ConditionOperator.eq,settlement.getAccentity()));
        scList.add(new SimpleCondition("accountdate", ConditionOperator.eq,currDate));
        MetaDaoHelper.batchDelete(SettlementDetail.ENTITY_NAME,scList);
        EntityTool.setUpdateStatus(settlement);
        MetaDaoHelper.update(Settlement.ENTITY_NAME,settlement);
    }


    /**
     * 总账是否日结检查
     */
    @Override
    public CtmJSONObject getCheckResult(String accentity, String period){
        CtmJSONObject json = new CtmJSONObject();
        if("".equals(accentity)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101104"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180415","会计主体不能为空！") /* "会计主体不能为空！" */);
        }
        if("".equals(period)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101105"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180417","会计期间不能为空！") /* "会计期间不能为空！" */);
        }
        try{
//            Long accBookTypeId =  FINBDApiUtil.getFI4BDService().getAccBookTypeByAccBody(accentity);//根据会计主体获取业务账簿id
//            Map<String, Object> map = FINBDApiUtil.getFI4BDAccPeriodService().getPeriodByParam(null,period,null,
//                    String.valueOf(accBookTypeId),new String[]{"begindate","enddate"});
            //判断日期格式格式
            String dateParse = period.split("-").length>2?DateUtils.pattern:DateUtils.MONTH_PATTERN;
            Map<String, Object> map = QueryBaseDocUtils.queryPeriodByAccbodyAndDate(accentity,DateUtils.dateParse(period, dateParse));
            Date enddate = (Date )map.get("enddate");
            QuerySchema settleSchema = QuerySchema.create().addSelect("settleflag");
            QueryConditionGroup qcg = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity),
                    QueryCondition.name("settlementdate").eq(enddate));
            settleSchema.addCondition(qcg);
            map = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,settleSchema);
            boolean  flag = false;
            if(map != null && map.get("settleflag") != null){
                flag = (boolean) map.get("settleflag");
            }
            json.put("code",200);
            json.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418040C","查询结账成功") /* "查询结账成功" */);
            json.put("data",flag);
            json.put("success",true);
        }catch(Exception e){
            json.put("code",999);
            json.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418040E","查询结账失败") /* "查询结账失败" */);
            json.put("success",false);
        }
        return json;
    }



    @Override
    public List<String> hasRefAccbook(List<String> ids) throws Exception{
        List<String> resultList = new ArrayList<>();
        if (ids == null || ids.size() < 1) {
            return resultList;
        }
        for (String id : ids) {
            boolean flag = buildQuery(id, Journal.ENTITY_NAME);
            if(flag){
                resultList.add(id);
                continue;
            }
            flag = buildQuery(id, Settlement.ENTITY_NAME);
            if(flag){
                resultList.add(id);
                continue;
            }
            flag = buildQuery(id, BankReconciliationSetting.ENTITY_NAME);
            if(flag){
                resultList.add(id);
            }
        }
        return resultList;
    }

    @Override
    public Date getMaxSettleDate(String accEntity) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("max(settlementdate)");
        //查询最大结账日
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accEntity),
                QueryCondition.name("settleflag").eq(1));
        querySchema.addCondition(group);
        Map<String, Object> maxSettleDate = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);
        if (maxSettleDate != null && maxSettleDate.size() > 0) {
            return (Date) maxSettleDate.get("max");
        }
        return null;
    }

    @Override
    public Date getMinUnSettleDate(String accEntity) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("min(settlementdate)");
        //查询最小未结账日
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accEntity),
                QueryCondition.name("settleflag").eq(0));
        querySchema.addCondition(group);
        Map<String, Object> maxSettleDate = MetaDaoHelper.queryOne(Settlement.ENTITY_NAME,querySchema);
        if (maxSettleDate != null && maxSettleDate.size() > 0) {
            return (Date) maxSettleDate.get("min");
        }
        return null;
    }


    @Override
    public Boolean checkDailySettlement(String accEntity, Date vouchDate) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("1");
        querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("settleflag").eq(1),
                QueryCondition.name("settlementdate").eq(vouchDate),
                QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accEntity)));
        List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME, querySchema);
        if (settlementList.size() > 0) {
            return true;
        }
        return false;
    }

    public boolean buildQuery(String id, String fullname) throws Exception {
        QuerySchema query = QuerySchema.create().addSelect("id");
        QueryConditionGroup accentity = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(id));
        // 业务期初期间引用校验调整优化
        if(Settlement.ENTITY_NAME.equals(fullname)){
            accentity.addCondition(QueryCondition.name("settleflag").eq(1));
        } else if (BankReconciliationSetting.ENTITY_NAME.equals(fullname)) {
            // 凭证对账，启用日期为总账模块启用日期；日记账对账，启用日期为业务单元“现金管理”启用日期
            accentity.addCondition(QueryCondition.name("reconciliationdatasource").eq(ReconciliationDataSource.BankJournal.getValue()));
        }
        List<Map> resList = MetaDaoHelper.query(fullname, query.addCondition(accentity));
        if (resList != null && resList.size() > 0) {
            return true;
        }
        return false;
    }


    /**
     * 获取总账的最小未结账期间
     * @param accentity 会计主体
     * @return accperiod 最小未结账会计期间
     * @throws Exception
     */
    public Map<String, String> getMinPeriod(String accentity)throws Exception {
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/account/minperiod";

        CtmJSONObject jsonobject = new CtmJSONObject();
        jsonobject.put("pk_org", accentity);
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();

        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        String str = HttpTookit.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(jsonobject), header,"UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
        Boolean successFlag = (Boolean)result.get("success");
        String code = String.valueOf(result.get("code"));
        Map<String, String> data = Maps.newHashMap();
        if(!successFlag){
            if ("996".equals(code)) {
                data.put("startperiod", null);
                return data;
            }
            if("997".equals(code)) {
                data.put("startperiod", null);
                return data;
            }
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101106"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418040A","查询总账是否结账失败：") /* "查询总账是否结账失败：" */ + result.get("message"));
        }

        if("996".equals(code)||"997".equals(code)){
            data.put("startperiod", null);
        }else{
            data = (Map)result.get("data");
        }
        return data;
    }

    @Override
    public String autoDailySettle(CtmJSONObject params) throws JsonProcessingException {
        CtmJSONObject paramObj = params.getJSONObject("params");
        String accentity = paramObj.getString("virtualaccbody");
        /*for (Map.Entry entry : paramObj.entrySet()) {
            accentity = String.valueOf(entry.getValue());
        }*/
        if (StringUtils.isEmpty(accentity)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101107"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180414","参数中无会计主体值") /* "参数中无会计主体值" */);
        }
        String subType = params.getString("subtype");
        CtmJSONObject jsonResult = new CtmJSONObject();
        long starttime = System.currentTimeMillis();
        try {
            if (Objects.equals(DAILYSETTLECHECKOUT, subType)) {
                log.info("结账开始========================================================");
                jsonResult = this.settle(accentity,  DateUtils.dateToStr(new Date()),true);
                log.info("结账结束========================================================");
            } else {
                log.info("结账检查开始========================================================");
                CtmJSONObject settleCheckRet = this.settleCheck(accentity, DateUtils.dateToStr(new Date()),true,"N");
                jsonResult.put("settleCheckRet", settleCheckRet);
                log.info("结账检查结束========================================================");
            }
            jsonResult.put("settleResult", true);
            jsonResult.put("errorInfo", "success");
            jsonResult.put("errorInfo2", "success");
            jsonResult.put("errorInfo3", "success");

        } catch (Exception e) {
            log.error(e.getMessage(),e);
            jsonResult.put("settleResult",false);
            if (e instanceof CtmException){
                CtmException err = (CtmException)e;
                String message = "";
                String message1 = "";
                String message2 = "";
                if (err.getErrorCode().startsWith("02")){
                    message = err.getErrorCode().substring(2);
                    message2 = err.getErrorCode().substring(2);
                }else if (err.getErrorCode().startsWith("00")){
                    message = err.getErrorCode().substring(2).split("&&&")[0];
                    message1 = err.getErrorCode().split("&&&")[1];
                }else if (err.getErrorCode().startsWith("01")){
                    message1 = err.getErrorCode().substring(2).split("&&&")[0];
                    message = err.getErrorCode().split("&&&")[1];
                }

                if (err.getErrorCode().startsWith("02")){
                    jsonResult.put("errorInfo",  com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message,null,message,Locale.SIMPLIFIED_CHINESE) /* "参数中无会计主体值" */+message1);
                    jsonResult.put("errorInfo2", com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage(message,null,message,Locale.US) /* "参数中无会计主体值" */+message1);
                    jsonResult.put("errorInfo3", com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage(message,null,message,Locale.TRADITIONAL_CHINESE) /* "参数中无会计主体值" */+message1);
                }else if (err.getErrorCode().startsWith("00")){
                    jsonResult.put("errorInfo", com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message,null,message,Locale.SIMPLIFIED_CHINESE) /* "参数中无会计主体值" */+message1);
                    jsonResult.put("errorInfo2", com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage(message,null,message,Locale.US) /* "参数中无会计主体值" */+message1);
                    jsonResult.put("errorInfo3", com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage(message,null,message,Locale.TRADITIONAL_CHINESE) /* "参数中无会计主体值" */+message1);
                }else {
                    jsonResult.put("errorInfo",  message1+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message,null,message,Locale.SIMPLIFIED_CHINESE) /* "参数中无会计主体值" */);
                    jsonResult.put("errorInfo2", message1+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage(message,null,message,Locale.US) /* "参数中无会计主体值" */);
                    jsonResult.put("errorInfo3", message1+com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage(message,null,message,Locale.TRADITIONAL_CHINESE) /* "参数中无会计主体值" */);
                }
            }else {
                jsonResult.put("errorInfo", e.getMessage());
                jsonResult.put("errorInfo2", e.getMessage());
                jsonResult.put("errorInfo3", e.getMessage());
            }
        }
        long endtime = System.currentTimeMillis();
        params.put("status", jsonResult.getBoolean("settleResult"));
        params.put("starttime", starttime);
        params.put("endtime", endtime);
        params.put("execute", true);
        params.put("msg", jsonResult.getString("errorInfo"));
        params.put("msg2", jsonResult.getString("errorInfo2"));
        params.put("msg3", jsonResult.getString("errorInfo3"));


        String ruleId = params.getJSONArray("rules").getString(0);
        CtmJSONArray logArray = new CtmJSONArray();
        CtmJSONObject logObj = new CtmJSONObject();
        logObj.put("rule", ruleId);
        logObj.put("starttime", starttime);
        logObj.put("endtime", endtime);
        logObj.put("period", "");
        logObj.put("status", jsonResult.getBoolean("settleResult"));
        if (Objects.equals(DAILYSETTLECHECKOUT, subType)) {
            logObj.put("msg", jsonResult.getString("errorInfo"));
            logObj.put("msg2", jsonResult.getString("errorInfo2"));
            logObj.put("msg3", jsonResult.getString("errorInfo3"));
            logArray.add(logObj);
        } else {
            // 日结检查log
            dailyCheckLog(jsonResult, ruleId, logArray, logObj);

        }
        params.put("logs", logArray);
        String callBackUrl = params.getString("url");
        // 执行post请求的方法
        //todo 315不合release
        if(log.isInfoEnabled()){
            log.info("autoDailySettle-callBackUrl-params:" + CtmJSONObject.toJSONString(params));
        }
        return HttpTookit.doPostWithJson(callBackUrl, CtmJSONObject.toJSONString(params), new HashMap<>());
    }

    /**
     * 封装日结检查log
     * @param jsonResult
     * @param logArray
     * @param logObj
     */
    private void dailyCheckLog(CtmJSONObject jsonResult, String ruleId, CtmJSONArray logArray, CtmJSONObject logObj) throws JsonProcessingException {
        // 日结检查
        CtmJSONObject settleCheckRet = jsonResult.getJSONObject("settleCheckRet");
        CtmJSONArray dataArr = settleCheckRet.getJSONArray("data");
        for (int i = 0; i < dataArr.size(); i++) {
            CtmJSONObject checkDetailObj = dataArr.getJSONObject(i);
            if ("pass".equals(checkDetailObj.getString("checkResult"))) {
                logObj.put("status", true);
            } else {
                logObj.put("status", false);
            }
            StringBuilder allmsg = new StringBuilder();
            StringBuilder allmsg2 = new StringBuilder();
            StringBuilder allmsg3 = new StringBuilder();
            String message = "";
            String message1 = "";
            String message2 = "";
            String message3 = "";
            String message4 = "";
            String allmessage = "";
            String msg = checkDetailObj.getJSONArray("checkdetail").getJSONObject(0).getString("messageLocale");
            if (msg.startsWith("02")){
                message = msg.substring(2);
            }else if (msg.startsWith("01")){
                message = msg.split("&&&")[1];
                message1 = msg.substring(2).split("&&&")[0];
            }else if (msg.startsWith("04")){
                String[] sarr = msg.substring(2).split("&&&");
                if (sarr != null) {
                    message = sarr[0];
                    message1 = sarr[1];
                }
            }else if (msg.startsWith("03")||msg.startsWith("05")) {
                String[] sarr = msg.substring(2).split("&&&");
                if (sarr != null) {
                    if (sarr.length == 3) {
                        message = sarr[0];
                        message1 = sarr[1];
                        message2 = sarr[2];
                    } else {
                        message = sarr[0];
                        message1 = sarr[1];
                        message2 = sarr[2];
                        message3 = sarr[3];
                        message4 = sarr[4];
                    }

                }
            }else{
                allmsg.append(msg);
            }
            if (!StringUtils.isEmpty(message)){
                allmsg.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message,null,message,Locale.SIMPLIFIED_CHINESE));
                allmsg2.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message,null,message,Locale.US));
                allmsg3.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message,null,message,Locale.TRADITIONAL_CHINESE));
            }
            if (!StringUtils.isEmpty(message1)){
                if (msg.startsWith("04")){
                    allmsg = new StringBuilder();
                    allmsg2 = new StringBuilder();
                    allmsg3 = new StringBuilder();
                    allmsg.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message1,null,message1,Locale.SIMPLIFIED_CHINESE));
                    allmsg2.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message1,null,message1,Locale.US));
                    allmsg3.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message1,null,message1,Locale.TRADITIONAL_CHINESE));
                    allmsg.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message,null,message,Locale.SIMPLIFIED_CHINESE));
                    allmsg2.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message,null,message,Locale.US));
                    allmsg3.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message,null,message,Locale.TRADITIONAL_CHINESE));
                }else {
                    allmsg.append(message1);
                    allmsg2.append(message1);
                    allmsg3.append(message1);
                }
            }
            if (!StringUtils.isEmpty(message2)){
                allmsg.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message2,null,message2,Locale.SIMPLIFIED_CHINESE));
                allmsg2.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message2,null,message2,Locale.US));
                allmsg3.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message2,null,message2,Locale.TRADITIONAL_CHINESE));
            }
            if (!StringUtils.isEmpty(message3)){
                if (msg.startsWith("05")){
                    allmsg.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message3,null,message3,Locale.SIMPLIFIED_CHINESE));
                    allmsg2.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message3,null,message3,Locale.US));
                    allmsg3.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message3,null,message3,Locale.TRADITIONAL_CHINESE));
                }else {
                    allmsg.append(message3);
                    allmsg2.append(message3);
                    allmsg3.append(message3);
                }
            }
            if (!StringUtils.isEmpty(message4)){
                allmsg.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message4,null,message4,Locale.SIMPLIFIED_CHINESE));
                allmsg2.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message4,null,message4,Locale.US));
                allmsg3.append(com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage( message4,null,message4,Locale.TRADITIONAL_CHINESE));
            }
            logObj.put("msg", allmsg.toString());
            logObj.put("msg2", allmsg2.toString());
            logObj.put("msg3", allmsg3.toString());
            String checkName = checkDetailObj.getString("checkName");
            logObj.put("rule", ruleId);
            logArray.add(CtmJSONObject.parseObject(CtmJSONObject.toJSONString(logObj)));
        }
    }

    /**
     * 根据会计主体查询现金参数
     * @param accentity
     * @return
     * @throws Exception
     */
    private Map<String, Object> queryAutoConfigByAccentity(String accentity)throws Exception{
        //查询cmp_autoconfig
        QuerySchema querySchema = QuerySchema.create().addSelect("checkDailySettlement");
        QueryConditionGroup newGroup = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity));
        querySchema.addCondition(newGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, querySchema);
        if(query.size()>0) {
            return query.get(0);
        }
        return null;
    }

    @Override
    public CtmJSONObject upgradeDailySettleCurrent() throws Exception {
        String yhtTenantId = AppContext.getYTenantId();
        upgradeDailySettleDetail(yhtTenantId);
        return null;
    }

    @Override
    public void upgradeDailySettle(CtmJSONObject param) throws Exception {
        log.error("开始执行账户期初改造日结数据升级任务2024-01-01日之后的日结数据");
        String uid = param.getString("uid");
        // 查询所有的租户信息
        List<String> ytenantIdList = QueryBaseDocUtils.getTenantList();
        //构建进度条信息
        ProcessUtil.initProcess(uid,ytenantIdList.size());
        // 通过机器人异步执行
        CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
        ExecutorService taskExecutor = null;
        taskExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"upgradeDailySettle-threadpool");
        try{
            taskExecutor.submit(() -> {
                try{
                    for (String item : ytenantIdList) {
                        try {
                            RobotExecutors.runAs(item, new Callable() {
                                @Override
                                public Object call() throws Exception {
                                    upgradeDailySettleDetail(item);
                                    return null;
                                }
                            }, ctmThreadPoolExecutor.getThreadPoolExecutor());
                        } catch (Exception e) {
                            log.error("upgradeDailySettle期初改造日结数据升级任务报错：{}", e.getMessage(), e);
                        }
                    }
                }catch (Exception e) {
                    log.error("upgradeDailySettle-error：", e);
                }finally{
                    ProcessUtil.completed(uid);
                }
            });
        }catch(Exception e){
            log.error(e.getMessage(), e);
        }finally{
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }
    }

    @Override
    public CtmJSONObject getSettlementlistRefData() throws Exception {
        List<String> orgPermissionsList = new ArrayList<>();
        orgPermissionsList.addAll(BillInfoUtils.getOrgPermissionsByAuth(IServicecodeConstant.BANKINITDATA));
        CtmJSONObject result = new CtmJSONObject();
        result.put("orgPermissions",orgPermissionsList);
        return result;
    }

    private void upgradeDailySettleDetail(String ytenantId) throws Exception {
        //1.查询需要升级的当前2024-01-01至今已经日结的账户和会计主体
        Map param = new HashMap();
        param.put("ytenantId",ytenantId);
        param.put("accountdate", "2024-01-01");
        List<Map<String, Object>> listBankAccount = SqlHelper.selectList(SETTLEMENT+".getSettleBankAccount", param);
        if (listBankAccount.isEmpty()) {
            return;
        }
        //需要新增的
        List<SettlementDetail> insertSettlementDetailList = new ArrayList<>();
        //需要更新的
        List<SettlementDetail> updateSettlementDetailList = new ArrayList<>();
        //2.找到这些账户对应的适用范围的会计主体
        EnterpriseParams params = new EnterpriseParams();
        Map<String, Set<String>> accIdDatesMap = new HashMap<>();
        Set<String> accentityIds = new HashSet<>();
        Map<String, SettlementDetail> existMap = new HashMap<>();
        for (Map<String, Object> item : listBankAccount) {
            SettlementDetail settlementDetail = new SettlementDetail();
            settlementDetail.init(item);
            String bankaccount = settlementDetail.getBankaccount();
            if (!StringUtils.isEmpty(bankaccount)) {
                accentityIds.add(settlementDetail.getAccentity());
                existMap.put(settlementDetail.getAccentity() + "#" + bankaccount + "#" + settlementDetail.getCurrency() + "#" + settlementDetail.getAccountdate(), settlementDetail);
                Set dates = accIdDatesMap.get(bankaccount) == null ? new HashSet<>() : accIdDatesMap.get(bankaccount);
                dates.add(item.get("accountdate").toString());
                accIdDatesMap.put(bankaccount, dates);
            }
        }
        //根据已经日结了的组织去找账户现在的适用组织（日结是按照组织日结的）
        params.setOrgidList(new ArrayList<>(accentityIds));
        params.setEnables(Arrays.asList(1, 2));
        List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVoWithRanges = enterpriseBankQueryService.queryAllWithRange(params);

        //组装一下唯一条件
        Set<String> allUniqueKeySet = new HashSet<>();
        // 查询企业银行账户信息的授权使用组织范围
        for (EnterpriseBankAcctVOWithRange item : enterpriseBankAcctVoWithRanges) {
            List<OrgRangeVO> orgRangeVOS = item != null ? item.getAccountApplyRange() : Collections.emptyList();
            // 获取范围内的组织ID列表
            List<String> rangeOrgIds = orgRangeVOS.stream().map(OrgRangeVO::getRangeOrgId).collect(Collectors.toList());
            List<BankAcctCurrencyVO> currencyList = item != null ? item.getCurrencyList() : Collections.emptyList();
            // 获取范围内的币种ID列表
            List<String> currenyIds = currencyList.stream().map(BankAcctCurrencyVO::getCurrency).collect(Collectors.toList());
            // 如果这个账户有适用范围的组织并且也有日结了的日期数据
            if (rangeOrgIds.size() > 0 && (accIdDatesMap.get(item.getId())!=null && accIdDatesMap.get(item.getId()).size() > 0)) {
                for (String rangeOrgId : rangeOrgIds) {
                    for (String currenyId : currenyIds) {
                        Set<String> dates = accIdDatesMap.get(item.getId());
                        for (String date : dates) {
                            allUniqueKeySet.add(rangeOrgId + "#" + item.getId() + "#" + currenyId + "#" + date);
                        }
                    }
                }
            }
        }
        //3.新增没的修改有的
        if (!allUniqueKeySet.isEmpty()) {
            for (String item : allUniqueKeySet) {
                String[] ids = item.split("#");
                Date currDate = DateUtils.parseDate(ids[3]);
                //构建日结数据
                Map<String, SettlementDetail> settlementDetailMap = DailySettleCompute.imitateDailyComputeNew(ids[0],
                        ids[2], null, ids[1], "2", "2", currDate,true);
                if (!existMap.keySet().contains(item)) {
                    for (SettlementDetail settlementDetail : settlementDetailMap.values()) {
                        settlementDetail.setId(ymsOidGenerator.nextId());
                        settlementDetail.setAccountdate(currDate);
                        settlementDetail.setEntityStatus(EntityStatus.Insert);
                        insertSettlementDetailList.add(settlementDetail);
                    }
                } else {
                    for (SettlementDetail settlementDetail : settlementDetailMap.values()) {
                        existMap.get(item).setTodayorimoney(settlementDetail.getTodayorimoney());
                        existMap.get(item).setTodaycreditorimoneysum(settlementDetail.getTodaycreditorimoneysum());
                        existMap.get(item).setTodaycreditlocalmoneysum(settlementDetail.getTodaycreditlocalmoneysum());
                        existMap.get(item).setTodaydebitorimoneysum(settlementDetail.getTodaydebitorimoneysum());
                        existMap.get(item).setTodaydebitlocalmoneysum(settlementDetail.getTodaydebitlocalmoneysum());
                        existMap.get(item).setTodaydebitnum(settlementDetail.getTodaydebitnum());
                        existMap.get(item).setTodaycreditnum(settlementDetail.getTodaycreditnum());
                        existMap.get(item).setYesterdayorimoney(settlementDetail.getYesterdayorimoney());
                        existMap.get(item).setYesterdaylocalmoney(settlementDetail.getYesterdaylocalmoney());
                        existMap.get(item).setTodayorimoney(settlementDetail.getTodayorimoney());
                        existMap.get(item).setTodaylocalmoney(settlementDetail.getTodaylocalmoney());
                        existMap.get(item).setEntityStatus(EntityStatus.Update);
                        updateSettlementDetailList.add(existMap.get(item));
                    }
                }
            }
        }
        if (!insertSettlementDetailList.isEmpty()) {
            CmpMetaDaoHelper.insert(SettlementDetail.ENTITY_NAME, insertSettlementDetailList);
        }
        if (!updateSettlementDetailList.isEmpty()) {
            CmpMetaDaoHelper.update(SettlementDetail.ENTITY_NAME, updateSettlementDetailList);
        }
    }

}
