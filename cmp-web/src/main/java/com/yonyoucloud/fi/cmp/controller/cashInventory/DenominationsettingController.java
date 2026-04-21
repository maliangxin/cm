package com.yonyoucloud.fi.cmp.controller.cashInventory;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.denominationSetting.DenominationsettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/denominationsetting")
@Authentication(value = false, readCookie = true)
@Slf4j
public class DenominationsettingController extends BaseController {

    private final static String ID = "id";
    @Autowired
    DenominationsettingService denominationsettingService;

    @RequestMapping("/checkDenominationsetting")
    @CMPDiworkPermission(IServicecodeConstant.DENOMINATIONSETTING)
    public void checkDenominationsetting(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        Long id = param.getLong(ID);
        renderJson(response, ResultMessage.data(denominationsettingService.checkDenominationsetting(id)));
    }

    @RequestMapping("/checkDenominationsetting_b")
    @CMPDiworkPermission(IServicecodeConstant.DENOMINATIONSETTING)
    public void checkDenominationsetting_b(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        Long id = param.getLong(ID);
        renderJson(response, ResultMessage.data(denominationsettingService.checkDenominationsetting_b(id)));
    }
}
