package com.yonyoucloud.fi.cmp.vo;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 预提规则设置以及银行账户利率设置 返回结果*
 * @author xuxbo
 * @date 2023/4/23 13:39
 */
@Data
public class WithholdingResultVO {
    private List<String> messages = new ArrayList<>();

    private CtmJSONObject failed = new CtmJSONObject();

    private int count;

    private int sucessCount;

    private String message;

    private int failCount;

    public void addFailCount() {
        failCount++;
    }


    public CtmJSONObject getResult() {
        CtmJSONObject result = new CtmJSONObject();
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
