package com.yonyoucloud.fi.cmp.util;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * 提供一些对象有效性校验的方法
 */
@SuppressWarnings("rawtypes")
@Slf4j
public final class ValueUtils {

    /**
     * 判断字符串是否是符合指定格式的时间
     *
     * @param date   时间字符串
     * @param format 时间格式
     * @return 是否符合
     */
    public final static boolean isDate(String date, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.parse(date);
            return true;
        } catch (ParseException e) {
        	log.error("日记账登账接口调用失败:" + e);
        }
        return false;
    }

    /**
     * 判断字符串有效性
     */
    public final static boolean isEmpty(String src) {
        return src == null || "".equals(src.trim()) || "null".equals(src.trim());
    }

    /**
     * 判断字符串有效性
     */
    public final static boolean isNotEmpty(String src) {
        return src != null && !"".equals(src.trim()) && !"null".equals(src.trim()) && !"NULL".equals(src.trim());
    }

    /**
     * 判断一个对象是否为空或空集合
     */
    public final static boolean isNotEmptyObj(Object obj) {
        if(obj instanceof String){
            if (!isNotEmpty(String.valueOf(obj))) {
                return false;
            }
        }
        if(obj instanceof Collection){
            if (!isNotEmpty((Collection)obj)) {
                return false;
            }
        }
        if(obj instanceof Map){
            if (!isNotEmpty((Map)obj)) {
                return false;
            }
        }
        return null != obj;
    }

    /**
     * 判断集合的有效性
     */
    public final static boolean isEmpty(Collection col) {
        return col == null || col.size() == 0;
    }

    /**
     * 判断集合的有效性
     */
    public final static boolean isNotEmpty(Collection col) {
        return col != null && col.size() > 0;
    }

    /**
     * 判断map是否有效
     *
     * @param map
     * @return
     */
    public final static boolean isEmpty(Map map) {
        return map == null || map.size() == 0;
    }

    /**
     * 判断map是否有效
     *
     * @param map
     * @return
     */
    public final static boolean isNotEmpty(Map map) {
        return map != null && map.size() > 0;
    }


    /**
     * <h2>校验时间是否正确</h2>
     *
     * @param pubts : 时间戳
     * @return boolean
     * @author Sun GuoCai
     * @since 2022/10/27 13:45
     */
    public final static boolean isMatchesPubts(String pubts) {
        String regular3="^(\\d{4})-([0-1]\\d)-([0-3]\\d)\\s([0-5]\\d):([0-5]\\d):([0-5]\\d)$";
        return Pattern.matches(regular3, pubts);
    }

    /**
     * 按传入参数 进行分组
     * @param list
     * @param size
     * @param <T>
     * @return
     */
    public static <T> List<List<T>> splitList(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        int chunks = list.size() / size;
        int remainder = list.size() % size;
        int offset = 0;
        for (int i = 0; i < chunks; i++) {
            result.add(list.subList(offset, offset + size));
            offset += size;
        }
        if (remainder > 0) {
            result.add(list.subList(offset, offset + remainder));
        }
        return result;
    }

    /**
     * 获取4位随机数
     * @return
     */
    public static String getL4() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            stringBuilder.append(new SecureRandom().nextInt(9));
        }
        return stringBuilder.toString();
    }

}
