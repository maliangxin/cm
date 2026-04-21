package com.yonyoucloud.fi.cmp.weekday;

import lombok.Data;

import java.time.DayOfWeek;

/**
 * @author shiqhs
 * @date 2022/04/26
 * @description 规则日期类
 */
@Data
public class RuleWeek {

    /**
     * 星期几
     */
    private DayOfWeek dayOfWeek;

    /**
     * 是否工作日
     */
    private Boolean isWorkDate;
}
