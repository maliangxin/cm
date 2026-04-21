package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 银行对账单导入时（在平台对日期转换成Date格式之前）判断交易日期是否有时分秒，有的话赋值给交易时间
 *
 * @Author chenmcht
 * @Date 2023/04/25
 */

@Slf4j
@Component
public class BankreconciliationImportPreRule extends AbstractCommonRule {
	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		try {
			List<BizObject> bills = getBills(billContext, paramMap);
			if (bills != null && bills.size() > 0) {
				BizObject bankReconciliation = bills.get(0);
				String tran_date = bankReconciliation.get("tran_date");
				// 长度大于16说明是带有时分秒的格式
				if (tran_date != null && tran_date.length() > 16) {
					String tran_time = bankReconciliation.get("tran_time");
					if (StringUtils.isBlank(tran_time)) {
						bankReconciliation.set("tran_time", tran_date);
					}
				}
			}
		} catch (Exception e) {
			log.error("catch exception at BankreconciliationImportPreRule"+e.getMessage(), e);
		}
		return new RuleExecuteResult();
	}
}
