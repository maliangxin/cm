package com.yonyoucloud.fi.cmp.accounthistorybalance.rule;

import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceServiceImpl;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("afterQueryHistoryBalanceRule")
public class AfterQueryHistoryBalanceRule extends AbstractCommonRule {
    @Autowired
    private AccountHistoryBalanceServiceImpl accountHistoryBalanceServiceImpl;

    @Autowired
    BankAccountSettingService bankAccountSettingService;

    /**
     * 查询后规则
     *
     * @param billContext
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        Pager pager = (Pager) paramMap.get("return");
        List<Map> historyInfos = (List<Map>) pager.getRecordList();
        List<String> bankaccounts = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(historyInfos)) {
            for (Map historyBalanceInfo : historyInfos) {
                if (historyBalanceInfo.get("enterpriseBankAccount") != null && !StringUtils.isEmpty(historyBalanceInfo.get("enterpriseBankAccount").toString())) {
                    bankaccounts.add(historyBalanceInfo.get("enterpriseBankAccount").toString());
                }
            }
            List<Map<String, Object>> enterpriseBankAccounts = bankAccountSettingService.queryBankAccountSettingByBankAccounts(bankaccounts);
            for (Map historyBalanceInfo : historyInfos) {
                if (historyBalanceInfo.get("enterpriseBankAccount") != null) {
                    if (enterpriseBankAccounts.stream().anyMatch(e -> e.get("enterpriseBankAccount").equals(historyBalanceInfo.get("enterpriseBankAccount")))) {
                        historyBalanceInfo.put("openFlag", 1);//直连
                    } else {
                        historyBalanceInfo.put("openFlag", 0);
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
