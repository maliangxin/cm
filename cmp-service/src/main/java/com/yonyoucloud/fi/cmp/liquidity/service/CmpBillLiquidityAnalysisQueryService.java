package com.yonyoucloud.fi.cmp.liquidity.service;

import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityItemDTO;
import com.yonyou.yonbip.ctm.liquidity.entity.dto.LiquidityQueryConditionDTO;

import java.util.List;

/**
 * <h1>CmpLiquidityAnalysisService</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-05-18 10:10
 */
public interface CmpBillLiquidityAnalysisQueryService {

    List<LiquidityItemDTO> queryFundPaymentLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception;
    List<LiquidityItemDTO> queryFundCollectionLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception;
    List<LiquidityItemDTO> querySalaryPayLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception;
    List<LiquidityItemDTO> queryTransferAccountLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception;
    List<LiquidityItemDTO> queryForeignPaymentLiquidityAnalysisData(LiquidityQueryConditionDTO conditionDTO) throws Exception;


}
