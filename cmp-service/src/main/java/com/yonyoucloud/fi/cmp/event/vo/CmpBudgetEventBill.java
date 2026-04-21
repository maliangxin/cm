package com.yonyoucloud.fi.cmp.event.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CmpBudgetEventBill {
    private String ruleId;
    private String ruleCtrlId;
    private String triggerType;
    private String ruleType;
    private String billCode;
    private String transacCode;
    private String transacId;
    private String billAmount;
    private String billTime;
    private String startDate;
    private String endDate;
    /**
     * 业务字段，在业务系统注册中配置的字段
     */
    private List<String> busifields;
    /**
     *控制使用的单据字段（控制字段+过滤字段） *
     */
    private List<String> billfields;
    /**
     * 控制使用的单据字段（控制字段）*
     */
    private List<String> ctrlFields;
    /**
     * 控制使用的单据字段（过滤字段）*
     */
    private List<String> filterFields;
    /**
     * 控制字段  字段映射里配置的字段，需要根据这里的数据进行过滤
     */
    private Object billfieldValues;
}
