package com.yonyoucloud.fi.cmp.controller.fund.InwardRemittance;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyoucloud.fi.cmp.inwardremittance.InwardRemittanceTaskService;
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
 * @author lidchn
 */
@RestController
@RequestMapping("/inwardremittancetask")
@Authentication(value = false, readCookie = true)
@Slf4j
public class InwardRemittanceTaskController extends BaseController {

    @Autowired
    InwardRemittanceTaskService inwardRemittanceTaskService;

    /**
     * 汇入汇款待确认业务列表查询SSFE3005调度任务
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @PostMapping("inwardRemittanceListQueryTask")
    public Map<String, Object> inwardRemittanceListQueryTask(@RequestBody(required = false) Map<String,Object> param, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject params = new CtmJSONObject();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId",Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId",Optional.ofNullable(request.getHeader("logId")).orElse(""));
        if (param.get("queryDaysNum") == null || "".equals(param.get("queryDaysNum"))) {
            params.put("queryDaysNum", 1);
        }
        return inwardRemittanceTaskService.inwardRemittanceListQueryTask(params);
    }

    @PostMapping("inwardRemittanceResultQueryTask")
    public Map<String, Object> inwardRemittanceResultQueryTask(@RequestBody(required = false) Map<String,Object> param, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject params = new CtmJSONObject();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId",Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId",Optional.ofNullable(request.getHeader("logId")).orElse(""));
        return inwardRemittanceTaskService.inwardRemittanceResultQueryTask(params);
    }


    /**
     * 汇入汇款业务明细查询SSFE3006 - 调度任务
     *
     * @param param
     * @param response
     * @auth lidchn
     */
    @PostMapping("inwardRemittanceDetailQueryTask")
    public Map<String, Object> inwardRemittanceDetailQueryTask(@RequestBody(required = false) Map<String,Object> param, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject params = new CtmJSONObject();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId",Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId",Optional.ofNullable(request.getHeader("logId")).orElse(""));
        if (param.get("queryDaysNum") == null || "".equals(param.get("queryDaysNum"))) {
            params.put("queryDaysNum", 1);
        }
        //执行自动结算
        return inwardRemittanceTaskService.inwardRemittanceDetailQueryTask(params);
    }

}
