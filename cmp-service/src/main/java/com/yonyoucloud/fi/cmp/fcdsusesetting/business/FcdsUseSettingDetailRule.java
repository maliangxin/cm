package com.yonyoucloud.fi.cmp.fcdsusesetting.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author guoxh
 */
@RequiredArgsConstructor
@Slf4j
@Component("fcdsUseSettingDetailRule")
public class FcdsUseSettingDetailRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        Object data = map.get("return");
        if(data != null){
            BizObject bizObject = (BizObject) data;
            if(bizObject.containsKey("fcDsUseSettingList")){
                List<BizObject> fcDsUseSettingList =  bizObject.get("fcDsUseSettingList");
                Set<String> cdpIds = fcDsUseSettingList.stream().filter(item -> item.containsKey("cdp") && item.get("cdp") != null).map(item -> item.getString("cdp")).collect(Collectors.toSet());
                if(CollectionUtils.isNotEmpty(cdpIds)) {
                    QuerySchema querySchema = QuerySchema.create().addSelect("id,mainid.name").appendQueryCondition(
                            QueryCondition.name("id").in(cdpIds)
                    );
                    List<BizObject> mainNames = MetaDaoHelper.queryObject("yonbip-fi-ctmtmsp.yonbip-fi-ctmtmsp.tmsp_cdp_ds", querySchema, "yonbip-fi-ctmtmsp");
                    if (CollectionUtils.isNotEmpty(mainNames)) {
                        Map<String, String> cdpMainMap = mainNames.stream().collect(Collectors.toMap(item -> item.getString("id"), item -> item.getString("mainid_name"), (k1, k2) -> k1));
                        for (BizObject biz : fcDsUseSettingList) {
                            if (biz.containsKey("cdp") && biz.get("cdp") != null) {
                                biz.put("cdp_mainid", cdpMainMap.get(biz.getString("cdp")));
                            }
                        }
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
