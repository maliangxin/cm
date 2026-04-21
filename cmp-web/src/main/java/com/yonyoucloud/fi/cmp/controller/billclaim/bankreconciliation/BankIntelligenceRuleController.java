package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;


import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.bankrecrule.ruleengine.BankIntelligenceRuleService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/bankIntelligenceRule")
@Slf4j
@Authentication(value = false, readCookie = true)
public class BankIntelligenceRuleController extends BaseController {
    @Resource
    private BankIntelligenceRuleService bankIntelligenceRuleService;

    @Resource
    private CtmThreadPoolExecutor executorServicePool;

    /**
     * 银行对账单辨识规则执行
     *
     * @param request
     * @throws Exception
     */
    @RequestMapping("/identification")
    public void executeIdentificationTask(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletRequest request,
                                          HttpServletResponse response) throws Exception {
        String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        if (paramMap == null) {
            paramMap = new CtmJSONObject();
        }
        paramMap.put("tenantId", tenantId);
        paramMap.put("userId", userId);
        paramMap.put("logId", logId);
        paramMap.put("token", InvocationInfoProxy.getYhtAccessToken());
        // 执行辨识规则同时给银行对账单辨识到的字段赋值
        try {
            CtmJSONObject jsonObject = bankIntelligenceRuleService.executeIdentificationRule(paramMap);
            renderJson(response, CtmJSONObject.toJSONString(jsonObject));
        }catch (Exception e){
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008F", "执行失败") /* "执行失败" */) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008E", "，失败原因：") /* "，失败原因：" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);

            renderJson(response, ResultMessage.error(InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80092", "账户交易流水自动辨识（智能认领）：") /* "账户交易流水自动辨识（智能认领）：" */) + e.getMessage()));
        }
    }

    /**
     * 银行对账单生单规则执行
     *
     * @param request
     * @throws Exception
     */
    @RequestMapping("/generateBill")
    public void executeGenerateBillTask(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletRequest request,
                                        HttpServletResponse response) {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
            String lockKey = ICmpConstant.AUTOBILL_AND_PUBLISH_TASK;
            //执行推单
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                try {
                    CtmLockTool.executeInOneServiceLock(lockKey, 60 * 30L, TimeUnit.SECONDS, (int lockstatus) -> {
                        try {
                            final CtmJSONObject paramMaps = paramMap;
                            paramMaps.put("tenantId", tenantId);
                            paramMaps.put("userId", userId);
                            paramMaps.put("logId", logId);
                            paramMaps.put("token", InvocationInfoProxy.getYhtAccessToken());
                            if (lockstatus == LockStatus.GETLOCK_FAIL) {
                                //加锁失败
                                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008F", "执行失败") /* "执行失败" */) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80091", "，当前任务正在执行中！") /* "，当前任务正在执行中！" */, TaskUtils.UPDATE_TASK_LOG_URL);
                                return;
                            }
                            // 通过银行回单关联银行交易明细给回单关联状态赋值
                            String errorMsg = bankIntelligenceRuleService.executeGenerateBillRule(paramMaps);
                            if (StringUtils.isNotEmpty(errorMsg) && !"[, ]".equals(errorMsg)) {
                                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId,
                                        InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114",
                                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008F", "执行失败") /* "执行失败" */) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80094", "失败原因：") /* "失败原因：" */ + errorMsg, TaskUtils.UPDATE_TASK_LOG_URL);
                            }
                            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00113", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80093", "执行成功") /* "执行成功" */) /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId,
                                    InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114",
                                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008F", "执行失败") /* "执行失败" */) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80094", "失败原因：") /* "失败原因：" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                            renderJson(response, ResultMessage.error(InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80095", "调度任务银行对账单自动生单任务执行失败：") /* "调度任务银行对账单自动生单任务执行失败：" */) + e.getMessage()));
                        }
                    });
                } catch (Exception e) {
                    renderJson(response, ResultMessage.error(InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80095", "调度任务银行对账单自动生单任务执行失败：") /* "调度任务银行对账单自动生单任务执行失败：" */) + e.getMessage()));
                }
            });
            //通知调度任务为异步执行
            CtmJSONObject result = new CtmJSONObject();
            result.put("asynchronized", true);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008F", "执行失败") /* "执行失败" */) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008E", "，失败原因：") /* "，失败原因：" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            renderJson(response, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80090", "调度任务银行对账单自动生单任务执行失败") /* "调度任务银行对账单自动生单任务执行失败" */ + e.getMessage());
        }
    }

    /**
     * 银行对账单提前入账规则执行
     *
     * @param request
     * @throws Exception
     */
    @RequestMapping("/advanceEnterAccount")
    public void executeAdvanceEnterTask(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletRequest request,
                                        HttpServletResponse response) {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
            String lockKey = ICmpConstant.AUTOBILL_AND_PUBLISH_TASK;
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                try {
                    CtmLockTool.executeInOneServiceLock(lockKey, 60 * 30L, TimeUnit.SECONDS, (int lockstatus) -> {
                        try {
                            final CtmJSONObject paramMaps = paramMap;
                            paramMaps.put("tenantId", tenantId);
                            paramMaps.put("userId", userId);
                            paramMaps.put("logId", logId);
                            paramMaps.put("token", InvocationInfoProxy.getYhtAccessToken());
                            if (lockstatus == LockStatus.GETLOCK_FAIL) {
                                //加锁失败
                                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008F", "执行失败") /* "执行失败" */) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80091", "，当前任务正在执行中！") /* "，当前任务正在执行中！" */, TaskUtils.UPDATE_TASK_LOG_URL);
                                return;
                            }
                            // 通过银行回单关联银行交易明细给回单关联状态赋值
                            String errorMsg = bankIntelligenceRuleService.executeAdvanceEnterRule(paramMaps);
                            if (StringUtils.isNotEmpty(errorMsg) && !"[, ]".equals(errorMsg) && !"[]".equals(errorMsg)) {
                                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId,
                                        InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114",
                                                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008F", "执行失败") /* "执行失败" */) /* "执行失败" */ + errorMsg, TaskUtils.UPDATE_TASK_LOG_URL);
                            }
                            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00113", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80093", "执行成功") /* "执行成功" */) /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId,
                                    InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00114",
                                            com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008F", "执行失败") /* "执行失败" */) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80094", "失败原因：") /* "失败原因：" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                            renderJson(response, ResultMessage.error(InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80095", "调度任务银行对账单自动生单任务执行失败：") /* "调度任务银行对账单自动生单任务执行失败：" */) + e.getMessage()));
                        }
                    });
                } catch (Exception e) {
                    renderJson(response, ResultMessage.error(InternationalUtils.getMessageWithDefault("", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80095", "调度任务银行对账单自动生单任务执行失败：") /* "调度任务银行对账单自动生单任务执行失败：" */) + e.getMessage()));
                }
            });
            //通知调度任务为异步执行
            CtmJSONObject result = new CtmJSONObject();
            result.put("asynchronized", true);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80096", "调度任务银行对账单提前入账自动任务执行失败") /* "调度任务银行对账单提前入账自动任务执行失败" */ + e.getMessage());
        }
    }
}


