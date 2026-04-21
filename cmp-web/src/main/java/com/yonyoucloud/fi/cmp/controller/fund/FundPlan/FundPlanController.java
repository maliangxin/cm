package com.yonyoucloud.fi.cmp.controller.fund.FundPlan;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundPlanOccupancyDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

import static com.yonyou.iuap.framework.sdk.common.utils.ResponseUtils.renderJson;

@Controller
@RequestMapping("/fundPlan")
@RequiredArgsConstructor
@Slf4j
public class FundPlanController {


    @Autowired
    private FundPlanOccupancyDataService fundPlanOccupancyDataService;


    /**
     * 资金计划占用时提示信息
     *
     * @param params
     * @param response
     */
    @RequestMapping("/queryFundPlanOccupancyTips")
    @ApplicationPermission("CM")
    public void queryFundPlanOccupancyTips(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        renderJson(response, ResultMessage.data(fundPlanOccupancyDataService.createFundPlanOccupancyData(params)));
    }
}
