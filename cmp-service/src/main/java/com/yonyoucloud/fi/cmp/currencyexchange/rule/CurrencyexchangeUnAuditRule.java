package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.DeliveryStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.ICurrencyExchangeNoticeMsgConstant;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
@Slf4j
@Component("currencyexchangeUnAuditRule")
public class CurrencyexchangeUnAuditRule extends AbstractCommonRule {

    @Autowired
    private JournalService journalService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);

            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102476"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F5","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }

            BizObject currentBill = MetaDaoHelper.findById(bizobject.getEntityName(), bizobject.getId());

            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102477"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F7","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }

            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102478"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F4","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }

            //交割相关。交割状态是手工交割，处理中，待交割，已交割不能审批
            if (currentBill.get("settlestatus") != null &&  (currentBill.get("settlestatus").equals(2)
                    || currentBill.get("settlestatus").equals(3) || currentBill.get("settlestatus").equals(4) ||currentBill.get("settlestatus").equals(5) )){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102479"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDIT_SETTLED_NOTICE,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400724", "该单据已结算，不能进行取消审批！") /* "该单据已结算，不能进行取消审批！" */));
            }
            EventSource eventSource =EventSource.find(currentBill.get(IBillConst.SRCITEM));
            if(currentBill.get(IBillConst.SRCITEM) != null && !EventSource.Cmpchase.equals(eventSource)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102480"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDIT_SCRIME_NOTICE,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400725", "该单据不是现金自制单据，不能进行取消审批！") /* "该单据不是现金自制单据，不能进行取消审批！" */));
            }
            // begin 日结逻辑控制调整 majfd 21/06/07
            //已日结后不能修改或删除期初数据
//            QuerySchema querySchema = QuerySchema.create().addSelect("1");
//            querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("settleflag").eq(true),QueryCondition.name("settlementdate").eq(currentBill.get("vouchdate"))
//                    ,QueryCondition.name("accentity").eq(currentBill.get("accentity"))));
//            List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME,querySchema);
//            if(ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0){
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102481"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026023") /* "该单据已日结，不能取消结算！" */);
//            }
            // end
            Boolean check = journalService.checkJournal(currentBill.getId());
            if (check) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102482"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F6","单据已勾兑不能弃审") /* "单据已勾兑不能弃审" */);
            }

            //交割状态是逾期或者交割失败的数据，弃审后更改为待处理
            if(currentBill.get("settlestatus") != null &&  (currentBill.get("settlestatus").equals(6) || currentBill.get("settlestatus").equals(7) )){
                bizobject.set("settlestatus", DeliveryStatus.todoDelivery.getValue());
            }
            //平台会自动赋值，去除自己赋值
            bizobject.set("auditstatus", AuditStatus.Incomplete.getValue());
//            bizobject.set("verifystate", VerifyState.INIT_NEW_OPEN.getValue());
            bizobject.set("auditorId", null);
            bizobject.set("auditor", null);
            bizobject.set("auditDate", null);
            bizobject.set("auditTime", null);
            journalService.updateJournal(bizobject);
        }

        return new RuleExecuteResult();
    }
}
