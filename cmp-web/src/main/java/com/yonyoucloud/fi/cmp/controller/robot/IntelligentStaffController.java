package com.yonyoucloud.fi.cmp.controller.robot;


import com.google.common.collect.Maps;
import com.yonyou.iuap.basedoc.social.util.JacksonUtils;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyou.ypd.bizflow.utils.JsonUtil;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountBalanceCheckService;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceAutoCheckService;
import com.yonyoucloud.fi.cmp.accounthistorybalance.AccountHistoryBalanceService;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fcdsusesetting.service.IFcdsUseSettingInnerService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CheckRuleCommonUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate.impl.BankDealDetailCompensate;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.openapi.service.OpenApiExternalService;
import com.yonyoucloud.fi.cmp.task.real.RTBalanceService;
import com.yonyoucloud.fi.cmp.task.real.RTHistoryBalanceService;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author: lichaor
 * @Date: 2026/03/05 10:22
 */
@RestController
@RequestMapping("/intelligent")
@Slf4j
@Lazy
public class IntelligentStaffController extends BaseController {

    @Autowired
    private BankReceiptService bankReceiptService;
    @Resource
    private BankIdentifyService bankIdentifyService;
    @Resource
    private IFcdsUseSettingInnerService fcdsUseSettingInnerService;
    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    @Autowired
    BankDealDetailCompensate bankDealDetailCompensate;
    @Autowired
    private OpenApiExternalService openApiExternalService;

    @Autowired
    private RTBalanceService rTBalanceService;
    @Autowired
    private RTHistoryBalanceService rTHistoryBalanceService;
    @Autowired
    AccountHistoryBalanceService accountHistoryBalanceService;
    @Autowired
    AccountHistoryBalanceAutoCheckService accountHistoryBalanceAutoCheckService;
    @Autowired
    private AccountBalanceCheckService accountBalanceCheckService;
    private static final String TASK_BALANCE_CHECK_LOCK = "TASK_BALANCE_CHECK_LOCK";

    /**
     * 数智员工 CM01_自动下载银行流水
     *
     * @param
     * @param request
     * @param response
     */
    @RequestMapping("/bill/AllBankTradeDetailElectron/list")
    public void BankTradeDetailElectronList(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                            HttpServletResponse response) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        Map<String,String> ipaParams = setIpaParams(request);
        try {
            log.error("bill AllBankTradeDetailElectron list start ==================：" + param.toString());
            if (param == null) {
                param = new CtmJSONObject();
            }
            boolean needRestore = false;
            if (param.get("startDate") == null || param.get("endDate") == null) {
                Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String today = dateFormat.format(now);
                param.put("startDate", today);//
                param.put("endDate", today);//
                needRestore = true;
            }
            adaptParam(param);
            param.put("logId", logId);
            param.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
            param.put("ipaParams",ipaParams);
            Map<String, Object> currentResult = openApiExternalService.queryBankTradeDetailElectronList(param, false);

            log.error("bill HistoryBankTradeDetailElectron list start ==================：" + param.toString());
            if (param == null) {
                param = new CtmJSONObject();
            } else {
                //任务参数校验
                String daysinadvance = String.valueOf(Optional.ofNullable(param.get("daysinadvance")).orElse(""));
                if(daysinadvance.equals("0")){
                    daysinadvance = null;
                }
                if(needRestore){
                    param.put("startDate", null);//
                    param.put("endDate", null);//
                }
                param.put("daysinadvance", daysinadvance);
                TaskUtils.dateCheck(param);
            }
            param.put("logId", logId);
            param.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
            Map<String, Object> historyResult = openApiExternalService.queryBankTradeDetailElectronList(param, true);

            Map<String, Map> result = new HashMap<>();
            result.put("currentResult", currentResult);
            result.put("historyResult", historyResult);

            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            log.error(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FE", "执行失败") /* "执行失败" */ + "[Failure Reason]" + e.getMessage());
        }
    }

    /**
     * 数智员工 CM02_流水自动处理
     *
     * @param
     * @param request
     * @param response
     */
    @RequestMapping("/bankDealDetail/histpull")
    public void bankDealDetailHistoryPull(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        Map<String,String> ipaParams = setIpaParams(request);
        param.put("ipaParams",ipaParams);

        log.error("bankDealDetailPull start ==================：" + param.toString());
        param.put("logId", logId);
        param.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
        executorServicePool.getThreadPoolExecutor().submit(() -> {
            //校验简强开关
            try {
                checkSmartFlowSwitch();
                CheckRuleCommonUtils.checkBankReconciliationIdentifyType(bankIdentifyService,fcdsUseSettingInnerService);
            }catch (Exception e){
                log.error("账户交易流水查询辨识失败，失败原因："+e.getMessage(),e);
                TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F6BA8605E00016", "账户交易流水查询辨识失败，失败原因：") /* "账户交易流水查询辨识失败，失败原因：" */+e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                throw new com.yonyoucloud.fi.cmp.common.CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8007A", "账户交易流水查询辨识失败，失败原因：初始化数据异常：") /* "账户交易流水查询辨识失败，失败原因：初始化数据异常：" */+e.getMessage());
            }
            try {
                HashMap<String, String> querydate = TaskUtils.queryDateProcess(param, "yyyy-MM-dd");
                Date startDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_START_DATE), DateUtils.DATE_PATTERN);
                Date endDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_END_DATE), DateUtils.DATE_PATTERN);
                if (startDate == null || endDate == null) {
                    throw new com.yonyou.yonbip.ctm.error.CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80083", "执行的调度任务配置的日期获取不对，请检查！") /* "执行的调度任务配置的日期获取不对，请检查！" */);
                }
                bankDealDetailCompensate.compensateInContext(startDate, endDate, param);
                TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F6BA8605E00014", "流水自动处理任务成功") /* "流水自动处理任务成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
            } catch (Exception e) {
                log.error("流水自动处理任务失败，失败原因：" + e.getMessage(), e);
                TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F6BA8605E00015", "流水自动处理任务失败，失败原因：") /* "流水自动处理任务失败，失败原因：" */ + e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            }
        });
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(retMap));
    }

    /**
     * 数智员工 CM03_自动下载余额
     **/
    @RequestMapping("/query/realTimeAndHistory/accountBalanceTask")
    @ResponseBody
    public void queryAccountBalance(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletRequest request, HttpServletResponse response) {
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
            Map<String,String> ipaParams = setIpaParams(request);
            paramMap.put("ipaParams",ipaParams);
            if (paramMap.get("corepoolsize") == null || (paramMap.get("corepoolsize") != null && paramMap.get("corepoolsize").toString().equals("0"))) {
                paramMap.put("corepoolsize", 1);
            }
            Map<String, Object> relatimeResult = rTBalanceService.queryAccountBalanceTask(paramMap);

            String daysinadvance = String.valueOf(Optional.ofNullable(paramMap.get("daysinadvance")).orElse(""));
            if(daysinadvance.equals("0")){
                daysinadvance = null;
            }
            paramMap.put("daysinadvance", daysinadvance);
            //日期参数检查
            TaskUtils.dateCheck(paramMap);
            Map<String, Object> historyResult = rTHistoryBalanceService.queryAccountHistoryBalanceTask(paramMap);

            Map<String, Object> result = new HashMap<>();
            result.put("relatimeResult", relatimeResult);
            result.put("historyResult", historyResult);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            renderJson(response, ResultMessage.error(e.getMessage()));
        }
    }

    /**
     * 数智员工 CM04_余额自动计算及检查
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
            //任务参数校验
            String CheckRange = String.valueOf(Optional.ofNullable(paramMap.get("CheckRange")).orElse(""));
            if(CheckRange.equals("0")){
                CheckRange = "";
            }
            paramMap.put("CheckRange", CheckRange);
            Map<String,String> ipaParams = setIpaParams(request);
            paramMap.put("ipaParams",ipaParams);
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

                    log.error("======taskBalanceCheck==========余额检查开始=================");
                    CtmLockTool.executeInOneServiceLock(TASK_BALANCE_CHECK_LOCK, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockStatus) -> {
                        if (lockStatus == LockStatus.GETLOCK_FAIL) {
                            //加锁失败
                            log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！"), TaskUtils.UPDATE_TASK_LOG_URL);
                            return;
                        }
                        accountBalanceCheckService.balanceCheck(paramMap);
                    });
                    log.error("======taskBalanceCheck==========余额检查结束=================");
                    TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,logId,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F6BA8605E00017", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                    log.error(e.getMessage(), e);
                }
            });


            //异步执行业务逻辑 执行成功status传1 失败传0；
           /* ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
                try {
                    CtmLockTool.executeInOneServiceLock(TASK_BALANCE_CHECK_LOCK, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockStatus) -> {
                        if (lockStatus == LockStatus.GETLOCK_FAIL) {
                            //加锁失败
                            log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747", "该数据正在处理，请稍后重试！"), TaskUtils.UPDATE_TASK_LOG_URL);
                            return;
                        }
                        accountBalanceCheckService.balanceCheck(paramMap);
                    });
                    TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_SUCCESS,logId,"执行成功", TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                    log.error(e.getMessage(), e);
                }
            });*/

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog((Map<String,String>)paramMap.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
        }
        retMap.put("asynchronized", "true");
        return retMap;
    }

    /**
     * 数智员工 CM05_自动下载回单
     *
     * @param request
     * @throws Exception
     */
    @RequestMapping("/bill/ReceiptDetail/list")
    public void ReceiptDetailList(@RequestBody CtmJSONObject param,HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            adaptParam(param);
            String daysinadvance = String.valueOf(Optional.ofNullable(param.get("daysinadvance")).orElse(""));
            if(daysinadvance.equals("0")){
                daysinadvance = null;
            }
            param.put("daysinadvance", daysinadvance);
            //任务参数校验
            TaskUtils.dateCheck(param);
            param.put("logId",logId);
            Map<String,String> ipaParams = setIpaParams(request);
            param.put("ipaParams",ipaParams);
            param.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
            Map<String, Object> result = new HashMap<>();
            log.error("CM05问题排查日志："+ JacksonUtils.toJSONString(ipaParams));
            result = openApiExternalService.ReceiptDetailListThread(param);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862FE3C05D00021", "获取电子回单任务错误：") /* "获取电子回单任务错误：" */ + e.getMessage()));
        }
    }

    /**
     * 数智员工 CM06_自动下载回单文件
     *
     * @param request
     * @throws Exception
     */
    @RequestMapping("/bankElectronByReqSeq/task/pdf")
    @Authentication(value = false, readCookie = true)
    public void bankTradeDetailElectronTask(@RequestBody(required = false) CtmJSONObject param, HttpServletRequest request,
                                            HttpServletResponse response) throws Exception {
        log.error("bill BankTradeDetailElectronTask list start ==================：" + param.toString());
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        try {
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
            if(null == param){
                param = new CtmJSONObject();
            }
            adaptParam(param);
            param.put("tenantId",tenantId);
            param.put("userId",userId);
            param.put("logId",logId);
            param.put("token", InvocationInfoProxy.getYhtAccessToken());
            Map<String,String> ipaParams = setIpaParams(request);
            param.put("ipaParams",ipaParams);
            //任务日期参数校验
            TaskUtils.dateCheck(param);
            CtmJSONObject jsonObject = openApiExternalService.bankTradeDetailElectronTask(param);
            renderJson(response, CtmJSONObject.toJSONString(jsonObject));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            TaskUtils.updateTaskLog((Map<String,String>)param.get("ipaParams"),TaskUtils.TASK_BACK_FAILURE,logId,e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418034A","下载银行回单pdf定时任务错误：") /* "下载银行回单pdf定时任务错误：" */ + e.getMessage()));
        }
    }

    /**
     * 数智员工 CM07_流水与回单自动关联
     *
     * @param request
     * @throws Exception
     */
    @RequestMapping("/bankreceipt/receiptRelate")
    public void relateBankReceiptTask(@RequestBody(required = false) CtmJSONObject paramMap, HttpServletRequest request,
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
        Map<String,String> ipaParams = setIpaParams(request);
        paramMap.put("ipaParams",ipaParams);
        // 通过银行回单关联银行交易明细给回单关联状态赋值
        CtmJSONObject jsonObject = bankReceiptService.relateBankReceiptDetail(paramMap);
        renderJson(response, CtmJSONObject.toJSONString(jsonObject));
    }


    /**
     * 获取智能流水开关
     *
     * @return
     */
    private static void checkSmartFlowSwitch() {
        String smartFlowSwitch = "0";
        List<Map<String, Object>> smart_flow_switchList = ((JdbcTemplate) AppContext.getBean("jdbcTemplate")).queryForList("SELECT smart_flow_switch from cmp_autoconfig where id = 2329971981028425728;");
        if (CollectionUtils.isNotEmpty(smart_flow_switchList)) {
            smartFlowSwitch = smart_flow_switchList.get(0).get("smart_flow_switch").toString();
        }
        if("0".equals(smartFlowSwitch)){
            boolean isOpenIntelligentDealDetail = DealDetailUtils.isOpenIntelligentDealDetail();
            if (isOpenIntelligentDealDetail) {
                smartFlowSwitch = "1";
            }
        }
        if ("0".equals(smartFlowSwitch)) {
            //throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008A", "系统没有开启智能流水功能，若想使用智能流水功能，请先配置相关的开关参数！") /* "系统没有开启智能流水功能，若想使用智能流水功能，请先配置相关的开关参数！" */);
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22F6BA8605E00018", "该任务暂不支持使用，如需获取直联流水，可通过设置”账户当日\\历史交易流水查询“完成") /* "该任务暂不支持使用，如需获取直联流水，可通过设置”账户当日\\历史交易流水查询“完成" */);
        }
    }

    private Map<String,String> setIpaParams(HttpServletRequest request) throws Exception {
        Map<String,String> ipaParams = new HashMap<>();
        //String exParams = Optional.ofNullable(request.getHeader("X-Ipa-Rest-Params")).map(Base64::getDecoder).orElse("");
        String exParams = new String(Base64.getMimeDecoder().decode(CtmJSONObject.toJSONString(request.getHeader("X-Ipa-Rest-Params"))), "UTF-8");
        Map<String, Object> exParamMap = JsonUtil.parseObject(exParams);
        String recordId = Optional.ofNullable(exParamMap.get("recordId").toString()).orElse("");
        String nodeId = Optional.ofNullable(exParamMap.get("nodeId").toString()).orElse("");
        ipaParams.put("recordId",recordId);
        ipaParams.put("nodeId",nodeId);
        return ipaParams;
    }

    private void adaptParam(CtmJSONObject param) {
        if(param.get("accentity") != null){
            List<String> accentityList = (List<String>) param.get("accentity");
            String accentityStr = String.join(",", accentityList);
            param.put("accentity",accentityStr);
        }
        if(param.get("banktype") != null){
            List<String> banktypeList = (List<String>) param.get("banktype");
            String banktypeStr = String.join(",", banktypeList);
            param.put("banktype",banktypeStr);
        }
        if(param.get("currency") != null){
            List<String> currencyList = (List<String>) param.get("currency");
            String currencyStr = String.join(",", currencyList);
            param.put("currency",currencyStr);
        }
        if(param.get("bankaccount") != null){
            List<String> bankaccountList = (List<String>) param.get("bankaccount");
            String bankaccountStr = String.join(",", bankaccountList);
            param.put("bankaccount",bankaccountStr);
        }
        if(param.get("accountId") != null){
            List<String> accountIdList = (List<String>) param.get("accountId");
            String accountIdStr = String.join(",", accountIdList);
            param.put("accountId",accountIdStr);
        }
    }
}
