package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext;
import lombok.Data;
import java.io.Serializable;
/**
 * @Author guoyangy
 * @Date 2024/6/27 14:17
 * @Description 描述规则执行结果
 * @Version 1.0
 */
@Data
public class BankDealDetailRuleResult implements Serializable {
    // 1:获取精准结果 2:当前流水被挂起 3:未精准匹配结果 4:异常原因未执行业务
    private String executeStatus;
    //备注(记录执行结果)
    private String businessCode;
    //规则名称
    private String ruleName;
    //执行顺序
    private int ruleorder;
    //执行失败规则的名称
    private String failrule;
    //耗时
    private String usedTime;
}