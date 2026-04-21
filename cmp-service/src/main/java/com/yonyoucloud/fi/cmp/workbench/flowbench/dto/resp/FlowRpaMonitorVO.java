package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 智能机器人作业监控
 */
@Data
public class FlowRpaMonitorVO {
    /**
     * 流水下载笔数
     */
    private Long downloadFlowNum;
    /**
     * 余额下载笔数
     */
    private Long downloadBalanceNum;
    /**
     * 回单明细笔数
     */
    private Long receiptDetailNum;
    /**
     * 回单文件笔数
     */
    private Long receiptFileNum;
    /**
     * 账户数
     */
    private Long accountNum;
    /**
     * 关键信息辨识笔数
     */
    private Long keyIdentifyNum;
    /**
     * 退票辨识笔数
     */
    private Long refundIdentifyNum;
    /**
     * 收付单据自动关联笔数
     */
    private Long autoRelationBillNum;
    /**
     * 收付单据自动生成笔数
     */
    private Long autoGenerateBillNum;
    /**
     * 业务凭据自动关联笔数
     */
    private Long autoRelationVoucherNum;
    /**
     * 流水自动发布笔数
     */
    private Long autoPublishFlowNum;
}
