package com.yonyoucloud.fi.cmp.export.commonevalrule;
/*
 * author lixuejun
 * create 2020-09-21-13:55
 */

import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;

public class NullEvalRule implements ConditionHandler {
    @Override
    public Object handler(Object o1, Object o2, Object o3) throws Exception {
        return "";
    }
}