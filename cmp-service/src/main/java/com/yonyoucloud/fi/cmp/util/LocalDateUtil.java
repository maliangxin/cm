package com.yonyoucloud.fi.cmp.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class LocalDateUtil {

    public static String getBeforeString(Long days) {
        LocalDate localDate = LocalDate.now().minusDays(days);
        LocalTime localTime = LocalTime.MAX;
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String result = localDateTime.format(df);
        return result;
    }

    public static String getBeforeDateString(Long days) {
        LocalDate localDate = LocalDate.now().minusDays(days);
        LocalTime localTime = LocalTime.MAX;
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMdd");
        String result = localDateTime.format(df);
        return result;
    }

    public static String getYesterdayString() {
        LocalDate localDate = LocalDate.now().minusDays(1L);
        LocalTime localTime = LocalTime.MAX;
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String result = localDateTime.format(df);
        return result;
    }

    public static String getNowDateString() {
        LocalDate date = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return date.format(fmt);
    }

    public static String getNowString() {
        LocalDate date = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        return date.format(fmt);
    }

}
