package com.yonyoucloud.fi.cmp.util;

import org.apache.commons.codec.digest.DigestUtils;


public class SHA512Util {

    /**
     * SHA512方法
     *
     * @param text 明文
     * @return 密文
     * @throws Exception
     */
    public static String getSHA512Str(String text) throws Exception {
        //加密后的字符串
        String encodeStr= DigestUtils.sha512Hex(text);
        return encodeStr;
    }
}

