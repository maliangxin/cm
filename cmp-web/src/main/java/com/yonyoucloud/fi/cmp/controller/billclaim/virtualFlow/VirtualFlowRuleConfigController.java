package com.yonyoucloud.fi.cmp.controller.billclaim.virtualFlow;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankcapitalvirtual.VirtualFlowRuleConfigService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;


/**
 * 新增一条虚拟流水
 *
 * @author
 */
@Controller
@Slf4j
@RequestMapping("/virtualflowconfig")
public class VirtualFlowRuleConfigController extends BaseController {

    @Autowired
    VirtualFlowRuleConfigService virtualFlowRuleConfigService;

    /**
     * 保存信息
     */
    @PostMapping("saveConfigInfo")
    @CMPDiworkPermission(IServicecodeConstant.VIRTUALFLOWRULECONFIGCARD)
    public void updateConfigInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(virtualFlowRuleConfigService.updateConfigInfo(param)));
    }
}
