package com.yonyoucloud.fi.cmp.paymentbill.service;

/**
 * desc: 银行账户实时余额在线查询异步处理计算进度百分比接口
 * author:wangqiangac
 * date:2023/7/10 19:42
 */
public interface CalcProcessService {

    String getProcess(String uid);

    void testProcess(String uid) throws InterruptedException;

}
