package com.yonyoucloud.fi.cmp.smartclassify;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @description: 单据智能分类接口类
 * @author: wanxbo@yonyou.com
 * @date: 2022/7/18 15:01
 */

public interface BillSmartClassifyService {

    /**
     * 根据会计主体，对方账号，币种进行分类
     * @param accentity 会计主体ID
     * @param toaccountno 对方账号
     * @param toaccountname 对方户名
     * @param currency 币种ID
     * @return 分类结果
     */
    BillSmartClassifyBO smartClassify(String accentity,String toaccountno, String toaccountname,String currency,short flag) throws Exception;

    /**
     * 银行对账单对方单位为空时，后台自动辨识对方单位调度任务
     * @param params
     * @return
     * @throws Exception
     */
    boolean autoClassify(JsonNode params) throws Exception;
}
