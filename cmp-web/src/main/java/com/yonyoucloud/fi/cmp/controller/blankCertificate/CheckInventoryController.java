package com.yonyoucloud.fi.cmp.controller.blankCertificate;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.diwork.permission.annotations.DiworkPermission;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.checkinventory.service.CheckInventoryService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;


/**
 * 支票盘点
 *
 * @author
 */
@RestController
@Slf4j
@RequestMapping("/checkInventory")
public class CheckInventoryController extends BaseController {

    @Autowired
    CheckInventoryService checkInventoryService;

    /**
     * 获取盘点信息
     */
    @PostMapping("/getInfo")
    @DiworkPermission(IServicecodeConstant.CHECKINVENTORY)
    public void getCheckInventoryInfo(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        renderJson(response, ResultMessage.data(checkInventoryService.getCheckInventoryInfo(param)));
    }
}
