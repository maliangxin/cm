package com.yonyoucloud.fi.cmp.util;

import com.yonyoucloud.fi.cmp.common.CtmException;

import java.math.BigDecimal;

/**
 * @ClassName TypeUtils
 * @Description 类型转换工具类
 * @Author tongyd
 * @Date 2019/7/12 16:54
 * @Version 1.0
 **/
public class TypeUtils {

    public static Short castToShort(Object value) throws CtmException {
        if (value == null) {
            return -1;
        } else if (value instanceof BigDecimal) {
            return shortValue((BigDecimal)value);
        } else if (value instanceof Number) {
            return ((Number)value).shortValue();
        } else if (value instanceof String) {
            String strVal = (String)value;
            return strVal.length() != 0 && !"null".equals(strVal) && !"NULL".equals(strVal) ? Short.parseShort(strVal) : -1;
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102020"),"can not cast to short, value : " + value);
        }
    }

    public static short shortValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        } else {
            int scale = decimal.scale();
            return scale >= -100 && scale <= 100 ? decimal.shortValue() : decimal.shortValueExact();
        }
    }
}
