package com.yonyoucloud.fi.cmp.batchtransferaccount.rule.workflow;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.ctm.stwb.settleapply.vo.PushOrder;
import com.yonyoucloud.ctm.stwb.unifiedsettle.pubitf.IUnifiedSettlePubService;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * @author xuxbo
 * @date 2025/6/5 16:12
 */
@Component
@Slf4j
public class BatchtransferaccountBeforeUnAuditRule extends AbstractCommonRule {

    @Autowired
    private IUnifiedSettlePubService unifiedSettlePubService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        for (BizObject bizobject : bills) {
            log.info("BatchtransferaccountBeforeUnAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
            BatchTransferAccount currentBill = MetaDaoHelper.findById(BatchTransferAccount.ENTITY_NAME, bizobject.getId());
            log.info("BatchtransferaccountBeforeUnAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
            if (currentBill == null) {
                throw new CtmException(new CtmErrorCode("033-502-100698"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180383",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400811", "单据不存在 id:") /* "单据不存在 id:" */) /* "单据不存在 id:" */ + bizobject.getId());
            }
            Date currentPubts = bizobject.getPubts();
            if (currentPubts != null) {
                if (!currentPubts.equals(currentBill.getPubts())) {
                    throw new CtmException(new CtmErrorCode("033-502-100700"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180385",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400812", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */) /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }

            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
            // 调用删除统一结算单接口
            List<String> idList = new ArrayList<>();
            idList.add(currentBill.getId());
            unifiedSettlePubService.deleteUnifiedSettle(BatchTransferAccount.ENTITY_NAME, idList.toArray(new String[0]), PushOrder.FIRST,PushOrder.SECOND);
        }
        return new RuleExecuteResult();
    }
}
