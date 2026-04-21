/*
package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.framework.sdk.common.relevant.adapt.IRuleEngineDelegateService;
import com.yonyou.iuap.framework.sdk.common.relevant.model.BillRelevantRuleDefine;
import com.yonyou.iuap.framework.sdk.common.relevant.service.BillRelevantRuleCollector;
import com.yonyou.ucf.mdd.ext.dao.meta.UIMetaUtils;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.BillContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.model.Entity;
import org.springframework.beans.factory.annotation.Autowired;

*/
/**
 * @Description: 重写相关性规则查询服务
 * 需要过滤掉银行对账单中的辨识、冻结和生单规则
 * @Author: gengrong
 * @createTime: 2022/10/28
 * @version: 1.0
 *//*

// 海康使用 打开  @Service
@Slf4j
// @Service("relevantRuleService")
public class CmpRelevantRuleService {

    @Autowired
    private IRuleEngineDelegateService engineDelegateService;

    CmpRelevantQueryServiceImpl cmpRelevantQueryService;

    public BillRelevantRuleDefine getBillRelevantRules(String billnum, boolean excludeCG) throws Exception {
        cmpRelevantQueryService = new CmpRelevantQueryServiceImpl(engineDelegateService);
        */
/* 1. 获取单据业务对象 *//*

        BillContext billContext = BillContextUtils.getBillContext(billnum);
        Entity metaDataClass = UIMetaUtils.getMainEntityByBillNumber(billContext);
        if (null == metaDataClass)
            return new BillRelevantRuleCollector().createResult(InvocationInfoProxy.getTenantid());
        return cmpRelevantQueryService.queryAllRelevantRules(metaDataClass, InvocationInfoProxy.getTenantid(), billContext.getMddBoId(), excludeCG, false);

    }
}
*/
