package com.yonyoucloud.fi.cmp.currencyexchange;

import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyou.ucf.mdd.ext.voucher.base.Vouch;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;

import java.util.Date;

/**
 * 外币兑换实体
 *
 * @author u
 * @version 1.0
 */
public class CurrencyExchange extends Vouch implements IApprovalInfo, ICurrency, IApprovalFlow, IPrintCount, AccentityRawInterface {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.currencyexchange.CurrencyExchange";
	// 业务对象编码
	public static final String BUSI_OBJ_CODE = "ctm-cmp.cmp_currencyexchange";
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
     * 获取交易类型
     *
     * @return 交易类型.ID
     */
	public String getTradetype() {
		return get("tradetype");
	}

    /**
     * 设置交易类型
     *
     * @param tradetype 交易类型.ID
     */
	public void setTradetype(String tradetype) {
		set("tradetype", tradetype);
	}

    /**
     * 获取单据编号
     *
     * @return 单据编号
     */
	public String getOrderno() {
		return get("orderno");
	}

    /**
     * 设置单据编号
     *
     * @param orderno 单据编号
     */
	public void setOrderno(String orderno) {
		set("orderno", orderno);
	}

	/**
	 * 获取状态
	 *
	 * @return 状态
	 */
	public Bsflag getFlag() {
		Number v = get("flag");
		return Bsflag.find(v);
	}

	/**
	 * 设置状态
	 *
	 * @param flag 状态
	 */
	public void setFlag(Bsflag flag) {
		if (flag != null) {
			set("flag", flag.getValue());
		} else {
			set("flag", null);
		}
	}


    /**
     * 获取结算方式
     *
     * @return 结算方式.ID
     */
	public Long getSettlemode() {
		return get("settlemode");
	}

    /**
     * 设置结算方式
     *
     * @param settlemode 结算方式.ID
     */
	public void setSettlemode(Long settlemode) {
		set("settlemode", settlemode);
	}

    /**
     * 获取买入银行账户
     *
     * @return 买入银行账户.ID
     */
	public String getPurchasebankaccount() {
		return get("purchasebankaccount");
	}

    /**
     * 设置买入银行账户
     *
     * @param purchasebankaccount 买入银行账户.ID
     */
	public void setPurchasebankaccount(String purchasebankaccount) {
		set("purchasebankaccount", purchasebankaccount);
	}

    /**
     * 获取买入账户币种
     *
     * @return 买入账户币种.ID
     */
	public String getPurchaseCurrency() {
		return get("purchaseCurrency");
	}

    /**
     * 设置买入账户币种
     *
     * @param purchaseCurrency 买入账户币种.ID
     */
	public void setPurchaseCurrency(String purchaseCurrency) {
		set("purchaseCurrency", purchaseCurrency);
	}

    /**
     * 获取买入金额
     *
     * @return 买入金额
     */
	public java.math.BigDecimal getPurchaseamount() {
		return get("purchaseamount");
	}

    /**
     * 设置买入金额
     *
     * @param purchaseamount 买入金额
     */
	public void setPurchaseamount(java.math.BigDecimal purchaseamount) {
		set("purchaseamount", purchaseamount);
	}

    /**
     * 获取买入汇率
     *
     * @return 买入汇率
     */
	public java.math.BigDecimal getPurchaserate() {
		return get("purchaserate");
	}

    /**
     * 设置买入汇率
     *
     * @param purchaserate 买入汇率
     */
	public void setPurchaserate(java.math.BigDecimal purchaserate) {
		set("purchaserate", purchaserate);
	}

    /**
     * 获取买入本币金额
     *
     * @return 买入本币金额
     */
	public java.math.BigDecimal getPurchaselocalamount() {
		return get("purchaselocalamount");
	}

    /**
     * 设置买入本币金额
     *
     * @param purchaselocalamount 买入本币金额
     */
	public void setPurchaselocalamount(java.math.BigDecimal purchaselocalamount) {
		set("purchaselocalamount", purchaselocalamount);
	}

    /**
     * 获取卖出银行账户
     *
     * @return 卖出银行账户.ID
     */
	public String getSellbankaccount() {
		return get("sellbankaccount");
	}

    /**
     * 设置卖出银行账户
     *
     * @param sellbankaccount 卖出银行账户.ID
     */
	public void setSellbankaccount(String sellbankaccount) {
		set("sellbankaccount", sellbankaccount);
	}

    /**
     * 获取卖出账户币种
     *
     * @return 卖出账户币种.ID
     */
	public String getSellCurrency() {
		return get("sellCurrency");
	}

    /**
     * 设置卖出账户币种
     *
     * @param sellCurrency 卖出账户币种.ID
     */
	public void setSellCurrency(String sellCurrency) {
		set("sellCurrency", sellCurrency);
	}

    /**
     * 获取卖出金额
     *
     * @return 卖出金额
     */
	public java.math.BigDecimal getSellamount() {
		return get("sellamount");
	}

    /**
     * 设置卖出金额
     *
     * @param sellamount 卖出金额
     */
	public void setSellamount(java.math.BigDecimal sellamount) {
		set("sellamount", sellamount);
	}

    /**
     * 获取卖出汇率
     *
     * @return 卖出汇率
     */
	public java.math.BigDecimal getSellrate() {
		return get("sellrate");
	}

    /**
     * 设置卖出汇率
     *
     * @param sellrate 卖出汇率
     */
	public void setSellrate(java.math.BigDecimal sellrate) {
		set("sellrate", sellrate);
	}

    /**
     * 获取卖出本币金额
     *
     * @return 卖出本币金额
     */
	public java.math.BigDecimal getSellloaclamount() {
		return get("sellloaclamount");
	}

    /**
     * 设置卖出本币金额
     *
     * @param sellloaclamount 卖出本币金额
     */
	public void setSellloaclamount(java.math.BigDecimal sellloaclamount) {
		set("sellloaclamount", sellloaclamount);
	}

    /**
     * 获取手续费账户
     *
     * @return 手续费账户.ID
     */
	public String getCommissionbankaccount() {
		return get("commissionbankaccount");
	}

    /**
     * 设置手续费账户
     *
     * @param commissionbankaccount 手续费账户.ID
     */
	public void setCommissionbankaccount(String commissionbankaccount) {
		set("commissionbankaccount", commissionbankaccount);
	}

    /**
     * 获取手续费账户币种
     *
     * @return 手续费账户币种.ID
     */
	public String getCommissionCurrency() {
		return get("commissionCurrency");
	}

    /**
     * 设置手续费账户币种
     *
     * @param commissionCurrency 手续费账户币种.ID
     */
	public void setCommissionCurrency(String commissionCurrency) {
		set("commissionCurrency", commissionCurrency);
	}

    /**
     * 获取手续费金额
     *
     * @return 手续费金额
     */
	public java.math.BigDecimal getCommissionamount() {
		return get("commissionamount");
	}

    /**
     * 设置手续费金额
     *
     * @param commissionamount 手续费金额
     */
	public void setCommissionamount(java.math.BigDecimal commissionamount) {
		set("commissionamount", commissionamount);
	}

    /**
     * 获取手续费汇率
     *
     * @return 手续费汇率
     */
	public java.math.BigDecimal getCommissionrate() {
		return get("commissionrate");
	}

    /**
     * 设置手续费汇率
     *
     * @param commissionrate 手续费汇率
     */
	public void setCommissionrate(java.math.BigDecimal commissionrate) {
		set("commissionrate", commissionrate);
	}

    /**
     * 获取手续费本币金额
     *
     * @return 手续费本币金额
     */
	public java.math.BigDecimal getCommissionlocalamount() {
		return get("commissionlocalamount");
	}

    /**
     * 设置手续费本币金额
     *
     * @param commissionlocalamount 手续费本币金额
     */
	public void setCommissionlocalamount(java.math.BigDecimal commissionlocalamount) {
		set("commissionlocalamount", commissionlocalamount);
	}

    /**
     * 获取资金来源
     *
     * @return 资金来源
     */
	public String getFundsource() {
		return get("fundsource");
	}

    /**
     * 设置资金来源
     *
     * @param fundsource 资金来源
     */
	public void setFundsource(String fundsource) {
		set("fundsource", fundsource);
	}

    /**
     * 获取资金用途
     *
     * @return 资金用途
     */
	public String getFundtarget() {
		return get("fundtarget");
	}

    /**
     * 设置资金用途
     *
     * @param fundtarget 资金用途
     */
	public void setFundtarget(String fundtarget) {
		set("fundtarget", fundtarget);
	}

	/**
	 * 获取结算人
	 *
	 * @return 结算人.ID
	 */
	public Long getSettleuser() {
		return get("settleuser");
	}

	/**
	 * 设置结算人
	 *
	 * @param settleuser 结算人.ID
	 */
	public void setSettleuser(Long settleuser) {
		set("settleuser", settleuser);
	}
    /**
     * 获取部门
     *
     * @return 部门.ID
     */
	public String getDept() {
		return get("dept");
	}

    /**
     * 设置部门
     *
     * @param dept 部门.ID
     */
	public void setDept(String dept) {
		set("dept", dept);
	}

    /**
     * 获取备注
     *
     * @return 备注
     */
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置备注
     *
     * @param description 备注
     */
	public void setDescription(String description) {
		set("description", description);
	}

    /**
     * 获取项目
     *
     * @return 项目.ID
     */
	public String getProject() {
		return get("project");
	}

    /**
     * 设置项目
     *
     * @param project 项目.ID
     */
	public void setProject(String project) {
		set("project", project);
	}

    /**
     * 获取摘要
     *
     * @return 摘要
     */
	public String getSummary() {
		return get("summary");
	}

    /**
     * 设置摘要
     *
     * @param summary 摘要
     */
	public void setSummary(String summary) {
		set("summary", summary);
	}

    /**
     * 获取事项来源
     *
     * @return 事项来源
     */
	public EventSource getSrcitem() {
		Number v = get("srcitem");
		return EventSource.find(v);
	}

    /**
     * 设置事项来源
     *
     * @param srcitem 事项来源
     */
	public void setSrcitem(EventSource srcitem) {
		if (srcitem != null) {
			set("srcitem", srcitem.getValue());
		} else {
			set("srcitem", null);
		}
	}

	/**
	 * 获取单据状态
	 *
	 * @return 单据状态
	 */
	public BillStatus getBillstatus() {
		Number v = get("billstatus");
		return BillStatus.find(v);
	}

	/**
	 * 设置单据状态
	 *
	 * @param billstatus 单据状态
	 */
	public void setBillstatus(BillStatus billstatus) {
		if (billstatus != null) {
			set("billstatus", billstatus.getValue());
		} else {
			set("billstatus", null);
		}
	}


	/**
	 * 获取兑换类型
	 *
	 * @return 兑换类型
	 */
	public ExchangeType getExchangetype() {
		Number v = get("exchangetype");
		return ExchangeType.find(v);
	}

	/**
	 * 设置兑换类型
	 *
	 * @param exchangetype 兑换类型
	 */
	public void setExchangetype(ExchangeType exchangetype) {
		if (exchangetype != null) {
			set("exchangetype", exchangetype.getValue());
		} else {
			set("exchangetype", null);
		}
	}




    /**
     * 获取交割状态
     *
     * @return 交割状态
     */
	public DeliveryStatus getSettlestatus() {
		Number v = get("settlestatus");
		return DeliveryStatus.find(v);
	}

    /**
     * 设置交割状态
     *
     * @param settlestatus 交割状态
     */
	public void setSettlestatus(DeliveryStatus settlestatus) {
		if (settlestatus != null) {
			set("settlestatus", settlestatus.getValue());
		} else {
			set("settlestatus", null);
		}
	}

    /**
     * 获取审批状态
     *
     * @return 审批状态
     */
	public AuditStatus getAuditstatus() {
		Number v = get("auditstatus");
		return AuditStatus.find(v);
	}

    /**
     * 设置审批状态
     *
     * @param auditstatus 审批状态
     */
	public void setAuditstatus(AuditStatus auditstatus) {
		if (auditstatus != null) {
			set("auditstatus", auditstatus.getValue());
		} else {
			set("auditstatus", null);
		}
	}

    /**
     * 获取审批人名称
     *
     * @return 审批人名称
     */
	public String getAuditor() {
		return get("auditor");
	}

    /**
     * 设置审批人名称
     *
     * @param auditor 审批人名称
     */
	public void setAuditor(String auditor) {
		set("auditor", auditor);
	}

    /**
     * 获取审批人
     *
     * @return 审批人.ID
     */
	public Long getAuditorId() {
		return get("auditorId");
	}

    /**
     * 设置审批人
     *
     * @param auditorId 审批人.ID
     */
	public void setAuditorId(Long auditorId) {
		set("auditorId", auditorId);
	}

    /**
     * 获取审批时间
     *
     * @return 审批时间
     */
	public java.util.Date getAuditTime() {
		return get("auditTime");
	}

    /**
     * 设置审批时间
     *
     * @param auditTime 审批时间
     */
	public void setAuditTime(java.util.Date auditTime) {
		set("auditTime", auditTime);
	}

    /**
     * 获取审批日期
     *
     * @return 审批日期
     */
	public java.util.Date getAuditDate() {
		return get("auditDate");
	}

    /**
     * 设置审批日期
     *
     * @param auditDate 审批日期
     */
	public void setAuditDate(java.util.Date auditDate) {
		set("auditDate", auditDate);
	}

    /**
     * 获取原币
     *
     * @return 原币.ID
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置原币
     *
     * @param currency 原币.ID
     */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

    /**
     * 获取本币
     *
     * @return 本币.ID
     */
	public String getNatCurrency() {
		return get("natCurrency");
	}

    /**
     * 设置本币
     *
     * @param natCurrency 本币.ID
     */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
	}

    /**
     * 获取汇率
     *
     * @return 汇率
     */
	public java.math.BigDecimal getExchRate() {
		return get("exchRate");
	}

    /**
     * 设置汇率
     *
     * @param exchRate 汇率
     */
	public void setExchRate(java.math.BigDecimal exchRate) {
		set("exchRate", exchRate);
	}


	/**
	 * 获取凭证状态
	 *
	 * @return 凭证状态
	 */
	public VoucherStatus getVoucherstatus() {
		Number v = get("voucherstatus");
		return VoucherStatus.find(v);
	}

	/**
	 * 设置凭证状态
	 *
	 * @param voucherstatus 凭证状态
	 */
	public void setVoucherstatus(VoucherStatus voucherstatus) {
		if (voucherstatus != null) {
			set("voucherstatus", voucherstatus.getValue());
		} else {
			set("voucherstatus", null);
		}
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
	 * 获取兑换汇率
	 *
	 * @return 兑换汇率
	 */
	public java.math.BigDecimal getExchangerate() {
		return get("exchangerate");
	}

	/**
	 * 设置兑换汇率
	 *
	 * @param exchangerate 兑换汇率
	 */
	public void setExchangerate(java.math.BigDecimal exchangerate) {
		set("exchangerate", exchangerate);
	}




	/**
	 * 获取汇兑损溢
	 *
	 * @return 汇兑损溢
	 */
	public java.math.BigDecimal getExchangeloss() {
		return get("exchangeloss");
	}

	/**
	 * 设置汇兑损溢
	 *
	 * @param exchangeloss 汇兑损溢
	 */
	public void setExchangeloss(java.math.BigDecimal exchangeloss) {
		set("exchangeloss", exchangeloss);
	}


	public Date getSettledate() {
		return (Date)this.get("settledate");
	}

	public void setSettledate(Date settledate) {
		this.set("settledate", settledate);
	}
	/**
	 * 获取是否审批流控制
	 *
	 * @return 是否审批流控制
	 */
	public Boolean getIsWfControlled() {
		return getBoolean("isWfControlled");
	}

	/**
	 * 设置是否审批流控制
	 *
	 * @param isWfControlled 是否审批流控制
	 */
	public void setIsWfControlled(Boolean isWfControlled) {
		set("isWfControlled", isWfControlled);
	}

	/**
	 * 获取审批状态
	 *
	 * @return 审批状态
	 */
	public Short getVerifystate() {
		return getShort("verifystate");
	}

	/**
	 * 设置审批状态
	 *
	 * @param verifystate 审批状态
	 */
	public void setVerifystate(Short verifystate) {
		set("verifystate", verifystate);
	}

	/**
	 * 获取退回次数
	 *
	 * @return 退回次数
	 */
	public Short getReturncount() {
		return getShort("returncount");
	}

	/**
	 * 设置退回次数
	 *
	 * @param returncount 退回次数
	 */
	public void setReturncount(Short returncount) {
		set("returncount", returncount);
	}

	/**
	 * 获取打印次数
	 *
	 * @return 打印次数
	 */
	public Integer getPrintCount() {
		return get("printCount");
	}

	/**
	 * 设置打印次数
	 *
	 * @param printCount 打印次数
	 */
	public void setPrintCount(Integer printCount) {
		set("printCount", printCount);
	}

	/**
	 * 获取买入现金账户
	 *
	 * @return 买入现金账户.ID
	 */
	public String getPurchasecashaccount() {
		return get("purchasecashaccount");
	}

	/**
	 * 设置买入现金账户
	 *
	 * @param purchasecashaccount 买入现金账户.ID
	 */
	public void setPurchasecashaccount(String purchasecashaccount) {
		set("purchasecashaccount", purchasecashaccount);
	}

	/**
	 * 获取卖出现金账户
	 *
	 * @return 卖出现金账户.ID
	 */
	public String getSellcashaccount() {
		return get("sellcashaccount");
	}

	/**
	 * 设置卖出现金账户
	 *
	 * @param sellcashaccount 卖出现金账户.ID
	 */
	public void setSellcashaccount(String sellcashaccount) {
		set("sellcashaccount", sellcashaccount);
	}

	/**
	 * 获取手续费现金账户
	 *
	 * @return 手续费现金账户.ID
	 */
	public String getCommissioncashaccount() {
		return get("commissioncashaccount");
	}

	/**
	 * 设置手续费现金账户
	 *
	 * @param commissioncashaccount 手续费现金账户.ID
	 */
	public void setCommissioncashaccount(String commissioncashaccount) {
		set("commissioncashaccount", commissioncashaccount);
	}

	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDef 自定义项特征属性组.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
	}

	/**
	 * 获取凭证号
	 *
	 * @return 凭证号
	 */
	public String getVoucherNo() {
		return get("voucherNo");
	}

	/**
	 * 设置凭证号
	 *
	 * @param voucherNo 凭证号
	 */
	public void setVoucherNo(String voucherNo) {
		set("voucherNo", voucherNo);
	}

	/**
	 * 获取凭证期间
	 *
	 * @return 凭证期间
	 */
	public String getVoucherPeriod() {
		return get("voucherPeriod");
	}

	/**
	 * 设置凭证期间
	 *
	 * @param voucherPeriod 凭证期间
	 */
	public void setVoucherPeriod(String voucherPeriod) {
		set("voucherPeriod", voucherPeriod);
	}

	/**
	 * 获取事项分录ID|凭证ID
	 *
	 * @return 事项分录ID|凭证ID
	 */
	public String getVoucherId() {
		return get("voucherId");
	}

	/**
	 * 设置事项分录ID|凭证ID
	 *
	 * @param voucherId 事项分录ID|凭证ID
	 */
	public void setVoucherId(String voucherId) {
		set("voucherId", voucherId);
	}

	/**
	 * 获取交割方式
	 *
	 * @return 交割方式
	 */
	public Short getDeliveryType() {
		return getShort("deliveryType");
	}

	/**
	 * 设置交割方式
	 *
	 * @param deliveryType 交割方式
	 */
	public void setDeliveryType(Short deliveryType) {
		set("deliveryType", deliveryType);
	}
	/**
	 * 获取外汇交易合约编号
	 *
	 * @return 外汇交易合约编号
	 */
	public String getContractNo() {
		return get("contractNo");
	}

	/**
	 * 设置外汇交易合约编号
	 *
	 * @param contractNo 外汇交易合约编号
	 */
	public void setContractNo(String contractNo) {
		set("contractNo", contractNo);
	}

	/**
	 * 获取是否延时交割
	 *
	 * @return 是否延时交割
	 */
	public Boolean getIsDelayed() {
		return getBoolean("isDelayed");
	}

	/**
	 * 设置是否延时交割
	 *
	 * @param isDelayed 是否延时交割
	 */
	public void setIsDelayed(Boolean isDelayed) {
		set("isDelayed", isDelayed);
	}
	/**
	 * 获取委托类型
	 *
	 * @return 委托类型
	 */
	public Short getDelegationType() {
		return getShort("delegationType");
	}

	/**
	 * 设置委托类型
	 *
	 * @param delegationType 委托类型
	 */
	public void setDelegationType(Short delegationType) {
		set("delegationType", delegationType);
	}

	/**
	 * 获取延时交割日期
	 *
	 * @return 延时交割日期
	 */
	public java.util.Date getDelayedDate() {
		return get("delayedDate");
	}

	/**
	 * 设置延时交割日期
	 *
	 * @param delayedDate 延时交割日期
	 */
	public void setDelayedDate(java.util.Date delayedDate) {
		set("delayedDate", delayedDate);
	}

	/**
	 * 获取询价方式
	 *
	 * @return 询价方式
	 */
	public Short getInquiryType() {
		return getShort("inquiryType");
	}

	/**
	 * 设置询价方式
	 *
	 * @param inquiryType 询价方式
	 */
	public void setInquiryType(Short inquiryType) {
		set("inquiryType", inquiryType);
	}

	/**
	 * 获取询价汇率
	 *
	 * @return 询价汇率
	 */
	public java.math.BigDecimal getInquiryExchangerate() {
		return get("inquiryExchangerate");
	}

	/**
	 * 设置询价汇率
	 *
	 * @param inquiryExchangerate 询价汇率
	 */
	public void setInquiryExchangerate(java.math.BigDecimal inquiryExchangerate) {
		set("inquiryExchangerate", inquiryExchangerate);
	}

	/**
	 * 获取询价编号
	 *
	 * @return 询价编号
	 */
	public String getInquiryNo() {
		return get("inquiryNo");
	}

	/**
	 * 设置询价编号
	 *
	 * @param inquiryNo 询价编号
	 */
	public void setInquiryNo(String inquiryNo) {
		set("inquiryNo", inquiryNo);
	}

	/**
	 * 获取是否待检查账户
	 *
	 * @return 是否待检查账户
	 */
	public Boolean getIsCheckAccount() {
		return getBoolean("isCheckAccount");
	}

	/**
	 * 设置是否待检查账户
	 *
	 * @param isCheckAccount 是否待检查账户
	 */
	public void setIsCheckAccount(Boolean isCheckAccount) {
		set("isCheckAccount", isCheckAccount);
	}

	/**
	 * 获取合格质押品占用类型
	 *
	 * @return 合格质押品占用类型
	 */
	public Short getCollateralOccupation() {
		return getShort("collateralOccupation");
	}

	/**
	 * 设置合格质押品占用类型
	 *
	 * @param collateralOccupation 合格质押品占用类型
	 */
	public void setCollateralOccupation(Short collateralOccupation) {
		set("collateralOccupation", collateralOccupation);
	}

	/**
	 * 获取保证金账户选择标志
	 *
	 * @return 保证金账户选择标志
	 */
	public Short getMarginAccountFlag() {
		return getShort("marginAccountFlag");
	}

	/**
	 * 设置保证金账户选择标志
	 *
	 * @param marginAccountFlag 保证金账户选择标志
	 */
	public void setMarginAccountFlag(Short marginAccountFlag) {
		set("marginAccountFlag", marginAccountFlag);
	}

	/**
	 * 获取保证金账号
	 *
	 * @return 保证金账号.ID
	 */
	public String getDepositAccountNo() {
		return get("depositAccountNo");
	}

	/**
	 * 设置保证金账号
	 *
	 * @param depositAccountNo 保证金账号.ID
	 */
	public void setDepositAccountNo(String depositAccountNo) {
		set("depositAccountNo", depositAccountNo);
	}

	/**
	 * 获取保证金币种
	 *
	 * @return 保证金币种.ID
	 */
	public String getDepositCurrency() {
		return get("depositCurrency");
	}

	/**
	 * 设置保证金币种
	 *
	 * @param depositCurrency 保证金币种.ID
	 */
	public void setDepositCurrency(String depositCurrency) {
		set("depositCurrency", depositCurrency);
	}

	/**
	 * 获取项目类别
	 *
	 * @return 项目类别
	 */
	public Short getProjectType() {
		return getShort("projectType");
	}

	/**
	 * 设置项目类别
	 *
	 * @param projectType 项目类别
	 */
	public void setProjectType(Short projectType) {
		set("projectType", projectType);
	}

	/**
	 * 获取交易编码
	 *
	 * @return 交易编码.ID
	 */
	public Long getTransactionCode() {
		return get("transactionCode");
	}

	/**
	 * 设置交易编码
	 *
	 * @param transactionCode 交易编码.ID
	 */
	public void setTransactionCode(Long transactionCode) {
		set("transactionCode", transactionCode);
	}

	/**
	 * 获取统计代码
	 *
	 * @return 统计代码
	 */
	public String getStatisticalCode() {
		return get("statisticalCode");
	}

	/**
	 * 设置统计代码
	 *
	 * @param statisticalCode 统计代码
	 */
	public void setStatisticalCode(String statisticalCode) {
		set("statisticalCode", statisticalCode);
	}
	/**
	 * 获取结售汇用途代码
	 *
	 * @return 结售汇用途代码.ID
	 */
	public Long getSettlePurposeCode() {
		return get("settlePurposeCode");
	}

	/**
	 * 设置结售汇用途代码
	 *
	 * @param settlePurposeCode 结售汇用途代码.ID
	 */
	public void setSettlePurposeCode(Long settlePurposeCode) {
		set("settlePurposeCode", settlePurposeCode);
	}

	/**
	 * 获取结售汇用途详情
	 *
	 * @return 结售汇用途详情
	 */
	public String getSettlePurposeDetail() {
		return get("settlePurposeDetail");
	}

	/**
	 * 设置结售汇用途详情
	 *
	 * @param settlePurposeDetail 结售汇用途详情
	 */
	public void setSettlePurposeDetail(String settlePurposeDetail) {
		set("settlePurposeDetail", settlePurposeDetail);
	}

	/**
	 * 获取外汇局批件号
	 *
	 * @return 外汇局批件号
	 */
	public String getSafeApprovalNo() {
		return get("safeApprovalNo");
	}

	/**
	 * 设置外汇局批件号
	 *
	 * @param safeApprovalNo 外汇局批件号
	 */
	public void setSafeApprovalNo(String safeApprovalNo) {
		set("safeApprovalNo", safeApprovalNo);
	}

	/**
	 * 获取文件名
	 *
	 * @return 文件名
	 */
	public String getFileName() {
		return get("fileName");
	}

	/**
	 * 设置文件名
	 *
	 * @param fileName 文件名
	 */
	public void setFileName(String fileName) {
		set("fileName", fileName);
	}

	/**
	 * 获取申报人
	 *
	 * @return 申报人
	 */
	public String getDeclarer() {
		return get("declarer");
	}

	/**
	 * 设置申报人
	 *
	 * @param declarer 申报人
	 */
	public void setDeclarer(String declarer) {
		set("declarer", declarer);
	}

	/**
	 * 获取申报人电话
	 *
	 * @return 申报人电话
	 */
	public String getDeclarerPhone() {
		return get("declarerPhone");
	}

	/**
	 * 设置申报人电话
	 *
	 * @param declarerPhone 申报人电话
	 */
	public void setDeclarerPhone(String declarerPhone) {
		set("declarerPhone", declarerPhone);
	}

	/**
	 * 获取银企联返回信息
	 *
	 * @return 银企联返回信息
	 */
	public String getEnterpriseBackMessg() {
		return get("enterpriseBackMessg");
	}

	/**
	 * 设置银企联返回信息
	 *
	 * @param enterpriseBackMessg 银企联返回信息
	 */
	public void setEnterpriseBackMessg(String enterpriseBackMessg) {
		set("enterpriseBackMessg", enterpriseBackMessg);
	}
	/**
	 * 获取业务类型
	 *
	 * @return 业务类型
	 */
	public Short getBusinessType() {
		return getShort("businessType");
	}

	/**
	 * 设置业务类型
	 *
	 * @param businessType 业务类型
	 */
	public void setBusinessType(Short businessType) {
		set("businessType", businessType);
	}

	/**
	 * 获取业务批次号
	 *
	 * @return 业务批次号
	 */
	public String getBatchno() {
		return get("batchno");
	}

	/**
	 * 设置业务批次号
	 *
	 * @param batchno 业务批次号
	 */
	public void setBatchno(String batchno) {
		set("batchno", batchno);
	}

	/**
	 * 获取银行对账码
	 *
	 * @return 银行对账码
	 */
	public String getBankcheckcode() {
		return get("bankcheckcode");
	}

	/**
	 * 设置银行对账码
	 *
	 * @param bankcheckcode 银行对账码
	 */
	public void setBankcheckcode(String bankcheckcode) {
		set("bankcheckcode", bankcheckcode);
	}

	/**
	 * 获取付款_是否关联
	 *
	 * @return 付款_是否关联
	 */
	public Short getAssociationStatusPay() {
		return getShort("associationStatusPay");
	}

	/**
	 * 设置付款_是否关联
	 *
	 * @param associationStatusPay 付款_是否关联
	 */
	public void setAssociationStatusPay(Short associationStatusPay) {
		set("associationStatusPay", associationStatusPay);
	}

	/**
	 * 获取付款_银行对账单ID
	 *
	 * @return 付款_银行对账单ID.ID
	 */
	public Long getPaybankbill() {
		return get("paybankbill");
	}

	/**
	 * 设置付款_银行对账单ID
	 *
	 * @param paybankbill 付款_银行对账单ID.ID
	 */
	public void setPaybankbill(Long paybankbill) {
		set("paybankbill", paybankbill);
	}

	/**
	 * 获取付款_认领单ID
	 *
	 * @return 付款_认领单ID.ID
	 */
	public Long getPaybillclaim() {
		return get("paybillclaim");
	}

	/**
	 * 设置付款_认领单ID
	 *
	 * @param paybillclaim 付款_认领单ID.ID
	 */
	public void setPaybillclaim(Long paybillclaim) {
		set("paybillclaim", paybillclaim);
	}

	/**
	 * 获取收款_是否关联
	 *
	 * @return 收款_是否关联
	 */
	public Short getAssociationStatusCollect() {
		return getShort("associationStatusCollect");
	}

	/**
	 * 设置收款_是否关联
	 *
	 * @param associationStatusCollect 收款_是否关联
	 */
	public void setAssociationStatusCollect(Short associationStatusCollect) {
		set("associationStatusCollect", associationStatusCollect);
	}

	/**
	 * 获取收款_银行对账单ID
	 *
	 * @return 收款_银行对账单ID.ID
	 */
	public Long getCollectbankbill() {
		return get("collectbankbill");
	}

	/**
	 * 设置收款_银行对账单ID
	 *
	 * @param collectbankbill 收款_银行对账单ID.ID
	 */
	public void setCollectbankbill(Long collectbankbill) {
		set("collectbankbill", collectbankbill);
	}

	/**
	 * 获取收款_认领单ID
	 *
	 * @return 收款_认领单ID.ID
	 */
	public Long getCollectbillclaim() {
		return get("collectbillclaim");
	}

	/**
	 * 设置收款_认领单ID
	 *
	 * @param collectbillclaim 收款_认领单ID.ID
	 */
	public void setCollectbillclaim(Long collectbillclaim) {
		set("collectbillclaim", collectbillclaim);
	}

	/**
	 * 获取数据来源
	 *
	 * @return 数据来源
	 */
	public Short getDatasource() {
		return getShort("datasource");
	}

	/**
	 * 设置数据来源
	 *
	 * @param datasource 数据来源
	 */
	public void setDatasource(Short datasource) {
		set("datasource", datasource);
	}

    public Long getFilesCount() {
        return get("filesCount");
    }

    public void setFilesCount(Long filesCount) {
        set("filesCount", filesCount);
    }

    /**
     * 获取收款方时间戳
     *
     * @return 收款方时间戳
     */
    public java.util.Date getCollectSourcePubts() {
        return get("collectSourcePubts");
    }

    /**
     * 设置收款方时间戳
     *
     * @param collectSourcePubts 收款方时间戳
     */
    public void setCollectSourcePubts(java.util.Date collectSourcePubts) {
        set("collectSourcePubts", collectSourcePubts);
    }

    /**
     * 获取付款方时间戳
     *
     * @return 付款方时间戳
     */
    public java.util.Date getPaySourcePubts() {
        return get("paySourcePubts");
    }

    /**
     * 设置付款方时间戳
     *
     * @param paySourcePubts 付款方时间戳
     */
    public void setPaySourcePubts(java.util.Date paySourcePubts) {
        set("paySourcePubts", paySourcePubts);
    }

	/**
	 * 获取事项类型
	 *
	 * @return 事项类型
	 */
	public Short getBilltype() {
		return getShort("billtype");
	}

	/**
	 * 设置事项类型
	 *
	 * @param billtype 事项类型
	 */
	public void setBilltype(Short billtype) {
		set("billtype", billtype);
	}

	/**
	 * 获取外币兑换申请单号
	 *
	 * @return 外币兑换申请单号
	 */
	public String getCurrencyapplynumber() {
		return get("currencyapplynumber");
	}

	/**
	 * 设置外币兑换申请单号
	 *
	 * @param currencyapplynumber 外币兑换申请单号
	 */
	public void setCurrencyapplynumber(String currencyapplynumber) {
		set("currencyapplynumber", currencyapplynumber);
	}

	/**
	 * 获取期望交割日期
	 *
	 * @return 期望交割日期
	 */
	public java.util.Date getExpectedDeliveryDate() {
		return get("expectedDeliveryDate");
	}

	/**
	 * 设置期望交割日期
	 *
	 * @param expectedDeliveryDate 期望交割日期
	 */
	public void setExpectedDeliveryDate(java.util.Date expectedDeliveryDate) {
		set("expectedDeliveryDate", expectedDeliveryDate);
	}

	/**
	 * 获取计价方
	 *
	 * @return 计价方
	 */
	public Short getPricingParty() {
		return getShort("pricingParty");
	}

	/**
	 * 设置计价方
	 *
	 * @param pricingParty 计价方
	 */
	public void setPricingParty(Short pricingParty) {
		set("pricingParty", pricingParty);
	}

	/**
	 * 获取外币兑换申请id
	 *
	 * @return 外币兑换申请id.ID
	 */
	public Long getCurrencyapplyid() {
		return get("currencyapplyid");
	}

	/**
	 * 设置外币兑换申请id
	 *
	 * @param currencyapplyid 外币兑换申请id.ID
	 */
	public void setCurrencyapplyid(Long currencyapplyid) {
		set("currencyapplyid", currencyapplyid);
	}

	/**
	 * 获取创建人
	 *
	 * @return 创建人
	 */
	public Long getCreatorId() {
		return get("creatorId");
	}

	/**
	 * 设置创建人
	 *
	 * @param creatorId 创建人
	 */
	public void setCreatorId(Long creatorId) {
		set("creatorId", creatorId);
	}

	/**
	 * 获取WBS
	 *
	 * @return WBS.ID
	 */
	public String getWbs() {
		return get("wbs");
	}

	/**
	 * 设置WBS
	 *
	 * @param wbs WBS.ID
	 */
	public void setWbs(String wbs) {
		set("wbs", wbs);
	}

	/**
	 * 获取活动
	 *
	 * @return 活动.ID
	 */
	public String getActivity() {
		return get("activity");
	}

	/**
	 * 设置活动
	 *
	 * @param activity 活动.ID
	 */
	public void setActivity(String activity) {
		set("activity", activity);
	}

	/**
	 * 获取地区币种
	 *
	 * @return 地区币种.ID
	 */
	public String getRegionCurrency() {
		return get("regionCurrency");
	}

	/**
	 * 设置地区币种
	 *
	 * @param regionCurrency 地区币种.ID
	 */
	public void setRegionCurrency(String regionCurrency) {
		set("regionCurrency", regionCurrency);
	}

	/**
	 * 获取来源代码
	 *
	 * @return 来源代码.ID
	 */
	public Long getSourcecode() {
		return get("sourcecode");
	}

	/**
	 * 设置来源代码
	 *
	 * @param sourcecode 来源代码.ID
	 */
	public void setSourcecode(Long sourcecode) {
		set("sourcecode", sourcecode);
	}

	/**
	 * 获取用途代码
	 *
	 * @return 用途代码.ID
	 */
	public Long getPurposecode() {
		return get("purposecode");
	}

	/**
	 * 设置用途代码
	 *
	 * @param purposecode 用途代码.ID
	 */
	public void setPurposecode(Long purposecode) {
		set("purposecode", purposecode);
	}

	/**
	 * 获取交割类型
	 *
	 * @return 交割类型
	 */
	public Short getDeliverytime() {
		return getShort("deliverytime");
	}

	/**
	 * 设置交割类型
	 *
	 * @param deliverytime 交割类型
	 */
	public void setDeliverytime(Short deliverytime) {
		set("deliverytime", deliverytime);
	}

	/**
	 * 获取项目归类
	 *
	 * @return 项目归类
	 */
	public String getProjectclassification() {
		return get("projectclassification");
	}

	/**
	 * 设置项目归类
	 *
	 * @param projectclassification 项目归类
	 */
	public void setProjectclassification(String projectclassification) {
		set("projectclassification", projectclassification);
	}

	/**
	 * 获取财资统一对账码（买入）
	 *
	 * @return 财资统一对账码（买入）
	 */
	public String getBuysmartcheckno() {
		return get("buysmartcheckno");
	}

	/**
	 * 设置财资统一对账码（买入）
	 *
	 * @param buysmartcheckno 财资统一对账码（买入）
	 */
	public void setBuysmartcheckno(String buysmartcheckno) {
		set("buysmartcheckno", buysmartcheckno);
	}

	/**
	 * 获取财资统一对账码（卖出）
	 *
	 * @return 财资统一对账码（卖出）
	 */
	public String getSellsmartcheckno() {
		return get("sellsmartcheckno");
	}

	/**
	 * 设置财资统一对账码（卖出）
	 *
	 * @param sellsmartcheckno 财资统一对账码（卖出）
	 */
	public void setSellsmartcheckno(String sellsmartcheckno) {
		set("sellsmartcheckno", sellsmartcheckno);
	}
	/**
	 * 获取是否占预算
	 *
	 * @return 是否占预算
	 */
	public Short getIsOccupyBudget() {
		return getShort("isOccupyBudget");
	}

	/**
	 * 设置是否占预算
	 *
	 * @param isOccupyBudget 是否占预算
	 */
	public void setIsOccupyBudget(Short isOccupyBudget) {
		set("isOccupyBudget", isOccupyBudget);
	}

	/**
	 * 获取申报额度编号
	 *
	 * @return 申报额度编号
	 */
	public String getForeignlimitno() {
		return get("foreignlimitno");
	}

	/**
	 * 设置申报额度编号
	 *
	 * @param foreignlimitno 申报额度编号
	 */
	public void setForeignlimitno(String foreignlimitno) {
		set("foreignlimitno", foreignlimitno);
	}

	/**
	 * 获取汇率折算方式
	 *
	 * @return 汇率折算方式
	 */
	public Short getRateconversiontype() {
		return getShort("rateconversiontype");
	}

	/**
	 * 设置汇率折算方式
	 *
	 * @param rateconversiontype 汇率折算方式
	 */
	public void setRateconversiontype(Short rateconversiontype) {
		set("rateconversiontype", rateconversiontype);
	}

	/**
	 * 获取成交汇率折算方式
	 *
	 * @return 成交汇率折算方式
	 */
	public Short getExchangeRateOps() {
		return getShort("exchangeRateOps");
	}

	/**
	 * 设置成交汇率折算方式
	 *
	 * @param exchangeRateOps 成交汇率折算方式
	 */
	public void setExchangeRateOps(Short exchangeRateOps) {
		set("exchangeRateOps", exchangeRateOps);
	}

	/**
	 * 获取折本币买入汇率折算方式
	 *
	 * @return 折本币买入汇率折算方式
	 */
	public Short getPurchaseRateOps() {
		return getShort("purchaseRateOps");
	}

	/**
	 * 设置折本币买入汇率折算方式
	 *
	 * @param purchaseRateOps 折本币买入汇率折算方式
	 */
	public void setPurchaseRateOps(Short purchaseRateOps) {
		set("purchaseRateOps", purchaseRateOps);
	}

	/**
	 * 获取折本币买入汇率类型
	 *
	 * @return 折本币买入汇率类型
	 */
	public String getPurchaseRateType() {
		return get("purchaseRateType");
	}

	/**
	 * 设置折本币买入汇率类型
	 *
	 * @param purchaseRateType 折本币买入汇率类型
	 */
	public void setPurchaseRateType(String purchaseRateType) {
		set("purchaseRateType", purchaseRateType);
	}

	/**
	 * 获取折本币卖出汇率折算方式
	 *
	 * @return 折本币卖出汇率折算方式
	 */
	public Short getSellRateOps() {
		return getShort("sellRateOps");
	}

	/**
	 * 设置折本币卖出汇率折算方式
	 *
	 * @param sellRateOps 折本币卖出汇率折算方式
	 */
	public void setSellRateOps(Short sellRateOps) {
		set("sellRateOps", sellRateOps);
	}

	/**
	 * 获取折本币卖出汇率类型
	 *
	 * @return 折本币卖出汇率类型
	 */
	public String getSellRateType() {
		return get("sellRateType");
	}

	/**
	 * 设置折本币卖出汇率类型
	 *
	 * @param sellRateType 折本币卖出汇率类型
	 */
	public void setSellRateType(String sellRateType) {
		set("sellRateType", sellRateType);
	}

	/**
	 * 获取折本币手续费折算方式
	 *
	 * @return 折本币手续费折算方式
	 */
	public Short getCommissionRateOps() {
		return getShort("commissionRateOps");
	}

	/**
	 * 设置折本币手续费折算方式
	 *
	 * @param commissionRateOps 折本币手续费折算方式
	 */
	public void setCommissionRateOps(Short commissionRateOps) {
		set("commissionRateOps", commissionRateOps);
	}

	/**
	 * 获取折本币手续费汇率类型
	 *
	 * @return 折本币手续费汇率类型
	 */
	public String getCommissionRateType() {
		return get("commissionRateType");
	}

	/**
	 * 设置折本币手续费汇率类型
	 *
	 * @param commissionRateType 折本币手续费汇率类型
	 */
	public void setCommissionRateType(String commissionRateType) {
		set("commissionRateType", commissionRateType);
	}
}
