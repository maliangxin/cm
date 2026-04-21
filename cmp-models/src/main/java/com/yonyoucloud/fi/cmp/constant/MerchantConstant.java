package com.yonyoucloud.fi.cmp.constant;

/**
 * 同步客商  实体
 * @author miaowb
 *
 */
public class MerchantConstant {

    private MerchantConstant() {}

    public static final String UPC_SAVE = "/api/upc/save?token=";

    public static final String UPC_QUERY = "/api/upc/getBizData?token=";

    public static final String DEFAULT_COUNTRY = "0040be98-735b-44c0-afe5-54d11a96037b";

    public static final String DEFAULT_CURRENCY = "G001ZM0000DEFAULTCURRENCT00000000001";

    public static final String TEMP_VENDOR_CLASS_NAME = "P_YS_FI_CM_0000026134" /* "临_供应商分类" */;
    public static final String TEMP_CUSTOMER_CLASS_NAME = "P_YS_FI_CM_0000026158" /* "临_客户分类" */;

    public static final String ORG = "org";
    public static final String CREATEORG = "createOrg";
    public static final String BELONGORG = "belongOrg";
    public static final String CODE = "code";
    public static final String NAME = "name";
    public static final String STATUS = "_status";
    public static final String INSERT = "Insert";
    public static final String UNCHANGED = "Unchanged";
    public static final String ERPCODE = "erpCode";
    public static final String ERP_CODE = "erp_code";
    public static final String EXTENDS_ERPCODE = "extends_erpcode";
    public static final String ISCREATOR = "isCreator";
    public static final String VENDOREXTENDS = "vendorextends";
    public static final String STOPSTATUS = "stopstatus";
    public static final String VENDORBANKS = "vendorbanks";
    public static final String VENDORCLASS = "vendorclass";
    public static final String VENDORORGS = "vendorOrgs";
    public static final String VENDORCLASS_CODE = "vendorClass_code";
    public static final String VENDORCLASS_ID = "vendorClass_id";
    public static final String ACCOUNTTYPE = "accountType";
    public static final String DEFAULTBANK = "defaultbank";
    public static final String COUNTRY = "country";
    public static final String CURRENCY = "currency";
    public static final String BANK = "bank";
    public static final String OPENACCOUNTBANK = "openaccountbank";
    public static final String ACCOUNT = "account";
    public static final String ACCOUNTNAME = "accountname";
    public static final String BILLNUM = "billnum";
    public static final String VENDORCLASS_NAME = "vendorclass_name";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String ISENABLED = "isEnabled";
    public static final String BANKDOT = "bankdot";
    public static final String ID = "id";
    public static final String OPENBANK = "openBank";
    public static final String BANKACCOUNT = "bankAccount";
    public static final String BANKACCOUNTNAME = "bankAccountName";
    public static final String MERCHANTAGENTFINANCIALINFOS = "merchantAgentFinancialInfos";
    public static final String PRINCIPALS = "principals";
    public static final String MERCHANTAPPLIEDDETAIL = "merchantAppliedDetail";
    public static final String MERCHANTAPPLYRANGES = "merchantApplyRanges";
    public static final String CUSTOMERCLASS = "customerClass";
    public static final String CUSTOMERCLASS_CODE = "customerClass_Code";
    public static final String CUSTOMERCLASS_ID = "customerClass_Id";
    public static final String CUSTOMERCLASS_ERPCODE = "customerClass_ErpCode";
    public static final String CUSTOMERCLASS_NAME = "customerClass_Name";
    public static final String ISDEFAULT = "isDefault";
    public static final String CUSTOMERBANK_ISDEFAULT = "customerBank_IsDefault";
    public static final String VENDORBANK_ISDEFAULT = "vendorBank_IsDefault";
    public static final String MESSAGES = "messages";
    public static final String DATA = "data";
    public static final String FULLNAME = "fullname";
    public static final String DATA_FROM = "data_from";
    public static final String CM = "cm";
    public static final String MERCHANTID = "merchantId";
    public static final String SPECIALMANAGEMENTDEP = "specialManagementDep";
    public static final String CSPECIALMANAGEMENTDEPNAME = "cSpecialManagementDepName";
    public static final String PROFESSSALESMAN = "professSalesman";
    public static final String PROFESSSALESMAN_NAME = "professSalesman_name";
    public static final String RANGEOBJECTID = "rangeObjectId";


    public static final String DETAILSTOPSTATUS = "detailStopStatus";

    public static final String CUSTOMERFLAG = "customerFlag";
    public static final String CUSTOMERID = "customerId";
    public static final String CUSTOMERERPCODE = "customerErpCode";
    public static final String CUSTOMERBANKFLAG = "customerbankFlag";
    public static final String CUSTOMERBANKERPCODE = "customerbankErpCode";
    public static final String BANKERPCODE = "bankErpCode";
    public static final String CUSTOMERBANKID = "customerbankId";
    public static final String SUPPLIERBANKID = "supplierbankId";
    public static final String JOINTLINENO = "jointLineNo";
    public static final String CORRESPONDENTCODE = "correspondentcode";

    public static final String VENDORFLAG = "vendorFlag";
    public static final String OTHERFLAG = "otherFlag";
    public static final String VENDORID = "vendorId";
    public static final String VENDORERPCODE = "vendorErpCode";
    public static final String VENDOREXTENDSERPCODE = "vendorextendsErpCode";
    public static final String EXTENDSERPCODE = "extendsErpCode";
    public static final String VENDORBANKFLAG = "vendorbankFlag";
    public static final String VENDORBANKERPCODE = "vendorbankErpCode";
    public static final String VENDORBANKID = "vendorbankId";


    public static final String ACCENTITY = "accentity";
    public static final String MERCHANT_CODE = "merchant_code";
    public static final String VENDOR_CODE = "vendor_code";
    public static final String MERCHANTCUSTOMERCLASS_CODE = "merchantCustomerClass_code";
    public static final String MERCHANTCUSTOMERCLASS = "merchantCustomerClass";
    public static final String TO_ACCT_NAME = "to_acct_name";
    public static final String MERCHANT_NAME = "merchant_name";
    public static final String VENDOR_NAME = "vendor_name";
    public static final String TO_ACCT_NO = "to_acct_no";
    public static final String MERCHANTCOUNTRY = "merchantCountry";
    public static final String MERCHANTACCOUNTTYPE = "merchantAccountType";
    public static final String MERCHANTBANK = "merchantBank";
    public static final String MERCHANTOPENBANK = "merchantOpenBank";
    public static final String MERCHANTOPENBANK_NAME = "merchantOpenBank_name";
    public static final String ENTERPRISEBANKACCOUNT_NAME = "enterpriseBankAccount_name";
    public static final String MERCHANT_FLAG = "merchant_flag";
    public static final String DC_FLAG = "dc_flag";
    public static final String TO_ACCT_BANK = "to_acct_bank";
    public static final String TO_ACCT_BANK_NAME = "to_acct_bank_name";



//    public static final String AA_MERCHANT = "aa_merchant";
    public static final String AA_MERCHANT = "productcenter.aa_merchantref";
    public static final String AA_VENDORCLASS_VENDORCLASS = "aa.vendorclass.VendorClass";
    public static final String AA_VENDORCLASSIFICATION = "aa_vendorclassification";
    public static final String AA_VENDOR = "aa_vendor";
    public static final String AA_VENDOR_VENDOR = "aa.vendor.Vendor";
    public static final String AA_VENDOR_VENDORBANK = "aa.vendor.VendorBank";
    public static final String AA_VENDOR_VENDOREXTENDS = "aa.vendor.vendorExtends";
    public static final String AA_CUSTCATEGORY_CUSTCATEGORY = "aa.custcategory.CustCategory";
    public static final String AA_MERCHANT_MERCHANTAPPLYRANGEDETAIL = "aa.merchant.MerchantApplyRangeDetail";
    public static final String AA_MERCHANT_MERCHANTAPPLYRANGE = "aa.merchant.MerchantApplyRange";

    public static final String AA_CUSTCATEGORY = "aa_custcategory";
    public static final String AA_MERCHANT_MERCHANT = "aa.merchant.Merchant";
    public static final String AA_MERCHANT_AGENTFINANCIAL = "aa.merchant.AgentFinancial";
    public static final String AA_MERCHANT_PRINCIPAL = "aa.merchant.Principal";
    public static final String BD_COUNTRY_COUNTRYVO = "bd.country.CountryVO";

    public static final String RPCTOKEN = "rpcToken";
    public static final String UCFBASEDOC = "ucfbasedoc";
    public static final String BANKTYPENAME = "bankTypeName";
    public static final String BANKTYPEID = "bankTypeId";
    public static final String BANKDOTNAME = "bankDotName";
    public static final String BANKDOTID = "bankDotId";
    public static final String MERCHANT_URL = "MERCHANT_URL";

    public static final String BD_STAFF_STAFFBANKACCT = "bd.staff.StaffBankAcct";

}
