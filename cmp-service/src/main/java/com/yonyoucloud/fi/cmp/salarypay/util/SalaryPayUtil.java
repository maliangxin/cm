package com.yonyoucloud.fi.cmp.salarypay.util;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.PayStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PaymentStatus;
import com.yonyoucloud.fi.cmp.constant.AgentTypeConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay_b;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;

import java.util.List;
import java.util.Map;

public class SalaryPayUtil {

	public static CtmJSONObject formatConversionToHR(Salarypay salarypay) throws Exception {
		CtmJSONObject param = new CtmJSONObject();
		param.put(IBussinessConstant.ACCENTITY, salarypay.getAccentity());
		param.put("srcbillid", salarypay.getSrcbillid());
		param.put("paymessage", salarypay.getPaymessage());
		param.put("paybankaccount", salarypay.getPayBankAccount());
		boolean hasfail = false;
		CtmJSONArray parambody = new CtmJSONArray();
		List<Salarypay_b> salarypay_bList = salarypay.getBizObjects("Salarypay_b", Salarypay_b.class);
		//线下支付，支付作废，取消线下支付时，没有表体数据
		if(salarypay_bList == null || salarypay_bList.size()==0) {
			QueryConditionGroup queryConditionGroupBody = new QueryConditionGroup(ConditionOperator.and);
			queryConditionGroupBody.appendCondition(QueryCondition.name("mainid").eq(salarypay.getId()));
			List<Map<String, Object>> payBListQuery = MetaDaoHelper.query(Salarypay_b.ENTITY_NAME,
					QuerySchema.create().addSelect("*").addCondition(queryConditionGroupBody));
			if (payBListQuery != null && payBListQuery.size() > 0) {
				for (Map<String, Object> map : payBListQuery) {
					CtmJSONObject body = new CtmJSONObject();
					body.put("personnum", map.get("personnum"));
					body.put("trademessage", map.get("trademessage"));
					short payStatus = salarypay.getPaystatus().getValue();
					if(PayStatus.NoPay.getValue() == payStatus) {
						// 待支付
						body.put("tradestatus", PaymentStatus.NoPay.getValue());
					}else if(PayStatus.OfflinePay.getValue() == payStatus) {
						// 支付失败
						body.put("tradestatus", PaymentStatus.PayDone.getValue());
					}else if(PayStatus.Fail.getValue() == payStatus) {
						// 支付成功
						body.put("tradestatus", PaymentStatus.PayFail.getValue());
					}else {
						body.put("tradestatus", map.get("tradestatus"));
					}
					if(PaymentStatus.PayFail.getValue() == body.getShortValue("tradestatus")){
						hasfail = true;
					}
					body.put("srcbillid_b", map.get("srcbillid_b"));
					parambody.add(body);
				}
			}
		}else {
			for (Salarypay_b salaryPay_b : salarypay_bList) {
				CtmJSONObject body = new CtmJSONObject();
				body.put("personnum", salaryPay_b.getPersonnum());
				body.put("trademessage", salaryPay_b.getTrademessage());
				PaymentStatus tradestatus = salaryPay_b.getTradestatus();
				short payStatus = salarypay.getPaystatus().getValue();
				if(PayStatus.NoPay.getValue() == payStatus) {
					// 待支付
					body.put("tradestatus", PaymentStatus.NoPay.getValue());
				}else if(PayStatus.OfflinePay.getValue() == payStatus) {
					// 支付失败
					body.put("tradestatus", PaymentStatus.PayDone.getValue());
				}else if(PayStatus.Fail.getValue() == payStatus) {
					// 支付成功
					body.put("tradestatus", PaymentStatus.PayFail.getValue());
				}else {
					body.put("tradestatus", tradestatus.getValue());
				}
				if(PaymentStatus.PayFail.getValue() == body.getShortValue("tradestatus")){
					hasfail = true;
				}
				body.put("srcbillid_b", salaryPay_b.getSrcbillid_b());
				parambody.add(body);
			}
		}
		short payStatus = salarypay.getPaystatus().getValue();
		if(hasfail && payStatus == PayStatus.Success.getValue()) {
			//当前支付单中，有失败的，返回部分成功，支付状态都是当前支付单的
			param.put("paystatus", PayStatus.PartSuccess.getValue());
		}else if(payStatus == PayStatus.OfflinePay.getValue()){
			//应薪资要求，线下支付通知时，返回状态支付成功，不管线下非线下
			param.put("paystatus", PayStatus.Success.getValue());
		}else {
			param.put("paystatus", payStatus);
		}
		param.put("salarypay_b", parambody);
		return param;
	}

	/**
	 * 获取组织本币精度
	 * @param salarypay
	 * @return
	 * @throws Exception
	 */
	public static Short getOlcmoneyDigit(Salarypay salarypay) throws Exception {
		Short olcmoneyDigit = 8;
		if(salarypay.get("natCurrency_moneyDigit") != null) {
			return Short.valueOf(salarypay.get("natCurrency_moneyDigit").toString());
		}
		List<Map<String, Object>> natCurrencyList = QueryBaseDocUtils.queryCurrencyById(salarypay.getNatCurrency());/* 暂不修改 静态方法*/

		if (natCurrencyList != null && natCurrencyList.size() > 0) {
			return Short.valueOf(natCurrencyList.get(0).get("moneyDigit").toString());
		}
		return olcmoneyDigit;
	}

	/**
	 * 根据代发类型编码返回代发类型名称
	 * @param agentType 代发类型编码
	 * @return 代发类型名称
	 */
	public static String getAgentTypeName(String agentType) {
		switch (agentType) {
			case AgentTypeConstant.AGENTTYPE_BYBC:
				return AgentTypeConstant.BYBC;
			case AgentTypeConstant.AGENTTYPE_BYBD:
				return AgentTypeConstant.BYBD;
			case AgentTypeConstant.AGENTTYPE_BYBE:
				return AgentTypeConstant.BYBE;
			case AgentTypeConstant.AGENTTYPE_BYBF:
				return AgentTypeConstant.BYBF;
			case AgentTypeConstant.AGENTTYPE_BYBG:
				return AgentTypeConstant.BYBG;
			case AgentTypeConstant.AGENTTYPE_BYBH:
				return AgentTypeConstant.BYBH;
			case AgentTypeConstant.AGENTTYPE_BYBI:
				return AgentTypeConstant.BYBI;
			case AgentTypeConstant.AGENTTYPE_BYBJ:
				return AgentTypeConstant.BYBJ;
			case AgentTypeConstant.AGENTTYPE_BYBK:
				return AgentTypeConstant.BYBK;
			case AgentTypeConstant.AGENTTYPE_BYFD:
				return AgentTypeConstant.BYFD;
			case AgentTypeConstant.AGENTTYPE_BYSU:
				return AgentTypeConstant.BYSU;
			case AgentTypeConstant.AGENTTYPE_BYTF:
				return AgentTypeConstant.BYTF;
			case AgentTypeConstant.AGENTTYPE_BYWF:
				return AgentTypeConstant.BYWF;
			case AgentTypeConstant.AGENTTYPE_BYWK:
				return AgentTypeConstant.BYWK;
			case AgentTypeConstant.AGENTTYPE_BYXA:
				return AgentTypeConstant.BYXA;
			case AgentTypeConstant.AGENTTYPE_BYXB:
				return AgentTypeConstant.BYXB;
			case AgentTypeConstant.AGENTTYPE_BYXC:
				return AgentTypeConstant.BYXC;
			case AgentTypeConstant.AGENTTYPE_BYXD:
				return AgentTypeConstant.BYXD;
			case AgentTypeConstant.AGENTTYPE_BYXE:
				return AgentTypeConstant.BYXE;
			case AgentTypeConstant.AGENTTYPE_BYXF:
				return AgentTypeConstant.BYXF;
			case AgentTypeConstant.AGENTTYPE_BYXG:
				return AgentTypeConstant.BYXG;
			case AgentTypeConstant.AGENTTYPE_BYXH:
				return AgentTypeConstant.BYXH;
			case AgentTypeConstant.AGENTTYPE_BYXI:
				return AgentTypeConstant.BYXI;
			case AgentTypeConstant.AGENTTYPE_BYXJ:
				return AgentTypeConstant.BYXJ;
			case AgentTypeConstant.AGENTTYPE_BYXK:
				return AgentTypeConstant.BYXK;
			case AgentTypeConstant.AGENTTYPE_BYXL:
				return AgentTypeConstant.BYXL;
			case AgentTypeConstant.AGENTTYPE_BYXM:
				return AgentTypeConstant.BYXM;
			case AgentTypeConstant.AGENTTYPE_BYXN:
				return AgentTypeConstant.BYXN;
			default :
				return null;
		}
	}

}
