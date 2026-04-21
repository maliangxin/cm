package com.yonyoucloud.fi.cmp.foreignpayment.rule.workflow;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 外汇付款 弃审前置规则*
 *
 * @author xuxbo
 * @date 2023/8/3 15:24
 */

@Slf4j
@Component
public class ForeignPaymentBeforeUnAuditRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizobject : bills) {
            String id = bizobject.getId().toString();
            YmsLock ymsLock = null;
            try {
                ymsLock = JedisLockUtils.lockRuleWithOutTrace(id,map);
                if (null == ymsLock) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100165"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180798", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
                log.info("ForeignPaymentBeforeUnAuditRule bizObject, id = {}, pubTs = {}", bizobject.getId(), bizobject.getPubts());
                ForeignPayment currentBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizobject.getId());
                log.info("ForeignPaymentBeforeUnAuditRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100166"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180799", "单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                Date currentPubts = bizobject.getPubts();
                if (currentPubts != null) {
                    if (!currentPubts.equals(currentBill.getPubts())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100167"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418079B", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                }
                Date date = currentBill.getVouchdate();
                if (null != billContext.getDeleteReason()) {
                    if (ICmpConstant.DELETEALL.equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                        return new RuleExecuteResult();
                    }
                }
                Date currentDate = BillInfoUtils.getBusinessDate();
                if (currentDate.compareTo(date) < 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100168"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418079A", "审核日期不能早于单据日期") /* "审核日期不能早于单据日期" */);
                }
                // 已弃审
                bizobject.putAll(currentBill);
                bizobject.set(ICmpConstant.VERIFY_STATE, VerifyState.INIT_NEW_OPEN.getValue());

                bizobject.set(ICmpConstant.AUDITORID, null);
                bizobject.set(ICmpConstant.AUDITOR, null);
                bizobject.set(ICmpConstant.AUDIT_DATE, null);
                bizobject.set(ICmpConstant.AUDIT_TIME, null);
            } catch (Exception e) {
                JedisLockUtils.unlockRuleWithOutTrace(map);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101781"),e.getMessage());
            }
        }
        return new RuleExecuteResult();
    }
}
