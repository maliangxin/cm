package com.yonyoucloud.fi.cmp.controller.migrade;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.migrade.CmpNewFiMigradeService;
import com.yonyoucloud.fi.cmp.migrade.CmpNewFiPreCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

@Controller
@Slf4j
@RequestMapping("/migrade")
public class CmpMigradeNewFiController extends BaseController {

    @Autowired
    private CmpNewFiPreCheckService cmpNewFiPreCheckService;

    @Autowired
    private CmpNewFiMigradeService cmpNewFiMigradeService;

    @PostMapping("/preCheck")
    @Authentication(value = false, readCookie = true)
    public void confirmPlaceOrder(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, cmpNewFiPreCheckService.newFiPreCheck(params));
    }

    @PostMapping("/migradePay")
    @Authentication(value = false, readCookie = true)
    public void migradePayToFunPayMent(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        cmpNewFiMigradeService.migradePayToFunPayMent(params);
        renderJson(response, ResultMessage.data(null));
    }

    @PostMapping("/migradeRe")
    @Authentication(value = false, readCookie = true)
    public void migradeReToFundCollection(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        cmpNewFiMigradeService.migradeReToFundCollection(params);
        renderJson(response, ResultMessage.data(null));
    }

    @PostMapping("/migradePayResult")
    @Authentication(value = false, readCookie = true)
    public void migradePayResult(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(cmpNewFiMigradeService.migradePayResult()));
    }

    @PostMapping("/migradeReResult")
    @Authentication(value = false, readCookie = true)
    public void migradeReResult(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(cmpNewFiMigradeService.migradeReResult()));
    }

    @PostMapping("/migradeUpdateTradetype")
    @Authentication(value = false, readCookie = true)
    public void migradeUpdateTradetype(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        cmpNewFiMigradeService.migradeUpdateTradetype(params);
        renderJson(response, ResultMessage.data(null));
    }

    @PostMapping("/migradeUpdateCharacterDef")
    @Authentication(value = false, readCookie = true)
    public void migradeUpdateCharacterDef(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        cmpNewFiMigradeService.migradeUpdateCharacterDef(params);
        renderJson(response, ResultMessage.data(null));
    }

}
