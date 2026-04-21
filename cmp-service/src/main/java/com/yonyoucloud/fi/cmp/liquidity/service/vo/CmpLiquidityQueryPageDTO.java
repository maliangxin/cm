package com.yonyoucloud.fi.cmp.liquidity.service.vo;

import lombok.Data;

/**
 * <h1>CmpLiquidityQueryPageDTO</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-22 16:01
 */
@Data
public class CmpLiquidityQueryPageDTO {

    /**
     * 查询页
     * 从1开始
     */
    private Integer pageIndex;

    /**
     * 查询页大小
     */
    private Integer pageSize;

    /**
     * 排序字段
     */
    private String orderFields;

    /**
     * 排序类型
     */
    private String orderType;


}
