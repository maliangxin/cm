//package com.yonyoucloud.fi.cmp.util;
//
//import java.util.regex.Pattern;
//
//public final class RegexUtil {
//
//    private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[<>\"'%{}@#&$?;°]");
//    private static final Pattern CHINESE_SYMBOLS = Pattern.compile("[，。；：“”‘’《》【】、！？]");
//    private static final Pattern CHINESE_CHARACTERS = Pattern.compile("[\u4E00-\u9FA5]");
//
//    public static boolean containsChinese(String str) {
//        return CHINESE_CHARACTERS.matcher(str).find();
//    }
//
//
//    public static boolean containsIllegalChars(String input) {
//        return ILLEGAL_CHAR_PATTERN.matcher(input).find();
//    }
//
//    public static boolean containsChineseSymbols(String input) {
//        return CHINESE_SYMBOLS.matcher(input).find();
//    }
//
//    public static boolean validateChineseAndIllegalChars(String input) {
//        if (StringUtils.isEmpty(input)) {
//            return false;
//        }
//        return containsChinese(input) || containsIllegalChars(input);
//    }
//
//    public static boolean validateChineseAndChineseSymbols(String input) {
//        if (StringUtils.isEmpty(input)) {
//            return false;
//        }
//        return containsChinese(input) || containsChineseSymbols(input);
//    }
//
//}
