package com.yonyoucloud.fi.cmp.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jingjpf
 * @date 2021/5/18 10:03
 */
public class CheckMarxUtils {

    /**
     * Log Forging漏洞校验
     *
     * @param logs
     * @return
     */
    public static String vaildLog(String logs) {
        List<String> list = new ArrayList<String>();
        list.add("%0d");
        list.add("%0a");
        list.add("%0A");
        list.add("%0D");
        list.add("\r");
        list.add("\n");
        String normalize = Normalizer.normalize(logs, Normalizer.Form.NFKC);
        for (String str : list) {
            normalize = normalize.replace(str, "");
        }
        return normalize;
    }
}
