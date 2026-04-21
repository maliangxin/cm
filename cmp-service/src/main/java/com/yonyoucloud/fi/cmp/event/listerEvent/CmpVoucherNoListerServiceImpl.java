package com.yonyoucloud.fi.cmp.event.listerEvent;

import com.yonyou.iuap.BusinessException;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.iuap.event.service.EventBodyFetchService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.api.vouchertype.IVoucherTypeService;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.enums.BillMessageEnum;
import com.yonyoucloud.fi.cmp.event.vo.EventInfo;
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
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * <h1>事件中心过账成功后生成凭证号监听</h1>
 * eaai_voucher_writeback_msg，回写事项分录信息
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-05-17 16:10
 */
@Service
@Slf4j
public class CmpVoucherNoListerServiceImpl implements IEventReceiveService {

    @Autowired
    private EventBodyFetchService fetchService;

    @Override
    public String onEvent(BusinessEvent businessEvent, String queueName) throws BusinessException {
        // 判断是否为过账状态事件监听
        if (isEventStatusBackwrite(businessEvent)) {
            // 过账状态逻辑的回写
            try {
                return handleEventStatusBackWrite(businessEvent);
            } catch (Exception e) {
                log.error("handleEventStatusBackWrite error:{}", e.getMessage(), e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100438"), e.getMessage());
            }
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }

    private boolean isEventStatusBackwrite(BusinessEvent businessEvent) {
        return com.yonyoucloud.fi.cmp.enums.EventCenterEnum.EVENT_VOUCHER_NO_BACK_WRITE.getSourceId().equals(businessEvent.getSourceID()) &&
                com.yonyoucloud.fi.cmp.enums.EventCenterEnum.EVENT_VOUCHER_NO_BACK_WRITE.getEventType().equals(businessEvent.getEventType());
    }

    private String handleEventStatusBackWrite(BusinessEvent businessEvent) throws Exception {
        String routingListenerTag = businessEvent.getRoutingListenerTag();
        String responseType = getResponseType(routingListenerTag, businessEvent);
        return JsonUtils.toJsonString(new EventResponseVO(Boolean.TRUE, responseType));
    }

    private String getResponseType(String routingListenerTag, BusinessEvent businessEvent) throws Exception {
        // 不是现金管理的单据过账消息直接返回
        if (StringUtils.isEmpty(routingListenerTag) || !routingListenerTag.contains("CM")) {
            return JsonUtils.toJsonString(EventResponseVO.success());
        }
        CtmJSONArray array = CtmJSONArray.parseArray(fetchService.fetchEvent(businessEvent));
        // 事件消息体为空直接返回
        if (arrayIsEmpty(array)) {
            return JsonUtils.toJsonString(EventResponseVO.success());
        }
        // 更新过账状态逻辑
        return handleEventResponse(array);
    }


    public String handleEventResponse(CtmJSONArray array) throws Exception {
        for (int i = 0; i < array.size(); i++) {
            EventInfo eventInfo = extractEventInfo(array.getJSONObject(i));
            String billNum = getBillNum(eventInfo.getSrcBusId(), eventInfo.getSrcBillTypeId());
            validateBillNum(billNum);
            String fullName = BillMessageEnum.findByKey(billNum).getBillType();
            String tableName = getTableName(fullName, billNum);
            String lockKey = fullName + eventInfo.getSrcBusId();
            if (JournalBill.ENTITY_NAME.equals(fullName)) {
                lockKey = fullName + eventInfo.getSrcBusId() + eventInfo.getSrcClassifier(); // 日记账录入锁粒度为明细表
            }
            CtmLockTool.executeInOneServiceLock(lockKey, 90L, 90L, TimeUnit.SECONDS, (int lockStatus) -> {
                if (lockStatus == LockStatus.GETLOCK_FAIL) {
                    // 枷锁失败
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100439"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080088", "监听凭证号回写事件加锁异常！") /* "监听凭证号回写事件加锁异常！" */);
                }
                try {
                    handleLockedSection(eventInfo, fullName, tableName);
                } catch (Exception e) {
                    log.error("CmpVoucherNoListerServiceImpl.error:userObject={}, e={}", array, e.getMessage());
                    throw e;
                }
            });
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }

    private String handleLockedSection(EventInfo eventInfo, String fullName, String tableName) throws Exception {
        BizObject bizObject;
        if (JournalBill.ENTITY_NAME.equals(fullName)) {
            bizObject = MetaDaoHelper.findById(JournalBill_b.ENTITY_NAME, eventInfo.getSrcClassifier(), 1);
        } else {
           bizObject = MetaDaoHelper.findById(fullName, eventInfo.getSrcBusId(), 1);
        }
        if (validateEventDataVersion(eventInfo, bizObject) || validateVoucherVersion(eventInfo, bizObject)) {
            return constructVersionMismatchResponse(eventInfo, bizObject);
        }
        short voucherStatus = bizObject.getShort(ICmpConstant.VOUCHER_STATUS);
        handleVoucherStatus(eventInfo, fullName, tableName, voucherStatus);
        return JsonUtils.toJsonString(EventResponseVO.success());
    }

    private void handleVoucherStatus(EventInfo eventInfo, String fullName, String tableName, short voucherStatus) throws Exception {
        Map<String, Object> params = buildParams(eventInfo);
        if (ValueUtils.isEmpty(eventInfo.getGlVoucherId()) && VoucherStatus.Created.getValue() != voucherStatus) {
            updateVoucherId(params, fullName, tableName);
        }
        if (ValueUtils.isNotEmptyObj(eventInfo.getGlVoucherId())) {
            updateVoucherNo(params, fullName, tableName);
        }
        if (ValueUtils.isEmpty(eventInfo.getGlVoucherId()) && VoucherStatus.Created.getValue() == voucherStatus) {
            updateVoucherNoByCancelSendGL(tableName, fullName, eventInfo);
        }

    }

    private static void updateVoucherNoByCancelSendGL(String tableName, String fullName, EventInfo eventInfo) throws Exception {
        if (FundPayment.ENTITY_NAME.equals(fullName)) {
            FundPayment fundPayment = new FundPayment();
            fundPayment.setVoucherstatus(VoucherStatus.POST_SUCCESS);
            fundPayment.setVoucherPeriod(null);
            fundPayment.setVoucherId(eventInfo.getVoucherId());
            fundPayment.setVoucherNo(null);
            fundPayment.setId(eventInfo.getSrcBusId());
            fundPayment.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundPayment.ENTITY_NAME, fundPayment);
        } else if (FundCollection.ENTITY_NAME.equals(fullName)) {
            FundCollection fundCollection = new FundCollection();
            fundCollection.setVoucherstatus(VoucherStatus.POST_SUCCESS);
            fundCollection.setVoucherPeriod(null);
            fundCollection.setVoucherId(eventInfo.getVoucherId());
            fundCollection.setVoucherNo(null);
            fundCollection.setId(eventInfo.getSrcBusId());
            fundCollection.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundCollection.ENTITY_NAME, fundCollection);
        } else if (BatchTransferAccount.ENTITY_NAME.equals(fullName)) {
            BatchTransferAccount batchTransferAccount = new BatchTransferAccount();
            batchTransferAccount.setVoucherstatus(VoucherStatus.POST_SUCCESS.getValue());
            batchTransferAccount.setVoucherPeriod(null);
            batchTransferAccount.setVoucherId(null);
            batchTransferAccount.setVoucherNo(null);
            batchTransferAccount.setId(eventInfo.getSrcBusId());
            batchTransferAccount.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BatchTransferAccount.ENTITY_NAME, batchTransferAccount);
        } else if (JournalBill.ENTITY_NAME.equals(fullName)) {
            JournalBill_b journalBillB = new JournalBill_b();
            journalBillB.setEntityStatus(EntityStatus.Update);
            journalBillB.setVoucherstatus(VoucherStatus.POST_SUCCESS.getValue());
            journalBillB.setVoucherId(null);
            journalBillB.setVoucherPeriod(null);
            journalBillB.setVoucherNo(null);
            journalBillB.setId(Long.valueOf(eventInfo.getSrcClassifier()));
            MetaDaoHelper.update(JournalBill_b.ENTITY_NAME, journalBillB);
        } else {
            Map<String, Object> paramsData = new HashMap<>(16);
            paramsData.put(ICmpConstant.TABLE_NAME, tableName);
            paramsData.put(ICmpConstant.ID, eventInfo.getSrcBusId());
            paramsData.put(ICmpConstant.VOUCHERNO, null);
            paramsData.put(ICmpConstant.VOUCHERPERIOD, null);
            paramsData.put(ICmpConstant.VOUCHER_STATUS, VoucherStatus.POST_SUCCESS.getValue());
            paramsData.put(ICmpConstant.VOUCHERID, eventInfo.getVoucherId());
            SqlHelper.update(ICmpConstant.UPDATE_VOUCHER_NO_STATUS, paramsData);
        }
    }

    private boolean validateEventDataVersion(EventInfo eventInfo, BizObject bizObject) {
        Long fiEventDataVersionDb = bizObject.getLong(ICmpConstant.FI_EVENT_DATA_VERSION);
        /*if(fiEventDataVersionDb != null){
            //CZFW-520714 精确到秒，有些数据会精确到毫秒，导致校验不过，导致后续更新凭证相关的逻辑没有走
            fiEventDataVersionDb = (fiEventDataVersionDb / 1000) * 1000;
        }*/
        return ValueUtils.isNotEmptyObj(fiEventDataVersionDb) && eventInfo.getFiEventDataVersion() < fiEventDataVersionDb;
    }

    private boolean validateVoucherVersion(EventInfo eventInfo, BizObject bizObject) {
        Long voucherVersionDb = bizObject.getLong(ICmpConstant.VOUCHER_VERSION);
       /* if(voucherVersionDb != null){
            //精确到秒，有些数据会精确到毫秒，导致校验不过，导致后续更新凭证相关的逻辑没有走
            voucherVersionDb = (voucherVersionDb / 1000) * 1000;
        }*/
        return ValueUtils.isNotEmptyObj(voucherVersionDb)
                && ValueUtils.isNotEmptyObj(eventInfo.getVoucherVersion())
                && eventInfo.getVoucherVersion() < voucherVersionDb;
    }

    private String constructVersionMismatchResponse(EventInfo eventInfo, BizObject bizObject) {
        if (validateEventDataVersion(eventInfo, bizObject)) {
            return CtmJSONObject.toJSONString(new EventResponseVO(Boolean.FALSE,
                    "Single bill posting version does not match, directly return! Message event input posting version:"
                            + eventInfo.getFiEventDataVersion()
                            + ", Database posting version:"
                            + bizObject.getLong(ICmpConstant.FI_EVENT_DATA_VERSION)));
        }
        if (validateVoucherVersion(eventInfo, bizObject)) {
            return CtmJSONObject.toJSONString(new EventResponseVO(Boolean.FALSE,
                    "Event accounting posting version does not match, directly return! Message event input posting version:"
                            + eventInfo.getVoucherVersion()
                            + ", Database posting version:"
                            + bizObject.getLong(ICmpConstant.VOUCHER_VERSION)));
        }
        return "";
    }

    private Map<String, Object> buildParams(EventInfo eventInfo) {
        HashMap<String, Object> params = new HashMap<>(16);
        params.put("yTenantId", eventInfo.getYTenantId());
        params.put(ICmpConstant.VOUCHERID, eventInfo.getVoucherId());
        params.put(ICmpConstant.PRIMARY_ID, eventInfo.getSrcBusId());
        params.put(ICmpConstant.GL_VOUCHER_ID, eventInfo.getGlVoucherId());
        params.put(ICmpConstant.GL_VOUCHER_TYPE, eventInfo.getGlVoucherType());
        params.put(ICmpConstant.GL_VOUCHER_NO, eventInfo.getGlVoucherNo());
        params.put(ICmpConstant.VOUCHERPERIOD, eventInfo.getPeriodCode());
        params.put(ICmpConstant.FI_EVENT_DATA_VERSION, eventInfo.getFiEventDataVersion());
        params.put(ICmpConstant.VOUCHER_VERSION, eventInfo.getVoucherVersion());
        params.put(ICmpConstant.CLASSIFIER, eventInfo.getSrcClassifier());
        return params;
    }

    private void validateBillNum(String billNum) {
        if (!ValueUtils.isNotEmpty(billNum)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100440"), "It is not queried billNum depending on the transaction type");
        }
    }


    private EventInfo extractEventInfo(CtmJSONObject srcObject) {
        CtmJSONObject fiEventInfo = srcObject.getJSONObject("fiEventInfo");
        EventInfo eventInfo = new EventInfo();
        eventInfo.setGlVoucherId(srcObject.getString(ICmpConstant.GL_VOUCHER_ID));
        eventInfo.setVoucherId(srcObject.getString(ICmpConstant.VOUCHERID));
        eventInfo.setYTenantId(fiEventInfo.getString("ytenantId"));
        eventInfo.setGlVoucherType(srcObject.getString(ICmpConstant.GL_VOUCHER_TYPE));
        eventInfo.setGlVoucherNo(srcObject.getString(ICmpConstant.GL_VOUCHER_NO));
        eventInfo.setPeriodCode(srcObject.getString("periodCode"));
        eventInfo.setSrcExtraInfo(srcObject.getString(ICmpConstant.SRC_EXTRA_INFO));
        eventInfo.setSrcBillTypeId(fiEventInfo.getString("srcBillTypeId"));
        eventInfo.setSrcBusId(fiEventInfo.getString(ICmpConstant.SRC_BUSI_ID));
        eventInfo.setFiEventDataVersion(fiEventInfo.getLong(ICmpConstant.FI_EVENT_DATA_VERSION));
        eventInfo.setVoucherVersion(srcObject.getLong(ICmpConstant.VOUCHER_VERSION));
        eventInfo.setFullName(getFullName(eventInfo.getSrcExtraInfo()));
        eventInfo.setFundBillFlag(isFundBill(eventInfo.getFullName()));
        eventInfo.setSrcClassifier(fiEventInfo.getString("srcClassifier"));
        return eventInfo;
    }

    private boolean isFundBill(String fullName) {
        return FundCollection.ENTITY_NAME.equals(fullName) || FundPayment.ENTITY_NAME.equals(fullName);
    }

    private String getFullName(String srcExtraInfo) {
        return ValueUtils.isNotEmptyObj(srcExtraInfo) ? String.valueOf(srcExtraInfo.split("\\|")[0]) : null;
    }


    private void updateVoucherId(Map<String, Object> params, String fullName, String tableName) {
        params.put(ICmpConstant.TABLE_NAME, tableName);
        try {
            if (FundPayment.ENTITY_NAME.equals(fullName)) {
                FundPayment fundPayment = new FundPayment();
                if (params.get("settleSuccessTime") != null) {
                    fundPayment.setSettleSuccessTime(DateUtils.convertToDate(params.get("settleSuccessTime").toString(), "yyyy-MM-dd"));
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
            } else if (FundCollection.ENTITY_NAME.equals(fullName)) {
                FundCollection fundCollection = new FundCollection();
                if (params.get("settleSuccessTime") != null) {
                    fundCollection.setSettleSuccessTime(DateUtils.convertToDate(params.get("settleSuccessTime").toString(), "yyyy-MM-dd"));
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
            } else if (BatchTransferAccount.ENTITY_NAME.equals(fullName)) {
                BatchTransferAccount batchTransferAccount = new BatchTransferAccount();
                if (params.get("voucherPeriod") != null) {
                    batchTransferAccount.setVoucherPeriod(params.get("voucherPeriod").toString());
                }
                if (params.get("voucherId") != null) {
                    batchTransferAccount.setVoucherId(params.get("voucherId").toString());
                }
                if (params.get("fiEventDataVersion") != null) {
                    batchTransferAccount.setFiEventDataVersion(Long.valueOf(params.get("fiEventDataVersion").toString()));
                }
                if (params.get("voucherVersion") != null) {
                    batchTransferAccount.setVoucherVersion(Long.valueOf(params.get("voucherVersion").toString()));
                }
                batchTransferAccount.setId(params.get("id").toString());
                batchTransferAccount.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BatchTransferAccount.ENTITY_NAME, batchTransferAccount);
            } else if (JournalBill.ENTITY_NAME.equals(fullName)) {
                JournalBill_b journalBillB = new JournalBill_b();
                journalBillB.setId(Long.valueOf(params.get(ICmpConstant.CLASSIFIER).toString()));
                if (params.get("voucherPeriod") != null) {
                    journalBillB.setVoucherPeriod(params.get("voucherPeriod").toString());
                }
                if (params.get("voucherId") != null) {
                    journalBillB.setVoucherId(params.get("voucherId").toString());
                }
                journalBillB.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(JournalBill_b.ENTITY_NAME, journalBillB);
            } else {
                SqlHelper.update(ICmpConstant.UPDATE_VOUCHER_ID, params);
            }
        } catch (Exception e) {
            log.error("Update VoucherId fail!, params={}, e={}", params, e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100441"), e.getMessage());
        }
    }

    private String getBillNum(String srcBusId, String srcBillTypeId) {
        String billNum = null;
        try {
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("code");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("id").eq(srcBillTypeId));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100442"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418034E", "查询交易类型失败！请检查数据。") /* "查询交易类型失败！请检查数据。" */);
                }
                billNum = MapUtils.getString(objectMap, "code");
            }
        } catch (Exception e) {
            log.error("query transType fail! id={}, yTenantId = {}, e = {}",
                    srcBusId, InvocationInfoProxy.getTenantid(), e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100443"), e.getMessage());
        }
        return billNum;
    }

    private void updateVoucherNo(Map<String, Object> params, String fullName, String tableName) {
        try {
            CtmJSONObject voucherNoJson = new CtmJSONObject();
            Object glVoucherType = params.get("glVoucherType");
            if (ValueUtils.isNotEmptyObj(glVoucherType)) {
                BizObject voucherTypeBiz = RemoteDubbo.get(IVoucherTypeService.class, IDomainConstant.MDD_DOMAIN_FIEPUB)
                        .getVoucherstrById(glVoucherType.toString());
                String zhCN = ValueUtils.isNotEmptyObj(voucherTypeBiz.getString(ICmpConstant.ZH_CN))
                        ? voucherTypeBiz.getString(ICmpConstant.ZH_CN).concat("-").concat(params.get(ICmpConstant.GL_VOUCHER_NO).toString())
                        : params.get("glVoucherNo").toString();
                voucherNoJson.put(ICmpConstant.ZH_CN, zhCN);
                voucherNoJson.put(ICmpConstant.EN_US, ValueUtils.isNotEmptyObj(voucherTypeBiz.getString(ICmpConstant.EN_US))
                        ? voucherTypeBiz.getString(ICmpConstant.EN_US).concat("-").concat(params.get("glVoucherNo").toString()) : zhCN);
                voucherNoJson.put(ICmpConstant.ZH_TW, ValueUtils.isNotEmptyObj(voucherTypeBiz.getString(ICmpConstant.ZH_TW))
                        ? voucherTypeBiz.getString(ICmpConstant.ZH_TW).concat("-").concat(params.get("glVoucherNo").toString()) : zhCN);
                log.error("glVoucherType query data, data={}", voucherNoJson);
            }
            Object glVoucherId = params.get("glVoucherId");
            Object srcBusId = params.get("id");
            log.error("by mq message update fund payment or collection bill voucherId response message, tableName = {}, voucherId = {}, srcBusId = {}, responseStr = {}",
                    tableName, glVoucherId, srcBusId, voucherNoJson);
            if (ValueUtils.isNotEmptyObj(tableName) && ValueUtils.isNotEmptyObj(glVoucherId)) {
                params.put(ICmpConstant.TABLE_NAME, tableName);
                params.put(ICmpConstant.ID, srcBusId);
                params.put("voucherNo", ValueUtils.isNotEmptyObj(voucherNoJson) ? voucherNoJson.toString() : null);
                if (ValueUtils.isNotEmptyObj(params.get(ICmpConstant.VOUCHERPERIOD))) {
                    params.put("voucherPeriod", params.get(ICmpConstant.VOUCHERPERIOD));
                }
                params.put("voucherstatus", VoucherStatus.Created.getValue());
                params.put("voucherId", glVoucherId);
                params.put("fullName", fullName);
                log.error("update voucher data, params={}", CtmJSONObject.toJSONString(params));
                updatetVoucherNo(params);
                if (
//                        !"cmp_fundcollection".equals(tableName)
//                        && !"cmp_fundpayment".equals(tableName)
//                        &&
                        ValueUtils.isNotEmptyObj(voucherNoJson.get(ICmpConstant.ZH_CN))) {
                    updateJournal(params, voucherNoJson, srcBusId);
                }
            }
        } catch (Exception e) {
            log.error("by mq message update bill voucherNo and voucherPeriod fail, tableName = {}, params = {}",
                    tableName, params);
        }
    }


    /**
     * 更新凭证编号和凭证期间
     *
     * @param params
     * @throws Exception
     */
    private void updatetVoucherNo(Map<String, Object> params) throws Exception {
        String fullName = params.get("fullName").toString();
        if (BatchTransferAccount.ENTITY_NAME.equals(fullName)) {
            // 同名账户批量划转
            BatchTransferAccount batchTransferAccount = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, params.get(ICmpConstant.ID).toString());
            batchTransferAccount.setEntityStatus(EntityStatus.Update);
            batchTransferAccount.setVoucherId(params.get("voucherId").toString());
            if (params.get("voucherNo") != null) {
                batchTransferAccount.setVoucherNo(params.get("voucherNo").toString());
            }
            if (params.get("voucherPeriod") != null) {
                batchTransferAccount.setVoucherPeriod(params.get("voucherPeriod").toString());
            }
            batchTransferAccount.setVoucherstatus(Short.parseShort(params.get("voucherstatus").toString()));
            MetaDaoHelper.update(BatchTransferAccount.ENTITY_NAME, batchTransferAccount);
        } else if (IBillNumConstant.JOURNAL_BILL_B.equals(params.get(ICmpConstant.TABLE_NAME))) {
            JournalBill_b journalBillB = new JournalBill_b();
            journalBillB.setId(Long.valueOf(params.get(ICmpConstant.CLASSIFIER).toString()));
            journalBillB.setEntityStatus(EntityStatus.Update);
            if (params.get("voucherstatus") != null) {
                journalBillB.setVoucherstatus(Short.parseShort(params.get("voucherstatus").toString()));
            }
            if (params.get("voucherId") != null) {
                journalBillB.setVoucherId(params.get("voucherId").toString());
            }
            if (params.get("voucherPeriod") != null) {
                journalBillB.setVoucherPeriod(params.get("voucherPeriod").toString());
            }
            if (params.get("voucherNo") != null) {
                journalBillB.setVoucherNo(params.get("voucherNo").toString());
            }
            MetaDaoHelper.update(JournalBill_b.ENTITY_NAME, journalBillB);
        } else if(FundPayment.ENTITY_NAME.equals(fullName)) {
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
            if (params.get("voucherNo") != null) {
                fundPayment.setVoucherNo(params.get("voucherNo").toString());
            }
            fundPayment.setId(params.get("id").toString());
            fundPayment.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundPayment.ENTITY_NAME, fundPayment);
            //SqlHelper.update(ICmpConstant.UPDATE_FUND_VOUCHER_STATUS, params);
        } else if (FundCollection.ENTITY_NAME.equals(fullName)) {
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
            if (params.get("voucherNo") != null) {
                fundCollection.setVoucherNo(params.get("voucherNo").toString());
            }
            fundCollection.setId(params.get("id").toString());
            fundCollection.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(FundCollection.ENTITY_NAME, fundCollection);
            //SqlHelper.update(ICmpConstant.UPDATE_FUND_VOUCHER_STATUS, params);
        } else {
            SqlHelper.update(ICmpConstant.UPDATE_VOUCHER_STATUS, params);
        }
    }

    private static void updateJournal(Map<String, Object> params, CtmJSONObject voucherNoJson, Object srcBusId) throws Exception {
        Map<String, Object> params1 = new HashMap<>();
        params1.put("voucherNo", voucherNoJson.get(ICmpConstant.ZH_CN));
        params1.put("id", srcBusId);
        if (ValueUtils.isNotEmptyObj(params.get(ICmpConstant.VOUCHERPERIOD))) {
            params1.put("voucherPeriod", params.get(ICmpConstant.VOUCHERPERIOD));
        }
        log.error("update journal voucherNo, id = {}, params = {}", srcBusId, params1);
        SqlHelper.update("com.yonyoucloud.fi.cmp.voucher.updateVoucherNoOfJournal", params1);
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
        TABLE_NAME_MAP.put(IBillNumConstant.JOURNAL_BILL, IBillNumConstant.JOURNAL_BILL_B);
    }

    private String getTableName(String fullName, String billNum) {
        return CmpCommonUtil.getString(fullName, billNum, TABLE_NAME_MAP);
    }
}
