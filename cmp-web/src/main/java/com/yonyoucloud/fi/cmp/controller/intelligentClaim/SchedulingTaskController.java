package com.yonyoucloud.fi.cmp.controller.intelligentClaim;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankidentify.BankIdentifyService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fcdsusesetting.service.IFcdsUseSettingInnerService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.common.IBankAccountDataPullService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailContext;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.impl.DefaultStreamBatchHandler;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match.BankDealDetailMatchChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.common.CheckRuleCommonUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate.impl.BankDealDetailCompensate;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.MD5Utils;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 流水拉取入口类
 *
 * @author maliangn
 */
@RequestMapping("/cmp/task")
@Slf4j
@Controller
public class SchedulingTaskController extends BaseController {

    @Autowired
    IBankAccountDataPullService bankAccountDataPullService;

    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    @Autowired
    BankDealDetailCompensate bankDealDetailCompensate;

    @Resource
    private BankIdentifyService bankIdentifyService;
    @Resource
    private IFcdsUseSettingInnerService fcdsUseSettingInnerService;
    public static String AUTO_CONFIRMBILL_TASK_LOCK = "AUTO_HIST_PULL_TASK_LOCK";
    public static String AUTO_pull_TASK_LOCK = "AUTO__PULL_TASK_LOCK";

    /**
     * 银行交易明细拉取（新）调度任务
     * 智能流水调度任务入口
     *
     * @param
     * @return
     * @throws Exception
     */
    @RequestMapping("/bankDealDetail/pull")
    public void bankDealDetailPull(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.error("bankDealDetailPull start ==================：" + param.toString());
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        YmsLock ymsLock = null;
        //校验简强开关
        try {
            ymsLock = getYmsLock(param, request);
            if (ymsLock == null) {
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008B", "执行失败，有正在执行的任务") /* "执行失败，有正在执行的任务" */, TaskUtils.UPDATE_TASK_LOG_URL);
                return;
            }
            checkSmartFlowSwitch();
            CheckRuleCommonUtils.checkBankReconciliationIdentifyType(bankIdentifyService, fcdsUseSettingInnerService);

            log.error("bankDealDetailPull start ==================：" + param.toString());

            param.put("logId", logId);
            param.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                try {
                    bankAccountDataPullService.pullData(param);
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180272", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    log.error("账户交易流水查询辨识失败，失败原因：" + e.getMessage(), e);
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                }
            });
        } catch (Exception e) {
            log.error("账户交易流水查询辨识失败，失败原因：" + e.getMessage(), e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            return;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }

        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(retMap));
    }

    private YmsLock getYmsLock(CtmJSONObject param, HttpServletRequest request) {
        //获取租户级自动任务参数
        String tenantid = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        StringBuilder builder = new StringBuilder();
        String key = AUTO_pull_TASK_LOCK + "_" + tenantid;
        if (param != null) {
            for (String paramKey : param.keySet()) {
                // 获取 value
                Object value = param.get(paramKey);
                // 将 value 转换为字符串并添加到 result 中
                if (value != null) {
                    builder.append(value.toString());
                }
            }
            String signContent = MD5Utils.MD5Encode(builder.toString());
            key = AUTO_pull_TASK_LOCK + "_" + tenantid + "_" + signContent;
        }
        YmsLock ymsLock = JedisLockUtils.taskLockWithOutTrace(key, 60 * 60);
        return ymsLock;
    }

    /**
     * 获取智能流水开关
     *
     * @return
     */
    public static void checkSmartFlowSwitch() {
        String smartFlowSwitch = "0";
        List<java.util.Map<java.lang.String, java.lang.Object>> smart_flow_switchList = ((JdbcTemplate) AppContext.getBean("jdbcTemplate")).queryForList("SELECT smart_flow_switch from cmp_autoconfig where id = 2329971981028425728;");
        if (CollectionUtils.isNotEmpty(smart_flow_switchList)) {
            smartFlowSwitch = smart_flow_switchList.get(0).get("smart_flow_switch").toString();
        }
        if ("0".equals(smartFlowSwitch)) {
            boolean isOpenIntelligentDealDetail = DealDetailUtils.isOpenIntelligentDealDetail();
            if (isOpenIntelligentDealDetail) {
                smartFlowSwitch = "1";
            }
        }
        if ("0".equals(smartFlowSwitch)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008A", "系统没有开启智能流水功能，若想使用智能流水功能，请先配置相关的开关参数！") /* "系统没有开启智能流水功能，若想使用智能流水功能，请先配置相关的开关参数！" */);
        }
    }

    /**
     * 智能流水补偿调度任务入口
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/bankDealDetail/histpull")
    public void bankDealDetailHistoryPull(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        //校验简强开关
        YmsLock ymsLock = null;
        try {
            ymsLock = getYmsLock(param, request);
            if (ymsLock == null) {
                TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008B", "执行失败，有正在执行的任务") /* "执行失败，有正在执行的任务" */, TaskUtils.UPDATE_TASK_LOG_URL);
                return;
            }
            checkSmartFlowSwitch();
            CheckRuleCommonUtils.checkBankReconciliationIdentifyType(bankIdentifyService, fcdsUseSettingInnerService);


            log.error("bankDealDetailPull start ==================：" + param.toString());
            param.put("logId", logId);
            param.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
            executorServicePool.getThreadPoolExecutor().submit(() -> {
                try {
                    HashMap<String, String> querydate = TaskUtils.queryDateProcess(param, "yyyy-MM-dd");
                    Date startDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_START_DATE), DateUtils.DATE_PATTERN);
                    Date endDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_END_DATE), DateUtils.DATE_PATTERN);
                    if (startDate == null || endDate == null) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8008C", "执行的调度任务配置的日期获取不对，请检查！") /* "执行的调度任务配置的日期获取不对，请检查！" */);
                    }
                    bankDealDetailCompensate.compensateInContext(startDate, endDate, param);
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180272", "执行成功") /* "执行成功" */, TaskUtils.UPDATE_TASK_LOG_URL);
                } catch (Exception e) {
                    log.error("流水弥补任务失败，失败原因：" + e.getMessage(), e);
                    TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
                }
            });

        } catch (Exception e) {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
            log.error("账户交易流水查询辨识失败，失败原因：" + e.getMessage(), e);
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId, e.getMessage(), TaskUtils.UPDATE_TASK_LOG_URL);
            return;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }

        Map<String, Object> retMap = new HashMap<>();
        retMap.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(retMap));
    }

    @Autowired
    Map<String, DefaultStreamBatchHandler> handlerMap;

    @RequestMapping("/testHandler")
    public void testHandler(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        DefaultStreamBatchHandler defaultStreamBatchHandler = handlerMap.get(param.get("handler_name"));
        BankDealDetailContext context = new BankDealDetailContext();
        QuerySchema schema = QuerySchema.create().addSelect(" * ");//
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("ispublish").eq("0"));
        conditionGroup.appendCondition(QueryCondition.name("associationstatus").eq("0"));
        schema.addCondition(conditionGroup);
        schema.addPager(1, 100);
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        List<BankDealDetailWrapper> wrappers = new ArrayList<>();
        for (BankReconciliation bankReconciliation : bankReconciliationList) {
            BankDealDetailWrapper wrapper = new BankDealDetailWrapper();
            wrapper.setBankReconciliation(bankReconciliation);
            wrappers.add(wrapper);
        }
        context.setWrappers(wrappers);
        defaultStreamBatchHandler.streamHandler(context, BankDealDetailMatchChainImpl.get().code(null));
    }
}
