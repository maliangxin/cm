package com.yonyoucloud.fi.cmp.util.Constant;

/**
 * @author: liaojbo
 * @Date: 2026年01月22日 16:34
 * @Description: accept为外部线程池，不带accept的为内部线程池；manu为手动按钮操作，task为调度任务自动运行
 */
public class ThreadConstant {

    //accept为外部线程池，不带accept的为内部线程池；manu为手动按钮操作，task为调度任务自动运行
    public static final String CMP_BANKDEAL_PULL_TASK_ACCEPT = "cmp_bankdeal_pull_task_accept";

    public static final String CMP_REALTIMEBAL_PULL_MANU_ACCEPT = "cmp_realtimebal_pull_manu_accept";
    //public static final String CMP_REALTIMEBAL_PULL_MANU = "cmp_realtimebal_pull_manu";
    public static final String CMP_REALTIMEBAL_PULL_TASK_ACCEPT = "cmp_realtimebal_pull_task_accept";

    public static final String CMP_HISTIMEBAL_PULL_MANU_ACCEPT = "cmp_histimebal_pull_manu_accept";

    public static final String CMP_BANKRECEIPT_PULL_TASK_ACCEPT = "cmp_bankreceipt_pull_task_accept";

    public static final String SHARE = "cmp_share";
    public static final String CMP_BANKRECEIPT_BATCHDOWNLOAD_SENDEVENT = "cmp_bankreceipt_batchdownload_sendevent";

    public static final String THREAD_PARAM_POSTFIX = ".thread.param";
    public static final String THREAD_YMS_PARAM_DEFAULT_VALUE = "16,32,1000";
    public static final String CMP_BANKRECEIPT_BATCHDOWNLOAD_SENDEVENT_THREAD_YMS_PARAM_DEFAULT_VALUE = "8,16,100";
    public static final String SHARE_THREAD_YMS_PARAM_DEFAULT_VALUE = "16,32,1000";
    public static final String MIN_YMS_PARAM_DEFAULT_VALUE = "8,16,1000";
}
