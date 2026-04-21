package com.yonyoucloud.fi.cmp.denominationSetting.rule;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.denominationSetting.DenominationsettingService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("denominationsettingDeleteRule")
public class DenominationsettingDeleteRule extends AbstractCommonRule {

    @Autowired
    DenominationsettingService denominationsettingService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);

        if (bills != null && bills.size() > 0) {
            BizObject bizObject = bills.get(0);

            Long id = bizObject.get("id");

            CtmJSONObject result = denominationsettingService.checkDenominationsetting(id);

            Boolean isQuoted = (Boolean) result.get("isQuoted");

            if (!isQuoted) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101666"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802ED","该币种已被现金盘点表引用，不允许删除") /* "该币种已被现金盘点表引用，不允许删除" */);
            }
        }

        return new RuleExecuteResult();
    }
}
