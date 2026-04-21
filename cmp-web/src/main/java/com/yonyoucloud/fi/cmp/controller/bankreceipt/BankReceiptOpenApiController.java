package com.yonyoucloud.fi.cmp.controller.bankreceipt;


import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.openapi.service.BankElectronicOpenApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author jpk
 * @Description: 银行交易回单api接口
 * @date 2023/07/31 14:54
 */
@RestController
@RequestMapping("/api/bankelectronicreceipt/")
@Slf4j
public class BankReceiptOpenApiController extends BaseController {

    @Autowired
    private BankElectronicOpenApiService service;


    /**
     * 银行对账单查询
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/querylist")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public CtmJSONObject querylist(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            return service.querylist(param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            CtmJSONObject result = new CtmJSONObject();
            result.put("code", 999);
            result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862FE3C05D0001D", "服务端逻辑异常：") /* "服务端逻辑异常：" */ + e.getMessage());
            return result;
        }
    }


}
