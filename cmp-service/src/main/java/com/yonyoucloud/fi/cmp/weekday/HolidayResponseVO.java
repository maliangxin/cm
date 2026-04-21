package com.yonyoucloud.fi.cmp.weekday;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author shiqhs
 * @date 2021/11/19
 * @description 根据工作日历信息和年份获取工作日历和节假日信息 接口返回值VO
 */
@Data
public class HolidayResponseVO implements Serializable {

    private String message;

    private VoData data;

    @Data
    public static class VoData implements Serializable {
        private WorkingCalendar workingCalendar;

        private List<Holiday> holiday;
    }

    @Data
    public static class Holiday implements Serializable {

        /**
         * 节假日id
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
         * 所属工作日历id
         */
        private String parentid;

        /**
         * 年份
         */
        private String year;

        private String holidayname;
        private String holidayname2;
        private String holidayname3;
        private String holidayname4;
        private String holidayname5;
        private String holidayname6;

        /**
         * 节假日开始时间
         */
        private long begintime;

        /**
         * 节假日结算时间
         */
        private long endtime;

        /**
         * 法定假日
         */
        private String legalholiday;

        /**
         * 调休上班时间
         */
        private String reptime;

        /**
         * 是否预制
         */
        private Boolean ispreset;
    }
}
