package com.yonyoucloud.fi.cmp.util.dataSignature.entity;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DataSignatureEntity {


    //对方名称
    private String opoppositeObjectName;
    //对方账户
    private String oppositeAccountName;
    //交易金额
    BigDecimal tradeAmount;
    //原始签名信息
    String oriSign;

}
