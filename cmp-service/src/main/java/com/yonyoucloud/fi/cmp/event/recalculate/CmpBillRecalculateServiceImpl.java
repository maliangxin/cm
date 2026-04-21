package com.yonyoucloud.fi.cmp.event.recalculate;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.accrualsWithholding.AccrualsWithholding;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.enums.FiEventActionEnum;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLoss;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundCommonServiceImpl;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill_b;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.fieaai.busievent.api.v1.IBusiQueryService;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.STATUS;

@Service("cmpBillRecalculateServiceImpl")
@Slf4j
public class CmpBillRecalculateServiceImpl implements IBusiQueryService {
    private CmCommonService commonService;
    private ICmpSendEventService cmpSendEventService;

    @Autowired
    public void setCommonService(CmCommonService commonService) {
        this.commonService = commonService;
    }

    @Autowired
    public void setCmpSendEventService(ICmpSendEventService cmpSendEventService) {
        this.cmpSendEventService = cmpSendEventService;
    }

    @Override
    public List<EventMessageDTO> query(BusiQueryDTO busiQueryDTO) {
        if (busiQueryDTO == null) {
            return null;
        }
        log.error("CmpBillRecalculateServiceImpl, receive data={}", (busiQueryDTO.getSrcBusiId() != null ? Arrays.toString(busiQueryDTO.getSrcBusiId().toArray()) : "id is null!"));
        // 获取单据id list  fullName
        List<BusiQueryItem> busiQueryItemList = busiQueryDTO.getSrcBusiId();
        //根据业务对象编码获取实体的url
        String fullName = getFullNameByBusiObjCode(busiQueryDTO.getSrcBusiObjCode());
        if (JournalBill.BUSI_OBJ_CODE.equals(busiQueryDTO.getSrcBusiObjCode())) {
            if (ValueUtils.isNotEmptyObj(busiQueryDTO.getSrcAction()) && "preview".equals(busiQueryDTO.getSrcAction())) {
                fullName = JournalBill_b.ENTITY_NAME; // 预览事项分录时用子表名称
            } else {
                fullName = JournalBill.ENTITY_NAME; // 重新生成会计事务时用主表名称
            }
        }
        List<EventMessageDTO> emdList = new ArrayList<>();
        for (BusiQueryItem busiQueryItem : busiQueryItemList) {
            String busiId = busiQueryItem.getBusiId();
            BizObject bizObject = null;
            try {
                bizObject = MetaDaoHelper.findById(fullName, busiId);
            } catch (Exception e) {
                log.error("CmpBillRecalculateServiceImpl, query bill fail! errorMsg={}", e.getMessage());
            }
            if (!ValueUtils.isNotEmptyObj(bizObject)) {
                continue;
            }
            String classifier = busiQueryItem.getClassifier();
            boolean isFundBillStopPayRecalculate = ValueUtils.isNotEmptyObj(classifier)
                    && (FundPayment.ENTITY_NAME.equals(fullName) || FundCollection.ENTITY_NAME.equals(fullName) || ForeignPayment.ENTITY_NAME.equals(fullName));
            if (isFundBillStopPayRecalculate) {
                // 重算时，处理资金收付单据，区分整单过账重算与止付过账重算
                handleFundBill(fullName, bizObject, classifier);
            }
            EventMessageDTO eventMessageDTO = new EventMessageDTO();
            EventInfo eventInfo = transToEventInfo(busiQueryDTO, bizObject, classifier);
            eventMessageDTO.setEventInfo(eventInfo);
            cmpSendEventService.translateUserId(FiEventActionEnum.ADD, bizObject);
            // 设置单据附件数: Sun GuoCai 2023/7/3
            commonService.getFilesCount(bizObject);

            if ("-1".equals(eventInfo.getSrcBillStatus())) {
                bizObject = preView(fullName, bizObject);
            }

            String jsonStr = CtmJSONObject.toJSONString(bizObject);
            Map<String, Object> map = CtmJSONObject.parseObject(jsonStr, HashMap.class);
            eventMessageDTO.setData(map);
            emdList.add(eventMessageDTO);
        }
        return emdList;
    }

    private static BizObject preView(String fullName, BizObject bizObject) {
        switch (fullName) {
            case FundPayment.ENTITY_NAME:
                List<FundPayment_b> FundPaymentBList = bizObject.get("FundPayment_b");
                List<FundPayment_b> FundPaymentBListNew = new ArrayList<>();
                try {
                    AppContext.getBean(FundCommonServiceImpl.class).setSettleSuccessPostValue(bizObject, bizObject.getString("accentity"));
                } catch (Exception e) {
                    bizObject.set("settleSuccessPost", 1);
                }
                bizObject.set("auditorId", AppContext.getCurrentUser().getId());
                bizObject.set("auditor", AppContext.getCurrentUser().getName());
                bizObject.set("auditDate", new Date());
                bizObject.set("auditTime", new Date());
                bizObject.set("classifier", null);
                bizObject.set("settleSuccessTime", bizObject.getDate("vouchdate"));
                for (FundPayment_b fundPayment_b : FundPaymentBList) {
                    fundPayment_b.set("settlesuccessSum", fundPayment_b.getOriSum());
                    FundPaymentBListNew.add(fundPayment_b);
                }
                bizObject.set("FundPayment_b", FundPaymentBListNew);
                break;
            case FundCollection.ENTITY_NAME:
                List<FundCollection_b> FundCollectionBList = bizObject.get("FundCollection_b");
                List<FundCollection_b> FundCollectionBListNew = new ArrayList<>();
                try {
                    AppContext.getBean(FundCommonServiceImpl.class).setSettleSuccessPostValue(bizObject, bizObject.getString("accentity"));
                } catch (Exception e) {
                    bizObject.set("settleSuccessPost", 1);
                }
                bizObject.set("auditorId", AppContext.getCurrentUser().getId());
                bizObject.set("auditor", AppContext.getCurrentUser().getName());
                bizObject.set("auditDate", new Date());
                bizObject.set("auditTime", new Date());
                bizObject.set("classifier", null);
                bizObject.set("settleSuccessTime", bizObject.getDate("vouchdate"));
                for (FundCollection_b fundCollection_b : FundCollectionBList) {
                    fundCollection_b.set("settlesuccessSum", fundCollection_b.getOriSum());
                    FundCollectionBListNew.add(fundCollection_b);
                }
                bizObject.set("FundCollection_b", FundCollectionBListNew);
                break;
            case TransferAccount.ENTITY_NAME:
                bizObject.set("auditorId", AppContext.getCurrentUser().getId());
                bizObject.set("auditor", AppContext.getCurrentUser().getName());
                bizObject.set("auditDate", new Date());
                bizObject.set("auditTime", new Date());
                bizObject.set("settledate", bizObject.getDate("vouchdate"));
                bizObject.set("settleSuccessAmount", bizObject.getBigDecimal("oriSum"));
                break;
            case CurrencyExchange.ENTITY_NAME:
                bizObject.set("auditorId", AppContext.getCurrentUser().getId());
                bizObject.set("auditor", AppContext.getCurrentUser().getName());
                bizObject.set("auditDate", new Date());
                bizObject.set("auditTime", new Date());
//                bizObject.set("settledate", bizObject.getDate("vouchdate"));
//                bizObject.set("settleSuccessAmount", bizObject.getBigDecimal("oriSum"));
                break;
            case Salarypay.ENTITY_NAME:
                bizObject.set("auditorId", AppContext.getCurrentUser().getId());
                bizObject.set("auditor", AppContext.getCurrentUser().getName());
                bizObject.set("auditDate", new Date());
                bizObject.set("auditTime", new Date());
//                bizObject.set("settledate", bizObject.getDate("vouchdate"));
                bizObject.set("successmoney", bizObject.getBigDecimal("oriSum"));
                bizObject.set("olcsuccessmoney", bizObject.getBigDecimal("oriSum"));
                break;
            case JournalBill_b.ENTITY_NAME:
                try {
                    BizObject bizObjectb = new BizObject(bizObject);
                    bizObject = MetaDaoHelper.findById(JournalBill.ENTITY_NAME, bizObject.get("mainid"));
                    bizObject.set("auditorId", AppContext.getCurrentUser().getId());
                    bizObject.set("auditor", AppContext.getCurrentUser().getName());
                    bizObject.set("auditDate", new Date());
                    bizObject.set("auditTime", new Date());
                    java.util.List<BizObject> JournalBill_b = new ArrayList<>();
                    JournalBill_b.add(bizObjectb);
                    bizObject.setBizObjects("JournalBill_b", JournalBill_b);
                }catch (Exception e){
                    log.error("query error!");
                }
                break;
            default:
                break;
        }
        return bizObject;
    }

    /**
     * 通过业务对象编码获取实体名称
     *
     * @param busiObjCode
     * @return
     */
    String getFullNameByBusiObjCode(String busiObjCode) {
        String fullName = null;
        switch (busiObjCode) {
            case FundPayment.BUSI_OBJ_CODE:
                // 资金付款
                fullName = FundPayment.ENTITY_NAME;
                break;
            case FundCollection.BUSI_OBJ_CODE:
                // 资金收款
                fullName = FundCollection.ENTITY_NAME;
                break;
            case CurrencyExchange.BUSI_OBJ_CODE:
                // 货币兑换
                fullName = CurrencyExchange.ENTITY_NAME;
                break;
            case Salarypay.BUSI_OBJ_CODE:
                // 薪资支付
                fullName = Salarypay.ENTITY_NAME;
                break;
            case TransferAccount.BUSI_OBJ_CODE:
                fullName = TransferAccount.ENTITY_NAME;
                break;
            case PayMargin.BUSI_OBJ_CODE:
                fullName = PayMargin.ENTITY_NAME;
                break;
            case ReceiveMargin.BUSI_OBJ_CODE:
                fullName = ReceiveMargin.ENTITY_NAME;
                break;
            case ExchangeGainLoss.BUSI_OBJ_CODE:
                fullName = ExchangeGainLoss.ENTITY_NAME;
                break;
            case AccrualsWithholding.BUSI_OBJ_CODE:
                // 利息预提
                fullName = AccrualsWithholding.ENTITY_NAME;
                break;
            case BatchTransferAccount.BUSI_OBJ_CODE:
                // 同名账户批量划转
                fullName = BatchTransferAccount.ENTITY_NAME;
                break;
            case ForeignPayment.BUSI_OBJ_CODE:
                // 外汇付款
                fullName = ForeignPayment.ENTITY_NAME;
                break;
            default:
                return null;
        }
        return fullName;
    }


    @Override
    public BusiStatusRespDTO fetch(BusiQueryStatusItemDTO busiQueryStatusItemDTO) {
        // 1.入参校验
        cmpSendEventService.dataVerify(busiQueryStatusItemDTO, Boolean.FALSE);
        // 2.查询领域数据
        BusiStatusRespDTO busiStatusRespDTO;
        try {
            // 获取查询参数
            QuerySchema querySchema;
            SimpleQuerySchema simpleQuerySchema = busiQueryStatusItemDTO.getSimpleQuerySchema();
            if (ValueUtils.isNotEmptyObj(simpleQuerySchema)) {
                querySchema = simpleQuerySchema.toQuerySchema();
            } else {
                querySchema = QuerySchema.create();
            }
            busiStatusRespDTO = cmpSendEventService.getBusiStatusRespDTO(busiQueryStatusItemDTO, Collections.emptyList(), querySchema, Boolean.FALSE);
        } catch (Exception e) {
            log.error("CmpBillRecalculateServiceImpl, handle fail, busiQueryStatusItemDTO={}, errorMsg={}",
                    CtmJSONObject.toJSONString(busiQueryStatusItemDTO), e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100636"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18DBD16405580007", "拉取从期初到当前时点的业务数据在新架构重新过账的逻辑错误！")
                    /* "拉取从期初到当前时点的业务数据在新架构重新过账的逻辑错误！" */ + "=>" + e.getMessage());
        }
        return busiStatusRespDTO;
    }

    private void handleFundBill(String fullName, BizObject bizObject, String classifier) {
        bizObject.set("classifier", classifier);
        switch (fullName) {
            case FundPayment.ENTITY_NAME:
                List<FundPayment_b> FundPaymentBList = bizObject.get("FundPayment_b");
                List<FundPayment_b> FundPaymentBListNew = new ArrayList<>();
                for (FundPayment_b fundPayment_b : FundPaymentBList) {
                    short caobject = fundPayment_b.getCaobject().getValue();
                    if (caobject == CaObject.Customer.getValue()) {
                        fundPayment_b.put("customer", fundPayment_b.get("oppositeobjectid"));
                    } else if (caobject == CaObject.Supplier.getValue()) {
                        fundPayment_b.put("supplier", fundPayment_b.get("oppositeobjectid"));
                    } else if (caobject == CaObject.Employee.getValue()) {
                        fundPayment_b.put("employee", fundPayment_b.get("oppositeobjectid"));
                    } else if (caobject == CaObject.CapBizObj.getValue()) {
                        fundPayment_b.put("fundbusinobj", fundPayment_b.get("oppositeobjectid"));
                    }
                }
                if (ValueUtils.isNotEmptyObj(classifier)) {
                    for (FundPayment_b fundPayment_b : FundPaymentBList) {
                        long id = fundPayment_b.getLong("id");
                        if (String.valueOf(id).equals(classifier)) {
                            FundPaymentBListNew.add(fundPayment_b);
                            bizObject.set("FundPayment_b", FundPaymentBListNew);
                            break;
                        }
                    }
                }
                break;
            case FundCollection.ENTITY_NAME:
                List<FundCollection_b> FundCollectionBList = bizObject.get("FundCollection_b");
                List<FundCollection_b> FundCollectionBListNew = new ArrayList<>();
                for (FundCollection_b fundCollection_b : FundCollectionBList) {
                    short caobject = fundCollection_b.getCaobject().getValue();
                    if (caobject == CaObject.Customer.getValue()) {
                        fundCollection_b.put("customer", fundCollection_b.get("oppositeobjectid"));
                    } else if (caobject == CaObject.Supplier.getValue()) {
                        fundCollection_b.put("supplier", fundCollection_b.get("oppositeobjectid"));
                    } else if (caobject == CaObject.Employee.getValue()) {
                        fundCollection_b.put("employee", fundCollection_b.get("oppositeobjectid"));
                    } else if (caobject == CaObject.CapBizObj.getValue()) {
                        fundCollection_b.put("fundbusinobj", fundCollection_b.get("oppositeobjectid"));
                    }
                }
                if (ValueUtils.isNotEmptyObj(classifier)) {
                    for (FundCollection_b fundCollection_b : FundCollectionBList) {
                        long id = fundCollection_b.getLong("id");
                        if (String.valueOf(id).equals(classifier)) {
                            FundCollectionBListNew.add(fundCollection_b);
                            bizObject.set("FundCollection_b", FundCollectionBListNew);
                            break;
                        }
                    }
                }
            case ForeignPayment.ENTITY_NAME:

                if (ValueUtils.isNotEmptyObj(classifier)) {
                    long id = bizObject.getLong("id");
                    if (String.valueOf(id).equals(classifier)) {
                        bizObject.set("classifier", id);
                        break;
                    }
                }
            case CurrencyExchange.ENTITY_NAME:

                if (ValueUtils.isNotEmptyObj(classifier)) {
                    long id = bizObject.getLong("id");
                    if (String.valueOf(id).equals(classifier)) {
                        bizObject.set("classifier", id);
                        break;
                    }
                }
            default:
                break;
        }
    }


    private EventInfo transToEventInfo(BusiQueryDTO busiQueryDTO, BizObject bill, String classifier) {
        EventInfo eventInfo = new EventInfo();
        Long billId = bill.getId() instanceof Long ? bill.getId() : Long.parseLong(bill.getId().toString());
        eventInfo.setSrcBillNo(bill.getString(ICmpConstant.CODE));
        eventInfo.setSrcBusiId(String.valueOf(billId));
        eventInfo.setSrcSystemId(busiQueryDTO.getSrcSystemId());
        eventInfo.setSrcModule(busiQueryDTO.getSrcModule());
        eventInfo.setSrcBillTypeId(busiQueryDTO.getSrcBillTypeId());
        long billVersion = ValueUtils.isNotEmptyObj(bill.getPubts())
                ? bill.getPubts().getTime() : bill.getDate("createTime").getTime();
        eventInfo.setSrcBillVersion(billVersion);
        eventInfo.setSrcBillStatus(ValueUtils.isNotEmptyObj(bill.get(STATUS))
                ? bill.get(STATUS).toString() : null);
        eventInfo.setYTenantId(busiQueryDTO.getYTenantId());
        eventInfo.setSrcExtraInfo(busiQueryDTO.getSrcExtraInfo());
        eventInfo.setSrcTradeTypeId(busiQueryDTO.getSrcTradeTypeId());
        eventInfo.setSrcClassifier(classifier);
        if (ValueUtils.isNotEmptyObj(busiQueryDTO.getSrcAction()) && "preview".equals(busiQueryDTO.getSrcAction())) {
            eventInfo.setSrcBillStatus("-1");
        } else {
            eventInfo.setSrcBillStatus(ValueUtils.isNotEmptyObj(bill.get(STATUS))
                    ? bill.get(STATUS).toString() : null);
        }
        eventInfo.setAccentity(bill.get(ICmpConstant.ACCENTITY));
        eventInfo.setOrgId(ValueUtils.isNotEmptyObj(bill.get(ICmpConstant.ORG))
                ? bill.get(ICmpConstant.ORG) : bill.get(ICmpConstant.ACCENTITY));
        return eventInfo;
    }
}
