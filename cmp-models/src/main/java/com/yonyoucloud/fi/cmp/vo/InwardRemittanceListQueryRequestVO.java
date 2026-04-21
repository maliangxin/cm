package com.yonyoucloud.fi.cmp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 汇入汇款确认提交SSFE1004接口请求报文实体
 */
@Data
public class InwardRemittanceListQueryRequestVO extends RequestHeaderVO implements Serializable {

    // 交易流水号
    private String tran_seq_no;
    // 收方账号
    private String rcv_acct_no;
    // 币种
    private String curr_code;
    // 起始日期
    private String beg_date;
    // 截止日期
    private String end_date;
    // 查询起始位置
    private String beg_num;
    // 查询条数
    private String query_num;
    // 查询扩展信息
    private String query_extend;

}
