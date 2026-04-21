package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 银行交易回单删除前规则
 *
 * @author jiangpengk
 * @version 1.0
 * @since 2023-05-05 16:38
 */
@Slf4j
@Component("bankReceiptBeforeDeleteRule")
public class BankReceiptBeforeDeleteRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (bills != null && bills.size() > 0) {
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100562"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00230", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
        }
        for (BizObject bizObject : bills) {
            BankElectronicReceipt bankElectronicReceipt = MetaDaoHelper.findById(BankElectronicReceipt.ENTITY_NAME, bizObject.getId());
            if (!DateOrigin.Created.equals(bankElectronicReceipt.getDataOrigin())) {
                String enterpriseBankAccount = bankElectronicReceipt.getEnterpriseBankAccount();
                EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(enterpriseBankAccount);
                Integer acctopentype = enterpriseBankAcctVO.getAcctopentype();
                //允许删除内部账户的回单（结算中心开户）
                if(AcctopenTypeEnum.SettlementCenter.getValue() != acctopentype){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100563"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00231", "回单编号：[%s]，为直联下载的回单，不允许删除！") /* "回单编号：[%s]，为直联下载的回单，不允许删除！" */, bankElectronicReceipt.getReceiptno()));
                }
            }
            if (bankElectronicReceipt.getBankreconciliationid() != null) {
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankElectronicReceipt.getBankreconciliationid());
                if (bankReconciliation != null ) {
                    //设置更新状态，设置回单关联状态为未关联
                    EntityTool.setUpdateStatus(bankReconciliation);
                    bankReconciliation.setReceiptassociation(ReceiptassociationStatus.NoAssociated.getValue());
                    bankReconciliation.setReceiptId(null);
                    CommonSaveUtils.updateBankReconciliation(bankReconciliation);
                }
            }
        }
        return new RuleExecuteResult();
    }


}