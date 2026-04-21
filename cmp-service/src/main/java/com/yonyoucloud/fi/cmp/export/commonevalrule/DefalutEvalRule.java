package com.yonyoucloud.fi.cmp.export.commonevalrule;

import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;

import java.util.Map;

/*
 *@author lixuejun
 *@create 2020-08-26-21:48
 */
public class DefalutEvalRule implements ConditionHandler {
    @Override
    public Object handler(Object o1, Object o2, Object o3)throws Exception {
        if (o1 instanceof CmpExportMap) {
            CmpExportMap cmpExportMap = (CmpExportMap) o1;
            String sourceEntityattrFactors = cmpExportMap.getSourceEntityattrFactors();
            String[] split = sourceEntityattrFactors.split(",");
            if (o2 instanceof Map) {
                Map<String, Object> oldMap = (Map<String, Object>) o2;
                Object o = oldMap.get(split[0]);
                if (o != null) {
                    if (o instanceof CellData) {
                        if (((CellData) o).getValue()!=null){
                            return ((CellData) o).getValue().toString();
                        }
                    } else {
                        return o.toString();
                    }
                }
            }
        }

        return "";
    }
}