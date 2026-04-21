package com.yonyoucloud.fi.cmp.fundpayment.service;

import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatusConverter;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FundPaymentCommonServiceImpl implements FundPaymentCommonService{
    private final ICmpSendEventService cmpSendEventService;
    @Override
    public void updateSubSettleStatusAndSettleAmountForFundPayment(CtmJSONArray array, List<FundPayment_b> fundPaymentBList) throws Exception {
        for (Object o : array) {
            CtmJSONObject obj = new CtmJSONObject((Map<String, Object>) o);
            Long id = obj.getLong("id");
            FundPayment_b currentBill = MetaDaoHelper.findById(FundPayment_b.ENTITY_NAME, id);
            if (!ValueUtils.isNotEmptyObj(currentBill)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102164"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BE", "根据id未查询到数据，id为：") /* "根据id未查询到数据，id为：" */+ id);
            }
            BigDecimal settleSuccessSum = obj.getBigDecimal("settlesuccessSum");
            Short settleStatus = obj.getShort("settlestatus");
            if (settleStatus!= FundSettleStatus.SettleSuccess.getValue()
                    && settleStatus!=FundSettleStatus.SettleFailed.getValue()
                    && settleStatus!=FundSettleStatus.PartSuccess.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102165"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BF", "结算状态只能为结算成功、结算止付或部分成功！，id为：") /* "结算状态只能为结算成功、结算止付或部分成功！，id为：" */+ id);
            }
            BigDecimal settleErrorSum = obj.getBigDecimal("settleerrorSum");
            Date settleSuccessTime = ValueUtils.isNotEmptyObj(obj.getDate("settleSuccessTime"))
                    ? obj.getDate("settleSuccessTime"): null;
            currentBill.setFundSettlestatus(FundSettleStatus.find(settleStatus));
            currentBill.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(currentBill.getFundSettlestatus()));
            currentBill.setSettlesuccessSum(settleSuccessSum);
            currentBill.setSettleerrorSum(settleErrorSum);
            currentBill.setSettleSuccessTime(settleSuccessTime);
            BigDecimal oriSum = currentBill.getOriSum();
            BigDecimal settleAmountSum = BigDecimalUtils.safeAdd(settleSuccessSum, settleErrorSum);
            if (settleStatus == FundSettleStatus.SettleSuccess.getValue()
                    && (oriSum.compareTo(settleSuccessSum) != 0 || BigDecimal.ZERO.compareTo(settleErrorSum) != 0)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102166"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B8", "传入的单据明细行结算状态为结算成功，结算成功金额必须等于原币金额且结算止付金额等于0！明细行id为：") /* "传入的单据明细行结算状态为结算成功，结算成功金额必须等于原币金额且结算止付金额等于0！明细行id为：" */+ currentBill.getId());
            }
            if (settleStatus == FundSettleStatus.SettleFailed.getValue()
                    && (oriSum.compareTo(settleErrorSum) != 0 || BigDecimal.ZERO.compareTo(settleSuccessSum) != 0)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102167"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BC", "传入的单据明细行结算状态为结算止付，结算止付金额必须等于原币金额且结算成功金额等于0！明细行id为：") /* "传入的单据明细行结算状态为结算止付，结算止付金额必须等于原币金额且结算成功金额等于0！明细行id为：" */+ currentBill.getId());
            }
            if(settleStatus == FundSettleStatus.PartSuccess.getValue() && oriSum.compareTo(settleAmountSum) != 0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102168"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BD", "传入的单据明细行结算状态为部分成功，结算成功金额加上结算止付金额不等于原币金额！明细行id为：") /* "传入的单据明细行结算状态为部分成功，结算成功金额加上结算止付金额不等于原币金额！明细行id为：" */+ currentBill.getId());
            }
            fundPaymentBList.add(currentBill);
        }
        EntityTool.setUpdateStatus(fundPaymentBList);
        MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentBList);
    }

    @Override
    public void updateSubSettleStatusAndSettleAmountForFundCollection(CtmJSONArray array, List<FundCollection_b> fundCollectionBList) throws Exception {
        for (Object o : array) {
            CtmJSONObject obj = (CtmJSONObject) o;
            Long id = obj.getLong("id");
            FundCollection_b currentBill = MetaDaoHelper.findById(FundCollection_b.ENTITY_NAME, id);
            if (!ValueUtils.isNotEmptyObj(currentBill)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102164"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BE", "根据id未查询到数据，id为：") /* "根据id未查询到数据，id为：" */+ id);
            }
            BigDecimal settleSuccessSum = obj.getBigDecimal("settlesuccessSum");
            Short settleStatus = obj.getShort("settlestatus");
            if (settleStatus!=FundSettleStatus.SettleSuccess.getValue()
                    && settleStatus!=FundSettleStatus.SettleFailed.getValue()
                    && settleStatus!=FundSettleStatus.PartSuccess.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102165"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BF", "结算状态只能为结算成功、结算止付或部分成功！，id为：") /* "结算状态只能为结算成功、结算止付或部分成功！，id为：" */+ id);
            }
            BigDecimal settleErrorSum = obj.getBigDecimal("settleerrorSum");
            Date settleSuccessTime = ValueUtils.isNotEmptyObj(obj.getDate("settleSuccessTime"))
                    ? obj.getDate("settleSuccessTime"): null;
            currentBill.setFundSettlestatus(FundSettleStatus.find(settleStatus));
            currentBill.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(currentBill.getFundSettlestatus()));
            currentBill.setSettlesuccessSum(settleSuccessSum);
            currentBill.setSettleerrorSum(settleErrorSum);
            currentBill.setSettleSuccessTime(settleSuccessTime);
            BigDecimal oriSum = currentBill.getOriSum();
            BigDecimal settleAmountSum = BigDecimalUtils.safeAdd(settleSuccessSum, settleErrorSum);
            if (settleStatus == FundSettleStatus.SettleSuccess.getValue()
                    && (oriSum.compareTo(settleSuccessSum) != 0 || BigDecimal.ZERO.compareTo(settleErrorSum) != 0)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102166"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B8", "传入的单据明细行结算状态为结算成功，结算成功金额必须等于原币金额且结算止付金额等于0！明细行id为：") /* "传入的单据明细行结算状态为结算成功，结算成功金额必须等于原币金额且结算止付金额等于0！明细行id为：" */+ currentBill.getId());
            }
            if (settleStatus == FundSettleStatus.SettleFailed.getValue()
                    && (oriSum.compareTo(settleErrorSum) != 0 || BigDecimal.ZERO.compareTo(settleSuccessSum) != 0)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102167"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BC", "传入的单据明细行结算状态为结算止付，结算止付金额必须等于原币金额且结算成功金额等于0！明细行id为：") /* "传入的单据明细行结算状态为结算止付，结算止付金额必须等于原币金额且结算成功金额等于0！明细行id为：" */+ currentBill.getId());
            }
            if(settleStatus == FundSettleStatus.PartSuccess.getValue() && oriSum.compareTo(settleAmountSum) != 0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102168"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BD", "传入的单据明细行结算状态为部分成功，结算成功金额加上结算止付金额不等于原币金额！明细行id为：") /* "传入的单据明细行结算状态为部分成功，结算成功金额加上结算止付金额不等于原币金额！明细行id为：" */+ currentBill.getId());
            }
            fundCollectionBList.add(currentBill);
        }
        EntityTool.setUpdateStatus(fundCollectionBList);
        MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollectionBList);
    }

    @Override
    public void updateSettleSuccessTimeAndGeneratorVoucherForFundPayment(List<FundPayment_b> fundPaymentBList) throws Exception {
        HashMap<String, List<FundPayment_b>> fundPaymentBMap = fundPaymentBList.stream()
                .collect(Collectors.groupingBy(FundPayment_b::getMainid, HashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<FundPayment_b>> fundPaymentBEntry : fundPaymentBMap.entrySet()) {
            String mainId = fundPaymentBEntry.getKey();
            BizObject bizObject = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, mainId, 2);
            if (bizObject.getShort("settleflag") ==1){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102169"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BA", "传结算单据，不允许回传结算结果！") /* "传结算单据，不允许回传结算结果！" */);
            }
            short verifyState = Short.parseShort(bizObject.get("verifystate").toString());
            if (verifyState != VerifyState.COMPLETED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102170"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B9", "未审核的单据，不允许回传结算结果！") /* "未审核的单据，不允许回传结算结果！" */);
            }
            List<FundPayment_b> fundPaymentBListDB = bizObject.getBizObjects(ICmpConstant.FUND_PAYMENT_B, FundPayment_b.class);
            List<FundPayment_b> fundPaymentBListIncoming = fundPaymentBEntry.getValue();
            Short settleSuccessPost = bizObject.getShort("settleSuccessPost");
            bizObject.set("_entityName", FundPayment.ENTITY_NAME);
            if (settleSuccessPost == 0) {
                auditAfterGeneratorVoucherPayment(fundPaymentBListIncoming, bizObject);
            } else {
                settleAfterGeneratorVoucherPayment(fundPaymentBListIncoming, fundPaymentBListDB, bizObject);
            }

        }
    }

    @Override
    public void updateSettleSuccessTimeAndGeneratorVoucherForFundCollection(List<FundCollection_b> fundCollectionBList) throws Exception {
        HashMap<String, List<FundCollection_b>> fundCollectionBMap = fundCollectionBList.stream()
                .collect(Collectors.groupingBy(FundCollection_b::getMainid, HashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<FundCollection_b>> fundCollectionBEntry : fundCollectionBMap.entrySet()) {
            String mainId = fundCollectionBEntry.getKey();
            BizObject bizObject = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, mainId, 2);
            if (bizObject.getShort("settleflag") ==1){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102169"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BA", "传结算单据，不允许回传结算结果！") /* "传结算单据，不允许回传结算结果！" */);
            }
            short verifyState = Short.parseShort(bizObject.get("verifystate").toString());
            if (verifyState != VerifyState.COMPLETED.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102170"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B9", "未审核的单据，不允许回传结算结果！") /* "未审核的单据，不允许回传结算结果！" */);
            }
            List<FundCollection_b> fundCollectionBListDB = bizObject.getBizObjects("FundCollection_b", FundCollection_b.class);
            List<FundCollection_b> fundCollectionBListIncoming = fundCollectionBEntry.getValue();
            Short settleSuccessPost = bizObject.getShort("settleSuccessPost");
            bizObject.set("_entityName", FundCollection.ENTITY_NAME);
            if (settleSuccessPost == 0) {
                auditAfterGeneratorVoucherCollection(fundCollectionBListIncoming, bizObject);
            } else {
                settleAfterGeneratorVoucherCollection(fundCollectionBListIncoming, fundCollectionBListDB, bizObject);
            }

        }
    }


    private void settleAfterGeneratorVoucherCollection(List<FundCollection_b> fundCollectionBListIncoming, List<FundCollection_b> fundCollectionBListDB, BizObject bizObject) throws Exception {
        if (bizObject.getShort(ICmpConstant.VOUCHER_STATUS) == VoucherStatus.POST_SUCCESS.getValue()
                || bizObject.getShort(ICmpConstant.VOUCHER_STATUS) == VoucherStatus.Created.getValue() ) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102171"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C0", "已更新结算结果生成凭证，不允许再次更新结算结果") /* "已更新结算结果生成凭证，不允许再次更新结算结果" */);
        }
        int incomingSize = fundCollectionBListIncoming.size();
        int dbSize = fundCollectionBListDB.size();
        if (incomingSize > dbSize) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102172"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BB", "传入的单据明细行数量比数据库对应单据的明细行数据多！请确认！") /* "传入的单据明细行数量比数据库对应单据的明细行数据多！请确认！" */);
        }
        updateSettleSuccessTimeCollection(fundCollectionBListDB, fundCollectionBListIncoming, bizObject);
        if (dbSize == incomingSize) {
            CtmJSONObject billClue = new CtmJSONObject();
            billClue.put("srcBusiId", bizObject.getId().toString());
            cmpSendEventService.sendSimpleEvent(bizObject, billClue);
        } else {
            Map<Object, FundCollection_b> dbMap = fundCollectionBListDB.stream().collect(Collectors.toMap(FundCollection_b::getId, item -> item));
            Map<Object, FundCollection_b> incomingMap = fundCollectionBListIncoming.stream().collect(Collectors.toMap(FundCollection_b::getId, item -> item));
            List<FundCollection_b> dataList = new ArrayList<>();
            for (Object o : dbMap.keySet()) {
                if(ValueUtils.isNotEmptyObj(incomingMap.get(o))){
                    dataList.add(incomingMap.get(o));
                } else {
                    dataList.add(dbMap.get(o));
                }
            }
            boolean allMatch = dataList.stream().allMatch(this::isSettleCompleteFundCollection);
            if (allMatch){
                CtmJSONObject billClue = new CtmJSONObject();
                billClue.put("srcBusiId", bizObject.getId().toString());
                cmpSendEventService.sendSimpleEvent(bizObject, billClue);
            }
        }
    }

    private boolean isSettleCompleteFundCollection(FundCollection_b item) {
        BigDecimal oriSum = item.getOriSum();
        BigDecimal settleSuccessSum = item.getSettlesuccessSum();
        BigDecimal settleErrorSum = item.getSettleerrorSum();
        BigDecimal settleAmountSum = BigDecimalUtils.safeAdd(settleSuccessSum, settleErrorSum);
        short settleStatus = item.getFundSettlestatus().getValue();
        switch (settleStatus){
            case 3:
                return settleSuccessSum.compareTo(oriSum) ==0;
            case 4:
                return settleErrorSum.compareTo(oriSum) ==0;
            case 5:
                return oriSum.compareTo(settleAmountSum) == 0;
            default:
                return false;
        }
    }

    private void auditAfterGeneratorVoucherCollection(List<FundCollection_b> fundCollectionBListIncoming, BizObject bizObject) throws Exception {
        for (FundCollection_b fundCollectionB : fundCollectionBListIncoming) {
            BigDecimal settleErrorSum = ValueUtils.isNotEmptyObj(fundCollectionB.getSettleerrorSum())
                    ? fundCollectionB.getSettleerrorSum() : BigDecimal.ZERO;
            if (settleErrorSum.compareTo(BigDecimal.ZERO) > 0) {
                bizObject.set("FundCollection_b", new ArrayList<>().add(fundCollectionB));
                CtmJSONObject billClue = new CtmJSONObject();
                billClue.put("classifier", fundCollectionB.getId().toString());
                billClue.put("srcBusiId", bizObject.getId().toString());
                billClue.put("billVersion", fundCollectionB.getPubts().getTime());
                cmpSendEventService.sendSimpleEvent(bizObject, billClue);
            }
        }
        if (bizObject.getShort(ICmpConstant.VOUCHER_STATUS) == VoucherStatus.Empty.getValue()) {
            CtmJSONObject billClue = new CtmJSONObject();
            billClue.put("srcBusiId", bizObject.getId().toString());
            cmpSendEventService.sendSimpleEvent(bizObject, billClue);
        }
    }

    private void settleAfterGeneratorVoucherPayment(List<FundPayment_b> fundPaymentBListIncoming, List<FundPayment_b> fundPaymentBListDB, BizObject bizObject) throws Exception {
        if (bizObject.getShort(ICmpConstant.VOUCHER_STATUS) == VoucherStatus.POST_SUCCESS.getValue()
                || bizObject.getShort(ICmpConstant.VOUCHER_STATUS) == VoucherStatus.Created.getValue() ) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102171"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C0", "已更新结算结果生成凭证，不允许再次更新结算结果") /* "已更新结算结果生成凭证，不允许再次更新结算结果" */);
        }
        int incomingSize = fundPaymentBListIncoming.size();
        int dbSize = fundPaymentBListDB.size();
        if (incomingSize > dbSize) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102172"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800BB", "传入的单据明细行数量比数据库对应单据的明细行数据多！请确认！") /* "传入的单据明细行数量比数据库对应单据的明细行数据多！请确认！" */);
        }
        updateSettleSuccessTimePayment(fundPaymentBListDB, fundPaymentBListIncoming, bizObject);
        if (dbSize == incomingSize) {
            CtmJSONObject billClue = new CtmJSONObject();
            billClue.put("srcBusiId", bizObject.getId().toString());
            cmpSendEventService.sendSimpleEvent(bizObject, billClue);
        } else {
            Map<Object, FundPayment_b> dbMap = fundPaymentBListDB.stream().collect(Collectors.toMap(FundPayment_b::getId, item -> item));
            Map<Object, FundPayment_b> incomingMap = fundPaymentBListIncoming.stream().collect(Collectors.toMap(FundPayment_b::getId, item -> item));
            List<FundPayment_b> dataList = new ArrayList<>();
            for (Object o : dbMap.keySet()) {
                if(ValueUtils.isNotEmptyObj(incomingMap.get(o))){
                    dataList.add(incomingMap.get(o));
                } else {
                    dataList.add(dbMap.get(o));
                }
            }
            boolean allMatch = dataList.stream().allMatch(this::isSettleCompleteFundPayment);
            if (allMatch){
                CtmJSONObject billClue = new CtmJSONObject();
                billClue.put("srcBusiId", bizObject.getId().toString());
                cmpSendEventService.sendSimpleEvent(bizObject, billClue);
            }
        }
    }


    private boolean isSettleCompleteFundPayment(FundPayment_b item) {
        BigDecimal oriSum = item.getOriSum();
        BigDecimal settleSuccessSum = item.getSettlesuccessSum();
        BigDecimal settleErrorSum = item.getSettleerrorSum();
        BigDecimal settleAmountSum = BigDecimalUtils.safeAdd(settleSuccessSum, settleErrorSum);
        short settleStatus = item.getFundSettlestatus().getValue();
        switch (settleStatus){
            case 3:
                return settleSuccessSum.compareTo(oriSum) ==0;
            case 4:
                return settleErrorSum.compareTo(oriSum) ==0;
            case 5:
                return oriSum.compareTo(settleAmountSum) == 0;
            default:
                return false;
        }
    }

    private void auditAfterGeneratorVoucherPayment(List<FundPayment_b> fundPaymentBListIncoming, BizObject bizObject) throws Exception {
        for (FundPayment_b fundPaymentB : fundPaymentBListIncoming) {
            BigDecimal settleErrorSum = ValueUtils.isNotEmptyObj(fundPaymentB.getSettleerrorSum())
                    ? fundPaymentB.getSettleerrorSum() : BigDecimal.ZERO;
            if (settleErrorSum.compareTo(BigDecimal.ZERO) > 0) {
                bizObject.set("FundPayment_b", new ArrayList<>().add(fundPaymentB));
                CtmJSONObject billClue = new CtmJSONObject();
                billClue.put("classifier", fundPaymentB.getId().toString());
                billClue.put("srcBusiId", bizObject.getId().toString());
                billClue.put("billVersion", fundPaymentB.getPubts().getTime());
                cmpSendEventService.sendSimpleEvent(bizObject, billClue);
            }
        }
        if (bizObject.getShort(ICmpConstant.VOUCHER_STATUS) == VoucherStatus.Empty.getValue()) {
            CtmJSONObject billClue = new CtmJSONObject();
            billClue.put("srcBusiId", bizObject.getId().toString());
            cmpSendEventService.sendSimpleEvent(bizObject, billClue);
        }
    }

    private void updateSettleSuccessTimePayment(List<FundPayment_b> fundPaymentBListDB, List<FundPayment_b> fundPaymentBListIncoming, BizObject bizObject) throws Exception {
        List<Date> dateListData = fundPaymentBListDB.stream()
                .map(FundPayment_b::getSettleSuccessTime)
                .filter(ValueUtils::isNotEmptyObj)
                .collect(Collectors.toList());
        List<Date> dateListBusiness = fundPaymentBListIncoming.stream()
                .map(FundPayment_b::getSettleSuccessTime)
                .filter(ValueUtils::isNotEmptyObj)
                .collect(Collectors.toList());
        Date settleSuccessTimeSub = null;
        if (dateListData.isEmpty() && dateListBusiness.isEmpty()) {
            return;
        }
        if (!dateListData.isEmpty() && !dateListBusiness.isEmpty()) {
            Date dateListDataDate = Collections.max(dateListData);
            Date dateListBusinessDate = Collections.max(dateListBusiness);
            settleSuccessTimeSub = DateUtils.dateCompare(dateListDataDate, dateListBusinessDate) >= 1 ? dateListDataDate : dateListBusinessDate;
        }
        if (!dateListData.isEmpty() && dateListBusiness.isEmpty()) {
            settleSuccessTimeSub = Collections.max(dateListData);
        }
        if (dateListData.isEmpty() && !dateListBusiness.isEmpty()) {
            settleSuccessTimeSub = Collections.max(dateListBusiness);
        }
        // 取出结算成功的最大结算时间
        Date settleSuccessTimeMain = bizObject.getDate("settleSuccessTime");
        if (!ValueUtils.isNotEmptyObj(settleSuccessTimeSub)) {
            return;
        }
        boolean subFlag = ValueUtils.isNotEmptyObj(settleSuccessTimeMain) && DateUtils.dateCompare(settleSuccessTimeSub, settleSuccessTimeMain) > 1;
        if ((!ValueUtils.isNotEmptyObj(settleSuccessTimeMain)) || subFlag) {
            bizObject.put("settleSuccessTime", settleSuccessTimeSub);
            // 更新主表结算成功时间
            bizObject.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundPayment.ENTITY_NAME, bizObject);
        }
    }
    private void updateSettleSuccessTimeCollection(List<FundCollection_b> fundCollectionBListDB, List<FundCollection_b> fundCollectionBListIncoming, BizObject bizObject) throws Exception {
        List<Date> dateListData = fundCollectionBListDB.stream()
                .map(FundCollection_b::getSettleSuccessTime)
                .filter(ValueUtils::isNotEmptyObj)
                .collect(Collectors.toList());
        List<Date> dateListBusiness = fundCollectionBListIncoming.stream()
                .map(FundCollection_b::getSettleSuccessTime)
                .filter(ValueUtils::isNotEmptyObj)
                .collect(Collectors.toList());
        Date settleSuccessTimeSub = null;
        if (dateListData.isEmpty() && dateListBusiness.isEmpty()) {
            return;
        }
        if (!dateListData.isEmpty() && !dateListBusiness.isEmpty()) {
            Date dateListDataDate = Collections.max(dateListData);
            Date dateListBusinessDate = Collections.max(dateListBusiness);
            settleSuccessTimeSub = DateUtils.dateCompare(dateListDataDate, dateListBusinessDate) >= 1 ? dateListDataDate : dateListBusinessDate;
        }
        if (!dateListData.isEmpty() && dateListBusiness.isEmpty()) {
            settleSuccessTimeSub = Collections.max(dateListData);
        }
        if (dateListData.isEmpty() && !dateListBusiness.isEmpty()) {
            settleSuccessTimeSub = Collections.max(dateListBusiness);
        }
        // 取出结算成功的最大结算时间
        Date settleSuccessTimeMain = bizObject.getDate("settleSuccessTime");
        if (!ValueUtils.isNotEmptyObj(settleSuccessTimeSub)) {
            return;
        }
        boolean subFlag = ValueUtils.isNotEmptyObj(settleSuccessTimeMain) && DateUtils.dateCompare(settleSuccessTimeSub, settleSuccessTimeMain) > 1;
        if ((!ValueUtils.isNotEmptyObj(settleSuccessTimeMain)) || subFlag) {
            bizObject.put("settleSuccessTime", settleSuccessTimeSub);
            // 更新主表结算成功时间
            bizObject.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundCollection.ENTITY_NAME, bizObject);
        }
    }


}
