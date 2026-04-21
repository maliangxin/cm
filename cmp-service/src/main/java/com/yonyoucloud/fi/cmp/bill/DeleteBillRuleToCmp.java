package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IActionConstant;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.util.business.SystemCodeUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 收款单与付款单删除业务规则
 */
@Slf4j
@Component
public class DeleteBillRuleToCmp extends AbstractCommonRule {

	@Autowired
	CmpVoucherService cmpVoucherService;

	@Autowired
	JournalService journalService;

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
		BillDataDto bill = (BillDataDto)map.get("param");
		List<BizObject> bills = getBills(billContext, map);
		if (bills == null || bills.size() == 0) {
			return new RuleExecuteResult();
		}
		String billNum = billContext.getBillnum();
		if (CmpCommonUtil.getNewFiFlag()) {
			if (Arrays.asList(IBillNumConstant.PAYMENT, IBillNumConstant.PAYMENTLIST).contains(billContext.getBillnum())) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103016"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2579404E00007", "在财务新架构环境下，不允许删除付款单。") /* "在财务新架构环境下，不允许删除付款单。" */);
			}
			if (Arrays.asList(IBillNumConstant.RECEIVE_BILL, IBillNumConstant.RECEIVE_BILL_LIST).contains(billContext.getBillnum())) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103028"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2616804A00005", "在财务新架构环境下，不允许删除收款单。") /* "在财务新架构环境下，不允许删除收款单。" */);
			}
		}

		if (StringUtils.isBlank(billNum)) {
			return new RuleExecuteResult();
		}
		BizObject bizObject = (BizObject) bills.get(0);
		Long id = bizObject.getId();
		BizObject bizObjectNew = null;
//		Object objectStatus = bizObject.get("_status");
		checkPubTs(bizObject.getPubts() , bizObject.getId() , billContext.getFullname());
		CommonSqlExecutor metaDaoSupport = new CommonSqlExecutor(AppContext.getSqlSession());
		if (ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())) {
			bizObjectNew = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, id);
			bizObject.put("srcbillid", bizObjectNew.get("srcbillid"));
			bizObject.put("billtype", bizObjectNew.get("billtype"));
			bizObject.put("srcitem", bizObjectNew.get("srcitem"));
		}
		if (PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
			bizObjectNew = MetaDaoHelper.findById(PayBill.ENTITY_NAME, id);
			bizObject.put("srcbillid", bizObjectNew.get("srcbillid"));
			bizObject.put("billtype", bizObjectNew.get("billtype"));
			bizObject.put("status", bizObjectNew.get("status"));
			bizObject.put("srcitem", bizObjectNew.get("srcitem"));
		}
		if((bizObject.get("srctypeflag") != null && bizObject.get("srctypeflag").equals("auto"))|| (null != bizObject.get("billtype") && bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue())))) {
			CtmJSONObject jsonObject = new CtmJSONObject();
			jsonObject.put("id",id);
			boolean checked = false;
			if(ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())){
				jsonObject.put("billnum", IBillNumConstant.RECEIVE_BILL);
				// 修复原本逻辑问题，所有单据在现金收款工作台删除时都需校验凭证是否勾对。
				checked = cmpVoucherService.isChecked(jsonObject);
				if(bizObject.getShort("srcitem")!= EventSource.Cmpchase.getValue()){
					List<BizObject> objects = new ArrayList<>(1);
					objects.add(bizObject);
					bill.setData(objects);
					map.put("param",bill);
					if (bizObject.getShort("caobject")== CaObject.Customer.getValue()){
						billContext.setBillnum("cmp_receivedelete");
						bizObject.put("ca_type","arap_receivebill");
						BillBiz.executeRule("deletess", billContext, map);
					}else if (bizObject.getShort("caobject")== CaObject.Supplier.getValue()){
						billContext.setBillnum("cmp_receivedelete");
						bizObject.put("ca_type","arap_refundbill");
						BillBiz.executeRule("deletess", billContext, map);
					}
				}
			}
			billContext.setBillnum(billNum);
			if(PayBill.ENTITY_NAME.equals(billContext.getFullname())){
				jsonObject.put("billnum","cmp_payment");
				checked = cmpVoucherService.isChecked(jsonObject);
			}
			if (checked){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101054"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000BF", "凭证已勾对，不能删除") /* "凭证已勾对，不能删除" */);
			}
			metaDaoSupport.executeSql("update cmp_receivebill set auditor = null where id ="+id);
//			if(ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())){
//				bizObject = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME,id);
//			}
		}
		// begin 日结逻辑控制调整 majfd 21/06/07
		//已日结后不能修改或删除期初数据
		if ((bizObject.get("srctypeflag") != null && bizObject.get("srctypeflag").equals("auto")) || (null != bizObject.get("billtype") && bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue())))) {
			QuerySchema querySchema = QuerySchema.create().addSelect("1");
			querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name("settleflag").eq(1), QueryCondition.name("settlementdate").eq(bizObject.get("vouchdate"))
					, QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bizObject.get(IBussinessConstant.ACCENTITY))));
			List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME, querySchema);
			if (ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101055"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180229","该单据已日结，不能修改或删除单据！") /* "该单据已日结，不能修改或删除单据！" */);
			}
		}
		// end
		Short srcItem = -1;
		if (bizObject.get("srcitem") != null) {
			srcItem = Short.valueOf(bizObject.get("srcitem").toString());
		}
		if (bizObject.get("writeoffstatus") != null && bizObject.getShort("writeoffstatus") != WriteOffStatus.Incomplete.getValue()) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101056"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180231","发生核销的单据不能删除") /* "发生核销的单据不能删除" */);
		}
		if ((bill.getPartParam() != null && bill.getPartParam().get("outsystem") != null)
				|| (bizObject.get("srctypeflag") != null && bizObject.get("srctypeflag").equals("auto"))
				|| (null != bizObject.get("billtype") && bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue())))) {
			//来源为费用的或者自动生单的不做校验
			//来源为商业汇票的要校验单据是否结算，结算后不能删除
			if (srcItem.equals(EventSource.Drftchase.getValue()) && bizObject.get("settlestatus").equals(SettleStatus.alreadySettled.getValue())) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101057"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418022C","该单据在现金管理已经结算,不能进行删除!") /* "该单据在现金管理已经结算,不能进行删除!" */);
			}
		} else {
			if (billNum.startsWith("cmp")) {
				if (!srcItem.equals(EventSource.Cmpchase.getValue())) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101058"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180230","该单据不是现金自制单据，不能进行删除！") /* "该单据不是现金自制单据，不能进行删除！" */);
				}
			}
			if ((billNum.startsWith("arap") || "paymentlist".equals(billNum))) {
				if ((srcItem.equals(EventSource.Cmpchase.getValue()) ||
						srcItem.equals(EventSource.SystemOut.getValue()))) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101059"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180228","该单据不是收付单据，不能进行删除！") /* "该单据不是收付单据，不能进行删除！" */);
				}
			}
			Short auditStatus = -1;
			if (bizObject.get("auditstatus") != null) {
				auditStatus = Short.valueOf(bizObject.get("auditstatus").toString());
			}
			if (auditStatus.equals(AuditStatus.Complete.getValue())) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101060"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418022E","已审核单据，不能进行删除！") /* "已审核单据，不能进行删除！" */);
			}
		}
		//勾兑校验
		Boolean checkFlag = journalService.checkJournal(bizObject.getId());
		if (checkFlag) {
			if (!(bizObject.get("srctypeflag") != null && bizObject.get("srctypeflag").equals("auto")) && !(null != bizObject.get("billtype") && bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue())))) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101061"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180227","该单据已勾对，不能进行删除！") /* "该单据已勾对，不能进行删除！" */);
			}
		}
		// 删除日记账逻辑
		CmpWriteBankaccUtils.delAccountBook(bizObject.getId().toString(), false);

		Object billtype = bizObject.get("billtype");
		if(bizObject.get("srctypeflag") != null && bizObject.get("srctypeflag").equals("auto")|| (null != billtype &&
				(billtype.toString().equals(String.valueOf(EventType.CashMark.getValue()))
						|| billtype.toString().equals(String.valueOf(EventType.ReceiveBill.getValue()))
						|| billtype.toString().equals(String.valueOf(EventType.ApRefund.getValue()))))) {
			//对账单 重置为未生单状态,清除勾对标记
			autobillReset(billContext, bizObject);
			if (ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())) {
				bizObject.put("_entityName", ReceiveBill.ENTITY_NAME);
				assert bizObjectNew != null;
				bizObjectNew.put("_entityName", ReceiveBill.ENTITY_NAME);
			}
			if (PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
				bizObject.put("_entityName",PayBill.ENTITY_NAME);
				assert bizObjectNew != null;
				bizObjectNew.put("_entityName", PayBill.ENTITY_NAME);
			}
			CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(bizObjectNew);
			if (!deleteResult.getBoolean("dealSucceed")) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101062"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C0", "删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
			}
			// 调用应收规则，删除对应生单单据
			if("auto".equals(bizObject.get("srctypeflag"))){
				BillContext context = new BillContext();
				context.setBillnum(IBillNumConstant.RECEIVE_BILL);
				Map<String,Object> paramMap = new HashMap<>();
				String srcbillid = bizObject.get("srcbillid");
				String systemCode = SystemCodeUtil.getSystemCode(bizObjectNew);
				//对于自动生单生成的单据，删除时需删除应收对应单据
				if(StringUtils.isNotEmpty(srcbillid) &&( "fiar".equals(systemCode) || "fiap".equals(systemCode))){
					paramMap.put("srcbillid",bizObject.get("srcbillid"));
					billContext.setAction(IActionConstant.AUTOBILLDELETE);
					BillBiz.executeRule(IActionConstant.AUTOBILLDELETE,context,paramMap);
				}
			}
		}
		if(ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())){
			List<BizObject> objects = new ArrayList<>(1);
			objects.add(bizObject);
			bill.setData(objects);
			map.put("param",bill);
		}
		if(ReceiveBill.ENTITY_NAME.equals(billContext.getFullname()) ||
				PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
//			bizObject.set("_status",objectStatus);
			//对账单生成收款单后将是否勾兑设置为已勾兑
			if (null != bizObject.get("billtype") && bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue())) &&
					bizObject.get("srcbillid") != null && !bizObject.get("srcbillid").toString().equals("")) {
				BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, Long.parseLong(bizObject.get("srcbillid").toString()));
				bankReconciliation.setCheckflag(false);
				bankReconciliation.setAutobill(false);
				EntityTool.setUpdateStatus(bankReconciliation);
				CommonSaveUtils.updateBankReconciliation(bankReconciliation);
			}
		}
		if(PayBill.ENTITY_NAME.equals(billContext.getFullname())) {
			//对账单生成收款单后将是否勾兑设置为已勾兑
			if (null != bizObject.get("billtype") && bizObject.get("billtype").toString().equals("59") && bizObject.get("status").toString().equals("0")) {

				QuerySchema querySchemaJ = QuerySchema.create().addSelect("*");
				querySchemaJ.addCondition(QueryConditionGroup.and(QueryCondition.name("mainid").eq(id)));
				try {
					List<Map<String, Object>> mapList = MetaDaoHelper.query(PayBillb.ENTITY_NAME, querySchemaJ);
					mapList.forEach(e -> {
						try {
							Long srcbillitemid = Long.parseLong(e.get("srcbillitemid").toString());
							BigDecimal oriSum = (BigDecimal) e.get(IBussinessConstant.ORI_SUM);
							// 更新子表预占金额
							PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, srcbillitemid);
							if (null != payApplicationBill_b) {
								if (null != payApplicationBill_b.getPaymentPreemptAmount()) {
									payApplicationBill_b.setPaymentPreemptAmount(BigDecimalUtils.safeSubtract(payApplicationBill_b.getPaymentPreemptAmount(),oriSum));
								} else {
									payApplicationBill_b.setPaymentPreemptAmount(oriSum);
								}
								EntityTool.setUpdateStatus(payApplicationBill_b);
								MetaDaoHelper.update(PayApplicationBill_b.ENTITY_NAME, payApplicationBill_b);
								// 更新主表预占总数
								Long mainid = payApplicationBill_b.getMainid();
								PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, mainid);
								if (null != payApplicationBill.getPaymentPreemptAmountSum()) {
									payApplicationBill.setPaymentPreemptAmountSum(BigDecimalUtils.safeSubtract(payApplicationBill.getPaymentPreemptAmountSum(),oriSum));
								} else {
									payApplicationBill.setPaymentPreemptAmountSum(null);
								}
								EntityTool.setUpdateStatus(payApplicationBill);
								if ((null == payApplicationBill.getPaymentPreemptAmountSum()
										|| (payApplicationBill.getPaymentPreemptAmountSum().compareTo(BigDecimal.ZERO) == 0))) {
									payApplicationBill.setPayBillStatus(PayBillStatus.PendingApproval);
								}
								if (payApplicationBill.getPreemptAmountFull() == 1) {
									payApplicationBill.setPreemptAmountFull(0);
								}
								MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplicationBill);
							}
						} catch (Exception exception) {
							log.error("调整已付金额数据失败!:" + exception.getMessage());
						}
					});
				} catch (Exception e)   {
					log.error("查询付款申请单卡片失败:" + e.getMessage());
				}
			}
		}

		return new RuleExecuteResult();
	}

	/**
	 * 对账单重置为  未生单状态
	 * @param billContext
	 * @param bizObject
	 */
	private void autobillReset(BillContext billContext,BizObject bizObject) {
		try {
			QueryConditionGroup condition = new QueryConditionGroup();
			condition.addCondition(QueryConditionGroup.and(QueryCondition.name("autobill").eq(1)));
			condition.addCondition(QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bizObject.get(IBussinessConstant.ACCENTITY))));
			condition.addCondition(QueryConditionGroup.and(QueryCondition.name("currency").eq(bizObject.get("currency"))));
			if(ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())){
				// 收款单 查贷方金额
				condition.addCondition(QueryConditionGroup.and(QueryCondition.name("creditamount").eq(bizObject.get(IBussinessConstant.ORI_SUM))));
			} else if(PayBill.ENTITY_NAME.equals(billContext.getFullname())){
				// 付款单 查借方金额
				condition.addCondition(QueryConditionGroup.and(QueryCondition.name("debitamount").eq(bizObject.get(IBussinessConstant.ORI_SUM))));
			}
			QuerySchema schema = QuerySchema.create().addSelect("*");
			schema.addCondition(condition);
			List<Map<String, Object>> bankList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, schema);
			if (CollectionUtils.isNotEmpty(bankList)) {
				BankReconciliation bank = new BankReconciliation();
				bank.init(bankList.get(0));
				bank.setAutobill(false);
				bank.setCheckflag(false);
				EntityTool.setUpdateStatus(bank);
				CommonSaveUtils.updateBankReconciliation(bank);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
	/*
	 *@Author
	 *@Description 校验时间戳    收付款删除
	 *@Date 2020/7/4 10:20
	 *@Param [rows]
	 *@Return void
	 **/
	private void checkPubTs(Date puts , Long id , String fullName) throws Exception{
		BizObject bizObject = null;
		if(PayBill.ENTITY_NAME.equals(fullName)){
			bizObject = MetaDaoHelper.findById(PayBill.ENTITY_NAME,id);

		} else if (ReceiveBill.ENTITY_NAME.equals(fullName)) {
			bizObject = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, id);
		}
		if (bizObject == null) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101063"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418022A","单据不存在 id:") /* "单据不存在 id:" */ + id);
		}
		if (puts.compareTo(bizObject.getPubts()) != 0) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101064"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418022D","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
		}
	}
}