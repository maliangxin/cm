package com.yonyoucloud.fi.cmp.controller.fund.transferaccount;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.transferaccount.service.TransferAccountTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 * @ClassName TransferAccountTaskController
 * @Desc 转账单调度任务，查询支付状态
 * @Author lidchn
 * @Date 2023年3月28日14:22:18
 * @Version 1.0
 */
@Controller
@RequestMapping("/cm/transferAccountTask/")
@Slf4j
public class TransferAccountTaskController extends BaseController {

    @Autowired
    TransferAccountTaskService transferAccountTaskService;

    /**
     * 汇入汇款待确认业务列表查询SSFE3005调度任务
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @PostMapping("transferPayStatusQueryTask")
    public Map<String, Object> transferPayStatusQueryTask(@RequestBody(required = false) Map<String,Object> param, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject params = new CtmJSONObject();
        try {
            //获取租户级自动任务参数
            params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
            params.put("userId",Optional.ofNullable(request.getHeader("userId")).orElse(""));
            params.put("logId",Optional.ofNullable(request.getHeader("logId")).orElse(""));
            transferAccountTaskService.queryPayStatus(params);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00154", "汇入汇款待确认业务列表查询SSFE3005调度任务错误：") /* "汇入汇款待确认业务列表查询SSFE3005调度任务错误：" */ + e.getMessage()));
        }
        ObjectNode result = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        result.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(result));
        return null;
    }

}
