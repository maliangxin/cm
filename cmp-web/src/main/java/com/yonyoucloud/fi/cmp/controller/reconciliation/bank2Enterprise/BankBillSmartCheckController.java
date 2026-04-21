package com.yonyoucloud.fi.cmp.controller.reconciliation.bank2Enterprise;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankBillSmartCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @description: 智能对账调度任务，自动对账功能
 * @author: wanxbo@yonyou.com
 * @date: 2022/9/29 10:45
 */

@Controller
@RequestMapping("/smartcheck")
@Slf4j
public class BankBillSmartCheckController extends BaseController {

    @Resource
    private BankBillSmartCheckService bankBillSmartCheckService;

    /**
     * 智能对账，自动对账任务入口
     */
    @PostMapping("/autocheck")
    public void autoPush(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode params = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId",Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId",Optional.ofNullable(request.getHeader("logId")).orElse(""));

        ExecutorService dataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"BankBillSmartChec-threadpool");
        //执行推单
        dataExecutor.submit(() -> {
            try {
                CtmLockTool.executeInOneServiceLock("BankReconciliationAutomaticTask", 60 * 5L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        //加锁失败
                        //通知任务执行失败
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, params.get("logId").asText(),"执行失败，有正在执行的任务", TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    }
                    //加锁成功
                    if(bankBillSmartCheckService.smartCheck(param)){
                        //通知任务执行成功
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, params.get("logId").asText(),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803CB", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE,  params.get("logId").asText(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001B3", "执行失败") /* "执行失败" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
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

    /**
     * 根据datasource和对应的数据idList查询勾对记录信息
     */
    @PostMapping("queryReconciliationRecordInfo")
    @CMPDiworkPermission({IServicecodeConstant.BANKRECONCILIATION})
    public void queryReconciliationRecordInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(bankBillSmartCheckService.queryReconciliationRecordInfo(param)));
    }
}
