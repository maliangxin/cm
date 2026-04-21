package com.yonyoucloud.fi.cmp.controller.bankbillcheck;

import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankbillcheck.service.BankBillCheckService;
import com.yonyoucloud.fi.cmp.bankbillcheck.service.BankBillCheckServiceImpl;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Optional;

import static com.yonyou.iuap.framework.sdk.common.utils.ResponseUtils.renderJson;


/**
 * @Author zhucongcong
 * @Date 2024/9/25
 */
@Slf4j
@Controller
@RequestMapping("/bankBillCheck")
public class BankBillCheckController {


    @Autowired
    private BankBillCheckService bankBillCheckService;

    /**
     * 查询对账信息
     */
    @PostMapping("/queryBillInfoUnNeedUkey")
    public void queryBillInfo(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        try {
            List<String>  errorList = bankBillCheckService.queryBillInfo(params);
            if (CollectionUtils.isNotEmpty(errorList)) {
                throw new CtmException(errorList.toString());
            }
            renderJson(response, String.format(ResultMessage.data(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001EF", "查询成功") /* "查询成功" */)));;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error( com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009D", "查询对账信息异常：") /* "查询对账信息异常：" */ + e.getMessage()));
        }
    }

    /**
     * 相符操作
     *
     * @param params
     * @param response
     */
    @PostMapping("/match")
    public void match(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        CtmJSONArray rows = params.getJSONArray("rows");
        if (CollectionUtils.isEmpty(rows)) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        renderJson(response,
                ResultMessage.data(bankBillCheckService.match(rows)));
    }

    /**
     * 不相符校验数据
     *
     * @param params
     * @param response
     */
    @PostMapping("/unMatch")
    public void unMatch(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        CtmJSONArray rows = params.getJSONArray("rows");
        if (CollectionUtils.isEmpty(rows)) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        renderJson(response,
                ResultMessage.data(bankBillCheckService.unMatch(rows)));
    }

    /**
     * 不相符回写数据
     *
     * @param params
     * @param response
     */
    @PostMapping("/unMatchUpdate")
    public void UnMatchUpdate(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        CtmJSONArray jsonArray = params.getJSONArray("data");
        if (CollectionUtils.isEmpty(jsonArray)) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        renderJson(response,
                ResultMessage.data(bankBillCheckService.unMatchUpdate(params)));
    }

    /**
     * 对账结果提交
     *
     * @param params
     * @param response
     */
    @PostMapping("/checkResultSubmit")
    public void checkResultSubmit(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        CtmJSONArray jsonArray = params.getJSONArray("data");
        if (CollectionUtils.isEmpty(jsonArray)) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        renderJson(response,
                ResultMessage.data(bankBillCheckService.checkResultSubmit(jsonArray)));
    }

    /**
     * 对账结果查询
     *
     * @param params
     * @param response
     */
    @PostMapping("/checkResultQuery")
    public void checkResultQuery(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        CtmJSONArray jsonArray = params.getJSONArray("data");
        if (CollectionUtils.isEmpty(jsonArray)) {
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000B2", "请至少选择一条数据！") /* "请至少选择一条数据！" */);
        }
        renderJson(response,
                ResultMessage.data(bankBillCheckService.checkResultQuery(jsonArray)));
    }


    /**
     * 调度任务“银企对账信息查询”，支持按照资金组织、期间、银行类别、银行账号等维度查询；最小时间间隔分钟；预置调度任务，时间间隔15分钟
     *
     * @param params
     * @param response
     */
    @PostMapping(value = "/scheduleQueryBillInfo")
    public void scheduleQueryBillInfo(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {

        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        params.put("logId", logId);
        params.put(ICmpConstant.IS_DISPATCH_TASK_CMP, true);
        renderJson(response,
                CtmJSONObject.toJSONString(bankBillCheckService.scheduleQueryBillInfo(params)));
    }

    /**
     * 调度任务“银企对账确认状态查询”，不预置维度；最小时间间隔分钟；预置调度任务，时间间隔5分钟（可参照调度任务：薪资支付、付款工作台支付状态查询）
     *
     * @param params
     * @param response
     */
    @PostMapping(value = "scheduleCheckResultQuery")
    public void scheduleCheckResultQuery(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        params.put("logId", logId);
        renderJson(response,
                CtmJSONObject.toJSONString(bankBillCheckService.scheduleCheckResultQuery(params)));
    }
}
