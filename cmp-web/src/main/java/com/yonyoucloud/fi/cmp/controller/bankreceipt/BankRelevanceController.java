package com.yonyoucloud.fi.cmp.controller.bankreceipt;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankCheckMatchService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

/**
 * 回单匹配
 */
@Controller
@Slf4j
@RequestMapping("/businessreceipt")
@Authentication(value = false, readCookie = true)
public class BankRelevanceController extends BaseController {
    @Autowired
    private BankCheckMatchService bankCheckMatchService;

    @RequestMapping("/automatch")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public void automatch(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        CtmJSONObject reback = bankCheckMatchService.automatch(params);
        renderJson(response, ResultMessage.data(reback));
    }

    @RequestMapping("/manualmatch")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public void manualmatch(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        CtmJSONObject reback = bankCheckMatchService.manualmatch(params);
        renderJson(response, ResultMessage.data(reback));
    }

    @RequestMapping("/cancelmatch")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public void cancelmatch(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        CtmJSONObject reback = bankCheckMatchService.cancelmatch(params);
        renderJson(response, ResultMessage.data(reback));
    }

}
