package com.yonyoucloud.fi.cmp.journal.rule;

import com.google.gson.internal.LinkedTreeMap;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.utils.json.GsonHelper;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import org.imeta.biz.base.BizContext;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.JsonFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
@Component
public class WdAutoCountRule extends AbstractCommonRule {

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		BillDataDto item = (BillDataDto)this.getParam(paramMap);
		List<BizObject> bills = getBills(billContext, paramMap);
		if (bills != null && bills.size()>0) {
			BizObject bizObject = (BizObject) bills.get(0);
			BigDecimal exchRate = bizObject.get("exchangerate");//获取汇率
			BigDecimal debitoriSum = bizObject.get("debitoriSum");//借方原币金额
			BigDecimal creditoriSum = bizObject.get("creditoriSum");//贷方原币金额
			//String currency = AccentityUtil.getNatCurrencyIdByAccentityId((String) bizObject.get("accentity"));
			//CurrencyDTO currencyDTO = CurrencyUtil.getCurrency((String) bizObject.get("currency"));
    		//int scale  = 	currencyDTO.getMoneydigit();
			BigDecimal debitnatSum = BigDecimalUtils.safeMultiply(debitoriSum,exchRate,2);
			BigDecimal creditnatSum = BigDecimalUtils.safeMultiply(creditoriSum,exchRate,2);
			bizObject.put("debitnatSum", debitnatSum);
			bizObject.put("creditnatSum", creditnatSum);
			JsonFormatter formatter = new JsonFormatter(BizContext.getMetaRepository());
			String json;
			LinkedTreeMap res;
			json = formatter.toJson(bizObject, billContext.getFullname(), 32).toString();
			res = (LinkedTreeMap) GsonHelper.FromJSon(json, Map.class);
			this.putParam(paramMap, "return", res);
		}
		return new RuleExecuteResult(item.getData());
	}

}
