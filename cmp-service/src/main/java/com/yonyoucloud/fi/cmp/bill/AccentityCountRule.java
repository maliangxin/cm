package com.yonyoucloud.fi.cmp.bill;


import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.checkstockapply.CmpBusiType;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class AccentityCountRule extends AbstractCommonRule {

	@Autowired
	AutoConfigService configService;
	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		BillDataDto dataDto = (BillDataDto) getParam(paramMap);
		CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
		if (!"accentity_name".equals(item.get("key"))) {
			return new RuleExecuteResult();
		}
		BizObject bill = getBills(billContext, paramMap).get(0);
		String billnum = billContext.getBillnum();
		if (!"cmp_checkStockApply".equals(billnum) && !"cmp_exchangegainloss".equals(billnum)
				&& !"cmp_payment".equals(billnum)  && !"cmp_receivebill".equals(billnum)) {
			return new RuleExecuteResult();
		}
		if("cmp_checkStockApply".equals(billnum)){ //支票入库，收入支票需要赋默认币种，空白支票不需要
			// 支票入库，默认“登记人”为当前用户
			bill.set("headCreator", AppContext.getCurrentUser().getName());
			bill.set("creator", AppContext.getCurrentUser().getName());
//			if(bill.get("chequeType") != null && 0 == (bill.getShort("chequeType"))){ //空白支票直接return
//				return new RuleExecuteResult();
//			}
			// 参数是否领用
			Boolean checkStockCanUse = configService.getCheckStockCanUse();
			// 空白支票入库 未领用 领用组织默认取入库组织值且不可修改/已领用 领用组织为空且不可修改
			if (bill.get("chequeType") != null && (CmpBusiType.Black.getValue() == (bill.getShort("chequeType")))) {
				if (checkStockCanUse) {
					bill.set("org",null);
					bill.set("custName",null);
				} else {
					bill.set("org",bill.get(IBussinessConstant.ACCENTITY));
					bill.set("custName",bill.get("accentity_name"));
				}
			}
		} else {
			bill.set("org",bill.get(IBussinessConstant.ACCENTITY));
			bill.set("org_name",bill.get("accentity_name"));
		}
		String accEntityId = bill.get("accentity");
		if(accEntityId == null){
			return new RuleExecuteResult();
		}
		List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accEntityId);
		if (accEntity.size() == 0) {
			return new RuleExecuteResult();
		}
		List<Map<String, Object>> natCurrency = QueryBaseDocUtils.queryCurrencyById(accEntity.get(0).get("currency"));
		if (natCurrency.size() == 0) {
			return new RuleExecuteResult();
		}
		bill.set("natCurrency", natCurrency.get(0).get("id"));
		bill.set("natCurrency_name", natCurrency.get(0).get("name"));
		bill.set("natCurrency_priceDigit", natCurrency.get(0).get("priceDigit"));
		bill.set("natCurrency_moneyDigit", natCurrency.get(0).get("moneyDigit"));
		if(null != bill.get("billtype") && (bill.get("billtype").toString().equals(EventType.CashMark.getValue()+"") ||
				bill.get("billtype").toString().equals(EventType.PayApplyBill.getValue()+""))){
			String currency = bill.get("currency");
			List<Map<String, Object>> currencyEntity = QueryBaseDocUtils.queryCurrencyById(currency);
			if(!CollectionUtils.isEmpty(currencyEntity)){
				bill.set("currency", currencyEntity.get(0).get("id"));
				bill.set("currency_name", currencyEntity.get(0).get("name"));
				bill.set("currency_priceDigit", currencyEntity.get(0).get("priceDigit"));
				bill.set("currency_moneyDigit", currencyEntity.get(0).get("moneyDigit"));
			}
			this.putParam(paramMap, "return", bill);
			return new RuleExecuteResult();
		}
		//银行流水处理卡片，已有币种时不需要处理币种
		if("cmp_bankreconciliation".equals(billnum) && bill.get("currency") != null){
			this.putParam(paramMap, "return", bill);
			return new RuleExecuteResult();
		}
		bill.set("currency", natCurrency.get(0).get("id"));
		bill.set("currency_name", natCurrency.get(0).get("name"));
		bill.set("currency_priceDigit", natCurrency.get(0).get("priceDigit"));
		bill.set("currency_moneyDigit", natCurrency.get(0).get("moneyDigit"));
		bill.set("exchRate", 1);
		this.putParam(paramMap, "return", bill);
		return new RuleExecuteResult();
	}

}
