package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationPullService;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.ISchemaConstant;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.util.MerchantRequst;
import com.yonyoucloud.fi.cmp.util.MerchantUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author shangxd
 * @date 2022/4/26 16:46
 * @describe
 */
@Slf4j
@Service
public class BankreconciliationPullServiceImpl implements BankreconciliationPullService {
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * 查询客户
     *
     * @param
     * @return
     */
    @Override
    public CtmJSONObject getCustomer(String accentity,  String toAcctNo) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
        if (StringUtils.isEmpty(accentity) || StringUtils.isEmpty(toAcctNo)  ) {
            return resultObj;
        }
        return doGetCustomer(new MerchantRequst(accentity, toAcctNo));
    }

    /**
     * 查询供应商
     *
     * @param
     * @return
     */
    @Override
    public CtmJSONObject getSupplier(String accentity,  String toAcctNo) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        if (StringUtils.isEmpty(accentity) || StringUtils.isEmpty(toAcctNo) ) {
            return resultObj;
        }
        // 供应商判断
        return doGetSupplier(new MerchantRequst(accentity, toAcctNo));
    }

    /**
     * 查询供应商
     *
     * @param
     * @return
     */
    @Override
    public CtmJSONObject getSupplierByAccName(String accentity, String toAcctName) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        if (StringUtils.isEmpty(accentity) || StringUtils.isEmpty(toAcctName)  ) {
            return resultObj;
        }

        // 供应商判断
        return doGetSupplier(new MerchantRequst(accentity, toAcctName,null));
    }

    /**
     * 智能分类查询内部员工
     * 按照银行账号，币种查询
     */
    @Override
    public Map<String, Object> getInnerEmployee(Object toAcctNo, Object accentity) throws Exception {
        QuerySchema querySchema_period = new QuerySchema();
        // 返回字段用于获取对应关系,需要啥加啥
        querySchema_period.addSelect(
                "bankAcctList.id as staffBankAccount, bankAcctList.account as staffBankAccount_account, bankAcctList.currency as staffBankAccount_currency ," +
                        "name, id, code, mainJobList.dept_id as dept, mainJobList.dept_id.name as dept_name, " +
                        "mainJobList.org_id as org, mainJobList.org_id.name as org_name");
        // 对应的员工姓名、银行账号
        querySchema_period.appendQueryCondition(QueryCondition.name("bankAcctList.account").eq(toAcctNo));
        querySchema_period.appendQueryCondition(QueryCondition.name("bankAcctList.dr").eq(0));
        //内部员工要在本会计主体下
        //设置yms 参数，如果是精准匹配=true，根据账号没有匹配到供应商或者客户信息，不用拿名称再次匹配匹配;
        // 设置根据账号没有匹配到供应商，客户，员工信息，要根本组织匹配；（大北农）
        Set<String> orgSet = MerchantUtils.getOrgByAccentity(String.valueOf(accentity));
        querySchema_period.appendQueryCondition(QueryCondition.name("mainJobList.org_id").in(orgSet.toArray()));
        //选择可用的
        querySchema_period.appendQueryCondition(QueryCondition.name("enable").eq(1));
        List<Map<String,Object>> result = MetaDaoHelper.query("bd.staff.Staff",querySchema_period, ISchemaConstant.MDD_SCHEMA_STAFFCENTER);/* 暂不修改 已登记*/
        Map<String,Object> map_result = new HashMap<>();
        if (ValueUtils.isNotEmpty(result)){
            for (Map<String,Object> map_staff:result){
                if (map_staff.get("name")!=null) {
                    map_result.put("name",map_staff.get("name"));
                    map_result.put("staffBankAccount",map_staff.get("staffBankAccount"));
                    map_result.put("staffBankAccount_account",map_staff.get("staffBankAccount_account"));
                    map_result.put("id",map_staff.get("id"));
                    map_result.put("code",map_staff.get("code"));
                    map_result.put("dept_name",map_staff.get("dept_name"));
                    map_result.put("dept",map_staff.get("dept"));
                    map_result.put("org_name",map_staff.get("org_name"));
                    map_result.put("org",map_staff.get("org"));
                }
            }
        }
        return map_result;
    }
    /**
     * 智能分类查询内部员工
     * 按照银行账号，币种查询
     */
    @Override
    public Map<String, Object> getInnerEmployeeForCheck(Object toAcctNo, Object accentity) throws Exception {
        QuerySchema querySchema_period = new QuerySchema();
        // 返回字段用于获取对应关系,需要啥加啥
        querySchema_period.addSelect(
                "bankAcctList.id as staffBankAccount, bankAcctList.account as staffBankAccount_account, bankAcctList.currency as staffBankAccount_currency ," +
                        "name, id, code, mainJobList.dept_id as dept, mainJobList.dept_id.name as dept_name, " +
                        "mainJobList.org_id as org, mainJobList.org_id.name as org_name");
        // 对应的员工姓名、银行账号
        querySchema_period.appendQueryCondition(QueryCondition.name("bankAcctList.account").eq(toAcctNo));
        querySchema_period.appendQueryCondition(QueryCondition.name("bankAcctList.dr").eq(0));
        //内部员工要在本会计主体下
        //设置yms 参数，如果是精准匹配=true，根据账号没有匹配到供应商或者客户信息，不用拿名称再次匹配匹配;
        // 设置根据账号没有匹配到供应商，客户，员工信息，要根本组织匹配；（大北农）
        Boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
        if (isAccuratematching.booleanValue()) {
            querySchema_period.appendQueryCondition(QueryCondition.name("mainJobList.org_id").eq(accentity));
        }
        //选择可用的
        querySchema_period.appendQueryCondition(QueryCondition.name("enable").eq(1));
        List<Map<String,Object>> result = MetaDaoHelper.query("bd.staff.Staff",querySchema_period, ISchemaConstant.MDD_SCHEMA_STAFFCENTER);/* 暂不修改 已登记*/
        Map<String,Object> map_result = new HashMap<>();
        if (ValueUtils.isNotEmpty(result)){
            for (Map<String,Object> map_staff:result){
                if (map_staff.get("name")!=null) {
                    map_result.put("name",map_staff.get("name"));
                    map_result.put("staffBankAccount",map_staff.get("staffBankAccount"));
                    map_result.put("staffBankAccount_account",map_staff.get("staffBankAccount_account"));
                    map_result.put("id",map_staff.get("id"));
                    map_result.put("code",map_staff.get("code"));
                    map_result.put("dept_name",map_staff.get("dept_name"));
                    map_result.put("dept",map_staff.get("dept"));
                    map_result.put("org_name",map_staff.get("org_name"));
                    map_result.put("org",map_staff.get("org"));
                }
            }
        }
        return map_result;
    }
    /**
     * 根据对方银行账号和币种匹配企业银行账户
     * @param account 对方银行账号
     * @return 企业银行账户
     * @throws Exception
     */
    @Override
    public Map<String, Object> getBankAcctByAccount(String account) throws Exception {
        BillContext billContextFinBank = new BillContext();
        billContextFinBank.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContextFinBank.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QueryConditionGroup groupBank = QueryConditionGroup.and(
                QueryCondition.name("account").eq(account),
                QueryCondition.name("enable").eq(1),
                QueryCondition.name("dr").in(new String[]{"0", "2"})
        );
        QuerySchema querySchemaEnterprise = QuerySchema.create().addSelect("id as bankacct,orgid,orgid.name as orgname,code,name,account,bank,bankNumber,lineNumber,currencyList.currency as currency,enable,acctType").addCondition(groupBank);
        List<Map<String, Object>> dataList = MetaDaoHelper.query(billContextFinBank,querySchemaEnterprise, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
        if (CollectionUtils.isEmpty(dataList)) {
            return null;
        }
        for (Map<String, Object> map:dataList) {
            if (account != null && account.equals(map.get("account"))) {
                return map;
            }
        }
        return null;
    }

    /**
     * 查询客户
     *
     * @param
     * @return
     */
    @Override
    public CtmJSONObject getCustomerByName(String accentity, String toAcctName) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
        if (StringUtils.isEmpty(accentity) || StringUtils.isEmpty(toAcctName) ) {
            return resultObj;
        }
        // start  wangdengk 20230731 根据银行账户名称查询银行账户 如果没有查询到直接返回
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("stopstatus", "0");
        paramMap.put("bankAccountName", toAcctName);
        List<Map<String, Object>> custBankList = QueryBaseDocUtils.queryCustomerBankAccountByCondition(paramMap);
        // 如果银行账户名称为空 直接返回
        if (CollectionUtils.isEmpty(custBankList)) {
            return resultObj;
        }
        Map<String, Object> custBankMap = custBankList.get(0);
        String accNo = custBankMap.get("bankAccount") == null ? "" : custBankMap.get("bankAccount").toString();
        if(StringUtils.isEmpty(accNo)){
            return resultObj;
        }
        MerchantRequst requst = new MerchantRequst(accentity, toAcctName, accNo);
        // end  wangdengk 20230731 根据银行账户名称查询银行账户 如果没有查询到直接返回
        CtmJSONObject custCheck = MerchantUtils.cust2CheckByName(requst);
        if (Objects.equals(MerchantConstant.TRUE, custCheck.getString(MerchantConstant.CUSTOMERFLAG))) {
            if (Objects.equals(true, custCheck.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101149"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180061","当前客户档案已停用") /* "当前客户档案已停用" */);
            }
            // 客户Id
            resultObj.put(MerchantConstant.CUSTOMERID, custCheck.getString(MerchantConstant.CUSTOMERID));
            // 客户名称
            resultObj.put(ICmpConstant.NAME, custCheck.get(ICmpConstant.NAME));
            // 客户银行账户Id
            resultObj.put(MerchantConstant.CUSTOMERBANKID, custCheck.get(MerchantConstant.CUSTOMERBANKID));
            // 客户银行账户名称
            resultObj.put(MerchantConstant.BANKACCOUNTNAME, custCheck.get(MerchantConstant.BANKACCOUNTNAME));
            // 客户银行账号
            resultObj.put(MerchantConstant.BANKACCOUNT, custCheck.get(MerchantConstant.BANKACCOUNT));
            // 客户银行联行号
            resultObj.put(ICmpConstant.OPPOSITEBANKLINENO, custCheck.get(MerchantConstant.JOINTLINENO));
            // 客户银行网点
            resultObj.put(ICmpConstant.OPPOSITEBANKADDR, custCheck.get(MerchantConstant.OPENBANK));
            // 客户标识
            resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.TRUE);
        }
        return resultObj;
    }


    /**
     * 智能分类查询内部员工
     * 按照对方户名，币种查询
     */
    @Override
    public Map<String, Object> getInnerEmployeeByName(Object toAcctName, Object accentity) throws Exception {
        QuerySchema querySchema_period = new QuerySchema();
        // 返回字段用于获取对应关系,需要啥加啥
        querySchema_period.addSelect(
                "bankAcctList.id as staffBankAccount, bankAcctList.account as staffBankAccount_account, bankAcctList.currency as staffBankAccount_currency ," +
                        "bankAcctList.dr as dr, name, id, code, mainJobList.dept_id as dept, mainJobList.dept_id.name as dept_name, " +
                        "mainJobList.org_id as org, mainJobList.org_id.name as org_name");
        // 对应的员工姓名、银行账号
        querySchema_period.appendQueryCondition(QueryCondition.name("name").eq(toAcctName));
        //内部员工要在本会计主体下
        querySchema_period.appendQueryCondition(QueryCondition.name("mainJobList.org_id").eq(accentity));
        //选择可用的
        querySchema_period.appendQueryCondition(QueryCondition.name("enable").eq(1));

        List<Map<String, Object>> result = MetaDaoHelper.query("bd.staff.Staff", querySchema_period, ISchemaConstant.MDD_SCHEMA_STAFFCENTER);
        Map<String, Object> map_result = new HashMap<>();
        if (ValueUtils.isNotEmpty(result)) {
            for (Map<String, Object> map_staff : result) {
                if (map_staff.get("name") != null) {
                    map_result.put("name", map_staff.get("name"));
                    // 户名辨识出去掉对方账户Id
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

    /**
     * 智能分类查询内部员工
     * 按照对方户名，币种查询
     */
    @Override
    public Map<String, Object> getInnerEmployeeByNameForCheck(Object toAcctName, Object accentity) throws Exception {
        QuerySchema querySchema_period = new QuerySchema();
        // 返回字段用于获取对应关系,需要啥加啥
        querySchema_period.addSelect(
                "bankAcctList.id as staffBankAccount, bankAcctList.account as staffBankAccount_account, bankAcctList.currency as staffBankAccount_currency ," +
                        "bankAcctList.dr as dr, name, id, code, mainJobList.dept_id as dept, mainJobList.dept_id.name as dept_name, " +
                        "mainJobList.org_id as org, mainJobList.org_id.name as org_name");
        // 对应的员工姓名、银行账号
        querySchema_period.appendQueryCondition(QueryCondition.name("name").eq(toAcctName));
        //内部员工要在本会计主体下
        querySchema_period.appendQueryCondition(QueryCondition.name("mainJobList.org_id").eq(accentity));
        //选择可用的
        querySchema_period.appendQueryCondition(QueryCondition.name("enable").eq(1));

        List<Map<String, Object>> result = MetaDaoHelper.query("bd.staff.Staff", querySchema_period, ISchemaConstant.MDD_SCHEMA_STAFFCENTER);
        Map<String, Object> map_result = new HashMap<>();
        if (ValueUtils.isNotEmpty(result)) {
            for (Map<String, Object> map_staff : result) {
                if (map_staff.get("name") != null) {
                    map_result.put("name", map_staff.get("name"));
                    // 户名辨识出去掉对方账户Id
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

    @Override
    public Map<String, Object> getInnerEmployeeByAccountNameForCheck(Object toAcctName, Object accentity) throws Exception {
        QuerySchema querySchema_period = new QuerySchema();
        // 返回字段用于获取对应关系,需要啥加啥
        querySchema_period.addSelect(
                "bankAcctList.id as staffBankAccount, bankAcctList.account as staffBankAccount_account, bankAcctList.currency as staffBankAccount_currency ," +
                        "name, id, code, mainJobList.dept_id as dept, mainJobList.dept_id.name as dept_name, " +
                        "mainJobList.org_id as org, mainJobList.org_id.name as org_name");
        // 对应的员工姓名、银行账号
        querySchema_period.appendQueryCondition(QueryCondition.name("bankAcctList.accountname").eq(toAcctName));
        querySchema_period.appendQueryCondition(QueryCondition.name("bankAcctList.dr").eq(0));
        //内部员工要在本会计主体下
        //设置yms 参数，如果是精准匹配=true，根据账号没有匹配到供应商或者客户信息，不用拿名称再次匹配匹配;
        // 设置根据账号没有匹配到供应商，客户，员工信息，要根本组织匹配；（大北农）
        Boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
        if (isAccuratematching.booleanValue()) {
            querySchema_period.appendQueryCondition(QueryCondition.name("mainJobList.org_id").eq(accentity));
        }
        //选择可用的
        querySchema_period.appendQueryCondition(QueryCondition.name("enable").eq(1));
        List<Map<String,Object>> result = MetaDaoHelper.query("bd.staff.Staff",querySchema_period, ISchemaConstant.MDD_SCHEMA_STAFFCENTER);/* 暂不修改 已登记*/
        Map<String,Object> map_result = new HashMap<>();
        if (ValueUtils.isNotEmpty(result)){
            for (Map<String,Object> map_staff:result){
                if (map_staff.get("name")!=null) {
                    map_result.put("name",map_staff.get("name"));
                    map_result.put("staffBankAccount",map_staff.get("staffBankAccount"));
                    map_result.put("staffBankAccount_account",map_staff.get("staffBankAccount_account"));
                    map_result.put("id",map_staff.get("id"));
                    map_result.put("code",map_staff.get("code"));
                    map_result.put("dept_name",map_staff.get("dept_name"));
                    map_result.put("dept",map_staff.get("dept"));
                    map_result.put("org_name",map_staff.get("org_name"));
                    map_result.put("org",map_staff.get("org"));
                }
            }
        }
        return map_result;
    }

    @Override
    public Map<String, Object> getBankAcctByAccountByName(String accountName, String currency) throws Exception {
        BillContext billContextFinBank = new BillContext();
        billContextFinBank.setFullname("bd.enterprise.OrgFinBankacctVO");
        billContextFinBank.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);/* 暂不修改 已登记*/
        QueryConditionGroup groupBank = QueryConditionGroup.and(
                QueryCondition.name("name").eq(accountName),
                QueryCondition.name("currencyList.currency").eq(currency),
                QueryCondition.name("dr").in(new String[]{"0", "2"}),
                QueryCondition.name("enable").eq(1)
//                ,//未删除的
//                QueryCondition.name("tenant").eq(AppContext.getYTenantId())
        );
        QuerySchema querySchemaEnterprise = QuerySchema.create().addSelect("id as bankacct,orgid,orgid.name as orgname,code,name,account,bank,bankNumber,lineNumber,currencyList.currency as currency,enable,acctType").addCondition(groupBank);
        List<Map<String, Object>> dataList = MetaDaoHelper.query(billContextFinBank, querySchemaEnterprise, ISchemaConstant.MDD_SCHEMA_UCFBASEDOC);
        for (Map<String, Object> map : dataList) {
            if (accountName != null && currency != null && accountName.equals(map.get("name")) && currency.equals(map.get("currency"))) {
                return map;
            }
        }
        return null;
    }

    /**
     * 根据银行账号和账户名称获取客户信息
     *
     * @param accentity  会计主体
     * @param toAcctNo   对方银行账号
     * @param toAcctName 对方账户名称
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject getCustomer(String accentity, String toAcctNo, String toAcctName) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
        if (StringUtils.isEmpty(accentity) || StringUtils.isEmpty(toAcctNo) || StringUtils.isEmpty(toAcctName)  ) {
            return resultObj;
        }
        return doGetCustomer(new MerchantRequst(accentity, toAcctName, toAcctNo));
    }

    @Override
    public CtmJSONObject getCustomerForCheck(String accentity, String toAcctNo, String toAcctName,String mark) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
        if ((StringUtils.isEmpty(accentity) || StringUtils.isEmpty(toAcctNo) || StringUtils.isEmpty(toAcctName)) && "1".equals( mark)) {
            return resultObj;
        }
        return doGetCustomerForCheck(new MerchantRequst(accentity, toAcctName, toAcctNo),mark);
    }

    /**
     * 根据银行账号和账户名称获取供应商信息
     *
     * @param accentity  会计主体
     * @param toAcctNo   对方银行账号
     * @param toAcctName 对方账户名称
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject getSupplier(String accentity, String toAcctNo, String toAcctName) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 供应商判断
        resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        if (StringUtils.isEmpty(accentity) || StringUtils.isEmpty(toAcctNo) || StringUtils.isEmpty(toAcctName) ) {
            return resultObj;
        }
        return doGetSupplier(new MerchantRequst(accentity, toAcctName, toAcctNo));
    }

    /**
     * 根据银行账号和账户名称获取供应商信息
     *
     * @param accentity  会计主体
     * @param toAcctNo   对方银行账号
     * @param toAcctName 对方账户名称
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject getSupplierForCheck(String accentity, String toAcctNo, String toAcctName, String mark) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 供应商判断
        resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        if ((StringUtils.isEmpty(accentity) || StringUtils.isEmpty(toAcctNo) || StringUtils.isEmpty(toAcctName)) && "1".equals(mark) ) {
            return resultObj;
        }
        return doGetSupplierForCheck(new MerchantRequst(accentity, toAcctName, toAcctNo),mark);
    }

    /**
     * 根据银行编号和账户名称获取供应商信息
     * @param request
     * @return
     */
    private CtmJSONObject doGetSupplier(MerchantRequst request) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        CtmJSONObject vendorCheck;
        // 供应商判断
        if (StringUtils.isEmpty(request.getAccNo())) {
            vendorCheck = MerchantUtils.vendor2CheckByName(request);
        } else {
            vendorCheck = MerchantUtils.vendor2Check(request);
        }
        if (Objects.equals(MerchantConstant.TRUE, vendorCheck.getString(MerchantConstant.VENDORFLAG))) {
            if (Objects.equals(true, vendorCheck.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101150"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180062","当前供应商档案已停用") /* "当前供应商档案已停用" */);
            }
            // 供应商名称和账号都存在
            resultObj.put("caobject", CaObject.Supplier.getValue());
            // 付款单位名称  --参照ID
            resultObj.put("supplierId", vendorCheck.getLong(MerchantConstant.VENDORID));
            // 付款单位名称  --参照name
            resultObj.put(ICmpConstant.NAME, vendorCheck.get(ICmpConstant.NAME));
            // 付款方账户名称  --参照ID
            resultObj.put(MerchantConstant.SUPPLIERBANKID, vendorCheck.getLong(MerchantConstant.VENDORBANKID));
            // 付款方账户名称  --参照name
            resultObj.put(MerchantConstant.ACCOUNTNAME , vendorCheck.get(MerchantConstant.ACCOUNTNAME));
            // 付款方账户id
            resultObj.put(MerchantConstant.ACCOUNT , vendorCheck.get(MerchantConstant.ACCOUNT));
            // 表体.付款方银行联行号
            resultObj.put("oppositebanklineno", vendorCheck.get(MerchantConstant.CORRESPONDENTCODE));
            // 付款方银行网点
            resultObj.put("oppositebankaddr", vendorCheck.get(MerchantConstant.OPENBANK));
            resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.TRUE);
            return resultObj;
        }
        if (Objects.equals(MerchantConstant.FALSE, vendorCheck.getString(MerchantConstant.VENDORFLAG))) {
            resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        }
        // 匹配到多个时，按照【其他】处理，不再进行匹配
        if (Objects.equals(MerchantConstant.TRUE, vendorCheck.getString(MerchantConstant.OTHERFLAG))) {
            resultObj.put(MerchantConstant.OTHERFLAG, MerchantConstant.TRUE);
        }
        return resultObj;
    }

    /**
     * 根据银行编号和账户名称获取供应商信息
     * @param request
     * @return
     */
    private CtmJSONObject doGetSupplierForCheck(MerchantRequst request,String mark) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        CtmJSONObject vendorCheck;
        // 供应商判断
        vendorCheck = MerchantUtils.vendor2CheckForCheck(request,mark);
        if (Objects.equals(MerchantConstant.TRUE, vendorCheck.getString(MerchantConstant.VENDORFLAG))) {
            if (Objects.equals(true, vendorCheck.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101150"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180062","当前供应商档案已停用") /* "当前供应商档案已停用" */);
            }
            // 供应商名称和账号都存在
            resultObj.put("caobject", CaObject.Supplier.getValue());
            // 付款单位名称  --参照ID
            resultObj.put("supplierId", vendorCheck.getLong(MerchantConstant.VENDORID));
            // 付款单位名称  --参照name
            resultObj.put(ICmpConstant.NAME, vendorCheck.get(ICmpConstant.NAME));
            // 付款方账户名称  --参照ID
            resultObj.put(MerchantConstant.SUPPLIERBANKID, vendorCheck.getLong(MerchantConstant.VENDORBANKID));
            // 付款方账户名称  --参照name
            resultObj.put(MerchantConstant.ACCOUNTNAME , vendorCheck.get(MerchantConstant.ACCOUNTNAME));
            // 付款方账户id
            resultObj.put(MerchantConstant.ACCOUNT , vendorCheck.get(MerchantConstant.ACCOUNT));
            // 表体.付款方银行联行号
            resultObj.put("oppositebanklineno", vendorCheck.get(MerchantConstant.CORRESPONDENTCODE));
            // 付款方银行网点
            resultObj.put("oppositebankaddr", vendorCheck.get(MerchantConstant.OPENBANK));
            resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.TRUE);
            return resultObj;
        }
        if (Objects.equals(MerchantConstant.FALSE, vendorCheck.getString(MerchantConstant.VENDORFLAG))) {
            resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        }
        // 匹配到多个时，按照【其他】处理，不再进行匹配
        if (Objects.equals(MerchantConstant.TRUE, vendorCheck.getString(MerchantConstant.OTHERFLAG))) {
            resultObj.put(MerchantConstant.OTHERFLAG, MerchantConstant.TRUE);
        }
        return resultObj;
    }

    /**
     * 获取客户信息
     * @param request
     * @return
     * @throws Exception
     */
    private CtmJSONObject doGetCustomer(MerchantRequst request) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        CtmJSONObject custCheck;
        if (StringUtils.isEmpty(request.getAccNo())) {
            custCheck = MerchantUtils.cust2CheckByName(request);
        } else {
            custCheck = MerchantUtils.cust2Check(request);
        }
        if (Objects.equals(MerchantConstant.TRUE, custCheck.getString(MerchantConstant.CUSTOMERFLAG))) {
            if (Objects.equals(true, custCheck.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101149"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180061","当前客户档案已停用") /* "当前客户档案已停用" */);
            }
            // 付款单位名称  --参照id
            resultObj.put(MerchantConstant.CUSTOMERID, custCheck.getString(MerchantConstant.CUSTOMERID));
            // 付款单位名称  --参照name
            resultObj.put(ICmpConstant.NAME, custCheck.get(ICmpConstant.NAME));
            // 付款方账户名称  --参照id
            resultObj.put(MerchantConstant.CUSTOMERBANKID , custCheck.get(MerchantConstant.CUSTOMERBANKID));
            // 付款方账户名称  --参照name
            resultObj.put(MerchantConstant.BANKACCOUNTNAME , custCheck.get(MerchantConstant.BANKACCOUNTNAME));
            // 付款方银行账号
            resultObj.put(MerchantConstant.BANKACCOUNT, custCheck.get(MerchantConstant.BANKACCOUNT));
            // 表体.付款方银行联行号
            resultObj.put(ICmpConstant.OPPOSITEBANKLINENO, custCheck.get(MerchantConstant.JOINTLINENO));
            // 付款方银行网点
            resultObj.put(ICmpConstant.OPPOSITEBANKADDR, custCheck.get(MerchantConstant.OPENBANK));
            // 失败成功标识
            resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.TRUE);
        }
        if (Objects.equals(MerchantConstant.FALSE, custCheck.getString(MerchantConstant.CUSTOMERFLAG))) {
            resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
        }
        // 匹配到多个时，按照【其他】处理，不再进行匹配
        if (Objects.equals(MerchantConstant.TRUE, custCheck.getString(MerchantConstant.OTHERFLAG))) {
            resultObj.put(MerchantConstant.OTHERFLAG, MerchantConstant.TRUE);
        }
        return resultObj;
    }

    private CtmJSONObject doGetCustomerForCheck(MerchantRequst request,String mark) throws Exception{
        CtmJSONObject resultObj = new CtmJSONObject();
        CtmJSONObject custCheck = MerchantUtils.cust2CheckForCheck(request,mark);
        if (Objects.equals(MerchantConstant.TRUE, custCheck.getString(MerchantConstant.CUSTOMERFLAG))) {
            if (Objects.equals(true, custCheck.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101149"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180061","当前客户档案已停用") /* "当前客户档案已停用" */);
            }
            // 付款单位名称  --参照id
            resultObj.put(MerchantConstant.CUSTOMERID, custCheck.getString(MerchantConstant.CUSTOMERID));
            // 付款单位名称  --参照name
            resultObj.put(ICmpConstant.NAME, custCheck.get(ICmpConstant.NAME));
            // 付款方账户名称  --参照id
            resultObj.put(MerchantConstant.CUSTOMERBANKID , custCheck.get(MerchantConstant.CUSTOMERBANKID));
            // 付款方账户名称  --参照name
            resultObj.put(MerchantConstant.BANKACCOUNTNAME , custCheck.get(MerchantConstant.BANKACCOUNTNAME));
            // 付款方银行账号
            resultObj.put(MerchantConstant.BANKACCOUNT, custCheck.get(MerchantConstant.BANKACCOUNT));
            // 表体.付款方银行联行号
            resultObj.put(ICmpConstant.OPPOSITEBANKLINENO, custCheck.get(MerchantConstant.JOINTLINENO));
            // 付款方银行网点
            resultObj.put(ICmpConstant.OPPOSITEBANKADDR, custCheck.get(MerchantConstant.OPENBANK));
            // 失败成功标识
            resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.TRUE);
        }
        if (Objects.equals(MerchantConstant.FALSE, custCheck.getString(MerchantConstant.CUSTOMERFLAG))) {
            resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
        }
        // 匹配到多个时，按照【其他】处理，不再进行匹配
        if (Objects.equals(MerchantConstant.TRUE, custCheck.getString(MerchantConstant.OTHERFLAG))) {
            resultObj.put(MerchantConstant.OTHERFLAG, MerchantConstant.TRUE);
        }
        return resultObj;
    }
}
