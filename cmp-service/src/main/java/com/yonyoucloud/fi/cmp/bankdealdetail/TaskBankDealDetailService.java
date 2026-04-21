package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;

import java.util.List;
import java.util.Map;

public interface TaskBankDealDetailService {

    public ThreadResult bankTradeDetailAsyncProcess(CtmJSONObject paramNew, Boolean isHistory, String logId, Map<String ,List<EnterpriseBankAcctVO>> bankAccountsGroup) throws Exception;

    Map<String ,List<EnterpriseBankAcctVO>> bankTradeDetailQueryAccounts(CtmJSONObject param) throws Exception;

    //public  void bankTradeDetailAsyncProcess(CtmJSONObject paramNew, Boolean isHistory, String logId) throws Exception;
}
