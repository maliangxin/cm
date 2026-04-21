package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author shangxd
 * @date 2023/3/29 16:06
 * @describe
 */
@Slf4j
public class EarlyWaringUtils {

    /**
     * 预警任务异步通知
     */
    public static String PRE_WARNING_UPDATE_TASK_LOG_URL;

    public static final String ACCTBAL_WARNING_TITLE = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005C8", "银行账户余额核对预警") /* "银行账户余额核对预警" */;

    public static final int NO_SEND = 0;//消息发送方式： 0：都不发送
    public static final int FAILE_SEND = 1;//1：失败发送
    public static final int ALL_SEND = 2;//成功失败都发送

    static {
        PRE_WARNING_UPDATE_TASK_LOG_URL = AppContext.getEnvConfig("PRE_WARNING_UPDATE_TASK_LOG_URL");
    }

    public static void preExpireUpdateTaskLog(String content, int status, String logId, int msgSend, CtmJSONArray data, String title, String url) {
        CtmJSONObject map = new CtmJSONObject();
        map.put("id", logId);
        map.put("status", status);
        map.put("content", content);
        map.put("msgSend", msgSend);
        if(data != null && data.size() != 0){
            map.put("data", data.toJavaList(CtmJSONObject.class));
        }
        map.put("title", title);
        try {
            CtmJSONObject jsonObject = RestTemplateUtils.doPostByJSON(url, map);
            log.info("预警异步通知响应成功:" + jsonObject.toString());
        } catch (Exception e) {
            log.error("预警异步通知响应异常" + e.getMessage());
        }
    }
    public static void preExpireUpdateTaskLogNew(String content, int status, String logId, int msgSend, CtmJSONArray data, String title, String url) {
        CtmJSONObject map = new CtmJSONObject();
        map.put("id", logId);
        map.put("status", status);
        map.put("content", content);
        map.put("msgSend", msgSend);
        if(data != null && data.size() != 0){
            map.put("data", data.toJavaList(CtmJSONObject.class));
        }
        map.put("title", title);
        try {
            CtmJSONObject jsonObject = RestTemplateUtils.doPostByJSON(url, map);
            log.info("预警异步通知响应成功:" + jsonObject.toString());
        } catch (Exception e) {
            log.error("预警异步通知响应异常" + e.getMessage());
        }
    }


}
