package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog;

import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleBusiLog;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleInfo;
import org.imeta.orm.base.BizObject;

import java.util.*;

/**
 * @Description: 银行对账单执行规则业务日志实体
 * @Author: gengrong
 * @createTime: 2022/11/14
 * @version: 1.0
 */
public class CmpRuleCheckLog extends BizObject {

    public static final String idField = "id";
    public static final String codeField = "bank_seq_no";
    public static final String nameField = "accentity_name";
    public static final String logName = "log_name";
    public static final String moduleName_rule_infos = "result_log";
    public CmpRuleCheckLog() {
        super();
        Map<String, List<Map<String,CmpRuleModuleLog>>> ModuleName_rule_infos = new LinkedHashMap<>();
        set(CmpRuleCheckLog.moduleName_rule_infos, ModuleName_rule_infos);
    }

    public String getId() {
        return get(CmpRuleCheckLog.idField);
    }

    public void setId(String id) {
        set(CmpRuleCheckLog.idField, id);
    }

    public String getCode() {
        return get(CmpRuleCheckLog.codeField);
    }

    public void setCode(String code) {
        set(CmpRuleCheckLog.codeField, code);
    }

    public String getLogName() {
        return get(CmpRuleCheckLog.logName);
    }

    public void setLogName(String logName) {
        set(CmpRuleCheckLog.logName, logName);
    }

    public String getName() {
        return get(CmpRuleCheckLog.nameField);
    }

    public void setName(String name) {
        set(CmpRuleCheckLog.nameField, name);
    }

    public Map<String, List<Map<String,CmpRuleModuleLog>>> getModuleName_rule_infos() {
        return get(CmpRuleCheckLog.moduleName_rule_infos);
    }
}
