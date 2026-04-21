package com.yonyoucloud.fi.cmp.bankreconciliation.service.banktorule;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;

import java.util.Map;

/**
 * 银行对账单/认领单转其他单据公共服务
 *
 * @Description
 * @Author hanll
 * @Date 2024/7/4-09:55
 */
public interface BankToCommonRuleService {

    /**
     * 填写对方信息
     *
     * @param mapSub   单据转换规则实体
     * @param paramMap 转单规则参数
     * @throws Exception
     */
    void fillOppositeInfo(Map<String, Object> mapSub, Map<String, Object> paramMap, Map<Long, BankReconciliation> bankReconciliationMap, Map<String, BillClaimItem> billClaimItemMap) throws Exception;
}
