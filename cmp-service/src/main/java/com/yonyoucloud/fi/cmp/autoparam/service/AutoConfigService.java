package com.yonyoucloud.fi.cmp.autoparam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;

import java.util.List;
import java.util.Map;

public interface AutoConfigService {

    /**
     * 设置默认自动化参数
     *
     * @return
     */
    JsonNode setDefault(Long id) throws Exception;

    /**
     * 根据前端传递的会计主体数组 判断是否需要校验ukey
     *
     * @param params
     * @return
     * @throws Exception
     */
    CtmJSONObject getCheckUkey(CtmJSONObject params, boolean isNewLogic) throws Exception;

    /**
     * 获取现金参数-转账单是否推送结算状态
     *
     * @return
     * @throws Exception
     */
    Boolean getCheckFundTransfer() throws Exception;

    Boolean getCheckBalanceIsQuery(String accentity) throws Exception;

    String getQueryBillType(String accentity) throws Exception;

    /**
     * 获取现金参数-转账单是否推送结算状态
     *
     * @return
     * @throws Exception
     */
    Boolean getCheckFundTransferForAssociation() throws Exception;


    /**
     * 获取现金参数 - 重空凭证参数
     *
     * @return
     * @throws Exception
     */
    Boolean getCheckStockIsUse() throws Exception;

    /**
     * 获取全局参数
     *
     * @return
     * @throws Exception
     */
    List<Map<String, Object>> getGlobalConfig() throws Exception;

    AutoConfig getGlobalConfigEntity() throws Exception;


    /**
     * 获取现金参数-我的认领是否需要复核
     *
     * @return
     * @throws Exception
     */
    Boolean getIsRecheck() throws Exception;

    //获取认领时是否启用资金中心代理模式 参数是否启用
    Boolean getEnableBizDelegationMode() throws Exception;

    //认领时是否启用统收统支模式 参数是否启用
    Boolean getUnifiedIEModelWhenClaim() throws Exception;

    /**
     * 根据会计主体查询现金参数
     *
     * @param accentity
     * @return
     * @throws Exception
     */
    AutoConfig queryAutoConfigByAccentity(String accentity) throws Exception;

    /**
     * 根据会计主体id查询现金参数
     *
     * @param accentity
     * @return
     * @throws Exception
     */
    AutoConfig getAutoConfigByAcc(String accentity) throws Exception;

    //银企直连账户的银行对账单是否允许维护 参数是否启用  默认是true
    Boolean getBankreconciliationCanUpdate() throws Exception;

    //判断支票是否启用领用
    Boolean getCheckStockCanUse() throws Exception;

    Boolean isPushHistory() throws Exception;

    List<String> getAccentityListNoProecss() throws Exception;

    /**
     * 获取企业银行账号级-日记账余额排序规则
     * @return
     * @throws Exception
     */
    Short getJournalBalanceSortRule() throws Exception;

}