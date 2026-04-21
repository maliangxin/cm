package com.yonyoucloud.fi.cmp.controller.balance.history;

import com.google.common.collect.Maps;
import com.yonyou.cloud.auth.sdk.service.utils.PropertyUtil;
import com.yonyou.iuap.bd.customerdoc.utils.AuthHttpClientUtils;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountBalanceCheckService;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceAutoCheckService;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceService;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.BatchLockGetKeysUtils;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.Json;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 银行账户历史余额
 *
 * @author wangyaoo@yonyou.com
 * @version 1.0
 */
@Controller
@RequestMapping("/accountBalance")
@Authentication(value = false, readCookie = true)
@Slf4j
public class AccountHistoryBalanceController extends BaseController {
    @Autowired
    AccountHistoryBalanceService accountHistoryBalanceService;
    @Autowired
    AccountHistoryBalanceAutoCheckService accountHistoryBalanceAutoCheckService;
    @Autowired
    AuthHttpClientUtils authHttpClientUtils;

    @Autowired
    private AccountBalanceCheckService accountBalanceCheckService;
    private static final String TASK_BALANCE_CHECK_LOCK = "TASK_BALANCE_CHECK_LOCK";
    // 余额弥补调度任务
    private static final String TASK_BALANCE_SUPPLEMENT_LOCK = "TASK_BALANCE_SUPPLEMENT_LOCK";

    /**
     * 新增历史余额
     *
     * @param bill
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/save")
    public void saveAccountBalance(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入AccountBalanceController类的{}", "/save");
        CtmJSONObject param = getJsonObject(bill);
        Json json = new Json(CtmJSONObject.toJSONString(param.getJSONObject("data")));
        AccountRealtimeBalance accountRealtimeBalance = Objectlizer.decodeObj(json, AccountRealtimeBalance.ENTITY_NAME);
        CtmJSONObject result = accountHistoryBalanceService.saveAccountBalance(accountRealtimeBalance);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 账户历史余额手动拉取
     *
     * @param bill
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/syncHistory")
    public void syncHistoryAccountBalance(@RequestBody CtmJSONObject bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        accountHistoryBalanceService.syncHistoryAccountBalance(bill);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * 历史余额 余额确认
     *
     * @param obj
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/confirm")
    public void confirmAccountBalance(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入AccountBalanceController类的{}", "/confirm");
        CtmJSONArray row = obj.getJSONArray("data");
        if (null == row || row.size() < 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105070"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(AccountRealtimeBalance.ENTITY_NAME, row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                Json json = new Json(lockRowData.toString());
                List<AccountRealtimeBalance> billList = Objectlizer.decode(json, AccountRealtimeBalance.ENTITY_NAME);
                CtmJSONObject result = accountHistoryBalanceService.confirmAccountBalance(billList);
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(AccountRealtimeBalance.ENTITY_NAME, lockResult.getJSONArray("lockRowData"));
            }
        }
    }

    /**
     * 历史余额 取消确认
     *
     * @param obj
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/cancelconfirm")
    public void cancelConfirmAccountBalance(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入AccountBalanceController类的{}", "/cancelConfirm");
        CtmJSONArray row = obj.getJSONArray("data");
        if (null == row || row.size() < 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105071"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        CtmJSONObject lockResult = CacheUtils.lockBill(AccountRealtimeBalance.ENTITY_NAME, row);
        if (!lockResult.getBoolean("dealSucceed")) {
            renderJson(response, ResultMessage.error(lockResult.getString("message")));
        } else {
            try {
                CtmJSONArray hasLockRowData = lockResult.getJSONArray("hasLockRowData");
                CtmJSONArray lockRowData = lockResult.getJSONArray("lockRowData");
                Json json = new Json(lockRowData.toString());
                List<AccountRealtimeBalance> billList = Objectlizer.decode(json, AccountRealtimeBalance.ENTITY_NAME);
                CtmJSONObject result = accountHistoryBalanceService.cancelConfirmAccountBalance(billList);
                renderJson(response, ResultMessage.data(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                renderJson(response, ResultMessage.error(e.getMessage()));
            } finally {
                CacheUtils.unlockBill(AccountRealtimeBalance.ENTITY_NAME, lockResult.getJSONArray("lockRowData"));
            }
        }
    }

    /**
     * 历史余额弥补调度任务
     *
     * @param request
     * @throws Exception
     */
    @RequestMapping("/taskBalanceCheck")
    @ResponseBody
    public Object taskBalanceCheck(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> paramMap) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        Map<String, Object> retMap = Maps.newHashMap();
        //异步执行业务逻辑 执行成功status传1 失败传0；
        try {
            //日期参数检查
            int preDays = TaskUtils.dateCheckForBalanceCheck(paramMap);
            if (preDays == 0) {
                paramMap.put("useStartDate", true);
            } else {
                paramMap.put("useStartDate", false);
            }

            CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
            ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                try {
                    paramMap.put("logId", logId);
                    log.error("======taskBalanceCheck==========余额弥补开始=================");
                    accountHistoryBalanceAutoCheckService.checkAccountBalance(paramMap, logId);
                    log.error("======taskBalanceCheck==========余额弥补结束=================");
                    //内部线程真正执行成功后，再回写执行成功
                    //TaskUtils.updateTaskLog(TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
//                int status = 1;
//                callbackPlatform(logId, status,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BB","执行成功") /* "执行成功" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BB","执行成功") /* "执行成功" */);
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                    //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101119"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800EE", "余额弥补调度任务执行失败") /* "余额弥补调度任务执行失败" */,e);
                }
            });

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101119"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800EE", "余额弥补调度任务执行失败") /* "余额弥补调度任务执行失败" */,e);
        }
        retMap.put("asynchronized", "true");
        return retMap;
    }

    @RequestMapping("/balanceCheck")
    @ResponseBody
    public Object balanceCheck(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> paramMap) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        Map<String, Object> retMap = Maps.newHashMap();
        //异步执行业务逻辑 执行成功status传1 失败传0；
        try {
            //日期参数检查
            int preDays = TaskUtils.dateCheckForBalanceCheck(paramMap);
            if (preDays == 0) {
                paramMap.put("useStartDate", true);
            } else {
                paramMap.put("useStartDate", false);
            }
            //异步执行业务逻辑 执行成功status传1 失败传0；
            CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
            ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                try {
                    CtmLockTool.executeInOneServiceLock(TASK_BALANCE_CHECK_LOCK, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockStatus) -> {
                        if (lockStatus == LockStatus.GETLOCK_FAIL) {
                            //加锁失败
                            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！"), TaskUtils.UPDATE_TASK_LOG_URL);
                            return;
                        }
                        accountBalanceCheckService.balanceCheck(paramMap);
//                    int status = 1;
//                    callbackPlatform(logId, status,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BB","执行成功") /* "执行成功" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BB","执行成功") /* "执行成功" */);
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00275", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                    });
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
//                int status = 1;
//                callbackPlatform(logId, status,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BB","执行成功") /* "执行成功" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BB","执行成功") /* "执行成功" */);
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId.toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
//                int status = 1;
//                callbackPlatform(logId, status,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BB","执行成功") /* "执行成功" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041801BB","执行成功") /* "执行成功" */);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId.toString(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00276", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
        }
        retMap.put("asynchronized", "true");
        return retMap;
    }

    /**
     * 历史数据升级--银行类别（账户历史余额，银行对账单）
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/updateHistory")
    public void updateHistory(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入AccountBalanceController类的{}", "/updateHistory");
        String schema = request.getParameter("schema");
        CtmJSONObject result = accountHistoryBalanceService.updateHistory(schema);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 历史数据升级--银行类别（银行账户交易回单，银行账户交易明细）
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/updateHistoryV2")
    public void updateHistoryV2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("进入AccountBalanceController类的{}", "/updateHistoryV2");
        String schema = request.getParameter("schema");
        CtmJSONObject result = accountHistoryBalanceService.updateHistoryV2(schema);
        renderJson(response, ResultMessage.data(result));
    }


    /*private void callbackPlatform(String logId, int status, String msg, String content) {
        // 回调调度任务
        CtmJSONObject param = new CtmJSONObject();
        param.put("status", status);
        param.put("id", logId);
        param.put("msg", msg);
        param.put("content", content);
        String url = PropertyUtil.getPropertyByKey("domain.iuap-apcom-coderule") + "/warning/warning/async/updateTaskLog";
        authHttpClientUtils.execPost(url, null, null, param.toString());
    }*/

    @NotNull
    private CtmJSONObject getJsonObject(@RequestBody BillDataDto bill) {
        Map<String, Object> map = new HashMap<>();
        map.put("billnum", bill.getBillnum());
        map.put("data", bill.getData());
        return new CtmJSONObject(map);
    }


}
