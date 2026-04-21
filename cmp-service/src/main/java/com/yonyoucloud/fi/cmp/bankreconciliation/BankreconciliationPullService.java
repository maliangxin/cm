package com.yonyoucloud.fi.cmp.bankreconciliation;



import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;
import java.util.Map;

public interface BankreconciliationPullService {
    CtmJSONObject getCustomer(String accentity, String toAcctNo) throws Exception;

    /**
     * 根据银行账号和账户名称获取客户信息
     * @param accentity 会计主体
     * @param toAcctNo 对方银行账号
     * @param toAcctName 对方账户名称
     * @return
     * @throws Exception
     */
    CtmJSONObject getCustomer(String accentity, String toAcctNo, String toAcctName) throws  Exception;

    CtmJSONObject getCustomerForCheck(String accentity, String toAcctNo, String toAcctName,String mark) throws  Exception;

    CtmJSONObject getSupplier(String accentity,  String toAcctNo) throws Exception;

    /**
     * 根据银行账号和账户名称获取供应商信息
     * @param accentity 会计主体
     * @param toAcctNo 对方银行账号
     * @param toAcctName 对方账户名称
     * @return
     * @throws Exception
     */
    CtmJSONObject getSupplier(String accentity, String toAcctNo, String toAcctName) throws  Exception;

    CtmJSONObject getSupplierForCheck(String accentity, String toAcctNo, String toAcctName,String mark) throws  Exception;

    CtmJSONObject getSupplierByAccName(String accentity,  String toAcctName) throws Exception;

    /**
     * 智能分类查询内部员工
     * 按照银行账号，币种查询
     */
    Map<String,Object> getInnerEmployee(Object toAcctNo, Object accentity) throws Exception;
    Map<String,Object> getInnerEmployeeForCheck(Object toAcctNo, Object accentity) throws Exception;
    /**
     * 根据银行账号查询企业银行信息
     */
    Map<String,Object> getBankAcctByAccount(String account) throws Exception;

    CtmJSONObject getCustomerByName(String accentity, String toAcctName) throws Exception;
    /**
     * 智能分类查询内部员工
     * 按照银行账号，币种查询
     */
    Map<String, Object> getInnerEmployeeByName(Object toAcctName, Object accentity) throws Exception;
    /**
     * 智能分类查询内部员工
     * 按照银行账号，币种查询
     */
    Map<String, Object> getInnerEmployeeByNameForCheck(Object toAcctName,  Object accentity) throws Exception;

    /**
     * 智能分类查询内部员工
     * 按照银行账号，币种查询
     */
    Map<String, Object> getInnerEmployeeByAccountNameForCheck(Object toAcctName, Object accentity) throws Exception;
    /**
     * 根据对方户名,币种查询企业银行信息
     */
    Map<String, Object> getBankAcctByAccountByName(String accountName, String currency) throws Exception;


}
