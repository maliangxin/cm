package com.yonyoucloud.fi.cmp.vo.cashwidgets;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算工作台-请求参数公共基类
 * 用于现金组件
 */
@Data
public class SettleWorkbenchBaseRequest {
    private String[] pk_entity;//资金组织
    private String pk_currency;//折算币种
    private String pk_ratetype;//折算汇率类型
    private BigDecimal amountwarning;//资金存量警戒值
    private String amountunit;//金额单位 1-元 2-万元 3-亿元 为空默认1
    private String plansettledate;//计划结算日期
    private String plansettledate_end;//计划结算结束日期
}
