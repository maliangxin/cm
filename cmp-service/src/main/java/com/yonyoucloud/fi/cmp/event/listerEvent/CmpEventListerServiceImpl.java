package com.yonyoucloud.fi.cmp.event.listerEvent;

import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.DeliveryStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.enums.EventCenterEnum;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill_b;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.stwb.IFundCollectionPushStwbBillService;
import com.yonyoucloud.fi.cmp.stwb.IFundPaymentPushStwbBillService;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * <h1>CmpEventListerServiceImpl</h1>
 * 事件名称：业务事项应答；事件编码：eeac_busievent_rep
 * 监听过账状态
 *
 * @author Sun GuoCai
 * @version 1.0
 * @file CmpEventListerServiceImpl
 * @date 2024/2/20 16:25
 */
@Service
@Slf4j
public class CmpEventListerServiceImpl implements IEventReceiveService {
    private static final String VOUCHER_STATUS = "voucherstatus";
    private static final String CMP_CMP = "ctm-cmp";
    @Resource
    private IFundPaymentPushStwbBillService fundPaymentPushStwbBillService;
    @Resource
    private IFundCollectionPushStwbBillService fundCollectionPushStwbBillService;

    @Override
    public String onEvent(BusinessEvent businessEvent, String queueName) {
        // 判断是否为过账状态事件监听
        if (isEventStatusBackwrite(businessEvent)) {
            // 过账状态逻辑的回写
            try {
                return handleEventStatusBackWrite(businessEvent);
            } catch (Exception e) {
                log.error("handleEventStatusBackWrite error:{}", e.getMessage(), e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100043"), e.getMessage());
            }
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }

    private boolean isEventStatusBackwrite(BusinessEvent businessEvent) {
        return EventCenterEnum.EVENT_STATUS_BACK_WRITE.getSourceId().equals(businessEvent.getSourceID()) &&
                EventCenterEnum.EVENT_STATUS_BACK_WRITE.getEventType().equals(businessEvent.getEventType());
    }

    private String handleEventStatusBackWrite(BusinessEvent businessEvent) throws Exception {
        String routingListenerTag = businessEvent.getRoutingListenerTag();
        String responseType = getResponseType(routingListenerTag, businessEvent);
        return JsonUtils.toJsonString(new EventResponseVO(Boolean.TRUE, responseType));
    }

    private String getResponseType(String routingListenerTag, BusinessEvent businessEvent) throws Exception {
        // 不是现金管理的单据过账消息直接返回
        if (!CMP_CMP.equals(routingListenerTag)) {
            return JsonUtils.toJsonString(EventResponseVO.success());
        }
        CtmJSONArray array = CtmJSONArray.parseArray(businessEvent.getUserObject());
        // 事件消息体为空直接返回
        if (arrayIsEmpty(array)) {
            return JsonUtils.toJsonString(EventResponseVO.success());
        }
        // 更新过账状态逻辑
        return handleEventResponse(array);
    }


    private String handleEventResponse(CtmJSONArray array) throws Exception {
        for (int i = 0; i < array.size(); i++) {
            // 构造EventInfo类对象
            EventInfo eventInfo = extractEventInfo(array.getJSONObject(i));
            // 判断是止付单据，直接返回
            if (eventInfo.isFundBill && isStopPosting(eventInfo.srcExtraInfo)) {
                return constructResponse();
            }
            String lockKey = eventInfo.fullName + eventInfo.srcBusId;
            if (JournalBill.ENTITY_NAME.equals(eventInfo.fullName)) {
                lockKey = eventInfo.fullName + eventInfo.srcBusId + eventInfo.srcClassifier;
            }
            CtmLockTool.executeInOneServiceLock(lockKey, 90L, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
                if (lockStatus == LockStatus.GETLOCK_FAIL) {
                    // 枷锁失败
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100044"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080025", "监听凭证号回写事件加锁异常！") /* "监听凭证号回写事件加锁异常！" */);
                }
                try {
                    // 保持幂等性
                    BizObject bizObject = MetaDaoHelper.findById(eventInfo.fullName, eventInfo.srcBusId, 2);
                    // 校验版本号，解决消息的无序性引发的问题
                    if (validateEventDataVersion(eventInfo, bizObject) || validateVoucherVersion(eventInfo, bizObject)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100045"), constructVersionMismatchResponse(eventInfo, bizObject));
                    }
                    Map<String, Object> params = buildParams(eventInfo);
                    // 校验单据是否为可以过账的单据（结算成功，已审核等），不是直接返回
                    if (verifyBill(eventInfo.fullName, bizObject, eventInfo.isFundBill))
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100046"), JsonUtils.toJsonString(EventResponseVO.success()));
                    // 获取单据对应的表名
                    String tableName = extractTableName(eventInfo);
                    params.put("tableName", tableName);
                    // 更新单据凭证状态
                    updateVoucherStatus(eventInfo, shouldPushStwb(eventInfo), getBillNum(eventInfo), params);
                } catch (Exception e) {
                    log.error("CmpEventListerServiceImpl.error:userObject = {}, errorMsg = {}", array, e.getMessage());
                    throw e;
                }
            });
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }

    private boolean shouldPushStwb(EventInfo eventInfo) throws Exception {
        String billNum = getBillNum(eventInfo);
        return isPushStwb(eventInfo.fullName, billNum, eventInfo.srcBusId);
    }

    private String extractTableName(EventInfo eventInfo) {
        String billNum = getBillNum(eventInfo);
        return getTableName(eventInfo.fullName, billNum);
    }

    private String getBillNum(EventInfo eventInfo) {
        return (ValueUtils.isNotEmptyObj(eventInfo.srcExtraInfo)
                && eventInfo.srcExtraInfo.split("\\|").length > 1)
                ? String.valueOf(eventInfo.srcExtraInfo.split("\\|")[1]) : null;
    }

    private boolean validateEventDataVersion(EventInfo eventInfo, BizObject bizObject) {
        Long fiEventDataVersionDb = bizObject.getLong(ICmpConstant.FI_EVENT_DATA_VERSION);
        /*if(fiEventDataVersionDb != null){
            //CZFW-520714 精确到秒，有些数据会精确到毫秒，导致校验不过，导致后续更新凭证相关的逻辑没有走
            fiEventDataVersionDb = (fiEventDataVersionDb / 1000) * 1000;
        }*/
        return ValueUtils.isNotEmptyObj(fiEventDataVersionDb) && eventInfo.fiEventDataVersion < fiEventDataVersionDb;
    }

    private boolean validateVoucherVersion(EventInfo eventInfo, BizObject bizObject) {
        Long voucherVersionDb = bizObject.getLong(ICmpConstant.VOUCHER_VERSION);
        /*if(voucherVersionDb != null){
            //精确到秒，有些数据会精确到毫秒，导致校验不过，导致后续更新凭证相关的逻辑没有走
            voucherVersionDb = (voucherVersionDb / 1000) * 1000;
        }*/
        return ValueUtils.isNotEmptyObj(voucherVersionDb)
                && ValueUtils.isNotEmptyObj(eventInfo.voucherVersion)
                && eventInfo.voucherVersion < voucherVersionDb;
    }

    private String constructVersionMismatchResponse(EventInfo eventInfo, BizObject bizObject) {
        if (validateEventDataVersion(eventInfo, bizObject)) {
            return CtmJSONObject.toJSONString(new EventResponseVO(Boolean.FALSE,
                    "Single bill posting version does not match, directly return! Message event input posting version:"
                            + eventInfo.fiEventDataVersion
                            + ", Database posting version:"
                            + bizObject.getLong(ICmpConstant.FI_EVENT_DATA_VERSION)));
        }
        if (validateVoucherVersion(eventInfo, bizObject)) {
            return CtmJSONObject.toJSONString(new EventResponseVO(Boolean.FALSE,
                    "Event accounting posting version does not match, directly return! Message event input posting version:"
                            + eventInfo.voucherVersion
                            + ", Database posting version:"
                            + bizObject.getLong(ICmpConstant.VOUCHER_VERSION)));
        }
        return "";
    }

    private Map<String, Object> buildParams(EventInfo eventInfo) {
        HashMap<String, Object> params = new HashMap<>(16);
        params.put("fiEventDataVersion", eventInfo.fiEventDataVersion);
        params.put(ICmpConstant.VOUCHER_VERSION, eventInfo.voucherVersion);
        params.put(ICmpConstant.PRIMARY_ID, Long.parseLong(eventInfo.srcBusId));
        params.put("postingMsg", eventInfo.postingMsg);
        return params;
    }

    private EventInfo extractEventInfo(CtmJSONObject jsonObject) {
        EventInfo eventInfo = new EventInfo();
        CtmJSONObject srcObject = jsonObject.getJSONObject("eventInfo");
        eventInfo.srcBusId = srcObject.getString("srcBusiId");
        eventInfo.srcExtraInfo = srcObject.getString("srcExtraInfo");
        eventInfo.fiEventDataVersion = srcObject.getLong("srcBillVersion");
        eventInfo.voucherVersion = srcObject.getLong("voucherVersion");
        eventInfo.fullName = getFullName(eventInfo.srcExtraInfo);
        eventInfo.isFundBill = isFundBill(eventInfo.fullName);
        eventInfo.code = jsonObject.get(ICmpConstant.CODE).toString();
        eventInfo.postingMsg = jsonObject.get("message").toString();
        eventInfo.srcClassifier = srcObject.getString("srcClassifier");
        return eventInfo;
    }

    private String constructResponse() {
        return JsonUtils.toJsonString(new EventResponseVO(true, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400670", "止付过账成功，直接返回！") /* "止付过账成功，直接返回！" */));
    }

    private boolean isFundBill(String fullName) {
        return FundCollection.ENTITY_NAME.equals(fullName) || FundPayment.ENTITY_NAME.equals(fullName);
    }

    private boolean isStopPosting(String srcExtraInfo) {
        String[] result = srcExtraInfo.split("\\|");
        int length = result.length;
        return length > 2 && ValueUtils.isNotEmptyObj(String.valueOf(srcExtraInfo.split("\\|")[2]));
    }

    private String getFullName(String srcExtraInfo) {
        return ValueUtils.isNotEmptyObj(srcExtraInfo) ? String.valueOf(srcExtraInfo.split("\\|")[0]) : null;
    }

    static class EventInfo {
        String srcBusId;
        String srcExtraInfo;
        String fullName;
        boolean isFundBill;
        Long fiEventDataVersion;
        Long voucherVersion;
        String code;
        String postingMsg;
        String srcClassifier;
    }

    private static boolean isPushStwb(String fullName, String billNum, String srcBusId) throws Exception {
        boolean pushStwb = true;
        if (FundPayment.ENTITY_NAME.equals(fullName) || IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
            FundPayment currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, srcBusId, 3);
            if (StringUtils.isNotEmpty(currentBill.FundPayment_b().get(0).getTransNumber()) || currentBill.getSettleflag() == 0) {
                pushStwb = false;
            }
        } else if (FundCollection.ENTITY_NAME.equals(fullName) || IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
            FundCollection currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, srcBusId, 3);
            if (StringUtils.isNotEmpty(currentBill.FundCollection_b().get(0).getTransNumber()) || currentBill.getSettleflag() == 0) {
                pushStwb = false;
            }
        }
        return pushStwb;
    }

    private void updateVoucherStatus(EventInfo eventInfo, boolean pushStwb, String billNum, Map<String, Object> params) throws Exception {
        // 200:成功
        // 204:不过账
        // 208:错版返回值，新版先于旧版已经被处理，建议业务作成功处理
        // 209:漏版返回值，删除时未找到数据，建议业务做失败处理
        // 500:失败
        log.error("CmpEventListerServiceImpl parameter change,srcBusId={}, pushStwb={}, code={}, billNumber={}"
                , eventInfo.srcBusId, pushStwb, eventInfo.code, billNum);
        if ("200".equals(eventInfo.code) || "204".equals(eventInfo.code)) {
            log.error("CmpEventListerServiceImpl Events Write Back, params={}", params);
            if ("204".equals(eventInfo.code)) {
                params.put(VOUCHER_STATUS, VoucherStatus.NO_POST.getValue());
            } else {
                params.put(VOUCHER_STATUS, VoucherStatus.POST_SUCCESS.getValue());
            }
            // 资金收付单据过账成功后要推结算
            if (pushStwb) {
                pushStwbData(eventInfo.srcBusId, billNum, eventInfo.fullName, params);
            }
            log.error("CmpEventListerServiceImpl.error:tableName handler, id= {}, params= {}", eventInfo.srcBusId, params);
        } else if ("208".equals(eventInfo.code) || "209".equals(eventInfo.code)) {
            //modfiy by lichaor 20250605 跟杨昊沟通 208 209 要丢，200 成功， 其他的失败。
            //params.put(VOUCHER_STATUS, VoucherStatus.POST_SUCCESS.getValue());
            return;
        } else if ("500".equals(eventInfo.code)) {
            params.put(VOUCHER_STATUS, VoucherStatus.POST_FAIL.getValue());
        } else {
            params.put(VOUCHER_STATUS, VoucherStatus.POST_FAIL.getValue());
        }

        updatetVoucherStatus(params, eventInfo);
    }

    /**
     * 更新凭证状态
     *
     * @param params
     * @param eventInfo
     * @throws Exception
     */
    private void updatetVoucherStatus(Map<String, Object> params, EventInfo eventInfo) throws Exception {
        if (IBillNumConstant.CMP_BATCHTRANSFERACCOUNT_TABLE.equals(params.get("tableName"))) {
            BatchTransferAccount batchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, eventInfo.srcBusId);
            batchTransferAccount.setEntityStatus(EntityStatus.Update);
            batchTransferAccount.setVoucherstatus(Short.parseShort(params.get(VOUCHER_STATUS).toString()));
            batchTransferAccount.setFiEventDataVersion(eventInfo.fiEventDataVersion);
            batchTransferAccount.setVoucherVersion(eventInfo.voucherVersion);
            MetaDaoHelper.update(BatchTransferAccount.ENTITY_NAME, batchTransferAccount);
        } else if (IBillNumConstant.JOURNAL_BILL_B.equals(params.get("tableName"))) {
            JournalBill_b journalBillB = MetaDaoHelper.findById(JournalBill_b.ENTITY_NAME, eventInfo.srcClassifier);
            journalBillB.setEntityStatus(EntityStatus.Update);
            if (params.get("voucherstatus") != null) {
                journalBillB.setVoucherstatus(Short.parseShort(params.get(VOUCHER_STATUS).toString()));
            }
            if (params.get("voucherId") != null) {
                journalBillB.setVoucherId(params.get("voucherId").toString());
            }
            if (params.get("voucherPeriod") != null) {
                journalBillB.setVoucherPeriod(params.get("voucherPeriod").toString());
            }
            MetaDaoHelper.update(JournalBill_b.ENTITY_NAME, journalBillB);
        } else if (eventInfo.fullName.contains("cmp.fundpayment.FundPayment")) {
            FundPayment fundPayment = new FundPayment();
            if (params.get("voucherstatus") != null) {
                fundPayment.setVoucherstatus(VoucherStatus.find(Short.valueOf(params.get("voucherstatus").toString())));
            }
            if (params.get("voucherPeriod") != null) {
                fundPayment.setVoucherPeriod(params.get("voucherPeriod").toString());
            }
            if (params.get("voucherId") != null) {
                fundPayment.setVoucherId(params.get("voucherId").toString());
            }
            if (params.get("fiEventDataVersion") != null) {
                fundPayment.setFiEventDataVersion(Long.valueOf(params.get("fiEventDataVersion").toString()));
            }
            if (params.get("voucherVersion") != null) {
                fundPayment.setVoucherVersion(Long.valueOf(params.get("voucherVersion").toString()));
            }
            fundPayment.setId(params.get("id").toString());
            fundPayment.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundPayment.ENTITY_NAME, fundPayment);
            //SqlHelper.update(ICmpConstant.UPDATE_FUND_VOUCHER_STATUS, params);
        } else if (eventInfo.fullName.contains("cmp.fundcollection.FundCollection")) {
            FundCollection fundCollection = new FundCollection();
            if (params.get("voucherstatus") != null) {
                fundCollection.setVoucherstatus(VoucherStatus.find(Short.valueOf(params.get("voucherstatus").toString())));
            }
            if (params.get("voucherPeriod") != null) {
                fundCollection.setVoucherPeriod(params.get("voucherPeriod").toString());
            }
            if (params.get("voucherId") != null) {
                fundCollection.setVoucherId(params.get("voucherId").toString());
            }
            if (params.get("fiEventDataVersion") != null) {
                fundCollection.setFiEventDataVersion(Long.valueOf(params.get("fiEventDataVersion").toString()));
            }
            if (params.get("voucherVersion") != null) {
                fundCollection.setVoucherVersion(Long.valueOf(params.get("voucherVersion").toString()));
            }
            fundCollection.setId(params.get("id").toString());
            fundCollection.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundCollection.ENTITY_NAME, fundCollection);
            //SqlHelper.update(ICmpConstant.UPDATE_FUND_VOUCHER_STATUS, params);
        } else {
            SqlHelper.update(ICmpConstant.UPDATE_VOUCHER_STATUS, params);
        }
    }

    private void pushStwbData(String srcBusId, String billNum, String fullName, Map<String, Object> params) {
        try {
            if (FundPayment.ENTITY_NAME.equals(fullName) || IBillNumConstant.FUND_PAYMENT.equals(billNum)) {
                FundPayment currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, srcBusId, 3);
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(currentBill);
                fundPaymentPushStwbBillService.pushBill(currentBillList, false);
            } else if (FundCollection.ENTITY_NAME.equals(fullName) || IBillNumConstant.FUND_COLLECTION.equals(billNum)) {
                FundCollection currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, srcBusId, 3);
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(currentBill);
                fundCollectionPushStwbBillService.pushBill(currentBillList, false);
            }
        } catch (Exception e) {
            log.error("CmpEventListerServiceImpl.error:Push Stwb fail, id= {}, errorMsg = {}", srcBusId, e.getMessage());
            params.put("postingMsg", e.getMessage());
            params.put("VOUCHER_STATUS", VoucherStatus.Empty.getValue());
        }
    }

    private boolean verifyBill(String fullName, BizObject bizObject, boolean isFundBill) {
        if (Salarypay.ENTITY_NAME.equals(fullName)) {
            short settleStatus = Short.parseShort(bizObject.get("settlestatus").toString());
            if (settleStatus != SettleStatus.alreadySettled.getValue()) {
                return true;
            }
        }
        if (TransferAccount.ENTITY_NAME.equals(fullName)) {
            short settleStatus = Short.parseShort(bizObject.get("settlestatus").toString());
            // 结算成功和止付，都可以进行过账
            // 已结算补单也要进行过账
            if (settleStatus != SettleStatus.alreadySettled.getValue() && settleStatus != SettleStatus.SettleFailed.getValue() && settleStatus != SettleStatus.SettledRep.getValue()) {
                return true;
            }
        }
        boolean isCurrencyExchange = CurrencyExchange.ENTITY_NAME.equals(fullName);
        if (isCurrencyExchange) {
            short settlestatus = Short.parseShort(bizObject.get("settlestatus").toString());
            if (settlestatus != DeliveryStatus.alreadyDelivery.getValue() && settlestatus != DeliveryStatus.completeDelivery.getValue()) {
                return true;
            }
        }
        if (isFundBill) {
            short verifyState = Short.parseShort(bizObject.get("verifystate").toString());
            return verifyState != VerifyState.COMPLETED.getValue();
        }
        return false;
    }

    private boolean arrayIsEmpty(CtmJSONArray array) {
        return array == null || array.isEmpty();
    }

    private static final Map<String, String> TABLE_NAME_MAP = new HashMap<>();

    static {
        TABLE_NAME_MAP.put(PayBill.ENTITY_NAME, IBillNumConstant.PAYBILL);
        TABLE_NAME_MAP.put(IBillNumConstant.PAYMENT, IBillNumConstant.PAYBILL);
        TABLE_NAME_MAP.put(ReceiveBill.ENTITY_NAME, IBillNumConstant.RECEIVE_BILL);
        TABLE_NAME_MAP.put(IBillNumConstant.RECEIVE_BILL, IBillNumConstant.RECEIVE_BILL);
        TABLE_NAME_MAP.put(TransferAccount.ENTITY_NAME, IBillNumConstant.TRANSFERACCOUNT);
        TABLE_NAME_MAP.put(IBillNumConstant.TRANSFERACCOUNT, IBillNumConstant.TRANSFERACCOUNT);
        TABLE_NAME_MAP.put(CurrencyExchange.ENTITY_NAME, IBillNumConstant.CURRENCYEXCHANGE);
        TABLE_NAME_MAP.put(IBillNumConstant.CURRENCYEXCHANGE, IBillNumConstant.CURRENCYEXCHANGE);
        TABLE_NAME_MAP.put(ExchangeGainLoss.ENTITY_NAME, IBillNumConstant.EXCHANG_EGAIN_LOSS);
        TABLE_NAME_MAP.put(IBillNumConstant.EXCHANG_EGAIN_LOSS, IBillNumConstant.EXCHANG_EGAIN_LOSS);
        TABLE_NAME_MAP.put(Salarypay.ENTITY_NAME, IBillNumConstant.SALARYPAY);
        TABLE_NAME_MAP.put(IBillNumConstant.SALARYPAY, IBillNumConstant.SALARYPAY);
        TABLE_NAME_MAP.put(FundPayment.ENTITY_NAME, IBillNumConstant.FUND_TABLE_NAME);
        TABLE_NAME_MAP.put(IBillNumConstant.FUND_PAYMENT, IBillNumConstant.FUND_TABLE_NAME);
        TABLE_NAME_MAP.put(FundCollection.ENTITY_NAME, IBillNumConstant.FUND_TABLE_NAME);
        TABLE_NAME_MAP.put(IBillNumConstant.FUND_COLLECTION, IBillNumConstant.FUND_TABLE_NAME);
        TABLE_NAME_MAP.put(AccrualsWithholding.ENTITY_NAME, IBillNumConstant.CMP_ACCRUALS_WITHHOLDING);
        TABLE_NAME_MAP.put(IBillNumConstant.CMP_DEPOSITINTERESTWITHHOLDINGLIST, IBillNumConstant.CMP_ACCRUALS_WITHHOLDING);
        TABLE_NAME_MAP.put(PayMargin.ENTITY_NAME, IBillNumConstant.CMP_PAYMARGIN);
        TABLE_NAME_MAP.put(IBillNumConstant.CMP_PAYMARGIN, IBillNumConstant.CMP_PAYMARGIN);
        TABLE_NAME_MAP.put(ReceiveMargin.ENTITY_NAME, IBillNumConstant.CMP_RECEIVEMARGIN);
        TABLE_NAME_MAP.put(IBillNumConstant.CMP_RECEIVEMARGIN, IBillNumConstant.CMP_RECEIVEMARGIN);
        TABLE_NAME_MAP.put(ForeignPayment.ENTITY_NAME, IBillNumConstant.CMP_FOREIGNPAYMENT);
        TABLE_NAME_MAP.put(IBillNumConstant.CMP_FOREIGNPAYMENT, IBillNumConstant.CMP_FOREIGNPAYMENT);
        TABLE_NAME_MAP.put(BatchTransferAccount.ENTITY_NAME, IBillNumConstant.CMP_BATCHTRANSFERACCOUNT_TABLE);
        TABLE_NAME_MAP.put(JournalBill.ENTITY_NAME, IBillNumConstant.JOURNAL_BILL_B);
    }

    private String getTableName(String fullName, String billNum) {
        return CmpCommonUtil.getString(fullName, billNum, TABLE_NAME_MAP);
    }
}
