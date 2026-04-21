package com.yonyoucloud.fi.cmp.controller.openapi;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.service.ForeignPaymentOpenApiService;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/api/foreignpayment")
@Slf4j
public class ForeignPaymentOpenApiController extends BaseController {

    @Autowired
    private ForeignPaymentOpenApiService foreignPaymentOpenApiService;

    @RequestMapping("/queryBillByIdOrCode")
    public void queryFundBillByIdOrCode(@RequestParam() String billNum,
                                        @RequestParam(required = false) Long id,
                                        @RequestParam(required = false) String code,
                                        HttpServletResponse response) {
        try {
            if (!ValueUtils.isNotEmptyObj(id) && !ValueUtils.isNotEmptyObj(code)) {
                renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180183", "单据id或单据编码不能为空") /* "单据id或单据编码不能为空" */));
                return;
            }
            if (ValueUtils.isNotEmptyObj(id) && ValueUtils.isNotEmptyObj(code)) {
                renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180185", "单据id或单据编码不能同时传递") /* "单据id或单据编码不能同时传递" */));
                return;
            }
            String data = foreignPaymentOpenApiService.queryBillByIdOrCode(billNum, id, code);
            renderJson(response, data);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, e.getMessage());
        }
    }

    @PostMapping("/deleteBillByIds")
    @CMPDiworkPermission({IServicecodeConstant.FOREIGNPAYMENT})
    public void deleteBillByIds(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = foreignPaymentOpenApiService.deleteBillByIds(param);
        renderJson(response, ResultMessage.data(jsonObject));
    }

}
