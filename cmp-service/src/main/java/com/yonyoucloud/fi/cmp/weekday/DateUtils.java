package com.yonyoucloud.fi.cmp.weekday;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;

/**
 * @author shqhs
 * @date 2022/03/23
 * @description 日期工具类
 */
public class DateUtils {
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String FORMAT_18 = "yyyy-MM-dd HH:mm:ss";

    /**
     * 根据前端获取的(yyyy-MM-dd HH:mm:ss)日期字符串转为日期对象
     *
     * @param date yyyy-MM-dd HH:mm:ss格式字符串
     */
    public static Date parseDateTime2Date(String date) throws Exception {
        if (org.apache.commons.lang3.StringUtils.isEmpty(date)) {
            throw new Exception(String.format(InternationalUtils.getMessageWithDefault("UID:P_CAM-BE_180DF34605780131","Date Format Error！") /* "Date Format Error！" */));//@notranslate
        }
        return new SimpleDateFormat(FORMAT_18).parse(date);
    }

    /**
     * 毫秒时间戳转 LocalDateTime
     * @author shiqhs
     * @date 2021/11/23
     * @param timestamp 时间戳
     * @return {@link LocalDateTime}
     */
    public static LocalDateTime milliTimestamp2LocalDateTime(long timestamp) {
        Timestamp tms = new Timestamp(timestamp);
        // 直接转换为LocalDateTime
        LocalDateTime localDateTime = tms.toLocalDateTime();
        return localDateTime;
    }

    /**
     * 半年包含的实际天数
     * @param date 要计算的日期
     * @return 天数
     */
    public static int daysOfHalfYear(LocalDate date) {
        long days;
        if (date.getMonthValue() < Month.JULY.getValue()) {
            LocalDate firstDayOfJanuary = LocalDate.of(date.getYear(), Month.JANUARY, 1);
            LocalDate endDayOfJune = LocalDate.of(date.getYear(), Month.JUNE, Month.JUNE.length(date.isLeapYear()));
            days = endDayOfJune.toEpochDay() - firstDayOfJanuary.toEpochDay() + 1;
        } else {
            LocalDate firstDayOfJuly = LocalDate.of(date.getYear(), Month.JULY, 1);
            LocalDate endDayOfDecember = LocalDate.of(date.getYear(), Month.DECEMBER, Month.DECEMBER.length(date.isLeapYear()));
            days = endDayOfDecember.toEpochDay() - firstDayOfJuly.toEpochDay() + 1;

        }
        return new Long(days).intValue();
    }


    /**
     * 半年包含的实际天数
     * @param date 要计算的日期
     * @return 天数
     */
    public static int daysOfHalfYear(LocalDate date, boolean excludeLeapDay) {
        long days;
        if (date.getMonthValue() < Month.JULY.getValue()) {
            LocalDate firstDayOfJanuary = LocalDate.of(date.getYear(), Month.JANUARY, 1);
            LocalDate endDayOfJune = LocalDate.of(date.getYear(), Month.JUNE, Month.JUNE.length(date.isLeapYear()));
            days = endDayOfJune.toEpochDay() - firstDayOfJanuary.toEpochDay() + 1;
            if (date.isLeapYear() && excludeLeapDay) {
                days = days - 1;
            }
        } else {
            LocalDate firstDayOfJuly = LocalDate.of(date.getYear(), Month.JULY, 1);
            LocalDate endDayOfDecember = LocalDate.of(date.getYear(), Month.DECEMBER, Month.DECEMBER.length(date.isLeapYear()));
            days = endDayOfDecember.toEpochDay() - firstDayOfJuly.toEpochDay() + 1;

        }
        return new Long(days).intValue();
    }


    /**
     * 季度包含的实际天数
     * @param date 要计算的日期
     * @return 天数
     */
    public static int daysOfQuarter(LocalDate date, boolean excludeLeapDay) {
        Month month = date.getMonth();
        Month firstMonthOfQuarter = month.firstMonthOfQuarter();
        Month endMonthOfQuarter = Month.of(firstMonthOfQuarter.getValue() + 2);
        LocalDate firstDayOfQuarter = LocalDate.of(date.getYear(), firstMonthOfQuarter, 1);
        LocalDate endDayOfQuarter = LocalDate.of(date.getYear(), endMonthOfQuarter, endMonthOfQuarter.length(date.isLeapYear()));
        long days = endDayOfQuarter.toEpochDay() - firstDayOfQuarter.toEpochDay() + 1;
        if (date.isLeapYear() && excludeLeapDay) {
            int firstQuarterStartDay = 1;
            int firstQuarterEndDay = Month.APRIL.firstDayOfYear(true);
            int todayIndex = date.getDayOfYear();
            if (todayIndex >= firstQuarterStartDay && todayIndex < firstQuarterEndDay) {
                days = days - 1;
            }
        }
        return new Long(days).intValue();
    }

    public static int daysOfMonth(LocalDate date, boolean excludeLeapDay) {
        Month month = date.getMonth();
        int days = date.lengthOfMonth();
        if (date.isLeapYear() && month == Month.FEBRUARY && excludeLeapDay) {
            days = days - 1;
        }
        return new Long(days).intValue();
    }

    public static int daysOfWeek(LocalDate date, boolean excludeLeapDay) {
        int days = 7;
        if (date.isLeapYear() && excludeLeapDay) {
            int dayOfWeekIndex = date.getDayOfWeek().ordinal();
            int weekStartDayOfYear = date.getDayOfYear() - dayOfWeekIndex + 1;
            int weekEndDayOfYear = weekStartDayOfYear + 7 - 1;
            int leapDayIndex = Month.MARCH.firstDayOfYear(true) - 1;
            if (leapDayIndex >= weekStartDayOfYear && leapDayIndex <= weekEndDayOfYear) {
                days = days - 1;
            }
        }
        return new Long(days).intValue();
    }

    public static boolean isLeapDay(LocalDate date) {
        if (!date.isLeapYear()) {
            return false;
        }
        int leapDayIndex = Month.MARCH.firstDayOfYear(true) - 1;
        return leapDayIndex == date.getDayOfYear();
    }

    /**
     * 是否包含闰年的2月29日
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @return 是否包含
     */
    public static Boolean containLeapYearDay(LocalDate beginDate, LocalDate endDate) {
        endDate = getFirstAfterYear(beginDate, endDate);
        Month beginDateMonth = beginDate.getMonth();
        Month endDateMonth = endDate.getMonth();
        // 都不是闰年
        if (!beginDate.isLeapYear() && !endDate.isLeapYear()) {
            return false;
        }
        // 都是闰年
        if (beginDate.isLeapYear() && endDate.isLeapYear()) {
            return beginDateMonth.getValue() < 3 && endDateMonth.getValue() > 2;
        }
        // 开始日期是闰年
        if (beginDate.isLeapYear() && beginDateMonth.getValue() < 3) {
            return true;
        }
        // 结束日期是闰年
        return endDate.isLeapYear() && endDateMonth.getValue() > 2;
    }

    /**
     * 不满一年且不包含2月29日
     * @param beginDate 开始日期
     * @param endDate 结算日期
     * @return 是否不满一年且不包含2月29日
     */
    public static Boolean notFullYearAndNotContainLeapYearDay(LocalDate beginDate, LocalDate endDate) {
        return beginDate.plusYears(1).isAfter(endDate) && !containLeapYearDay(beginDate, endDate);
    }

    /**
     * 获取开始日期后结束日期第一年的日期
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @return 结束日期第一年的日期
     */
    public static LocalDate getFirstAfterYear(LocalDate beginDate, LocalDate endDate) {
        if (endDate.minusYears(1).isAfter(beginDate)) {
            endDate = getFirstAfterYear(beginDate, endDate.minusYears(1));
        }
        return endDate;
    }

    /**
     * 季度包含的实际天数
     * @param date 要计算的日期
     * @return 天数
     */
    public static int daysOfQuarter(LocalDate date) {
        Month month = date.getMonth();
        Month firstMonthOfQuarter = month.firstMonthOfQuarter();
        Month endMonthOfQuarter = Month.of(firstMonthOfQuarter.getValue() + 2);
        LocalDate firstDayOfQuarter = LocalDate.of(date.getYear(), firstMonthOfQuarter, 1);
        LocalDate endDayOfQuarter = LocalDate.of(date.getYear(), endMonthOfQuarter, endMonthOfQuarter.length(date.isLeapYear()));
        long days = endDayOfQuarter.toEpochDay() - firstDayOfQuarter.toEpochDay() + 1;
        return new Long(days).intValue();
    }

    /**
     * 获取下个月相同日，如果不存在，则取月底最后一天，如：date=2021-01-31, firstDayOfMonth=31,则返回值是2021-02-28
     * @param lastDate {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextMonth(LocalDate lastDate, int firstDayOfMonth) {
        return getSameDayOfNextTimeUnit(lastDate, firstDayOfMonth, 1);
    }

    /**
     * 获取下个季度相同日
     * @param lastDate {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextQuarter(LocalDate lastDate, int firstDayOfMonth) {
        return getSameDayOfNextTimeUnit(lastDate, firstDayOfMonth, 3);
    }

    /**
     * 获取下个半年相同日
     * @param lastDate {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextHalfYear(LocalDate lastDate, int firstDayOfMonth) {
        return getSameDayOfNextTimeUnit(lastDate, firstDayOfMonth, 6);
    }

    /**
     * 获取下个半年相同日
     * @param lastDate {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextYear(LocalDate lastDate, int firstDayOfMonth) {
        return getSameDayOfNextTimeUnit(lastDate, firstDayOfMonth, 12);
    }

    /**
     * 获取下个时间单位（月、季度、半年、年）相同日
     * @param lastDate {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @param timeUnit 时间单位：月-1、季度-3、半年-6、年-12
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextTimeUnit(LocalDate lastDate, int firstDayOfMonth, int timeUnit) {
        LocalDate nextMonthDate = lastDate.plusMonths(timeUnit);
        Month nextMonth = nextMonthDate.getMonth();
        int maxDayOfNextMonth = nextMonth.length(nextMonthDate.isLeapYear());
        return LocalDate.of(nextMonthDate.getYear(), nextMonth, Math.min(firstDayOfMonth, maxDayOfNextMonth));
    }

    /**
     * 获取下 N 个时间单位（月、季度、半年、年）相同日
     * @param firstDate 首个日期
     * @param timeUnit 时间单位: 月-1、季度-3、半年-6、年-12
     * @param periods 周期数
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextNTimeUnit(LocalDate firstDate, int timeUnit, long periods, Integer monthAdjustDate) {
        LocalDate nextMonthDate = firstDate.plusMonths((long) timeUnit * periods);
        Month nextMonth = nextMonthDate.getMonth();
        int maxDayOfNextMonth = nextMonth.length(nextMonthDate.isLeapYear());
        //月末调整日期 大于起始日期，则需要按照调整后日期处理
        int firstDay = firstDate.getDayOfMonth();
        if(monthAdjustDate !=null && monthAdjustDate > firstDate.getDayOfMonth()){
            firstDay = monthAdjustDate;
        }
        return LocalDate.of(nextMonthDate.getYear(), nextMonth, Math.min(firstDay, maxDayOfNextMonth));
    }

    /**
     * 校验日期是否在开始日期和结算日期之间
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @param checkDate 校验日期
     * @return 是否
     */
    public static boolean checkBetweenPeriod(LocalDate beginDate, LocalDate endDate, LocalDate checkDate) {
        return !checkDate.isBefore(beginDate) && !checkDate.isAfter(endDate);
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

}
