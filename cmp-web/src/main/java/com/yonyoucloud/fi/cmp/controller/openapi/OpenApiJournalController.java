package com.yonyoucloud.fi.cmp.controller.openapi;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.openapi.service.OpenApiJournalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/journal")
@Slf4j
@Lazy
public class OpenApiJournalController {


    @Autowired
    private OpenApiJournalService service;

    /**
     * 根据参数查询日记账，可查询现金日记账和银行日记账
     *
     * @param param
     * @param request
     * @param response
     */
    @RequestMapping("/query")
    public CtmJSONObject querylist(@RequestBody CtmJSONObject param, HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            return service.queryJournalByParam(param);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            CtmJSONObject result = new CtmJSONObject();
            result.put("code",999);
            result.put("message", e.getMessage());
            return result;
        }
    }
}
