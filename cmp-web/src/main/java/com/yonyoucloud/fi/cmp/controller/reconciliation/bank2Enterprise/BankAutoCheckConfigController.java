package com.yonyoucloud.fi.cmp.controller.reconciliation.bank2Enterprise;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoCheckConfigService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @description: 银行对账，自动对账设置相关接口
 * @author: wanxbo@yonyou.com
 * @date: 2022/11/10 19:00
 */
@Controller
@RequestMapping("/bankautocheckconfig")
@Slf4j
public class BankAutoCheckConfigController extends BaseController {

    @Resource
    private BankAutoCheckConfigService bankAutoCheckConfigService;

    /**
     * 查询自动对账方案设置信息
     */
    @PostMapping("queryConfigInfo")
    @CMPDiworkPermission({IServicecodeConstant.BANKRECONCILIATIONSETTING, IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH})
    public void queryConfigInfo(@RequestBody CtmJSONObject param , HttpServletResponse response)throws Exception{
            renderJson(response, ResultMessage.data(bankAutoCheckConfigService.queryConfigInfo(param)));
    }

    /**
     * 查询自动对账方案设置信息
     */
    @PostMapping("updateConfigInfo")
    @CMPDiworkPermission({IServicecodeConstant.BANKRECONCILIATIONSETTING, IServicecodeConstant.CMP_BANKVOUCHERCHECK_WORKBENCH})
    public void updateConfigInfo(@RequestBody CtmJSONObject param , HttpServletResponse response)throws Exception{
            renderJson(response, ResultMessage.data(bankAutoCheckConfigService.updateConfigInfo(param)));
    }
}
