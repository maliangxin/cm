package com.yonyoucloud.fi.cmp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 即期结售汇交易提交SSFE1002接口请求报文实体
 */
@Data
public class RequestHeaderVO implements Serializable {

    private String operator;
    private String signature;
    private String customNo;

}
