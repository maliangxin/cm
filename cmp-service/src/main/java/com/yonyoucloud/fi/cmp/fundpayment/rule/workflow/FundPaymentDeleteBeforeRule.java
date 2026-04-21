package com.yonyoucloud.fi.cmp.fundpayment.rule.workflow;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.transferaccount.util.AssertUtil;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author guanshaoting
 * @Date 2023/3/22
 **/
@Component
public class FundPaymentDeleteBeforeRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        Object isExecuteRule = bills.get(0).get("isExecuteRule");
        if (ValueUtils.isNotEmptyObj(isExecuteRule)) { //判断是否需要执行此规则，如果是结算工作台弃审则不执行此规则
            return new RuleExecuteResult();
        }
        for (BizObject bill : bills) {
            FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bill.getId(), null);
            AssertUtil.isNotNull(fundPayment, () -> new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800FA","单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */)); /*单据不存在*/
            //当前所选单据事项类型为“统收统支协同单”时，提示：“事项类型为统收统支协同单的资金付款单不允许删除！”；
            EventType billtype = fundPayment.getBilltype();
            if (EventType.Unified_Synergy.equals(billtype)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101782"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1833288A04E00004", "事项类型为统收统支协同单的资金付款单不允许删除！") /* "事项类型为统收统支协同单的资金付款单不允许删除！" */);
            }
            //如果当前单据是迁移单据，需要查询上游单据有没有 付款申请，有付款申请的 不能删除
            if(fundPayment.getMigradeid()!=null){
                //查询上游付款工作台
                PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, bill.getId(), null);
                if(payBill.getBilltype().getValue() == EventType.PayApplyBill.getValue()){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101783"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508006E", "此单据为来源于付款申请的付款工作台升级而来，不能删除。") /* "此单据为来源于付款申请的付款工作台升级而来，不能删除。" */);
                }
            }
        }
        return new RuleExecuteResult();
    }
}
