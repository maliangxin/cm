package com.yonyoucloud.fi.cmp.controller.billclaim.virtualFlow;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.bankcapitalvirtual.VirtualFlowRuleConfigService;
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
 * 虚拟流水规则 - 启用状态修改
 *
 * @author msc
 */
@Controller
@Slf4j
@RequestMapping("/virtualflow")
public class VirtualFlowRuleEnableController extends BaseController {

    @Autowired
    VirtualFlowRuleConfigService virtualFlowRuleConfigService;

    /**
     * 设置为启用
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("/enable")
    public void enable(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(virtualFlowRuleConfigService.enable(paramMap)));
    }

    /**
     * 设置为禁用
     *
     * @param paramMap
     * @param request
     * @param response
     */
    @PostMapping("/disEnable")
    public void disEnable(@RequestBody(required = false) Map<String, Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(virtualFlowRuleConfigService.disenable(paramMap)));
    }
}
