package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
/**
 * @Author guoyangy
 * @Date 2024/6/28 11:39
 * @Description 流水业务操作，包括具体的去重逻辑和交易明细入库
 * @Version 1.0
 */
@Service
public interface IBankDealDetailBusiOper {
    /**
     * 调用业务流水批量去重逻辑返回集合<p>
     * 集合反参说明<p>
     *     key:见枚举值
     * @see DealDetailEnumConst.Deduplication_KEYEnum
     * */
    Map<String, List<BankReconciliation>> executeDedulication(List<BankReconciliation> bankDealDetails);
}
