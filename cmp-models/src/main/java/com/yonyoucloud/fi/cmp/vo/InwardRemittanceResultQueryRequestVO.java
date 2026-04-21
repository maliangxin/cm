package com.yonyoucloud.fi.cmp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 汇入汇款确认交易结果查询 SSFE3004 接口请求报文实体
 */
@Data
public class InwardRemittanceResultQueryRequestVO extends RequestHeaderVO implements Serializable {

    // 交易流水号
    private String tran_seq_no;
    // 汇入汇款编号
    private String bank_ref_no;

}
