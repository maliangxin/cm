package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.service.CurrencyExchangeService;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("currencyexchangeDoSettleRule")
@RequiredArgsConstructor
public class CurrencyexchangeDoSettleRule extends AbstractCommonRule {

    private final CurrencyExchangeService currencyExchangeService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if (bills != null && bills.size()>0) {
            for (BizObject bizobject : bills){
                Long id = bizobject.getId();
                if (!ValueUtils.isNotEmptyObj(id)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100846"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180392","ID不能为空！") /* "ID不能为空！" */);
                }
                CtmJSONObject cancelMap = currencyExchangeService.singleSettle(id);
                if(!cancelMap.getBoolean("dealSucceed")){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100847"),cancelMap.getString(ICmpConstant.MSG));
                }
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100848"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180393","请选择单据！") /* "请选择单据！" */);
        }
        return new RuleExecuteResult();
    }
}
