package com.yonyoucloud.fi.cmp.controller.billclaim.bankreconciliation;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * @Author qihaoc
 * 银行对账单调度任务
 * @Date 2022/10/10 15:14
 */

@RestController
@RequestMapping("/bankreconciliationtask")
@Slf4j
public class BankreconciliationTaskController extends BaseController {

    @Autowired
    BankreconciliationTaskService taskService;

    /**
     * 银行对账单自动发布
     */
    @PostMapping("/autoPublic")
    public void automaticPublic(@RequestBody CtmJSONObject params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if(params == null){
            params = new CtmJSONObject();
        }
        //获取自动任务参数
        params.put("logId", Optional.ofNullable(request.getHeader("logId")).orElse(""));
        params.put("oppositetype", params.get("oppositetype") == null ? null : params.get("oppositetype"));
        try {
            CtmJSONObject object = taskService.automaticPublic(params);
            renderJson(response, CtmJSONObject.toJSONString(object));
        } catch (Exception e) {
            renderJson(response, ResultMessage.error(e.getMessage()));
        }

    }
}
