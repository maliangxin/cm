package com.yonyoucloud.fi.cmp.export.sifangcompany;

import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;
import com.yonyoucloud.fi.cmp.util.SAPUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/*
 *@author lixuejun
 *@create 2020-08-28-20:53
 */
public class TradePartnerEvalRule implements ConditionHandler {
    @Autowired
    private CmCommonService cmCommonService;

    //根据会计主体id 查询对方用户名，与对方户名比对，比对上取默认值999
    @Override
    public Object handler(Object o1, Object o2, Object o3) throws Exception {
        if (o1 instanceof CmpExportMap) {
            CmpExportMap cmpExportMap = (CmpExportMap) o1;
            String sourceEntityattrFactors = cmpExportMap.getSourceEntityattrFactors();
            String[] split = sourceEntityattrFactors.split(",");
            if (o2 instanceof Map) {
                Map<String, Object> oldMap = (Map<String, Object>) o2;
                Object o = oldMap.get(split[0]);
                if (o != null) {
                    Map<String, String> orgs = getOrgs();
                    if (o instanceof CellData) {
                        if (((CellData) o).getValue() != null) {
                            String toAcctName = ((CellData) o).getValue().toString();
                            return getTradePartnerCode(toAcctName, orgs, cmpExportMap);
                        }

                    } else {
                        return getTradePartnerCode(o.toString(), orgs, cmpExportMap);
                    }
                }
            }
        }
        return "";
    }


    public Map<String, String> getOrgs() throws Exception {
        Map<String, Object> sapexportContext = SAPUtils.getSapexportContext();
        return (Map<String, String>) sapexportContext.get(SAPUtils.OrgsKey);
    }

    public String getTradePartnerCode(String toAcctName, Map<String, String> orgs, CmpExportMap cmpExportMap) throws Exception {
        boolean b = orgs.containsKey(toAcctName);
        if (b) {
            if (orgs.get(toAcctName) != null && !orgs.get(toAcctName).equals("")) {
                //返回组织单元编码
                return orgs.get(toAcctName);
            } else {
                //取默认
                return cmpExportMap.getExtra1();
            }

        } else {
            //取默认
            return cmpExportMap.getExtra1();
        }
    }
}