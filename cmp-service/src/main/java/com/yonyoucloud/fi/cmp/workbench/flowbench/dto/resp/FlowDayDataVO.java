package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 流水处理日历 -单日数据
 * @author guoxh
 */
@Data
public class FlowDayDataVO {
    /**
     * 流水笔数
     */
    private Long billNum;
    /**
     * 已处理流水笔数
     */
    private Long processedBillNum;
    /**
     * 未处理流水笔数
     */
    private Long unprocessedBillNum;
    /**
     * 发布处理中流水笔数
     */
    private Long processingBillNum;
    /**
     * 回单笔数
     */
    private Long receiptsBillNum;
    /**
     * 明细未关联回单笔数
     */
    private Long unrelationreceiptsBillNum;
    /**
     * 回单未关联明细笔数
     */
    private Long receiptsUnrelationBillNum;
    /**
     * 未下单回单笔数
     */
    private Long receiptsUnDownloadBillNum;
}
