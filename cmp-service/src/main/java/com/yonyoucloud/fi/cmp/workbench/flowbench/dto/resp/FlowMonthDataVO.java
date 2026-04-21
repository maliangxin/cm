package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 流水处理日历 - 月份按天的数据;
 * @author guoxh
 */
@Data
public class FlowMonthDataVO {
    /**
     * 日期
     */
    private String days;
    /**
     * 存在待处理数据标识
     */
    private Boolean flag;
    /**
     * 存在待处理数据标识
     */
    private Boolean handleState;
    /**
     * 流水笔数
     */
    private Long billNum;
    /**
     * 已完成数据量
     */
    private Long finishNum;
    /**
     * 待处理数据量
     */
    private Long unfinishNum;
    /**
     * 已处理流水笔数
     */
    private Long processedBillNum;
}
