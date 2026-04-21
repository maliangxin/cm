package com.yonyoucloud.fi.cmp.fundcollection.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.billmake.service.MakeBillRuleClientService;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cmp.autoorderrule.Autoorderrule_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.billclaim.CorrDataEntityParam;
import com.yonyoucloud.fi.cmp.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yonyou.bpm.rest.RepositoryService;
import yonyou.bpm.rest.request.repository.ProcessDefinitionQueryParam;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.SELECT_TOTAL_PARAM;

/**
 * 到账认领V2
 * 银行对账单生成资金收款单
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class BankReconciliationGenerateFundCollectionService {

    private final String FUNDCOLLECTIONRULE = "banktofundcollection"; //对账单转资金付款单转单规则Code
    private final String MAKETYPE = "pull"; //转单规则makeType
    private String ytenant = null;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    MakeBillRuleClientService makeBillRuleClientService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private ProcessService processService;
    @Autowired
    CtmcmpReWriteBusRpcService ctmcmpReWriteBusRpcService;

    @Resource
    private IFundCommonService fundCommonService;

    private static final String QUICK_TYPE_MAPPER = "com.yonyoucloud.fi.cmp.mapper.QuickTypeMapper";

    /**
     * 1,根据转单规则生成资金收款单
     * 2,回写业务生单信息
     * @param bankReconciliations
     * @param autoorderrule_b
     */
    public void bankReconciliationGenerateFundCollection(List<BankReconciliation> bankReconciliations , Autoorderrule_b autoorderrule_b) throws Exception {
        //设置款项类型 -- 交易类型
        autoorderrule_b.setTradetype(autoorderrule_b.getTradetype() == null ? getDefultTradeType() : autoorderrule_b.getTradetype());
        autoorderrule_b.setQuickType(autoorderrule_b.getQuickType() == null ? getDefaultQuickType() : autoorderrule_b.getQuickType());
        List<String> lockFailBankSeqNoList = new ArrayList<>();
        /**
         * 1，数据转换为可保存的收款单实体
         * 2，拼接需要回写的关联信息实体
         */
        for (BankReconciliation bankReconciliation:bankReconciliations) {
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(bankReconciliation.getId().toString());
            try{
                if (ymsLock == null) {
                    lockFailBankSeqNoList.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540059A", "加锁失败：银行交易流水号：") /* "加锁失败：银行交易流水号：" */+bankReconciliation.getBank_seq_no());
                    continue;
                }
                //需要每一条都要调用关联接口
                //需要回写关联信息实体
                List<CorrDataEntityParam> corrDataEntities = new ArrayList<>();
                Map<String,Object> map = generateFundCollection(bankReconciliation,autoorderrule_b);
                List<Map<String, Object>> tarList = (List)map.get("tarList");//生成资金付款单数据列表
                if(tarList.isEmpty()) continue;
                FundCollection fundCollection = getFundCollection(tarList.get(0),autoorderrule_b);
                fundCollection.put("bizObjType", "cmp.fundcollection.FundCollection");
                if (fundCollection.getInteger("autoSubmit") != null && 1 == fundCollection.getInteger("autoSubmit")){
                    fundCollection.setConfirmstatus(Relationstatus.Confirmed.getValue());
                    //需要标记是自动生单，否则生单确认页面查询不出来
                    fundCollection.put("isAuto",true);
                    List<FundCollection_b> fundCollection_bs = fundCollection.FundCollection_b();
                    for (FundCollection_b fundCollection_b : fundCollection_bs) {
                        fundCollection_b.put("bizobjtype", "cmp.fundcollection.FundCollection_b");
                    }
                    //自动提交是走后台规则，会有关联回写逻辑，不需要调用回写接口
                    executeRule(fundCollection);
                }else {
                    corrDataEntities.add(getCorrDataEntity(fundCollection));
                    //关联关系回写，解析财资统一对账码
                    String result =  ctmcmpReWriteBusRpcService.batchReWriteBankRecilicationForRpc(corrDataEntities);//回写关联信息
                    CtmJSONObject ctmJSONObject = CtmJSONArray.parseArray(result).getJSONObject(0);
                    String smartcheckno = ctmJSONObject.getString("smartcheckno");
                    //资金收款单财资统一对账码赋值
                    List<FundCollection_b> fundCollection_bs = fundCollection.FundCollection_b();
                    for (FundCollection_b fundCollection_b : fundCollection_bs) {
                        fundCollection_b.setFundSettlestatus(FundSettleStatus.SettlementSupplement);
                        fundCollection_b.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundCollection_b.getFundSettlestatus()));
                        //财资统一对账码赋值
                        fundCollection_b.setSmartcheckno(smartcheckno);
                        fundCollection_b.put("bizobjtype", "cmp.fundcollection.FundCollection_b");
                    }
                    //如果关联成功则资金收入库
                    if (result !=null && result.contains("iscorrsuccess")){
                        fundCollection.setCreateDate(new Date());
                        fundCollection.setCreateTime(new Date());
                        fundCollection.setCreator(InvocationInfoProxy.getUsername());
                        //结算简强字段赋值
                        fundCommonService.setSimpleSettleValue(IBillNumConstant.FUND_COLLECTION, fundCollection);
                        CmpMetaDaoHelper.insert(FundCollection.ENTITY_NAME,fundCollection);//保存资金收款单信息
                    }
                }
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102232"),e.getMessage());
            }finally {
                if(ymsLock!=null){
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                }
            }
        }
        if(CollectionUtils.isNotEmpty(lockFailBankSeqNoList)){
            log.error("资金收款单-账户交易流水自动生单，获取锁失败信息："+ JSON.toJSONString(lockFailBankSeqNoList));
        }
    }
    private RuleExecuteResult executeRule(BizObject fundCollection) {
        //以判断过是否开启审批流，不用重复判断
//        try {
//            BillContext billContext = CmpCommonUtil.getBillContextByFundPayment();
//            boolean isWfControlled = processService.bpmControl(billContext, fundCollection);
//            fundCollection.put(ICmpConstant.IS_WFCONTROLLED, isWfControlled);
//        } catch (Exception e) {
//            fundCollection.put(ICmpConstant.IS_WFCONTROLLED, false);
//        }
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum(IBillNumConstant.FUND_COLLECTION);
        dataDto.setData(CtmJSONObject.toJSONString(fundCollection));
        RuleExecuteResult result = cmCommonService.doSaveAndSubmitAction(dataDto);
        // 注意这里判断result是否正常结束的状态，1:代表保存并提交成功；999：代表保存失败；910：代表保存成功但提交失败
        if (1 != result.getMsgCode()) {
            if (999 == result.getMsgCode()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100227"),result.getMessage());
            } else {
                return result;
            }
        } else {
            return result;
        }
    }

    /**
     * 自动生单确认 -- 确认生成资金收款单
     * @param id -- 参数为对账单关联表id 防止String转Long问题 用Object接收
     * @return
     */
    public void confirmGenerateFundCollection(Object id) throws Exception {
        Long autorrid = Long.valueOf(id.toString());
        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
        /**
         * 获取需要确认的业务单据实体
         */
        List<Map<String, Object>> bankReconciliationbusrelation_bs = MetaDaoHelper.queryById(BankReconciliationbusrelation_b.ENTITY_NAME, SELECT_TOTAL_PARAM, autorrid);
        if (bankReconciliationbusrelation_bs.isEmpty())
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100228"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180074", "需确认数据为空！") /* "需确认数据为空！" */));
        bankReconciliationbusrelation_b.init(bankReconciliationbusrelation_bs.get(0));
        Long fundCollectionId = bankReconciliationbusrelation_b.getSrcbillid();
        if(fundCollectionId == null){
            return;
        }
        List<Map<String, Object>> fundCollections = MetaDaoHelper.queryById(FundCollection.ENTITY_NAME, SELECT_TOTAL_PARAM, fundCollectionId.toString());
        /**
         * 进行确认操作
         */
        if (!fundCollections.isEmpty()) {//需要确认的收款单不为空
            FundCollection fundCollection = new FundCollection();
            fundCollection.init(fundCollections.get(0));
//            if (fundCollection.getConfirmstatus() != Relationstatus.Confirm.getValue())
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100229"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180073", "数据非待确认状态，不允许确认!") /* "数据非待确认状态，不允许确认!" */));
            fundCollection.setConfirmstatus(Relationstatus.Confirmed.getValue());//确认状态修改为已确认.
            fundCollection.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundCollection.ENTITY_NAME, fundCollection);
            //202409修改，自动生单时财资统一对账码已赋值，不需要重新赋值
//            //银行对账单
//            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME,
//                    bankReconciliationbusrelation_b.getBankreconciliation());
//            String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
//            if (bankReconciliation != null) {
//                bankReconciliation.setEntityStatus(EntityStatus.Update);
//                if (StringUtils.isNotEmpty(bankReconciliation.getSmartcheckno())) {
//                    smartcheckno = bankReconciliation.getSmartcheckno();
//                }
//                bankReconciliation.setSmartcheckno(smartcheckno);
//                CommonSaveUtils.updateBankReconciliation(bankReconciliation);
//            }
//            //更新资金收款单明细勾兑号
//            List<FundCollection_b> fundCollection_bs = new ArrayList<FundCollection_b>();
//            QuerySchema querySchema = QuerySchema.create().addSelect("*");
//            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(fundCollection.getId()));
//            querySchema.addCondition(group);
//            fundCollection_bs = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
//            for (FundCollection_b fundCollection_b : fundCollection_bs) {
//                fundCollection_b.setEntityStatus(EntityStatus.Update);
//                fundCollection_b.setSmartcheckno(smartcheckno);
//                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollection_b);
//            }
        }
    }

    /**
     * 自动生单确认 -- 拒绝生成资金收款单
     * @param id -- 参数为对账单关联表id 防止String转Long问题 用Object接收
     * @return
     */
    public void refuseGenerateFundCollection(Object id) throws Exception {
        Long autorrid = Long.valueOf(id.toString());
        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
        /**
         * 获取需要确认的业务单据实体
         */
        List<Map<String, Object>> bankReconciliationbusrelation_bs = MetaDaoHelper.queryById(BankReconciliationbusrelation_b.ENTITY_NAME,SELECT_TOTAL_PARAM,autorrid);
        if(bankReconciliationbusrelation_bs.isEmpty())
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100230"), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0022E", "需拒绝数据为空！") /* "需拒绝数据为空！" */));
        bankReconciliationbusrelation_b.init(bankReconciliationbusrelation_bs.get(0));
        Long fundCollectionId = bankReconciliationbusrelation_b.getSrcbillid();
        if(fundCollectionId == null){
            return;
        }
        List<Map<String, Object>> fundCollections = MetaDaoHelper.queryById(FundCollection.ENTITY_NAME,SELECT_TOTAL_PARAM,fundCollectionId.toString());
            /**
             * 进行删除操作
             */
            if(!fundCollections.isEmpty()){//需要拒绝的收款单不为空
                FundCollection fundCollection = new FundCollection();
                fundCollection.init(fundCollections.get(0));
//                if (fundCollection.getConfirmstatus() != Relationstatus.Confirm.getValue())
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100231"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180075","数据非待确认状态，不允许拒绝!") /* "数据非待确认状态，不允许拒绝!" */));
                fundCollection.setEntityStatus(EntityStatus.Delete);
                MetaDaoHelper.delete(FundCollection.ENTITY_NAME,fundCollection);
            }
    }

    /**
     * 生成资金付款单单据
     * 返回已生成的单据用于回写信息
     * @param bankReconciliation
     * @param autoorderrule_b
     * @return
     */
    public Map<String, Object> generateFundCollection(BankReconciliation bankReconciliation, Autoorderrule_b autoorderrule_b) throws Exception {
        PushAndPullVO pullVO = forPushAndPullVOValue(bankReconciliation,autoorderrule_b);//获取转单实体
        Map<String, Object> map = makeBillRuleClientService.getTargetList(pullVO,MAKETYPE);//调用转单规则方法 - 返回转单成功的单据
        return map;
    }

    /**
     * 获取转单规则实体
     * @param bankReconciliation
     * @param autoorderrule_b
     * @return
     */
    private PushAndPullVO forPushAndPullVOValue(BankReconciliation bankReconciliation, Autoorderrule_b autoorderrule_b){
        List<BizObject> bizObjects = new ArrayList<BizObject>();
        List<String> ids = new ArrayList<String>();
        /**
         * 将对账单实体转成BizObject
         * 添加参数 -- 款项类型&交易类型
         * 将单据id放入ids
         */
        BizObject data = bankReconciliation;
        data.set("quickType",autoorderrule_b.getQuickType());
        data.set("tradetype",autoorderrule_b.getTradetype());
        data.put("isAutoPull","auto");//BankSetEntrytypePullBeforeRule类处理入账类型自动生单无需处理，在此做标记用于规则类直接return
        bizObjects.add(data);
        ids.add(bankReconciliation.getId().toString());
        PushAndPullVO pullVO = new PushAndPullVO();
        pullVO.setCode(FUNDCOLLECTIONRULE);
        pullVO.setMakeType(MAKETYPE);
        pullVO.setChildIds(ids);
        pullVO.setSourceData(bizObjects);
        pullVO.setIsMainSelect(1);
        return pullVO;
    }

    /**
     * 获取业务生单 - 资金收款单对象
     * @param obj
     * @param autoorderrule_b
     * @return
     */
    private FundCollection getFundCollection(Map<String, Object> obj,Autoorderrule_b autoorderrule_b) throws Exception{
        FundCollection fundCollection = new FundCollection();
        fundCollection.init(obj);
        fundCollection.setFundCollection_b(getFundCollection_b(obj,autoorderrule_b));
        fundCollection.setEntityStatus(EntityStatus.Insert);
        fundCollection.setOriSum(fundCollection.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundCollection.setId(ymsOidGenerator.nextId());
        fundCollection.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        fundCollection.setVoucherstatus(VoucherStatus.Empty);
        fundCollection.FundCollection_b().get(0).setMainid(String.valueOf((long)fundCollection.getId()));
        fundCollection.setNatSum(fundCollection.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundCollection.setConfirmstatus(Relationstatus.Confirm.getValue());//待确认状态
        fundCollection.setTradetype(autoorderrule_b.getTradetype());
        fundCollection.setCode(getBillCode(fundCollection));
        if(!ValueUtils.isNotEmptyObj(fundCollection.getSettleflag())){
            fundCollection.setSettleflag(Short.parseShort("1"));
        }
        if(!ValueUtils.isNotEmptyObj(fundCollection.getEntrytype())){
            fundCollection.setEntrytype(EntryType.Normal_Entry.getValue());
        }
//        fundCollection.setSettleflag(ValueUtils.isNotEmptyObj(fundCollection.get("settleflag"))
//                ? fundCollection.get("settleflag") : Short.parseShort("1"));
//        fundCollection.setEntrytype(ValueUtils.isNotEmptyObj(fundCollection.get("entrytype"))
//                ? fundCollection.get("entrytype") : EntryType.Normal_Entry.getValue());

        // 特征处理 否则保存报错
        BizObject characterDef = fundCollection.getCharacterDef();
        if(characterDef != null){
            characterDef.put("id",ymsOidGenerator.nextId());
        }
        BizObject characterDefb = fundCollection.FundCollection_b().get(0).getCharacterDefb();
        if(characterDefb != null){
            characterDefb.put("id",ymsOidGenerator.nextId());
        }
        if(null == fundCollection.getIsWfControlled()) {
            //资金收款单是否开启审批流赋值
            ProcessDefinitionQueryParam param = new ProcessDefinitionQueryParam();
            param.setCategory(fundCollection.getTradetype());
            param.setBillTypeId(ICmpConstant.CM_CMP_FUND_COLLECTION);
            param.setOrgId(fundCollection.getAccentity());
            RepositoryService repositoryService = processService.bpmRestServices().getRepositoryService();
            Object result = repositoryService.checkProcessDefinition(param);
            if (!((ObjectNode) result).get("hasProcessDefinition").booleanValue()) {
                fundCollection.setIsWfControlled(false);
            } else {
                fundCollection.setIsWfControlled(true);
            }
        }
        return fundCollection;
    }

    /**
     * 获取业务生单 - 收款单子表对象
     * @param obj
     * @param autoorderrule_b
     * @return
     */
    private List<FundCollection_b> getFundCollection_b(Map<String, Object> obj,Autoorderrule_b autoorderrule_b){
        List<Map<String,Object>> fundCollection_bs = (List<Map<String,Object>>)obj.get("FundCollection_b");
        List<FundCollection_b> fundCollection_bList = new ArrayList<FundCollection_b>();
        FundCollection_b fundCollection_b = new FundCollection_b();
        fundCollection_b.init(fundCollection_bs.get(0));
        fundCollection_b.setNatSum(fundCollection_b.getNatSum().setScale(8,BigDecimal.ROUND_DOWN));
        fundCollection_b.setOriSum(fundCollection_b.getOriSum().setScale(8,BigDecimal.ROUND_DOWN));
//        自动生单不再给结算金额赋值，影响海康对账流程。缺陷编码 CZFW-88700
//        fundCollection_b.setSettlesuccessSum(fundCollection_b.getNatSum().setScale(8,BigDecimal.ROUND_DOWN));
        fundCollection_b.setQuickType(autoorderrule_b.getQuickType());
        fundCollection_b.setId(ymsOidGenerator.nextId());
        fundCollection_b.setBankReconciliationId(fundCollection_b.get("bankReconciliationId").toString());
        fundCollection_b.setAssociationStatus(AssociationStatus.Associated.getValue());
        fundCollection_b.setEntityStatus(EntityStatus.Insert);
        fundCollection_bList.add(fundCollection_b);
        return  fundCollection_bList;
    }

    /**
     * 生成关联关系实体
     * @param fundCollection
     * @return
     */
    private CorrDataEntityParam getCorrDataEntity(FundCollection fundCollection){
        CorrDataEntityParam corrData = new CorrDataEntityParam();
        FundCollection_b fundCollection_b = fundCollection.FundCollection_b().get(0);
        corrData.setBankReconciliationId(Long.valueOf(fundCollection_b.getBankReconciliationId()));
        corrData.setAccentity(fundCollection.getAccentity());
        corrData.setCode(fundCollection.getCode());
        corrData.setAuto(true);
        corrData.setGenerate(true);
        corrData.setBillNum(IBillNumConstant.FUND_COLLECTION);
        corrData.setDept(fundCollection_b.getDept());
        corrData.setBillType(String.valueOf(EventType.FundCollection.getValue()));
        corrData.setBusid(fundCollection_b.getId());
        corrData.setMainid(fundCollection.getId());
        corrData.setOriSum(fundCollection_b.getOriSum());
        corrData.setVouchdate(fundCollection.getVouchdate());
        corrData.setProject(fundCollection_b.getProject());
        return corrData;
    }

    /**
     * 获取单据编码
     * @param fundCollection
     * @return
     */
    private String getBillCode(FundCollection fundCollection){
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.FUND_COLLECTION),IBillNumConstant.FUND_COLLECTION,fundCollection.get("ytenantId"),null,null,new BillCodeObj[]{new BillCodeObj(fundCollection)});
        String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
        if(codelist!=null && codelist.length>0){
            return codelist[0];
        }else {
            return null;
        }
    }

    /**
     * 获取默认的交易类型
     * @return
     */
    private String getDefultTradeType(){
        String billTypeId = null;
        try {
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(ICmpConstant.CM_CMP_FUND_COLLECTION));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error("查询资金付款单交易类型失败！请检查数据！");
//                    throw new CtmException("查询资金付款单交易类型失败！请检查数据。");
                }
                billTypeId = MapUtils.getString(objectMap, "id");
            }
            Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById(billTypeId, "1", null);
            if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                return  tradetypeMap.get("id").toString();
            }
        } catch (Exception e) {
            log.error("未获取到默认的交易类型！, billTypeId = {}, e = {}", billTypeId, e.getMessage());
//            throw new CtmException("未获取到默认的交易类型！");
        }
        return null;
    }

    /**
     * 获取默认款项类型
     * @return
     */
    private Long getDefaultQuickType(){
        try{
            short quickCode = QuickType.sundry.getValue();
            HashMap<String, Object> quickCodeMap = SqlHelper.selectOne(QUICK_TYPE_MAPPER + ".getFundCollectionQuickTypeCode", AppContext.getTenantId());
            if (null != quickCodeMap && null != quickCodeMap.get("cDefaultValue") && !"".equals(quickCode)) {
                quickCode = Short.parseShort(String.valueOf(quickCodeMap.get("cDefaultValue")));
            }
            List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(quickCode));
            if(!quickTypeMap.isEmpty()){
                return Long.valueOf(quickTypeMap.get(0).get("id").toString());
            }
        }catch (Exception e){
            log.error("获取默认款项类型错误::"+e.getMessage());
        }
        return null;
    }
}
