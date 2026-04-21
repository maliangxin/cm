package com.yonyoucloud.fi.cmp.controller.bankreconciliation;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankreceipt.CtmCmpBankReceiptRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.QueryReceiptByRelationBillInputDTO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.dto.QueryReceiptByRelationBillReturnDTO;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 银行对账单，web入口
 * author：lidchn
 * 2024年7月8日11:03:59
 */
@Controller
@RequestMapping("/bankReconciliation")
@Slf4j
@Lazy
public class BankReconciliationController extends BaseController {

    @Autowired
    BankreconciliationService bankreconciliationService;
    @Autowired
    CtmCmpBankReceiptRpcService ctmCmpBankReceiptRpcService;

    /**
     * 根据业务单据，查询关联的电子回单信息，用于展示回单附件
     *
     * @param queryReceiptByRelationBillInputDTO 业务单据查询条件，包含业务单据id，主组织id(非必填)，单据类型id
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @throws Exception 业务处理异常
     */
    @RequestMapping("/queryReceiptByRelationBill")
    public void queryReceiptByRelationBill(@RequestBody @Valid QueryReceiptByRelationBillInputDTO queryReceiptByRelationBillInputDTO, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 调用银行对账服务查询关联的电子回单信息
        QueryReceiptByRelationBillReturnDTO queryReceiptByRelationBillReturnDTO = ctmCmpBankReceiptRpcService.queryReceiptByRelationBill(queryReceiptByRelationBillInputDTO);
        // 将查询结果转换为JSON格式并输出到响应中
        renderJson(response, CtmJSONObject.toJSONString(queryReceiptByRelationBillReturnDTO));
    }


    /**
     * 查询子表数据
     *
     * @param
     * @param request
     * @param response
     */
    @RequestMapping("/querySub")
    public void BankTradeDetailElectronList(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (param == null || param.get("id") == null) {
            return;
        }
        String id = param.get("id").toString();
        Map<String, Object> result = bankreconciliationService.querySub(id);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    @RequestMapping("/queryById")
    public void queryById(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (param == null || param.get("id") == null) {
            return;
        }
        Long id = Long.parseLong(param.get("id").toString());
        Map<String, Object> result = bankreconciliationService.queryById(id);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    @RequestMapping("/queryByIds")
    public void queryById(@RequestBody List<CtmJSONObject> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (CollectionUtils.isEmpty(params)) {
            return;
        }
        List<Long> ids = params.stream().map(item -> Long.parseLong(item.get("id").toString())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        List<BankReconciliation> result = bankreconciliationService.queryByIds(ids);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }

    /**
     * 查询批改需要过滤掉的字段
     *
     * @param params
     * @param response
     */
    @RequestMapping("/queryFilterFields")
    @CMPDiworkPermission({IServicecodeConstant.CMPBANKRECONCILIATION})
    public void queryFilterFields(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception{
        renderJson(response, ResultMessage.data(bankreconciliationService.queryFilterFields()));
    }

    /*@RequestMapping("/testSqlBuilder")
    public void testSqlBuilder(HttpServletRequest request, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(CommonSaveUtils.testSqlBuilder()));
        } catch (Exception e) {
            log.error("testSqlBuilder error:{}", e.getMessage(), e);
            renderJson(response, e.getMessage());
        }
    }*/
}
