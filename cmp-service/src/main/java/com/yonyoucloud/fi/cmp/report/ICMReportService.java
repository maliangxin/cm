package com.yonyoucloud.fi.cmp.report;

import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;

import java.util.Date;
import java.util.List;

public interface ICMReportService {

	/**
	 * 根据日期，会计主体查询汇总数据
	 * @param accentity
	 * @param date
	 * @return
	 * @throws Exception
	 */
	CtmJSONObject getSummaryData(String accentity, String currency, Date date)throws Exception;;

	/**
	 * 根据日期，会计主体查询账户类型汇总数据
	 * @param accentity
	 * @param date
	 * @return
	 * @throws Exception
	 */
	CtmJSONArray getAccountSum(String accentity, String currency, Date date)throws Exception;;

	/**
	 * 根据日期，会计主体,账户类型查询账户列表
	 * @param params
	 * @return
	 * @throws Exception
	 */
	List<SettlementDetail> getAccountList(CtmJSONObject params)throws Exception;;

	/**
	 * 根据账户id,收支类型,日期，会计主体查询账户收支详情
	 * @param params
	 * @return
	 * @throws Exception
	 */
	List<CtmJSONObject> getAccountDetail(CtmJSONObject params)throws Exception;;

	/**
	 * 获取租户下的所有币种
	 * @return
	 */
	List<CurrencyTenantDTO>  getCurrenctData() throws Exception;

	CtmJSONObject getOwnerCurrency(CtmJSONObject params) throws Exception;
}
