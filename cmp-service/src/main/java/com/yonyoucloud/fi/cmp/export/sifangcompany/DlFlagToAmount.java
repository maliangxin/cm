package com.yonyoucloud.fi.cmp.export.sifangcompany;

import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;

import java.util.Map;

/*
 *@author lixuejun
 *@create 2020-08-30-19:13
 */
public class DlFlagToAmount implements ConditionHandler {
    //借方和贷方金额
    @Override
    public Object handler(Object o1, Object o2, Object o3) throws Exception {
        if (o1 instanceof CmpExportMap) {
            CmpExportMap cmpExportMap = (CmpExportMap) o1;
            String sourceEntityattrFactors = cmpExportMap.getSourceEntityattrFactors();
            String[] split = sourceEntityattrFactors.split(",");
            if (o2 instanceof Map) {
                Map<String, Object> oldMap = (Map<String, Object>) o2;
                Object dcFlagObject = oldMap.get(split[0]);
                String dcFlag = "";
                if (dcFlagObject != null) {
                    if (dcFlagObject instanceof CellData) {
                        if (((CellData) dcFlagObject).getValue() != null) {
                            dcFlag = ((CellData) dcFlagObject).getValue().toString();
                        }
                    } else {
                        dcFlag = dcFlagObject.toString();
                    }
                    if (dcFlag != null && !"".equals(dcFlag) && ((Short)Direction.Debit.getValue()).toString().equals(dcFlag)) {
                        String dAmount = "";
                        Object debitAmount = oldMap.get(split[1]);
                        if (debitAmount != null) {
                            if (debitAmount instanceof CellData) {
                                dAmount = ((CellData) debitAmount).getValue().toString();
                            } else {
                                dAmount = debitAmount.toString();
                            }

                        }
                        return dAmount;

                    } else {
                        String dAmount = "";
                        Object createAmount = oldMap.get(split[2]);
                        if (createAmount != null) {
                            if (createAmount instanceof CellData) {
                                dAmount = ((CellData) createAmount).getValue().toString();
                            } else {
                                dAmount = createAmount.toString();
                            }
                        }
                        return dAmount;


                    }

                }

            }
        }
        return "";
    }
}