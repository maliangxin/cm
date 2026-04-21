package com.yonyoucloud.fi.cmp.bankvouchercheck.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @description: 银企对账进度条-使用通义灵码辅助实现
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/20 10:26
 */
@Data
public class ReconciliationProcessItem {
    private String key;
    private String accentity_code;
    private String title;//标题
    private String accentity;
    private String bankaccount;
    private String date;//对账截止日期
    private String checkStartDate;//对账开始日期
    private Set<String> details = new CopyOnWriteArraySet<>(); // 使用Set来存储不可重复的银行账户名称
    private Set<String> bankaccounList;
    private Set<String> bankaccoutInfoList;
    private int status; // 0: 未开始 1: 进行中 2: 已完成 3: error
    private int reconciliationStatus; //0未对符；1已对符
    private String errmsg; //异常信息
    private BigDecimal vouchCheckedNum;//凭证勾对笔数
    private BigDecimal bankCheckedNum;//银行流水勾对笔数
    public ReconciliationProcessItem() {
        this.details = new LinkedHashSet<>();
        this.bankaccounList = new HashSet<>();
        this.bankaccoutInfoList = new HashSet<>();
    }
    public Set<String> getDetails() {
        return details;
    }
    public void setDetails(Set<String> details) {
        this.details = details;
    }
}
