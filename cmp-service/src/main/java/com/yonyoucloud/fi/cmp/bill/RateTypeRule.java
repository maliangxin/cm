package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.FICurrencyRateService;
import com.yonyoucloud.fi.cmp.cmpentity.QuickType;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill_b;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
@Component
public class RateTypeRule extends AbstractCommonRule {

	@Autowired
	private CmCommonService cmCommonService;

    @Autowired
    FICurrencyRateService currencyRateService;

	private static final String QUICKTYPEMAPPER = "com.yonyoucloud.fi.cmp.mapper.QuickTypeMapper";

	@Override
	public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {

		BillDataDto dataDto = (BillDataDto) getParam(paramMap);
		CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
		BizObject bill = getBills(billContext, paramMap).get(0);
		if ("accentity_name".equals(item.get("key"))){
			if(item.get("value")==null||StringUtils.isEmpty(item.get("value").toString())) {
				return new RuleExecuteResult();
			}
			if (bill.get(IBussinessConstant.ACCENTITY) == null) {
				// 清空资金组织时，无需查询数据
				return new RuleExecuteResult();
            }
			String accentity = bill.get(IBussinessConstant.ACCENTITY).toString();
			Map<String, Object> defaultExchangeRateType = new HashMap<>();
			if ("cmp.currencyexchange.CurrencyExchange".equals(billContext.getFullname()) || "cmp.currencyapply.CurrencyApply".equals(billContext.getFullname())) {
				//货币兑换和货币兑换申请 根据资金组织取汇率类型
				defaultExchangeRateType = cmCommonService.getDefaultExchangeRateTypeByFundsOrgid(accentity);
				// v5,适配新汇率；货币兑换和货币兑换申请，折本币汇率类型和手续费汇率类型采用会计核算类
				Map<String, Object> accentityRawRateType = cmCommonService.getDefaultExchangeRateType(accentity);
				if (accentityRawRateType!=null && accentityRawRateType.get("id") != null){
					//折本币买入汇率类型
					bill.set("purchaseRateType",defaultExchangeRateType.get("id"));
					bill.set("purchaseRateType_name",defaultExchangeRateType.get("name"));
					bill.set("purchaseRateType_digit",defaultExchangeRateType.get("digit"));
					//折本币卖出汇率类型
					bill.set("sellRateType",defaultExchangeRateType.get("id"));
					bill.set("sellRateType_name",defaultExchangeRateType.get("name"));
					bill.set("sellRateType_digit",defaultExchangeRateType.get("digit"));
					//折本币手续费汇率类型
					bill.set("commissionRateType",defaultExchangeRateType.get("id"));
					bill.set("commissionRateType_name",defaultExchangeRateType.get("name"));
					bill.set("commissionRateType_digit",defaultExchangeRateType.get("digit"));
				}
			} else {
				defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(accentity);
			}

			// 21/12/22 素允确认全部使用业务单元下汇率类型，不和总账关联
//			if("cmp_exchangegainloss".equals(billContext.getBillnum())){
//				defaultExchangeRateType = cmCommonService.getDefaultExchangeRateTypeFromGl(accentity);
//			}else{
//				defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(accentity);
//			}
			if (defaultExchangeRateType!=null&&defaultExchangeRateType.get("id")!=null){
					bill.set("exchangeRateType",defaultExchangeRateType.get("id"));
					bill.set("exchangeRateType_name",defaultExchangeRateType.get("name"));
					bill.set("exchangeRateType_digit",defaultExchangeRateType.get("digit"));
			}
			String billtype = "";
			if ("cmp.paybill.PayBill".equals(billContext.getFullname())){
				billtype = "FICM2";
				if(null != bill.get("PayBillb")){
					List<PayBillb> PayBillbList = bill.getBizObjects("PayBillb",PayBillb.class);
					if(CollectionUtils.isEmpty(PayBillbList)){
						CtmJSONObject tpljson = CtmJSONObject.parseObject( paramMap.get("requestData") ==null? null :paramMap.get("requestData").toString());
						Map<String,Object> param = new HashMap<>();
						if (tpljson!=null && tpljson.containsKey("currenttplid")){
							param.put("tplid", tpljson.getLong("currenttplid"));
						}
						param.put("tenantid", AppContext.getTenantId());
						HashMap<String,Object> quickCode = SqlHelper.selectOne(QUICKTYPEMAPPER+".getPaybillQuickTypeCode", param);
						if(null != quickCode && null != quickCode.get("cDefaultValue") && !"".equals(quickCode.get("cDefaultValue"))){
							payBillBodyQuickTypeSetting(bill,Short.parseShort(String.valueOf(quickCode.get("cDefaultValue"))));
						}else{
							payBillBodyQuickTypeSetting(bill,QuickType.sundry.getValue());
						}
					}
				}else{
					HashMap<String,Object> quickCode = SqlHelper.selectOne(QUICKTYPEMAPPER+".getPaybillQuickTypeCode", AppContext.getTenantId());
					if(null != quickCode && null != quickCode.get("cDefaultValue") && !"".equals(quickCode.get("cDefaultValue"))){
						payBillBodyQuickTypeSetting(bill,Short.parseShort(String.valueOf(quickCode.get("cDefaultValue"))));
					}else{
						payBillBodyQuickTypeSetting(bill,QuickType.sundry.getValue());
					}
				}
			}else if("cmp.receivebill.ReceiveBill".equals(billContext.getFullname())){
				billtype = "FICM1";
				if(null != bill.get("ReceiveBill_b")){
					List<ReceiveBill_b> ReceiveBill_bList = bill.getBizObjects("ReceiveBill_b",ReceiveBill_b.class);
					if(CollectionUtils.isEmpty(ReceiveBill_bList)){
						CtmJSONObject tpljson = CtmJSONObject.parseObject( paramMap.get("requestData") ==null? null :paramMap.get("requestData").toString());
						Map<String,Object> param = new HashMap<>();
						if (tpljson!=null && tpljson.containsKey("currenttplid")){
							param.put("tplid", tpljson.getLong("currenttplid"));
						}
						param.put("tenantid", AppContext.getTenantId());
						HashMap<String,Object> quickCode = SqlHelper.selectOne(QUICKTYPEMAPPER+".getReceivebillQuickTypeCode", param);
						if(null != quickCode && null != quickCode.get("cDefaultValue") && !"".equals(quickCode.get("cDefaultValue"))){
							receiveBillBodyQuickTypeSetting(bill, Short.parseShort(String.valueOf(quickCode.get("cDefaultValue"))));
						}else{
							receiveBillBodyQuickTypeSetting(bill,QuickType.sundry.getValue());
						}
					}
				}else{
					HashMap<String,Object> quickCode = SqlHelper.selectOne(QUICKTYPEMAPPER+".getReceivebillQuickTypeCode", AppContext.getTenantId());
					if(null != quickCode && null != quickCode.get("cDefaultValue") && !"".equals(quickCode.get("cDefaultValue"))){
						receiveBillBodyQuickTypeSetting(bill,Short.parseShort(String.valueOf(quickCode.get("cDefaultValue"))));
					}else{
						receiveBillBodyQuickTypeSetting(bill,QuickType.sundry.getValue());
					}
				}
			}
			// 选择了交易类型，则不再给默认值
//			String tradetype = null;
//			CtmJSONObject transtypejson = CtmJSONObject.parseObject( paramMap.get("requestData") ==null? null :paramMap.get("requestData").toString());
//			if (transtypejson!=null && transtypejson.containsKey("tradetype")){
//				tradetype=transtypejson.getString("tradetype");
//			}
//			if (StringUtils.isEmpty(tradetype) && !StringUtils.isEmpty(billtype)) {
//				Map<String, Object> queryTransType = cmCommonService.queryTransTypeById(billtype, "1", null);
//				if (ValueUtils.isNotEmptyObj(queryTransType)) {
//					bill.set("tradetype", queryTransType.get("id"));
//					bill.set("tradetype_name", queryTransType.get("name"));
//					bill.set("tradetype_code", queryTransType.get("code"));
//				}
//			}
		}
			// begin 以下逻辑作用：参照未勾兑对账单生成应收应付单据时，重新生成单据编号赋值，不然会生成现金的单据编号，注释
//			if ("arap.paybill.PayBill".equals(billContext.getFullname()) && EventSource.Manual.getValue() == Short.parseShort(bill.get("srcitem").toString())) {
//				IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
//				String billCode = billCodeComponentService.getBillCode(CmpBillCodeMappingConfUtils.getBillCode("arap_payment"),
//						PayBill.ENTITY_NAME, InvocationInfoProxy.getTenantid(), "",
//						new BillCodeObj(bill), true);
//				bill.set("code", billCode);
//			}
//			if ("arap.receivebill.ReceiveBill".equals(billContext.getFullname()) && EventSource.Manual.getValue() == Short.parseShort(bill.get("srcitem").toString())) {
//				IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
//				String billCode = billCodeComponentService.getBillCode(CmpBillCodeMappingConfUtils.getBillCode("arap_receivebill"),
//						ReceiveBill.ENTITY_NAME, InvocationInfoProxy.getTenantid(), "",
//						new BillCodeObj(bill), true);
//				bill.set("code", billCode);
//			}
			// end
		return new RuleExecuteResult();
	}

	/**
	 * 给单据默认添加一行带款项类型得子表
	 * @param bizObject
	 * @throws Exception
	 */
	private void receiveBillBodyQuickTypeSetting(BizObject bizObject,short quickCode) throws Exception {
		ReceiveBill_b receiveBill_b = new ReceiveBill_b();
		List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(String.valueOf(quickCode)));
		if (quickTypeMap.size() > 0) {
			//停用的款项类型不添加
			if(!MapUtils.getBoolean(quickTypeMap.get(0), "stopstatus")){
				receiveBill_b.setQuickType(MapUtils.getLong(quickTypeMap.get(0), "id"));
				receiveBill_b.set("quickType_name", MapUtils.getString(quickTypeMap.get(0), "name"));
				receiveBill_b.set("quickType_code", MapUtils.getString(quickTypeMap.get(0), "code"));
			}
			bizObject.set("ReceiveBill_b", Arrays.asList(receiveBill_b));
		}
	}

	/**
	 * 给单据默认添加一行带款项类型得子表
	 * @param bizObject
	 * @throws Exception
	 */
	private void payBillBodyQuickTypeSetting(BizObject bizObject,short quickCode) throws Exception {
		PayBillb payBillb = new PayBillb();
		List<Map<String, Object>> quickTypeMap = QueryBaseDocUtils.getQuickTypeByCode(Collections.singletonList(String.valueOf(quickCode)));
		if (quickTypeMap.size() > 0) {
			//停用的款项类型不添加
			if(!MapUtils.getBoolean(quickTypeMap.get(0), "stopstatus")){
				payBillb.setQuickType(MapUtils.getLong(quickTypeMap.get(0), "id"));
				payBillb.set("quickType_name", MapUtils.getString(quickTypeMap.get(0), "name"));
				payBillb.set("quickType_code", MapUtils.getString(quickTypeMap.get(0), "code"));
			}
			bizObject.set("PayBillb", Arrays.asList(payBillb));
		}
	}

}
