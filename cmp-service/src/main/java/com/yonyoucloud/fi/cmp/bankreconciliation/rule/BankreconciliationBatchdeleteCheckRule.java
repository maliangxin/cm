//package com.yonyoucloud.fi.cmp.bankreconciliation.rule;
//
//import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
//import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
//import com.yonyou.ucf.mdd.ext.model.BillContext;
//import com.yonyoucloud.fi.cmp.bankreconciliation.enums.BankreconciliationActionEnum;
//import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.imeta.orm.base.BizObject;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * zxl
// * 删除银行账单-更新账户历史余额
// */
//@Slf4j
//@Component
//   /**
//    * @deprecated
//    */
//  @Deprecated
//public class BankreconciliationBatchdeleteCheckRule extends AbstractCommonRule {
//
//    @Override
//    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
//        List<BizObject> bills = getBills(billContext, paramMap);
//        BankreconciliationUtils.checkDataLegalList(bills, BankreconciliationActionEnum.DELETE);
//        return new RuleExecuteResult();
//    }
//}