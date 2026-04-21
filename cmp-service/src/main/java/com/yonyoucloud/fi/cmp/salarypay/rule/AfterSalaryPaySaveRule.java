package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.ctm.stwb.stwbentity.BusinessBillType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PaymentStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.salarypay.SalaryPayService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay_b;
import com.yonyoucloud.fi.cmp.salarypay.util.SalaryPayUtil;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.SendBizMessageUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import com.yonyoucloud.fi.tmsp.enums.ServiceNameEnum;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;

/**
 * @Desc 薪资支付保存rule
 * @Date 2020/07/10
 * @Version 1.0
 * @author majfd
 */
@Slf4j
@Component
public class AfterSalaryPaySaveRule extends AbstractCommonRule {
	@Autowired
	BaseRefRpcService baseRefRpcService;
	@Autowired
	private CmpVoucherService cmpVoucherService;
	@Autowired
	CmpWriteBankaccUtils cmpWriteBankaccUtils;
	@Autowired
	private IFundCommonService fundCommonService;
	@Autowired
	private CmpBudgetManagerService cmpBudgetManagerService;
	@Autowired
	private CmCommonService commonService;
	@Autowired
	private CmpBudgetSalarypayManagerService cmpBudgetSalarypayManagerService;
	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		List<BizObject> bills = getBills(billContext, paramMap);
		if (bills != null && bills.size() > 0) {
			BizObject bizObject = bills.get(0);
			//回退逻辑
            CmpWriteBankaccUtils.delAccountBook(bizObject.get("id").toString());
			String billnum = billContext.getBillnum();

			//预占用
			preEmployFundPlan(bizObject, billnum);

			if(StringUtils.isEmpty(bizObject.get("payBankAccount"))) {
				return new RuleExecuteResult();
			}
			Journal journal = generateJournal(bizObject, Direction.Credit, bizObject.get("payBankAccount"), true, billContext);
			//支付成功，更新日记账记账日期，生成凭证，发送业务消息
			if (bizObject.get("paystatus") != null
					&& bizObject.getShort("paystatus").compareTo(PayStatus.Success.getValue()) == 0) {
				if (bizObject.get("settledate") != null) {
					journal.setDzdate(bizObject.get("settledate"));
				} else {
					if (BillInfoUtils.getBusinessDate() != null) {
						journal.setDzdate(BillInfoUtils.getBusinessDate());
					} else {
						journal.setDzdate(new Date());
					}
				}

				Salarypay payBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, bizObject.getId(), 3);
				payBill.set("_entityName", Salarypay.ENTITY_NAME);
				CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(payBill);
				if (!generateResult.getBoolean("dealSucceed")) {
					throw new CtmException(
							com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E3","发送会计平台失败：") /* "发送会计平台失败：" */
									/* "发送会计平台失败：" */ + generateResult.get("message"));
				}
				// 发送业务消息
				SendBizMessageUtils.sendBizMessageBySalaryFailData(payBill, "cmp_salarypay", "paysucceed");
				// 每条表体行支付失败的发送业务消息
				List<Salarypay_b> salarypay_bList = querySalarypay_bByMainId(bizObject.getId());
				List<Salarypay_b> bList = new ArrayList<Salarypay_b>();
				if (salarypay_bList != null && salarypay_bList.size() > 0) {
					for (Salarypay_b salaryPay_b : salarypay_bList) {
						if (PaymentStatus.PayFail.equals(salaryPay_b.getTradestatus())
								&& salaryPay_b.getInvalidflag().booleanValue()) {
							bList.add(salaryPay_b);
						}
					}
					if (bList != null && bList.size() > 0) {
						Salarypay newBill = payBill;
						newBill.setSalarypay_b(bList);
						SendBizMessageUtils.sendBizMessageBySalaryFailData(newBill, "cmp_salarypay", "payfail");
					}
				}
				if (payBill.getShort("srcitem") == EventSource.HRSalaryChase.getValue()) {
					payBill.setSalarypay_b(salarypay_bList);
					payCallback(payBill);
				}
			}
			cmpWriteBankaccUtils.addAccountBook(journal);
			//结算变更的单据，后置规则里面重新匹配
			if ("cmp_salarysettle".equals(billnum)) {
				log.error("结算变更的单据，后置规则里面重新匹配");
				if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
					Salarypay payBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, bizObject.getId(), 3);
					cmpBudgetSalarypayManagerService.executeSubmit(payBill);
					//刷新pubts
					Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, payBill.getId(), null);
					bizObject.setPubts(newBill.getPubts());
				}
			}

		}
		return new RuleExecuteResult();
	}

	private void preEmployFundPlan(BizObject bizObject, String billnum) throws Exception {
		if (fundCommonService.checkFundPlanIsEnabledBySalarypay(ServiceNameEnum.SALARY_PAYMENT) && IBillNumConstant.SALARYPAY.equals(billnum)) {
			List<BizObject> preEmployFundBillForFundPlanProjectList = new ArrayList<>();
			boolean isOpenBill = Short.parseShort(bizObject.get("verifystate").toString()) == VerifyState.INIT_NEW_OPEN.getValue();
			if (isOpenBill){
				if ("Insert".equals(bizObject.get("_status").toString())
						&& ValueUtils.isNotEmptyObj(bizObject.get("fundPlanProject"))){
					preEmployFundBillForFundPlanProjectList.add(bizObject);
				}
			}
			if (CollectionUtils.isNotEmpty(preEmployFundBillForFundPlanProjectList)){
				Map<String, Object> map2 = new HashMap<>();
				map2.put("accentity", bizObject.get("accentity"));
				map2.put("vouchdate", bizObject.get("vouchdate"));
				map2.put("code", bizObject.get("code"));
				List<CapitalPlanExecuteModel> checkObject2 = commonService.putCheckParameterSalarypay(preEmployFundBillForFundPlanProjectList, IStwbConstantForCmp.EMPLOY, billnum, map2);
				if (ValueUtils.isNotEmptyObj(checkObject2)) {
					try {
						RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).preEmployAndrelease(checkObject2);
						Map<String, Object> params = new HashMap<>();
						params.put("ytenantId", InvocationInfoProxy.getTenantid());
						params.put("id", bizObject.getId());
						params.put("tableName", "cmp_salarypay");
						params.put("isToPushCspl", 2);
						log.error("com.yonyoucloud.fi.cmp.fundcommon.business.AfterSaveFundBillRule.preFundPlan#InsertAndDelete, params={}", params);
						SqlHelper.update("com.yonyoucloud.fi.cmp.mapper.FundBillMapper.updateFundBillSubById", params);
					} catch (Exception e) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101293"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080023", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
					}
				}
			}
		}
	}

	/**
     * 支付回调外部系统
     *
     * @param bizObject
     */
	private void payCallback(Salarypay bizObject) throws Exception {
		// 回调外部系统
		if (EventSource.HRSalaryChase.getValue() != bizObject.getShort("srcitem")) {
			return;
		}
		String serverUrl = AppContext.getEnvConfig("hrservice.url");
		if (StringUtils.isEmpty(serverUrl)) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101294"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E2","hr通知路径【hrservice.url】为空，请检查配置文件") /* "hr通知路径【hrservice.url】为空，请检查配置文件" */);
		}

		CtmJSONObject params = new CtmJSONObject();
		CtmJSONObject param = SalaryPayUtil.formatConversionToHR(bizObject);
		params.put("data", param);
		params.put("yht_tenantid", InvocationInfoProxy.getTenantid().toString());
		params.put("yht_userid", AppContext.getCurrentUser().getYhtUserId());
		params.put("newArch", true);//财务新架构标识
		if(log.isInfoEnabled()) {
			log.info("AfterSalaryPaySaveRule通知薪资发放单数据：" + CtmJSONObject.toJSONString(params));
		}
		serverUrl = serverUrl + "/internal/pay/biz/payment/setPaymentStatus" + "?token=" + InvocationInfoProxy.getYhtAccessToken();
		String responseStr = HttpTookit.doPostWithJson(serverUrl, CtmJSONObject.toJSONString(params), null);
	}

	/**
	 * @return com.yonyoucloud.fi.cmp.journal.Journal
	 * @Author tongyd
	 * @Description 生成日记账
	 * @Date 2019/10/12
	 * @Param [bizObject, direction, accountId, isBank]
	 **/
	private Journal generateJournal(BizObject bizObject, Direction direction, String accountId, boolean isBank, BillContext billContext)
			throws Exception {
//		String serverUrl = AppContext.getEnvConfig("fifrontservername");
		Journal journal = new Journal();
		journal.setAccentity(bizObject.get(IBussinessConstant.ACCENTITY));
		journal.setBankaccount(accountId);
		journal.setBankaccountno(bizObject.get("payBankAccount_account"));
		if (!StringUtils.isEmpty(accountId)) {
			EnterpriseBankAcctVO dataMap = baseRefRpcService.queryEnterpriseBankAccountById(accountId);
			//增加 dataMap的判空
			if (dataMap != null && dataMap.getBank() != null) {
                journal.setBanktype(dataMap.getBank());
            }
		}
		journal.setNatCurrency(bizObject.get("natCurrency"));
		journal.setCurrency(bizObject.get("currency"));
		journal.setVouchdate(bizObject.get("vouchdate"));
		journal.setDescription(bizObject.get("summary"));
		journal.setTradetype(bizObject.get("tradetype"));
		journal.setExchangerate(bizObject.get("exchRate"));
		journal.setSettlemode(bizObject.get("settlemode"));
		journal.setNoteno(null);
		journal.setCheckflag(false);
		journal.setInsidecheckflag(false);
		journal.setProject(bizObject.get("project"));
		journal.setCostproject(bizObject.get("expenseitem"));
		journal.setRefund(false);
		journal.setSrcbillno(bizObject.get("code"));
//		journal.setSrcbillitemno(bizObject.get("code")); //来源单据行号，不赋值，因为薪资支付是总金额计日记账
        journal.setSrcbillitemid(bizObject.get("id").toString());
        journal.setOrg(bizObject.get("org"));
        journal.setDept(bizObject.get("dept"));
        journal.set("billnum", bizObject.get("code"));
        journal.setCreateDate(new Date());
        journal.setCreateTime(new Date());
        journal.setCreatorId(AppContext.getCurrentUser().getId());
        journal.setCreator(AppContext.getCurrentUser().getName());
        journal.setTenant(bizObject.get("tenant"));
        journal.setDirection(direction);
        if (bizObject.get("transeqno") != null) {
            journal.set("transeqno", bizObject.get("transeqno"));// 交易流水号
        }
        if (direction == Direction.Debit) {
            if (bizObject.get("paystatus") != null
                    && bizObject.getShort("paystatus").compareTo(PayStatus.Success.getValue()) == 0) {
                //支付成功，部分成功状态支付变更后，全部表体确认完表头支付状态为支付成功，记账金额取成功总金额
                journal.setDebitoriSum(bizObject.get("successmoney"));
                journal.setDebitnatSum(BigDecimalUtils.safeMultiply(bizObject.get("successmoney"), bizObject.get("exchRate")));
            } else {
                journal.setDebitoriSum(bizObject.get(IBussinessConstant.ORI_SUM));
                journal.setDebitnatSum(bizObject.get(IBussinessConstant.NAT_SUM));
            }
            journal.setCreditoriSum(BigDecimal.ZERO);
            journal.setCreditnatSum(BigDecimal.ZERO);
        } else {
            journal.setDebitoriSum(BigDecimal.ZERO);
            journal.setDebitnatSum(BigDecimal.ZERO);
            if (bizObject.get("paystatus") != null
                    && bizObject.getShort("paystatus").compareTo(PayStatus.Success.getValue()) == 0) {
                //支付成功，部分成功状态支付变更后，全部表体确认完表头支付状态为支付成功，记账金额取成功总金额
                journal.setCreditoriSum(bizObject.get("successmoney"));
                journal.setCreditnatSum(BigDecimalUtils.safeMultiply(bizObject.get("successmoney"), bizObject.get("exchRate")));
            } else {
                journal.setCreditoriSum(bizObject.get(IBussinessConstant.ORI_SUM));
                journal.setCreditnatSum(bizObject.get(IBussinessConstant.NAT_SUM));
            }
        }
        journal.set("srcitem", bizObject.get("srcitem"));
        journal.set("billtype", bizObject.get("billtype"));
        journal.set("paymentstatus", bizObject.get("paystatus"));
        journal.set("auditstatus", bizObject.get("auditstatus"));
        journal.set("settlestatus", bizObject.get("settlestatus"));
        journal.setBillno("cmp_salarypay"); //支付变更和结算变更后，银行日记账联查单据应该调转正常卡片页，非结算变更卡片页
        journal.setServicecode(IServicecodeConstant.SALARYPAY);
//        journal.setTargeturl(serverUrl+"/meta/ArchiveList/"+"cmp_salarypay");
		//添加源头单据信息处理
		if (bizObject.get("srcitem").toString().equals(String.valueOf(EventSource.HRSalaryChase.getValue()))) {
			if (bizObject.get("srcbillno") != null) {
				journal.set("topsrcbillno", bizObject.get("srcbillno").toString());
			}
			if (bizObject.get("srcbillid") != null) {
				journal.set("topsrcbillid", bizObject.get("srcbillid").toString());
			}
		}
		// 来源业务系统
		journal.set("topsrcitem", bizObject.get("srcitem"));
		// 业务单据类型
		journal.set("topbilltype", bizObject.get("billtype"));
		return journal;
	}

	/**
	 * 根据主表id查询所有表体数据
	 * @param id
	 * @throws Exception
	 */
	private List<Salarypay_b> querySalarypay_bByMainId(Long id) throws Exception {
		List<Map<String, Object>> payBListQuery = MetaDaoHelper.query(Salarypay_b.ENTITY_NAME,
				QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("mainid").eq(id)));
		List<Salarypay_b> payBList = new ArrayList<Salarypay_b>();
		if(payBListQuery == null || payBListQuery.size() == 0) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101295"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805E1","未查询到该单据明细信息。") /* "未查询到该单据明细信息。" */);
		}
		for (Map<String, Object> map : payBListQuery) {
			Salarypay_b pbyBill_b = new Salarypay_b();
			pbyBill_b.setEntityStatus(EntityStatus.Unchanged);
			pbyBill_b.init(map);
			payBList.add(pbyBill_b);
		}
		return payBList;
	}

}
