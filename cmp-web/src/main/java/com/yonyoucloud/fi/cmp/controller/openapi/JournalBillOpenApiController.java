package com.yonyoucloud.fi.cmp.controller.openapi;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.journalbill.service.JournalBillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/api/journalbill")
@Slf4j
public class JournalBillOpenApiController extends BaseController {

    @Autowired
    private JournalBillService journalBillService;

    @RequestMapping("/batchSave")
    public void batchSaveOrSubmit(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        try {
            renderJson(response, ResultMessage.data(journalBillService.batchSaveOrUpdateForOpenApi(param)));
        } catch (Exception e) {
            log.error("bill process error， error message is：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001F7", "现金管理生成单据异常：") /* "现金管理生成单据异常：" */ + e.getMessage()));
        }
    }

    @RequestMapping("/batchDelete")
    public void batchDelete(@RequestBody CtmJSONObject param, HttpServletResponse response) {
        try {
            renderJson(response, journalBillService.batchDeleteForOpenApi(param));
        } catch (Exception e) {
            log.error("bill process error， error message is：" + e.getMessage(), e);
            renderJson(response, ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21C9A02804880002", "现金管理删除单据异常：") /* "现金管理删除单据异常：" */ + e.getMessage()));
        }
    }

}
