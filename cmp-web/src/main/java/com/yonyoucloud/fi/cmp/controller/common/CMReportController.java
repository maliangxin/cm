package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.ifreconciliation.IFReconciliationService;
import com.yonyoucloud.fi.cmp.report.ICMReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Controller
@RequestMapping("/cm/report")
public class CMReportController extends BaseController {

    @Autowired
    ICMReportService reportService;

    @Autowired
    IFReconciliationService ifReconciliationService;

    /**
     * 查询币种列表
     *
     * @param request
     * @param response
     */
    @RequestMapping("/getcurrency")
    @Authentication(false)
    public void getCurrency(HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(reportService.getCurrenctData()));
    }

    /**
     * 查询资金组织本位币币种
     *
     * @param request
     * @param response
     */
    @RequestMapping("/getOwnerCurrency")
    @Authentication(false)
    public void getOwnerCurrency(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(reportService.getOwnerCurrency(params)));
    }


    /**
     * 查询当日汇总数据
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/getsummary")
    @Authentication(false)
    public void getSummaryData(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Date date = params.getDate("date");
        String accentity = params.getString("accentity");
        String currency = params.getString("currency");
        CtmJSONObject summaryData = reportService.getSummaryData(accentity, currency, date);
        renderJson(response, ResultMessage.data(summaryData));
    }

    /**
     * 查询当日现金，银行存款分类汇总额
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/getaccountsum")
    @Authentication(false)
    public void getAccountSum(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Date date = params.getDate("date");
        String accentity = params.getString("accentity");
        String currency = params.getString("currency");
        CtmJSONArray accountSum = reportService.getAccountSum(accentity, currency, date);
        renderJson(response, ResultMessage.data(accountSum));
    }


    /**
     * 根据分类查询账户列表
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/getaccountlist")
    @Authentication(false)
    public void getAccountList(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(reportService.getAccountList(params)));
    }


    /**
     * 查询账户收支详情
     *
     * @param params
     * @param request
     * @param response
     */
    @RequestMapping("/getaccountdetail")
    @Authentication(false)
    public void getAccountDetail(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(reportService.getAccountDetail(params)));
    }
}
