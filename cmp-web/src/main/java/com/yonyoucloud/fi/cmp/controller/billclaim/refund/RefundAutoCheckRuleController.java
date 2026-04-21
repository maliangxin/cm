package com.yonyoucloud.fi.cmp.controller.billclaim.refund;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankreconciliation.RefundAutoCheckRuleService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @description: 退票辨识规则查询相关接口
 * @author: wanxbo@yonyou.com
 * @date: 2023/2/6 15:16
 */

@Controller
@RequestMapping("/refundautocheckrule")
@Slf4j
public class RefundAutoCheckRuleController extends BaseController {

    @Resource
    private RefundAutoCheckRuleService refundAutoCheckRuleService;

    /**
     * 查询退票辨识规则
     */
    @PostMapping("queryRuleInfo")
    @CMPDiworkPermission(IServicecodeConstant.AUTOREFUNDCHECKRULE)
    @ApplicationPermission("CM")
    public void queryConfigInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(refundAutoCheckRuleService.queryRuleInfo(param)));
    }

    /**
     * 更新退票辨识规则信息
     */
    @PostMapping("updateRuleInfo")
    @CMPDiworkPermission(IServicecodeConstant.AUTOREFUNDCHECKRULE)
    public void updateConfigInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(refundAutoCheckRuleService.updateRuleInfo(param)));
    }
}
