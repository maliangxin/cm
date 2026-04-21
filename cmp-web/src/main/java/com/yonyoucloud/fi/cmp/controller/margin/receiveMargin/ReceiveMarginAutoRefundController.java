package com.yonyoucloud.fi.cmp.controller.margin.receiveMargin;

import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.receiveMarginAuto.ReceiveMarginAutoService;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/receiveMarginAuto")
@RequiredArgsConstructor
public class ReceiveMarginAutoRefundController extends BaseController {

    private final ReceiveMarginAutoService receiveMarginAutoService;

    /**
     * 根据收到保证金自动退还参数定时任务
     * @param request
     * @param response
     * @param body
     * @return
     * @throws Exception
     */
    @PostMapping("/autoRefund")
    @Authentication(value = false, readCookie = true)
    public Map<String,Object> autoRefund(HttpServletRequest request, HttpServletResponse response, @RequestBody CtmJSONObject body) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("writeOffTask RequestBody:{}", CtmJSONObject.toJSONString(body));
        }
        try {
            String logIdVail = Optional.ofNullable(request.getHeader("logId")).orElse("");
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String logId = Optional.of(logIdVail).orElse("");
            String beforeDaysStr = body.getString("beforeDays");
            String accentity =  body.getString("accentity");
            Integer beforeDays = 0;
            if (ValueUtils.isNotEmptyObj(beforeDaysStr)) {
                beforeDays = Integer.parseInt(beforeDaysStr);
            }
            if(beforeDays.intValue() > 45){
                beforeDays = 45;
            }
            return receiveMarginAutoService.receiveMarginAutoTask(beforeDays,logId,tenantId,accentity);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error("" + e.getMessage()));
        }
        return null;
    }

}
