package com.yonyoucloud.fi.cmp.fcdsusesetting.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author guoxh
 */
@Component("fcdsUseSettingBeforeDeleteRule")
@Slf4j
public class FcdsUseSettingBeforeDeleteRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (CollectionUtils.isEmpty(bills)) {
            new RuleExecuteResult();
        }
        BizObject bizObject = bills.get(0);
        if(bizObject != null && bizObject.get("enable") != null && "1".equals(bizObject.getString("enable"))){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100564"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CDA2A05180001","流程编码[%s]已启用，无法删除！"),bizObject.getString("code")));
        }
        if(bizObject != null && bizObject.get("isPreset")!= null && "1".equals(bizObject.getString("isPreset"))){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100565"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4CDA5404D80004","流程编码[%s]为预置数据，无法删除！"),bizObject.getString("code")));
        }
        return new RuleExecuteResult();
    }
}
