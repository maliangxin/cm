package com.yonyoucloud.fi.cmp.controller.dailySettlement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * @author zhaodzh@yonyou.com
 * @version 1.0
 */
@Controller
@RequestMapping("/settlement")
@Slf4j
public class SettlementController extends BaseController {

    @Autowired
    SettlementService settlementService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    /**
     * 根据会计主体查询会计期间及日结明细
     *
     * @param accentity
     * @param period
     * @param request
     * @param response
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping("/getSettlementPeriod")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void getSettlementPeriod(@RequestParam(value = "accentity") String accentity, @RequestParam(value = "period") String period, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject result = settlementService.getSettlementPeriod(accentity, period);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 获取日结检查项
     *
     * @param request
     * @param response
     */
    @ResponseBody
    @RequestMapping("/getCheckItem")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void getCheckItem(
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Map<String, Object>> checkItemList = settlementService.getCheckItem();
        renderJson(response, ResultMessage.data(checkItemList));
    }


    /**
     * 获取默认会计主体
     *
     * @param request
     * @param response
     */
    @ResponseBody
    @RequestMapping("/getDefaultAccentity")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void getDefaultAccentity(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = settlementService.getDefaultAccentity();
        renderJson(response, ResultMessage.data(map));
    }


    /**
     * 日结检查结果
     *
     * @param
     * @param request
     * @param response
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping("/settleCheck")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void settleCheck(@RequestParam(value = "accentity") String accentity, @RequestParam(value = "period") String period,
                            @RequestParam(value = "settleFlag", required = false) String settleFlag, HttpServletRequest request, HttpServletResponse response) throws Exception {
        settlementService.getPeriodByAccentity(accentity);//获取会计主体对应期间，若获取异常则抛出对应异常，主要是用于校验
        CtmJSONObject result = settlementService.settleCheck(accentity, period, false, settleFlag);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 查询日结检查结果
     *
     * @param
     * @param request
     * @param response
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping("/querySettleCheck")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void querySettleCheck(@RequestParam(value = "accentity") String accentity, @RequestParam(value = "period") String period, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // settlementService.getPeriodByAccentity(accentity);//获取会计主体对应期间，若获取异常则抛出对应异常，主要是用于校验
        CtmJSONObject result = settlementService.queryCheckResult(accentity, period);
        renderJson(response, ResultMessage.data(result));
    }

    /* */

    /**
     * 日结
     *
     * @param
     * @param request
     * @param response
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping("/settle")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void settle(@RequestParam(value = "accentity") String accentity, @RequestParam(value = "period") String period, HttpServletRequest request, HttpServletResponse response) throws Exception {
        settlementService.getPeriodByAccentity(accentity);
        CtmJSONObject result = settlementService.settle(accentity, period, false);
        ctmcmpBusinessLogService.saveBusinessLog(result, "", "", IServicecodeConstant.DAYSETTLE, IMsgConstant.DAY_SETTLE, IMsgConstant.CLOSING);
        renderJson(response, ResultMessage.data(result));
    }


    /**
     * 定时日结 执行日结
     *
     * @param params
     * @param request
     * @param response
     */
    @ResponseBody
    @RequestMapping("/autoDailySettle")
    public CtmJSONObject autoDailySettle(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) {
        try {
            String autoSettleResult = settlementService.autoDailySettle(params);
            return CtmJSONObject.parseObject(autoSettleResult);
        } catch (Exception e) {
            CtmJSONObject retObj = new CtmJSONObject();
            retObj.put("errInfo", e.getMessage());
            return retObj;
        }
    }

    /**
     * 定时日结  获取最大结账日
     *
     * @param params
     * @param request
     * @param response
     * @return
     */
    @ResponseBody
    @RequestMapping("/getMaxSettleDate")
    public CtmJSONObject getMaxSettleDate(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) {
        CtmJSONObject retObj = new CtmJSONObject();
        try {
            String virtualaccbody = params.getString("virtualaccbody");
            Date settleDate = settlementService.getMaxSettleDate(virtualaccbody);
            if (settleDate == null) {
                settleDate = settlementService.getMinUnSettleDate(virtualaccbody);
            }
            if (settleDate == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102173"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418061F", "查无当前会计主体的结账日") /* "查无当前会计主体的结账日" */);
            }
            CtmJSONObject dataObj = new CtmJSONObject();
            dataObj.put("accperiod", DateUtils.dateFormat(settleDate, DateUtils.MONTH_PATTERN));
            retObj.put("data", dataObj);
            retObj.put("success", true);
            retObj.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180624", "查询成功！") /* "查询成功！" */);
            return retObj;
        } catch (Exception e) {
            log.error("getMaxSettleDate-excepiton:" + e);
            retObj.put("success", false);
            retObj.put("message", e.getMessage());
            return retObj;
        }
    }

    /* */

    /**
     * 取消日结
     *
     * @param
     * @param request
     * @param response
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping("/unsettle")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void unsettle(@RequestParam(value = "accentity") String accentity, @RequestParam(value = "period") String period,
                         HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject result = settlementService.unsettle(accentity, period);
        renderJson(response, ResultMessage.data(result));
    }


    /**
     * 总账是否日结检查
     */
    @ResponseBody
    @RequestMapping("/getCheckResult")
    public void getCheckResult(@RequestParam(value = "pk_org") String pk_org, @RequestParam(value = "accperiod") String accperiod, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject json = settlementService.getCheckResult(pk_org, accperiod);
        renderJson(response, CtmJSONObject.toJSONString(json));
    }

    /**
     * 总账判断是否引用会计账簿
     * 业务单元-期初期间取消时调用业务账簿是否引用
     *
     * @param accbookIds
     * @return
     */
    @ResponseBody
    @RequestMapping("/hasRefAccbook")
    public void hasRefAccbook(@RequestBody List<String> accbookIds, HttpServletResponse response) throws Exception {
        CtmJSONObject json = new CtmJSONObject();
        List<String> list = settlementService.hasRefAccbook(accbookIds);
        json.put("code", 200);
        json.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180625", "查询启用账簿集合成功") /* "查询启用账簿集合成功" */);
        json.put("data", list);
        json.put("success", true);
        renderJson(response, CtmJSONObject.toJSONString(json));
    }


    /**
     * 期初改造日结数据升级-全体租户
     *
     * @param params
     * @param request
     * @param response
     * @throws Exception 核心1：约10w
     *                   核心2：2139488
     *                   核心3：约20w
     *                   核心4:约12w
     */
    @RequestMapping("/upgradeDailySettle")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void upgradeDailySettle(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        settlementService.upgradeDailySettle(params);
        renderJson(response, ResultMessage.data(null));
    }

    /**
     * 期初数据升级-当前租户
     *
     * @param params
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/upgradeDailySettleCurrent")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void upgradeDailySettleCurrent(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject resultData = settlementService.upgradeDailySettleCurrent();
        renderJson(response, ResultMessage.data(resultData));
    }

    /**
     * 日结页面获会计主体权限
     *
     * @param params
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/getSettlementlistRefData")
    @CMPDiworkPermission(IServicecodeConstant.DAYSETTLE)
    public void getSettlementlistRefData(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject resultData = settlementService.getSettlementlistRefData();
        renderJson(response, ResultMessage.data(resultData));
    }
}
