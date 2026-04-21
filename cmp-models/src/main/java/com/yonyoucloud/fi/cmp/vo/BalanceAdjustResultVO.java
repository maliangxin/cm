package com.yonyoucloud.fi.cmp.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Data
public class BalanceAdjustResultVO {
    private List<String> messages = new ArrayList<>();
    ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;

    private CtmJSONObject failed = new CtmJSONObject();

    private int count;

    private int sucessCount;

    private String message;

    private int failCount;

    public void addFailCount() {
        failCount++;
    }


    public CtmJSONObject getResult() {
        CtmJSONObject result =new CtmJSONObject();
        result.put("messages", messages);
        result.put("msgs", messages);
        result.put("msg", message);
        result.put("count", getCount());
        result.put("sucessCount", getSucessCount());
        result.put("message", getMessage());
        result.put("failCount", failed.size());
        result.put("failed", failed);
        return result;
    }

}
