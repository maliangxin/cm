package com.yonyoucloud.fi.cmp.liquidity.service.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * <h1>CmpLiquidityQueryConditionDTO</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-22 15:59
 */
@Data
public class CmpLiquidityQueryConditionDTO {

    /**
     * 查询分页信息
     */
    private CmpLiquidityQueryPageDTO queryPage;

    /**
     * 交易类型
     */
    private List<String> transType;

    /**
     * 会计主体
     */
    private List<String> accentitys;

    /**
     * 开始日期
     */
    private Date beginDate;

    /**
     * 结束日期
     */
    private Date endDate;

    /**
     * 币种编码
     */
    private String currencyCode;

    /**
     * 币种id
     */
    private String currencyId;

    /**
     * 企业银行账户pk
     */
    private List<String> bankAccountIds;

    // 现金流动性查询条件 现金专用
    private List<CmpLiquidityCashQueryConditionDTO> cashLiquidityQuerys;
}
