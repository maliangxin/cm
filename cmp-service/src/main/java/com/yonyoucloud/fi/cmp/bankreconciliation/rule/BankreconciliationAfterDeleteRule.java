package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.alibaba.fastjson.JSON;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceServiceImpl;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * zxl
 * 删除银行账单-更新账户历史余额
 */
@Slf4j
@Component
public class BankreconciliationAfterDeleteRule extends AbstractCommonRule {
    @Autowired
    private AccountHistoryBalanceServiceImpl accountHistoryBalanceServiceImpl;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String check = AppContext.getEnvConfig("cmp.bankReconciliation.delete.check", "0");
        if (!CollectionUtils.isEmpty(bills) && StringUtils.equals("1", check)) {
            BankReconciliation bankReconciliation = (BankReconciliation) bills.get(0);
            log.error("01.删除bankReconciliation:" + JSON.toJSONString(bankReconciliation));
            //02.删除历史余额
            QuerySchema schema = QuerySchema.create().addSelect(" * ");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bankReconciliation.getAccentity()));
            conditionGroup.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(bankReconciliation.getBankaccount()));
            conditionGroup.appendCondition(QueryCondition.name("balancedate").egt(new SimpleDateFormat("yyyy-MM-dd").format(bankReconciliation.getTran_date())));
            conditionGroup.appendCondition(QueryCondition.name("currency").eq(bankReconciliation.getCurrency()));
            conditionGroup.appendCondition(QueryCondition.name("first_flag").not_eq("1"));
            conditionGroup.appendCondition(QueryCondition.name("datasource").in("4", "6"));
            schema.addCondition(conditionGroup);
            schema.addOrderBy("balancedate");
            List<AccountRealtimeBalance> allBalanceOfAccount = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema, null);
            log.error("02.删除历史余额:" + JSON.toJSONString(allBalanceOfAccount));
            if (!CollectionUtils.isEmpty(allBalanceOfAccount)) {
                MetaDaoHelper.delete(AccountRealtimeBalance.ENTITY_NAME, allBalanceOfAccount);
            } else {
                return new RuleExecuteResult();
            }
            //03.从当前余额日期的下一天开始补充缺失余额
            //删除单据日期前一天日期
            Date startDate = DateUtils.dateAdd(bankReconciliation.getTran_date(), -1, Boolean.FALSE);
            //今天日期
            Date endDate = DateUtils.getNowDateShort();
            //根据银行账号查询所有历史余额
            QuerySchema schema1 = QuerySchema.create().addSelect(" * ");
            QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup1.appendCondition(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bankReconciliation.getAccentity()));
            conditionGroup1.appendCondition(QueryCondition.name("enterpriseBankAccount").eq(bankReconciliation.getBankaccount()));
            conditionGroup1.appendCondition(QueryCondition.name("currency").eq(bankReconciliation.getCurrency()));
            schema1.addCondition(conditionGroup1);
            schema1.addOrderBy("balancedate");
            List<AccountRealtimeBalance> accountRealtimeBalanceList = MetaDaoHelper.queryObject(AccountRealtimeBalance.ENTITY_NAME, schema1, null);
            log.error("0303银行账号查询所有历史余额:" + JSON.toJSONString(accountRealtimeBalanceList));
            if (CollectionUtils.isEmpty(accountRealtimeBalanceList)) {
                return new RuleExecuteResult();
            }
            // 将当前银行账户历史余额存入
            Map<Object, BizObject> accountRealtimeBalanceMap = new HashMap<>();
            for (AccountRealtimeBalance accountRealtimeBalance : accountRealtimeBalanceList) {
                String balanceKey = new SimpleDateFormat("yyyy-MM-dd").format(accountRealtimeBalance.getBalancedate()) + accountRealtimeBalance.getCurrency();
                accountRealtimeBalanceMap.put(balanceKey, accountRealtimeBalance);
            }
            AccountRealtimeBalance nowAccountRealtimeBalance = accountRealtimeBalanceList.get(accountRealtimeBalanceList.size() - 1);
            //从当前余额日期的下一天开始补充缺失余额
            log.error("03.交易日期补充历史余额:" + JSON.toJSONString(accountRealtimeBalanceList));
            accountHistoryBalanceServiceImpl.supplementBalance(DateUtils.formatBalanceDate(startDate), endDate,
                    nowAccountRealtimeBalance, accountRealtimeBalanceMap);
        }
        return new RuleExecuteResult();
    }
}