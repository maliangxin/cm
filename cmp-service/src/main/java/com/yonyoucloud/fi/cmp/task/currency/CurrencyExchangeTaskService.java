package com.yonyoucloud.fi.cmp.task.currency;

import java.util.Map;

/**
 * 外币兑换-结售汇结果查询-定时任务
 * @author Administrator
 *
 */
public interface CurrencyExchangeTaskService {

	 /**
      * 外币兑换-结售汇结果查询-定时任务
      * @return
      * @throws Exception
      */
     Map<String,Object> queryResult(Map<String,Object> paramMap) throws Exception;
}
