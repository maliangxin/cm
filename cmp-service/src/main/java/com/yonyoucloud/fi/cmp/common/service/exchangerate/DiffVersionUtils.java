package com.yonyoucloud.fi.cmp.common.service.exchangerate;

import com.yonyou.ucf.mdd.ext.core.AppContext;

public class DiffVersionUtils {
    private static final String CMP_VERSIONS="cmp_versions";

    /**
     * 获取当前版本
     * @return
     */
    public static String getVersion(){
        return AppContext.getEnvConfig("cmp_versions","R6");
    }
}
