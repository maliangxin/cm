package com.yonyoucloud.fi.cmp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 即期结售汇交易提交SSFE1002接口请求报文实体
 */
@Data
public class ExchangeSubmitRequestVO implements Serializable {

    private String tran_seq_no;
    private String batch_no;
    private String trade_flag;
    private String bus_type;
    private String delay_flag;
    private String sell_acct_no;
    private String buy_acct_no;
    private String sell_curr;
    private String buy_curr;
    private String for_amt;
    private String cny_amt;
    private String delivery_date;
    private String delegation_type;
    private SelfInquiry_detail selfInquiry_detail;
    private SelfPend_detail selfPend_detail;
    private String qualified_pledge_type;
    private String accno_tp_code;
    private String deposit_acct_no;
    private String is_check_acct;
    private String check_acct_no;
    private Dec_info dec_info;
    private String remark;
    private String file_info;
    private String obmdef1;
    private String obmdef2;
    private String obmdef3;
    private String extend_data;

    @Data
    public static class SelfInquiry_detail{
        private String inquiry_type;
        private String inquiry_id;
        private String deal_rate;
    }

    @Data
    public static class SelfPend_detail{
        private String pend_end_date;
        private String pend_rate;
        private String loss_rate;
    }

    @Data
    public static class Dec_info{
        private String pro_code;
        private String trade_code;
        private String st_code;
        private String for_ex_useof;
        private String for_ex_useof_det;
        private String for_ex_num;
        private String dec_tel;
        private String dec_name;
        private String dec_date;
    }

}
