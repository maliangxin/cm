package com.yonyoucloud.fi.cmp.aop;


import com.yonyoucloud.fi.cmp.billclaim.BillClaim;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Description: 我的认领权限管控按钮
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ButtonAuth {

    //默认编辑
    DataAuthActionEnum supportAction() default DataAuthActionEnum.ACTION_BILL_AUDIT;

    //业务对象
    String fullName() default BillClaim.ENTITY_NAME;
}
