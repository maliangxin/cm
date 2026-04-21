package com.yonyoucloud.fi.cmp.withholdingrulesetting.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.withholding.WithholdingRuleSetting;

import java.util.List;

/**
 * 预提规则设置业务接口*
 *
 * @author xuxbo
 * @date 2023/4/19 13:53
 */
public interface WithholdingRuleSettingService {

    /**
     * 账户同步*
     *
     * @return
     */
    int synchronousAccount();

    /**
     * 预提规则设置 提交保存接口*
     *
     * @param withholdingRuleSetting
     * @return
     * @throws Exception
     */
    CtmJSONObject withholdingRuleSettingSave(WithholdingRuleSetting withholdingRuleSetting) throws Exception;

    /**
     * 仅仅银行账户信息设置详情
     * @param id
     * @throws Exception
     */
    WithholdingRuleSetting onlyWithholdingRuleSetting(Long id) throws Exception;

    /**
     * 协定利率设置详情
     * @param id
     * @throws Exception
     */
    WithholdingRuleSetting agreeIRSettingHistoryDetail(Long id) throws Exception;


    /**
     * 协定利率设置删除
     * @param id
     * @throws Exception
     */
    void agreeIRSettingHistoryDelete(Long id) throws Exception;

    /**
     * 启停用状态更新*
     * @param data
     * @param type
     * @return
     * @throws Exception
     */
    CtmJSONObject updateStatus(List<WithholdingRuleSetting> data, Short type) throws Exception;


}
