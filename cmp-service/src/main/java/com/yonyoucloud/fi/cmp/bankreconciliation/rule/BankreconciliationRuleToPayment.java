package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.basedoc.model.BankdotVO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JsonUtils;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.IMultilangConstant;
import com.yonyoucloud.fi.cmp.constant.ISchemaConstant;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialQryDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
public class BankreconciliationRuleToPayment extends AbstractCommonRule {
    @Autowired
    CmCommonService cmCommonService;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    private SettlementService settlementService;

    @Autowired
    VendorQueryService vendorQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String, Object>> omakes = (List) paramMap.get("omake");

        Iterator<Map<String, Object>> iterator = omakes.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> map = iterator.next();
            String accentity = map.get(IBussinessConstant.ACCENTITY).toString();
            String currency = map.get("currency").toString();
            Object toAcctName = map.get("to_acct_name");
            Object toAcctNo = map.get("to_acct_no");
            //付款对象---当前会计主体以及对应的业务组织的客户档案进行匹配，如果匹配到对应客户，则收付款对象类型=客户；如果没有匹配，则再在供应商档案进行匹配，如果匹配对应供应商，则收付款对象类型=供应商，如果仍未匹配，则收付款对象类型=其他；
            BankReconciliation bankReconciliation = new BankReconciliation();
            bankReconciliation.setTran_date(DateUtils.strToDate(map.get("tran_date").toString()));
            bankReconciliation.setAccentity(accentity);

            if (toAcctName != null) {
                bankReconciliation.setTo_acct_name(toAcctName.toString());
            }
            if (toAcctNo != null) {
                bankReconciliation.setTo_acct_no(toAcctNo.toString());
            }
            //事项来源
            map.put("srcitem", EventSource.Cmpchase.getValue());
            //交易类型id 	如果生成应付付款单，取应付付款单默认交易类型； 如果生成现金付款单，取现金付款单默认交易类型； 可改
            Map<String, Object> queryTransType = cmCommonService.queryTransTypeById("FICM2", "1", null);
            if (ValueUtils.isNotEmptyObj(queryTransType)) {
                map.put("tradetype_id", queryTransType.get("id"));
                map.put("tradetype_name", queryTransType.get("name"));
                map.put("tradetype_code", queryTransType.get("code"));
            }
            if (null == toAcctName || null == toAcctNo) {
                map.put("caobject", CaObject.Other.getValue());
                map.put("retailerAccountName", toAcctName);//付款账户名称
                map.put("retailerAccountNo", map.get("to_acct_no"));//付款账号
            } else {
                CtmJSONObject customerJson = getCustomer(bankReconciliation);
                if (Objects.equals(MerchantConstant.TRUE, customerJson.getString(MerchantConstant.CUSTOMERFLAG))) {
                    // 客户存在 生成客户的付款单
                    map.put("caobject", CaObject.Customer.getValue());
                    //付款客户id
                    Long customerId = customerJson.getLong("customerId");
                    map.put("customer", customerId);

                    if (customerId != null) {
                        AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
                        agentFinancialQryDTO.setMerchantId(customerId);
                        agentFinancialQryDTO.setStopStatus(Boolean.FALSE);
                        agentFinancialQryDTO.setIfDefault(Boolean.TRUE);
                        if (map.get("currency") != null) {
                            agentFinancialQryDTO.setCurrency(map.get("currency").toString());
                        }
                        List<AgentFinancialDTO> bankAccounts = QueryBaseDocUtils.queryCustomerBankAccountByCondition(agentFinancialQryDTO);
                        if (bankAccounts.size() > 0) {
                            map.put("customerbankaccount_bankAccount", bankAccounts.get(0).getBankAccount()); // 客户银行账号
                            // 查询开户行
                            if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).getOpenBank())) {
                                BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenBank());
                                if (depositBank != null) {
                                    map.put("customerbankaccount_openBank_name", depositBank.getName()); // 客户账户银行网点
                                } else {
                                    map.put("customerbankaccount_openBank_name", null); // 客户账户银行网点
                                }
                            }
                        } else {
                            map.put("customerbankaccount_bankAccount", null); // 客户银行账号
                        }
                    }

                    //付款客户账户
                    map.put("customerbankaccount", customerJson.getLong("customerbankId"));
                    map.put("customerbankaccount_bankAccountName", toAcctName);
                    //付款客户
                    map.put("customer_name", customerJson.get("name"));
                    map.put("org", customerJson.get("orgId"));
                    map.put("org_name", customerJson.get("orgName"));
                    //部门
//                    map.put("dept", customerJson.get("specialManagementDep"));
//                    map.put("dept_name", customerJson.get("cSpecialManagementDepName"));
//                    //业务人员
//                    map.put("operator", customerJson.get("cProfessSalesmanId"));
//                    map.put("operator_name", customerJson.get("cProfessSalesmanName"));
                } else {
                    CtmJSONObject supplierJson = getSupplier(bankReconciliation);
                    if (Objects.equals(MerchantConstant.TRUE, supplierJson.getString(MerchantConstant.VENDORFLAG))) {
                        // 供应商存在 生成供应商退款单
                        map.put("caobject", CaObject.Supplier.getValue());
                        //付款供应商id
                        Long supplierId = supplierJson.getLong("supplierId");
                        map.put("supplier", supplierId);

                        if (supplierId != null) {
                            Map<String, Object> condition = new HashMap<>();
                            condition.put("vendor", supplierId);
                            condition.put("stopstatus", "0");
                            condition.put("defaultbank", true);
                            Object currencyId = map.get("currency");
                            if (currencyId != null) {
                                condition.put("currency", currencyId);
                            }
                            List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(condition);
                            if (bankAccounts.size() > 0) {
                                map.put("supplierbankaccount_account", bankAccounts.get(0).getAccount());// 供应商银行账号
                                // 查询开户行
                                BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenaccountbank());
                                if (depositBank != null) {
                                    map.put("supplierbankaccount_openaccountbank_name", depositBank.getName()); // 供应商账户银行网点
                                } else {
                                    map.put("supplierbankaccount_openaccountbank_name", null); // 供应商账户银行网点
                                }
                            } else {
                                map.put("supplierbankaccount_account", null);// 供应商银行账号
                            }
                        }


                        //付款供应商账户
                        map.put("supplierbankaccount", supplierJson.getLong("supplierbankId"));
                        map.put("supplierbankaccount_accountname", toAcctName);

                        //付款供应商
                        map.put("supplier_name", supplierJson.get("name"));
                        map.put("org", supplierJson.get("orgId"));
                        map.put("org_name", supplierJson.get("orgName"));
                        //部门
//                        map.put("dept", supplierJson.get("iDepartmentId"));
//                        map.put("dept_name", supplierJson.get("iDepartmentIdName"));
//                        //业务人员
//                        map.put("operator", supplierJson.get("iPersonId"));
//                        map.put("operator_name", supplierJson.get("iPersonIdName"));
                    } else {
                        Map<String, Object> employee = getEmployee(toAcctName, toAcctNo, map.get(IBussinessConstant.ACCENTITY));
                        if (ValueUtils.isNotEmpty(employee)) {
                            map.put("caobject", CaObject.Employee.getValue());
                            Object employeeId = employee.get("id");
                            map.put("employee", employeeId);

                            if (employeeId != null) {
                                Map<String, Object> condition = new HashMap<>();
                                condition.put("staff_id", employeeId);
                                condition.put("isdefault", 1);
                                condition.put("dr", 0);
                                Object currencyId = map.get("currency");
                                if (currencyId != null) {
                                    condition.put("currency", currencyId);
                                }
                                List<Map<String, Object>> bankAccounts = QueryBaseDocUtils.queryStaffBankAccountByCondition(condition);
                                if (bankAccounts.size() > 0 && bankAccounts.get(0).get("bankname") != null) {
                                    BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).get("bankname").toString());
                                    if (depositBank != null) {
                                        map.put("staffBankAccount_bankname_name", depositBank.getName());// 员工账户银行网点
                                    } else {
                                        map.put("staffBankAccount_bankname_name", null);// 员工账户银行网点
                                    }
                                }
                            }

                            map.put("employee_name", employee.get("name"));
                            map.put("staffBankAccount", employee.get("staffBankAccount"));
                            map.put("staffBankAccount_account", employee.get("staffBankAccount_account"));
                            map.put("dept", employee.get("dept"));
                            map.put("dept_name", employee.get("dept_name"));
                        } else {
                            map.put("caobject", CaObject.Other.getValue());
                            map.put("retailerAccountName", toAcctName);//付款账户名称
                            map.put("retailerAccountNo", map.get("to_acct_no"));//付款账号
                        }
                    }
                }

            }
            getPayment(map, accentity, currency, bankReconciliation);
        }

        paramMap.put("omake", omakes);
        if (log.isInfoEnabled()) {
            log.info("execute info result =======================>" + JsonUtils.toJSON(omakes));
        }
        return new RuleExecuteResult();
    }

    private Map<String, Object> getEmployee(Object toAcctName, Object toAcctNo, Object accentity) throws Exception {
        QuerySchema querySchema_period = new QuerySchema();
        // 返回字段用于获取对应关系,需要啥加啥
        querySchema_period.addSelect(
                "bankAcctList.id as staffBankAccount, bankAcctList.account as staffBankAccount_account, " +
                        "name, id, code, mainJobList.dept_id as dept, mainJobList.dept_id.name as dept_name, " +
                        "mainJobList.org_id as org, mainJobList.org_id.name as org_name");
        // 对应的员工姓名、银行账号
        querySchema_period.appendQueryCondition(QueryCondition.name("bankAcctList.account").eq(toAcctNo));
        querySchema_period.appendQueryCondition(QueryCondition.name("name").eq(toAcctName));
        querySchema_period.appendQueryCondition(QueryCondition.name("mainJobList.org_id").eq(accentity));

        List<Map<String, Object>> result = MetaDaoHelper.query("bd.staff.Staff", querySchema_period, ISchemaConstant.MDD_SCHEMA_STAFFCENTER);
        Map<String, Object> map_result = new HashMap<>();
        if (ValueUtils.isNotEmpty(result)) {
            for (Map<String, Object> map_staff : result) {
                if (map_staff.get("name") != null && map_staff.get("staffBankAccount_account") != null) {
                    map_result.put("name", map_staff.get("name"));
                    map_result.put("staffBankAccount", map_staff.get("staffBankAccount"));
                    map_result.put("staffBankAccount_account", map_staff.get("staffBankAccount_account"));
                    map_result.put("id", map_staff.get("id"));
                    map_result.put("code", map_staff.get("code"));
                    map_result.put("dept_name", map_staff.get("dept_name"));
                    map_result.put("dept", map_staff.get("dept"));
                    map_result.put("org_name", map_staff.get("org_name"));
                    map_result.put("org", map_staff.get("org"));
                }
            }
        }
        return map_result;
    }

    private void getPayment(Map<String, Object> map, String accentity, String currency, BankReconciliation bankReconciliation) throws Exception {
        String oriCurrencyName = AppContext.getBean(CurrencyQueryService.class).findById(currency).getName();
        //本币币种
        List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accentity);
        if (accEntity.size() != 0) {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(accEntity.get(0).get("currency").toString());
            if (currencyTenantDTO != null) {
                map.put("natCurrency", currencyTenantDTO.getId());
                map.put("natCurrency_name", currencyTenantDTO.getName());
                map.put("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                map.put("natCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
            }
        } else {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(currency);
            map.put("natCurrency", currencyTenantDTO.getId());
            map.put("natCurrency_name", currencyTenantDTO.getName());
        }
        //付款金额

        //收款银行账户
        map.put("enterprisebankaccount", map.get("bankaccount"));
        map.put("enterprisebankaccount_name", map.get("bankaccount_name"));
        map.put("enterprisebankaccount_account", map.get("bankaccount_account"));

        // 款项类型
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, schema);
        Map<String, Object> condition = new HashMap<String, Object>();
        if (query != null && query.size() > 0 && query.get(0).get("payQuickType") != null) {
            condition.put("id", query.get(0).get("payQuickType"));
            List<Map<String, Object>> payQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
            if (ValueUtils.isNotEmpty(payQuickType)) {
                String quickType_code = String.valueOf(payQuickType.get(0).get("code"));
                if ("1".equals(quickType_code) || "2".equals(quickType_code) || "5".equals(quickType_code) || "6".equals(quickType_code)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101774"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418049A", "款项类型默认值不能为应收应付，请修改款项类型！") /* "款项类型默认值不能为应收应付，请修改款项类型！" */);
                }
                map.put("quickType_name", payQuickType.get(0).get("name"));
                map.put("quickType", payQuickType.get(0).get("id"));
                map.put("quickType_code", payQuickType.get(0).get("code"));
            }

        } else {
            condition.put("code", "9");
            List<Map<String, Object>> quickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);
            if (ValueUtils.isNotEmpty(quickType)) {
                map.put("quickType", quickType.get(0).get("id"));
                map.put("quickType_name", quickType.get(0).get("name"));
                map.put("quickType_code", quickType.get(0).get("code"));
            }
        }


        //汇率类型-----前端js事件 人民币变化是修改汇率类型
        Map<String, Object> defaultExchangeRateType = null;
        defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(accentity);
        if (defaultExchangeRateType.isEmpty() || defaultExchangeRateType.get("id") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101775"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050023", "此资金组织对应会计主体下无默认汇率类型，请检查数据！") /* "此资金组织对应会计主体下无默认汇率类型，请检查数据！" */);
        }
        map.put("exchangeRateType", defaultExchangeRateType.get("id").toString());
        map.put("exchangeRateType_name", null != defaultExchangeRateType.get("name") ? defaultExchangeRateType.get("name").toString() : null);
        //本币金额
        BigDecimal creditamount = BigDecimal.ZERO;
        //收款金额
        map.put(IBussinessConstant.ORI_SUM, map.get("debitamount"));
        //汇率
        if (!StringUtils.isEmpty(accentity) && !StringUtils.isEmpty(currency)) {
            String orgCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(accentity);
            log.info("execute info orgCurrency =======================>" + orgCurrency);
            if (orgCurrency != null) {
                if (currency.equals(orgCurrency)) {
                    map.put("exchRate", BigDecimal.valueOf(1));
                    if (null != map.get(IBussinessConstant.ORI_SUM)) {
                        creditamount = new BigDecimal(map.get(IBussinessConstant.ORI_SUM).toString());
                        map.put("natSum", creditamount.multiply(BigDecimal.valueOf(1)));
                    }
                } else {
                    if (bankReconciliation.getTran_date() == null) bankReconciliation.setTran_date(new Date());
                    if (defaultExchangeRateType != null && defaultExchangeRateType.get("id") != null) {
                        try {
                            Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, defaultExchangeRateType.get("id").toString(), currency, orgCurrency, bankReconciliation.getTran_date(), 6);
                            if (currencyRateNew == null || currencyRateNew == 0) {
                                throw new CtmException(String.format(IMultilangConstant.noRateStringError /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,
                                        map.get("exchangeRateType_name"), oriCurrencyName, map.get("natCurrency_name")));
                            }
                            map.put("exchRate", BigDecimal.valueOf(currencyRateNew));
                            if (null != map.get(IBussinessConstant.ORI_SUM)) {
                                creditamount = new BigDecimal(map.get(IBussinessConstant.ORI_SUM).toString());
                                map.put("natSum", creditamount.multiply(BigDecimal.valueOf(currencyRateNew)));
                            }
                        } catch (Exception e) {
                            log.error("==================》取不到汇率", e);
                            throw new CtmException(String.format(IMultilangConstant.noRateStringError /* "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！" */,
                                    map.get("exchangeRateType_name"), oriCurrencyName, map.get("natCurrency_name")));
                        }
                    }
                }
            }
        }
        //余额
        map.put("balance", map.get("acct_bal"));
        //单据日期
        Date vouchdate = DateUtils.strToDate(map.get("tran_date").toString());
        map.put("vouchdate", vouchdate);
        Date maxSettleDate = settlementService.getMaxSettleDate(accentity);
        if (maxSettleDate != null) {
            if (maxSettleDate.compareTo(vouchdate) >= 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101776"), com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153370") /* "单据日期已日结，不能保存单据！" */);
            }
        }
        //备注
        map.put("description", map.get("remark"));

        //结算方式
        QuerySchema querySchema = QuerySchema.create().addSelect("settlemode,settlemode.name as settlemodeName ");
        querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(accentity)));
        List<Map<String, Object>> autoConfigList = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, querySchema);
        if (CollectionUtils.isEmpty(autoConfigList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101777"), MessageUtils.getMessage("P_YS_CTM_CM-BE_0001587833") /* "未配置现金参数，请配置" */);
        }
        if (autoConfigList.get(0).get("settlemode") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101778"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180497", "自动化参数未配置结算方式") /* "自动化参数未配置结算方式" */);
        }
        map.put("settlemode", autoConfigList.get(0).get("settlemode"));
        map.put("settlemode_name", autoConfigList.get(0).get("settlemodeName"));


        //事项类型
        map.put("billtype", EventType.CashMark.getValue());
        //不是自动生单 autobill
        map.put("autobill", false);
        //来源单据id
        map.put("srcbillid", map.get("id"));
    }

    /**
     * 查询客户
     *
     * @param bankReconciliation
     * @return
     */
    private static CtmJSONObject getCustomer(BankReconciliation bankReconciliation) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
        String accentity = bankReconciliation.getAccentity();
        if (StringUtils.isEmpty(bankReconciliation.getTo_acct_name()) || StringUtils.isEmpty(bankReconciliation.getTo_acct_no())) {
            return resultObj;
        }
        MerchantRequst requst = new MerchantRequst(accentity, bankReconciliation.getTo_acct_name(), bankReconciliation.getTo_acct_no());
        CtmJSONObject cust2Check = MerchantUtils.cust2Check(requst);
        if (Objects.equals(MerchantConstant.TRUE, cust2Check.getString(MerchantConstant.CUSTOMERFLAG))) {
            if (Objects.equals(true, cust2Check.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101779"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00040", "当前客户档案已停用") /* "当前客户档案已停用" */);
            }
            // 客户名称和账号都存在
            resultObj.put("type", CaObject.Customer.getValue());
            resultObj.put("customerId", cust2Check.getLong(MerchantConstant.CUSTOMERID));
            resultObj.put("customerbankId", cust2Check.getLong(MerchantConstant.CUSTOMERBANKID));
            resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.TRUE);
//            resultObj.put("specialManagementDep", cust2Check.get("specialManagementDep"));
//            resultObj.put("cSpecialManagementDepName", cust2Check.get("cSpecialManagementDepName"));
//            resultObj.put("cProfessSalesmanId", cust2Check.get("cProfessSalesmanId"));
//            resultObj.put("cProfessSalesmanName", cust2Check.get("cProfessSalesmanName"));
            resultObj.put("orgId", cust2Check.getString("orgId"));
//            resultObj.put("orgName", cust2Check.getString("orgName"));
            resultObj.put("name", cust2Check.getString("name"));
            return resultObj;
        }
        return resultObj;
    }

    /**
     * 查询供应商
     *
     * @param bankReconciliation
     * @return
     */
    private static CtmJSONObject getSupplier(BankReconciliation bankReconciliation) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        String accentity = bankReconciliation.getAccentity();
        resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        if (StringUtils.isEmpty(bankReconciliation.getTo_acct_name()) || StringUtils.isEmpty(bankReconciliation.getTo_acct_no())) {
            return resultObj;
        }

        // 供应商判断
        MerchantRequst requst = new MerchantRequst(accentity, bankReconciliation.getTo_acct_name(), bankReconciliation.getTo_acct_no());
        CtmJSONObject vendor2Check = MerchantUtils.vendor2Check(requst);
        if (Objects.equals(MerchantConstant.TRUE, vendor2Check.getString(MerchantConstant.VENDORFLAG))) {
            if (Objects.equals(true, vendor2Check.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101780"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00041", "当前供应商档案已停用") /* "当前供应商档案已停用" */);
            }
            // 供应商名称和账号都存在
            resultObj.put("type", CaObject.Supplier.getValue());
            resultObj.put("supplierId", vendor2Check.getLong(MerchantConstant.VENDORID));
            resultObj.put("supplierbankId", vendor2Check.getLong(MerchantConstant.VENDORBANKID));
            resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.TRUE);
//            resultObj.put("iDepartmentId", vendor2Check.get("iDepartmentId"));
//            resultObj.put("iDepartmentIdName", vendor2Check.get("iDepartmentIdName"));
//            resultObj.put("iPersonId", vendor2Check.get("iPersonId"));
//            resultObj.put("iPersonIdName", vendor2Check.get("iPersonIdName"));
            resultObj.put("orgId", vendor2Check.getString("orgId"));
//            resultObj.put("orgName", vendor2Check.getString("orgName"));
            resultObj.put("name", vendor2Check.getString("name"));
            return resultObj;
        }
        if (Objects.equals(MerchantConstant.FALSE, vendor2Check.getString(MerchantConstant.VENDORFLAG))) {
            resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        }
        return resultObj;
    }

}
