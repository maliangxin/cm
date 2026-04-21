package com.yonyoucloud.fi.cmp.bankreconciliationrepeat.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 疑重列表数据参照过滤
 *
 * @author guoxh
 */
@Component("bankreconciliationRepeatReferFilterRule")
@Slf4j
@RequiredArgsConstructor
public class BankreconciliationRepeatReferFilterRule extends AbstractCommonRule {
    private final OrgDataPermissionService orgDataPermissionService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        return new RuleExecuteResult();
    }
}
