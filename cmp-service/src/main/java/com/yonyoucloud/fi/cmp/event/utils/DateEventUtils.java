package com.yonyoucloud.fi.cmp.event.utils;

import com.yonyoucloud.fi.cmp.weekday.DateUtil;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * @author chenyangn
 * @description 日期工具类
 * @date 2021/10/11
 */
public class DateEventUtils {
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String FORMAT_18 = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter Formatter_18 = DateTimeFormatter.ofPattern(FORMAT_18);

    /**
     * 获取当前时间
     *
     * @return
     */
    public static String getNowStr() {
        return Formatter_18.format(LocalDateTime.now());
    }

    /**
     * @param dateStr
     * @return java.util.Date
     * @description 日期字符串转日期
     * @author chenyangn
     * @version 1.0
     */
    public static Date parseDateStr(String dateStr) {
        return parseDateStr(dateStr, DATE_FORMAT);
    }


    /**
     * 格式化字符串到日期
     *
     * @param dateStr
     * @param format
     * @return
     */
    public static Date parseDateStr(String dateStr, String format) {
        try {
            LocalDate parse = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
            return DateUtil.localDate2Date(parse);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * 指定Date获取年月日
     *
     * @param date
     * @return String
     */
    public static String getDate(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        return simpleDateFormat.format(date);
    }

    /**
     * 指定Date获取年月日时分秒
     *
     * @param date
     * @return String
     */
    public static String getDateTime(Date date) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT_18);
            return simpleDateFormat.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param str
     * @return boolean
     * @Description 校验字符是否为指定的日期格式
     * @createTime 2021/12/11 10:02
     * @version 1.0
     * @author zhaowdw
     */
    public static boolean isValidDate(String str, String format) {
        boolean convertSuccess = true;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        try {
            simpleDateFormat.parse(str);
        } catch (Exception e) {
            convertSuccess = false;
        }
        return convertSuccess;
    }

    /**
     * @param dateStr 日期字符串
     * @param num     正数相加，负数相减
     * @return
     */
    public static String dateComputeByYear(String dateStr, int num) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = sdf.parse(dateStr);
            calendar.setTime(date);
            calendar.add(Calendar.YEAR, num);
        } catch (Exception e) {
            return null;
        }
        return sdf.format(calendar.getTime());
    }


}
