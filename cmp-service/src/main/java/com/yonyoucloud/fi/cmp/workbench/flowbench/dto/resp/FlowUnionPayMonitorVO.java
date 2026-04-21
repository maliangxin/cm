package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * 智能监控银联
 */
@Data
public class FlowUnionPayMonitorVO {
    /**
     * 接入银行数量
     */
    private Long totalNum;
    /**
     * 接入正常数量
     */
    private Long normalNum;
    /**
     * 接入异常数量
     */
    private Long exceptNum;
    /**
     * 正常银行
     */
    private List<String> normalList;
    /**
     * 异常银行
     */
    private List<String> exceptList;
}
