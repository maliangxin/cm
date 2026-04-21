package com.yonyoucloud.fi.cmp.bankreconciliation.rule.noprocess;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonBankReconciliationProcessor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * zxl
 * 删除银行账单-更新账户历史余额
 */
@Slf4j
@Component
public class BankreconciliationCancelNoProcessRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        List<BankReconciliation> resultList = new ArrayList<>();
        //List<String> errorNoticeList = new ArrayList<>();
        for (BizObject bill : bills) {
            BankReconciliation originBankReconciliation = new BankReconciliation();
            originBankReconciliation.init(bill);
            if (originBankReconciliation.getSerialdealtype() != null && originBankReconciliation.getSerialdealtype() == 5 && originBankReconciliation.getSerialdealendstate() == 1) {
                BankReconciliation bankReconciliation = new BankReconciliation();
                bankReconciliation.setId(bill.getId());
                bankReconciliation.setSerialdealtype(null);
                bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());
                bankReconciliation.setEntityStatus(EntityStatus.Update);
                resultList.add(bankReconciliation);
            } else {
                String bankSeqNo = originBankReconciliation.getBank_seq_no();
                //处理完结方式不是”无需处理“
                if (originBankReconciliation.getSerialdealtype() == null || originBankReconciliation.getSerialdealtype() != 5) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103003"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E98336604900000", "交易流水号【%s】流水处理完结方式非'无需处理'，取消无需处理失败，请检查！"), bankSeqNo) /* "交易流水号【%s】流水处理完结方式非'无需处理'，取消无需处理失败，请检查！" */);
                }
                //处理完结状态该不是”已完结“
                if (originBankReconciliation.getSerialdealendstate() != 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103004"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E98338205A80003", "交易流水号【%s】流水处理完结状态非'已完结'，取消无需处理失败，请检查！"), bankSeqNo) /* "交易流水号【%s】流水处理完结状态非'已完结'，取消无需处理失败，请检查！" */);
                }
            }
        }
        CommonBankReconciliationProcessor.batchReconciliationBeforeUpdate(resultList);
        MetaDaoHelper.update(BankReconciliation.ENTITY_NAME, resultList);
        return new RuleExecuteResult();
    }
}