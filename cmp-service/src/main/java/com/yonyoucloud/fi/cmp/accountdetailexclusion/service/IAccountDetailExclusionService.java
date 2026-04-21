package com.yonyoucloud.fi.cmp.accountdetailexclusion.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.CullingStatus;

import javax.servlet.http.HttpServletResponse;

/**
 * IAccountDetailExclusionCommonService
 *
 * @author jpk
 * @version 1.0
 */
public interface IAccountDetailExclusionService {

    /**
     * * 更新银行对账单剔除状态
     * @param accountDetailExclusionId 剔除单Id
     * @param cullingStatus 剔除状态
     * @throws Exception
     */
    void updateBankreconciliationExclusion(Long accountDetailExclusionId , CullingStatus cullingStatus) throws Exception;

    /*
     *@Description 计算收入支出剔除总额
     *@Date 2023/11/09 14:36
     **/
    CtmJSONObject calculateExcludingAmount(CtmJSONObject params, HttpServletResponse response) throws Exception;
}
