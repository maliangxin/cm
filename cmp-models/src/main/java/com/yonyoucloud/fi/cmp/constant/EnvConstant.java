package com.yonyoucloud.fi.cmp.constant;


import cn.hutool.core.date.DateUtil;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

/**
 * @author: liaojbo
 * @Date: 2025年03月14日 14:35
 * @Description:不同环境配置不同
 */
public class EnvConstant {

    //内网网址
    public static String DOMAIN_URL = AppContext.getEnvConfig("domain.url", "");

    //"false"
    public static final String FALSE = Boolean.FALSE.toString();
    //"true"
    public static final String TRUE = Boolean.TRUE.toString();

    //基础全局公共配置下的yms变量，是否专属化部署。查不到时，默认专属化部署
    public static boolean ISPREMISES = Boolean.parseBoolean(AppContext.getEnvConfig("isPremises", TRUE));


    //银企联日志：默认专属化开启，公有云不开启
    public static Boolean saveYQLBusinessLogc =  ISPREMISES ? Boolean.parseBoolean(AppContext.getEnvConfig("cmp.saveYQLBusinessLog",TRUE))
            :Boolean.parseBoolean(AppContext.getEnvConfig("cmp.saveYQLBusinessLog",FALSE));

    //银企联测试接口：默认不开启
    public static boolean YQLTestData = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.YQLTestData",FALSE));

    //银企联回单唯一码验重规则开启日期：默认不开启，实施确认银企联开通回单唯一码后，再设置，格式为"yyyy-MM-dd"
    public static Date RECEIPT_UNIQUE_NO_OPEN_DATE;

    ////回单打印时，pdf是否转为图片
    //public static boolean PDF_TO_IMAGE = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.pdf_to_image", TRUE));


    static {
        try {
            RECEIPT_UNIQUE_NO_OPEN_DATE = DateUtil.parseDate(AppContext.getEnvConfig("cmp.receipt.unique_no_open_date", "2026-02-01"));
        } catch (Exception e) {
            RECEIPT_UNIQUE_NO_OPEN_DATE = null;
        }
    }

    //公有云客户需要此功能，私有云客户不需要此功能[通过审批加客商档案，且查询时数据量大，效率低]
    public static boolean syncMerchant = ISPREMISES ? Boolean.parseBoolean(AppContext.getEnvConfig("cmp.syncMerchant.switch",FALSE))
            :Boolean.parseBoolean(AppContext.getEnvConfig("cmp.syncMerchant.switch",TRUE));

    //打印回单时，每个线程处理的数量。超时的话，需要调小，但是会开更多线程
    public static  int CMP_PRINT_RECEIPTTOIMAGETASK_BATCH_COUNT = Integer.parseInt(AppContext.getEnvConfig("cmp.print.receiptToImageTask.batchCount", "10"));
    public static  int CMP_PRINT_RECEIPT_MAXNUM = Integer.parseInt(AppContext.getEnvConfig("cmp.print.receipt.maxnum", "100"));

    public static  String CMP_PRINTRECEIPTTOIMAGE_TASK_DEFAULT_VALUE = AppContext.getEnvConfig( "cmp_printreceipttoimage_task_default_value", "20,32,1000");


}
