package com.yonyoucloud.fi.cmp.https.utils.entity;

import lombok.Data;

@Data
public class QueryMessage {

    String version;
    String request_seq_no;
    String cust_no;
    String cust_chnl;
    String request_date;
    String request_time;
    String oper;
    String oper_sign;
    String tran_code;

}
