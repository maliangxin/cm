package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyService;
import com.yonyoucloud.fi.cmp.util.StringUtils;
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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2024/3/13 9:50
 */
@Controller
@RequestMapping("/bankautoclassify")
@Slf4j
public class BankAutoClassifyController extends BaseController {

    @Resource
    private BillSmartClassifyService billSmartClassifyService;

    /**
     * 银行对账单，自动辨识对方单位为空的调度任务
     */
    @PostMapping("/autoclassify")
    public void autoPush(@RequestBody JsonNode param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode params = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId", Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId", Optional.ofNullable(request.getHeader("logId")).orElse(""));
        //接口配置参数day，用来判断交易日期范围
        params.put("pagesize", param.get("pagesize") == null ? 1000 : param.get("pagesize").asInt());
        params.put("oppositetype", param.get("oppositetype") == null ? null : param.get("oppositetype").asText());
        params.put("accentity", param.get("accentity") == null ? null : param.get("accentity").asText());
        //公有云默认传空字符串，会导致不填参数时，默认为0；这里写死，默认为3天
        params.put("dataRange", param.get("dataRange").asText());

        if (param.get(TaskUtils.BALANCE_CHECK_START_DATE) != null){
            params.put(TaskUtils.TASK_START_DATE,param.get(TaskUtils.BALANCE_CHECK_START_DATE).asText());
        }
        if (param.get(TaskUtils.BALANCE_CHECK_END_DATE) != null){
            params.put(TaskUtils.TASK_END_DATE,param.get(TaskUtils.BALANCE_CHECK_END_DATE).asText());
        }
        //执行推单
        ExecutorService dataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"BankAutoClassify-threadpool");
        dataExecutor.submit(() -> {
            try {
                CtmLockTool.executeInOneServiceLock("BANK_AUTO_CLASSIFY_TASK_LOCK", 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        //加锁失败
                        //通知任务执行失败
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, params.get("logId").asText(),"执行失败，有正在执行的任务", TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    }
                    //加锁成功
                    if(billSmartClassifyService.autoClassify(params)){
                        //通知任务执行成功
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, params.get("logId").asText(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803CB", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE,  params.get("logId").asText(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B3", "执行失败") /* "执行失败" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                renderJson(response, ResultMessage.error(InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80098", "账户交易流水自动辨识（智能认领）：") /* "账户交易流水自动辨识（智能认领）：" */) + e.getMessage()));

            }finally {
                if(dataExecutor != null){
                    dataExecutor.shutdown();
                }
            }
        });
        ObjectNode result = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        result.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

}
