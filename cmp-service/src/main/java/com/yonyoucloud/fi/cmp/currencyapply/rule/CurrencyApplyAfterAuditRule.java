package com.yonyoucloud.fi.cmp.currencyapply.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.currencyapply.CurrencyApply;
import com.yonyoucloud.fi.cmp.currencyexchange.service.CurrencyExchangeService;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @description: 外币兑换申请审核后规则 -31
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/24 16:44
 */

@Slf4j
@Component("currencyApplyAfterAuditRule")
public class CurrencyApplyAfterAuditRule extends AbstractCommonRule {

    @Resource
    private CurrencyExchangeService currencyExchangeService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size() > 0) {
            BizObject bizobject = bills.get(0);
            Date date = BillInfoUtils.getBusinessDate();
            BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizobject.getId(), 3);
            if(currentBill==null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100099"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0001F", "单据【[%s]】已删除，请刷新后重试") /* "单据【[%s]】已删除，请刷新后重试" */, bizobject.get("code")));
            }

            //审核通过生成外币兑换单
            CurrencyApply currencyApply = new CurrencyApply();
            currencyApply.init(currentBill);
            currencyExchangeService.insertCurrencyApply(currencyApply);

        }
        return new RuleExecuteResult();
    }
}
