package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
* @version 1.0
* @since 2022-01-17
*/
@Slf4j
@Component("bankreconciliationBankaccountCheckRule")
@RequiredArgsConstructor
public class BankreconciliationBankaccountCheckRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;
   @Override
   public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
       BillDataDto dataDto = (BillDataDto) getParam(paramMap);
       CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
       if (!ICmpConstant.BANKACCOUNT_NAME.equals(item.get("key"))) {
           return new RuleExecuteResult();
       }
       BizObject bill = getBills(billContext, paramMap).get(0);
       String currency = bill.get(IBussinessConstant.CURRENCY);
       if (null == currency) {
           return new RuleExecuteResult();
       }
       CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(currency);
       if (currencyDTO == null) {
           throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100950"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418061E","当前币种不存在") /* "当前币种不存在" */);
       }
       bill.set("currency_priceDigit",currencyDTO.getPricedigit());
       bill.set("currency_moneyDigit",currencyDTO.getMoneydigit());

       this.putParam(paramMap, "return", bill);
       return new RuleExecuteResult();
   }


}
