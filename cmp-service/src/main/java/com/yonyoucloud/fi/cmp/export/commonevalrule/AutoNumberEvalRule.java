package com.yonyoucloud.fi.cmp.export.commonevalrule;

import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;

/*
 *@author lixuejun
 *@create 2020-08-26-21:48
 */
public class AutoNumberEvalRule implements ConditionHandler {
    @Override
    public Object handler(Object o1, Object o2, Object  o3) throws Exception{
        if (o3 instanceof Integer) {
            return String.valueOf((Integer) o3);
        }
        return "";
    }
}