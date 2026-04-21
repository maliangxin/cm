package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.google.common.collect.Sets;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.cspl.vo.response.CapitalPlanExecuteResp;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.precision.CheckPrecision;
import com.yonyoucloud.fi.basecom.precision.CheckPrecisionVo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.salarypay.SalaryPayService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay_b;
import com.yonyoucloud.fi.cmp.salarypay.util.SalaryPayUtil;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.BillImportCheckUtil;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.tmsp.enums.ServiceNameEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.lang.DateTimeUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.dialect.DbDialect;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.ACCENTITY;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_ONE;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_ZERO;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CURRENCY;
import static com.yonyoucloud.fi.cmp.constant.IDomainConstant.MDD_DOMAIN_CTMCSPL;


/**
 * @Desc 薪资支付保存rule
 * @Date 2020/07/10
 * @Version 1.0
 * @author majfd
 *
 */
@Slf4j
@Component
public class BeforeSalaryPaySaveRule extends AbstractCommonRule {
	@Autowired
	private SettlementService settlementService;
	@Autowired
	BaseRefRpcService baseRefRpcService;
	@Autowired
	private JournalService journalService;

	@Autowired
	private SalaryPayService salaryPayService;

	@Autowired
	private CtmSignatureService digitalSignatureService;

	@Autowired
	CmCommonService cmCommonService;

	@Autowired
	BankAccountSettingService bankaccountSettingService;
    @Autowired
    private IFundCommonService fundCommonService;
    @Autowired
    private CmCommonService commonService;
	@Resource
	private YmsOidGenerator ymsOidGenerator;

    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;
	@Autowired
	private CmpBudgetManagerService cmpBudgetManagerService;
	@Autowired
	private CmpBudgetSalarypayManagerService cmpBudgetSalarypayManagerService;

	private static final String ENTERPRISEBANKACCOUNT = "enterpriseBankAccount";
	private static final String SYSTEMCODE = "system_0001";

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		List<BizObject> bills = getBills(billContext, paramMap);
		BillDataDto bill = (BillDataDto)paramMap.get("param");
		if (bills != null && bills.size() > 0) {
			Salarypay salaryPay = (Salarypay) bills.get(0);
			String billnum = billContext.getBillnum();
			String accEntity = salaryPay.getAccentity();
			// 数据校验
			beforeSaveCheck(salaryPay);
			Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(salaryPay.getAccentity());/* 暂不修改 */
			if(enabledBeginData == null) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101870"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F4","该资金组织现金管理模块未启用，不能保存单据！"));
			}
			//因为薪资传递的vouchdate有时间，所以转换为日期格式，去掉时间
			salaryPay.setVouchdate(DateUtils.dateTimeToDate(salaryPay.getVouchdate()));
			if(salaryPay.getPaydate() != null) {
				salaryPay.setPaydate(DateUtils.dateTimeToDate(salaryPay.getPaydate()));
			}
			if (enabledBeginData.compareTo(salaryPay.getVouchdate()) > 0) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100655"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180481","单据日期早于模块启用日期，不能保存单据！") );
			}
			Date currentDate = new Date();
			if (salaryPay.getVouchdate().compareTo(currentDate) > 0) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101871"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180482","单据日期晚于当前日期,不能保存单据！") );
			}

			// 校验日结日期，结算变更不校验日结
			if(!IBillNumConstant.SALARYPAY_SETTLE.equals(billnum) && !IBillNumConstant.SALARYPAY_UPDATE.equals(billnum)) {
				Date maxSettleDate = settlementService.getMaxSettleDate(salaryPay.getAccentity());
				if (maxSettleDate != null) {
					Date compareDate = salaryPay.getVouchdate();
					if (maxSettleDate.compareTo(compareDate) >= 0) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101872"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153370") /* "单据日期已日结，不能保存单据！" */);
					}
				}
			}
			//
			String payBankAccount = salaryPay.getPayBankAccount();
			if(!StringUtils.isEmpty(payBankAccount)){
				String data = bankaccountSettingService.getOpenFlag(payBankAccount);
				CtmJSONObject jsonObject = CtmJSONObject.parseObject(data);
				if(null != jsonObject){
					CtmJSONObject jsonData = jsonObject.getJSONObject("data");
					if(null != jsonData){
						salaryPay.setIsdirectconn(jsonData.getBoolean("openFlag"));
					}
				}
			}
			//签名验签校验
			StringBuffer signMsg = new StringBuffer();
			// 新增导入时处理逻辑，赋值默认值
			if(salaryPay.getEntityStatus() == null) {
				salaryPay.setEntityStatus(EntityStatus.Insert);
			}
			if (salaryPay.getEntityStatus() == EntityStatus.Insert) {
				Short olcmoneyDigit = 8;
				Integer olcratetype_digit = 6;
				// 组织本币
				String natCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(salaryPay.getAccentity());
				if(StringUtils.isEmpty(natCurrency)) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101873"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_FP_0000034921", "资金组织[") +salaryPay.get("accentity_name") +
							com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("P_YS_FI_CM_0001123705", "]组织本币币种为空!"));
				}
				CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(natCurrency);
				if (currencyTenantDTO!=null) {
					olcmoneyDigit = currencyTenantDTO.getMoneydigit().shortValue();
				}
				salaryPay.setNatCurrency(natCurrency);
				// 审批状态
				salaryPay.setAuditstatus(AuditStatus.Incomplete);
				salaryPay.setPaystatus(PayStatus.NoPay);
				salaryPay.setSettlestatus(SettleStatus.noSettlement);
				salaryPay.setVoucherstatus(VoucherStatus.Empty);
				BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
				//导入
				boolean importFlag =  "import".equals(billDataDto.getRequestAction());
				// OpenApi
				boolean openApiFlag = salaryPay.containsKey("_fromApi") && salaryPay.get("_fromApi").equals(true);
				boolean fromApi = billDataDto.getFromApi();
				// 事项来源、事项类型
				if((importFlag || openApiFlag || fromApi) || salaryPay.getBilltype() == null) {
					// 区分导入和薪资系统
					salaryPay.setSrcitem(EventSource.ManualImport);
					salaryPay.setBilltype(EventType.SalaryPayment);
					// 获取业务类型并检查是否为 null
					BusiTypeEnum busitype = salaryPay.getBusitype();
					if (busitype == null) {
						salaryPay.setBusitype(BusiTypeEnum.PAY_WAGES);
					}
					// 再次获取业务类型以确保不会出现空指针异常
					busitype = salaryPay.getBusitype();
					if (busitype == BusiTypeEnum.PAY_WAGES) {
						salaryPay.setAgenttype(null);
					}
				}else {
					salaryPay.setSrcitem(EventSource.HRSalaryChase);
					salaryPay.setBilltype(EventType.SalaryPayment);
					if(bill.getPartParam() != null && bill.getPartParam().get("srcbillno") != null){
						salaryPay.setSrcbillno(bill.getPartParam().get("srcbillno").toString());
					}
					salaryPay.setBusitype(BusiTypeEnum.PAY_WAGES);
				}

				// 交易类型
				if(StringUtils.isEmpty(salaryPay.getTradetype())) {
					Map<String, Object> condition = new HashMap<>();
					condition.put("code","DFGZ");
	                condition.put("billtype_id","FICA6");
					List<Map<String, Object>> transTypes = cmCommonService.getTransTypeByCondition(condition);
					if (!transTypes.isEmpty()) {
						salaryPay.set("tradetype", transTypes.get(0).get("id"));
						salaryPay.set("tradetype_code", transTypes.get(0).get("code"));
						salaryPay.set("tradetype_name", transTypes.get(0).get("name"));
					}
				}
				// 汇率类型
				//20260130 CZFW-495932 适配导入和OPENAPI汇率类型的转义问题
				ExchangeRateTypeVO exchangeRateTypeByFinOrg;
				if(StringUtils.isNotEmpty(salaryPay.getString("exchangeRateType"))){
					exchangeRateTypeByFinOrg =  baseRefRpcService.queryExchangeRateById(salaryPay.getString("exchangeRateType"));
				}else{
					exchangeRateTypeByFinOrg = CmpExchangeRateUtils.getNewExchangeRateType(salaryPay.getAccentity(), true);
				}
                salaryPay.set("exchangeRateType", exchangeRateTypeByFinOrg.getId());
                salaryPay.set("exchangeRateType_digit", exchangeRateTypeByFinOrg.getDigit());
                salaryPay.set("exchangeRateType_name", exchangeRateTypeByFinOrg.getName());
                olcratetype_digit = exchangeRateTypeByFinOrg.getDigit();
				// 汇率
				BigDecimal exchRate = BigDecimal.ZERO;
				Short exchRateOps = (short)1;
				CmpExchangeRateVO exchangeRateVO;
				//用户自定义汇率，不询汇，取导入或者OpenApi传入的汇率
				if(CurrencyRateTypeCode.CustomCode.getValue().equals(exchangeRateTypeByFinOrg.getCode())){
					if (salaryPay.get("exchRate") == null){
						throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21AF2CCC05B0000B", "汇率值不可为空，汇率类型【%s】需要指定对应的汇率值") /* "汇率值不可为空，汇率类型【%s】需要指定对应的汇率值" */,exchangeRateTypeByFinOrg.getName()));
					}
					exchRate = salaryPay.getBigDecimal("exchRate");
					exchRateOps = "/".equals(salaryPay.getString("exchRateOps")) || "2".equals(salaryPay.getString("exchRateOps")) ? (short)2 : (short)1;
				}else {
					exchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(salaryPay.getCurrency(), natCurrency, salaryPay.getVouchdate(), exchangeRateTypeByFinOrg.getId());
					if(BigDecimal.ZERO.equals(exchangeRateVO.getExchangeRate())) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101874"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804E4","币种找不到汇率") /* "币种找不到汇率" */);
					}
					exchRate = exchangeRateVO.getExchangeRate();
					exchRateOps = exchangeRateVO.getExchangeRateOps();
				}

				salaryPay.setExchRate(exchRate.setScale(olcratetype_digit, BigDecimal.ROUND_HALF_UP));
                salaryPay.setExchRateOps(exchRateOps);

				List<Salarypay_b> salarypay_bList = salaryPay.getBizObjects("Salarypay_b", Salarypay_b.class);
				if(salarypay_bList == null || salarypay_bList.size() == 0) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101875"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804E7","薪资支付单表体数据不能为空，请检查") /* "薪资支付单表体数据不能为空，请检查" */);
				}
				BigDecimal amount = BigDecimal.ZERO;
				int line = 0;
				List<String> listSrcBillid_b = new ArrayList<String>();
				List<Object> personids = new ArrayList<Object>();
				// 计算金额
				boolean srcformHR = EventSource.HRSalaryChase.equals(salaryPay.getSrcitem());
				// start 针对薪资支付的数据，为了根据id进行排序签名，所以需要提前给id赋值
				for(Salarypay_b salarypay_b : salarypay_bList){
					salarypay_b.setId(ymsOidGenerator.nextId());
				}
				salarypay_bList = salarypay_bList.stream().sorted(Comparator.comparing(map -> map.get("id"))).collect(Collectors.toList());
				// end 针对薪资支付的数据，为了根据id进行排序签名，所以需要提前给id赋值
				for (Salarypay_b salaryPay_b : salarypay_bList) {
					salaryPay_b.setEntityStatus(EntityStatus.Insert);
					if (BigDecimal.ZERO.compareTo(salaryPay_b.getAmount()) >= 0) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101876"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153412") /* "转账金额必须大于0" */);
					}
					if(srcformHR) {
						if(StringUtils.isEmpty(salaryPay_b.getPersonnum())) {
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101877"),MessageUtils.getMessage("P_YS_FI_CM_0001155032") /* "员工编号不能为空" */);
						}
						personids.add(salaryPay_b.getPersonnum());
					}else{
						if(StringUtils.isEmpty(salaryPay_b.getShowpersonnum())) {
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101878"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F5","员工编号不能为空") /* "员工编号不能为空" */);
						}
					}

					// 2024.11.28 去掉此校验 CZFW-394469 薪资支付单导入去掉 收方证件类型 与 收方证件号码 两个字段的必输（答应项目1203）
//					if (StringUtils.isEmpty(salaryPay_b.getIdentitytype()) || StringUtils.isEmpty(salaryPay_b.getIdentitynum())) {
//						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101879"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D2","收方证件类型和收方证件号码不能为空") /* "收方证件类型和收方证件号码不能为空" */);
//					}
					if (StringUtils.isEmpty(salaryPay_b.getCrtacc()) || StringUtils.isEmpty(salaryPay_b.getCrtaccname())) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101880"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D4","收款账号和收款户名不能为空") /* "收款账号和收款户名不能为空" */);
					}

					salaryPay_b.setTradestatus(PaymentStatus.NoPay);
					line++;
					amount = BigDecimalUtils.safeAdd(amount, salaryPay_b.getAmount());
//					amount = amount.add(salaryPay_b.getAmount());
					signMsg.append(salaryPay_b.getAmount().stripTrailingZeros().toPlainString() + salaryPay_b.getCrtacc()
							+ salaryPay_b.getCrtaccname());
				}
				if(personids != null && personids.size() >0) {
					List<Map<String, Object>> queryStaffByIds = QueryBaseDocUtils.queryStaffByIds(personids);/* 暂不修改 */
					Map<String,String> statffMap = new HashMap<String,String>();
					for (int i = 0; i < queryStaffByIds.size(); i++) {
						statffMap.put(queryStaffByIds.get(i).get("id").toString(), queryStaffByIds.get(i).get("code").toString());
					}
					for (Salarypay_b salaryPay_b : salarypay_bList) {
						if(statffMap.containsKey(salaryPay_b.getPersonnum())) {
							salaryPay_b.setShowpersonnum(statffMap.get(salaryPay_b.getPersonnum()));
						}
					}
				}
				if(salaryPay.getOriSum() == null || BigDecimal.ZERO.compareTo(salaryPay.getOriSum()) >= 0) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101881"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153412") /* "转账金额必须大于0" */);
				}
				if(salaryPay.getOriSum().compareTo(amount) != 0) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101882"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804EA","表体收款金额合计不等于表头付款总金额") /* "表体收款金额合计不等于表头付款总金额" */);
				}
				if(salaryPay.getNumline() == null || salaryPay.getNumline().intValue() != line) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101883"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804EE","表体收款人信息合计行不等于表头付款总笔数") /* "表体收款人信息合计行不等于表头付款总笔数" */);
				}
                salaryPay.setNatSum(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, salaryPay.getOriSum(), Integer.valueOf(olcmoneyDigit)));
				/**
				 *  薪资对支付失败数据重新发起时，id不变，所以需要校验原单据是否已处理完成
				 *  完成标志：表头添加字段invalidflag 已经确认支付失败并且作废的，或者表体和表头支付成功的单据，记标
				 */
				if(EventSource.HRSalaryChase.equals(salaryPay.getSrcitem()) && salaryPay.getSrcbillid() != null) {
					salaryPayService.hasSalaryPayBySrcbillid(salaryPay.getSrcbillid(), listSrcBillid_b);
				}
				// 导入数据校验
				if (importFlag || openApiFlag) {
	                doImportCheck(billContext,salaryPay);
				}
				//数据签名
				salaryPay.set("signature", DigitalSignatureUtils.NEW_SIGN + digitalSignatureService.iTrusSignMessage(signMsg.toString()));
				//新增时给审批流状态赋值 初始开立
                salaryPay.set("verifystate",VerifyState.INIT_NEW_OPEN.getValue());
			}

            // 签名信息(收款金额、收方银行账号、收方银行账号名称)
            // 存在旧的账户余额，则将余额表数据还原，将日记账记录删除
            if (salaryPay.getEntityStatus() == EntityStatus.Update) {
                //校验签名
                checkSign(salaryPay);
                Boolean check = journalService.checkJournal(salaryPay.getId());
                if (check) {
                    throw new CtmException(
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D5","单据已勾对") /* "单据已勾对" */
                            /* "单据已勾对" */);
                }


				// 处理跨行标识
				if(IBillNumConstant.SALARYPAY.equals(billnum) || IBillNumConstant.SALARYPAY_SETTLE.equals(billnum)) {
					//不在保存的时候处理，因为修改保存没有表体数据

				} else if (IBillNumConstant.SALARYPAY_UPDATE.equals(billnum)) {
					salaryPayUpdateProcess(salaryPay);
				}
                //薪资支付适配资金计划项目：导入和编辑保存  都需要校验部门和资金计划项目
                checkFundPlanProject(billnum, salaryPay, accEntity);
			}
			//当结算方式与付款银行账号为空时，进行默认值的赋值
			//1.判断结算方式是否为空，如果为空，则赋值为默认值：银行转账
			if (ObjectUtils.isEmpty(salaryPay.get("settlemode"))){
				//根据结算方式code、资金组织、是否启用、业务属性进行查询
				SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
				settleMethodQueryParam.setCode(SYSTEMCODE);
				settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
				settleMethodQueryParam.setServiceAttr(CONSTANT_ZERO);
				List<SettleMethodModel> settleMethodModelList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
                if (CollectionUtils.isNotEmpty(settleMethodModelList)) {
                    salaryPay.set("settlemode", settleMethodModelList.get(0).getId());
                }
			}

			//2.如果付款银行账号为空，则根据资金组织和币种进行查询默认的银行账号
			if(StringUtils.isEmpty(salaryPay.get("payBankAccount"))) {
				//根据资金组织和币种，获取默认银行账号
				EnterpriseBankAcctVO defaultBankAccount = cmCommonService.getDefaultBankAccount(salaryPay.get(ACCENTITY), salaryPay.get(CURRENCY));
				//如果没有默认银行账号，则直接返回
				if (ObjectUtils.isEmpty(defaultBankAccount)){
					return new RuleExecuteResult();
				} else {
					//为付款银行账号赋值
					salaryPay.set("payBankAccount", defaultBankAccount.getId());
					//是否直联标识
					String payBankAccount_default = salaryPay.getPayBankAccount();
//					CtmJSONObject param = new CtmJSONObject();
//					param.put(ENTERPRISEBANKACCOUNT,payBankAccount_default);
					String data = bankaccountSettingService.getOpenFlag(payBankAccount_default);
					CtmJSONObject jsonObject = CtmJSONObject.parseObject(data);
					if (null != jsonObject){
						CtmJSONObject jsonData = jsonObject.getJSONObject("data");
						if (null != jsonData){
							salaryPay.setIsdirectconn(jsonData.getBoolean("openFlag"));
						}
					}
				}
			}

			//结算变更的单据，先释放，后置规则里面重新匹配
			if ("cmp_salarysettle".equals(billnum)) {
				log.error("结算变更的单据，先释放");
				if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
					cmpBudgetSalarypayManagerService.reMatchBudget(salaryPay, false);
					//刷新pubts
					Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, salaryPay.getId(), null);
					salaryPay.setPubts(newBill.getPubts());
				}
			} else if (salaryPay.getVerifystate() != null && salaryPay.getVerifystate() != VerifyState.INIT_NEW_OPEN.getValue()) {
				//保存提交后重新匹配预算规则，原预算规则预占删除，新规则占预占,审批结束后结算变更重新预占
				if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
					cmpBudgetSalarypayManagerService.reMatchBudget(salaryPay,true);
					//刷新pubts
					Salarypay newBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, salaryPay.getId(), null);
					salaryPay.setPubts(newBill.getPubts());
					//salaryPay.setIsOccupyBudget(newBill.getIsOccupyBudget());
				}
			}
		}
		return new RuleExecuteResult();
	}

	/**
	 * 支付变更逻辑处理
	 * @throws Exception
	 */
	private void salaryPayUpdateProcess(Salarypay salaryPay) throws Exception {
		// 处理支付变更，所有表体确认后，赋值表头支付状态为支付成功
		List<Salarypay_b> salarypay_bList = salaryPay.getBizObjects("Salarypay_b", Salarypay_b.class);
		boolean unkunown = false;
		int failnum = 0;
		int successnum = 0;
		if(salarypay_bList == null || salarypay_bList.size() == 0) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101884"),MessageUtils.getMessage("P_YS_FI_CM_0001155011") /* "没有变更的表体明细行支付状态信息，请取消保存。" */);
		}
		Short olcmoneyDigit = SalaryPayUtil.getOlcmoneyDigit(salaryPay);
		BigDecimal exchRate = salaryPay.getExchRate();
        Short exchRateOps = salaryPay.getExchRateOps();
		List<Salarypay_b> bodys = querySalarypay_bByMainId(salaryPay.getId());
		Map<Long, Salarypay_b> map = new HashMap<Long, Salarypay_b>();
		for (Salarypay_b salaryPay_b : salarypay_bList) {
			if (PaymentStatus.PayFail.equals(salaryPay_b.getTradestatus())) {
				if(StringUtils.isEmpty(salaryPay_b.getTrademessage())) {
					salaryPay_b.setTrademessage(MessageUtils.getMessage("P_YS_FI_CM_0001154978") /* "【支付状态确认失败】" */);
				}else {
					salaryPay_b.setTrademessage(salaryPay_b.getTrademessage() + MessageUtils.getMessage("P_YS_FI_CM_0001154978") /* "【支付状态确认失败】" */);
				}
				salaryPay_b.setInvalidflag(Boolean.TRUE);
				salaryPay.setFailmoney(BigDecimalUtils.safeAdd(salaryPay.getFailmoney(), salaryPay_b.getAmount()));
                salaryPay.setOlcfailmoney(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, salaryPay.getFailmoney(), Integer.valueOf(olcmoneyDigit)));
				salaryPay.setFailnum(BigDecimalUtils.safeAdd(salaryPay.getFailnum(), new BigDecimal(1)));
				salaryPay.setUnknownmoney(BigDecimalUtils.safeSubtract(salaryPay.getUnknownmoney(), salaryPay_b.getAmount()));
                salaryPay.setOlcunknownmoney(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, salaryPay.getUnknownmoney(), Integer.valueOf(olcmoneyDigit)));
				salaryPay.setUnknownnum(BigDecimalUtils.safeSubtract(salaryPay.getUnknownnum(), new BigDecimal(1)));
			}else if (PaymentStatus.PayDone.equals(salaryPay_b.getTradestatus())) {
				if(StringUtils.isEmpty(salaryPay_b.getTrademessage())) {
					salaryPay_b.setTrademessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804DC","【支付状态确认成功】") /* "【支付状态确认成功】" */);
				}else {
					salaryPay_b.setTrademessage(salaryPay_b.getTrademessage() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804DC","【支付状态确认成功】") /* "【支付状态确认成功】" */);
				}
				salaryPay.setSuccessmoney(BigDecimalUtils.safeAdd(salaryPay.getSuccessmoney(), salaryPay_b.getAmount()));
                salaryPay.setOlcsuccessmoney(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, salaryPay.getSuccessmoney(), Integer.valueOf(olcmoneyDigit)));
				salaryPay.setSuccessnum(BigDecimalUtils.safeAdd(salaryPay.getSuccessnum(), new BigDecimal(1)));
				salaryPay.setUnknownmoney(BigDecimalUtils.safeSubtract(salaryPay.getUnknownmoney(), salaryPay_b.getAmount()));
                salaryPay.setOlcunknownmoney(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(exchRateOps, exchRate, salaryPay.getUnknownmoney(), Integer.valueOf(olcmoneyDigit)));
				salaryPay.setUnknownnum(BigDecimalUtils.safeSubtract(salaryPay.getUnknownnum(), new BigDecimal(1)));
			}
			map.put(salaryPay_b.getId(), salaryPay_b);
		}

		for (Salarypay_b body : bodys) {
			if(map.containsKey(body.getId())) {
				body = map.get(body.getId());
			}
			if (PaymentStatus.UnkownPay.equals(body.getTradestatus())
					|| body.getTradestatus() == null) {
				unkunown = true;
				break;
			}
			if (PaymentStatus.PayFail.equals(body.getTradestatus())) {
				failnum++;
			} else if (PaymentStatus.PayDone.equals(body.getTradestatus())) {
				successnum++;
			}
		}
		if(unkunown) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101885"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F0","当前单据表体行存在支付状态不明的表体明细，请支付状态确认后保存！") /* "当前单据表体行存在支付状态不明的表体明细，请支付状态确认后保存！" */);
		}
		if (BigDecimalUtils.safeAdd(salaryPay.getSuccessmoney(),salaryPay.getFailmoney()).compareTo(salaryPay.getOriSum()) != 0) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101886"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F2","成功总金额和失败总金额合计不等于付款总金额，请检查！") /* "成功总金额和失败总金额合计不等于付款总金额，请检查！" */);
		}
		salaryPay.setPayupdateman(AppContext.getCurrentUser().getId());
		salaryPay.setPayupdatedate(new Date());
		if (PayStatus.PartSuccess.equals(salaryPay.getPaystatus())) {
			salaryPay.setPaystatus(PayStatus.Success);
		}
		if ((PayStatus.Paying.equals(salaryPay.getPaystatus()) || PayStatus.PayUnknown.equals(salaryPay.getPaystatus())) && failnum == salaryPay.getNumline().intValue()) {
			salaryPay.setPaystatus(PayStatus.Fail);
			if (StringUtils.isEmpty(salaryPay.getPaymessage())) {
				salaryPay.setPaymessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D3","【支付状态确认失败】") /* "【支付状态确认失败】" */);
			}
		} else {
			salaryPay.setPaystatus(PayStatus.Success);
		}

		if(PayStatus.Success.equals(salaryPay.getPaystatus())) {
			// 校验日结日期，支付变更成功需要校验日结
			Date maxSettleDate = settlementService.getMaxSettleDate(salaryPay.getAccentity());
			if (maxSettleDate != null) {
				//支付变更时，和系统日期比较，因为支付变更为支付成功，赋值的登账日期为系统日期
				Date compareDate = DateUtils.dateTimeToDate(new Date());
				if (maxSettleDate.compareTo(compareDate) >= 0) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101887"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153370") /* "单据日期已日结，不能保存单据！" */);
				}
			}
			salaryPay.setSettleuserId(AppContext.getCurrentUser().getId());
			salaryPay.setSettleuser(AppContext.getCurrentUser().getName());
			salaryPay.setSettledate(new Date());
			salaryPay.setSettlestatus(SettleStatus.alreadySettled);
		}
        List<BizObject> releaseFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> partReleaseFundBillForFundPlanProjectList = new ArrayList<>();
        //资金计划相关逻辑
        //如果失败金额不为空 并且 与本币总金额不相等 则是部分失败 调用释放部分金额的接口
        Object isToPushCspl = salaryPay.get(ICmpConstant.IS_TO_PUSH_CSPL);
        if (ValueUtils.isNotEmptyObj(isToPushCspl) && 1 == Integer.parseInt(isToPushCspl.toString())) {
			if (ValueUtils.isNotEmptyObj(salaryPay.getOlcfailmoney()) && salaryPay.getOlcfailmoney().compareTo(salaryPay.getNatSum()) != 0) {
				partReleaseFundBillForFundPlanProjectList.add(salaryPay);
			}
        }
        //资金计划部分金额释放
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(partReleaseFundBillForFundPlanProjectList)) {
            Map<String, Object> map2 = new HashMap<>();
            map2.put("accentity", salaryPay.get("accentity"));
            map2.put("vouchdate", salaryPay.get("vouchdate"));
            map2.put("code", salaryPay.get("code"));
            map2.put("settleFailed", true);
            List<CapitalPlanExecuteModel> checkObject = cmCommonService.putCheckParameterSalaryPayOldInterface(partReleaseFundBillForFundPlanProjectList, IStwbConstantForCmp.RELEASE, IBillNumConstant.SALARYPAY, map2);
            if (ValueUtils.isNotEmptyObj(checkObject)) {
                try {
                    RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).releaseExecutionAmount(checkObject);
                } catch (Exception e) {
                    log.error("SalaryPay part release fail! data={}, e={}", CtmJSONObject.toJSONString(checkObject), e.getMessage());
                }
            }
        }
		//占预算
		if (salaryPay.getPaystatus() != null && salaryPay.getPaystatus().getValue() == PayStatus.Success.getValue()) {
			if (cmpBudgetManagerService.isCanStart(IBillNumConstant.SALARYPAY)) {
				log.error("支付成功，执行paySuccessBudget");
				paySuccessBudget(salaryPay);
			}
		}
	}

	//支付成功实占
	public void paySuccessBudget(Salarypay payBill) throws Exception {
		Salarypay oldBill = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, payBill.getId(), null);
		Short isOccupyBudget = oldBill.getIsOccupyBudget();
		if (isOccupyBudget != null && isOccupyBudget == OccupyBudget.ActualSuccess.getValue()) {
			log.error("已经实占跳过");
		} else {
			if (isOccupyBudget != null && isOccupyBudget == OccupyBudget.PreSuccess.getValue()) {
				//释放原来的
				log.error("释放原来的");
				ResultBudget releaseBudget = cmpBudgetSalarypayManagerService.releaseBudget(oldBill, oldBill, IBillNumConstant.SALARYPAY, BillAction.CANCEL_SUBMIT);
				if (releaseBudget.isSuccess()) {
					log.error("释放成功");
					payBill.set("characterDef",oldBill.get("characterDef"));
					//实占最新的
					ResultBudget resultBudget = cmpBudgetSalarypayManagerService.gcExecuteTrueAudit(payBill, IBillNumConstant.SALARYPAY, BillAction.SUBMIT, false);
					if (resultBudget.isSuccess()) {
						log.error("实占成功");
						payBill.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
					} else {
						log.error("实占失败");
						payBill.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
					}
				}
			} else {
				payBill.set("characterDef",oldBill.get("characterDef"));
				//实占最新的
				ResultBudget resultBudget = cmpBudgetSalarypayManagerService.gcExecuteTrueAudit(payBill, IBillNumConstant.SALARYPAY, BillAction.SUBMIT, false);
				if (resultBudget.isSuccess()) {
					log.error("实占成功");
					payBill.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
				} else {
					log.error("实占失败");
					payBill.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
				}
			}
		}
	}

	private void checkSign(Salarypay bizObject) throws Exception{
		if(StringUtils.isEmpty(bizObject.get("signaturestr")) || ICmpConstant.AUTOTEST_SIGNATURE_Constant.equalsIgnoreCase(bizObject.get("signaturestr"))) {
			return;
		}
		String SIGNCONST = "guan1226";
		// 页面数据进行抓包验证
		String payBankAccount = bizObject.get("payBankAccount");
		String payBankAccount_account = bizObject.get("payBankAccount_account");
		String payBankAccount_name = bizObject.get("payBankAccount_name");
		String currency = bizObject.get("currency");
		String currency_name = bizObject.get("currency_name");

		BigDecimal numline = bizObject.getNumline() == null ? null : bizObject.getNumline();
		String numlineNoZero=numline.stripTrailingZeros().toPlainString();
		BigDecimal oriSum = bizObject.getOriSum();
		String oriSumsNoZero=oriSum.stripTrailingZeros().toPlainString();
		BigDecimal natSum= bizObject.getNatSum();
		String natSumNoZero=natSum.stripTrailingZeros().toPlainString();

		BigDecimal successnum = bizObject.getSuccessnum() == null ? null : bizObject.getSuccessnum();
		BigDecimal successmoney = bizObject.getSuccessmoney();
		BigDecimal olcsuccessmoney= bizObject.getOlcsuccessmoney();
		BigDecimal unknownnum = bizObject.getUnknownnum() == null ? null : bizObject.getUnknownnum();
		BigDecimal unknownmoney = bizObject.getUnknownmoney();
		BigDecimal olcunknownmoney= bizObject.getOlcunknownmoney();
		BigDecimal failnum = bizObject.getFailnum() == null? null : bizObject.getFailnum();
		BigDecimal failmoney = bizObject.getFailmoney();
		BigDecimal olcfailmoney= bizObject.getOlcfailmoney();

		StringBuffer signStr = new StringBuffer();
		if(!StringUtils.isEmpty(payBankAccount)) {
			signStr.append(payBankAccount);
		}
		if(!StringUtils.isEmpty(payBankAccount_account)) {
			signStr.append(payBankAccount_account);
		}
		if(!StringUtils.isEmpty(payBankAccount_name)) {
			signStr.append(payBankAccount_name);
		}
		if(!StringUtils.isEmpty(currency)) {
			signStr.append(currency);
		}
		if(!StringUtils.isEmpty(currency_name)) {
			signStr.append(currency_name);
		}
		signStr.append(numlineNoZero).append(oriSumsNoZero).append(natSumNoZero);
		if(successnum != null && BigDecimal.ZERO.compareTo(successnum) != 0) {
			String successnumNoZero=successnum.stripTrailingZeros().toPlainString();
			signStr.append(successnumNoZero);
		}
		if(successmoney != null && BigDecimal.ZERO.compareTo(successmoney) != 0) {
			String successmoneyNoZero=successmoney.stripTrailingZeros().toPlainString();
			signStr.append(successmoneyNoZero);
		}
		if(olcsuccessmoney != null && BigDecimal.ZERO.compareTo(olcsuccessmoney) != 0) {
			String olcsuccessmoneyNoZero=olcsuccessmoney.stripTrailingZeros().toPlainString();
			signStr.append(olcsuccessmoneyNoZero);
		}
		if(unknownnum != null && BigDecimal.ZERO.compareTo(unknownnum) != 0) {
			String unknownnumNoZero=unknownnum.stripTrailingZeros().toPlainString();
			signStr.append(unknownnumNoZero);
		}
		if(unknownmoney != null && BigDecimal.ZERO.compareTo(unknownmoney) != 0) {
			String unknownmoneyNoZero=unknownmoney.stripTrailingZeros().toPlainString();
			signStr.append(unknownmoneyNoZero);
		}
		if(olcunknownmoney != null && BigDecimal.ZERO.compareTo(olcunknownmoney) != 0) {
			String olcunknownmoneyNoZero=olcunknownmoney.stripTrailingZeros().toPlainString();
			signStr.append(olcunknownmoneyNoZero);
		}

        if (failnum != null && BigDecimal.ZERO.compareTo(failnum) != 0) {
            String failnumNoZero = failnum.stripTrailingZeros().toPlainString();
            signStr.append(failnumNoZero);
        }
        if (failmoney != null && BigDecimal.ZERO.compareTo(failmoney) != 0) {
            String failmoneyNoZero = failmoney.stripTrailingZeros().toPlainString();
            signStr.append(failmoneyNoZero);
        }
        if (olcfailmoney != null && BigDecimal.ZERO.compareTo(olcfailmoney) != 0) {
            String olcfailmoneyNoZero = olcfailmoney.stripTrailingZeros().toPlainString();
            signStr.append(olcfailmoneyNoZero);
        }
        signStr.append(SIGNCONST);                     // 加密串
        String md5Str = SHA512Util.getSHA512Str(signStr.toString());
        if (!md5Str.equalsIgnoreCase(bizObject.get("signaturestr"))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101888"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804EC","单据【") /* "单据【" */ + bizObject.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804ED","】数据签名验证失败") /* "】数据签名验证失败" */);
        }
    }

    /**
     * 根据主表id查询所有表体数据
     *
     * @param id
     * @throws Exception
     */
    private List<Salarypay_b> querySalarypay_bByMainId(Long id) throws Exception {
        List<Map<String, Object>> payBListQuery = MetaDaoHelper.query(Salarypay_b.ENTITY_NAME,
                QuerySchema.create().addSelect("*").appendQueryCondition(QueryCondition.name("mainid").eq(id)));
        List<Salarypay_b> payBList = new ArrayList<Salarypay_b>();
        if (payBListQuery == null || payBListQuery.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101889"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F6","未查询到该单据明细信息。") /* "未查询到该单据明细信息。" */);
        }
        for (Map<String, Object> map : payBListQuery) {
            Salarypay_b pbyBill_b = new Salarypay_b();
            pbyBill_b.setEntityStatus(EntityStatus.Unchanged);
            pbyBill_b.init(map);
            payBList.add(pbyBill_b);
        }
        return payBList;
    }

	private void doImportCheck(BillContext billContext, BizObject bizObject) throws Exception {
        //导入进来的数据,导入翻译数据时添加过滤了
//		if(bizObject.get("settlemode") != null) {
//			Map<String, Object> condition = new HashMap<>();
//			condition.put("id", bizObject.get("settlemode"));
//			Map<String, Object> settleModeMap = QueryBaseDocUtils.querySettlementWayByCondition(condition).get(0);
//			if(!(Boolean) settleModeMap.get("isEnabled")){
//				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101890"),MessageUtils.getMessage("P_YS_FI_CM_0001123696") /* "结算方式未启用,请检查！" */);
//			}
//			if(((Integer)settleModeMap.get("serviceAttr")) != 0) {
//				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101891"),MessageUtils.getMessage("P_YS_FI_CM_0001155043") /* "结算方式非法,结算方式业务属性必须银行业务！" */);
//			}
//		}

		//导入翻译数据时添加过滤了，不再次校验
//		if(bizObject.get("tradetype") != null) {
//			Map<String, Object> transTypeMap = QueryBaseDocUtils.queryTransTypeById(bizObject.get("tradetype"));
//			if(((Integer)transTypeMap.get("enable")).intValue() != 1 || ((Integer)transTypeMap.get("dr")).intValue() != 0) {
//				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101892"),MessageUtils.getMessage("P_YS_FI_CM_0001154971") /* "交易类型未启用或者已删除,请检查！" */);
//			}
//			if(!transTypeMap.get("billtype_id").equals("FICA6")) {
//				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101893"),MessageUtils.getMessage("P_YS_FI_CM_0001155000") /* "交易类型非法,请导入所属薪资支付单的交易类型！" */);
//			}
//		}

        //资金计划项目
        Map<String, String> fundPlanProjectCodeMap = new HashMap<>();
        // 资金计划项目档案
        Set<String> fundPlanProjectList = new HashSet<>();
		// 数据权限过滤
		Map<String, String> fieldName = new HashMap<String, String>();
		fieldName.put("dept", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0007B", "部门") /* "部门" */);
		fieldName.put("project", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0007C", "项目") /* "项目" */);
		fieldName.put("expenseitem", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0007D", "费用项目") /* "费用项目" */);
		fieldName.put("settlemode", MessageUtils.getMessage("P_YS_FI_ETL_0001077740") /* "结算方式" */);
		fieldName.put("payBankAccount", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0007E", "付款银行账号") /* "付款银行账号" */);
		List<String> fieldList = new ArrayList<String>();
		for (String field : fieldName.keySet()) {
			if (bizObject.get(field) == null) {
				continue;
			}
			fieldList.add(field);
		}
		Map<String, List<Object>> accounts = AuthUtil.dataPermission("CM", Salarypay.ENTITY_NAME, null, fieldList.toArray(new String[0]));
		if (accounts != null && accounts.size() > 0) {
			for (String field : fieldList) {
				if (bizObject.get(field) == null) {
					continue;
				}
				List<Object> fieldData = accounts.get(field);
				if (CollectionUtils.isNotEmpty(fieldData)) {
					if (!fieldData.contains(bizObject.get(field).toString())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101894"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804E2","数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(field) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804E1","无权") /* "无权" */);
					}
				}
			}
		}

		//1.选了业务组织，   除了银行账户/现金账户，其它都是以业务组织为准。部门，人员也是以业务组织+职能共享 参照出来。
		//2.没有选业务组织，除了银行账户/现金账户，其它都是以资金组织+核算委托关系的组织为准。部门，人员也是以组织+职能共享 参照出来。
		//3.银行账户/现金账户，以资金组织为准
		String[] fields = new String[] { "dept", "project", "expenseitem" };
		List<String> project = new ArrayList<>();
		if (bizObject.get("project") != null) {
			project.add(bizObject.get("project"));
		}
//		for (String field : fields) {
//			if (bizObject.get(field) == null) {
//				continue;
//			}
//			String fullname = null;
//			Map<String, Object> queryDept = new HashMap<>();
//			if("dept".equals(field)) {
//				queryDept = QueryBaseDocUtils.queryDeptById(bizObject.get(field)).get(0);/* 暂不修改 已登记*/
//				fullname = "aa.baseorg.DeptMV";
//			}else if("project".equals(field)) {
//				//已修改
//				project.add(bizObject.get("project"));
//				//queryDept = QueryBaseDocUtils.queryProjectById(bizObject.get(field)).get(0);/* 暂不修改 已登记*/
//				//fullname = "bd.project.ProjectVO";
//				continue;
//			}else if("expenseitem".equals(field)) {
//				queryDept = QueryBaseDocUtils.queryExpenseItemById(bizObject.get(field)).get(0);/* 暂不修改 已登记*/
//				fullname = "bd.expenseitem.ExpenseItem";
//			}
//
//			Set<String> filterOrgMC = filterOrgMC(fullname, bizObject);
//			String orgfield = AuthUtil.getBDOrgField(fullname);
//            Object value = queryDept.get(orgfield);
//            if ("dept".equals(field)) {
//                filterDept(bizObject, value, filterOrgMC);
//            } else {
//                if (!filterOrgMC.contains(value)) {
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101895"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D1","组织管控数据校验异常:") /* "组织管控数据校验异常:" */ + fieldName.get(field) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D0","与资金组织业务组织不匹配"));
//                }
//            }
//		}
		// 校验业务组织权限
		CmpCommonUtil.checkAuthBusOrg(bizObject.getString("org"),bizObject.get(IBussinessConstant.ACCENTITY));
		// 部门权限校验
//		CmpCommonUtil.checkdept(bizObject.getString("dept"), bizObject.get(IBussinessConstant.ACCENTITY));
		if(bizObject.get("expenseitem") != null && !CmpCommonUtil.checkExpenseitem(bizObject.get("expenseitem").toString(), bizObject.get(IBussinessConstant.ACCENTITY))){
			throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1EA3864005680008","所选费用项目无权限或存在未启用财资业务领域的情况，保存失败！ ") /* "所选费用项目无权限或存在未启用财资业务领域的情况，保存失败！ " */);
		}
		if(project.size()>0){
			List<String> orgids = BillImportCheckUtil.queryOrgRangeSByProjectIds(project);
			// 核算委托关系的没有校验，后续优化，该逻辑后续调整为批量处理，要优化
			if (!orgids.contains(bizObject.get(IBussinessConstant.ACCENTITY))) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101896"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D9","导入的项目所属使用组织与导入资金组织不一致！"));
			}
		}
		if(bizObject.get("payBankAccount")!=null){
			//银行账户
			EnterpriseBankAcctVO payBankAccount = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("payBankAccount"));
			//导入数据，记日记账银行账户编码赋值
			bizObject.set("payBankAccount_account", payBankAccount.getAccount());
			Integer enable = payBankAccount.getEnable();
			if(1!=enable){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101897"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804DD","银行账号为:") /* "银行账号为:" */ + payBankAccount.getAccount() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804DE","的账户未启用,请检查!"));
			}
			CmpCommonUtil.checkBankAcctCurrency(bizObject.get("payBankAccount"), bizObject.get("currency"));
			EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bizObject.get("payBankAccount"));
			if(enterpriseBankAcctVoWithRange != null){
				List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
				// 使用范围中的组织是否是授权的组织
				String accentity = bizObject.getString(IBussinessConstant.ACCENTITY);
				Set<String> rangeOrgIdList = new HashSet<>();
				for(OrgRangeVO orgRangeVO : orgRangeVOS){
					String rangeOrgId = orgRangeVO.getRangeOrgId();
					rangeOrgIdList.add(rangeOrgId);
				}
				if(!rangeOrgIdList.contains(accentity)){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101898"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180801","银行账号与会计主体不匹配！") /* "银行账号与会计主体不匹配！" */);
				}
			}
		}

		//校验资金计划项目
		if (fundCommonService.checkFundPlanIsEnabledBySalarypay(ServiceNameEnum.SALARY_PAYMENT)) {
			String fundPlanProjectCode = bizObject.getString("fundPlanProject_code");
			if (ValueUtils.isNotEmptyObj(fundPlanProjectCode)) {
				// 查询资金计划档案
				String accentity = bizObject.getString(ICmpConstant.ACCENTITY);
				String currency = bizObject.getString(ICmpConstant.CURRENCY);
				Date expectdate= bizObject.get("vouchdate");
				String dept = bizObject.get("dept");
				List<Long> resultData = QueryBaseDocUtils.queryPlanStrategyIsEnable(accentity, currency, expectdate, dept);
				if (CollectionUtils.isEmpty(resultData)) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101899"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508007B", "资金计划项目编码[%s]未查询到符合条件的计划编辑明细") /* "资金计划项目编码[%s]未查询到符合条件的计划编辑明细" */ , fundPlanProjectCode));
				}
				BillContext fundPlanBillContext = new BillContext();
				fundPlanBillContext.setFullname("cspl.plansummary.PlanSummaryB");
				fundPlanBillContext.setDomain(IDomainConstant.MDD_DOMAIN_CTMCSPL);
				QueryConditionGroup fundPlanCondition = new QueryConditionGroup();
				fundPlanCondition.addCondition(QueryCondition.name("id").in(resultData));

				Set<String> codeList = new HashSet<>();
				Set<Long> idList = new HashSet<>();
				try {
					Long id = Long.valueOf(fundPlanProjectCode);
					idList.add(id);
					codeList.add(fundPlanProjectCode);
				} catch (NumberFormatException e) {
					codeList.add(fundPlanProjectCode);
				}
				if (CollectionUtils.isNotEmpty(idList)) {
					fundPlanCondition.addCondition(QueryConditionGroup.or(QueryCondition.name("capitalPlanProjectNo").in(codeList.toArray()), QueryCondition.name("id").in(idList.toArray())));
				} else {
					fundPlanCondition.addCondition(QueryCondition.name("capitalPlanProjectNo").in(codeList.toArray()));
				}
				List<Map<String, Object>> fundPlanList = MetaDaoHelper.queryAll(fundPlanBillContext,
						"id",
						fundPlanCondition, null);
				if (CollectionUtils.isNotEmpty(fundPlanList)) {
					Map<String, Object> objectMap = fundPlanList.get(0);
					bizObject.set("fundPlanProject", objectMap.get("id"));
				} else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101900"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418080B","资金计划项目编码[%s]未在资金计划项目档案中查询到，请检查是否存在该编码，或是否已停用") /* "资金计划项目编码[%s]未在资金计划项目档案中查询到，请检查是否存在该编码，或是否已停用" */, fundPlanProjectCode));
				}
			}
		} else {
			bizObject.set("fundPlanProject", null);
		}

		String childrenField = MetaDaoHelper.getChilrenField(billContext.getFullname());
		List<BizObject> lines = (List) bizObject.get(childrenField);
		if(Objects.isNull(lines)||lines.size()<1){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101901"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804E3","子表获取失败，请查看主子表关联关系") /* "子表获取失败，请查看主子表关联关系" */);
		}

		BigDecimal exchrate = bizObject.get("exchRate");
		if(exchrate ==  null ){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101902"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804E5","汇率不能为空！") /* "汇率不能为空！" */);
		}
		if(exchrate.doubleValue()<0){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101903"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804E6","汇率不能小于0！") /* "汇率不能小于0！" */);
		}
        Map<String,BigDecimal> exchrateMap =new HashMap();
        exchrateMap.put("exchRate", exchrate);
        //汇率类型精度
        CheckPrecisionVo checkPrecisionVo = new CheckPrecisionVo();
		checkPrecisionVo.setEntityName(billContext.getFullname());
		ExchangeRateTypeVO exchangeRateTypeVO = baseRefRpcService.queryExchangeRateTypeById(bizObject.getString("exchangeRateType"));
        Integer digit = exchangeRateTypeVO.getDigit();
        checkPrecisionVo.setPrecisionNum(digit);
        checkPrecisionVo.setNumericalMap(exchrateMap);
        CheckPrecision.checkPreByPreNum(checkPrecisionVo);
		//精度校验
		Map<String, BigDecimal> numericalMap = new HashMap<String, BigDecimal>();
		//原币金额精度校验
		BigDecimal oriSum = bizObject.get(IBussinessConstant.ORI_SUM);
		checkPrecisionVo.setPrecisionId(bizObject.get("currency"));
		numericalMap.put(IBussinessConstant.ORI_SUM,oriSum);
		checkPrecisionVo.setNumericalMap(numericalMap);
		CheckPrecision.checkMoneyByCurrency(checkPrecisionVo);
		//数据重复校验
		QuerySchema schema = QuerySchema.create().addSelect("id");
		QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
		conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(getValue(bizObject.get(IBussinessConstant.ACCENTITY))));
		conditionGroup.appendCondition(QueryCondition.name("vouchdate").eq(getValue(bizObject.get("vouchdate"))));
		if (bizObject.get("payBankAccount") != null) {
			conditionGroup.appendCondition(
					QueryCondition.name("payBankAccount").eq(getValue(bizObject.get("payBankAccount"))));
		}
		conditionGroup.appendCondition(QueryCondition.name("numline").eq(getValue(bizObject.get("numline"))));
		conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ORI_SUM).eq(getValue(oriSum)));
		schema.addCondition(conditionGroup);
		List<Map<String, Object>> payBillList = MetaDaoHelper.query(Salarypay.ENTITY_NAME, schema);
		if (payBillList != null && payBillList.size() > 0) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101904"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804DA","导入数据校验，数据重复！") /* "导入数据校验，数据重复！" */);
		}

	}

	private void filterDept(BizObject data, Object value, Set<String> filterOrgMC) throws Exception {
		/** 1）当前会计主体及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门
		    2）如果有销售组织，就取销售组织下的部门，以及和他有职能共享关系的部门 */
		Set<String> orgids  = new HashSet<String>();
		Set<String> deptids = new HashSet<String>() ;
		buildDeptInfo(orgids, deptids, data);

		if(!deptids.isEmpty() && deptids.contains(data.get("dept").toString())) {
			orgids.addAll(filterOrgMC);
			if(!orgids.contains(value)) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101905"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804E0","该部门无权限，非当前会计主体及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门") /* "该部门无权限，非当前会计主体及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门" */);
			}
		}else {
			if (orgids != null && !orgids.contains(value)) {
				throw new CtmException(
						com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D1","组织管控数据校验异常:") /* "组织管控数据校验异常:" */ + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D6","部门") /* "部门" */
								+ com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804D0","与会计主体业务组织不匹配") /* "与会计主体业务组织不匹配" */);
			}
		}
	}

	/**
	1）当前会计主体及通过核算委托关系的组织下的部门，以及有职能共享关系的业务单元下的部门
   	2）如果有业务组织，就取业务组织下的部门，以及和他有职能共享关系的部门
   	3)当前会计主体、业务组织（库存组织）对应的部门+与会计主体有核算委托关系的组织下的部门
	 */
	private void buildDeptInfo(Set<String> orgids,Set<String> deptids,BizObject data) throws Exception {
		List<String>  bizorgs =  AuthUtil.getBizObjectAttr(data,IBillConst.ORG);
		Set<String> orgidts =null;
		Set<String> deptidts =null;
		if(bizorgs == null ) {
			// 当组织为空时显示当前会计主体 以及有核算委托关系的销售组织下的启用状态的部门。
			List<String> accentity = AuthUtil.getBizObjectAttr(data,IBillConst.ACCENTITY) ;
			//根据委托关系
			orgidts = FIDubboUtils.getDelegateHasSelf(accentity.toArray(new String[0]) ) ;
			if(orgidts==null) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101906"),"keynull");
			}
			//根据职能共享
			deptidts = FIDubboUtils.getDeptShare(orgidts.toArray(new String[0])) ;
			orgidts = FIDubboUtils.getOrgShareHasSelf(orgidts.toArray(new String[0])) ;
			orgidts.addAll(accentity);
			deptids.addAll(deptidts);
			orgids.addAll(orgidts);
		}else {
			//当前会计主体、业务组织（库存组织）对应的部门+与会计主体有核算委托关系的组织下的部门
			orgidts = FIDubboUtils.getOrgShareHasSelf( bizorgs.toArray(new String[0])) ;
			deptidts = FIDubboUtils.getDeptShare( bizorgs.toArray(new String[0])) ;

			orgids.addAll(orgidts);
			deptids.addAll(deptidts);
		}
	}

	private Set<String> filterOrgMC(String fullname, BizObject bizObject) throws Exception {
		List<String> orgs = new ArrayList<>();
		orgs.add(bizObject.get(IBussinessConstant.ACCENTITY));
		boolean needDelegate = true;
		List<String> bizorgs = new ArrayList<>();
		if(bizObject.get("org") != null) {
			bizorgs.add(bizObject.get("org"));
			if(bizorgs!=null) {
				orgs=bizorgs;
				needDelegate=false;
			}
		}
		String[] orgarrs = orgs.toArray(new String[0]);
        String orgfield = AuthUtil.getBDOrgField(fullname);
        if (orgfield != null && orgarrs != null && orgarrs.length > 0 && orgarrs[0] != null) {
            Set<String> retorgids = null;
            if (needDelegate) {
                retorgids = FIDubboUtils.getDelegateHasSelf(orgarrs);
                retorgids = FIDubboUtils.orgMCFilterHasSelf(fullname, retorgids.toArray(new String[0]));
            } else {
                retorgids = Sets.newHashSet(orgarrs);
            }
			retorgids.add(IStwbConstantForCmp.GLOBAL_ACCENTITY);
            return retorgids;
        }
		return null;
    }

	private static String getValue(Object v) {
        if (v == null) {
            return DbDialect.NULL;
        } else if (v instanceof String) {
            return v.toString();
        } else if (v instanceof Date) {
            return DateTimeUtils.format((Date) v, false);
        }
        return v.toString();
	}

	private void beforeSaveCheck(Salarypay salaryPay) throws Exception {
		// TODO Auto-generated method stub
		 try {
             if(FIDubboUtils.isSingleOrg()){
                 BizObject singleOrg = FIDubboUtils.getSingleOrg();
                 if(singleOrg != null){
                	 salaryPay.set(IBussinessConstant.ACCENTITY,singleOrg.get("id"));
                	 salaryPay.set("accentity_name",singleOrg.get("name"));
                 }
             }
         }catch (Exception e){
             log.error("单组织判断异常!",e);
			 throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101907"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804EB","单组织判断异常！") /* "单组织判断异常！" */ + e.getMessage());
         }
		if(StringUtils.isEmpty(salaryPay.getAccentity())) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101908"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804EF","会计主体不能为空") /* "会计主体不能为空" */);
		}
		if(salaryPay.getVouchdate() == null ) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101909"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F1","单据日期不能为空") /* "单据日期不能为空" */);
		}
		if(StringUtils.isEmpty(salaryPay.getCurrency())) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101910"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804F3","币种不能为空") /* "币种不能为空" */);
		}
		//交易类型停用校验
		CmpCommonUtil.checkTradeTypeEnable(salaryPay.getTradetype());
	}


    /**
     * 部门：现金参数-是否启用资金计划为是，并且资金计划-计划控制设置-业务控制方式为按部门控制，导入时，校验部门必填
     * 资金计划项目：现金参数-是否启用资金计划为是，并且资金计划-计划控制设置-业务控制方式为按部门控制/按会计主体控制，导入时，校验资金计划项目必填
     *
	 */
	private void checkFundPlanProject(String billnum, Salarypay salaryPay, String accEntity) throws Exception {
		// 预占用
		List<BizObject> preEmployFundBillForFundPlanProjectList = new ArrayList<>();
		// 释放预占
		List<BizObject> preReleaseFundBillForFundPlanProjectList = new ArrayList<>();
		// 现金管理参数是否启用资金计划
		if (fundCommonService.checkFundPlanIsEnabledBySalarypay(ServiceNameEnum.SALARY_PAYMENT) && IBillNumConstant.SALARYPAY.equals(billnum)) {
//            Object controlSet = commonService.queryStrategySetbByCondition(salaryPay.get(ACCENTITY), salaryPay.get(CURRENCY), salaryPay.get("vouchdate"));
			// 2.资金计划项目额度校验
			List<Object> ids = new ArrayList<>();
			if (ValueUtils.isNotEmptyObj(salaryPay.get("fundPlanProject"))) {
				ids.add(salaryPay.get("fundPlanProject"));
			}
			if (CollectionUtils.isNotEmpty(ids)){
				Map<Object, Object> fundPlanProject = QueryBaseDocUtils.queryPlanStrategyControlMethodBySubId(ids);
				List<BizObject> checkFundBillForFundPlanProjectList = new ArrayList<>();

				// 1)进行额度校验。根据“计划项目编号”，在最小计划周期对应的策略里查询当前“计划项目编号”的控制方式，
				// 如果是监控，则计划校验接口无需任何输入，直接放行，薪资支付业务正常进行；
				// 如果是管控，则计划校验接口正常处理。（以下是管控的逻辑）
				short setControlMethod = ValueUtils.isNotEmptyObj(fundPlanProject.get(salaryPay.get("fundPlanProject")))
						? Short.parseShort(fundPlanProject.get(salaryPay.get("fundPlanProject")).toString()) : -1;
				if (setControlMethod == 1) {
					checkFundBillForFundPlanProjectList.add(salaryPay);
				}
				Map<String, Object> map = new HashMap<>();
				map.put("accentity", salaryPay.get("accentity"));
				map.put("vouchdate", salaryPay.get("vouchdate"));
				map.put("code", salaryPay.get("code"));
				List<CapitalPlanExecuteModel> checkObject5 = commonService.putCheckParameterSalarypay(checkFundBillForFundPlanProjectList, IStwbConstantForCmp.CHENK, billnum, map);
				if (ValueUtils.isNotEmptyObj(checkObject5)) {
					// 2)根据会计主体、计划项目编号、计划周期、单据日期、币种查询出符合条件的已生效和调整中的资金计划编制单。
					// 如果付款金额超出计划余额，输出给现金管理：“计划项目【XXX计划项目名称】的余额【YYY】，余额不足！”。
					try {
						RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).employAndrelease(checkObject5);
					} catch (Exception e) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101911"),e.getMessage());
					}
				}
			}

			if ("Update".equals(salaryPay.get("_status").toString())) {
				Object id = salaryPay.get("id");
				long fundPlanProjectPage = ValueUtils.isNotEmptyObj(salaryPay.get("fundPlanProject"))
						? Long.parseLong(salaryPay.get("fundPlanProject").toString()) : -1L;
				Salarypay salarypay_old = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, id, 1);
				long fundPlanProjectDB = ValueUtils.isNotEmptyObj(salarypay_old.getFundPlanProject()) ? salarypay_old.getFundPlanProject() : -1L;
				if (fundPlanProjectPage != fundPlanProjectDB) {
					if (fundPlanProjectPage != -1 && fundPlanProjectDB != -1) {
						salaryPay.put("isToPushCspl", 2);
						preReleaseFundBillForFundPlanProjectList.add(salarypay_old);
						preEmployFundBillForFundPlanProjectList.add(salaryPay);
					}
					if (fundPlanProjectPage == -1) {
						salaryPay.put("isToPushCspl", 0);
						preReleaseFundBillForFundPlanProjectList.add(salarypay_old);
					}
					if (fundPlanProjectPage != -1 && fundPlanProjectDB == -1) {
						salaryPay.put("isToPushCspl", 2);
						preEmployFundBillForFundPlanProjectList.add(salaryPay);
					}
				} else {
					BigDecimal oriSumPage = salaryPay.getOriSum();
					BigDecimal oriSumDb = salarypay_old.getOriSum();
					if (oriSumDb.compareTo(oriSumPage) != 0) {
						preReleaseFundBillForFundPlanProjectList.add(salarypay_old);
						preEmployFundBillForFundPlanProjectList.add(salaryPay);
					}
				}
			} else if ("Delete".equals(salaryPay.get("_status").toString())) {
				salaryPay.put("isToPushCspl", 0);
				preReleaseFundBillForFundPlanProjectList.add(salaryPay);
			}

			// 释放预占资金计划
			if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
				Map<String, Object> map2 = new HashMap<>();
				map2.put("accentity", salaryPay.get("accentity"));
				map2.put("vouchdate", salaryPay.get("vouchdate"));
				map2.put("code", salaryPay.get("code"));
				List<CapitalPlanExecuteModel> checkObject2 = commonService.putCheckParameterSalarypay(preReleaseFundBillForFundPlanProjectList, IStwbConstantForCmp.RELEASE, billnum, map2);
				if (ValueUtils.isNotEmptyObj(checkObject2)) {
					try {
						RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).preEmployAndrelease(checkObject2);
					} catch (Exception e) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101912"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508007C", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
					}
				}
			}
			// 预占用资金计划
			if (CollectionUtils.isNotEmpty(preEmployFundBillForFundPlanProjectList)) {
				Map<String, Object> map1 = new HashMap<>();
				map1.put("accentity", salaryPay.get("accentity"));
				map1.put("vouchdate", salaryPay.get("vouchdate"));
				map1.put("code", salaryPay.get("code"));
				List<CapitalPlanExecuteModel> checkObject1 = commonService.putCheckParameterSalarypay(preEmployFundBillForFundPlanProjectList, IStwbConstantForCmp.EMPLOY, billnum, map1);
				CapitalPlanExecuteResp capitalPlanExecuteResp;
				if (ValueUtils.isNotEmptyObj(checkObject1)) {
					try {
						capitalPlanExecuteResp = RemoteDubbo.get(CapitalPlanExecuteService.class, MDD_DOMAIN_CTMCSPL).preEmployAndrelease(checkObject1);
						if (ValueUtils.isNotEmptyObj(capitalPlanExecuteResp)
								&& "500".equals(capitalPlanExecuteResp.getCode())
								&& capitalPlanExecuteResp.getSuccessCount() == 0) {
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080089", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + capitalPlanExecuteResp.getMessage().toString());
						}
					} catch (Exception e) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101912"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508007C", "调用资金计划占用或释放接口失败：") /* "调用资金计划占用或释放接口失败：" */ + e.getMessage());
					}
				}
			}


		} else {
			if (IBillNumConstant.SALARYPAY.equals(billnum)) {
				Object id = salaryPay.get("id");
				Salarypay salarypay_old = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, id, 1);
				if (ValueUtils.isNotEmptyObj(salarypay_old.getFundPlanProject()) && salarypay_old.getIsToPushCspl().equals(1)) {
					preReleaseFundBillForFundPlanProjectList.add(salarypay_old);
					// 释放资金计划
					if (CollectionUtils.isNotEmpty(preReleaseFundBillForFundPlanProjectList)) {
						Map<String, Object> map2 = new HashMap<>();
						map2.put("accentity", salaryPay.get("accentity"));
						map2.put("vouchdate", salaryPay.get("vouchdate"));
						map2.put("code", salaryPay.get("code"));
						List<CapitalPlanExecuteModel> checkObject2 = commonService.putCheckParameterSalarypay(preReleaseFundBillForFundPlanProjectList, IStwbConstantForCmp.RELEASE, billnum, map2);
						if (ValueUtils.isNotEmptyObj(checkObject2)) {
							String csplDomain = MDD_DOMAIN_CTMCSPL;
							String result1 = RemoteDubbo.get(CapitalPlanExecuteService.class, csplDomain).release(checkObject2);
							if (result1 != null && !"".equals(result1)) {
								throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101913"),result1);
							}
//                            EntityTool.setUpdateStatus(releaseFundBillForFundPlanProjectList);
//                            if (IBillNumConstant.SALARYPAY.equals(billnum)) {
//                                MetaDaoHelper.update(Salarypay.ENTITY_NAME, releaseFundBillForFundPlanProjectList);
//                            }
						}
					}
				}
				salaryPay.setFundPlanProject(null);
				salaryPay.setIsToPushCspl(0);
			}
		}
	}
}
