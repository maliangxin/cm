package com.yonyoucloud.fi.cmp.controller.bankAccountInterestWithholding;

import com.yonyou.ucf.mdd.ext.controller.Authentication;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.interestAutoWithholding.service.InterestAutoWithholdingService;
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

/**
 * 银行账户利息预提调度任务
 */
@Slf4j
@RestController
@RequestMapping("/interestAutoWithholding")
@RequiredArgsConstructor
public class InterestAutoWithholdingController {

    private final InterestAutoWithholdingService interestAutoWithholdingService;

    /**
     * 自动预提
     * @param request
     * @param response
     * @param body
     * @return
     * @throws Exception
     */
    @PostMapping("/autoWithholding")
    @Authentication(value = false, readCookie = true)
    public Map<String,Object> autoWithholding(HttpServletRequest request, HttpServletResponse response, @RequestBody CtmJSONObject body) throws Exception {
        log.error("writeOffTask RequestBody:{}", CtmJSONObject.toJSONString(body));
        try {
            String logIdVail = Optional.ofNullable(request.getHeader("logId")).orElse("");
            String tenantId = Optional.ofNullable(request.getHeader("tenantId")).orElse("");
            String logId = Optional.of(logIdVail).orElse("");
            //会计主体
            String accentity = body.getString("accentity");
            //银行类别
            String bankType = body.getString("bankType");
            //银行账号
            String bankaccount = body.getString("bankaccount");
            //币种
            String currency = body.getString("currency");
            return interestAutoWithholdingService.interestAutoWithholding(logId,tenantId,accentity,bankType,bankaccount,currency);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            //renderJson(response, ResultMessage.error("" + e.getMessage()));
        }
        return null;
    }

}
