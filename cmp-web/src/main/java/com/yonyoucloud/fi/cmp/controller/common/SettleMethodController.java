package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.diwork.permission.annotations.DiworkPermission;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.common.service.SettleMethodService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @BelongsProject: ctm-cmp
 * @BelongsPackage: com.yonyoucloud.fi.cmp.controller.common
 * @Author: wenyuhao
 * @CreateTime: 2024-01-11  09:17
 * @Description: 结算方式公共接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/settleMethod")
@Slf4j
public class SettleMethodController extends BaseController {

    @Autowired
    private SettleMethodService settlementService;

    /**
     * @description: 判断结算方式是否由非直连的银行转账变为直连银行转账，是则返回true
     * @author: wenyuhao
     * @date: 2024/1/10 16:50
     * @param: [param, request, response]
     * @return: void
     **/
    @PostMapping("/checkSettleMethodCleanPayBankAccount")
    @DiworkPermission({IServicecodeConstant.TRANSFERACCOUNT, IServicecodeConstant.FOREIGNPAYMENT})
    public void checkSettleMethodCleanPayBankAccount(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.error(String.format(">>>>>checkSettleMethodCleanPayBankAccount接口入参：{}", param.toString()));
        Boolean result = settlementService.checkSettleMethodCleanPayBankAccount(param);
        log.error(String.format(">>>>>checkSettleMethodCleanPayBankAccount接口返回结果：{}", result.toString()));
        renderJson(response, result.toString());
    }
}
