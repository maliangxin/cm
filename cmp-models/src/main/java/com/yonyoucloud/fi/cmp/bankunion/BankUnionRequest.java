package com.yonyoucloud.fi.cmp.bankunion;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 接收银企联接口的请求实体类
 */
@Data
public class BankUnionRequest {

    @NotNull
    BankUnionRequestBody request_body;
    @NotNull
    BankUnionRequestHead request_head;
}
