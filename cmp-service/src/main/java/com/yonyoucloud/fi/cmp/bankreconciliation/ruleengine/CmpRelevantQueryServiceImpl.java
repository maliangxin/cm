/*
package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import com.yonyou.iuap.framework.sdk.common.relevant.adapt.IRuleEngineDelegateService;
import com.yonyou.iuap.framework.sdk.common.relevant.model.BillRelevantRuleDefine;
import com.yonyou.iuap.framework.sdk.common.relevant.service.BillRelevantRuleCollector;
import com.yonyou.iuap.framework.sdk.common.relevant.service.impl.BusiObjRelevantRuleQueryAndParse;
import com.yonyou.iuap.framework.sdk.common.relevant.service.impl.CharacterRelevantRuleQueryAndParse;
import com.yonyou.iuap.framework.sdk.common.relevant.service.impl.RelevantQueryServiceImpl;
import org.imeta.core.model.Entity;

public class CmpRelevantQueryServiceImpl extends RelevantQueryServiceImpl {

    private BusiObjRelevantRuleQueryAndParse busiObjRelevantRuleQueryAndParse;
    private CharacterRelevantRuleQueryAndParse characterQueryAndParseService;
    private IRuleEngineDelegateService engineDelegateService;

    public CmpRelevantQueryServiceImpl(IRuleEngineDelegateService engineDelegateService) {
        super(engineDelegateService);
        this.engineDelegateService = engineDelegateService;
    }

    public BillRelevantRuleDefine queryAllRelevantRules(Entity entity, String ytenantId, String bizObjCode, boolean excludeCG, boolean keepOrigin) {
        busiObjRelevantRuleQueryAndParse = new CmpBusiObjRelevantRuleQueryAndParse(engineDelegateService);
        characterQueryAndParseService = new CmpCharacterRelevantRuleQueryAndParse(engineDelegateService);
        BillRelevantRuleCollector collector = new BillRelevantRuleCollector();
        if (null == entity) {
            return collector.createResult(ytenantId);
        } else {
            collector.setFullname(entity.fullname());
            collector.setBoid(bizObjCode);
            if (!excludeCG) {
                this.characterQueryAndParseService.queryAndParse(entity, collector, ytenantId, bizObjCode, keepOrigin);
            }

            this.busiObjRelevantRuleQueryAndParse.queryAndParse(entity, collector, ytenantId, bizObjCode, keepOrigin);
            return collector.createResult(ytenantId);
        }
    }
}
*/
