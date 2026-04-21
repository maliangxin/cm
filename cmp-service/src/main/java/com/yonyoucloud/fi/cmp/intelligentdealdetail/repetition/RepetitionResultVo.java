package com.yonyoucloud.fi.cmp.intelligentdealdetail.repetition;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import lombok.Data;

import java.util.List;

@Data
public class RepetitionResultVo {

    List<BankReconciliation> updateList;
    List<BankReconciliation> insertList;
    List<BankReconciliation> repetitionList;
    List<BankReconciliation> successList;
    List<BankReconciliation> rollbackList;
}
