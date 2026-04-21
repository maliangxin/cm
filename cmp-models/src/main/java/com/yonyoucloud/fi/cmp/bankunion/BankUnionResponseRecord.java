package com.yonyoucloud.fi.cmp.bankunion;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 接收银企联接口的响应实体类
 */
@Data
public class BankUnionResponseRecord {

    @NotNull
    String unique_no;//唯一码
    @NotNull
    String code;//响应码
    @NotNull
    String message;//响应信息

    public BankUnionResponseRecord success(){
        this.code = "000000";
        return this;
    }

    public BankUnionResponseRecord fail(String message){
        this.message = message;
        return this;
    }

}
