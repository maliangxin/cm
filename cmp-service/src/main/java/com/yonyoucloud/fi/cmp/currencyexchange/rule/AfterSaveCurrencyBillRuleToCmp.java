package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.currencyexchange.service.CurrencyExchangeService;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 外币兑换工作台业务规则
 */
@Component
public class AfterSaveCurrencyBillRuleToCmp extends AbstractCommonRule {

	@Autowired
	BaseRefRpcService baseRefRpcService;

	@Autowired
	CmpWriteBankaccUtils cmpWriteBankaccUtils;

	@Autowired
	CurrencyExchangeService currencyExchangeService;
	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
		List<BizObject> bills = getBills(billContext, map);
		if (bills != null && bills.size()>0) {
			BizObject bizObject = bills.get(0);

			//转换过程
			Journal journal = currencyExchangeService.createJounal(bizObject,billContext.getBillnum(),0);
			Journal journal2 = currencyExchangeService.createJounal(bizObject,billContext.getBillnum(),1);
			Journal journal3 = null;
			if (null != bizObject.get("commissionamount") && ( null !=bizObject.get("commissionbankaccount") || null != bizObject.get("commissioncashaccount"))){
				journal3 = currencyExchangeService.createJounal(bizObject,billContext.getBillnum(),2);
			}

			if((StringUtils.isEmpty(journal.getBankaccount()) || StringUtils.isEmpty(journal2.getBankaccount()))
				&& (StringUtils.isEmpty(journal.getCashaccount()) || StringUtils.isEmpty(journal2.getCashaccount()))){
				return new RuleExecuteResult();
			}

			//回退逻辑
			CmpWriteBankaccUtils.delAccountBook(bizObject.getId().toString());
			//生成日记账
			cmpWriteBankaccUtils.addAccountBook(journal);
			cmpWriteBankaccUtils.addAccountBook(journal2);
			cmpWriteBankaccUtils.addAccountBook(journal3);
		}
		return new RuleExecuteResult();
	}

}
