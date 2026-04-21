package com.yonyoucloud.fi.cmp.accountregularbalance.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accountfixedbalance.AccountFixedBalance;

import java.util.List;

/**
 * 定期余额服务类
 */
public interface AccountRegularBalanceService {

    /**
     * 余额确认
     * @param billList
     * @return
     * @throws Exception
     */
    CtmJSONObject confirmAccountBalance(List<AccountFixedBalance> billList) throws Exception;

    /**
     * 取消确认
     * @param billList
     * @return
     * @throws Exception
     */
    CtmJSONObject cancelConfirmAccountBalance(List<AccountFixedBalance> billList) throws Exception;

}
