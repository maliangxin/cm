package com.yonyoucloud.fi.cmp.balanceadjust.vo;

import lombok.Data;

/**
 * @description: 余额调节表查询通用参数
 * @author: wanxbo@yonyou.com
 * @date: 2025/10/30 10:02
 */
@Data
public class BalanceAdjustQueryInfoVO {
    /**
     * 对账截止日期
     */
    private String checkEndDate;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;
}
