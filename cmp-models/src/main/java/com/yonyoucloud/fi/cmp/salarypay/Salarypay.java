package com.yonyoucloud.fi.cmp.salarypay;

import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyou.ucf.mdd.ext.voucher.base.Vouch;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;

/**
 * 薪资支付实体
 *
 * @author u
 * @version 1.0
 */
public class Salarypay extends Vouch implements IApprovalInfo, ICurrency, IApprovalFlow, IPrintCount, AccentityRawInterface {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.salarypay.Salarypay";
	// 业务对象编码
	public static final String BUSI_OBJ_CODE = "ctm-cmp.cmp_salarypay";
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
     * 获取单据类型
     *
     * @return 单据类型
     */
	public String getType() {
		return get("type");
	}

    /**
     * 设置单据类型
     *
     * @param type 单据类型
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
     * 获取来源单据编号
     *
     * @return 来源单据编号
     */
	public String getSrcbillno() {
		return get("srcbillno");
	}

    /**
     * 设置来源单据编号
     *
     * @param srcbillno 来源单据编号
     */
	public void setSrcbillno(String srcbillno) {
		set("srcbillno", srcbillno);
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
     * 获取业务组织
     *
     * @return 业务组织.ID
     */
	public String getOrg() {
		return get("org");
	}

    /**
     * 设置业务组织
     *
     * @param org 业务组织.ID
     */
	public void setOrg(String org) {
		set("org", org);
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
     * 获取费用项目
     *
     * @return 费用项目.ID
     */
	public Long getExpenseitem() {
		return get("expenseitem");
	}

    /**
     * 设置费用项目
     *
     * @param expenseitem 费用项目.ID
     */
	public void setExpenseitem(Long expenseitem) {
		set("expenseitem", expenseitem);
	}

    /**
     * 获取付款总金额
     *
     * @return 付款总金额
     */
	public java.math.BigDecimal getOriSum() {
		return get("oriSum");
	}

    /**
     * 设置付款总金额
     *
     * @param oriSum 付款总金额
     */
	public void setOriSum(java.math.BigDecimal oriSum) {
		set("oriSum", oriSum);
	}

    /**
     * 获取本币总金额
     *
     * @return 本币总金额
     */
	public java.math.BigDecimal getNatSum() {
		return get("natSum");
	}

    /**
     * 设置本币总金额
     *
     * @param natSum 本币总金额
     */
	public void setNatSum(java.math.BigDecimal natSum) {
		set("natSum", natSum);
	}

	/**
	 * 获取付款总笔数
	 *
	 * @return 付款总笔数
	 */
	public java.math.BigDecimal getNumline() {
		return get("numline");
	}

	/**
	 * 设置付款总笔数
	 *
	 * @param numline 付款总笔数
	 */
	public void setNumline(java.math.BigDecimal numline) {
		set("numline", numline);
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
     * 获取收方跨行标识
     *
     * @return 收方跨行标识
     */
	public OtherBankFlag getOtherbankflag() {
		Number v = get("otherbankflag");
		return OtherBankFlag.find(v);
	}

    /**
     * 设置收方跨行标识
     *
     * @param otherbankflag 收方跨行标识
     */
	public void setOtherbankflag(OtherBankFlag otherbankflag) {
		if (otherbankflag != null) {
			set("otherbankflag", otherbankflag.getValue());
		} else {
			set("otherbankflag", null);
		}
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
		Number v = get("settlestatus");
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
     * 获取结算人
     *
     * @return 结算人.ID
     */
	public Long getSettleuserId() {
		return get("settleuserId");
	}

    /**
     * 设置结算人
     *
     * @param settleuserId 结算人.ID
     */
	public void setSettleuserId(Long settleuserId) {
		set("settleuserId", settleuserId);
	}

	/**
     * 获取结算人
     *
     * @return 结算人
     */
	public String getSettleuser() {
		return get("settleuser");
	}

    /**
     * 设置结算人
     *
     * @param settleuser 结算人.ID
     */
	public void setSettleuser(String settleuser) {
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
	public void setVoucherstatus(VoucherStatus voucherstatus) {
		if (voucherstatus != null) {
			set("voucherstatus", voucherstatus.getValue());
		} else {
			set("voucherstatus", null);
		}
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
	 * 获取成功笔数
	 *
	 * @return 成功笔数
	 */
	public java.math.BigDecimal getSuccessnum() {
		return get("successnum");
	}

	/**
	 * 设置成功笔数
	 *
	 * @param successnum 成功笔数
	 */
	public void setSuccessnum(java.math.BigDecimal successnum) {
		set("successnum", successnum);
	}

    /**
     * 获取成功总金额
     *
     * @return 成功总金额
     */
	public java.math.BigDecimal getSuccessmoney() {
		return get("successmoney");
	}

    /**
     * 设置成功总金额
     *
     * @param successmoney 成功总金额
     */
	public void setSuccessmoney(java.math.BigDecimal successmoney) {
		set("successmoney", successmoney);
	}

	/**
     * 获取成功本币总金额
     *
     * @return 成功本币总金额
     */
	public java.math.BigDecimal getOlcsuccessmoney() {
		return get("olcsuccessmoney");
	}

    /**
     * 设置成功本币总金额
     *
     * @param olcsuccessmoney 成功本币总金额
     */
	public void setOlcsuccessmoney(java.math.BigDecimal olcsuccessmoney) {
		set("olcsuccessmoney", olcsuccessmoney);
	}

	/**
	 * 获取未明笔数
	 *
	 * @return 未明笔数
	 */
	public java.math.BigDecimal getUnknownnum() {
		return get("unknownnum");
	}

	/**
	 * 设置未明笔数
	 *
	 * @param unknownnum 未明笔数
	 */
	public void setUnknownnum(java.math.BigDecimal unknownnum) {
		set("unknownnum", unknownnum);
	}

    /**
     * 获取未明总金额
     *
     * @return 未明总金额
     */
	public java.math.BigDecimal getUnknownmoney() {
		return get("unknownmoney");
	}

    /**
     * 设置未明总金额
     *
     * @param unknownmoney 未明总金额
     */
	public void setUnknownmoney(java.math.BigDecimal unknownmoney) {
		set("unknownmoney", unknownmoney);
	}

	/**
     * 获取未明本币总金额
     *
     * @return 未明本币总金额
     */
	public java.math.BigDecimal getOlcunknownmoney() {
		return get("olcunknownmoney");
	}

    /**
     * 设置未明本币总金额
     *
     * @param olcunknownmoney 未明本币总金额
     */
	public void setOlcunknownmoney(java.math.BigDecimal olcunknownmoney) {
		set("olcunknownmoney", olcunknownmoney);
	}

	/**
	 * 获取失败笔数
	 *
	 * @return 失败笔数
	 */
	public java.math.BigDecimal getFailnum() {
		return get("failnum");
	}

	/**
	 * 设置失败笔数
	 *
	 * @param failnum 失败笔数
	 */
	public void setFailnum(java.math.BigDecimal failnum) {
		set("failnum", failnum);
	}

    /**
     * 获取失败总笔数
     *
     * @return 失败总笔数
     */
	public java.math.BigDecimal getFailmoney() {
		return get("failmoney");
	}

    /**
     * 设置失败总笔数
     *
     * @param failmoney 失败总笔数
     */
	public void setFailmoney(java.math.BigDecimal failmoney) {
		set("failmoney", failmoney);
	}

	/**
     * 获取失败本币总金额
     *
     * @return 失败本币总金额
     */
	public java.math.BigDecimal getOlcfailmoney() {
		return get("olcfailmoney");
	}

    /**
     * 设置失败本币总金额
     *
     * @param olcfailmoney 失败本币总金额
     */
	public void setOlcfailmoney(java.math.BigDecimal olcfailmoney) {
		set("olcfailmoney", olcfailmoney);
	}

	/**
     * 获取作废标志
     *
     * @return 作废标志
     */
	public Boolean getInvalidflag() {
	    return getBoolean("invalidflag");
	}

    /**
     * 设置作废标志
     *
     * @param invalidflag 作废标志
     */
	public void setInvalidflag(Boolean invalidflag) {
		set("invalidflag", invalidflag);
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
     * 获取单据日期
     *
     * @return 单据日期
     */
	public java.util.Date getVouchdate() {
		return get("vouchdate");
	}

    /**
     * 设置单据日期
     *
     * @param vouchdate 单据日期
     */
	public void setVouchdate(java.util.Date vouchdate) {
		set("vouchdate", vouchdate);
	}

    /**
     * 获取模板id
     *
     * @return 模板id
     */
	public Long getTplid() {
		return get("tplid");
	}

    /**
     * 设置模板id
     *
     * @param tplid 模板id
     */
	public void setTplid(Long tplid) {
		set("tplid", tplid);
	}

    /**
     * 获取单据状态
     *
     * @return 单据状态
     */
	public Status getStatus() {
		Short v = get("status");
		return Status.find(v);
	}

    /**
     * 设置单据状态
     *
     * @param status 单据状态
     */
	public void setStatus(Status status) {
		if (status != null) {
			set("status", status.getValue());
		} else {
			set("status", null);
		}
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
     * 获取编码
     *
     * @return 编码
     */
	public String getCode() {
		return get("code");
	}

    /**
     * 设置编码
     *
     * @param code 编码
     */
	public void setCode(String code) {
		set("code", code);
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
     * 获取创建人名称
     *
     * @return 创建人名称
     */
	public String getCreator() {
		return get("creator");
	}

    /**
     * 设置创建人名称
     *
     * @param creator 创建人名称
     */
	public void setCreator(String creator) {
		set("creator", creator);
	}

    /**
     * 获取修改人名称
     *
     * @return 修改人名称
     */
	public String getModifier() {
		return get("modifier");
	}

    /**
     * 设置修改人名称
     *
     * @param modifier 修改人名称
     */
	public void setModifier(String modifier) {
		set("modifier", modifier);
	}

    /**
     * 获取创建人
     *
     * @return 创建人.ID
     */
	public Long getCreatorId() {
		return get("creatorId");
	}

    /**
     * 设置创建人
     *
     * @param creatorId 创建人.ID
     */
	public void setCreatorId(Long creatorId) {
		set("creatorId", creatorId);
	}

    /**
     * 获取修改人
     *
     * @return 修改人.ID
     */
	public Long getModifierId() {
		return get("modifierId");
	}

    /**
     * 设置修改人
     *
     * @param modifierId 修改人.ID
     */
	public void setModifierId(Long modifierId) {
		set("modifierId", modifierId);
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
     * 获取薪资支付子表集合
     *
     * @return 薪资支付子表集合
     */
	public java.util.List<Salarypay_b> Salarypay_b() {
		return getBizObjects("Salarypay_b", Salarypay_b.class);
	}

    /**
     * 设置薪资支付子表集合
     *
     * @param Salarypay_b 薪资支付子表集合
     */
	public void setSalarypay_b(java.util.List<Salarypay_b> Salarypay_b) {
		setBizObjects("Salarypay_b", Salarypay_b);
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
	 * 获取业务类型
	 *
	 * @return 业务类型
	 */
	public BusiTypeEnum getBusitype() {
		Number v = get("busitype");
		return BusiTypeEnum.find(v);
	}

	/**
	 * 设置业务类型
	 *
	 * @param busitype 业务类型
	 */
	public void setBusitype(BusiTypeEnum busitype) {
		if (busitype != null) {
			set("busitype", busitype.getValue());
		} else {
			set("busitype", null);
		}
	}

	/**
	 * 获取代发类型
	 *
	 * @return 代发类型
	 */
	public String getAgenttype() {
		return get("agenttype");
	}

	/**
	 * 设置代发类型
	 *
	 * @param agenttype 代发类型
	 */
	public void setAgenttype(String agenttype) {
		set("agenttype", agenttype);
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
	 * 获取资金计划项目
	 *
	 * @return 资金计划项目.ID
	 */
	public Long getFundPlanProject() {
		return getString("fundPlanProject") == null ? null : Long.parseLong(getString("fundPlanProject"));
	}

	/**
	 * 设置资金计划项目
	 *
	 * @param fundPlanProject 资金计划项目.ID
	 */
	public void setFundPlanProject(Long fundPlanProject) {
		set("fundPlanProject", fundPlanProject);
	}

	/**
	 * 获取是否要占用资金计划
	 *
	 * @return 是否要占用资金计划
	 */
	public Integer getIsToPushCspl() {
		return get("isToPushCspl");
	}

	/**
	 * 设置是否要占用资金计划
	 *
	 * @param isToPushCspl 是否要占用资金计划
	 */
	public void setIsToPushCspl(Integer isToPushCspl) {
		set("isToPushCspl", isToPushCspl);
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

}
