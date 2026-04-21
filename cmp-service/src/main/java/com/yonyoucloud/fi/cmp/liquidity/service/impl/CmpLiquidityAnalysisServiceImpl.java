package com.yonyoucloud.fi.cmp.liquidity.service.impl;

import com.yonyou.iuap.context.YmsContextWrappers;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.yonbip.ctm.liquidity.api.ICtmLiquidityQueryService;
import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityCashQueryConditionDTO;
import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityItemDTO;
import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityQueryConditionDTO;
import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityQueryPageDTO;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquidityAuditStatusEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquidityNetMovementCategoryEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquidityOrderTypeEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquidityPayStatusEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquiditySettleStatusEnum;
import com.yonyoucloud.fi.cmp.liquidity.service.CmpBillLiquidityAnalysisQueryService;
import com.yonyoucloud.fi.cmp.liquidity.service.vo.CmpLiquidityCashQueryConditionDTO;
import com.yonyoucloud.fi.cmp.liquidity.service.vo.CmpLiquidityItemDTO;
import com.yonyoucloud.fi.cmp.liquidity.service.vo.CmpLiquidityQueryConditionDTO;
import com.yonyoucloud.fi.cmp.liquidity.service.vo.CmpLiquidityQueryPageDTO;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <h1>流动性分析：现金管理单据流量表数据提供服务</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-18 10:13
 */
@Slf4j
@Service
@RequiredArgsConstructor()
public class CmpLiquidityAnalysisServiceImpl implements ICtmLiquidityQueryService {

    String QUERY_CMP_LIQUIDITY_ITEM_PAGE_BY_CONDITION = "com.yonyoucloud.fi.cmp.mapper.liquidityanalysis.queryCmpLiquidityItemPageByCondition";

    String QUERY_CMP_LIQUIDITY_ITEM_COUNT_BY_CONDITION = "com.yonyoucloud.fi.cmp.mapper.liquidityanalysis.queryCmpLiquidityItemCountByCondition";

    private final CmpBillLiquidityAnalysisQueryService cmpBillLiquidityAnalysisQueryService;
    private final ExecutorService executorService = YmsContextWrappers.wrap(Executors.newFixedThreadPool(10));

    @Override
    public List<LiquidityItemDTO> queryLiquidityItemsByCondition(LiquidityQueryConditionDTO conditionDTO) throws Exception {

        if (!ValueUtils.isNotEmptyObj(conditionDTO.getBeginDate())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101842"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080076", "流行性分析，开始日期入参不能为空!") /* "流行性分析，开始日期入参不能为空!" */);
        }
        if (!ValueUtils.isNotEmptyObj(conditionDTO.getEndDate())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101843"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080078", "流行性分析，结束日期入参不能为空!") /* "流行性分析，结束日期入参不能为空!" */);
        }
        List<CompletableFuture<List<LiquidityItemDTO>>> completableFutureList = new ArrayList<>();
        List<String> billTypeList = Arrays.asList("fundPayment", "fundCollection", "salaryPay", "foreignPayment", "transferAccount");
        for (String billType : billTypeList) {
            CompletableFuture<List<LiquidityItemDTO>> r = setHandlerDataSync(billType, conditionDTO);
            completableFutureList.add(r);
        }
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
        List<LiquidityItemDTO> liquidityItemDTOS = new ArrayList<>();
        for (CompletableFuture<List<LiquidityItemDTO>> completableFuture : completableFutureList) {
            liquidityItemDTOS.addAll(completableFuture.get());
        }

        for (LiquidityItemDTO liquidityItemDTO : liquidityItemDTOS) {
            String serviceCode = liquidityItemDTO.getServiceCode();
            short payStatus = liquidityItemDTO.getPayStatus().getValue();
            short settleStatus = Short.parseShort(liquidityItemDTO.getSettleStatus().getCode());
            short businessStatus = liquidityItemDTO.getBusinessStatus().getValue();

            switch (serviceCode) {
                case "ficmp0026":
                case "ficmp0024":
                    if (liquidityItemDTO.getIsIntegrateSettlementPlatform()
                            && (
                            businessStatus == LiquidityAuditStatusEnum.UnSubmit.getValue()
                                    || businessStatus == LiquidityAuditStatusEnum.Approval.getValue()
                                    || businessStatus == LiquidityAuditStatusEnum.Retured.getValue()
                    )
                    ) {
                        liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                    }
                    if (!liquidityItemDTO.getIsIntegrateSettlementPlatform()
                            && (
                            Objects.equals(String.valueOf(settleStatus), LiquiditySettleStatusEnum.UNSETTLE.getCode())
                                    || Objects.equals(String.valueOf(settleStatus), LiquiditySettleStatusEnum.INSETTLE.getCode())
                    )
                    ){
                        liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                    }
                    if (!liquidityItemDTO.getIsIntegrateSettlementPlatform()
                            && Objects.equals(String.valueOf(settleStatus), LiquiditySettleStatusEnum.SETTLESUCCESS.getCode())){
                        liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Current);
                    }
                    break;
                case "cmp_salarypaylist":
                    if (payStatus == LiquidityPayStatusEnum.NoPay.getValue()
                            || payStatus==LiquidityPayStatusEnum.PayUnknown.getValue()){
                        liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                    } else if (payStatus == LiquidityPayStatusEnum.Success.getValue()){
                        liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Current);
                    }
                    break;
                case "ficmp0052":
                    if (liquidityItemDTO.getIsIntegrateSettlementPlatform()
                            && (
                            businessStatus == LiquidityAuditStatusEnum.UnSubmit.getValue()
                                    || businessStatus == LiquidityAuditStatusEnum.Approval.getValue()
                                    || businessStatus == LiquidityAuditStatusEnum.Retured.getValue()
                    )
                    ) {
                        liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                    }
                    if (payStatus == LiquidityPayStatusEnum.NoPay.getValue()
                            || payStatus==LiquidityPayStatusEnum.PayUnknown.getValue()
                            || payStatus==LiquidityPayStatusEnum.Paying.getValue()
                            || payStatus==LiquidityPayStatusEnum.PreSuccess.getValue()
                    ){
                        liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                    } else if (payStatus == LiquidityPayStatusEnum.Success.getValue()
                            || payStatus == LiquidityPayStatusEnum.OfflinePay.getValue()
                            || payStatus == LiquidityPayStatusEnum.PartSuccess.getValue()
                    ){
                        liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Current);
                    }
                    break;
                case "cmp_foreignpayment":
                    if (liquidityItemDTO.getIsIntegrateSettlementPlatform()
                            && (
                            businessStatus == LiquidityAuditStatusEnum.UnSubmit.getValue()
                                    || businessStatus == LiquidityAuditStatusEnum.Approval.getValue()
                                    || businessStatus == LiquidityAuditStatusEnum.Retured.getValue()
                    )
                    ) {
                        liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                    }
                    break;
                default:
                    break;
            }
        }


        return liquidityItemDTOS;
    }


    private CompletableFuture<List<LiquidityItemDTO>> setHandlerDataSync(String billType, LiquidityQueryConditionDTO conditionDTO) {
        return CompletableFuture.supplyAsync((() -> {
            switch (billType) {
                case "fundPayment":
                    try {
                        return cmpBillLiquidityAnalysisQueryService.queryFundPaymentLiquidityAnalysisData(conditionDTO);
                    } catch (Exception e) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101844"),"error!");
                    }
                case "fundCollection":
                    try {
                        return cmpBillLiquidityAnalysisQueryService.queryFundCollectionLiquidityAnalysisData(conditionDTO);
                    } catch (Exception e) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101845"),"error!");
                    }
                case "salaryPay":
                    try {
                        return cmpBillLiquidityAnalysisQueryService.querySalaryPayLiquidityAnalysisData(conditionDTO);
                    } catch (Exception e) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101846"),"error!");
                    }
                case "foreignPayment":
                    try {
                        return cmpBillLiquidityAnalysisQueryService.queryForeignPaymentLiquidityAnalysisData(conditionDTO);
                    } catch (Exception e) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101847"),"error!");
                    }
                case "transferAccount":
                    try {
                        return cmpBillLiquidityAnalysisQueryService.queryTransferAccountLiquidityAnalysisData(conditionDTO);
                    } catch (Exception e) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101848"),"error!");
                    }
                default:
                    log.error("未适配单据类型");
            }
            return null;
        }), executorService).handle((r, e) -> {
            if (e != null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101849"),"error!");
            }
            return r;
        });
    }

    @Override
    public LiquidityQueryPageDTO queryLiquidityItemPageByCondition(LiquidityQueryConditionDTO conditionDTO) {

        CmpLiquidityQueryConditionDTO cmpLiquidityQueryConditionDTO = new CmpLiquidityQueryConditionDTO();
        Date beginDate = conditionDTO.getBeginDate();
        if (!ValueUtils.isNotEmptyObj(beginDate)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101842"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080076", "流行性分析，开始日期入参不能为空!") /* "流行性分析，开始日期入参不能为空!" */);
        }
        Date endDate = conditionDTO.getEndDate();
        if (!ValueUtils.isNotEmptyObj(endDate)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101843"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080078", "流行性分析，结束日期入参不能为空!") /* "流行性分析，结束日期入参不能为空!" */);
        }
        cmpLiquidityQueryConditionDTO.setBeginDate(beginDate);
        cmpLiquidityQueryConditionDTO.setEndDate(endDate);
        cmpLiquidityQueryConditionDTO.setAccentitys(conditionDTO.getAccentitys());
        if (CollectionUtils.isNotEmpty(conditionDTO.getBankAccountIds())) {
            cmpLiquidityQueryConditionDTO.setBankAccountIds(conditionDTO.getBankAccountIds());
        }
        cmpLiquidityQueryConditionDTO.setCurrencyId(conditionDTO.getCurrencyId());
       cmpLiquidityQueryConditionDTO.setTransType(conditionDTO.getTransType());
        if (CollectionUtils.isNotEmpty(conditionDTO.getCashLiquidityQuerys())) {
            getCmpCashLiquidyQuerys(conditionDTO, cmpLiquidityQueryConditionDTO);
        }


        LiquidityQueryPageDTO queryPage = conditionDTO.getQueryPage();
        if (!ValueUtils.isNotEmptyObj(queryPage)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101850"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080079", "流行性分析，查询分页信息入参不能为空!") /* "流行性分析，查询分页信息入参不能为空!" */);
        }
        Integer pageIndex = queryPage.getPageIndex();
        if (!ValueUtils.isNotEmptyObj(pageIndex)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101851"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508007A", "流行性分析，查询页入参不能为空!") /* "流行性分析，查询页入参不能为空!" */);
        }
        Integer pageSize = queryPage.getPageSize();
        if (!ValueUtils.isNotEmptyObj(pageSize)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101852"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080075", "流行性分析，查询页大小入参不能为空!") /* "流行性分析，查询页大小入参不能为空!" */);
        }
        if (pageSize > 2000) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101853"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080077", "流行性分析，查询页大小不能超过2000条!") /* "流行性分析，查询页大小不能超过2000条!" */);
        }
        CmpLiquidityQueryPageDTO cmpLiquidityQueryPageDTO = new CmpLiquidityQueryPageDTO();
        cmpLiquidityQueryPageDTO.setPageIndex((pageIndex - 1) * pageSize);
        cmpLiquidityQueryPageDTO.setPageSize(pageSize);
        cmpLiquidityQueryPageDTO.setOrderFields("businessID");
        LiquidityOrderTypeEnum orderTypeEnum = queryPage.getOrderType();
        String orderType;
        if (ValueUtils.isNotEmptyObj(orderTypeEnum)) {
            orderType = orderTypeEnum==LiquidityOrderTypeEnum.ASC ? "asc" : "desc";
        } else {
            orderType = "desc";
        }
        cmpLiquidityQueryPageDTO.setOrderType(orderType);
        cmpLiquidityQueryConditionDTO.setQueryPage(cmpLiquidityQueryPageDTO);
        Integer count = SqlHelper.selectFirst(QUERY_CMP_LIQUIDITY_ITEM_COUNT_BY_CONDITION, cmpLiquidityQueryConditionDTO);
        List<CmpLiquidityItemDTO> result = SqlHelper.selectList(QUERY_CMP_LIQUIDITY_ITEM_PAGE_BY_CONDITION, cmpLiquidityQueryConditionDTO);

        LiquidityQueryPageDTO liquidityQueryPageDTO = new LiquidityQueryPageDTO();
        liquidityQueryPageDTO.setPageIndex(pageIndex);
        liquidityQueryPageDTO.setPageSize(pageSize);
        List<LiquidityItemDTO> list = getLiquidityItemDTOS(result);
        liquidityQueryPageDTO.setQueryResult(list);
        liquidityQueryPageDTO.setTotalCount(count);
        liquidityQueryPageDTO.setPageIndexCount((int) Math.ceil((double) count / pageSize));
        return liquidityQueryPageDTO;
    }

    private static void getCmpCashLiquidyQuerys(LiquidityQueryConditionDTO conditionDTO, CmpLiquidityQueryConditionDTO cmpLiquidityQueryConditionDTO) {
        List<LiquidityCashQueryConditionDTO> cashLiquidityQueryList = conditionDTO.getCashLiquidityQuerys();
        List<CmpLiquidityCashQueryConditionDTO> cmpCashLiquidityQueryList = new ArrayList<>();
        for (LiquidityCashQueryConditionDTO cashLiquidityQuery : cashLiquidityQueryList) {
            CmpLiquidityCashQueryConditionDTO cmpLiquidityCashQueryConditionDTO = new CmpLiquidityCashQueryConditionDTO();
            // 是否集成资金结算
            cmpLiquidityCashQueryConditionDTO.setIsIntegrateSettlementPlatform(cashLiquidityQuery.getIsIntegrateSettlementPlatform());

            // 单据状态
            List<LiquidityAuditStatusEnum> businessStatusList = cashLiquidityQuery.getBusinessStatus();
            if (CollectionUtils.isNotEmpty(businessStatusList)) {
                List<Short> businessStatus = new ArrayList<>();
                for (LiquidityAuditStatusEnum liquidityAuditStatusEnum : businessStatusList) {
                    businessStatus.add(liquidityAuditStatusEnum.getValue());
                }
                cmpLiquidityCashQueryConditionDTO.setBusinessStatus(businessStatus);
            }

            // 结算状态
            List<LiquiditySettleStatusEnum> settleStatusList = cashLiquidityQuery.getSettleStatus();
            if (CollectionUtils.isNotEmpty(settleStatusList)) {
                List<Short> settleStatus = new ArrayList<>();
                for (LiquiditySettleStatusEnum settleStatusEnum : settleStatusList) {
                    settleStatus.add(Short.parseShort(settleStatusEnum.getCode()));
                }
                cmpLiquidityCashQueryConditionDTO.setSettleStatus(settleStatus);
            }

            // 支付状态
            List<LiquidityPayStatusEnum> payStatusList = cashLiquidityQuery.getPayStatus();
            if (CollectionUtils.isNotEmpty(payStatusList)) {
                List<Short> payStatus = new ArrayList<>();
                for (LiquidityPayStatusEnum payStatusEnum : payStatusList) {
                    payStatus.add(payStatusEnum.getValue());
                }
                cmpLiquidityCashQueryConditionDTO.setPayStatus(payStatus);
            }
            cmpCashLiquidityQueryList.add(cmpLiquidityCashQueryConditionDTO);
        }
        cmpLiquidityQueryConditionDTO.setCashLiquidityQuerys(cmpCashLiquidityQueryList);
    }

    private static @NotNull List<LiquidityItemDTO> getLiquidityItemDTOS(List<CmpLiquidityItemDTO> result) {
        List<LiquidityItemDTO> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(result)) {
            for (CmpLiquidityItemDTO cmpLiquidityItemDTO : result) {
                LiquidityItemDTO liquidityItemDTO = new LiquidityItemDTO();
                liquidityItemDTO.setLiquidityProject(cmpLiquidityItemDTO.getLiquidityProject());
                liquidityItemDTO.setInstrument(cmpLiquidityItemDTO.getInstrument());
                liquidityItemDTO.setTransType(cmpLiquidityItemDTO.getTransType());
                liquidityItemDTO.setPlanProgram(cmpLiquidityItemDTO.getPlanProgram());
                liquidityItemDTO.setLaunchBusinessSystem(cmpLiquidityItemDTO.getLaunchBusinessSystem());
                liquidityItemDTO.setAccentity(cmpLiquidityItemDTO.getAccentity());
                liquidityItemDTO.setAccentityName(cmpLiquidityItemDTO.getAccentityName());
                liquidityItemDTO.setOrgId(cmpLiquidityItemDTO.getOrgId());
                liquidityItemDTO.setDeptId(cmpLiquidityItemDTO.getDeptId());
                liquidityItemDTO.setProjectId(cmpLiquidityItemDTO.getProjectId());
                liquidityItemDTO.setBusinessDate(cmpLiquidityItemDTO.getBusinessDate());
                liquidityItemDTO.setLaunchBusiness(cmpLiquidityItemDTO.getLaunchBusiness());
                liquidityItemDTO.setBillNum(cmpLiquidityItemDTO.getBillNum());
                liquidityItemDTO.setBillId(cmpLiquidityItemDTO.getBillId());
                liquidityItemDTO.setServiceCode(cmpLiquidityItemDTO.getServiceCode());
                liquidityItemDTO.setBillDetailNum(cmpLiquidityItemDTO.getBillDetailNum());
                liquidityItemDTO.setPaymentType(cmpLiquidityItemDTO.getPaymentType());
                liquidityItemDTO.setSettlementMethod(cmpLiquidityItemDTO.getSettlementMethod());
                liquidityItemDTO.setOurAccId(cmpLiquidityItemDTO.getOurAccId());
                liquidityItemDTO.setOurAcc(cmpLiquidityItemDTO.getOurAcc());
                liquidityItemDTO.setOurCashAccId(cmpLiquidityItemDTO.getOurCashAccId());
                liquidityItemDTO.setOurCashAcc(cmpLiquidityItemDTO.getOurCashAcc());
                liquidityItemDTO.setNoteId(cmpLiquidityItemDTO.getNoteId());
                liquidityItemDTO.setNoteCode(cmpLiquidityItemDTO.getNoteCode());
                liquidityItemDTO.setExpenseItem(cmpLiquidityItemDTO.getExpenseItem());
                liquidityItemDTO.setContractNum(cmpLiquidityItemDTO.getContractNum());
                liquidityItemDTO.setContractName(cmpLiquidityItemDTO.getContractName());
                liquidityItemDTO.setOppDocId(cmpLiquidityItemDTO.getOppDocId());
                liquidityItemDTO.setOppName(cmpLiquidityItemDTO.getOppName());
                liquidityItemDTO.setOppAcc(cmpLiquidityItemDTO.getOppAcc());
                liquidityItemDTO.setOppAccName(cmpLiquidityItemDTO.getOppAccName());
                liquidityItemDTO.setOppAccType(cmpLiquidityItemDTO.getOppAccType());
                liquidityItemDTO.setOppAccOpenName(cmpLiquidityItemDTO.getOppAccOpenName());
                liquidityItemDTO.setIsOpeningBalance(cmpLiquidityItemDTO.getIsOpeningBalance());
                liquidityItemDTO.setReceiptType(cmpLiquidityItemDTO.getReceiptType());
                liquidityItemDTO.setOriginCurrency(cmpLiquidityItemDTO.getOriginCurrency());
                liquidityItemDTO.setOriginAmount(cmpLiquidityItemDTO.getOriginAmount());
                liquidityItemDTO.setExchangeRateType(cmpLiquidityItemDTO.getExchangeRateType());
                liquidityItemDTO.setRemark(cmpLiquidityItemDTO.getRemark());
                liquidityItemDTO.setPlanSettlementDate(cmpLiquidityItemDTO.getPlanSettlementDate());
                liquidityItemDTO.setBusinessContrastCode(cmpLiquidityItemDTO.getBusinessContrastCode());
                liquidityItemDTO.setExtra(cmpLiquidityItemDTO.getExtra());
                liquidityItemDTO.setBusinessLink(cmpLiquidityItemDTO.getBusinessLink());
                liquidityItemDTO.setIsIntegrateSettlementPlatform(cmpLiquidityItemDTO.getIsIntegrateSettlementPlatform());
                Short businessStatus = cmpLiquidityItemDTO.getBusinessStatus();
                if (businessStatus==LiquidityAuditStatusEnum.UnSubmit.getValue()) {
                    liquidityItemDTO.setBusinessStatus(LiquidityAuditStatusEnum.UnSubmit);
                } else if(businessStatus==LiquidityAuditStatusEnum.Approval.getValue()) {
                    liquidityItemDTO.setBusinessStatus(LiquidityAuditStatusEnum.Approval);
                } else if (businessStatus==LiquidityAuditStatusEnum.Retured.getValue()){
                    liquidityItemDTO.setBusinessStatus(LiquidityAuditStatusEnum.Retured);
                } else if (businessStatus == LiquidityAuditStatusEnum.Passed.getValue()){
                    liquidityItemDTO.setBusinessStatus(LiquidityAuditStatusEnum.Passed);
                } else {
                    liquidityItemDTO.setBusinessStatus(LiquidityAuditStatusEnum.Stopped);
                }
                Short settleStatus = cmpLiquidityItemDTO.getSettleStatus();
                if (Objects.equals(String.valueOf(settleStatus), LiquiditySettleStatusEnum.UNSETTLE.getCode())) {
                    liquidityItemDTO.setSettleStatus(LiquiditySettleStatusEnum.UNSETTLE);
                } else if(Objects.equals(String.valueOf(settleStatus), LiquiditySettleStatusEnum.SETTLESUCCESS.getCode())) {
                    liquidityItemDTO.setSettleStatus(LiquiditySettleStatusEnum.SETTLESUCCESS);
                } else if (Objects.equals(String.valueOf(settleStatus), LiquiditySettleStatusEnum.INSETTLE.getCode())){
                    liquidityItemDTO.setSettleStatus(LiquiditySettleStatusEnum.INSETTLE);
                } else {
                    liquidityItemDTO.setSettleStatus(LiquiditySettleStatusEnum.SETTLEFAIL);
                }
                Short payStatus = cmpLiquidityItemDTO.getPayStatus();
                liquidityItemDTO.setPayStatus(LiquidityPayStatusEnum.find(payStatus));

                String serviceCode = cmpLiquidityItemDTO.getServiceCode();
                switch (serviceCode) {
                    case "ficmp0026":
                    case "ficmp0024":
                        setNetMovementCategory(cmpLiquidityItemDTO, businessStatus, liquidityItemDTO, settleStatus);
                        break;
                    case "cmp_salarypaylist":
                       if (payStatus == LiquidityPayStatusEnum.NoPay.getValue()
                               || payStatus==LiquidityPayStatusEnum.PayUnknown.getValue()){
                           liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                       } else if (payStatus == LiquidityPayStatusEnum.Success.getValue()){
                           liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Current);
                       }
                        break;
                    case "ficmp0052":
                        if (cmpLiquidityItemDTO.getIsIntegrateSettlementPlatform()
                                && (
                                businessStatus == LiquidityAuditStatusEnum.UnSubmit.getValue()
                                        || businessStatus == LiquidityAuditStatusEnum.Approval.getValue()
                                        || businessStatus == LiquidityAuditStatusEnum.Retured.getValue()
                        )
                        ) {
                            liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                        }
                        if (payStatus == LiquidityPayStatusEnum.NoPay.getValue()
                                || payStatus==LiquidityPayStatusEnum.PayUnknown.getValue()
                                || payStatus==LiquidityPayStatusEnum.Paying.getValue()
                                || payStatus==LiquidityPayStatusEnum.PreSuccess.getValue()
                        ){
                            liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                        } else if (payStatus == LiquidityPayStatusEnum.Success.getValue()
                                || payStatus == LiquidityPayStatusEnum.OfflinePay.getValue()
                                || payStatus == LiquidityPayStatusEnum.PartSuccess.getValue()
                        ){
                            liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Current);
                        }
                        break;
                    case "cmp_foreignpayment":
                        if (cmpLiquidityItemDTO.getIsIntegrateSettlementPlatform()
                                && (
                                businessStatus == LiquidityAuditStatusEnum.UnSubmit.getValue()
                                        || businessStatus == LiquidityAuditStatusEnum.Approval.getValue()
                                        || businessStatus == LiquidityAuditStatusEnum.Retured.getValue()
                        )
                        ) {
                            liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
                        }
                        break;
                    default:
                        break;
                }
                list.add(liquidityItemDTO);
            }
        }
        return list;
    }

    private static void setNetMovementCategory(CmpLiquidityItemDTO cmpLiquidityItemDTO, Short businessStatus, LiquidityItemDTO liquidityItemDTO, Short settleStatus) {
        if (cmpLiquidityItemDTO.getIsIntegrateSettlementPlatform()
                && (
                businessStatus == LiquidityAuditStatusEnum.UnSubmit.getValue()
                        || businessStatus == LiquidityAuditStatusEnum.Approval.getValue()
                        || businessStatus == LiquidityAuditStatusEnum.Retured.getValue()
        )
        ) {
            liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
        }
        if (!cmpLiquidityItemDTO.getIsIntegrateSettlementPlatform()
                && (
                Objects.equals(String.valueOf(settleStatus), LiquiditySettleStatusEnum.UNSETTLE.getCode())
                        || Objects.equals(String.valueOf(settleStatus), LiquiditySettleStatusEnum.INSETTLE.getCode())
        )
        ){
            liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Forecast);
        }
        if (!cmpLiquidityItemDTO.getIsIntegrateSettlementPlatform()
                && Objects.equals(String.valueOf(settleStatus), LiquiditySettleStatusEnum.SETTLESUCCESS.getCode())){
            liquidityItemDTO.setNetMovementCategory(LiquidityNetMovementCategoryEnum.Current);
        }
    }


}
