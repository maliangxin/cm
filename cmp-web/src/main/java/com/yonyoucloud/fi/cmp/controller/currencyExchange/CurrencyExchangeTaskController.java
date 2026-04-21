package com.yonyoucloud.fi.cmp.controller.currencyExchange;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.task.currency.CurrencyExchangeTaskService;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * @author sz@yonyou.com
 * @version 1.0
 */
@Controller
@RequestMapping("/currencyexchangetask")
@Authentication(value = false, readCookie = true)
@Slf4j
public class CurrencyExchangeTaskController extends BaseController {

    @Autowired
    CurrencyExchangeTaskService currencyExchangeTaskService;

    /**
     * 交割结果查询定时任务
     */
    @PostMapping("/resultQuery")
    @Authentication(value = false, readCookie = true)
    public Map<String,Object> currencyExchangeResultQueryTask(@RequestBody(required = false) Map<String,Object> paramMap, HttpServletRequest request, HttpServletResponse response) {
        try {
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String userId = Optional.ofNullable(request.getHeader("userId")).orElse("");
            String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
            if(null == paramMap){
                paramMap = new HashMap<>();
            }
            paramMap.put("tenantId",tenantId);
            paramMap.put("userId",userId);
            paramMap.put("logId",logId);
            currencyExchangeTaskService.queryResult(paramMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00154", "交割结果查询定时任务错误：") /* "交割结果查询定时任务错误：" */ + e.getMessage()));
        }
        ObjectNode result = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        result.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(result));
        return null;
    }

}
