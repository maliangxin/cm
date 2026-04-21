package com.yonyoucloud.fi.cmp.constant;

/**
 * 银行账单转 同名账户划转 对应业务流编码
 */
public interface ITransferAccountBusinessConstant {
    //银行转账单 单据转换规则编码
    String BANK_TO_BANK_TRANSFER = "bankToBankTransfer";
    //缴存现金单 单据转换规则编码
    String BANK_TO_STORE_CASH = "bankToStoreCash";
    //提取现金单 单据转换规则编码
    String BANK_TO_EXTRACT_CASH = "bankToExtractCash";
    //第三方转账 单据转换规则编码
    String BANK_TO_THIRD_PARTY_TRANSFER = "bankToThirdPartyTransfer";

    //认领单-银行转账单 单据转换规则编码
    String BANK_TO_BANK_TRANSFER_CLAIM = "bankToBankTransferClaim";
    //认领单-缴存现金单 单据转换规则编码
    String BANK_TO_STORE_CASH_CLAIM = "bankToStoreCashClaim";
    //认领单-提取现金单 单据转换规则编码
    String BANK_TO_EXTRACT_CASH_CLAIM = "bankToExtractCashClaim";
    //认领单-第三方转账 单据转换规则编码
    String BANK_TO_THIRD_PARTY_TRANSFER_CLAIM = "bankToThirdPartyTransferClaim";

    /** 转账单 type存储值 */
    //银行转账单
    String BT = "bt";
    //缴存现金单
    String SC = "sc";
    //提取现金单
    String EC = "ec";
    //第三方转账
    String TPT = "tpt";
    //现金互转
    String CT = "ct";

    //银行转账单 交易类型编码
    String YHZZ = "yhzz";
    //缴存现金单 交易类型编码
    String JCXJ = "jcxj";
    //提取现金单 交易类型编码
    String TQXJ = "tqxj";
    //第三方转账 交易类型编码
    String DSFZZ = "dsfzz";

    //银行转账 默认付款方结算方式编码
    String SYSTEM_0001 = "system_0001";
    //默认为system_0002：现金收付款，支持编辑
    String SYSTEM_0002 = "system_0002";
}
