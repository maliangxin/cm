package com.yonyoucloud.fi.cmp.common.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类说明注解
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
public @interface ClassDescription {

    String message();

    String[] tableNames();



}
