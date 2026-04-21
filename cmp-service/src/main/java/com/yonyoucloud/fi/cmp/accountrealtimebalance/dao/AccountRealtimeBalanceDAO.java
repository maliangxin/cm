package com.yonyoucloud.fi.cmp.accountrealtimebalance.dao;

import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;

import java.util.List;

public interface AccountRealtimeBalanceDAO {
    List<AccountRealtimeBalance> queryTraceabilityBalance(List<String> enterpriseBankAccounts, List<String> accentitys, String currency, List<String> currencyList,String startDate, String endDate);
}
