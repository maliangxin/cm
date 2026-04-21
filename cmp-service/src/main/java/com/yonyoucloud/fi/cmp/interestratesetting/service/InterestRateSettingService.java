package com.yonyoucloud.fi.cmp.interestratesetting.service;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.interestratesetting.InterestRateSetting;

/**
 * 银行利率设置接口*
 *
 * @author xuxbo
 * @date 2023/4/25 19:40
 */

public interface InterestRateSettingService {

    /**
     * 银行利率设置提交保存接口*
     *
     * @param interestRateSetting
     * @return
     * @throws Exception
     */
    CtmJSONObject interestRateSettingSave(InterestRateSetting interestRateSetting) throws Exception;

    /**
     * 银行利率设置提交保存接口*
     *
     * @param interestRateSetting
     * @return
     * @throws Exception
     */
    CtmJSONObject agreeRateSettingSave(CtmJSONObject interestRateSetting) throws Exception;

    /**
     * 银行利率设置提交保存接口*
     *
     * @param interestRateSetting
     * @return
     * @throws Exception
     */
    RuleExecuteResult agreeRateSettingSavedetail(CtmJSONObject interestRateSetting) throws Exception;

    /**
     * 银行利率设置提交保存接口*
     *
     * @param interestRateSetting
     * @return
     * @throws Exception
     */
    RuleExecuteResult agreeRateSettingdelete(CtmJSONObject interestRateSetting) throws Exception;

    void agreeRateSettingSavedetailList(CtmJSONObject bill) throws Exception;
}
