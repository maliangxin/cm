package com.yonyoucloud.fi.cmp.initdata;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.cmpentity.MoneyForm;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 期初余额实体
 *
 * @author u
 * @version 1.0
 */
public class InitData extends BizObject implements IAuditInfo, ITenant, AccentityRawInterface {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.initdata.InitData";

	/**
	 * 获取核算会计 主体
	 *
	 * @return 核算会计 主体.ID
	 */
	@Override
	public String getAccentityRaw() {
		return get("accentityRaw");
	}

	/**
	 * 设置核算会计 主体
	 *
	 * @param accentityRaw 核算会计 主体.ID
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
	 * 获取所属组织
	 *
	 * @return 所属组织.ID
	 */
	public String getParentAccentity() {
		return get("parentAccentity");
	}

	/**
	 * 设置所属组织
	 *
	 * @param parentAccentity 所属组织.ID
	 */
	public void setParentAccentity(String parentAccentity) {
		set("parentAccentity", parentAccentity);
	}


	/**
     * 获取会计期间
     *
     * @return 会计期间.ID
     */
	public Long getPeriod() {
		return get("period");
	}

    /**
     * 设置会计期间
     *
     * @param period 会计期间.ID
     */
	public void setPeriod(Long period) {
		set("period", period);
	}

    /**
     * 获取建账日期
     *
     * @return 建账日期
     */
	public java.util.Date getAccountdate() {
		return get("accountdate");
	}

    /**
     * 设置建账日期
     *
     * @param accountdate 建账日期
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
     * 获取银行方期初本币余额
     *
     * @return 银行方期初本币余额
     */
	public java.math.BigDecimal getBankinitlocalbalance() {
		return get("bankinitlocalbalance");
	}

    /**
     * 设置银行方期初本币余额
     *
     * @param bankinitlocalbalance 银行方期初本币余额
     */
	public void setBankinitlocalbalance(java.math.BigDecimal bankinitlocalbalance) {
		set("bankinitlocalbalance", bankinitlocalbalance);
	}

    /**
     * 获取企业方期初本币余额
     *
     * @return 企业方期初本币余额
     */
	public java.math.BigDecimal getCoinitlocalbalance() {
		return get("coinitlocalbalance");
	}

    /**
     * 设置企业方期初本币余额
     *
     * @param coinitlocalbalance 企业方期初本币余额
     */
	public void setCoinitlocalbalance(java.math.BigDecimal coinitlocalbalance) {
		set("coinitlocalbalance", coinitlocalbalance);
	}

    /**
     * 获取企业方账面本币余额
     *
     * @return 企业方账面本币余额
     */
	public java.math.BigDecimal getCobooklocalbalance() {
		return get("cobooklocalbalance");
	}

    /**
     * 设置企业方账面本币余额
     *
     * @param cobooklocalbalance 企业方账面本币余额
     */
	public void setCobooklocalbalance(java.math.BigDecimal cobooklocalbalance) {
		set("cobooklocalbalance", cobooklocalbalance);
	}

    /**
     * 获取银行方期初原币余额
     *
     * @return 银行方期初原币余额
     */
	public java.math.BigDecimal getBankinitoribalance() {
		return get("bankinitoribalance");
	}

    /**
     * 设置银行方期初原币余额
     *
     * @param bankinitoribalance 银行方期初原币余额
     */
	public void setBankinitoribalance(java.math.BigDecimal bankinitoribalance) {
		set("bankinitoribalance", bankinitoribalance);
	}

    /**
     * 获取企业方期初原币余额
     *
     * @return 企业方期初原币余额
     */
	public java.math.BigDecimal getCoinitloribalance() {
		return get("coinitloribalance");
	}

    /**
     * 设置企业方期初原币余额
     *
     * @param coinitloribalance 企业方期初原币余额
     */
	public void setCoinitloribalance(java.math.BigDecimal coinitloribalance) {
		set("coinitloribalance", coinitloribalance);
	}

    /**
     * 获取企业方账面原币余额
     *
     * @return 企业方账面原币余额
     */
	public java.math.BigDecimal getCobookoribalance() {
		return get("cobookoribalance");
	}

    /**
     * 设置企业方账面原币余额
     *
     * @param cobookoribalance 企业方账面原币余额
     */
	public void setCobookoribalance(java.math.BigDecimal cobookoribalance) {
		set("cobookoribalance", cobookoribalance);
	}


	/**
	 * 获取企业方账面原币余额（实占）
	 *
	 * @return 企业方账面原币余额（实占）
	 */
	public java.math.BigDecimal getCobooklocalbalancesettled() {
		return get("cobooklocalbalancesettled");
	}

	/**
	 * 设置企业方账面原币余额（实占）
	 *
	 * @param cobooklocalbalancesettled 企业方账面原币余额（实占）
	 */
	public void setCobooklocalbalancesettled(java.math.BigDecimal cobooklocalbalancesettled) {
		set("cobooklocalbalancesettled", cobooklocalbalancesettled);
	}

	/**
	 * 获取企业方账面原币余额（实占）
	 *
	 * @return 企业方账面原币余额（实占）
	 */
	public java.math.BigDecimal getCobookoribalancesettled() {
		return get("cobookoribalancesettled");
	}

	/**
	 * 设置企业方账面原币余额（实占）
	 *
	 * @param cobookoribalancesettled 企业方账面原币余额（实占）
	 */
	public void setCobookoribalancesettled(java.math.BigDecimal cobookoribalancesettled) {
		set("cobookoribalancesettled", cobookoribalancesettled);
	}



    /**
     * 获取日记账余额方向
     *
     * @return 日记账余额方向
     */
	public Direction getDirection() {
		Number v = get("direction");
		return Direction.find(v);
	}

    /**
     * 设置日记账余额方向
     *
     * @param direction 日记账余额方向
     */
	public void setDirection(Direction direction) {
		if (direction != null) {
			set("direction", direction.getValue());
		} else {
			set("direction", null);
		}
	}

	/**
	 * 获取银行方余额方向
	 *
	 * @return 银行方余额方向
	 */
	public Direction getBankdirection() {
		Number v = get("bankdirection");
		return Direction.find(v);
	}

	/**
	 * 设置银行方余额方向
	 *
	 * @param bankdirection 银行方余额方向
	 */
	public void setBankdirection(Direction bankdirection) {
		if (bankdirection != null) {
			set("bankdirection", bankdirection.getValue());
		} else {
			set("bankdirection", null);
		}
	}

    /**
     * 获取业务描述
     *
     * @return 业务描述
     */
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置业务描述
     *
     * @param description 业务描述
     */
	public void setDescription(String description) {
		set("description", description);
	}

	/**
	 * 获取银行账户银行类别
	 *
	 * @return 银行账户银行类别.ID
	 */
	public String getBanktype() {
		return get("banktype");
	}

	/**
	 * 设置银行账户银行类别
	 *
	 * @param banktype 银行账户银行类别.ID
	 */
	public void setBanktype(String banktype) {
		set("banktype", banktype);
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
	 * 期初标志
	 *
	 * @return 期初标志
	 */
	public Boolean getQzbz() {
		return getBoolean("qzbz");
	}

	/**
	 * 期初标志
	 *
	 * @param qzbz 期初标志
	 */
	public void setQzbz(Boolean qzbz) {
		set("qzbz", qzbz);
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
	 * 获取汇率类型
	 *
	 * @return 汇率类型.ID
	 */
	public String getExchangeRateType() {
		return get("exchangeRateType");
	}

	/**
	 * 设置汇率类型
	 *
	 * @param exchangeRateType 汇率类型.ID
	 */
	public void setExchangeRateType(String exchangeRateType) {
		set("exchangeRateType", exchangeRateType);
	}

	/**
	 * 获取透支控制
	 *
	 * @return 透支控制
	 */
	public Short getOverdraftCtrl() {
		return getShort("overdraftCtrl");
	}

	/**
	 * 设置透支控制
	 *
	 * @param overdraftCtrl 透支控制
	 */
	public void setOverdraftCtrl(Short overdraftCtrl) {
		set("overdraftCtrl", overdraftCtrl);
	}


	/**
	 * 获取升级标志
	 *
	 * @return 升级标志
	 */
	public Short getUpgradesign() {
		return getShort("upgradesign");
	}

	/**
	 * 设置升级标志
	 *
	 * @param upgradesign 升级标志
	 */
	public void setUpgradesign(Short upgradesign) {
		set("upgradesign", upgradesign);
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

	/**
	 * 获取账户期初子表集合
	 *
	 * @return 资金付款子表集合
	 */
	public java.util.List<InitDatab> InitDatab() {
		return getBizObjects("InitDatab", InitDatab.class);
	}

	/**
	 * 设置账户期初子表集合
	 *
	 * @param InitDatab 资金付款子表集合
	 */
	public void setInitDatab(java.util.List<InitDatab> InitDatab) {
		setBizObjects("InitDatab", InitDatab);
	}

    /**
     * 获取汇率折算方式
     *
     * @return 汇率折算方式
     */
    public Short getExchRateOps() {
        return getShort("exchRateOps");
    }

    /**
     * 设置汇率折算方式
     *
     * @param exchRateOps 汇率折算方式
     */
    public void setExchRateOps(Short exchRateOps) {
        set("exchRateOps", exchRateOps);
    }
}
