package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationAutomaticStayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * @Author zhangxiaojun
 * @Date 2022/10/10 15:14
 */

@RestController
@RequestMapping("/bankreconciliationAuto")
@Slf4j
public class BankreconciliationAutomaticStayController extends BaseController {

    @Autowired
    BankreconciliationAutomaticStayService automaticStayService;

    /**
     * 银行对账单自动冻结
     */
    @PostMapping("/automaticStay")
    public void automaticStay(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject params = new CtmJSONObject();
        //获取自动任务参数
        params.put("tenantId", Optional.ofNullable(request.getHeader("tenantId")).orElse(""));
        params.put("userId", Optional.ofNullable(request.getHeader("userId")).orElse(""));
        params.put("logId", Optional.ofNullable(request.getHeader("logId")).orElse(""));
        params.put("maxQueryNum", param.get("maxQueryNum"));
        params.put("singleProcessNum", param.get("singleProcessNum"));
        CtmJSONObject object = automaticStayService.automaticStay(params);
        renderJson(response, ResultMessage.data(object));
    }
}
