package com.yonyoucloud.fi.cmp.controller.billclaim.autoGenerate;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.autoorderrule.AutoOrderRuleConfigService;
import com.yonyoucloud.fi.cmp.autoorderrule.AutoorderruleConfig;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @description:
 * @author: wanxbo@yonyou.com
 * @date: 2023/2/28 15:49
 */
@Controller
@RequestMapping("/autoorderruleconfig")
@Slf4j
public class AutoGenerateBillRuleConfigController extends BaseController {

    @Resource
    private AutoOrderRuleConfigService autoOrderRuleConfigService;

    /**
     * 查询自动生单规则 客户/供应商配置信息
     */
    @PostMapping("queryConfigInfo")
    @CMPDiworkPermission(IServicecodeConstant.AUTOORDERRULE)
    public void queryConfigInfo(@RequestBody JsonNode param, HttpServletResponse response) throws Exception {
        AutoorderruleConfig autoorderruleConfig = JSONBuilderUtils.jsonToBean(param, AutoorderruleConfig.class);
        Long mainid = param.get("mainid").asLong();
        if (mainid != null) {
            autoorderruleConfig.setMainid(mainid);
        }
        renderJson(response, ResultMessage.data(autoOrderRuleConfigService.queryConfigInfo(autoorderruleConfig)));
    }

    /**
     * 更新自动生单规则客户/供应商配置信息
     */
    @PostMapping("updateConfigInfo")
    @CMPDiworkPermission(IServicecodeConstant.AUTOORDERRULE)
    public void updateConfigInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(autoOrderRuleConfigService.updateConfigInfo(param)));
    }
}
