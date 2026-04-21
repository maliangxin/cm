package com.yonyoucloud.fi.cmp.controller.cashwidgets;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.cashwidgets.CashQueryBalanceWidgetsForStwbService;
import com.yonyoucloud.fi.cmp.vo.cashwidgets.CashQueryBalanceWidgetsForStwbVo;
import com.yonyoucloud.fi.cmp.vo.cashwidgets.SettleWorkbenchBaseRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

/**
 * 现金组件控制
 */
@Slf4j
@Controller
@RequestMapping("/cmp/settleworkbench")
public class CashWidgetsController extends BaseController {

    @Autowired
    CashQueryBalanceWidgetsForStwbService cashQueryService;

    @RequestMapping("/cashmanage")
    public void cashManage(@RequestBody SettleWorkbenchBaseRequest settleWorkbenchBaseRequest, HttpServletResponse response) throws Exception {
        CashQueryBalanceWidgetsForStwbVo result = cashQueryService.cashQueryBalanceWidgetsForStwb(settleWorkbenchBaseRequest);
        renderJson(response, ResultMessage.data(result));
    }

}
