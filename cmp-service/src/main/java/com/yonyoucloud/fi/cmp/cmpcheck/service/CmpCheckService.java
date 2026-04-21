package com.yonyoucloud.fi.cmp.cmpcheck.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;

import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2024/1/26 15:13
 */

public interface CmpCheckService {

    /**
     * 查询银企对账
     * @param param
     * @return
     */
    CtmJSONObject querySettingDetailInfo(CtmJSONObject  param) throws Exception;

    /**
     * 查询期初未达关联的授权使用组织和对账财务账簿等信息
     * @param param
     * bankreconciliationscheme 对账id
     * bankaccount 银行账户
     * currency 币种
     * @return
     * @throws Exception
     */
    CtmJSONObject queryOpenOutstandingInfo(CtmJSONObject  param) throws Exception;


    /**
     * 查询银行账户的授权使用组织信息
     * @param param
     * bankaccountid 银行账户id
     * @return
     * @throws Exception
     */
    CtmJSONObject queryBankAccountUseOrgInfo(CtmJSONObject  param) throws Exception;

    /**
     * 银企对账，银企对账设置查询专用接口，返回的是资金组织
     * @param param
     * @return
     * @throws Exception
     */
    List<BankReconciliationSettingVO> findUseOrg(PlanParam param) throws Exception;

    /**
     * 处理凭证回单关联事件发送
     * @param bankMap 勾兑的银行对账单
     * @param journalMap 勾兑的凭证数据
     */
    void handleBankReceiptEvent(Map<String, List<BankReconciliation>> bankMap, Map<String,List<Journal>> journalMap) throws Exception;

    /**
     * 根据服务编码，获取银行账号的数据权限
     * 仅限 ficmp0014：银行流水对账
     * @param serviceCode 服务编码
     * @return 银行账号的数据权限集合
     */
    String[] getBankAccountDataPermission(String serviceCode) throws Exception;

}
