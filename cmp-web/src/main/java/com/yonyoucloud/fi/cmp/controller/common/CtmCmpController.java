package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.api.IBillService;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/bill")
public class CtmCmpController extends BaseController {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CtmCmpController.class);
    @Autowired
    private IBillService billService;

    @RequestMapping("/voucherDo")
    public void voucherDo(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (bill.getData() != null) {
            if (StringUtils.isEmpty(bill.getAction())) {
                bill.setAction(request.getParameter("action"));
            }
            RuleExecuteResult ruleResult = billService.handle(bill);
            renderJson(response, ResultMessage.data(ruleResult.getData()));
        } else {
            renderJson(response, ResultMessage.error("no data"));
        }
    }
}
