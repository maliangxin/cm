package com.yonyoucloud.fi.cmp.controller.currencyExchange;

import com.yonyou.diwork.permission.annotations.DiworkPermission;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.currencyexchange.service.CurrencyExchangeService;
import com.yonyoucloud.fi.cmp.util.CheckMarxUtils;
import com.yonyoucloud.fi.cmp.weekday.WeekdayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;


/**
 * @author sz@yonyou.com
 * @version 1.0
 */
@Controller
@RequestMapping("/currencyexchange")
@Authentication(value = false, readCookie = true)
@Slf4j
public class CurrencyExchangeController extends BaseController {

    @Autowired
    CurrencyExchangeService currencyExchangeService;
    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;
    @Autowired
    WeekdayServiceImpl weekdayService;

    private final static String CMP_CURRENCYEXCHANGE = "cmp_currencyexchange";

    @RequestMapping("/settle")
    public void settle(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (Objects.isNull(obj.get(ICmpConstant.DATA))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101773"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807C7", "请求参数不能为空!") /* "请求参数不能为空!" */);
        }
        String billnum = obj.getString(ICmpConstant.BILL_NUM);
        Json json = null;
        if (billnum != null && CMP_CURRENCYEXCHANGE.equals(billnum)) {
            json = new Json(CtmJSONObject.toJSONString(obj.getJSONObject(ICmpConstant.DATA)));
        } else {
            json = new Json(CtmJSONObject.toJSONString(obj.getJSONArray(ICmpConstant.DATA).toString()));
        }
        List<CurrencyExchange> currencyExchangeList = Objectlizer.decode(json, CurrencyExchange.ENTITY_NAME);
        CtmJSONObject result = currencyExchangeService.settle(currencyExchangeList);
        renderJson(response, ResultMessage.data(result));
    }

    @RequestMapping("/unsettle")
    public void unSettle(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (Objects.isNull(obj.get(ICmpConstant.DATA))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101773"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807C7", "请求参数不能为空!") /* "请求参数不能为空!" */);
        }
        String billnum = obj.getString(ICmpConstant.BILL_NUM);
        Json json = null;
        if (billnum != null && CMP_CURRENCYEXCHANGE.equals(billnum)) {
            json = new Json(CtmJSONObject.toJSONString(obj.getJSONObject(ICmpConstant.DATA)));
        } else {
            json = new Json(CtmJSONObject.toJSONString(obj.getJSONArray(ICmpConstant.DATA)));
        }
        List<CurrencyExchange> currencyExchangeList = Objectlizer.decode(json, CurrencyExchange.ENTITY_NAME);
        CtmJSONObject result = currencyExchangeService.unSettle(currencyExchangeList);
        renderJson(response, ResultMessage.data(result));
    }

    @RequestMapping("/batchaudit")
    public void receiveBillSp(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Json json = new Json(CtmJSONObject.toJSONString(obj.getJSONArray(ICmpConstant.DATA)));
        List<CurrencyExchange> currencyExchangeList = Objectlizer.decode(json, CurrencyExchange.ENTITY_NAME);
        CtmJSONObject result = currencyExchangeService.audit(currencyExchangeList);
        renderJson(response, ResultMessage.data(result));
    }

    @RequestMapping("/batchunaudit")
    public void receiveBillQxsp(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Json json = new Json(CtmJSONObject.toJSONString(obj.getJSONArray(ICmpConstant.DATA)));
        List<CurrencyExchange> currencyExchangeList = Objectlizer.decode(json, CurrencyExchange.ENTITY_NAME);
        CtmJSONObject result = currencyExchangeService.unAudit(currencyExchangeList);
        renderJson(response, ResultMessage.data(result));
    }

    @RequestMapping("/audit")
    public void receiveBillBodySp(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Json json = new Json(CtmJSONObject.toJSONString(obj.getJSONObject(ICmpConstant.DATA)));
        List<CurrencyExchange> currencyExchangeList = Objectlizer.decode(json, CurrencyExchange.ENTITY_NAME);
        CtmJSONObject result = currencyExchangeService.audit(currencyExchangeList);
        renderJson(response, ResultMessage.data(result.get(ICmpConstant.MSG)));
    }

    @RequestMapping("/unaudit")
    public void receiveBillBodyQxsp(@RequestBody CtmJSONObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Json json = new Json(CtmJSONObject.toJSONString(obj.getJSONObject(ICmpConstant.DATA)));
        List<CurrencyExchange> currencyExchangeList = Objectlizer.decode(json, CurrencyExchange.ENTITY_NAME);
        CtmJSONObject result = currencyExchangeService.unAudit(currencyExchangeList);
        renderJson(response, ResultMessage.data(result.get(ICmpConstant.MSG)));
    }

    /**
     * 获取用户自定义类型(汇率类型)
     *
     * @param param
     * @param response
     */
    @PostMapping("getRateType")
    @DiworkPermission({IServicecodeConstant.INVENTORY, IServicecodeConstant.TRANSFERACCOUNT, IServicecodeConstant.CURRENCYEXCHANGE, IServicecodeConstant.FUNDCOLLECTION, IServicecodeConstant.FUNDPAYMENT, IServicecodeConstant.PAYMENTBILL,
            IServicecodeConstant.RECEIVEBILL, IServicecodeConstant.SALARYPAY, IServicecodeConstant.PAYMARGIN, IServicecodeConstant.RECEIVEMARGIN, IServicecodeConstant.FOREIGNPAYMENT})
    public void getRateType(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, currencyExchangeService.getRateType(param));
    }

    /**
     * 获取用户自定义类型(汇率类型)
     *
     * @param param
     * @param response
     */
    @PostMapping("getRateTypeByFundBill")
    @DiworkPermission({IServicecodeConstant.FUNDCOLLECTION, IServicecodeConstant.FUNDPAYMENT})
    public void getRateTypeByFundBill(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, currencyExchangeService.getRateTypeByFundBill(param));
    }

    /**
     * 即期结售汇交易提交SSFE1002
     *
     * @param param
     * @param response
     * @auth lidchn
     */
    @PostMapping("currencyExchangeSubmit")
    @CMPDiworkPermission(IServicecodeConstant.CURRENCYEXCHANGE)
    public void currencyExchangeSubmit(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = currencyExchangeService.currencyExchangeSubmit(param);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 即期结售汇交割SSFE1003
     *
     * @param param
     * @param response
     * @auth lidchn
     */
    @PostMapping("currencyExchangeDelivery")
    @CMPDiworkPermission(IServicecodeConstant.CURRENCYEXCHANGE)
    public void currencyExchangeDelivery(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = currencyExchangeService.currencyExchangeDelivery(param);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 结售汇交易结果查询SSFE3001
     *
     * @param param
     * @param response
     * @auth lidchn
     */
    @PostMapping("currencyExchangeResultQuery")
    @CMPDiworkPermission(IServicecodeConstant.CURRENCYEXCHANGE)
    public void currencyExchangeResultQuery(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = currencyExchangeService.currencyExchangeResultQuery(param);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 即期结售汇询价SSFE1001
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @PostMapping("currencyExchangeRateQuery")
    @CMPDiworkPermission(IServicecodeConstant.CURRENCYEXCHANGE)
    public void currencyExchangeInquiry(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = currencyExchangeService.currencyExchangeRateQuery(param);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 工作日查询
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @PostMapping("currencyExchangeWeekdayQuery")
    @CMPDiworkPermission(IServicecodeConstant.CURRENCYEXCHANGE)
    public void currencyExchangeWeekdayQuery(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = weekdayService.getWorkday(param);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 汇率查询接口，SSFE3012
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @PostMapping("financeCompanyRateQuery")
    @CMPDiworkPermission(IServicecodeConstant.CURRENCYEXCHANGE)
    public void financeCompanyRateQuery(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = currencyExchangeService.financeCompanyRateQuery(param);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 交易类型发布菜单，校验serviceCode与实际交易类型是否匹配
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @PostMapping("checkAddTransType")
    @CMPDiworkPermission(IServicecodeConstant.CURRENCYEXCHANGE)
    public void checkAddTransType(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = currencyExchangeService.checkAddTransType(param);
        renderJson(response, ResultMessage.data(result));
    }

    @PostMapping("/queryBudgetDetail")
    public void queryBudgetDetail(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) {
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
        String result = currencyExchangeService.budgetCheckNew(cmpBudgetVO);
        renderJson(response, result);
    }

}
