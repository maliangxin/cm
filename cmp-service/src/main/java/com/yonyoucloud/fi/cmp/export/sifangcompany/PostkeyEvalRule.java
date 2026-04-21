package com.yonyoucloud.fi.cmp.export.sifangcompany;

import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;

import java.util.Map;

/*
 *@author lixuejun
 *@create 2020-08-30-18:46
 */
public class PostkeyEvalRule implements ConditionHandler {
    //记账码
    @Override
    public Object handler(Object o1, Object o2, Object o3) throws Exception {
        if (o1 instanceof CmpExportMap) {
            CmpExportMap cmpExportMap = (CmpExportMap) o1;
            String sourceEntityattrFactors = cmpExportMap.getSourceEntityattrFactors();
            String[] split = sourceEntityattrFactors.split(",");
            if (o2 instanceof Map) {
                Map<String, Object> oldMap = (Map<String, Object>) o2;
                Object dcFlagObject = oldMap.get(split[0]);
                if (dcFlagObject != null) {
                    String dcFlag = "";
                    if (dcFlagObject instanceof CellData) {
                        if (((CellData) dcFlagObject).getValue()!=null){
                            dcFlag = ((CellData) dcFlagObject).getValue().toString();
                        }
                    } else {
                        dcFlag = dcFlagObject.toString();
                    }
                    String extra1 = cmpExportMap.getExtra1();
                    String[] split1 = extra1.split(",");
                    if (dcFlag != null && !"".equals(dcFlag) && ((Short)Direction.Debit.getValue()).toString().equals(dcFlag)) {
                        //取固定值
                        String s = split1[1].toString();
                        return s;

                    } else {
                        return split1[0].toString();

                    }

                }
            }

        }


        return "";
    }
}
