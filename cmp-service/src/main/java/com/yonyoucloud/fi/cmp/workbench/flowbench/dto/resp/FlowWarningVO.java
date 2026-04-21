package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流水接入/导入提醒
 */
@Data
public class FlowWarningVO {
    /**
     * 风险项数
     */
    private Integer riskNum;
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

    /**
     * 流水接入/导入提醒 异常状态标识
     */
    private Boolean billInfo;
    /**
     * 异常账户数量
     */
    private Integer accountNum;
    /**
     * 异常账户列表
     */
    private List<String> accountList;

    private List<Map<String,Object>> bankAccountList;

    /**
     * 检查结果 true 表示无异常,false表示有异常
     */
    private Boolean balanceCheck;
    /**
     * 余额不符记录
     */
    private Integer balanceUnMatchNum;

    private List<Map<String,Object>> balanceUnMatchNumAccountMapList;
    /**
     * 余额缺失记录
     */
    private Integer balanceMissingNum;

    private List<Map<String,Object>> balanceMissingAccountMapList;

    /**
     * 余额不相等记录
     */
    private Integer balanceUnEqualNum;

    private List<String> balanceUnEqualAccountList;

    private List<String> balanceMissingAccountList;

    private List<Map<String,Object>> balanceUnEqualAccounts;

    private List<Map<String,Object>> balanceMissingAccounts;
}
