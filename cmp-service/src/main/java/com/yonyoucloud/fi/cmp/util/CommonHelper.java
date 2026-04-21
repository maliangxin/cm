package com.yonyoucloud.fi.cmp.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashMap;

/**
 * <h1>description</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021-06-17 21:58
 */
public class CommonHelper {
    public static BigDecimal getBigDecimal(Object obj) {
        if (obj == null)
            return BigDecimal.ZERO;
        if (obj instanceof Long) {
            return new BigDecimal(Long.toString((Long) obj));
        } else if (obj instanceof Integer) {
            return new BigDecimal(Integer.toString((Integer)obj));
        } else if (obj instanceof Double) {
            return new BigDecimal(Double.toString((Double)obj));
        } else if (obj instanceof String) {
            return new BigDecimal((String) obj);
        } else if (obj instanceof LinkedHashMap) {
            return BigDecimal.ZERO;
        } else if (obj instanceof BigDecimal) {
            return (BigDecimal)obj;
        }
        return new BigDecimal(((BigDecimal) obj).doubleValue(), MathContext.DECIMAL64);
    }

}
