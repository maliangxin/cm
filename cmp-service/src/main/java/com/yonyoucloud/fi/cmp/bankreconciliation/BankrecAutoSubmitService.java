package com.yonyoucloud.fi.cmp.bankreconciliation;

import java.util.List;

/**
 * 银行对账单提前入账智能规则生成资金首付款单自动提交接口
 * PS：湖南建投项目需求开发
 * author wq
 * date 2023年10月19日10:03:03
 */
public interface BankrecAutoSubmitService {
    void autoSubmit(String fullname, List<String> ids, String billnum) throws Exception;
}
