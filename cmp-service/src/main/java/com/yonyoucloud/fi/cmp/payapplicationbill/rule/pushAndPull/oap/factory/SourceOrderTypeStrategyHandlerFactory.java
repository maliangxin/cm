package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.factory;

import com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap.strategy.SourceOrderTypeStrategyHandler;
import com.yonyoucloud.fi.cmp.util.ValueUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <h1>工厂模式：注册策略实现类，根据类型获取相应的策略实现类</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-11-19 12:56
 */
public class SourceOrderTypeStrategyHandlerFactory {

    private static final Map<Short , SourceOrderTypeStrategyHandler> strategyMap = new HashMap<>();

    public static SourceOrderTypeStrategyHandler getInvokeStrategy(short sourceOrderType){
        return strategyMap.get(sourceOrderType);
    }

    public static void register(short sourceOrderType, SourceOrderTypeStrategyHandler handler){
        if (!ValueUtils.isNotEmptyObj(sourceOrderType) || null == handler){
            return;
        }
        strategyMap.put(sourceOrderType, handler);
    }
}
