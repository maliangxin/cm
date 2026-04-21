package com.yonyoucloud.fi.cmp.controller.reconciliation.bank2Enterprise;

import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.reconciliate.AutoBillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 银行对账-自动生单
 *
 * @author miaowb
 */
@Controller
@RequestMapping("/autobill")
@Slf4j
public class AutoBillController extends BaseController {

    @Autowired
    AutoBillService autoBillService;

    @RequestMapping("/autoGenerateBill")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void autoGenerateBill(@RequestBody JsonNode params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonNode object = autoBillService.autoGenerateBill(params);
        renderJson(response, ResultMessage.data(JSONBuilderUtils.jsonToMap(object)));
    }
}
