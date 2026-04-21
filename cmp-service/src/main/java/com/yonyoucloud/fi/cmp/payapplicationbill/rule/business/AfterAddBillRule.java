package com.yonyoucloud.fi.cmp.payapplicationbill.rule.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>AfterAddBillRule</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-07-20 11:21
 */
@Component("afterAddBillRule")
@RequiredArgsConstructor
@Slf4j
public class AfterAddBillRule extends AbstractCommonRule {
    private final CmCommonService cmCommonService;
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = this.getBills(billContext, paramMap);
        for (BizObject bill : bills) {
            String billnum = billContext.getBillnum();
            switch (billnum ){
                case "cmp_payapplicationbill":
                    cmCommonService.setTransTypeValueForBizObject(bill, "CM.cmp_payapplicationbill");
                    break;
                case "cmp_fundpayment":
                    cmCommonService.setTransTypeValueForBizObject(bill, "CM.cmp_fundpayment");
                    break;
                case "cmp_fundcollection":
                    cmCommonService.setTransTypeValueForBizObject(bill, "CM.cmp_fundcollection");
                    break;
                case "cmp_payment":
                    cmCommonService.setTransTypeValueForBizObject(bill, "CM.cmp_payment");
                    break;
                case "cmp_receivebill":
                    cmCommonService.setTransTypeValueForBizObject(bill, "CM.cmp_receivebill");
                    break;
                case "cmp_currencyexchange":
                    //货币兑换add方法有交易类型获取。不需要公共处理
//                    cmCommonService.setTransTypeValueForBizObject(bill, "CM.cmp_currencyexchange");
                    break;
                case "cm_transfer_account":
                    //cmCommonService.setTransTypeValueForBizObject(bill, "CM.cm_transfer_account");
                    cmCommonService.setAccentityRawForBizObject(bill, "CM.cm_transfer_account");
                    break;
                case "cmp_foreignpayment":
                    //cmCommonService.setTransTypeValueForBizObject(bill, "CM.cm_transfer_account");
                    cmCommonService.setAccentityRawForBizObject(bill, null);
                    break;
                default:
                    break;
            }
        }
        return new RuleExecuteResult();
    }
}
