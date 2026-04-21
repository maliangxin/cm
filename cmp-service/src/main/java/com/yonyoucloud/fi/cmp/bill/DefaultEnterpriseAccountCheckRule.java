package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Component
public class DefaultEnterpriseAccountCheckRule extends AbstractCommonRule {


    @Autowired
    BaseRefRpcService baseRefRpcService;

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		List<BizObject> bills = getBills(billContext, paramMap);
		if (bills == null || bills.size() == 0) {
			return new RuleExecuteResult();
		}
		BizObject bill = (BizObject) bills.get(0);
        Object billtype = bill.get("billtype");
		if(null != billtype && (billtype.toString().equals(String.valueOf(EventType.CashMark.getValue())))){
            return new RuleExecuteResult();
        }
		Object accEntityId = bill.get(IBussinessConstant.ACCENTITY);
		Object settleModeId = bill.get("settlemode");
		Object currencyId = bill.get("currency");
		if (accEntityId != null && settleModeId != null && currencyId != null) {
            SettleMethodModel settleMode = baseRefRpcService.querySettleMethodsById(settleModeId.toString());
            Map<String, Object> condition = new HashMap<>();
            condition.put("orgid", accEntityId);
            condition.put("enable", 1);
            condition.put("dr", 0);
		    if (settleMode.getServiceAttr().equals(0)) {
                condition.put(ICmpConstant.CURRENCY_REf, currencyId);
                condition.put(ICmpConstant.CURRENCY_DEFAULT_REf, true);
                condition.put(ICmpConstant.CURRENCY_ENABLE_REf, 1);
                bill.set("cashaccount", null);
                bill.set("cashaccount_name", null);
                List<Map<String, Object>> bankAccounts = QueryBaseDocUtils.queryEnterpriseBankAccountByCondition(condition,billContext);/* 暂不修改 */
                if (bankAccounts.size() > 0) {
                    bill.set("enterprisebankaccount", bankAccounts.get(0).get("id"));
                    bill.set("enterprisebankaccount_name", bankAccounts.get(0).get("name"));
                }
            } else if (settleMode.getServiceAttr().equals(1)) {
                condition.put("currency", currencyId);
                condition.put("isdefault", true);
                bill.set("enterprisebankaccount", null);
                bill.set("enterprisebankaccount_name", null);
                List<Map<String, Object>> cashAccounts = QueryBaseDocUtils.queryEnterpriseCashAccountByCondition(condition,billContext);/* 暂不修改 */
                if (cashAccounts.size() > 0) {
                    bill.set("cashaccount", cashAccounts.get(0).get("id"));
                    bill.set("cashaccount_name", cashAccounts.get(0).get("name"));
                }
            }
        }
		this.putParam(paramMap, "return", bill);
		return new RuleExecuteResult();
	}

}
