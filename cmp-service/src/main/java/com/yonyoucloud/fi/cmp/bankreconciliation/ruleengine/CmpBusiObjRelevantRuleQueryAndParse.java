/*
package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.framework.sdk.common.relevant.adapt.IRuleEngineDelegateService;
import com.yonyou.iuap.framework.sdk.common.relevant.model.RelevantRuleDefine;
import com.yonyou.iuap.framework.sdk.common.relevant.service.BillRelevantRuleCollector;
import com.yonyou.iuap.framework.sdk.common.relevant.service.impl.BusiObjRelevantRuleQueryAndParse;
import com.yonyoucloud.fi.cmp.constant.CmpRuleEngineTypeConstant;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.model.Entity;

import java.util.ArrayList;
import java.util.List;

public class CmpBusiObjRelevantRuleQueryAndParse extends BusiObjRelevantRuleQueryAndParse {

    protected CmpBusiObjRelevantRuleQueryAndParse(IRuleEngineDelegateService engineDelegateService) {
        super(engineDelegateService);
    }

    public void queryAndParse(Entity entity, BillRelevantRuleCollector collector, String ytenantId, String bizObjCode, boolean keepOrigin) {
        if (null != entity && !StringUtils.isBlank(bizObjCode)) {
            List<RelevantRuleDefine> busiRules = this.engineDelegateService.getRelevantRuleDefinesByBussObj(bizObjCode, ytenantId, keepOrigin);
            // 过滤海康威视专属化需求规则
            busiRules.removeIf(rule -> rule.getCode().startsWith(CmpRuleEngineTypeConstant.identification_prefix)
                    || rule.getCode().startsWith(CmpRuleEngineTypeConstant.freeze_prefix)
                    || rule.getCode().startsWith(CmpRuleEngineTypeConstant.generate_prefix));
            if (CollectionUtils.isEmpty(busiRules)) {
                //logger.info("当前业务对象{}，fullname:{}, tenant:{} 没有查询到相关性规则定义", new Object[]{bizObjCode, entity.fullname(), ytenantId});
            } else {
                //collectBusiRuleDefines(entity, collector, busiRules);
            }
        }
    }
}

*/
