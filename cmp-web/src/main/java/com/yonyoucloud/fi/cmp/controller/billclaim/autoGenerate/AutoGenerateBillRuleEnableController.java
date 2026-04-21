package com.yonyoucloud.fi.cmp.controller.billclaim.autoGenerate;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.autoorderrule.AutoOrderRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 自动生单规则 - 启用状态修改
 *
 * @author msc
 */
@Controller
@Slf4j
@RequestMapping("/autocorrrule")
public class AutoGenerateBillRuleEnableController extends BaseController {

    @Autowired
    AutoOrderRuleService autoOrderRuleService;

    /**
     * 将自动生单规则设置为启用
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("/enable")
    public void enable(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoOrderRuleService.enable(paramMap)));
    }

    /**
     * 将自动生单规则设置为禁用
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("/disEnable")
    public void disEnable(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoOrderRuleService.disenable(paramMap)));
    }
}
