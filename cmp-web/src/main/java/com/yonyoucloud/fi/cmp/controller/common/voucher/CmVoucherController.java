package com.yonyoucloud.fi.cmp.controller.common.voucher;

import com.yonyou.diwork.permission.annotations.ApplicationPermission;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/cm/voucher")
@Slf4j
public class CmVoucherController extends BaseController {

    @Autowired
    private CmpVoucherService cmpVoucherService;

    @RequestMapping("/getid")
    @ApplicationPermission("CM")
    public void getVoucherId(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, cmpVoucherService.queryVoucherId(param));
    }
}
