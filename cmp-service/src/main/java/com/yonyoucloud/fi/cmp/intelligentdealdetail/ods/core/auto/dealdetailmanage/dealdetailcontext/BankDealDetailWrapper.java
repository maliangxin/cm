package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
/**
 * @Author guoyangy
 * @Date 2024/6/27 13:53
 * @Description todo
 * @Version 1.0
 */
@Data
public class BankDealDetailWrapper implements Serializable {
    //业务表流水原始信息
    private BankReconciliation bankReconciliation;
    //ods流水主键
    private String odsId;
    //流水业务表主键id
    private Long bankReconciliationId ;
    //记录流水执行的每条规则结果
    private List<BankDealDetailRuleResult> ruleList;
    //记录过程实体
    private DealDetailRuleExecRecord dealDetailRuleExecRecord;
    //流水执行当前规则执行结果描述
    private BankDealDetailRuleResult ruleResult;
    /**
     * 1:阻断性规则且满足阻断条件，下一步执行具体流程处理
     * 2:需要人工介入
     * 3:本规则顺利执行完成，可以执行下一个规则
     * 4:系统异常，比如空指针、超时、数据库操作失败等
     * */
    private int executeStatus;
}