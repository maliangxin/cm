package com.yonyoucloud.fi.cmp.weekday;

import lombok.Data;

import java.io.Serializable;

@Data
public class WorkingCalendar implements Serializable {

    /**
     * 工作日历id
     */
    private String id;

    /**
     * 时间戳
     */
    private String ts;

    private String es;

    /**
     * 创建时间戳
     */
    private long creationtime;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 修改时间戳
     */
    private long modifiedtime;

    /**
     * 修改人
     */
    private String modifier;

    /**
     * 租户ID
     */
    private String tenantid;

    /**
     * 友互通租户ID
     */
    private String ytenantid;

    /**
     * 工作日历名称
     */
    private String workingcalendarname;
    private String workingcalendarname2;
    private String workingcalendarname3;
    private String workingcalendarname4;
    private String workingcalendarname5;
    private String workingcalendarname6;

    /**
     * 国家编码
     */
    private String countrycode;

    /**
     * 国家名称
     */
    private String countryname;

    private Boolean isdefault;

    /**
     * 星期一是否工作日
     */
    private Boolean ismonday;

    /**
     * 星期二是否工作日
     */
    private Boolean istuesday;

    /**
     * 星期三是否工作日
     */
    private Boolean iswednesday;

    /**
     * 星期四是否工作日
     */
    private Boolean isthursday;

    /**
     * 星期五是否工作日
     */
    private Boolean isfriday;

    /**
     * 星期六是否工作日
     */
    private Boolean issaturday;

    /**
     * 星期日是否工作日
     */
    private Boolean issunday;

    /**
     * 所属组织（用于权限控制） 0 无控制
     */
    private String busiorg;
}
