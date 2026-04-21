package com.yonyoucloud.fi.cmp.constant;

/**
 * @Description: 银行对账单规则引擎类型常量
 * @Author: gengrong
 * @createTime: 2022/9/29
 * @version: 1.0
 */
public class CmpRuleEngineTypeConstant {
    // ruleType ：cmp_identification-辨识规则，cmp_freeze-冻结规则，cmp_generate-生单规则
    public static final String cmp_identification = "cmp_identification";
    public static final String cmp_freeze = "cmp_freeze";
    public static final String cmp_generate = "cmp_generate";

    // 相关性规则编码前缀 辨识规则、冻结规则、生单规则
    public static final String identification_prefix = "cmp_identification_";
    public static final String freeze_prefix = "cmp_freeze_";
    public static final String generate_prefix = "cmp_generate_";
}
