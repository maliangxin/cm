package com.yonyoucloud.fi.cmp.transferaccount.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;

/**
 * 转账单定时任务服务
 */
public interface TransferAccountTaskService {

    String BATCH_PAY_DETAIL_STATUS_QUERY = "40B10";

    Map queryPayStatus(CtmJSONObject param);

}
