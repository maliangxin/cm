package com.yonyoucloud.fi.cmp.margincommon.service;

import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ruleengine.utils.StringUtil;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.transtype.exception.TransTypeRpcException;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.model.TransTypeQueryParam;
import com.yonyou.ucf.transtype.service.itf.ITransTypeService;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.QuerySettledDetailModel;
import com.yonyoucloud.ctm.stwb.stwbentity.WSettleStatus;
import com.yonyoucloud.ctm.stwb.stwbentity.WSettlementResult;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PaymentType;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.event.constant.IEventCenterConstant;
import com.yonyoucloud.fi.cmp.marginworkbench.service.MarginWorkbenchService;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginService;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.receivemargin.service.ReceiveMarginService;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.SendEventMessageUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 保证金台账管理 业务接口实现*
 *
 * @author xuxbo
 * @date 2023/8/4 10:02
 */

@Slf4j
@Service
public class MarginCommonServiceImpl implements MarginCommonService {

    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    FIBillService fiBillService;

    @Autowired
    ITransTypeService transTypeService;

    @Autowired
    @Qualifier("stwbPayMarginServiceImpl")
    private StwbBillService stwbBillService;

    @Autowired
    MarginWorkbenchService marginWorkbenchService;

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    @Lazy
    private PayMarginService payMarginService;

    @Autowired
    @Lazy
    private ReceiveMarginService receiveMarginService;

    @Resource
    private IApplicationService appService;
    /**
     * 更新支付保证金的结算状态以及结算时间
     * * @param dataSettledDetail
     *
     * @param payMargin
     * @throws Exception
     */
    @Override
    public void updateSettledInfoOfPayMargin(DataSettledDetail dataSettledDetail, PayMargin payMargin) throws Exception {

        try {
            if (dataSettledDetail.getBusinessBillId().equals(payMargin.getId().toString()) && payMargin.getVerifystate().equals(VerifyState.COMPLETED.getValue())) {
                // 结算成功和结算止付 都要调用工作台更新接口
                CtmJSONObject params = new CtmJSONObject();
                //保证金原始业务号
                params.put(ICmpConstant.MARGINBUSINESSNO, payMargin.getMarginbusinessno());
                params.put(ICmpConstant.MARGINAMOUNT, payMargin.getMarginamount());
                params.put(ICmpConstant.NATMARGINAMOUNT, payMargin.getNatmarginamount());
                params.put(ICmpConstant.TRADETYPE, payMargin.getTradetype());
                if (ObjectUtils.isNotEmpty(payMargin.getConversionamount())) {
                    params.put(ICmpConstant.CONVERSIONAMOUNT, payMargin.getConversionamount());
                    params.put(ICmpConstant.NATCONVERSIONAMOUNT, payMargin.getNatconversionamount());
                }
                params.put(ICmpConstant.SETTLEFLAG, payMargin.getSettleflag());
                params.put(ICmpConstant.SRC_ITEM, payMargin.getSrcitem());

                Short settlestatus = payMargin.getSettlestatus();
                Date settlesuccesstime = payMargin.getSettlesuccesstime();
                String pushtimes = payMargin.getPushtimes();
                if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())) { //全部结算成功
                    //更新第一次推送的结算状态以及结算成功时间
                    if (ICmpConstant.FIRST.equals(payMargin.getPushtimes())) {
                        if (!payMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue())) {
                            payMargin.setSettlestatus(FundSettleStatus.SettleSuccess.getValue());
                            settlestatus = FundSettleStatus.SettleSuccess.getValue();
                            //只有第一次推送的时候 才需要支付保证金更新工作台的金额
                            params.put(ICmpConstant.ACTION, ICmpConstant.SETTLESUCCESS);
                            params.put(ICmpConstant.PAYMARGIN, payMargin);
                            marginWorkbenchService.payMarginWorkbenchUpdate(params);
                        }
//                        payMargin.setSettlesuccesstime(DateUtils.dateParse(dataSettledDetail.getPayDownData(), DateUtils.DATE_TIME_PATTERN));
                        settlesuccesstime = DateUtils.dateParse(dataSettledDetail.getPayDownData(), DateUtils.DATE_TIME_PATTERN);

                        generateVoucher(payMargin);
                    }

                    //  当第一条结算成功 并且 为同名账户划转 并且第二次还没推送  则推送第二次 并且推送的结算状态为 已结算补单
                    if ((payMargin.getSettlestatus().equals(FundSettleStatus.SettleSuccess.getValue()) || payMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue()))
                            && payMargin.getSamenametransferflag() == 1 && ICmpConstant.FIRST.equals(payMargin.getPushtimes())) {
                        //2024.10.28 保证金需求 当监听到下游付款结算单已经结算完成，并且“同名账户划转”字段为：是 且“内部单位银行账户名称”字段为空 的支付保证金不生成对方单位的待结算单据
                        if (ObjectUtils.isNotEmpty(payMargin.getOurbankaccount())) {

                            if(dataSettledDetail.getDataSettledDistribute().get(0).getSettleSuccBizTime() != null){
                                payMargin.put("secondExpectSettleDate",dataSettledDetail.getDataSettledDistribute().get(0).getSettleSuccBizTime());
                            }else{
                                payMargin.put("secondExpectSettleDate",DateUtils.dateParse(dataSettledDetail.getPayDownData(), DateUtils.DATE_TIME_PATTERN));
                            }
                            log.info("同名账户划转，对方单位银行账户名称不为空，不生成对方单位的待结算单据");
                            //设置推送次数为第二次
                            payMargin.setPushtimes(ICmpConstant.SECOND);
                            pushtimes = ICmpConstant.SECOND;
                            //推送资金结算
                            List<BizObject> currentBillList = new ArrayList<>();
                            currentBillList.add(payMargin);
                            stwbBillService.pushBill(currentBillList, false);// 推送资金结算
                        }
                    }

                } else if (String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {
                    //只更新结算状态
                    if (!payMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue())) {
                        payMargin.setSettlestatus(FundSettleStatus.SettleFailed.getValue());
                        settlestatus = FundSettleStatus.SettleFailed.getValue();
                        params.put(ICmpConstant.ACTION, ICmpConstant.STOPPAY);
                        params.put(ICmpConstant.PAYMARGIN, payMargin);
                        marginWorkbenchService.payMarginWorkbenchUpdate(params);
                    }

                }

                // 推送上游领域结算结果
                payMargin.setSettlesuccesstime(settlesuccesstime);
                SendEventMessageUtils.sendEventMessageEos(payMargin, IEventCenterConstant.MARGIN_ACCOUNT, IEventCenterConstant.MARGIN_ACCOUNT_SETTLE);
                PayMargin currentBill = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, payMargin.getId());
                currentBill.setSettlestatus(settlestatus);
                if (currentBill.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue() ||  currentBill.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue()) {
                    // 付款类的计算明细设置本方财资统一对账码
                    if (StringUtil.isEmpty(currentBill.getOurcheckno())) {
                        currentBill.setOurcheckno(dataSettledDetail.getCheckIdentificationCode());
                    } else {
                        // 收款类的计算明细设置对方财资统一对账码
                        currentBill.setOppcheckno(dataSettledDetail.getCheckIdentificationCode());
                    }
                }
                currentBill.setSettlesuccesstime(settlesuccesstime);
                currentBill.setPushtimes(pushtimes);
                currentBill.setVoucherstatus(payMargin.getVoucherstatus());
                currentBill.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(PayMargin.ENTITY_NAME, currentBill);
                //结算状态更新为结算成功
                //是否占预算为预占成功时，删除预占，实占预算，实占成功后，是否占预算为实占成功。
                payMarginService.budgetAfterSettleStatusChange(currentBill,false);
            }

        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100195"),e.getMessage());
        }
    }


    @Override
    public void updateSettledInfoOfReceiveMargin(DataSettledDetail dataSettledDetail, ReceiveMargin receiveMargin) throws Exception {

        try {
            if (dataSettledDetail.getBusinessBillId().equals(receiveMargin.getId().toString())) {
                // 结算成功和结算止付 都要调用工作台更新接口
                CtmJSONObject params = new CtmJSONObject();
                //保证金原始业务号
                params.put(ICmpConstant.MARGINBUSINESSNO, receiveMargin.getMarginbusinessno());
                params.put(ICmpConstant.MARGINAMOUNT, receiveMargin.getMarginamount());
                params.put(ICmpConstant.NATMARGINAMOUNT, receiveMargin.getNatmarginamount());
                params.put(ICmpConstant.TRADETYPE, receiveMargin.getTradetype());
                if (ObjectUtils.isNotEmpty(receiveMargin.getConversionamount())) {
                    params.put(ICmpConstant.CONVERSIONAMOUNT, receiveMargin.getConversionamount());
                    params.put(ICmpConstant.NATCONVERSIONAMOUNT, receiveMargin.getNatconversionamount());
                }
                params.put(ICmpConstant.SETTLEFLAG, receiveMargin.getSettleflag());
                params.put(ICmpConstant.SRC_ITEM, receiveMargin.getSrcitem());

                if (String.valueOf(WSettlementResult.AllSuccess.getValue()).equals(dataSettledDetail.getWsettlementResult())) { //全部结算成功
                    //更新第一次推送的结算状态以及结算成功时间
                    if (!receiveMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue())) {
                        receiveMargin.setSettlestatus(FundSettleStatus.SettleSuccess.getValue());
                        params.put(ICmpConstant.ACTION, ICmpConstant.SETTLESUCCESS);
                        params.put(ICmpConstant.RECMARGIN, receiveMargin);
                        marginWorkbenchService.recMarginWorkbenchUpdate(params);
                    }

                    receiveMargin.setSettlesuccesstime(DateUtils.dateParse(dataSettledDetail.getPayDownData(), DateUtils.DATE_TIME_PATTERN));

                    generateVoucher(receiveMargin);
                } else if (String.valueOf(WSettlementResult.AllFaital.getValue()).equals(dataSettledDetail.getWsettlementResult())) {
                    //只更新结算状态
                    if (!receiveMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue())) {
                        receiveMargin.setSettlestatus(FundSettleStatus.SettleFailed.getValue());
                        params.put(ICmpConstant.ACTION, ICmpConstant.STOPPAY);
                        params.put(ICmpConstant.RECMARGIN, receiveMargin);
                        marginWorkbenchService.recMarginWorkbenchUpdate(params);
                    }
                }
                // 财资统一对账码
                if (receiveMargin.getSettlestatus() == FundSettleStatus.SettleSuccess.getValue() || receiveMargin.getSettlestatus() == FundSettleStatus.SettlementSupplement.getValue()) {
                    receiveMargin.setCheckno(dataSettledDetail.getCheckIdentificationCode());
                }
                // 推送上游领域结算结果
                SendEventMessageUtils.sendEventMessageEos(receiveMargin, IEventCenterConstant.MARGIN_ACCOUNT, IEventCenterConstant.MARGIN_ACCOUNT_SETTLE);
                receiveMargin.remove("pubts");
                receiveMargin.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, receiveMargin);
                //结算状态！=已结算补单、结算成功、部分成功、为空时，删除实占预算执行记录。
                receiveMarginService.budgetAfterSettleStatusChange(receiveMargin, false);
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100196"),e.getMessage());
        }
    }

    /**
     * <h2>生成转换支付保证金</h2>
     *
     * @param bizObject : 支付保证金id
     * @author Sun GuoCai
     * @since 2023/8/10 15:32
     */
    @Override
    public PayMargin generateConversionPayMargin(BizObject bizObject) {
        try {
            PayMargin payMargin = MetaDaoHelper.findById(PayMargin.ENTITY_NAME, bizObject.getId());
            payMargin.setConversionmarginid(payMargin.getId().toString());
            payMargin.setConversionmargincode(payMargin.getCode());
            payMargin.setCode(null);
            payMargin.setId(null);
            payMargin.setMarginamount(payMargin.getConversionamount());
            payMargin.setNatmarginamount(payMargin.getNatconversionamount());
            payMargin.setConversionmarginflag((short) 0);
            payMargin.setPaymenttype(PaymentType.FundPayment.getValue());
            payMargin.setSettleflag((short) 0);
            payMargin.setMarginbusinessno(payMargin.getNewmarginbusinessno());
            payMargin.setMargintype(payMargin.getNewmargintype());
            payMargin.setProject(payMargin.getNewproject());
            payMargin.setDept(payMargin.getNewdept());
            payMargin.setExpectedretrievaldate(payMargin.getNewexpectedretrievaldate());
            payMargin.setNewexpectedretrievaldate(null);
            payMargin.setNewmarginbusinessno(null);
            payMargin.setNewmargintype(null);
            payMargin.setNewproject(null);
            payMargin.setNewdept(null);
            payMargin.setConversionamount(null);
            payMargin.setNatconversionamount(null);
            payMargin.setMarginbalance(null);
            // 设置交易类型为支付保证金
            payMargin.setTradetype(getTransTypeId(ICmpConstant.CM_CMP_PAYMARGIN, "cmp_paymargin_payment"));
            // 设置审核状态为已审核
            payMargin.setIsWfControlled(false);
            payMargin.setVerifystate(VerifyState.COMPLETED.getValue());
            Date currentDate = new Date();
            payMargin.setAuditDate(currentDate);
            payMargin.setAuditTime(currentDate);
            payMargin.setAuditorId(AppContext.getCurrentUser().getId());
            payMargin.setAuditor(AppContext.getCurrentUser().getName());

            payMargin.set("isConvert", true);
            BillDataDto dataDto = new BillDataDto();
            dataDto.setBillnum(IBillNumConstant.CMP_PAYMARGIN);
            payMargin.setEntityStatus(EntityStatus.Insert);
            dataDto.setData(CtmJSONObject.toJSONString(payMargin));
            RuleExecuteResult ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
            Map<String,Object> result = (HashMap) ruleExecuteResult.getData();
            //PayMargin payMargin1 = (PayMargin) result;
            PayMargin conversionPayMargin = new PayMargin();
            conversionPayMargin.init(result);
            conversionPayMargin.set("_entityName",PayMargin.ENTITY_NAME);

            // 调用工作台更新接口
            CtmJSONObject params = new CtmJSONObject();
            //保证金原始业务号
            params.put(ICmpConstant.MARGINBUSINESSNO, conversionPayMargin.getMarginbusinessno());
            params.put(ICmpConstant.MARGINAMOUNT, conversionPayMargin.getMarginamount());
            params.put(ICmpConstant.NATMARGINAMOUNT, conversionPayMargin.getNatmarginamount());
            params.put(ICmpConstant.TRADETYPE, conversionPayMargin.getTradetype());
            if (ObjectUtils.isNotEmpty(conversionPayMargin.getConversionamount())) {
                params.put(ICmpConstant.CONVERSIONAMOUNT, conversionPayMargin.getConversionamount());
                params.put(ICmpConstant.NATCONVERSIONAMOUNT, conversionPayMargin.getNatconversionamount());
            }
            params.put(ICmpConstant.SETTLEFLAG, conversionPayMargin.getSettleflag());
            params.put(ICmpConstant.SRC_ITEM, conversionPayMargin.getSrcitem());
            //先调用save 后调用update
            params.put(ICmpConstant.PAYMARGIN, conversionPayMargin);
            String marginvirtualaccount = marginWorkbenchService.payMarginWorkbenchSave(params);
            params.put(ICmpConstant.ACTION, ICmpConstant.AUDIT);
            marginWorkbenchService.payMarginWorkbenchUpdate(params);
            // 生成凭证
            generateVoucher(conversionPayMargin);
            conversionPayMargin.setMarginvirtualaccount(Long.valueOf(marginvirtualaccount));
            conversionPayMargin.setSettlestatus(FundSettleStatus.SettleSuccess.getValue());
            conversionPayMargin.setSettlesuccesstime(new Date());
            conversionPayMargin.setEntityStatus(EntityStatus.Update);
            conversionPayMargin.setPubts(null); // 避免由于pubts过老导致更新不了
            MetaDaoHelper.update(PayMargin.ENTITY_NAME, conversionPayMargin);
            return conversionPayMargin;
        }catch (Exception e) {
            log.error("generateConversionPayMargin Exception", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100197"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18DBD04C05C80011", "生成转换支付保证金逻辑处理异常，请检查！") /* "生成转换支付保证金逻辑处理异常，请检查！" */ +"=>"+e.getMessage());
        }
    }

    @Nullable
    private String getTransTypeId(String formId, String transTypeCode){
        try {
            TransTypeQueryParam transTypeQueryParam = new TransTypeQueryParam();
            transTypeQueryParam.setTenantId(InvocationInfoProxy.getTenantid());
            transTypeQueryParam.setFormId(formId);
            transTypeQueryParam.setTransTypeCode(transTypeCode);
            List<BdTransType> transTypeList = transTypeService.queryTransTypes(transTypeQueryParam);
            if (CollectionUtils.isNotEmpty(transTypeList)) {
                return transTypeList.get(0).getId();
            } {
                log.error("margin bill query tradeTpe fail! formId={}, transTypeCode={}", formId, transTypeCode);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100198"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18DBD04C05C80012", "根据参数未查询到交易类型，请检查！") /* "根据参数未查询到交易类型，请检查！" */);
            }
        } catch (TransTypeRpcException e) {
            log.error("margin bill query tradeTpe error! formId={}, transTypeCode={}, errorMsg={}"
                    , formId, transTypeCode, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100199"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18DBD04C05C80013", "获取交易类型id异常，请检查！") /* "获取交易类型id异常，请检查！" */ );
        }
    }

    /**
     * <h2>生成转换收到保证金</h2>
     *
     * @param bizObject : 收到保证金id
     * @author Sun GuoCai;
     * @since 2023/8/10 15:32
     */
    @Override
    public void generateConversionReceiveMargin(BizObject bizObject) {
        try {
            ReceiveMargin receiveMargin = MetaDaoHelper.findById(ReceiveMargin.ENTITY_NAME, bizObject.getId());
            if (receiveMargin != null) {
                receiveMargin.setConversionmarginid(receiveMargin.getId().toString());
                receiveMargin.setConversionmargincode(receiveMargin.getCode());
                receiveMargin.setCode(null);
                receiveMargin.setId(null);
                receiveMargin.setMarginamount(receiveMargin.getConversionamount());
                receiveMargin.setNatmarginamount(receiveMargin.getNatconversionamount());
                receiveMargin.setConversionmarginflag((short) 0);
                receiveMargin.setPaymenttype(PaymentType.FundCollection.getValue());
                receiveMargin.setSettleflag((short) 0);
                receiveMargin.setMarginbusinessno(receiveMargin.getNewmarginbusinessno());
                receiveMargin.setMargintype(receiveMargin.getNewmargintype());
                receiveMargin.setProject(receiveMargin.getNewproject());
                receiveMargin.setDept(receiveMargin.getNewdept());
                receiveMargin.setNewmarginbusinessno(null);
                receiveMargin.setNewmargintype(null);
                receiveMargin.setNewproject(null);
                receiveMargin.setNewdept(null);
                receiveMargin.setConversionamount(null);
                receiveMargin.setNatconversionamount(null);
                receiveMargin.setAutorefundflag((short) 0);
                receiveMargin.setRefunddate(null);
                receiveMargin.setRefundsettleflag((short) 0);
                receiveMargin.setMarginbalance(null);
                receiveMargin.setLatestreturndate(receiveMargin.getNewlatestreturndate());
                // 设置审核状态为已审核
                receiveMargin.setVerifystate(VerifyState.COMPLETED.getValue());
                Date currentDate = new Date();
                receiveMargin.setAuditDate(currentDate);
                receiveMargin.setAuditTime(currentDate);
                receiveMargin.setAuditorId(AppContext.getCurrentUser().getId());
                receiveMargin.setAuditor(AppContext.getCurrentUser().getName());
                // 设置交易类型为收到保证金
                receiveMargin.setTradetype(getTransTypeId(ICmpConstant.CM_CMP_RECEIVEMARGIN, "cmp_receivemargin_receive"));
                receiveMargin.set("isConvert", true);
                BillDataDto dataDto = new BillDataDto();
                dataDto.setBillnum(IBillNumConstant.CMP_RECEIVEMARGIN);
                receiveMargin.setEntityStatus(EntityStatus.Insert);
                dataDto.setData(CtmJSONObject.toJSONString(receiveMargin));
                RuleExecuteResult ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
//                BizObject result = (BizObject) ruleExecuteResult.getData();
                Map<String, Object> result = (Map<String, Object>) ruleExecuteResult.getData();

                ReceiveMargin receiveMargin1 = new ReceiveMargin();
                receiveMargin1.init(result);
                receiveMargin1.set("_entityName", ReceiveMargin.ENTITY_NAME);

                bizObject.set(ICmpConstant.CONVERSIONMARGINID, result.get(ICmpConstant.PRIMARY_ID));
                bizObject.set(ICmpConstant.CONVERSIONMARGINCODE, result.get(ICmpConstant.CODE));

                // 调用工作台更新接口
                CtmJSONObject params = new CtmJSONObject();
                //保证金原始业务号
                params.put(ICmpConstant.MARGINBUSINESSNO, receiveMargin1.getMarginbusinessno());
                params.put(ICmpConstant.MARGINAMOUNT, receiveMargin1.getMarginamount());
                params.put(ICmpConstant.NATMARGINAMOUNT, receiveMargin1.getNatmarginamount());
                params.put(ICmpConstant.TRADETYPE, receiveMargin1.getTradetype());
                if (ObjectUtils.isNotEmpty(receiveMargin1.getConversionamount())) {
                    params.put(ICmpConstant.CONVERSIONAMOUNT, receiveMargin1.getConversionamount());
                    params.put(ICmpConstant.NATCONVERSIONAMOUNT, receiveMargin1.getNatconversionamount());
                }
                params.put(ICmpConstant.SETTLEFLAG, receiveMargin1.getSettleflag());
                params.put(ICmpConstant.SRC_ITEM, receiveMargin1.getSrcitem());
                //先调用save 后调用update
                params.put(ICmpConstant.RECMARGIN, receiveMargin1);
                String marginvirtualaccount = marginWorkbenchService.recMarginWorkbenchSave(params);
                params.put(ICmpConstant.ACTION, ICmpConstant.AUDIT);
                marginWorkbenchService.recMarginWorkbenchUpdate(params);
                generateVoucher(receiveMargin1);

                // 设置审核状态为已审核
                receiveMargin1.setVerifystate(VerifyState.COMPLETED.getValue());
                receiveMargin1.setMarginvirtualaccount(Long.valueOf(marginvirtualaccount));
                receiveMargin1.setSettlestatus(FundSettleStatus.SettleSuccess.getValue());
                receiveMargin1.setEntityStatus(EntityStatus.Update);
                receiveMargin1.setSettlesuccesstime(new Date());
                receiveMargin1.setPubts(null);  // 避免由于pubts过老导致更新不了
                MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, receiveMargin1);
            }
        }catch (Exception e) {
            log.error("generateConversionReceiveMargin Exception", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100197"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18DBD04C05C80011", "生成转换支付保证金逻辑处理异常，请检查！") /* "生成转换支付保证金逻辑处理异常，请检查！" */ +"=>"+e.getMessage());
        }
    }

    @Override
    public boolean useByMulPayMargin(String workBenchId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name("marginvirtualaccount").eq(workBenchId));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<PayMargin> payMarginList = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null);
        return payMarginList != null && payMarginList.size() < 2;
    }

    @Override
    public boolean useByMulRecMargin(String workBenchId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name("marginvirtualaccount").eq(workBenchId));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<ReceiveMargin> payMarginList = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null);
        return payMarginList != null && payMarginList.size() < 2;
    }

    @Override
    public boolean findPayMargin(String marginbusinessno) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name("marginbusinessno").eq(marginbusinessno));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<PayMargin> payMarginList = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null);
        return payMarginList == null || payMarginList.size() < 1;
    }

    @Override
    public boolean findRecMargin(String marginbusinessno) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name("marginbusinessno").eq(marginbusinessno));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<ReceiveMargin> payMarginList = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null);
        return payMarginList == null || payMarginList.size() < 1;
    }

    @Override
    public void checkHasSettlementBill(String businessDetailsId) throws Exception {
        // 需求，支付、收到保证金已结算补单传结算时，支持上游撤回
        // 调用结算的查询接口，检查待结算数据是否已经生成结算单
        QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
        querySettledDetailModel.setWdataorigin(8); // 来源业务系统 8-现金管理
        querySettledDetailModel.setBusinessDetailsId(businessDetailsId); // 业务单据明细ID -> 转账单-ID

        try {
            List<DataSettledDetail> dataSettledDetailList = RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).querySettledDetails(querySettledDetailModel);

            // 检查是否有待结算数据已生成结算单
            boolean hasSettlementGenerated = false;
            if (ObjectUtils.isNotEmpty(dataSettledDetailList)) {
                for (DataSettledDetail detail : dataSettledDetailList) {
                    // 如果有任何一条待结算数据已生成结算单，则不允许撤回
                    if (ArrayUtils.contains(new short[] {WSettleStatus.SettleDone.getValue(), WSettleStatus.SettleProssing.getValue()}, Short.parseShort(detail.getWsettleStatus()))) {
                        hasSettlementGenerated = true;
                        break;
                    }
                }
            }

            // 如果已生成结算单，则按原逻辑提示不允许撤回
            if (hasSettlementGenerated) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100171"),
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6FF92405880003",
                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540046E", "结算状态为已结算补单并且推送结算为是并且凭证状态是未生成,不能进行撤回！") /* "结算状态为已结算补单并且推送结算为是并且凭证状态是未生成,不能进行撤回！" */) /* "结算状态为已结算补单并且推送结算为是并且凭证状态是未生成,不能进行撤回！" */);
            }
            // 如果未生成结算单，则允许继续执行撤回操作（不抛出异常）
            // 这里不需要额外处理，继续执行撤回流程
        } catch (Exception e) {
            log.error("查询待结算数据异常，按原逻辑处理", e);
            // 如果调用接口异常，默认按不能撤回处理
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100171"),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A6FF92405880003",
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540046E", "结算状态为已结算补单并且推送结算为是并且凭证状态是未生成,不能进行撤回！") /* "结算状态为已结算补单并且推送结算为是并且凭证状态是未生成,不能进行撤回！" */) /* "结算状态为已结算补单并且推送结算为是并且凭证状态是未生成,不能进行撤回！" */);
        }
    }

    public void generateVoucher(ReceiveMargin receiveMargin) throws Exception {
        ApplicationVO appVoEvnt = appService.findByTenantIdAndApplicationCode(InvocationInfoProxy.getTenantid(), "EVNT");
        receiveMargin.put("_entityName", ReceiveMargin.ENTITY_NAME);
        receiveMargin.setEntityStatus(EntityStatus.Update);
        if (appVoEvnt == null || !appVoEvnt.isEnable()) {
            log.error("客户环境未安装事项中台服务");
            receiveMargin.setVoucherstatus(VoucherStatus.NONCreate.getValue());
        } else {
            CtmJSONObject generateRedResult = cmpVoucherService.generateVoucherWithResult(receiveMargin);
            if (!generateRedResult.getBoolean("dealSucceed")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102474"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00252", "单据【") /* "单据【" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00253", "】发送会计平台失败：") /* "】发送会计平台失败：" */ + generateRedResult.get("message"));
            }
            if (generateRedResult.get("genVoucher") != null && !generateRedResult.getBoolean("genVoucher")) {
                receiveMargin.setVoucherstatus(VoucherStatus.NONCreate.getValue());
            } else {
                receiveMargin.setVoucherstatus(VoucherStatus.POSTING.getValue());
            }
        }
        updateReceiveMarginVoucherStatus(receiveMargin);
    }

    private void updateReceiveMarginVoucherStatus(ReceiveMargin receiveMargin) throws Exception {
        ReceiveMargin updateReceiveMargin = new ReceiveMargin();
        updateReceiveMargin.setId(receiveMargin.getId());
        updateReceiveMargin.setVoucherstatus(receiveMargin.getVoucherstatus());
        updateReceiveMargin.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(ReceiveMargin.ENTITY_NAME, updateReceiveMargin);
    }

    public void generateVoucher(PayMargin payMargin) throws Exception {
        ApplicationVO appVoEvnt = appService.findByTenantIdAndApplicationCode(InvocationInfoProxy.getTenantid(), "EVNT");
        payMargin.setEntityStatus(EntityStatus.Update);
        payMargin.put("_entityName", PayMargin.ENTITY_NAME);
        if (appVoEvnt == null || !appVoEvnt.isEnable()) {
            log.error("客户环境未安装事项中台服务");
            payMargin.setVoucherstatus(VoucherStatus.NONCreate.getValue());
        } else {
            CtmJSONObject generateRedResult = cmpVoucherService.generateVoucherWithResult(payMargin);
            if (!generateRedResult.getBoolean("dealSucceed")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102474"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00252", "单据【") /* "单据【" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00253", "】发送会计平台失败：") /* "】发送会计平台失败：" */ + generateRedResult.get("message"));
            }
            if (generateRedResult.get("genVoucher") != null && !generateRedResult.getBoolean("genVoucher")) {
                payMargin.setVoucherstatus(VoucherStatus.NONCreate.getValue());
            } else {
                payMargin.setVoucherstatus(VoucherStatus.POSTING.getValue());
            }
        }
        updatePaymarginVoucherStatus(payMargin);
    }

    private void updatePaymarginVoucherStatus(PayMargin payMargin) throws Exception {
        PayMargin updatePayMargin = new PayMargin();
        updatePayMargin.setId(payMargin.getId());
        updatePayMargin.setVoucherstatus(payMargin.getVoucherstatus());
        updatePayMargin.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(PayMargin.ENTITY_NAME, updatePayMargin);
    }

}
