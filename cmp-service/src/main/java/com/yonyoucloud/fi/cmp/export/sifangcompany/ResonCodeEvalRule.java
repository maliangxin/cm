package com.yonyoucloud.fi.cmp.export.sifangcompany;

import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.SAPUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/*
 *@author lixuejun
 *@create 2020-08-30-19:13
 */
public class ResonCodeEvalRule implements ConditionHandler {
    @Autowired
    private CmCommonService cmCommonService;
    //  SAP导出，付款数据，根据对账单上的银行交易流水号和付款单上的银行交易流水号进行对应，找到原始付款单上的项目信息；银行扣款，没有项目信息。收款数据，全部默认“101”

    //原因代码
    @Override
    public Object handler(Object o1, Object o2, Object o3) throws Exception {
        if (o1 instanceof CmpExportMap) {
            CmpExportMap cmpExportMap = (CmpExportMap) o1;
            String sourceEntityattrFactors = cmpExportMap.getSourceEntityattrFactors();
            String[] split = sourceEntityattrFactors.split(",");
            if (o2 instanceof Map) {
                Map<String, Object> oldMap = (Map<String, Object>) o2;
                Object bankSeqNoObject = oldMap.get(split[0]);
                Object dlFlagObject = oldMap.get(split[1]);
                String bankSeqNo = "";
                //借贷标志
                String dlFlag = "";

                if (bankSeqNoObject != null) {
                    if (bankSeqNoObject instanceof CellData) {
                        if (((CellData) bankSeqNoObject).getValue() != null) {
                            bankSeqNo = ((CellData) bankSeqNoObject).getValue().toString();
                        }
                    } else {
                        bankSeqNo = bankSeqNoObject.toString();
                    }

                }

                if (dlFlagObject != null) {
                    if (dlFlagObject instanceof CellData) {
                        if (((CellData) dlFlagObject).getValue() != null) {
                            dlFlag = ((CellData) dlFlagObject).getValue().toString();
                        }
                    } else {
                        dlFlag = dlFlagObject.toString();
                    }

                }

                if (!StringUtils.isEmpty(dlFlag)&&((Short)Direction.Debit.getValue()).toString().equals(dlFlag)) {
                    if (!StringUtils.isEmpty(bankSeqNo)){
                        String projectName = getProjectName(split[0], dlFlag, bankSeqNo);
                        if(StringUtils.isEmpty(projectName)){
                            return "";
                        }else {
                            return projectName;
                        }
                    }else {
                        return "";
                    }
                } else {
                    String extra1 = cmpExportMap.getExtra1();
                    if(StringUtils.isEmpty(extra1)){
                        return "101";
                    }
                    return extra1;
                }
            }
        }
        return "";
    }


    //得到对应的项目名
    public String getProjectName(String bankSeqNoColume, String dlFlag, String bankSeqNo) throws Exception {
        //先从上下文中获取数据
        Map<String, Object> sapexportContext = SAPUtils.getSapexportContext();
        Map<String, String> resonCodeMap = (Map<String, String>) sapexportContext.get(SAPUtils.ResonCode);
        if (resonCodeMap.isEmpty()) {
            return "";
        } else {
            String projectName = resonCodeMap.get(bankSeqNo);
            if (StringUtils.isEmpty(projectName)) {
                return "";
            } else {
                return projectName;
            }
        }

    }
}