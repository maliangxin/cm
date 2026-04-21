package com.yonyoucloud.fi.cmp.weekday;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TlmCacheable {

    String[] keyELs() default {};

    TlmCacheType cacheType() default TlmCacheType.NORMAL;
}
