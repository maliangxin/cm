package com.yonyoucloud.fi.cmp.controller.billclaim.refund;


import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankManualRefundService;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.refund.BankReconciliationReFundCheckTaskService;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.refund.BankReconciliationReFundService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * 到账认领V2 - 退票检测任务
 */
@Controller
@Slf4j
@RequestMapping("/check")
public class CheckReFundController extends BaseController {

    @Autowired
    BankReconciliationReFundCheckTaskService bankReconciliationReFundCheckTaskService;

    @Autowired
    BankReconciliationReFundService bankReconciliationReFundService;

    @Resource
    private BankManualRefundService bankManualRefundService;

    /**
     * 退票检测
     * 按规则匹配到对账单后赋予疑似退票标识
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("/reFund")
    public void checkReFund(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<String, Object>();
        String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantId", tenantId);
        map.put("userId", userId);
        map.put("logId", logId);
        //执行推单
        ExecutorService executorService = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1, 2, 200, "BankAutoPushBill-threadpool");
        executorService.submit(() -> {
            YmsLock ymsLock = null;
            try {
                ymsLock = JedisLockUtils.lockBillWithOutTraceByTime("refundcheck" + InvocationInfoProxy.getTenantid(), 60 * 10);
                if (ymsLock != null) {
                    bankReconciliationReFundCheckTaskService.bankReconciliationCheckReFund(map);
                } else {
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, map.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "退票辨识调度任务正在执行中") /* "退票辨识调度任务正在执行中" */, TaskUtils.UPDATE_TASK_LOG_URL);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, paramMap.get("logId").toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418017E", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }
                if (ymsLock != null) {
                    ymsLock.unLock();
                }
            }
        });
        //通知任务执行成功
        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, map.get("logId").toString(),
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803CB", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
        result.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    /**
     * 确认退票
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("confirm")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION_CHECKREFUND)
    public void confirmRefund(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List ids = paramMap.get("ids") == null ? null : (List) paramMap.get("ids");
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(bankReconciliationReFundService.confirmRefund(ids))));
    }

    /**
     * 拒绝退票
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("refuse")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION_CHECKREFUND)
    public void refuseRefund(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List ids = paramMap.get("ids") == null ? null : (List) paramMap.get("ids");
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(bankReconciliationReFundService.refuseRefund(ids))));
    }

    /**
     * 手工退票相关
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("manualrefund")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public void manualRefund(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(bankManualRefundService.manualRefund(paramMap))));
    }

    /**
     * 运营工具-清除银行流水退票状态
     * 不校验流水是否关联了下游单据，只要根据条件查询出来时已退票的流水，就清除对应退票状态。
     * 若流水已经关联资金结算明细，只清空现金银行流水退票状态是不够的，需要让结算同步清理*
     * @param params
     * @param response
     * @throws Exception
     */
    @PostMapping("clearRefundStatus")
    public void clearRefundStatus(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        bankManualRefundService.clearRefundStatusById(params);
        renderJson(response, ResultMessage.success("clearRefundStatus success!"));
    }
}
