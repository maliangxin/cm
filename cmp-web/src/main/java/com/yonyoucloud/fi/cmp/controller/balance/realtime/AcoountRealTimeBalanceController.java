package com.yonyoucloud.fi.cmp.controller.balance.realtime;

import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.accountbalance.AccountBalanceService;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BankLinkParam;
import com.yonyoucloud.fi.cmp.task.real.RTBalanceService;
import com.yonyoucloud.fi.cmp.task.real.RTHistoryBalanceService;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
public class AcoountRealTimeBalanceController extends BaseController {

    @Autowired
    private RTBalanceService rTBalanceService;
    @Autowired
    private RTHistoryBalanceService rTHistoryBalanceService;

    // 银行账户余额查询
    @Autowired
    private AccountBalanceService accountBalanceService;


    /**
     * 查询企业银行账户实时余额--定时任务
     *@Author shangxd
     *@Description 查询企业银行账户实时余额--定时任务
     *@Date 2019/5/31 11:30   @RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response
     *@Param [params, request, response]
     *@Return void
     **/
    @PostMapping("/query/realTime/accountBalanceTask")
    @Authentication(value = false, readCookie = true)
    public Map<String, Object> queryAccountBalance(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
            if (null == paramMap) {
                paramMap = new HashMap<>();
            }
            paramMap.put("tenantId", tenantId);
            paramMap.put("userId", userId);
            paramMap.put("logId", logId);
            return rTBalanceService.queryAccountBalanceTask(paramMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0017F", "账户实时余额查询错误：") /* "账户实时余额查询错误：" */ + e.getMessage()));
        }
        return null;
    }

    /**
     * 查询企业银行账户历史余额--定时任务
     *@Author shangxd
     *@Description 查询企业银行账户历史余额--定时任务
     *@Date 2019/5/31 11:30   @RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response
     *@Param [params, request, response]
     *@Return void
     **/
    @PostMapping("/query/history/accountHistoryBalanceTask")
    @Authentication(value = false, readCookie = true)
    public Map<String, Object> queryAccountHistoryBalance(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletRequest request, HttpServletResponse response) {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
            if (null == paramMap) {
                paramMap = new CtmJSONObject();
            }
            paramMap.put("tenantId", tenantId);
            paramMap.put("userId", userId);
            paramMap.put("logId", logId);
            //日期参数检查
            TaskUtils.dateCheck(paramMap);
            return rTHistoryBalanceService.queryAccountHistoryBalanceTask(paramMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            //通知任务执行失败
            Map<String, Object> retMap = new HashMap<>();
            retMap.put("status", TaskUtils.TASK_BACK_FAILURE);
            retMap.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0017F", "账户实时余额查询错误：") /* "账户实时余额查询错误：" */ + e.getMessage());
            renderJson(response, CtmJSONObject.toJSONString(retMap));
        }
        return null;
    }

    @PostMapping("/balance/batchQueryAccountBalance")
    public void queryAccountBalanceUnNeedUkeyAPI(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception{
        renderJson(response, CtmJSONObject.toJSONString(accountBalanceService.batchQueryAccountBalance(params)));
    }

    @PostMapping(value = "/transDetail/batchQueryTransDetail")
    public void batchQueryAccountTransactionDetailAPI(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception{
        renderJson(response, CtmJSONObject.toJSONString(accountBalanceService.batchQueryAccountTransactionDetail(params)));
    }
}
