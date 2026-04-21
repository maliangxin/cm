package com.yonyoucloud.fi.cm.transferaccount;

import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyou.ucf.mdd.ext.voucher.base.Vouch;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;

/**
 * 转账单实体
 *
 * @author u
 * @version 1.0
 */
public class TransferAccount extends Vouch implements IApprovalInfo, ICurrency, IApprovalFlow, IPrintCount, AccentityRawInterface {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cm.transferaccount.TransferAccount";
	// 业务对象编码
	public static final String BUSI_OBJ_CODE = "ctm-cmp.cm_transfer_account";

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
     * 获取事项类型
     *
     * @return 事项类型
     */
	public EventType getBilltype() {
		Number v = get("billtype");
		return EventType.find(v);
	}

    /**
     * 设置事项类型
     *
     * @param billtype 事项类型
     */
	public void setBilltype(EventType billtype) {
		if (billtype != null) {
			set("billtype", billtype.getValue());
		} else {
			set("billtype", null);
		}
	}

    /**
     * 获取类型
     *
     * @return 类型
     */
	public String getType() {
		return get("type");
	}

    /**
     * 设置类型
     *
     * @param type 类型
     */
	public void setType(String type) {
		set("type", type);
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
	 * 获取收款方结算方式
	 * @return
	 */
	public Long getCollectsettlemode() {
		return get("collectsettlemode");
	}

	/**
	 * 设置收款方结算方式
	 * @param collectsettlemode
	 */
	public void setCollectsettlemode(Long collectsettlemode) {
		set("collectsettlemode", collectsettlemode);
	}

    /**
     * 获取付款银行账户
     *
     * @return 付款银行账户.ID
     */
	public String getPayBankAccount() {
		return get("payBankAccount");
	}

    /**
     * 设置付款银行账户
     *
     * @param payBankAccount 付款银行账户.ID
     */
	public void setPayBankAccount(String payBankAccount) {
		set("payBankAccount", payBankAccount);
	}


	/**
     * 获取付款现金账户
     *
     * @return 付款现金账户.ID
     */
	public String getPayCashAccount() {
		return get("payCashAccount");
	}

    /**
     * 设置付款现金账户
     *
     * @param payCashAccount 付款现金账户.ID
     */
	public void setPayCashAccount(String payCashAccount) {
		set("payCashAccount", payCashAccount);
	}

    /**
     * 获取收款银行账户
     *
     * @return 收款银行账户.ID
     */
	public String getRecBankAccount() {
		return get("recBankAccount");
	}

    /**
     * 设置收款银行账户
     *
     * @param recBankAccount 收款银行账户.ID
     */
	public void setRecBankAccount(String recBankAccount) {
		set("recBankAccount", recBankAccount);
	}


	/**
     * 获取收款现金账户
     *
     * @return 收款现金账户.ID
     */
	public String getRecCashAccount() {
		return get("recCashAccount");
	}

    /**
     * 设置收款现金账户
     *
     * @param recCashAccount 收款现金账户.ID
     */
	public void setRecCashAccount(String recCashAccount) {
		set("recCashAccount", recCashAccount);
	}

    /**
     * 获取付款金额
     *
     * @return 付款金额
     */
	public java.math.BigDecimal getOriSum() {
		return get("oriSum");
	}

    /**
     * 设置付款金额
     *
     * @param oriSum 付款金额
     */
	public void setOriSum(java.math.BigDecimal oriSum) {
		set("oriSum", oriSum);
	}

    /**
     * 获取本币金额
     *
     * @return 本币金额
     */
	public java.math.BigDecimal getNatSum() {
		return get("natSum");
	}

    /**
     * 设置本币金额
     *
     * @param natSum 本币金额
     */
	public void setNatSum(java.math.BigDecimal natSum) {
		set("natSum", natSum);
	}

	/**
	 * 获取转入手续费
	 *
	 * @return 转入手续费
	 */
	public java.math.BigDecimal getInBrokerage() {
		return get("inBrokerage");
	}

	/**
	 * 设置转入手续费
	 *
	 * @param inBrokerage 转入手续费
	 */
	public void setInBrokerage(java.math.BigDecimal inBrokerage) {
		set("inBrokerage", inBrokerage);
	}

	/**
	 * 获取转出手续费
	 *
	 * @return 转出手续费
	 */
	public java.math.BigDecimal getOutBrokerage() {
		return get("outBrokerage");
	}

	/**
	 * 设置转出手续费
	 *
	 * @param outBrokerage 转出手续费
	 */
	public void setOutBrokerage(java.math.BigDecimal outBrokerage) {
		set("outBrokerage", outBrokerage);
	}

	/**
	 * 获取转入手续费本币
	 *
	 * @return 转入手续费本币
	 */
	public java.math.BigDecimal getInBrokerageNatSum() {
		return get("inBrokerageNatSum");
	}

	/**
	 * 设置转入手续费本币
	 *
	 * @param inBrokerageNatSum 转入手续费本币
	 */
	public void setInBrokerageNatSum(java.math.BigDecimal inBrokerageNatSum) {
		set("inBrokerageNatSum", inBrokerageNatSum);
	}

	/**
	 * 获取转出手续费本币
	 *
	 * @return 转出手续费本币
	 */
	public java.math.BigDecimal getOutBrokerageNatSum() {
		return get("outBrokerageNatSum");
	}

	/**
	 * 设置转出手续费本币
	 *
	 * @param outBrokerageNatSum 转出手续费本币
	 */
	public void setOutBrokerageNatSum(java.math.BigDecimal outBrokerageNatSum) {
		set("outBrokerageNatSum", outBrokerageNatSum);
	}
    /**
     * 获取用途
     *
     * @return 用途
     */
	public String getPurpose() {
		return get("purpose");
	}

    /**
     * 设置用途
     *
     * @param purpose 用途
     */
	public void setPurpose(String purpose) {
		set("purpose", purpose);
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
     * 获取票据号
     *
     * @return 票据号
     */
	public String getNoteno() {
		return get("noteno");
	}

    /**
     * 设置票据号
     *
     * @param noteno 票据号
     */
	public void setNoteno(String noteno) {
		set("noteno", noteno);
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
     * 获取支付状态
     *
     * @return 支付状态
     */
	public PayStatus getPaystatus() {
		Number v = get("paystatus");
		return PayStatus.find(v);
	}

    /**
     * 设置支付状态
     *
     * @param paystatus 支付状态
     */
	public void setPaystatus(PayStatus paystatus) {
		if (paystatus != null) {
			set("paystatus", paystatus.getValue());
		} else {
			set("paystatus", null);
		}
	}

    /**
     * 获取支付信息
     *
     * @return 支付信息
     */
	public String getPaymessage() {
		return get("paymessage");
	}

    /**
     * 设置支付信息
     *
     * @param paymessage 支付信息
     */
	public void setPaymessage(String paymessage) {
		set("paymessage", paymessage);
	}

    /**
     * 获取预下单编码
     *
     * @return 预下单编码
     */
	public String getPorderid() {
		return get("porderid");
	}

    /**
     * 设置预下单编码
     *
     * @param porderid 预下单编码
     */
	public void setPorderid(String porderid) {
		set("porderid", porderid);
	}

    /**
     * 获取批量支付编号
     *
     * @return 批量支付编号
     */
	public String getBatno() {
		return get("batno");
	}

    /**
     * 设置批量支付编号
     *
     * @param batno 批量支付编号
     */
	public void setBatno(String batno) {
		set("batno", batno);
	}

    /**
     * 获取请求流水号
     *
     * @return 请求流水号
     */
	public String getRequestseqno() {
		return get("requestseqno");
	}

    /**
     * 设置请求流水号
     *
     * @param requestseqno 请求流水号
     */
	public void setRequestseqno(String requestseqno) {
		set("requestseqno", requestseqno);
	}

    /**
     * 获取银行流水号
     *
     * @return 银行流水号
     */
	public String getBankseqno() {
		return get("bankseqno");
	}

    /**
     * 设置银行流水号
     *
     * @param bankseqno 银行流水号
     */
	public void setBankseqno(String bankseqno) {
		set("bankseqno", bankseqno);
	}

    /**
     * 获取交易流水号
     *
     * @return 交易流水号
     */
	public String getTranseqno() {
		return get("transeqno");
	}

    /**
     * 设置交易流水号
     *
     * @param transeqno 交易流水号
     */
	public void setTranseqno(String transeqno) {
		set("transeqno", transeqno);
	}

    /**
     * 获取支付日期
     *
     * @return 支付日期
     */
	public java.util.Date getPaydate() {
		return get("paydate");
	}

    /**
     * 设置支付日期
     *
     * @param paydate 支付日期
     */
	public void setPaydate(java.util.Date paydate) {
		set("paydate", paydate);
	}

    /**
     * 获取支付人
     *
     * @return 支付人.ID
     */
	public Long getPayman() {
		return get("payman");
	}

    /**
     * 设置支付人
     *
     * @param payman 支付人.ID
     */
	public void setPayman(Long payman) {
		set("payman", payman);
	}

    /**
     * 获取变更人
     *
     * @return 变更人.ID
     */
	public Long getPayupdateman() {
		return get("payupdateman");
	}

    /**
     * 设置变更人
     *
     * @param payupdateman 变更人.ID
     */
	public void setPayupdateman(Long payupdateman) {
		set("payupdateman", payupdateman);
	}

    /**
     * 获取变更时间
     *
     * @return 变更时间
     */
	public java.util.Date getPayupdatedate() {
		return get("payupdatedate");
	}

    /**
     * 设置变更时间
     *
     * @param payupdatedate 变更时间
     */
	public void setPayupdatedate(java.util.Date payupdatedate) {
		set("payupdatedate", payupdatedate);
	}

    /**
     * 获取变更原因
     *
     * @return 变更原因
     */
	public String getPayupdatereason() {
		return get("payupdatereason");
	}

    /**
     * 设置变更原因
     *
     * @param payupdatereason 变更原因
     */
	public void setPayupdatereason(String payupdatereason) {
		set("payupdatereason", payupdatereason);
	}

    /**
     * 获取结算状态
     *
     * @return 结算状态
     */
	public SettleStatus getSettlestatus() {
		Number v = get("settlestatus") instanceof String ? Short.parseShort(get("settlestatus")) : get("settlestatus");
		return SettleStatus.find(v);
	}

    /**
     * 设置结算状态
     *
     * @param settlestatus 结算状态
     */
	public void setSettlestatus(SettleStatus settlestatus) {
		if (settlestatus != null) {
			set("settlestatus", settlestatus.getValue());
		} else {
			set("settlestatus", null);
		}
	}

	/**
	 * 获取是否结算处理
	 *
	 * @return 是否结算处理
	 */
	public Boolean getIsSettlement() {
		return getBoolean("isSettlement");
	}

	/**
	 * 设置是否结算处理
	 *
	 * @param isSettlement 是否结算处理
	 */
	public void setIsSettlement(Boolean isSettlement) {
		set("isSettlement", isSettlement);
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
     * 获取结算时间
     *
     * @return 结算时间
     */
	public java.util.Date getSettledate() {
		return get("settledate");
	}

    /**
     * 设置结算时间
     *
     * @param settledate 结算时间
     */
	public void setSettledate(java.util.Date settledate) {
		set("settledate", settledate);
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
	public void setVoucherstatus(short voucherstatus) {
		set("voucherstatus", voucherstatus);
	}

    /**
     * 获取数据签名
     *
     * @return 数据签名
     */
	public String getSignature() {
		return get("signature");
	}

    /**
     * 设置数据签名
     *
     * @param signature 数据签名
     */
	public void setSignature(String signature) {
		set("signature", signature);
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
	 * 获取来源单据号
	 *
	 * @return 来源单据号
	 */
	public String getSrcbillno() {
		return get("srcbillno");
	}

	/**
	 * 设置来源单据号
	 *
	 * @param srcbillno 来源单据号
	 */
	public void setSrcbillno(String srcbillno) {
		set("srcbillno", srcbillno);
	}

	/**
	 * 获取银行对账编号
	 *
	 * @return 银行对账编号
	 */
	public String getBankcheckno() {
		return get("bankcheckno");
	}

	/**
	 * 设置银行对账编号
	 *
	 * @param bankcheckno 银行对账编号
	 */
	public void setBankcheckno(String bankcheckno) {
		set("bankcheckno", bankcheckno);
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

	/**
	 * 获取支票id
	 *
	 * @return 支票id
	 */
	public Long getCheckid() {
		return get("checkid");
	}

	/**
	 * 设置支票id
	 *
	 * @param checkid 支票id
	 */
	public void setCheckid(Long checkid) {
		set("checkid", checkid);
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
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public Long getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDef 自定义项特征属性组.ID
	 */
	public void setCharacterDef(Long characterDef) {
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
	public Boolean getAssociationStatusPay() {
		if (getBoolean("associationStatusPay") == null) {
			return false;
		}
		return getBoolean("associationStatusPay");
	}

	/**
	 * 设置付款_是否关联
	 *
	 * @param associationStatusPay 付款_是否关联
	 */
	public void setAssociationStatusPay(Boolean associationStatusPay) {
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
	public Boolean getAssociationStatusCollect() {
		if (getBoolean("associationStatusCollect") == null) {
			return false;
		}
		return getBoolean("associationStatusCollect") == null;
	}

	/**
	 * 设置收款_是否关联
	 *
	 * @param associationStatusCollect 收款_是否关联
	 */
	public void setAssociationStatusCollect(Boolean associationStatusCollect) {
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
	 * 获取是否直联账户标识
	 *
	 * @return 是否直联账户标识
	 */
	public Boolean getIsdirectconn() {
		return getBoolean("isdirectconn");
	}

	/**
	 * 设置是否直联账户标识
	 *
	 * @param isdirectconn 是否直联账户标识
	 */
	public void setIsdirectconn(Boolean isdirectconn) {
		set("isdirectconn", isdirectconn);
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
	 * 获取三方转账类型
	 *
	 * @return 三方转账类型
	 */
	public Short getVirtualBank() {
		return getShort("virtualBank");
	}

	/**
	 * 设置三方转账类型
	 *
	 * @param virtualBank 三方转账类型
	 */
	public void setVirtualBank(Short virtualBank) {
		set("virtualBank", virtualBank);
	}

	/**
	 * 获取付款虚拟账户
	 *
	 * @return 付款虚拟账户.ID
	 */
	public String getPayVirtualAccount() {
		return get("payVirtualAccount");
	}

	/**
	 * 设置付款虚拟账户
	 *
	 * @param payVirtualAccount 付款虚拟账户.ID
	 */
	public void setPayVirtualAccount(String payVirtualAccount) {
		set("payVirtualAccount", payVirtualAccount);
	}

	/**
	 * 获取收款虚拟账户
	 *
	 * @return 收款虚拟账户.ID
	 */
	public String getCollVirtualAccount() {
		return get("collVirtualAccount");
	}

	/**
	 * 设置收款虚拟账户
	 *
	 * @param collVirtualAccount 收款虚拟账户.ID
	 */
	public void setCollVirtualAccount(String collVirtualAccount) {
		set("collVirtualAccount", collVirtualAccount);
	}

	/**
	 * 获取是否结算
	 *
	 * @return 是否结算
	 */
	public Boolean getIsSettle() {
		return getBoolean("isSettle");
	}

	/**
	 * 设置是否结算
	 *
	 * @param isSettle 是否结算
	 */
	public void setIsSettle(Boolean isSettle) {
		set("isSettle", isSettle);
	}

	/**
	 * 获取结算成功金额
	 *
	 * @return 结算成功金额
	 */
	public java.math.BigDecimal getSettleSuccessAmount() {
		return get("settleSuccessAmount");
	}

	/**
	 * 设置结算成功金额
	 *
	 * @param settleSuccessAmount 结算成功金额
	 */
	public void setSettleSuccessAmount(java.math.BigDecimal settleSuccessAmount) {
		set("settleSuccessAmount", settleSuccessAmount);
	}

	/**
	 * 获取结算止付金额
	 *
	 * @return 结算止付金额
	 */
	public java.math.BigDecimal getSettleStopPayAmount() {
		return get("settleStopPayAmount");
	}

	/**
	 * 设置结算止付金额
	 *
	 * @param settleStopPayAmount 结算止付金额
	 */
	public void setSettleStopPayAmount(java.math.BigDecimal settleStopPayAmount) {
		set("settleStopPayAmount", settleStopPayAmount);
	}

	/**
	 * 获取跨行标识
	 *
	 * @return 跨行标识
	 */
	public String getTobanktype() {
		return get("tobanktype");
	}

	/**
	 * 设置跨行标识
	 *
	 * @param tobanktype 跨行标识
	 */
	public void setTobanktype(String tobanktype) {
		set("tobanktype", tobanktype);
	}

	/**
	 * 获取是否存在支付风险
	 *
	 * @return 是否存在支付风险
	 */
	public Boolean getRiskPayFlag() {
		return getBoolean("riskPayFlag");
	}

	/**
	 * 设置是否存在支付风险
	 *
	 * @param riskPayFlag 是否存在支付风险
	 */
	public void setRiskPayFlag(Boolean riskPayFlag) {
		set("riskPayFlag", riskPayFlag);
	}

	/**
	 * 获取风险类型
	 *
	 * @return 风险类型
	 */
	public String getRiskPayType() {
		return get("riskPayType");
	}

	/**
	 * 设置风险类型
	 *
	 * @param riskPayType 风险类型
	 */
	public void setRiskPayType(String riskPayType) {
		set("riskPayType", riskPayType);
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
	 * 获取支付扩展信息
	 *
	 * @return 支付扩展信息
	 */
	public String getPayExtend() {
		return get("payExtend");
	}

	/**
	 * 设置支付扩展信息
	 *
	 * @param payExtend 支付扩展信息
	 */
	public void setPayExtend(String payExtend) {
		set("payExtend", payExtend);
	}

	/**
	 * 设置支票用途
	 * @param checkpurpose 支票用途 1转账，2提现
	 */
	public void setCheckpurpose(short checkpurpose) {
		set("checkpurpose", checkpurpose);
	}

	/**
	 * 获取支票用途
	 * @return 支票用途
	 */
	public short getCheckpurpose() {
		return get("checkpurpose");
	}

	/**
	 * 获取付款待结算数据id
	 *
	 * @return 获取付款待结算数据id.ID
	 */
	public Long getPaymentid() {
		return get("paymentid");
	}

	/**
	 * 设置付款待结算数据id
	 *
	 * @param paymentid 付款待结算数据id.ID
	 */
	public void setPaymentid(Long paymentid) {
		set("paymentid", paymentid);
	}

	/**
	 * 获取收款待结算数据id
	 *
	 * @return 获取收款待结算数据id.ID
	 */
	public Long getCollectid() {
		return get("collectid");
	}

	/**
	 * 设置收款待结算数据id
	 *
	 * @param collectid 收款待结算数据id.ID
	 */
	public void setCollectid(Long collectid) {
		set("collectid", collectid);
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
	 * 获取swift码
	 *
	 * @return swift码
	 */
	public String getSwiftCode() {
		return get("swiftCode");
	}

	/**
	 * 设置swift码
	 *
	 * @param swiftCode swift码
	 */
	public void setSwiftCode(String swiftCode) {
		set("swiftCode", swiftCode);
	}

	/**
	 * 获取对账单入账类型
	 *
	 * @return 对账单入账类型
	 */
	public Short getEntrytype() {
		return getShort("entrytype");
	}

	/**
	 * 设置对账单入账类型
	 *
	 * @param entrytype 对账单入账类型
	 */
	public void setEntrytype(Short entrytype) {
		set("entrytype", entrytype);
	}
	/**
	 * 获取收款勾对码
	 *
	 * @return 收款勾对码
	 */
	public String getSmartcheckno() {
		return get("smartcheckno");
	}

	/**
	 * 设置收款勾对码
	 *
	 * @param smartcheckno 收款勾对码
	 */
	public void setSmartcheckno(String smartcheckno) {
		set("smartcheckno", smartcheckno);
	}
	/**
	 * 获取付款勾对码
	 *
	 * @return 付款勾对码
	 */
	public String getPaysmartcheckno() {
		return get("paysmartcheckno");
	}

	/**
	 * 设置付款勾对码
	 *
	 * @param paysmartcheckno 付款勾对码
	 */
	public void setPaysmartcheckno(String paysmartcheckno) {
		set("paysmartcheckno", paysmartcheckno);
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
	 * 获取是否再次办结
	 *
	 * @return 是否再次办结
	 */
	public Boolean getIsSettleAgain() {
		return getBoolean("isSettleAgain");
	}

	/**
	 * 设置是否再次办结
	 *
	 * @param isSettleAgain 是否再次办结
	 */
	public void setIsSettleAgain(Boolean isSettleAgain) {
		set("isSettleAgain", isSettleAgain);
	}

	/**
	 * 获取凭证失败信息
	 *
	 * @return 凭证失败信息
	 */
	public String getVoucherMessage() {
		return get("voucherMessage");
	}

	/**
	 * 设置凭证失败信息
	 *
	 * @param voucherMessage 凭证失败信息
	 */
	public void setVoucherMessage(String voucherMessage) {
		set("voucherMessage", voucherMessage);
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
	 * 获取首笔已处理
	 *
	 * @return 首笔已处理
	 */
	public Short getIsfirsthandler() {
		return getShort("isfirsthandler");
	}

	/**
	 * 设置首笔已处理
	 *
	 * @param isfirsthandler 首笔已处理
	 */
	public void setIsfirsthandler(Short isfirsthandler) {
		set("isfirsthandler", isfirsthandler);
	}


	/**
	 * 获取对方银行账户名称
	 *
	 * @return 对方银行账户名称
	 */
	public String getOppositeBankAccountName() {
		return get("oppositebankaccountname");
	}

	/**
	 * 设置对方银行账户名称
	 *
	 * @param oppositebankaccountname 对方银行账户名称
	 */
	public void setOppositeBankAccountName(String oppositebankaccountname) {
		set("oppositebankaccountname", oppositebankaccountname);
	}
	/**
	 * 获取对方开户网点
	 *
	 * @return 对方开户网点
	 */
	public String getOppositebankNumber() {
		return get("oppositebankNumber");
	}

	/**
	 * 设置对方开户网点
	 *
	 * @param oppositebankNumber 对方开户网点
	 */
	public void setOppositebankNumber(String oppositebankNumber) {
		set("oppositebankNumber", oppositebankNumber);
	}
	/**
	 * 获取对方银行账号
	 *
	 * @return 对方银行账号
	 */
	public String getOppositebankAccount() {
		return get("oppositebankaccount");
	}

	/**
	 * 设置对方银行账号
	 *
	 * @param oppositebankaccount 对方银行账号
	 */
	public void setOppositebankAccount(String oppositebankaccount) {
		set("oppositebankaccount", oppositebankaccount);
	}
}
