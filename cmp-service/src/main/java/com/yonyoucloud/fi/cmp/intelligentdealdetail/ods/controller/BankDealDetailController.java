package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.controller;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.impl.BankDealDetailClearProcessImpl;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;

import static com.yonyou.iuap.framework.sdk.common.utils.ResponseUtils.renderJson;

@Controller
@Slf4j
@RequestMapping("/bankDealDetail")
public class BankDealDetailController {

    @Autowired
    private BankDealDetailClearProcessImpl bankDealDetailClearProcess;
    @GetMapping("/clearBankDealDetail")
    public void clearBankDealDetail(@RequestParam String  pubtsTime, HttpServletResponse response) {
        try {
            String openTeanant = AppContext.getEnvConfig("clear.BankDealDetail");
            if (StringUtils.isNotEmpty(openTeanant)){
                renderJson(response, ResultMessage.data(bankDealDetailClearProcess.clearBankDealDetail(pubtsTime)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, e.getMessage());
        }
    }
}
