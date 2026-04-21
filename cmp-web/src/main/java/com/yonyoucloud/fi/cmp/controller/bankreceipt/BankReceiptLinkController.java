package com.yonyoucloud.fi.cmp.controller.bankreceipt;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * desc:银行交易回单联查根据银行交易回单，如果传过来的银行对账单主键有值直接查询银行对账单关联的回单，如果没有根据6要素直接查询匹配的回单
 * 6要素：收付方向、本方银行账号、对方银行账号、对方户名、金额、摘要
 * author:wangqiangac
 * date:2023/5/19 11:35
 */
@RestController
@RequestMapping("/receipt")
@Slf4j
public class BankReceiptLinkController extends BaseController {
    @Autowired
    private BankReceiptLinkService bankReceiptLinkService;

    @PostMapping("/queryId")
    public void queryId(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Map<String, Object>> list = bankReceiptLinkService.queryMathData(params);
        renderJson(response, ResultMessage.data(list));
    }
}
