package com.yonyoucloud.fi.cmp.weekday;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WorkingCalendarVO implements Serializable {

    private String message;

    private String i18nCode;

    private List params;

    private List<WorkingCalendar> data;
}
