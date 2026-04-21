package com.yonyoucloud.fi.cmp.util;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author shangxd
 * @date 2023/4/17 16:13
 * @describe
 */
public class DepositinterestWithholdingUtil {


    public static Date getMaxDate(Date a, Date b, Date c) {
        List<Date> list = Lists.newArrayList();
        if (a != null) {
            list.add(a);
        }
        if (b != null) {
            list.add(b);
        }
        if (c != null) {
            list.add(c);
        }
        Date max = Collections.max(list);
        list.clear();
        return max;
    }

}
