package com.yonyoucloud.fi.cmp.receivebill.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class ReceiveBillSubmitRule extends AbstractCommonRule{

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103034"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2625E04A00007", "在财务新架构环境下，不允许提交收款单。")       /* "在财务新架构环境下，不允许提交收款单。" */);
        }
		List<BizObject> receiveBillList = getBills(billContext, paramMap);
		if (receiveBillList == null || receiveBillList.size() == 0) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101832"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804BE","请选择单据！") /* "请选择单据！" */);
		}
		for (BizObject bizObject : receiveBillList) {
			ReceiveBill currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId());
			Date currentPubts = bizObject.getPubts();
			short verifyState = ValueUtils.isNotEmptyObj(bizObject.get("verifystate")) ? Short.parseShort(bizObject.get("verifystate").toString()) : (short) -1;
			if (currentPubts != null) {
				if (currentBill.getPubts().compareTo(currentPubts) != 0) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101833"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804BB","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
				}
			}
			if (currentBill.getSettlemode() == null) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101834"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804BC","该单据的【结算方式】为空不能进行提交操作") /* "该单据的【结算方式】为空不能进行提交操作" */);
			}
			if (currentBill.getAuditstatus().getValue() == AuditStatus.Complete.getValue()) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101835"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804BD","单据已审批") /* "单据已审批" */);
			}
			if (VerifyState.TERMINATED.getValue() == verifyState) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101836"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804BF","流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
			}
//			boolean isWfControlled = ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.IS_WFCONTROLLED)) && bizObject.getBoolean(ICmpConstant.IS_WFCONTROLLED);
//			bizObject.set("isWfControlled", isWfControlled);
		}
		return new RuleExecuteResult();
	}

}
