package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model;

import lombok.Data;

/**
 * @Author guoyangy
 * @Date 2024/6/26 22:54
 * @Description todo
 * @Version 1.0
 */
@Data
public class YQLDataAccessModel {
    String requestParam;//请求报文
    String response;//响应报文
    String usedTime;//耗时
    String operType;// 1-调度任务拉取
    String bankaccountId;//银行账户id
    String currencyId;//币种
    String orgId;//所属组织id
    //流水接入组件为该字段赋值
    String requestSeqNo;
}
