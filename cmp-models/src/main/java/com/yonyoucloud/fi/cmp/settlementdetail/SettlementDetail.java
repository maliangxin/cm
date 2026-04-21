package com.yonyoucloud.fi.cmp.settlementdetail;

import com.yonyoucloud.fi.cmp.cmpentity.MoneyForm;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 日结明细实体
 *
 * @author u
 * @version 1.0
 */
public class SettlementDetail extends BizObject implements IAuditInfo, ITenant, AccentityRawInterface {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.settlementdetail.SettlementDetail";

	/**
	 * 获取核算会计
	 *
	 * @return 核算会计.ID
	 */
	@Override
	public String getAccentityRaw() {
		return get("accentityRaw");
	}

	/**
	 * 设置核算会计
	 *
	 * @param accentityRaw 核算会计.ID
	 */
	@Override
	public void setAccentityRaw(String accentityRaw) {
		set("accentityRaw", accentityRaw);
	}

	/**
	 * 获取资金组织
	 *
	 * @return 资金组织.ID
	 */
	@Override
	public String getAccentity() {
		return get("accentity");
	}

	/**
	 * 设置资金组织
	 *
	 * @param accentity 资金组织.ID
	 */
	@Override
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

    /**
     * 获取日期
     *
     * @return 日期
     */
	public java.util.Date getAccountdate() {
		return get("accountdate");
	}

    /**
     * 设置日期
     *
     * @param accountdate 日期
     */
	public void setAccountdate(java.util.Date accountdate) {
		set("accountdate", accountdate);
	}

    /**
     * 获取资金形态
     *
     * @return 资金形态
     */
	public MoneyForm getMoneyform() {
		Number v = get("moneyform");
		return MoneyForm.find(v);
	}

    /**
     * 设置资金形态
     *
     * @param moneyform 资金形态
     */
	public void setMoneyform(MoneyForm moneyform) {
		if (moneyform != null) {
			set("moneyform", moneyform.getValue());
		} else {
			set("moneyform", null);
		}
	}

    /**
     * 获取银行账户
     *
     * @return 银行账户.ID
     */
	public String getBankaccount() {
		return get("bankaccount");
	}

    /**
     * 设置银行账户
     *
     * @param bankaccount 银行账户.ID
     */
	public void setBankaccount(String bankaccount) {
		set("bankaccount", bankaccount);
	}

    /**
     * 获取银行账号
     *
     * @return 银行账号
     */
	public String getBankaccountno() {
		return get("bankaccountno");
	}

    /**
     * 设置银行账号
     *
     * @param bankaccountno 银行账号
     */
	public void setBankaccountno(String bankaccountno) {
		set("bankaccountno", bankaccountno);
	}

    /**
     * 获取现金账户
     *
     * @return 现金账户.ID
     */
	public String getCashaccount() {
		return get("cashaccount");
	}

    /**
     * 设置现金账户
     *
     * @param cashaccount 现金账户.ID
     */
	public void setCashaccount(String cashaccount) {
		set("cashaccount", cashaccount);
	}

    /**
     * 获取现金账号
     *
     * @return 现金账号
     */
	public String getCashaccountno() {
		return get("cashaccountno");
	}

    /**
     * 设置现金账号
     *
     * @param cashaccountno 现金账号
     */
	public void setCashaccountno(String cashaccountno) {
		set("cashaccountno", cashaccountno);
	}

    /**
     * 获取币种
     *
     * @return 币种.ID
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置币种
     *
     * @param currency 币种.ID
     */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

    /**
     * 获取汇率
     *
     * @return 汇率
     */
	public java.math.BigDecimal getExchangerate() {
		return get("exchangerate");
	}

    /**
     * 设置汇率
     *
     * @param exchangerate 汇率
     */
	public void setExchangerate(java.math.BigDecimal exchangerate) {
		set("exchangerate", exchangerate);
	}

    /**
     * 获取昨日本币余额
     *
     * @return 昨日本币余额
     */
	public java.math.BigDecimal getYesterdaylocalmoney() {
		return get("yesterdaylocalmoney");
	}

    /**
     * 设置昨日本币余额
     *
     * @param yesterdaylocalmoney 昨日本币余额
     */
	public void setYesterdaylocalmoney(java.math.BigDecimal yesterdaylocalmoney) {
		set("yesterdaylocalmoney", yesterdaylocalmoney);
	}

    /**
     * 获取昨日原币余额
     *
     * @return 昨日原币余额
     */
	public java.math.BigDecimal getYesterdayorimoney() {
		return get("yesterdayorimoney");
	}

    /**
     * 设置昨日原币余额
     *
     * @param yesterdayorimoney 昨日原币余额
     */
	public void setYesterdayorimoney(java.math.BigDecimal yesterdayorimoney) {
		set("yesterdayorimoney", yesterdayorimoney);
	}

    /**
     * 获取今日借方本币合计
     *
     * @return 今日借方本币合计
     */
	public java.math.BigDecimal getTodaydebitlocalmoneysum() {
		return get("todaydebitlocalmoneysum");
	}

    /**
     * 设置今日借方本币合计
     *
     * @param todaydebitlocalmoneysum 今日借方本币合计
     */
	public void setTodaydebitlocalmoneysum(java.math.BigDecimal todaydebitlocalmoneysum) {
		set("todaydebitlocalmoneysum", todaydebitlocalmoneysum);
	}

    /**
     * 获取今日借方原币合计
     *
     * @return 今日借方原币合计
     */
	public java.math.BigDecimal getTodaydebitorimoneysum() {
		return get("todaydebitorimoneysum");
	}

    /**
     * 设置今日借方原币合计
     *
     * @param todaydebitorimoneysum 今日借方原币合计
     */
	public void setTodaydebitorimoneysum(java.math.BigDecimal todaydebitorimoneysum) {
		set("todaydebitorimoneysum", todaydebitorimoneysum);
	}

    /**
     * 获取今日贷方本币合计
     *
     * @return 今日贷方本币合计
     */
	public java.math.BigDecimal getTodaycreditlocalmoneysum() {
		return get("todaycreditlocalmoneysum");
	}

    /**
     * 设置今日贷方本币合计
     *
     * @param todaycreditlocalmoneysum 今日贷方本币合计
     */
	public void setTodaycreditlocalmoneysum(java.math.BigDecimal todaycreditlocalmoneysum) {
		set("todaycreditlocalmoneysum", todaycreditlocalmoneysum);
	}

    /**
     * 获取今日贷方原币合计
     *
     * @return 今日贷方原币合计
     */
	public java.math.BigDecimal getTodaycreditorimoneysum() {
		return get("todaycreditorimoneysum");
	}

    /**
     * 设置今日贷方原币合计
     *
     * @param todaycreditorimoneysum 今日贷方原币合计
     */
	public void setTodaycreditorimoneysum(java.math.BigDecimal todaycreditorimoneysum) {
		set("todaycreditorimoneysum", todaycreditorimoneysum);
	}

    /**
     * 获取今日本币余额
     *
     * @return 今日本币余额
     */
	public java.math.BigDecimal getTodaylocalmoney() {
		return get("todaylocalmoney");
	}

    /**
     * 设置今日本币余额
     *
     * @param todaylocalmoney 今日本币余额
     */
	public void setTodaylocalmoney(java.math.BigDecimal todaylocalmoney) {
		set("todaylocalmoney", todaylocalmoney);
	}

    /**
     * 获取今日原币余额
     *
     * @return 今日原币余额
     */
	public java.math.BigDecimal getTodayorimoney() {
		return get("todayorimoney");
	}

    /**
     * 设置今日原币余额
     *
     * @param todayorimoney 今日原币余额
     */
	public void setTodayorimoney(java.math.BigDecimal todayorimoney) {
		set("todayorimoney", todayorimoney);
	}

    /**
     * 获取借方笔数
     *
     * @return 借方笔数
     */
	public java.math.BigDecimal getTodaydebitnum() {
		return get("todaydebitnum");
	}

    /**
     * 设置借方笔数
     *
     * @param todaydebitnum 借方笔数
     */
	public void setTodaydebitnum(java.math.BigDecimal todaydebitnum) {
		set("todaydebitnum", todaydebitnum);
	}

    /**
     * 获取贷方笔数
     *
     * @return 贷方笔数
     */
	public java.math.BigDecimal getTodaycreditnum() {
		return get("todaycreditnum");
	}

    /**
     * 设置贷方笔数
     *
     * @param todaycreditnum 贷方笔数
     */
	public void setTodaycreditnum(java.math.BigDecimal todaycreditnum) {
		set("todaycreditnum", todaycreditnum);
	}


	/**
	 * 获取开户行
	 *
	 * @return 开户行
	 */
	public String getBankname() {
		return get("bankname");
	}

	/**
	 * 设置开户行
	 *
	 * @param bankname 开户行
	 */
	public void setBankname(String bankname) {
		set("bankname", bankname);
	}

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
	public java.util.Date getCreateTime() {
		return get("createTime");
	}

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
	public void setCreateTime(java.util.Date createTime) {
		set("createTime", createTime);
	}

    /**
     * 获取创建日期
     *
     * @return 创建日期
     */
	public java.util.Date getCreateDate() {
		return get("createDate");
	}

    /**
     * 设置创建日期
     *
     * @param createDate 创建日期
     */
	public void setCreateDate(java.util.Date createDate) {
		set("createDate", createDate);
	}

    /**
     * 获取修改时间
     *
     * @return 修改时间
     */
	public java.util.Date getModifyTime() {
		return get("modifyTime");
	}

    /**
     * 设置修改时间
     *
     * @param modifyTime 修改时间
     */
	public void setModifyTime(java.util.Date modifyTime) {
		set("modifyTime", modifyTime);
	}

    /**
     * 获取修改日期
     *
     * @return 修改日期
     */
	public java.util.Date getModifyDate() {
		return get("modifyDate");
	}

    /**
     * 设置修改日期
     *
     * @param modifyDate 修改日期
     */
	public void setModifyDate(java.util.Date modifyDate) {
		set("modifyDate", modifyDate);
	}

    /**
     * 获取创建人
     *
     * @return 创建人
     */
	public String getCreator() {
		return get("creator");
	}

    /**
     * 设置创建人
     *
     * @param creator 创建人
     */
	public void setCreator(String creator) {
		set("creator", creator);
	}

    /**
     * 获取修改人
     *
     * @return 修改人
     */
	public String getModifier() {
		return get("modifier");
	}

    /**
     * 设置修改人
     *
     * @param modifier 修改人
     */
	public void setModifier(String modifier) {
		set("modifier", modifier);
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
	 * 获取本币币种
	 *
	 * @return 本币币种.ID
	 */
	public String getNatCurrency() {
		return get("natCurrency");
	}

	/**
	 * 设置本币币种
	 *
	 * @param natCurrency 本币币种.ID
	 */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
	}

	/**
	 * 获取联查用资金组织
	 *
	 * @return 联查用资金组织
	 */
	public String getAccentityForQuery() {
		return get("accentityForQuery");
	}

	/**
	 * 设置联查用资金组织
	 *
	 * @param accentityForQuery 联查用资金组织
	 */
	public void setAccentityForQuery(String accentityForQuery) {
		set("accentityForQuery", accentityForQuery);
	}

	/**
	 * 获取联查用银行账号
	 *
	 * @return 联查用银行账号
	 */
	public String getBankaccountForQuery() {
		return get("bankaccountForQuery");
	}

	/**
	 * 设置联查用银行账号
	 *
	 * @param bankaccountForQuery 联查用银行账号
	 */
	public void setBankaccountForQuery(String bankaccountForQuery) {
		set("bankaccountForQuery", bankaccountForQuery);
	}

	/**
	 * 获取联查用现金账号
	 *
	 * @return 联查用现金账号
	 */
	public String getCashaccountForQuery() {
		return get("cashaccountForQuery");
	}

	/**
	 * 设置联查用现金账号
	 *
	 * @param cashaccountForQuery 联查用现金账号
	 */
	public void setCashaccountForQuery(String cashaccountForQuery) {
		set("cashaccountForQuery", cashaccountForQuery);
	}

	/**
	 * 获取联查用币种
	 *
	 * @return 联查用币种
	 */
	public String getCurrencyForQuery() {
		return get("currencyForQuery");
	}

	/**
	 * 设置联查用币种
	 *
	 * @param currencyForQuery 联查用币种
	 */
	public void setCurrencyForQuery(String currencyForQuery) {
		set("currencyForQuery", currencyForQuery);
	}

	/**
	 * 获取报表查询租户
	 *
	 * @return 报表查询租户
	 */
	public String getTenantIdForReport() {
		return get("tenantIdForReport");
	}

	/**
	 * 设置报表查询租户
	 *
	 * @param tenantIdForReport 报表查询租户
	 */
	public void setTenantIdForReport(String tenantIdForReport) {
		set("tenantIdForReport", tenantIdForReport);
	}

	/**
	 * 获取报表查询币种
	 *
	 * @return 报表查询币种
	 */
	public String getCurrencyForReport() {
		return get("currencyForReport");
	}

	/**
	 * 设置报表查询币种
	 *
	 * @param currencyForReport 报表查询币种
	 */
	public void setCurrencyForReport(String currencyForReport) {
		set("currencyForReport", currencyForReport);
	}

	/**
	 * 获取开户类型
	 *
	 * @return 开户类型
	 */
	public Short getOpentype() {
		return getShort("opentype");
	}

	/**
	 * 设置开户类型
	 *
	 * @param opentype 开户类型
	 */
	public void setOpentype(Short opentype) {
		set("opentype", opentype);
	}

	/**
	 * 获取账户类型
	 *
	 * @return 账户类型
	 */
	public Short getAccttype() {
		return getShort("accttype");
	}

	/**
	 * 设置账户类型
	 *
	 * @param accttype 账户类型
	 */
	public void setAccttype(Short accttype) {
		set("accttype", accttype);
	}

	/**
	 * 获取账户性质
	 *
	 * @return 账户性质
	 */
	public Short getAcctnature() {
		return getShort("acctnature");
	}

	/**
	 * 设置账户性质
	 *
	 * @param acctnature 账户性质
	 */
	public void setAcctnature(Short acctnature) {
		set("acctnature", acctnature);
	}
}
