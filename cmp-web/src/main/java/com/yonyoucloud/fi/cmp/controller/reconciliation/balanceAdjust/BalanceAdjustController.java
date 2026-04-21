package com.yonyoucloud.fi.cmp.controller.reconciliation.balanceAdjust;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.balanceadjust.service.impl.BalanceAdjustAutoGenerateService;
import com.yonyoucloud.fi.cmp.balanceadjust.service.impl.BalanceAdjustService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankAccountSetting.BankAccountSettingVO;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @author sz@yonyou.com
 * @version 1.0
 */
@Controller
@RequestMapping("/balanceadjust")
@Authentication(value = false, readCookie = true)
@Slf4j
public class BalanceAdjustController extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(BalanceAdjustController.class);
    @Autowired
    BalanceAdjustService balanceAdjustService;
    @Autowired
    private BalanceAdjustAutoGenerateService balanceAdjustAutoGenerateService;

    @RequestMapping("/query")
    @CMPDiworkPermission(IServicecodeConstant.OPENINGOUTSTANDING)
    public void query(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List commonVOs = (List) obj.get("commonVOs");
        String isQc = null;
        for (int i = 0; i < commonVOs.size(); i++) {
            Map map = (Map) commonVOs.get(i);
            if ("isQc".equals(map.get("itemName"))) {
                isQc = String.valueOf(map.get("value1"));
            }
        }
        CtmJSONObject jSONObject = null;
        if (StringUtils.isNotEmpty(isQc)) {
            jSONObject = balanceAdjustService.query(obj, true);
        } else {
            jSONObject = balanceAdjustService.query(obj, false);
        }
        renderJson(response, ResultMessage.data(jSONObject));
    }

    /**
     * 页面调平的接口
     *
     * @param --+9obj
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/queryBalanceState")
    public void queryBalanceState(@RequestBody CtmJSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception{
        JsonNode jSONObject = balanceAdjustService.queryBalanceState(paramObj);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(jSONObject)));
    }

    @RequestMapping("/initQuery")
    public void initQuery(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject jSONObject = balanceAdjustService.query(obj, true);
        renderJson(response, ResultMessage.data(jSONObject));
    }

    /**
     * 查询银行账户历史余额，期初余额
     * @param bankAccountSettingVO
     * * accentity，bankaccount，currency：账户使用组织，银行账号，币种
     * * enableDateStr ：查询日期，启用日期前一天
     * @param response
     */
    @RequestMapping("/getBankAccountOpeningBalance")
    public void getBankAccountHistoryBalance(@RequestBody BankAccountSettingVO bankAccountSettingVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(balanceAdjustService.getBankAccountHistoryBalance(bankAccountSettingVO)));
    }

    /**
     * 余额重算:重新获取企业方余额及银行方余额
     * 银行方余额: 直联账户点击【余额重算】按钮后，调用账户历史余额拉取的接口，获取当前账户截止日当天的余额
     * 非直联账户，和之前查询逻辑一致
     * @param bankAccountSettingVO
     * @param response
     * @throws Exception
     */
    @RequestMapping("/recalculateBalance")
    public void recalculateBalance(@RequestBody BankAccountSettingVO bankAccountSettingVO, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(balanceAdjustService.recalculateBalance(bankAccountSettingVO)));
    }

    /**
     * CM34_自动生成月末余额调节表,自动生成余额调节表,调度任务方法入口
     */
    @PostMapping("/generateMonthEndBalanceAdjust")
    public void generateMonthEndBalanceAdjust(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode params = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId",Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId",Optional.ofNullable(request.getHeader("logId")).orElse(""));

        ExecutorService dataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"GenerateMonthEndBalanceAdjust-threadpool");
        //执行推单
        dataExecutor.submit(() -> {
            try {
                CtmLockTool.executeInOneServiceLock("GenerateMonthEndBalanceAdjustTask", 60 * 5L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        //加锁失败
                        //通知任务执行失败
                        TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, params.get("logId").asText(),"执行失败，有正在执行的任务", TaskUtils.UPDATE_TASK_LOG_URL);
                        return;
                    }
                    //加锁成功
                    if(balanceAdjustAutoGenerateService.generateMonthEndBalanceAdjust(param)){
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
     * 预警未生成余额调节表，预警任务调用方法入口
     */
    @PostMapping("/warningMonthEndUngenerated")
    public void warningMonthEndUngenerated(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            Map<String, Object> result = balanceAdjustAutoGenerateService.warningMonthEndUngenerated(param);
            log.info("warningMonthEndUngenerated. response parameter : {}", result);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Throwable e) {
            log.error("warningMonthEndUngenerated"+e.getMessage(),e);
            CtmJSONObject CtmJSONObject = new CtmJSONObject();
            CtmJSONObject.put("status", TaskUtils.TASK_BACK_FAILURE);
            CtmJSONObject.put("msg", e.getMessage());
        }
    }

}
