package com.yonyoucloud.fi.cmp.controller.reconciliation.bank2Enterprise;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.reconciliate.ReconciliateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/reconciliate")
public class ReconciliateController extends BaseController {

    private static final String JOURNALS = "journals";
    private static final String RECONCILIATION = "bankreconciliations";
    private static final String BANKRECONCILIATIONSCHEMECODE = "bankreconciliationschemecode";
    private static final String BANKRECONCILIATIONSCHEMENAME = "bankreconciliationschemename";
    @Autowired
    ReconciliateService reconciliateService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    /**
     * 自动勾对
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/automateTick")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void automateTick(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String paramStr = params == null ? "params为null" : CtmJSONObject.toJSONString(params);//@notranslate
        log.error("自动勾对记录参数：" + paramStr);
        CtmJSONObject reback = reconciliateService.automateTick(params);
        ctmcmpBusinessLogService.saveBusinessLog(params, "", "", IServicecodeConstant.BANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009F", "银企对账") /* "银企对账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D800A4", "自动对账") /* "自动对账" */);
        renderJson(response, ResultMessage.data(reback));
    }

    /**
     * 手动勾对
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/handTick")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void handTick(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String paramStr = params == null ? "params为null" : CtmJSONObject.toJSONString(params);//@notranslate
        log.error("手动勾对记录参数：" + paramStr);
        CtmJSONObject reback = reconciliateService.handTick(params);
        ctmcmpBusinessLogService.saveBusinessLog(params, "", "", IServicecodeConstant.BANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009F", "银企对账") /* "银企对账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D800A0", "手工对账") /* "手工对账" */);
        renderJson(response, ResultMessage.data(reback));
    }

    /**
     * 取消勾对
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/cancelTick")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void cancelTick(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String paramStr = params == null ? "params为null" : CtmJSONObject.toJSONString(params);//@notranslate
        log.error("取消勾对记录参数：" + paramStr);
        CtmJSONObject reback = reconciliateService.cancelTick(params);
        ctmcmpBusinessLogService.saveBusinessLog(params, "", "", IServicecodeConstant.BANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009F", "银企对账") /* "银企对账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D800A5", "取消对账") /* "取消对账" */);
        renderJson(response, ResultMessage.data(reback));
    }

    /**
     * 单边勾对
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/onesideTick")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void onesideTick(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String paramStr = params == null ? com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009E", "params为null") /* "params为null" */ : CtmJSONObject.toJSONString(params);
        log.error("单边勾对记录参数：" + paramStr);
        CtmJSONObject reback = reconciliateService.onesideTick(params);
        ctmcmpBusinessLogService.saveBusinessLog(params, "", "", IServicecodeConstant.BANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009F", "银企对账") /* "银企对账" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D800A1", "单边勾对") /* "单边勾对" */);
        renderJson(response, ResultMessage.data(reback));
    }

    /**
     * 校验对账截止日期
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/checkDzEndDate")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void checkDzEndDate(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String res = reconciliateService.checkDzEndDate(params);
        renderJson(response, res);
    }

    private CtmJSONObject parseParams(CtmJSONObject params) {
        params.put("code", ((Map) params.getJSONArray(JOURNALS).get(0)).get(BANKRECONCILIATIONSCHEMECODE));
        params.put("name", ((Map) params.getJSONArray(JOURNALS).get(0)).get(BANKRECONCILIATIONSCHEMENAME));
        return params;
    }

    /**
     * 对账封存
     */
    @RequestMapping("/seal")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    @ApplicationPermission("CM")
    public void seal(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String paramStr = params == null ? com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009E", "params为null") /* "params为null" */ : CtmJSONObject.toJSONString(params);
        log.error("对账封存记录参数：" + paramStr);
        CtmJSONObject reback = reconciliateService.seal(params);
        ctmcmpBusinessLogService.saveBusinessLog(params, "", "", IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D800A3", "对账封存") /* "对账封存" */);
        renderJson(response, ResultMessage.data(reback));
    }

    /**
     * 取消封存
     */
    @RequestMapping("/cancelSeal")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void cancelSeal(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String paramStr = params == null ? "params为null" : CtmJSONObject.toJSONString(params);//@notranslate
        log.error("取消封存记录参数：" + paramStr);
        CtmJSONObject reback = reconciliateService.cancelSeal(params);
        ctmcmpBusinessLogService.saveBusinessLog(params, "", "", IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D800A6", "取消封存") /* "取消封存" */);
        renderJson(response, ResultMessage.data(reback));
    }

    /**
     * 净额对账
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/netamountTick")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECONCILIATION)
    public void netamountTick(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String paramStr = params == null ? com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D8009E", "params为null") /* "params为null" */ : CtmJSONObject.toJSONString(params);
        log.error("净额对账记录参数：" + paramStr);
        CtmJSONObject reback = reconciliateService.netamountTick(params);
        ctmcmpBusinessLogService.saveBusinessLog(params, "", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D800A2", "净额对账") /* "净额对账" */, IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D800A2", "净额对账") /* "净额对账" */);
        renderJson(response, ResultMessage.data(reback));
    }

}
