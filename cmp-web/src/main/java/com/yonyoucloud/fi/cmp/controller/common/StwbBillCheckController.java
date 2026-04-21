package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.stwb.StwbBillCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @description: 国机专用：结算检查接口，用来校验相关风险
 * @author: wanxbo@yonyou.com
 * @date: 2023/5/16 10:42
 */
@Controller
@RequestMapping("/stwbbillcheck")
@Slf4j
public class StwbBillCheckController extends BaseController {

    @Resource
    private StwbBillCheckService stwbBillCheckService;

    /**
     * 结算风险检查
     *
     * @param param
     * @return
     * @throws Exception
     */
    @PostMapping("/billcheck")
    @ApplicationPermission("CM")
    public void billcheck(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, CtmJSONObject.toJSONString(stwbBillCheckService.billCheck(param)));
    }
}
