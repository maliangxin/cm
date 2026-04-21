package com.yonyoucloud.fi.cmp.controller.migrade;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.migrade.CmpMigradeForSystemService;
import com.yonyoucloud.fi.cmp.vo.migrade.CmpMigradeForSystemRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

/**
 * 工具服务统一发起，调用领域服务
 */
@Controller
@Slf4j
@RequestMapping("/cmpMigrate/upgradeTenant")
public class CmpMigradeForSystemController extends BaseController {

    @Autowired
    CmpMigradeForSystemService cmpNewFiPreCheckService;

    /**
     * 数据升级预检 与平台工具对接
     *
     * @param params
     * @param response
     */
    @PostMapping("/check")
    @Authentication(value = false, readCookie = true)
    public void confirmPlaceOrder(@RequestBody CmpMigradeForSystemRequest params, HttpServletResponse response) throws Exception {
        renderJson(response, CtmJSONObject.toJSONString(cmpNewFiPreCheckService.cmpMigradeForSystemCheck(params)));
    }


    /**
     * 配置数据升迁(交易类型、特征等) 与平台对接
     *
     * @param params
     * @param response
     */
    @PostMapping("/updateconfig")
    @Authentication(value = false, readCookie = true)
    public void updateconfig(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, CtmJSONObject.toJSONString(cmpNewFiPreCheckService.updateconfig(params)));
    }

    /**
     * 业务数据升级与平台对接
     *
     * @param params
     * @param response
     */
    @PostMapping("/update")
    @Authentication(value = false, readCookie = true)
    public void update(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, CtmJSONObject.toJSONString(cmpNewFiPreCheckService.update(params)));
    }


}
