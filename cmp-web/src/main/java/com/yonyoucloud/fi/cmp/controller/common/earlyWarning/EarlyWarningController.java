package com.yonyoucloud.fi.cmp.controller.common.earlyWarning;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.earlywarning.service.EarlyWarningService;
import com.yonyoucloud.fi.cmp.earlywarning.service.NotClaimBankreconciliationWarningService;
import com.yonyoucloud.fi.cmp.earlywarning.service.UnclaimedWarningService;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 * <h1>告警任务Controller</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-08-15 10:44
 */
@Slf4j
@RestController
@RequestMapping("/earlywarning")
@RequiredArgsConstructor
public class EarlyWarningController extends BaseController {

    private final EarlyWarningService earlyWarningService;

    private final UnclaimedWarningService unclaimedWarningService;

    private final NotClaimBankreconciliationWarningService notClaimBankreconciliationWarningService;

    @PostMapping("/paymentDateWarningTask")
    @Authentication(value = false, readCookie = true)
    public void payApplyBillPayDateWarningTask(HttpServletRequest request, HttpServletResponse response,@RequestBody CtmJSONObject body) throws Exception {
        String logIdVail = Optional.ofNullable(request.getHeader("logId")).orElse("");
        String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        String logId = Optional.of(logIdVail).orElse("");
        String beforeDaysStr = body.getString("beforeDays");
        int beforeDays = -1;
        if (ValueUtils.isNotEmptyObj(beforeDaysStr)) {
            beforeDays = Integer.parseInt(beforeDaysStr);
        }
        Map<String,Object> result = earlyWarningService.payApplyBillPayDateWarningTask(beforeDays, logId, tenantId);
        log.info("updateDistanceProposePaymentDateDaysTask. response parameter : {}", result);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    @PostMapping("/acctbalWarningTask")
    @Authentication(value = false, readCookie = true)
    public void acctbalWarningTask(HttpServletRequest request, HttpServletResponse response,@RequestBody CtmJSONObject body) throws Exception {
        log.info("acctbalWarningTask RequestBody:{}", body.toString());
        CtmJSONObject data = new CtmJSONObject();
        try {
            String logIdVail = Optional.ofNullable(request.getHeader("logId")).orElse("");
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String logId = Optional.of(logIdVail).orElse("");
            String beforeDaysStr = body.getString("beforeDays");
            Integer beforeDays = 1;
            if (ValueUtils.isNotEmptyObj(beforeDaysStr)) {
                beforeDays = Integer.parseInt(beforeDaysStr);
            }
            if(beforeDays.intValue() > 3){
                beforeDays = 3;
            }
            earlyWarningService.acctbalWarningTask(beforeDays, logId, tenantId);
            data.put("asynchronized", "true");
            log.info("acctbalWarningTask. response parameter : {}", data);
        } catch (Exception e) {
            data.put("status", TaskUtils.TASK_BACK_FAILURE);//执行结果： 0：失败；1：成功
            data.put("data", null);//业务方自定义结果集字段
            data.put("msg", e.getMessage());//	异常信息
        }
        renderJson(response, data.toString());
    }

    /**
     * 余额自动检查预警
     */
    @PostMapping("/acctbalCheckWarning")
    @Authentication(value = false, readCookie = true)
    public void acctbalCheckWarning(HttpServletRequest request, HttpServletResponse response, @RequestBody CtmJSONObject body) throws Exception {
        try {
            log.error("acctbalCheckWarning RequestBody:{}", CtmJSONObject.toJSONString(body));
            String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            //            Integer checkRange = body.getInteger("CheckRange");
            Integer checkRange = 0;
            Map<String, Object> result = earlyWarningService.acctbalCheckWarning(checkRange, logId, tenantId);
            log.info("acctbalCheckWarning result  : {}", result);
            renderJson(response, CtmJSONObject.toJSONString(result));
        }catch (Throwable e){
            log.error("catchAccCheckException"+e.getMessage(),e);
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("status",TaskUtils.TASK_BACK_FAILURE);
            jsonObject.put("msg",e.getMessage());
        }
    }

    @PostMapping("/unclaimedWarning")
    @Authentication(value = false, readCookie = true)
    public void unclaimedWarning(HttpServletRequest request, HttpServletResponse response,@RequestBody CtmJSONObject body) throws Exception {
        try {
            String logIdVail = Optional.ofNullable(request.getHeader("logId")).orElse("");
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String logId = Optional.of(logIdVail).orElse("");
            String accentitys = body.getString("accentity");
            //单据日期范围（前X日）
            Integer checkRange = body.getInteger("checkRange");
            //超时天数
            Integer timeOuts = body.getInteger("timeOuts");
            Map<String, Object> result = unclaimedWarningService.unclaimedWarning(accentitys, checkRange, timeOuts, logId, tenantId);
            log.info("unclaimedWarning. response parameter : {}", result);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Throwable e) {
            log.error("unclaimedWarningException" + e.getMessage(), e);
            CtmJSONObject jsonObject = new CtmJSONObject();
            jsonObject.put("status", TaskUtils.TASK_BACK_FAILURE);
            jsonObject.put("msg", e.getMessage());
        }
    }

    /**
     * * 疑似退票预警任务
     * @param request
     * @param response
     * @param params
     * @throws Exception
     */
    @PostMapping("/suspectedRefundWarning")
    @Authentication(value = false, readCookie = true)
    public void suspectedRefundWarning(HttpServletRequest request, HttpServletResponse response,@RequestBody CtmJSONObject params) throws Exception {
        try {
            String logIdVail = Optional.ofNullable(request.getHeader("logId")).orElse("");
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String logId = Optional.of(logIdVail).orElse("");
            Map<String, Object> result = earlyWarningService.suspectedRefundWarning(params, logId, tenantId);
            log.info("suspectedRefundWarning. response parameter : {}", result);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Throwable e) {
            log.error("suspectedRefundWarning"+e.getMessage(),e);
            CtmJSONObject CtmJSONObject = new CtmJSONObject();
            CtmJSONObject.put("status", TaskUtils.TASK_BACK_FAILURE);
            CtmJSONObject.put("msg", e.getMessage());
        }
    }


    /**
     * * 未导入流水预警
     * @throws Exception
     */
    @PostMapping("/notImportWarning")
    @Authentication(value = false, readCookie = true)
    public void notImportWarning(HttpServletRequest request, HttpServletResponse response,@RequestBody CtmJSONObject body) throws Exception {
        try {
            String logIdVail = Optional.ofNullable(request.getHeader("logId")).orElse("");
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String logId = Optional.of(logIdVail).orElse("");
            //银行类别
            String bankType = body.getString("bank_type");
            //币种
            String currency = body.getString("currency");
            String accentity = body.getString("accentity");
            //是否包含非直连
            String checkRange = body.getString("CheckRange");
            //是否包含冻结账户
            String cotainFreezeAccount = body.getString("cotainFreezeAccount");
            //检查前几日
            Integer checkDate = body.getInteger("CheckDate");
            Map<String, Object> result = unclaimedWarningService.notImportWarning(accentity, bankType, currency,checkRange,checkDate,cotainFreezeAccount,logId, tenantId);
            log.info("unclaimedWarning. response parameter : {}", result);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Throwable e) {
            log.error("unclaimedWarningException" + e.getMessage(), e);
            CtmJSONObject CtmJSONObject = new CtmJSONObject();
            CtmJSONObject.put("status", TaskUtils.TASK_BACK_FAILURE);
            CtmJSONObject.put("msg", e.getMessage());
        }
    }

    /**
     * * 未认领流水预警
     * @throws Exception
     */
    @PostMapping("/notClaimWarning")
    @Authentication(value = false, readCookie = true)
    public void notClaimWarning(HttpServletRequest request, HttpServletResponse response,@RequestBody CtmJSONObject body) throws Exception {
        try {
            String logIdVail = Optional.ofNullable(request.getHeader("logId")).orElse("");
            String logId = Optional.of(logIdVail).orElse("");
            Map<String, Object> result = notClaimBankreconciliationWarningService.notClaimBankreconciliationWarning(body,logId );
            log.info("unclaimedWarning. response parameter : {}", result);
            renderJson(response, CtmJSONObject.toJSONString(result));
        } catch (Throwable e) {
            log.error("unclaimedWarningException" + e.getMessage(), e);
            CtmJSONObject ctmJSONObject = new CtmJSONObject();
            ctmJSONObject.put("status", TaskUtils.TASK_BACK_FAILURE);
            ctmJSONObject.put("msg", e.getMessage());
        }
    }
}
