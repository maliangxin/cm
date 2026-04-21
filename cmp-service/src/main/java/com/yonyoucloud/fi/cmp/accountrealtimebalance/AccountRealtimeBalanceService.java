package com.yonyoucloud.fi.cmp.accountrealtimebalance;

import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @ClassName AccountRealtimeBalanceService
 * @Description 账户实时余额接口
 * @Author yangjn
 * @Date 2021/8/25 16:23
 * @Version 1.0
 **/
public interface AccountRealtimeBalanceService {

    /**
     * 账户实时余额数据拉取--新版
     *
     * @return
     * @throws Exception
     */
    CtmJSONObject queryRealbalanceBalanceNew(CtmJSONObject params) throws Exception;

    /**
     * 查询内部账户余额接口
     *
     * @param params
     * @param accounts 内部账户相关信息
     * @param uid
     * @return
     */
    public CtmJSONObject queryAccountBalance(CtmJSONObject params, List<String> accounts, List<EnterpriseBankAcctVO> bankAccounts, String uid) throws Exception;

    /**
     * 根据前端条件查询相应的银行账户
     * @return
     * @throws Exception
     */
    public List<EnterpriseBankAcctVO>  queryEnterpriseBankAccountByCondition(CtmJSONObject params)throws Exception;


    /**
     * 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、客户号等信息 并返回
     * @param lists
     * @return
     * @throws Exception
     */
    public Map<String , Object> getBankAcctVOsGroup (List<EnterpriseBankAcctVO> lists)throws Exception;

    /**
     * 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、客户号等信息 并返回
     * @param lists
     * @return
     * @throws Exception
     */
    public Map<String , Object> getBankAcctVOsGroupByTask (List<EnterpriseBankAcctVO> lists)throws Exception;


    /**
     * 在无Ukey情况下 查询企业银行账户实时余额
     *
     * @return
     * @throws Exception
     */
    CtmJSONObject queryAccountBalanceUnNeedUkey(CtmJSONObject params) throws Exception;

    /**
     * 在无Ukey情况下 查询企业银行账户实时余额
     *
     * @return 异步信息，用于等待获取结果
     * @throws Exception
     */
    Future<CtmJSONObject> queryAccountBalanceUnNeedUkeyAsync(CtmJSONObject params) throws Exception;

    /**
     * 根据从银企联拉下来的数据 进行查询
     * @param balances
     * @return
     * @throws Exception
     */
    public List<AccountRealtimeBalance> queryExistRealBalanceData(List<AccountRealtimeBalance> balances) throws Exception;

    /**
     * 对比已存在的余额信息 和 从银企联拉下来的信息 进行更新
     * @param existBalances
     * @param balances
     * @throws Exception
     */
    public int executeRealGroupData(List<AccountRealtimeBalance> existBalances, List<AccountRealtimeBalance> balances) throws Exception;


    /**
     * 如果前端没有选择币种的话，需要将查询到的银行账户按照子表币种拆成多个账户给银企联发送请求
     * @param currency
     * @param netAccountsToHttp
     * @return
     */
    public List<EnterpriseBankAcctVO> getEnterpriseBankAcctVOS(String currency, List<EnterpriseBankAcctVO> netAccountsToHttp);

    /**
     * 将集合拆分为多个集合
     * @param sourceList
     * @param groupNum
     * @return
     */
    public List<List<EnterpriseBankAcctVO>> groupData(List<EnterpriseBankAcctVO> sourceList, int groupNum);

    /**
     * 根据币种主键查询对应的编码
     * 如果缓存中存在的话取缓存 没有的话查询
     * @param currencyList
     * @return
     * @throws Exception
     */
    public HashMap<String, String> queryCurrencyCode(List<BankAcctCurrencyVO> currencyList) throws Exception;

    /**
     *@Author tongyd
     *@Description 插入企业银行账户实时余额数据
     *@Date 2019/5/31 15:43
     *@Param [accEntity, bankAccounts, responseBody]
     *@Return void
     **/
    public int insertAccountBalanceData(String accEntity, List<EnterpriseBankAcctVO> bankAccounts, CtmJSONObject responseHead, CtmJSONObject responseBody, String uid, Set<String> failAccountSet, CtmJSONObject requsetNum) throws Exception;

    public List<AccountRealtimeBalance> getAccountBalanceData(String accEntity, List<EnterpriseBankAcctVO> bankAccounts, CtmJSONObject responseBody) throws Exception;

    /**
     * 判断是否追溯，如果需要追溯，则需要往前追加查询
     * @param enterpriseBankAccounts 银行账号
     * @param accentitys 会计主体
     * @param currency 币种id
     * @param currencyList 币种集合
     * @param startDate 开始时间
     * @param endDate 结束时间
     */
    List<AccountRealtimeBalance> queryTraceabilityBalance(List<String> enterpriseBankAccounts, List<String> accentitys, String currency, List<String> currencyList, String startDate, String endDate);
}
