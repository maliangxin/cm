package com.yonyoucloud.fi.cmp.controller.bankdealdetail;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.api.openapi.InnerAccountDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * 定时任务获取内部账户交易明细，同时进行自动关联自动确认
 * author：wq
 * 2023年11月23日09:04:58
 */
@Controller
@RequestMapping("/api")
@Slf4j
@Lazy
public class InnerAccountDetailController extends BaseController {

    @Autowired
    InnerAccountDetailService innerAccountDetailService;
    /**
     * 任务执行时，默认获取按照参数设置维度，获取满足条件的内部账户当日的交易数据获取到交易数据后，自动调用自动关联规则，按照规则设置的维度，进行业务关联(可参照”银行对账单自动关联任务“的处理逻辑)
     * @param
     * @param request
     * @param response
     */
    @RequestMapping("/queryInnerDetailTask")
    public void BankTradeDetailElectronList(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                            HttpServletResponse response) throws Exception {
        String logId = Optional.ofNullable(request.getHeader("logId")).orElse("");
        if (param == null){
            param = new CtmJSONObject();
        }
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String today = dateFormat.format(now);
        param.put("startDate", today);//
        param.put("endDate", today);//
        param.put("logId",logId);
        Map<String,Object> result = innerAccountDetailService.queryInnerAccountDetail(param);
        renderJson(response, CtmJSONObject.toJSONString(result));
    }
}
