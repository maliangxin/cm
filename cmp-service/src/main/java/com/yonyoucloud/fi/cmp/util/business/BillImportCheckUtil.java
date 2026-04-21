package com.yonyoucloud.fi.cmp.util.business;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yonyou.ucf.basedoc.service.itf.IProjectService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialQryDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;
import java.util.*;

/**
 * 本类主要作用于现金管理导入数据，Openapi传入数据一些公共校验逻辑方法
 * 动态列字段翻译时使用
 * majfd
 * 2022/03/22
 */
@Slf4j
public class BillImportCheckUtil {

    private static final @NonNull Cache<String, List<AgentFinancialDTO>> agentFinanciaDTOListCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .softValues()
            .build();

    /**
     * 根据客户银行账户，币种，客户id获取客户银行账户档案
     *
     * @param merchantId
     * @param bankAccount
     * @param currency
     * @return
     * @throws Exception
     */
    public static AgentFinancialDTO queryCustomerBankAccountByCondition(Long merchantId, String bankAccount, String currency) throws Exception {
        if (bankAccount == null || currency == null) {
            return null;
        }

        String agentFinanciaCacheKey = merchantId + "_" + bankAccount + "_" + currency;
        List<AgentFinancialDTO> cacheValue = agentFinanciaDTOListCache.getIfPresent(agentFinanciaCacheKey);
        List<AgentFinancialDTO> custList;
        if (cacheValue != null) {
            custList = cacheValue;
        } else {

            AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
            if (merchantId != null) {
                agentFinancialQryDTO.setMerchantId(merchantId);
            }
            agentFinancialQryDTO.setStopStatus(Boolean.FALSE);
            agentFinancialQryDTO.setBankAccount(bankAccount);
            agentFinancialQryDTO.setCurrency(currency);
            custList = QueryBaseDocUtils.queryCustomerBankAccountByCondition(agentFinancialQryDTO);
            agentFinanciaDTOListCache.put(agentFinanciaCacheKey, custList);
        }

        if (CollectionUtils.isNotEmpty(custList)) {
            return custList.get(0);
        }
        return null;
    }

    /**
     * 根据供应商银行账户，币种，供应商id获取供应商银行账户档案
     *
     * @param vendorId
     * @param bankAccount
     * @param currency
     * @return
     * @throws Exception
     */
    public static Map<String, Object> querySupplierBankAccountByCondition(Object vendorId, Object bankAccount, Object currency) throws Exception {
        if (bankAccount == null || currency == null) {
            return null;
        }
        Map<String, Object> condition1 = new HashMap<>();
        if (vendorId != null && !StringUtils.isBlank(vendorId.toString())) {
            condition1.put("vendor", vendorId);
        }
        condition1.put("stopstatus", "0");
        condition1.put("account", bankAccount);
        condition1.put("currency", currency);
        VendorQueryService vendorQueryService = AppContext.getBean(VendorQueryService.class);
        List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(condition1);/* 暂不修改 已登记*/
        if (CollectionUtils.isNotEmpty(bankAccounts)) {
            return bankAccounts.get(0);
        }
        return null;
    }

    /**
     * 根据员工银行账户，币种，员工id获取员工银行账户档案
     *
     * @param staffId
     * @param bankAccount
     * @param currency
     * @return
     * @throws Exception
     */
    public static Map<String, Object> queryStaffBankAccountByCondition(Object staffId, Object bankAccount, Object currency) throws Exception {
        if (bankAccount == null || currency == null) {
            return null;
        }
        Map<String, Object> condition1 = new HashMap<>();
        if (staffId != null && !StringUtils.isBlank(staffId.toString())) {
            condition1.put("staff_id", staffId);
        }
        condition1.put("account", bankAccount);
        condition1.put("dr", "0");
        condition1.put("currency", currency);
        List<Map<String, Object>> staffList = QueryBaseDocUtils.queryStaffBankAccountByCondition(condition1);/* 暂不修改 已登记*/
        if (CollectionUtils.isNotEmpty(staffList)) {
            return staffList.get(0);
        }
        return null;
    }

    /**
     * 通过项目ids查询项目的组织适用范围
     *
     * @param projectIds
     * @return
     * @throws Exception
     */
    public static List<String> queryOrgRangeSByProjectIds(List<String> projectIds) throws Exception {
        List<String> orgIds = new ArrayList<>();
        if (CollectionUtils.isEmpty(projectIds)) {
            return null;
        }
        // 基础档案项目信息
        Map<String, Set<String>> projectSetMap = RemoteDubbo.get(IProjectService.class, IDomainConstant.MDD_DOMAIN_UCFBASEDOC).
                queryOrgRangeSByProjectId(projectIds);
        if (MapUtils.isNotEmpty(projectSetMap)) {
            for (Set<String> value : projectSetMap.values()) {
                orgIds.addAll(value);
            }
        }
        return orgIds;
    }
}


