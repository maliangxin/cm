package com.yonyoucloud.fi.cmp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 即期结售汇交易提交SSFE1002接口请求报文实体
 */
@Data
public class ExchangeDeliveryRequestVO extends RequestHeaderVO implements Serializable {

    private String tran_seq_no;
    private String batch_no;
    private String fxtn_ar_id;
    private String sell_acct_no;

}
