package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.accountdetailexclusion.service.IAccountDetailExclusionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @ClassName AccountDetailExclusionController
 * @Description 账户收支明细剔除
 * @Author JPK
 * @Date 2023/11/07 10:44
 * @Version 1.0
 **/
@Controller
@Slf4j
public class AccountDetailExclusionController extends BaseController {

    @Autowired
    private IAccountDetailExclusionService accountDetailExclusionService;


    /**
     * 计算收入支出剔除总额
     *
     * @param param
     * @param request
     * @param response
     * @throws Exception
     */
    @PostMapping("/accountdetailexclusion/calculateExcludingAmount")
    public void calculateExcludingAmount(@RequestBody CtmJSONObject param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CtmJSONObject jsonObject = accountDetailExclusionService.calculateExcludingAmount(param, response);
        renderJson(response, ResultMessage.data(jsonObject));
    }

}
