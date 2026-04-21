package com.yonyoucloud.fi.cmp.event.sendEvent;


import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.model.BusinessEventBuilder;
import com.yonyou.iuap.event.service.EventService;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.log.model.BusinessObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.log.util.BusiObjectBuildUtil;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.dubbo.DubboReferenceUtils;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.enums.BillMapEnum;
import com.yonyoucloud.fi.cmp.enums.BillMessageEnum;
import com.yonyoucloud.fi.cmp.enums.FiEventActionEnum;
import com.yonyoucloud.fi.cmp.event.entity.SimpleUserObject;
import com.yonyoucloud.fi.cmp.event.enums.DataFlattenEnum;
import com.yonyoucloud.fi.cmp.event.enums.EventCenterEnum;
import com.yonyoucloud.fi.cmp.event.utils.DateEventUtils;
import com.yonyoucloud.fi.cmp.event.utils.DetermineUtils;
import com.yonyoucloud.fi.cmp.event.vo.EventInfoFetch;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;

import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill_b;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.fieaai.busievent.api.v1.IBusiReceiveService;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.apache.logging.log4j.util.Strings;
import org.imeta.biz.base.BizException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>对接财务新架构业务事项处理类</h1>
 * CmpSendEventServiceImpl#deleteEvent(org.imeta.orm.base.BizObject)：现金管理调用事项中心Dubbo服务接口,实现过账回退功能
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-05-17 8:01
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Transactional
public class CmpSendEventServiceImpl implements ICmpSendEventService {
    private static final List<String> Y_TENANT_VERIFY_STATE_LIST = Arrays.asList(TransferAccount.ENTITY_NAME, CurrencyExchange.ENTITY_NAME);
    private static final List<String> Y_TENANT_SETTLE_SUCCESS_TIME_VERIFY_STATE_Y_TENANT_ID_LIST = Arrays.asList(FundPayment.ENTITY_NAME, FundCollection.ENTITY_NAME);
    private static final List<String> VERIFY_STATE_LIST = Collections.singletonList(Salarypay.ENTITY_NAME);
    private static final List<String> Y_TENANT_LIST = Collections.singletonList(ExchangeGainLoss.ENTITY_NAME);
    public static final String FUND_PAYMENT_FULLNAME = "cmp.fundpayment.FundPayment";
    public static final String FUND_COLLECTION_FULLNAME = "cmp.fundcollection.FundCollection";


    private final CmCommonService cmCommonService;
    private final YmsOidGenerator ymsOidGenerator;
    private final EventService eventService ;
    private final CtmThreadPoolExecutor executorServicePool;

    /**
     * 发送全量消息事件
     *
     * @param bizObject:
     */
    @Override
    public void sendEvent(BizObject bizObject) throws Exception {
        BusinessEventBuilder builder = new BusinessEventBuilder();
        builder.setSourceId(EventCenterEnum.EVENT_ENTRIES.getSourceId());
        builder.setEventType(EventCenterEnum.EVENT_ENTRIES.getEventType());
        String entityName = bizObject.getEntityName();
        String billNum = BillMessageEnum.findByValue(entityName).getBillNum();
        builder.setBillno(billNum);
        builder.setBillCode(bizObject.get(CODE));
        CtmJSONObject billClue = new CtmJSONObject();
        billClue.put("classifier", null);
        billClue.put("srcBusiId", bizObject.getId().toString());
        List<EventMessageDTO> userObject = createUserObject(billClue, bizObject, FiEventActionEnum.ADD, getBillVersion(bizObject));
        log.error("现金管理同步事项中心：userObject-{}", userObject);
        builder.setUserObject(CtmJSONObject.toJSONString(userObject));
        builder.setTenantCode(InvocationInfoProxy.getTenantid());
        BusinessEvent businessEvent = builder.build();
        eventService.fireLocalEvent(businessEvent);
    }

    /**
     * <h2>发送精简消息事件</h2>
     *
     * @param msgBizObject: 实体信息
     * @author Sun GuoCai
     * @date 2022/5/18 11:50
     */
    @Override
    public void sendSimpleEvent(BizObject msgBizObject, CtmJSONObject billClue) throws Exception {
        BusinessEventBuilder businessEventBuilder = new BusinessEventBuilder();
        businessEventBuilder.setSourceId(EventCenterEnum.EVENT_ENTRIES.getSourceId());
        businessEventBuilder.setEventType(EventCenterEnum.EVENT_ENTRIES.getEventType());
        String entityName = msgBizObject.getEntityName();
        long billVersion;
        if (ValueUtils.isNotEmptyObj(billClue.getString("classifier"))
                && ValueUtils.isNotEmptyObj(billClue.getLong("billVersion"))) {
            billVersion = billClue.getLong("billVersion");
        } else {
            billVersion = ValueUtils.isNotEmptyObj(msgBizObject.getPubts())
                    ? msgBizObject.getPubts().getTime() : System.currentTimeMillis();
        }
        SimpleUserObject simpleUserObject = SimpleUserObject.Builder
                .builder(msgBizObject.getId().toString(), DateEventUtils.getDateTime(new Date()), billVersion, "simpleMessageCallbackEventsImpl")
                .billClue(CtmJSONObject.toJSONString(billClue))
                .fullName(entityName)
                .build();
        String billNum = BillMessageEnum.findByValue(entityName).getBillNum();
        businessEventBuilder.setUserObject(simpleUserObject);
        businessEventBuilder.setTenantCode(InvocationInfoProxy.getTenantid());
        businessEventBuilder.setBillId(msgBizObject.getId().toString());
        businessEventBuilder.setBillno(billNum);
        businessEventBuilder.setBillCode(msgBizObject.get(CODE));
        BusinessEvent businessEvent = businessEventBuilder.build();
        log.error("fund generate voucher, data={}, code={}", CtmJSONObject.toJSONString(businessEvent), msgBizObject.get(CODE));
        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("businessEvent", businessEvent);
        BillMapEnum billMapEnum = BillMapEnum.getEnumByBillNum(billNum);
        String defaultServiceCode = IServicecodeConstant.FUNDPAYMENT;
        String serviceCode = defaultServiceCode;
        if(billMapEnum != null){
            serviceCode = billMapEnum.getServiceCode();
        }
        AppContext.getBean(CTMCMPBusinessLogService.class).saveBusinessLog(logparam, msgBizObject.getJavaObject(CODE,String.class), "生成事项前日志", serviceCode, "生成事项前日志", "生成事项前日志");//@notranslate
        eventService.fireLocalEvent(businessEvent);
        AppContext.getBean(CTMCMPBusinessLogService.class).saveBusinessLog(logparam, msgBizObject.getJavaObject(CODE,String.class), "生成事项后日志", serviceCode, "生成事项后日志", "生成事项后日志");//@notranslate
    }

    /**
     * 反向操作，现金管理调用事项中心Dubbo服务接口,实现过账回退功能，即：取消过账（删除凭证）
     *
     * @param msgBizObject:
     */
    @Override
    public void deleteEvent(BizObject msgBizObject) throws Exception {
        log.error("bill reverse operator fourth, id= {}, code= {}, voucherStatus={}, yTenantId= {}"
                , msgBizObject.get("id"), msgBizObject.get("code"), msgBizObject.get("voucherstatus"), InvocationInfoProxy.getTenantid());
        IBusiReceiveService busReceiveService = DubboReferenceUtils.getDubboService(IBusiReceiveService.class, EventCenterEnum.EVENT_VOUCHER_DELETE.getEventType(), null);
        CtmJSONObject billClue = new CtmJSONObject();
        billClue.put("classifier", null);
        if (JournalBill.ENTITY_NAME.equals(msgBizObject.getEntityName())) {
            billClue.put("classifier", ((JournalBill) msgBizObject).JournalBill_b().get(0).getId());
        }
        billClue.put("srcBusiId", msgBizObject.getId().toString());
        List<EventMessageDTO> message = createUserObject(billClue, msgBizObject, FiEventActionEnum.DELETE, getBillVersion(msgBizObject));
        List<ResponseDTO> responseList = busReceiveService.reverse(message);
        for (ResponseDTO response : responseList) {
            if (!"200".equals(response.getCode()) && !"208".equals(response.getCode()) && !"209".equals(response.getCode()) && !"404".equals(response.getCode())) {
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F1", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400485", "事务反向操作失败:") /* "事务反向操作失败:" */) /* "事务反向操作失败:" */ + response.getMessage());
            }
        }
    }

    /**
     * 反向操作，现金管理调用事项中心Dubbo服务接口,实现过账回退功能，即：取消过账（删除凭证）
     *
     * @param msgBizObject:
     */
    @Override
    public CtmJSONObject deleteEventWithoutException(BizObject msgBizObject) throws Exception {
        CtmJSONObject deleteResult = new CtmJSONObject();
        log.error("bill reverse operator fourth, id= {}, code= {}, voucherStatus={}, yTenantId= {}"
                , msgBizObject.get("id"), msgBizObject.get("code"), msgBizObject.get("voucherstatus"), InvocationInfoProxy.getTenantid());
        IBusiReceiveService busReceiveService = DubboReferenceUtils.getDubboService(IBusiReceiveService.class, EventCenterEnum.EVENT_VOUCHER_DELETE.getEventType(), null);
        CtmJSONObject billClue = new CtmJSONObject();
        billClue.put("classifier", null);
        billClue.put("srcBusiId", msgBizObject.getId().toString());
        List<EventMessageDTO> message = createUserObject(billClue, msgBizObject, FiEventActionEnum.DELETE, getBillVersion(msgBizObject));
        List<ResponseDTO> responseList = busReceiveService.reverse(message);
        for (ResponseDTO response : responseList) {
            if (!"200".equals(response.getCode()) && !"208".equals(response.getCode()) && !"209".equals(response.getCode())) {
                deleteResult.put("dealSucceed", false);
                deleteResult.put("message", response.getMessage());
                return deleteResult;
            }
        }
        deleteResult.put("dealSucceed", true);
        return deleteResult;
    }

    /**
     * <h2>创建消息实体类型</h2>
     *
     * @param bizObject:         处理的有业务实体类
     * @param actionEnum: 过账操作类型
     * @return java.util.List<com.yonyoucloud.fi.fieaai.busievent.dto.v1.EventMessageDTO>
     * @author Sun GuoCai
     * @date 2022/6/8 10:42
     */
    @Override
    public List<EventMessageDTO> createUserObject(CtmJSONObject jsonObject, BizObject bizObject,FiEventActionEnum actionEnum, long billVersion) throws Exception{
        // 组装EventMessageDTO
        String fullName = bizObject.get(FULL_NAME);
        if (!ValueUtils.isNotEmptyObj(fullName)){
            fullName = bizObject.get("_entityName");
        }
        EventMessageDTO messageDto = new EventMessageDTO();
        String subId = null;
        if (FundPayment.ENTITY_NAME.equals(fullName) && !FiEventActionEnum.DELETE.getValue().equals(actionEnum.getValue())) {
            Integer isSettleSuccessToPost = bizObject.getInteger("settleSuccessPost");
            List<FundPayment_b> fundPaymentBList = bizObject.get("FundPayment_b");
            if (isSettleSuccessToPost==1){
                // 结算成功后过账，所有单据都是结算止付
                boolean allMatchSettleFailed = fundPaymentBList.stream().allMatch(t ->t.getShort(ICmpConstant.SETTLE_STATUS) == FundSettleStatus.SettleFailed.getValue());
                if (allMatchSettleFailed){
                    FundPayment fundPayment = new FundPayment();
                    fundPayment.setId(bizObject.getId());
                    fundPayment.setVoucherstatus(VoucherStatus.NONCreate);
                    fundPayment.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(FUND_PAYMENT_FULLNAME,fundPayment);
                    return Collections.emptyList();
                }
                // 结算成功后过账，所有单据都是已结算补单
                boolean allMatchSettlementSupplement = fundPaymentBList.stream().allMatch(t -> t.getShort(ICmpConstant.SETTLE_STATUS) == FundSettleStatus.SettlementSupplement.getValue());
                if (allMatchSettlementSupplement){
                    boolean allMatchSettlementSupplementFinal = fundPaymentBList.stream().allMatch(t ->ValueUtils.isNotEmptyObj(t.getSettlesuccessSum()));
                    if (!allMatchSettlementSupplementFinal){
                        return Collections.emptyList();
                    }
                } else {
                    boolean allSettleStatusMatch = fundPaymentBList.stream()
                            .filter(item -> item.getShort(ICmpConstant.SETTLE_STATUS) != FundSettleStatus.SettleFailed.getValue())
                            .allMatch(t -> ValueUtils.isNotEmptyObj(t.getSettlesuccessSum()));

                    if (!allSettleStatusMatch){
                        return Collections.emptyList();
                    }
                }
            } else {
                subId = handleFundBill(jsonObject, fullName, bizObject, billVersion);
            }
        }else if (FundCollection.ENTITY_NAME.equals(fullName) && !FiEventActionEnum.DELETE.getValue().equals(actionEnum.getValue())){
            Integer isSettleSuccessToPost = bizObject.getInteger("settleSuccessPost");
            List<FundCollection_b> fundCollectionBList = bizObject.get("FundCollection_b");
            if (isSettleSuccessToPost==1){
                // 结算成功后过账，所有单据都是结算止付
                boolean allMatchSettleFailed = fundCollectionBList.stream().allMatch(t ->t.getShort(ICmpConstant.SETTLE_STATUS) == FundSettleStatus.SettleFailed.getValue());
                if (allMatchSettleFailed){
                    FundCollection fundCollection = new FundCollection();
                    fundCollection.setId(bizObject.getId());
                    fundCollection.setVoucherstatus(VoucherStatus.NONCreate);
                    fundCollection.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(FUND_COLLECTION_FULLNAME,fundCollection);
                    return Collections.emptyList();
                }
                boolean allMatchSettlementSupplement = fundCollectionBList.stream().allMatch(t ->t.getShort(ICmpConstant.SETTLE_STATUS) == FundSettleStatus.SettlementSupplement.getValue());
                if (allMatchSettlementSupplement){
                    // 结算成功后过账，所有单据都是已结算补单
                    boolean allMatchSettlementSupplementFinal = fundCollectionBList.stream().allMatch(t ->t.getShort(ICmpConstant.SETTLE_STATUS) == FundSettleStatus.SettlementSupplement.getValue()
                            && ValueUtils.isNotEmptyObj(t.getSettlesuccessSum()));
                    if (!allMatchSettlementSupplementFinal){
                        return Collections.emptyList();
                    }
                } else {
                    boolean allSettleStatusMatch = fundCollectionBList.stream()
                            .filter(item -> item.getShort(ICmpConstant.SETTLE_STATUS) != FundSettleStatus.SettleFailed.getValue())
                            .allMatch(t -> ValueUtils.isNotEmptyObj(t.getSettlesuccessSum()));

                    if (!allSettleStatusMatch){
                        return Collections.emptyList();
                    }
                }
            } else {
                subId = handleFundBill(jsonObject, fullName, bizObject, billVersion);
            }
        } else if(ForeignPayment.ENTITY_NAME.equals(fullName) && !FiEventActionEnum.DELETE.getValue().equals(actionEnum.getValue())){
            subId = handleFundBill(jsonObject, fullName, bizObject, billVersion);
        } else if (BatchTransferAccount.ENTITY_NAME.equals(fullName) && !FiEventActionEnum.DELETE.getValue().equals(actionEnum.getValue())) {
            bizObject.set("id", Long.parseLong(bizObject.getId()));
            bizObject.set("tradetype", bizObject.get("tradeType"));
            bizObject.set(VOUCHDATE, DateEventUtils.parseDateStr(bizObject.getString("billDate")));
        } else if (JournalBill.ENTITY_NAME.equals(fullName) && !FiEventActionEnum.DELETE.getValue().equals(actionEnum.getValue())) {
            Long classifier = Long.valueOf(jsonObject.getString("classifier"));
            bizObject.set("classifier", jsonObject.getString("classifier"));
            JournalBill_b journalBillB = ((List<JournalBill_b>)bizObject.get("JournalBill_b")).stream().filter(item -> Objects.equals(item.getId(), classifier)).findFirst().get();
            bizObject.set("JournalBill_b", Collections.singletonList(journalBillB));
            bizObject.set(VOUCHDATE, DateEventUtils.parseDateStr(bizObject.getString("billDate")));
        }
        EventInfo eventInfo = createEventInfo(bizObject, actionEnum, jsonObject, billVersion, subId);
        messageDto.setEventInfo(eventInfo);
        translateUserId(actionEnum, bizObject);
        updateSrcBillVersion(bizObject, eventInfo);
        messageDto.setData(bizObject);
        List<EventMessageDTO> list = new ArrayList<>();
        list.add(messageDto);
        return list;
    }

    private static void updateSrcBillVersion(BizObject bizObject, EventInfo eventInfo) throws Exception {
        String fullName = bizObject.getString(FULL_NAME);
        if (ValueUtils.isEmpty(fullName)){
            return;
        }
        Map<String, String> subItemMapping = eventInfo.getSubItemMapping();
        if (ValueUtils.isEmpty(subItemMapping)){
            return;
        }
        String billNum = subItemMapping.get(fullName);
        if (ValueUtils.isEmpty(billNum)){
            return;
        }
        Long srcBillVersion = eventInfo.getSrcBillVersion();
        if (ValueUtils.isNotEmptyObj(srcBillVersion)){
            Map<String, Object> params = new HashMap<>(16);
            params.put("fiEventDataVersion", srcBillVersion);
            params.put(ICmpConstant.TABLE_NAME, billNum);
            params.put(ICmpConstant.ID, bizObject.getId());
            SqlHelper.update(ICmpConstant.UPDATE_VOUCHER_NO_STATUS, params);
        }
    }

    @Override
    public void translateUserId(FiEventActionEnum actionEnum, BizObject bizObject) {
        if (FiEventActionEnum.ADD.getValue().equals(actionEnum.getValue())) {
            Object auditorId = bizObject.get("auditorId");
            Object creatorId = bizObject.get("creatorId");
            if (ValueUtils.isNotEmptyObj(auditorId) || ValueUtils.isNotEmptyObj(creatorId)) {
                try {
                    Map<String, Object> user1 = queryUserById(auditorId);
                    Map<String, Object> user2 = queryUserById(creatorId);
                    assert user2 != null;
                    if (ValueUtils.isNotEmptyObj(user1)) {
                        bizObject.put("auditorId", user1.get("yhtUserId"));
                    }
                    if (ValueUtils.isNotEmptyObj(user2)) {
                        bizObject.put("creatorId", user2.get("yhtUserId"));
                    }
                } catch (Exception e) {
                    log.info("get user id fail, e = {} ", e.getMessage());
                }
            }
        }
    }


    private EventInfo createEventInfo(BizObject msgBizObject, FiEventActionEnum actionEnum, CtmJSONObject jsonObject, long billVersion, String subId) {
        // 组装EventInfo
        EventInfo eventInfo = new EventInfo();
        String billTypeId = "";
        Object billId = msgBizObject.getId();
        eventInfo.setReqId(UUID.randomUUID().toString());
        eventInfo.setSrcBusiId(billId instanceof Long ? String.valueOf(billId) : billId.toString());
        eventInfo.setAction(actionEnum.getValue());
        eventInfo.setSrcTradeTypeId(msgBizObject.get(TRADE_TYPE));
        String fullName = msgBizObject.get(FULL_NAME);
        if (!ValueUtils.isNotEmptyObj(fullName)) {
            fullName = msgBizObject.getEntityName();
        }
        String billNum = BillMessageEnum.findByValue(fullName).getBillNum();
        eventInfo.setSrcSystemId("0");
        eventInfo.setSrcModule(CMDOMAIN);
        eventInfo.setSrcBillNo(msgBizObject.get(CODE));
        eventInfo.setBusiDate(ValueUtils.isNotEmptyObj(msgBizObject.get(VOUCHDATE)) ? DateEventUtils.getDate(msgBizObject.get(VOUCHDATE)) : null);
        eventInfo.setSrcClassifier(jsonObject.getString("classifier"));
        if (StringUtils.isNotBlank(msgBizObject.get(TRADE_TYPE))) {
            try {
                billTypeId = cmCommonService.catBillType(InvocationInfoProxy.getTenantid(), eventInfo.getSrcTradeTypeId());
            } catch (Exception e) {
                log.info("通过交易类型获取单据类型异常：", e);
            }
        }
        if (!ValueUtils.isNotEmptyObj(billTypeId)) {
            log.info("query billType is null, id={},code={},tradeType={}",
                    msgBizObject.getId(), msgBizObject.get(CODE), msgBizObject.get(TRADE_TYPE));
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100395"),"query billType fail! billType is null!");
        }
        eventInfo.setSrcBillTypeId(billTypeId);
        if (ValueUtils.isNotEmptyObj(billVersion) && !fullName.equals(ExchangeGainLoss.ENTITY_NAME )
                && !fullName.equals(TransferAccount.ENTITY_NAME )) {
            billVersion = ValueUtils.isNotEmptyObj(msgBizObject.getPubts())
                    ? msgBizObject.getPubts().getTime() : System.currentTimeMillis();
        } else {
            billVersion = System.currentTimeMillis();
        }
        eventInfo.setSrcBillVersion(billVersion);
        eventInfo.setSrcBillStatus(ValueUtils.isNotEmptyObj(msgBizObject.get(STATUS))
                ? msgBizObject.get(STATUS).toString() : null);
        eventInfo.setYTenantId(InvocationInfoProxy.getTenantid());
        eventInfo.setOrgId(msgBizObject.get(ACCENTITY));
        eventInfo.setCurrency(msgBizObject.get(IBussinessConstant.CURRENCY));
        eventInfo.setAmount(msgBizObject.get(ORISUM));
        if (ValueUtils.isNotEmptyObj(subId)){
            eventInfo.setSrcExtraInfo(String.format("%s|%s|%s", fullName, billNum, subId));
        } else {
            eventInfo.setSrcExtraInfo(String.format("%s|%s", fullName, billNum));
        }
        return eventInfo;
    }

    private String handleFundBill(CtmJSONObject jsonObject, String fullName, BizObject bizObject, long billVersion) throws Exception{
        try {
            String classifier = jsonObject.getString("classifier");
            switch (fullName) {
                case FundPayment.ENTITY_NAME:
                    List<FundPayment_b> FundPaymentBList = bizObject.get("FundPayment_b");
                    if (ValueUtils.isNotEmptyObj(classifier)){
                        bizObject.set("classifier", classifier);
                        List<FundPayment_b> FundPaymentBListNew = new ArrayList<>();
                        for (FundPayment_b fundPayment_b : FundPaymentBList) {
                            long id = fundPayment_b.getLong("id");
                            if (id == Long.parseLong(classifier)) {
                                FundPaymentBListNew.add(fundPayment_b);
                                bizObject.set("FundPayment_b", FundPaymentBListNew);
                                return fundPayment_b.getId().toString();
                            }
                        }
                    }
                    return null;
                case FundCollection.ENTITY_NAME:
                    List<FundCollection_b> FundCollectionBList = bizObject.get("FundCollection_b");
                    List<FundCollection_b> FundCollectionBListNew = new ArrayList<>();
                    if (ValueUtils.isNotEmptyObj(classifier)){
                        bizObject.set("classifier", classifier);
                        for (FundCollection_b fundCollection_b : FundCollectionBList) {
                            long id = fundCollection_b.getLong("id");
                            if (id == Long.parseLong(classifier)) {
                                FundCollectionBListNew.add(fundCollection_b);
                                bizObject.set("FundCollection_b", FundCollectionBListNew);
                                return fundCollection_b.getId().toString();
                            }
                        }
                    }
                    return null;
                case ForeignPayment.ENTITY_NAME:

                    if (ValueUtils.isNotEmptyObj(classifier)) {
                        long id = bizObject.getLong("id");
                        if (String.valueOf(id).equals(classifier)) {
                            bizObject.set("classifier",id);
                            return bizObject.get("id").toString();
                        }
                    }
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new CtmException("Fund bill process fail, errorMsg", e);
        }
    }


    private long getBillVersion(BizObject msgBizObject) {
        return ValueUtils.isNotEmptyObj(msgBizObject.getPubts())
                ? msgBizObject.getPubts().getTime() : msgBizObject.getDate("createTime").getTime();
    }

    private Map<String, Object> queryUserById(Object id) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("base.user.User");
        billContext.setDomain("iuap-apcom-bipuser");
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
        if (query != null && query.size() > 0) {
            return query.get(0);
        }
        return null;
    }

    public List<Short> getVoucherStatusId(BusiQueryStatusItemDTO busiQueryStatusItemDTO) {
        List<String> srcBillStatusList = busiQueryStatusItemDTO.getSrcBillStatus();
        List<Short> list = new ArrayList<>();
        // 99:代表未生成，对应现金凭证状态：未生成（2）与待过账（9）；1：代表过账中，对应现金凭证状态为：过账中（5）
        for (String srcBillStatus : srcBillStatusList) {
            DetermineUtils.isTureOrFalse(DataFlattenEnum.NO_CREATE.getCode().equals(srcBillStatus)).trueOrFalseHandle(
                    ()->{list.add(VoucherStatus.Empty.getValue());
                        list.add(VoucherStatus.TO_BE_POST.getValue());},
                    ()-> list.add(VoucherStatus.POSTING.getValue())
            );
        }
        return list;
    }

    public BusiStatusRespDTO getBusiStatusRespDTO(BusiQueryStatusItemDTO busiQueryStatusItemDTO, List<Short> list, QuerySchema querySchema,boolean isDataFlatten) throws Exception {
        BusiStatusRespDTO busiStatusRespDTO;
        busiStatusRespDTO = new BusiStatusRespDTO();
        // 查询数据
        List<BusiEventBriefInfo> busiEventBriefInfos = getBusiEventBriefInfos(busiQueryStatusItemDTO, list,querySchema);
        busiStatusRespDTO.setMessage(ICmpConstant.SUCCESS);
        busiStatusRespDTO.setCode(ICmpConstant.REQUEST_SUCCESS_STATUS_CODE);
        busiStatusRespDTO.setIsSync(ICmpConstant.CONSTANT_STR_ZERO);
        busiStatusRespDTO.setTotal(CollectionUtils.isNotEmpty(busiEventBriefInfos)
                ? String.valueOf(busiEventBriefInfos.size()): ICmpConstant.CONSTANT_STR_ZERO);
        busiStatusRespDTO.setBusiEventBriefInfos(busiEventBriefInfos);
        //2024-03-21 yangjn新增 异步发送消息 适配业务事项查询取数(根据id查询单据全部信息 逐条发送)
        if(!isDataFlatten && !busiEventBriefInfos.isEmpty()){
            asyncSendMessageForFetch(busiQueryStatusItemDTO,busiEventBriefInfos);
        }
        return busiStatusRespDTO;
    }

    //异步发送 业务事项查询取数的Fetch接口消息
    private void asyncSendMessageForFetch(BusiQueryStatusItemDTO busiQueryStatusItemDTO,List<BusiEventBriefInfo> busiEventBriefInfos ) throws Exception {
        ExecutorService autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 1, 10, "asyncSendMessageForFetch-threadpool");
        autoAssociatedDataExecutor.submit(() -> {
            EventInfoFetch infoFetch = new EventInfoFetch();
            infoFetch.setBusiFetchBatchId(busiQueryStatusItemDTO.getBatchId());//批次号
            infoFetch.setBusiFetchITotal(String.valueOf(busiEventBriefInfos.size()));//总数 total
            int index = 1;
            for(BusiEventBriefInfo bbInfo : busiEventBriefInfos){
                try {
                    BizObject bizObject = MetaDaoHelper.findById(busiQueryStatusItemDTO.getEntityUri(), bbInfo.getSrcBusiId(), 3);
                    infoFetch.setBusiFetchIndex(String.valueOf(index));
                    //发送全量消息
                    bizObject.put("_entityName",busiQueryStatusItemDTO.getEntityUri());
                    sendEventForFetch(bizObject,infoFetch);
                } catch (Exception e) {
                    log.error("asyncSendMessageForFetch：",e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100396"),e.getMessage());
                }finally {
                    if(autoAssociatedDataExecutor != null){
                        autoAssociatedDataExecutor.shutdown();
                    }
                }
                index ++;
            }
        });
    }

    private void sendEventForFetch(BizObject bizObject,EventInfoFetch infoFetch) throws Exception {
        BusinessEventBuilder builder = new BusinessEventBuilder();
        builder.setSourceId(EventCenterEnum.EVENT_ENTRIES.getSourceId());
        builder.setEventType(EventCenterEnum.EVENT_ENTRIES.getEventType());
        String entityName = bizObject.getEntityName();
        String billNum = BillMessageEnum.findByValue(entityName).getBillNum();
        builder.setBillno(billNum);
        builder.setBillCode(bizObject.get(CODE));
        infoFetch.setClassifier(null);
        infoFetch.setSrcBusiId(bizObject.getId().toString());
        List<EventMessageDTO> userObject = createUserObjectFetch(infoFetch, bizObject, FiEventActionEnum.ADD, getBillVersion(bizObject));
        log.error("现金管理同步事项中心Fetch：userObject-{}", userObject);
        builder.setUserObject(CtmJSONObject.toJSONString(userObject));
        builder.setTenantCode(InvocationInfoProxy.getTenantid());
        BusinessEvent businessEvent = builder.build();
        eventService.fireLocalEvent(businessEvent);
    }

    public List<EventMessageDTO> createUserObjectFetch(EventInfoFetch infoFetch, BizObject bizObject,FiEventActionEnum actionEnum, long billVersion) throws Exception{
        // 组装EventMessageDTO
        String fullName = bizObject.get(FULL_NAME);
        if (!ValueUtils.isNotEmptyObj(fullName)){
            fullName = bizObject.get("_entityName");
        }
        EventMessageDTO messageDto = new EventMessageDTO();
        String subId = null;
        if (FundPayment.ENTITY_NAME.equals(fullName)) {
            if (!ValueUtils.isNotEmptyObj(subId) && !FiEventActionEnum.DELETE.getValue().equals(actionEnum.getValue())){
                bizObject.set("fiEventDataVersion", billVersion);
                bizObject.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(FundPayment.ENTITY_NAME, bizObject);
            }
        }else if (FundCollection.ENTITY_NAME.equals(fullName)){
            if (!ValueUtils.isNotEmptyObj(subId) && !FiEventActionEnum.DELETE.getValue().equals(actionEnum)){
                bizObject.set("fiEventDataVersion", billVersion);
                bizObject.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(FundCollection.ENTITY_NAME, bizObject);
            }
        } else {
            if(!FiEventActionEnum.DELETE.getValue().equals(actionEnum.getValue())){
                bizObject.set("fiEventDataVersion", billVersion);
                bizObject.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(fullName, bizObject);
            }
        }
        messageDto.setEventInfo(createEventInfoFetch(bizObject, actionEnum, infoFetch,billVersion, subId));
        translateUserId(actionEnum, bizObject);
        messageDto.setData(bizObject);
        List<EventMessageDTO> list = new ArrayList<>();
        list.add(messageDto);
        return list;
    }
    private EventInfo createEventInfoFetch(BizObject msgBizObject, FiEventActionEnum actionEnum, EventInfoFetch infoFetch, long billVersion, String subId) {
        // 组装EventInfo
        EventInfo eventInfo = new EventInfo();
        String billTypeId = "";
        Object billId = msgBizObject.getId();
        eventInfo.setReqId(UUID.randomUUID().toString());
        eventInfo.setSrcBusiId(billId instanceof Long ? String.valueOf(billId) : billId.toString());
        eventInfo.setAction(actionEnum.getValue());
        eventInfo.setSrcTradeTypeId(msgBizObject.get(TRADE_TYPE));
        String fullName = msgBizObject.get(FULL_NAME);
        if (!ValueUtils.isNotEmptyObj(fullName)) {
            fullName = msgBizObject.getEntityName();
        }
        String billNum = BillMessageEnum.findByValue(fullName).getBillNum();
        eventInfo.setSrcSystemId("0");
        eventInfo.setSrcModule(CMDOMAIN);
        eventInfo.setSrcBillNo(msgBizObject.get(CODE));
        eventInfo.setBusiDate(ValueUtils.isNotEmptyObj(msgBizObject.get(VOUCHDATE)) ? DateEventUtils.getDate(msgBizObject.get(VOUCHDATE)) : null);
        eventInfo.setSrcClassifier(infoFetch.getClassifier());
        if (StringUtils.isNotBlank(msgBizObject.get(TRADE_TYPE))) {
            try {
                billTypeId = cmCommonService.catBillType(InvocationInfoProxy.getTenantid(), eventInfo.getSrcTradeTypeId());
            } catch (Exception e) {
                log.info("通过交易类型获取单据类型异常：", e);
            }
        }
        if (!ValueUtils.isNotEmptyObj(billTypeId)) {
            log.info("query billType is null, id={},code={},tradeType={}",
                    msgBizObject.getId(), msgBizObject.get(CODE), msgBizObject.get(TRADE_TYPE));
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100397"),"query billType fail! billType is null!");
        }
        eventInfo.setSrcBillTypeId(billTypeId);
        if (!ValueUtils.isNotEmptyObj(billVersion)) {
            billVersion = ValueUtils.isNotEmptyObj(msgBizObject.getPubts())
                    ? msgBizObject.getPubts().getTime() : msgBizObject.getDate("createTime").getTime();
        }
        eventInfo.setSrcBillVersion(billVersion);
        eventInfo.setSrcBillStatus(ValueUtils.isNotEmptyObj(msgBizObject.get(STATUS))
                ? msgBizObject.get(STATUS).toString() : null);
        eventInfo.setYTenantId(InvocationInfoProxy.getTenantid());
        eventInfo.setOrgId(msgBizObject.get(ACCENTITY));
        eventInfo.setCurrency(msgBizObject.get(IBussinessConstant.CURRENCY));
        eventInfo.setAmount(msgBizObject.get(ORISUM));
        if (ValueUtils.isNotEmptyObj(subId)){
            eventInfo.setSrcExtraInfo(String.format("%s|%s|%s", fullName, billNum, subId));
        } else {
            eventInfo.setSrcExtraInfo(String.format("%s|%s", fullName, billNum));
        }
        eventInfo.setBusiFetchBatchId(infoFetch.getBusiFetchBatchId());
        eventInfo.setBusiFetchIndex(infoFetch.getBusiFetchIndex());
        eventInfo.setBusiFetchITotal(infoFetch.getBusiFetchITotal());
        Map<String, String> subItemMapping= new HashMap<>();
        subItemMapping.put(fullName, billNum);
        eventInfo.setSubItemMapping(subItemMapping);
        return eventInfo;
    }

    private List<BusiEventBriefInfo> getBusiEventBriefInfos(BusiQueryStatusItemDTO busiQueryStatusItemDTO, List<Short> list,QuerySchema querySchema) throws Exception {
        // 组装查询参数p0
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        String selectField ="id, code, accentity, voucherstatus, tradetype, vouchdate";

        if (Y_TENANT_VERIFY_STATE_LIST.contains(busiQueryStatusItemDTO.getEntityUri())){
            selectField = selectField.concat(", auditDate, auditTime");
            queryConditionGroup.addCondition(QueryConditionGroup.and(
                    QueryCondition.name(ICmpConstant.VERIFY_STATE).eq(2),
                    QueryCondition.name(ICmpConstant.TENANT).eq(AppContext.getTenantId())
            ));
        }
        if (Y_TENANT_SETTLE_SUCCESS_TIME_VERIFY_STATE_Y_TENANT_ID_LIST.contains(busiQueryStatusItemDTO.getEntityUri())){
            selectField = selectField.concat(", settleSuccessTime, ytenant, auditDate, auditTime");
            queryConditionGroup.addCondition(QueryConditionGroup.and(
                    QueryCondition.name(ICmpConstant.VERIFY_STATE).eq(2),
                    QueryCondition.name(ICmpConstant.Y_TENANT_ID).eq(busiQueryStatusItemDTO.getYtenantId())
            ));
        }
        if (VERIFY_STATE_LIST.contains(busiQueryStatusItemDTO.getEntityUri())){
            selectField = selectField.concat(", auditDate, auditTime");
            queryConditionGroup.addCondition(QueryConditionGroup.and(
                    QueryCondition.name(ICmpConstant.VERIFY_STATE).eq(2)
            ));
        }
        if (Y_TENANT_LIST.contains(busiQueryStatusItemDTO.getEntityUri())){
            queryConditionGroup.addCondition(QueryConditionGroup.and(
                    QueryCondition.name(ICmpConstant.TENANT).eq(AppContext.getTenantId() )
            ));
        }
        // 同名账户批量划转
        if (isBatchTransferFlag(busiQueryStatusItemDTO)){
            selectField ="id, code, accentity, voucherstatus, tradeType, billDate,auditDate, auditTime";
        }
        querySchema.addSelect(selectField);
        queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).in(busiQueryStatusItemDTO.getAccentity())));
        if (CollectionUtils.isNotEmpty(list)){
            queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.VOUCHER_STATUS).in(list)));
        }
        if (CollectionUtils.isNotEmpty(busiQueryStatusItemDTO.getSrcTradeTypeIds())) {
            if (isBatchTransferFlag(busiQueryStatusItemDTO)) {
                if (busiQueryStatusItemDTO.getSrcTradeTypeIds().contains(null)) {
                    queryConditionGroup.addCondition(QueryConditionGroup.or(QueryCondition.name(ICmpConstant.TRADETYPE_NEW).in(busiQueryStatusItemDTO.getSrcTradeTypeIds())
                            , QueryCondition.name(ICmpConstant.TRADETYPE_NEW).is_null()));
                } else {
                    queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.TRADETYPE_NEW).in(busiQueryStatusItemDTO.getSrcTradeTypeIds())));
                }
            } else {
                if (busiQueryStatusItemDTO.getSrcTradeTypeIds().contains(null)) {
                    queryConditionGroup.addCondition(QueryConditionGroup.or(QueryCondition.name(ICmpConstant.TRADETYPE).in(busiQueryStatusItemDTO.getSrcTradeTypeIds())
                            , QueryCondition.name(ICmpConstant.TRADETYPE).is_null()));
                } else {
                    queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.TRADETYPE).in(busiQueryStatusItemDTO.getSrcTradeTypeIds())));
                }
            }
        }else if(Strings.isNotEmpty(busiQueryStatusItemDTO.getSrcTradeTypeId())){
            if (isBatchTransferFlag(busiQueryStatusItemDTO)) {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(TRADETYPE_NEW).eq(busiQueryStatusItemDTO.getSrcTradeTypeId())));
            } else {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.TRADETYPE).eq(busiQueryStatusItemDTO.getSrcTradeTypeId())));
            }
        } else{
            if (isBatchTransferFlag(busiQueryStatusItemDTO)) {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(TRADETYPE_NEW).is_null()));
            } else {
                queryConditionGroup.addCondition(QueryConditionGroup.and(QueryCondition.name(ICmpConstant.TRADETYPE).is_null()));
            }
        }
        // 组装时间查询参数
        List<String> accDateFieldList = busiQueryStatusItemDTO.getBusiAccDateField();
        QueryConditionGroup queryConditionGroupOr = new QueryConditionGroup(ConditionOperator.or);
        for (String dataField : accDateFieldList) {
            QueryConditionGroup queryConditionGroupAnd = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroupAnd.appendCondition(QueryConditionGroup.and(
                            QueryCondition.name(dataField).egt(busiQueryStatusItemDTO.getBusiDateStart()),
                            QueryCondition.name(dataField).elt(busiQueryStatusItemDTO.getBusiDateEnd())
                    )
            );
            queryConditionGroupOr.addCondition(queryConditionGroupAnd);
        }
        QueryConditionGroup queryConditionGroupAll = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroupAll.appendCondition(queryConditionGroup);
        queryConditionGroupAll.appendCondition(queryConditionGroupOr);
        querySchema.addCondition(queryConditionGroupAll);

        // 限制返回数量
        if (ValueUtils.isNotEmptyObj(busiQueryStatusItemDTO.getLimit())){
            querySchema.addPager(0, Integer.parseInt(busiQueryStatusItemDTO.getLimit()));
        }

        // 查询数据
        List<Map<String, Object>> bizObjectList = MetaDaoHelper.query(busiQueryStatusItemDTO.getEntityUri(), querySchema);
        // 组装返回参数
        List<BusiEventBriefInfo> busiEventBriefInfos = new ArrayList<>();

        // 组装缓存Map，缓存单据类型，用完清空
        assembleOutputParameter(bizObjectList, busiEventBriefInfos, busiQueryStatusItemDTO);
        return busiEventBriefInfos;
    }

    private void assembleOutputParameter(List<Map<String, Object>> bizObjectList, List<BusiEventBriefInfo> busiEventBriefInfos, BusiQueryStatusItemDTO busiQueryStatusItemDTO) {
        Map<String, String> billTypeCache = new HashMap<>();
        for (Map<String, Object> bizObject : bizObjectList) {
            buildBizObjectOfBatchTransferAccount(bizObject, busiQueryStatusItemDTO);
            BusiEventBriefInfo eventInfo = new BusiEventBriefInfo();
            eventInfo.setSrcBusiId(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.ID)) ? bizObject.get(ICmpConstant.ID).toString(): null);
            eventInfo.setSrcBillNo(ValueUtils.isNotEmptyObj(bizObject.get(CODE)) ? bizObject.get(CODE).toString(): null);
            short voucherStatus= ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.VOUCHER_STATUS))
                    ? Short.parseShort(bizObject.get(ICmpConstant.VOUCHER_STATUS).toString()) : -1;
            String srcBillStatus = VoucherStatus.POSTING.getValue() == voucherStatus
                    ? DataFlattenEnum.POSTING.getCode() : DataFlattenEnum.NO_CREATE.getCode();
            eventInfo.setSrcBillStatus(srcBillStatus);
            eventInfo.setSrcSystemId("0");
            eventInfo.setYtenantId(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.Y_TENANT_ID)) ? bizObject.get(ICmpConstant.Y_TENANT_ID).toString(): null);
            String tradeType = ValueUtils.isNotEmptyObj(bizObject.get(TRADE_TYPE)) ? bizObject.get(TRADE_TYPE).toString() : null;
            eventInfo.setSrcTradeTypeId(tradeType);
            eventInfo.setSrcModule("CM");
            eventInfo.setAccentity(ValueUtils.isNotEmptyObj(bizObject.get(ACCENTITY)) ? bizObject.get(ACCENTITY).toString(): null);
            boolean isFundBill = FundCollection.ENTITY_NAME.equals(busiQueryStatusItemDTO.getEntityUri())
                    || FundPayment.ENTITY_NAME.equals(busiQueryStatusItemDTO.getEntityUri());

            try {
                String busiDate;
                if (isFundBill) {
                    busiDate = ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.SETTLE_SUCCESS_TIME))
                            ? DateUtils.dateFormat((Date) bizObject.get(ICmpConstant.SETTLE_SUCCESS_TIME), DateUtils.DATE_PATTERN):
                            ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.AUDIT_DATE))
                                    ? DateUtils.dateFormat((Date) bizObject.get(ICmpConstant.AUDIT_DATE), DateUtils.DATE_PATTERN)
                                    : ValueUtils.isNotEmptyObj(bizObject.get(VOUCHDATE))
                                    ? DateUtils.dateFormat((Date) bizObject.get(VOUCHDATE), DateUtils.DATE_PATTERN) : null;
                } else {
                    busiDate = ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.AUDIT_DATE))
                            ? DateUtils.dateFormat((Date) bizObject.get(ICmpConstant.AUDIT_DATE), DateUtils.DATE_PATTERN)
                            : ValueUtils.isNotEmptyObj(bizObject.get(VOUCHDATE))
                            ? DateUtils.dateFormat((Date) bizObject.get(VOUCHDATE), DateUtils.DATE_PATTERN) : null;
                }
                eventInfo.setBusiDate(busiDate);
            } catch (ParseException e) {
                log.error("format date fail!auditDate={}", bizObject.get(ICmpConstant.AUDIT_DATE));
            }
            // 处理单据类型赋值
            String billTypeId;
            billTypeId = billTypeCache.get(tradeType);
            if (StringUtils.isNotBlank(tradeType) && !ValueUtils.isNotEmptyObj(billTypeId)) {
                try {
                    billTypeId = cmCommonService.catBillType(InvocationInfoProxy.getTenantid(), eventInfo.getSrcTradeTypeId());
                    if (!ValueUtils.isNotEmptyObj(billTypeId)) {
                        log.error("assembleOutputParameter, query billType is null,id={},code={},tradeType={}",
                                bizObject.get(ICmpConstant.ID), bizObject.get(CODE), bizObject.get(TRADE_TYPE));
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100398"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B8003E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400488", "通过交易类型未查询到单据类型!") /* "通过交易类型未查询到单据类型!" */) /* "通过交易类型未查询到单据类型!" */);
                    }
                    billTypeCache.put(tradeType,billTypeId);
                } catch (Exception e) {
                    log.error("assembleOutputParameter, query billType fail, errorMsg={}", e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100399"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B80043", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540048A", "通过交易类型获取单据类型异常!") /* "通过交易类型获取单据类型异常!" */) /* "通过交易类型获取单据类型异常!" */);
                }
            }
            eventInfo.setSrcBillTypeId(billTypeId);
            busiEventBriefInfos.add(eventInfo);
        }
        // 清空本地缓存
        billTypeCache.clear();
    }

    /**
     * 构建同名账户批量划转的数据
     * @param bizObject
     * @param busiQueryStatusItemDTO
     */
    private void buildBizObjectOfBatchTransferAccount(Map<String, Object> bizObject, BusiQueryStatusItemDTO busiQueryStatusItemDTO) {
        if (isBatchTransferFlag(busiQueryStatusItemDTO)) {
            bizObject.put(TRADE_TYPE, bizObject.get(TRADETYPE_NEW));
            bizObject.put(VOUCHDATE, bizObject.get(BILL_DATE));
        }
    }

    /**
     * 是否同名账户批量划转
     * @param busiQueryStatusItemDTO
     * @return
     */
    private boolean isBatchTransferFlag(BusiQueryStatusItemDTO busiQueryStatusItemDTO) {
        return BatchTransferAccount.ENTITY_NAME.equals(busiQueryStatusItemDTO.getEntityUri());
    }

    @Override
    public void dataVerify(BusiQueryStatusItemDTO busiQueryStatusItemDTO, boolean isDataFlatten) {
        log.error("dataVerify, input parameter busiQueryStatusItemDTO={}", CtmJSONObject.toJSONString(busiQueryStatusItemDTO));
        // 返给事项平台的参数校验
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(busiQueryStatusItemDTO)).throwMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B80040", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540047F", "业务端数据拉取，请求参数实体为空！") /* "业务端数据拉取，请求参数实体为空！" */) /* "业务端数据拉取，请求参数实体为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(busiQueryStatusItemDTO.getAccentity())).throwMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B80041", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400480", "业务端数据拉取，请求参数的会计主体为空！") /* "业务端数据拉取，请求参数的会计主体为空！" */) /* "业务端数据拉取，请求参数的会计主体为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(busiQueryStatusItemDTO.getBillNum())).throwMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B80042", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400482", "业务端数据拉取，请求参数的单据类型编码为空！") /* "业务端数据拉取，请求参数的单据类型编码为空！" */) /* "业务端数据拉取，请求参数的单据类型编码为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(busiQueryStatusItemDTO.getYtenantId())).throwMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B80044", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400483", "业务端数据拉取，请求参数的租户ID为空！") /* "业务端数据拉取，请求参数的租户ID为空！" */) /* "业务端数据拉取，请求参数的租户ID为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(busiQueryStatusItemDTO.getBusiAccDateField())).throwMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B80045", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400484", "业务端数据拉取，请求参数的日期字段为空！") /* "业务端数据拉取，请求参数的日期字段为空！" */) /* "业务端数据拉取，请求参数的日期字段为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(busiQueryStatusItemDTO.getBusiDateStart())).throwMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B80046", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400486", "业务端数据拉取，请求参数的查询起期为空！") /* "业务端数据拉取，请求参数的查询起期为空！" */) /* "业务端数据拉取，请求参数的查询起期为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(busiQueryStatusItemDTO.getBusiDateEnd())).throwMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B80047", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400487", "业务端数据拉取，请求参数的查询止期为空！") /* "业务端数据拉取，请求参数的查询止期为空！" */) /* "业务端数据拉取，请求参数的查询止期为空！" */);
        if (isDataFlatten) {
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(busiQueryStatusItemDTO.getSrcBillStatus())).throwMessage(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B80048", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400489", "业务端数据拉取，请求参数的单据过账状态为空！") /* "业务端数据拉取，请求参数的单据过账状态为空！" */) /* "业务端数据拉取，请求参数的单据过账状态为空！" */);
        }
    }

    /**
     * <h2>银行流水支持发送事件消息</h2>
     *
     * @param bizObject : 消息体
     * @author Sun GuoCai
     * @date 2024/4/29 10:11
     */
    @Override
    public void sendEventByBankClaim(BizObject bizObject, String action) throws Exception {

        if (!ValueUtils.isNotEmptyObj(bizObject)) {
            return;
        }
        BizObject bizObjectNew = getBizObject(bizObject);
        BusinessEventBuilder builder = new BusinessEventBuilder();
        builder.setSourceId(EventCenterEnum.BANK_CLAIM.getSourceId());
        builder.setEventType(EventCenterEnum.BANK_CLAIM.getEventType());
        builder.setBillno(IBillNumConstant.BANKRECONCILIATION);
        builder.setBillCode(bizObjectNew.get("bank_seq_no"));
        builder.setBillId(bizObjectNew.get("id").toString());
        builder.setServiceCode("yonbip-fi-ctmcmp");
        builder.setBillName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400481", "银行流水认领") /* "银行流水认领" */);
        builder.setEntityName(BankReconciliation.ENTITY_NAME);
        builder.setAction(action);

        log.error("银行流水变更事件消息：userObject={}", bizObjectNew);
        builder.setUserObject(CtmJSONObject.toJSONString(bizObjectNew));
        builder.setTenantCode(InvocationInfoProxy.getTenantid());
        BusinessEvent businessEvent = builder.build();
        eventService.fireLocalEvent(businessEvent);
    }

    /**
     * 批量发送消息 - 假的，主要是为了整合代码
     * @param newBankRecords
     * @param action
     * @throws Exception
     */
    @Override
    public void sendEventByBankClaimBatch(List<BankReconciliation> newBankRecords, String action) throws Exception {
        log.error("开始流水推送事件中心");
        if(CollectionUtils.isEmpty(newBankRecords)){
            return;
        }

        String bankClaimBillChangeEvent = AppContext.getEnvConfig("bank_claim_bill_change_event", "false");
        if("false".equals(bankClaimBillChangeEvent)){
            return;
        }

        for (BankReconciliation bizObject : newBankRecords) {
            sendEventByBankClaim(bizObject, action);
        }
        log.error("推送事件中心成功");
    }

    private static BizObject getBizObject(BizObject bizObject) {
        BizObject bizObjectNew = new BizObject();
        bizObjectNew.set("id", bizObject.get("id"));
        bizObjectNew.set("accentity", bizObject.get("accentity"));
        bizObjectNew.set("bankaccount", bizObject.get("bankaccount"));
        bizObjectNew.set("currency", bizObject.get("currency"));
        bizObjectNew.set("dc_flag", bizObject.get("dc_flag"));
        bizObjectNew.set("tran_date", bizObject.get("tran_date"));
        bizObjectNew.set("tran_time", bizObject.get("tran_time"));
        bizObjectNew.set("oppositeobjectname", bizObject.get("oppositeobjectname"));
        bizObjectNew.set("oppositeobjectid", bizObject.get("oppositeobjectid"));
        bizObjectNew.set("to_acct_name", bizObject.get("to_acct_name"));
        bizObjectNew.set("to_acct_no", bizObject.get("to_acct_no"));
        bizObjectNew.set("to_acct_bank_name", bizObject.get("to_acct_bank_name"));
        bizObjectNew.set("tran_amt", bizObject.get("tran_amt"));
        bizObjectNew.set("acct_bal", bizObject.get("acct_bal"));
        bizObjectNew.set("remark", bizObject.get("remark"));
        bizObjectNew.set("use_name", bizObject.get("use_name"));
        bizObjectNew.set("bankcheckno", bizObject.get("bankcheckno"));
        bizObjectNew.set("unique_no", bizObject.get("unique_no"));
        bizObjectNew.set("dataOrigin", bizObject.get("dataOrigin"));
        bizObjectNew.set("bank_seq_no", bizObject.get("bank_seq_no"));
        bizObjectNew.set("refundstatus", bizObject.get("refundstatus"));
        return bizObjectNew;
    }

}
