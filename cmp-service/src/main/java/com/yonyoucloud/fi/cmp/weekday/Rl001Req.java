package com.yonyoucloud.fi.cmp.weekday;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * @author shiqhs
 * @date 2021/11/24
 * @description RL001规则请求参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Rl001Req extends RuleBaseReq {

    /**
     * 工作日请求参数
     */
    private WorkdayReq workdayReq;

    /**
     * 自然日
     */
    private LocalDate naturalDay;
}
