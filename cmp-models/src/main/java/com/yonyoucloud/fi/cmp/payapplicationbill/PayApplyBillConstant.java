package com.yonyoucloud.fi.cmp.payapplicationbill;

import com.google.common.collect.ImmutableSortedSet;

/**
 * <h1>description</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021-08-18 10:57
 */
public interface PayApplyBillConstant {
    ImmutableSortedSet<String> CTM_CACHE_KEY = ImmutableSortedSet.of(
            "viewmodel*",
            "MddExtbillrule_*",
            "enumCache*",
            "MddExtviewmodel_*",
            "MddExt3_*",
            "billrule*",
            "MddExtMENU*",
            "auth*",
            "option_*",
            "action*"
    );
}
