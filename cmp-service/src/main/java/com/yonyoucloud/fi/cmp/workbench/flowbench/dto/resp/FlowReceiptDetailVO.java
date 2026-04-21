package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import lombok.Data;

import java.util.List;

@Data
public class FlowReceiptDetailVO {
    /**
     * 未关联回单笔数
     */
    private List<Long> unrelationReceiptList;
    /**
     * 未关联明细笔数
     */
    private List<Long> unrelationBillList;
    /**
     * 未下载回单笔数
     */
    private List<Long> undownReceiptList;
    /**
     * 流水笔数
     */
    private List<Long> billList;
    /**
     * 回单笔数
     */
    private List<Long> receiptsList;

}
