package com.yonyoucloud.fi.cmp.journal.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 同步更新日记账中审批状态、结算状态、登账日期
 * 应收应付模块注册调用
 */
@Component
public class JournalSynchStatusRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size()>0) {
            BizObject bizObject = bills.get(0);
            QuerySchema queryJournalSchema = QuerySchema.create().addSelect("id,pubts,dzdate,auditstatus,settlestatus");
            queryJournalSchema.appendQueryCondition(QueryCondition.name("srcbillitemid").eq(bizObject.getId().toString()));
            List<Journal> journals = MetaDaoHelper.queryObject(Journal.ENTITY_NAME, queryJournalSchema, null);
            Date currentDate = new Date();
            for (Journal journal : journals) {
                /** begin 去掉触发器修改,有结算状态和审核状态的进行更新  */
                if(bizObject.getShort("settlestatus") != null) {
                    if (SettleStatus.alreadySettled.getValue() == bizObject.getShort("settlestatus")) {
                        if (bizObject.get("dzdate") != null){
                            journal.setDzdate(bizObject.get("dzdate"));
                        }else {
                            if(BillInfoUtils.getBusinessDate() != null) {
                                journal.setDzdate(BillInfoUtils.getBusinessDate());
                            }else {
                                journal.setDzdate(currentDate);
                            }
                        }
                        journal.setSettlestatus(SettleStatus.alreadySettled);
                    } else {
                        journal.setDzdate(null);
                        journal.setSettlestatus(SettleStatus.noSettlement);
                    }
                }
                if(bizObject.getShort("auditstatus") != null) {
                    if(AuditStatus.Complete.getValue() == bizObject.getShort("auditstatus")) {
                        journal.setAuditstatus(AuditStatus.Complete);
                    }else {
                        journal.setAuditstatus(AuditStatus.Incomplete);
                    }
                }
                /** end 去掉触发器修改  */
            }
            EntityTool.setUpdateStatus(journals);
            MetaDaoHelper.update(Journal.ENTITY_NAME, journals);
        }
        return new RuleExecuteResult();
    }
}
