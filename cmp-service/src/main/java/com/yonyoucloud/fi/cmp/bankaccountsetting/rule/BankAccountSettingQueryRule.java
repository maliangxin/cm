package com.yonyoucloud.fi.cmp.bankaccountsetting.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 同步账号功能迁移到Controller
 */
@Slf4j
public class BankAccountSettingQueryRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap)  {
        return new RuleExecuteResult();
    }

}
