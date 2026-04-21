package com.yonyoucloud.fi.cmp.controller.common;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.writeOff.service.WriteOffService;
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
@RequestMapping("/writeOff")
@RequiredArgsConstructor
public class WriteOffController extends BaseController {

    private final WriteOffService writeOffService;

    /**
     * 汇兑损益红冲定时任务
     * @param request
     * @param response
     * @param body
     * @return
     * @throws Exception
     */
    @PostMapping("/writeOffTask")
    @Authentication(value = false, readCookie = true)
    public Map<String,Object> writeOffTask(HttpServletRequest request, HttpServletResponse response, @RequestBody CtmJSONObject body) throws Exception {
        log.error("writeOffTask RequestBody:{}", CtmJSONObject.toJSONString(body));
        try {
            String logIdVail = Optional.ofNullable(request.getHeader("logId")).orElse("");
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String logId = Optional.of(logIdVail).orElse("");
            String beforeDaysStr = body.getString("beforeDays");
            Integer beforeDays = 1;
            if (ValueUtils.isNotEmptyObj(beforeDaysStr)) {
                beforeDays = Integer.parseInt(beforeDaysStr);
            }
            if(beforeDays.intValue() > 45){
                beforeDays = 45;
            }
            return writeOffService.WriteOffTask(beforeDays,logId,tenantId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error("" + e.getMessage()));
        }
        return null;
    }

}
