package com.yonyoucloud.fi.cmp.cmpentity;

import com.yonyoucloud.ctm.stwb.settleapply.enums.SettleApplyDetailStateEnum;

/**
 * 资金结算状态枚举转换工具类
 */
public class SettleStatusConverter {

    /**
     * 将SettleApplyDetailStateEnum转换为FundSettleStatus
     *
     * @param stateEnum SettleApplyDetailStateEnum枚举值
     * @return 对应的FundSettleStatus枚举值
     */
    public static FundSettleStatus convertToFundSettleStatus(SettleApplyDetailStateEnum stateEnum) {
        if (stateEnum == null) {
            return null;
        }
        switch (stateEnum) {
            case WAIT_HANDLE:
                return FundSettleStatus.WaitSettle; // 待结算状态
            case HANDLING:
                return FundSettleStatus.SettleProssing; // 结算中状态
            case ALL_SUCCESS:
                return FundSettleStatus.SettleSuccess; // 结算成功状态
            case ALL_FAIL:
                return FundSettleStatus.SettleFailed; // 结算止付状态
            case PART_SUCCESS:
                return FundSettleStatus.PartSuccess; // 部分成功状态
            case REFUNDD:
                return FundSettleStatus.Refund; // 退票状态
            case SETTLE_DONE:
                return FundSettleStatus.SettlementSupplement; // 已结算补单状态
            default:
                return null;
        }
    }

    /**
     * 将FundSettleStatus转换为SettleApplyDetailStateEnum
     *
     * @param fundSettleStatus FundSettleStatus枚举值
     * @return 对应的SettleApplyDetailStateEnum枚举值
     */
    public static String convertToSettleApplyDetailStateEnum(FundSettleStatus fundSettleStatus) {
        if (fundSettleStatus == null) {
            return null;
        }

        // 注意：这里需要根据实际的SettleApplyDetailStateEnum定义来映射
        switch (fundSettleStatus) {
            case WaitSettle:
                return SettleApplyDetailStateEnum.WAIT_HANDLE.getValue(); // 待结算状态
            case SettleProssing:
                return SettleApplyDetailStateEnum.HANDLING.getValue(); // 结算中状态
            case SettleSuccess:
                return SettleApplyDetailStateEnum.ALL_SUCCESS.getValue(); // 结算成功状态
            case SettleFailed:
                return SettleApplyDetailStateEnum.ALL_FAIL.getValue(); // 结算止付状态
            case PartSuccess:
                return SettleApplyDetailStateEnum.PART_SUCCESS.getValue(); // 部分成功状态
            case Refund:
                return SettleApplyDetailStateEnum.REFUNDD.getValue(); // 退票状态
            case SettlementSupplement:
                return SettleApplyDetailStateEnum.SETTLE_DONE.getValue(); // 已结算补单状态
            default:
                return null;
        }
    }
}
