package com.yonyoucloud.fi.cmp.controller.fundexpense;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.fundexpense.service.FundexpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

/**
 * 资金费用相关接口*
 */

@Controller
@RequestMapping("/fundexpense")
@RequiredArgsConstructor
@Slf4j
public class FundExpenseController extends BaseController {

    private final FundexpenseService fundexpenseService;

    @PostMapping("queryExchangeRate")
    public void queryExchangeRate(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        if (param.getString("expensenatCurrency") != null && param.getString("natCurrency") != null) {
            result = fundexpenseService.queryExchangeRate(param);
        }
        renderJson(response, ResultMessage.data(result));
    }
}
