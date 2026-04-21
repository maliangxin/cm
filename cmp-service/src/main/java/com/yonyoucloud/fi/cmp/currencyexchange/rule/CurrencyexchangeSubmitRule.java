package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.DeliveryType;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component("currencyexchangeSubmitRule")
public class CurrencyexchangeSubmitRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BizObject bizObject = getBills(billContext, map).get(0);
        CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME,bizObject.get("id").toString());
        Short auditStatus = bizObject.getShort("auditstatus");
        if (auditStatus != null && auditStatus.equals(AuditStatus.Complete.getValue())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100759"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A1","单据已审批") /* "单据已审批" */);
        }
        short verifyState = ValueUtils.isNotEmptyObj(bizObject.get("verifystate")) ? Short.parseShort(bizObject.get("verifystate").toString()) : (short) -1;
        if (VerifyState.TERMINATED.getValue() == verifyState) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100760"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805A2","流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
        }
        // 直连交割时，交易编码必输
//        if (Short.valueOf(currencyExchange.get("deliveryType").toString()) == DeliveryType.DirectDelivery.getValue()) {
//            if (currencyExchange.get("transactionCode")==null) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100761"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A94E8060448000A","直连交割时，交易编码必输") /* "直连交割时，交易编码必输" */);
//            }
//            if (currencyExchange.get("deliverytime")==null) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100762"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A94E8060448000B","直连交割时，交割类型必输") /* "直连交割时，交割类型必输" */);
//            }
//        }
        return new RuleExecuteResult();
    }
}
