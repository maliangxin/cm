package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.OccupyBudget;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.BillAction;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 薪资支付弃审规则
 * @author majfd
 *
 */
@Slf4j
@Component
public class SalaryPayUnAuditRule extends AbstractCommonRule{

	@Autowired
	private JournalService journalService;

	@Autowired
    private SettlementService settlementService;

	@Autowired
	private CmCommonService commonService;

	@Autowired
	private CmpBudgetManagerService cmpBudgetManagerService;
	@Autowired
	private CmpBudgetSalarypayManagerService cmpBudgetSalarypayManagerService;

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		List<BizObject> bills = getBills(billContext, paramMap);
		if(bills != null && bills.size() > 0) {
			Salarypay payBill = (Salarypay)bills.get(0);
			Salarypay currentBill = (Salarypay)MetaDaoHelper.findById(billContext.getFullname(), payBill.getId(), 3);
			//如果输入无效id 返回未查询到薪资支付单
			if (null == currentBill) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101637"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800EE","未查询到薪资支付单") /* "未查询到薪资支付单" */);
			}
			//判断 OpenAPI 过来的单据 如果是审批流控制 则不允许审批
			if(ObjectUtils.isNotEmpty(currentBill.getIsWfControlled())){
				String str = paramMap.get("requestData").toString();
				if (str.contains("_fromApi") && currentBill.getIsWfControlled().equals(true)){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101638"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00044", "当前单据【%s】已启用审批流，弃审失败！") /* "当前单据【%s】已启用审批流，弃审失败！" */, currentBill.getCode()));
				}
			}

            Date currentPubts = payBill.getPubts();
            if (currentPubts != null) {
                if (currentPubts.compareTo(currentBill.getPubts()) != 0) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101639"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00045", "当前单据【%s】不是最新状态，请刷新单据重新操作。") /* "当前单据【%s】不是最新状态，请刷新单据重新操作。" */, currentBill.getCode()));
                }
            }
			YmsLock ymsLock = null;
            try {
                //和线下支付并发问题，添加pk锁
                ymsLock = JedisLockUtils.lockBillWithOutTrace(payBill.getId().toString());
                if (ymsLock == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101640"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E9","单据【") /* "单据【" */ + payBill.getCode()
                            + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800EA","】已锁定，请勿操作") /* "】已锁定，请勿操作" */);
                }
                PayStatus payStatus = currentBill.getPaystatus();
                if (payStatus != null && payStatus != PayStatus.NoPay && payStatus != PayStatus.PreFail
                        && payStatus != PayStatus.Fail) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800EB","单据【") /* "单据【" */
                                    /* "单据【" */ + currentBill.getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                    .getMessage("P_YS_FI_CM_0000153342") /* "】支付状态不能进行取消审批！" */);
                }
                if (currentBill.getInvalidflag() != null && currentBill.getInvalidflag().booleanValue()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101641"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00031", "单据【%s】已作废，不能进行取消审批！") /* "单据【%s】已作废，不能进行取消审批！" */,currentBill.getCode()));
                }
                AuditStatus auditStatus = currentBill.getAuditstatus();
                if (auditStatus != null && auditStatus.getValue() == AuditStatus.Incomplete.getValue()) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800EB","单据【") /* "单据【" */
                                    /* "单据【" */ + currentBill.getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                    .getMessage("P_YS_FI_CM_0000153352") /* "】未审批，不能进行取消审批！" */);
                }
                // 勾对完成后不能取消审批
                Boolean check = journalService.checkJournal(currentBill.getId());
                if (check) {

                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800EB","单据【") /* "单据【" */
                                    /* "单据【" */ + currentBill.getCode() + com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                    .getMessage("P_YS_FI_CM_0000153366") /* "】已勾对，不能取消审批！" */);
                }

				// 如果没有开启审批流 弃审的时候进行资金计划项目的释放
//				if(ObjectUtils.isNotEmpty(payBill.getIsWfControlled()) && payBill.getIsWfControlled().equals(false)) {
//					//释放资金计划
//					List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
//					Object isToPushCspl = payBill.get(ICmpConstant.IS_TO_PUSH_CSPL);
//					if (ValueUtils.isNotEmptyObj(isToPushCspl) && 1 == Integer.parseInt(isToPushCspl.toString())) {
//						payBill.set(ICmpConstant.IS_TO_PUSH_CSPL, 0);
//						releaseFundBillForFundPlanProjectList.add(payBill);
//					}
//					if (CollectionUtils.isNotEmpty(releaseFundBillForFundPlanProjectList)) {
//						Map<String, Object> map = new HashMap<>();
//						map.put(ICmpConstant.ACCENTITY, payBill.get(ICmpConstant.ACCENTITY));
//						map.put(ICmpConstant.VOUCHDATE, payBill.get(ICmpConstant.VOUCHDATE));
//						map.put(ICmpConstant.CODE, payBill.get(ICmpConstant.CODE));
//						List<CapitalPlanExecuteModel> checkObject = commonService.putCheckParameterSalarypay(releaseFundBillForFundPlanProjectList, IStwbConstantForCmp.RELEASE, IBillNumConstant.SALARYPAY, map);
//						if (ValueUtils.isNotEmptyObj(checkObject)) {
//							String result1 = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).release(checkObject);
//							if (result1 != null && !"".equals(result1)) {
//								throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101642"),result1);
//							}
//
//						}
//					}
//				}

				// 子表预算，实占成功，弃审时，需要释放实占，重新预占
				if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
					executeUnAudit(currentBill);
				}

                payBill.setEntityStatus(EntityStatus.Update);
                payBill.setAuditstatus(AuditStatus.Incomplete);
				payBill.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
                payBill.setPaystatus(PayStatus.NoPay);
                payBill.setPaymessage(null);
                payBill.setAuditorId(null);
                payBill.setAuditor(null);
                payBill.setAuditTime(null);
                payBill.setAuditDate(null);

				// 取消审批后规则
				// 更新日记账
				currentBill.setAuditstatus(AuditStatus.Incomplete);
				currentBill.setPaystatus(PayStatus.NoPay);
				currentBill.setPaymessage(null);
				currentBill.setAuditorId(null);
				currentBill.setAuditor(null);
				currentBill.setAuditTime(null);
				currentBill.setAuditDate(null);
				journalService.updateJournal(currentBill);
			}catch (Exception e){
				log.error(e.getMessage());
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101643"),e.getMessage());
			} finally {
				JedisLockUtils.unlockBillWithOutTrace(ymsLock);
			}
		}
		return new RuleExecuteResult();
	}

	/**
	 * 删除实占，重新预占
	 * @param salarypay
	 * @throws Exception
	 */
	private void executeUnAudit(Salarypay salarypay) throws Exception {
		Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, salarypay.getId());
		Short budgeted = newBill.getIsOccupyBudget();
		// 已经释放仍要释放，直接跳过不执行了
		if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
			return;
		} else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
			ResultBudget resultBudget = cmpBudgetSalarypayManagerService.gcExecuteTrueUnAudit(newBill, IBillNumConstant.SALARYPAY, BillAction.CANCEL_SUBMIT);
			if (resultBudget.isSuccess()) {
				//重新预占
				log.error("重新预占.....");
				//且结算状态应置为待结算、并清空结算成功时间
				ResultBudget budgetResult = cmpBudgetSalarypayManagerService.budget(newBill, newBill, IBillNumConstant.SALARYPAY, BillAction.SUBMIT);
				if (budgetResult.isSuccess()) {//可能是没有匹配上规则，也可能是没有配置规则
					salarypay.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
					Salarypay update = new Salarypay();
					update.setId(salarypay.getId());
					update.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
					update.setEntityStatus(EntityStatus.Update);
					MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
				} else {
					salarypay.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
					Salarypay update = new Salarypay();
					update.setId(salarypay.getId());
					update.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
					update.setEntityStatus(EntityStatus.Update);
					MetaDaoHelper.update(Salarypay.ENTITY_NAME, update);
				}
			} else {
				log.error("释放实占失败,resultBudget:{}", resultBudget);
			}
		}
	}

}
