package com.yonyoucloud.fi.cmp.fundexpense.rule.check;

import com.yonyou.ucf.mdd.common.model.CheckItem;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * desc:字段编辑后规则
 * author:yuanjqr
 * date:2024/7/26 19:18
 */
@Component("afterEditDischargeapplyCheckRule")
public class AfterEditFundexpenseCheckRule extends AbstractCommonRule {


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        List<BizObject> bills = getBills(billContext, paramMap);
        RuleExecuteResult result = new RuleExecuteResult();
        if (CollectionUtils.isEmpty(bills)) {
            return result;
        }
        if (bills != null && bills.size() > 0) {
            //获取前端传过来的值对象
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            CheckItem checkItem = billDataDto.getCheckItem();
            if (Objects.isNull(checkItem)) {
                return result;
            }
            String key = checkItem.getKey();
            if (Objects.isNull(key)) {
                return result;
            }
            //判断是哪个字段进行check规则
            switch (key) {
                case "dfenterprise_customer_name":
                    checkDfenterpriseCustomerName(fundexpense);
                    break;
                case "dfenterprise_supplier_name":
                    checkDfenterpriseSupplierName(fundexpense);
                    break;
                case "dfenterprise_funbusobj_name":
                    checkDfenterpriseFunbusobjName(fundexpense);
                    break;
                case "dfcustomerbankaccount_account":
                    checkDfenterpriseCustomeraccount(fundexpense);
                    break;
                case "dfsupplierbankaccount_account":
                    checkDfenterpriseSupplieraccount(fundexpense);
                    break;
                case "dffunbusobjbankaccount_account":
                    checkDfenterpriseFunbusobjaccount(fundexpense);
                    break;
                default:
                    return new RuleExecuteResult();
            }

        }
        return new RuleExecuteResult();
    }

    /**
     * 根据供应商自动带出默认银行账号等信息
     * @param fundexpense
     * @throws Exception
     */
    public void checkDfenterpriseSupplieraccount(Fundexpense fundexpense) throws Exception {
        Long dfsupplierbankaccount = fundexpense.getDfsupplierbankaccount();
        String fillname="aa.vendor.VendorBank";
        QuerySchema querySchemaNew = QuerySchema.create().
                addSelect("id,account,accountname,openaccountbank,openaccountbank.name as openaccountbankName,correspondentcode,bank,bank.name as banktypename");
        querySchemaNew.appendQueryCondition(QueryCondition.name("vendor").eq(dfsupplierbankaccount));
//        querySchemaNew.appendQueryCondition(QueryCondition.name("defaultbank").eq(1));
        List<Map<String, Object>> mapList = MetaDaoHelper.query(fillname, querySchemaNew, "productcenter");
        if(!CollectionUtils.isEmpty(mapList)){
            Map<String, Object> map = mapList.get(0);
            fundexpense.setDfaccountlinenumber(map.get("correspondentcode")!=null?map.get("correspondentcode").toString():null);
            fundexpense.setDfbankacounttype(map.get("banktypename")!=null?map.get("banktypename").toString():null);
            fundexpense.setDfsupplierbankaccount(map.get("id")!=null?Long.parseLong(map.get("id").toString()):null);
            fundexpense.setDfenterprisecountbankname(map.get("accountname")!=null?map.get("accountname").toString():null);
            fundexpense.setDfbankcountopenbank(map.get("openaccountbank")!=null?map.get("openaccountbank").toString():null);
            fundexpense.setDfbankcountopenbankname(map.get("openaccountbankName")!=null?map.get("openaccountbankName").toString():null);
        }
    }

    /**
     * 资金伙伴自动带出默认银行账号等信息
     * @param fundexpense
     * @throws Exception
     */
    public void checkDfenterpriseFunbusobjaccount(Fundexpense fundexpense) throws Exception {
        String dffunbusobjbankaccount = fundexpense.getDffunbusobjbankaccount();
        String fillname="tmsp.fundbusinobjarchives.FundBusinObjArchivesItem";
        QuerySchema querySchemaNew = QuerySchema.create().
                addSelect("id,bankaccount,accountname,bopenaccountbankid,bopenaccountbankid.name as bopenaccountbankidName,linenumber,bbankid,bbankid.name as banktypename");//unified_social_credit_code
        querySchemaNew.appendQueryCondition(QueryCondition.name("id").eq(dffunbusobjbankaccount));
//        querySchemaNew.appendQueryCondition(QueryCondition.name("isdefaultaccount").eq(1));
        List<Map<String, Object>> mapList = MetaDaoHelper.query(fillname, querySchemaNew, "yonbip-fi-ctmtmsp");
        if(!CollectionUtils.isEmpty(mapList)){
            Map<String, Object> map = mapList.get(0);
            fundexpense.setDfaccountlinenumber(map.get("linenumber")!=null?map.get("linenumber").toString():null);
            fundexpense.setDfbankacounttype(map.get("banktypename")!=null?map.get("banktypename").toString():null);
            fundexpense.setDfsupplierbankaccount(map.get("id")!=null?Long.parseLong(map.get("id").toString()):null);
            fundexpense.setDfenterprisecountbankname(map.get("accountname")!=null?map.get("accountname").toString():null);
            fundexpense.setDfbankcountopenbank(map.get("bopenaccountbankid")!=null?map.get("bopenaccountbankid").toString():null);
            fundexpense.setDfbankcountopenbankname(map.get("bopenaccountbankidName")!=null?map.get("bopenaccountbankidName").toString():null);

        }
    }

    /**
     * 客户自动带出默认银行账号等信息
     * @param fundexpense
     * @throws Exception
     */
    public void checkDfenterpriseCustomeraccount(Fundexpense fundexpense) throws Exception {
        Long dfcustomerbankaccount = fundexpense.getDfcustomerbankaccount();
        String fillname="aa.merchant.AgentFinancial";
        QuerySchema querySchemaNew = QuerySchema.create().
                addSelect("id,bankAccount,bankAccountName,openBank,openBank.name as openBankName ,jointLineNo,bank,bank.name as banktypename");
        querySchemaNew.appendQueryCondition(QueryCondition.name("id").eq(dfcustomerbankaccount));
//        querySchemaNew.appendQueryCondition(QueryCondition.name("isDefault").eq(1));
        List<Map<String, Object>> mapList = MetaDaoHelper.query(fillname, querySchemaNew, "productcenter");
        if(!CollectionUtils.isEmpty(mapList)){
            Map<String, Object> map = mapList.get(0);
            fundexpense.setDfaccountlinenumber(map.get("jointLineNo")!=null?map.get("jointLineNo").toString():null);
            fundexpense.setDfbankacounttype(map.get("banktypename")!=null?map.get("banktypename").toString():null);
            fundexpense.setDfsupplierbankaccount(map.get("id")!=null?Long.parseLong(map.get("id").toString()):null);
            fundexpense.setDfenterprisecountbankname(map.get("bankAccountName")!=null?map.get("bankAccountName").toString():null);
            fundexpense.setDfbankcountopenbank(map.get("openBank")!=null?map.get("openBank").toString():null);
            fundexpense.setDfbankcountopenbankname(map.get("openBankName")!=null?map.get("openBankName").toString():null);
        }
    }

    /**
     * 根据供应商自动带出默认银行账号等信息
     * @param fundexpense
     * @throws Exception
     */
    public void checkDfenterpriseSupplierName(Fundexpense fundexpense) throws Exception {
        Long dfenterprise_supplier = fundexpense.getDfenterprise_supplier();

        String fillname="aa.vendor.VendorBank";
        QuerySchema querySchemaNew = QuerySchema.create().
                addSelect("id,account,accountname,openaccountbank,openaccountbank.name as openaccountbankName,correspondentcode,bank,bank.name as banktypename");
        querySchemaNew.appendQueryCondition(QueryCondition.name("vendor").eq(dfenterprise_supplier));
        querySchemaNew.appendQueryCondition(QueryCondition.name("defaultbank").eq(1));
        List<Map<String, Object>> mapList = MetaDaoHelper.query(fillname, querySchemaNew, "productcenter");
        if(!CollectionUtils.isEmpty(mapList)){
            Map<String, Object> map = mapList.get(0);
            fundexpense.setDfaccountlinenumber(map.get("correspondentcode")!=null?map.get("correspondentcode").toString():null);
            fundexpense.setDfbankacounttype(map.get("banktypename")!=null?map.get("banktypename").toString():null);
            fundexpense.setDfsupplierbankaccount(map.get("id")!=null?Long.parseLong(map.get("id").toString()):null);
            fundexpense.setDfenterprisecountbankname(map.get("accountname")!=null?map.get("accountname").toString():null);
            fundexpense.setDfbankcountopenbank(map.get("openaccountbank")!=null?map.get("openaccountbank").toString():null);
            fundexpense.setDfbankcountopenbankname(map.get("openaccountbankName")!=null?map.get("openaccountbankName").toString():null);
        }
    }

    /**
     * 资金伙伴自动带出默认银行账号等信息
     * @param fundexpense
     * @throws Exception
     */
    public void checkDfenterpriseFunbusobjName(Fundexpense fundexpense) throws Exception {
        String dfenterprise_funbusobj = fundexpense.getDfenterprise_funbusobj();
        String fillname="tmsp.fundbusinobjarchives.FundBusinObjArchivesItem";
        QuerySchema querySchemaNew = QuerySchema.create().
                addSelect("id,bankaccount,accountname,bopenaccountbankid,bopenaccountbankid.name as bopenaccountbankidName,linenumber,bbankid,bbankid.name as banktypename");//unified_social_credit_code
        querySchemaNew.appendQueryCondition(QueryCondition.name("mainid").eq(dfenterprise_funbusobj));
        querySchemaNew.appendQueryCondition(QueryCondition.name("isdefaultaccount").eq(1));
        List<Map<String, Object>> mapList = MetaDaoHelper.query(fillname, querySchemaNew, "yonbip-fi-ctmtmsp");
        if(!CollectionUtils.isEmpty(mapList)){
            Map<String, Object> map = mapList.get(0);
            fundexpense.setDfaccountlinenumber(map.get("linenumber")!=null?map.get("linenumber").toString():null);
            fundexpense.setDfbankacounttype(map.get("banktypename")!=null?map.get("banktypename").toString():null);
            fundexpense.setDfsupplierbankaccount(map.get("id")!=null?Long.parseLong(map.get("id").toString()):null);
            fundexpense.setDfenterprisecountbankname(map.get("accountname")!=null?map.get("accountname").toString():null);
            fundexpense.setDfbankcountopenbank(map.get("bopenaccountbankid")!=null?map.get("bopenaccountbankid").toString():null);
            fundexpense.setDfbankcountopenbankname(map.get("bopenaccountbankidName")!=null?map.get("bopenaccountbankidName").toString():null);

        }
    }

    /**
     * 客户自动带出默认银行账号等信息
     * @param fundexpense
     * @throws Exception
     */
    public void checkDfenterpriseCustomerName(Fundexpense fundexpense) throws Exception {
        String dfenterprise_customer = fundexpense.getDfenterprise_customer();
        String fillname="aa.merchant.AgentFinancial";
        QuerySchema querySchemaNew = QuerySchema.create().
                addSelect("id,bankAccount,bankAccountName,openBank,openBank.name as openBankName ,jointLineNo,bank,bank.name as banktypename");
        querySchemaNew.appendQueryCondition(QueryCondition.name("merchantId").eq(dfenterprise_customer));
        querySchemaNew.appendQueryCondition(QueryCondition.name("isDefault").eq(1));
        List<Map<String, Object>> mapList = MetaDaoHelper.query(fillname, querySchemaNew, "productcenter");
        if(!CollectionUtils.isEmpty(mapList)){
            Map<String, Object> map = mapList.get(0);
            fundexpense.setDfaccountlinenumber(map.get("jointLineNo")!=null?map.get("jointLineNo").toString():null);
            fundexpense.setDfbankacounttype(map.get("banktypename")!=null?map.get("banktypename").toString():null);
            fundexpense.setDfsupplierbankaccount(map.get("id")!=null?Long.parseLong(map.get("id").toString()):null);
            fundexpense.setDfenterprisecountbankname(map.get("bankAccountName")!=null?map.get("bankAccountName").toString():null);
            fundexpense.setDfbankcountopenbank(map.get("openBank")!=null?map.get("openBank").toString():null);
            fundexpense.setDfbankcountopenbankname(map.get("openBankName")!=null?map.get("openBankName").toString():null);

        }
    }

}
