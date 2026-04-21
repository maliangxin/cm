package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.workflow;


import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.common.CtmException;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author xuxbo
 * @date 2025/6/5 16:12
 */
@Component
@Slf4j
public class BatchtransferaccountBeforeAuditRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        BatchTransferAccount batchTransferAccount = (BatchTransferAccount) bills.get(0);
        BatchTransferAccount currentBill = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, batchTransferAccount.getId());
        Date currentPubts = batchTransferAccount.getPubts();
        if (currentPubts != null) {
            if (!currentPubts.equals(currentBill.getPubts())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100716"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048C", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
            }
        }
        Date date = BillInfoUtils.getBusinessDate();
        if (null != billContext.getDeleteReason()) {
            if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {// 删除流程实例
                return new RuleExecuteResult();
            }
        }

        Date currentDate = BillInfoUtils.getBusinessDate();
        if (currentDate.compareTo(date) < 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100717"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048B", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
        }

        return new RuleExecuteResult();
    }
}
