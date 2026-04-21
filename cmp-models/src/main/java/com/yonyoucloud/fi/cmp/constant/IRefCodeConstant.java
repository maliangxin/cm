package com.yonyoucloud.fi.cmp.constant;


import java.util.Arrays;
import java.util.List;

/**
 * 参照常量类
 *
 * @author mal
 *
 */
public interface IRefCodeConstant {

    //兼容新旧参照
    List<String> FUNDS_ORG_ADN_FINANCE_ORG_LIST = Arrays.asList("bd_fundsorg_na","ucf-org-center.bd_fundsorg_na",
            "ucf-org-center.bd_financeorgtreeref_na","ucf-org-center.bd_accbody");
    // 资金组织参照
    String FUNDS_ORGTREE = "ucf-org-center.bd_fundsorg_na";

    // 资金组织参照
    String FUNDS_ORGTREE_NA = "bd_fundsorg_na";

    // 核算会计 主体参照
    String FINANCE_ORGTREE = "ucf-org-center.bd_financeorgtreeref_na";

    // 核算会计 主体树形参照
    String BD_FINANCEORGTREEREF_NA = "ucf-org-center.bd_financeorgtreeref_na";
    String BD_FUNDSORG_NA = "ucf-org-center.bd_fundsorg_na";

    // 旧核算会计 主体参照，不删除了，留着兼容R5代码
    String BD_ACCBODY = "ucf-org-center.bd_accbody";

    // 业务员参照
    String AA_OPERATOR = "aa_operator";

    // 客户参照
    String AA_MERCHANTREF = "productcenter.aa_merchantref";

    // 供应商参照
    String YSSUPPLIER_AA_VENDOR = "yssupplier.aa_vendor";

    // 员工参照
    String BD_STAFF_REF = "bd_staff_ref";
    String DOMAIN_BD_STAFF_REF = "ucf-staff-center.bd_staff_ref";
    String BD_STAFF_LEAVE_REF = "ucf-staff-center.bd_staff_leave_ref";

    // 客户账户参照
    String AA_MERCHANTBANKREF = "productcenter.aa_merchantbankref";
    String CUSTOMER_REF_FULLNAME = "aa.merchant.AgentFinancial";

    // 客户账户参照(远程)
    String PRODUCTCENTER_AA_MERCHANTAGENTFINANCIALREF = "productcenter.aa_merchantagentfinancialref";

    // 供应商账户参照(远程)
    String YSSUPPLIER_AA_VENDORBANKREF = "yssupplier.aa_vendorbankref";
    String SUPPLIER_REF_FULLNAME = "aa.vendor.VendorBank";

    // 项目参照(远程)
    String UCFBASEDOC_BD_PROJECTCLASSTREEREF = "ucfbasedoc.bd_projectclasstreeref";
    String PROJECT_REF_FULLNAME = "bd.project.ProjectVO";

    // 员工账户参照
    String BD_STAFF_BANKACCT_REF = "ucf-staff-center.bd_staffbankaccref";
    String STAFF_BANKACCT_REF_FULLNAME = "bd.staff.StaffBankAcct";

    //资金业务对象参照
    String TMSP_FUNDBUSINOBJ_REF = "yonbip-fi-ctmtmsp.tmsp_fundbusinobj_Ref";

    //资金业务对象银行账户
    String TMSP_FUNDBUSINOBJBANK_REF = "yonbip-fi-ctmtmsp.tmsp_fundbusinobjbank_Ref";

    //票据号
    String DRFT_BILLNOREF = "drft.drft_billnoref";

    String UCFBASEDOC_BD_BANKDOTREF = "ucfbasedoc.bd_bankdotref";

    //企业银行账户
    String UCFBASEDOC_BD_ENTERPRISEBANKACCT = "ucfbasedoc.bd_enterprisebankacct";
    //银行账户类别
    String UCFBASEDOC_BD_BANKCARD = "ucfbasedoc.bd_bankcard";

    // 企业现金账户
    String UCFBASEDOC_BD_ENTERPRISECASHACCT = "ucfbasedoc.bd_enterprisecashacct";

    String FINBD_BD_PAYMENTTYPEREF = "finbd.bd_paymenttyperef";
    //单据类型
    String  TRANSTYPE_BD_BILLTYPE_GRIDREF = "transtype.bd_billtype_gridref";

    //资金计划
    String REF_CSPL_FUND_PLAN = "yonbip-fi-ctmcspl.cspl_plansummaryref";

    //结算方式
    String REF_SETTLEMENT =  "productcenter.aa_settlemethodref";

    //部门参照
    String DEPT_REF =  "ucf-org-center.bd_adminorgsharetreeref";

    //部门参照
    String AA_DEPT_REF =  "productcenter.aa_department";

    String AA_ORG =  "ucf-org-center.bd_adminorgtreeref";

    // 交易类型参照
    String TRANSTYPE_BD_BILLTYPEREF = "transtype.bd_billtyperef";


    // 交易类型参照
    String TRANSTYPE_BD_BILLTYPERE_LOCAL = "bd_billtyperef";


}
