package com.yonyoucloud.fi.cmp.controller.fund.foreignpayment;

import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.foreignpayment.service.ForeignPaymentService;
import com.yonyoucloud.fi.cmp.util.CheckMarxUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.yonyou.iuap.framework.sdk.common.utils.ResponseUtils.renderJson;

/**
 * <h1>ForeignPaymentController</h1>
 *
 * @author xuxbo
 * @version 1.0
 */
@Controller
@RequestMapping("/foreignpayment")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ForeignPaymentController {

    private final ForeignPaymentService foreignPaymentService;
    private final CmpBudgetCommonManagerService cmpBudgetCommonManagerService;

    /**
     * 查询银行类别code
     *
     * @param params
     * @param response
     */
    @RequestMapping("/queryBankCode")
    @CMPDiworkPermission({IServicecodeConstant.FOREIGNPAYMENT})
    public void queryBankCode(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(foreignPaymentService.queryBankCode(params)));
    }

    @PostMapping("/queryBudgetDetail")
    public void queryBudgetDetail(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject json = CtmJSONObject.parseObject(CheckMarxUtils.vaildLog(CtmJSONObject.toJSONString(param)));
        String result = cmpBudgetCommonManagerService.queryBudgetDetail(json);
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
        String result = foreignPaymentService.budgetCheckNew(cmpBudgetVO);
        renderJson(response, result);
    }

}
