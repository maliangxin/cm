package com.yonyoucloud.fi.cmp.controller.basicSetting;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankaccountsetting.service.AccountSynchronizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @Author xuyao2
 * @Date 2023/3/23 11:08
 */

@RestController
@RequestMapping("/bankaccountsettingAuto")
@Slf4j
public class AccountSynchronizationController extends BaseController {

    @Autowired
    AccountSynchronizationService accountSynchronizationService;

    /**
     *账号同步调度任务
     */
    @PostMapping("/bankaccountsync")
    public void bankaccountsync(@RequestBody(required = false) Map<String,Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject params = new CtmJSONObject();
        //获取自动任务参数
        params.put("logId", Optional.ofNullable(request.getHeader("logId")).orElse(""));
        accountSynchronizationService.bankaccountsync(params);
        ObjectNode result = com.yonyoucloud.fi.cmp.util.JSONBuilderUtil.createJson();
        result.put("asynchronized", true);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }
}
