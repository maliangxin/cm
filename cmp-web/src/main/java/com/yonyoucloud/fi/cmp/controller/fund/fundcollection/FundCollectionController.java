package com.yonyoucloud.fi.cmp.controller.fund.fundcollection;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundcollection.service.FundCollectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @time：2023/3/20--21:08
 * @author：xuyao2 资金收款单控制层 --
 **/

@RestController
@RequestMapping("/fundcollection")
@Slf4j
public class FundCollectionController extends BaseController {
    @Autowired
    private FundCollectionService fundCollectionService;

    /**
     * 整单拒绝 资金付款单子表“是否委托驳回更新”字段为“是”，且结算状态更新为结算止付；
     */
    @PostMapping("/entrustReject")
    @ApplicationPermission("CM")
    public void entrustReject(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        fundCollectionService.entrustReject(params);
        renderJson(response, ResultMessage.success());
    }

    @PostMapping("/entrustRejectSub")
    @ApplicationPermission("CM")
    public void entrustRejectSub(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        fundCollectionService.entrustRejectSub(params);
        renderJson(response, ResultMessage.success());
    }
}
