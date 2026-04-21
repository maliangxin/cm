package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import lombok.Data;

/**
 * 超期未处理流水
 * @author guoxh
 */
@Data
public class FlowOutdateVO {
    /**
     * 超期处理结果 true 说明没有,false说明存在数据
     */
    private Boolean overDayInfo;
    /**
     * 超期天数
     */
    private Integer overDays;
    /**
     * 超期笔数
     */
    private Long billNum;
}
