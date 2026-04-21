package com.yonyoucloud.fi.cmp.util;


import cn.hutool.core.date.DateUtil;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class DateUtils {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String MINUTE_PATTERN = "yyyy-MM-dd HH:mm";
    public static final String HOUR_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String MONTH_PATTERN = "yyyy-MM";
    public static final String YEAR_PATTERN = "yyyy";
    public static final String MINUTE_ONLY_PATTERN = "mm";
    public static final String HOUR_ONLY_PATTERN = "HH";
    public static final String MILLISECOND_PATTERN = "yyyyMMddHHmmssSSS";
    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
    public static final String YYYYMMDD = "yyyyMMdd";
    public static final String YQL_REQUEST_DATE_FORMAT = "yyyyMMdd";
    public static final String HHMMSS = "HHmmss";
    public static final String pattern = "yyyy-MM-dd";

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String START_DATE_DATE_TYPE = "startDate_dateType";
    public static final String END_DATE_DATE_TYPE = "endDate_dateType";

    public static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);

    public static DateTimeFormatter getDateTimeFormatter() {
        return dateFormatter;
    }


    public static SimpleDateFormat getYYMMDDFormat() {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        return formatter;
    }

    public static SimpleDateFormat getYYMMDDSSFormat() {
        SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter2;
    }

    /**
     * 日期相加减天数
     *
     * @param date        如果为Null，则为当前时间
     * @param days        加减天数
     * @param includeTime 是否包括时分秒,true表示包含
     * @return
     * @throws ParseException
     */
    public static Date dateAdd(Date date, int days, boolean includeTime) throws ParseException {
        if (date == null) {
            date = new Date();
        }
        if (!includeTime) {
            SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.DATE_PATTERN);
            date = sdf.parse(sdf.format(date));
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    /**
     * 时间格式化成字符串
     *
     * @param date    Date
     * @param pattern StrUtils.DATE_TIME_PATTERN || StrUtils.DATE_PATTERN， 如果为空，则为yyyy-MM-dd
     * @return
     * @throws ParseException
     */
    public static String dateFormat(Date date, String pattern) throws ParseException {
        if (!ValueUtils.isNotEmpty(pattern)) {
            pattern = DateUtils.DATE_PATTERN;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    /**
     * 字符串解析成时间对象
     *
     * @param dateTimeString String
     * @param pattern        StrUtils.DATE_TIME_PATTERN || StrUtils.DATE_PATTERN，如果为空，则为yyyy-MM-dd
     * @return
     * @throws ParseException
     */
    public static Date dateParse(String dateTimeString, String pattern) throws ParseException {
        if (dateTimeString.contains("CST")) {
            return DateUtil.parseCST(dateTimeString);
        }
        if (!ValueUtils.isNotEmpty(pattern)) {
            pattern = DateUtils.DATE_PATTERN;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            return sdf.parse(dateTimeString);
        } catch (ParseException e) {
            //解析错误时，尝试再用hutool解析一次
            return DateUtil.parse(dateTimeString);
        }
    }

    /**
     * 将日期时间格式成只有日期的字符串（可以直接使用dateFormat，Pattern为Null进行格式化）
     *
     * @param dateTime Date
     * @return
     * @throws ParseException
     */
    public static String dateTimeToDateString(Date dateTime) throws ParseException {
        String dateTimeString = DateUtils.dateFormat(dateTime, DateUtils.DATE_TIME_PATTERN);
        return dateTimeString.substring(0, 10);
    }

    /**
     * 当时、分、秒为00:00:00时，将日期时间格式成只有日期的字符串，
     * 当时、分、秒不为00:00:00时，直接返回
     *
     * @param dateTime Date
     * @return
     * @throws ParseException
     */
    public static String dateTimeToDateStringIfTimeEndZero(Date dateTime) throws ParseException {
        String dateTimeString = DateUtils.dateFormat(dateTime, DateUtils.DATE_TIME_PATTERN);
        if (dateTimeString.endsWith("00:00:00")) {
            return dateTimeString.substring(0, 10);
        } else {
            return dateTimeString;
        }
    }

    /**
     * 将日期时间格式成日期对象，和dateParse互用
     *
     * @param dateTime Date
     * @return Date
     * @throws ParseException
     */
    public static Date dateTimeToDate(Date dateTime) throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateTime);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 时间加减小时
     *
     * @param startDate 要处理的时间，Null则为当前时间
     * @param hours     加减的小时
     * @return Date
     */
    public static Date dateAddHours(Date startDate, int hours) {
        if (startDate == null) {
            startDate = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        c.set(Calendar.HOUR, c.get(Calendar.HOUR) + hours);
        return c.getTime();
    }

    /**
     * 时间加减分钟
     *
     * @param startDate 要处理的时间，Null则为当前时间
     * @param minutes   加减的分钟
     * @return
     */
    public static Date dateAddMinutes(Date startDate, int minutes) {
        if (startDate == null) {
            startDate = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) + minutes);
        return c.getTime();
    }

    /**
     * 时间加减秒数
     *
     * @param startDate 要处理的时间，Null则为当前时间
     * @return
     */
    public static Date dateAddSeconds(Date startDate, int seconds) {
        if (startDate == null) {
            startDate = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        c.set(Calendar.SECOND, c.get(Calendar.SECOND) + seconds);
        return c.getTime();
    }

    /**
     * 时间加减天数
     *
     * @param startDate 要处理的时间，Null则为当前时间
     * @param days      加减的天数
     * @return Date
     */
    public static Date dateAddDays(Date startDate, int days) {
        if (startDate == null) {
            startDate = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        c.set(Calendar.DATE, c.get(Calendar.DATE) + days);
        return c.getTime();
    }

    /**
     * 时间加减月数
     *
     * @param startDate 要处理的时间，Null则为当前时间
     * @param months    加减的月数
     * @return Date
     */
    public static Date dateAddMonths(Date startDate, int months) {
        if (startDate == null) {
            startDate = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) + months);
        return c.getTime();
    }

    /**
     * 时间加减年数
     *
     * @param startDate 要处理的时间，Null则为当前时间
     * @param years     加减的年数
     * @return Date
     */
    public static Date dateAddYears(Date startDate, int years) {
        if (startDate == null) {
            startDate = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) + years);
        return c.getTime();
    }

    /**
     * 转换时间到对应的字符串格式
     *
     * @param date
     * @param formatstr
     * @return
     */
    public static String parseDateToStr(Date date, String formatstr) {
        if (date == null || formatstr == null) {
            return null;
        }
        SimpleDateFormat sf = new SimpleDateFormat(formatstr);
        return sf.format(date);
    }

    /**
     * 时间比较（如果myDate>compareDate返回1，<返回-1，相等返回0）
     *
     * @param myDate      时间
     * @param compareDate 要比较的时间
     * @return int
     */
    public static int dateCompare(Date myDate, Date compareDate) {
        Calendar myCal = Calendar.getInstance();
        Calendar compareCal = Calendar.getInstance();
        myCal.setTime(myDate);
        compareCal.setTime(compareDate);
        return myCal.compareTo(compareCal);
    }

    /**
     * 获取两个时间中最小的一个时间
     *
     * @param date
     * @param compareDate
     * @return
     */
    public static Date dateMin(Date date, Date compareDate) {
        if (date == null) {
            return compareDate;
        }
        if (compareDate == null) {
            return date;
        }
        if (1 == dateCompare(date, compareDate)) {
            return compareDate;
        } else if (-1 == dateCompare(date, compareDate)) {
            return date;
        }
        return date;
    }

    /**
     * 获取两个时间中最大的一个时间
     *
     * @param date
     * @param compareDate
     * @return
     */
    public static Date dateMax(Date date, Date compareDate) {
        if (date == null) {
            return compareDate;
        }
        if (compareDate == null) {
            return date;
        }
        if (1 == dateCompare(date, compareDate)) {
            return date;
        } else if (-1 == dateCompare(date, compareDate)) {
            return compareDate;
        }
        return date;
    }

    /**
     * 获取两个日期（不含时分秒）相差的天数，不包含今天
     *
     * @param startDate
     * @param endDate
     * @return
     * @throws ParseException
     */
    public static int dateBetween(Date startDate, Date endDate) throws ParseException {
        Date dateStart = dateParse(dateFormat(startDate, DATE_PATTERN), DATE_PATTERN);
        Date dateEnd = dateParse(dateFormat(endDate, DATE_PATTERN), DATE_PATTERN);
        return (int) ((dateEnd.getTime() - dateStart.getTime()) / 1000 / 60 / 60 / 24);
    }

    /**
     * 获取两个日期（不含时分秒）相差的天数，不包含今天
     *
     * @param startDateStr
     * @param endDateStr
     */
    public static int dateBetween(String startDateStr, String endDateStr) throws ParseException {
        Date startDate = dateParse(startDateStr, null);
        Date endDate = dateParse(endDateStr, null);
        return dateBetween(startDate, endDate);
    }

    /**
     * 获取两个日期（不含时分秒）相差的天数，包含今天
     *
     * @param startDate
     * @param endDate
     * @return
     * @throws ParseException
     */
    public static int dateBetweenIncludeToday(Date startDate, Date endDate) throws ParseException {
        return dateBetween(startDate, endDate) + 1;
    }

    /**
     * 获取日期时间的年份，如2017-02-13，返回2017
     *
     * @param date
     * @return
     */
    public static int getYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    /**
     * 获取日期时间的月份，如2017年2月13日，返回2
     *
     * @param date
     * @return
     */
    public static int getMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取日期时间的第几天（即返回日期的dd），如2017-02-13，返回13
     *
     * @param date
     * @return
     */
    public static int getDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DATE);
    }

    /**
     * 获取日期时间当月的总天数，如2017-02-13，返回28
     *
     * @param date
     * @return
     */
    public static int getDaysOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.getActualMaximum(Calendar.DATE);
    }

    /**
     * 获取日期时间当年的总天数，如2017-02-13，返回2017年的总天数
     *
     * @param date
     * @return
     */
    public static int getDaysOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    /**
     * 根据时间获取当月最大的日期
     * <li>2017-02-13，返回2017-02-28</li>
     * <li>2016-02-13，返回2016-02-29</li>
     * <li>2016-01-11，返回2016-01-31</li>
     *
     * @param date Date
     * @return
     * @throws Exception
     */
    public static Date maxDateOfMonth(Date date) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int value = cal.getActualMaximum(Calendar.DATE);
        return dateParse(dateFormat(date, MONTH_PATTERN) + "-" + value, null);
    }

    /**
     * 根据时间获取当月最小的日期，也就是返回当月的1号日期对象
     *
     * @param date Date
     * @return
     * @throws Exception
     */
    public static Date minDateOfMonth(Date date) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int value = cal.getActualMinimum(Calendar.DATE);
        return dateParse(dateFormat(date, MONTH_PATTERN) + "-" + value, null);
    }

    /**
     * 获取现在时间
     *
     * @return 返回时间类型 yyyy-MM-dd HH:mm:ss
     */
    public static Date getNowDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = getYYMMDDFormat().format(new Date());
        ParsePosition pos = new ParsePosition(8);
        Date currentTime_2 = getYYMMDDFormat().parse(dateString, pos);
        return currentTime_2;
    }

    public static Date getNowModifyDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        ParsePosition pos = new ParsePosition(0);
        Date currentTime_2 = formatter.parse(dateString, pos);
        return currentTime_2;
    }

    /**
     * 获取现在时间
     *
     * @return返回短时间格式 yyyy-MM-dd
     */
    public static Date getNowDateShort() {
        String dateString = getYYMMDDFormat().format(new Date());
        ParsePosition pos = new ParsePosition(8);
        Date currentTime_2 = getYYMMDDFormat().parse(dateString, pos);
        return currentTime_2;
    }

    /**
     * 获取现在时间
     *
     * @return返回短时间格式 yyyy-MM-dd
     */
    public static Date getNowDateShort2() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = sdf.format(new Date());
        Date nowDateStr = sdf.parse(dateString);
        return nowDateStr;
    }

    /**
     * 获取现在时间
     *
     * @return返回字符串格式 yyyy-MM-dd HH:mm:ss
     */
    public static String getStringDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = getYYMMDDFormat().format(new Date());
        return dateString;
    }

    /**
     * 获取现在时间
     *
     * @return返回字符串格式 yyyyMMddHHmmss
     */
    public static String getStringAllDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String dateString = getYYMMDDFormat().format(new Date());
        return dateString;
    }

    /**
     * 获取现在时间
     *
     * @return返回字符串格式 yyyyMMddHHmmssSSS
     */
    public static String getLongStringAllDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String dateString = formatter.format(new Date());
        return dateString;
    }

    /**
     * 获取现在时间
     *
     * @return 返回短时间字符串格式yyyy-MM-dd
     */
    public static String getStringDateShort() {
        String dateString = getYYMMDDFormat().format(new Date());
        return dateString;
    }

    /**
     * 获取时间 小时:分;秒 HH:mm:ss
     *
     * @return
     */
    public static String getTimeShort() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String dateString = getYYMMDDFormat().format(new Date());
        return dateString;
    }

    /**
     * 将长时间格式字符串转换为时间 yyyy-MM-dd HH:mm:ss
     *
     * @param strDate
     * @return
     */
    public static Date strToDateLong(String strDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = getYYMMDDFormat().parse(strDate, pos);
        return strtodate;
    }

    /**
     * 将长时间格式时间转换为字符串 yyyy-MM-dd HH:mm:ss
     *
     * @param dateDate
     * @return
     */
    public static String dateToStrLong(Date dateDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = getYYMMDDFormat().format(dateDate);
        return dateString;
    }

    /**
     * 将短时间格式时间转换为字符串 yyyy-MM-dd
     *
     * @param dateDate
     * @param
     * @return
     */
    public static String dateToStr(Date dateDate) {
        String dateString = getYYMMDDFormat().format(dateDate);
        return dateString;
    }

    public static String dateToStr(java.time.LocalDate dateDate) {
        String dateString = dateFormatter.format(dateDate);
        return dateString;
    }

    /**
     * 将短时间格式字符串转换为时间 yyyy-MM-dd
     *
     * @param strDate
     * @return
     */
    public static Date strToDate(String strDate) {
        if (Objects.isNull(strDate)) {
            return null;
        }
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = getYYMMDDFormat().parse(strDate, pos);
        return strtodate;
    }

    /**
     * 将短时间格式字符串转换为时间 yyyy-MM-dd HH:mm:ss
     *
     * @param strDate
     * @return
     */
    public static Timestamp strToDateSql(String strDate) {
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = getYYMMDDSSFormat().parse(strDate, pos);
        return new Timestamp(strtodate.getTime());
    }

    /**
     * 得到现在时间
     *
     * @return
     */
    public static Date getNow() {
        Date currentTime = new Date();
        return currentTime;
    }

    /**
     * 提取一个月中的最后一天
     *
     * @param day
     * @return
     */
    public static Date getLastDate(long day) {
        Date date = new Date();
        long date_3_hm = date.getTime() - 3600000 * 34 * day;
        Date date_3_hm_date = new Date(date_3_hm);
        return date_3_hm_date;
    }

    /**
     * 得到现在时间
     *
     * @return 字符串 yyyyMMdd HHmmss
     */
    public static String getStringToday() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HHmmss");
        String dateString = getYYMMDDFormat().format(currentTime);
        return dateString;
    }

    /**
     * 功能：<br/>
     *
     * @author Tony
     * @version 2016年12月16日 下午4:41:51 <br/>
     */
    public static String getTodayShort() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        String dateString = getYYMMDDFormat().format(currentTime);
        return dateString;
    }

    /**
     * @param @param  value
     * @param @return
     * @return String
     * @throws
     * @Description: 输入一个整数类型的字符串, 然后转换成时分秒的形式输出
     * 例如：输入568
     * 返回结果为：00:09:28
     * 输入null或者“”
     * 返回结果为:00:00:00
     * @author Tony 鬼手卡卡
     * @date 2016-4-20
     */
    public static String getHHMMSS(String value) {
        String hour = "00";
        String minute = "00";
        String second = "00";
        if (value != null && !value.trim().equals("")) {
            int v_int = Integer.valueOf(value);
            hour = v_int / 3600 + "";//获得小时;
            minute = v_int % 3600 / 60 + "";//获得小时;
            second = v_int % 3600 % 60 + "";//获得小时;
        }
        return (hour.length() > 1 ? hour : "0" + hour) + ":" + (minute.length() > 1 ? minute : "0" + minute) + ":" + (second.length() > 1 ? second : "0" + second);
    }

    /**
     * 得到现在小时
     */
    public static String getHour() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = getYYMMDDFormat().format(currentTime);
        String hour;
        hour = dateString.substring(11, 13);
        return hour;
    }

    /**
     * 得到现在分钟
     *
     * @return
     */
    public static String getTime() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = getYYMMDDFormat().format(currentTime);
        String min;
        min = dateString.substring(14, 16);
        return min;
    }

    /**
     * 根据用户传入的时间表示格式，返回当前时间的格式 如果是yyyyMMdd，注意字母y不能大写。
     *
     * @param sformat yyyyMMddhhmmss
     * @return
     */
    public static String getUserDate(String sformat) {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(sformat);
        String dateString = getYYMMDDFormat().format(currentTime);
        return dateString;
    }

    /**
     * 二个小时时间间的差值,必须保证二个时间都是"HH:MM"的格式，返回字符型的分钟
     */
    public static String getTwoHour(String st1, String st2) {
        String[] kk = null;
        String[] jj = null;
        kk = st1.split(":");
        jj = st2.split(":");
        if (Integer.parseInt(kk[0]) < Integer.parseInt(jj[0]))
            return "0";
        else {
            double y = Double.parseDouble(kk[0]) + Double.parseDouble(kk[1]) / 60;
            double u = Double.parseDouble(jj[0]) + Double.parseDouble(jj[1]) / 60;
            if ((y - u) > 0)
                return y - u + "";
            else
                return "0";
        }
    }

    /**
     * 得到二个日期间的间隔天数
     */
    public static String getTwoDay(String sj1, String sj2) {
        long day = 0;
        try {
            Date date = getYYMMDDFormat().parse(sj1);
            Date mydate = getYYMMDDFormat().parse(sj2);
            day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
        } catch (Exception e) {
            return "";
        }
        return day + "";
    }

    /**
     * 时间前推或后推分钟,其中JJ表示分钟.
     */
    public static String getPreTime(String sj1, String jj) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String mydate1 = "";
        try {
            Date date1 = format.parse(sj1);
            long Time = (date1.getTime() / 1000) + Integer.parseInt(jj) * 60;
            date1.setTime(Time * 1000);
            mydate1 = format.format(date1);
        } catch (Exception e) {
        }
        return mydate1;
    }

    /**
     * 得到一个时间延后或前移几天的时间,nowdate(yyyy-mm-dd)为时间,delay为前移或后延的天数
     */
    public static String getNextDay(String nowdate, String delay) {
        try {
            String mdate = "";
            Date d = strToDate(nowdate);
            long myTime = (d.getTime() / 1000) + Integer.parseInt(delay) * 24 * 60 * 60;
            d.setTime(myTime * 1000);
            mdate = getYYMMDDFormat().format(d);
            return mdate;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 功能：<br/> 距离现在几天的时间是多少
     * 获得一个时间字符串，格式为：yyyy-MM-dd HH:mm:ss
     * day  如果为整数，表示未来时间
     * 如果为负数，表示过去时间
     *
     * @author Tony
     * @version 2016年11月29日 上午11:02:56 <br/>
     */
    public static String getFromNow(int day) {
        Date date = new Date();
        long dateTime = (date.getTime() / 1000) + day * 24 * 60 * 60;
        date.setTime(dateTime * 1000);
        return getYYMMDDSSFormat().format(date);
    }

    /**
     * 判断是否润年
     *
     * @param ddate
     * @return
     */
    public static boolean isLeapYear(String ddate) {

        /**
         * 详细设计： 1.被400整除是闰年，否则： 2.不能被4整除则不是闰年 3.能被4整除同时不能被100整除则是闰年
         * 3.能被4整除同时能被100整除则不是闰年
         */
        Date d = strToDate(ddate);
        GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
        gc.setTime(d);
        int year = gc.get(Calendar.YEAR);
        if ((year % 400) == 0)
            return true;
        else if ((year % 4) == 0) {
            if ((year % 100) == 0)
                return false;
            else
                return true;
        } else
            return false;
    }

    /**
     * 返回美国时间格式 26 Apr 2006
     *
     * @param str
     * @return
     */
    public static String getEDate(String str) {
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = getYYMMDDFormat().parse(str, pos);
        String j = strtodate.toString();
        String[] k = j.split(" ");
        return k[2] + k[1].toUpperCase() + k[5].substring(2, 4);
    }

    /**
     * 判断该日期是否是该月的最后一天
     *
     * @param dateStr 需要判断的日期
     * @return
     */
    public static boolean isLastDayOfMonth(String dateStr) {
        Calendar calendar = Calendar.getInstance();
        Date date = strToDate(dateStr);
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH) == calendar
                .getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取一个月的最后一天
     *
     * @param dat
     * @return
     */
    public static String getEndDateOfMonth(String dat) {// yyyy-MM-dd
        String str = dat.substring(0, 8);
        String month = dat.substring(5, 7);
        int mon = Integer.parseInt(month);
        if (mon == 1 || mon == 3 || mon == 5 || mon == 7 || mon == 8 || mon == 10 || mon == 12) {
            str += "31";
        } else if (mon == 4 || mon == 6 || mon == 9 || mon == 11) {
            str += "30";
        } else {
            if (isLeapYear(dat)) {
                str += "29";
            } else {
                str += "28";
            }
        }
        return str;
    }

    /**
     * 判断二个时间是否在同一个周
     *
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isSameWeekDates(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        int subYear = cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR);
        if (0 == subYear) {
            if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR))
                return true;
        } else if (1 == subYear && 11 == cal2.get(Calendar.MONTH)) {
            // 如果12月的最后一周横跨来年第一周的话则最后一周即算做来年的第一周
            if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR))
                return true;
        } else if (-1 == subYear && 11 == cal1.get(Calendar.MONTH)) {
            if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR))
                return true;
        }
        return false;
    }

    /**
     * 产生周序列,即得到当前时间所在的年度是第几周
     *
     * @return
     */
    public static String getSeqWeek() {
        Calendar c = Calendar.getInstance(Locale.CHINA);
        String week = Integer.toString(c.get(Calendar.WEEK_OF_YEAR));
        if (week.length() == 1)
            week = "0" + week;
        String year = Integer.toString(c.get(Calendar.YEAR));
        return year + week;
    }

    /**
     * 获得一个日期所在的周的星期几的日期，如要找出2002年2月3日所在周的星期一是几号
     *
     * @param sdate
     * @param num
     * @return
     */
    public static String getWeek(String sdate, String num) {
        // 再转换为时间
        Date dd = DateUtils.strToDate(sdate);
        Calendar c = Calendar.getInstance();
        c.setTime(dd);
        switch (num) {
            case "1":
                c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                break;
            case "2":
                c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                break;
            case "3":
                c.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                break;
            case "4":
                c.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
                break;
            case "5":
                c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
                break;
            case "6":
                c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                break;
            case "0":
                c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                break;
            default:
                break;
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
    }

    /**
     * 根据一个日期，返回是星期几的字符串
     *
     * @param sdate
     * @return
     */
    public static String getWeek(String sdate) {
        // 再转换为时间
        Date date = DateUtils.strToDate(sdate);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        // int hour=c.get(Calendar.DAY_OF_WEEK);
        // hour中存的就是星期几了，其范围 1~7
        // 1=星期日 7=星期六，其他类推
        return new SimpleDateFormat("EEEE").format(c.getTime());
    }

    public static String getWeekStr(String sdate) {
        String str = "";
        str = DateUtils.getWeek(sdate);
        switch (str) {
            case "1":
                str = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180149", "星期日") /* "星期日" */;
                break;
            case "2":
                str = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418014A", "星期一") /* "星期一" */;
                break;
            case "3":
                str = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418014B", "星期二") /* "星期二" */;
                break;
            case "4":
                str = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418014C", "星期三") /* "星期三" */;
                break;
            case "5":
                str = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418014D", "星期四") /* "星期四" */;
                break;
            case "6":
                str = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418014E", "星期五") /* "星期五" */;
                break;
            case "7":
                str = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418014F", "星期六") /* "星期六" */;
                break;
            default:
                break;
        }
        return str;
    }

    /**
     * 两个时间之间的天数
     *
     * @param date1
     * @param date2
     * @return
     */
    public static long getDays(String date1, String date2) {
        if (date1 == null || "".equals(date1))
            return 0;
        if (date2 == null || "".equals(date2))
            return 0;
        // 转换为标准时间
        Date date = new Date();
        Date mydate = new Date();
        try {
            date = getYYMMDDFormat().parse(date1);
            mydate = getYYMMDDFormat().parse(date2);
        } catch (Exception e) {
        }
        long day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
        return day;
    }

    /**
     * 形成如下的日历 ， 根据传入的一个时间返回一个结构 星期日 星期一 星期二 星期三 星期四 星期五 星期六 下面是当月的各个时间
     * 此函数返回该日历第一行星期日所在的日期
     *
     * @param sdate
     * @return
     */
    public static String getNowMonth(String sdate) {
        // 取该时间所在月的一号
        sdate = sdate.substring(0, 8) + "01";

        // 得到这个月的1号是星期几
        Date date = DateUtils.strToDate(sdate);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int u = c.get(Calendar.DAY_OF_WEEK);
        String newday = DateUtils.getNextDay(sdate, (1 - u) + "");
        return newday;
    }

    /**
     * 取得数据库主键 生成格式为yyyymmddhhmmss+k位随机数
     *
     * @param k 表示是取几位随机数，可以自己定
     */

    public static String getNo(int k) {

        return getUserDate("yyyyMMddhhmmss") + getRandom(k);
    }

    /**
     * 返回一个随机数
     *
     * @param i
     * @return
     */
    public static String getRandom(int i) {
        SecureRandom random = new SecureRandom();
        // int suiJiShu = jjj.nextInt(9);
        if (i == 0)
            return "";
        String str = "";
        for (int k = 0; k < i; k++) {
            str = str + random.nextInt(9);
        }
        return str;
    }

    public static boolean RightDate(String date) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        ;
        if (date == null)
            return false;
        if (date.length() > 10) {
            sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        } else {
            sdf = new SimpleDateFormat("yyyy-MM-dd");
        }
        try {
            sdf.parse(date);
        } catch (ParseException pe) {
            return false;
        }
        return true;
    }

    /***************************************************************************
     * //nd=1表示返回的值中包含年度 //yf=1表示返回的值中包含月份 //rq=1表示返回的值中包含日期 //format表示返回的格式 1
     * 以年月日中文返回 2 以横线-返回 // 3 以斜线/返回 4 以缩写不带其它符号形式返回 // 5 以点号.返回
     **************************************************************************/
    public static String getStringDateMonth(String sdate, String nd, String yf, String rq, String format) {
        Date currentTime = new Date();
        String dateString = getYYMMDDFormat().format(currentTime);
        String s_nd = dateString.substring(0, 4); // 年份
        String s_yf = dateString.substring(5, 7); // 月份
        String s_rq = dateString.substring(8, 10); // 日期
        String sreturn = "";
        //roc.util.MyChar mc = new roc.util.MyChar();
        //if (sdate == null || sdate.equals("") || !mc.Isdate(sdate)) { // 处理空值情况
        if (sdate == null || "".equals(sdate)) {
            if ("1".equals(nd)) {
                sreturn = s_nd;
                switch (format) {
                    case "1":
                        sreturn = sreturn + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180148", "年") /* "年" */;
                        break;
                    case "2":
                        sreturn = sreturn + "-";
                        break;
                    case "3":
                        sreturn = sreturn + "/";
                        break;
                    case "5":
                        sreturn = sreturn + ".";
                        break;
                    default:
                        break;
                }
            }
            // 处理月份
            if ("1".equals(yf)) {
                sreturn = sreturn + s_yf;
                switch (format) {
                    case "1":
                        sreturn = sreturn + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180146", "月") /* "月" */;
                        break;
                    case "2":
                        sreturn = sreturn + "-";
                        break;
                    case "3":
                        sreturn = sreturn + "/";
                        break;
                    case "5":
                        sreturn = sreturn + ".";
                        break;
                    default:
                        break;
                }
            }
            // 处理日期
            if ("1".equals(rq)) {
                sreturn = sreturn + s_rq;
                if ("1".equals(format))
                    sreturn = sreturn + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180147", "日") /* "日" */;
            }
        } else {
            // 不是空值，也是一个合法的日期值，则先将其转换为标准的时间格式
            sdate = getOKDate(sdate);
            s_nd = sdate.substring(0, 4); // 年份
            s_yf = sdate.substring(5, 7); // 月份
            s_rq = sdate.substring(8, 10); // 日期
            if ("1".equals(nd)) {
                sreturn = s_nd;
                // 处理间隔符
                switch (format) {
                    case "1":
                        sreturn = sreturn + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180148", "年") /* "年" */;
                        break;
                    case "2":
                        sreturn = sreturn + "-";
                        break;
                    case "3":
                        sreturn = sreturn + "/";
                        break;
                    case "5":
                        sreturn = sreturn + ".";
                        break;
                    default:
                        break;
                }
            }
            // 处理月份
            if ("1".equals(yf)) {
                sreturn = sreturn + s_yf;
                switch (format) {
                    case "1":
                        sreturn = sreturn + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180146", "月") /* "月" */;
                        break;
                    case "2":
                        sreturn = sreturn + "-";
                        break;
                    case "3":
                        sreturn = sreturn + "/";
                        break;
                    case "5":
                        sreturn = sreturn + ".";
                        break;
                    default:
                        break;
                }
            }
            // 处理日期
            if ("1".equals(rq)) {
                sreturn = sreturn + s_rq;
                if ("1".equals(format))
                    sreturn = sreturn + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180147", "日") /* "日" */;
            }
        }
        return sreturn;
    }

    public static String getNextMonthDay(String sdate, int m) {
        sdate = getOKDate(sdate);
        int year = Integer.parseInt(sdate.substring(0, 4));
        int month = Integer.parseInt(sdate.substring(5, 7));
        month = month + m;
        if (month < 0) {
            month = month + 12;
            year = year - 1;
        } else if (month > 12) {
            month = month - 12;
            year = year + 1;
        }
        String smonth = "";
        if (month < 10)
            smonth = "0" + month;
        else
            smonth = "" + month;
        return year + "-" + smonth + "-10";
    }

    /**
     * 功能：<br/>
     *
     * @author Tony
     * @version 2015-3-31 上午09:29:31 <br/>
     */
    public static String getOKDate(String sdate) {
        if (sdate == null || "".equals(sdate))
            return getStringDateShort();

//      if (!VeStr.Isdate(sdate)) {
//       sdate = getStringDateShort();
//      }
//      // 将“/”转换为“-”
//      sdate = VeStr.Replace(sdate, "/", "-");
        // 如果只有8位长度，则要进行转换
        if (sdate.length() == 8)
            sdate = sdate.substring(0, 4) + "-" + sdate.substring(4, 6) + "-" + sdate.substring(6, 8);
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = getYYMMDDFormat().parse(sdate, pos);
        String dateString = getYYMMDDFormat().format(strtodate);
        return dateString;
    }

    /**
     * 获取当前时间的前一天时间
     *
     * @param cl
     * @return
     */
    private static String getBeforeDay(Calendar cl) {
        //使用roll方法进行向前回滚
        //cl.roll(Calendar.DATE, -1);
        //使用set方法直接进行设置
        // int day = cl.get(Calendar.DATE);
        cl.add(Calendar.DATE, -1);
        return getYYMMDDFormat().format(cl.getTime());
    }

    /**
     * 获取当前时间的后一天时间
     *
     * @param cl
     * @return
     */
    private static String getAfterDay(Calendar cl) {
        //使用roll方法进行回滚到后一天的时间
        //cl.roll(Calendar.DATE, 1);
        //使用set方法直接设置时间值
        //int day = cl.get(Calendar.DATE);
        cl.add(Calendar.DATE, 1);
        return getYYMMDDFormat().format(cl.getTime());
    }

    private static String getDateAMPM() {
        GregorianCalendar ca = new GregorianCalendar();
        //结果为“0”是上午     结果为“1”是下午
        int i = ca.get(GregorianCalendar.AM_PM);
        return i == 0 ? "AM" : "PM";
    }

    private static int compareToDate(String date1, String date2) {
        return date1.compareTo(date2);
    }

    private static int compareToDateString(String date1, String date2) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int i = 0;
        try {
            long ldate1 = getYYMMDDFormat().parse(date1).getTime();
            long ldate2 = getYYMMDDFormat().parse(date2).getTime();
            if (ldate1 > ldate2) {
                i = 1;
            } else if (ldate1 == ldate2) {
                i = 0;
            } else {
                i = -1;
            }

        } catch (ParseException e) {
            log.error(e.getMessage(), e);
        }
        return i;
    }

    public static String[] getFiveDate() {
        String[] dates = new String[2];
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String five = " 05:00:00";

        if ("AM".equals(getDateAMPM()) && compareToDateString(getStringDate(), getStringDateShort() + five) == -1) {
            dates[0] = getBeforeDay(calendar) + five;
            dates[1] = getStringDateShort() + five;
        } else {
            dates[0] = getStringDateShort() + five;
            dates[1] = getAfterDay(calendar) + five;
        }

        return dates;
    }

    public static String getFiveDate2() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String five = " 05:00:00";
        String reStr = "";
        if ("AM".equals(getDateAMPM()) && compareToDateString(getStringDate(), getStringDateShort() + five) == -1) {
            reStr = getBeforeDay(calendar);
        } else {
            reStr = getStringDateShort();
        }
        return reStr;
    }

    /**
     * 获取当年的第一天
     *
     * @return
     */
    public static Date getCurrYearFirst() {
        Calendar currCal = Calendar.getInstance();
        int currentYear = currCal.get(Calendar.YEAR);
        return getYearFirst(currentYear);
    }


    /**
     * 获取当年的最后一天
     *
     * @return
     */
    public static Date getCurrYearLast() {
        Calendar currCal = Calendar.getInstance();
        int currentYear = currCal.get(Calendar.YEAR);
        return getYearLast(currentYear);
    }

    /**
     * 获取某年第一天日期
     *
     * @param year 年份
     * @return Date
     */
    public static Date getYearFirst(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        Date currYearFirst = calendar.getTime();
        return currYearFirst;
    }

    /**
     * 获取某年最后一天日期
     *
     * @param year 年份
     * @return Date
     */
    public static Date getYearLast(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.roll(Calendar.DAY_OF_YEAR, -1);
        Date currYearLast = calendar.getTime();
        return currYearLast;
    }

    /**
     * 获取前一天日期
     *
     * @return
     */
    public static Date getBeforeDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -1);
        return calendar.getTime();
    }

    public static String getLastMonth(Date date) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date); // 设置为当前时间
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1); // 设置为上一个月
        date = calendar.getTime();
        String accDate = dateFormat(date, MONTH_PATTERN);
        return accDate;
    }

    //把字符串转换成日期
    public static Date convertToDate(String s, String formate) {
        SimpleDateFormat sdf = new SimpleDateFormat(formate);
        try {
            Date date = sdf.parse(s);
            return date;
        } catch (ParseException e) {
            log.error("格式化日期失败：", e);
        }
        return null;
    }

    //日期转字符串
    public static String convertToStr(Date date, String formate) {
        SimpleDateFormat sdf = new SimpleDateFormat(formate);
        String format = sdf.format(date);
        return format;
    }


    /**
     * 获取当前日期的工具类
     */
    public static Date getCurrentDate(String pattern) {
        if (StringUtils.isEmpty(pattern)) {
            pattern = "yyyy-MM-dd";
        }
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        Date date = null;
        try {
            date = dateFormat.parse(dateFormat.format(calendar.getTime()));
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
        }
        return date;
    }

    //获取间隔对应天数的日期，并格式化“YYYY-MM-DD”
    public static Date formatBalanceDate(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, days);
        Date date = calendar.getTime();
        String dateString = DateUtil.format(date, "yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(dateString);
        date = java.sql.Date.valueOf(localDate);
        return date;
    }

    //日期格式化“YYYY-MM-DD”
    public static Date formatBalanceDate(Date date) {
        String dateString = DateUtil.format(date, "yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(dateString);
        date = java.sql.Date.valueOf(localDate);
        return date;
    }


    public static Date parseDate(String date, String format) throws Exception {
        if (org.apache.commons.lang3.StringUtils.isBlank(date)) {
            return null;
        }
        SimpleDateFormat myFormatter = new SimpleDateFormat(format);
        return myFormatter.parse(date);

    }

    /**
     * 转化时间
     *
     * @param date
     * @return
     * @throws Exception
     */
    public static Date parseDate(String date) throws Exception {
        if (org.apache.commons.lang3.StringUtils.isBlank(date)) {
            return null;
        }
        // 创建格式化对象
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.parse(date);
    }


    /**
     * 获取date的前一天
     *
     * @param date
     * @return
     */
    public static Date preDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTime();
    }

    /**
     * 获取date的后一天
     *
     * @param date
     * @return
     */
    public static Date afterDay(Date date) {
        return afterDay(date, 1);
    }

    /**
     * 获取date的后n天
     *
     * @param date
     * @return
     */
    public static Date afterDay(Date date, int n) {
        if (Objects.isNull(date)) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, n);
        return calendar.getTime();
    }

    /**
     * 获取date的前一天
     *
     * @param date
     * @return
     */
    public static Date beforeDay(Date date) {
          return beforeDay(date, 1);
    }

    /**
     * 获取date的前n天
     *
     * @param date
     * @return
     */
    public static Date beforeDay(Date date, int n) {
        if (Objects.isNull(date)) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -n);
        return calendar.getTime();
    }

    /**
     * 格式化时间
     *
     * @param date
     * @return
     * @throws Exception
     */
    public static String formatDate(Date date) throws Exception {
        if (Objects.isNull(date)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * 将前端插件的世界协调时间 (UTC) 转为 北京时间
     *
     * @param utcDate
     * @return
     * @throws ParseException
     */
    public static Date parseUTCDateToDate(String utcDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date date = sdf.parse(utcDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 8);
        Date result = formatBalanceDate(calendar.getTime());
        return result;
    }

    /**
     * 获取两个日期期间内的所有日期集合
     *
     * @param startDate
     * @param endDate
     * @return
     * @throws ParseException
     */
    public static List<String> getBetweenDateForStrEnd(Date startDate, Date endDate) throws ParseException {
        Calendar start = Calendar.getInstance();
        start.setTime(startDate); // 将开始日期传入Calendar实例中

        Calendar end = Calendar.getInstance();
        end.setTime(endDate); // 将结束日期传入Calendar实例中

        List<String> dateList = new ArrayList<>();

        while (!start.after(end)) { // 判断是否到达结束日期
            dateList.add(DateUtils.dateFormat(start.getTime(), DATE_PATTERN));
            start.add(Calendar.DAY_OF_MONTH, 1); // 日期加一天
        }
        return dateList;
    }

    /**
     * 获取当前日期的最后时刻(23:59:59)
     *
     * @param date yyyy-MM-dd
     * @return
     */
    public static Date getLastTimeForThisDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date maxTime = calendar.getTime();
        return maxTime;
    }

    public static Date clearTime(Date date) {
        if (date == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date clearTime = cal.getTime();
        return clearTime;
    }

    /**
     * 设置传入日期的时分秒为当前时间的时分秒
     *
     * @param date 要修改的日期对象
     * @return 修改后的新日期对象
     */
    public static Date setTimeToCurrent(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Input date cannot be null");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Calendar currentCalendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, currentCalendar.get(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, currentCalendar.get(Calendar.MILLISECOND));
        return calendar.getTime();
    }

    /**
     * 日期字符串转换为不带时分秒的格式
     *
     * @param dateStr
     * @return 格式为yyyy-MM-dd的日期
     * @throws Exception
     */
    public static String dateFormatWithoutTime(String dateStr) throws Exception {
        SimpleDateFormat dateFormatWithoutTime = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateFormatWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        Date withoutTimeDate = null;
        try {
            withoutTimeDate = dateFormatWithTime.parse(dateStr);
        } catch (Exception e) {
            // 忽略异常，尝试使用包含时分秒的格式解析
        }
        if (withoutTimeDate == null) {
            try {
                withoutTimeDate = dateFormatWithoutTime.parse(dateStr);
            } catch (Exception e) {
                // 忽略异常，尝试使用包含时分秒的格式解析
            }
        }
        if (withoutTimeDate == null) {
            try {
                withoutTimeDate = formatter.parse(dateStr);
            } catch (Exception e) {
                // 忽略异常，尝试使用包含时分秒的格式解析
            }
        }
        if (withoutTimeDate == null) {
            withoutTimeDate = new Date();
        }
        return DateUtils.dateFormat(withoutTimeDate, "yyyy-MM-dd");
    }

    public static Optional<String> getLocalDateTimeString(LocalDateTime localDateTime) throws Exception {
        return Optional.ofNullable(localDateTime.format(DateTimeFormatter.ofPattern(DateUtils.DATE_TIME_PATTERN)));
    }

    /**
     * 获取当前日期或业务日期
     * @return
     */
    public static Date getCurrDateOrBusinessDate() {
        if (BillInfoUtils.getBusinessDate() != null) {
            return BillInfoUtils.getBusinessDate();
        } else {
            return getCurrentDate(null);
        }
    }

    public static boolean isSameDay(Date startDateDateType, Date dateToday) {
        return org.apache.commons.lang3.time.DateUtils.isSameDay(startDateDateType, dateToday);
    }
}