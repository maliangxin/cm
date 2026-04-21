package com.yonyoucloud.fi.cmp.intelligentdealdetail.bankreconciliation;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;

import java.util.List;

/**
 * @Description: 银行对账单规则引擎调用接口
 * 1、辨识规则前缀：cmp_identification_
 * 2、冻结规则前缀：cmp_freeze_
 * 3、生单规则前缀：cmp_generate_
 * 4、分派规则前缀：cmp_distribute_
 * @Author: gengrong
 * @createTime: 2022/9/28
 * @version: 1.0
 */
public interface IBankrecRuleEngineService {

    /**
     * @Describe 执行规则接口
     * @Param bankrecList ：指定单据数组
     * @Param ruleType ：cmp_identification-辨识规则，cmp_freeze-冻结规则，cmp_generate-生单规则
     * @Param isReturnMsg ：是否返回提示信息，是则发生错误时抛出异常信息，否则正常返回不返回提示信息
     * @Return
     */
    public void executeRuleEngine(List<BankReconciliation> bankrecList,
                                  String ruleType, boolean isReturnMsg) throws Exception;

}
