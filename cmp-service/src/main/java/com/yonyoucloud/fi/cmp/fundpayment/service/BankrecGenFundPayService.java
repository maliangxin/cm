package com.yonyoucloud.fi.cmp.fundpayment.service;

import com.alibaba.fastjson.JSON;
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
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankrecAutoSubmitService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import yonyou.bpm.rest.RepositoryService;
import yonyou.bpm.rest.request.repository.ProcessDefinitionQueryParam;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 到账认领V2 -- 自动生单 -- 银行对账单生成资金付款单
 * 新建service BankreconciliationGenerateFundpaymentService
 *
 * @author msc
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class BankrecGenFundPayService {

    private final String FUNDPAYMENTRULE = "banktofundpayment"; //对账单转资金付款单转单规则Code
    private final String MAKETYPE = "push"; //转单规则makeType

    @Autowired
    ProcessService processService;
    @Autowired
    MakeBillRuleClientService makeBillRuleClientService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private CtmSignatureService signatureService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    CtmcmpReWriteBusRpcService ctmcmpReWriteBusRpcService;
    @Autowired
    BankrecAutoSubmitService bankrecAutoSubmitService;
    @Autowired
    public IBusinessLogService businessLogService;
    private static final String QUICK_TYPE_MAPPER = "com.yonyoucloud.fi.cmp.mapper.QuickTypeMapper";
    @Resource
    private IFundCommonService fundCommonService;
    /**
     * 1，自动生单 -- 银行对账单生成资金付款单
     * 2，生单成功后，回调关联接口 - 回写生单关联信息
     *
     * @param bankReconciliations
     * @param
     */
    public void bankreconciliationGenerateFundpayment(List<BankReconciliation> bankReconciliations, boolean autoSubmit1) throws Exception {
        //CZFW-354521 问题修复：batchReWriteBankRecilicationCorrelation 接口看似是批量，目前只支持单条数据
        List<FundPayment> fundPayments = new ArrayList<>();
        List<String> lockFailBankSeqNoList = new ArrayList<>();
        /**
         * 1，转换数据格式为可保存付款单实体
         * 2，拼接回写关联信息实体
         */
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(bankReconciliation.getId().toString());
            try{
                if (ymsLock == null) {
                    lockFailBankSeqNoList.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005C2", "加锁失败：银行交易流水号：") /* "加锁失败：银行交易流水号：" */+bankReconciliation.getBank_seq_no());
                    continue;
                }
                if (bankReconciliation.getOppositetype() != null && OppositeType.Other.getValue() == bankReconciliation.getOppositetype()){
                    bankReconciliation.setOppositeobjectname(bankReconciliation.getTo_acct_name());
                }

                //需要每一条都要调用关联接口
//                List<CorrDataEntity> corrDataEntities = new ArrayList<>();//需要回写关联信息实体
                // 将要生单的流水转成资金付款单
                Map<String, Object> map = generateFundpayment(bankReconciliation);
                List<Map<String, Object>> tarList = (List) map.get("tarList");//生成资金付款单数据列表
                if (tarList.isEmpty()) continue;
                FundPayment fundPayment = getFundPayment(tarList.get(0));
                fundPayment.put("bizObjType", "cmp.fundpayment.FundPayment");
                int autoSubmit = ValueUtils.isNotEmptyObj(fundPayment.get("autoSubmit"))
                        ? Integer.parseInt(fundPayment.get("autoSubmit").toString()): 0;
                if(autoSubmit1){
                    autoSubmit = 1;
                }
                fundPayment.set("autoSubmit", autoSubmit);
                if(StringUtils.isEmpty(fundPayment.getTradetype())){//交易类型如果没有通过单据转换配置无值的话设置默认值
                    fundPayment.setTradetype(queryTransTypeId("cmp_fund_payment_other"));//1840286060811650631 cmp_fund_payment_other 交易类型 其他付款
                }
                fundPayment.setSettleflag((short)1);// fundPaymentAuditRule 付款单审批Settleflag为true传结算
                fundPayments.add(fundPayment);
                List<CorrDataEntityParam> corrDataEntities = new ArrayList<>();//需要回写关联信息实体
                if (bankReconciliation.getIsadvanceaccounts() != null && bankReconciliation.getIsadvanceaccounts()){
                    fundPayment.setIsadvanceaccounts(bankReconciliation.getIsadvanceaccounts());
                    fundPayment.setAssociationcount(bankReconciliation.getAssociationcount());
                }
                corrDataEntities.add(getCorrDataEntity(fundPayment, fundPayment.FundPayment_b().get(0)));
                //财资统一对账码获取，放到资金付款单保存之前
                //回写关联信息
                String result = ctmcmpReWriteBusRpcService.batchReWriteBankRecilicationForRpc(corrDataEntities);
                CtmJSONObject ctmJSONObject = CtmJSONArray.parseArray(result).getJSONObject(0);
                String smartcheckno = ctmJSONObject.getString("smartcheckno");
                List<FundPayment_b> fundPayment_bs = fundPayment.FundPayment_b();
                for (FundPayment_b fundPayment_b : fundPayment_bs) {
                    fundPayment_b.setFundSettlestatus(FundSettleStatus.SettlementSupplement);
                    fundPayment_b.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPayment_b.getFundSettlestatus()));
                    //财资统一对账码赋值
                    fundPayment_b.setSmartcheckno(smartcheckno);
                    fundPayment_b.setBizobjtype("cmp.fundpayment.FundPayment_b");
                    if (OppositeType.Other.getValue() == bankReconciliation.getOppositetype()){
                        fundPayment_b.setOppositeaccountid(bankReconciliation.getOppositeobjectid());
                        fundPayment_b.setOppositeobjectname(bankReconciliation.getTo_acct_name());
                    }
                }
                fundPayment.setCreateDate(new Date());
                fundPayment.setCreateTime(new Date());
                fundPayment.setCreator(InvocationInfoProxy.getUsername());

                //结算简强字段赋值
                fundCommonService.setSimpleSettleValue(IBillNumConstant.FUND_PAYMENT, fundPayment);
                //如果关联成功则资金收入库
                if (result !=null && result.contains("iscorrsuccess")){
                    CmpMetaDaoHelper.insert(FundPayment.ENTITY_NAME,fundPayment);//保存资金付款单主表信息
                }
                //走保存规则，以命中审批流
                //202409 是否开启审批流方法重写。 走平台保存规则会导致id和code被重新赋值
//            BillContext billContext = new BillContext();
//            billContext.setBillnum(IBillNumConstant.FUND_PAYMENT);
//            billContext.setAction(IActionConstant.SAVE);
//            Map<String, Object> paramMap = new HashMap<>();
//            billContext.setCardKey(IBillNumConstant.FUND_PAYMENT);
//            BillDataDto dataDto = new BillDataDto(IBillNumConstant.FUND_PAYMENT);
//            dataDto.setData(fundPayment);
//            paramMap.put("param", dataDto);
//            billContext.setSubid(ICmpConstant.CMP_MODUAL_NAME);
//            billContext.setBilltype("ArchiveList");
//            billContext.setEntityCode(IBillNumConstant.FUND_PAYMENT);
//            billContext.setAction(IActionConstant.SAVE);
//            billContext.setFullname(FundPayment.ENTITY_NAME);
//            billContext.setContextValue("taskSave", Boolean.TRUE);
//            BillBiz.executeRule(IActionConstant.SAVE, billContext, paramMap);
            } catch (Exception e) {
                log.error("资金付款-账户交易流水自动生单（智能认领），保存资金付款单失败："+e.getMessage(),e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102232"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005C4", "银行交易流水号：") /* "银行交易流水号：" */ + bankReconciliation.getBank_seq_no() +com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005C3", ",错误原因:") /* ",错误原因:" */ + e.getMessage());
            }finally {
                if(ymsLock!=null){
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                }
            }
        }
        if(CollectionUtils.isNotEmpty(lockFailBankSeqNoList)){
            log.error("资金付款-账户交易流水自动生单（智能认领），获取锁失败信息："+ JSON.toJSONString(lockFailBankSeqNoList));
        }

        //事物结束后异步执行
        List<String> ids = new ArrayList<>();//fundPayments.stream().map(e->String.valueOf(e.getId())).collect(Collectors.toList());
        for (FundPayment fundPayment : fundPayments) {
            Integer autoSubmit = fundPayment.getInteger("autoSubmit");
            if (autoSubmit != null && autoSubmit == 1){
                Long id = fundPayment.getId();
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
                            bankrecAutoSubmitService.autoSubmit(FundPayment.ENTITY_NAME,ids,"cmp_fundpayment");
                            return null;
                        }
                    }, ctmThreadPoolExecutor.getThreadPoolExecutor());
                } catch (Exception e) {
                     log.error("智能规则提前入账生单自动提交资金付款单单据提交失败：" + e.getMessage());
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
    public Map<String, Object> generateFundpayment(BankReconciliation bankReconciliation) throws Exception {
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
        pullVO.setCode(FUNDPAYMENTRULE);
        pullVO.setMakeType(MAKETYPE);
        pullVO.setChildIds(ids);
        pullVO.setSourceData(bizObjects);
        pullVO.setIsMainSelect(1);
        return pullVO;
    }

    /**
     * 获取业务生单 - 资金付款单对象
     *
     * @param obj
     * @return
     */
    private FundPayment getFundPayment(Map<String, Object> obj) throws Exception {
        FundPayment fundPayment = new FundPayment();
        fundPayment.init(obj);
        fundPayment.setSrcitem(EventSource.Cmpchase);
        fundPayment.setFundPayment_b(getFundPayment_b(obj));
        fundPayment.setEntityStatus(EntityStatus.Insert);
        fundPayment.setConfirmstatus(Relationstatus.Confirm.getValue());//生成单据为待确认状态
        fundPayment.setNatSum(fundPayment.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));//规范精度
        fundPayment.setOriSum(fundPayment.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundPayment.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        fundPayment.setVoucherstatus(VoucherStatus.Empty);
        Long mainid = ymsOidGenerator.nextId();
        fundPayment.setId(mainid);
        fundPayment.FundPayment_b().get(0).setMainid(String.valueOf(mainid));
        if(!ValueUtils.isNotEmptyObj(fundPayment.getEntrytype())){
            fundPayment.setEntrytype(EntryType.Normal_Entry.getValue());
        }
        //fundPayment.setTradetype(autoorderrule_b.getTradetype());
        fundPayment.setCode(getBillCode(fundPayment));
        // 特征处理 否则保存报错
        BizObject characterDef = fundPayment.get("characterDef");
        if(characterDef != null){
            characterDef.put("id",ymsOidGenerator.nextId());
            characterDef.put("ytenant",fundPayment.getYtenantId());
        }
        //资金收款单子表单据转换配置特征需要处理给特征添加主键及租户id
        log.error("----------------------资金付款单单据转换模板转换后的收款单------------------------------------");
        log.error(CtmJSONObject.toJSONString(fundPayment));
        log.error("----------------------资金付款单单据转换模板转换后的收款单------------------------------------");
        CtmJSONObject logData = new CtmJSONObject();
        logData.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005C5", "资金付款单单据转换模板转换后的付款单") /* "资金付款单单据转换模板转换后的付款单" */,CtmJSONObject.toJSONString(fundPayment));
        BusinessObject businessObject = BusiObjectBuildUtil.build(IServicecodeConstant.CMPBANKRECONCILIATION,"", OperCodeTypes.query,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005C6", "银行对账单生成收款单") /* "银行对账单生成收款单" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005C6", "银行对账单生成收款单") /* "银行对账单生成收款单" */,logData);
        businessLogService.saveBusinessLog(businessObject);
        BizObject fundCollection_bCharacterDef = fundPayment.FundPayment_b().get(0).get("characterDefb");
        if(fundCollection_bCharacterDef != null){
            fundCollection_bCharacterDef.put("id",ymsOidGenerator.nextId());
            fundCollection_bCharacterDef.put("ytenant",fundPayment.get("ytenantId"));
        }
        if(null == fundPayment.getIsWfControlled()) {
            //判断资金付款是否开启审批流
            ProcessDefinitionQueryParam param = new ProcessDefinitionQueryParam();
            param.setCategory(fundPayment.getTradetype());
            param.setBillTypeId(ICmpConstant.CM_CMP_FUND_PAYMENT);
            param.setOrgId(fundPayment.getAccentity());
            RepositoryService repositoryService = processService.bpmRestServices().getRepositoryService();
            Object result = repositoryService.checkProcessDefinition(param);
            if (!((ObjectNode) result).get("hasProcessDefinition").booleanValue()) {
                fundPayment.setIsWfControlled(false);
            } else {
                fundPayment.setIsWfControlled(true);
            }
        }
        return fundPayment;
    }

    /**
     * 获取业务生单 - 付款单子表对象
     *
     * @param obj
     * @return
     */
    private List<FundPayment_b> getFundPayment_b(Map<String, Object> obj) throws Exception {
        List<Map<String, Object>> fundPayment_bs = (List<Map<String, Object>>) obj.get("FundPayment_b");
        List<FundPayment_b> fundPayment_bs1 = new ArrayList<FundPayment_b>();
        FundPayment_b fundPayment_b = new FundPayment_b();
        fundPayment_b.init(fundPayment_bs.get(0));
        fundPayment_b.setAssociationStatus(AssociationStatus.Associated.getValue());
        fundPayment_b.setNatSum(fundPayment_b.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundPayment_b.setOriSum(fundPayment_b.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
//        fundPayment_b.setSettlesuccessSum(fundPayment_b.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundPayment_b.setBankReconciliationId(fundPayment_b.get("bankReconciliationId").toString());
        fundPayment_b.setOppositeaccountname(fundPayment_b.getOppositeaccountname() == null ? "" : fundPayment_b.getOppositeaccountname());
        fundPayment_b.setOppositeobjectname(fundPayment_b.getOppositeobjectname() == null ? "" : fundPayment_b.getOppositeobjectname());
        //fundPayment_b.setSignature(getSign(fundPayment_b));
        //fundPayment_b.setQuickType(autoorderrule_b.getQuickType());
        fundPayment_b.setId(ymsOidGenerator.nextId());
        fundPayment_b.setLineno(new BigDecimal("1"));
        fundPayment_b.setEntityStatus(EntityStatus.Insert);
        fundPayment_bs1.add(fundPayment_b);
        return fundPayment_bs1;
    }

    /**
     * 生成关联关系实体
     *
     * @param fundPayment
     * @param fundPayment_b
     * @return
     */
    private CorrDataEntityParam getCorrDataEntity(FundPayment fundPayment, FundPayment_b fundPayment_b) {
        CorrDataEntityParam corrData = new CorrDataEntityParam();
        corrData.setBankReconciliationId(Long.valueOf(fundPayment_b.getBankReconciliationId()));
        corrData.setAccentity(fundPayment.getAccentity());
        corrData.setCode(fundPayment.getCode());
        corrData.setAuto(true);
        corrData.setGenerate(true);
        corrData.setBillNum(IBillNumConstant.FUND_PAYMENT);
        corrData.setDept(fundPayment_b.getDept());
        corrData.setBillType(String.valueOf(EventType.FundPayment.getValue()));
        corrData.setBusid(fundPayment_b.getId());
        corrData.setMainid(fundPayment.getId());
        corrData.setOriSum(fundPayment_b.getOriSum());
        corrData.setVouchdate(fundPayment.getVouchdate());
        corrData.setProject(fundPayment_b.getProject());
        corrData.setIsadvanceaccounts(fundPayment.getIsadvanceaccounts());
        corrData.setAssociationcount(fundPayment.getAssociationcount());
        return corrData;
    }

    /**
     * 获取单据编码
     *
     * @param fundPayment
     * @return
     */
    private String getBillCode(FundPayment fundPayment) {
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        String bizObjCode = CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.FUND_PAYMENT);
        log.error("生成资金付款单据编码bizObjCode:"+bizObjCode);
        String tenantId = InvocationInfoProxy.getTenantid();
        log.error("生成资金付款单据编码TenantId:"+tenantId);
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(
                null,
                IBillNumConstant.FUND_PAYMENT,
                tenantId,
                null,
                FundPayment.ENTITY_NAME,
                new BillCodeObj[]{new BillCodeObj(fundPayment)});
        String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
        log.error("生成资金付款单据编码codelist:"+CtmJSONObject.toJSONString(codelist));
        if (codelist != null && codelist.length > 0) {
            return codelist[0];
        } else {
            return null;
        }
    }

    /**
     * 获取付款单子表签名
     *
     * @param fundPayment_b
     * @return
     * @throws Exception
     */
    private String getSign(FundPayment_b fundPayment_b) throws Exception {
        DecimalFormat decimalFormat = new DecimalFormat("0.00#");
        String oppositeObjectName = fundPayment_b.getOppositeobjectname();
        String oppositeAccountName = fundPayment_b.getOppositeaccountname();
        String oriSum = decimalFormat.format(fundPayment_b.getOriSum());
        CtmJSONObject oriJson = new CtmJSONObject();
        oriJson.put("oppositeObjectName", oppositeObjectName);
        oriJson.put("oppositeAccountName", oppositeAccountName);
        oriJson.put(IBussinessConstant.ORI_SUM, oriSum);
        String signMsg = signatureService.chanPaySignMessage(oriJson.toString());
        return signMsg;
    }
}
