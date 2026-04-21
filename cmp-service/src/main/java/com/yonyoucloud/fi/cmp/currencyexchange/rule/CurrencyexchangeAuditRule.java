package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCurrencyExchangeManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.DeliveryStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.ICurrencyExchangeNoticeMsgConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("currencyexchangeAuditRule")
public class CurrencyexchangeAuditRule extends AbstractCommonRule {
    @Autowired
    private JournalService journalService;

    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    CmpBudgetCurrencyExchangeManagerService cmpBudgetCurrencyExchangeManagerService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            Date date = BillInfoUtils.getBusinessDate();
            BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizobject.getId(), 3);
            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101112"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_193888140588000F","单据不存在，请刷新重试") /* "单据不存在，请刷新重试" */);
            }
            // 直连交割时，交易编码必输
            if ((Integer) currentBill.get("deliveryType") == 1 && currentBill.get("transactionCode")==null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101113"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1938881405880010","直连交割时，交易编码必输") /* "直连交割时，交易编码必输" */);
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101114"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180151","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            Date currentDate = BillInfoUtils.getBusinessDate();
            if (currentDate.compareTo(date) == -1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101115"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180150","审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
            }
            //交割相关。交割状态是手工交割，处理中，待交割，已交割不能审批
            if (currentBill.get("settlestatus") != null &&  (currentBill.get("settlestatus").equals(2)
                    || currentBill.get("settlestatus").equals(3) || currentBill.get("settlestatus").equals(4) ||currentBill.get("settlestatus").equals(5))){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101116"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDIT_SETTLED_NOTICE,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540059F", "该单据已结算，不能进行审批！") /* "该单据已结算，不能进行审批！" */));
            }
            EventSource eventSource = EventSource.find(currentBill.get(IBillConst.SRCITEM));
            if(currentBill.get(IBillConst.SRCITEM) != null && !EventSource.Cmpchase.equals(eventSource)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101117"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDIT_SCRIME_NOTICE,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A0", "该单据不是现金自制单据，不能进行审批！") /* "该单据不是现金自制单据，不能进行审批！" */));
            }
            //平台会自动赋值，不需要手动赋值 会报错
            bizobject.set("auditstatus", AuditStatus.Complete.getValue());
//            bizobject.set("verifystate", VerifyState.COMPLETED.getValue());
            bizobject.set("auditorId", AppContext.getCurrentUser().getId());
            bizobject.set("auditor", AppContext.getCurrentUser().getName());
            bizobject.set("auditDate", BillInfoUtils.getBusinessDate());
            bizobject.set("auditTime", new Date());

            //国机相关：银行对账单或认领单生成的外币兑换单，审核后直接更改为手工交割成功，并生成凭证
            if (currentBill.get("datasource") !=null &&
                    ("16".equals(currentBill.get("datasource").toString()) ||"80".equals(currentBill.get("datasource").toString()) )){
                currentBill.set("auditstatus", AuditStatus.Complete.getValue());
                currentBill.set("auditorId", AppContext.getCurrentUser().getId());
                currentBill.set("auditor", AppContext.getCurrentUser().getName());
                currentBill.set("auditDate", BillInfoUtils.getBusinessDate());
                currentBill.set("auditTime", new Date());
                currentBill.set("billtype", EventType.CurrencyExchangeBill.getValue());
                currentBill.set("_entityName", CurrencyExchange.ENTITY_NAME);
                currentBill.set("srcitem",currentBill.get(IBillConst.SRCITEM));
                Date settledate = null;
                if (BillInfoUtils.getBusinessDate() != null) {
                    settledate = BillInfoUtils.getBusinessDate();
                } else {
                    settledate = new Date();
                }
                currentBill.set("settledate",settledate);
                CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(currentBill);
                if (!generateResult.getBoolean("dealSucceed")) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101118"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070C","发送会计平台失败：") /* "发送会计平台失败：" */ + generateResult.get("message"));
                }
                //结算状态更改为手工交割成功
                bizobject.set("settledate", settledate);
                bizobject.set("settlestatus", DeliveryStatus.completeDelivery.getValue());
            }
            journalService.updateJournal(bizobject);

        }

        return new RuleExecuteResult();
    }
}
