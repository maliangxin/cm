package com.yonyoucloud.fi.cmp.currencyapply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.currencyexchange.service.CurrencyExchangeService;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @description: 外币兑换申请删除规则
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/30 15:49
 */

@Slf4j
@Component("currencyApplyDeleteRule")
public class CurrencyApplyDeleteRule extends AbstractCommonRule {

    @Resource
    private CurrencyExchangeService currencyExchangeService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills == null || bills.size() == 0) {
            return new RuleExecuteResult();
        }

        BizObject bizObject = bills.get(0);
        Short auditStatus = -1;
        if (bizObject.get("auditstatus") != null) {
            auditStatus = Short.valueOf(bizObject.get("auditstatus").toString());
        }
        if (auditStatus.equals(AuditStatus.Complete.getValue())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101858"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802E8","已审核单据，不能进行删除！") /* "已审核单据，不能进行删除！" */);
        }

        //调用外币兑换单删除接口，一般弃审的时候已删除关联的外币兑换单
        currencyExchangeService.deleteCurrencyApply(bizObject.getId());

        return new RuleExecuteResult();
    }
}
