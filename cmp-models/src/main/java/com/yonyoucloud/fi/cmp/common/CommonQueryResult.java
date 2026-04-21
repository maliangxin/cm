package com.yonyoucloud.fi.cmp.common;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

/**
 *
 * 本类用于常用查询rpc接口实体类
 * @author maliangn
 * 请使用api包下类进行替换
 * @deprecated (use com.yonyoucloud.fi.cmp.vo.common.CommonRequestDataVo instead)
 */
  @Deprecated
@Data
public class CommonQueryResult {

    private long totalCount;//总数
    private BigDecimal maxSum;//银行账号
    private boolean openflag;//是否开通银企连服务

}
