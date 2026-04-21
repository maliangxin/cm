package com.yonyoucloud.fi.cmp.weekday;

import lombok.Data;

/**
 * @author shiqhs
 * @date 2022/09/03
 * @description 工作日请求参数
 */
@Data
public class WorkdayReq {

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 组织ID
     */
    private String orgId;

    /**
     * 节假日原则
     */
    private HolidayPrincipleEnum holidayPrinciple;

    /**
     * 节假日计息
     */
    private HolidayPrincipleEnum holidayInterest;
}
