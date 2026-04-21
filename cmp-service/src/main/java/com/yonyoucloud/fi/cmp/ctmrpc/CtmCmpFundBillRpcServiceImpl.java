package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.service.itf.IProjectService;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BaseDto;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.CommonRuleUtils;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.ctm.stwb.reqvo.AgentPaymentReqVO;
import com.yonyoucloud.ctm.stwb.settlebench.SettleBench;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpFundBillRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.vo.FundCollectionVO;
import com.yonyoucloud.fi.cmp.vo.FundPaymentVO;
import com.yonyoucloud.fi.cmp.vo.common.CommonQueryResultVo;
import lombok.RequiredArgsConstructor;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_COLLECTION;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_PAYMENT;

/**
 * <h1>资金收付款单对外提供RPC接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-02-28 19:06
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CtmCmpFundBillRpcServiceImpl implements CtmCmpFundBillRpcService {

    private final FIBillService fiBillService;
    private final ISettleParamPubQueryService settleParamPubQueryService;
    private final IFundCommonService fundCommonService;
    private final CmCommonService commonService;

    private final ProcessService processService;

    private final YmsOidGenerator ymsOidGenerator;

    private final IProjectService projectService;

    private final BaseRefRpcService baseRefRpcService;

    private static final String SETTLEBENCHTOFUNDCOLLECTION = "settleBenchToFundCollection";

    private static final String SETTLEBENCHTOFUNDPAYMENT = "settleBenchToFundPayment";


    //    private final CmCommonService currencyQueryService;
    private void tradeTypeHandler(BizObject biz, String billnum, boolean isEntrust) {
        String formId = null;
        String tradetypeCode = null;
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            formId = CM_CMP_FUND_PAYMENT;
            tradetypeCode = "cmp_fund_payment_third_party_transfers";
            if (isEntrust) {
                tradetypeCode = "cmp_fund_payment_delegation";
            }
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            formId = CM_CMP_FUND_COLLECTION;
            tradetypeCode = "cmp_fund_collection_third_party_transfers";
            if (isEntrust) {
                tradetypeCode = "cmp_fundcollection_delegation";
            }
        }

        String billTypeId = null;
        try {
            List<Map<String, Object>> list = CmpCommonUtil.setTradeTypeByCode(formId);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error("查询资金付款单交易类型失败！请检查数据！");
                }
                billTypeId = MapUtils.getString(objectMap, ICmpConstant.PRIMARY_ID);
            }
            Map<String, Object> tradetypeMap = commonService.queryTransTypeById(billTypeId, "0", tradetypeCode);
            if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                biz.set(ICmpConstant.TRADE_TYPE, tradetypeMap.get(ICmpConstant.PRIMARY_ID));
            }
        } catch (Exception e) {
            log.error("未获取到默认的交易类型！, billTypeId = {}, e = {}", billTypeId, e.getMessage());
        }
    }

    private BizObject checkData(String data) throws Exception {
        BizObject biz = new BizObject();
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(data);
        biz.putAll(jsonObject);
        if (biz.get(ICmpConstant.VOUCHDATE) == null) {
            biz.set(ICmpConstant.VOUCHDATE, BillInfoUtils.getBusinessDate());
        }
        String accentity = biz.get(ICmpConstant.ACCENTITY);
        if (!ValueUtils.isNotEmptyObj(accentity)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100679"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050019", "资金组织不能为空") /* "资金组织不能为空" */);
        }
        Object currency = biz.get(ICmpConstant.CURRENCY);
        if (currency == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100680"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D0", "币种不能为空！") /* "币种不能为空！" */);
        }
        String natCurrency = biz.get(ICmpConstant.NATCURRENCY);
        if (natCurrency == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100681"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D1", "本币币种不能为空！") /* "本币币种不能为空！" */);
        }

        // 汇率类型
        if (!ValueUtils.isNotEmptyObj(biz.get("exchangeRateType"))) {
            ExchangeRateTypeVO defaultExchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(accentity, false);
            if (defaultExchangeRateType != null && defaultExchangeRateType.getId() != null) {
                biz.set("exchangeRateType", defaultExchangeRateType.getId());
                biz.set("exchangeRateType_digit", defaultExchangeRateType.getDigit());
            }
        }

        // 汇率（取汇率表中报价日期小于等于单据日期的值）
        if (!ValueUtils.isNotEmptyObj(biz.get(ICmpConstant.EXCHRATE))) {
            if (currency.equals(natCurrency)) {
                biz.set(ICmpConstant.EXCHRATE, 1);
            } else {
                CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(biz.get("currency"), natCurrency, biz.get("vouchdate"), (String) biz.get("exchangeRateType"));
                if (cmpExchangeRateVO == null || cmpExchangeRateVO.getExchangeRate() == BigDecimal.ZERO) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100682"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807CD", "币种找不到汇率") /* "币种找不到汇率" */);
                }
                biz.set(ICmpConstant.EXCHRATE, cmpExchangeRateVO.getExchangeRate().setScale(biz.get("exchangeRateType_digit"), RoundingMode.HALF_UP));
                biz.set(ICmpConstant.EXCHRATEOPS, cmpExchangeRateVO.getExchangeRateOps());
            }
        }
        return biz;
    }

    private static BillContext getBillContextByFundCollection() {
        BillContext billContext = new BillContext();
        billContext.setAction(ICmpConstant.SAVE);
        billContext.setbMain(true);
        billContext.setBillnum(IBillNumConstant.FUND_COLLECTION);
        billContext.setBilltype("Voucher");
        billContext.setMddBoId("ctm-cmp.cmp_fundcollection");
        billContext.setName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003E", "资金收款单") /* "资金收款单" */);
        billContext.setSupportBpm(true);
        billContext.setTenant(AppContext.getCurrentUser().getTenant());
        billContext.setFullname(FundCollection.ENTITY_NAME);
        billContext.setEntityCode(IBillNumConstant.FUND_COLLECTION);
        return billContext;
    }

    private static BillContext getBillContextByFundPayment() {
        return CmpCommonUtil.getBillContextByFundPayment();
    }

    @Override
    public Object paymentBillCreate(List<FundPaymentVO> fundPayments) throws Exception {
        if (!ValueUtils.isNotEmptyObj(fundPayments)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100683"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807CC", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }
        RuleExecuteResult ruleExecuteResult = null;
        List<FundPaymentVO> resultData = new ArrayList<>();
        Map<String, Object> resultResponse = new HashMap<>();
        for (FundPaymentVO paymentVO : fundPayments) {
            //1.校验数据
            checkBillData(paymentVO);
            //生成单据编码
            CmpCommonUtil.billCodeHandler(paymentVO, FundPayment.ENTITY_NAME, IBillNumConstant.FUND_PAYMENT);
            setFieldValue(paymentVO);//字段赋值
            //根据单据转换补充值
            appendDataFundPaymentVO(paymentVO);
            //2.生成付款单
            ruleExecuteResult = createPayBill(paymentVO);
            BizObject data = (BizObject) ruleExecuteResult.getData();
            FundPaymentVO fundPaymentVo = new FundPaymentVO();
            fundPaymentVo.putAll(data);
            resultData.add(fundPaymentVo);//返回结果
        }
        //3.回滚处理
        YtsContext.setYtsContext("SAVE_PAYMENT_BILL_CREATE", fundPayments);
        //4.处理返回结果
        resultResponse.put("code", 200);
        resultResponse.put("isSuccess", true);
        ruleExecuteResult.setData(resultData);
        ruleExecuteResult.setOutParams(resultResponse);
        return ruleExecuteResult;
    }

    private void appendDataFundPaymentVO(FundPaymentVO fundPaymentVO) throws Exception {
        //来源数据 元数据格式化
        CtmJSONObject ctmJSONObject = fundPaymentVO.getSourceData();
        if (ctmJSONObject == null) {
            return;
        }
        Object sourceData = ctmJSONObject.get("sourceData");
        BillContext context = new BillContext();
        context.setFullname(SettleBench.ENTITY_NAME);
        BaseDto baseDto = new BaseDto();
        baseDto.setData(sourceData);
        List<BizObject> bills = CommonRuleUtils.getBills(context, baseDto);
        SettleBench sourceSettleData = (SettleBench) bills.get(0);
        //调用单据转换
        List<String> ids = new ArrayList<>();
        ids.add(sourceSettleData.getId().toString());
        PushAndPullVO pullVO = new PushAndPullVO();
        pullVO.setCode(SETTLEBENCHTOFUNDPAYMENT);
        pullVO.setChildIds(ids);
        List<BizObject> transSourceData = new ArrayList<>();
        transSourceData.add(sourceSettleData);
        pullVO.setSourceData(transSourceData);
        pullVO.setData(transSourceData);
        //调用转单规则方法 - 返回转单成功的单据
        Map<String, Object> map = commonService.commonBillConvertRuleHandler(pullVO);
        if (map.get("tarList") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100684"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B9975205480013", "结算单转换生成资金付款单失败！") /* "结算单转换生成资金付款单失败！" */);
        }
        List<FundPayment> tarList = (List<FundPayment>) map.get("tarList");
        FundPayment targetData = tarList.get(0);
        List<FundPayment_b> fundPaymentBs = new ArrayList<>(fundPaymentVO.FundPayment_b());
        fundPaymentVO.putAll(targetData);
        fundPaymentVO.setFundPayment_b(fundPaymentBs);

        //处理转换后的数据，只保留 入账类型、项目、费用项目、特征
        if (targetData.getShort("entrytype") != null) {
            fundPaymentVO.setEntrytype(targetData.getShort("entrytype"));
        }
        if (targetData.get("autoSubmit") != null) {
            fundPaymentVO.set("autoSubmit", Integer.valueOf(targetData.get("autoSubmit").toString()));
        } else {
            fundPaymentVO.set("autoSubmit", 0);
        }
        if (targetData.getDate("vouchdate") != null) {
            fundPaymentVO.setVouchdate(targetData.getDate("vouchdate"));
        }
        if (targetData.getString("project") != null) {
            if (checkProject(targetData.getString("project"), fundPaymentVO.getAccentity())) {
                fundPaymentVO.setProject(targetData.getString("project"));
            } else {
                fundPaymentVO.setProject(null);
            }
        }
        if (targetData.getLong("expenseitem") != null) {
            if (checkExpenseitem(targetData.getString("expenseitem"), fundPaymentVO.getAccentity())) {
                fundPaymentVO.setExpenseitem(targetData.getLong("expenseitem"));
            } else {
                fundPaymentVO.setExpenseitem(null);
            }
        }
        if (targetData.get("characterDef") != null) {
            BizObject characterDef = targetData.get("characterDef");
            characterDef.setEntityStatus(EntityStatus.Insert);
            characterDef.setId(ymsOidGenerator.nextId());
            fundPaymentVO.put("characterDef", characterDef);
        }
        List<FundPayment_b> fundFundPaymentBList = targetData.FundPayment_b();
        for (FundPayment_b targetB : fundFundPaymentBList) {
            for (FundPayment_b fundCollectionB : fundPaymentVO.FundPayment_b()) {
                if (fundCollectionB.getString("settledId").equals(targetB.getString("settledId"))) {
                    if (targetB.getString("project") != null) {
                        if (checkProject(targetB.getString("project"), fundPaymentVO.getAccentity())) {
                            fundCollectionB.setProject(targetB.getString("project"));
                        } else {
                            fundCollectionB.setProject(null);
                        }
                    }
                    if (targetB.getLong("expenseitem") != null) {
                        if (checkExpenseitem(targetB.getString("expenseitem"), fundPaymentVO.getAccentity())) {
                            fundCollectionB.setExpenseitem(targetB.getLong("expenseitem"));
                        } else {
                            fundCollectionB.setExpenseitem(null);
                        }
                    }
                    if (targetB.get("characterDefb") != null) {
                        BizObject characterDef = targetB.get("characterDefb");
                        characterDef.setEntityStatus(EntityStatus.Insert);
                        characterDef.setId(ymsOidGenerator.nextId());
                        fundCollectionB.put("characterDefb", characterDef);
                    }
                }
            }
        }
    }

    @Override
    public Object rollBackPaymentBillCreate(List<FundPaymentVO> fundPayments) throws Exception {
        log.error("rollBackPaymentBillCreate, data={}", fundPayments.toString());
        List<FundPayment> payments = (List<FundPayment>) YtsContext.getYtsContext("SAVE_PAYMENT_BILL_CREATE");
        log.error("rollBackPaymentBillCreate, data={}", payments.toString());
        if (CollectionUtils.isNotEmpty(payments)) {
            payments.forEach(payment -> payment.setEntityStatus(EntityStatus.Delete));
            MetaDaoHelper.delete(FundPayment.ENTITY_NAME, payments);
        }
        CommonQueryResultVo result = new CommonQueryResultVo();
        result.setCode("200");
        result.setMessage("delete success");
        result.setData(payments);
        return result;
    }


    @Override
    public Object deletePaymentBill(List<String> settleIds) throws Exception {
        if (!ValueUtils.isNotEmptyObj(settleIds)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100683"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807CC", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }
        RuleExecuteResult ruleExecuteResult = new RuleExecuteResult();
        Map<String, Object> resultResponse = new HashMap<>();
        //查询
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (enableSimplify) {
            queryConditionGroup.appendCondition(QueryCondition.name("settledId").in(settleIds));
        } else {
            queryConditionGroup.appendCondition(QueryCondition.name("id").in(settleIds));
        }
        schema.addCondition(queryConditionGroup);
        List<FundPayment_b> fundPayment_bs = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, schema, null);
        if (ValueUtils.isNotEmptyObj(fundPayment_bs)) {
            Set<String> mainidIds = fundPayment_bs.stream().map(FundPayment_b::getMainid).collect(Collectors.toSet());
            List<BizObject> bizObjects = new ArrayList<>();
            for (String mainid : mainidIds) {
                BizObject bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, mainid, 2);
//                List<FundPayment_b> filterList = fundPayment_bs.stream().filter(item -> item.get("settlestatus").equals(7)).collect(Collectors.toList());
//                if(ValueUtils.isNotEmptyObj(filterList)){
//                    //删除主表
//                    MetaDaoHelper.delete(FundPayment.ENTITY_NAME,bizObject);
//                    //删除子表
//                    MetaDaoHelper.delete(FundPayment_b.ENTITY_NAME,filterList);
//                    bizObjects.add(bizObject);
//                }else{
                bizObject.setEntityStatus(EntityStatus.Delete);
                BillDataDto dataDto = new BillDataDto();
                CtmJSONObject json = new CtmJSONObject();
                json.put("id", mainid);
                json.put("isExecuteRule", false);//是否执行删除前规则标识
                dataDto.setData(json);
                dataDto.setBillnum(IBillNumConstant.FUND_PAYMENT);
                bizObjects.add(bizObject);
                ruleExecuteResult = fiBillService.delete(dataDto);
//                }
            }
            //回滚处理
            YtsContext.setYtsContext("DELETE_PAYMENT_BILL", bizObjects);
            resultResponse.put("isSuccess", true);
            ruleExecuteResult.setOutParams(resultResponse);
        }
        return ruleExecuteResult;
    }

    @Override
    public Object rollBackDeletePaymentBill(List<String> settleIds) throws Exception {
        List<BizObject> bizObjectList = (List<BizObject>) YtsContext.getYtsContext("DELETE_PAYMENT_BILL");
        if (CollectionUtils.isNotEmpty(bizObjectList)) {
            for (BizObject bizObject : bizObjectList) {
                bizObject.setEntityStatus(EntityStatus.Insert);
                bizObject.set("createTime", new Date());
                bizObject.set("createDate", new Date());
                bizObject.set("creator", InvocationInfoProxy.getUsername());
                bizObject.set("userId", InvocationInfoProxy.getUserid());
                CmpMetaDaoHelper.insert(FundPayment.ENTITY_NAME, bizObject);
                List<FundPayment_b> fundPayment_bs = bizObject.get("FundPayment_b");
                CmpMetaDaoHelper.insert(FundPayment_b.ENTITY_NAME, fundPayment_bs);
            }
            Map<String, Object> resultResponse = new HashMap<>();
            resultResponse.put("code", 200);
            resultResponse.put("isSuccess", true);
            return resultResponse;
        }
        return new RuleExecuteResult(999, "no data");
    }

    @Override
    public Map<String, Object> queryFundPaymentStatus(List<String> settleIds) throws Exception {
        if (!ValueUtils.isNotEmptyObj(settleIds)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100685"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00036", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }
        Map<String, Object> statusMap = new HashMap<>();
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);

        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (enableSimplify) {
            queryConditionGroup.appendCondition(QueryCondition.name("settledId").in(settleIds));
        } else {
            queryConditionGroup.appendCondition(QueryCondition.name("id").in(settleIds));
        }
        schema.addCondition(queryConditionGroup);
        List<FundPayment_b> list = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, schema, null);
        if (!ValueUtils.isNotEmptyObj(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100686"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003A", "单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */);
        }
        // 获取业务单据的结算成功的时间
        Optional<FundPayment_b> dataSettled = list.stream().filter(p -> p != null && p.getSettleSuccessTime() != null).max(Comparator.comparing(FundPayment_b::getSettleSuccessTime));
        if (dataSettled.isPresent()) {
            String settlesuccessdate = DateUtils.convertToStr(dataSettled.get().getSettleSuccessTime(), DateUtils.DATE_PATTERN);
            statusMap.put("settlesuccessdate", settlesuccessdate);
        }
        list.forEach(fundPayment -> {
            if (enableSimplify) {
                statusMap.put(fundPayment.getSettledId().toString(), fundPayment.get("settlestatus"));
            } else {
                statusMap.put(fundPayment.getId().toString(), fundPayment.get("settlestatus"));
            }
        });
        return statusMap;
    }

    @Override
    public List<AgentPaymentReqVO> queryFundPaymentStatusForSettle(List<String> settleIds) throws Exception {
        if (!ValueUtils.isNotEmptyObj(settleIds)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100685"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00036", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }
        List<AgentPaymentReqVO> agentPaymentReqVOlist = new ArrayList<>();
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);

        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (enableSimplify) {
            queryConditionGroup.appendCondition(QueryCondition.name("settledId").in(settleIds));
        } else {
            queryConditionGroup.appendCondition(QueryCondition.name("id").in(settleIds));
        }
        schema.addCondition(queryConditionGroup);
        List<FundPayment_b> list = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, schema, null);
        if (!ValueUtils.isNotEmptyObj(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100686"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003A", "单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */);
        }
        // 获取业务单据的结算成功的时间
        final String settlesuccessdate;
        Optional<FundPayment_b> dataSettled = list.stream()
                .filter(p -> p != null && p.getSettleSuccessTime() != null)
                .max(Comparator.comparing(FundPayment_b::getSettleSuccessTime));
        if (dataSettled.isPresent()) {
            settlesuccessdate = DateUtils.convertToStr(dataSettled.get().getSettleSuccessTime(), DateUtils.DATE_PATTERN);
        } else {
            settlesuccessdate = ""; // 默认空字符串
        }

        list.forEach(fundPayment -> {
            AgentPaymentReqVO agentPaymentReqVO = new AgentPaymentReqVO();
            if (enableSimplify) {
                agentPaymentReqVO.setSettleDetailId(fundPayment.getSettledId());
            } else {
                agentPaymentReqVO.setSettleDetailId(fundPayment.getId().toString());
            }
            agentPaymentReqVO.setStatementdetailstatus(Integer.parseInt(fundPayment.get("settlestatus").toString()));
            short settlestatus = fundPayment.get("settlestatus");
            if ((FundSettleStatus.SettlementSupplement.getValue() == settlestatus && !Objects.isNull(fundPayment.getSettleSuccessTime())) || FundSettleStatus.SettleSuccess.getValue() == settlestatus) {
                agentPaymentReqVO.setStatementdetailstatus(Integer.parseInt(String.valueOf(FundSettleStatus.SettleSuccess.getValue())));
            } else if (FundSettleStatus.SettleFailed.getValue() == settlestatus) {
                agentPaymentReqVO.setStatementdetailstatus(Integer.parseInt(String.valueOf(FundSettleStatus.SettleFailed.getValue())));
            } else if (FundSettleStatus.PartSuccess.getValue() == settlestatus) {
                agentPaymentReqVO.setStatementdetailstatus(7);
                agentPaymentReqVO.setSuccessTotalAmt(fundPayment.getSettlesuccessSum());
                agentPaymentReqVO.setFailTotalAmt(fundPayment.getSettleerrorSum());
            } else {
                agentPaymentReqVO.setStatementdetailstatus(Integer.parseInt(fundPayment.get("settlestatus").toString()));
            }
            agentPaymentReqVO.setSettlesuccessdate(settlesuccessdate);
            agentPaymentReqVOlist.add(agentPaymentReqVO);
        });
        return agentPaymentReqVOlist;
    }



    @Override
    public Object fundCollectBillCreate(List<FundCollectionVO> fundCollections) throws Exception {
        if (!ValueUtils.isNotEmptyObj(fundCollections)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100683"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807CC", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }
        RuleExecuteResult ruleExecuteResult = null;
        List<Object> resultData = new ArrayList<>();
        Map<String, Object> resultResponse = new HashMap<>();
        for (FundCollectionVO collectionVO : fundCollections) {
            //1.校验数据
            checkBillData(collectionVO);
            //字段赋值
            setFieldValue(collectionVO);
            //根据单据转换补充值
            appendDataFundCollectionVO(collectionVO);
            //生成单据编码
            CmpCommonUtil.billCodeHandler(collectionVO, FundCollection.ENTITY_NAME, IBillNumConstant.FUND_COLLECTION);
            //2.生成收款单
            ruleExecuteResult = createFundBill(collectionVO);
            resultData.add(ruleExecuteResult.getData());//返回结果
        }
        //3.回滚处理
        YtsContext.setYtsContext("SAVE_FUND_COLLECT_BILL_CREATE", fundCollections);
        //4.处理返回结果
        resultResponse.put("code", 200);
        resultResponse.put("isSuccess", true);
        ruleExecuteResult.setData(resultData);
        ruleExecuteResult.setOutParams(resultResponse);
        return ruleExecuteResult;
    }

    @Override
    public Object rollBackFundCollectBillCreate(List<FundCollectionVO> fundCollections) throws Exception {
        log.error("rollBackPaymentBillCreate, data={}", fundCollections.toString());
        List<FundCollection> fundCollectBills = (List<FundCollection>) YtsContext.getYtsContext("SAVE_FUND_COLLECT_BILL_CREATE");
        log.error("rollBackPaymentBillCreate, data={}", fundCollectBills.toString());
        if (CollectionUtils.isNotEmpty(fundCollectBills)) {
            fundCollectBills.forEach(fundCollection -> fundCollection.setEntityStatus(EntityStatus.Delete));
            MetaDaoHelper.delete(FundCollection.ENTITY_NAME, fundCollectBills);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("code", 200);
        result.put("message", "delete success");
        result.put("data", fundCollectBills);
        return result;
    }

    @Override
    public Object deleteFundCollectBill(List<String> settleIds) throws Exception {
        if (!ValueUtils.isNotEmptyObj(settleIds)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100683"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807CC", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }
        RuleExecuteResult ruleExecuteResult = new RuleExecuteResult();
        Map<String, Object> resultResponse = new HashMap<>();
        //查询
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (enableSimplify) {
            queryConditionGroup.appendCondition(QueryCondition.name("settledId").in(settleIds));
        } else {
            queryConditionGroup.appendCondition(QueryCondition.name("id").in(settleIds));
        }
        schema.addCondition(queryConditionGroup);
        List<FundCollection_b> fundCollection_bs = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, schema, null);
        if (ValueUtils.isNotEmptyObj(fundCollection_bs)) {
            Set<String> mainIds = fundCollection_bs.stream().map(FundCollection_b::getMainid).collect(Collectors.toSet());
            List<BizObject> bizObjects = new ArrayList<>();
            for (String mainid : mainIds) {
                BizObject bizObject = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, mainid, 2);
                bizObject.setEntityStatus(EntityStatus.Delete);
                BillDataDto dataDto = new BillDataDto();
                bizObject.set("isExecuteRule", false);//是否执行删除前规则标识
                dataDto.setData(CtmJSONObject.toJSONString(bizObject));
                dataDto.setBillnum(IBillNumConstant.FUND_COLLECTION);
                dataDto.setAction("Delete");
                bizObjects.add(bizObject);
                ruleExecuteResult = fiBillService.delete(dataDto);
            }
            YtsContext.setYtsContext("DELETE_FUND_COLLECT_BILL", bizObjects);
            resultResponse.put("isSuccess", true);
            ruleExecuteResult.setOutParams(resultResponse);
        }
        return ruleExecuteResult;
    }

    @Override
    public Object rollBackDeleteFundCollectBill(List<String> settleIds) throws Exception {
        List<BizObject> bizObjectList = (List<BizObject>) YtsContext.getYtsContext("DELETE_FUND_COLLECT_BILL");
        if (CollectionUtils.isNotEmpty(bizObjectList)) {
            for (BizObject bizObject : bizObjectList) {
                bizObject.setEntityStatus(EntityStatus.Insert);
                bizObject.set("createTime", new Date());
                bizObject.set("createDate", new Date());
                bizObject.set("creator", InvocationInfoProxy.getUsername());
                bizObject.set("userId", InvocationInfoProxy.getUserid());
                CmpMetaDaoHelper.insert(FundCollection.ENTITY_NAME, bizObject);
                List<FundCollection_b> fundCollection_bs = bizObject.get("FundCollection_b");
                CmpMetaDaoHelper.insert(FundCollection_b.ENTITY_NAME, fundCollection_bs);
            }
            Map<String, Object> resultResponse = new HashMap<>();
            resultResponse.put("code", 200);
            resultResponse.put("isSuccess", true);
            return resultResponse;
        }
        return new RuleExecuteResult(999, "no data");
    }

    @Override
    public Map<String, Object> queryFundCollectionStatus(List<String> settleIds) throws Exception {
        if (!ValueUtils.isNotEmptyObj(settleIds)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100685"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00036", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }
        Map<String, Object> statusMap = new HashMap<>();
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        //简强后传结算明细id，否则传资金收付款子表id
        if (enableSimplify) {
            queryConditionGroup.appendCondition(QueryCondition.name("settledId").in(settleIds));
        } else {
            queryConditionGroup.appendCondition(QueryCondition.name("id").in(settleIds));
        }
        schema.addCondition(queryConditionGroup);
        List<FundCollection_b> list = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, schema, null);
        if (!ValueUtils.isNotEmptyObj(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100686"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003A", "单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */);
        }
        // 获取业务单据的结算成功的时间
        Optional<FundCollection_b> dataSettled = list.stream().filter(p -> p != null && p.getSettleSuccessTime() != null).max(Comparator.comparing(FundCollection_b::getSettleSuccessTime));
        if (dataSettled.isPresent()) {
            String settlesuccessdate = DateUtils.convertToStr(dataSettled.get().getSettleSuccessTime(), DateUtils.DATE_PATTERN);
            statusMap.put("settlesuccessdate", settlesuccessdate);
        }
        list.forEach(fundCollection -> {
            //修改刷新结算状态传递的参数，只有结算止付和结算成功
            short settlestatus = fundCollection.get("settlestatus");

            if (FundSettleStatus.SettlementSupplement.getValue() == settlestatus || FundSettleStatus.SettleSuccess.getValue() == settlestatus) {
                if (enableSimplify) {
                    statusMap.put(fundCollection.getSettledId().toString(), FundSettleStatus.SettleSuccess.getValue());
                } else {
                    statusMap.put(fundCollection.getSettledId().toString(), FundSettleStatus.SettleSuccess.getValue());
                }
            } else if (FundSettleStatus.SettleFailed.getValue() == settlestatus) {
                if (enableSimplify) {
                    statusMap.put(fundCollection.getSettledId().toString(), FundSettleStatus.SettleFailed.getValue());
                } else {
                    statusMap.put(fundCollection.getSettledId().toString(), FundSettleStatus.SettleFailed.getValue());
                }
            } else {
                if (enableSimplify) {
                    statusMap.put(fundCollection.getSettledId().toString(), settlestatus);
                } else {
                    statusMap.put(fundCollection.getSettledId().toString(), settlestatus);
                }
            }
        });
        return statusMap;
    }

    @Override
    public List<AgentPaymentReqVO> queryFundCollectionStatusForSettle(List<String> settleIds) throws Exception {
        if (!ValueUtils.isNotEmptyObj(settleIds)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100685"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00036", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }

        List<AgentPaymentReqVO> agentPaymentReqVOlist = new ArrayList<>();
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        //简强后传结算明细id，否则传资金收付款子表id
        if (enableSimplify) {
            queryConditionGroup.appendCondition(QueryCondition.name("settledId").in(settleIds));
        } else {
            queryConditionGroup.appendCondition(QueryCondition.name("id").in(settleIds));
        }
        schema.addCondition(queryConditionGroup);
        List<FundCollection_b> list = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, schema, null);
        if (!ValueUtils.isNotEmptyObj(list)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100686"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003A", "单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */);
        }
        // 获取业务单据的结算成功的时间
        final String settlesuccessdate;
        Optional<FundCollection_b> dataSettled = list.stream()
                .filter(p -> p != null && p.getSettleSuccessTime() != null)
                .max(Comparator.comparing(FundCollection_b::getSettleSuccessTime));
        if (dataSettled.isPresent()) {
            settlesuccessdate = DateUtils.convertToStr(dataSettled.get().getSettleSuccessTime(), DateUtils.DATE_PATTERN);
        } else {
            settlesuccessdate = ""; // 默认空字符串
        }
        list.forEach(fundCollection -> {
            //修改刷新结算状态传递的参数，只有结算止付和结算成功
            AgentPaymentReqVO agentPaymentReqVO = new AgentPaymentReqVO();
            if (enableSimplify) {
                agentPaymentReqVO.setSettleDetailId(fundCollection.getSettledId());
            } else {
                agentPaymentReqVO.setSettleDetailId(fundCollection.getId().toString());
            }
            agentPaymentReqVO.setStatementdetailstatus(Integer.parseInt(fundCollection.get("settlestatus").toString()));
            short settlestatus = fundCollection.get("settlestatus");
            if ((FundSettleStatus.SettlementSupplement.getValue() == settlestatus && !Objects.isNull(fundCollection.getSettleSuccessTime())) || FundSettleStatus.SettleSuccess.getValue() == settlestatus) {
                agentPaymentReqVO.setStatementdetailstatus(Integer.parseInt(String.valueOf(FundSettleStatus.SettleSuccess.getValue())));
            } else if (FundSettleStatus.SettleFailed.getValue() == settlestatus) {
                agentPaymentReqVO.setStatementdetailstatus(Integer.parseInt(String.valueOf(FundSettleStatus.SettleFailed.getValue())));
            } else if (FundSettleStatus.PartSuccess.getValue() == settlestatus) {
                agentPaymentReqVO.setStatementdetailstatus(7);
                agentPaymentReqVO.setSuccessTotalAmt(fundCollection.getSettlesuccessSum());
                agentPaymentReqVO.setFailTotalAmt(fundCollection.getSettleerrorSum());
            } else {
                agentPaymentReqVO.setStatementdetailstatus(Integer.parseInt(fundCollection.get("settlestatus").toString()));
            }
            agentPaymentReqVO.setSettlesuccessdate(settlesuccessdate);
            agentPaymentReqVOlist.add(agentPaymentReqVO);
        });
        return agentPaymentReqVOlist;
    }

    /**
     * 校验数据
     *
     * @param fundPayment
     * @return
     */
    private void checkBillData(FundPayment fundPayment) {
        String accentity = fundPayment.getAccentity();
        if (!ValueUtils.isNotEmptyObj(accentity)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100679"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050019", "资金组织不能为空") /* "资金组织不能为空" */);
        }
        /**
         * srcitem 事项来源
         * billtype 事项类型
         * vouchdate 单据日期
         * entrustedUnit 委托单位
         * tradetype 交易类型
         * currency 币种
         * 汇率类型 exchangeRateType
         * 汇率 exchRate
         * 款项类型 quickType
         * 付款金额 oriSum
         * 付款对象 caobject
         * 收款单位名称 oppositeobjectname
         * 结算明细id settledId
         */
        String entrustedUnit = fundPayment.getEntrustedUnit();
        if (!ValueUtils.isNotEmptyObj(entrustedUnit)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100687"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003B", "委托单位不能为空") /* "委托单位不能为空" */);
        }
        String currency = fundPayment.getCurrency();
        if (!ValueUtils.isNotEmptyObj(currency)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100688"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003C", "币种不能为空") /* "币种不能为空" */);
        }
    }

    private void checkBillData(FundCollection fundCollection) {
        String accentity = fundCollection.getAccentity();
        if (!ValueUtils.isNotEmptyObj(accentity)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100679"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050019", "资金组织不能为空") /* "资金组织不能为空" */);
        }
        /**
         * srcitem 事项来源
         * billtype 事项类型
         * vouchdate 单据日期
         * entrustedUnit 委托单位
         * tradetype 交易类型
         * currency 币种
         * 汇率类型 exchangeRateType
         * 汇率 exchRate
         * 款项类型 quickType
         * 付款金额 oriSum
         * 付款对象 caobject
         * 收款单位名称 oppositeobjectname
         * 结算明细id settledId
         */
        String entrustedUnit = fundCollection.getEntrustedUnit();
        if (!ValueUtils.isNotEmptyObj(entrustedUnit)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100687"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003B", "委托单位不能为空") /* "委托单位不能为空" */);
        }
        String currency = fundCollection.getCurrency();
        if (!ValueUtils.isNotEmptyObj(currency)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100688"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003C", "币种不能为空") /* "币种不能为空" */);
        }
    }

    /**
     * 付款单字段赋值
     *
     * @param paymentVO
     * @throws Exception
     */
    private void setFieldValue(FundPaymentVO paymentVO) throws Exception {
        if (!ValueUtils.isNotEmptyObj(paymentVO.get(ICmpConstant.TRADE_TYPE))) {
            tradeTypeHandler(paymentVO, IBillNumConstant.FUND_PAYMENT, true);//交易类型
        }
        String accentity = paymentVO.getAccentity();
        paymentVO.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());//设置审批流状态
        paymentVO.setNatCurrency(AccentityUtil.getNatCurrencyIdByAccentityId(accentity));//本币币种
        paymentVO.setVouchdate(DateUtils.getCurrentDate("yyyy-MM-dd"));
        ExchangeRateTypeVO defaultExchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(accentity, false);
        ;
        paymentVO.set("srcitem", EventSource.StwbSettlement.getValue());
        paymentVO.set("billtype", EventType.Unified_Synergy.getValue());
        paymentVO.set("caobject", CaObject.Other.getValue());
        paymentVO.set("voucherstatus", VoucherStatus.Empty.getValue());
        paymentVO.setOrg(paymentVO.getAccentity());
        paymentVO.setExchangeRateType((String) defaultExchangeRateType.getId());
        CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(paymentVO.get("currency"), AccentityUtil.getNatCurrencyIdByAccentityId(accentity), paymentVO.get("vouchdate"), paymentVO.get("exchangeRateType"));
        if (cmpExchangeRateVO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100689"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00037", "币种找不到汇率") /* "币种找不到汇率" */);
        }
        paymentVO.setExchRate(cmpExchangeRateVO.getExchangeRate());
        paymentVO.set("exchangeRateOps", cmpExchangeRateVO.getExchangeRateOps());
        BigDecimal natSum = BigDecimal.ZERO;
        if (cmpExchangeRateVO.getExchangeRateOps() == 1) {
            natSum = BigDecimalUtils.safeMultiply(new BigDecimal(paymentVO.getExchRate().toString()), new BigDecimal(paymentVO.getOriSum().toString()));
        } else {
            natSum = BigDecimalUtils.safeDivide(new BigDecimal(paymentVO.getOriSum().toString()), new BigDecimal(paymentVO.getExchRate().toString()), ICmpConstant.CONSTANT_TEN);
        }
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(paymentVO.getNatCurrency());
        if(currencyTenantDTO != null){
            natSum = natSum.setScale(currencyTenantDTO.getMoneydigit(), BigDecimal.ROUND_HALF_UP);
        }
        paymentVO.set("natSum",natSum);
    }

    /**
     * 收款单字段赋值
     *
     * @param fundCollection
     * @throws Exception
     */
    private void setFieldValue(FundCollection fundCollection) throws Exception {
        if (!ValueUtils.isNotEmptyObj(fundCollection.get(ICmpConstant.TRADE_TYPE))) {
            tradeTypeHandler(fundCollection, IBillNumConstant.FUND_COLLECTION, true);//交易类型
        }
        String accentity = fundCollection.getAccentity();
        fundCollection.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());//设置审批流状态
        fundCollection.setNatCurrency(AccentityUtil.getNatCurrencyIdByAccentityId(accentity));//本币币种
        fundCollection.setVouchdate(DateUtils.getCurrentDate("yyyy-MM-dd"));
        //取汇率类型
        ExchangeRateTypeVO defaultExchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(accentity, false);
        ;
        fundCollection.set("srcitem", EventSource.StwbSettlement.getValue());
        fundCollection.set("billtype", EventType.Unified_Synergy.getValue());
        fundCollection.set("voucherstatus", VoucherStatus.Empty.getValue());
        fundCollection.setOrg(fundCollection.getAccentity());
        fundCollection.setExchangeRateType((String) defaultExchangeRateType.getId());
        CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(fundCollection.get("currency"), AccentityUtil.getNatCurrencyIdByAccentityId(accentity), fundCollection.get("vouchdate"), fundCollection.get("exchangeRateType"));
        if (cmpExchangeRateVO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100689"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00037", "币种找不到汇率") /* "币种找不到汇率" */);
        }
        fundCollection.setExchRate(cmpExchangeRateVO.getExchangeRate());
        fundCollection.set("exchangeRateOps", cmpExchangeRateVO.getExchangeRateOps());
        BigDecimal natSum = BigDecimal.ZERO;
        if (cmpExchangeRateVO.getExchangeRateOps() == 1) {
            natSum = BigDecimalUtils.safeMultiply(new BigDecimal(fundCollection.getExchRate().toString()), new BigDecimal(fundCollection.getOriSum().toString()));
        } else {
            natSum = BigDecimalUtils.safeDivide(new BigDecimal(fundCollection.getOriSum().toString()),new BigDecimal(fundCollection.getExchRate().toString()), ICmpConstant.CONSTANT_TEN);
        }
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(fundCollection.getNatCurrency());
        if(currencyTenantDTO != null){
            natSum = natSum.setScale(currencyTenantDTO.getMoneydigit(), BigDecimal.ROUND_HALF_UP);
        }
        fundCollection.set("natSum",natSum);
    }

    private void appendDataFundCollectionVO(FundCollectionVO fundCollection) throws Exception {
        //来源数据 元数据格式化
        CtmJSONObject ctmJSONObject = fundCollection.getSourceData();
        if (ctmJSONObject == null) {
            return;
        }
        Object sourceData = ctmJSONObject.get("sourceData");
        BillContext context = new BillContext();
        context.setFullname(SettleBench.ENTITY_NAME);
        BaseDto baseDto = new BaseDto();
        baseDto.setData(sourceData);
        List<BizObject> bills = CommonRuleUtils.getBills(context, baseDto);
        SettleBench sourceSettleData = (SettleBench) bills.get(0);
        //调用单据转换
        List<String> ids = new ArrayList<>();
        ids.add(sourceSettleData.getId().toString());
        PushAndPullVO pullVO = new PushAndPullVO();
        pullVO.setCode(SETTLEBENCHTOFUNDCOLLECTION);
        pullVO.setIsMainSelect(1);
        pullVO.setChildIds(ids);
        List<BizObject> transSourceData = new ArrayList<>();
        transSourceData.add(sourceSettleData);
        pullVO.setSourceData(transSourceData);
        pullVO.setData(transSourceData);
        //调用转单规则方法 - 返回转单成功的单据
        Map<String, Object> map = commonService.commonBillConvertRuleHandler(pullVO);
        if (map.get("tarList") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100690"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19B9975205480014", "结算单转换生成资金收款单失败！") /* "结算单转换生成资金收款单失败！" */);
        }
        List<FundCollection> tarList = (List<FundCollection>) map.get("tarList");
        FundCollection targetData = tarList.get(0);
        //处理转换后的数据，只保留 入账类型、项目、费用项目、特征
        if (targetData.getShort("entrytype") != null) {
            fundCollection.setEntrytype(targetData.getShort("entrytype"));
        }

        if (targetData.getDate("vouchdate") != null) {
            fundCollection.setVouchdate(targetData.getDate("vouchdate"));
        }

        if (targetData.get("autoSubmit") != null) {
            fundCollection.set("autoSubmit", Integer.valueOf(targetData.get("autoSubmit").toString()));
        } else {
            fundCollection.set("autoSubmit", 0);
        }
        if (targetData.getString("project") != null) {
            if (checkProject(targetData.getString("project"), fundCollection.getAccentity())) {
                fundCollection.setProject(targetData.getString("project"));
            } else {
                fundCollection.setProject(null);
            }
        }
        if (targetData.getLong("expenseitem") != null) {
            if (checkExpenseitem(targetData.getString("expenseitem"), fundCollection.getAccentity())) {
                fundCollection.setExpenseitem(targetData.getLong("expenseitem"));
            } else {
                fundCollection.setExpenseitem(null);
            }
        }
        if (targetData.get("characterDef") != null) {
            BizObject characterDef = targetData.get("characterDef");
            characterDef.setEntityStatus(EntityStatus.Insert);
            characterDef.setId(ymsOidGenerator.nextId());
            fundCollection.put("characterDef", characterDef);
        }
        List<FundCollection_b> fundCollectionBList = targetData.FundCollection_b();
        for (FundCollection_b targetB : fundCollectionBList) {
            for (FundCollection_b fundCollectionB : fundCollection.FundCollection_b()) {
                if (fundCollectionB.getString("settledId").equals(targetB.getString("settledId"))) {
                    if (targetB.getString("project") != null) {
                        if (checkProject(targetB.getString("project"), fundCollection.getAccentity())) {
                            fundCollectionB.setProject(targetB.getString("project"));
                        } else {
                            fundCollectionB.setProject(null);
                        }
                    }
                    if (targetB.getLong("expenseitem") != null) {
                        if (checkExpenseitem(targetB.getString("expenseitem"), fundCollection.getAccentity())) {
                            fundCollectionB.setExpenseitem(targetB.getLong("expenseitem"));
                        } else {
                            fundCollectionB.setExpenseitem(null);
                        }
                    }
                    if (targetB.get("characterDefb") != null) {
                        BizObject characterDef = targetB.get("characterDefb");
                        characterDef.setEntityStatus(EntityStatus.Insert);
                        characterDef.setId(ymsOidGenerator.nextId());
                        fundCollectionB.put("characterDefb", characterDef);
                    }
                }
            }
        }
    }


    /**
     * 生成付款单
     *
     * @param paymentVO
     * @return
     * @throws Exception
     */
    private RuleExecuteResult createPayBill(FundPaymentVO paymentVO) throws Exception {
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum(IBillNumConstant.FUND_PAYMENT);
        List<FundPayment_b> fundPayment_bs = paymentVO.FundPayment_b();
        if (!ValueUtils.isNotEmptyObj(fundPayment_bs)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100691"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0003D", "资金付款单子表数据不能为空") /* "资金付款单子表数据不能为空" */);
        }
        // 判断是否启用商业汇票
        boolean isEnabledBsd = false;
        try {
            CtmJSONObject enableBsdModule = fundCommonService.isEnableBsdModule(paymentVO.getAccentity());
            if (ValueUtils.isNotEmptyObj(enableBsdModule)) {
                isEnabledBsd = MapUtils.getBoolean(enableBsdModule, "isEnabled");
            }
        } catch (Exception e) {
            log.error("get BSD is enabled fail!, e = {}", e.getMessage());
        }
        paymentVO.setIsEnabledBsd(isEnabledBsd);
        paymentVO.setEntityStatus(EntityStatus.Insert);
        BigDecimal lineNo = new BigDecimal(10);
        for (FundPayment_b fundPayment_b : fundPayment_bs) {
            //档案取
            //若结算传了，用结算的，未传需要调接口
            if (null == fundPayment_b.getQuickType()) {
                Map<String, Object> condition = new HashMap<String, Object>();
                List<Map<String, Object>> quickType = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(String.valueOf(QuickType.sundry.getValue())));
                if (ValueUtils.isNotEmpty(quickType)) {
                    fundPayment_b.set("quickType", quickType.get(0).get("id"));//款项类型 杂项
                }
            }
            //传了就用传的，未传就用other
            if (null == fundPayment_b.get("caobject")) {
                fundPayment_b.set("caobject", CaObject.Other.getValue());//付款对象 其他
            } else if ("Employee".equals(fundPayment_b.get("caobject").toString())) {
                fundPayment_b.set("caobject", CaObject.Employee.getValue());
            } else if ("Customer".equals(fundPayment_b.get("caobject").toString())) {
                fundPayment_b.set("caobject", CaObject.Customer.getValue());
            } else if ("Supplier".equals(fundPayment_b.get("caobject").toString())) {
                fundPayment_b.set("caobject", CaObject.Supplier.getValue());
            } else if ("CapBizObj".equals(fundPayment_b.get("caobject").toString())) {
                fundPayment_b.set("caobject", CaObject.CapBizObj.getValue());
            } else if ("Other".equals(fundPayment_b.get("caobject").toString())) {
                fundPayment_b.set("caobject", CaObject.Other.getValue());
            } else if ("InnerUnit".equals(fundPayment_b.get("caobject").toString())) {
                fundPayment_b.set("caobject", CaObject.InnerUnit.getValue());
            }
            String accentity = paymentVO.getAccentity();
            fundPayment_b.setNatCurrency(AccentityUtil.getNatCurrencyIdByAccentityId(accentity));
            fundPayment_b.setEntityStatus(EntityStatus.Insert);
            BigDecimal natSum = BigDecimal.ZERO;
            if (paymentVO.getInteger("exchangeRateOps") == 1) {
                natSum = BigDecimalUtils.safeMultiply(new BigDecimal(paymentVO.getExchRate().toString()), new BigDecimal(fundPayment_b.getOriSum().toString()));
            } else {
                natSum = BigDecimalUtils.safeDivide(new BigDecimal(fundPayment_b.getOriSum().toString()),new BigDecimal(paymentVO.getExchRate().toString()), ICmpConstant.CONSTANT_TEN);
            }
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(fundPayment_b.getNatCurrency());
            if(currencyTenantDTO != null){
                natSum = natSum.setScale(currencyTenantDTO.getMoneydigit(), BigDecimal.ROUND_HALF_UP);
            }
            fundPayment_b.set("natSum",natSum);
            fundPayment_b.setExchangeRateType(paymentVO.getExchangeRateType());
            fundPayment_b.setExchRate(paymentVO.getExchRate());
            fundPayment_b.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
            fundPayment_b.setIsIncomeAndExpenditure(false);
            fundPayment_b.setWhetherRefundAndRepayment(0);
            fundPayment_b.set("lineno", lineNo);
            fundPayment_b.set("settleCurrency", paymentVO.getCurrency());
            fundPayment_b.set("swapOutExchangeRateType", paymentVO.getExchangeRateType());
            fundPayment_b.set("swapOutExchangeRateEstimate", new BigDecimal(1));
            fundPayment_b.set("swapOutAmountEstimate", fundPayment_b.getOriSum());
            lineNo = BigDecimalUtils.safeAdd(lineNo, (new BigDecimal(10)));
        }
        dataDto.setData(paymentVO);
        RuleExecuteResult ruleExecuteResult = new RuleExecuteResult();
        //自动提交
        Integer isAutoSubmit = paymentVO.getInteger("autoSubmit");
        if (isAutoSubmit != null && isAutoSubmit == 1) {
            BizObject fundPayment = new BizObject(paymentVO);
            ruleExecuteResult = fundPaymentExecuteRule(fundPayment);
        } else {
            ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
        }
        return ruleExecuteResult;
    }

    private RuleExecuteResult fundPaymentExecuteRule(BizObject fundPayment) {
        try {
            BillContext billContext = CmpCommonUtil.getBillContextByFundCollection();
            boolean isWfControlled = processService.bpmControl(billContext, fundPayment);
            fundPayment.put(ICmpConstant.IS_WFCONTROLLED, isWfControlled);
        } catch (Exception e) {
            fundPayment.put(ICmpConstant.IS_WFCONTROLLED, false);
        }
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum(IBillNumConstant.FUND_PAYMENT);
        dataDto.setData(CtmJSONObject.toJSONString(fundPayment));
        RuleExecuteResult result = commonService.doSaveAndSubmitAction(dataDto);
        // 注意这里判断result是否正常结束的状态，1:代表保存并提交成功；999：代表保存失败；910：代表保存成功但提交失败
        if (1 != result.getMsgCode()) {
            if (999 == result.getMsgCode()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100692"), result.getMessage());
            } else {
                return result;
            }
        } else {
            return result;
        }
    }

    /**
     * 生成收款单
     *
     * @param fundCollectionVO
     * @return
     * @throws Exception
     */
    private RuleExecuteResult createFundBill(FundCollectionVO fundCollectionVO) throws Exception {
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum(IBillNumConstant.FUND_COLLECTION);
        List<FundCollection_b> fundCollection_bs = fundCollectionVO.FundCollection_b();
        if (!ValueUtils.isNotEmptyObj(fundCollection_bs)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100693"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00039", "资金收款单子表数据不能为空") /* "资金收款单子表数据不能为空" */);
        }
        // 判断是否启用商业汇票
        boolean isEnabledBsd = false;
        try {
            CtmJSONObject enableBsdModule = fundCommonService.isEnableBsdModule(fundCollectionVO.getAccentity());
            if (ValueUtils.isNotEmptyObj(enableBsdModule)) {
                isEnabledBsd = MapUtils.getBoolean(enableBsdModule, "isEnabled");
            }
        } catch (Exception e) {
            log.error("get BSD is enabled fail!, e = {}", e.getMessage());
        }
        fundCollectionVO.setIsEnabledBsd(isEnabledBsd);
        fundCollectionVO.setEntityStatus(EntityStatus.Insert);
        BigDecimal lineNo = new BigDecimal(10);
        for (FundCollection_b fundCollection_b : fundCollection_bs) {
            //若结算传了，用结算的，未传需要调接口
            if (null == fundCollection_b.getQuickType()) {
                Map<String, Object> condition = new HashMap<String, Object>();
                List<Map<String, Object>> quickType = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(String.valueOf(QuickType.sundry.getValue())));
                if (ValueUtils.isNotEmpty(quickType)) {
                    fundCollection_b.set("quickType", quickType.get(0).get("id"));//款项类型 杂项
                }
            }
            //传了就用传的，未传就用other
            if (null == fundCollection_b.get("caobject")) {
                fundCollection_b.set("caobject", CaObject.Other.getValue());//付款对象 其他
            } else if ("Employee".equals(fundCollection_b.get("caobject").toString())) {
                fundCollection_b.set("caobject", CaObject.Employee.getValue());
            } else if ("Customer".equals(fundCollection_b.get("caobject").toString())) {
                fundCollection_b.set("caobject", CaObject.Customer.getValue());
            } else if ("Supplier".equals(fundCollection_b.get("caobject").toString())) {
                fundCollection_b.set("caobject", CaObject.Supplier.getValue());
            } else if ("CapBizObj".equals(fundCollection_b.get("caobject").toString())) {
                fundCollection_b.set("caobject", CaObject.CapBizObj.getValue());
            } else if ("Other".equals(fundCollection_b.get("caobject").toString())) {
                fundCollection_b.set("caobject", CaObject.Other.getValue());
            } else if ("InnerUnit".equals(fundCollection_b.get("caobject").toString())) {
                fundCollection_b.set("caobject", CaObject.InnerUnit.getValue());
            }
            String accentity = fundCollectionVO.getAccentity();
            fundCollection_b.setNatcurrency(AccentityUtil.getNatCurrencyIdByAccentityId(accentity));//本币币种
            fundCollection_b.setEntityStatus(EntityStatus.Insert);
            BigDecimal natSum = BigDecimal.ZERO;
            if (fundCollectionVO.getInteger("exchangeRateOps") == 1) {
                natSum = BigDecimalUtils.safeMultiply(new BigDecimal(fundCollectionVO.getExchRate().toString()), new BigDecimal(fundCollection_b.getOriSum().toString()));
            } else {
                natSum = BigDecimalUtils.safeDivide(new BigDecimal(fundCollectionVO.getOriSum().toString()), new BigDecimal(fundCollectionVO.getExchRate().toString()), ICmpConstant.CONSTANT_TEN);
            }
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(fundCollectionVO.getNatCurrency());
            if(currencyTenantDTO != null){
                natSum = natSum.setScale(currencyTenantDTO.getMoneydigit(), BigDecimal.ROUND_HALF_UP);
            }
            fundCollection_b.set("natSum",natSum);
            fundCollection_b.setExchangeRateType(fundCollectionVO.getExchangeRateType());
            fundCollection_b.setExchRate(fundCollectionVO.getExchRate());
            fundCollection_b.set("lineno", lineNo);
            fundCollection_b.setIsIncomeAndExpenditure(false);
            fundCollection_b.set("settleCurrency", fundCollectionVO.getCurrency());
            fundCollection_b.set("swapOutExchangeRateType", fundCollectionVO.getExchangeRateType());
            fundCollection_b.set("swapOutExchangeRateEstimate", new BigDecimal(1));
            fundCollection_b.set("swapOutAmountEstimate", fundCollection_b.getOriSum());
            lineNo = BigDecimalUtils.safeAdd(lineNo, (new BigDecimal(10)));
        }
        dataDto.setData(fundCollectionVO);
        RuleExecuteResult ruleExecuteResult = new RuleExecuteResult();
        //自动提交
        Integer isAutoSubmit = fundCollectionVO.getInteger("autoSubmit");
        if (isAutoSubmit != null && isAutoSubmit == 1) {
            BizObject fundCollection = new BizObject(fundCollectionVO);
            ruleExecuteResult = executeRule(fundCollection);
        } else {
            ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
        }
        BizObject data = (BizObject) ruleExecuteResult.getData();
        FundCollectionVO fundCollectionVo = new FundCollectionVO();
        fundCollectionVo.putAll(data);
        ruleExecuteResult.setData(fundCollectionVo);
        return ruleExecuteResult;
    }

    private RuleExecuteResult executeRule(BizObject fundCollection) {
        try {
            BillContext billContext = CmpCommonUtil.getBillContextByFundCollection();
            boolean isWfControlled = processService.bpmControl(billContext, fundCollection);
            fundCollection.put(ICmpConstant.IS_WFCONTROLLED, isWfControlled);
        } catch (Exception e) {
            fundCollection.put(ICmpConstant.IS_WFCONTROLLED, false);
        }
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum(IBillNumConstant.FUND_COLLECTION);
        dataDto.setData(CtmJSONObject.toJSONString(fundCollection));
        RuleExecuteResult result = commonService.doSaveAndSubmitAction(dataDto);
        // 注意这里判断result是否正常结束的状态，1:代表保存并提交成功；999：代表保存失败；910：代表保存成功但提交失败
        if (1 != result.getMsgCode()) {
            if (999 == result.getMsgCode()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100694"), result.getMessage());
            } else {
                return result;
            }
        } else {
            return result;
        }
    }

    private boolean checkProject(String projectId, String currAccentity) throws Exception {
        Map<String, Set<String>> orgRangeMap = projectService.queryOrgRangeSByProjectId(Arrays.asList(new String[]{projectId}));
        if (orgRangeMap == null || orgRangeMap.isEmpty()) {
            return false;
        }
        Set<String> orgRange = orgRangeMap.get(projectId);
        if (orgRange == null || orgRange.isEmpty()) {
            return false;
        }
        return orgRange.contains(currAccentity) || orgRange.contains(IStwbConstantForCmp.GLOBAL_ACCENTITY);
    }

    private boolean checkExpenseitem(String expenseitem, String accentity) throws Exception {
        List<Map<String, Object>> expenseItemList = QueryBaseDocUtils.queryExpenseItemById(expenseitem);
        if (!org.apache.commons.collections4.CollectionUtils.isEmpty(expenseItemList)) {
            Map<String, Object> expenseitemMap = expenseItemList.get(0);
            if (expenseitemMap == null || expenseitemMap.isEmpty() || expenseitemMap.get("propertybusiness") == null) {
                return false;
            }
            //组织权限校验
            String orgfield = AuthUtil.getBDOrgField("bd.expenseitem.ExpenseItem");
            Object value = expenseitemMap.get(orgfield);
            if (!(accentity.equals(value) || IStwbConstantForCmp.GLOBAL_ACCENTITY.equals(value.toString()))) {
                return false;
            }
            //判断是否勾选财资服务
            if (expenseitemMap.get("propertybusiness") instanceof Short && (Short) expenseitemMap.get("propertybusiness") == 1) {
                return true;
            } else if (expenseitemMap.get("propertybusiness") instanceof Boolean && (Boolean) expenseitemMap.get("propertybusiness") == true) {
                return true;
            } else if (expenseitemMap.get("propertybusiness") instanceof Integer && (Integer) expenseitemMap.get("propertybusiness") == 1) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
