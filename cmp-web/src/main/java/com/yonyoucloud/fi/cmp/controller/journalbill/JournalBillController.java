package com.yonyoucloud.fi.cmp.controller.journalbill;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.journalbill.service.JournalBillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/journalbill")
@Slf4j
public class JournalBillController  extends BaseController {

    @Autowired
    private JournalBillService journalBillService;

    /**
     * 交易类型发布菜单，校验serviceCode与实际交易类型是否匹配
     *
     * @param param
     * @param response
     * @throws Exception
     */
    @PostMapping("/checkAddTransType")
    public void checkAddTransType(@RequestBody CtmJSONObject param, HttpServletResponse response) throws Exception {
        CtmJSONObject result = journalBillService.checkAddTransType(param);
        renderJson(response, ResultMessage.data(result));
    }

}
