package com.yonyoucloud.fi.cmp.export.conditionhandler;

import java.io.Serializable;

/*
 *@author lixuejun
 *@create 2020-08-26-22:09
 */
public interface ConditionHandler extends Serializable{
    long serialVersionUID = -1L;
    Object handler(Object o1, Object o2, Object o3) throws Exception;
}
