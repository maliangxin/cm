package com.yonyoucloud.fi.cmp.fundpayment.service;

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
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.cmp.autoorderrule.Autoorderrule_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
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
import java.text.DecimalFormat;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_PAYMENT;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.SELECT_TOTAL_PARAM;

/**
 * 到账认领V2 -- 自动生单 -- 银行对账单生成资金付款单
 *
 * @author msc
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class BankreconciliationGenerateFundpaymentService {

    private final String FUNDPAYMENTRULE = "banktofundpayment"; //对账单转资金付款单转单规则Code
    private final String MAKETYPE = "push"; //转单规则makeType

    @Autowired
    MakeBillRuleClientService makeBillRuleClientService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private CtmSignatureService signatureService;
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
     * 1，自动生单 -- 银行对账单生成资金付款单
     * 2，生单成功后，回调关联接口 - 回写生单关联信息
     *
     * @param bankReconciliations
     * @param autoorderrule_b
     */
    public void bankreconciliationGenerateFundpayment(List<BankReconciliation> bankReconciliations, Autoorderrule_b autoorderrule_b) throws Exception {
        //设置款项类型 -- 交易类型
        autoorderrule_b.setTradetype(autoorderrule_b.getTradetype() == null ? getDefultTradeType() : autoorderrule_b.getTradetype());
        autoorderrule_b.setQuickType(autoorderrule_b.getQuickType() == null ? getDefaultQuickType() : autoorderrule_b.getQuickType());
        List<String> lockFailBankSeqNoList = new ArrayList<>();
        /**
         * 1，转换数据格式为可保存付款单实体
         * 2，拼接回写关联信息实体
         */
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(bankReconciliation.getId().toString());
            try {
                if (ymsLock == null) {
                    lockFailBankSeqNoList.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400792", "加锁失败：银行交易流水号：") /* "加锁失败：银行交易流水号：" */ + bankReconciliation.getBank_seq_no());
                    continue;
                }
                //需要每一条都要调用关联接口
                //需要回写关联信息实体
                List<CorrDataEntityParam> corrDataEntities = new ArrayList<>();
                Map<String, Object> map = generateFundpayment(bankReconciliation, autoorderrule_b);
                List<Map<String, Object>> tarList = (List) map.get("tarList");//生成资金付款单数据列表
                if (tarList.isEmpty()) continue;
                FundPayment fundPayment = getFundPayment(tarList.get(0), autoorderrule_b);
                fundPayment.put("bizObjType", "cmp.fundpayment.FundPayment");
                if (fundPayment.getInteger("autoSubmit") != null && 1 == fundPayment.getInteger("autoSubmit")) {
                    //自动提交是走后台规则，会有关联回写逻辑，不需要调用回写接口
                    fundPayment.setConfirmstatus(Relationstatus.Confirmed.getValue());
                    //需要标记是自动生单，否则生单确认页面查询不出来
                    fundPayment.put("isAuto", true);
                    List<FundPayment_b> fundPayment_bs = fundPayment.FundPayment_b();
                    for (FundPayment_b fundPayment_b : fundPayment_bs) {
                        //财资统一对账码赋值
                        fundPayment_b.setBizobjtype("cmp.fundpayment.FundPayment_b");
                    }
                    executeRule(fundPayment);
                } else {
                    corrDataEntities.add(getCorrDataEntity(fundPayment, fundPayment.FundPayment_b().get(0)));
                    //关联关系回写，解析财资统一对账码
                    String result = ctmcmpReWriteBusRpcService.batchReWriteBankRecilicationForRpc(corrDataEntities);//回写关联信息
                    CtmJSONObject ctmJSONObject = CtmJSONArray.parseArray(result).getJSONObject(0);
                    String smartcheckno = ctmJSONObject.getString("smartcheckno");
                    //资金付款单入库
                    List<FundPayment_b> fundPayment_bs = fundPayment.FundPayment_b();
                    for (FundPayment_b fundPayment_b : fundPayment_bs) {
                        //财资统一对账码赋值
                        fundPayment_b.setSmartcheckno(smartcheckno);
                        fundPayment_b.setBizobjtype("cmp.fundpayment.FundPayment_b");
                    }
                    //如果关联成功则资金收入库
                    if (result != null && result.contains("iscorrsuccess")) {
                        fundPayment.setCreateDate(new Date());
                        fundPayment.setCreateTime(new Date());
                        fundPayment.setCreator(InvocationInfoProxy.getUsername());
                        //结算简强字段赋值
                        fundCommonService.setSimpleSettleValue(IBillNumConstant.FUND_PAYMENT, fundPayment);
                        CmpMetaDaoHelper.insert(FundPayment.ENTITY_NAME, fundPayment);//保存资金付款单主表信息
                    }
                }
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102232"),e.getMessage());

            } finally {
                if (ymsLock != null) {
                    JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(lockFailBankSeqNoList)) {
            log.error("资金付款单-账户交易流水自动生单，获取锁失败信息：" + JSON.toJSONString(lockFailBankSeqNoList));
        }
    }

    private RuleExecuteResult executeRule(BizObject fundPayment) {
        //前边已判断是否开启审批流，不用重复判断
//        try {
//            BillContext billContext = CmpCommonUtil.getBillContextByFundPayment();
//            boolean isWfControlled = processService.bpmControl(billContext, fundPayment);
//            fundPayment.put(ICmpConstant.IS_WFCONTROLLED, isWfControlled);
//        } catch (Exception e) {
//            fundPayment.put(ICmpConstant.IS_WFCONTROLLED, false);
//        }
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum(IBillNumConstant.FUND_PAYMENT);
        dataDto.setData(CtmJSONObject.toJSONString(fundPayment));
        RuleExecuteResult result = cmCommonService.doSaveAndSubmitAction(dataDto);
        // 注意这里判断result是否正常结束的状态，1:代表保存并提交成功；999：代表保存失败；910：代表保存成功但提交失败
        if (1 != result.getMsgCode()) {
            if (999 == result.getMsgCode()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101766"), result.getMessage());
            } else {
                return result;
            }
        } else {
            return result;
        }
    }

    /**
     * 自动生单确认 -- 确认生成资金付款单
     *
     * @param id -- 参数为对账单关联表id 防止String转Long问题 用Object接收
     * @return
     */
    public void confirmGenerateFundpayment(Object id) throws Exception {
        Long autorrid = Long.valueOf(id.toString());//关联表id
        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
        /**
         * 获取需要确认的业务单据实体
         */
        List<Map<String, Object>> bankReconciliationbusrelation_bs = MetaDaoHelper.queryById(BankReconciliationbusrelation_b.ENTITY_NAME, SELECT_TOTAL_PARAM, autorrid);
        if (bankReconciliationbusrelation_bs.isEmpty())
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101767"), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800FB", "需确认数据为空！") /* "需确认数据为空！" */));
        bankReconciliationbusrelation_b.init(bankReconciliationbusrelation_bs.get(0));
        Long fundPaymentId = bankReconciliationbusrelation_b.getSrcbillid();//获取付款单主表id
        if(fundPaymentId == null){
            return;
        }
        List<Map<String, Object>> fundPayments = MetaDaoHelper.queryById(FundPayment.ENTITY_NAME, SELECT_TOTAL_PARAM, fundPaymentId.toString());
        /**
         * 进行确认操作
         */
        if (!fundPayments.isEmpty()) {//需要确认的付款单不为空
            FundPayment fundPayment = new FundPayment();
            fundPayment.init(fundPayments.get(0));
//            if (fundPayment.getConfirmstatus() != Relationstatus.Confirm.getValue())
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101768"), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0002C", "数据非待确认状态，不允许确认!") /* "数据非待确认状态，不允许确认!" */));
            fundPayment.setConfirmstatus(Relationstatus.Confirmed.getValue());//确认状态修改为已确认.
            fundPayment.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundPayment.ENTITY_NAME, fundPayment);
            //202409修改，自动生单时财资统一对账码已赋值，不需要重新赋值
//            //银行对账单
//            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME,
//                    bankReconciliationbusrelation_b.getBankreconciliation());
//            //智能对账：添加勾兑号
//            String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
//            if (bankReconciliation != null) {
//                bankReconciliation.setEntityStatus(EntityStatus.Update);
//                if (StringUtils.isNotEmpty(bankReconciliation.getSmartcheckno())) {
//                    smartcheckno = bankReconciliation.getSmartcheckno();
//                }
//                bankReconciliation.setSmartcheckno(smartcheckno);
//                CommonSaveUtils.updateBankReconciliation(bankReconciliation);
//            }
//            //更新资金付款单明细勾兑号
//            List<FundPayment_b> fundPaymentList = new ArrayList<FundPayment_b>();
//            QuerySchema querySchema = QuerySchema.create().addSelect("*");
//            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(fundPayment.getId()));
//            querySchema.addCondition(group);
//            fundPaymentList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
//            for (FundPayment_b fundPayment_b : fundPaymentList) {
//                fundPayment_b.setEntityStatus(EntityStatus.Update);
//                fundPayment_b.setSmartcheckno(smartcheckno);
//                MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPayment_b);
//            }
        }
    }

    /**
     * 自动生单确认 -- 拒绝生成资金付款单
     *
     * @param id -- 参数为对账单关联表id 防止String转Long问题 用Object接收
     * @return
     */
    public void refuseGenerateFundPayment(Object id) throws Exception {
        Long autorrid = Long.valueOf(id.toString());//关联表id
        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
        /**
         * 获取需要确认的业务单据实体
         */
        List<Map<String, Object>> bankReconciliationbusrelation_bs = MetaDaoHelper.queryById(BankReconciliationbusrelation_b.ENTITY_NAME, SELECT_TOTAL_PARAM, autorrid);
        if (bankReconciliationbusrelation_bs.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101769"), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0002B", "需拒绝数据为空！") /* "需拒绝数据为空！" */));
        }
        bankReconciliationbusrelation_b.init(bankReconciliationbusrelation_bs.get(0));
        Long fundPaymentId = bankReconciliationbusrelation_b.getSrcbillid();//获取付款单主表id
        if(fundPaymentId == null){
            return;
        }
        List<Map<String, Object>> fundPayments = MetaDaoHelper.queryById(FundPayment.ENTITY_NAME, SELECT_TOTAL_PARAM, fundPaymentId.toString());
        /**
         * 进行删除操作
         */
        if (!fundPayments.isEmpty()) {//需要拒绝的付款单不为空
            FundPayment fundPayment = new FundPayment();
            fundPayment.init(fundPayments.get(0));
//            if (fundPayment.getConfirmstatus() != Relationstatus.Confirm.getValue()) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101770"), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0002D", "数据非待确认状态，不允许拒绝!") /* "数据非待确认状态，不允许拒绝!" */));
//            }
            fundPayment.setEntityStatus(EntityStatus.Delete);
            MetaDaoHelper.delete(FundPayment.ENTITY_NAME, fundPayment);
        }
    }

    /**
     * 生成资金付款单单据
     * 返回已生成的单据用于回写信息
     *
     * @param bankReconciliation
     * @param autoorderrule_b
     * @return
     */
    public Map<String, Object> generateFundpayment(BankReconciliation bankReconciliation, Autoorderrule_b autoorderrule_b) throws Exception {
        PushAndPullVO pullVO = forPushAndPullVOValue(bankReconciliation, autoorderrule_b);//获取转单实体
        Map<String, Object> map = makeBillRuleClientService.getTargetList(pullVO, MAKETYPE);//调用转单规则方法 - 返回转单成功的单据
        return map;
    }

    /**
     * 获取转单规则实体
     *
     * @param bankReconciliation
     * @param autoorderrule_b
     * @return
     */
    private PushAndPullVO forPushAndPullVOValue(BankReconciliation bankReconciliation, Autoorderrule_b autoorderrule_b) {
        List<BizObject> bizObjects = new ArrayList<BizObject>();
        List<String> ids = new ArrayList<String>();
        /**
         * 将对账单实体转成BizObject
         * 添加参数 -- 款项类型&交易类型
         * 将单据id放入ids
         */

        BizObject data = bankReconciliation;
        data.set("quickType", autoorderrule_b.getQuickType());
        data.set("tradetype", autoorderrule_b.getTradetype());
        data.put("isAutoPull", "auto");//BankSetEntrytypePullBeforeRule类处理入账类型自动生单无需处理，在此做标记用于规则类直接return
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
     * @param autoorderrule_b
     * @return
     */
    private FundPayment getFundPayment(Map<String, Object> obj, Autoorderrule_b autoorderrule_b) throws Exception {
        FundPayment fundPayment = new FundPayment();
        fundPayment.init(obj);
        fundPayment.setFundPayment_b(getFundPayment_b(obj, autoorderrule_b));
        //fundPayment.FundPayment_b().get(0).setMainid(String.valueOf(fundPayment.getId()));
        fundPayment.setEntityStatus(EntityStatus.Insert);
        fundPayment.setConfirmstatus(Relationstatus.Confirm.getValue());//生成单据为待确认状态
        fundPayment.setNatSum(fundPayment.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));//规范精度
        fundPayment.setOriSum(fundPayment.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundPayment.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        fundPayment.setVoucherstatus(VoucherStatus.Empty);
        fundPayment.setId(ymsOidGenerator.nextId());
        fundPayment.FundPayment_b().get(0).setMainid(String.valueOf((long)fundPayment.getId()));
        fundPayment.setTradetype(autoorderrule_b.getTradetype());
        fundPayment.setCode(getBillCode(fundPayment));
//        fundPayment.setEntrytype(ValueUtils.isNotEmptyObj(fundPayment.get("entrytype"))
//                ? fundPayment.get("entrytype") : EntryType.Normal_Entry.getValue());
        if (!ValueUtils.isNotEmptyObj(fundPayment.getSettleflag())) {
            fundPayment.setSettleflag(Short.parseShort("1"));
        }
        if (!ValueUtils.isNotEmptyObj(fundPayment.getEntrytype())) {
            fundPayment.setEntrytype(EntryType.Normal_Entry.getValue());
        }
        // 特征处理 否则保存报错
        BizObject characterDef = fundPayment.getCharacterDef();
        if (characterDef != null) {
            characterDef.put("id", ymsOidGenerator.nextId());
        }
        BizObject characterDefb = fundPayment.FundPayment_b().get(0).getCharacterDefb();
        if (characterDefb != null) {
            characterDefb.put("id", ymsOidGenerator.nextId());
        }
        if (null == fundPayment.getIsWfControlled()) {
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
     * @param autoorderrule_b
     * @return
     */
    private List<FundPayment_b> getFundPayment_b(Map<String, Object> obj, Autoorderrule_b autoorderrule_b) throws Exception {
        List<Map<String, Object>> fundPayment_bs = (List<Map<String, Object>>) obj.get("FundPayment_b");
        List<FundPayment_b> fundPayment_bs1 = new ArrayList<FundPayment_b>();
        FundPayment_b fundPayment_b = new FundPayment_b();
        fundPayment_b.init(fundPayment_bs.get(0));
        fundPayment_b.setAssociationStatus(AssociationStatus.Associated.getValue());
        fundPayment_b.setNatSum(fundPayment_b.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundPayment_b.setOriSum(fundPayment_b.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
//        fundPayment_b.setSettlesuccessSum(fundPayment_b.getNatSum().setScale(8,BigDecimal.ROUND_DOWN));
        fundPayment_b.setBankReconciliationId(fundPayment_b.get("bankReconciliationId").toString());
        fundPayment_b.setOppositeaccountname(fundPayment_b.getOppositeaccountname() == null ? "" : fundPayment_b.getOppositeaccountname());
        fundPayment_b.setOppositeobjectname(fundPayment_b.getOppositeobjectname() == null ? "" : fundPayment_b.getOppositeobjectname());
        fundPayment_b.setSignature(getSign(fundPayment_b));
        fundPayment_b.setQuickType(autoorderrule_b.getQuickType());
        fundPayment_b.setId(ymsOidGenerator.nextId());
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
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.FUND_PAYMENT), IBillNumConstant.FUND_PAYMENT, fundPayment.getYtenantId(), null, null, new BillCodeObj[]{new BillCodeObj(fundPayment)});
        String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
        if (codelist != null && codelist.length > 0) {
            return codelist[0];
        } else {
            return null;
        }
    }

    /**
     * 获取默认的交易类型
     *
     * @return
     */
    private String getDefultTradeType() {
        String billTypeId = null;
        try {
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(CM_CMP_FUND_PAYMENT));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error("查询资金付款单交易类型失败！请检查数据！");
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101771"),"查询资金付款单交易类型失败！请检查数据。");
                }
                billTypeId = MapUtils.getString(objectMap, "id");
            }
            Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById(billTypeId, "1", null);
            if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                return tradetypeMap.get("id").toString();
            }
        } catch (Exception e) {
            log.error("未获取到默认的交易类型！, billTypeId = {}, e = {}", billTypeId, e.getMessage());
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101772"),"未获取到默认的交易类型！");
        }
        return null;
    }

    /**
     * 获取默认款项类型
     *
     * @return
     */
    private Long getDefaultQuickType() {
        try {
            short quickCode = QuickType.sundry.getValue();
            HashMap<String, Object> quickCodeMap = SqlHelper.selectOne(QUICK_TYPE_MAPPER + ".getFundPaymentQuickTypeCode", AppContext.getTenantId());
            if (null != quickCodeMap && null != quickCodeMap.get("cDefaultValue") && !"".equals(quickCode)) {
                quickCode = Short.parseShort(String.valueOf(quickCodeMap.get("cDefaultValue")));
            }
            List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(quickCode));
            if (!quickTypeMap.isEmpty()) {
                return Long.valueOf(quickTypeMap.get(0).get("id").toString());
            }
        } catch (Exception e) {
            log.error("获取默认款项类型错误::" + e.getMessage());
        }
        return null;
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
        String signMsg = signatureService.iTrusSignMessage(oriJson.toString());
        return signMsg;
    }
}
