package com.yonyoucloud.fi.cmp.cmpentity;

import java.io.Serializable;

/**
 * @author SSY
 * @description 款项类型
 * @date 2022/5/11 18:38
 */

public class QuickTypeVO implements Serializable {

    private Long id;
    private String code;
    private String name;
    private Boolean stopstatus;
    private Integer blnPrepare;
    private Integer blnShouldPay;

    public QuickTypeVO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getStopstatus() {
        return stopstatus;
    }

    public void setStopstatus(Boolean stopstatus) {
        this.stopstatus = stopstatus;
    }

    public Integer getBlnPrepare() {
        return blnPrepare;
    }

    public void setBlnPrepare(Integer blnPrepare) {
        this.blnPrepare = blnPrepare;
    }

    public Integer getBlnShouldPay() {
        return blnShouldPay;
    }

    public void setBlnShouldPay(Integer blnShouldPay) {
        this.blnShouldPay = blnShouldPay;
    }

    @Override
    public String toString() {
        return "QuickTypeVO{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
