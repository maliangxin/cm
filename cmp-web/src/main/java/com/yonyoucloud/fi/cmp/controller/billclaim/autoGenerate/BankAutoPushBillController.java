package com.yonyoucloud.fi.cmp.controller.billclaim.autoGenerate;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoPushBillService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.vo.AutoTaskCommonVO;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @description: 银行对账单 自动下推资金调度 生单调度任务
 * @author: wanxbo@yonyou.com
 * @date: 2022/7/7 11:18
 */

@Controller
@RequestMapping("/autopushbill")
@Slf4j
public class BankAutoPushBillController extends BaseController {

    @Resource
    private BankAutoPushBillService bankAutoPushBillService;

    @Resource
    private CtmThreadPoolExecutor executorServicePool;

    /**
     * 银行对账单，自动下推资金调度等 生单推送接口
     */
    @PostMapping("/autopush")
    public void autoPush(@RequestBody JsonNode param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode params = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        //CtmJSONObject paramForFays = new CtmJSONObject();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId", Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId", Optional.ofNullable(request.getHeader("logId")).orElse(""));
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        String lockKey = ICmpConstant.AUTO_PUSH;
        CtmLockTool.executeInOneServiceLock(lockKey, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
            if (lockstatus == LockStatus.GETLOCK_FAIL) {
                //加锁失败
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80084", "自动生单任务正在执行中") /* "自动生单任务正在执行中" */, TaskUtils.UPDATE_TASK_LOG_URL);
                return;
            }
            //执行推单
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                try {
                    //接口配置参数day，用来判断交易日期范围
                    List<Date> startAndEndDate = TaskUtils.queryStartAndEndDateByParam(param, "day");
                    bankAutoPushBillService.autoPush(startAndEndDate.get(0),startAndEndDate.get(1),params);
                    //通知任务执行成功
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, params.get("logId").asText(),
                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803CB", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId,
                            InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114",
                                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008F", "执行失败") /* "执行失败" */) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80094", "失败原因：") /* "失败原因：" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                    renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "调度任务银行对账单自动生单任务执行失败：") /* "账户实时余额查询错误：" */ + e.getMessage()));
                }
            });
        });
        ObjectNode result = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        result.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    /**
     * 银行对账单，生单手工确认接口
     */
    @PostMapping("/confirmBill")
    public void confirmBill(@RequestBody JsonNode params, HttpServletResponse response) throws Exception {
        BankReconciliation bankReconciliation = JSONBuilderUtils.jsonToBean(params, BankReconciliation.class);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(bankAutoPushBillService.confirmBill(params))));
    }

    /**
     * 银行对账单，自动生单后自动确认，调度任务使用
     */
    @PostMapping("/autoConfirmBill")
    public void autoConfirmBill(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AutoTaskCommonVO params = new AutoTaskCommonVO();
        //获取租户级自动任务参数
        String tenantid = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        params.setTenantId(tenantid);
        params.setUserId(Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.setLogId(Optional.ofNullable(request.getHeader("logId")).orElse(""));
        //异步执行确认
        ExecutorService executorService = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 2, 2, "autoConfirmBill-threadpool");
        executorService.submit(() -> {
            YmsLock ymsLock = JedisLockUtils.taskLockWithOutTrace(ICmpConstant.AUTO_CONFIRMBILL_TASK_LOCK + "_" + tenantid, 60 * 60);
            if (ymsLock == null) {
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, params.getLogId(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80082", "执行失败，有正在执行的任务") /* "执行失败，有正在执行的任务" */, TaskUtils.UPDATE_TASK_LOG_URL);
                return;
            }
            try {
                params.setYmsLock(ymsLock);
                //加锁成功
                bankAutoPushBillService.autoConfirmBill(params);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                JedisLockUtils.unlockBillWithOutTrace(ymsLock);
                //通知任务执行成功
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, params.getLogId(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B3", "执行失败") /* "执行失败" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }
            }
        });
        AutoTaskCommonVO result = new AutoTaskCommonVO();
        result.setAsynchronized(true);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }


    /**
     * 银行对账单，生单手工拒绝接口
     */
    @PostMapping("/refuseBill")
    public void refuseBill(@RequestBody JsonNode params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(bankAutoPushBillService.refuseBill(params))));
    }

}
