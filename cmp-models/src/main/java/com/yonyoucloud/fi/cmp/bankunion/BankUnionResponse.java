package com.yonyoucloud.fi.cmp.bankunion;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 接收银企联接口的响应实体类
 */
@Data
public class BankUnionResponse implements Serializable {

    @NotNull
    String code;//响应码
    @NotNull
    String message;//响应信息

    List<BankUnionResponseRecord> data;//响应数据
    public BankUnionResponse success(){
        this.code = "000000";
        return this;
    }

    public BankUnionResponse fail(String message){
        this.message = message;
        return this;
    }

}
