package com.yonyoucloud.fi.cmp.controller.common;


import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyoucloud.fi.cmp.autosettle.CmpAutoSettleTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2022/12/9 10:32
 */
@Controller
@RequestMapping("/autosettletask")
@Slf4j
public class CmpAutoSettleTaskController extends BaseController {

    @Resource
    private CmpAutoSettleTaskService cmpAutoSettleTaskService;

    /**
     * 银行对账单，自动下推资金调度等 生单推送接口
     */
    @PostMapping("/paybillsettle")
    public void paybillsettle(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {

        CtmJSONObject params = new CtmJSONObject();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId",Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId",Optional.ofNullable(request.getHeader("logId")).orElse(""));
        //接口配置参数day，用来判断最大批处理数据量
        params.put("batchNum",Optional.ofNullable(param.getString("batchNum")).orElse("500"));
        //执行自动结算
        CtmJSONObject res = cmpAutoSettleTaskService.payBillAutoSettle(params);
        renderJson(response, CtmJSONObject.toJSONString(res));
    }

    @PostMapping("/receivesettle")
    public void receivesettle(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {

        CtmJSONObject params = new CtmJSONObject();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId",Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId",Optional.ofNullable(request.getHeader("logId")).orElse(""));
        //接口配置参数day，用来判断最大批处理数据量
        params.put("batchNum",Optional.ofNullable(param.getString("batchNum")).orElse("500"));
        //执行自动结算
        CtmJSONObject res = cmpAutoSettleTaskService.receiveBillAutoSettle(params);
        renderJson(response, CtmJSONObject.toJSONString(res));
    }
}
