package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.merchant.MerchantService;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/merchant")
public class MerchantController extends BaseController {

    @Autowired
    private MerchantService merchantService;

    /**
     * 同步客商页面，点击保存
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/synMerchant")
    public void synMerchant(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, merchantService.synMerchant(params));
    }

    /**
     * 同步客商页面，ajax请求获取，中国，编码
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/getCountryByName")
    public void getCountryByName(@RequestBody CtmJSONObject params, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        String defaultCountry = AppContext.getEnvConfig("default.country");
        params.put(MerchantConstant.NAME, defaultCountry);
        CtmJSONObject result = merchantService.getCountryByName(params);
        renderJson(response, ResultMessage.data(result));
    }


    /**
     * 同步客商页面加载，判断是否需要同步客商
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/checkMerchant")
    public void checkMerchant(@RequestBody CtmJSONObject params, HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        CtmJSONObject result = merchantService.checkMerchant(params);
        renderJson(response, ResultMessage.data(result));
    }


}
