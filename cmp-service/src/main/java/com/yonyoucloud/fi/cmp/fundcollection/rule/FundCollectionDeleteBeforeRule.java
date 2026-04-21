package com.yonyoucloud.fi.cmp.fundcollection.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.transferaccount.util.AssertUtil;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @time：2023/3/21--14:20
 * @author：xuyao2
 **/
@Component
@Slf4j
public class FundCollectionDeleteBeforeRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        Object isExecuteRule = bills.get(0).get("isExecuteRule");//是否执行删除前规则标识
        if (ValueUtils.isNotEmptyObj(isExecuteRule)) { //判断是否需要执行此规则，如果是结算工作台弃审则不执行此规则
            return new RuleExecuteResult();
        }
        for (BizObject bill : bills) {
            FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bill.getId(), null);
            AssertUtil.isNotNull(fundCollection, () -> new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800FA","单据不存在，请刷新后重试！") /* "单据不存在，请刷新后重试！" */)); /*单据不存在*/
            //当前所选单据事项类型为“统收统支协同单”时，提示：“事项类型为统收统支协同单的资金付款单不允许删除！”；
            EventType billtype = fundCollection.getBilltype();
            if (EventType.Unified_Synergy.equals(billtype)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102614"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1811A04805B00032", "事项类型为统收统支协同单的资金收款单不允许删除！") /* "事项类型为统收统支协同单的资金收款单不允许删除！" */);
            }
        }
        return new RuleExecuteResult();
    }
}
