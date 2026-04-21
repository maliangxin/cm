package com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine;

import org.imeta.orm.base.BizObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 银行对账单执行规则业务日志实体
 * @Author: gengrong
 * @createTime: 2022/11/14
 * @version: 1.0
 */
public class CmpRuleBusiLog extends BizObject {

    public static final String idField = "id";
    public static final String codeField = "bank_seq_no";
    public static final String nameField = "accentity_name";
    //  规则信息列表(List<CmpRuleInfo>类型)
    public static final String rule_infos = "rule_infos";

    public CmpRuleBusiLog() {
        super();
        List<CmpRuleInfo> rule_infos = new ArrayList<>();
        set(CmpRuleBusiLog.rule_infos, rule_infos);
    }

    public String getId() {
        return get(CmpRuleBusiLog.idField);
    }

    public void setId(String id) {
        set(CmpRuleBusiLog.idField, id);
    }

    public String getCode() {
        return get(CmpRuleBusiLog.codeField);
    }

    public void setCode(String code) {
        set(CmpRuleBusiLog.codeField, code);
    }

    public String getName() {
        return get(CmpRuleBusiLog.nameField);
    }

    public void setName(String name) {
        set(CmpRuleBusiLog.nameField, name);
    }

    public List<CmpRuleInfo> getRuleInfos() {
        return get(CmpRuleBusiLog.rule_infos);
    }

//    public void setRuleInfos(List<CmpRuleInfo> rule_infos) {
//        set(CmpRuleBusiLog.rule_infos, rule_infos);
//    }

}
