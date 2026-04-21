package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.paymentbill.service.PaymentService;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: 付款工作台批量结算-线下支付
 * @author: wanxbo@yonyou.com
 * @date: 2022/5/30 11:18
 */
@Component
public class PaymentOffLinePayRule extends AbstractCommonRule {

    @Autowired
    PaymentService paymentService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103024"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C2594204E00004", "在财务新架构环境下，不允许线下支付。") /* "在财务新架构环境下，不允许线下支付。" */);
        }
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            Long id = bizobject.getId();
            if (!ValueUtils.isNotEmptyObj(id)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100695"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804FA","操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
            }
            CtmJSONObject params = new CtmJSONObject();
            List<CtmJSONObject> rows = new ArrayList<>();
            rows.add(CtmJSONObject.parseObject(JsonUtils.toJson(bizobject)));
            params.put("rows", rows);
            paymentService.offLinePay(params);
        }
        return new RuleExecuteResult();
    }
}
