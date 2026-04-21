package com.yonyoucloud.fi.cmp.checkStockApply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @Author ningff
 * @Date 2024/9/14 11:38
 * @DESCRIPTION
 */
@Component("checkStockAddRule")
public class CheckStockAddRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            //单组织逻辑
            if (FIDubboUtils.isSingleOrg()) {
                BizObject singleOrg = FIDubboUtils.getSingleOrg();
                if (singleOrg != null) {
                    bizobject.set("accentity", singleOrg.get("id"));
                    bizobject.set("accentity_name", singleOrg.get("name"));
                }
            }
        }
        return new RuleExecuteResult();
    }
}
