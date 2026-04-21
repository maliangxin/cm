package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.bankdealdetail.SwitchDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletResponse;

@Controller
public class ReceiptDetailSwitchController extends BaseController {

    @Autowired
    private SwitchDTO switchDTO;

    @PostMapping(value = "/switch/multi")
    public void switchTo(HttpServletResponse response) {
        switchDTO.setMultiThreads(!switchDTO.getMultiThreads());
        String result = "";
        if (switchDTO.getMultiThreads()) {
            result = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001A9", "当前是单线程运行") /* "当前是单线程运行" */;
        }else{
            result = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001AA", "当前是多线程运行") /* "当前是多线程运行" */;
        }
        renderJson(response, ResultMessage.data(result));
    }

    @PostMapping(value = "/get/multi/threads")
    public void switchGet(HttpServletResponse response) {
        renderJson(response, ResultMessage.data(switchDTO.getMultiThreads()));
    }

}
