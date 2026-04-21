package com.yonyoucloud.fi.cmp.event.vo;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;

import java.util.List;

public class ResultBudget {
    private short budgeted;//是否占预算
    private boolean isSuccess;//是否执行 true执行成功
    private List<String> ids;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String message;//消息

    public ResultBudget(short budgeted, boolean isSuccess, String message) {
        this.budgeted = budgeted;
        this.isSuccess = isSuccess;
        this.message = message;
    }

    public ResultBudget(short budgeted, boolean isSuccess, List<String> ids) {
        this.budgeted = budgeted;
        this.isSuccess = isSuccess;
        this.ids = ids;
    }


    public short isBudgeted() {
        return budgeted;
    }

    public void setBudgeted(short budgeted) {
        this.budgeted = budgeted;
    }

    public short getBudgeted() {
        return budgeted;
    }

    /**
     * 是否成功
     * @return
     */
    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public ResultBudget(short budgeted, boolean isSuccess) {
        this.budgeted = budgeted;
        this.isSuccess = isSuccess;
        this.message= InternationalUtils.getMessageWithDefault("UID:P_GRM-BE_18113A5204B80010",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006A8", "成功") /* "成功" */) /* "成功" */;
    }
}
