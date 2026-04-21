package com.yonyoucloud.fi.cmp.checkStockApply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.checkstockapply.CheckStockApply;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * 重空凭证入库提交
 */
@Slf4j
@Component("checkStockApplySubmitRule")
public class CheckStockApplySubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        BizObject bizObject = getBills(billContext, map).get(0);
        BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId(), 3);
        if (null == currentBill) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        Short auditStatus = Short.parseShort(currentBill.get("auditstatus").toString());
        if (auditStatus != null && auditStatus.equals(AuditStatus.Complete.getValue())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100889"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003E","单据[%s]已审批") /* "单据[%s]已审批" */,currentBill.get(ICmpConstant.CODE).toString()));
        }

        //未开启审批流则直接审核通过
        if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED) ) {
            CheckStockApply checkStockApply = MetaDaoHelper.findById(CheckStockApply.ENTITY_NAME, bizObject.getId());
            if (checkStockApply == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100062"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CE","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
            }
            // 未启动审批流，单据直接审批通过
            result = BillBiz.executeRule(ICmpConstant.AUDIT, billContext, map);
            result.setCancel(true);
            return result;
        }else {
            short verifyState = ValueUtils.isNotEmptyObj(currentBill.get("verifystate")) ? Short.parseShort(currentBill.get("verifystate").toString()) : (short) -1;
            if (VerifyState.TERMINATED.getValue() == verifyState) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100104"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804AA", "流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
            }
            return result;
        }
    }
}
