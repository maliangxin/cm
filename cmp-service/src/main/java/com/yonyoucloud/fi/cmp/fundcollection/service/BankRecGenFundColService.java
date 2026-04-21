package com.yonyoucloud.fi.cmp.fundcollection.service;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.log.model.BusinessObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.log.util.BusiObjectBuildUtil;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.mdd.ext.bill.billmake.service.MakeBillRuleClientService;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankrecAutoSubmitService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.billclaim.CorrDataEntityParam;
import com.yonyoucloud.fi.cmp.util.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import yonyou.bpm.rest.RepositoryService;
import yonyou.bpm.rest.request.repository.ProcessDefinitionQueryParam;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * 到账认领V2
 * 银行对账单生成资金收款单
 * 新建service BankReconciliationGenerateFundCollectionService
 */
@Service
@Slf4j
public class BankRecGenFundColService {

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
    CtmcmpReWriteBusRpcService ctmcmpReWriteBusRpcService;
    @Autowired
    ProcessService processService;
    @Autowired
    BankrecAutoSubmitService bankrecAutoSubmitService;
    @Autowired
    private IFIBillService fiBillService;
    @Autowired
    public IBusinessLogService businessLogService;
    private static final String QUICK_TYPE_MAPPER = "com.yonyoucloud.fi.cmp.mapper.QuickTypeMapper";

    @Resource
    private IFundCommonService fundCommonService;
    /**
     * 1,根据转单规则生成资金收款单
     * 2,回写业务生单信息
     *
     * @param bankReconciliations
     * @param
     */
    public void bankReconciliationGenerateFundCollection(List<BankReconciliation> bankReconciliations, boolean autoSubmit1) throws Exception {
        //CZFW-354521 问题修复：batchReWriteBankRecilicationCorrelation 接口看似是批量，目前只支持单条数据
        List<FundCollection> fundCollections = new ArrayList<FundCollection>();
        List<String> lockFailBankSeqNoList = new ArrayList<>();
        /**
         * 1，数据转换为可保存的收款单实体
         * 2，拼接需要回写的关联信息实体
         */
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(bankReconciliation.getId().toString());
            try{
                if (ymsLock == null) {
                    lockFailBankSeqNoList.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A07804C0000A", "加锁失败，银行交易流水号为：") /* "加锁失败，银行交易流水号为：" */+bankReconciliation.getBank_seq_no());
                    continue;
                }
                bankReconciliation.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BankReconciliation.ENTITY_NAME,bankReconciliation);
                if (bankReconciliation.getOppositetype() != null && OppositeType.Other.getValue() == bankReconciliation.getOppositetype()){
                    bankReconciliation.setOppositeobjectname(bankReconciliation.getTo_acct_name());
                }
                //需要每一条都要调用关联接口
                Map<String, Object> map = generateFundCollection(bankReconciliation);
                List<Map<String, Object>> tarList = (List) map.get("tarList");//生成资金付款单数据列表
                if (tarList.isEmpty()) continue;
                FundCollection fundCollection = getFundCollection(tarList.get(0));
                fundCollection.put("bizObjType", "cmp.fundcollection.FundCollection");
                int autoSubmit = ValueUtils.isNotEmptyObj(fundCollection.get("autoSubmit"))
                        ? Integer.parseInt(fundCollection.get("autoSubmit").toString()): 0;
                if(autoSubmit1){
                    autoSubmit = 1;
                }
                fundCollection.set("autoSubmit", autoSubmit);
                if(StringUtils.isEmpty(fundCollection.getTradetype())){//交易类型如果没有通过单据转换配置无值的话设置默认值
                    fundCollection.setTradetype(queryTransTypeId("cmp_fundcollection_other"));//1840286060811650645 cmp_fundcollection_other 交易类型其他收款
                }
                fundCollection.setSettleflag((short)1);// FundCollectionAuditRule 会判断settleflag为true生成推结算
                fundCollections.add(fundCollection);
                List<CorrDataEntityParam> corrDataEntities = new ArrayList<>();//需要回写关联信息实体
                //提前入账场景赋值
                if (bankReconciliation.getIsadvanceaccounts() != null && bankReconciliation.getIsadvanceaccounts()){
                    fundCollection.setIsadvanceaccounts(bankReconciliation.getIsadvanceaccounts());
                    fundCollection.setAssociationcount(bankReconciliation.getAssociationcount());
                }
                CorrDataEntityParam corrDataEntity = getCorrDataEntity(fundCollection);
                if (corrDataEntity == null) {
                    continue;
                }
                if (corrDataEntity.getExtendFields() != null){
                    corrDataEntity.getExtendFields().put("bankReconciliation",bankReconciliation);
                }else {
                    HashMap<String, Object> extendFields = new HashMap();
                    extendFields.put("bankReconciliation",bankReconciliation);
                    corrDataEntity.setExtendFields(extendFields);
                }
                corrDataEntities.add(corrDataEntity);
                //财资统一对账码获取，放到资金付款单保存之前
                //回写关联信息
                String result = ctmcmpReWriteBusRpcService.batchReWriteBankRecilicationForRpc(corrDataEntities);
                CtmJSONObject ctmJSONObject = CtmJSONArray.parseArray(result).getJSONObject(0);
                String smartcheckno = ctmJSONObject.getString("smartcheckno");

                List<FundCollection_b> fundCollection_bs = fundCollection.FundCollection_b();
                for (FundCollection_b fundCollection_b : fundCollection_bs) {
                    fundCollection_b.setFundSettlestatus(FundSettleStatus.SettlementSupplement);
                    fundCollection_b.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundCollection_b.getFundSettlestatus()));
                    //财资统一对账码赋值
                    fundCollection_b.setSmartcheckno(smartcheckno);
                    fundCollection_b.put("bizobjtype", "cmp.fundcollection.FundCollection_b");
                    if (OppositeType.Other.getValue() == bankReconciliation.getOppositetype()){
                        fundCollection_b.setOppositeaccountid(bankReconciliation.getOppositeobjectid());
                        fundCollection_b.setOppositeobjectname(bankReconciliation.getTo_acct_name());
                    }
                }
                fundCollection.setCreateDate(new Date());
                fundCollection.setCreateTime(new Date());
                fundCollection.setCreator(InvocationInfoProxy.getUsername());
                //结算简强字段赋值
                fundCommonService.setSimpleSettleValue(IBillNumConstant.FUND_COLLECTION, fundCollection);
                //如果关联成功则资金收入库
                if (result !=null && result.contains("iscorrsuccess")){
                    CmpMetaDaoHelper.insert(FundCollection.ENTITY_NAME,fundCollection);//保存资金收款单信息
                }

                //走保存规则，以命中审批流
                //202409 是否开启审批流方法重写。 走平台保存规则会导致id和code被重新赋值
//            BillContext billContext = new BillContext();
//            billContext.setBillnum(IBillNumConstant.FUND_COLLECTION);
//            billContext.setAction(IActionConstant.SAVE);
//            Map<String, Object> paramMap = new HashMap<>();
//            billContext.setCardKey(IBillNumConstant.FUND_COLLECTION);
//            BillDataDto dataDto = new BillDataDto(IBillNumConstant.FUND_COLLECTION);
//            dataDto.setData(fundCollection);
//            paramMap.put("param", dataDto);
//            billContext.setSubid(ICmpConstant.CMP_MODUAL_NAME);
//            billContext.setBilltype("ArchiveList");
//            billContext.setEntityCode(IBillNumConstant.FUND_COLLECTION);
//            billContext.setAction(IActionConstant.SAVE);
//            billContext.setFullname(FundCollection.ENTITY_NAME);
//            billContext.setContextValue("taskSave", Boolean.TRUE);
//            BillBiz.executeRule(IActionConstant.SAVE, billContext, paramMap);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102232"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A00C05400014", "银行交易流水号：") /* "银行交易流水号：" */ + bankReconciliation.getBank_seq_no() +com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A00C05400013", ",错误原因:") /* ",错误原因:" */ + e.getMessage());
            }finally {
                if(ymsLock!=null){
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                }
            }
        }
        if(CollectionUtils.isNotEmpty(lockFailBankSeqNoList)){
            log.error("资金收款-账户交易流水自动生单（智能认领），获取锁失败信息："+ JSON.toJSONString(lockFailBankSeqNoList));
        }

        //事物结束后异步执行
        List<String> ids = new ArrayList();//fundCollections.stream().map(e->String.valueOf(e.getId())).collect(Collectors.toList());
        for (FundCollection fundCollection : fundCollections) {
            Integer autoSubmit = fundCollection.getInteger("autoSubmit");
            if (autoSubmit !=null && autoSubmit == 1){
                Long id = fundCollection.getId();
                ids.add(id.toString());
            }
        }
        if (CollectionUtils.isEmpty(ids)){
            return;
        }
        CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @SneakyThrows
            @Override
            public void afterCommit() {
                try {
                    RobotExecutors.runAs(AppContext.getYTenantId(), new Callable() {
                        @Override
                        public Object call() throws Exception {
                            bankrecAutoSubmitService.autoSubmit(FundCollection.ENTITY_NAME,ids,"cmp_fundcollection");
                            return null;
                        }
                    }, ctmThreadPoolExecutor.getThreadPoolExecutor());
                } catch (Exception e) {
                    log.error("智能规则提前入账生单自动提交资金收款单单据提交失败：" + e.getMessage());
                    throw e;
                }
            }
        });
    }
    /**
     *  资金收款单 cmp_fundcollection_other  其他款项
     *  资金付款单 cmp_fund_payment_other    其他付款
     * @return根据code获取当前租户的交易类型默认值
     * @throws Exception
     */
    public String queryTransTypeId(String code) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bill.TransType");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_TRANSTYPE);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect(" id ");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").eq(code));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> transTypes = MetaDaoHelper.query(billContext, schema);
        if(CollectionUtils.isEmpty(transTypes)){
            log.error("未获取到交易类型"+code);
            return null;
        }else{
            Object id = transTypes.get(0).get("id");
            return id.toString();
        }
    }
    /**
     * 生成资金付款单单据
     * 返回已生成的单据用于回写信息
     *
     * @param bankReconciliation
     * @return
     */
    public Map<String, Object> generateFundCollection(BankReconciliation bankReconciliation) throws Exception {
        PushAndPullVO pullVO = forPushAndPullVOValue(bankReconciliation);//获取转单实体
        Map<String, Object> map = makeBillRuleClientService.getTargetList(pullVO, MAKETYPE);//调用转单规则方法 - 返回转单成功的单据
        return map;
    }

    /**
     * 获取转单规则实体
     *
     * @param bankReconciliation
     * @return
     */
    private PushAndPullVO forPushAndPullVOValue(BankReconciliation bankReconciliation) {
        List<BizObject> bizObjects = new ArrayList<BizObject>();
        List<String> ids = new ArrayList<String>();
        /**
         * 将对账单实体转成BizObject
         * 添加参数 -- 款项类型&交易类型
         * 将单据id放入ids
         */
        BizObject data = bankReconciliation;
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
     *
     * @param obj
     * @return
     */
    private FundCollection getFundCollection(Map<String, Object> obj) throws Exception {
        FundCollection fundCollection = new FundCollection();
        fundCollection.init(obj);
        fundCollection.setFundCollection_b(getFundCollection_b(obj));
        fundCollection.setSrcitem(EventSource.Cmpchase);
        fundCollection.setEntityStatus(EntityStatus.Insert);
        fundCollection.setOriSum(fundCollection.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
        Long mainid =ymsOidGenerator.nextId();
        fundCollection.setId(mainid);
        fundCollection.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        fundCollection.setVoucherstatus(VoucherStatus.Empty);
        fundCollection.FundCollection_b().get(0).setMainid(String.valueOf(mainid));
        fundCollection.setNatSum(fundCollection.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundCollection.setConfirmstatus(Relationstatus.Confirm.getValue());//待确认状态
        if(!ValueUtils.isNotEmptyObj(fundCollection.getEntrytype())){
            fundCollection.setEntrytype(EntryType.Normal_Entry.getValue());
        }
        //fundCollection.setTradetype(autoorderrule_b.getTradetype());
        fundCollection.setCode(getBillCode(fundCollection));
        // 特征处理 否则保存报错
        BizObject characterDef = fundCollection.get("characterDef");
        if(characterDef != null){
            characterDef.put("id",ymsOidGenerator.nextId());
            characterDef.put("ytenant",fundCollection.get("ytenantId"));
        }
        //资金收款单子表单据转换配置特征需要处理给特征添加主键及租户id
        log.error("----------------------资金收款单单据转换模板转换后的收款单------------------------------------");
        log.error(CtmJSONObject.toJSONString(fundCollection));
        log.error("----------------------资金收款单单据转换模板转换后的收款单------------------------------------");
        CtmJSONObject logData = new CtmJSONObject();
        logData.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A00C05400011", "资金收款单单据转换模板转换后的收款单") /* "资金收款单单据转换模板转换后的收款单" */,CtmJSONObject.toJSONString(fundCollection));
        BusinessObject businessObject = BusiObjectBuildUtil.build(IServicecodeConstant.CMPBANKRECONCILIATION,"", OperCodeTypes.query,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A00C05400012", "银行对账单生成收款单") /* "银行对账单生成收款单" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A00C05400012", "银行对账单生成收款单") /* "银行对账单生成收款单" */,logData);
        businessLogService.saveBusinessLog(businessObject);
        BizObject fundCollection_bCharacterDef = fundCollection.FundCollection_b().get(0).get("characterDefb");
        if(fundCollection_bCharacterDef != null){
            fundCollection_bCharacterDef.put("id",ymsOidGenerator.nextId());
            fundCollection_bCharacterDef.put("ytenant",fundCollection.get("ytenantId"));
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
     *
     * @param obj
     * @return
     */
    private List<FundCollection_b> getFundCollection_b(Map<String, Object> obj) {
        List<Map<String, Object>> fundCollection_bs = (List<Map<String, Object>>) obj.get("FundCollection_b");
        List<FundCollection_b> fundCollection_bList = new ArrayList<FundCollection_b>();
        FundCollection_b fundCollection_b = new FundCollection_b();
        fundCollection_b.init(fundCollection_bs.get(0));
        fundCollection_b.setNatSum(fundCollection_b.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundCollection_b.setOriSum(fundCollection_b.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
//        fundCollection_b.setSettlesuccessSum(fundCollection_b.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        //fundCollection_b.setQuickType(autoorderrule_b.getQuickType());
        fundCollection_b.setId(ymsOidGenerator.nextId());
        fundCollection_b.setBankReconciliationId(fundCollection_b.get("bankReconciliationId").toString());
        fundCollection_b.setAssociationStatus(AssociationStatus.Associated.getValue());
        fundCollection_b.setLineno(new BigDecimal("1"));
        fundCollection_b.setEntityStatus(EntityStatus.Insert);
        fundCollection_bList.add(fundCollection_b);
        return fundCollection_bList;
    }

    /**
     * 生成关联关系实体
     *
     * @param fundCollection
     * @return
     */
    private CorrDataEntityParam getCorrDataEntity(FundCollection fundCollection) {
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
        corrData.setIsadvanceaccounts(fundCollection.getIsadvanceaccounts());
        corrData.setAssociationcount(fundCollection.getAssociationcount());
        return corrData;
    }

    /**
     * 获取单据编码
     *
     * @param fundCollection
     * @return
     */
    private String getBillCode(FundCollection fundCollection) {
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        String bizObjCode = CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.FUND_COLLECTION);
        log.error("生成资金收款单据编码bizObjCode:"+bizObjCode);
        String tenantId = InvocationInfoProxy.getTenantid();
        log.error("生成资金收款单据编码===TenantId:"+tenantId);
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(
                null,
                IBillNumConstant.FUND_COLLECTION,
                tenantId,
                null,
                FundCollection.ENTITY_NAME,
                new BillCodeObj[]{new BillCodeObj(fundCollection)});
        String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
        log.error("生成资金收款单据编码codelist:"+CtmJSONObject.toJSONString(codelist));
        if (codelist != null && codelist.length > 0) {
            return codelist[0];
        } else {
            return null;
        }

    }
}
