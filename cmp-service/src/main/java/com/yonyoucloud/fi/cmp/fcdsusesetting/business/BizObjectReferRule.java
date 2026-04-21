package com.yonyoucloud.fi.cmp.fcdsusesetting.business;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.util.CTMAuthHttpClientUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务对象类型参照
 * https://gfwiki.yyrd.com/pages/viewpage.action?pageId=46538485   这个#9+#11  或者#13 查询数据
 * @author guoxh
 */
@Slf4j
@RequiredArgsConstructor
@Component("bizObjectReferRule")
public class BizObjectReferRule extends AbstractCommonRule {
    @Value("${domain.iuap-metadata-designer}")
    private String designerUrl;

    /**
     * https://gfwiki.yyrd.com/pages/viewpage.action?pageId=22537536
     * @param billContext
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<Map<String,Object>> result = new ArrayList();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = billDataDto.getCondition();

        Map<String, String> queryParam = new HashMap<String, String>();
        queryParam.put("yht_access_token", InvocationInfoProxy.getYhtAccessToken());
        Map<String, String> header = new HashMap<String, String>();
        header.put("yht_access_token", InvocationInfoProxy.getYhtAccessToken());
//        header.put("isTenant", "true");
//        String json = "{\"type\":\"BUSINESS_OBJECT\",\"fillDomain\":true,\"removeEmptyNode\":false,\"fillBillNos\":true}";
        String json = "{\"type\":\"BUSINESS_OBJECT\",\"fillDomain\":false,\"removeEmptyNode\":true,\"fillBillNos\":false}";
        //https://bip-test.yonyoucloud.com/iuap-metadata-designer/metadata-designer/businessobject/getDomainTree
        String res = CTMAuthHttpClientUtils.execPost(designerUrl + "/metadata-designer/businessobject/getDomainTree", queryParam, header, json);
        CtmJSONObject parseObject = CtmJSONObject.parseObject(res);
        CtmJSONArray array = parseObject.getJSONArray("data");
        return new RuleExecuteResult(array);
//        for (Object object : array) {
//            HashMap label = (HashMap) object;
//
//            Map<String, Object> labelMap = new HashMap<>();
//            labelMap.put("id", label.get("id"));
//            labelMap.put("code", label.get("code"));
//            labelMap.put("name", label.get("name"));
//            labelMap.put("isLeaf", label.get("isLeaf"));
//            labelMap.put("orders", label.get("order"));
//
//            if (label.containsKey("children") && label.get("children") != null) {
//                ArrayList appArr = (ArrayList) label.get("children");
////                log.error("children size :{}", appArr.size());
//                List<Map<String, Object>> appChildren = new ArrayList<>(appArr.size());
//                for (Object value : appArr) {
//                    HashMap app = (HashMap) value;
//
//                    HashMap<String, Object> appMap = new HashMap<>();
//                    appMap.put("parentId", app.get("parentId"));
//                    appMap.put("id", app.get("id"));
//                    appMap.put("code", app.get("code"));
//                    appMap.put("name", app.get("name"));
//                    appMap.put("order", app.get("order"));
//                    appMap.put("isLeaf", app.get("isLeaf"));
//
//                    if (app.containsKey("children") && app.get("children") != null) {
//                        ArrayList bizArr = (ArrayList) app.get("children");
//                        List<Map<String, Object>> bizChildren = new ArrayList<>(bizArr.size());
//                        for (Object obj : bizArr) {
//                            HashMap<String, Object> biz = (HashMap) obj;
//
//                            Map<String, Object> bizObject = new HashMap<>();
//                            bizObject.put("parentId", biz.get("parentId"));
//                            bizObject.put("id", biz.get("id"));
//                            bizObject.put("code", biz.get("code"));
//                            bizObject.put("name", biz.get("name"));
//                            bizObject.put("order", biz.get("order"));
//                            bizObject.put("type", biz.get("business"));
//                            bizObject.put("billNo", biz.get("billNo"));
//                            bizObject.put("serviceCodes", biz.get("serviceCodes"));
//                            bizObject.put("isLeaf", biz.get("isLeaf"));
//                            bizChildren.add(bizObject);
//                        }
//                        appMap.put("children", bizChildren);
//                        appChildren.add(appMap);
//                    }
//                }
//                labelMap.put("children", appChildren);
//            }
//            result.add(labelMap);
//        }
//
//        return new RuleExecuteResult(result);
    }
}
