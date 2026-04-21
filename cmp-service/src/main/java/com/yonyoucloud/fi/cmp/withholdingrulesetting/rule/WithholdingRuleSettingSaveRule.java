package com.yonyoucloud.fi.cmp.withholdingrulesetting.rule;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.model.LoginUser;
import com.yonyoucloud.fi.cmp.cmpentity.OptionType;
import com.yonyoucloud.fi.cmp.cmpentity.WithholdingRuleStatus;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.withholding.*;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预提规则设置保存规则*
 *
 * @author xuxbo
 * @date 2023/4/23 16:44
 */
@Component
public class WithholdingRuleSettingSaveRule extends AbstractCommonRule {

    private static final String END_DATE = "9999-12-31";

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (CollectionUtils.isNotEmpty(bills)) {
            WithholdingRuleSetting withholdingRuleSetting = (WithholdingRuleSetting) bills.get(0);
            //只有当规则设置状态为待设置的时候 才生成第一条利率变更历史记录 同时更新规则设置的状态为启用
            short ruleStatus = withholdingRuleSetting.getRuleStatus();
            if (ValueUtils.isNotEmptyObj(ruleStatus) && ruleStatus == WithholdingRuleStatus.Tobeset.getValue()) {
                //批量更新预提规则设置状态为启用
                for (BizObject bizobject : bills) {
                    bizobject.set("ruleStatus",WithholdingRuleStatus.Enable.getValue());
                }
            } else {
                return new RuleExecuteResult();
            }
        }
        return new RuleExecuteResult();
    }

}
