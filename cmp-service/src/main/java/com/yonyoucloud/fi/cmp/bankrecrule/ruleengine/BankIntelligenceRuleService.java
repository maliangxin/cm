package com.yonyoucloud.fi.cmp.bankrecrule.ruleengine;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;


/**
 * @description: 银行对账单智能规则执行接口
 * @author: wangdengk@yonyou.com
 * @date: 2023/06/13 11:40
 */

public interface BankIntelligenceRuleService {

    /*
     *@Description 银行对账单后台任务辨识处理
     *@Date 2023/06/13 14:36
     **/
    CtmJSONObject executeIdentificationRule(CtmJSONObject params) throws Exception;
    /*
     *@Description 银行对账单后台任务生单处理
     *@Date 2023/06/13 14:36
     **/
    String executeGenerateBillRule(CtmJSONObject params) throws Exception;
    /*
     *@Description 银行对账单后台任务提前入账处理
     *@Date 2023/06/13 14:36
     **/
    String executeAdvanceEnterRule(CtmJSONObject params) throws Exception;
}
