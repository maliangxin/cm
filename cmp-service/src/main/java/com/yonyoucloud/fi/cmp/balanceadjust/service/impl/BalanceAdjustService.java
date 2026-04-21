package com.yonyoucloud.fi.cmp.balanceadjust.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankVoucherInfoQueryVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;

import java.math.BigDecimal;

/**
 * Created by Administrator on 2019/4/20 0020.
 */
public interface BalanceAdjustService {
    CtmJSONObject query(CtmJSONObject obj, Boolean initFlag) throws Exception;

    JsonNode queryBalanceState(CtmJSONObject obj) throws Exception;

    /**
     * 查询银行账户余额
     * @param bankVoucherInfoQueryVO
     * @return 账户余额信息 bankye
     * @throws Exception
     */
    CtmJSONObject getBankBalanceAmount(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception;

    /**
     * 查询企业日记账余额信息
     * @param bankVoucherInfoQueryVO
     * @return 业日记账余额信息 journalye
     * @throws Exception
     */
    CtmJSONObject getJournalBalanceAmount(BankVoucherInfoQueryVO bankVoucherInfoQueryVO) throws Exception;

    /**
     * 获取凭证余额
     * @param bankAccountSettingVO 账户信息
     * @return journalye 凭证余额；voucherDetailInfoList 凭证余额详情
     * @throws Exception
     */
    CtmJSONObject getVoucherBalanceAmount(BankAccountSettingVO bankAccountSettingVO) throws Exception;

    /**
     * 根据条件获取银行账户历史余额
     * 接口银行账户期初余额查询，获取对账方案启用日期前一条的银行账户历史余额
     * @param bankAccountSettingVO
     * @return 账户历史余额
     * @throws Exception
     */
    CtmJSONObject getBankAccountHistoryBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception;

    /**
     * 余额调节表生成时，查询银行账户余额
     * 1.银行账户为直联账户，直接获取对账截止日账户历史余额
     * 2.银行账户为非直联 本期余额=上期审批通过的调节表余额+本期流水发生额*
     * @param bankAccountSettingVO 银行账户信息
     * @return bankye 银行账户余额 ; isEmptyBalance 直联账户是否余额未查询到
     * @throws Exception
     */
    CtmJSONObject calculateBankAccountBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception;

    /**
     * 获取银行账户当前余额
     * 本期余额=上期审批通过的调节表余额+本期流水发生额
     * @param bankAccountSettingVO 账户信息
     * @return 当期余额 bankye：账户余额；isHasBalance 账户是否余额未查询到
     * @throws Exception
     */
    CtmJSONObject getBankAccountCurrentBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception;

    /**
     * 获取银行账户关联的期初余额 + 流水发生额
     * @param bankAccountSettingVO 账户信息
     * @return 银行方余额：bankye
     * @throws Exception
     */
    CtmJSONObject getBankAccountOpeningBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception;

    /**
     * 余额调节表-余额重算
     * @param bankAccountSettingVO 账户信息
     * @return 银行方余额：bankye
     * @throws Exception
     */
    CtmJSONObject recalculateBalance(BankAccountSettingVO bankAccountSettingVO) throws Exception;
}
