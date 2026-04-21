package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import org.imeta.orm.base.BizObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 银行对账单规则信息
 * @Author: gengrong
 * @createTime: 2022/11/14
 * @version: 1.0
 */
public class CmpRuleInfo extends BizObject {

    // 规则编码
    public static final String ruleCode = "ruleCode";
    // 规则类型（脚本或者规则配置）
    public static final String ruleType = "ruleType";
    // 规则条件（Map类型）
    public static final String sources = "sources";
    // 规则结果（Map类型）
    public static final String targets = "targets";

    public CmpRuleInfo() {
        super();
        Map sources = new HashMap<String, Object>();
        Map targets = new HashMap<String, Object>();
        set(CmpRuleInfo.sources, sources);
        set(CmpRuleInfo.targets, targets);
    }

    public String getRuleCode() {
        return get(CmpRuleInfo.ruleCode);
    }

    public void setRuleCode(String ruleCode) {
        set(CmpRuleInfo.ruleCode, ruleCode);
    }

    public String getRuleType() {
        return get(CmpRuleInfo.ruleType);
    }

    public void setRuleType(String ruleType) {
        set(CmpRuleInfo.ruleType, ruleType);
    }

    public HashMap<String, Object> getSources() {
        return get(CmpRuleInfo.sources);
    }

    public HashMap<String, Object> getTargets() {
        return get(CmpRuleInfo.targets);
    }

}
