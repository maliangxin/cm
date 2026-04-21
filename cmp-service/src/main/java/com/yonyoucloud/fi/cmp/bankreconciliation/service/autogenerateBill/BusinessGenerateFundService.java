package com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.autoorderrule.Autoorderrule;
import com.yonyoucloud.fi.cmp.autoorderrule.Autoorderrule_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.fundcollection.service.BankReconciliationGenerateFundCollectionService;
import com.yonyoucloud.fi.cmp.fundpayment.service.BankreconciliationGenerateFundpaymentService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 到账认领V2
 * 银行对账单 --> 自动生单 --> 资金收付款单
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BusinessGenerateFundService {

    @Autowired
    BankreconciliationGenerateFundpaymentService bankreconciliationGenerateFundpaymentService;

    @Autowired
    BankReconciliationGenerateFundCollectionService bankReconciliationGenerateFundCollectionService;

    /**
     * 1,根据autoorderrule判断生成收款单或付款单 -- 枚举 EventType
     * 2,调用收付款单不同的生单Service
     * @param bankReconciliations
     * @param autoorderrule
     */
    public void bankreconciliationGenerateDoc(List<BankReconciliation> bankReconciliations, Autoorderrule autoorderrule) throws Exception {
        //查询生单所需要的参数
        Autoorderrule_b autoorderrule_b = new Autoorderrule_b();
        List<Map<String, Object>> autoorderrule_bs = MetaDaoHelper.queryById(Autoorderrule_b.ENTITY_NAME,"*",autoorderrule.getDetailid());
        if(!autoorderrule_bs.isEmpty()){
         autoorderrule_b.init(autoorderrule_bs.get(0));
        }
        //生成资金收付款单
        if(autoorderrule.getBusDocumentType().equals(EventType.FundPayment.getValue())){//生成资金付款单
            bankreconciliationGenerateFundpaymentService.bankreconciliationGenerateFundpayment(bankReconciliations,autoorderrule_b);
        }else if(autoorderrule.getBusDocumentType().equals(EventType.FundCollection.getValue())){//生成资金收款单
            bankReconciliationGenerateFundCollectionService.bankReconciliationGenerateFundCollection(bankReconciliations,autoorderrule_b);
        }
    }

    /**
     * 业务单据确认 -- 资金收付款单
     * @param ids -- 关联id集合
     * @param eventType -- 业务单据类型
     * @return
     * @throws Exception
     */
    public void confirmGenerateDoc(List ids,Short eventType) throws Exception {
        if(ids.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102300"),MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00050", "需确认数据为空！") /* "需确认数据为空！" */));
        }
        if(eventType.equals(EventType.FundPayment.getValue())){//确认资金付款单
            bankreconciliationGenerateFundpaymentService.confirmGenerateFundpayment(ids.get(0));
        }else if(eventType.equals(EventType.FundCollection.getValue())){//确认资金收款单
            bankReconciliationGenerateFundCollectionService.confirmGenerateFundCollection(ids.get(0));
        }
    }

    /**
     * 业务单据拒绝 - 资金收付款单
     * @param ids -- 关联id集合
     * @param eventType -- 业务单据集合
     * @return
     */
    public void refuseGenerateDoc(List ids,Short eventType) throws Exception {
        if(ids.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102301"), MessageUtils.getMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0004F", "需拒绝数据为空！") /* "需拒绝数据为空！" */));
        }
        if(eventType.equals(EventType.FundPayment.getValue())){//资金付款单拒绝
        bankreconciliationGenerateFundpaymentService.refuseGenerateFundPayment(ids.get(0));
        }else if(eventType.equals(EventType.FundCollection.getValue())){//资金收款单拒绝
            bankReconciliationGenerateFundCollectionService.refuseGenerateFundCollection(ids.get(0));
        }
    }
}
