package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import lombok.Data;

/**
 * 流水工作台-余额检查
 */
@Data
public class FlowBalanceCheckVO {
    /**
     * 检查结果 true 表示无异常,false表示有异常
     */
    private Boolean balanceCheck;
    /**
     * 余额不符记录
     */
    private Integer balanceUnMatchNum;
    /**
     * 余额缺失记录
     */
    private Integer balanceMissingNum;
}
