package com.yonyoucloud.fi.cmp.receivebill.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.service.CmpReceiveBillSettleServiceImpl;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * 用于收款单批量结算时使用
 *
 */
@Component
public class ReceiveBillSettleRule extends AbstractCommonRule{

    @Autowired
    CmpReceiveBillSettleServiceImpl cmpReceiveBillSettleServiceImpl;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103032"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C261F804A00006", "在财务新架构环境下，不允许结算收款单。") /* "在财务新架构环境下，不允许结算收款单。" */);
        }
        List<ReceiveBill> receiveBillList =  this.getBills(billContext, map);
        ReceiveBill currentReceiveBill = receiveBillList.get(0);
        cmpReceiveBillSettleServiceImpl.settle(currentReceiveBill);

        return new RuleExecuteResult();
    }
}
