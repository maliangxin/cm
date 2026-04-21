package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.enums.MerchantOperateEnum;
import com.yonyoucloud.fi.cmp.util.MerchantResult;
import com.yonyoucloud.fi.cmp.util.MerchantUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialQryDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorExtendVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class DefaultAccountCheckRule extends AbstractCommonRule {


	@Autowired
	BaseRefRpcService baseRefRpcService;
	@Autowired
	VendorQueryService vendorQueryService;

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		List<BizObject> bills = getBills(billContext, paramMap);
		if (bills == null || bills.size() == 0) {
			return new RuleExecuteResult();
		}
		BizObject bill = (BizObject) bills.get(0);
		Object currencyId = bill.get("currency");
		Object accentity = bill.get(IBussinessConstant.ACCENTITY);
		Object customerId = bill.get(IBussinessConstant.CUSTOMER);
		if (customerId != null) {
			AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
			agentFinancialQryDTO.setMerchantId(Long.valueOf(customerId.toString()));
			agentFinancialQryDTO.setStopStatus(Boolean.FALSE);
			agentFinancialQryDTO.setIfDefault(Boolean.TRUE);
			if (currencyId != null) {
				agentFinancialQryDTO.setCurrency(currencyId.toString());
			}
			List<AgentFinancialDTO> bankAccounts = QueryBaseDocUtils.queryCustomerBankAccountByCondition(agentFinancialQryDTO);
			if (bankAccounts.size() > 0) {
				bill.set("customerbankaccount", bankAccounts.get(0).getId());// 客户银行账户id
				bill.set("customerbankaccount_bankAccount", bankAccounts.get(0).getBankAccount()); // 客户银行账号
				bill.set("customerbankaccount_bankAccountName", bankAccounts.get(0).getBankAccountName()); // 客户银行账户名称
				// 查询开户行
				if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).getOpenBank())){
                    BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenBank());
                    if (depositBank != null) {
                        bill.set("customerbankaccount_openBank_name", depositBank.getName()); // 客户账户银行网点
                    } else {
                        bill.set("customerbankaccount_openBank_name", null); // 客户账户银行网点
                    }
				}
			} else {
				bill.set("customerbankaccount", null);// 客户银行账户id
				bill.set("customerbankaccount_bankAccount", null); // 客户银行账号
				bill.set("customerbankaccount_bankAccountName", null); // 客户银行账户名称
			}
			// BUG: CZFW-6861  新增收付款单时，选择客户时带出对应的专管部门
			setDeptByMerchant(bill, accentity, customerId);
		}
		Object supplierId = bill.get("supplier");
		if (supplierId != null) {
			Map<String, Object> condition = new HashMap<>();
			condition.put("vendor", supplierId);
			condition.put("stopstatus", "0");
			condition.put("defaultbank", true);
			if (currencyId != null) {
				condition.put("currency", currencyId);
			}
			List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(condition);
			if (bankAccounts.size() > 0) {
				bill.set("supplierbankaccount", bankAccounts.get(0).getId());// 供应商银行账户id
				bill.set("supplierbankaccount_account", bankAccounts.get(0).getAccount());// 供应商银行账号
				bill.set("supplierbankaccount_accountname", bankAccounts.get(0).getAccountname());// 供应商银行账户名称
				bill.set("supplierbankaccount_correspondentcode", bankAccounts.get(0).getCorrespondentcode());// 供应商联行号
				// 查询开户行
				BankdotVO  depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenaccountbank());
				if (depositBank != null) {
					bill.set("supplierbankaccount_openaccountbank_name", depositBank.getName()); // 供应商账户银行网点
				} else {
					bill.set("supplierbankaccount_openaccountbank_name", null); // 供应商账户银行网点
				}
			} else {
				bill.set("supplierbankaccount", null);// 供应商银行账户id
				bill.set("supplierbankaccount_account", null);// 供应商银行账号
				bill.set("supplierbankaccount_accountname", null);// 供应商银行账户名称
				bill.set("supplierbankaccount_correspondentcode", null);// 供应商联行号
			}
			// BUG: CZFW-6861  新增收付款单时，选择供应商时带出对应的专管部门
			setDeptByVendor(bill, accentity, supplierId);
		}
		Object employeeId = bill.get("employee");
		if (employeeId != null) {
			Map<String, Object> condition = new HashMap<>();
			condition.put("staff_id", employeeId);
			condition.put("isdefault", 1);
			condition.put("dr", 0);
			if (currencyId != null) {
				condition.put("currency", currencyId);
			}
			List<Map<String, Object>> bankAccounts = QueryBaseDocUtils.queryStaffBankAccountByCondition(condition);/* 暂不修改 已登记*/
			if (bankAccounts.size() > 0) {
				bill.set("staffBankAccount", bankAccounts.get(0).get("id"));// 员工银行账户名称
				bill.set("staffBankAccount_accountno", bankAccounts.get(0).get("account"));// 员工银行账户名称
				bill.set("staffBankAccount_account", bankAccounts.get(0).get("account"));// 员工银行账户名称
				if(bankAccounts.get(0).get("bankname") != null){
					BankdotVO  depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).get("bankname").toString());
					if (depositBank != null) {
						bill.set("staffBankAccount_bankname_name", depositBank.getName());// 员工账户银行网点
					}
				}else {
					bill.set("staffBankAccount_bankname_name", null);// 员工账户银行网点
				}

			} else {
				bill.set("staffBankAccount", null);// 员工银行账户名称1
				bill.set("staffBankAccount_accountno", null);// 员工银行账户名称
				bill.set("staffBankAccount_account", null);// 员工银行账户名称
			}
		}
		this.putParam(paramMap, "return", bill);
		return new RuleExecuteResult();
	}

	private void setDeptByMerchant(BizObject bill, Object accentity, Object customerId) {
		MerchantResult merchantResult;
		try {
			BillDataDto data = new BillDataDto();
			data.setFullname(MerchantConstant.AA_MERCHANT_MERCHANT);
			data.setData("*");
			FilterVO condition = new FilterVO();
			condition.setIsExtend(true);
			Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity.toString());
			SimpleFilterVO orgFilter = new SimpleFilterVO();
			orgFilter.setField("merchantAppliedDetail.merchantApplyRangeId.orgId");
			orgFilter.setOp("in");
			orgFilter.setValue1(orgSet.toArray());

			SimpleFilterVO bankAccountNameFilter = new SimpleFilterVO();
			bankAccountNameFilter.setField("id");
			bankAccountNameFilter.setOp(ICmpConstant.QUERY_EQ);
			bankAccountNameFilter.setValue1(customerId);

			SimpleFilterVO[] simpleVOs = {orgFilter, bankAccountNameFilter};
			condition.setSimpleVOs(simpleVOs);
			data.setCondition(condition);
			// 设置principals参数
			Map<String, Object> principals = new HashMap<>();
			principals.put(MerchantConstant.FULLNAME, MerchantConstant.AA_MERCHANT_PRINCIPAL);
			principals.put(MerchantConstant.DATA,
					"professSalesman, professSalesman.name as professSalesman_name, specialManagementDep,specialManagementDep.name as cSpecialManagementDepName");

			Map<String, Object> partParam = new HashMap<>();
			partParam.put(MerchantConstant.PRINCIPALS, principals);
			data.setPartParam(partParam);

			merchantResult = MerchantUtils.operateMerchant(data, MerchantOperateEnum.QUERY);
			if (merchantResult.getCode() != 200) {
				log.info("MerchantUtils--queryCust--exception:", merchantResult.getMessage());
			} else {
				CtmJSONArray merchantArray = CtmJSONArray.parseArray(CtmJSONObject.toJSONString(merchantResult.getData()));
				if (!merchantArray.isEmpty()) {
					CtmJSONObject merchantObj = merchantArray.getJSONObject(0);
					CtmJSONArray principal = merchantObj.getJSONArray("principals");
					CtmJSONObject jsonObject = principal.getJSONObject(0);
					if (!jsonObject.isEmpty()) {
						if (jsonObject.get("cSpecialManagementDepName") != null) {
							bill.set("dept_name", jsonObject.get("cSpecialManagementDepName"));
						}
						if (jsonObject.get("specialManagementDep") != null) {
							bill.set("dept", jsonObject.get("specialManagementDep"));
						}
						if (jsonObject.get("professSalesman") != null) {
							bill.set("operator", jsonObject.get("professSalesman"));
						}
						if (jsonObject.get("professSalesman_name") != null) {
							bill.set("operator_name", jsonObject.get("professSalesman_name"));
						}
					}
				}
			}
		} catch (Exception e) {
			log.info("DefaultAccountCheckRule--queryCust--exception:", e.getMessage());
		}
	}

	private void setDeptByVendor(BizObject bill, Object accentity, Object supplierId) {
		try {
			// 供应商id集合
			List<Long> supplierList = new ArrayList<>();
			supplierList.add(Long.valueOf(supplierId.toString()));
			// 供应商使用组织id集合
			List<String> supplierOrgList = new ArrayList<>();
			supplierOrgList.add(accentity.toString());
			if (bill.getString(ICmpConstant.ORG) != null) {
				supplierOrgList.add(bill.getString(ICmpConstant.ORG));
			}
			List<VendorExtendVO> vendorExtendList = vendorQueryService.getVendorExtendFieldByVendorIdListAndOrgIdList(supplierList, supplierOrgList);
			if (CollectionUtils.isNotEmpty(vendorExtendList)) {
				bill.set("dept_name", vendorExtendList.get(0).get("department_name"));
				bill.set("dept", vendorExtendList.get(0).getDepartment());
				bill.set("operator", vendorExtendList.get(0).getPerson());
				bill.set("operator_name", vendorExtendList.get(0).get("person_name"));
			}
		} catch (Exception e) {
			log.error("DefaultAccountCheckRule--setDeptByVendor--exception:", e.getMessage());
		}
	}

}
