package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 流水工作台-回单统计
 * @author guoxh
 */
@Data
public class FlowReceiptVO {
    /**
     * 流水笔数
     */
    private Long billNum;
    /**
     * 未关联回单笔数
     */
    private Long unrelationReceiptBillNum;
    /**
     * 回单笔数
     */
    private Long receiptsBillNum;
    /**
     * 回单未关联明细笔数
     */
    private Long receiptsUnrelationBillNum;
    /**
     * 未下单回单笔数
     */
    private Long receiptsUnDownloadBillNum;

    private FlowReceiptDetailVO detail;
}
