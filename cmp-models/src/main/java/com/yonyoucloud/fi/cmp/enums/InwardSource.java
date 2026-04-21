package com.yonyoucloud.fi.cmp.enums;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

/**
 * 汇入来源：
 * 1-境外;2-境内
 * 系统默认为1-境外，可以修改
 */
public enum InwardSource {

    OUT(1, MessageUtils.getMessage("P_DIWORK_YC_BASEDOC_0000062781") /* "境外" */),
    IN(2, MessageUtils.getMessage("P_DIWORK_YC_BASEDOC_0000063031") /* "境内" */);

    private int index;
    private String name;

    InwardSource(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setCode(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
