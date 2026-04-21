package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.QuerySettledDetailModel;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.ctm.stwb.settleapply.vo.PushOrder;
import com.yonyoucloud.ctm.stwb.unifiedsettle.pubitf.IUnifiedSettlePubService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatusConverter;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.stwb.IFundPaymentPushStwbBillService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.business.StwbBillBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@Transactional
public class StwbPaymentBillServiceImpl extends StwbBillCommonService implements IFundPaymentPushStwbBillService {

    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;

    @Autowired
    private IUnifiedSettlePubService unifiedSettlePubService;
    // 资金付款单子表
    private static final String CMP_FUNDPAYMENT_B = "cmp_fundpayment_b";

    /**
     * 审核推送资金付款单据
     *
     * @param billList
     */
    @Override
    public void pushBill(List<BizObject> billList, boolean bCheck) throws Exception {
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        if (enableSimplify) {
            return;
        }
        FundPayment fundPayment = (FundPayment) billList.get(0);
        FundPayment payment = (FundPayment) fundPayment.clone();
        IFundCommonService fundCommonService = CtmAppContext.getBean(IFundCommonService.class);
        boolean checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundPayment.getValue());
        Map<String, Object> params = new HashMap<>();
        params.put("bCheck", bCheck);
        params.put("checkFundPlanIsEnabled", checkFundPlanIsEnabled);
        if (bCheck) {
            List<FundPayment_b> fundPayment_bs = fundPayment.FundPayment_b();
            List<FundPayment_b> fundPaymentSubList = new ArrayList<>();
            for (FundPayment_b fundPaymentB : fundPayment_bs) {
                if (!EntityStatus.Delete.equals(fundPaymentB.getEntityStatus())) {
                    fundPaymentSubList.add(fundPaymentB);
                }
            }
            payment.set("FundPayment_b", fundPaymentSubList);
        }
        builtSystem(payment, StwbBillBuilder.builderFundPayment(fundPayment, params), CMP_FUNDPAYMENT_B);
    }


    /**
     * 审核推送资金付款单据
     *
     * @param billList
     */
    @Override
    public void pushBillSimple(BizObject bizobject) throws Exception {
        FundPayment fundPayment = (FundPayment) bizobject;
        FundPayment payment = (FundPayment) fundPayment.clone();
        //资金收付结算是否简强
        boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
        boolean settleflagBool = fundPayment.getSettleflag() == null || fundPayment.getSettleflag() == 1;
        if (enableSimplify && settleflagBool) {
            Map<String, Date> detailIdMapPubts = new HashMap<>();
            List<FundPayment_b> pushsubList = new ArrayList<>();
            List<String> topushList = new ArrayList<>();
            if (!fundPayment.FundPayment_b().isEmpty()) {
                List<FundPayment_b> subList = fundPayment.FundPayment_b();
                for (FundPayment_b item : subList) {
                    if (item.getOriSum().compareTo(BigDecimal.ZERO) != 0 && (null == item.getEntrustReject() || item.getEntrustReject() != 1)) {
                        detailIdMapPubts.put(item.getId().toString(), item.getPubts());
                        pushsubList.add(item);
                        topushList.add(item.getId().toString());
                    }
                }
                fundPayment.setFundPayment_b(pushsubList);
            }
            builtSystem(fundPayment, StwbBillBuilder.builderSettleMap(fundPayment, "cmpfundpaymentToSimpleSettle", "ctm-cmp.cmp_fundpayment",
                    null, topushList), FundPayment.ENTITY_NAME, payment.getCode(), detailIdMapPubts);
            return;
        }
    }


    /**
     * 弃审删除资金付款单据
     *
     * @param billList
     */
    @Override
    public void deleteBill(List<BizObject> billList) {
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
        FundPayment fundPayment = (FundPayment) billList.get(0);
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        ctmJSONObject.put("fundPayment", fundPayment);
        try {
            QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
            querySettledDetailModel.setBusinessId(fundPayment.getId().toString());
            querySettledDetailModel.setWdataorigin(8);// 来源业务系统，现金管理
            List detailsIds = new ArrayList();
            List<FundPayment_b> listb = fundPayment.FundPayment_b();
            for (FundPayment_b bill_b : listb) {
                if (StringUtils.isNotEmpty(bill_b.getTransNumber())) {
                    detailsIds.add(bill_b.getId().toString());
                }
            }
            if (CollectionUtils.isNotEmpty(detailsIds)) {
                querySettledDetailModel.setBusinessDetailsIds(detailsIds);
                ctmJSONObject.put("data", querySettledDetailModel);
                RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).datasettledDelete(querySettledDetailModel);
            }
            //资金收付结算是否简强
            boolean enableSimplify = settleParamPubQueryService.simplifyEnable();
            if (enableSimplify) {
                // 调用删除统一结算单接口
                List<String> idList = new ArrayList<>();
                idList.add(fundPayment.getId());
                unifiedSettlePubService.deleteUnifiedSettle(FundPayment.ENTITY_NAME, idList.toArray(new String[0]), PushOrder.FIRST);
            }
            for (FundPayment_b bill_b : listb) {
                //结算状态为结算止付 是否委托驳回为是 表示已经被委托拒绝 不进行更新
                if (bill_b.getFundSettlestatus().getValue() != FundSettleStatus.SettleFailed.getValue() && bill_b.getEntrustReject() != 1) {
                    Map<String, Object> params = new HashMap<>();
                    FundPayment_b fundPayment_b = new FundPayment_b();
                    /*params.put("ytenantId", AppContext.getYTenantId());
                    params.put("detailId", bill_b.getId());// 子表明细id集合
                    params.put("transNumber", null);// 待结算数据id
                    params.put("tableName", CMP_FUNDPAYMENT_B);// 资金收付款单表名
                    params.put("settleSuccessTime", null);
                    params.put("actualSettlementExchangeRateType", null);
                    params.put("actualSettlementAmount", null);
                    params.put("actualSettlementExchangeRate", null);*/
                    //若结算状态不是已结算补单，则结算状态修改为待结算
                    if (bill_b.getFundSettlestatus().getValue() != FundSettleStatus.SettlementSupplement.getValue()
                            && bill_b.getFundSettlestatus().getValue() != FundSettleStatus.Refund.getValue() ) {
                        //params.put("settlestatus", FundSettleStatus.WaitSettle.getValue());
                        fundPayment_b.setFundSettlestatus(FundSettleStatus.WaitSettle);
                        fundPayment_b.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPayment_b.getFundSettlestatus()));
                    } else {
                        //params.put("settlesuccessSum", BigDecimal.ZERO);
                        fundPayment_b.setSettlesuccessSum(BigDecimal.ZERO);
                    }
                    fundPayment_b.setId(bill_b.getId());
                    fundPayment_b.setTransNumber(null);
                    fundPayment_b.setSettleSuccessTime(null);
                    fundPayment_b.setActualSettlementExchangeRateType(null);
                    fundPayment_b.setActualSettlementAmount(null);
                    fundPayment_b.setActualSettlementExchangeRate(null);
                    fundPayment_b.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPayment_b);
                    //SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateTransNumberByDetailId", params);
                }
            }
        } catch (Exception e) {
            ctmJSONObject.put("errorMag", e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100882"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802E5", "删除结算单据失败：") /* "删除结算单据失败：" */ + e.getMessage());
        } finally {
//            saveBusinessLog(fundPayment, ctmJSONObject);
        }
    }

    /**
     * 记录业务日志
     *
     * @param bizObject
     * @param ctmJSONObject
     */
    public void saveBusinessLog(BizObject bizObject, CtmJSONObject ctmJSONObject) {
        try {
            ctmcmpBusinessLogService.saveBusinessLog(
                    CtmJSONObject.toJSONString(ctmJSONObject),
                    bizObject.getString(ICmpConstant.CODE),
                    IMsgConstant.FUND_PAYMENT_DELETE_DATA_SETTLES,
                    IServicecodeConstant.FUNDPAYMENT,
                    IMsgConstant.FUND_PAYMENT,
                    OperCodeTypes.batchclear.getDefaultOperateName());
        } catch (Exception var8) {
            log.error("记录业务日志失败：" + var8.getMessage());
        }
    }

}
