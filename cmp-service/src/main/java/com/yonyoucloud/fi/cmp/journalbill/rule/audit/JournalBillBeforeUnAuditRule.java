package com.yonyoucloud.fi.cmp.journalbill.rule.audit;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill;
import com.yonyoucloud.fi.cmp.journalbill.JournalBill_b;
import com.yonyoucloud.fi.cmp.stwb.JournalCommonService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JournalBillBeforeUnAuditRule extends AbstractCommonRule {

    @Autowired
    private JournalCommonService journalCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizObject : bills) {
            JournalBill journalBillDB = MetaDaoHelper.findById(JournalBill.ENTITY_NAME, bizObject.getId(), 2);
            // 校验主子表时间戳
            checkMainAndSubPubts(bizObject, journalBillDB);
            for (JournalBill_b journalBillB : journalBillDB.JournalBill_b()) {
                if (journalBillB.getVoucherstatus() == VoucherStatus.POSTING.getValue()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101387"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418038E", "过账中的单据，不能进行撤回！") /* "过账中的单据，不能进行撤回！" */);
                }
            }
            List<Journal> journalsByItemBodyIdList = journalCommonService.getJournalsByItemBodyIdList(journalBillDB.getAccentity(),
                    journalBillDB.JournalBill_b().stream().map(item -> ((Long) item.getId()).toString()).collect(Collectors.toList()));
            boolean isAnyChecked = journalsByItemBodyIdList.stream().anyMatch(Journal::getCheckflag);
            if (isAnyChecked) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105031"),
                        com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_218DA43E05B00001","该单据有明细已勾对，不能撤回！") /* "该单据有明细已勾对，不能撤回！" */);
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 校验主表和子表的时间戳
     * @param bizObject 前端传入数据
     * @param journalBillDB 数据库中的数据
     */
    private void checkMainAndSubPubts(BizObject bizObject, JournalBill journalBillDB) {
        // 校验主表时间戳
        JournalBill frontJounalBill = (JournalBill) bizObject;
        if (frontJounalBill.getPubts().compareTo(journalBillDB.getPubts()) != 0) {
            throwPubtsDiffException();
        }
        List<JournalBill_b> fJounalBillBlist = frontJounalBill.JournalBill_b();
        // 列表点击撤回按钮
        if (CollectionUtils.isEmpty(fJounalBillBlist)) {
            return;
        }
        Map<Long, Date> fJournalBillMap = fJounalBillBlist.stream().collect(Collectors.toMap(JournalBill_b::getId, JournalBill_b::getPubts));
        List<JournalBill_b> dbJournalBillBlist = journalBillDB.JournalBill_b();
        for (JournalBill_b journalBillB : dbJournalBillBlist) {
            if (journalBillB.getPubts().compareTo(fJournalBillMap.get((Long)journalBillB.getId())) != 0) {
                throwPubtsDiffException();
            }
        }
        // 清空子表 防止unauditbillrule覆盖子表数据
        frontJounalBill.setJournalBill_b(null);
    }

    /**
     * 抛出异常
     */
    private void throwPubtsDiffException() {
        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100513"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00164", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
    }

}
