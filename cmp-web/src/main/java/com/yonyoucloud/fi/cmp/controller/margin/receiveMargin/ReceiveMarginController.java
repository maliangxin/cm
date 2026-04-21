package com.yonyoucloud.fi.cmp.controller.margin.receiveMargin;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetPaymarginManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetReceivemarginManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.receivemargin.service.ReceiveMarginService;
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
 * @CreateTime: 2023-12-14  09:41
 * @Description: 收到保证金工作台相关Controller
 * @Version: 1.0
 */
@Controller
@RequestMapping("/receivemargin")
@Slf4j
public class ReceiveMarginController extends BaseController {

    @Autowired
    private ReceiveMarginService receiveMarginService;
    @Autowired
    private CmpBudgetReceivemarginManagerService cmpBudgetReceivemarginManagerService;

    /**
     * @description: 收到保证金最迟退还日期预警
     * @author: wenyuhao
     * @date: 2023/12/14 9:44
     * @param: [request, response, body]
     * @return: void
     **/
    @PostMapping("/latestReturnDateWarning")
    public void latestReturnDateWarning(HttpServletRequest request, HttpServletResponse response, @RequestBody CtmJSONObject body) throws Exception {
        String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
        String warnDays = Optional.ofNullable((String) body.get("warnDays")).orElse("");
        String accentity = Optional.ofNullable((String) body.get("accentity")).orElse("");
        Map<String, Object> result = receiveMarginService.latestReturnDateWarning(tenantId, warnDays, accentity);
        log.info("收到保证金最迟退还日期预警接口返回数据：{}", result);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    @PostMapping("/queryBudgetDetail")
    public void queryBudgetDetail(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject json = CtmJSONObject.parseObject(CheckMarxUtils.vaildLog(CtmJSONObject.toJSONString(param)));
        String result = cmpBudgetReceivemarginManagerService.queryBudgetDetail(json);
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
        String result = receiveMarginService.budgetCheckNew(cmpBudgetVO);
        renderJson(response, result);
    }
}
