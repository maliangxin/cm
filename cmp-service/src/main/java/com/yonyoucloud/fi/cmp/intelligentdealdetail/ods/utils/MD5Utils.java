package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils;
import java.security.MessageDigest;
/**
 * @Author guoyangy
 * @Date 2024/6/22 13:48
 * @Description MD5工具类
 * @Version 1.0
 */
public class MD5Utils {
    private static final String hexDigIts[] = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
    /**
     * MD5加密
     * @param origin 字符
     * @return
     */
    public static String MD5Encode(String origin){
        String resultString = null;
        try{
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToHexString(md.digest(resultString.getBytes("UTF-8")));
        }catch (Exception e){
        }
        return resultString;
    }
    public static String byteArrayToHexString(byte b[]){
        StringBuffer resultSb = new StringBuffer();
        for(int i = 0; i < b.length; i++){
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }
    public static String byteToHexString(byte b){
        int n = b;
        if(n < 0){
            n += 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigIts[d1] + hexDigIts[d2];
    }
}