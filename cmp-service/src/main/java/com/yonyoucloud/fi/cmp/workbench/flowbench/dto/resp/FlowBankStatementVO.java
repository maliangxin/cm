package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 流水工作台 银行流水 top
 * @author guoxh
 */
@Data
public class FlowBankStatementVO {
    /**
     * 排名
     */
    private Integer sn;
    /**
     * 银行类别
     */
    private String bankType;
    /**
     *金额
     */
    private BigDecimal amount;
    /**
     * 笔数
     */
    private Long num;
    /**
     * 金额占比
     */
    private Double amountPercent;
    /**
     * 笔数占比
     */
    private String numPercent;
}
