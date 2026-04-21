package com.yonyoucloud.fi.cmp.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;


/**
 * 银行回单文件上传
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTranBatchUpdateVO implements Serializable {


    /**
     * 回单编号 存储传入值
     */
    @NotEmpty(message = "回单编号不能为空")//@notranslate
    private String receiptno;

    /**
     * 回单id
     */
    @NotEmpty(message = "回单id不能为空")//@notranslate
    private String id;

    @NotEmpty(message = "文件名称不能为空")//@notranslate
    private String fileName;

    /**
     * 回单文件
     */
    @NotEmpty(message = "回单文件不能为空 base64")//@notranslate
    private String bankpdf;

}
