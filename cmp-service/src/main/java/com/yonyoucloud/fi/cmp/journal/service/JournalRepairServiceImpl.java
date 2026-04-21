package com.yonyoucloud.fi.cmp.journal.service;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseCashVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.ctm.stwb.settlebench.ReceiptType;
import com.yonyoucloud.ctm.stwb.settlebench.SettleBench;
import com.yonyoucloud.ctm.stwb.settlebench.SettleBench_b;
import com.yonyoucloud.ctm.stwb.stwbentity.JournalType;
import com.yonyoucloud.ctm.stwb.stwbentity.SingleBatch;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.BizException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yonyou.iuap.yonscript.support.utils.cryptogram.Base64Utils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class JournalRepairServiceImpl implements JournalRepairService{

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    JournalService journalService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;//业务日志

    private static final String YHTTENANTID = "ytenant_id";
    private static final String ID = "id";
    private static final String STWBBILLNO = "stwbBillNo";
    private static final String SERVICE_CODE = "stwb_manualsettlementlist";
    private static final String BILL_NO = "stwb_manualsettlement";
    private static final String JOURNALTYPE = "JournalType";

    private static final String JOURNALREPAIRMAPPER = "com.yonyoucloud.fi.cmp.mapper.JournalRepairMapper.";

    @Override
    public void addJournalForStwbCode(CtmJSONObject params) throws Exception {
        checkJournalRepairParam(params);
        //校验来源单据号 这里指定结算单号
        String stwbBillNo = String.valueOf(params.get(STWBBILLNO));
        if (!ValueUtils.isNotEmptyObj(stwbBillNo)) {
            throw new Exception("not stwbBillNo!");
        }
        //结算单的登账状态
        short journalType = params.getShort(JOURNALTYPE);
        if(!ValueUtils.isNotEmptyObj(journalType)){
            throw new Exception("not JournalType!");
        }
        //根据传入的 结算单号查询结算单
        Map<String, Object> settleBenchMap = querySettleBenchByCode(stwbBillNo);
        if(settleBenchMap == null){
            throw new Exception("not settleBenchMap!");
        }
        SettleBench settleBench = new SettleBench();
        settleBench.init(settleBenchMap);
        List<SettleBench_b> settleBenchbList = querySettleBenchBodyById(settleBenchMap.get("id").toString());
        List<Long> settleBenchbIdIsRegister = new ArrayList<>();
        //通过srcbillno查询日记账 通过未登记的日记账 筛选出结算单子表信息
        List<Journal> journalList = queryJournalBySrcBillno(stwbBillNo);
        //如果有日记账说明当前结算单子表升级了一部分日记账 需要过滤已经生成日记账的 结算单子表；如果没有日记账说明当前结算单没有生成任何日记账 需要按子表全部生成
        List<SettleBench_b> settleBenchbListForJournal = new ArrayList<>();
        if(!CollectionUtils.isEmpty(journalList) && journalList.size()!=settleBenchbList.size()){
            for(Journal journal : journalList){
                for(SettleBench_b body:settleBenchbList){
                    if(journal.getSrcbillitemid().equals(body.getId())){
                        //获取已经登账的数据id
                        settleBenchbIdIsRegister.add(body.getId());
                    }
                }
            }
            for(SettleBench_b body:settleBenchbList){
                if(!settleBenchbIdIsRegister.contains(body.getId())){
                    settleBenchbListForJournal.add(body);
                }
            }
        }else{
            settleBenchbListForJournal.addAll(settleBenchbList);
        }
        //通过结算单子表 组装日记账
        if(CollectionUtils.isEmpty(settleBenchbListForJournal) ){
            throw new Exception("not settleBenchbListForJournal!");
        }
        List<Journal> journalInsertList = buildJournalInsertVo(settleBench,settleBenchbListForJournal,journalType);
        //登账处理
        CmpMetaDaoHelper.insert(Journal.ENTITY_NAME, journalInsertList);
        //修改余额
        for(Journal journal:journalInsertList){
            Map<String, Object> map = new HashMap<>();
            changeInitBalance(journal, params,map);
        }
//        completedThisCommit(params);
    }

    @Override
    public void deleteJournalByStwbCode(CtmJSONObject params) throws Exception {
        checkJournalRepairParam(params);
        String yTenantId = InvocationInfoProxy.getTenantid();
        params.put(YHTTENANTID,yTenantId);
        LinkedHashMap<String,String> data = (LinkedHashMap<String, String>) params.get("data");
        if (!ValueUtils.isNotEmptyObj(data.get(STWBBILLNO))) {
            throw new Exception("not stwbBillNos!");
        }
        //校验来源单据号 这里指定结算单号
        String[]  stwbBillNos = String.valueOf(data.get(STWBBILLNO)).split(",");

        List<String> stwbBillNoList = Arrays.asList(stwbBillNos);
        //查询结算单据
        List<String> settleBench_b_ids = new ArrayList<>();
        List<Map<String, Object>> settleBenchMapList = querySettleBenchByCodeList(stwbBillNoList);
        //查询日记账
        List<Journal> journalList = queryJournalBySrcBillnoList(stwbBillNoList);
        List<Journal> deletejournalList = new ArrayList<>();
        //对比结算已经删除 但是日记账还存在的数据
        if (CollectionUtils.isEmpty(settleBenchMapList)) {
            //结算为空 说明结算已经删除 可以删除和此结算单相关的全部日记账
            deletejournalList.addAll(journalList);
        }else{
            for (Map<String, Object> settleBenchMap:settleBenchMapList) {
                settleBench_b_ids.add(settleBenchMap.get("id").toString());
            }
            List<SettleBench_b> settleBenchbList = querySettleBenchBodyByIdList(settleBench_b_ids);
            List<String> settleBenchbIdIsRegister = new ArrayList<>();//存在的结算单子表id 这部分账不删
            for(Journal journal : journalList){
                for(SettleBench_b body:settleBenchbList){
                    if(journal.getSrcbillitemid().equals(body.getId().toString())){
                        //获取已经登账的数据id
                        settleBenchbIdIsRegister.add(body.getId().toString());
                    }
                }
            }
            for(Journal journal : journalList){//如果日记账的来源单据行id不存在当前集合中 则删除此日记账
                if(!settleBenchbIdIsRegister.contains(journal.getSrcbillitemid())){
                    deletejournalList.add(journal);
                }
            }
        }
        //修改余额 、删除日记账
        for(Journal journal : deletejournalList){
            changeInitAndDeleteJournal(journal, params);
        }
        //记录业务日志
        saveBusinessLog(deletejournalList,stwbBillNoList.toString(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400556", "通过结算单号删除日记账") /* "通过结算单号删除日记账" */,IServicecodeConstant.BANKJOURNAL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400555", "银行日记账") /* "银行日记账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400557", "逻辑删除") /* "逻辑删除" */);
        completedThisCommit(params);
    }

    @Override
    public void deleteJournalById(CtmJSONObject params) throws Exception {
        LinkedHashMap<String,String> data = (LinkedHashMap<String, String>) params.get("data");
        checkJournalRepairParam(params);
        String yTenantId = InvocationInfoProxy.getTenantid();
        params.put(YHTTENANTID,yTenantId);

        if (!ValueUtils.isNotEmptyObj(data.get(ID))) {
            throw new Exception("not id!");
        }
        String[]  ids = String.valueOf(data.get(ID)).split(",");
        List<String> idList = Arrays.asList(ids);

        //查询日记账数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(idList));
        schema.addCondition(conditionGroup);
        List<Journal> journalVos = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, schema,null);
        if(journalVos.isEmpty()){
            throw new Exception("not journalVo!");
        }
        for(Journal journal:journalVos){
            changeInitAndDeleteJournal(journal, params);
        }
        //记录业务日志
        saveBusinessLog(journalVos,idList.toString(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540055A", "通过id删除日记账") /* "通过id删除日记账" */,IServicecodeConstant.BANKJOURNAL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400555", "银行日记账") /* "银行日记账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400557", "逻辑删除") /* "逻辑删除" */);
        completedThisCommit(params);
    }

    @Override
    public void modifyInitBalanceById(CtmJSONObject params) throws Exception {
        checkJournalRepairParam(params);
        //根据ids查询 initdata信息
        LinkedHashMap<String,String> data = (LinkedHashMap<String, String>) params.get("data");
        String id = String.valueOf(data.get(ID));
        if (!ValueUtils.isNotEmptyObj(id)) {
            throw new Exception("not id!");
        }
        BigDecimal cobookoribalance = new BigDecimal(data.get("cobookoribalance").toString());
//        BigDecimal cobooklocalbalance = new BigDecimal(params.get("cobooklocalbalance").toString());
        InitData initDataVo = MetaDaoHelper.findById(InitData.ENTITY_NAME,id);
        if(initDataVo != null){
            Map<String, Object> map = new HashMap<>();
            //设置余额信息
            map.put("initDataId", id);
            map.put("ytenant_id", InvocationInfoProxy.getTenantid());
            map.put("cobookoribalance", cobookoribalance);
            //进行更新
            SqlHelper.update(JOURNALREPAIRMAPPER + "modifyInitBalanceById", map);
        }else{
            throw new Exception("initDataVo is null!");
        }
        //记录业务日志
        saveBusinessLog(initDataVo,id,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400559", "通过账户期初id修改余额") /* "通过账户期初id修改余额" */,IServicecodeConstant.BANKINITDATA, IMsgConstant.BANK_INITDATA, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400558", "修改余额") /* "修改余额" */);
        completedThisCommit(params);
    }

    @Override
    //前端屏蔽 此方法不会调用
    public void updateCmpDataByQueryCondition(CtmJSONObject params) throws Exception {
        CommonSqlExecutor metaDaoSupport = new CommonSqlExecutor(AppContext.getSqlSession());

        if (!ValueUtils.isNotEmptyObj(params)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101155"),"not params!");
        }
        //解析前端加密的executeSql
        LinkedHashMap<String,String> data = (LinkedHashMap<String, String>) params.get("data");
//        String executeSql = String.valueOf(data.get("executeSql")).substring(1,data.get("executeSql").toString().length()-1);
        String executeSql = String.valueOf(data.get("executeSql"));
        if(executeSql == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101156"),"not executeSql!");
        }
//        String sql = executeSql;
        String sql = new String(Base64Utils.decode(executeSql)).toLowerCase(Locale.ROOT);
        //获取where条件 没有id，ytenantid的进行报错
        if(sql.indexOf("where") == -1){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101157"),"not where QueryCondition!");
        }
        String whereSql =  sql.substring(sql.indexOf("where")).trim();
        if(whereSql.indexOf("ytenant_id") == -1){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101158"),"not where ytenant_id!");
        }
        //截取sql进行查询 判断数据量 不能大于10
        String tableName = sql.substring(sql.indexOf("update") + "update".length(), sql.indexOf("set")).trim();
        String bulidQuerySql = "select count(id) as count from " + tableName + " " +whereSql;
        List<Map<String,Object>> queryResult = metaDaoSupport.executeSelectSql(bulidQuerySql);
        if(!queryResult.isEmpty() && Integer.valueOf(queryResult.get(0).get("count").toString())>10){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101159"),"update data count not More than 10 size!");
        }
        //执行sql
        metaDaoSupport.executeSql(sql);
        completedThisCommit(params);
    }

    private void changeInitAndDeleteJournal(Journal journal, CtmJSONObject params) throws Exception {
        Map<String, Object> map = new HashMap<>();
        //修改账户期初 余额
        changeInitBalance(journal, params,map);
        //删除日记账(逻辑删除)
        map.put("journalId",journal.getId());
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        String dateString = formatter.format(currentTime);
        map.put("newytenant_id", map.get("ytenant_id").toString()+dateString);
        map.put("dateString", Long.parseLong(dateString));
        SqlHelper.update(JOURNALREPAIRMAPPER + "deleteJournal", map);
    }

    private void changeInitBalance(Journal journal, CtmJSONObject params,Map<String, Object> map) throws Exception {
        //修改账户期初 余额
        //借方原币
        BigDecimal debitoriSum = journal.getDebitoriSum()!=null?journal.getDebitoriSum(): BigDecimal.ZERO;
        //借方本币
        BigDecimal debitnatSum = journal.getDebitnatSum()!=null?journal.getDebitnatSum(): BigDecimal.ZERO;
        //贷方原币
        BigDecimal creditoriSum = journal.getCreditoriSum()!=null?journal.getCreditoriSum(): BigDecimal.ZERO;
        //贷方本币
        BigDecimal creditnatSum = journal.getCreditnatSum()!=null?journal.getCreditnatSum(): BigDecimal.ZERO;

        //计算变化金额
        BigDecimal changeOriSum = BigDecimalUtils.safeSubtract(creditoriSum, debitoriSum);
        BigDecimal changeNatSum = BigDecimalUtils.safeSubtract(creditnatSum, debitnatSum);

        map.put("ytenant_id",params.getString(YHTTENANTID));
        map.put("changeNatSum",changeNatSum);
        map.put("changeOriSum",changeOriSum);
        InitData initData = CmpWriteBankaccUtils.queryInitDatabyAccid
                (journal.getAccentity(),journal.getBankaccount()!=null?journal.getBankaccount():journal.getCashaccount(),journal.getCurrency());
        map.put("initDataId",initData.getId());
        SqlHelper.update(JOURNALREPAIRMAPPER + "updateInitDataAccount", map);
    }

    public void checkJournalRepairParam(CtmJSONObject params) throws Exception {
        if (!ValueUtils.isNotEmptyObj(params)) {
            throw new Exception("not param!");
        }
        params.put(YHTTENANTID,InvocationInfoProxy.getTenantid());
//        String yhtTenantId = String.valueOf(params.get(YHTTENANTID));
//        if (!ValueUtils.isNotEmptyObj(yhtTenantId)) {
//            throw new Exception("not ytenant_id!");
//        }
//        String yTenantId = InvocationInfoProxy.getTenantid();
//        if (!yTenantId.equals(yhtTenantId)) {
//            throw new Exception("not authentication!");
//        }

    }

    public  List<Map<String, Object>>  querySettleBenchByCodeList(List<String> code) throws Exception {
        if (null == code) {
            return null;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname(ICmpConstant.STWB);
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STWB);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("code").in(code));
        schema.addCondition(conditionGroup);
//        schema.addCompositionSchema(QuerySchema.create().name("SettleBench_b").addSelect("*"));
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(query)){
            return null;
        }
        return query;
    }

    public Map<String, Object> querySettleBenchByCode(String code) throws Exception {
        if (null == code) {
            return null;
        }
        BillContext billContext = new BillContext();
        billContext.setFullname(ICmpConstant.STWB);
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STWB);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("code").eq(code));
        schema.addCondition(conditionGroup);
//        schema.addCompositionSchema(QuerySchema.create().name("SettleBench_b").addSelect("*"));
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(query)){
            return null;
        }
        return query.get(0);
    }

    public List<SettleBench_b> querySettleBenchBodyByIdList(List<String> id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("stwb.settlebench.SettleBench_b");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STWB);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("mainid").in(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(query)){
            return null;
        }else{
            List<SettleBench_b> settleBenchbList = new ArrayList<>();
            for(Map<String, Object> obj : query){
                SettleBench_b b = new SettleBench_b();
                b.init(obj);
                settleBenchbList.add(b);
            }
            return settleBenchbList;
        }
    }

    public List<SettleBench_b> querySettleBenchBodyById(String id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("stwb.settlebench.SettleBench_b");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_STWB);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("mainid").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (CollectionUtils.isEmpty(query)){
            return null;
        }else{
            List<SettleBench_b> settleBenchbList = new ArrayList<>();
            for(Map<String, Object> obj : query){
                SettleBench_b b = new SettleBench_b();
                b.init(obj);
                settleBenchbList.add(b);
            }
            return settleBenchbList;
        }
    }

    public List<Journal> queryJournalBySrcBillnoList(List<String> srcBillno) throws Exception {
        if (null == srcBillno) {
            return null;
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("srcbillno").in(srcBillno));
        schema.addCondition(conditionGroup);
        List<Journal> journalVos = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, schema,null);

        return journalVos;
    }

    public List<Journal> queryJournalBySrcBillno(String srcBillno) throws Exception {
        if (null == srcBillno) {
            return null;
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("srcbillno").eq(srcBillno));
        schema.addCondition(conditionGroup);
        List<Journal> journalVos = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, schema,null);
        return journalVos;
    }

    /**
     * 组装日记账信息
     * @param settleBenchObj
     * @param childList
     * @return
     */
    private List<Journal> buildJournalInsertVo(SettleBench settleBenchObj, List<SettleBench_b> childList,Short journalType) throws Exception {
        List<Journal> journalList = new ArrayList<>(childList.size());
        Journal journal = new Journal();
        for (SettleBench_b entity : childList) {
            journal = new Journal();
            //设置基本的日记账信息
            setBasicJournalInfo(settleBenchObj, journal, entity,journalType);
            //如果当前单据为银行账 则查询所属组织并赋值
            journalService.setParentAccentityForJournal(journal);
            //根据不同的对方类型给相关字段赋值登账 (对方类型、对方名称等)
            journalService.addOthertitle(journal);
            // 设置银行账号 现金账号
            journal.setBankaccount(entity.getOurbankaccount());
            journal.setCashaccount(entity.getOurcashaccount());
            if(journal.getBankaccount()!=null){
                EnterpriseParams params = new EnterpriseParams();
                params.setId(journal.getBankaccount());
                EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAcctByCondition(params).get(0);
                journal.setBankaccountno(enterpriseBankAcctVO.getAccount());
                journal.setBanktype(enterpriseBankAcctVO.getBank());
            }else if(journal.getCashaccount()!=null){
                EnterpriseCashVO enterpriseCashVO = baseRefRpcService.queryOneCashAcctByCondition(journal.getCashaccount());
                journal.setCashaccountno(enterpriseCashVO.getCode());
            }


            //根据收付类型设置借贷方金额
            setDebitOrCreditAmount(journal, entity, settleBenchObj.getReceipttype());
            // 根据对方类型，设置对方信息
            if(ValueUtils.isNotEmpty(entity. getCounterpartytype())){
                setCounterInfo(journal, entity, entity.getCounterpartytype());
            }
            //设置 各种状态值
            setJournalStatus(journal, journalType);
            journal.setEntityStatus(EntityStatus.Insert);
            journalList.add(journal);
        }
        return journalList;
    }

    private void setBasicJournalInfo(BizObject settleBenchObj, Journal journal, SettleBench_b entity,short journalType) {
        Boolean isExchangePayment=entity.getIsExchangePayment()==null?false:entity.getIsExchangePayment();
        journal.setAccentity(settleBenchObj.get("accentity"));
        journal.setId(ymsOidGenerator.nextId());
        //本币币种 原币币种 来源交易类型
        journal.setNatCurrency(entity.getNatCurrency());
        journal.setCurrency(isExchangePayment?entity.getExchangePaymentCurrency():entity.getOriginalcurrency());
        //交易类型赋值
        Object busType = settleBenchObj.get("bustype");
        journal.setTradetype(busType == null ? "" : busType.toString());
        journal.setSrcbillitemno(entity.getId().toString());
        journal.setSrcbillitemid(entity.getId().toString());
        journal.setTopsrcbillno(entity.getBizbillno());//来源单据号,对应上游业务单据编号

        journal.setBillno(BILL_NO);
        journal.setBillnum(settleBenchObj.get("code"));
        journal.setServicecode(SERVICE_CODE);

        journal.setExchangerate(entity.getExchangerate());
        //如果是线下结算，而且结算成功日期小于期望结算日期， 这种是重新登账的情况，单据日期需要取结算成功日期
        setVouchDateAndDzDate(journal, entity, journalType,settleBenchObj);

        journal.setSrcitem(EventSource.StwbSettlement);
        journal.setBilltype(EventType.StwbSettleMentDetails);
        journal.setTopsrcitem(StringUtils.isEmpty(entity.getBizsyssrc()) ? null : Short.parseShort(entity.getBizsyssrc()));
        journal.setTopbilltype(StringUtils.isEmpty(entity.getBizbilltype()) ? null : Short.parseShort(entity.getBizbilltype()));

        journal.setSettlemode(entity.getSettlemode());
        if (entity.get("transeqno") != null) {
            journal.set("transeqno", entity.get("transeqno"));
        }
        journal.setSrcbillno(settleBenchObj.get("code"));
        //项目， 部门
        journal.setProject(entity.getProject());
        journal.setDept(entity.getDept());
        //备注
        journal.setDescription(entity.getDescription());
        //勾兑号
        journal.setBankcheckno(entity.getCheckIdentificationCode());
        //费用项目
        journal.setCostproject(entity.getExpenseitem());

        journal.setCreator(AppContext.getCurrentUser().getId().toString());
        journal.setBookkeeper(AppContext.getCurrentUser().getId());
    }

    private void setVouchDateAndDzDate(Journal journal, SettleBench_b entity, short journalType, BizObject settleBenchObj) {
        if (Objects.nonNull(entity.getOffLineSettleFlag())) {
            Date settleSuccessDate = entity.getSettlesuccessdate();
            if (settleSuccessDate != null && settleSuccessDate.before(entity.getExpectdate())) {
                // 如果结算成功日期小于期望结算日期 结算重新登账 单据日期和登账日期  取结算成功日期
                journal.setVouchdate(settleSuccessDate);
            } else {
                journal.setVouchdate(entity.getExpectdate());
            }
            //直联取单据日期，非直连取期望结算日期
            if(Boolean.TRUE.equals(entity.getIsdirectacc())) {
                journal.setVouchdate(settleBenchObj.get("vouchdate"));
            }
        } else {
            //直联取单据日期，非直连取期望结算日期
            if(Boolean.TRUE.equals(entity.getIsdirectacc())){
                journal.setVouchdate(settleBenchObj.get("vouchdate"));
            }else{
                journal.setVouchdate(entity.getExpectdate());
            }
        }
        //如果是退票红冲，登账日期是系统日期
        if (journalType == JournalType.StrikeSubmit.getValue()) {
            journal.setDzdate(new Date());
        }
        //如果是已结算补单，登账日期是结算成功日期
        if (journalType == JournalType.SuccessSubmit.getValue()) {
            journal.setDzdate(entity.getSettlesuccessdate());
        }
    }
    private void setDebitOrCreditAmount(Journal journal, SettleBench_b entity, Integer receiptType) {
        Boolean isExchangePayment=entity.getIsExchangePayment()==null?false:entity.getIsExchangePayment();
        if (receiptType.shortValue() == ReceiptType.pay.getValue()) {
            //借贷方向  贷,借方原币金额,借方本币金额,贷方原币金额,贷方本币金额, 收付款类型 -- 付款
            journal.setDirection(Direction.Credit);
            journal.set("debitoriSum", BigDecimal.ZERO);
            journal.set("debitnatSum", BigDecimal.ZERO);
            if(entity.getSinglebatch()!=null
                    && (SingleBatch.Batch.getValue()+"").equals(entity.getSinglebatch()+"")
                    && entity.getSuccessTotalAmt()!=null
                    && entity.getSuccessTotalAmt().compareTo(BigDecimal.ZERO) > 0){
                journal.set("creditoriSum", entity.getSuccessTotalAmt());
                BigDecimal creditnatSum=BigDecimalUtils.safeMultiply(entity.getSuccessTotalAmt(),entity.getExchangerate());
                journal.set("creditnatSum",creditnatSum);
            }else{
                journal.set("creditoriSum", isExchangePayment?entity.getExchangePaymentAmount():entity.getOriginalcurrencyamt());
                journal.set("creditnatSum", entity.getNatAmt());
            }
            journal.setRptype(RpType.find(RpType.PayBill.getValue()));

        } else if (receiptType.shortValue() == ReceiptType.receive.getValue()) {
            //借贷方向  借 ,借方原币金额,借方本币金额,贷方原币金额,贷方本币金额
            journal.setDirection(Direction.Debit);
            journal.set("debitoriSum", entity.getOriginalcurrencyamt());
            journal.set("debitnatSum", entity.getNatAmt());
            journal.set("creditoriSum", BigDecimal.ZERO);
            journal.set("creditnatSum", BigDecimal.ZERO);
        }
    }

    private void setCounterInfo(Journal journal, SettleBench_b entity, String counterPartyType) {
        journal.setCaobject(CaObject.find(Integer.parseInt(counterPartyType)));
        String counterPartyBankAccount = entity.getCounterpartybankaccount();
        journal.setOthertitle(entity.getCounterpartyname());
        if (Short.parseShort(counterPartyType) == CaObject.Customer.getValue()) {
            journal.setCustomer(entity.getCounterpartyid()!=null?Long.parseLong(entity.getCounterpartyid()):null);
            //对方的银行账户信息不会空就统计
            journal.setCustomerbankaccount(counterPartyBankAccount != null ? Long.parseLong(counterPartyBankAccount) : null);
        } else if (Short.parseShort(counterPartyType) == CaObject.Supplier.getValue()) {
            journal.setSupplier(entity.getCounterpartyid()!=null?Long.parseLong(entity.getCounterpartyid()):null);
            journal.setSupplierbankaccount(counterPartyBankAccount != null ? Long.parseLong(counterPartyBankAccount) : null);
        } else if (Short.parseShort(counterPartyType) == CaObject.Employee.getValue()) {
            journal.setEmployee(entity.getCounterpartyid());
            journal.setEmployeeaccount(counterPartyBankAccount);
        }else if (Short.parseShort(counterPartyType) == CaObject.CapBizObj.getValue()) {
            journal.setCapBizObj(StringUtils.isEmpty(entity.getCounterpartyid())?null:entity.getCounterpartyid());
            journal.setCapBizObjbankaccount(!StringUtils.isEmpty(counterPartyBankAccount)?counterPartyBankAccount:null);
        }else if (Short.parseShort(counterPartyType) == CaObject.InnerUnit.getValue()) {
            journal.setInnerunit(StringUtils.isEmpty(entity.getCounterpartyid())?null:entity.getCounterpartyid());
            journal.setInnerunitbankaccount(!StringUtils.isEmpty(counterPartyBankAccount)?counterPartyBankAccount:null);
        }else{
            journal.setOthername(entity.getCounterpartyname());
        }

    }

    private void setJournalStatus(Journal journal,short journalType) throws BizException {
        //变更后提交跟新增审核态一样
        if (JournalType.EditSubmit.getValue() == journalType) {
            journalType = JournalType.AuditSubmit.getValue();
        }
        if(journalType == JournalType.FirstSubmit.getValue()){
            journal.setPaymentstatus(PaymentStatus.NoPay);
            journal.setSettlestatus(SettleStatus.noSettlement);
            journal.setAuditstatus(AuditStatus.Incomplete);
        }else if(journalType == JournalType.AuditSubmit.getValue()){
            journal.setPaymentstatus(PaymentStatus.NoPay);
            journal.setSettlestatus(SettleStatus.noSettlement);
            journal.setAuditstatus(AuditStatus.Complete);
        }else if(journalType == JournalType.SuccessSubmit.getValue() || journalType == JournalType.StrikeSubmit.getValue()){
            journal.setPaymentstatus(PaymentStatus.PayDone);
            journal.setSettlestatus(SettleStatus.alreadySettled);
            journal.setAuditstatus(AuditStatus.Complete);
        }else{
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101160"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_STWB-BE_17FEC3DA04180309","请输入正确的日记账类型") /* "请输入正确的日记账类型" */);
        }
    }

    /**
     * 执行操作后调用 适配前端 完成当前操作
     * @param params
     */
    private void completedThisCommit(CtmJSONObject params){
        String uid = params.getString("uid");
        ProcessUtil.completed(uid);
    }

    /**
     *
     * @param dataInformation  修改的数据信息
     * @param code             单据号(日记账为来源单据号、期初信息为账户account)
     * @param name             当前执行的操作
     * @param serviceCode       修改数据的节点服务code
     * @param busiObjTypeNameResId 节点名称
     * @param operationNameResId    操作名称
     */
    private void saveBusinessLog(Object dataInformation, String code, String name, String serviceCode,
                                 String busiObjTypeNameResId, String operationNameResId){
        try {
            ctmcmpBusinessLogService.saveBusinessLog(
                    CtmJSONObject.toJSONString(dataInformation),
                    code,
                    name,
                    serviceCode,
                    busiObjTypeNameResId,
                    operationNameResId);
        } catch (Exception var8) {
            log.error("记录业务日志失败：" + var8.getMessage());
        }

    }
}
