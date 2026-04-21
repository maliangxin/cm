package com.yonyoucloud.fi.cmp.fundexpense.rule.delete;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cmpentity.ExpenseAuditStatus;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

@Component("beforeFundexpenseDeleteRule")
public class BeforeFundexpenseDeleteRule extends AbstractCommonRule {


	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		List<BizObject> bills = getBills(billContext, paramMap);
		if (CollectionUtils.isEmpty(bills)) {
			return new RuleExecuteResult();
		}
		if (bills != null && bills.size() > 0) {
			//获取前端传过来的值对象
			Fundexpense fundexpense = (Fundexpense) bills.get(0);
			Short auditstatus = fundexpense.getAuditstatus();
			if(!auditstatus.equals(ExpenseAuditStatus.unsubmit.getValue())){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100702"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B2", "单据状态为{0}，不能删除") /* "单据状态为{0}，不能删除" */, ExpenseAuditStatus.getName(auditstatus)));
			}
		}
		return new RuleExecuteResult();
	}

}
