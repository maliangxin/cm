package com.yonyoucloud.fi.cmp.journalbill.rule.audit;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalVo;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill_b;
import com.yonyoucloud.fi.cmp.sdk.journal.service.CmpJournalService;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JournalBillAfterUnAuditRule extends AbstractCommonRule {

    @Autowired
    private CmpJournalService cmpJournalService;

    @Autowired
    private CmpVoucherService cmpVoucherService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizObject : bills) {
            JournalBill journalBillDB = MetaDaoHelper.findById(JournalBill.ENTITY_NAME, bizObject.getId(), 2);
            setUpdateEntityStatus(journalBillDB);
            deleteJournal(journalBillDB);
            journalBillDB.JournalBill_b().forEach(item -> item.setUnireconciliationcode(null)); // 置空财资统一对账码
            boolean isAllSucceed = deleteVoucher(journalBillDB);
            // 先更新状态，然后如果没有全部删成功，报个错
            MetaDaoHelper.update(JournalBill.ENTITY_NAME, journalBillDB);
            if (!isAllSucceed) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105043"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21CA9BF605A00008","凭证没有全部删除成功! 请排查。") /* "凭证没有全部删除成功! 请排查。" */);
            }
        }
        return new RuleExecuteResult();
    }

    private void setUpdateEntityStatus(JournalBill journalBillDB) {
        journalBillDB.setEntityStatus(EntityStatus.Update);
        journalBillDB.JournalBill_b().forEach(item -> item.setEntityStatus(EntityStatus.Update));
    }

    private boolean deleteVoucher(JournalBill journalBill) throws Exception {
        boolean isAllSucceed = true;
        for (JournalBill_b journalBillB : journalBill.JournalBill_b()) {
            CtmJSONObject deleteResult;
            try {
                JournalBill journalBillToVoucher = new JournalBill();
                journalBillToVoucher.set(ICmpConstant.ENTITYNAME, JournalBill.ENTITY_NAME);
                journalBillToVoucher.init(journalBill);
                journalBillToVoucher.put("voucherstatus", journalBillB.getVoucherstatus());
                journalBillToVoucher.setJournalBill_b(Collections.singletonList(journalBillB));
                deleteResult = cmpVoucherService.deleteVoucherWithResult(journalBillToVoucher);
                if (!deleteResult.getBoolean("dealSucceed")) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100348"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418008F","删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
                }
            } catch (Exception e) {
                isAllSucceed = false;
                log.error("delete voucher failed, ", e);
                continue;
            }
            journalBillB.setVoucherstatus(deleteResult.getShort("voucherstatus"));
            journalBillB.setVoucherId(null);
            journalBillB.setVoucherNo(null);
            journalBillB.setVoucherPeriod(null);
        }
        return isAllSucceed;
    }

    private void deleteJournal(JournalBill journalBill) throws Exception {
        JournalVo journalVo = new JournalVo();
        journalVo.setUniqueIdentification(EventSource.Cmpchase.getValue() + "-" + journalBill.getAccentity() + "-" + journalBill.getId());
        List<Journal> journalsList = new ArrayList<>();
        journalVo.setJournalList(journalsList);
        for (JournalBill_b journalBillB : journalBill.JournalBill_b()) {
            Journal journal = new Journal();
            journal.setBillnum(journalBill.getCode());
            journal.setSrcbillno(journalBill.getCode());
            journal.setSrcbillitemno(journalBillB.getId().toString());
            journal.setSrcbillitemid(journalBill.getId().toString());
            journal.setServicecode(ICmpConstant.JOURNAL_BILL_SERVICE_CODE);
            journal.setTopbilltype(EventType.JournalBill);
            journal.setTopsrcitem(EventSource.Cmpchase);
            journal.setSrcitem(EventSource.Cmpchase);
            journal.setAccentity(journalBill.getAccentity());
            journal.setAccentityRaw(journalBill.getAccentityRaw());
            journal.setBilltype(EventType.JournalBill);
            journalsList.add(journal);
        }

        cmpJournalService.delete(journalVo);
    }
}
