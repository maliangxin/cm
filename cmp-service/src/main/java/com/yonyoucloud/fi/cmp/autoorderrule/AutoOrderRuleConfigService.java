package com.yonyoucloud.fi.cmp.autoorderrule;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;

/**
 * @description: 自动生单规则设置接口
 * @author: wanxbo@yonyou.com
 * @date: 2023/2/28 15:39
 */

public interface AutoOrderRuleConfigService {

    /**
     * 根据条件查询自动生单规则里 客户供应商的配置信息
     * @param params
     * @return
     * @throws Exception
     */
    List<AutoorderruleConfig> queryConfigInfo(AutoorderruleConfig autoorderruleConfig) throws Exception;

    /**
     * 更新自动生单规则里客户供应商的配置信息
     * @param params
     * @return
     * @throws Exception
     */
    CtmJSONObject updateConfigInfo(CtmJSONObject params) throws Exception;
}
