package com.yonyoucloud.fi.cmp.controller.bankAccountInterestWithholding;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.interestratesetting.InterestRateSetting;
import com.yonyoucloud.fi.cmp.interestratesetting.service.InterestRateSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

/**
 * 银行利率设置相关接口*
 *
 * @author xuxbo
 * @date 2023/4/25 19:30
 */

@Controller
@RequestMapping("/cm/interestratesetting")
@RequiredArgsConstructor
@Slf4j
public class InterestRateSettingController extends BaseController {

    private final InterestRateSettingService interestRateSettingService;


    /**
     * 利率设置提交接口*
     *
     * @param interestRateSetting
     * @param response
     */
    @RequestMapping("mysave")
    public void interestRateSettingSave(@RequestBody InterestRateSetting interestRateSetting, HttpServletResponse response) throws Exception {
        CtmJSONObject result = interestRateSettingService.interestRateSettingSave(interestRateSetting);
        renderJson(response, ResultMessage.data(result));

    }

    /**
     * 协定存款利率设置提交接口*
     *
     * @param bill
     * @param response
     */
    @RequestMapping("myagreesave")
    public void agreeRateSettingSave(@RequestBody CtmJSONObject bill, HttpServletResponse response) throws Exception {
        CtmJSONObject result = interestRateSettingService.agreeRateSettingSave(bill);
        renderJson(response, ResultMessage.data(result));
    }

    /**
     * 协定存款利率设置详情提交接口*
     *
     * @param bill
     * @param response
     */
    @RequestMapping("myagreesavedetail")
    public void agreeRateSettingSavedetail(@RequestBody CtmJSONObject bill, HttpServletResponse response) throws Exception {
        RuleExecuteResult result = interestRateSettingService.agreeRateSettingSavedetail(bill);
        renderJson(response, ResultMessage.data(result));
    }


    /**
     * 协定存款利率设置批量校验详情提交接口*
     *
     * @param bill
     * @param response
     */
    @RequestMapping("agreeRateSettingSavedetailList")
    public void agreeRateSettingSavedetailList(@RequestBody CtmJSONObject bill, HttpServletResponse response) {
        try {
            interestRateSettingService.agreeRateSettingSavedetailList(bill);
            renderJson(response, ResultMessage.success());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error(e.getMessage()));
        }

    }

    /**
     * 协定存款利率设置删除接口*
     *
     * @param bill
     * @param response
     */
    @RequestMapping("myagreedelete")
    public void agreeRateSettingdelete(@RequestBody CtmJSONObject bill, HttpServletResponse response) throws Exception {
        RuleExecuteResult result = interestRateSettingService.agreeRateSettingdelete(bill);
        renderJson(response, ResultMessage.data(result));
    }
}
