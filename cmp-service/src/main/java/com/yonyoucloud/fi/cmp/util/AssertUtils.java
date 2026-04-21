package com.yonyoucloud.fi.cmp.util;

import com.yonyoucloud.fi.cmp.common.CtmException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author xuxbo
 * @date 2023/4/21 15:17
 */
public class AssertUtils {

    /**
     * 判断参数是否为空
     *
     * @param obj
     * @param message
     */
    public static void isNull(Object obj, String message) {
        if (Objects.isNull(obj)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101026"),message);
        }
    }

    /**
     * 判断参数非空
     *
     * @param obj
     * @param message
     */
    public static void notNull(Object obj, String message) {
        if (Objects.nonNull(obj)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101027"),message);
        }
    }

    /**
     * 判断集合是否为空
     *
     * @param collection
     * @param message
     */
    public static void isEmpty(Collection<?> collection, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101028"),message);
        }
    }

    /**
     * 判断集合非空
     *
     * @param collection
     * @param message
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (CollectionUtils.isNotEmpty(collection)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101029"),message);
        }
    }

    /**
     * 判断map是否为空
     *
     * @param map
     * @param message
     */
    public static void isEmpty(Map<?, ?> map, String message) {
        if (MapUtils.isEmpty(map)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101030"),message);
        }
    }

    /**
     * 判断map非空
     *
     * @param map
     * @param message
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (MapUtils.isNotEmpty(map)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101031"),message);
        }
    }

    /**
     * 判断数组是否为空
     *
     * @param array
     * @param message
     */
    public static void isEmpty(Object[] array, String message) {
        if (ObjectUtils.isEmpty(array)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101032"),message);
        }
    }

    /**
     * 判断数组非空
     *
     * @param array
     * @param message
     */
    public static void notEmpty(Object[] array, String message) {
        if (!ObjectUtils.isEmpty(array)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101033"),message);
        }
    }

    /**
     * 判断是否存在空
     *
     * @param message
     * @param collections
     */
    public static void isAnyEmpty(String message, Collection<?>... collections) {
        if (ObjectUtils.isEmpty(collections) || Arrays.stream(collections).anyMatch(CollectionUtils::isEmpty)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101034"),message);
        }
    }

    /**
     * 判断是否全部为空
     *
     * @param message
     * @param collections
     */
    public static void isAllEmpty(String message, Collection<?>... collections) {
        if (ObjectUtils.isEmpty(collections) || Arrays.stream(collections).allMatch(CollectionUtils::isEmpty)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101035"),message);
        }
    }

    /**
     * 是否满足条件
     *
     * @param flag
     * @param message
     */
    public static void isTrue(boolean flag, String message) {
        if (flag) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101036"),message);
        }
    }

    /**
     * 判断字符串是否为空
     *
     * @param s
     * @param message
     */
    public static void isBlank(String s, String message) {
        if (StringUtils.isBlank(s)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101037"),message);
        }
    }

    /**
     * 判断字符串是否为空
     *
     * @param s
     * @param message
     */
    public static void isNotBlank(String s, String message) {
        if (StringUtils.isNotBlank(s)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101038"),message);
        }
    }

    /**
     * 判断是否全部为空
     *
     * @param message
     * @param objs
     */
    public static void isAllNull(String message, Object... objs) {
        if (Arrays.stream(objs).allMatch(ObjectUtils::isEmpty)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101039"),message);
        }
    }

    /**
     * 判断是否存在为空的
     *
     * @param message
     * @param objs
     */
    public static void isAnyNull(String message, Object... objs) {
        if (Arrays.stream(objs).anyMatch(ObjectUtils::isEmpty)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101040"),message);
        }
    }


}
