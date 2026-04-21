package com.yonyoucloud.fi.cmp.fundexpense;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IBackWrite;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 资金费用子表实体
 *
 * @author u
 * @version 1.0
 */
public class Fundexpense_b extends BizObject implements IBackWrite , ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.fundexpense.Fundexpense_b";

	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

    /**
     * 获取业务币种id
     *
     * @return 业务币种id.ID
     */
	public String getBusiness_currency() {
		return get("business_currency");
	}

    /**
     * 设置业务币种id
     *
     * @param business_currency 业务币种id.ID
     */
	public void setBusiness_currency(String business_currency) {
		set("business_currency", business_currency);
	}

    /**
     * 获取费用金额(业务币种)
     *
     * @return 费用金额(业务币种)
     */
	public java.math.BigDecimal getBusiness_currency_expenseSum() {
		return get("business_currency_expenseSum");
	}

    /**
     * 设置费用金额(业务币种)
     *
     * @param business_currency_expenseSum 费用金额(业务币种)
     */
	public void setBusiness_currency_expenseSum(java.math.BigDecimal business_currency_expenseSum) {
		set("business_currency_expenseSum", business_currency_expenseSum);
	}

    /**
     * 获取调整金额(业务币种)
     *
     * @return 调整金额(业务币种)
     */
	public java.math.BigDecimal getBusiness_currency_tiaozSum() {
		return get("business_currency_tiaozSum");
	}

    /**
     * 设置调整金额(业务币种)
     *
     * @param business_currency_tiaozSum 调整金额(业务币种)
     */
	public void setBusiness_currency_tiaozSum(java.math.BigDecimal business_currency_tiaozSum) {
		set("business_currency_tiaozSum", business_currency_tiaozSum);
	}

    /**
     * 获取调整后金额(业务币种)
     *
     * @return 调整后金额(业务币种)
     */
	public java.math.BigDecimal getBusiness_currency_tiaozhSum() {
		return get("business_currency_tiaozhSum");
	}

    /**
     * 设置调整后金额(业务币种)
     *
     * @param business_currency_tiaozhSum 调整后金额(业务币种)
     */
	public void setBusiness_currency_tiaozhSum(java.math.BigDecimal business_currency_tiaozhSum) {
		set("business_currency_tiaozhSum", business_currency_tiaozhSum);
	}

    /**
     * 获取费用币种
     *
     * @return 费用币种
     */
	public String getExpense_b_currency() {
		return get("expense_b_currency");
	}

    /**
     * 设置费用币种
     *
     * @param expense_b_currency 费用币种
     */
	public void setExpense_b_currency(String expense_b_currency) {
		set("expense_b_currency", expense_b_currency);
	}

    /**
     * 获取本币币种汇率
     *
     * @return 本币币种汇率
     */
	public java.math.BigDecimal getExpense_b_exchRate() {
		return get("expense_b_exchRate");
	}

    /**
     * 设置本币币种汇率
     *
     * @param expense_b_exchRate 本币币种汇率
     */
	public void setExpense_b_exchRate(java.math.BigDecimal expense_b_exchRate) {
		set("expense_b_exchRate", expense_b_exchRate);
	}

    /**
     * 获取汇率类型
     *
     * @return 汇率类型
     */
	public String getExpense_b_exchangeRateType() {
		return get("expense_b_exchangeRateType");
	}

    /**
     * 设置汇率类型
     *
     * @param expense_b_exchangeRateType 汇率类型
     */
	public void setExpense_b_exchangeRateType(String expense_b_exchangeRateType) {
		set("expense_b_exchangeRateType", expense_b_exchangeRateType);
	}

    /**
     * 获取费用金额(本币币种)
     *
     * @return 费用金额(本币币种)
     */
	public java.math.BigDecimal getExpense_b_expenseSum() {
		return get("expense_b_expenseSum");
	}

    /**
     * 设置费用金额(本币币种)
     *
     * @param expense_b_expenseSum 费用金额(本币币种)
     */
	public void setExpense_b_expenseSum(java.math.BigDecimal expense_b_expenseSum) {
		set("expense_b_expenseSum", expense_b_expenseSum);
	}

	/**
	 * 获取费用金额(费用币种)
	 *
	 * @return 费用金额(费用币种)
	 */
	public java.math.BigDecimal getExpense_b_currency_expenseSum() {
		return get("expense_b_currency_expenseSum");
	}

	/**
	 * 设置费用金额(费用币种)
	 *
	 * @param expense_b_currency_expenseSum 费用金额(费用币种)
	 */
	public void setExpense_b_currency_expenseSum(java.math.BigDecimal expense_b_currency_expenseSum) {
		set("expense_b_currency_expenseSum", expense_b_currency_expenseSum);
	}

    /**
     * 获取本币币种
     *
     * @return 本币币种
     */
	public String getExpense_b_natcurrency() {
		return get("expense_b_natcurrency");
	}

    /**
     * 设置本币币种
     *
     * @param expense_b_natcurrency 本币币种
     */
	public void setExpense_b_natcurrency(String expense_b_natcurrency) {
		set("expense_b_natcurrency", expense_b_natcurrency);
	}

    /**
     * 获取待摊金额(费用币种)
     *
     * @return 待摊金额(费用币种)
     */
	public java.math.BigDecimal getExpense_b_toShare_expenseSum() {
		return get("expense_b_toShare_expenseSum");
	}

    /**
     * 设置待摊金额(费用币种)
     *
     * @param expense_b_toShare_expenseSum 待摊金额(费用币种)
     */
	public void setExpense_b_toShare_expenseSum(java.math.BigDecimal expense_b_toShare_expenseSum) {
		set("expense_b_toShare_expenseSum", expense_b_toShare_expenseSum);
	}

	/**
	 * 获取待摊金额(费用币种)
	 *
	 * @return 待摊金额(费用币种)
	 */
	public java.math.BigDecimal getExpense_b_shared_expenseSum() {
		return get("expense_b_shared_expenseSum");
	}

	/**
	 * 设置待摊金额(费用币种)
	 *
	 * @param expense_b_shared_expenseSum 待摊金额(费用币种)
	 */
	public void setExpense_b_shared_expenseSum(java.math.BigDecimal expense_b_shared_expenseSum) {
		set("expense_b_shared_expenseSum", expense_b_shared_expenseSum);
	}

    /**
     * 获取费用币种汇率
     *
     * @return 费用币种汇率
     */
	public java.math.BigDecimal getExpense_bexchRate() {
		return get("expense_bexchRate");
	}

    /**
     * 设置费用币种汇率
     *
     * @param expense_bexchRate 费用币种汇率
     */
	public void setExpense_bexchRate(java.math.BigDecimal expense_bexchRate) {
		set("expense_bexchRate", expense_bexchRate);
	}

    /**
     * 获取费用币种汇率类型
     *
     * @return 费用币种汇率类型.ID
     */
	public String getExpense_bexchangeRateType() {
		return get("expense_bexchangeRateType");
	}

    /**
     * 设置费用币种汇率类型
     *
     * @param expense_bexchangeRateType 费用币种汇率类型.ID
     */
	public void setExpense_bexchangeRateType(String expense_bexchangeRateType) {
		set("expense_bexchangeRateType", expense_bexchangeRateType);
	}

    /**
     * 获取费用明细编号
     *
     * @return 费用明细编号
     */
	public String getExpense_detail_code() {
		return get("expense_detail_code");
	}

    /**
     * 设置费用明细编号
     *
     * @param expense_detail_code 费用明细编号
     */
	public void setExpense_detail_code(String expense_detail_code) {
		set("expense_detail_code", expense_detail_code);
	}

    /**
     * 获取首次分摊日
     *
     * @return 首次分摊日
     */
	public java.util.Date getFirst_sharedate() {
		return get("first_sharedate");
	}

    /**
     * 设置首次分摊日
     *
     * @param first_sharedate 首次分摊日
     */
	public void setFirst_sharedate(java.util.Date first_sharedate) {
		set("first_sharedate", first_sharedate);
	}

    /**
     * 获取子表自定义项
     *
     * @return 子表自定义项.ID
     */
	public Long getFundexpense_b_Define() {
		return get("fundexpense_b_Define");
	}

    /**
     * 设置子表自定义项
     *
     * @param fundexpense_b_Define 子表自定义项.ID
     */
	public void setFundexpense_b_Define(Long fundexpense_b_Define) {
		set("fundexpense_b_Define", fundexpense_b_Define);
	}

    /**
     * 获取分组任务KEY
     *
     * @return 分组任务KEY
     */
	public String getGroupTaskKey() {
		return get("groupTaskKey");
	}

    /**
     * 设置分组任务KEY
     *
     * @param groupTaskKey 分组任务KEY
     */
	public void setGroupTaskKey(String groupTaskKey) {
		set("groupTaskKey", groupTaskKey);
	}

    /**
     * 获取行号
     *
     * @return 行号
     */
	public java.math.BigDecimal getLineno() {
		return get("lineno");
	}

    /**
     * 设置行号
     *
     * @param lineno 行号
     */
	public void setLineno(java.math.BigDecimal lineno) {
		set("lineno", lineno);
	}

    /**
     * 获取主表id
     *
     * @return 主表id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

    /**
     * 获取生单规则编号
     *
     * @return 生单规则编号
     */
	public String getMakeRuleCode() {
		return get("makeRuleCode");
	}

    /**
     * 设置生单规则编号
     *
     * @param makeRuleCode 生单规则编号
     */
	public void setMakeRuleCode(String makeRuleCode) {
		set("makeRuleCode", makeRuleCode);
	}

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
	public java.util.Date getPubts() {
		return get("pubts");
	}

    /**
     * 设置时间戳
     *
     * @param pubts 时间戳
     */
	public void setPubts(java.util.Date pubts) {
		set("pubts", pubts);
	}

    /**
     * 获取序号
     *
     * @return 序号
     */
	public Integer getRowno() {
		return get("rowno");
	}

    /**
     * 设置序号
     *
     * @param rowno 序号
     */
	public void setRowno(Integer rowno) {
		set("rowno", rowno);
	}

    /**
     * 获取分摊周期
     *
     * @return 分摊周期
     */
	public Short getShare_cycle() {
	    return getShort("share_cycle");
	}

    /**
     * 设置分摊周期
     *
     * @param share_cycle 分摊周期
     */
	public void setShare_cycle(Short share_cycle) {
		set("share_cycle", share_cycle);
	}

    /**
     * 获取分摊结束日期
     *
     * @return 分摊结束日期
     */
	public java.util.Date getShare_endate() {
		return get("share_endate");
	}

    /**
     * 设置分摊结束日期
     *
     * @param share_endate 分摊结束日期
     */
	public void setShare_endate(java.util.Date share_endate) {
		set("share_endate", share_endate);
	}

    /**
     * 获取分摊开始日期
     *
     * @return 分摊开始日期
     */
	public java.util.Date getShare_startdate() {
		return get("share_startdate");
	}

    /**
     * 设置分摊开始日期
     *
     * @param share_startdate 分摊开始日期
     */
	public void setShare_startdate(java.util.Date share_startdate) {
		set("share_startdate", share_startdate);
	}

    /**
     * 获取上游单据类型
     *
     * @return 上游单据类型
     */
	public String getSource() {
		return get("source");
	}

    /**
     * 设置上游单据类型
     *
     * @param source 上游单据类型
     */
	public void setSource(String source) {
		set("source", source);
	}

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
	public java.util.Date getSourceMainPubts() {
		return get("sourceMainPubts");
	}

    /**
     * 设置时间戳
     *
     * @param sourceMainPubts 时间戳
     */
	public void setSourceMainPubts(java.util.Date sourceMainPubts) {
		set("sourceMainPubts", sourceMainPubts);
	}

    /**
     * 获取上游单据子表id
     *
     * @return 上游单据子表id
     */
	public Long getSourceautoid() {
		return get("sourceautoid");
	}

    /**
     * 设置上游单据子表id
     *
     * @param sourceautoid 上游单据子表id
     */
	public void setSourceautoid(Long sourceautoid) {
		set("sourceautoid", sourceautoid);
	}

    /**
     * 获取上游单据主表id
     *
     * @return 上游单据主表id
     */
	public Long getSourceid() {
		return get("sourceid");
	}

    /**
     * 设置上游单据主表id
     *
     * @param sourceid 上游单据主表id
     */
	public void setSourceid(Long sourceid) {
		set("sourceid", sourceid);
	}

    /**
     * 获取来源模块
     *
     * @return 来源模块
     */
	public Short getSrcbigtype() {
	    return getShort("srcbigtype");
	}

    /**
     * 设置来源模块
     *
     * @param srcbigtype 来源模块
     */
	public void setSrcbigtype(Short srcbigtype) {
		set("srcbigtype", srcbigtype);
	}

    /**
     * 获取来源单据id
     *
     * @return 来源单据id
     */
	public String getSrcbillid() {
		return get("srcbillid");
	}

    /**
     * 设置来源单据id
     *
     * @param srcbillid 来源单据id
     */
	public void setSrcbillid(String srcbillid) {
		set("srcbillid", srcbillid);
	}

	/**
	 * 获取来源业务id
	 *
	 * @return 来源业务id
	 */
	public String getSrcbillno() {
		return get("srcbillno");
	}

	/**
	 * 设置来源业务id
	 *
	 * @param srcbillno 来源业务id
	 */
	public void setSrcbillno(String srcbillno) {
		set("srcbillno", srcbillno);
	}

	/**
	 * 获取来源业务
	 *
	 * @return 来源业务
	 */
	public String getSrcbillno_name() {
		return get("srcbillno_name");
	}

	/**
	 * 设置来源业务
	 *
	 * @param srcbillno_name 来源业务
	 */
	public void setSrcbillno_name(String srcbillno_name) {
		set("srcbillno_name", srcbillno_name);
	}

    /**
     * 获取来源单据类型
     *
     * @return 来源单据类型
     */
	public String getSrcbilltype() {
		return get("srcbilltype");
	}

    /**
     * 设置来源单据类型
     *
     * @param srcbilltype 来源单据类型
     */
	public void setSrcbilltype(String srcbilltype) {
		set("srcbilltype", srcbilltype);
	}

    /**
     * 获取来源业务上游合同
     *
     * @return 来源业务上游合同
     */
	public String getSrcbusinesstopcontract() {
		return get("srcbusinesstopcontract");
	}

    /**
     * 设置来源业务上游合同
     *
     * @param srcbusinesstopcontract 来源业务上游合同
     */
	public void setSrcbusinesstopcontract(String srcbusinesstopcontract) {
		set("srcbusinesstopcontract", srcbusinesstopcontract);
	}

    /**
     * 获取来源费用计划
     *
     * @return 来源费用计划
     */
	public String getSrcexpenseplancode() {
		return get("srcexpenseplancode");
	}

    /**
     * 设置来源费用计划
     *
     * @param srcexpenseplancode 来源费用计划
     */
	public void setSrcexpenseplancode(String srcexpenseplancode) {
		set("srcexpenseplancode", srcexpenseplancode);
	}

    /**
     * 获取租户
     *
     * @return 租户.ID
     */
	public Long getTenant() {
		return get("tenant");
	}

    /**
     * 设置租户
     *
     * @param tenant 租户.ID
     */
	public void setTenant(Long tenant) {
		set("tenant", tenant);
	}

    /**
     * 获取上游单据号
     *
     * @return 上游单据号
     */
	public String getUpcode() {
		return get("upcode");
	}

    /**
     * 设置上游单据号
     *
     * @param upcode 上游单据号
     */
	public void setUpcode(String upcode) {
		set("upcode", upcode);
	}

    /**
     * 获取租户id
     *
     * @return 租户id.ID
     */
	public String getYtenant() {
		return get("ytenant");
	}

    /**
     * 设置租户id
     *
     * @param ytenant 租户id.ID
     */
	public void setYtenant(String ytenant) {
		set("ytenant", ytenant);
	}

}
