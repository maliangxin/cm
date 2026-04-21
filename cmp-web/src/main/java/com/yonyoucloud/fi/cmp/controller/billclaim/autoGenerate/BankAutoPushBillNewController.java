package com.yonyoucloud.fi.cmp.controller.billclaim.autoGenerate;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoPushBillNewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 *  银行对账单 自动下推资金调度 生单调度任务
 *  新建controller 避免侵入原BankAutoPushBillController*/



@Controller
@RequestMapping("/autopushbillnew")
@Slf4j
public class BankAutoPushBillNewController extends BaseController {

    @Resource
    private BankAutoPushBillNewService bankAutoPushBillNewService;


     /** 银行对账单，自动下推资金调度等 生单推送接口*/


    @PostMapping("/autopush")
    public void autoPush(@RequestBody(required = false) Map<String,Object> paramMap, HttpServletRequest request, HttpServletResponse response) throws Exception {

        CtmJSONObject params = new CtmJSONObject();
        //获取租户级自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId",Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId",Optional.ofNullable(request.getHeader("logId")).orElse(""));
        params.put("maxQueryNum",paramMap.get("maxQueryNum"));
        params.put("singleProcessNum",paramMap.get("singleProcessNum"));
        //接口配置参数day，用来判断交易日期范围
        //params.put("day",Optional.ofNullable(param.getString("day")).orElse("0"));
        //执行推单
        CtmJSONObject res = bankAutoPushBillNewService.autoPush(params);
        renderJson(response, CtmJSONObject.toJSONString(res));

    }


}
