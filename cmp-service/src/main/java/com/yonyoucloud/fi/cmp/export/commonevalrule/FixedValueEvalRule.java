package com.yonyoucloud.fi.cmp.export.commonevalrule;

import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;

/*
 *@author lixuejun
 *@create 2020-08-26-21:48
 */
public class FixedValueEvalRule implements ConditionHandler {
    @Override
    public Object handler(Object o1,
                          Object o2,
                          Object o3) throws Exception{
        if (o1 instanceof CmpExportMap) {
            CmpExportMap cmpExportMap = (CmpExportMap) o1;
            String extra1 = cmpExportMap.getExtra1();
            if (extra1 != null &&!"".equals(extra1)) {
                return extra1;
            }
        }
        return "";
    }
}
