package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog;

import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleInfo;
import org.imeta.orm.base.BizObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 银行对账单执行规则业务日志实体
 * @Author: gengrong
 * @createTime: 2022/11/14
 * @version: 1.0
 */
public class CmpRuleModuleLog extends BizObject {

    public static final String moduleName = "module_name";
    public static final String moduleName_steps = "module_steps";
    // 规则编码
    public static final String ruleCode = "ruleCode";
    public static final String moduleName_rule_info = "cmp_rule_infos";
    public static final String moduleName_rule_steps = "rule_steps";
    public CmpRuleModuleLog() {
        super();
        CmpRuleInfo cmpRuleInfo = new CmpRuleInfo();
        Map<Integer, String> ModuleName_rule_steps = new HashMap<>();
        set(CmpRuleModuleLog.moduleName_rule_info, cmpRuleInfo);
        set(CmpRuleModuleLog.moduleName_rule_steps, ModuleName_rule_steps);
    }

    public String getModuleName() {
        return get(CmpRuleModuleLog.moduleName);
    }

    public void setModuleName(String id) {
        set(CmpRuleModuleLog.moduleName, id);
    }

    public String getModuleName_steps() {
        return get(CmpRuleModuleLog.moduleName_steps);
    }

    public void setModuleName_steps(String moduleName_steps) {
        set(CmpRuleModuleLog.moduleName_steps, moduleName_steps);
    }
    public CmpRuleInfo getModuleName_rule_info() {
        return get(CmpRuleModuleLog.moduleName_rule_info);
    }
    public void setModuleName_rule_info(CmpRuleInfo cmpRuleInfo) {
        set(CmpRuleModuleLog.moduleName_rule_info, cmpRuleInfo);
    }
    public String getRuleCode() {
        return get(CmpRuleInfo.ruleCode);
    }

    public void setRuleCode(String ruleCode) {
        set(CmpRuleInfo.ruleCode, ruleCode);
    }
    public Map<Integer, String> getModuleName_rule_steps() {
        return get(CmpRuleModuleLog.moduleName_rule_steps);
    }
}
