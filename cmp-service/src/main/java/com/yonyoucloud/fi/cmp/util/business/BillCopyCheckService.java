package com.yonyoucloud.fi.cmp.util.business;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.service.ref.OrgRpcService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.iuap.upc.api.IMerchantServiceV2;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import com.yonyoucloud.upc.pub.api.vendor.service.vendor.IVendorPubQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorExtendVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于单据复制时进行校验的工具类，主要校验客户、供应商、员工、组织、结算方式、交易类型等等多种现金管理所使用档案
 *
 * @Author maliangn
 * @date 2023.1.4
 */
@Slf4j
@Service
public class BillCopyCheckService {

    @Autowired
    BaseRefRpcService baseRefRpcService;
    //@Autowired
//    OrgRpcService orgRpcService;
    @Autowired
    IVendorPubQueryService vendorPubQueryService;

    @Autowired
    IMerchantServiceV2 merchantService;

    /**
     * 校验会计主体
     *
     * @param bizObject
     */
    public static void checkAccentity(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //会计主体-必输
        if (bizObject.get(IBussinessConstant.ACCENTITY) != null) {
            List<Map<String, Object>> accentityList = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(bizObject.get(IBussinessConstant.ACCENTITY));/* 暂不修改 静态方法*/
//            AccentityUtil.getFinOrgDTOByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
            if (!CollectionUtils.isEmpty(accentityList)) {
                Object accEntityFlag = accentityList.get(0).get(MerchantConstant.STOPSTATUS);
                if (accEntityFlag != null && "1".equals(accEntityFlag.toString())) {//0是启用，1是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100991"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050028", "资金组织未启用，保存失败！") /* "资金组织未启用，保存失败！" */);
                } else {
                    checkCacheMap.put("accentity_" + bizObject.get(IBussinessConstant.ACCENTITY), 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100992"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050027", "未查询到对应的资金组织，保存失败！") /* "未查询到对应的资金组织，保存失败！" */);
            }
        }
    }

    /**
     * 校验组织
     *
     * @param bizObject
     */
    public static void checkOrg(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //付款申请组织-必输
        if (bizObject.get("org") != null) {
            List<Map<String, Object>> orgMVList = QueryBaseDocUtils.getOrgMVById(bizObject.get("org"));/* 暂不修改 静态方法*/
            if (!CollectionUtils.isEmpty(orgMVList)) {
                Object accEntityFlag = orgMVList.get(0).get(MerchantConstant.STOPSTATUS);
                if (accEntityFlag != null && "1".equals(accEntityFlag.toString())) {//0是启用，1是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100993"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418001D","付款申请组织未启用，保存失败！") /* "付款申请组织未启用，保存失败！" */);
                } else {
                    checkCacheMap.put("org_" + bizObject.get("org"), 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100994"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180021","未查询到对应的付款申请组织，保存失败！") /* "未查询到对应的付款申请组织，保存失败！" */);
            }
        }
    }

    /**
     * 校验交易类型
     *
     * @param bizObject
     */
    public static void checkTradetype(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //交易类型-必输
        if (bizObject.get("tradetype") != null) {
            Map tradeTypeMap = QueryBaseDocUtils.queryTransTypeById(bizObject.get("tradetype"));/* 暂不修改 静态方法*/
            if (tradeTypeMap != null && tradeTypeMap.size() > 0) {
                Object tradeTypeFlag = tradeTypeMap.get("enable");
                if (tradeTypeFlag != null && "2".equals(tradeTypeFlag.toString())) {//1是启用，2是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100995"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180027","交易类型未启用，保存失败！") /* "交易类型未启用，保存失败！" */);
                } else {
                    checkCacheMap.put("tradetype_" + bizObject.get("tradetype"), 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100996"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418002B","未查询到对应的交易类型，保存失败！") /* "未查询到对应的交易类型，保存失败！" */);
            }
        }
    }

    /**
     * 校验结算方式
     *
     * @param bizObject
     */
    public static void checkSettlemode(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //结算方式-必输
        if (bizObject.get("settlemode") != null) {
            Map settleModeMap = QueryBaseDocUtils.querySettlementWayById(bizObject.get("settlemode"));/* 暂不修改 静态方法*/
            if (settleModeMap != null && settleModeMap.size() > 0) {
                Object settleModeFlag = settleModeMap.get("isEnabled");
                if (settleModeFlag != null && !(boolean) settleModeFlag) {//true是启用，false是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100997"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180032","结算方式未启用，保存失败！") /* "结算方式未启用，保存失败！" */);
                } else {
                    checkCacheMap.put("settlemode_" + bizObject.get("settlemode"), 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100998"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180017","未查询到对应的结算方式，保存失败！") /* "未查询到对应的结算方式，保存失败！" */);
            }
        }
    }

    /**
     * 校验币种
     *
     * @param bizObject
     */
    public static void checkCurrency(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //币种-必输
        if (bizObject.get("currency") != null) {
            List<Map<String, Object>> currencyList = QueryBaseDocUtils.queryCurrencyById(bizObject.get("currency"));/* 暂不修改 静态方法*/

            if (!CollectionUtils.isEmpty(currencyList)) {
                Object currencyFlag = currencyList.get(0).get("enable");
                if (currencyFlag != null && "2".equals(currencyFlag.toString())) {//1是启用，2是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100999"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180020","币种未启用，保存失败！") /* "币种未启用，保存失败！" */);
                } else {
                    checkCacheMap.put("currency_" + bizObject.get("currency"), 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180022","未查询到对应的币种，保存失败！") /* "未查询到对应的币种，保存失败！" */);
            }
        }
    }

    /**
     * 校验币种
     *
     * @param bizObject
     */
    public static void checkSettleCurrency(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //币种-必输
        if (bizObject.get("settleCurrency") != null) {
            List<Map<String, Object>> currencyList = QueryBaseDocUtils.queryCurrencyById(bizObject.get("settleCurrency"));/* 暂不修改 静态方法*/

            if (!CollectionUtils.isEmpty(currencyList)) {
                Object currencyFlag = currencyList.get(0).get("enable");
                if (currencyFlag != null && "2".equals(currencyFlag.toString())) {//1是启用，2是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101001"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DD", "结算币种未启用！") /* "结算币种未启用！" */);
                } else {
                    checkCacheMap.put("settleCurrency_" + bizObject.get("settleCurrency"), 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101002"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DC", "未查询到对应的结算币种！") /* "未查询到对应的结算币种！" */);
            }
        }
    }

    /**
     * 校验客户
     *
     * @param bizObject
     */
    public static void checkCustomer(BizObject bizObject) throws Exception {
        //收款客户-非必输
        if (bizObject.get(IBussinessConstant.CUSTOMER) != null) {
            MerchantDTO merchantByIdAndOrg = QueryBaseDocUtils.getMerchantByIdAndOrg(bizObject.get(IBussinessConstant.CUSTOMER), bizObject.get(IBussinessConstant.ACCENTITY));
            if (merchantByIdAndOrg != null) {
                if (merchantByIdAndOrg.getDetailStopStatus().booleanValue()) {//0是启用，1是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101003"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180029","收款客户未启用，保存失败！") /* "收款客户未启用，保存失败！" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101004"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418002D","未查询到对应的收款客户，保存失败！") /* "未查询到对应的收款客户，保存失败！" */);
            }
        }
    }

    /**
     * 校验客户byid，需要根据客户id和组织id进行过滤，判断该使用组织下该客户是否停用
     *
     * @param customerId
     */
    public void checkCustomerByid(Object customerId, String orgid, Map<String, Integer> checkCacheMap) throws Exception {
        //收款客户-非必输
        if (customerId != null && orgid != null) {
            MerchantDTO merchantDTO = merchantService.getMerchantByIdAndOrg((Long) customerId, Long.valueOf(orgid), new String[]{MerchantConstant.DETAILSTOPSTATUS});
            if (merchantDTO != null) {
                if (null != merchantDTO.getDetailStopStatus() && merchantDTO.getDetailStopStatus()) {//0是启用，1是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101005"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00246", "收款客户未启用，保存失败！") /* "收款客户未启用，保存失败！" */);
                } else {
                    checkCacheMap.put("oppositeobjectid_customer_" + customerId, 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101006"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00249", "未查询到对应的收款客户，保存失败！") /* "未查询到对应的收款客户，保存失败！" */);
            }
        }
    }

    /**
     * 校验客户银行账户
     *
     * @param bizObject
     */
    public static void checkCustomerbankaccount(BizObject bizObject) throws Exception {
        if (bizObject.get("customerbankaccount") != null) {
            List<AgentFinancialDTO> customerBankAccountMap = QueryBaseDocUtils.queryCustomerBankAccountById(bizObject.getLong("customerbankaccount"));/* 暂不修改 静态方法*/
            if (customerBankAccountMap != null && customerBankAccountMap.size() > 0) {
                Boolean stopstatus = customerBankAccountMap.get(0).getStopStatus();
                if (stopstatus) {//0是启用，1是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101007"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180016","收款客户账户未启用，保存失败！") /* "收款客户账户未启用，保存失败！" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101008"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180018","未查询到对应的收款客户账户，保存失败！") /* "未查询到对应的收款客户账户，保存失败！" */);
            }
        }
    }

    /**
     * 校验客户银行账户byid
     *
     * @param id
     */
    public static void checkCustomerbankaccountById(Object id, Map<String, Integer> checkCacheMap) throws Exception {
        if (id != null) {
            List<AgentFinancialDTO> customerBankAccountMap = QueryBaseDocUtils.queryCustomerBankAccountById(Long.valueOf(id.toString()));/* 暂不修改 静态方法*/
            if (customerBankAccountMap != null && customerBankAccountMap.size() > 0) {
                Boolean stopstatus = customerBankAccountMap.get(0).getStopStatus();
                if (stopstatus) {//0是启用，1是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101007"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180016","收款客户账户未启用，保存失败！") /* "收款客户账户未启用，保存失败！" */);
                } else {
                    checkCacheMap.put("oppositeaccountid_customer_" + id, 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101008"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180018","未查询到对应的收款客户账户，保存失败！") /* "未查询到对应的收款客户账户，保存失败！" */);
            }
        }
    }

    /**
     * 根据条件校验供应商银行账户
     *
     * @param condition
     */
    public void checkSupplierbankaccountById(Map<String, Object> condition, Object id, Map<String, Integer> checkCacheMap) throws Exception {
        //供应商银行账户没有校验
        if (MapUtils.isNotEmpty(condition)) {
            List<VendorBankVO> supplierbankaccounts = vendorPubQueryService.getVendorBanksByCondition(condition);
            if (supplierbankaccounts != null && supplierbankaccounts.size() > 0) {
                for (VendorBankVO vendorBankVO : supplierbankaccounts) {
                    if (vendorBankVO.getStopstatus()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101009"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00248", "供应商账户未启用，保存失败！") /* "供应商账户未启用，保存失败！" */);
                    } else {
                        checkCacheMap.put("oppositeaccountid_supplier_" + id, 1);
                    }
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101010"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0024B", "未查询到对应的供应商账户，保存失败！") /* "未查询到对应的供应商账户，保存失败！" */);
            }
        }
    }

    /**
     * 校验费用项目
     *
     * @param bizObject
     */
    public static void checkExpenseitem(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //费用项目-非必输
        if (bizObject.get("expenseitem") != null) {
            List<Map<String, Object>> expenseItemList = QueryBaseDocUtils.queryExpenseItemById(bizObject.get("expenseitem"));/* 暂不修改 已登记*/
            if (!CollectionUtils.isEmpty(expenseItemList)) {
                Object expenseItemFlag = expenseItemList.get(0).get("enabled");
                if (expenseItemFlag != null && !(boolean) expenseItemFlag) {//true是启用，false是未启用
                    // 将无效或停用的费用项目清掉
                    bizObject.set("expenseitem", null);
                }
                //判断是否勾选财资服务
//                Object propertyBusinessFlag = expenseItemList.get(0).get("propertybusiness");
//                if (propertyBusinessFlag != null && !propertyBusinessFlag.equals("1")) { // 1为勾选，其他为没勾选
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101011"),MessageUtils.getMessage("P_YS_CTM_CM-BE_1613096893618323459") /* "所选费用项目中存在未启用财资业务领域的情况，保存失败！" */);
//                } else {
//                    checkCacheMap.put("expenseitem_" + bizObject.get("expenseitem"), 1);
//                }
            } else {
                // 将无效或停用的费用项目清掉
                bizObject.set("expenseitem", null);
            }
        }
    }

    /**
     * 校验项目
     *
     * @param bizObject
     */
    public static void checkProject(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //项目-非必输
        if (bizObject.get("project") != null) {
            List<Map<String, Object>> projectList = QueryBaseDocUtils.queryProjectById(bizObject.get("project"));/* 暂不修改 静态方法*/
            if (!CollectionUtils.isEmpty(projectList)) {
                Object projectFlag = projectList.get(0).get("enable");
                if (projectFlag != null && "2".equals(projectFlag.toString())) {//1是启用，2是未启用
                    // 将无效或停用的项目清掉
                    bizObject.set("project", null);
                } else {
                    checkCacheMap.put("project_" + bizObject.get("project"), 1);
                }
            } else {
                // 将无效或停用的项目清掉
                bizObject.set("project", null);
            }
        }
    }

    /**
     * 校验部门
     *
     * @param bizObject
     */
    public static void checkDept(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //部门-非必输
        if (bizObject.get("dept") != null) {
            List<Map<String, Object>> deptList = QueryBaseDocUtils.queryDeptById(bizObject.get("dept"));/* 暂不修改 静态方法*/
            if (!CollectionUtils.isEmpty(deptList)) {
                Object stopstatus = deptList.get(0).get(MerchantConstant.STOPSTATUS);
                if (stopstatus != null && "1".equals(stopstatus.toString())) {//0是启用，1是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101012"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180023","部门未启用，保存失败！") /* "部门未启用，保存失败！" */);
                } else {
                    checkCacheMap.put("dept_" + bizObject.get("dept"), 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101013"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180024","未查询到对应的部门，保存失败！") /* "未查询到对应的部门，保存失败！" */);
            }
        }
    }

    /**
     * 根据供应商id及使用组织id查询对于供应商档案，并判断该供应商档案是否停用
     * @param supplierid
     * @param orgid
     * @param checkCacheMap
     * @throws Exception
     */
    public void checkSupplier(Long supplierid,String orgid ,Map<String, Integer> checkCacheMap) throws Exception {
        if (supplierid != null) {
            try {
//                VendorExtendVO vendorExtendVO = vendorPubQueryService.getVendorExtendByVendorIdAndOrgId(supplierid,Long.valueOf(orgid));
                //CZFW-125361 供应商校验去掉 orgid默认使用管理组织
                VendorExtendVO vendorExtendVO = vendorPubQueryService.getVendorExtendByVendorIdAndOrgId(supplierid,null);
                if (vendorExtendVO != null) {
                    if (vendorExtendVO.getStopstatus()) {//false是启用，true是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101014"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00247", "供应商未启用，保存失败！") /* "供应商未启用，保存失败！" */);
                    } else {
                        checkCacheMap.put("oppositeobjectid_supplier_" + supplierid, 1);
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101015"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0024A", "未查询到对应的供应商，保存失败！") /* "未查询到对应的供应商，保存失败！" */);
                }
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101016"),e.getMessage());
            }
        }
    }

    /**
     * 校验款项类型
     *
     * @param bizObject
     */
    public static void checkQuickType(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        //款项类型
        if (bizObject.get("quickType") != null) {
            Map<String, Object> condition = new HashMap<String, Object>();
            condition.put("id", bizObject.get("quickType"));
            List<Map<String, Object>> payQuickType = QueryBaseDocUtils.queryQuickTypeByCondition(condition);/* 暂不修改 静态方法*/
            if (!CollectionUtils.isEmpty(payQuickType)) {
                Map<String, Object> payQuickTypeMap = payQuickType.get(0);
                if (null != payQuickTypeMap.get(MerchantConstant.STOPSTATUS)) {
                    Boolean stopstatus = (Boolean) payQuickTypeMap.get(MerchantConstant.STOPSTATUS);
                    if (stopstatus) {//0是启用，1是未启用
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101017"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180030","款项类型未启用，保存失败！") /* "款项类型未启用，保存失败！" */);
                    } else {
                        checkCacheMap.put("quickType_" + bizObject.get("quickType"), 1);
                    }
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101018"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180033","未查询到对应的款项类型，保存失败！") /* "未查询到对应的款项类型，保存失败！" */);
            }
        }
    }

    /**
     * 校验现金账户-非必输
     *
     * @param bizObject
     */
    public static void checkCashaccount(BizObject bizObject) throws Exception {
        //现金账户-非必输
        if (bizObject.get("cashaccount") != null) {
            Map<String, Object> cashBankAccountMap = QueryBaseDocUtils.queryCashBankAccountById(bizObject.get("cashaccount"));/* 暂不修改 静态方法*/
            if (cashBankAccountMap != null && cashBankAccountMap.size() > 0) {
                Object cashAccountFlag = cashBankAccountMap.get("enable");
                if (cashAccountFlag != null && "0".equals(cashAccountFlag.toString())) {//1是启用，0是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101019"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418001C","现金账户未启用，保存失败！") /* "现金账户未启用，保存失败！" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101020"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418001E","未查询到对应的现金账户，保存失败！") /* "未查询到对应的现金账户，保存失败！" */);
            }
        }
    }

    /**
     * 校验企业银行-非必输
     *
     * @param bizObject
     */
    public static void checkBankaccount(BizObject bizObject, Map<String, Integer> checkCacheMap) throws Exception {
        if (bizObject.get("enterprisebankaccount") != null) {
            //企业银行账户-非必输
            Map<String, Object> enterpriseBankAccountMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(bizObject.get("enterprisebankaccount"));/* 暂不修改 静态方法*/
            if (enterpriseBankAccountMap != null && enterpriseBankAccountMap.size() > 0) {
                Object enterpriseBankAccountFlag = enterpriseBankAccountMap.get("enable");
                if (enterpriseBankAccountFlag != null && "2".equals(enterpriseBankAccountFlag.toString())) {//1是启用，2是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101021"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180025","企业银行账户未启用，保存失败！") /* "企业银行账户未启用，保存失败！" */);
                } else {
                    checkCacheMap.put("enterprisebankaccount_" + bizObject.get("enterprisebankaccount"), 1);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101022"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180028","未查询到对应的企业银行账户，保存失败！") /* "未查询到对应的企业银行账户，保存失败！" */);
            }
        }
    }
}
