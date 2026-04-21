package com.yonyoucloud.fi.cmp.controller.margin.payMargin;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetPaymarginManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetSalarypayManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.paymargin.service.PayMarginService;
import com.yonyoucloud.fi.cmp.util.CheckMarxUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 * @BelongsProject: ctm-cmp
 * @BelongsPackage: com.yonyoucloud.fi.cmp.controller
 * @Author: wenyuhao
 * @CreateTime: 2023-12-13  14:22
 * @Description: 支付保证金工作台相关Controller
 * @Version: 1.0
 */
@Controller
@RequestMapping("/paymargin")
@Slf4j
public class PayMarginController extends BaseController {

    @Autowired
    private PayMarginService payMarginService;
    @Autowired
    private CmpBudgetPaymarginManagerService cmpBudgetPaymarginManagerService;

    /**
     * @description: 支付保证金预计收回日期预警
     * @author: wenyuhao
     * @date: 2023/12/13 15:10
     * @param: [request, response, body]
     * @return: void
     **/
    @PostMapping("/expectedRetrievalDateWarning")
    public void expectedRetrievalDateWarning(HttpServletRequest request, HttpServletResponse response, @RequestBody CtmJSONObject body) throws Exception {
        String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        String warnDays = Optional.ofNullable((String) body.get("warnDays")).orElse("");
        String accentity = Optional.ofNullable((String) body.get("accentity")).orElse("");
        Map<String, Object> result = payMarginService.expectedRetrievalDateWarning(tenantId,warnDays,accentity);
        log.info("支付保证金预计收回日期预警接口返回数据：{}", result);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }


    @PostMapping("/queryBudgetDetail")
    public void queryBudgetDetail(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject json = CtmJSONObject.parseObject(CheckMarxUtils.vaildLog(CtmJSONObject.toJSONString(param)));
        String result = cmpBudgetPaymarginManagerService.queryBudgetDetail(json);
        renderJson(response, result);
    }

    /**
     * 预算测算
     *
     * @param
     * @param response
     * @throws Exception
     */
    @PostMapping("/budgetCheck")
    public void budget(@RequestBody CmpBudgetVO cmpBudgetVO, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String result = payMarginService.budgetCheckNew(cmpBudgetVO);
        renderJson(response, result);
    }
}
