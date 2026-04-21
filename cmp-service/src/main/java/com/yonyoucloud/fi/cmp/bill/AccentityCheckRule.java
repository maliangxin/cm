package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.Json;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class AccentityCheckRule extends AbstractCommonRule {

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		RuleExecuteResult result = new RuleExecuteResult();
		if (null == billContext) {
			return result;
		}
		BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
		BizObject data = getBizObject(billDataDto, billContext, paramMap);
		if (data == null) {
			return result;
		}
		String billnum = billDataDto.getBillnum();
		// 资金组织字段不校验
		if (!billDataDto.getKey().equals("accentity") && (!FIDubboUtils.isSingleOrg())) {
			checkAccentity(data, billnum);
		}
		return result;
	}

	private BizObject getBizObject(BillDataDto billDataDto, BillContext billContext, Map<String, Object> paramMap)
			throws Exception {
		Object ret = billDataDto.getData();

		java.util.List<?> data = null;
		if (ret instanceof String) {
			// 临时修改,形成表单报错 lnm
			// data = this.getBills(billContext, paramMap);
			data = innerGetBizObject((String) ret);
		} else {
			data = (List<?>) ret;
		}
		if (data == null) {
			return null;
		}
		if (data.size() > 0) {
			BizObject bizobj = (BizObject) data.get(0);
			return bizobj;
		}
		return null;
	}

	private List<BizObject> innerGetBizObject(String data) {
		Json json = new Json(data);
		List<BizObject> objs = json.decode();
		if (objs == null || objs.size() == 0) {
			return null;
		}
		return objs;
	}

	public static void checkAccentity(BizObject data, String billnum) throws RuntimeException {
		List<String> accentities = AuthUtil.getBizObjectAttr(data, IBillConst.ACCENTITY);
		if (accentities == null || accentities.size() == 0) {
			if("cmp_balanceadjustresult".equals(billnum)){
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102412"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1BBBAC1404C0000C","请先选择对账组织！") /* "请先选择对账组织！" */);
			}else {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102413"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("", "请先选择资金组织！"));
			}
		}

	}

}
