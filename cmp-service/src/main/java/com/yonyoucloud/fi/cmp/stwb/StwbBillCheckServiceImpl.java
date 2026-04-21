package com.yonyoucloud.fi.cmp.stwb;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JsonUtils;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.risk.bs.riskengine.RiskExecuteEngine;
import com.yonyou.yonbip.ctm.risk.util.RiskEnvironment;
import com.yonyou.yonbip.ctm.risk.vo.riskengine.RiskExecuteResult;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.reqvo.SettleCheckReqVO;
import com.yonyoucloud.ctm.stwb.reqvo.SettleReqVO;
import com.yonyoucloud.ctm.stwb.respvo.SettleCheckRespVO;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.cmpentity.BillCheck;
import com.yonyoucloud.fi.cmp.cmpentity.BillCheckObject;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.fa.vo.riskstrategy.RiskStrategy;
import com.yonyoucloud.fi.fa.vo.riskstrategy.RiskStrategyEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 结算检查，风险参数校验
 * @author: wanxbo@yonyou.com
 * @date: 2023/5/16 10:48
 */
@Service
@Slf4j
public class StwbBillCheckServiceImpl implements StwbBillCheckService{

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Resource
    private YmsOidGenerator ymsOidGenerator;

    @Resource
    private CmCommonService cmCommonService;

    @Resource
    private EnterpriseBankQueryService enterpriseBankQueryService;

    /**
     * 风险校验
     * @param params
     * @return -1无需校验 0经校验不存在风险 1存在风险 999校验接口错误
     * @throws Exception
     */
    @Override
    public CtmJSONObject billCheck(CtmJSONObject params) throws Exception {
        CtmJSONObject result = new CtmJSONObject();

        //是否开启风险校验分支，开启则校验走风险校验模块
        boolean isEnableRiskCheck = RiskEnvironment.isEnable();

        String billnum = params.getString("billnum");
        //判断结算检查的节点
        String action = params.getString("action");

        //行数据
        CtmJSONObject data = params.getJSONObject("data");
        //获取会计主体
        String accentity = data.getString(IBussinessConstant.ACCENTITY);
        //根据会计主体查询配置的现金参数-结算检查参数
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity));
        querySchema1.addCondition(group1);
        List<AutoConfig> configList= MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME,querySchema1,null);
        if ( !isEnableRiskCheck && ( configList == null || configList.size() == 0 )){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }

        AutoConfig autoConfig = CollectionUtils.isEmpty(configList)? new AutoConfig() : configList.get(0);
        //未开启结算检查或者结算检查对象，结算检查项为空，直接跳过
        if (!isEnableRiskCheck  && (!autoConfig.getBillCheckFlag() || StringUtils.isEmpty(autoConfig.getBillCheckObject())
                || StringUtils.isEmpty(autoConfig.getBillCheck()) )){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }
        //结算检查对象集合
        List<String> billCheckObjects = autoConfig.getBillCheckObject() == null? new ArrayList<>() : Arrays.asList(autoConfig.getBillCheckObject().split(","));
        //结算检查项集合
        List<String> billCheckList = autoConfig.getBillCheck() ==null ? new ArrayList<>() : Arrays.asList(autoConfig.getBillCheck().split(","));
        //json转map
        Map<String,Object> map = new HashMap<>();
        for (Object set: data.entrySet()){
            map.put(((Map.Entry)set).getKey().toString(),((Map.Entry)set).getValue());
        }
        //风险管理参数
        //资金组织
        String pk_entity;
        //单据类型编码
        String billTypeCode;
        //风险操作编码
        String actionCode;

        //资金付款单
        switch (billnum) {
            case "cmp_fundpayment": {
                if (!isEnableRiskCheck && "save".equals(action) && !billCheckObjects.contains(BillCheckObject.paybillSave.getValue() + "")) { //不包含资金付款单：保存，则跳过
                    //-1,未配置结算检查，跳过
                    result.put("checkFlag", "-1");
                    result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
                    return result;
                }
                //卡片和页面提交区分
                if (!isEnableRiskCheck && ("listsubmit".equals(action) || "submit".equals(action)) && !billCheckObjects.contains(BillCheckObject.paybillSubmit.getValue() + "")) { //不包含资金付款单：提交，则跳过
                    //-1,未配置结算检查，跳过
                    result.put("checkFlag", "-1");
                    result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
                    return result;
                }


                FundPayment fundPayment = new FundPayment();
                if ("listsubmit".equals(action)) {
                    fundPayment = getFundPaymentById(params.get("id"));
                } else {
                    if ("submit".equals(action)) {
                        fundPayment = getFundPaymentById(map.get("id"));
                    } else {
                        fundPayment.init(map);
                    }
                }
                // 克隆 FundPayment 对象
                FundPayment fundPaymentNew = (FundPayment) fundPayment.clone();
                // 获取 FundPayment_b 列表
                List<FundPayment_b> bList = fundPayment.FundPayment_b();
                // 使用 Stream 对列表进行过滤
                List<FundPayment_b> fundPaymentBLists = bList.stream()
                        .filter(b -> b != null && !"Delete".equals(b.get("_status"))) // 过滤掉 _status 为 "Delete" 的项
                        .filter(b -> FundSettleStatus.SettlementSupplement.getValue() != Short.parseShort("" + b.get("settlestatus"))) // 过滤掉 已结算补单的数据
                        .filter(b -> FundSettleStatus.Refund.getValue() != Short.parseShort("" + b.get("settlestatus"))) // 过滤掉 退票的数据
                        .collect(Collectors.toList()); // 收集成新的 List
                // 判断结算状态,全都是已结算补单的数据，不需要风险检查
                if (CollectionUtils.isEmpty(fundPaymentBLists)) {
                    //-1,未配置结算检查，跳过
                    result.put("checkFlag","-1");
                    result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400711", "已结算补单数据不需要风险检查！") /* "已结算补单数据不需要风险检查！" */);
                    return result;
                }
                // 设置过滤后的列表到 FundPaymentNew 对象
                fundPaymentNew.set("FundPayment_b", fundPaymentBLists);
                //结算风险检查入参
                SettleCheckReqVO settleCheckVo = initFundSettleCheckVo(fundPaymentNew, billCheckList);
                SettleCheckRespVO resJson = null;
                //230915 接入风险管理,未接入风险管理则走老逻辑
                if (!isEnableRiskCheck) {
                    try {
                        resJson = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).settleCheck(settleCheckVo);
                    } catch (Exception e) {
                        result.put("checkFlag", "999");
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070F", "结算检查接口异常，跳过检查！") /* "结算检查接口异常，跳过检查！" */ + e.getMessage());
                        return result;
                    }
                }

                //230915 接入风险管理,开启参数则走风险管理
                if (isEnableRiskCheck) {
                    try {
                        if ("save".equals(action)) {
                            actionCode = "SAVE";
                        } else {
                            actionCode = "SUBMIT";
                        }
                        pk_entity = fundPaymentNew.getAccentity();
                        billTypeCode = "cmp_fundpayment";
                        //第一次保存时无主键id，临时id用来记录风险校验结果
                        if (fundPaymentNew.getId() == null) {
                            fundPaymentNew.setId(ymsOidGenerator.nextId());
                            result.put("checkid", fundPaymentNew.getId().toString());
                        }

                        //风控引擎接口
                        RiskExecuteResult riskExecuteResult = RiskExecuteEngine.executeWithRuleEngin(pk_entity, billTypeCode, actionCode, fundPaymentNew, settleCheckVo, FundPayment.ENTITY_NAME);

//                        RiskStrategy riskStrategy = riskExecuteResult.getRiskStrategy();
//                        if (riskStrategy != null) {
//                            String code = riskStrategy.getCode();
//                            result.put("riskExecuteResultCode", code);
//                        }
//                        //解析成源返回数据
//                        resJson = initRiskCheckResult(riskExecuteResult);
                        result.put("riskCheckResult", JsonUtils.toJSON(riskExecuteResult));
                        if (!riskExecuteResult.isPass()){
                            result.put("checkFlag","1");//有风险
                            if(StringUtils.equals(riskExecuteResult.getRiskStrategy().getCode(), RiskStrategyEnum.INTERCEPT.getCode())) {
                                result.put("confirmType","2");
                                result.put("confirmMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F49B2005E00001", "风险检查不通过！") /* "风险检查不通过！" */);
                                return result;
                            }else if(StringUtils.equals(riskExecuteResult.getRiskStrategy().getCode(), RiskStrategyEnum.WARNING.getCode())){
                                result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F49B2005E00001", "风险检查不通过！") /* "风险检查不通过！" */);
                                return result;
                            }
                        } else {
                            return result;
                        }
                    } catch (Exception e) {
                        result.put("checkFlag", "999");
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400712", "风险管理风控引擎接口异常，跳过检查！") /* "风险管理风控引擎接口异常，跳过检查！" */ + e.getMessage());
                        return result;
                    }
                }

                //解析风险校验结果
                try {
                    result = initFundCheckResult(bList, fundPaymentNew, resJson, result, action);
                } catch (Exception e) {
                    result.put("checkFlag", "999");
                    result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070E", "检查结果解析错误，跳过检查！") /* "检查结果解析错误，跳过检查！" */ + e.getMessage());
                    return result;
                }

                break;
            }
            case "cm_transfer_account": { //转账工作台
                if (!isEnableRiskCheck && "save".equals(action) && !billCheckObjects.contains(BillCheckObject.transferSave.getValue() + "")) { //不包含转账单：保存，则跳过
                    //-1,未配置结算检查，跳过
                    result.put("checkFlag", "-1");
                    result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
                    return result;
                }
                if (!isEnableRiskCheck && ("listsubmit".equals(action) || "submit".equals(action)) && !billCheckObjects.contains(BillCheckObject.transferSubmit.getValue() + "")) { //不包含转账单：提交，则跳过
                    //-1,未配置结算检查，跳过
                    result.put("checkFlag", "-1");
                    result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
                    return result;
                }
                TransferAccount transferAccount = new TransferAccount();
                if ("listsubmit".equals(action)) {
                    transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, params.get("id"));
                } else {
                    transferAccount.init(map);
                }
                // 判断结算状态
                if (!checkSettleStatus(billnum, transferAccount)) {
                    //-1,未配置结算检查，跳过
                    result.put("checkFlag","-1");
                    result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400711", "已结算补单数据不需要风险检查！") /* "已结算补单数据不需要风险检查！" */);
                    return result;
                }
                //结算风险检查入参
                SettleCheckReqVO settleCheckVo = initTransferSettleCheckVo(transferAccount, billCheckList);
                SettleCheckRespVO settleCheckRespVO = null;
                //230915 接入风险管理,未接入风险管理则走老逻辑
                if (!isEnableRiskCheck) {
                    try {
                        settleCheckRespVO = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).settleCheck(settleCheckVo);
                    } catch (Exception e) {
                        result.put("checkFlag", "999");
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070F", "结算检查接口异常，跳过检查！") /* "结算检查接口异常，跳过检查！" */ + e.getMessage());
                        return result;
                    }
                }

                //230915 接入风险管理,未接入风险管理则走老逻辑
                if (isEnableRiskCheck) {
                    try {
                        if ("save".equals(action)) {
                            actionCode = "SAVE";
                        } else {
                            actionCode = "SUBMIT";
                        }
                        pk_entity = transferAccount.getAccentity();
                        //业务对象编码->单据类型编码
                        billTypeCode = "cm_transfer_account";
                        //第一次保存时无主键id，临时id用来记录风险校验结果。转账单新增态有id传过来
                        if (transferAccount.getId() == null) {
                            transferAccount.setId(ymsOidGenerator.nextId());
                            result.put("checkid", transferAccount.getId().toString());
                        } else {
                            result.put("checkid", transferAccount.getId().toString());
                        }
                        //风控引擎接口
                        RiskExecuteResult riskExecuteResult = RiskExecuteEngine.executeWithRuleEngin(pk_entity, billTypeCode, actionCode, transferAccount, settleCheckVo, TransferAccount.ENTITY_NAME);
//                        RiskStrategy riskStrategy = riskExecuteResult.getRiskStrategy();
//                        if (riskStrategy != null) {
//                            String code = riskStrategy.getCode();
//                            result.put("riskExecuteResultCode", code);
//                        }
//                        //解析成源返回数据
//                        settleCheckRespVO = initRiskCheckResult(riskExecuteResult);
//
                        result.put("riskCheckResult", JsonUtils.toJSON(riskExecuteResult));
                        if (!riskExecuteResult.isPass()){
                            result.put("checkFlag","1");//有风险
                            if(StringUtils.equals(riskExecuteResult.getRiskStrategy().getCode(), RiskStrategyEnum.INTERCEPT.getCode())) {
                                result.put("confirmType","2");
                                result.put("confirmMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F49B2005E00001", "风险检查不通过！") /* "风险检查不通过！" */);
                                return result;
                            }else if(StringUtils.equals(riskExecuteResult.getRiskStrategy().getCode(), RiskStrategyEnum.WARNING.getCode())){
                                result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F49B2005E00001", "风险检查不通过！") /* "风险检查不通过！" */);
                                return result;
                            }
                        } else {
                            return result;
                        }
                    } catch (Exception e) {
                        result.put("checkFlag", "999");
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400712", "风险管理风控引擎接口异常，跳过检查！") /* "风险管理风控引擎接口异常，跳过检查！" */ + e.getMessage());
                        return result;
                    }
                }

                //解析风险校验结果
                try {
                    result = initTransferCheckResult(transferAccount, settleCheckRespVO, result, action);
                } catch (Exception e) {
                    result.put("checkFlag", "999");
                    result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070E", "检查结果解析错误，跳过检查！") /* "检查结果解析错误，跳过检查！" */ + e.getMessage());
                    return result;
                }


                break;
            }
            case "cmp_foreignpayment": { //外汇付款
                if (!isEnableRiskCheck && "saveAndSubmitX".equals(action)) {
                    if (!billCheckObjects.contains(BillCheckObject.foreignPaymentSave.getValue() + "") && !billCheckObjects.contains(BillCheckObject.foreignPaymentSubmit.getValue() + "")) {
                        //-1,未配置结算检查，跳过
                        result.put("checkFlag", "-1");
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
                        return result;
                    }
                } else if (!isEnableRiskCheck && "save".equals(action) && !billCheckObjects.contains(BillCheckObject.foreignPaymentSave.getValue() + "")) { //不包含外汇付款：保存，则跳过
                    //-1,未配置结算检查，跳过
                    result.put("checkFlag", "-1");
                    result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
                    return result;
                } else if (!isEnableRiskCheck && ("listsubmit".equals(action) || "submit".equals(action)) && !billCheckObjects.contains(BillCheckObject.foreignPaymentSubmit.getValue() + "")) { //不包含外汇付款：提交，则跳过
                    //-1,未配置结算检查，跳过
                    result.put("checkFlag", "-1");
                    result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
                    return result;
                }
                ForeignPayment foreignPayment = new ForeignPayment();
                if ("listsubmit".equals(action)) {
                    foreignPayment = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, params.get("id"));
                } else {
                    foreignPayment.init(map);
                }
                // 判断结算状态
                if (!checkSettleStatus(billnum, foreignPayment)) {
                    //-1,未配置结算检查，跳过
                    result.put("checkFlag","-1");
                    result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400711", "已结算补单数据不需要风险检查！") /* "已结算补单数据不需要风险检查！" */);
                    return result;
                }
                //结算风险检查入参
                SettleCheckReqVO settleCheckVo = initForeignpaymentSettleCheckVo(foreignPayment, billCheckList);
                SettleCheckRespVO settleCheckRespVO = null;
                //是否接入风险管理,未接入风险管理则走老逻辑
                if (!isEnableRiskCheck) {
                    try {
                        settleCheckRespVO = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).settleCheck(settleCheckVo);
                    } catch (Exception e) {
                        result.put("checkFlag", "999");
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070F", "结算检查接口异常，跳过检查！") /* "结算检查接口异常，跳过检查！" */ + e.getMessage());
                        return result;
                    }
                }
                //接入风险管理,开启参数则走风险管理
                if (isEnableRiskCheck) {
                    try {
                        if ("save".equals(action) || "saveAndSubmitX".equals(action)) {
                            actionCode = "SAVE";
                        } else {
                            actionCode = "SUBMIT";
                        }
                        pk_entity = foreignPayment.getAccentity();
                        billTypeCode = "cmp_foreignpayment";
                        //第一次保存时无主键id，临时id用来记录风险校验结果
                        if (foreignPayment.getId() == null) {
                            foreignPayment.setId(ymsOidGenerator.nextId());
                            result.put("checkid", foreignPayment.getId().toString());
                        } else {
                            result.put("checkid", foreignPayment.getId().toString());
                        }
                        //风控引擎接口
                        RiskExecuteResult riskExecuteResult = RiskExecuteEngine.executeWithRuleEngin(pk_entity, billTypeCode, actionCode, foreignPayment, settleCheckVo, ForeignPayment.ENTITY_NAME);
//                        RiskStrategy riskStrategy = riskExecuteResult.getRiskStrategy();
//                        if (riskStrategy != null) {
//                            String code = riskStrategy.getCode();
//                            result.put("riskExecuteResultCode", code);
//                        }
//                        //解析成源返回数据
//                        settleCheckRespVO = initRiskCheckResult(riskExecuteResult);
                        result.put("riskCheckResult", JsonUtils.toJSON(riskExecuteResult));
                        if (!riskExecuteResult.isPass()){
                            result.put("checkFlag","1");//有风险
                            if(StringUtils.equals(riskExecuteResult.getRiskStrategy().getCode(), RiskStrategyEnum.INTERCEPT.getCode())) {
                                result.put("confirmType","2");//拦截
                                result.put("confirmMessage", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F49B2005E00001", "风险检查不通过！") /* "风险检查不通过！" */);
                                return result;
                            }else if(StringUtils.equals(riskExecuteResult.getRiskStrategy().getCode(), RiskStrategyEnum.WARNING.getCode())){
                                result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F49B2005E00001", "风险检查不通过！") /* "风险检查不通过！" */);
                                return result;
                            }
                        } else {
                            return result;
                        }
                    } catch (Exception e) {
                        result.put("checkFlag", "999");
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400712", "风险管理风控引擎接口异常，跳过检查！") /* "风险管理风控引擎接口异常，跳过检查！" */ + e.getMessage());
                        return result;
                    }
                }

                //解析风险校验结果
                try {
                    result = initForeignpaymentCheckResult(foreignPayment, settleCheckRespVO, result, action);
                } catch (Exception e) {
                    result.put("checkFlag", "999");
                    result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070E", "检查结果解析错误，跳过检查！") /* "检查结果解析错误，跳过检查！" */ + e.getMessage());
                    return result;
                }
                break;
            }
            default:
                break;
        }

        return result;
    }

    /**
     * 校验结算状态，已结算补单时返回false,不需要进行结算检查
     * @param billnum
     * @param data
     * @return
     * @throws Exception
     */
    private boolean checkSettleStatus(String billnum, Object data) throws Exception {
        switch (billnum){
            case IBillNumConstant.CMP_FOREIGNPAYMENT:
                ForeignPayment foreignPayment = (ForeignPayment) data;
                if(foreignPayment.getSettlestatus() != null && FundSettleStatus.SettlementSupplement.getValue() == foreignPayment.getSettlestatus()){
                    return false;
                }
                break;
            case IBillNumConstant.TRANSFERACCOUNT:
                TransferAccount transferAccount = (TransferAccount) data;
                if(transferAccount.getSettlestatus() != null && SettleStatus.SettledRep.getValue() == transferAccount.getSettlestatus().getValue()){
                    return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    public FundPayment getFundPaymentById(Object id) throws Exception {
        FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id);
        if (Objects.isNull(fundPayment)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100700"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180385", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(fundPayment.getId()));
        querySchema.addCondition(group);
        querySchema.addOrderBy(new QueryOrderby("lineno", "asc"));
        List<FundPayment_b> bList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
        fundPayment.setFundPayment_b(bList);
        return fundPayment;
    }

    /**
     * 资金付款单提交结算风险检查
     * @param fundPayment
     * @return 1有风险 其他跳过
     * @throws Exception
     */
    @Override
    public CtmJSONObject fundSubmitBillCheck(FundPayment fundPayment) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //获取资金组织
        String accentity = fundPayment.getAccentity();
        //是否开启风险校验分支，开启则校验走风险校验模块
        boolean isEnableRiskCheck = RiskEnvironment.isEnable();
        //根据资金组织查询配置的现金参数-结算检查参数
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity));
        querySchema1.addCondition(group1);
        List<AutoConfig> configList= MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME,querySchema1,null);
        if ( !isEnableRiskCheck &&  (configList == null || configList.size() == 0)){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }

        AutoConfig autoConfig = CollectionUtils.isEmpty(configList)? new AutoConfig() : configList.get(0);
        //未开启结算检查或者结算检查对象，结算检查项为空，直接跳过
        if ( !isEnableRiskCheck && (!autoConfig.getBillCheckFlag() || StringUtils.isEmpty(autoConfig.getBillCheckObject())
                || StringUtils.isEmpty(autoConfig.getBillCheck())) ){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }
        //结算检查对象集合
        List<String> billCheckObjects = autoConfig.getBillCheckObject() == null? new ArrayList<>() : Arrays.asList(autoConfig.getBillCheckObject().split(","));
        //不包含资金付款单：提交，则跳过
        if ( !isEnableRiskCheck &&  !billCheckObjects.contains(BillCheckObject.paybillSubmit.getValue() + "")){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }

        //结算检查项集合
        List<String> billCheckList = autoConfig.getBillCheck() ==null ? new ArrayList<>() : Arrays.asList(autoConfig.getBillCheck().split(","));
        //结算风险检查入参
        SettleCheckReqVO settleCheckVo = initFundSettleCheckVo(fundPayment,billCheckList);
        SettleCheckRespVO resJson = null;

        //230915 接入风险管理,未接入风险管理则走老逻辑
        if (!isEnableRiskCheck){
            try {
                resJson = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).settleCheck(settleCheckVo);
            }catch (Exception e){
                result.put("checkFlag","999");
                result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070F", "结算检查接口异常，跳过检查！") /* "结算检查接口异常，跳过检查！" */ + e.getMessage());
                return result;
            }
        }

        //230915 接入风险管理,开启参数则走风险管理
        if (isEnableRiskCheck){
            try {
                String actionCode ="SUBMIT";
                String pk_entity = fundPayment.getAccentity();
                String billTypeCode = "cmp_fundpayment";
                //风控引擎接口
                RiskExecuteResult riskExecuteResult = RiskExecuteEngine.executeWithRuleEngin(pk_entity,billTypeCode,actionCode,fundPayment,settleCheckVo, FundPayment.ENTITY_NAME);
                //解析成源返回数据
//                resJson = initRiskCheckResult(riskExecuteResult);
                result.put("riskCheckResult", JsonUtils.toJSON(riskExecuteResult));
                if (!riskExecuteResult.isPass()){
                    if(StringUtils.equals(riskExecuteResult.getRiskStrategy().getCode(), RiskStrategyEnum.INTERCEPT.getCode())) {
                        result.put("checkFlag","1");//有风险
                        result.put("confirmType","2");//拦截
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F49B2005E00001", "风险检查不通过！") /* "风险检查不通过！" */);
                        return result;
                    }
                } else {
                    return result;
                }
            }catch (Exception e){
                result.put("checkFlag","999");
                result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400712", "风险管理风控引擎接口异常，跳过检查！") /* "风险管理风控引擎接口异常，跳过检查！" */ + e.getMessage());
                return result;
            }
        }
        // 克隆 FundPayment 对象
        FundPayment fundPaymentNew = (FundPayment) fundPayment.clone();
        // 获取 FundPayment_b 列表
        List<FundPayment_b> bList = fundPayment.FundPayment_b();
        // 使用 Stream 对列表进行过滤
        List<FundPayment_b> fundPaymentBLists = bList.stream()
                .filter(b -> b != null && b.get("settlestatus") != null && FundSettleStatus.SettlementSupplement.getValue() != Short.parseShort("" + b.get("settlestatus"))) // 过滤掉 已结算补单的数据
                .filter(b -> FundSettleStatus.Refund.getValue() != Short.parseShort("" + b.get("settlestatus"))) // 过滤掉 退票的数据
                .collect(Collectors.toList()); // 收集成新的 List
        fundPaymentNew.setFundPayment_b(fundPaymentBLists);
        //解析风险校验结果
        try {
            result = initFundCheckResult(bList, fundPaymentNew, resJson,result,"submit");
        }catch (Exception e){
            result.put("checkFlag","999");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070E", "检查结果解析错误，跳过检查！") /* "检查结果解析错误，跳过检查！" */ + e.getMessage());
            return result;
        }
        return result;
    }

    /**
     * 转账单提交结算风险检查
     * @param transferAccount
     * @return 1有风险，其他跳过
     * @throws Exception
     */
    @Override
    public CtmJSONObject transferSubmitBillCheck(TransferAccount transferAccount) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //获取资金组织
        String accentity = transferAccount.getAccentity();
        //是否开启风险校验分支，开启则校验走风险校验模块
        boolean isEnableRiskCheck = RiskEnvironment.isEnable();
        //根据资金组织查询配置的现金参数-结算检查参数
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity));
        querySchema1.addCondition(group1);
        List<AutoConfig> configList= MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME,querySchema1,null);
        if ( !isEnableRiskCheck && (configList == null || configList.size() == 0)){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }

        AutoConfig autoConfig = CollectionUtils.isEmpty(configList)? new AutoConfig() : configList.get(0);
        //未开启结算检查或者结算检查对象，结算检查项为空，直接跳过
        if (!isEnableRiskCheck && (!autoConfig.getBillCheckFlag() || StringUtils.isEmpty(autoConfig.getBillCheckObject())
                || StringUtils.isEmpty(autoConfig.getBillCheck())) ){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }
        //结算检查对象集合
        List<String> billCheckObjects = autoConfig.getBillCheckObject() == null? new ArrayList<>() : Arrays.asList(autoConfig.getBillCheckObject().split(","));
        //不包含转账单：提交，则跳过
        if ( !isEnableRiskCheck && !billCheckObjects.contains(BillCheckObject.transferSubmit.getValue() + "")){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }
        //结算检查项集合
        List<String> billCheckList = autoConfig.getBillCheck() ==null ? new ArrayList<>() : Arrays.asList(autoConfig.getBillCheck().split(","));
        //结算风险检查入参
        SettleCheckReqVO settleCheckVo = initTransferSettleCheckVo(transferAccount,billCheckList);
        SettleCheckRespVO resJson = null;

        //230915 接入风险管理,未接入风险管理则走老逻辑
        if (!isEnableRiskCheck){
            try {
                resJson = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).settleCheck(settleCheckVo);
            }catch (Exception e){
                result.put("checkFlag","999");
                result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070F", "结算检查接口异常，跳过检查！") /* "结算检查接口异常，跳过检查！" */ + e.getMessage());
                return result;
            }
        }

        //230915 接入风险管理,未接入风险管理则走老逻辑
        if (isEnableRiskCheck){
            try {
                String actionCode ="SUBMIT";
                String pk_entity = transferAccount.getAccentity();
                //业务对象编码->单据类型编码
                String billTypeCode = "cm_transfer_account";
                //风控引擎接口
                RiskExecuteResult riskExecuteResult = RiskExecuteEngine.executeWithRuleEngin(pk_entity,billTypeCode,actionCode,transferAccount,settleCheckVo, TransferAccount.ENTITY_NAME);
                //解析成源返回数据
//                resJson = initRiskCheckResult(riskExecuteResult);

                result.put("riskCheckResult", JsonUtils.toJSON(riskExecuteResult));
                if (!riskExecuteResult.isPass()){
                    if(StringUtils.equals(riskExecuteResult.getRiskStrategy().getCode(), RiskStrategyEnum.INTERCEPT.getCode())) {
                        result.put("checkFlag","1");//有风险
                        result.put("confirmType","2");//拦截
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F49B2005E00001", "风险检查不通过！") /* "风险检查不通过！" */);
                        return result;
                    }
                } else {
                    return result;
                }
            }catch (Exception e){
                result.put("checkFlag","999");
                result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400712", "风险管理风控引擎接口异常，跳过检查！") /* "风险管理风控引擎接口异常，跳过检查！" */ + e.getMessage());
                return result;
            }
        }

        try {
            //解析风险校验结果
            result = initTransferCheckResult(transferAccount,resJson,result,"submit");
        }catch (Exception e){
            result.put("checkFlag","999");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070E", "检查结果解析错误，跳过检查！") /* "检查结果解析错误，跳过检查！" */ + e.getMessage());
            return result;
        }

        return result;
    }

    /**
     * 外汇付款 提交结算风险检查*
     * @param foreignPayment
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject foreignpaymentSubmitBillCheck(ForeignPayment foreignPayment) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        //获取资金组织
        String accentity = foreignPayment.getAccentity();
        //是否开启风险校验分支，开启则校验走风险校验模块
        boolean isEnableRiskCheck = RiskEnvironment.isEnable();
        //根据资金组织查询配置的现金参数-结算检查参数
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("accentity").eq(accentity));
        querySchema1.addCondition(group1);
        List<AutoConfig> configList= MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME,querySchema1,null);
        if ( !isEnableRiskCheck && (configList == null || configList.size() == 0)){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }

        AutoConfig autoConfig = CollectionUtils.isEmpty(configList)? new AutoConfig() : configList.get(0);
        //未开启结算检查或者结算检查对象，结算检查项为空，直接跳过
        if (!isEnableRiskCheck && (!autoConfig.getBillCheckFlag() || StringUtils.isEmpty(autoConfig.getBillCheckObject())
                || StringUtils.isEmpty(autoConfig.getBillCheck())) ){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }
        //结算检查对象集合
        List<String> billCheckObjects = autoConfig.getBillCheckObject() == null? new ArrayList<>() : Arrays.asList(autoConfig.getBillCheckObject().split(","));
        //不包含转账单：提交，则跳过
        if ( !isEnableRiskCheck && !billCheckObjects.contains(BillCheckObject.foreignPaymentSubmit.getValue() + "")){
            //-1,未配置结算检查，跳过
            result.put("checkFlag","-1");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400710", "未配置对应结算风险检查！") /* "未配置对应结算风险检查！" */);
            return result;
        }
        //结算检查项集合
        List<String> billCheckList = autoConfig.getBillCheck() ==null ? new ArrayList<>() : Arrays.asList(autoConfig.getBillCheck().split(","));
        //结算风险检查入参
        SettleCheckReqVO settleCheckVo = initForeignpaymentSettleCheckVo(foreignPayment,billCheckList);
        SettleCheckRespVO resJson = null;

        //230915 接入风险管理,未接入风险管理则走老逻辑
        if (!isEnableRiskCheck){
            try {
                resJson = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).settleCheck(settleCheckVo);
            }catch (Exception e){
                result.put("checkFlag","999");
                result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070F", "结算检查接口异常，跳过检查！") /* "结算检查接口异常，跳过检查！" */ + e.getMessage());
                return result;
            }
        }

        //230915 接入风险管理,未接入风险管理则走老逻辑
        if (isEnableRiskCheck){
            try {
                String actionCode ="SUBMIT";
                String pk_entity = foreignPayment.getAccentity();
                //业务对象编码->单据类型编码
                String billTypeCode = "cmp_foreignpayment";
                //风控引擎接口
                RiskExecuteResult riskExecuteResult = RiskExecuteEngine.executeWithRuleEngin(pk_entity,billTypeCode,actionCode,foreignPayment,settleCheckVo,ForeignPayment.ENTITY_NAME);
                //解析成源返回数据
//                resJson = initRiskCheckResult(riskExecuteResult);

                result.put("riskCheckResult", JsonUtils.toJSON(riskExecuteResult));
                if (!riskExecuteResult.isPass()){
                    if(StringUtils.equals(riskExecuteResult.getRiskStrategy().getCode(), RiskStrategyEnum.INTERCEPT.getCode())) {
                        result.put("checkFlag","1");//有风险
                        result.put("confirmType","2");//拦截
                        result.put("checkMsg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F49B2005E00001", "风险检查不通过！") /* "风险检查不通过！" */);
                        return result;
                    }
                } else {
                    return result;
                }
            }catch (Exception e){
                result.put("checkFlag","999");
                result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400712", "风险管理风控引擎接口异常，跳过检查！") /* "风险管理风控引擎接口异常，跳过检查！" */ + e.getMessage());
                return result;
            }
        }

        try {
            //解析风险校验结果
            result = initForeignpaymentCheckResult(foreignPayment,resJson,result,"submit");
        }catch (Exception e){
            result.put("checkFlag","999");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540070E", "检查结果解析错误，跳过检查！") /* "检查结果解析错误，跳过检查！" */ + e.getMessage());
            return result;
        }

        return result;
    }



    /**
     * 组装资金付款单风险检查入参
     * @param fundPayment
     * @param billCheckList
     * @return
     */
    private SettleCheckReqVO initFundSettleCheckVo(FundPayment fundPayment, List<String> billCheckList){
        SettleCheckReqVO settleCheckVo = new SettleCheckReqVO();
        //疑重校验
        settleCheckVo.setQuestionCheck(billCheckList.contains(BillCheck.duplication.getValue() +""));
        //直联账户余额校验
        settleCheckVo.setAccBalanceCheck(billCheckList.contains(BillCheck.balance.getValue() +""));
        //非直联账户余额校验
        settleCheckVo.setNoDirAccBalanceCheck(billCheckList.contains(BillCheck.noDirAccBalance.getValue() +""));
        //大额交易
        settleCheckVo.setBlockTradeCheck(billCheckList.contains(BillCheck.blockTrade.getValue() +""));
        //黑名单
        settleCheckVo.setBlackListCheck(billCheckList.contains(BillCheck.blackList.getValue() +""));
        //敏感词
        settleCheckVo.setSensitiveWordCheck(billCheckList.contains(BillCheck.sensitiveWord.getValue() +""));
        //账户支付限额
        settleCheckVo.setAccountLimit(billCheckList.contains(BillCheck.paymentLimit.getValue() +""));
        //关联内部账户可用余额
        settleCheckVo.setInternalAccountBalance(billCheckList.contains(BillCheck.availableBalance.getValue() +""));
        //高频交易
        settleCheckVo.setHighFrequencyTrade(billCheckList.contains(BillCheck.highFrequency.getValue() +""));
        //灰名单
        settleCheckVo.setGreyRollCheck(billCheckList.contains(BillCheck.grayList.getValue() + ""));
        //来源业务系统：现金管理 8
        settleCheckVo.setBizsyssrc((short)8);
        //资金组织
        settleCheckVo.setAccentity(fundPayment.getAccentity());

        //伙伴黑灰名单需要：5资金付；6转账单；7外汇付款单
        settleCheckVo.setBillType((short) 5);

        //数据明细
        List<SettleReqVO> settleVoList = new ArrayList<>();
        List<FundPayment_b> fundPayment_bList = fundPayment.FundPayment_b();
        if (fundPayment_bList !=null && fundPayment_bList.size()>0){
            for (FundPayment_b b: fundPayment_bList){
                SettleReqVO settleVo = new SettleReqVO();
                settleVo.setAccentity(fundPayment.getAccentity());
                //明细id,新增时id为空，赋值一个临时id用来做判断
                if (b.getId() == null){
                    b.setId(ymsOidGenerator.nextId());
                }
                settleVo.setBizBillDetailId(b.getId().toString());
                //收付类型 1收2付。资金付款单默认2
                settleVo.setReceipttypeb(2);
                //结算方式id
                settleVo.setSettlemode(b.get("settlemode") !=null ?Long.parseLong(b.get("settlemode").toString()):null);
                //本方银行账户id
                settleVo.setOurbankaccount(b.getEnterprisebankaccount() != null ?b.getEnterprisebankaccount():null);
                //原币币种id
                settleVo.setOriginalcurrency(b.getCurrency());
                //原币交易金额
                settleVo.setOriginalcurrencyamt(new BigDecimal(b.get("oriSum").toString()));
                //换出币种=原币
                settleVo.setExchangePaymentCurrency(b.getCurrency());
                //换出金额=原币金额
                settleVo.setExchangePaymentAmount(new BigDecimal(b.get("oriSum").toString()));
                //对方类型
                settleVo.setCounterpartytype(b.get("caobject").toString());
                //对方类型为其他时增加对公对私
                if(b.get("caobject")!= null && "4".equals(b.get("caobject").toString())){
                    if (b.get("publicPrivate") !=null && "1".equals(b.get("publicPrivate").toString())){//对私
                        settleVo.setDebitType((short)1);
                    }
                    if (b.get("publicPrivate") !=null && "2".equals(b.get("publicPrivate").toString())){//对公
                        settleVo.setDebitType((short)2);
                    }
                }
                //对方id
                settleVo.setCounterpartyid(b.get("oppositeobjectid") == null ? null :b.get("oppositeobjectid").toString());
                //对方名称
                settleVo.setCounterpartyname(b.getOppositeobjectname());
                //对方银行账号id
                settleVo.setCounterpartybankaccount(b.get("oppositeaccountid") == null ?null:b.get("oppositeaccountid").toString());
                //对方账户名称
                settleVo.setCounterpartyaccname(b.getOppositeaccountname());
                //对方账号
                settleVo.setCounterpartybankacc(b.getOppositeaccountno());
                //对方开户行名
                settleVo.setCounterpartybankname(b.getOppositebankaddr());
                //来源业务id
                settleVo.setBizsyssrc("8");
                //本币金额
                settleVo.setNatAmt(new BigDecimal(b.get("natSum").toString()));
                //备注
                settleVo.setDescription(b.getDescription());
                //是否统收统支
                settleVo.setIncomeAndExpenditure(false);
                //款项类型
                if(b.get("quickType") != null){
                    settleVo.setProceedType(b.get("quickType").toString());
                }
                settleVoList.add(settleVo);
            }
        }
        settleCheckVo.setSettleVoList(settleVoList);

        //开通风险管理的，增加结算参数传递
        if (RiskEnvironment.isEnable()){
            //存放结算方式
            Map<Long, Integer> serviceAttrs = new HashMap<>();
            List<Long> serviceAttrIds = new ArrayList<>();
            for (SettleReqVO s : settleCheckVo.getSettleVoList()) {
                if (ObjectUtils.isNotEmpty(s.getSettlemode())) {
                    serviceAttrIds.add(s.getSettlemode());
                }
            }
            //获取结算方式
            if (ValueUtils.isNotEmpty(serviceAttrIds)) {
                serviceAttrs = cmCommonService.getServiceAttrs(serviceAttrIds);
            }
            settleCheckVo.setServiceAttrs(serviceAttrs);
        }
        return settleCheckVo;
    }

    /**
     * 处理风险检查结果
     * @param fundPayment
     * @param resJson
     * @param result
     * @return
     */
    private CtmJSONObject initFundCheckResult(List<FundPayment_b> oldBList, FundPayment fundPayment,SettleCheckRespVO resJson,CtmJSONObject result,String action) throws Exception {
        //存在的风险项明细ID
        //疑重校验
        List<String> dignifiedIds = resJson.getDignifiedFailedList();
        //直联账户余额
        List<String> balanceIds = resJson.getAccBalanceCheckList();
        //非直联账户余额
        List<String> noDirAccBalances = resJson.getNoDirAccBalanceCheckList();
        //大额交易
        List<String> blockTradeIds = resJson.getTransactionFailedList();
        //黑名单
        List<String> blackIds = resJson.getBlackFailedList();
        //敏感词
        List<String> sensitiveWordIds = resJson.getResultSensitiveList();
        //账户支付限额
        List<String> paymentLimitIds = resJson.getAccountLimitList();
        //关联内部户可用余额
        List<String> availableBalanceIds = resJson.getInternalAccountList();
        //高频交易
        List<String> highFrequencyIds = resJson.getHighFrequencyList();
        //灰名单
        List<String> grayListIds = resJson.getGreyFailedList();

        //校验结果
        StringBuilder message = new StringBuilder();
        StringBuilder confirmMessage = new StringBuilder();
        //判断是否有对应的
        List<FundPayment_b> bList = fundPayment.FundPayment_b();
        boolean isContainRisk = false;
        //控制类型 默认提示;1提示；2控制
        String confirmType = "1";
        HashMap<String, Integer> idOrderMap = new HashMap<>();
        for(int i=0;i<oldBList.size();i++){
            idOrderMap.put(oldBList.get(i).get("lineno").toString(), i + 1);
        }
        for(int i=0;i<bList.size();i++){
            List<String> riskList = new ArrayList<>();
            List<String> riskTypeList = new ArrayList<>();
            List<String> riskControlList = new ArrayList<>();
            if (bList.get(i) != null && "Delete".equals(bList.get(i).get("_status"))){
                continue;
            }
            //疑重校验
            if (CollectionUtils.isNotEmpty(dignifiedIds) && dignifiedIds.contains(bList.get(i).getId().toString())){
                riskList.add(BillCheck.duplication.getName());
                riskTypeList.add(BillCheck.duplication.getValue() + "");
                //疑重校验没有是否控制的类型
            }
            //直联账户余额
            if (CollectionUtils.isNotEmpty(balanceIds) && balanceIds.contains(bList.get(i).getId().toString())){
                riskList.add(BillCheck.balance.getName());
                riskTypeList.add(BillCheck.balance.getValue() + "");
                Map<String, Short> c = resJson.getAccBalanceCheckMap();
                if (c != null){
                    Object cType = c.get(bList.get(i).getId().toString());
                    if (cType!=null && "2".equals(cType.toString())){
                        confirmType = "2";
                        riskControlList.add(BillCheck.balance.getName());
                    }
                }
            }
            //非直联账户余额
            if (CollectionUtils.isNotEmpty(noDirAccBalances) && noDirAccBalances.contains(bList.get(i).getId().toString())) {
                riskList.add(BillCheck.noDirAccBalance.getName());
                riskTypeList.add(BillCheck.noDirAccBalance.getValue() + "");
                Map<String, Short> c = resJson.getNoDirAccBalanceCheckMap();
                if (c != null) {
                    Object cType = c.get(bList.get(i).getId().toString());
                    if (cType != null && "2".equals(cType.toString())) {
                        confirmType = "2";
                        riskControlList.add(BillCheck.noDirAccBalance.getName());
                    }
                }
            }
            //大额交易
            if (CollectionUtils.isNotEmpty(blockTradeIds) && blockTradeIds.contains(bList.get(i).getId().toString())){
                riskList.add(BillCheck.blockTrade.getName());
                riskTypeList.add(BillCheck.blockTrade.getValue() + "");
                Map<String, Short> c = resJson.getTransactionFailedMap();
                if (c != null){
                    Object cType = c.get(bList.get(i).getId().toString());
                    if (cType!=null && "2".equals(cType.toString())){
                        confirmType = "2";
                        riskControlList.add(BillCheck.blockTrade.getName());
                    }
                }
            }
            //黑名单
            if (CollectionUtils.isNotEmpty(blackIds) && blackIds.contains(bList.get(i).getId().toString())){
                riskList.add(BillCheck.blackList.getName());
                riskTypeList.add(BillCheck.blackList.getValue() + "");
                Map<String, Short> c = resJson.getBlackFailedMap();
                if (c != null){
                    Object cType = c.get(bList.get(i).getId().toString());
                    if (cType!=null && "2".equals(cType.toString())){
                        confirmType = "2";
                        riskControlList.add(BillCheck.blackList.getName());
                    }
                }
            }
            //敏感词
            if (CollectionUtils.isNotEmpty(sensitiveWordIds) && sensitiveWordIds.contains(bList.get(i).getId().toString())){
                riskList.add(BillCheck.sensitiveWord.getName());
                riskTypeList.add(BillCheck.sensitiveWord.getValue() + "");
                Map<String, Short> c = resJson.getResultSensitiveMap();
                if (c != null){
                    Object cType = c.get(bList.get(i).getId().toString());
                    if (cType!=null && "2".equals(cType.toString())){
                        confirmType = "2";
                        riskControlList.add(BillCheck.sensitiveWord.getName());
                    }
                }
            }
            //账户支付限额
            if (CollectionUtils.isNotEmpty(paymentLimitIds) && paymentLimitIds.contains(bList.get(i).getId().toString())){
                riskList.add(BillCheck.paymentLimit.getName());
                riskTypeList.add(BillCheck.paymentLimit.getValue() + "");
                Map<String, Short> c = resJson.getAccountLimitMap();
                if (c != null){
                    Object cType = c.get(bList.get(i).getId().toString());
                    if (cType!=null && "2".equals(cType.toString())){
                        confirmType = "2";
                        riskControlList.add(BillCheck.paymentLimit.getName());
                    }
                }
            }
            //关联内部户可用余额
            if (CollectionUtils.isNotEmpty(availableBalanceIds) && availableBalanceIds.contains(bList.get(i).getId().toString())){
                riskList.add(BillCheck.availableBalance.getName());
                riskTypeList.add(BillCheck.availableBalance.getValue() + "");
                Map<String, Short> c = resJson.getInternalAccountMap();
                if (c != null){
                    Object cType = c.get(bList.get(i).getId().toString());
                    if (cType!=null && "2".equals(cType.toString())){
                        confirmType = "2";
                        riskControlList.add(BillCheck.availableBalance.getName());
                    }
                }
            }
            //高频交易
            if (CollectionUtils.isNotEmpty(highFrequencyIds) && highFrequencyIds.contains(bList.get(i).getId().toString())){
                riskList.add(BillCheck.highFrequency.getName());
                riskTypeList.add(BillCheck.highFrequency.getValue() + "");
                Map<String, Short> c = resJson.getHighFrequencyMap();
                if (c != null){
                    Object cType = c.get(bList.get(i).getId().toString());
                    if (cType!=null && "2".equals(cType.toString())){
                        confirmType = "2";
                        riskControlList.add(BillCheck.highFrequency.getName());
                    }
                }
            }
            //灰名单
            if (CollectionUtils.isNotEmpty(grayListIds) && grayListIds.contains(bList.get(i).getId().toString())){
                riskList.add(BillCheck.grayList.getName());
                riskTypeList.add(BillCheck.grayList.getValue() + "");
                Map<String, Short> c = resJson.getGreyFailedMap();
                if (c != null){
                    Object cType = c.get(bList.get(i).getId().toString());
                    if (cType!=null && "2".equals(cType.toString())){
                        confirmType = "2";
                        riskControlList.add(BillCheck.grayList.getName());
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(riskList)){
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18210E2A04B00008", "明细第【%s】行，存在【%s】风险项。") /* "明细第【%s】行，存在【%s】风险项。" */,idOrderMap.get(bList.get(i).get("lineno").toString()),StringUtils.join(riskList,"，")));
                result.put("riskIndex"+(idOrderMap.get(bList.get(i).get("lineno").toString())-1),"1");
                result.put("riskTypeList"+(idOrderMap.get(bList.get(i).get("lineno").toString())-1),StringUtils.join(riskTypeList,","));
                isContainRisk = true;
            }else {
                result.put("riskIndex"+(idOrderMap.get(bList.get(i).get("lineno").toString())-1),"0");
            }
            if (CollectionUtils.isNotEmpty(riskControlList)){
                confirmMessage.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F4F4405B00011","第【%s】行，存在【%s】风险，风险管控方式为控制。") /* "第【%s】行，存在【%s】风险，风险管控方式为控制。" */,idOrderMap.get(bList.get(i).get("lineno").toString()),StringUtils.join(riskControlList,"，")));
            }
        }
        if (isContainRisk){
            //1代表有风险
            result.put("checkFlag","1");
            result.put("checkMsg",message.toString());
            result.put("confirmMessage",confirmMessage.toString());
            //1:提示；2控制
            String code = result.getString("riskExecuteResultCode");
            if(StringUtils.isNotEmpty(code)){
                if(RiskStrategyEnum.INTERCEPT.getCode().equals(code)){
                    confirmType = "2";
                    result.put("confirmMessage",result.get("checkMsg"));
                }else if(RiskStrategyEnum.WARNING.getCode().equals(code)){
                    confirmType = "1";
                }
            }
            result.put("confirmType",confirmType);
        }else {
            result.put("checkFlag","0");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400713", "不存在风险！") /* "不存在风险！" */);
        }

        return result;
    }


    /**
     * 组装转账单风险检查入参
     * @param transferAccount
     * @return
     */
    private SettleCheckReqVO initTransferSettleCheckVo(TransferAccount transferAccount, List<String> billCheckList) throws Exception {
        SettleCheckReqVO settleCheckVo = new SettleCheckReqVO();
        //疑重校验
        settleCheckVo.setQuestionCheck(billCheckList.contains(BillCheck.duplication.getValue() +""));
        //直联账户余额校验
        settleCheckVo.setAccBalanceCheck(billCheckList.contains(BillCheck.balance.getValue() +""));
        //非直联账户余额校验
        settleCheckVo.setNoDirAccBalanceCheck(billCheckList.contains(BillCheck.noDirAccBalance.getValue() +""));
        //大额交易
        settleCheckVo.setBlockTradeCheck(billCheckList.contains(BillCheck.blockTrade.getValue() +""));
        //黑名单
        settleCheckVo.setBlackListCheck(billCheckList.contains(BillCheck.blackList.getValue() +""));
        //敏感词
        settleCheckVo.setSensitiveWordCheck(billCheckList.contains(BillCheck.sensitiveWord.getValue() +""));
        //账户支付限额
        settleCheckVo.setAccountLimit(billCheckList.contains(BillCheck.paymentLimit.getValue() +""));
        //关联内部账户可用余额
        settleCheckVo.setInternalAccountBalance(billCheckList.contains(BillCheck.availableBalance.getValue() +""));
        //高频交易
        settleCheckVo.setHighFrequencyTrade(billCheckList.contains(BillCheck.highFrequency.getValue() +""));
        //灰名单
        settleCheckVo.setGreyRollCheck(billCheckList.contains(BillCheck.grayList.getValue() + ""));
        //来源业务系统：现金管理 8
        settleCheckVo.setBizsyssrc((short)8);
        //资金组织
        settleCheckVo.setAccentity(transferAccount.getAccentity());

        //伙伴黑灰名单需要：5资金付；6转账单；7外汇付款单
        settleCheckVo.setBillType((short) 6);

        //数据明细
        List<SettleReqVO> settleVoList = new ArrayList<>();
        SettleReqVO settleVo = new SettleReqVO();
        settleVo.setAccentity(transferAccount.getAccentity());
        //明细id,新增时id可能为空，赋值一个临时id用来做判断
        if (transferAccount.getId() == null){
            transferAccount.setId(ymsOidGenerator.nextId());
        }
        settleVo.setBizBillDetailId(transferAccount.getId().toString());
        //收付类型 1收2付。
        settleVo.setReceipttypeb(2);
        //账户类型 1是对私2是对公,转账单默认对公
        settleVo.setDebitType((short)2);
        //结算方式id
        settleVo.setSettlemode(transferAccount.get("settlemode") !=null ?Long.parseLong(transferAccount.get("settlemode").toString()):null);
        //本方银行账户id
        settleVo.setOurbankaccount(transferAccount.getPayBankAccount() != null ?transferAccount.getPayBankAccount():null);
        //原币币种id
        settleVo.setOriginalcurrency(transferAccount.getCurrency());
        //原币交易金额
        settleVo.setOriginalcurrencyamt(new BigDecimal(transferAccount.get("oriSum").toString()));
        //换出币种=原币
        settleVo.setExchangePaymentCurrency(transferAccount.getCurrency());
        //换出金额=原币金额
        settleVo.setExchangePaymentAmount(new BigDecimal(transferAccount.get("oriSum").toString()));
        //对方银行账号id
        settleVo.setCounterpartybankaccount(transferAccount.getRecBankAccount());
        if(!StringUtils.isEmpty(transferAccount.getRecBankAccount())){
            EnterpriseBankAcctVO   enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(transferAccount.getRecBankAccount());
            if(null != enterpriseBankAcctVO){
                //对方账户名称
                settleVo.setCounterpartyaccname(enterpriseBankAcctVO.getAcctName());
                //对方账号
                settleVo.setCounterpartybankacc(enterpriseBankAcctVO.getAccount());
            }
        }
        //高频交易校验 对方名称不能为空，取核算会计主体名称
        String accEntityName = (String) QueryBaseDocUtils.queryAccRawEntityByAccEntityId(transferAccount.getAccentity()).get(0).get("name");
        settleVo.setCounterpartyname(accEntityName);
        //来源业务id
        settleVo.setBizsyssrc("8");
        //本币金额
        settleVo.setNatAmt(new BigDecimal(transferAccount.get("natSum").toString()));
        //备注
        settleVo.setDescription(transferAccount.getDescription());
        //是否统收统支
        settleVo.setIncomeAndExpenditure(false);
        //对方类型
        settleVo.setCounterpartytype("6");
        //对方id
        settleVo.setCounterpartyid(transferAccount.getAccentity());
        settleVoList.add(settleVo);
        settleCheckVo.setSettleVoList(settleVoList);

        //开通风险管理的，增加结算参数传递
        if (RiskEnvironment.isEnable()){
            //存放结算方式
            Map<Long, Integer> serviceAttrs = new HashMap<>();
            List<Long> serviceAttrIds = new ArrayList<>();
            for (SettleReqVO s : settleCheckVo.getSettleVoList()) {
                if (ObjectUtils.isNotEmpty(s.getSettlemode())) {
                    serviceAttrIds.add(s.getSettlemode());
                }
            }
            //获取结算方式
            if (ValueUtils.isNotEmpty(serviceAttrIds)) {
                serviceAttrs = cmCommonService.getServiceAttrs(serviceAttrIds);
            }
            settleCheckVo.setServiceAttrs(serviceAttrs);
        }

        return settleCheckVo;
    }


    /**
     * 组装外汇付款 风险检查入参*
     * @param foreignPayment
     * @param billCheckList
     * @return
     * @throws Exception
     */

    private SettleCheckReqVO initForeignpaymentSettleCheckVo(ForeignPayment foreignPayment, List<String> billCheckList) throws Exception {
        SettleCheckReqVO settleCheckVo = new SettleCheckReqVO();
        //疑重校验
        settleCheckVo.setQuestionCheck(billCheckList.contains(BillCheck.duplication.getValue() +""));
        //直联账户余额校验
        settleCheckVo.setAccBalanceCheck(billCheckList.contains(BillCheck.balance.getValue() +""));
        //非直联账户余额校验
        settleCheckVo.setNoDirAccBalanceCheck(billCheckList.contains(BillCheck.noDirAccBalance.getValue() +""));
        //大额交易
        settleCheckVo.setBlockTradeCheck(billCheckList.contains(BillCheck.blockTrade.getValue() +""));
        //黑名单
        settleCheckVo.setBlackListCheck(billCheckList.contains(BillCheck.blackList.getValue() +""));
        //敏感词
        settleCheckVo.setSensitiveWordCheck(billCheckList.contains(BillCheck.sensitiveWord.getValue() +""));
        //账户支付限额
        settleCheckVo.setAccountLimit(billCheckList.contains(BillCheck.paymentLimit.getValue() +""));
        //关联内部账户可用余额
        settleCheckVo.setInternalAccountBalance(billCheckList.contains(BillCheck.availableBalance.getValue() +""));
        //高频交易
        settleCheckVo.setHighFrequencyTrade(billCheckList.contains(BillCheck.highFrequency.getValue() +""));
        //灰名单
        settleCheckVo.setGreyRollCheck(billCheckList.contains(BillCheck.grayList.getValue() + ""));
        //来源业务系统：现金管理 8
        settleCheckVo.setBizsyssrc((short)8);
        //资金组织
        settleCheckVo.setAccentity(foreignPayment.getAccentity());

        //伙伴黑灰名单需要：5资金付；6转账单；7外汇付款单
        settleCheckVo.setBillType((short) 7);

        //数据明细
        List<SettleReqVO> settleVoList = new ArrayList<>();
        SettleReqVO settleVo = new SettleReqVO();
        settleVo.setAccentity(foreignPayment.getAccentity());
        //明细id,新增时id可能为空，赋值一个临时id用来做判断
        if (foreignPayment.getId() == null){
            foreignPayment.setId(ymsOidGenerator.nextId());
        }
        settleVo.setBizBillDetailId(foreignPayment.getId().toString());
        //收付类型 1收2付。外汇付款 默认2
        settleVo.setReceipttypeb(2);
        //结算方式id
        settleVo.setSettlemode(foreignPayment.get("settlemode") !=null ?Long.parseLong(foreignPayment.get("settlemode").toString()):null);
        //本方银行账户id
        settleVo.setOurbankaccount(foreignPayment.getPaymenterprisebankaccount() != null ?foreignPayment.getPaymenterprisebankaccount():null);
        //原币币种id
        settleVo.setOriginalcurrency(foreignPayment.getCurrency());
        //原币交易金额
        settleVo.setOriginalcurrencyamt(new BigDecimal(foreignPayment.get("amount").toString()));
        //换出币种=原币
        settleVo.setExchangePaymentCurrency(foreignPayment.getCurrency());
        //换出金额=原币金额
        settleVo.setExchangePaymentAmount(new BigDecimal(foreignPayment.get("amount").toString()));
        //对方类型
        settleVo.setCounterpartytype(foreignPayment.getReceivetype().toString());
        //对方类型为其他时增加对公对私
        if(foreignPayment.get("receivetype")!= null && "4".equals(foreignPayment.get("receivetype").toString())){
            if (foreignPayment.get("publicorprivate") !=null && "1".equals(foreignPayment.get("publicorprivate").toString())){//对私
                settleVo.setDebitType((short)1);
            }
            if (foreignPayment.get("publicorprivate") !=null && "2".equals(foreignPayment.get("publicorprivate").toString())){//对公
                settleVo.setDebitType((short)2);
            }
        }

        //对方id
        settleVo.setCounterpartyid(foreignPayment.get("receivenameid") == null ? null :foreignPayment.get("receivenameid").toString());
        //对方名称
        settleVo.setCounterpartyname(foreignPayment.getReceivename());
        //对方银行账号id
        settleVo.setCounterpartybankaccount(foreignPayment.get("receivebankaccountid") == null ?null:foreignPayment.get("receivebankaccountid").toString());
        //对方账户名称
        settleVo.setCounterpartyaccname(foreignPayment.getReceivebankaccountname());
        //对方账号
        settleVo.setCounterpartybankacc(foreignPayment.getReceivebankaccount());
        //对方开户行名
        //需要根据开户行id查询一下
        String receivebankaddr = foreignPayment.getReceivebankaddr();
        if (ObjectUtils.isNotEmpty(receivebankaddr)) {
            BankdotVO bankdotVO = enterpriseBankQueryService.querybankNumberlinenumberById(receivebankaddr);
            String bankName = bankdotVO.getName();
            settleVo.setCounterpartybankname(bankName);
        }

        //来源业务id
        settleVo.setBizsyssrc("8");
        //本币金额
        settleVo.setNatAmt(new BigDecimal(foreignPayment.get("currencyamount").toString()));
        //备注
        settleVo.setDescription(foreignPayment.getDescription());
        //是否统收统支
        settleVo.setIncomeAndExpenditure(false);
        if(foreignPayment.get("quickType") != null){
            settleVo.setProceedType(foreignPayment.get("quickType").toString());
        }
        settleVoList.add(settleVo);
        settleCheckVo.setSettleVoList(settleVoList);
        //开通风险管理的，增加结算参数传递
        if (RiskEnvironment.isEnable()){
            //存放结算方式
            Map<Long, Integer> serviceAttrs = new HashMap<>();
            List<Long> serviceAttrIds = new ArrayList<>();
            for (SettleReqVO s : settleCheckVo.getSettleVoList()) {
                if (ObjectUtils.isNotEmpty(s.getSettlemode())) {
                    serviceAttrIds.add(s.getSettlemode());
                }
            }
            //获取结算方式
            if (ValueUtils.isNotEmpty(serviceAttrIds)) {
                serviceAttrs = cmCommonService.getServiceAttrs(serviceAttrIds);
            }
            settleCheckVo.setServiceAttrs(serviceAttrs);
        }

        return settleCheckVo;
    }


    /**
     * 处理风险检查结果
     * @param transferAccount
     * @param resJson
     * @param result
     * @return
     */
    private CtmJSONObject initTransferCheckResult(TransferAccount transferAccount,SettleCheckRespVO resJson,CtmJSONObject result,String action) throws Exception {
        //存在的风险项明细ID
        //疑重校验
        List<String> dignifiedIds = resJson.getDignifiedFailedList();
        //直联账户余额
        List<String> balanceIds = resJson.getAccBalanceCheckList();
        //非直联账户余额
        List<String> noDirAccBalances = resJson.getNoDirAccBalanceCheckList();
        //大额交易
        List<String> blockTradeIds = resJson.getTransactionFailedList();
        //黑名单
        List<String> blackIds = resJson.getBlackFailedList();
        //敏感词
        List<String> sensitiveWordIds = resJson.getResultSensitiveList();
        //账户支付限额
        List<String> paymentLimitIds = resJson.getAccountLimitList();
        //关联内部户可用余额
        List<String> availableBalanceIds = resJson.getInternalAccountList();
        //高频交易
        List<String> highFrequencyIds = resJson.getHighFrequencyList();
        //灰名单
        List<String> grayListIds = resJson.getGreyFailedList();

        //校验结果
        StringBuilder message = new StringBuilder();
        StringBuilder confirmMessage = new StringBuilder();
        //判断是否有对应的
        boolean isContainRisk = false;
        List<String> riskList = new ArrayList<>();
        List<String> riskTypeList = new ArrayList<>();
        List<String> riskControlList = new ArrayList<>();
        //控制类型 默认提示;1提示；2控制
        String confirmType = "1";
        //疑重校验
        if (CollectionUtils.isNotEmpty(dignifiedIds) && dignifiedIds.contains(transferAccount.getId().toString())){
            riskList.add(BillCheck.duplication.getName());
            riskTypeList.add(BillCheck.duplication.getValue() + "");
            //疑重校验没有是否控制的类型
        }
        //直联账户余额
        if (CollectionUtils.isNotEmpty(balanceIds) && balanceIds.contains(transferAccount.getId().toString())){
            riskList.add(BillCheck.balance.getName());
            riskTypeList.add(BillCheck.balance.getValue() + "");
            Map<String, Short> c = resJson.getAccBalanceCheckMap();
            if(c != null){
                Object cType = c.get(transferAccount.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.balance.getName());
                }
            }
        }
        //非直联账户余额
        if (CollectionUtils.isNotEmpty(noDirAccBalances) && noDirAccBalances.contains(transferAccount.getId().toString())) {
            riskList.add(BillCheck.noDirAccBalance.getName());
            riskTypeList.add(BillCheck.noDirAccBalance.getValue() + "");
            Map<String, Short> c = resJson.getNoDirAccBalanceCheckMap();
            if (c != null) {
                Object cType = c.get(transferAccount.getId().toString());
                if (cType != null && "2".equals(cType.toString())) {
                    confirmType = "2";
                    riskControlList.add(BillCheck.noDirAccBalance.getName());
                }
            }
        }
        //大额交易
        if (CollectionUtils.isNotEmpty(blockTradeIds) && blockTradeIds.contains(transferAccount.getId().toString())){
            riskList.add(BillCheck.blockTrade.getName());
            riskTypeList.add(BillCheck.blockTrade.getValue() + "");
            Map<String, Short> c = resJson.getTransactionFailedMap();
            if(c != null){
                Object cType = c.get(transferAccount.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.blockTrade.getName());
                }
            }
        }
        //黑名单
        if (CollectionUtils.isNotEmpty(blackIds) && blackIds.contains(transferAccount.getId().toString())){
            riskList.add(BillCheck.blackList.getName());
            riskTypeList.add(BillCheck.blackList.getValue() + "");
            Map<String, Short> c = resJson.getBlackFailedMap();
            if(c != null){
                Object cType = c.get(transferAccount.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.blackList.getName());
                }
            }
        }
        //敏感词
        if (CollectionUtils.isNotEmpty(sensitiveWordIds) && sensitiveWordIds.contains(transferAccount.getId().toString())){
            riskList.add(BillCheck.sensitiveWord.getName());
            riskTypeList.add(BillCheck.sensitiveWord.getValue() + "");
            Map<String, Short> c = resJson.getResultSensitiveMap();
            if(c != null){
                Object cType = c.get(transferAccount.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.sensitiveWord.getName());
                }
            }
        }
        //账户支付限额
        if (CollectionUtils.isNotEmpty(paymentLimitIds) && paymentLimitIds.contains(transferAccount.getId().toString())){
            riskList.add(BillCheck.paymentLimit.getName());
            riskTypeList.add(BillCheck.paymentLimit.getValue() + "");
            Map<String, Short> c = resJson.getAccountLimitMap();
            if(c != null){
                Object cType = c.get(transferAccount.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.paymentLimit.getName());
                }
            }
        }
        //关联内部户可用余额
        if (CollectionUtils.isNotEmpty(availableBalanceIds) && availableBalanceIds.contains(transferAccount.getId().toString())){
            riskList.add(BillCheck.availableBalance.getName());
            riskTypeList.add(BillCheck.availableBalance.getValue() + "");
            Map<String, Short> c = resJson.getInternalAccountMap();
            if(c != null){
                Object cType = c.get(transferAccount.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.availableBalance.getName());
                }
            }
        }
        //高频交易
        if (CollectionUtils.isNotEmpty(highFrequencyIds) && highFrequencyIds.contains(transferAccount.getId().toString())){
            riskList.add(BillCheck.highFrequency.getName());
            riskTypeList.add(BillCheck.highFrequency.getValue() + "");
            Map<String, Short> c = resJson.getHighFrequencyMap();
            if(c != null){
                Object cType = c.get(transferAccount.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.highFrequency.getName());
                }
            }
        }
        //灰名单
        if (CollectionUtils.isNotEmpty(grayListIds) && grayListIds.contains(transferAccount.getId().toString())){
            riskList.add(BillCheck.grayList.getName());
            riskTypeList.add(BillCheck.grayList.getValue() + "");
            Map<String, Short> c = resJson.getGreyFailedMap();
            if(c != null){
                Object cType = c.get(transferAccount.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.grayList.getName());
                }
            }
        }
        if (CollectionUtils.isNotEmpty(riskList)){
            message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00034", "转账单存在【%s】风险项。\r\n") /* "转账单存在【%s】风险项。\r\n" */,StringUtils.join(riskList,"，")));
            result.put("riskTypeList",StringUtils.join(riskTypeList,","));
            isContainRisk = true;
        }
        if (CollectionUtils.isNotEmpty(riskControlList)){
            confirmMessage.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F4F4405B00010","转账单存在【%s】风险，风险管控方式为控制。") /* "转账单存在【%s】风险，风险管控方式为控制。" */,StringUtils.join(riskControlList,"，")));
        }

        if (isContainRisk){
            //1代表有风险
            result.put("checkFlag","1");
            result.put("checkMsg",message.toString());
            result.put("confirmMessage",confirmMessage.toString());
            //1:提示；2控制
            String code = result.getString("riskExecuteResultCode");
            if(StringUtils.isNotEmpty(code)){
                if(RiskStrategyEnum.INTERCEPT.getCode().equals(code)){
                    confirmType = "2";
                    result.put("confirmMessage",result.get("checkMsg"));
                }else if(RiskStrategyEnum.WARNING.getCode().equals(code)){
                    confirmType = "1";
                }
            }
            result.put("confirmType",confirmType);
        }else {
            result.put("checkFlag","0");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400713", "不存在风险！") /* "不存在风险！" */);
        }
        return result;
    }

    /**
     * 处理外汇付款 风险检查结果*
     * @param foreignPayment
     * @param resJson
     * @param result
     * @param action
     * @return
     * @throws Exception
     */
    private CtmJSONObject initForeignpaymentCheckResult(ForeignPayment foreignPayment,SettleCheckRespVO resJson,CtmJSONObject result,String action) throws Exception {
        //存在的风险项明细ID
        //疑重校验
        List<String> dignifiedIds = resJson.getDignifiedFailedList();
        //直联账户余额
        List<String> balanceIds = resJson.getAccBalanceCheckList();
        //非直联账户余额
        List<String> noDirAccBalances = resJson.getNoDirAccBalanceCheckList();
        //大额交易
        List<String> blockTradeIds = resJson.getTransactionFailedList();
        //黑名单
        List<String> blackIds = resJson.getBlackFailedList();
        //敏感词
        List<String> sensitiveWordIds = resJson.getResultSensitiveList();
        //账户支付限额
        List<String> paymentLimitIds = resJson.getAccountLimitList();
        //关联内部户可用余额
        List<String> availableBalanceIds = resJson.getInternalAccountList();
        //高频交易
        List<String> highFrequencyIds = resJson.getHighFrequencyList();
        //灰名单
        List<String> grayListIds = resJson.getGreyFailedList();

        //校验结果
        StringBuilder message = new StringBuilder();
        StringBuilder confirmMessage = new StringBuilder();
        //判断是否有对应的
        boolean isContainRisk = false;
        List<String> riskList = new ArrayList<>();
        List<String> riskTypeList = new ArrayList<>();
        List<String> riskControlList = new ArrayList<>();
        //控制类型 默认提示;1提示；2控制
        String confirmType = "1";
        //疑重校验
        if (CollectionUtils.isNotEmpty(dignifiedIds) && dignifiedIds.contains(foreignPayment.getId().toString())){
            riskList.add(BillCheck.duplication.getName());
            riskTypeList.add(BillCheck.duplication.getValue() + "");
            //疑重校验没有是否控制的类型
        }
        //直联账户余额
        if (CollectionUtils.isNotEmpty(balanceIds) && balanceIds.contains(foreignPayment.getId().toString())){
            riskList.add(BillCheck.balance.getName());
            riskTypeList.add(BillCheck.balance.getValue() + "");
            Map<String, Short> c = resJson.getAccBalanceCheckMap();
            if(c != null){
                Object cType = c.get(foreignPayment.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.balance.getName());
                }
            }
        }
        //非直联账户余额
        if (CollectionUtils.isNotEmpty(noDirAccBalances) && noDirAccBalances.contains(foreignPayment.getId().toString())) {
            riskList.add(BillCheck.noDirAccBalance.getName());
            riskTypeList.add(BillCheck.noDirAccBalance.getValue() + "");
            Map<String, Short> c = resJson.getNoDirAccBalanceCheckMap();
            if (c != null) {
                Object cType = c.get(foreignPayment.getId().toString());
                if (cType != null && "2".equals(cType.toString())) {
                    confirmType = "2";
                    riskControlList.add(BillCheck.noDirAccBalance.getName());
                }
            }
        }
        //大额交易
        if (CollectionUtils.isNotEmpty(blockTradeIds) && blockTradeIds.contains(foreignPayment.getId().toString())){
            riskList.add(BillCheck.blockTrade.getName());
            riskTypeList.add(BillCheck.blockTrade.getValue() + "");
            Map<String, Short> c = resJson.getTransactionFailedMap();
            if(c != null){
                Object cType = c.get(foreignPayment.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.blockTrade.getName());
                }
            }
        }
        //黑名单
        if (CollectionUtils.isNotEmpty(blackIds) && blackIds.contains(foreignPayment.getId().toString())){
            riskList.add(BillCheck.blackList.getName());
            riskTypeList.add(BillCheck.blackList.getValue() + "");
            Map<String, Short> c = resJson.getBlackFailedMap();
            if(c != null){
                Object cType = c.get(foreignPayment.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.blackList.getName());
                }
            }
        }
        //敏感词
        if (CollectionUtils.isNotEmpty(sensitiveWordIds) && sensitiveWordIds.contains(foreignPayment.getId().toString())){
            riskList.add(BillCheck.sensitiveWord.getName());
            riskTypeList.add(BillCheck.sensitiveWord.getValue() + "");
            Map<String, Short> c = resJson.getResultSensitiveMap();
            if(c != null){
                Object cType = c.get(foreignPayment.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.sensitiveWord.getName());
                }
            }
        }
        //账户支付限额
        if (CollectionUtils.isNotEmpty(paymentLimitIds) && paymentLimitIds.contains(foreignPayment.getId().toString())){
            riskList.add(BillCheck.paymentLimit.getName());
            riskTypeList.add(BillCheck.paymentLimit.getValue() + "");
            Map<String, Short> c = resJson.getAccountLimitMap();
            if(c != null){
                Object cType = c.get(foreignPayment.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.paymentLimit.getName());
                }
            }
        }
        //关联内部户可用余额
        if (CollectionUtils.isNotEmpty(availableBalanceIds) && availableBalanceIds.contains(foreignPayment.getId().toString())){
            riskList.add(BillCheck.availableBalance.getName());
            riskTypeList.add(BillCheck.availableBalance.getValue() + "");
            Map<String, Short> c = resJson.getInternalAccountMap();
            if(c != null){
                Object cType = c.get(foreignPayment.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.availableBalance.getName());
                }
            }
        }
        //高频交易
        if (CollectionUtils.isNotEmpty(highFrequencyIds) && highFrequencyIds.contains(foreignPayment.getId().toString())){
            riskList.add(BillCheck.highFrequency.getName());
            riskTypeList.add(BillCheck.highFrequency.getValue() + "");
            Map<String, Short> c = resJson.getHighFrequencyMap();
            if(c != null){
                Object cType = c.get(foreignPayment.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.highFrequency.getName());
                }
            }
        }
        //灰名单
        if (CollectionUtils.isNotEmpty(grayListIds) && grayListIds.contains(foreignPayment.getId().toString())){
            riskList.add(BillCheck.grayList.getName());
            riskTypeList.add(BillCheck.grayList.getValue() + "");
            Map<String, Short> c = resJson.getGreyFailedMap();
            if(c != null){
                Object cType = c.get(foreignPayment.getId().toString());
                if (cType!=null && "2".equals(cType.toString())){
                    confirmType = "2";
                    riskControlList.add(BillCheck.grayList.getName());
                }
            }
        }
        if (CollectionUtils.isNotEmpty(riskList)){
            message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A67BE2804C80001", "外汇付款存在【%s】风险项。") /* "外汇付款存在【%s】风险项。" */,StringUtils.join(riskList,"，")));
            result.put("riskTypeList",StringUtils.join(riskTypeList,","));
            isContainRisk = true;
        }
        if (CollectionUtils.isNotEmpty(riskControlList)){
            confirmMessage.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A67BDEC04C80004","外汇付款存在【%s】风险，风险管控方式为控制。") /* "外汇付款存在【%s】风险，风险管控方式为控制。" */,StringUtils.join(riskControlList,"，")));
        }

        if (isContainRisk){
            //1代表有风险
            result.put("checkFlag","1");
            result.put("checkMsg",message.toString());
            result.put("confirmMessage",confirmMessage.toString());
            //1:提示；2控制
            String code = result.getString("riskExecuteResultCode");
            if(StringUtils.isNotEmpty(code)){
                if(RiskStrategyEnum.INTERCEPT.getCode().equals(code)){
                    confirmType = "2";
                    result.put("confirmMessage",result.get("checkMsg"));
                }else if(RiskStrategyEnum.WARNING.getCode().equals(code)){
                    confirmType = "1";
                }
            }
            result.put("confirmType",confirmType);
        }else {
            result.put("checkFlag","0");
            result.put("checkMsg",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400713", "不存在风险！") /* "不存在风险！" */);
        }

        return result;
    }


    /**
     * 解析风险控制返回的结果
     * @param riskExecuteResult
     * @return
     */
    private SettleCheckRespVO initRiskCheckResult(RiskExecuteResult riskExecuteResult){
        SettleCheckRespVO respVO = new SettleCheckRespVO();
        if (riskExecuteResult.getExtendDatas() == null || riskExecuteResult.getExtendDatas().isEmpty()){
            return respVO;
        }

        //解析结果，返回值
        List<SettleCheckRespVO> settleCheckRespVOList = riskExecuteResult.getExtendDatas().
                stream().filter(Objects::nonNull).map(s->CtmJSONObject.parseObject(s.toString(),SettleCheckRespVO.class)).collect(Collectors.toList());
        for (SettleCheckRespVO s: settleCheckRespVOList){
            //疑重校验
            if (CollectionUtils.isNotEmpty(s.getDignifiedFailedList())){
                respVO.setDignifiedFailedList(s.getDignifiedFailedList());
            }
            //账户余额
            if (CollectionUtils.isNotEmpty(s.getAccBalanceCheckList())){
                //受控数据id
                respVO.setAccBalanceCheckList(s.getAccBalanceCheckList());
                //控制类型map
                respVO.setAccBalanceCheckMap(s.getAccBalanceCheckMap());
            }
            //非直连账户余额
            if (CollectionUtils.isNotEmpty(s.getNoDirAccBalanceCheckList())){
                //受控数据id
                respVO.setNoDirAccBalanceCheckList(s.getNoDirAccBalanceCheckList());
                //控制类型map
                respVO.setNoDirAccBalanceCheckMap(s.getNoDirAccBalanceCheckMap());
            }
            //大额交易
            if (CollectionUtils.isNotEmpty(s.getTransactionFailedList())){
                //受控数据id
                respVO.setTransactionFailedList(s.getTransactionFailedList());
                //控制类型map
                respVO.setTransactionFailedMap(s.getTransactionFailedMap());
            }
            //黑名单
            if (CollectionUtils.isNotEmpty(s.getBlackFailedList())){
                //受控数据id
                respVO.setBlackFailedList(s.getBlackFailedList());
                //控制类型map
                respVO.setBlackFailedMap(s.getBlackFailedMap());
            }
            //敏感词
            if (CollectionUtils.isNotEmpty(s.getResultSensitiveList())){
                //受控数据id
                respVO.setResultSensitiveList(s.getResultSensitiveList());
                //控制类型map
                respVO.setResultSensitiveMap(s.getResultSensitiveMap());
            }
            //账户支付限额
            if (CollectionUtils.isNotEmpty(s.getAccountLimitList())){
                //受控数据id
                respVO.setAccountLimitList(s.getAccountLimitList());
                //控制类型map
                respVO.setAccountLimitMap(s.getAccountLimitMap());
            }
            //关联内部户可用余额
            if (CollectionUtils.isNotEmpty(s.getInternalAccountList())){
                //受控数据id
                respVO.setInternalAccountList(s.getInternalAccountList());
                //控制类型map
                respVO.setInternalAccountMap(s.getInternalAccountMap());
            }
            //高频交易
            if (CollectionUtils.isNotEmpty(s.getHighFrequencyList())){
                //受控数据id
                respVO.setHighFrequencyList(s.getHighFrequencyList());
                //控制类型map
                respVO.setHighFrequencyMap(s.getHighFrequencyMap());
            }
            //灰名单
            if (CollectionUtils.isNotEmpty(s.getGreyFailedList())){
                //受控数据id
                respVO.setGreyFailedList(s.getGreyFailedList());
                //控制类型map
                respVO.setGreyFailedMap(s.getGreyFailedMap());
            }
        }

        return respVO;
    }
}
