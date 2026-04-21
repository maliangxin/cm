package com.yonyoucloud.fi.cmp.export.sifangcompany;

import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;
import com.yonyoucloud.fi.cmp.util.StringUtils;

import java.util.Map;

/*
 *@author lixuejun
 *@create 2020-08-26-21:48
 */
public class TextEvalRule implements ConditionHandler {
    @Override
    public Object handler(Object o1, Object o2, Object o3)throws Exception {
        if (o1 instanceof CmpExportMap) {
            CmpExportMap cmpExportMap = (CmpExportMap) o1;
            String sourceEntityattrFactors = cmpExportMap.getSourceEntityattrFactors();
            String[] split = sourceEntityattrFactors.split(",");
            if (o2 instanceof Map) {
                Map<String, Object> oldMap = (Map<String, Object>) o2;
                Object o = oldMap.get(split[0]);
                //用途
                Object userNameObject = oldMap.get(split[1]);
                String userName = "";
                String toAccntName="";
                if (userNameObject != null) {
                    if (userNameObject instanceof CellData) {
                        if (((CellData) userNameObject).getValue() != null) {
                            userName = ((CellData) userNameObject).getValue().toString();
                        }
                    } else {
                        userName = userNameObject.toString();
                    }
                }
                if (o != null) {
                    if (o instanceof CellData) {
                        if (((CellData) o).getValue()!=null){
                            toAccntName= ((CellData) o).getValue().toString();
                        }
                    } else {
                        toAccntName=o.toString();
                    }
                }
                return !StringUtils.isEmpty(userName)?toAccntName+" "+userName:toAccntName;
            }
        }
        return "";
    }
}