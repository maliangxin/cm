package com.yonyoucloud.fi.cmp.controller.fund.fundpayment;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundpayment.service.FundPaymentService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.FundCommonQueryDataVo;
import com.yonyoucloud.fi.cmp.util.CheckMarxUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @time：2023/3/6--17:08
 * @author：yanglu 资金付款单控制层 --
 **/

@RestController
@RequestMapping("/fundpayment")
@Slf4j
public class FundpaymentController extends BaseController {

    @Resource
    private FundPaymentService fundPaymentService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    /**
     * 整单拒绝 资金付款单子表“是否委托驳回更新”字段为“是”，且结算状态更新为结算止付；
     */
    @PostMapping("/entrustReject")
    @ApplicationPermission("CM")
    public void entrustReject(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        fundPaymentService.entrustReject(params);
        renderJson(response, ResultMessage.success());
    }

    @PostMapping("/entrustRejectSub")
    @ApplicationPermission("CM")
    public void entrustRejectSub(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        fundPaymentService.entrustRejectSub(params);
        renderJson(response, ResultMessage.success());
    }

    @PostMapping("/queryBudgetDetail")
    public void queryBudgetDetail(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject json = CtmJSONObject.parseObject(CheckMarxUtils.vaildLog(CtmJSONObject.toJSONString(param)));
        String result = cmpBudgetManagerService.queryBudgetDetail(json);
        renderJson(response, result);
    }

    /**
     * <h2>退票重付</h2>
     *
     * @param param:
     * @param request:
     * @param response:
     * @author Sun GuoCai
     * @date 2023/11/18 10:45
     */
    @PostMapping("/refundAndRepayment")
    public void refundAndRepayment(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject json = CtmJSONObject.parseObject(CheckMarxUtils.vaildLog(CtmJSONObject.toJSONString(param)));
        String result = fundPaymentService.refundAndRepayment(json);
        renderJson(response, ResultMessage.success(result));
    }

    /**
     * <h2>资金收付款单提供RPC查询接口</h2>
     *
     * @param param:
     * @param request:
     * @param response:
     * @author Sun GuoCai
     * @date 2023/11/18 10:45
     */
    @PostMapping("/queryFundBillDataByParams")
    public void queryFundBillDataByParams(@RequestBody FundCommonQueryDataVo param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object result = fundPaymentService.queryFundBillDataByParams(param);
        renderJson(response, ResultMessage.data(result));
    }

    @PostMapping("/findBillNoById")
    public void findBillNoById(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = fundPaymentService.findBillNoById(param);
        renderJson(response, ResultMessage.data(map));
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
        String result = fundPaymentService.budgetCheckNew(cmpBudgetVO);
        renderJson(response, result);
    }
}
