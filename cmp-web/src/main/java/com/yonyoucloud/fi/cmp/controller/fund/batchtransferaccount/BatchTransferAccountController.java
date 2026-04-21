package com.yonyoucloud.fi.cmp.controller.fund.batchtransferaccount;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.batchtransferaccount.service.BatchtransferaccountService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetBatchTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.util.CheckMarxUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description
 * @Author hanll
 * @Date 2025/11/25-20:02
 */
@RestController
@RequestMapping("/batchtransferaccount")
@Slf4j
public class BatchTransferAccountController extends BaseController {

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;
    @Autowired
    private CmpBudgetBatchTransferAccountManagerService cmpBudgetBatchTransferAccountManagerService;
    @Autowired
    private BatchtransferaccountService batchtransferaccountService;

    /**
     * 获取预算执行情况
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @PostMapping("/queryBudgetDetail")
    public void queryBudgetDetail(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject json = CtmJSONObject.parseObject(CheckMarxUtils.vaildLog(CtmJSONObject.toJSONString(param)));
        String result = cmpBudgetManagerService.queryBudgetDetail(json);
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
    public void budgetCheck(@RequestBody CmpBudgetVO cmpBudgetVO, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String result = cmpBudgetBatchTransferAccountManagerService.budgetCheckNew(cmpBudgetVO);
        renderJson(response, result);
    }

    /**
     * 交易类型发布菜单，校验serviceCode与实际交易类型是否匹配
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @PostMapping("checkAddTransType")
    public void checkAddTransType(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = batchtransferaccountService.checkAddTransType(param);
        renderJson(response, ResultMessage.data(result));
    }
}
