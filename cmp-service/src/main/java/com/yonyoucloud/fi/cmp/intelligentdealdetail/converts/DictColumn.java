package com.yonyoucloud.fi.cmp.intelligentdealdetail.converts;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DictColumn {

    String detailField() default "";
    String reconciliationField() default "";

    String odsField() default "";

}
