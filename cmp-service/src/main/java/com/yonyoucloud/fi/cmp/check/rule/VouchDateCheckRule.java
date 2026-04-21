package com.yonyoucloud.fi.cmp.check.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class VouchDateCheckRule extends AbstractCommonRule{

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		log.debug("VouchDateCheckRule.debug:begin==========");

		RuleExecuteResult ruleResult = new RuleExecuteResult();
		BizObject data = getBills(billContext, paramMap).get(0);
		String accEntity = data.get(IBillConst.ACCENTITY);
		Date billDate = data.get(IBillConst.VOUCHDATE);
		if(accEntity == null || billDate == null) {
			return ruleResult;
		}
		BillDataDto billDataDto = (BillDataDto) paramMap.get("param");
		if(billDataDto != null  && billDataDto.getItem() != null) {
			CtmJSONObject valueObject = CtmJSONObject.parseObject(billDataDto.getItem());
			if (!(valueObject.containsKey("key")) ||
                    (!(valueObject.getString("key").equals("vouchdate")) &&
					!(valueObject.getString("key").equals("accentity_name"))) ) {//对于点击单据日期和会计主体名称的check，需要校验模块开启状态
				return ruleResult;
			}
		}
		String enablePeriod;
		Date periodFirstDate = null;
		try {
			periodFirstDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(accEntity);
			if(periodFirstDate == null){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102283"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00029", "模块未启用") /* "模块未启用" */);
			}
		} catch (Exception e) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102283"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00029", "模块未启用") /* "模块未启用" */ + e.getMessage(), e);
		}
		log.debug("VouchDateCheckRule.debug:end==========");
		if (periodFirstDate!=null && billDate.compareTo(periodFirstDate) < 0) {
			throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102284"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0002A", "单据日期不能早于模块启用时间") /* "单据日期不能早于模块启用时间" */);
		}
		return ruleResult;
	}

}
