package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.exchangegainloss.ExchangeGainLossService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

/**
 * Created  by xudy on 2019/9/26.
 * 汇率损益计算
 */
@Controller
@RequestMapping("/exchangegainloss")
@Slf4j
public class ExchangeGainLossController extends BaseController {

    @Autowired
    private ExchangeGainLossService exchangeGainLossService;


    /**
     * @param params
     * @param response 汇率损益界面初始化
     */
    @RequestMapping("/initData")
    @ResponseBody
    @CMPDiworkPermission(IServicecodeConstant.EXCHANGEGAINLOSS)
    public void initData(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(exchangeGainLossService.initData(params)));
    }


}
