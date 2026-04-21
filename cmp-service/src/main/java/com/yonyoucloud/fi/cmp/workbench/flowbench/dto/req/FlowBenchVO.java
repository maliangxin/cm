package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.Transient;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流水工作台-新建视图
 * @author guoxh
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowBenchVO {
    private Long id;
    /**
     * 视图名称
     */
    private String viewName;
    /**
     * 日期范围 ["2024-06-24 00:00:00","2024-06-30 00:00:00"]
     */
    private String[] dateRange;
    /**
     * 会计主体
     */

    private String accentity;
    /**
     * 银行账号
     */
    private String accountNo;

    private String accountName;
    private String accountCurrency;
    /**
     * 超期未处理预警天数
     */
    private Integer warningDays;
    /**
     * 折算币种
     */
    private String currency;

    private String currencyName;
    /**
     * 折算汇率类型 bd.exchangeRate.ExchangeRateTypeVO
     */
    private String exchangeRateType;

    private String exchangeRateTypeName;
    /**
     * 金额单位 枚举 "1" 元 "2" 万元 "3"  亿元
     */
    private String currencyUnit;

    private String month;

    /**
     * 是否默认 1 / 0
     */
    private Integer isDefault;
    /**
     * 排序
     */
    private Integer iorder;

    // 1 付款 2 收款  3 总计 (此处有调整)
    private Integer checkDirection;
    private String type;

    private Integer isPreset;

    private Date chooseDay;

    private List<Map<String,Object>> accentityList;
    private List<Map<String,Object>> accountNoList;
    private Map<String,Object> currencyMap;
    private Map<String,Object> exchangeRateTypeMap;
    @Transient
    private Set<String> accentitySet;
    @Transient
    private Set<String> bankAccountSet;
}
