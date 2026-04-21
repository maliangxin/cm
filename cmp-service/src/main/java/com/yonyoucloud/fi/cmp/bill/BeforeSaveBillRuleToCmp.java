package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyoucloud.fi.basecom.precision.CheckPrecision;
import com.yonyoucloud.fi.basecom.precision.CheckPrecisionVo;
import com.yonyoucloud.fi.basecom.precision.SetScalUtil;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.payapplicationbill.CloseStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill_b;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill_b;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.fi.cmp.util.business.BillCopyCheckService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialQryDTO;
import com.yonyoucloud.iuap.upc.dto.MerchantApplyRangeDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorOrgVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

@Slf4j
@Component
public class BeforeSaveBillRuleToCmp extends AbstractCommonRule {

	private static final String BANKRECONCILIATIONMAPPER = "com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper";

	private static final String SIGNCONST = "guan1226";
	private static final String ENTERPRISEBANKACCOUNT = "enterprisebankaccount";
	private static final String ENTERPRISEBANKACCOUNTS = "enterpriseBankAccount";

	@Autowired
	private SettlementService settlementService;
	@Autowired
	private CmCommonService cmCommonService;
	@Autowired
	private CtmSignatureService signatureService;
	@Autowired
	BaseRefRpcService baseRefRpcService;
	//@Autowired
//	OrgRpcService orgRpcService;
	@Autowired
	BankAccountSettingService bankaccountSettingService;
	@Autowired
	BillCopyCheckService billCopyCheckService;

	@Autowired
    VendorQueryService vendorQueryService;
	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		List<BizObject> bills = getBills(billContext, paramMap);
		BillDataDto billDataDto = (BillDataDto) getParam(paramMap);

        if (CmpCommonUtil.getNewFiFlag()) {
            if (IBillNumConstant.PAYMENT.equals(billContext.getBillnum())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103014"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C255F404A00006", "在财务新架构环境下，不允许保存付款单。") /* "在财务新架构环境下，不允许保存付款单。" */);
            }
            if (IBillNumConstant.RECEIVE_BILL.equals(billContext.getBillnum())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103029"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2618E04A0000D", "在财务新架构环境下，不允许保存收款单。") /* "在财务新架构环境下，不允许保存收款单。" */);
            }
        }

		if("copy".equals(bills.get(0).get("actionType"))){
			copyCheck( billContext,bills.get(0));
		}
		// 获取应收应付标识
		Boolean arapFlag;
		if(((BillDataDto)paramMap.get("param")).getPartParam() !=null){
			arapFlag= (Boolean)((BillDataDto)paramMap.get("param")).getPartParam().get("arapFlag");
			if(arapFlag != null && arapFlag){
				BizObject bizObject = bills.get(0);
				Date maxSettleDate = settlementService.getMaxSettleDate(bizObject.get(IBussinessConstant.ACCENTITY));
				if (maxSettleDate != null) {
					Date compareDate = bizObject.get("vouchdate");
					if (maxSettleDate.compareTo(compareDate) >= 0) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100454"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00162", "单据日期已日结，不能保存单据！") /* "单据日期已日结，不能保存单据！" */);
					}
					if (bizObject.get("dzdate") != null && maxSettleDate.compareTo(bizObject.get("dzdate")) >= 0) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100455"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00165", "登账日期已日结，不能保存单据！") /* "登账日期已日结，不能保存单据！" */);
					}
				}
				// 对于应收应付的单据，为了在收款工作台能够显示，需设置cmpflag为true
				bills.get(0).set("cmpflag",true);
				return new RuleExecuteResult();
			}
		}
		for (BizObject bizObject : bills) {
			String billnum = billContext.getBillnum();
			log.info("save data before rule. billNum = {}, id = {}, code = {}, bizObject = {}, paramMap = {}",
					billnum, bizObject.getId(), bizObject.get("code"), JsonUtils.toJson(bizObject), paramMap);
			if (StringUtils.isEmpty(billnum)) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100456"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418044E","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
			}
			if (billContext.getFullname().equals(ReceiveBill.ENTITY_NAME) && bizObject.get("billtype") == null) {//导入的数据，没有billtype，需要设置默认值
				bizObject.set("billtype", EventType.ReceiveBill.getValue());
			} else if (billContext.getFullname().equals(PayBill.ENTITY_NAME) && bizObject.get("billtype") == null) {
				bizObject.set("billtype", EventType.PayMent.getValue());
			}
			if (billContext.getFullname().equals(PayBill.ENTITY_NAME)){
				String enterpriseBankAccount = bizObject.get(ENTERPRISEBANKACCOUNT);
				if(!StringUtils.isEmpty(enterpriseBankAccount)){
					String data = bankaccountSettingService.getOpenFlag(enterpriseBankAccount);
					CtmJSONObject jsonObject = CtmJSONObject.parseObject(data);
					if(null != jsonObject){
						CtmJSONObject jsonData = jsonObject.getJSONObject("data");
						if(null != jsonData){
							bizObject.set("isdirectconn",jsonData.get("openFlag"));
						}
					}
				}
			}

			String childrenFields = MetaDaoHelper.getChilrenField(billContext.getFullname());
			List<BizObject> childs = (List) bizObject.get(childrenFields);
			if (ValueUtils.isNotEmptyObj(childs)) {
				for (BizObject child : childs) {
					//原币金额
					BigDecimal oriSum = child.get(IBussinessConstant.ORI_SUM);
					if(oriSum ==  null ){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100457"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180458","原币金额不能为空！") /* "原币金额不能为空！" */);
					}
					if(oriSum.doubleValue() == 0 && !bizObject.get("srcitem").equals(EventSource.SystemOut.getValue())){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100458"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180459","原币金额不能等于0！") /* "原币金额不能等于0！" */);
					}
					//本币金额
					BigDecimal natSum = child.get("natSum");
					if(natSum ==  null ){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100459"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418045A","本币金额不能为空！") /* "本币金额不能为空！" */);
					}
					if(natSum.doubleValue() == 0 && !bizObject.get("srcitem").equals(EventSource.SystemOut.getValue())){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100460"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418045B","本币金额不能等于0！") /* "本币金额不能等于0！" */);
					}
				}
			}
			boolean importFlag =  !"import".equals(billDataDto.getRequestAction());
			boolean openApiFlag = !bizObject.containsKey("_fromApi") || bizObject.get("_fromApi").equals(false);
			boolean fromapi = billDataDto.getFromApi();
			if (!importFlag || !openApiFlag || fromapi) {
				// openapi进来，单据日期格式化
				bizObject.set("vouchdate", DateUtils.dateTimeToDate(bizObject.get("vouchdate")));
				String childrenFieldCheck = MetaDaoHelper.getChilrenField(billContext.getFullname());
				List<BizObject> linesCheck = (List) bizObject.get(childrenFieldCheck);
				if (ValueUtils.isNotEmptyObj(bizObject.get("taxCategory")) && !ValueUtils.isNotEmptyObj(bizObject.get("taxSum"))) {
					String taxCategory = bizObject.get("taxCategory").toString();
					log.info("bill save by openApi, taxCategory = {}", taxCategory);
					BdTaxRateVO taxRate = cmCommonService.getTaxRateById(taxCategory);
					if (taxRate != null) {
						Double ntaxRate = taxRate.getNtaxRate().doubleValue();
						if (new BigDecimal(ntaxRate).compareTo(BigDecimal.ZERO) == 0) {
							bizObject.set("taxSum", BigDecimal.ZERO);
							bizObject.set("taxRate", BigDecimal.ZERO);
						} else {
							bizObject.set("taxRate", BigDecimalUtils.safeMultiply(ntaxRate, CONSTANT_ZERO_POINT_ONE));
							bizObject.set("taxSum", BigDecimalUtils.safeMultiply(BigDecimalUtils.safeMultiply(ntaxRate, CONSTANT_ZERO_POINT_ONE),
									bizObject.get(IBussinessConstant.NAT_SUM)));
						}
					}
				}
				if (ValueUtils.isNotEmptyObj(linesCheck) && bizObject.getEntityStatus().name().equals("Insert")) {
					BigDecimal oriSumLines = BigDecimal.ZERO;
					for (BizObject line : linesCheck) {
						//原币金额
						BigDecimal oriSum = line.get(IBussinessConstant.ORI_SUM);
						oriSumLines = BigDecimalUtils.safeAdd(oriSum, oriSumLines);
					}
					BigDecimal oriSumHead = bizObject.get(IBussinessConstant.ORI_SUM);
					if (ValueUtils.isNotEmptyObj(oriSumHead) && oriSumHead.compareTo(oriSumLines) !=0) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100461"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418046A","单据的表头金额与明细金额总和不相等！") /* "单据的表头金额与明细金额总和不相等！" */);
					}
				}
				if (ValueUtils.isNotEmptyObj(linesCheck) && bizObject.getEntityStatus().name().equals("Update")) {
					BizObject bizObj = MetaDaoHelper.findById(bizObject.getEntityName(), bizObject.getId());
					BigDecimal oriSumLines = new BigDecimal(bizObj.get(IBussinessConstant.ORI_SUM).toString());
					for (BizObject line : linesCheck) {
						//原币金额
						BigDecimal oriSum = new BigDecimal(line.get(IBussinessConstant.ORI_SUM).toString());
						if (line.getEntityStatus().name().equals("Insert")) {
							oriSumLines = BigDecimalUtils.safeAdd(oriSum, oriSumLines);
						} else if (line.getEntityStatus().name().equals("Update")) {
							if (!ValueUtils.isNotEmptyObj(line.getId())) {
								throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100462"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418046F","数据修改时，ID不能为空！") /* "数据修改时，ID不能为空！" */);
							}
							BizObject currentBill = MetaDaoHelper.findById(line.getEntityName(), line.getId());
							if (!ValueUtils.isNotEmptyObj(currentBill)) {
								throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100463"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180470","修改的数据不存在！") /* "修改的数据不存在！" */);
							}
							oriSumLines = BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(oriSum, new BigDecimal(currentBill.get(IBussinessConstant.ORI_SUM).toString())), oriSumLines);
						} else if (line.getEntityStatus().name().equals("Delete")) {
							oriSumLines = BigDecimalUtils.safeSubtract(oriSumLines, oriSum);
						}
					}
					BigDecimal oriSumHead = bizObject.get(IBussinessConstant.ORI_SUM);
					if ( (!ValueUtils.isNotEmptyObj(oriSumHead)) ||(ValueUtils.isNotEmptyObj(oriSumHead) && oriSumHead.compareTo(oriSumLines) !=0)) {
						bizObject.put(IBussinessConstant.ORI_SUM, oriSumLines);
					}
				}
			}

			// 2020/12/2 当为付款单拉取银行对账单时 对明细的付款金额合计和表头的付款金额进行是否相等校验
			if (PayBill.ENTITY_NAME.equals(billContext.getFullname()) && bizObject.get("billtype").toString().equals(EventType.CashMark.getValue() + "")) {
				checkOriSumEqualsByBankreconciliation(billContext, bizObject);
			}
			// 2020/12/6 当为付款单拉取付款申请单时 对明细的付款金额合计和表头的付款金额进行是否相等校验
			if (PayBill.ENTITY_NAME.equals(billContext.getFullname()) && bizObject.get("billtype").toString().equals(EventType.PayApplyBill.getValue() + "")) {
				checkOriSumEqualsByPayApplicationBill(billContext, bizObject);
				if (bizObject.getEntityStatus().name().equals("Update") && ! IBillNumConstant.PAYMENT_UPDATE.equals(billContext.getBillnum())) {
					List<BizObject> bizObjects = getBills(billContext, paramMap);
					BizObject object = bizObjects.get(0);
					BizObject currentBill = MetaDaoHelper.findById(object.getEntityName(), object.getId());
					String childrenField = MetaDaoHelper.getChilrenField(billContext.getFullname());
					List<BizObject> lines = currentBill.get(childrenField);
					Map<Long,BigDecimal> map_original = new HashMap();
					for (BizObject line : lines) {
						Long srcbillitemid = Long.valueOf(line.get(SRCBILLITEMID).toString());
						BigDecimal oriSum = line.get(ORISUM);
						map_original.put(srcbillitemid, oriSum);
					}
					paramMap.put("map_original", map_original);
				}
			}

			if (bizObject.getEntityStatus().name().equals("Insert")) {
				bizObject.put("creatorId", AppContext.getCurrentUser().getId());
			}

			if (FIDubboUtils.isSingleOrg()) {
				BizObject singleOrg = FIDubboUtils.getSingleOrg();
				if (singleOrg != null) {
					bizObject.set(IBussinessConstant.ACCENTITY, singleOrg.get("id"));
					bizObject.set("accentity_name", singleOrg.get("name"));
				}
			}
//			if(BillInfoUtils.getBusinessDate() != null) {
//				bizObject.set("vouchdate", BillInfoUtils.getBusinessDate());
//        	}
			//精度处理
			CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bizObject.get("currency"));
			bizObject.set("settlestatus",1);
			bizObject.set("cmpflag",true);
			String currency = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
			bizObject.set("natCurrency",currency);
			CurrencyTenantDTO currencyOrgDTO = baseRefRpcService.queryCurrencyById(currency);

			if (!importFlag || !openApiFlag) {
                doImportCheck(billContext,bizObject);
			}

			if(bizObject.get("srcitem").equals(EventSource.SystemOut.getValue())&& bizObject.getEntityStatus().name().equals("Insert")){
				//费用推来的单据
				String childrenField = MetaDaoHelper.getChilrenField(billContext.getFullname());
				List<BizObject> lines = bizObject.get(childrenField);
				if(lines == null || lines.size() == 0){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100464"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418042A","表体数据为空！不允许保存！") /* "表体数据为空！不允许保存！" */);
				}
				for (BizObject  child :lines){
					Map<String, Object> condition  = new HashMap<String, Object>();
					condition.put("code",child.get("quickType"));
					Map<String, Object>  quickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition).get(0);/* 暂不修改 已登记*/
					child.set("quickType",quickType.get("id"));
					child.set("quickType_code",quickType.get("code"));
					child.set("quickType_name",quickType.get("name"));
				}
				bizObject.set(childrenField,lines);
			}
			//添加商业汇票推单处理 0911
			if(!bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && !bizObject.get("srcitem").equals(EventSource.Drftchase.getValue()) && bizObject.getEntityStatus().name().equals("Insert")){
				if(!bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue()))){
					bizObject.set("srcitem",EventSource.Cmpchase.getValue());
				}
			}
			// 如果是批改保存，则不需要校验是否已审核 ：!StringUtil.isNotEmpty(bizObject.get("batchModifyFlag"))
			if(!StringUtil.isNotEmpty(bizObject.get(BATCH_MODIFY_FLAG)) && !IBillNumConstant.RECEIVE_BILL_UPDATE.equals(billnum) && !IBillNumConstant.PAYMENT_UPDATE.equals(billnum) && !IBillNumConstant.PAYMENT_SETTLE.equals(billnum)
					&& bizObject.getEntityStatus().name().equals("Update") && bizObject.get("auditstatus").equals(AuditStatus.Complete.getValue())){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100465"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180431","该单据已审批，不能进行修改！") /* "该单据已审批，不能进行修改！" */);
			}
			//添加商业汇票推单处理 0911
			if(!StringUtil.isNotEmpty(bizObject.get(BATCH_MODIFY_FLAG)) && !IBillNumConstant.RECEIVE_BILL_UPDATE.equals(billnum) && !IBillNumConstant.PAYMENT_UPDATE.equals(billnum) && !IBillNumConstant.PAYMENT_SETTLE.equals(billnum)
					&& !bizObject.get("srcitem").equals(EventSource.Cmpchase.getValue()) && !bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && !bizObject.get("srcitem").equals(EventSource.Drftchase.getValue())){
				if(!bizObject.get("billtype").toString().equals(String.valueOf(EventType.CashMark.getValue()))){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100466"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180434","该单据不是来源现金自制单据，不能进行修改！") /* "该单据不是来源现金自制单据，不能进行修改！" */);
				}
			}
			if(!bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.get("settlemode") == null){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100467"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180436","结算方式不能为空！") /* "结算方式不能为空！" */);
			}
			//若单据为费用管理 则赋值默认结算方式、默认银行账户 yangjn 2021/03/31
			if(PayBill.ENTITY_NAME.equals(billContext.getFullname()) &&  bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.getEntityStatus().name().equals("Insert")) {
				//赋值默认银行账号
				if(bizObject.get("enterprisebankaccount")==null) {
					Map<String, Object> defaultBankAccount = cmCommonService.getDefaultBankAccountByOrgId(bizObject.get(IBussinessConstant.ACCENTITY), bizObject.get("currency"));
					if(defaultBankAccount!=null) {
						bizObject.set("enterprisebankaccount", defaultBankAccount.get("id"));
					}
					//如果设置了会计主体默认账户 则给结算方式赋值 否则不赋值
					if(defaultBankAccount!=null && defaultBankAccount.get("id")!=null) {
						//查询现金参数 赋值默认结算方式
						Map<String, Object> autoConfig = cmCommonService.queryAutoConfigByAccentity(bizObject.get(IBussinessConstant.ACCENTITY));
						if(autoConfig!=null && autoConfig.get("settlemode")!=null) {
							bizObject.set("settlemode", autoConfig.get("settlemode"));
						}
					}
				}
			}
			if(bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.getEntityStatus().name().equals("Insert")){
				CmpCommonUtil.checkPayBillExist(ICmpConstant.SRCBILLID, bizObject.get("srcbillid").toString(), bizObject.getEntityName());
				CmpCommonUtil.checkBankAcctCurrency(bizObject.get("enterprisebankaccount"), bizObject.get("currency"));
			}
			//结算方式
			if (bizObject.get("settlemode") != null){
				SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
				settleMethodQueryParam.setId(bizObject.get("settlemode"));
				settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
				settleMethodQueryParam.setTenantId(AppContext.getTenantId());
				List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
				//结算方式
				if(dataList != null && dataList.size() > 0){
					if(dataList.get(0).getServiceAttr().equals(0)){
						if(bizObject.get("enterprisebankaccount") == null){
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100468"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180440","结算方式业务属性为银行业务，请录入银行账户！") /* "结算方式业务属性为银行业务，请录入银行账户！" */);
						}
					} else if (dataList.get(0).getServiceAttr().equals(1)) {
						if(bizObject.get("cashaccount") == null){
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100469"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180441","结算方式业务属性为现金业务，请录入现金账户！") /* "结算方式业务属性为现金业务，请录入现金账户！" */);
						}
					}else{
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100470"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180442","结算方式业务属性只能为银行业务与现金业务！") /* "结算方式业务属性只能为银行业务与现金业务！" */);
					}
				}
			}
			if (billContext.getBillnum().equals(IBillNumConstant.PAYMENT)) {
				if (bizObject.get("srcitem").equals(EventSource.SystemOut.getValue())) {
					String originalMsg = DigitalSignatureUtils.getOriginalMsg(bizObject);
					if (!StringUtil.isNotEmpty(bizObject.get(BATCH_MODIFY_FLAG)) && !signatureService.iTrusVerifySignature(originalMsg, bizObject.get("signature"))) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100471"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180435", "数据签名验签失败！") /* "数据签名验签失败！" */);
					}
				}
			}
			if(bizObject.get("caobject") == null){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100472"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180444","收付款对象类型不能为空！") /* "收付款对象类型不能为空！" */);
			}else{
				if(bizObject.get("caobject").equals(CaObject.Supplier.getValue())){
					if(bizObject.get("supplier") == null){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100473"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180446","收付款对象类型为供应商，供应商不能为空！") /* "收付款对象类型为供应商，供应商不能为空！" */);
					}
					/*if(!bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.get("supplierbankaccount") == null && "arap.paybill.PayBill".equals(billContext.getFullname())){
						if(serviceAttr.equals(0)){
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100474"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026300") *//* "收付款对象类型为供应商，供应商银行账户不能为空！" *//*);
						}
					}*/
					bizObject.set(IBussinessConstant.CUSTOMER,null);
					bizObject.set("customerbankaccount",null);
					bizObject.set("employee",null);

					//region CZFW-51716 根据客户供应商员工带出默认收款银行账户信息 20220809 add by wanxb
					if (bizObject.get("supplierbankaccount") == null){
						Object supplierId = bizObject.get("supplier");
						Map<String, Object> condition = new HashMap<>();
						condition.put("vendor", supplierId);
						condition.put("stopstatus", "0");
						condition.put("defaultbank", true);
						if (bizObject.get("currency")!= null) {
							condition.put("currency", bizObject.get("currency"));
						}
						List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(condition);
						if (bankAccounts.size() > 0) {
							bizObject.set("supplierbankaccount", bankAccounts.get(0).getId());// 供应商银行账户id
							bizObject.set("supplierbankaccount_account", bankAccounts.get(0).getAccount());// 供应商银行账号
							bizObject.set("supplierbankaccount_accountname", bankAccounts.get(0).getAccountname());// 供应商银行账户名称
							// 查询开户行
							BankdotVO  depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenaccountbank());
							if (depositBank != null) {
								bizObject.set("supplierbankaccount_openaccountbank_name", depositBank.getName()); // 供应商账户银行网点
							} else {
								bizObject.set("supplierbankaccount_openaccountbank_name", null); // 供应商账户银行网点
							}
						} else {
							bizObject.set("supplierbankaccount", null);// 供应商银行账户id
							bizObject.set("supplierbankaccount_account", null);// 供应商银行账号
							bizObject.set("supplierbankaccount_accountname", null);// 供应商银行账户名称
						}
					}
					//endregion
				}
				if(bizObject.get("caobject").equals(CaObject.Customer.getValue())){
					if(bizObject.get(IBussinessConstant.CUSTOMER) == null){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100475"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00161", "收付款对象类型为客户，客户不能为空！") /* "收付款对象类型为客户，客户不能为空！" */);
					}
					/*if(!bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.get("customerbankaccount") == null && "arap.paybill.PayBill".equals(billContext.getFullname())){
						if(serviceAttr.equals(0)){
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100476"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026032") *//* "收付款对象类型为客户，客户银行账户不能为空！" *//*);
						}
					}*/
					bizObject.set("supplier",null);
					bizObject.set("supplierbankaccount",null);
					bizObject.set("employee",null);

					//region CZFW-51716 根据客户供应商员工带出默认收款银行账户信息 20220809 add by wanxb
					if (bizObject.get("customerbankaccount") == null){
						Object customerId = bizObject.get(IBussinessConstant.CUSTOMER);
						if (customerId != null){
							AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
							agentFinancialQryDTO.setMerchantId(Long.valueOf(customerId.toString()));
							agentFinancialQryDTO.setStopStatus(Boolean.FALSE);
							agentFinancialQryDTO.setIfDefault(Boolean.TRUE);
							if (bizObject.get("currency") != null) {
								agentFinancialQryDTO.setCurrency(bizObject.getString("currency"));
							}
							List<AgentFinancialDTO> bankAccounts = QueryBaseDocUtils.queryCustomerBankAccountByCondition(agentFinancialQryDTO);
							if (bankAccounts != null && bankAccounts.size() > 0) {
								bizObject.set("customerbankaccount", bankAccounts.get(0).getId());// 客户银行账户id
								bizObject.set("customerbankaccount_bankAccount", bankAccounts.get(0).getBankAccount()); // 客户银行账号
								bizObject.set("customerbankaccount_bankAccountName", bankAccounts.get(0).getBankAccountName()); // 客户银行账户名称
								// 查询开户行
								if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).getOpenBank())){
                                    BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenBank());
                                    if (depositBank != null) {
                                        bizObject.set("customerbankaccount_openBank_name", depositBank.getName()); // 客户账户银行网点
                                    } else {
                                        bizObject.set("customerbankaccount_openBank_name", null); // 客户账户银行网点
                                    }
								}
							} else {
								bizObject.set("customerbankaccount", null);// 客户银行账户id
								bizObject.set("customerbankaccount_bankAccount", null); // 客户银行账号
								bizObject.set("customerbankaccount_bankAccountName", null); // 客户银行账户名称
							}
						}
					}
					//endregion
				}
				if(bizObject.get("caobject").equals(CaObject.Employee.getValue())){
					if(bizObject.get("employee") == null){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100477"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00169", "收付款对象类型为员工，员工不能为空！") /* "收付款对象类型为员工，员工不能为空！" */);
					}
					/*if(!bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.get("staffBankAccount") == null  && "arap.paybill.PayBill".equals(billContext.getFullname())){
						if(serviceAttr.equals(0)){
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100478"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026027") *//* "收付款对象类型为员工，员工银行账户不能为空！" *//*);
						}
					}*/
					bizObject.set(IBussinessConstant.CUSTOMER,null);
					bizObject.set("customerbankaccount",null);
					bizObject.set("supplier",null);
					bizObject.set("supplierbankaccount",null);

					//region CZFW-51716 根据客户供应商员工带出默认收款银行账户信息 20220809 add by wanxb
					if (bizObject.get("staffBankAccount") == null){
						Object employeeId = bizObject.get("employee");
						Map<String, Object> condition = new HashMap<>();
						condition.put("staff_id", employeeId);
						condition.put("isdefault", 1);
						condition.put("dr", 0);
						if ( bizObject.get("currency") != null) {
							condition.put("currency",  bizObject.get("currency"));
						}
						List<Map<String, Object>> bankAccounts = QueryBaseDocUtils.queryStaffBankAccountByCondition(condition);/* 暂不修改 已登记*/
						if (bankAccounts.size() > 0) {
							bizObject.set("staffBankAccount", bankAccounts.get(0).get("id"));// 员工银行账户名称
							bizObject.set("staffBankAccount_accountno", bankAccounts.get(0).get("account"));// 员工银行账户名称
							bizObject.set("staffBankAccount_account", bankAccounts.get(0).get("account"));// 员工银行账户名称
							BankdotVO  depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).get("bankname").toString());
							if (depositBank != null) {
								bizObject.set("staffBankAccount_bankname_name", depositBank.getName());// 员工账户银行网点
							} else {
								bizObject.set("staffBankAccount_bankname_name", null);// 员工账户银行网点
							}
						} else {
							bizObject.set("staffBankAccount", null);// 员工银行账户名称
							bizObject.set("staffBankAccount_accountno", null);// 员工银行账户名称
							bizObject.set("staffBankAccount_account", null);// 员工银行账户名称
						}
					}
					//endregion
				}

				if(bizObject.get("caobject").equals(CaObject.Other.getValue())) {
					bizObject.set(IBussinessConstant.CUSTOMER,null);
					bizObject.set("customerbankaccount",null);
					bizObject.set("supplier",null);
					bizObject.set("supplierbankaccount",null);
					bizObject.set("employee",null);
					bizObject.set("staffbankaccount",null);
				}

			}
			if(bizObject.get("supplier") != null && bizObject.get(IBussinessConstant.CUSTOMER) != null){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100479"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418041E","供应商与客户不能同时存在！") /* "供应商与客户不能同时存在！" */);
			}
			if(bizObject.get("supplier") != null && bizObject.get("employee") != null){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100480"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180421","供应商与员工不能同时存在！") /* "供应商与员工不能同时存在！" */);
			}
			if(bizObject.get(IBussinessConstant.CUSTOMER) != null && bizObject.get("employee") != null){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100481"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180422","客户与员工不能同时存在！") /* "客户与员工不能同时存在！" */);
			}
			if(bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.getEntityStatus().name().equals("Insert")){
				//外系统来源数据第一次不做校验
			}else{
				if(bizObject.get("enterprisebankaccount") != null && bizObject.get("cashaccount") != null){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100482"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180424","银行账户与现金账户不能同时存在！") /* "银行账户与现金账户不能同时存在！" */);
				}
				if(bizObject.get("enterprisebankaccount") == null && bizObject.get("cashaccount") == null){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100483"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180425","银行账户与现金账户必须录入其中一个！") /* "银行账户与现金账户必须录入其中一个！" */);
				}
			}
			if(bizObject.get("vouchdate") == null){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100484"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180426","单据日期不能为空！") /* "单据日期不能为空！" */);
			}
			//校验模块启用日期
			Date enabledBeginData = QueryBaseDocUtils.queryOrgPeriodBeginDate(bizObject.get(IBussinessConstant.ACCENTITY));/* 暂不修改 已登记*/
			if (enabledBeginData != null) {
				if (enabledBeginData.compareTo(bizObject.get("vouchdate")) > 0) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100485"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180428","单据日期早于模块启用日期，不能保存单据！") /* "单据日期早于模块启用日期，不能保存单据！" */);
				}
				if (bizObject.get("dzdate") != null && enabledBeginData.compareTo(bizObject.get("dzdate")) > 0) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100486"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180429","登账日期早于模块启用日期，不能保存单据！") /* "登账日期早于模块启用日期，不能保存单据！" */);
				}
			}
			if (IBillNumConstant.PAYMENT_UPDATE.equals(billnum)) {
				paymentBillUpdateProcess(bizObject);
			}
			//校验日结日期
			//修改逻辑：结算变更时，不校验日结 majfd
			if(!IBillNumConstant.RECEIVE_BILL_UPDATE.equals(billnum) && !IBillNumConstant.PAYMENT_SETTLE.equals(billnum)) {
				Date compareDate = bizObject.get("vouchdate");
				if(IBillNumConstant.PAYMENT_UPDATE.equals(billnum) && bizObject.get("paystatus") != null && PayStatus.Success.getValue() == bizObject.getShort("paystatus")){
					//付款工作台支付变更时，和系统日期比较，因为支付变更为支付成功，赋值的登账日期为系统日期
					compareDate = DateUtils.dateTimeToDate(new Date());
				}else if(IBillNumConstant.PAYMENT_UPDATE.equals(billnum) && bizObject.get("paystatus") != null && PayStatus.Fail.getValue() == bizObject.getShort("paystatus")){
					//支付变更失败，不校验日结
					compareDate = null;
				}
				checkSettleData(bizObject, compareDate);
			}
			//校验签名
			if (importFlag && openApiFlag) {
				if(bizObject.get("srcitem").equals(com.yonyoucloud.fi.cmp.cmpentity.EventSource.Cmpchase.getValue())&&PayBill.ENTITY_NAME.equals(billContext.getFullname())){
					checkSign(bizObject);
				}
			}
			//新增金额校验
			if(!bizObject.get("srcitem").equals(EventSource.SystemOut.getValue())){
				doInsertMoneyCheck(billContext, bizObject,currencyDTO,currencyOrgDTO);
			}
			if(((BigDecimal)bizObject.get(IBussinessConstant.ORI_SUM)).compareTo(BigDecimal.ZERO) <= 0){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100487"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180432","金额必须大于0！") /* "金额必须大于0！" */);
			}
			bizObject.set("balance",bizObject.get(IBussinessConstant.ORI_SUM));
			bizObject.set("localbalance",bizObject.get(IBussinessConstant.NAT_SUM));
			if (billContext.getBillnum().equals(IBillNumConstant.PAYMENT)) {
				String originalMsg = DigitalSignatureUtils.getOriginalMsg(bizObject);
				if(bizObject.get("srcitem").equals(EventSource.SystemOut.getValue())){
					if (!StringUtils.isEmpty(bizObject.get("dept"))||!StringUtils.isEmpty(bizObject.get("project"))||!StringUtils.isEmpty(bizObject.get("expenseitem"))){
						if(bizObject.get("PayBillb")!=null){
							List<PayBillb> sublist = bizObject.get("PayBillb");
							for(PayBillb sub:sublist){
								if (sub.get("dept")==null) {
									sub.setDept(bizObject.get("dept"));
								}
								if (sub.get("project")==null) {
									sub.setProject(bizObject.get("project"));
								}
								if (sub.get("expenseitem")==null) {
									sub.setExpenseitem(bizObject.get("expenseitem"));
								}
							}

						}
					}
					if(bizObject.get("exchangeRateType")==null){
						ExchangeRateTypeVO rateType = baseRefRpcService.queryExchangeRateRateTypeByCode("02");
						bizObject.set("exchangeRateType", rateType.getId());
					}
				}else {
				  //对关键数据项进行数据签名
				  bizObject.set("signature", signatureService.iTrusSignMessage(originalMsg));
				}
			}

			//结算变更时，从库里面查询最新的值
			if(IBillNumConstant.RECEIVE_BILL_UPDATE.equals(billnum) || IBillNumConstant.PAYMENT_SETTLE.equals(billnum)) {
				BizObject oldObject = MetaDaoHelper.findById(billContext.getFullname(), bizObject.get("id"),3);
				bizObject.set("billtype",oldObject.get("billtype"));
			}
			if(ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())){
				if(bizObject.get("billtype") == null){
					bizObject.set("billtype",7);
				}
				if( bizObject.get("billtype").toString().equals(EventType.CashMark.getValue()+"")){
					checkMoney( bizObject,billContext,paramMap);
				}
			}else if (PayBill.ENTITY_NAME.equals(billContext.getFullname())){
				if(bizObject.get("billtype") == null){
					bizObject.set("billtype",10);
				}
				if( bizObject.get("billtype").toString().equals(EventType.CashMark.getValue()+"")){
					checkMoney( bizObject,billContext,paramMap);
				}
				if(bizObject.get("paystatus") == null){
					bizObject.set("paystatus", PayStatus.NoPay.getValue());
				}
			}
		}
		return new RuleExecuteResult();

	}

	/**
	 * 校验日结
	 */
	private void checkSettleData(BizObject bizObject, Date compareDate) throws Exception{
		if(compareDate == null){
			return;
		}
		Date maxSettleDate = settlementService.getMaxSettleDate(bizObject.get(IBussinessConstant.ACCENTITY));
		if (maxSettleDate != null) {
			if (maxSettleDate.compareTo(compareDate) >= 0) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100488"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180445","单据日期已日结，不能保存单据！") /* "单据日期已日结，不能保存单据！" */);
			}
			if (bizObject.get("dzdate") != null && maxSettleDate.compareTo(bizObject.get("dzdate")) >= 0) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100489"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180447","登账日期已日结，不能保存单据！") /* "登账日期已日结，不能保存单据！" */);
			}
		}
	}

	/**
	 * 付款工作台支付变更处理逻辑
	 * @param bizObject
	 */
	private void paymentBillUpdateProcess(BizObject bizObject) {
		bizObject.set("payupdateman",AppContext.getCurrentUser().getId());
		bizObject.set("payupdatedate",new Date());
		if (PayStatus.Success.getValue() == bizObject.getShort("paystatus")) {
			bizObject.set("settleuser",AppContext.getCurrentUser().getName());
			bizObject.set("settledate",new Date());
			bizObject.set("dzdate",new Date());
			bizObject.set("settlestatus",SettleStatus.alreadySettled.getValue());
		}
	}

	/**
	 * 整单复制功能跳转过来的保存请求，做字段启用性校验
	 * @param bizObject
	 * @throws Exception
	 */
	private void copyCheck(BillContext billContext,BizObject bizObject) throws Exception{
		//整单复制功能过来的保存，需进行字段的启用有效性校验 by lidchn@yonyou.com
		Map<String,Integer> cacheMap = new HashMap<>();
		if("copy".equals(bizObject.get("actionType"))){
			String billnum = billContext.getBillnum();
			if (org.apache.commons.lang3.StringUtils.isEmpty(billnum)) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100456"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418044E","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
			}
			Long startTime = System.currentTimeMillis();
			log.info("整单复制功能过来的保存，需进行字段的启用有效性校验！");
			//会计主体-必输
			if (bizObject.get(IBussinessConstant.ACCENTITY)!=null) {
				List<Map<String, Object>> accentityList = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(bizObject.get(IBussinessConstant.ACCENTITY));/* 暂不修改 已登记*/
//				FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
				if (!CollectionUtils.isEmpty(accentityList)) {
					Object accEntityFlag = accentityList.get(0).get("stopstatus");
					if (accEntityFlag!=null && "1".equals(accEntityFlag.toString())){//0是启用，1是未启用
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100490"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418045F","会计主体未启用，保存失败！"));
					}
				}else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100491"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180460","未查询到对应的会计主体，保存失败！"));
				}
			}
			//交易类型-必输
			if (bizObject.get("tradetype")!=null) {
				BdTransType bdTransType= baseRefRpcService.queryTransTypeById(bizObject.get("tradetype"));
				if (bdTransType!=null) {
					Object tradeTypeFlag = bdTransType.getEnable();
					if (tradeTypeFlag!=null && "2".equals(tradeTypeFlag.toString())){//1是启用，2是未启用
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100492"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180462","交易类型未启用，保存失败！") /* "交易类型未启用，保存失败！" */);
					}
				}else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100493"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180463","未查询到对应的交易类型，保存失败！") /* "未查询到对应的交易类型，保存失败！" */);
				}
			}
			//结算方式-必输
			if (bizObject.get("settlemode")!=null) {
				SettleMethodModel settleModeMap = baseRefRpcService.querySettleMethodsById(bizObject.get("settlemode").toString());
				if (settleModeMap!=null) {
					Object settleModeFlag = settleModeMap.getIsEnabled();
					if (settleModeFlag!=null && !(boolean)settleModeFlag){//true是启用，false是未启用
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100494"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180467","结算方式未启用，保存失败！") /* "结算方式未启用，保存失败！" */);
					}
				}else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100495"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180469","未查询到对应的结算方式，保存失败！") /* "未查询到对应的结算方式，保存失败！" */);
				}
			}
			//汇率类型-必输
			if (bizObject.get("exchangeRateType")!=null) {
				ExchangeRateTypeVO exchangeRateTypeVO = baseRefRpcService.queryExchangeRateTypeById(bizObject.getString("exchangeRateType"));
				if (exchangeRateTypeVO != null) {
					String exchangeRateTypeFlag = exchangeRateTypeVO.getEnable();
					if (exchangeRateTypeFlag!=null && "2".equals(exchangeRateTypeFlag)){//1是启用
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100496"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418046C","汇率类型未启用，保存失败！") /* "汇率类型未启用，保存失败！" */);
					}
				}else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100497"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418046D","未查询到对应的汇率类型，保存失败！") /* "未查询到对应的汇率类型，保存失败！" */);
				}
			}
			//币种-必输
			if (bizObject.get("currency")!=null) {
				CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(bizObject.get("currency"));
				if (currencyTenantDTO!=null) {
					Object currencyFlag = currencyTenantDTO.getEnable();
					if (currencyFlag!=null && "2".equals(currencyFlag.toString())){//1是启用，2是未启用
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100498"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180471","币种未启用，保存失败！") /* "币种未启用，保存失败！" */);
					}
				}else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100499"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180474","未查询到对应的币种，保存失败！") /* "未查询到对应的币种，保存失败！" */);
				}
			}
			//企业银行账户-非必输
			if (bizObject.get("enterprisebankaccount")!=null) {
				EnterpriseBankAcctVO enterpriseBankAcctVO= baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("enterprisebankaccount"));
				if (enterpriseBankAcctVO!=null) {
					Object enterpriseBankAccountFlag = enterpriseBankAcctVO.getEnable();
					if (enterpriseBankAccountFlag!=null && "2".equals(enterpriseBankAccountFlag.toString())){//1是启用，2是未启用
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100500"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180476","企业银行账户未启用，保存失败！") /* "企业银行账户未启用，保存失败！" */);
					}
				}else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100501"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180478","未查询到对应的企业银行账户，保存失败！") /* "未查询到对应的企业银行账户，保存失败！" */);
				}
			}
			//现金账户-非必输
			if (bizObject.get("cashaccount")!=null) {
				EnterpriseParams enterpriseParams = new EnterpriseParams();
				enterpriseParams.setId(bizObject.get("cashaccount"));
				EnterpriseCashVO cashBankAccount = baseRefRpcService.queryEnterpriseCashAcctByCondition(enterpriseParams).get(0);
				if (cashBankAccount!=null ) {
					Object cashAccountFlag = cashBankAccount.getEnable();
					if (cashAccountFlag!=null && "0".equals(cashAccountFlag.toString())){//1是启用，0是未启用
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100502"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418041C","现金账户未启用，保存失败！") /* "现金账户未启用，保存失败！" */);
					}
				}else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100503"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180420","未查询到对应的现金账户，保存失败！") /* "未查询到对应的现金账户，保存失败！" */);
				}
			}
			//业务组织（同会计主体）
			//收款客户-非必输
			if (bizObject.get(IBussinessConstant.CUSTOMER)!=null) {
				billCopyCheckService.checkCustomerByid(bizObject.get(IBussinessConstant.CUSTOMER), bizObject.get(IBussinessConstant.ACCENTITY) ,cacheMap);
			}
			if (bizObject.get("customerbankaccount")!=null) {
				billCopyCheckService.checkCustomerbankaccount(bizObject);
			}
			//供应商-非必输
			if (bizObject.get("supplier")!=null) {
				billCopyCheckService.checkSupplier(bizObject.get("supplier"),bizObject.get(IBussinessConstant.ACCENTITY), cacheMap);
//				Map<String, Object> supplierMap = QueryBaseDocUtils.querySupplierById(bizObject.get("supplier"));
//				if (supplierMap!=null && supplierMap.size()>0) {
//					Object supplierFlag =  supplierMap.get("freezestatus");
//					if (supplierFlag!=null && (boolean)supplierFlag){//false是启用，true是未启用
//						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100504"),MessageUtils.getMessage("P_YS_FI_CM_0001263405") /* "供应商未启用，保存失败！" */);
//					}
//				}else {
//					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100505"),MessageUtils.getMessage("P_YS_FI_CM_0001263402") /* "未查询到对应的供应商，保存失败！" */);
//				}
			}
			//部门-非必输
			if (bizObject.get("dept") != null) {
				billCopyCheckService.checkDept(bizObject, cacheMap);
			}
			//员工？？？？员工没有启用状态？？？？，先不进行校验
			//业务员？？？？业务员没有启用状态？？？？，先不进行校验
			//项目-非必输
			if (bizObject.get("project") != null) {
				ProjectDTO projectDTO = baseRefRpcService.queryProjectById(bizObject.get("project").toString());
				if (projectDTO!=null) {
					Object projectFlag = projectDTO.getEnable();
					if (projectFlag!=null && "2".equals(projectFlag.toString())){//1是启用，2是未启用
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100506"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418042B","项目未启用，保存失败！") /* "项目未启用，保存失败！" */);
					}
				}else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100507"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418042D","未查询到对应的项目，保存失败！") /* "未查询到对应的项目，保存失败！" */);
				}
			}
			//费用项目-非必输
			if (bizObject.get("expenseitem") != null) {
				List<Map<String, Object>> expenseItemList = QueryBaseDocUtils.queryExpenseItemById(bizObject.get("expenseitem"));/* 暂不修改 已登记*/
				if (!CollectionUtils.isEmpty(expenseItemList)) {
					Object expenseItemFlag = expenseItemList.get(0).get("enabled");
					if (expenseItemFlag!=null && !(boolean)expenseItemFlag){//true是启用，false是未启用
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100508"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418042F","费用项目未启用，保存失败！") /* "费用项目未启用，保存失败！" */);
					}
//					//判断是否勾选财资服务
//					Object propertyBusinessFlag = expenseItemList.get(0).get("propertybusiness");
//					if (propertyBusinessFlag != null && !propertyBusinessFlag.equals("1")){ // 1为勾选，其他为没勾选
//						throw new com.yonyou.ucf.mdd.ext.exceptions.BusinessException(MessageUtils.getMessage("P_YS_CTM_CM-BE_1613096893618323459") /* "所选费用项目中存在未启用财资业务领域的情况，保存失败！" */);
//					}
				}else {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100509"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180430","未查询到对应的费用项目，保存失败！") /* "未查询到对应的费用项目，保存失败！" */);
				}
			}

			if(IBillNumConstant.RECEIVE_BILL.equals(billnum)){
				List<ReceiveBill_b> billbs = bizObject.getBizObjects("ReceiveBill_b",ReceiveBill_b.class);
				for (ReceiveBill_b receiveBillB: billbs){
					billCopyCheckService.checkQuickType(receiveBillB, cacheMap);
					billCopyCheckService.checkExpenseitem(receiveBillB, cacheMap);
				}
			}else if(IBillNumConstant.PAYMENT.equals(billnum)){
				List<PayBillb> billbs = bizObject.getBizObjects("PayBillb",PayBillb.class);
				for (PayBillb payBillb: billbs){
					billCopyCheckService.checkQuickType(payBillb, cacheMap);
					billCopyCheckService.checkExpenseitem(payBillb, cacheMap);
				}
			}
			Long endTime = System.currentTimeMillis();
			log.info("复制单据字段启用性校验耗时： "+(endTime-startTime)+" 毫秒！！！！！！");

			//对于复制的单据，清空一些业务字段
			bizObject.put("paydate", null);
			bizObject.put("settledate", null);
			bizObject.put("dzdate", null);
			bizObject.put("auditstatus", AuditStatus.Incomplete.getValue());
			bizObject.put("paystatus", PayStatus.NoPay.getValue());
		}
		cacheMap.clear();
	}

	// 校验签名
	private void checkSign(BizObject bizObject) throws Exception{
		//自动化测试，不进行验签处理
		if(StringUtil.isNotEmpty(bizObject.get("signaturestr")) && ICmpConstant.AUTOTEST_SIGNATURE_Constant.equalsIgnoreCase(bizObject.get("signaturestr").toString())) {
			return;
		}
		// 页面数据进行抓包验证
		String enterprisebankaccount = bizObject.get("enterprisebankaccount");
		BigDecimal oriSum = bizObject.get(IBussinessConstant.ORI_SUM);
		BigDecimal natSum= bizObject.get(IBussinessConstant.NAT_SUM);
		String currencySign = bizObject.get("currency");
		String receivingBank = null;
		Short caObject = bizObject.get("caobject");
		if(caObject.equals(CaObject.Supplier.getValue())){
			receivingBank = bizObject.get("supplier").toString();
		}else if(caObject.equals(CaObject.Customer.getValue())){
			receivingBank = bizObject.get(IBussinessConstant.CUSTOMER).toString();
		}else if(caObject.equals(CaObject.Employee.getValue())){
			receivingBank = bizObject.get("employee").toString();
		}else{
			if(bizObject.get("retailerAccountName")!=null&&bizObject.get("retailerAccountNo")!=null){
				receivingBank = bizObject.get("retailerAccountName");
				receivingBank +=bizObject.get("retailerAccountNo");
			}
		}
		StringBuilder signStr=new StringBuilder(currencySign);          //币种
		if(bizObject.get("PayBillb")!=null){   // 插入验证明细
			List<PayBillb> sublist = bizObject.get("PayBillb");
			for(PayBillb sub:sublist){
				BigDecimal subNatSum=sub.get("natSum");
				BigDecimal subOriSum=sub.get(IBussinessConstant.ORI_SUM);
				String subNatSumNoZero=subNatSum.stripTrailingZeros().toPlainString();
				String subOriSumNoZero=subOriSum.stripTrailingZeros().toPlainString();
				signStr.append(subOriSumNoZero).append(subNatSumNoZero);    // 金额
			}
		}
		if(enterprisebankaccount!=null){
			signStr.append(enterprisebankaccount);     // 付款账号
		}
		if(receivingBank!=null){
			signStr.append(receivingBank);             // 收款账号
		}
		signStr.append(SIGNCONST);                     // 加密串
		String md5Str = SHA512Util.getSHA512Str(signStr.toString());
		if(!md5Str.equalsIgnoreCase(bizObject.get("signaturestr")))
		{
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100510"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00166", "单据【") /* "单据【" */ + bizObject.get("code") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00167", "】数据签名验证失败") /* "】数据签名验证失败" */);
		}
	}
	private void checkOriSumEqualsByPayApplicationBill(BillContext billContext, BizObject bizObject) throws Exception {
		String childrenFields = MetaDaoHelper.getChilrenField(billContext.getFullname());
		List<BizObject> childrenLines = bizObject.get(childrenFields);
		Long payApplyBillId = Long.parseLong(bizObject.get("srcbillid").toString());
		PayApplicationBill payApplicationBill = MetaDaoHelper.findById(PayApplicationBill.ENTITY_NAME, payApplyBillId);
		BigDecimal childrenOriSum;
		if (bizObject.getEntityStatus().name().equals("Insert")) {
			if (payApplicationBill.getPayBillStatus().getValue() == PayBillStatus.Auditing.getValue() ) {
				throw new CtmException(
						com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180451","关联的付款申请单") /* "关联的付款申请单" */ + payApplicationBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180452","未审核！") /* "未审核！" */);
			}
			if (payApplicationBill.getCloseStatus().getValue() == CloseStatus.Closed.getValue() ) {
				throw new CtmException(
						com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180451","关联的付款申请单") /* "关联的付款申请单" */ + payApplicationBill.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180453","已关闭！") /* "已关闭！" */);
			}
 			checkBillStatus(bizObject, payApplicationBill.getPubts());
			for (BizObject childrenLine : childrenLines) {
				childrenOriSum = new BigDecimal(childrenLine.get(IBussinessConstant.ORI_SUM).toString());
				Long srcbillitemid = Long.valueOf(childrenLine.get("srcbillitemid").toString());
				PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, srcbillitemid);
				BigDecimal paymentPreemptAmount = payApplicationBill_b.getPaymentPreemptAmount();
				BigDecimal subtract;
				if (null != paymentPreemptAmount) {
					subtract = BigDecimalUtils.safeSubtract(payApplicationBill_b.getPaymentApplyAmount(), paymentPreemptAmount);
				} else {
					subtract = payApplicationBill_b.getPaymentApplyAmount();
				}
				boolean flag = childrenOriSum.compareTo(subtract) < 1;
				if (!flag) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100511"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180456","参照付款申请单新增错误，付款单明细行付款金额大于可申请金额，请检查！") /* "参照付款申请单新增错误，付款单明细行付款金额大于可申请金额，请检查！" */);
				}
			}
		} else if (bizObject.getEntityStatus().name().equals("Update") && ! IBillNumConstant.PAYMENT_UPDATE.equals(billContext.getBillnum())) {
			if (ValueUtils.isNotEmptyObj(childrenLines)) {
				for (BizObject childrenLine : childrenLines) {
					Long srcbillitemid = Long.valueOf(childrenLine.get("srcbillitemid").toString());
					PayApplicationBill_b payApplicationBill_b = MetaDaoHelper.findById(PayApplicationBill_b.ENTITY_NAME, srcbillitemid);
					PayBillb payBillb = MetaDaoHelper.findById(PayBillb.ENTITY_NAME, childrenLine.getId());
					BigDecimal subtract = BigDecimalUtils.safeSubtract(payBillb.getOriSum(), new BigDecimal(childrenLine.get(IBussinessConstant.ORI_SUM).toString()));
					if (! (subtract.compareTo(BigDecimal.ZERO) == 0) ) {
						childrenOriSum = BigDecimalUtils.safeSubtract(payApplicationBill_b.getPaymentPreemptAmount(), subtract);
						boolean flag = childrenOriSum.compareTo(payApplicationBill_b.getPaymentApplyAmount()) < 1;
						if (!flag) {
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100512"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418045D","修改付款单明细金额错误！付款单明细行付款金额大于可申请金额，请检查！") /* "修改付款单明细金额错误！付款单明细行付款金额大于可申请金额，请检查！" */);
						}
					}
				}
			}
		}
	}

	private void checkBillStatus(BizObject bizObject, Date pubts) {
		if (null != bizObject.get("modifyTime")) {
			Date modifyTime = bizObject.get("modifyTime");
			bizObject.put("modifyTime", null);
			if (pubts.getTime() != modifyTime.getTime()) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100513"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00164", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
			}
		}
	}

	private void checkOriSumEqualsByBankreconciliation(BillContext billContext, BizObject bizObject) throws Exception {
		String childrenFields = MetaDaoHelper.getChilrenField(billContext.getFullname());
		List<BizObject> childrenLines = bizObject.get(childrenFields);
		BigDecimal oriSum = bizObject.get(IBussinessConstant.ORI_SUM);
		BigDecimal childrenOriSum = new BigDecimal(0);
		for (BizObject childrenLine : childrenLines) {
			childrenOriSum = childrenOriSum.add(childrenLine.get(IBussinessConstant.ORI_SUM));
		}
		if (!oriSum.equals(childrenOriSum)) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100514"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180464","付款单表头的付款金额与明细的付款金额合计不相等，请检查！") /* "付款单表头的付款金额与明细的付款金额合计不相等，请检查！" */));
		}
	}

	private void doInsertMoneyCheck(BillContext billContext, BizObject bizObject, CurrencyTenantDTO currencyDTO, CurrencyTenantDTO currencyOrgDTO) throws Exception {
		if(bizObject.getEntityStatus().equals(EntityStatus.Insert)){
            BigDecimal oriSum = BigDecimal.ZERO;
            BigDecimal natSum = BigDecimal.ZERO;
			//判断 ar、ap是否启用  若没启用则后续校验修改
			Boolean arBegin = true;
			Boolean apBegin = true;
			try{
				 Date arDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(bizObject.get(IBussinessConstant.ACCENTITY), ISystemCodeConstant.ORG_MODULE_AR);/* 暂不修改 内部调用财务公共 需要财务公共修改*/
				if(arDate == null){
					arBegin = false;
				}
			}catch(Exception e){
				arBegin = false;
			}
			try{
				Date apDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(bizObject.get(IBussinessConstant.ACCENTITY), ISystemCodeConstant.ORG_MODULE_AP);/* 暂不修改 内部调用财务公共 需要财务公共修改*/
				if(apDate == null){
					apBegin = false;
				}
			}catch(Exception e){
				apBegin = false;
			}
            if(ReceiveBill.ENTITY_NAME.equals(billContext.getFullname())){
                List<ReceiveBill_b> ReceiveBill_bList = bizObject.getBizObjects("ReceiveBill_b",ReceiveBill_b.class);
				if(bizObject.get("billtype") == null){
					bizObject.set("billtype",7);
				}
                if(ReceiveBill_bList == null || ReceiveBill_bList.size() == 0){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100515"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180427","表体不能为空！") /* "表体不能为空！" */);
                }
                for(ReceiveBill_b bb:ReceiveBill_bList){
                    if(bb.getEntityStatus().equals(EntityStatus.Delete)){
						continue;
					}
					if(currencyDTO != null && currencyDTO.getMoneydigit() != null){
						bb.setOriSum(bb.getOriSum().setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount()));
					}
					if(currencyOrgDTO != null && currencyOrgDTO.getMoneydigit() != null){
						bb.setNatSum(bb.getNatSum().setScale(currencyOrgDTO.getMoneydigit(),currencyOrgDTO.getMoneyrount()));
					}
					String code;
					if(ValueUtils.isNotEmptyObj(bb.get("quickType_code"))) {
						code = bb.get("quickType_code").toString();
					} else {
						Map<String, Object> condition = new HashMap<>();
						condition.put("id", bb.get("quickType"));
						Map<String, Object> quickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition).get(0);/* 暂不修改 已登记*/
						code = quickType.get("code").toString();
					}
					//当启用收付时不能录入款项类型未应收和应付的(预收款，应收款)
					if(arBegin && ("1".equals(code)||"2".equals(code))){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100516"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418041F","当启用收付时不能录入款项类型为应收和应付的") /* "当启用收付时不能录入款项类型为应收和应付的" */);
					}
					if(currencyDTO != null && currencyDTO.getMoneydigit() != null){
						oriSum = BigDecimalUtils.safeAdd(oriSum.setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount()),bb.getOriSum().setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount()));
					}else{
						oriSum = BigDecimalUtils.safeAdd(oriSum,bb.getOriSum());
					}
					if(currencyOrgDTO != null && currencyOrgDTO.getMoneydigit() != null){
						natSum = BigDecimalUtils.safeAdd(natSum.setScale(currencyOrgDTO.getMoneydigit(),currencyOrgDTO.getMoneyrount()),bb.getNatSum().setScale(currencyOrgDTO.getMoneydigit(),currencyOrgDTO.getMoneyrount()));
					}else{
						natSum = BigDecimalUtils.safeAdd(natSum,bb.getNatSum());
					}
                }
            }
            if(PayBill.ENTITY_NAME.equals(billContext.getFullname())){
				List<PayBillb> ReceiveBill_bList = bizObject.getBizObjects("PayBillb",PayBillb.class);
				if(bizObject.get("billtype") == null){
					bizObject.set("billtype",10);
				}
                if(ReceiveBill_bList == null || ReceiveBill_bList.size() == 0){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100515"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180427","表体不能为空！") /* "表体不能为空！" */);
                }
                for(PayBillb bb:ReceiveBill_bList){
                    if(bb.getEntityStatus().equals(EntityStatus.Delete)){
                        continue;
                    }
					if(currencyDTO != null && currencyDTO.getMoneydigit() != null){
						bb.getOriSum().setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount());
					}
					if(currencyOrgDTO != null && currencyOrgDTO.getMoneydigit() != null){
						bb.setNatSum(bb.getNatSum().setScale(currencyOrgDTO.getMoneydigit(),currencyOrgDTO.getMoneyrount()));
					}
					String code = null;
					if(ValueUtils.isNotEmptyObj(bb.get("quickType_code"))) {
						code = bb.get("quickType_code").toString();
					} else {
						Map<String, Object> condition = new HashMap<>();
						condition.put("id", bb.get("quickType"));
						List<Map<String, Object>> quickTypeList = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
						if (CollectionUtils.isNotEmpty(quickTypeList)) {
							Map<String, Object> quickType =quickTypeList.get(0);
							code = quickType.get("code").toString();
						} else {
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100517"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418042E","款项类型不能为空！") /* "款项类型不能为空！" */);
						}
					}
                    //当启用收付时不能录入款项类型未应收和应付的(预付款，应付款)
					if(apBegin && ("5".equals(code)||"6".equals(code))){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100516"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418041F","当启用收付时不能录入款项类型为应收和应付的") /* "当启用收付时不能录入款项类型为应收和应付的" */);
                    }
					if(currencyDTO != null && currencyDTO.getMoneydigit() != null){
						oriSum = BigDecimalUtils.safeAdd(oriSum.setScale(currencyDTO.getMoneydigit()),bb.getOriSum().setScale(currencyDTO.getMoneydigit()));
					}else{
						oriSum = BigDecimalUtils.safeAdd(oriSum,bb.getOriSum());
					}
					if(currencyOrgDTO != null && currencyOrgDTO.getMoneydigit() != null){
						natSum = BigDecimalUtils.safeAdd(natSum.setScale(currencyOrgDTO.getMoneydigit()),bb.getNatSum().setScale(currencyOrgDTO.getMoneydigit()));
					}else{
						natSum = BigDecimalUtils.safeAdd(natSum,bb.getNatSum());
					}
                }
            }
            if(!bizObject.get("srcitem").equals(EventSource.SystemOut.getValue())){
                bizObject.set(IBussinessConstant.ORI_SUM,oriSum);
                bizObject.set("natSum",natSum);
            }
        }
	}

	private void doImportCheck(BillContext billContext, BizObject bizObject) throws Exception {
		//导入进来的数据
		if(billContext.getFullname().equals(ReceiveBill.ENTITY_NAME)){
			bizObject.set("billtype",EventType.ReceiveBill.getValue());
		}else {
			bizObject.set("billtype",EventType.PayMent.getValue());
		}
		bizObject.set("auditstatus",AuditStatus.Incomplete.getValue());
		bizObject.set("srcitem",EventSource.Cmpchase.getValue());
		bizObject.set("paystatus", PayStatus.NoPay.getValue());
		bizObject.set("voucherstatus", VoucherStatus.Empty.getValue());
		if(bizObject.get("tradetype")==null){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100518"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180437","交易类型不能为空！") /* "交易类型不能为空！" */);
		}

		if(bizObject.get("currency")==null){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100519"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180439","币种不能为空！") /* "币种不能为空！" */);
		}
		if (bizObject.get("caobject")==null||!(bizObject.getShort("caobject")==1||bizObject.getShort("caobject")==2||bizObject.getShort("caobject")==3||bizObject.getShort("caobject")==4)){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100520"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418043A","付款对象出错！") /* "付款对象出错！" */);
		}
		if(bizObject.get("exchangeRateType")==null){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100521"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418043C","汇率类型不能为空！") /* "汇率类型不能为空！" */);
		}
		if(bizObject.get("settlemode") == null){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100522"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00163", "结算方式不能为空！") /* "结算方式不能为空！" */);
		}
		if(PayBill.ENTITY_NAME.equals(billContext.getFullname()) && bizObject.getShort("caobject")==4){
//			if(null != bizObject.get("retailerAccountNo")){//收款账号
//				String retailerAccountNo = bizObject.get("retailerAccountNo").toString().trim();
//				if(!StringUtils.isEmpty(retailerAccountNo) && !retailerAccountNo.matches("[0-9]+")){
//					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100523"),MessageUtils.getMessage("P_YS_FI_CM_0001223673") /* "收款账号格式有误" */);
//				}
//			}
			if(null != bizObject.get("retailerLineNumber")){//收款账户联行号
				String retailerLineNumber = bizObject.get("retailerLineNumber").toString();
				if(!(!StringUtils.isEmpty(retailerLineNumber) && retailerLineNumber.length() == 12 && retailerLineNumber.matches("[0-9]+"))){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100524"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418043F","收款账户联行号格式有误") /* "收款账户联行号格式有误" */);
				}
			}
		}
		Map<String, Object> condition = new HashMap<>();
		condition.put("id", bizObject.get("settlemode"));
		//校验业务组织权限  要根据会计主体查询有核算委托关系的业务组织集合
		Set<String>  orgList = new HashSet<>();//有核算委托关系的组织id集合
		condition = new HashMap<>();
		condition.put("finOrg", bizObject.get(IBussinessConstant.ACCENTITY));
		List<Map<String, Object>> orgList_temp = QueryBaseDocUtils.queryConsignmentByCondition(condition);
		for (Map<String, Object> org:orgList_temp){
			orgList.add((String) org.get("adminOrg"));
		}
		orgList.add(bizObject.get(IBussinessConstant.ACCENTITY).toString());
		// 支持企业账号级
		orgList.add(IStwbConstantForCmp.GLOBAL_ACCENTITY);
		if(!StringUtils.isEmpty(bizObject.get("org"))){
			if(!orgList.contains(bizObject.get("org").toString())){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100525"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180448","只允许导入与当前会计主体存在核算委托关系的组织!") /* "只允许导入与当前会计主体存在核算委托关系的组织!" */);
			}
		}

		// 数据权限过滤
		String[] fields = new String[] { IBussinessConstant.CUSTOMER, "supplier", "employee" };
		Map<String, String> fieldName = new HashMap<>();
		fieldName.put(IBussinessConstant.CUSTOMER, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180449","客户") /* "客户" */);
		fieldName.put("supplier", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418044C","供应商") /* "供应商" */);
		fieldName.put("employee", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418044D","员工") /* "员工" */);
		Map<String, List<Object>> accounts = AuthUtil.dataPermission("CM", billContext.getFullname(), null, fields);
		if (accounts != null && accounts.size() > 0) {
			for (String field : fields) {
				if (bizObject.get(field) == null) {
					continue;
				}
				List<Object> fieldData = accounts.get(field);
				if (CollectionUtils.isNotEmpty(fieldData)) {
					if (!fieldData.contains(bizObject.get(field).toString())) {
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100526"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180450","数据权限控制校验异常:") /* "数据权限控制校验异常:" */ + fieldName.get(field) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418044F","无权") /* "无权" */);
					}
				}
			}
		}
		if(bizObject.get("caobject").equals(CaObject.Customer.getValue())){
			if(bizObject.get(IBussinessConstant.CUSTOMER)!=null){
				List<MerchantApplyRangeDTO> customeList =  QueryBaseDocUtils.queryMerchantApplyRange(bizObject.getLong(IBussinessConstant.CUSTOMER));
				boolean exist = false;
				for (MerchantApplyRangeDTO customerMap : customeList) {
					if (orgList.contains(customerMap.getOrgId())) {
						exist = true;
					}
				}
				if(!exist){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100527"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180454","导入的客户所属使用组织与导入会计主体不一致！") /* "导入的客户所属使用组织与导入会计主体不一致！" */);
				}
				if(bizObject.get("customerbankaccount")!=null){
					List<AgentFinancialDTO> customerbankaccount = QueryBaseDocUtils.queryCustomerBankAccountById(bizObject.get("customerbankaccount"), bizObject.get(IBussinessConstant.CUSTOMER), Boolean.FALSE);
					if(CollectionUtils.isEmpty(customerbankaccount)){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100528"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180455","导入的客户银行账号与导入的客户不匹配！") /* "导入的客户银行账号与导入的客户不匹配！" */);
					}
				}
			}
		}else if (bizObject.get("caobject").equals(CaObject.Supplier.getValue())){
			if(bizObject.get("supplier")!=null){
				List<VendorOrgVO> supplierList=  vendorQueryService.getVendorOrgByVendorId(bizObject.get("supplier"));
				if(log.isInfoEnabled()) {
					log.info("  test logger  supplierList is  BeforeSaveBillRuleToCmp   ---- 770  ---" + JsonUtils.toJson(supplierList));
				}
				boolean exist = false;
				for (VendorOrgVO vendorOrgVO : supplierList) {
					if (orgList.contains(vendorOrgVO.getOrg())) {
						exist = true;
					}
				}
				if(!exist){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100529"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418045C","导入的供应商所属使用组织与导入会计主体不一致！") /* "导入的供应商所属使用组织与导入会计主体不一致！" */);
				}
				if(bizObject.get("supplierbankaccount")!=null){
					VendorBankVO supplierbankaccount  = vendorQueryService.getVendorBanksByAccountId(bizObject.get("supplierbankaccount"));
					if(supplierbankaccount != null){
						if(!supplierbankaccount.getVendor().equals(bizObject.get("supplier"))){
							throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100530"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418045E","导入的供应商银行账号与导入的供应商不匹配！") /* "导入的供应商银行账号与导入的供应商不匹配！" */);
						}
					}
				}
			}
		}else if (bizObject.get("caobject").equals(CaObject.Employee.getValue())){
			if(bizObject.get("employee")!=null){
				List<Map<String, Object>> staffList=  QueryBaseDocUtils.queryOrgByStaffId(bizObject.get("employee"));
				List<String> staffDeptList = new ArrayList<String>();
				boolean staffOrg = false;
				if(staffList == null || staffList.size() == 0){
					staffOrg = false;
				}else{
					for(Map<String, Object> staff : staffList){
						if(orgList.contains(staff.get("org_id").toString())){
							staffDeptList.add(staff.get("dept_id").toString());
							staffOrg = true;
							break;
						}
					}
				}
//				if(bizObject.get("dept") != null){
//					if(!staffDeptList.contains(bizObject.get("dept").toString())){
//						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100531"),MessageUtils.getMessage("P_YS_CTM_CM-BE_0001378999") /* "导入的员工所属部门与导入部门不一致！" */);
//					}
//				}
				if(!staffOrg){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100532"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1ECD8B0605C80005","导入的员工所属资金组织与导入资金组织不一致！") /* "导入的员工所属资金组织与导入资金组织不一致！" */);
				}
				if(bizObject.get("staffBankAccount")!=null){
					Map<String, Object> staffBankAccount  = QueryBaseDocUtils.queryStaffBankAccountById(bizObject.get("staffBankAccount"));
					if(!staffBankAccount.get("staff_id").equals(bizObject.get("employee"))){
						throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100533"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418046B","导入的员工银行账号与导入的员工不匹配！") /* "导入的员工银行账号与导入的员工不匹配！" */);
					}
				}
			}

		}

		if(bizObject.get("caobject").equals(CaObject.Other.getValue())) {
			Short retailerAccountType=bizObject.getShort("retailerAccountType");
			if (retailerAccountType!=null&&!(1==retailerAccountType||2==retailerAccountType||3==retailerAccountType)){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100534"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418046E","收款类型错误！") /* "收款类型错误！" */);
			}
		}
		if(bizObject.get("enterprisebankaccount")!=null){
			//银行账户
			EnterpriseBankAcctVO enterpriseBankAcctVO= baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("enterprisebankaccount"));
			Integer enable = enterpriseBankAcctVO.getEnable();
			if(1!=enable){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100535"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180472","银行账号为:") /* "银行账号为:" */ + enterpriseBankAcctVO.getAccount() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180473","的账户未启用,请检查!") /* "的账户未启用,请检查!" */);
			}
			CmpCommonUtil.checkBankAcctCurrency(bizObject.get("enterprisebankaccount"), bizObject.get("currency"));
			String accentity = enterpriseBankAcctVO.getOrgid();
			if (!bizObject.get(IBussinessConstant.ACCENTITY).equals(accentity)) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100536"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180475","银行账号与会计主体不匹配！") /* "银行账号与会计主体不匹配！" */);
			}

		}
		if(bizObject.get("cashaccount")!=null){
			//现金账户
			EnterpriseCashVO EnterpriseCashVO = baseRefRpcService.queryOneCashAcctByCondition(bizObject.getString("cashaccount"));
			if(!EnterpriseCashVO.getOrgid().equals(bizObject.getString(IBussinessConstant.ACCENTITY))){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100537"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180477","导入的现金账户所属会计主体与导入会计主体不一致！") /* "导入的现金账户所属会计主体与导入会计主体不一致！" */);
			}
		}
		String childrenField = MetaDaoHelper.getChilrenField(billContext.getFullname());
		List<BizObject> lines = (List) bizObject.get(childrenField);
		if(Objects.isNull(lines)||lines.size()<1){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100538"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180479","子表获取失败，请查看主子表关联关系") /* "子表获取失败，请查看主子表关联关系" */);
		}

		BigDecimal exchrate = bizObject.get("exchRate");
		if(exchrate ==  null ){
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100539"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418041B","汇率不能为空！") /* "汇率不能为空！" */);
		}
		if (exchrate.doubleValue() < 0) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100540"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418041D","汇率不能小于0！") /* "汇率不能小于0！" */);
		}
		Map<String, BigDecimal> exchrateMap = new HashMap();
		exchrateMap.put("exchRate", exchrate);
		//汇率类型精度
		CheckPrecisionVo checkPrecisionVo = new CheckPrecisionVo();
		checkPrecisionVo.setEntityName(billContext.getFullname());
		Map<String, Object> condition1 = new HashMap<String, Object>();
		condition1.put("id", bizObject.get("exchangeRateType"));
		List<Map<String, Object>> mapList = QueryBaseDocUtils.queryExchangeRateTypeByCondition(condition1);
		if (CollectionUtils.isEmpty(mapList)) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100541"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180423","未获取到汇率类型！") /* "未获取到汇率类型！" */);
		}
		Map<String, Object> exchangeRateVo = mapList.get(0);
		Integer digit = (Integer) exchangeRateVo.get("digit");
		checkPrecisionVo.setPrecisionNum(digit);
		checkPrecisionVo.setNumericalMap(exchrateMap);
		CheckPrecision.checkPreByPreNum(checkPrecisionVo);
		//精度校验
		Map<String, BigDecimal> numericalMap = new HashMap<String, BigDecimal>();
		for (BizObject line : lines) {
			//原币金额
			BigDecimal oriSum = line.get(IBussinessConstant.ORI_SUM);
			//本币金额
			BigDecimal natSum = line.get("natSum");
			checkPrecisionVo.setPrecisionId(bizObject.get("currency"));
			numericalMap.put(IBussinessConstant.ORI_SUM,oriSum);
			checkPrecisionVo.setNumericalMap(numericalMap);
			CheckPrecision.checkMoneyByCurrency(checkPrecisionVo);

			String natCurrency = bizObject.get("natCurrency");
			checkPrecisionVo.setPrecisionId(natCurrency);
			numericalMap = new HashMap<String, BigDecimal>();
			numericalMap.put("natSum",natSum);
			checkPrecisionVo.setNumericalMap(numericalMap);
			CheckPrecision.checkMoneyByCurrency(checkPrecisionVo);

			if(SetScalUtil.setScalByCurerncy(oriSum.multiply(exchrate),natCurrency,"money").compareTo(natSum) !=0){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100542"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418042C","录入的原币、本币金额与汇率不匹配！") /* "录入的原币、本币金额与汇率不匹配！" */);
			}

			if(line.get("quickType")==null){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100517"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418042E","款项类型不能为空！") /* "款项类型不能为空！" */);
			}
		}
		if (bizObject.getEntityStatus().name().equals("Insert")) {
			boolean sifangFlag = ("Sifang".equals(bizObject.get("srcflag")));
			if (sifangFlag && StringUtils.isEmpty(bizObject.get("srcbillid"))) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100543"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080092", "来源单据号不能为空！") /* "来源单据号不能为空！" */);
			}
			if (!StringUtils.isEmpty(bizObject.get("srcbillid"))) {
				CmpCommonUtil.checkPayBillExist(ICmpConstant.SRCBILLID, bizObject.get("srcbillid").toString(), bizObject.getEntityName());
			}
		}
	}

    /**
     * 对账单录入付款单判断金额是否相等
     *
     * @param bizObject
     */
    private void checkMoney(BizObject bizObject, BillContext billContext,Map<String,Object> map) throws Exception {
        if (bizObject.get("srcbillid") != null && !bizObject.get("srcbillid").toString().equals("")) {
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, Long.parseLong(bizObject.get("srcbillid").toString()));
            YmsLock ymsLock = JedisLockUtils.lockRuleWithOutTrace(bankReconciliation.getId().toString(),map);
			try {
                if (bankReconciliation != null && ymsLock==null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100544"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180438","该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
                }
            } finally {
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            }
            if (bankReconciliation != null && (bankReconciliation.getAutobill() || bankReconciliation.getCheckflag())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100545"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418043B","该对账单不是最新状态，请重新查询！") /* "该对账单不是最新状态，请重新查询！" */);
            }
            if (null != bankReconciliation.getCreditamount() && ((BigDecimal) bizObject.get(IBussinessConstant.ORI_SUM)).compareTo(bankReconciliation.getCreditamount()) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100546"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418043D","收款单录入金额与对账单不相等") /* "收款单录入金额与对账单不相等" */));
            }
            if (null != bankReconciliation.getDebitamount() && ((BigDecimal) bizObject.get(IBussinessConstant.ORI_SUM)).compareTo(bankReconciliation.getDebitamount()) != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100547"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418043E","付款单录入金额与对账单不相等") /* "付款单录入金额与对账单不相等" */));
            }
            checkBillStatus(bizObject, bankReconciliation.getPubts());

			if (bizObject.getEntityStatus().name().equals("Insert")) {
				Date curDate = new Date();
				bizObject.put("auditstatus", AuditStatus.Complete.getValue());
//				bizObject.set("verifystate", VerifyState.COMPLETED.getValue());
				bizObject.put("auditorId", AppContext.getCurrentUser().getId());
				bizObject.put("auditTime", new Date());
				bizObject.put("auditDate", BillInfoUtils.getBusinessDate());
				bizObject.put("settlestatus", SettleStatus.alreadySettled.getValue());
				bizObject.put("status", Status.confirmed.getValue());
				bizObject.put("cmpflag", true);
				bizObject.put("voucherstatus", VoucherStatus.Received.getValue());
				bizObject.put("settleuser", AppContext.getCurrentUser().getId().toString());
				bizObject.put("settledate", curDate);
				bizObject.put("creator", AppContext.getCurrentUser().getName());
				bizObject.put("creatorId", AppContext.getCurrentUser().getId());
				bizObject.put("createDate", curDate);
//				bizObject.put("srctypeflag","auto");
				if (bankReconciliation.getCheckflag()) {
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100548"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180443","对账单不能重复录入") /* "对账单不能重复录入" */));
				}
				if (PayBill.ENTITY_NAME.equals(billContext.getFullname())){
					bizObject.put("paystatus", PayStatus.OfflinePay.getValue());
				}
				//对账单生成收款单后将是否已自动生单设置为已自动生单
				bankReconciliation.setAutobill(true);
				//对账单生成收款单后将是否勾兑设置为已勾兑
				bankReconciliation.setCheckflag(true);
				int result = SqlHelper.update(BANKRECONCILIATIONMAPPER + ".updateBankReconciliation",bankReconciliation);
				if(result <= 0){
					throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100548"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180443","对账单不能重复录入") /* "对账单不能重复录入" */));
				}
			}
		}
	}

}
