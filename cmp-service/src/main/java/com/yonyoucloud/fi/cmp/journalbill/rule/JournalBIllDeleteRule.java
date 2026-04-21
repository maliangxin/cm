package com.yonyoucloud.fi.cmp.journalbill.rule;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JournalBIllDeleteRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizObject : bills) {
            JournalBill journalBillDB = MetaDaoHelper.findById(JournalBill.ENTITY_NAME, bizObject.getId(), 2);
            if (journalBillDB.getVerifystate() != VerifyState.INIT_NEW_OPEN.getValue() && journalBillDB.getVerifystate() != VerifyState.REJECTED_TO_MAKEBILL.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105016"), String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218AC50604380001", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540060D", "【%s】单据，不允许删除，仅允许删除审批流状态为初始开立/驳回到制单的单据") /* "【%s】单据，不允许删除，仅允许删除审批流状态为初始开立/驳回到制单的单据" */), journalBillDB.getCode()) /* 【%s】单据，不允许删除，仅允许删除审批流状态为初始开立/驳回到制单的单据 */);
            }
        }

        return new RuleExecuteResult();
    }
}
