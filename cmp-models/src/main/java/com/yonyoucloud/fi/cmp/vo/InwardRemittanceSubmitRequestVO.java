package com.yonyoucloud.fi.cmp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 汇入汇款确认提交SSFE1004接口请求报文实体
 */
@Data
public class InwardRemittanceSubmitRequestVO extends RequestHeaderVO implements Serializable {

    // 交易流水号
    private String tran_seq_no;
    private String bank_ref_no;
    private String cust_org_code;
    private String use_desc;
    private String remt_form;
    private String irt_type;
    private String refund_flag;
    private String refund_reason;
    private String ret_type;

    private Dec_info dec_info;
    private Re_info re_info;
    private Cips_cny cips_cny;

    private String apy_name;
    private String apy_tel;
    private String reg_date;
    private String rcv_name;
    private String rcv_org_code;
    private String ttl_amt;
    private String good_amt;
    private String advc_amt;
    private String con_rate;
    private String dey_day;
    private String rpt_name;
    private String rpt_org_code;
    private String cny_rpt;
    private String trdn_amt;
    private String trdf_amt;
    private String trdo_amt;
    private String trdb_amt;
    private String nolg_amt;
    private String ntrd_amt;
    private String ntdr_amt;
    private String svic_amt;
    private String svit_rd;
    private String ivst_amt;
    private String invt_code;
    private String invc_code;
    private String trsf_amt;
    private String trsf_code;
    private String cpit_amt;
    private String cpit_code;
    private String dict_amt;
    private String dict_code;
    private String dicc_code;
    private String stck_amt;
    private String stck_code;
    private String oinv_amt;
    private String oinv_code;
    private String apy_remark;

    @Data
    public class Dec_info{
        private String payernation_code;
        private String amt_type;
        private String trans_code1;
        private String trans_amt1;
        private String trans_remark1;
        private String trans_code2;
        private String trans_amt2;
        private String trans_remark2;
        private String for_ex_num;
        private String retin_type;
        private String bod_flag;
        private String dec_tel;
        private String dec_name;
        private String dec_date;
        private Record record;
    }

    @Data
    public class Record{
        private String file_summary;
        private String upload_id;
        private String file_name;
        private String file_info;
    }

    @Data
    public class Re_info{
        private String boc_flag;
        private String tran_type;
        private String payee_acct_no;
        private String payee_name;
        private String rebk_name;
        private String rebk_no;
    }

    @Data
    public class Cips_cny{
        private String reg_date;
        private String rcv_org_code;
        private String apy_name;
        private String ttl_amt;
        private String apy_tel;
        private String rcv_name;
    }

}
