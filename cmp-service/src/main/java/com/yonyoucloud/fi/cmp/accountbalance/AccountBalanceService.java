package com.yonyoucloud.fi.cmp.accountbalance;

import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AccountBalanceService {

    CtmJSONObject batchQueryAccountBalance(CtmJSONObject params) throws Exception;

    CtmJSONObject batchQueryAccountTransactionDetail(CtmJSONObject params) throws Exception;
}
