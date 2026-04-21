package com.yonyoucloud.fi.cmp.bankunion;

import lombok.Data;

/**
 * 接收银企联接口的请求实体类
 */
@Data
public class BankUnionRequestHead {

    String tran_code;//交易编码

    String request_date;

    String request_time;

}
