package com.yonyoucloud.fi.cmp.accounthistorybalance;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.cashhttp.CashHttpBankEnterpriseLinkVo;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @ClassName AccountHistoryBalanceService
 * @Description 账户历史余额接口
 * @Author wnagyao
 * @Date 2021/01/21
 * @Version 1.0
 **/
public interface AccountHistoryBalanceService {
    //账户实时余额历史数据查询
    String QUERY_HIS_ACCOUNT_BALANCE = "40T21";

    /**
     * 根据条件查询对应的历史余额账户
     * @param balance
     * @return
     * @throws Exception
     */
    public Map<String ,List<EnterpriseBankAcctVO>> getBankAccountsGroupForBalance(CtmJSONObject balance) throws Exception ;

    /**
     * 根据条件查询对应的历史余额账户
     * @param balance
     * @return
     * @throws Exception
     */
    public Map<String ,List<EnterpriseBankAcctVO>> getBankAccountsGroupForBalanceByTask(CtmJSONObject balance) throws Exception ;


    /**
     * 构建历史越查询条件
     * @param httpBankAccounts
     * @param balance
     * @return
     * @throws Exception
     */
    public List<CashHttpBankEnterpriseLinkVo>  querHttpAccount(List<EnterpriseBankAcctVO> httpBankAccounts, CtmJSONObject balance, boolean isTask) throws Exception;
    /**
     * 查询内部账户历史余额接口
     * @param params
     * @param accounts 内部账户相关信息
     * @return
     */
    public CtmJSONObject queryAccountBalance(CtmJSONObject params, List<String> accounts, List<Map<String, Object>> bankAccounts) throws Exception;


    CtmJSONObject saveAccountBalance(AccountRealtimeBalance accountRealtimeBalance) throws Exception;

    /**
     * 页面拉取历史余额
     * @param accountRealtimeBalance
     * @return
     * @throws Exception
     */
    CtmJSONObject syncHistoryAccountBalance(CtmJSONObject accountRealtimeBalance) throws Exception;

    /**
     * 构建需要删除的历史余额数组
     * @param accEntity
     * @param deleteCurrencyIds
     * @param enterpriseBankAccountIds
     * @param startDate
     * @param endDate
     * @return
     * @throws Exception
     */
     List<AccountRealtimeBalance>  deleteAccountBalanceList(String accEntity,  List<String> deleteCurrencyIds,  List<String> enterpriseBankAccountIds, String startDate,String endDate) throws Exception;

     /**
     * 组装接口查询请求数据
     * @param bankAccount
     * @return
     */
     CtmJSONObject buildQueryHistoryBalanceMsg(CashHttpBankEnterpriseLinkVo bankAccount) throws Exception;

    /**
     * 历史余额入库
     *
     * @param bankAccount
     * @param responseBody
     * @param uid
     * @throws Exception
     */
    public int insertAccountHistoryBalanceData(CashHttpBankEnterpriseLinkVo bankAccount, CtmJSONObject responseBody, String uid) throws Exception;

    CtmJSONObject confirmAccountBalance(List<AccountRealtimeBalance> billList) throws Exception;

    CtmJSONObject cancelConfirmAccountBalance(List<AccountRealtimeBalance> billList) throws Exception;

    CtmJSONObject checkAccountBalance(Integer preDays) throws Exception;

    CtmJSONObject updateHistory(String schema) throws Exception;

    CtmJSONObject updateHistoryV2(String schema) throws Exception;

    /**
     * 同步结算中心内部账户的历史余额
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param innerBankAccounts 结算中心账户
     * @param uid 进度条id
     * @throws Exception
     */
    void syncHistoryInnerAccountBalance(Date startDate, Date endDate, List<EnterpriseBankAcctVO> innerBankAccounts, String uid) throws Exception;

    /**
     * 同步直联银行账户的历史余额
     * @param httpList
     * @param uid
     * @return
     * @throws Exception
     */
    CtmJSONObject doSyncHistoryAccountBalance(List<CashHttpBankEnterpriseLinkVo> httpList, String uid) throws Exception;
}
