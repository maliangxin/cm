package com.yonyoucloud.fi.cmp.controller.bankreceipt;


import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.api.openapi.BankReceiptCloudOpenApiService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * @author wangdengk
 * @Description: 银行对账单openAPI接口
 * @date 2023/08/07 14:54
 */
@RestController
@RequestMapping("/api/bankReceiptCloud/")
@Slf4j
public class BankReceiptCloudController extends BaseController {

    @Autowired
    private BankReceiptCloudOpenApiService service;

    /**
     * 银行对账单新增
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/save")
    @CMPDiworkPermission(IServicecodeConstant.CMPBANKRECONCILIATION)
    public Result save(@RequestBody CtmJSONObject param, HttpServletRequest request,
                       HttpServletResponse response) {

        try {
            HashMap<String, Object> map = service.saveBankCloudData(param);
            return Result.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180316","接口请求异常！") /* "接口请求异常！" */
                    + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180317","异常详细信息：") /* "异常详细信息：" */
                    + e.getMessage());
        }
    }


}
