package com.yonyoucloud.fi.cmp.weekday;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * @author shiqhs
 * @date 2021/11/24
 * @description RL001规则返回参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Rl001Resp extends RuleBaseResp {

    /**
     * 工作日
     */
    private LocalDate workday;
}
