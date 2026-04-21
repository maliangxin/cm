package com.yonyoucloud.fi.cmp.util;


import lombok.extern.slf4j.Slf4j;

/*
 *@author lixuejun
 *@create 2020-09-01-19:04
 */
@Slf4j
public class CommonFactory {
    public static <T> T getInstance(String classPath) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        T t = null;
        try {
            t = (T) Class.forName(classPath).newInstance();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
        return t;
    }
}
