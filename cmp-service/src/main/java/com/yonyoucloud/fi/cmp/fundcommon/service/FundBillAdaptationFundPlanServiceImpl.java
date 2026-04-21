package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.cspl.vo.response.CapitalPlanExecuteResp;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.PushCsplStatusEnum;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;

/**
 * <h1>资金收付款单更新资金计划接口实现类</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-29 10:50
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class FundBillAdaptationFundPlanServiceImpl implements FundBillAdaptationFundPlanService {

    public static final String FUND_PAYMENT_B_FULLNAME = "cmp.fundpayment.FundPayment_b";
    public static final String FUND_COLLECTION_B_FULLNAME = "cmp.fundcollection.FundCollection_b";

    @Resource
    private CmCommonService<Object> commonService;


    /**
     * <h2>资金付款单提交占用资金计划</h2>
     *
     * @param fundPayment : 资金付款单数据
     * @author Sun GuoCai
     * @since 2024/5/29 11:25
     */
    @Override
    public void fundPaymentSubmitEmployFundPlan(FundPayment fundPayment) throws Exception {
        List<BizObject> checkFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> fundPaymentBList = fundPayment.get("FundPayment_b");
        for (BizObject biz : fundPaymentBList) {
            if (biz.get("entrustReject") != null && biz.getInteger("entrustReject") == 1) {
                continue;
            }
            Short settleStatus = biz.getShort("settlestatus");
            if (settleStatus == null || settleStatus == FundSettleStatus.Refund.getValue()) {
                continue;
            }
            if (biz.get(ICmpConstant.FUND_PLAN_PROJECT) != null) {
                biz.set(ICmpConstant.IS_TO_PUSH_CSPL, 1);
                checkFundBillForFundPlanProjectList.add(biz);
            }
        }
        if (CollectionUtils.isNotEmpty(checkFundBillForFundPlanProjectList)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("1.fundPayment", "fundPayment");
            jsonObject.put("2.checkFundBillForFundPlanProjectList", checkFundBillForFundPlanProjectList);
            Map<String, Object> map = new HashMap<>();
            map.put(ICmpConstant.ACCENTITY, fundPayment.get(ICmpConstant.ACCENTITY));
            map.put(ICmpConstant.VOUCHDATE, fundPayment.get(ICmpConstant.VOUCHDATE));
            map.put(ICmpConstant.CODE, fundPayment.get(ICmpConstant.CODE));
            jsonObject.put("3.map", map);
            List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameter(checkFundBillForFundPlanProjectList, IStwbConstant.EMPLOY, IBillNumConstant.FUND_PAYMENT, map);
            jsonObject.put("4.checkObject", checkObject);
            if (ValueUtils.isNotEmptyObj(checkObject)) {
                CapitalPlanExecuteResp capitalPlanExecuteResp;
                try {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                    jsonObject.put("5.capitalPlanExecuteResp#employAndrelease", capitalPlanExecuteResp);
                    if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
                            && "500".equals(capitalPlanExecuteResp.getCode())
                            && capitalPlanExecuteResp.getSuccessCount() == 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102023"),capitalPlanExecuteResp.getMessage().toString());
                    }
                    List<Map<String, Object>> paramList = new ArrayList<>();
                    for (BizObject bizObject : checkFundBillForFundPlanProjectList) {
                        /*Map<String, Object> params = new HashMap<>();
                        params.put("ytenantId", InvocationInfoProxy.getTenantid());
                        params.put("id", bizObject.getId());
                        params.put("tableName", "cmp_fundpayment_b");
                        params.put("isToPushCspl", 1);
                        paramList.add(params);
                        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateFundBillSubById", params);*/
                        FundPayment_b fundPaymentB = new FundPayment_b();
                        fundPaymentB.setId(bizObject.getId());
                        fundPaymentB.setIsToPushCspl(1);
                        fundPaymentB.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FUND_PAYMENT_B_FULLNAME,fundPaymentB);
                    }
                    jsonObject.put("6.paramList", paramList);
                } catch (Exception e) {
                    log.error("fundPaymentSubmitEmployFundPlan error, errorMsg={}", e.getMessage());
                    jsonObject.put("7.errorMsg", e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
                } finally {
                    jsonObject.put("8.method",
                            "com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanServiceImpl.fundPaymentSubmitEmployFundPlan#employ");
                    CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                    ctmcmpBusinessLogService.saveBusinessLog(
                            jsonObject,
                            fundPayment.getString(ICmpConstant.CODE),
                            IMsgConstant.FUND_PAYMENT_EMPLOY_AND_RELEASE_FUND_PLAN,
                            IServicecodeConstant.FUNDPAYMENT,
                            IMsgConstant.FUND_PAYMENT,
                            OperCodeTypes.lock.getDefaultOperateName());
                }

            }
        }
    }

    /**
     * <h2>资金付款单提交占用资金计划</h2>
     *
     * @param fundCollection : 资金收款单数据
     * @author Sun GuoCai
     * @since 2024/5/29 11:25
     */
    @Override
    public void fundCollectionSubmitEmployFundPlan(FundCollection fundCollection) throws Exception {
        List<BizObject> checkFundBillForFundPlanProjectList = new ArrayList<>();
        assert fundCollection != null;
        List<BizObject> fundCollectionBList = fundCollection.get("FundCollection_b");
        for (BizObject biz : fundCollectionBList) {
            if (biz.get("entrustReject") != null && biz.getInteger("entrustReject") == 1) {
                continue;
            }
            Short settleStatus = biz.getShort("settlestatus");
            if (settleStatus == null || settleStatus == FundSettleStatus.Refund.getValue()) {
                continue;
            }
            if (biz.get(ICmpConstant.FUND_PLAN_PROJECT) != null) {
                biz.set(ICmpConstant.IS_TO_PUSH_CSPL, 1);
                checkFundBillForFundPlanProjectList.add(biz);
            }
        }
        if (CollectionUtils.isNotEmpty(checkFundBillForFundPlanProjectList)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("1.fundPayment", "fundPayment");
            jsonObject.put("2.checkFundBillForFundPlanProjectList", checkFundBillForFundPlanProjectList);
            Map<String, Object> map = new HashMap<>();
            map.put(ICmpConstant.ACCENTITY, fundCollection.get(ICmpConstant.ACCENTITY));
            map.put(ICmpConstant.VOUCHDATE, fundCollection.get(ICmpConstant.VOUCHDATE));
            map.put(ICmpConstant.CODE, fundCollection.get(ICmpConstant.CODE));
            jsonObject.put("3.map", map);
            List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameter(checkFundBillForFundPlanProjectList, IStwbConstant.EMPLOY, IBillNumConstant.FUND_COLLECTION, map);
            jsonObject.put("4.checkObject", checkObject);
            if (ValueUtils.isNotEmptyObj(checkObject)) {
                CapitalPlanExecuteResp capitalPlanExecuteResp;
                try {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                    jsonObject.put("5.capitalPlanExecuteResp#employAndrelease", capitalPlanExecuteResp);
                    if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
                            && "500".equals(capitalPlanExecuteResp.getCode())
                            && capitalPlanExecuteResp.getSuccessCount() == 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102025"),capitalPlanExecuteResp.getMessage().toString());
                    }
                    List<Map<String, Object>> paramList = new ArrayList<>();
                    for (BizObject bizObject : checkFundBillForFundPlanProjectList) {
                        /*Map<String, Object> params = new HashMap<>();
                        params.put("ytenantId", InvocationInfoProxy.getTenantid());
                        params.put("id", bizObject.getId());
                        params.put("tableName", "cmp_fundcollection_b");
                        params.put("isToPushCspl", 1);
                        paramList.add(params);
                        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateFundBillSubById", params);*/
                        FundCollection_b fundCollection_b = new FundCollection_b();
                        fundCollection_b.setId(bizObject.getId());
                        fundCollection_b.setIsToPushCspl(1);
                        fundCollection_b.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FUND_COLLECTION_B_FULLNAME,fundCollection_b);
                    }
                    jsonObject.put("6.paramList", paramList);
                } catch (Exception e) {
                    log.error("fundCollectionSubmitEmployFundPlan error, errorMsg={}", e.getMessage());
                    jsonObject.put("7.errorMsg", e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
                } finally {
                    jsonObject.put("8.method",
                            "com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanServiceImpl.fundCollectionSubmitEmployFundPlan#employ");
                    // 记录业务日志，用于排查问题
                    CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                    ctmcmpBusinessLogService.saveBusinessLog(
                            jsonObject,
                            fundCollection.getString(ICmpConstant.CODE),
                            IMsgConstant.FUND_COLLECTION_EMPLOY_AND_RELEASE_FUND_PLAN,
                            IServicecodeConstant.FUNDCOLLECTION,
                            IMsgConstant.FUND_COLLECTION,
                            OperCodeTypes.lock.getDefaultOperateName());
                }

            }
        }
    }

    @Override
    public void fundCollectionUnSubmitReleaseFundPlan(FundCollection fundCollection) throws Exception {
        List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> fundCollectionBList = fundCollection.get("FundCollection_b");
        for (BizObject biz : fundCollectionBList) {
            Object isToPushCspl = biz.get("isToPushCspl");
            if (ValueUtils.isNotEmptyObj(biz.get(ICmpConstant.FUND_PLAN_PROJECT))
                    && ValueUtils.isNotEmptyObj(isToPushCspl)
                    && 1 == Integer.parseInt(isToPushCspl.toString())) {
                biz.set("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                releaseFundBillForFundPlanProjectList.add(biz);
            }
        }
        if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("1.fundPayment", "fundPayment");
            jsonObject.put("2.releaseFundBillForFundPlanProjectList", releaseFundBillForFundPlanProjectList);
            Map<String, Object> map = new HashMap<>();
            map.put("accentity", fundCollection.get("accentity"));
            map.put("vouchdate", fundCollection.get("vouchdate"));
            map.put("code", fundCollection.get("code"));
            jsonObject.put("3.map", map);
            List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameter(releaseFundBillForFundPlanProjectList, IStwbConstant.RELEASE, IBillNumConstant.FUND_COLLECTION, map);
            jsonObject.put("4.checkObject", checkObject);
            if (ValueUtils.isNotEmptyObj(checkObject)) {
                CapitalPlanExecuteResp capitalPlanExecuteResp;
                try {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                    jsonObject.put("5.capitalPlanExecuteResp", capitalPlanExecuteResp);
                    if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
                            && "500".equals(capitalPlanExecuteResp.getCode())
                            && capitalPlanExecuteResp.getSuccessCount() == 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + capitalPlanExecuteResp.getMessage().toString());
                    }
                    List<Map<String, Object>> paramList = new ArrayList<>();
                    for (BizObject bizObject : releaseFundBillForFundPlanProjectList) {
                       /* Map<String, Object> params = new HashMap<>();
                        params.put("ytenantId", InvocationInfoProxy.getTenantid());
                        params.put("id", bizObject.getId());
                        params.put("tableName", "cmp_fundcollection_b");
                        params.put("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                        paramList.add(params);
                        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateFundBillSubById", params);*/
                        FundCollection_b fundCollection_b = new FundCollection_b();
                        fundCollection_b.setId(bizObject.getId());
                        fundCollection_b.setIsToPushCspl(PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                        fundCollection_b.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FUND_COLLECTION_B_FULLNAME,fundCollection_b);
                    }
                    jsonObject.put("6.paramList", paramList);
                } catch (Exception e) {
                    log.error("fundCollectionUnSubmitReleaseFundPlan error, errorMsg={}", e.getMessage());
                    jsonObject.put("7.errorMsg", e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
                } finally {
                    jsonObject.put("8.method",
                            "com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanServiceImpl.fundCollectionUnSubmitReleaseFundPlan#release");
                    CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                    ctmcmpBusinessLogService.saveBusinessLog(
                            jsonObject,
                            fundCollection.getString(ICmpConstant.CODE),
                            IMsgConstant.FUND_COLLECTION_EMPLOY_AND_RELEASE_FUND_PLAN,
                            IServicecodeConstant.FUNDCOLLECTION,
                            IMsgConstant.FUND_COLLECTION,
                            OperCodeTypes.unlock.getDefaultOperateName());
                }
            }
        }
    }

    @Override
    public void fundPaymentUnSubmitReleaseFundPlan(FundPayment fundPayment) throws Exception {
        List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> fundCollectionBList = fundPayment.get("FundPayment_b");
        for (BizObject biz : fundCollectionBList) {
            Object isToPushCspl = biz.get(ICmpConstant.IS_TO_PUSH_CSPL);
            if (ValueUtils.isNotEmptyObj(biz.get(ICmpConstant.FUND_PLAN_PROJECT))
                    && ValueUtils.isNotEmptyObj(isToPushCspl)
                    && 1 == Integer.parseInt(isToPushCspl.toString())) {
                biz.set(ICmpConstant.IS_TO_PUSH_CSPL, PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                releaseFundBillForFundPlanProjectList.add(biz);
            }
        }
        if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("1.fundPayment", "fundPayment");
            jsonObject.put("2.releaseFundBillForFundPlanProjectList", releaseFundBillForFundPlanProjectList);
            Map<String, Object> map = new HashMap<>();
            map.put(ICmpConstant.ACCENTITY, fundPayment.get(ICmpConstant.ACCENTITY));
            map.put(ICmpConstant.VOUCHDATE, fundPayment.get(ICmpConstant.VOUCHDATE));
            map.put(ICmpConstant.CODE, fundPayment.get(ICmpConstant.CODE));
            jsonObject.put("3.map", map);
            List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameter(releaseFundBillForFundPlanProjectList, IStwbConstant.RELEASE, IBillNumConstant.FUND_PAYMENT, map);
            jsonObject.put("4.checkObject", checkObject);
            if (ValueUtils.isNotEmptyObj(checkObject)) {
                CapitalPlanExecuteResp capitalPlanExecuteResp;
                try {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                    jsonObject.put("5.capitalPlanExecuteResp", capitalPlanExecuteResp);
                    if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
                            && "500".equals(capitalPlanExecuteResp.getCode())
                            && capitalPlanExecuteResp.getSuccessCount() == 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + capitalPlanExecuteResp.getMessage().toString());
                    }
                    List<Map<String, Object>> paramList = new ArrayList<>();
                    for (BizObject bizObject : releaseFundBillForFundPlanProjectList) {
                        /*Map<String, Object> params = new HashMap<>();
                        params.put("ytenantId", InvocationInfoProxy.getTenantid());
                        params.put("id", bizObject.getId());
                        params.put("tableName", "cmp_fundpayment_b");
                        params.put("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                        paramList.add(params);
                        SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateFundBillSubById", params);*/
                        FundPayment_b fundPaymentB = new FundPayment_b();
                        fundPaymentB.setId(bizObject.getId());
                        fundPaymentB.setIsToPushCspl(PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                        fundPaymentB.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(FUND_PAYMENT_B_FULLNAME,fundPaymentB);
                    }
                    jsonObject.put("6.paramList", paramList);
                } catch (Exception e) {
                    log.error("fundPaymentUnSubmitReleaseFundPlan error, errorMsg={}", e.getMessage());
                    jsonObject.put("7.errorMsg", e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
                } finally {
                    jsonObject.put("8.method", "com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanServiceImpl.fundPaymentUnSubmitReleaseFundPlan#release");
                    CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                    ctmcmpBusinessLogService.saveBusinessLog(
                            jsonObject,
                            fundPayment.getString(ICmpConstant.CODE),
                            IMsgConstant.FUND_PAYMENT_EMPLOY_AND_RELEASE_FUND_PLAN,
                            IServicecodeConstant.FUNDPAYMENT,
                            IMsgConstant.FUND_PAYMENT,
                            OperCodeTypes.unlock.getDefaultOperateName());
                }
            }
        }
    }


    @Override
    public void fundBillEditEmployOrReleaseFundPlan(
            String billnum,
            BizObject bizObject,
            List<BizObject> employFundBillForFundPlanProjectList,
            List<BizObject> releaseFundBillForFundPlanProjectList,
            List<BizObject> preEmployFundBillForFundPlanProjectList,
            List<BizObject> preReleaseFundBillForFundPlanProjectList
    ) throws Exception {
        List<BizObject> fundSubBList = null;
        BizObject originFund = null;
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            fundSubBList = bizObject.get("FundPayment_b");
            originFund = MetaDaoHelper.findById(FundPayment.ENTITY_NAME,bizObject.getId(),3);
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            fundSubBList = bizObject.get("FundCollection_b");
            originFund = MetaDaoHelper.findById(FundCollection.ENTITY_NAME,bizObject.getId(),3);
        }
        boolean notOpenBill = Short.parseShort(bizObject.get("verifystate").toString()) != VerifyState.INIT_NEW_OPEN.getValue()
                && Short.parseShort(bizObject.get("verifystate").toString()) != VerifyState.REJECTED_TO_MAKEBILL.getValue();
        assert fundSubBList != null;
        for (BizObject subObj : fundSubBList) {
            if (notOpenBill && "Update".equals(bizObject.get("_status").toString()) && "Update".equals(subObj.get("_status").toString())) {
                if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                    Object id = subObj.get("id");
                    String fundPlanProjectPage = ValueUtils.isNotEmptyObj(subObj.get("fundPlanProject"))
                            ? subObj.getString("fundPlanProject") : null;
                    short settleStatusPage = ValueUtils.isNotEmptyObj(subObj.getShort("settlestatus"))
                            ? subObj.getShort("settlestatus") : null;
                    //FundPayment_b fundPayment_b = MetaDaoHelper.findById(FundPayment_b.ENTITY_NAME, id, 1);
                    List<FundPayment_b> originFundPayment_bList = originFund.get("FundPayment_b");
                    FundPayment_b fundPayment_b = originFundPayment_bList.stream().filter(item->item.getId().toString().equals(subObj.getId().toString())).findFirst().orElse(null);
                    String fundPlanProjectDB = ValueUtils.isNotEmptyObj(fundPayment_b.getString("fundPlanProject"))
                            ? fundPayment_b.getString("fundPlanProject") : null;
                    short settleStatusDB = ValueUtils.isNotEmptyObj(fundPayment_b.getFundSettlestatus())
                            ? fundPayment_b.getFundSettlestatus().getValue() : null;

                    boolean isNotRefund = settleStatusPage != FundSettleStatus.Refund.getValue() && settleStatusDB != FundSettleStatus.Refund.getValue();
                    BigDecimal oriSumPage = subObj.getBigDecimal(ICmpConstant.ORISUM);
                    BigDecimal oriSumDB = fundPayment_b.getOriSum();
                    if (!Objects.equals(fundPlanProjectPage, fundPlanProjectDB) && isNotRefund) {
                        if (fundPlanProjectPage != null && fundPlanProjectDB != null) {
                            subObj.put("isToPushCspl", 1);
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                                employFundBillForFundPlanProjectList.add(subObj);
                            }
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                releaseFundBillForFundPlanProjectList.add(fundPayment_b);
                                preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                            }
                        }
                        if (fundPlanProjectPage == null) {
                            subObj.put("isToPushCspl", 0);
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                releaseFundBillForFundPlanProjectList.add(fundPayment_b);
                                preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                            }
                        }
                        if (fundPlanProjectPage != null && fundPlanProjectDB == null) {
                            subObj.put("isToPushCspl", 1);
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                                employFundBillForFundPlanProjectList.add(subObj);
                            }
                        }
                    }

                    String fundPlanProjectDetailPage = ValueUtils.isNotEmptyObj(subObj.get("fundPlanProjectDetail"))
                            ? subObj.getString("fundPlanProjectDetail") : null;
                    String fundPlanProjectDetailDB = ValueUtils.isNotEmptyObj(fundPayment_b.getString("fundPlanProjectDetail"))
                            ? fundPayment_b.getString("fundPlanProjectDetail") : null;
                    if (Objects.equals(fundPlanProjectPage, fundPlanProjectDB)
                            && !Objects.equals(fundPlanProjectDetailPage, fundPlanProjectDetailDB)
                            && isNotRefund) {
                        if (fundPlanProjectDetailPage != null && fundPlanProjectDetailDB != null) {
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                                employFundBillForFundPlanProjectList.add(subObj);
                            }
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                releaseFundBillForFundPlanProjectList.add(fundPayment_b);
                                preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                            }
                        }
                        if (fundPlanProjectPage == null) {
                            subObj.put("isToPushCspl", 2);
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                releaseFundBillForFundPlanProjectList.add(fundPayment_b);
                                preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                            }
                        }
                        if (fundPlanProjectPage != null && fundPlanProjectDB == null) {
                            subObj.put("isToPushCspl", 1);
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                                employFundBillForFundPlanProjectList.add(subObj);
                            }
                        }
                    }

                    if (!isNotRefund) {
                        if (settleStatusPage == FundSettleStatus.Refund.getValue()
                                && settleStatusDB != FundSettleStatus.Refund.getValue()
                                && fundPlanProjectDB != null
                                && (BigDecimal.ZERO.compareTo(oriSumDB) < 0)) {
                            releaseFundBillForFundPlanProjectList.add(fundPayment_b);
                        }
                        if (settleStatusPage != FundSettleStatus.Refund.getValue()
                                && settleStatusDB == FundSettleStatus.Refund.getValue()
                                && fundPlanProjectPage != null
                                && (BigDecimal.ZERO.compareTo(oriSumDB) < 0)) {
                            employFundBillForFundPlanProjectList.add(subObj);
                        }
                    }
                    // 当在审批流审批时，编辑单据，占用了资金计划，并且修改了金额，这时释放原金额单据，占用修改后的金额
                    boolean isModifyOriSumWithFundPlanProjectNotChange =
                            !Objects.equals(fundPlanProjectPage, null) && !Objects.equals(fundPlanProjectDetailPage, null) &&
                                    Objects.equals(fundPlanProjectPage, fundPlanProjectDB) && Objects.equals(fundPlanProjectDetailPage, fundPlanProjectDetailDB);
                    if (isModifyOriSumWithFundPlanProjectNotChange && (oriSumPage.compareTo(oriSumDB) != 0)) {
                        fundPayment_b.setIsToPushCspl(2);
                        if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                            releaseFundBillForFundPlanProjectList.add(fundPayment_b);
                            preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                        }
                        subObj.put("isToPushCspl", 1);
                        if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                            employFundBillForFundPlanProjectList.add(subObj);
                            preEmployFundBillForFundPlanProjectList.add(subObj);
                        }
                    }
                }
                if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
                    Object id = subObj.get("id");
                    String fundPlanProjectPage = ValueUtils.isNotEmptyObj(subObj.get("fundPlanProject"))
                            ? subObj.getString("fundPlanProject") : null;
                    //FundCollection_b fundCollection_b = MetaDaoHelper.findById(FundCollection_b.ENTITY_NAME, id, 1);
                    List<FundCollection_b> originFundCollection_bList = originFund.get("FundCollection_b");
                    FundCollection_b fundCollection_b = originFundCollection_bList.stream().filter(item->item.getId().toString().equals(subObj.getId().toString())).findFirst().orElse(null);
                    String fundPlanProjectDB = ValueUtils.isNotEmptyObj(fundCollection_b.getString("fundPlanProject"))
                            ? fundCollection_b.getString("fundPlanProject") : null;
                    BigDecimal oriSumPage = subObj.getBigDecimal(ICmpConstant.ORISUM);
                    BigDecimal oriSumDB = fundCollection_b.getOriSum();
                    if (!Objects.equals(fundPlanProjectPage, fundPlanProjectDB)) {
                        if (fundPlanProjectPage != null && fundPlanProjectDB != null) {
                            subObj.put("isToPushCspl", 1);
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                                employFundBillForFundPlanProjectList.add(subObj);
                            }
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                releaseFundBillForFundPlanProjectList.add(fundCollection_b);
                                preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                            }
                        }
                        if (fundPlanProjectPage == null) {
                            subObj.put("isToPushCspl", 0);
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                releaseFundBillForFundPlanProjectList.add(fundCollection_b);
                                preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                            }
                        }
                        if (fundPlanProjectPage != null && fundPlanProjectDB == null) {
                            subObj.put("isToPushCspl", 1);
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                                employFundBillForFundPlanProjectList.add(subObj);
                            }
                        }
                    }


                    String fundPlanProjectDetailPage = ValueUtils.isNotEmptyObj(subObj.get("fundPlanProjectDetail"))
                            ? subObj.getString("fundPlanProjectDetail") : null;
                    String fundPlanProjectDetailDB = ValueUtils.isNotEmptyObj(fundCollection_b.getString("fundPlanProjectDetail"))
                            ? fundCollection_b.getString("fundPlanProjectDetail") : null;
                    if (Objects.equals(fundPlanProjectPage, fundPlanProjectDB)
                            && !Objects.equals(fundPlanProjectDetailPage, fundPlanProjectDetailDB)) {
                        if (fundPlanProjectDetailPage != null && fundPlanProjectDetailDB != null) {
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                                employFundBillForFundPlanProjectList.add(subObj);
                            }
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                releaseFundBillForFundPlanProjectList.add(fundCollection_b);
                                preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                            }
                        }
                        if (fundPlanProjectPage == null) {
                            subObj.put("isToPushCspl", 2);
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                releaseFundBillForFundPlanProjectList.add(fundCollection_b);
                                preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                            }
                        }
                        if (fundPlanProjectPage != null && fundPlanProjectDB == null) {
                            subObj.put("isToPushCspl", 1);
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                                employFundBillForFundPlanProjectList.add(subObj);
                            }
                        }
                    }

                    // 当在审批流审批时，编辑单据，占用了资金计划，并且修改了金额，这时释放原金额单据，占用修改后的金额单据
                    boolean isModifyOriSumWithFundPlanProjectNotChange =
                            !Objects.equals(fundPlanProjectPage, null) && !Objects.equals(fundPlanProjectDetailPage, null) &&
                                    Objects.equals(fundPlanProjectPage, fundPlanProjectDB) && Objects.equals(fundPlanProjectDetailPage, fundPlanProjectDetailDB);
                    if (isModifyOriSumWithFundPlanProjectNotChange && oriSumPage.compareTo(oriSumDB) != 0) {
                        fundCollection_b.setIsToPushCspl(2);
                        if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                            releaseFundBillForFundPlanProjectList.add(fundCollection_b);
                            preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                        }
                        subObj.put("isToPushCspl", 1);
                        if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                            preEmployFundBillForFundPlanProjectList.add(subObj);
                            employFundBillForFundPlanProjectList.add(subObj);
                        }
                    }
                }
            }
            if (notOpenBill && "Update".equals(bizObject.get("_status").toString()) && "Delete".equals(subObj.get("_status").toString())) {
                if (subObj.get("fundPlanProject") == null) {
                    continue;
                }
                if (BigDecimal.ZERO.compareTo(subObj.getBigDecimal("oriSum")) < 0) {
                    releaseFundBillForFundPlanProjectList.add(subObj);
                }
            }
        }
    }

    /**
     * <h2>资金收付款单更新和删除明细行时，预占用和释放资金计划</h2>
     *
     * @param billNum: 单据编码
     * @param bizObject: 单据数据
     * @param preEmployFundBillForFundPlanProjectList: 预占数据集合
     * @param preReleaseFundBillForFundPlanProjectList: 释放预占数据集合
     * @return java.util.List<org.imeta.orm.base.BizObject>
     * @author Sun GuoCai
     * @date 2024/10/11 10:25
     */
    @Override
    public List<BizObject> fundBillPreEmployOrReleaseFundPlanBeforeSaveForUpdateAndDelete(String billNum, BizObject bizObject, List<BizObject> preEmployFundBillForFundPlanProjectList, List<BizObject> preReleaseFundBillForFundPlanProjectList) throws Exception {
        List<BizObject> fundSubBList = null;
        BizObject originFund = new BizObject();
        if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
            fundSubBList = bizObject.get("FundPayment_b");
            originFund = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizObject.getId(), 3);
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
            fundSubBList = bizObject.get("FundCollection_b");
            originFund = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizObject.getId(), 3);
        }
        assert fundSubBList != null;
        for (BizObject subObj : fundSubBList) {
            Object id = subObj.get("id");
            if ("Update".equals(bizObject.get("_status").toString()) && "Update".equals(subObj.get("_status").toString())) {
                if (IBillNumConstant.FUND_PAYMENT.equals(billNum) && Objects.nonNull(id)) {
                    String fundPlanProjectPage = ValueUtils.isNotEmptyObj(subObj.get("fundPlanProject"))
                            ? subObj.getString("fundPlanProject") : null;
                    short settleStatusPage = ValueUtils.isNotEmptyObj(subObj.getShort("settlestatus"))
                            ? subObj.getShort("settlestatus") : null;
                    //FundPayment_b fundPayment_b = MetaDaoHelper.findById(FundPayment_b.ENTITY_NAME, id, 1);
                    List<FundPayment_b> originFundPayment_bList = originFund.get("FundPayment_b");
                    FundPayment_b fundPayment_b = originFundPayment_bList.stream()
                            .filter(item -> item.getId() != null && subObj.getId() != null && item.getId().toString().equals(subObj.getId().toString()))
                            .findFirst()
                            .orElse(null);
                    String fundPlanProjectDB = ValueUtils.isNotEmptyObj(fundPayment_b.getString("fundPlanProject"))
                            ? fundPayment_b.getString("fundPlanProject") : null;
                    short settleStatusDB = ValueUtils.isNotEmptyObj(fundPayment_b.getFundSettlestatus())
                            ? fundPayment_b.getFundSettlestatus().getValue() : null;

                    boolean isRefund = settleStatusPage != FundSettleStatus.Refund.getValue() && settleStatusDB != FundSettleStatus.Refund.getValue();
                    BigDecimal oriSumPage = subObj.getBigDecimal(ICmpConstant.ORISUM);
                    BigDecimal oriSumDB = fundPayment_b.getOriSum();
                    if (!Objects.equals(fundPlanProjectPage, fundPlanProjectDB) && isRefund) {
                        if (fundPlanProjectPage != null && fundPlanProjectDB != null) {
                            subObj.put("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                            }
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                            }
                        }
                        if (fundPlanProjectPage == null) {
                            subObj.put("isToPushCspl", PushCsplStatusEnum.NO_OCCUPIED.getValue());
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                            }
                        }
                        if (fundPlanProjectPage != null && fundPlanProjectDB == null) {
                            subObj.put("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                            }
                        }
                    }

                    String fundPlanProjectDetailPage = ValueUtils.isNotEmptyObj(subObj.get("fundPlanProjectDetail"))
                            ? subObj.getString("fundPlanProjectDetail") : null;
                    String fundPlanProjectDetailDB = ValueUtils.isNotEmptyObj(fundPayment_b.getString("fundPlanProjectDetail"))
                            ? fundPayment_b.getString("fundPlanProjectDetail") : null;
                    if (Objects.equals(fundPlanProjectPage, fundPlanProjectDB)
                            && !Objects.equals(fundPlanProjectDetailPage, fundPlanProjectDetailDB)
                            && isRefund) {
                        if (fundPlanProjectDetailPage != null && fundPlanProjectDetailDB != null) {
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);

                            }
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                            }
                        }
                        if (fundPlanProjectPage == null) {
                            subObj.put("isToPushCspl", 2);
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                            }
                        }
                        if (fundPlanProjectPage != null && fundPlanProjectDB == null) {
                            subObj.put("isToPushCspl", 1);
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                            }
                        }
                    }

                    if (!isRefund) {
                        if (settleStatusPage == FundSettleStatus.Refund.getValue()
                                && settleStatusDB != FundSettleStatus.Refund.getValue()
                                && fundPlanProjectDB != null
                                && (BigDecimal.ZERO.compareTo(oriSumDB) < 0)) {
                            preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                        }
                        if (settleStatusPage != FundSettleStatus.Refund.getValue()
                                && settleStatusDB == FundSettleStatus.Refund.getValue()
                                && fundPlanProjectPage != null
                                && (BigDecimal.ZERO.compareTo(oriSumDB) < 0)) {
                            preEmployFundBillForFundPlanProjectList.add(subObj);
                        }
                    }
                    // 当在审批流审批时，编辑单据，占用了资金计划，并且修改了金额，这时释放原金额单据，占用修改后的金额单据
                    boolean isModifyOriSumWithFundPlanProjectNotChange =
                            (fundPlanProjectPage != null || fundPlanProjectDetailPage != null) &&
                                    Objects.equals(fundPlanProjectPage, fundPlanProjectDB) && Objects.equals(fundPlanProjectDetailPage, fundPlanProjectDetailDB);
                    if (isModifyOriSumWithFundPlanProjectNotChange && (oriSumPage.compareTo(oriSumDB) != 0 )) {
                        if (ValueUtils.isNotEmptyObj(oriSumDB)
                                && settleStatusDB != FundSettleStatus.Refund.getValue()) {
                            preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                        }
                        subObj.put("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                        if (ValueUtils.isNotEmptyObj(oriSumPage)
                                && settleStatusPage != FundSettleStatus.Refund.getValue()) {
                            preEmployFundBillForFundPlanProjectList.add(subObj);
                        }
                    }
                }
                if (IBillNumConstant.FUND_COLLECTION.equals(billNum) && Objects.nonNull(id)) {
                    String fundPlanProjectPage = ValueUtils.isNotEmptyObj(subObj.get("fundPlanProject"))
                            ? subObj.getString("fundPlanProject") : null;
                    //FundCollection_b fundCollection_b = MetaDaoHelper.findById(FundCollection_b.ENTITY_NAME, id, 1);
                    List<FundCollection_b> originFundCollection_bList = originFund.get("FundCollection_b");
                    FundCollection_b fundCollection_b = originFundCollection_bList.stream()
                            .filter(item -> item.getId() != null && subObj.getId() != null && item.getId().toString().equals(subObj.getId().toString()))
                            .findFirst()
                            .orElse(null);
                    String fundPlanProjectDB = ValueUtils.isNotEmptyObj(fundCollection_b.getString("fundPlanProject"))
                            ? fundCollection_b.getString("fundPlanProject") : null;
                    BigDecimal oriSumPage = subObj.getBigDecimal(ICmpConstant.ORISUM);
                    BigDecimal oriSumDB = fundCollection_b.getOriSum();
                    if (!Objects.equals(fundPlanProjectPage, fundPlanProjectDB)) {
                        if (fundPlanProjectPage != null && fundPlanProjectDB != null) {
                            subObj.put("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                            }
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                            }
                        }
                        if (fundPlanProjectPage == null) {
                            subObj.put("isToPushCspl", PushCsplStatusEnum.NO_OCCUPIED.getValue());
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                            }
                        }
                        if (fundPlanProjectPage != null && fundPlanProjectDB == null) {
                            subObj.put("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                            }
                        }
                    }

                    String fundPlanProjectDetailPage = ValueUtils.isNotEmptyObj(subObj.get("fundPlanProjectDetail"))
                            ? subObj.getString("fundPlanProjectDetail") : null;
                    String fundPlanProjectDetailDB = ValueUtils.isNotEmptyObj(fundCollection_b.getString("fundPlanProjectDetail"))
                            ? fundCollection_b.getString("fundPlanProjectDetail") : null;
                    if (Objects.equals(fundPlanProjectPage, fundPlanProjectDB)
                            && !Objects.equals(fundPlanProjectDetailPage, fundPlanProjectDetailDB)) {
                        if (fundPlanProjectDetailPage != null && fundPlanProjectDetailDB != null) {
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                            }
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                            }
                        }
                        if (fundPlanProjectPage == null) {
                            subObj.put("isToPushCspl", 2);
                            if (BigDecimal.ZERO.compareTo(oriSumDB) < 0) {
                                preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                            }
                        }
                        if (fundPlanProjectPage != null && fundPlanProjectDB == null) {
                            subObj.put("isToPushCspl", 1);
                            if (BigDecimal.ZERO.compareTo(oriSumPage) < 0) {
                                preEmployFundBillForFundPlanProjectList.add(subObj);
                            }
                        }
                    }

                    // 当在审批流审批时，编辑单据，占用了资金计划，并且修改了金额，这时释放原金额单据，占用修改后的金额单据
                    boolean isModifyOriSumWithFundPlanProjectNotChange =
                            (fundPlanProjectPage != null || fundPlanProjectDetailPage != null) &&
                                    Objects.equals(fundPlanProjectPage, fundPlanProjectDB) && Objects.equals(fundPlanProjectDetailPage, fundPlanProjectDetailDB);
                    if (isModifyOriSumWithFundPlanProjectNotChange && oriSumPage.compareTo(oriSumDB) != 0) {
                        if (ValueUtils.isNotEmptyObj(oriSumDB)) {
                            preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                        }
                        subObj.put("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                        if (ValueUtils.isNotEmptyObj(oriSumPage)) {
                            preEmployFundBillForFundPlanProjectList.add(subObj);
                        }
                    }
                }
            }
            if ("Update".equals(bizObject.get("_status").toString()) && "Delete".equals(subObj.get("_status").toString())) {
                if (subObj.get("fundPlanProject") == null) {
                    continue;
                }
                if (ValueUtils.isNotEmptyObj(subObj.getBigDecimal("oriSum"))) {
                    subObj.put("isToPushCspl", PushCsplStatusEnum.NO_OCCUPIED.getValue());
                    if ("Delete".equals(subObj.get("_status").toString())) {
                        if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
                            List<FundPayment_b> originFundPayment_bList = originFund.get("FundPayment_b");
                            FundPayment_b fundPayment_b = originFundPayment_bList.stream().filter(item -> item.getId().toString().equals(subObj.getId().toString())).findFirst().orElse(null);
                            preReleaseFundBillForFundPlanProjectList.add(fundPayment_b);
                        } else if (IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
                            List<FundCollection_b> originFundCollection_bList = originFund.get("FundCollection_b");
                            FundCollection_b fundCollection_b = originFundCollection_bList.stream().filter(item -> item.getId().toString().equals(subObj.getId().toString())).findFirst().orElse(null);
                            preReleaseFundBillForFundPlanProjectList.add(fundCollection_b);
                        }
                    } else {
                        preReleaseFundBillForFundPlanProjectList.add(subObj);
                    }
                }
            }
        }
        return fundSubBList;
    }

    /**
     * <h2>资金收付款单保存以及更新插入明细行时，预占用和释放资金计划</h2>
     *
     * @param billNum: 单据编码
     * @param bizObject: 单据数据
     * @param preEmployFundBillForFundPlanProjectList: 预占数据集合
     * @param preReleaseFundBillForFundPlanProjectList: 释放预占数据集合
     * @return java.util.List<org.imeta.orm.base.BizObject>
     * @author Sun GuoCai
     * @date 2024/10/11 10:26
     */
    @Override
    public List<BizObject> fundBillPreEmployOrReleaseFundPlanAfterSaveForInsert(String billNum, BizObject bizObject, List<BizObject> preEmployFundBillForFundPlanProjectList, List<BizObject> preReleaseFundBillForFundPlanProjectList) throws Exception {
        List<BizObject> fundSubBList = null;
        if (IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
            fundSubBList = bizObject.get("FundPayment_b");
        } else if (IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
            fundSubBList = bizObject.get("FundCollection_b");
        }
        assert fundSubBList != null;
        for (BizObject subObj : fundSubBList) {
            if ("Insert".equals(subObj.get("_status").toString())) {
                // 退票的资金收付款单不占用资金计划项目
                Short settleStatus = subObj.getShort("settlestatus");
                if (settleStatus == null || settleStatus == FundSettleStatus.Refund.getValue()) {
                    continue;
                }
                if (ValueUtils.isNotEmptyObj(subObj.getBigDecimal("oriSum"))  && subObj.get("fundPlanProject") != null) {
                    subObj.put("isToPushCspl", PushCsplStatusEnum.PRE_OCCUPIED.getValue());
                    preEmployFundBillForFundPlanProjectList.add(subObj);
                }
            }
        }
        return fundSubBList;
    }

    @Override
    public void fundPlanProjectPreEmployOrReleaseNotControl(String billnum, BizObject bizObject, List<BizObject> preReleaseFundBillForFundPlanProjectList) throws Exception {
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            if (ValueUtils.isNotEmptyObj(bizObject.get("_status")) && "Update".equals(bizObject.get("_status").toString())) {
                Object id = bizObject.getId();
                BizObject fundPaymentSuper = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id, 2);
                List<FundPayment_b> fundPaymentSub = fundPaymentSuper.getBizObjects("FundPayment_b", FundPayment_b.class);
                List<FundPayment_b> fundPaymentSubUpdate = new ArrayList<>();
                for (FundPayment_b fundPaymentB : fundPaymentSub) {
                    if (fundPaymentB.getString("fundPlanProject") != null
                            && BigDecimal.ZERO.compareTo(fundPaymentB.getOriSum()) < 0
                            && fundPaymentB.getIsToPushCspl() != null
                            && Objects.equals(fundPaymentB.getIsToPushCspl(), PushCsplStatusEnum.PRE_OCCUPIED.getValue())) {
                        fundPaymentB.setIsToPushCspl(PushCsplStatusEnum.NO_OCCUPIED.getValue());
                        preReleaseFundBillForFundPlanProjectList.add(fundPaymentB);
                        fundPaymentSubUpdate.add(fundPaymentB);
                    }
                }
                if (CollectionUtils.isNotEmpty(fundPaymentSubUpdate)) {
                    EntityTool.setUpdateStatus(fundPaymentSubUpdate);
                    MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentSubUpdate);
                }
            }

        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            if (ValueUtils.isNotEmptyObj(bizObject.get("_status")) && "Update".equals(bizObject.get("_status").toString())) {
                Object id = bizObject.getId();
                BizObject fundPaymentSuper = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, id, 2);
                List<FundCollection_b> fundCollectionSub = fundPaymentSuper.getBizObjects("FundCollection_b", FundCollection_b.class);
                List<FundCollection_b> fundCollectionSubUpdate = new ArrayList<>();
                for (FundCollection_b fundCollectionB : fundCollectionSub) {
                    if (fundCollectionB.getString("fundPlanProject") != null
                            && BigDecimal.ZERO.compareTo(fundCollectionB.getOriSum()) < 0
                            && fundCollectionB.getIsToPushCspl() != null
                            && Objects.equals(fundCollectionB.getIsToPushCspl(), PushCsplStatusEnum.PRE_OCCUPIED.getValue())) {
                        fundCollectionB.setIsToPushCspl(PushCsplStatusEnum.NO_OCCUPIED.getValue());
                        preReleaseFundBillForFundPlanProjectList.add(fundCollectionB);
                        fundCollectionSubUpdate.add(fundCollectionB);

                    }
                }
                if (CollectionUtils.isNotEmpty(fundCollectionSubUpdate)) {
                    EntityTool.setUpdateStatus(fundCollectionSubUpdate);
                    MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollectionSubUpdate);
                }
            }
        }
    }


    @Override
    public void fundBillReleaseFundPlan(String billnum, BizObject bizObject, List<BizObject> releaseFundBillForFundPlanProjectList, Object settleFailed, Object reFund, String occupyFlag) throws Exception {
        CtmJSONObject jsonObject= new CtmJSONObject();
        jsonObject.put("1.billnum",billnum);
        jsonObject.put("2.employFundBillForFundPlanProjectList", releaseFundBillForFundPlanProjectList);
        jsonObject.put("3.occupyFlag", occupyFlag);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("accentity", bizObject.get("accentity"));
        map2.put("vouchdate", bizObject.get("vouchdate"));
        map2.put("code", bizObject.get("code"));
        map2.put("settleFailed", settleFailed);
        map2.put("reFund", reFund);
        jsonObject.put("4.map", map2);
        List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameter(releaseFundBillForFundPlanProjectList, IStwbConstant.RELEASE, billnum, map2);
        jsonObject.put("5.checkObject", checkObject);
        if (ValueUtils.isNotEmptyObj(checkObject)) {
            CapitalPlanExecuteResp capitalPlanExecuteResp;
            try {
                if (occupyFlag.equals("act")) {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                } else {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).preEmployAndrelease(checkObject);
                }
                jsonObject.put("6.capitalPlanExecuteResp", capitalPlanExecuteResp);
                if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
                        && "500".equals(capitalPlanExecuteResp.getCode())
                        && capitalPlanExecuteResp.getSuccessCount() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + capitalPlanExecuteResp.getMessage().toString());
                }
                List<BizObject> bizObjectList = releaseFundBillForFundPlanProjectList.stream()
                        .filter(item -> item.get("_status") == null || !"Delete".equals(item.get("_status").toString())).collect(Collectors.toList());
                jsonObject.put("7.bizObjectList", bizObjectList);
//                TODO 先注释掉，不知道干什么用的，影响结算成功金额回写
//                if (occupyFlag.equals("act")) {
//                    if (CollectionUtils.isNotEmpty(bizObjectList)) {
//                        EntityTool.setUpdateStatus(bizObjectList);
//                        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
//                            MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, bizObjectList);
//                        } else {
//                            MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, bizObjectList);
//                        }
//                    }
//                }
            } catch (Exception e) {
                log.error("fundBillReleaseFundPlan error, errorMsg={}", e.getMessage());
                jsonObject.put("8.errorMsg", e.getMessage());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
            } finally {
                jsonObject.put("9.pushType", "release");
                jsonObject.put("10.pushCate", occupyFlag.equals("act") ? "release" : "preRelease");
                jsonObject.put("11.method", "com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanServiceImpl.fundBillReleaseFundPlan");
                String desc = IBillNumConstant.FUND_PAYMENT.equals(billnum)
                        ? (occupyFlag.equals("act") ? IMsgConstant.FUND_PAYMENT_EMPLOY_AND_RELEASE_FUND_PLAN
                        : IMsgConstant.FUND_PAYMENT_PRE_EMPLOY_AND_RELEASE_FUND_PLAN) :
                        (occupyFlag.equals("act") ? IMsgConstant.FUND_COLLECTION_EMPLOY_AND_RELEASE_FUND_PLAN
                                : IMsgConstant.FUND_COLLECTION_PRE_EMPLOY_AND_RELEASE_FUND_PLAN);

                String serviceCode = IBillNumConstant.FUND_PAYMENT.equals(billnum)
                        ? IServicecodeConstant.FUNDPAYMENT : IServicecodeConstant.FUNDCOLLECTION;

                String billNum = IBillNumConstant.FUND_PAYMENT.equals(billnum)
                        ? IMsgConstant.FUND_PAYMENT : IMsgConstant.FUND_COLLECTION;

                CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                ctmcmpBusinessLogService.saveBusinessLog(
                        jsonObject,
                        bizObject.getString(ICmpConstant.CODE),
                        desc,
                        serviceCode,
                        billNum,
                        OperCodeTypes.unlock.getDefaultOperateName());
            }
        }
    }

    @Override
    public void fundBillEmployFundPlan(String billnum, BizObject bizObject, List<BizObject> employFundBillForFundPlanProjectList, String occupyFlag) throws Exception {
        CtmJSONObject jsonObject= new CtmJSONObject();
        jsonObject.put("1.billnum",billnum);
        jsonObject.put("2.employFundBillForFundPlanProjectList", employFundBillForFundPlanProjectList);
        jsonObject.put("3.occupyFlag", occupyFlag);
        Map<String, Object> map1 = new HashMap<>();
        map1.put("accentity", bizObject.get("accentity"));
        map1.put("vouchdate", bizObject.get("vouchdate"));
        map1.put("code", bizObject.get("code"));
        jsonObject.put("4.map", map1);
        List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameter(employFundBillForFundPlanProjectList, IStwbConstant.EMPLOY, billnum, map1);
        jsonObject.put("5.checkObject", checkObject);
        if (ValueUtils.isNotEmptyObj(checkObject)) {
            CapitalPlanExecuteResp capitalPlanExecuteResp;
            try {
                if (occupyFlag.equals("act")) {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
                } else {
                    capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).preEmployAndrelease(checkObject);
                }
                jsonObject.put("6.capitalPlanExecuteResp", capitalPlanExecuteResp);
                if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
                        && "500".equals(capitalPlanExecuteResp.getCode())
                        && capitalPlanExecuteResp.getSuccessCount() == 0) {
                    throw new CtmException(capitalPlanExecuteResp.getMessage().toString());
                }
                if (occupyFlag.equals("act")) {
                    EntityTool.setUpdateStatus(employFundBillForFundPlanProjectList);
                    if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
                        MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, employFundBillForFundPlanProjectList);
                    } else {
                        MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, employFundBillForFundPlanProjectList);
                    }
                }
            } catch (Exception e) {
                log.error("fundBillEmployFundPlan error, errorMsg={}", e.getMessage());
                jsonObject.put("7.errorMsg", e.getMessage());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
            }finally {
                jsonObject.put("8.pushType", "employ");
                jsonObject.put("9.pushCate", occupyFlag.equals("act") ? "employ" : "preEmploy");
                jsonObject.put("10.method", "com.yonyoucloud.fi.cmp.fundcommon.service.FundBillAdaptationFundPlanServiceImpl.fundBillEmployFundPlan");
                String desc = IBillNumConstant.FUND_PAYMENT.equals(billnum)
                        ? (occupyFlag.equals("act") ? IMsgConstant.FUND_PAYMENT_EMPLOY_AND_RELEASE_FUND_PLAN
                        : IMsgConstant.FUND_PAYMENT_PRE_EMPLOY_AND_RELEASE_FUND_PLAN) :
                        (occupyFlag.equals("act") ? IMsgConstant.FUND_COLLECTION_EMPLOY_AND_RELEASE_FUND_PLAN
                                : IMsgConstant.FUND_COLLECTION_PRE_EMPLOY_AND_RELEASE_FUND_PLAN);

                String serviceCode = IBillNumConstant.FUND_PAYMENT.equals(billnum)
                        ? IServicecodeConstant.FUNDPAYMENT : IServicecodeConstant.FUNDCOLLECTION;

                String billNum = IBillNumConstant.FUND_PAYMENT.equals(billnum)
                        ? IMsgConstant.FUND_PAYMENT : IMsgConstant.FUND_COLLECTION;

                CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                ctmcmpBusinessLogService.saveBusinessLog(
                        jsonObject,
                        bizObject.getString(ICmpConstant.CODE),
                        desc,
                        serviceCode,
                        billNum,
                        OperCodeTypes.lock.getDefaultOperateName());
            }
        }
    }

    @Override
    public void fundPlanProjectNotControl(String billnum, BizObject
            bizObject, List<BizObject> releaseFundBillForFundPlanProjectList) throws Exception {
        if (IBillNumConstant.FUND_PAYMENT.equals(billnum)) {
            if ("Update".equals(bizObject.get("_status").toString())) {
                Object id = bizObject.getId();
                BizObject fundPaymentSuper = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id, 2);
                List<FundPayment_b> fundPaymentSub = fundPaymentSuper.getBizObjects("FundPayment_b", FundPayment_b.class);
                List<FundPayment_b> fundPaymentSubUpdate = new ArrayList<>();
                for (FundPayment_b fundPaymentB : fundPaymentSub) {
                    if (fundPaymentB.getString("fundPlanProject") != null
                            && BigDecimal.ZERO.compareTo(fundPaymentB.getOriSum()) < 0
                            && fundPaymentB.getIsToPushCspl() != null
                            && fundPaymentB.getIsToPushCspl() == 1) {
                        releaseFundBillForFundPlanProjectList.add(fundPaymentB);
                        fundPaymentSubUpdate.add(fundPaymentB);
                    }
                }
                if (CollectionUtils.isNotEmpty(fundPaymentSubUpdate)) {
                    EntityTool.setUpdateStatus(fundPaymentSubUpdate);
                    MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentSubUpdate);
                }
            }

        } else if (IBillNumConstant.FUND_COLLECTION.equals(billnum)) {
            if ("Update".equals(bizObject.get("_status").toString())) {
                Object id = bizObject.getId();
                BizObject fundPaymentSuper = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, id, 2);
                List<FundCollection_b> fundCollectionSub = fundPaymentSuper.getBizObjects("FundCollection_b", FundCollection_b.class);
                List<FundCollection_b> fundCollectionSubUpdate = new ArrayList<>();
                for (FundCollection_b fundCollectionB : fundCollectionSub) {
                    if (fundCollectionB.getString("fundPlanProject") != null
                            && BigDecimal.ZERO.compareTo(fundCollectionB.getOriSum()) < 0
                            && fundCollectionB.getIsToPushCspl() != null
                            && fundCollectionB.getIsToPushCspl() == 1) {
                        releaseFundBillForFundPlanProjectList.add(fundCollectionB);
                        fundCollectionSubUpdate.add(fundCollectionB);

                    }
                }
                if (CollectionUtils.isNotEmpty(fundCollectionSubUpdate)) {
                    EntityTool.setUpdateStatus(fundCollectionSubUpdate);
                    MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollectionSubUpdate);
                }
            }
        }
    }

    /**
     * <h2>根据资金计划明细id查询资金计划明细</h2>
     *
     * @param param : 入参
     * @return com.yonyou.yonbip.ctm.json.CtmJSONObject
     * @author Sun GuoCai
     * @since 2024/6/15 14:15
     */
    @Override
    public CtmJSONObject queryFundPlanDetailById(CtmJSONObject param) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        String fundPlanDetailId = param.getString("fundPlanDetailId");
        BillContext fundPlanDetailBillContext = new BillContext();
        fundPlanDetailBillContext.setFullname("cspl.plansummary.PlanSummaryDetail");
        fundPlanDetailBillContext.setDomain(MDD_DOMAIN_CTMCSPL);
        QueryConditionGroup fundPlanDetailCondition = new QueryConditionGroup();
        fundPlanDetailCondition.addCondition(QueryCondition.name("id").eq(fundPlanDetailId));
        List<Map<String, Object>> fundPlanDetailList = MetaDaoHelper.queryAll(fundPlanDetailBillContext,
                "oppType, oppId, oppName, oppAccId, oppAcc, oppAccName, oppBankType, oppBankName, paymentType," +
                        "paymentType.name,expenseProject, expenseProject.name, " +
                        "project,project.name, settleMode, settleMode.name,settleMode.serviceAttr,unExecuteAmount, dept, dept.name, " +
                        "bankAccount,bankAccount.name,bankAccount.account, cashAccount, cashAccount.name,billNumberId",
                fundPlanDetailCondition, null);
        if (CollectionUtils.isNotEmpty(fundPlanDetailList)) {
            Map<String, Object> objectMap = fundPlanDetailList.get(0);
            result.put("oppId", objectMap.get("oppId"));
            result.put("oppName", objectMap.get("oppName"));
            result.put("oppAccId", objectMap.get("oppAccId"));
            result.put("oppAcc", objectMap.get("oppAcc"));
            result.put("oppAccName", objectMap.get("oppAccName"));
            result.put("oppBankType", objectMap.get("oppBankType"));
            result.put("oppBankName", objectMap.get("oppBankName"));
            result.put("paymentType", objectMap.get("paymentType"));
            result.put("paymentType_name", objectMap.get("paymentType_name"));
            result.put("expenseProject", objectMap.get("expenseProject"));
            result.put("expenseProject_name", objectMap.get("expenseProject_name"));
            result.put("project", objectMap.get("project"));
            result.put("project_name", objectMap.get("project_name"));
            result.put("settleMode", objectMap.get("settleMode"));
            result.put("settleMode_name", objectMap.get("settleMode_name"));
            result.put("settleMode_serviceAttr", objectMap.get("settleMode_serviceAttr"));
            result.put("amount", objectMap.get("unExecuteAmount"));
            result.put("dept", objectMap.get("dept"));
            result.put("dept_name", objectMap.get("dept_name"));


            if (ValueUtils.isNotEmptyObj(objectMap.get("settleMode_serviceAttr"))
                    && Integer.parseInt(objectMap.get("settleMode_serviceAttr").toString()) == 0) {
                result.put("bankAccount", objectMap.get("bankAccount"));
                result.put("bankAccount_name", objectMap.get("bankAccount_name"));
                result.put("bankAccount_account", objectMap.get("bankAccount_account"));

            } else if (ValueUtils.isNotEmptyObj(objectMap.get("settleMode_serviceAttr"))
                    && Integer.parseInt(objectMap.get("settleMode_serviceAttr").toString()) == 1) {
                result.put("cashAccount", objectMap.get("cashAccount"));
                result.put("cashAccount_name", objectMap.get("cashAccount_name"));
            }


            // 处理资金业务对象类型id
            Object oppType = objectMap.get("oppType");
            if (ValueUtils.isNotEmptyObj(oppType)) {
                processFundBusinessObjTypeId(oppType, result, objectMap);
            }

            // 处理票据号
            if (ValueUtils.isNotEmptyObj(objectMap.get("settleMode_serviceAttr"))
                    && Integer.parseInt(objectMap.get("settleMode_serviceAttr").toString()) == 2
                    && ValueUtils.isNotEmptyObj(objectMap.get("billNumberId"))) {
                processNoteNo(objectMap, result);
            }
        }
        return result;
    }

    private static void processFundBusinessObjTypeId(Object oppType, CtmJSONObject result, Map<String, Object> objectMap) throws Exception {
        int oppTypeId = Integer.parseInt(oppType.toString());
        short caObject = getCaobject(oppTypeId);
        result.put("caobject", caObject);
        if (oppTypeId == 1) {
            Object oppId = objectMap.get("oppId");
            BillContext context = new BillContext();
            context.setFullname("tmsp.fundbusinobjarchives.FundBusinObjArchives");
            context.setDomain("yonbip-fi-ctmtmsp");
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id , fundbusinobjtypeid");
            schema.appendQueryCondition(QueryCondition.name("id").eq(oppId));
            List<Map<String, Object>> fundBusinessObjResult = MetaDaoHelper.query(context, schema);
            // 获取账户信息对象
            if (CollectionUtils.isNotEmpty(fundBusinessObjResult)) {
                // 获取数据实体
                CtmJSONObject jsonObject = new CtmJSONObject(fundBusinessObjResult.get(0));
                result.put("fundbusinobjtypeid", jsonObject.get("fundbusinobjtypeid"));
            }
        }
    }

    private void processNoteNo(Map<String, Object> objectMap, CtmJSONObject result) throws Exception {
        Object noteNoId = objectMap.get("billNumberId");
        BillContext context = new BillContext();
        context.setFullname("drft.billno.Billno");
        context.setDomain(IDomainConstant.MDD_WORKBENCH_DRFT);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("id, noteno, notemoney,billdirection,notetype, notetype.billtypename");
        schema.appendQueryCondition(QueryCondition.name("id").eq(noteNoId));
        List<Map<String, Object>> noteNoResult = MetaDaoHelper.query(context, schema);
        // 获取账户信息对象
        if (CollectionUtils.isNotEmpty(noteNoResult)) {
            // 获取数据实体
            CtmJSONObject jsonObject = new CtmJSONObject(noteNoResult.get(0));
            result.put("noteno", jsonObject.get("id"));
            result.put("noteno_noteno", jsonObject.get("noteno"));
            result.put("noteDirection", jsonObject.get("billdirection"));
            result.put("noteSum", jsonObject.get("notemoney"));
            result.put("notetype", jsonObject.get("notetype"));
            result.put("notetype_billtypename", jsonObject.get("notetype_billtypename"));
        }
    }

    private static short getCaobject(int oppTypeId) {
        short caobject = 0;
        if (oppTypeId == 1) {
            caobject = CaObject.CapBizObj.getValue();
        } else if (oppTypeId == 2) {
            caobject = CaObject.Supplier.getValue();
        } else if (oppTypeId == 3) {
            caobject = CaObject.Customer.getValue();
        } else if (oppTypeId == 4) {
            caobject = CaObject.Employee.getValue();
        } else if (oppTypeId == 9) {
            caobject = CaObject.Other.getValue();
        }
        return caobject;
    }

}
