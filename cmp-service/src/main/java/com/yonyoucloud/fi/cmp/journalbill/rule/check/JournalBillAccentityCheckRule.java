package com.yonyoucloud.fi.cmp.journalbill.rule.check;

import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * 日记账录入会计主体检查规则
 */
@Component
@Slf4j
public class JournalBillAccentityCheckRule extends AbstractCommonRule {
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        if (!"accentity_name".equals(item.get("key"))) {
            return new RuleExecuteResult();
        }
        BizObject bill = getBills(billContext, paramMap).get(0);
        String accEntityId = bill.get(IBussinessConstant.ACCENTITY);
        if (accEntityId == null) {
            return new RuleExecuteResult();
        }
        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accEntityId);
        try {
            CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
            if (currencyTenantDTO != null) {
                bill.set("natCurrency", currencyTenantDTO.getId());
                bill.set("natCurrency_name", currencyTenantDTO.getName());
                bill.set("natCurrency_priceDigit", currencyTenantDTO.getPricedigit());
                bill.set("natCurrency_moneyDigit", currencyTenantDTO.getMoneydigit());
            }
        } catch (Exception e) {
            log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18D4A5DC05400008","未取到本币币种！ e = {}") /* "未取到本币币种！ e = {}" */, e.getMessage());
        }
        ExchangeRateTypeVO defaultExchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(accEntityId,true);
        if (defaultExchangeRateType != null && defaultExchangeRateType.getId() != null) {
            bill.set("exchangeRateType", defaultExchangeRateType.getId());
            bill.set("exchangeRateType_code", defaultExchangeRateType.getCode());
            bill.set("exchangeRateType_name", defaultExchangeRateType.getName());
            bill.set("exchangeRateType_digit", defaultExchangeRateType.getDigit());
        }

        Date periodFirstDate = null;
        try {
            periodFirstDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(accEntityId);
            if(periodFirstDate == null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102283"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00029", "模块未启用") /* "模块未启用" */);
            }
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102283"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00029", "模块未启用") /* "模块未启用" */ + e.getMessage(), e);
        }
        return new RuleExecuteResult();
    }
}
