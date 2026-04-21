package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankenterprise.InternetBankPayService;
import com.yonyoucloud.fi.cmp.util.CacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

/**
 * @ClassName BankEnterpriseController
 * @Desc 银企联controller
 * @Author tongyd
 * @Date 2019/9/12
 * @Version 1.0
 */
@Controller
@RequestMapping("/cm/bankEnterprise/")
@Slf4j
public class BankEnterpriseController extends BaseController {

    @Autowired
    private InternetBankPayService internetBankPayService;

    @PostMapping(value = "batchPayPreOrder")
    public void batchPayPreOrder(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        try {
            renderJson(response, internetBankPayService.batchPayPreOrder(param));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, e.getMessage());
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("row"));
        }
    }

    @PostMapping(value = "preOrderTransactionConfirm")
    public void preOrderTransactionConfirm(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        try {
            renderJson(response, internetBankPayService.preOrderTransactionConfirm(param));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, e.getMessage());
        } finally {
            CacheUtils.unlockBill(param.getJSONArray("row"));
        }
    }

    @PostMapping(value = "batchPayDetailStatusQuery")
    public void batchPayDetailStatusQuery(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, internetBankPayService.batchPayDetailStatusQuery(param));
    }

}
