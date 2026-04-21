package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;
@Component
public class SalaryPayUnSubmitRule extends AbstractCommonRule{

	@Autowired
	private CmCommonService commonService;
	@Autowired
	private  CmpBudgetManagerService cmpBudgetManagerService;

	@Autowired
	private CmpBudgetSalarypayManagerService cmpBudgetSalarypayManagerService;

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		RuleExecuteResult result = new RuleExecuteResult();
		// TODO Auto-generated method stub
		List<BizObject> bills = getBills(billContext, paramMap);
		if (bills != null && bills.size() > 0) {
			Salarypay payBill = (Salarypay) bills.get(0);
			Salarypay currentBill = (Salarypay)MetaDaoHelper.findById(billContext.getFullname(), payBill.getId(), 3);
			if(currentBill == null) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100326"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00157", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, payBill.get("code")));
			}
			PayStatus payStatus = currentBill.getPaystatus();
            Date currentPubts = payBill.getPubts();
            if (currentPubts != null) {
                if (currentPubts.compareTo(currentBill.getPubts()) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100327"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00155", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            }
            if (currentBill.getInvalidflag() != null && currentBill.getInvalidflag().booleanValue()) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100328"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508006B", "单据【%s】已作废，不允许撤回！") /* "单据【%s】已作废，不允许撤回！" */,currentBill.getCode()));
			}
			short verifystate = Short.parseShort(payBill.get("verifystate").toString());
            //审批流状态为初始开立/驳回到制单/审批终止时，不允许撤回
			if(verifystate == VerifyState.INIT_NEW_OPEN.getValue() ||verifystate == VerifyState.REJECTED_TO_MAKEBILL.getValue() ||verifystate == VerifyState.TERMINATED.getValue() ){
				String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00041","单据[%s]，为初始开立/驳回到制单状态，不允许撤回") /* "单据[%s]，为初始开立/驳回到制单状态，不允许撤回" */,currentBill.get(ICmpConstant.CODE).toString());
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100329"),message);
			}
            //支付状态为预下单成功/支付成功/部分成功/线下支付成功/支付中/支付不明时，不允许撤回
            if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.Fail
					&& payStatus != PayStatus.PreFail) {
				String message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C00040","单据[%s]，支付处理中或者已支付完成，不允许撤回") /* "单据[%s]，支付处理中或者已支付完成，不允许撤回" */,currentBill.get(ICmpConstant.CODE).toString());
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100330"),message);
			}
			//释放资金计划
			releaseFundBillForFundPlan(payBill);
			if (null == payBill.getIsWfControlled() || !payBill.getIsWfControlled()) {
				if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
					cmpBudgetSalarypayManagerService.executeAuditDelete(currentBill);
					//刷新pubts
					Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, currentBill.getId(), null);
					payBill.setPubts(newBill.getPubts());
					payBill.setIsOccupyBudget(newBill.getIsOccupyBudget());
				}
				// 未启动审批流，单据直接调用弃审规则
				result = BillBiz.executeRule("unaudit", billContext, paramMap);
				result.setCancel(true);
			} else {
				if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
					cmpBudgetSalarypayManagerService.executeAuditDelete(currentBill);
				}
			}
			//刷新pubts
			Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, currentBill.getId(), null);
			payBill.setPubts(newBill.getPubts());
			payBill.setIsOccupyBudget(newBill.getIsOccupyBudget());
		}
		return result;
	}

	/**
	 * 释放资金计划
	 * @param payBill
	 */
	private void releaseFundBillForFundPlan(Salarypay payBill) throws Exception {
		List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
		Object isToPushCspl = payBill.get(ICmpConstant.IS_TO_PUSH_CSPL);
		if (ValueUtils.isNotEmptyObj(isToPushCspl) && 1 == Integer.parseInt(isToPushCspl.toString())) {
			payBill.set(ICmpConstant.IS_TO_PUSH_CSPL, 2);
			releaseFundBillForFundPlanProjectList.add(payBill);
		}
		if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
			Map<String, Object> map = new HashMap<>();
			map.put(ICmpConstant.ACCENTITY, payBill.get(ICmpConstant.ACCENTITY));
			map.put(ICmpConstant.VOUCHDATE, payBill.get(ICmpConstant.VOUCHDATE));
			map.put(ICmpConstant.CODE, payBill.get(ICmpConstant.CODE));
			List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameterSalarypay(releaseFundBillForFundPlanProjectList, IStwbConstantForCmp.RELEASE, IBillNumConstant.SALARYPAY, map);
			if (ValueUtils.isNotEmptyObj(checkObject)) {
				try {
					RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject);
				} catch (Exception e) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100331"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508006A", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
				}

			}
		}
	}





}
