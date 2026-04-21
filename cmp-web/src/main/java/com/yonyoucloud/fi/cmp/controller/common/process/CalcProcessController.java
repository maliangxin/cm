package com.yonyoucloud.fi.cmp.controller.common.process;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;

import com.yonyoucloud.fi.cmp.paymentbill.service.CalcProcessServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * desc:银行账户实时余额在线下载异步优化，前端定时获取进度
 * author:wangqiangac
 * date:2023/7/10 19:33
 */
@RestController
@Slf4j
public class CalcProcessController extends BaseController {

    /**
     * 获取实时余额异步进度
     */
    @Autowired
    CalcProcessServiceImpl calcProcessServiceImpl;
    @PostMapping("/calc/calcProcess")
    public void calcProcess(@RequestBody CtmJSONObject params, HttpServletResponse response){
        String uid = (String) params.get("uid");
        renderJson(response, ResultMessage.data(calcProcessServiceImpl.getProcess(uid)));
    }


    @GetMapping("/calc/process")
    public void process(@RequestParam String asyncKey, HttpServletResponse response){
        renderJson(response, ResultMessage.data(calcProcessServiceImpl.process(asyncKey)));
    }


    /**
     * 测试2s更新一次进度
     * @param params
     * @param response
     * @throws InterruptedException
     */
    @PostMapping("/calc/testProcess")
    public void testProcess(@RequestBody CtmJSONObject params, HttpServletResponse response) throws InterruptedException {
        String uid = (String) params.get("uid");
        calcProcessServiceImpl.testProcess(uid);
        renderJson(response, ResultMessage.data(null));
    }
}
