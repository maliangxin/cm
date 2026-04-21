package com.yonyoucloud.fi.cmp.controller.fund.InwardRemittance;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.inwardremittance.InwardRemittanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

/**
 * @author lidchn
 */
@Controller
@RequestMapping("/inwardremittance")
@Authentication(value = false, readCookie = true)
@Slf4j
public class InwardRemittanceController extends BaseController {

    @Autowired
    InwardRemittanceService inwardRemittanceService;

    /**
     * 汇入汇款确认提交SSFE1004
     *
     * @param param
     * @param response
     * @auth lidchn
     */
    @PostMapping("inwardRemittanceSubmit")
    public void currencyExchangeSubmit(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        inwardRemittanceService.inwardRemittanceSubmit(param);
        renderJson(response, String.format(ResultMessage.data(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001EE", "确认成功") /* "确认成功" */)));
    }

    /**
     * 汇入汇款确认交易结果查询SSFE3004
     *
     * @param param
     * @param response
     * @auth lidchn
     */
    @PostMapping("inwardRemittanceResultQuery")
    public void inwardRemittanceResultQuery(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        inwardRemittanceService.inwardRemittanceResultQuery(param);
        renderJson(response, String.format(ResultMessage.data(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001EF", "查询成功") /* "查询成功" */)));
    }

    /**
     * 汇入汇款待确认业务列表查询SSFE3005
     *
     * @param param
     * @param response
     * @auth lidchn
     */
    @PostMapping("inwardRemittanceListQuery")
    public void inwardRemittanceListQuery(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        inwardRemittanceService.inwardRemittanceListQuery(param);
        renderJson(response, String.format(ResultMessage.data(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001EF", "查询成功") /* "查询成功" */)));
    }

    /**
     * 汇入汇款子表查询
     *
     * @param param
     * @param response
     * @auth lidchn
     */
    @PostMapping("inwardRemittance_b")
    @CMPDiworkPermission(IServicecodeConstant.INWARD_REMITTANCE)
    public void inwardRemittancebDetail(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = inwardRemittanceService.inwardRemittance_b(param);
        renderJson(response, ResultMessage.data(result));
    }
}
