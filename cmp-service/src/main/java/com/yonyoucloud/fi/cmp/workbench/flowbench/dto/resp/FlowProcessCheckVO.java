package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import lombok.Data;

/**
 * 流水工作台-流水处理率
 */
@Data
public class FlowProcessCheckVO {
    /**
     * 流水处理率 百分比
     */
    private String flowPercent;
    /**
     * 未处理流水笔数
     */
    private Long unProcessFlowNum;
    /**
     * 已处理流水笔数
     */
    private Long processedFlowNum;
}
