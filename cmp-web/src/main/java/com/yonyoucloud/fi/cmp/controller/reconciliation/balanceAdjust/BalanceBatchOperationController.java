package com.yonyoucloud.fi.cmp.controller.reconciliation.balanceAdjust;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.balanceadjust.service.impl.BalanceBatchOperationService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/balanceBatchOperation")
@Authentication(value = false, readCookie = true)
@Slf4j
public class BalanceBatchOperationController extends BaseController {


    @Resource
    private BalanceBatchOperationService balanceBatchOperationService;


    /**
     * 查询待生成余额调节表接口
     *
     * @param params   入参集合
     * @param request
     * @param response
     */
    @RequestMapping("/queryBatchConfirmedBalances")
    public void queryBatchConfirmedBalances(@RequestBody List<BizObject> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<CtmJSONObject> jSONObjects = null;
        renderJson(response, ResultMessage.data(jSONObjects));
    }


    /**
     * 批量生成余额调节表接口
     *
     * @param data
     * @param request
     * @param response
     */
    @RequestMapping("/saveBatchBalances")
    public void saveBatchBalances(@RequestBody CtmJSONObject data, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject ctmJSONObjects = balanceBatchOperationService.saveBatchBalances(data);
        renderJson(response, ResultMessage.data(ctmJSONObjects));
    }


    /**
     * 查询接口(排序)接口
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/queryBatchBalances")
    public void queryBatchBalances(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject jSONObject = balanceBatchOperationService.queryBatchBalances(params);
        renderJson(response, ResultMessage.data(jSONObject));
    }
}
