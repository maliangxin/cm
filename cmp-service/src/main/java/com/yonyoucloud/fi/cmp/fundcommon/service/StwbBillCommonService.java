package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.settleapply.processor.PushSettleParam;
import com.yonyou.yonbip.ctm.settleapply.processor.PushSettleProcessor;
import com.yonyoucloud.ctm.stwb.common.app.Application;
import com.yonyoucloud.ctm.stwb.datasettled.DataSettled;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.ResponseResult;
import com.yonyoucloud.ctm.stwb.openapi.Result;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.ctm.stwb.settleapply.vo.PushOrder;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class StwbBillCommonService {

    public static final String FUND_PAYMENT_B_FULLNAME = "cmp.fundpayment.FundPayment_b";
    public static final String FUND_COLLECTION_B_FULLNAME = "cmp.fundcollection.FundCollection_b";
    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;
    @Autowired
    private PushSettleProcessor pushSettleProcessor;

    /**
     * 简强推送单据至资金结算
     *
     * @param settleMap
     * @return
     */
    @Transactional(rollbackFor = RuntimeException.class)
    public void builtSystem(BizObject bizObject,List<Map<String, Object>> settleMap, String entityname, String code,
                            Map<String, Date> detailIdMapPubts) throws Exception {
        //现金传结算参数，默认传结算，为false直接return
        String pushSettleStr = null;
        try {
            pushSettleStr = AppContext.getEnvConfig("cmp.settle.push");
            if (!StringUtils.isEmpty(pushSettleStr)) {
                boolean pushSettle = Boolean.parseBoolean(pushSettleStr);
                if (!pushSettle) {
                    return;
                }
            }
        } catch (Exception e) {
            log.error("获取cmp.settle.push参数失败！", e);
            //todo 待翻译
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C9", "获取cmp.settle.push参数失败！") /* "获取cmp.settle.push参数失败！" */, e);
        }
        //没有需要传结算的数据
        if (settleMap == null || settleMap.isEmpty()) {
            return;
        }
        //资金收付结算是否简强
//        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
//        if (!enableSimplify) {
//            return;
//        }
        Map<String, Object> params = new HashMap<>();
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        try {
            ctmJSONObject.put("params", params);
            for (Map<String, Object> map : settleMap) {
                params.put("code", map.get(ICmpConstant.CODE));//单据编号
                params.put("mainId", bizObject.get(ICmpConstant.ID));//单据id
                params.put("subId", map.get("bizbilldetailid"));//子表id
                if (FundCollection.ENTITY_NAME.equals(entityname)) {
                    map.put("sourceCode", IBillNumConstant.FUND_COLLECTION);
                }
                if (FundPayment.ENTITY_NAME.equals(entityname)) {
                    map.put("sourceCode", IBillNumConstant.FUND_PAYMENT);
                }
                log.error("StwbBillCommonService.builtSystem start, settleMap:{}, entityname:{}", settleMap, entityname);
                PushSettleParam pushSettleParam = PushSettleParam.builder().pushOrder(PushOrder.FIRST)
                        .billDate(map.get(ICmpConstant.VOUCHDATE).toString())
                        .settleApplyUri(entityname)
                        .id(bizObject.get(ICmpConstant.ID).toString())
                        .code(bizObject.get(ICmpConstant.CODE).toString())
                        .pubts((Date) bizObject.get("pubts"))
                        .detailIdMapPubts(detailIdMapPubts).build();
                pushSettleProcessor.doProcessor(Application.CMP, pushSettleParam, map);
                log.error("StwbBillCommonService.builtSystem end, settleMap:{}, entityname:{}", settleMap, entityname);
            }

        } catch (Exception e) {
            ctmJSONObject.put("status", 200);
            ctmJSONObject.put("result", e.getMessage());
            log.error("ctmcmp invoke ctmstwb fail, data={}, e={}", params, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102278"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180190", "推送结算单据失败：") /* "推送结算单据失败：" */ + e.getMessage());
        } finally {
            ctmJSONObject.put("params", params);
            ctmJSONObject.put("data", settleMap);
            CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
            ctmcmpBusinessLogService.saveBusinessLog(
                    ctmJSONObject, code,
                    entityname.equals(FundCollection.ENTITY_NAME) ? IMsgConstant.FUND_COLLECTION_PUSH_DATA_SETTLES :
                            IMsgConstant.FUND_PAYMENT_PUSH_DATA_SETTLES,
                    entityname.equals(FundCollection.ENTITY_NAME) ? IServicecodeConstant.FUNDCOLLECTION :
                            IServicecodeConstant.FUNDPAYMENT,
                    entityname.equals(FundCollection.ENTITY_NAME) ? IMsgConstant.FUND_COLLECTION : IMsgConstant.FUND_PAYMENT,
                    OperCodeTypes.assubmit.getDefaultOperateName());
        }
    }

    /**
     * 推送单据至资金结算
     *
     * @param dataSettleds
     * @return
     */
    @Transactional(rollbackFor = RuntimeException.class)
    public void saveDataSettleds(List<DataSettled> dataSettleds, String tableName) throws Exception {
        Map<String, Object> params = new HashMap<>();
        for (DataSettled dataSettled : dataSettleds) {
            params.put("code", dataSettled.getBusinessbillnum());//单据编号
            params.put("mainId", dataSettled.getBusinessId());//单据id
            params.put("subId", dataSettled.getBusinessdetailsid());//子表id
        }
        try {
            ResponseResult responseResult = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).builtSystem(dataSettleds);
            if (responseResult.getCode() == 200) {
                log.error("fund bill push stwb success, data={}", params);
                saveTransNumber(responseResult.getSuccessList(), tableName);
            } else {
                log.error("fund bill push stwb fail, data={}", params);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102277"), responseResult.getMessage());
            }
        } catch (Exception e) {
            log.error("ctmcmp invoke ctmstwb fail, data={}, e={}", params, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102278"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180190", "推送结算单据失败：") /* "推送结算单据失败：" */ + e.getMessage());
        }
    }

    /**
     * 推送单据至资金结算
     *
     * @param dataSettleds
     * @return
     */
    @Transactional(rollbackFor = RuntimeException.class)
    public void builtSystem(BizObject bizObject, List<DataSettled> dataSettleds, String tableName) throws Exception {
        //现金传结算参数，默认传结算，为false直接return
        String pushSettleStr = null;
        try {
            pushSettleStr = AppContext.getEnvConfig("cmp.settle.push");
            if (!StringUtils.isEmpty(pushSettleStr)) {
                boolean pushSettle = Boolean.parseBoolean(pushSettleStr);
                if (!pushSettle) {
                    return;
                }
            }
        } catch (Exception e) {
            log.error("获取cmp.settle.push参数失败！", e);
        }
        //没有需要传结算的数据
        if (dataSettleds == null || dataSettleds.size() == 0) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        for (DataSettled dataSettled : dataSettleds) {
            params.put("code", dataSettled.getBusinessbillnum());//单据编号
            params.put("mainId", dataSettled.getBusinessId());//单据id
            params.put("subId", dataSettled.getBusinessdetailsid());//子表id
        }
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        ctmJSONObject.put("params", params);
        try {
            ctmJSONObject.put("data", dataSettleds);
            ResponseResult responseResult = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).builtSystem(dataSettleds);
            if (responseResult.getCode() == 200) {
                ctmJSONObject.put("status", 200);
                ctmJSONObject.put("result", responseResult);
                log.error("fund bill push stwb success, data={}", params);
                saveTransNumber(responseResult.getSuccessList(), tableName);
                //推送成功，推送业务日志
            } else {
                log.error("fund bill push stwb fail, data={}", params);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102279"), responseResult.getMessage());
            }
        } catch (Exception e) {
            ctmJSONObject.put("status", 200);
            ctmJSONObject.put("result", e.getMessage());
            log.error("ctmcmp invoke ctmstwb fail, data={}, e={}", params, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102278"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180190", "推送结算单据失败：") /* "推送结算单据失败：" */ + e.getMessage());
        } finally {
            String billnumber = tableName;
            saveBusinessLog(bizObject, ctmJSONObject, billnumber);
        }
    }

    /**
     * 修改子表单据的待结算流水号
     *
     * @param successList
     * @throws Exception
     */
    private void saveTransNumber(List<Object> successList, String tableName) throws Exception {
        for (Object obj : successList) {
            if (obj instanceof Result) {
                Result result = ((Result) obj);
                String transNumber = result.getTransNumber();
                DataSettled dataSettled = (DataSettled) result.getData();
                Map<String, Object> params = new HashMap<>();
                params.put("ytenantId", AppContext.getYTenantId());
                params.put("detailId", dataSettled.getBusinessdetailsid());// 子表明细id集合
                params.put("transNumber", transNumber);// 待结算数据id
                params.put("tableName", tableName);// 资金收付款单表名
                if (tableName.equals(IBillNumConstant.CMP_PAYMARGIN) || tableName.equals(IBillNumConstant.CMP_RECEIVEMARGIN)) {
                    SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateTransNumberByDetailIdMargin", params);
                } else if (tableName.equals(IBillNumConstant.CMP_FOREIGNPAYMENT) || tableName.equals(IBillNumConstant.CMP_FOREIGNPAYMENTLIST)) {
                    SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateTransNumberByDetailIdForeignpayment", params);
                } else if (tableName.equals(IBillNumConstant.FUND_PAYMENT_B)) {
                    FundPayment_b fundPayment_b = new FundPayment_b();
                    fundPayment_b.setId(dataSettled.getBusinessdetailsid());
                    fundPayment_b.setTransNumber(transNumber);
                    fundPayment_b.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(FUND_PAYMENT_B_FULLNAME, fundPayment_b);
                } else if (tableName.equals(IBillNumConstant.FUND_COLLECTION_B)) {
                    FundCollection_b fundCollection_b = new FundCollection_b();
                    fundCollection_b.setId(dataSettled.getBusinessdetailsid());
                    fundCollection_b.setTransNumber(transNumber);
                    fundCollection_b.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(FUND_COLLECTION_B_FULLNAME, fundCollection_b);
                } else {
                    SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateTransNumberByDetailId", params);
                }
            }
        }
    }

    /**
     * 记录业务日志
     *
     * @param bizObject
     * @param ctmJSONObject
     */
    public void saveBusinessLog(BizObject bizObject, CtmJSONObject ctmJSONObject, String billnumber) {
        try {
            if ("cmp_fundpayment_b".equals(billnumber)) {
                CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                ctmcmpBusinessLogService.saveBusinessLog(
                        ctmJSONObject,
                        bizObject.getString(ICmpConstant.CODE),
                        IMsgConstant.FUND_PAYMENT_PUSH_DATA_SETTLES,
                        IServicecodeConstant.FUNDPAYMENT,
                        IMsgConstant.FUND_PAYMENT,
                        OperCodeTypes.assubmit.getDefaultOperateName());
            } else if ("cmp_fundcollection_b".equals(billnumber)) {
                CTMCMPBusinessLogService ctmcmpBusinessLogService = AppContext.getBean(CTMCMPBusinessLogService.class);
                ctmcmpBusinessLogService.saveBusinessLog(
                        ctmJSONObject,
                        bizObject.getString(ICmpConstant.CODE),
                        IMsgConstant.FUND_COLLECTION_PUSH_DATA_SETTLES,
                        IServicecodeConstant.FUNDCOLLECTION,
                        IMsgConstant.FUND_COLLECTION,
                        OperCodeTypes.assubmit.getDefaultOperateName());
            }
        } catch (Exception var8) {
            log.error("记录业务日志失败：" + var8.getMessage());
        }
    }

}
