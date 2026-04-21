package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class TransferUnSubmitRule extends AbstractCommonRule {
    @Autowired
    private JournalService journalService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);

            String billnum = billContext.getBillnum();
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102114"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180066","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }

            BizObject currentBill = MetaDaoHelper.findById(bizobject.getEntityName(), bizobject.getId());

            if (currentBill == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102115"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180069","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
            }

            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102116"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180064","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }

            EventSource eventSource =EventSource.find(currentBill.get(IBillConst.SRCITEM));
            EventType eventType = EventType.find(currentBill.get(IBillConst.BILLTYPE));
            if (EventSource.Drftchase.equals(eventSource) || EventType.SignNote.equals(eventType)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102117"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180068","商业汇票传入的单据不能撤回") /* "商业汇票传入的单据不能撤回" */) /* "商业汇票传入的单据不能弃审" */);
            }
            Short payStatus = currentBill.getShort("paystatus");
            if (payStatus.compareTo(PayStatus.NoPay.getValue()) != 0
                    && payStatus.compareTo(PayStatus.PreFail.getValue()) != 0
                    && payStatus.compareTo(PayStatus.Fail.getValue()) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102118"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180063","该单据已结算，不能撤回") /* "该单据已结算，不能撤回" */) /* "支付状态不能弃审" */);
            }
            //已日结后不能修改或删除期初数据
            QuerySchema querySchema = QuerySchema.create().addSelect("1");
            querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("settleflag").eq(1),QueryCondition.name("settlementdate").eq(currentBill.get("vouchdate"))
                    ,QueryCondition.name(IBussinessConstant.ACCENTITY).eq(currentBill.get(IBussinessConstant.ACCENTITY))));
            List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME,querySchema);
            if(ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102119"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180067","该单据已日结，不能取消结算！") /* "该单据已日结，不能取消结算！" */);
            }
            //
            Boolean check = journalService.checkJournal(currentBill.getId());
            if (check) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102120"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418006A","单据已勾兑，不能撤回") /* "单据已勾兑，不能撤回" */) /* "单据已勾兑不能弃审" */);
            }

            Short auditStatus = currentBill.getShort("auditstatus");

            if(auditStatus==null||auditStatus.equals(AuditStatus.Incomplete.getValue())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102121"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180065","单据未审核，不能撤回") /* "单据未审核，不能撤回" */));
            }
            bizobject.set("auditstatus", AuditStatus.Incomplete.getValue());
            bizobject.set("auditorId", null);
            bizobject.set("auditor", null);
            bizobject.set("auditDate", null);
            bizobject.set("auditTime", null);
        }
        return new RuleExecuteResult();
    }


}
