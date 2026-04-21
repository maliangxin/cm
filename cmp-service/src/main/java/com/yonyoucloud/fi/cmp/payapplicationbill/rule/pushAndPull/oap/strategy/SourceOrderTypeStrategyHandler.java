package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.strategy;

import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;

/**
 * <h1>策略模式：应付事项推付款申请根据源头来源订单类型策略处理接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-11-19 12:51
 */
public interface SourceOrderTypeStrategyHandler extends InitializingBean {
    /**
     * <h2>处理应付事项推过来的单据</h2>
     * @author Sun GuoCai
     * @date 2022/11/19 12:58
     * @param list: 上游过来的单据明细
     * @return java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     */
    List<Map<String, Object>> process(List<Map<String, Object>> list,Map<String, Object> map) throws Exception;
}
