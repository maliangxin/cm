package com.yonyoucloud.fi.cmp.enums;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

/**
 * 银行对账状态枚举值
 */
public enum BalanceStateEnum {
    AlreadyLeveled (1L, MessageUtils.getMessage("P_YS_FI_OMC_0001100928") /* "已平" */),
    NotAlreadyLeveled(2L,MessageUtils.getMessage("P_YS_FI_OMC_0001100927") /* "未平" */);


    private Long index;
    private String name;

    private BalanceStateEnum(Long index, String name){
        this.index = index;
        this.name = name;
    }

    public Long getIndex() {
        return index;
    }

    public void setCode(Long index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }}
