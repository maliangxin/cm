package com.yonyoucloud.fi.cmp.transferaccount.util;

import com.yonyoucloud.fi.cmp.common.CtmException;

import java.util.function.Supplier;

/**
 * @time：2023/3/3--08:46
 * @author：yanglu
 **/
public class AssertUtil {


    public static void isTrue(boolean condition, Supplier<CtmException> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }


    public static void isNull(Object obj, Supplier<CtmException> exceptionSupplier) {
        if (obj != null) {
            throw exceptionSupplier.get();
        }
    }

    public static void isNotNull(Object obj, Supplier<CtmException> exceptionSupplier) {
        if (obj == null) {
            throw exceptionSupplier.get();
        }
    }

    public static void isNotBlank(String obj, Supplier<CtmException> exceptionSupplier) {
        if (obj == null || obj.length() == 0) {
            throw exceptionSupplier.get();
        }
    }
}
