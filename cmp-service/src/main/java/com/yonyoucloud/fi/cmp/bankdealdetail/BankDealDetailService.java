package com.yonyoucloud.fi.cmp.bankdealdetail;


import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BankLinkParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yangjn on 2020/8/23 0020.
 * 用于银行交易明细 部分方法实现
 */
public interface BankDealDetailService {


    /**
     * @Author yangjn
     * @param params
     * @throws Exception
     * @Description 获取内部账户明细接口
     */
    public CtmJSONObject queryInnerAccountDetails(CtmJSONObject params) throws Exception;

    /**
     * @Author yangjn
     * @param responseBody
     * @throws Exception
     * @Description 用于插入内部账户明细、对账单相关
     */
    public int insertAccountDetailForInner(CtmJSONArray responseBody, CtmJSONObject params) throws Exception;

    /**
     * 用于直联联账户交易明细查询条件拼接梳理 通过前端条件查询相关银行账户
     * @param params
     * @return
     * @throws Exception
     */
    public List<EnterpriseBankAcctVO> getEnterpriseBankAccountVos(CtmJSONObject params)throws Exception;

    /**
     * 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
     * @param lists
     * @return
     * @throws Exception
     */
    public Map<String , List<EnterpriseBankAcctVO>> getBankAcctVOsGroup (List<EnterpriseBankAcctVO> lists)throws Exception;

    /**
     * 通过传入的账户vo 对账户进行分组 ：直联账户、内部账户、不可用账户 并返回
     * @param lists
     * @return
     * @throws Exception
     */
    public Map<String , List<EnterpriseBankAcctVO>> getBankAcctVOsGroupByTask (List<EnterpriseBankAcctVO> lists)throws Exception;


    public void queryBankAccountTransactionDetail (CtmJSONObject params, List<Map<String,Object>> checkSuccess, CtmJSONObject responseMsg, String uid,int batchcount,int totalTask)throws Exception;


    /*
     *@Author tongyd
     *@Description 账户交易明细查询
     *@Date 2019/5/21 14:36
     *@Param [params]
     *@Return com.yonyou.yonbip.ctm.json.CtmJSONObject
     **/
    CtmJSONObject queryAccountTransactionDetail(CtmJSONObject params) throws Exception;

    /*
     *@Author jiangpengk
     *@Description 批量账户交易明细查询
     *@Date 2023/6/08 14:36
     *@Param [params]
     *@Return BankLinkParam
     **/
    CtmJSONObject batchQueryTransactionDetailForRpc(BankLinkParam paramMap) throws Exception;


    /**
     * 根据币种list 获取币种code信息
     * @param currencyList
     * @return
     * @throws Exception
     */
    public HashMap<String, String> queryCurrencyCode(List<BankAcctCurrencyVO> currencyList) throws Exception;

    /**
     * 构建查询信息供定时任务使用
     * @param params
     * @return
     * @throws Exception
     */
    public CtmJSONObject buildQueryTransactionDetailMsg(CtmJSONObject params) throws Exception;

    /**
     * 解析银企联数据 进行插入操作
     * @param enterpriseInfo
     * @param responseBody
     * @throws Exception
     */
    public int insertTransactionDetail(Map<String, Object> enterpriseInfo, CtmJSONObject responseBody) throws Exception;
}
