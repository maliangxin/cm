package com.yonyoucloud.fi.cmp.vo;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: ResultVo
 * @Date: 2022/4/20 15:47
 **/
@Data
public class ResultVo {

    private List<String> messages = new ArrayList<>();

    private CtmJSONObject failed = new CtmJSONObject();

    private int count;

    private int sucessCount;

    private String message;

    private int failCount;

    public void addFailCount() {
        failCount++;
    }


    public CtmJSONObject getResult(String message,List<String> messages,int count,int sucessCount,int failCount,CtmJSONObject failed) {
        CtmJSONObject result = new CtmJSONObject();
        result.put("messages", messages);
        result.put("msgs", messages);
        result.put("msg", message);
        result.put("count", count);
        result.put("sucessCount", sucessCount);
        result.put("message", message);
        result.put("failCount", failCount);
        result.put("failed", failed);
        return result;
    }
}
