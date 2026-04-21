package com.yonyoucloud.fi.cmp.journal.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import org.imeta.biz.base.BizContext;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.JsonFormatter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 新增单据规则
 */
@Component
public class CMPBillAddRule extends AbstractCommonRule {

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		List<BizObject> bills = getBills(billContext, paramMap);
		if (bills != null && bills.size() > 0) {
			BizObject bizobject = bills.get(0);
			//单组织逻辑
			if(FIDubboUtils.isSingleOrg()){
                BizObject singleOrg = FIDubboUtils.getSingleOrg();
				if(singleOrg != null){
					bizobject.set("accentity",singleOrg.get("id"));
					bizobject.set("accentity_name",singleOrg.get("name"));
				}
			}
			JsonFormatter formatter = new JsonFormatter(BizContext.getMetaRepository());
			String json = formatter.toJson(bizobject, billContext.getFullname(), true).toString();


			return new RuleExecuteResult(json);
		}

		return new RuleExecuteResult();
	}


}
