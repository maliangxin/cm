package com.yonyoucloud.fi.cmp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 财务公司银行账户，汇率查询SSFE3012接口请求报文实体
 */
@Data
public class FinanceCompanyRateQueryRequestVO extends RequestHeaderVO implements Serializable {

    // 交易流水号
    private String tran_seq_no;
    // 付款账号
    private String acct_no;
    // 付款账户名
    private String acct_name;
    // 原币币种
    private String sell_curr;
    // 兑换币种
    private String buy_curr;
    // 买卖方向
    private String dealt_side;

}
