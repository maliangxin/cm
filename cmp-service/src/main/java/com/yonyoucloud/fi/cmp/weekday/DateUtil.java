package com.yonyoucloud.fi.cmp.weekday;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;

import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class DateUtil {

    public static final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String now(DateTimeFormatter formatter) {
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }

    /**
     * 将（yyyy-MM-dd HH:mm:ss）类型的字符型转为LocalDate类型
     */
    public static LocalDate parseTime2LocalDate(String date) {
        if (StringUtils.isEmpty(date)) {
            return null;
        }
        return LocalDate.parse(date, FORMAT_TIME);
    }

    /**
     * 格式化日期对象为yyyy-MM-dd格式
     *
     * @param date 日期对象
     * @return yyyy-MM-dd格式
     */
    public static String formatDate2String(Date date) {
        if (ObjectUtils.isEmpty(date)) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    /**
     * 根据前端获取的(yyyy-MM-dd)日期字符串转为日期对象
     *
     * @param date yyyy-MM-dd格式字符串
     */
    public static Date parseDate2Date(String date) throws ParseException {
        if (StringUtils.isEmpty(date)) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd").parse(date);
    }

    /**
     * 根据前端获取的(yyyy-MM-dd HH:mm:ss)日期字符串转为日期对象
     *
     * @param date yyyy-MM-dd HH:mm:ss格式字符串
     */
    public static Date parseTime2Date(String date) throws ParseException {
        if (StringUtils.isEmpty(date)) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
    }


    /**
     * 根据前端获取的(yyyy-MM-dd)日期字符串转为日期对象
     *
     * @param date yyyy-MM-dd格式字符串
     */
    public static Date formatDate(String date) throws ParseException {
        if (ObjectUtils.isEmpty(date)) {
            return null;
        }
        Date date1;
        try {
            date1 = new SimpleDateFormat("yyyy-MM-dd").parse(date);
        } catch (Exception e) {
            return new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(date);
        }
        return date1;
    }


    /**
     * 获取date的前一天
     */
    public static Date preDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTime();
    }

    /**
     * 获取date的后一天
     */
    public static Date nextDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    /**
     * 获取date的后n天
     */
    public static Date nextDay(Date date, int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, n);
        return calendar.getTime();
    }

    /**
     * 计算n个月
     *
     * @param date
     * @param n
     * @return
     */
    public static Date nextMonth(Date date, int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, n);
        return calendar.getTime();
    }

    /**
     * 将字符串或Date类型的日期，格式化成yyyy-MM-dd的格式，再进行比较
     */
    public static int compareDate(Object arg1, Object arg2) {
        Date date1 = format(arg1);
        Date date2 = format(arg2);
        if (date1 == null || date2 == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102389"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_180EFB4404B8006C", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B0", "日期不能为空！") /* "日期不能为空！" */));
        }
        // 如果有日期为空，返回0
        return date1.compareTo(date2);
    }

    /**
     * 将`String、Date、LocalDate`格式化为Date（yyyy-MM-dd）
     *
     * @param arg
     * @return
     */
    public static Date format(Object arg) {
        Date date1 = null;
        if (ObjectUtils.isEmpty(arg)) {
            return null;
        }
        if (arg instanceof String) {
            String arg1 = (String) arg;
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            try {
                if (arg1.contains("/")) {
                    format = new SimpleDateFormat("yyyy/MM/dd");
                } else if (arg1.contains("-")) {
                    format = new SimpleDateFormat("yyyy-MM-dd");
                }
                date1 = format.parse(arg1);
            } catch (ParseException e) {
                if (arg1.contains("/")) {
                    format = new SimpleDateFormat("yyyy/M/dd");
                } else if (arg1.contains("-")) {
                    format = new SimpleDateFormat("yyyy-M-dd");
                }
                try {
                    date1 = format.parse(arg1);
                } catch (ParseException parseException) {
                    if (arg1.contains("/")) {
                        format = new SimpleDateFormat("yyyy/MM/d");
                    } else if (arg1.contains("-")) {
                        format = new SimpleDateFormat("yyyy-MM-d");
                    }
                    try {
                        date1 = format.parse(arg1);
                    } catch (ParseException exception) {
                        if (arg1.contains("/")) {
                            format = new SimpleDateFormat("yyyy/M/d");
                        } else if (arg1.contains("-")) {
                            format = new SimpleDateFormat("yyyy-M-d");
                        }
                        try {
                            date1 = format.parse(arg1);
                        } catch (ParseException ex) {
                            log.error("", e);
                        }
                    }
                }
            }
        } else if (arg instanceof Date) {
            try {
                date1 = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(arg));
            } catch (ParseException e) {
                log.error("", e);
            }
        } else if (arg instanceof LocalDate) {
            date1 = localDate2Date((LocalDate) arg);
            try {
                date1 = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(date1));
            } catch (ParseException e) {
                log.error("", e);
            }
        }
        return date1;
    }


    /**
     * 获取日期得后一天
     *
     * @param date
     * @return
     */
    public static Date laterDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    /**
     * 获取当天0点时间
     */
    public static Date getNowDate() throws ParseException {
        Date nowDate = new Date();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String formatDate = format.format(nowDate);
        return format.parse(formatDate);
    }

    /**
     * 获取日期和时间
     */
    public static Date getNowDateTime() {
        return new Date();
    }


    /**
     * 自动付款编码
     *
     * @param k
     */
    public static String getAutoCode(int k) {

        return getStringLocalDate() + getRandom(k);
    }

    /**
     * 获取现在时间
     *
     * @return返回字符串格式 yyyyMMddHHmmss
     */
    public static String getStringLocalDate() {
        return LocalDateTime.now().toString();
    }

    /**
     * 返回一个随机数
     *
     * @param i
     * @return
     */
    public static String getRandom(int i) {
        SecureRandom jjj = new SecureRandom();
        if (i == 0) {
            return "";
        }
        StringBuilder jj = new StringBuilder();
        for (int k = 0; k < i; k++) {
            jj.append(jjj.nextInt(9));
        }
        return jj.toString();
    }

    /**
     * 获取新增的天数
     *
     * @param days
     * @return
     */
    public static Date newDateAddDay(int days) throws ParseException {
        Date nowDate = getNowDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nowDate);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    /**
     * 获取新增的天数
     *
     * @param days
     * @return
     */
    public static Date addDay(int days) {
        Date nowDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nowDate);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    //获取两个时间之间的日期

    /**
     * 算头不算尾 sum=0
     * 算尾不算头 sum=0
     * 算头算尾 sum=1
     *
     * @param endDate 截止
     * @param nowDate 起始
     * @param sum     增量
     * @return
     */
    public static int getDatePoor(Date endDate, Date nowDate, int sum) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(endDate);
        long time1 = cal.getTimeInMillis();
        cal.setTime(nowDate);
        long time2 = cal.getTimeInMillis();
        long between_days = (time2 - time1) / (1000 * 3600 * 24);
        int parseInt = Integer.parseInt(String.valueOf(between_days));
        parseInt = parseInt + sum;
        return parseInt;

    }

    /**
     * String 转日期
     *
     * @param dateStr
     * @param format
     * @return
     * @throws ParseException
     */
    public static Date getDate(String dateStr, String format) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);//注意月份是MM
        Date date = simpleDateFormat.parse(dateStr);
        return date;
    }

    public static Date getDate(Date date) throws ParseException {
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        String str = localDate.format(FORMAT_DATE);
        LocalDate parsed = LocalDate.parse(str, FORMAT_DATE);
        return Date.from(parsed.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static String getDateStr(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(FORMAT_DATE);
    }

    /**
     * localDate日期转为Date类型
     */
    public static Date localDate2Date(LocalDate localDate) {
        if (null == localDate) {
            return null;
        }
        String localDateString = formatLocalDate(localDate);
        return java.sql.Date.valueOf(localDateString);
    }

    /**
     * localDate日期时间转为Date类型
     */
    public static Date localDateTime2Date(LocalDateTime localDateTime) {
        if (null == localDateTime) {
            return null;
        }
        String localDateString = formatLocalDateTime(localDateTime);
        return java.sql.Date.valueOf(localDateString.split(" ")[0]);
    }



    /**
     * 半年包含的实际天数
     *
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
        return Long.valueOf(days).intValue();
    }

    /**
     * 季度包含的实际天数
     *
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
        return Long.valueOf(days).intValue();
    }

    /**
     * 获取下个月相同日，如果不存在，则取月底最后一天，如：date=2021-01-31, firstDayOfMonth=31,则返回值是2021-02-28
     *
     * @param lastDate        {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextMonth(LocalDate lastDate, int firstDayOfMonth) {
        return getSameDayOfNextTimeUnit(lastDate, firstDayOfMonth, 1);
    }

    /**
     * 获取下个季度相同日
     *
     * @param lastDate        {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextQuarter(LocalDate lastDate, int firstDayOfMonth) {
        return getSameDayOfNextTimeUnit(lastDate, firstDayOfMonth, 3);
    }

    /**
     * 获取下个半年相同日
     *
     * @param lastDate        {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextHalfYear(LocalDate lastDate, int firstDayOfMonth) {
        return getSameDayOfNextTimeUnit(lastDate, firstDayOfMonth, 6);
    }

    /**
     * 获取下个半年相同日
     *
     * @param lastDate        {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextYear(LocalDate lastDate, int firstDayOfMonth) {
        return getSameDayOfNextTimeUnit(lastDate, firstDayOfMonth, 12);
    }

    /**
     * 获取下个时间单位（月、季度、半年、年）相同日
     *
     * @param lastDate        {@link LocalDate} 上期日期
     * @param firstDayOfMonth 首次日
     * @param timeUnit        时间单位：月-1、季度-3、半年-6、年-12
     * @return 相同日
     */
    public static LocalDate getSameDayOfNextTimeUnit(LocalDate lastDate, int firstDayOfMonth, int timeUnit) {
        LocalDate nextMonthDate = lastDate.plusMonths(timeUnit);
        Month nextMonth = nextMonthDate.getMonth();
        int maxDayOfNextMonth = nextMonth.length(nextMonthDate.isLeapYear());
        return LocalDate.of(nextMonthDate.getYear(), nextMonth, Math.min(firstDayOfMonth, maxDayOfNextMonth));
    }

    /**
     * 校验日期是否在开始日期和结算日期之间
     *
     * @param beginDate 开始日期
     * @param endDate   结束日期
     * @param checkDate 校验日期
     * @return 是否
     */
    public static boolean checkBetweenPeriod(LocalDate beginDate, LocalDate endDate, LocalDate checkDate) {
        return !checkDate.isBefore(beginDate) && !checkDate.isAfter(endDate);
    }

    /**
     * @Author zhangwwei
     * @Description 获取date的前n天
     * @Param date:
     * @Param n:
     * @Return java.util.Date
     * @Date 14:54 2022/12/14
     */
    public static Date beforeDays(Date date, int n) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -n);
        return calendar.getTime();
    }

    /**
     * @Author zhangwwei
     * @Description 获取当月的月末
     * @Param date:
     * @Param n:
     * @Return java.util.Date
     * @Date 14:58 2022/12/14
     */
    public static Date MonthDays(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.roll(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTime();
    }

    /**
     * 转化时间
     *
     * @param date
     * @return
     * @throws Exception
     */
    public static Date parseDate(String date) throws Exception {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.parse(date);
    }

    //获取基于当前日期截止日
    public static Date getNowEndDate(int beforeDays) {
        Date endTime = new Date();
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(endTime);
        calEnd.set(Calendar.HOUR_OF_DAY, 23); //时
        calEnd.set(Calendar.MINUTE, 59); //分
        calEnd.set(Calendar.SECOND, 59); //秒
        calEnd.set(Calendar.MILLISECOND, 999); //毫秒
        calEnd.add(Calendar.DAY_OF_MONTH, beforeDays);
        Date endTimeAfterDeal = calEnd.getTime();
        return endTimeAfterDeal;
    }

    public static void checkDate(Date checkDate) throws Exception {
        Date nowDate = new Date();
        Date endDateNow = DateUtil.nextMonth(nowDate, 1200);//100年
        if (checkDate.compareTo(endDateNow) > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102390"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_181DDCF60528007B", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B1", "输入日期不能超过100年") /* "输入日期不能超过100年" */));
        }
    }

    public static void checklocakDate(LocalDate localDate) throws Exception {
        Date reqEndDate = localDate2Date(localDate);
        Date nowDate = new Date();
        Date endDateNow = DateUtil.nextMonth(nowDate, 1200);//100年
        if (reqEndDate.compareTo(endDateNow) > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102390"),InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_181DDCF60528007B", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003B1", "输入日期不能超过100年") /* "输入日期不能超过100年" */));
        }
    }

    /**
     * 格式化日期
     */
    public static String formatLocalDate(LocalDate localDate) {
        return FORMAT_DATE.format(localDate);
    }

    /**
     * 格式化日期时间
     */
    public static String formatLocalDateTime(LocalDate localDate) {
        return FORMAT_TIME.format(localDate);
    }

    /**
     * 格式化日期时间
     */
    public static String formatLocalDateTime(LocalDateTime localDateTime) {
        return FORMAT_TIME.format(localDateTime);
    }
}

