package com.yonyoucloud.fi.cmp.liquidity.service.vo;

import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquidityAuditStatusEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquidityPayStatusEnum;
import com.yonyou.yonbip.ctm.liquidity.entity.enums.LiquiditySettleStatusEnum;
import lombok.Data;

import java.util.List;

/**
 * <h1>CmpLiquidityCashQueryConditionDTO</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-23 13:39
 */
@Data
public class CmpLiquidityCashQueryConditionDTO {
    private Boolean isIntegrateSettlementPlatform;
    private List<Short> businessStatus;
    private List<Short> settleStatus;
    private List<Short> payStatus;
}
