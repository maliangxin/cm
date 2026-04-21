package com.yonyoucloud.fi.cmp.controller.openapi;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.api.openapi.OpenApiAccountBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/accountbalance")
@Slf4j
public class OpenApiAccountBalanceController extends BaseController {

    @Autowired
    private OpenApiAccountBalanceService apiAccountBalanceService;


    /**
     * 余额批量新增
     * @param jsonArray
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/batchsave")
    public CtmJSONObject batchSaveAccountBalance(@RequestBody CtmJSONArray jsonArray, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            return apiAccountBalanceService.batchSaveAccountBalance(jsonArray);
        } catch (Exception e) {
            CtmJSONObject ctmJSONObject = new CtmJSONObject();
            ctmJSONObject.put("code",999);
            ctmJSONObject.put("message",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80072", "服务端逻辑异常！") /* "服务端逻辑异常！" */);
            return ctmJSONObject;
        }
    }

    /**
     * 余额区间查询
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/query")
    public void queryAccountBalance(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                    HttpServletResponse response) {
        try {
            CtmJSONObject jsonObject = apiAccountBalanceService.queryAccountBalanceByInterval(param);
            renderJson(response,CtmJSONObject.toJSONString(jsonObject));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response,ResultMessage.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80073", "服务端出错！") /* "服务端出错！" */));
        }
    }
}
