package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcommon.service.StwbCollectionBillServiceImpl;
import com.yonyoucloud.fi.cmp.fundcommon.service.StwbPaymentBillServiceImpl;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>资金收付款单审核的后置规则：通知第三方单据状态</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-04-06 14:43
 */
@Slf4j
@Component("afterAuditFundBillRule")
@RequiredArgsConstructor
public class AfterAuditFundBillRule extends AbstractCommonRule {
    final private StwbCollectionBillServiceImpl stwbCollectionBillService;
    final private StwbPaymentBillServiceImpl stwbPaymentBillService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        String billnum = billContext.getBillnum();
        for (BizObject bizobject : bills) {
            BizObject currentBill;
            if (IBillNumConstant.FUND_PAYMENT.equals(billnum) || IBillNumConstant.FUND_PAYMENTLIST.equals(billnum)) {
                currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizobject.getId());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102611"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E4","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                stwbPaymentBillService.pushBillSimple(currentBill);
                return new RuleExecuteResult();
            }

            if (IBillNumConstant.FUND_COLLECTION.equals(billnum) || IBillNumConstant.FUND_COLLECTIONLIST.equals(billnum)) {
                currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizobject.getId());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102611"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E4","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                stwbCollectionBillService.pushBillSimple(currentBill);
                return new RuleExecuteResult();
            }
            if (IBillNumConstant.TRANSFERACCOUNTLIST.equals(billnum) || IBillNumConstant.TRANSFERACCOUNT.equals(billnum)) {
                currentBill = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizobject.getId());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102611"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E4","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
            }
        }
        afterPushBill(bills);
        return new RuleExecuteResult();
    }

    private boolean afterPushBill(List<BizObject> billList) throws Exception {
        return true;
    }

}
