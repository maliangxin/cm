package com.yonyoucloud.fi.cmp.accountregularbalance.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accountfixedbalance.AccountFixedBalance;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("beforeDeleteFixedBalanceRule")
public class BeforeDeleteFixedBalanceRule extends AbstractCommonRule {

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<AccountFixedBalance> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size()>0) {
            AccountFixedBalance balance = bills.get(0);
            // 查询企业银行账户实体
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(balance.getEnterpriseBankAccount());
            QuerySchema schema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            if (balance.getAccentity() != null) {
                conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(balance.getAccentity()));
            }
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(balance.getEnterpriseBankAccount()));
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(balance.getCurrency()));
            conditionGroup.appendCondition(QueryCondition.name("balancedate").eq(balance.getBalancedate()));
            schema.addCondition(conditionGroup);
            List<AccountFixedBalance> existBalances = MetaDaoHelper.queryObject(AccountFixedBalance.ENTITY_NAME, schema, null);
            existBalances.forEach(e -> {
                if (e.getIsconfirm()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101391"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19C695180538000D","[%s] 账户的日期 [%s] 的数据已进行确认，不允许删除，请检查！") /* "[%s] 账户的日期 [%s] 的数据已进行确认，不允许删除，请检查！" */, enterpriseBankAcctVO.getAcctName(), sdf.format(balance.getBalancedate())));
                }
            });
        }
        return new RuleExecuteResult();
    }

}
