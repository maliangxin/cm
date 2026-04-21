package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 薪资支付审核规则
 * @author majfd
 *
 */
@Component
public class SalaryPayAuditRule extends AbstractCommonRule{

	@Autowired
	private JournalService journalService;
	@Autowired
	private IFundCommonService fundCommonService;
	@Autowired
	private CmCommonService commonService;

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		// TODO Auto-generated method stub
		List<BizObject> bills = getBills(billContext, paramMap);
		if(bills != null && bills.size() > 0) {
			Salarypay payBill = (Salarypay)bills.get(0);
			Date date = BillInfoUtils.getBusinessDate();
			Salarypay currentBill = (Salarypay)MetaDaoHelper.findById(billContext.getFullname(), payBill.getId(), 3);
			if(currentBill == null) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101820"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00263", "未查询到薪资支付单") /* "未查询到薪资支付单" */);
				//throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101821"),String.format(MessageUtils.getMessage("P_YS_FI_CM_0001353552") /* "单据【[%s]】已删除，请刷新后重试" */, payBill.get("code")));
			}
			//判断 OpenAPI 过来的单据 如果是审批流控制 则不允许审批
			if(ObjectUtils.isNotEmpty(currentBill.getIsWfControlled())){
				String str = paramMap.get("requestData").toString();
				if (str.contains("_fromApi") && currentBill.getIsWfControlled().equals(true)){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101822"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0025E", "当前单据【%s】已启用审批流，审核失败！") /* "当前单据【%s】已启用审批流，审核失败！" */, currentBill.getCode()));
				}
			}
			PayStatus payStatus = currentBill.getPaystatus();
            Date currentPubts = payBill.getPubts();
            if (currentPubts != null) {
                if (currentPubts.compareTo(currentBill.getPubts()) != 0) {
					//修改多语
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101823"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00261", "当前单据【%s】不是最新状态，请刷新单据重新操作。") /* "当前单据【%s】不是最新状态，请刷新单据重新操作。" */, currentBill.getCode()));
                }
            }
            if (null != billContext.getDeleteReason()) {
                if ("deleteAll".equalsIgnoreCase(billContext.getDeleteReason())) {//删除流程实例
                    return new RuleExecuteResult();
                }
            }
			if (date != null && currentBill.getVouchdate() != null && date.compareTo(currentBill.getVouchdate()) < 0) {
					throw new CtmException(
							com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0025F", "单据【") /* "单据【" */
									/* "单据【" */ + currentBill.getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils
											.getMessage("P_YS_FI_CM_0001066023") /* "】审批日期小于单据日期，不能审批！" */);
			}
			if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.Fail
					&& payStatus != PayStatus.PreFail) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101824"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00262", "当前单据【%s】支付状态不能进行审批！") /* "当前单据【%s】支付状态不能进行审批！" */, currentBill.getCode()));
			}
			AuditStatus auditStatus = currentBill.getAuditstatus();
			if (auditStatus != null && auditStatus == AuditStatus.Complete) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101825"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0025C", "当前单据【%s】已审批，不能进行重复审批！") /* "当前单据【%s】已审批，不能进行重复审批！" */, currentBill.getCode()));
			}
			if (StringUtils.isEmpty(currentBill.get("payBankAccount"))) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101826"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0025D", "当前单据【%s】的付款账户为空不能进行审核操作。") /* "当前单据【%s】的付款账户为空不能进行审核操作。" */, currentBill.getCode()));
			}
			if (currentBill.get("settlemode") == null) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101827"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00260", "当前单据【%s】的结算方式为空不能进行审核操作。") /* "当前单据【%s】的结算方式为空不能进行审核操作。" */, currentBill.getCode()));
			}

			//这里移动至Submit 进行占用
			// 如果没有开启审批流 审核的时候进行资金计划项目的占用
//			if(ObjectUtils.isNotEmpty(payBill.getIsWfControlled()) && payBill.getIsWfControlled().equals(false)){
//				//提交校验资金计划项目以及部门的必填性并占用资金计划
//				if (fundCommonService.checkFundPlanIsEnabled(payBill.get(IBussinessConstant.ACCENTITY))) {
//					Object controlSet = commonService.queryStrategySetbByCondition(payBill.get(IBussinessConstant.ACCENTITY), payBill.get(IBussinessConstant.CURRENCY), payBill.get(ICmpConstant.VOUCHDATE));
//					if (controlSet != null && !(CsplControlType.NOCONTROL.getValue() == controlSet)) {
//						List<BizObject> employFundBillForFundPlanProjectList = new ArrayList<>();
//
//						if (payBill.get(ICmpConstant.FUND_PLAN_PROJECT) == null) {
//							throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180552", "当前会计主体受控于资金计划且资金计划项目为空不能提交!") /* "当前会计主体受控于资金计划且资金计划项目为空不能提交!" */);
//						} else if (CsplControlType.CONTROLBYDEBT.getValue() == controlSet && payBill.get(ICmpConstant.DEPT) == null) {
//							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101828"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187FEF5E05600008", "当前部门受控于资金计划，请填写部门！") /* "当前部门受控于资金计划，请填写部门！" */);
//						} else {
//							payBill.set(ICmpConstant.IS_TO_PUSH_CSPL, 1);
//							employFundBillForFundPlanProjectList.add(payBill);
//						}
//
//						if (CollectionUtils.isNotEmpty(employFundBillForFundPlanProjectList)) {
//							Map<String, Object> map = new HashMap<>();
//							map.put(ICmpConstant.ACCENTITY, payBill.get(ICmpConstant.ACCENTITY));
//							map.put(ICmpConstant.VOUCHDATE, payBill.get(ICmpConstant.VOUCHDATE));
//							map.put(ICmpConstant.CODE, payBill.get(ICmpConstant.CODE));
//							List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameterSalarypay(employFundBillForFundPlanProjectList, IStwbConstantForCmp.EMPLOY, IBillNumConstant.SALARYPAY, map);
//							if (com.yonyoucloud.fi.cmp.util.ValueUtils.isNotEmptyObj(checkObject)) {
//								String result1 = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employ(checkObject);
//								if (result1 != null && !"".equals(result1)) {
//									throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101829"),result1);
//								}
//							}
//						}
//					}
//				}
//			}

			payBill.setEntityStatus(EntityStatus.Update);
			payBill.setAuditstatus(AuditStatus.Complete);
//			payBill.setVerifystate(VerifyState.COMPLETED.getValue());
			payBill.setAuditorId(AppContext.getCurrentUser().getId());
			payBill.setAuditor(AppContext.getCurrentUser().getName());
			payBill.setAuditTime(new Date());
			payBill.setAuditDate(BillInfoUtils.getBusinessDate());
			payBill.setPaystatus(PayStatus.NoPay);
			// 审批后规则
			// 更新日记账
			currentBill.setAuditstatus(AuditStatus.Complete);
			currentBill.setAuditorId(AppContext.getCurrentUser().getId());
			currentBill.setAuditor(AppContext.getCurrentUser().getName());
			currentBill.setAuditTime(new Date());
			currentBill.setAuditDate(BillInfoUtils.getBusinessDate());
			currentBill.setPaystatus(PayStatus.NoPay);
			journalService.updateJournal(currentBill);

		}
		return new RuleExecuteResult();
	}

}
