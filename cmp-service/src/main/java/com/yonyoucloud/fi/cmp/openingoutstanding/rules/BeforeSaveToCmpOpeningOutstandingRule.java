package com.yonyoucloud.fi.cmp.openingoutstanding.rules;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding;
import com.yonyoucloud.fi.cmp.initdata.OpeningOutstanding_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.BizContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.JsonFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 期初余额保存前切口类
 */
@Component("beforeSaveToCmpOpeningOutstandingRule")
@Slf4j
public class BeforeSaveToCmpOpeningOutstandingRule extends AbstractCommonRule {
	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
		List<BizObject> bills = getBills(billContext, paramMap);
		if (bills != null && bills.size() > 0) {
			BizObject bizobject = bills.get(0);


			String billnum = billContext.getBillnum();
			if (StringUtils.isEmpty(billnum)) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100361"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DC", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
			}

			//银行方余额和值
			BigDecimal bankinitoribalance = bizobject.getBigDecimal("bankinitoribalance");
			if (ValueUtils.isNotEmptyObj(bankinitoribalance)){
				if (bankinitoribalance.compareTo(new BigDecimal(0)) > 0) {
					bizobject.set("bankdirection", Direction.Debit.getValue());
				} else {
					bizobject.set("bankdirection", Direction.Credit.getValue());
					bizobject.set("bankinitoribalance", bankinitoribalance.abs());
				}
			}

			//企业方余额和值
			BigDecimal coinitloribalance = bizobject.getBigDecimal("coinitloribalance");
			if (ValueUtils.isNotEmptyObj(coinitloribalance)){
				if (coinitloribalance.compareTo(new BigDecimal(0)) > 0) {
					bizobject.set("direction", Direction.Debit.getValue());
				} else {
					bizobject.set("direction", Direction.Credit.getValue());
					bizobject.set("coinitloribalance", coinitloribalance.abs());
				}
			}

			doExecuteBizobject(bizobject);

			JsonFormatter formatter = new JsonFormatter(BizContext.getMetaRepository());
			String json = formatter.toJson(bizobject, billContext.getFullname(), true).toString();
			return new RuleExecuteResult(json);
		}
		return new RuleExecuteResult();
	}

	private void doExecuteBizobject(BizObject bizObject) throws Exception {
		BizObject bizObjectDB = MetaDaoHelper.findById(OpeningOutstanding.ENTITY_NAME, bizObject.getId(), 2);
		List<OpeningOutstanding_b> openingOutstandingSubListDB = bizObjectDB.getBizObjects("openingOutstanding_b", OpeningOutstanding_b.class);
		List<OpeningOutstanding_b> openingOutstandingSubListPage = bizObject.getBizObjects("openingOutstanding_b", OpeningOutstanding_b.class);
		if (!ValueUtils.isNotEmptyObj(openingOutstandingSubListPage)){
			return;
		}
		//只有一条数据时，直接取第一条数据值
		if (openingOutstandingSubListPage.size() == 1){
			bizObject.set("direction", openingOutstandingSubListPage.get(0).get("direction"));
			bizObject.set("bankdirection", openingOutstandingSubListPage.get(0).get("bankdirection"));
			bizObject.set("bankinitoribalance", openingOutstandingSubListPage.get(0).getBigDecimal("bankinitoribalance"));
			bizObject.set("coinitloribalance", openingOutstandingSubListPage.get(0).getBigDecimal("coinitloribalance"));
		}
		if (ValueUtils.isNotEmptyObj(openingOutstandingSubListDB)) {
			for (OpeningOutstanding_b subObj : openingOutstandingSubListPage) {
				if (subObj.getEntityStatus().name().equals("Delete")) {
                    openingOutstandingSubListDB.removeIf(useOrg -> useOrg.getId().equals(subObj.getId()));
				}
			}

			for (OpeningOutstanding_b subObj : openingOutstandingSubListPage) {
				if (subObj.getEntityStatus().name().equals("Update")) {
					Iterator<OpeningOutstanding_b> iterator = openingOutstandingSubListDB.iterator();
					while (iterator.hasNext()) {
						OpeningOutstanding_b outstandingB = iterator.next();
						if (outstandingB.getId().equals(subObj.getId())) {
							outstandingB.setUseOrg(subObj.getUseOrg());
						}
					}
				}
			}
			List<String> useOrgDBList = new ArrayList<>();
			for (OpeningOutstanding_b outstandingB : openingOutstandingSubListDB) {
				useOrgDBList.add(outstandingB.getUseOrg());
			}
			for (OpeningOutstanding_b subObj : openingOutstandingSubListPage) {
				if (subObj.getEntityStatus().name().equals("Insert")) {
					useOrgDBList.add(subObj.getUseOrg());
				}
			}
			int size = useOrgDBList.size();
			HashSet<String> hashSet = new HashSet<>(useOrgDBList);
			if (size != hashSet.size()) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100381"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080073", "已存在相同授权使用组织的数据，请检查！") /* "已存在相同授权使用组织的数据，请检查！" */);
			}

		} else {
			int size = openingOutstandingSubListPage.size();
			Set<String> stringSet = new HashSet<>();
			for (OpeningOutstanding_b openingOutstandingB : openingOutstandingSubListPage) {
				String useOrg = openingOutstandingB.getUseOrg();
				stringSet.add(useOrg);
			}
			if (stringSet.size() != size) {
				throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100381"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080073", "已存在相同授权使用组织的数据，请检查！") /* "已存在相同授权使用组织的数据，请检查！" */);
			}
		}
    }
}
