package com.yonyoucloud.fi.cmp.bankunion;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 接收银企联接口的请求实体类
 */
@Data
public class BankUnionRequestBody {

    @NotNull
    String back_num;
    @NotNull
    List<BankUnionRequestBodyRecord> record;

}
