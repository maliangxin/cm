package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.util.TypeUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @ClassName PaymentCaObjectCheckRule
 * @Description
 * @Author tongyd
 * @Date 2019/7/12 16:30
 * @Version 1.0
 **/
@Component
public class PaymentCaObjectCheckRule  extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bill = (BizObject) getBills(billContext, paramMap).get(0);
        Short caObject = TypeUtils.castToShort(bill.get("caobject"));
        bill.set("org",bill.get(IBussinessConstant.ACCENTITY));
        bill.set("org_name",bill.get("accentity_name"));
        if (caObject.equals(CaObject.Supplier.getValue())) {
            bill.set("customer_name", null);
            bill.set("customerbankaccount", null);
            bill.set("customerbankaccount_bankAccountName", null);
            bill.set("employee", null);
            bill.set("employee_name", null);
            bill.set("staffBankAccount", null);
            bill.set("staffBankAccount_account", null);
            bill.set("retailer", null);
            bill.set("retailerAccountName", null);
            bill.set("retailerAccountNo", null);
            bill.set("retailerLineNumber", null);
            bill.set("retailerAccountType", null);
            bill.set("retailerBankType", null);
            bill.set("retailerBankType_name", null);
        } else if (caObject.equals(CaObject.Customer.getValue())) {
            bill.set("supplier", null);
            bill.set("supplier_name", null);
            bill.set("supplierbankaccount", null);
            bill.set("supplierbankaccount_accountname", null);
            bill.set("employee", null);
            bill.set("employee_name", null);
            bill.set("staffBankAccount", null);
            bill.set("staffBankAccount_account", null);
            bill.set("retailer", null);
            bill.set("retailerAccountName", null);
            bill.set("retailerAccountNo", null);
            bill.set("retailerLineNumber", null);
            bill.set("retailerAccountType", null);
            bill.set("retailerBankType", null);
            bill.set("retailerBankType_name", null);
        } else if (caObject.equals(CaObject.Employee.getValue())) {
            bill.set("supplier", null);
            bill.set("supplier_name", null);
            bill.set("supplierbankaccount", null);
            bill.set("supplierbankaccount_accountname", null);
            bill.set("customer", null);
            bill.set("customer_name", null);
            bill.set("customerbankaccount", null);
            bill.set("customerbankaccount_bankAccountName", null);
            bill.set("retailer", null);
            bill.set("retailerAccountName", null);
            bill.set("retailerAccountNo", null);
            bill.set("retailerLineNumber", null);
            bill.set("retailerAccountType", null);
            bill.set("retailerBankType", null);
            bill.set("retailerBankType_name", null);
        } else if (caObject.equals(CaObject.Other.getValue())) {
            bill.set("supplier", null);
            bill.set("supplier_name", null);
            bill.set("supplierbankaccount", null);
            bill.set("supplierbankaccount_accountname", null);
            bill.set("customer", null);
            bill.set("customer_name", null);
            bill.set("customerbankaccount", null);
            bill.set("customerbankaccount_bankAccountName", null);
            bill.set("employee", null);
            bill.set("employee_name", null);
            bill.set("staffBankAccount", null);
            bill.set("staffBankAccount_account", null);
        }
        putParam(paramMap, "return", bill);
        return  new RuleExecuteResult();
    }
}
