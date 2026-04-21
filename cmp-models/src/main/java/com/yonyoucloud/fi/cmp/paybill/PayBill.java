package com.yonyoucloud.fi.cmp.paybill;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 付款单主表实体
 *
 * @author u
 * @version 1.0
 */
public class PayBill extends CmpbillEntityHeader implements IApprovalFlow, ICurrency, IPrintCount,IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.paybill.PayBill";

    /**
     * 获取核算目的
     *
     * @return 核算目的.ID
     */
	public Long getAccpurpose() {
		return get("accpurpose");
	}

    /**
     * 设置核算目的
     *
     * @param accpurpose 核算目的.ID
     */
	public void setAccpurpose(Long accpurpose) {
		set("accpurpose", accpurpose);
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
     * 获取收付事项类型
     *
     * @return 收付事项类型
     */
	public EventType getBilltype() {
		Number v = get("billtype");
		return EventType.find(v);
	}

    /**
     * 设置收付事项类型
     *
     * @param billtype 收付事项类型
     */
	public void setBilltype(EventType billtype) {
		if (billtype != null) {
			set("billtype", billtype.getValue());
		} else {
			set("billtype", null);
		}
	}

    /**
     * 获取事项类型
     *
     * @return 事项类型.ID
     */
	public String getBasebilltype() {
		return get("basebilltype");
	}

    /**
     * 设置事项类型
     *
     * @param basebilltype 事项类型.ID
     */
	public void setBasebilltype(String basebilltype) {
		set("basebilltype", basebilltype);
	}

    /**
     * 获取事项类型编码
     *
     * @return 事项类型编码
     */
	public String getBasebilltypecode() {
		return get("basebilltypecode");
	}

    /**
     * 设置事项类型编码
     *
     * @param basebilltypecode 事项类型编码
     */
	public void setBasebilltypecode(String basebilltypecode) {
		set("basebilltypecode", basebilltypecode);
	}

    /**
     * 获取来源标识
     *
     * @return 来源标识
     */
	public String getSrcflag() {
		return get("srcflag");
	}

    /**
     * 设置来源标识
     *
     * @param srcflag 来源标识
     */
	public void setSrcflag(String srcflag) {
		set("srcflag", srcflag);
	}

    /**
     * 获取生单来源标识
     *
     * @return 生单来源标识
     */
	public String getPushsrcflag() {
		return get("pushsrcflag");
	}

    /**
     * 设置生单来源标识
     *
     * @param pushsrcflag 生单来源标识
     */
	public void setPushsrcflag(String pushsrcflag) {
		set("pushsrcflag", pushsrcflag);
	}

    /**
     * 获取组织
     *
     * @return 组织.ID
     */
	public String getOrg() {
		return get("org");
	}

    /**
     * 设置组织
     *
     * @param org 组织.ID
     */
	public void setOrg(String org) {
		set("org", org);
	}


	/**
	 * 获取资金组织
	 *
	 * @return 资金组织.ID
	 */
	public String getAccentity() {
		return get("accentity");
	}

	/**
	 * 设置资金组织
	 *
	 * @param accentity 资金组织.ID
	 */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

    /**
     * 获取收付款对象类型
     *
     * @return 收付款对象类型
     */
	public CaObject getCaobject() {
		Number v = get("caobject");
		return CaObject.find(v);
	}

    /**
     * 设置收付款对象类型
     *
     * @param caobject 收付款对象类型
     */
	public void setCaobject(CaObject caobject) {
		if (caobject != null) {
			set("caobject", caobject.getValue());
		} else {
			set("caobject", null);
		}
	}

    /**
     * 获取供应商
     *
     * @return 供应商.ID
     */
	public Long getSupplier() {
		return get("supplier");
	}

    /**
     * 设置供应商
     *
     * @param supplier 供应商.ID
     */
	public void setSupplier(Long supplier) {
		set("supplier", supplier);
	}

    /**
     * 获取客户
     *
     * @return 客户.ID
     */
	public Long getCustomer() {
		return get("customer");
	}

    /**
     * 设置客户
     *
     * @param customer 客户.ID
     */
	public void setCustomer(Long customer) {
		set("customer", customer);
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
     * 获取员工
     *
     * @return 员工.ID
     */
	public String getEmployee() {
		return get("employee");
	}

    /**
     * 设置员工
     *
     * @param employee 员工.ID
     */
	public void setEmployee(String employee) {
		set("employee", employee);
	}

    /**
     * 获取业务员
     *
     * @return 业务员.ID
     */
	public String getOperator() {
		return get("operator");
	}

    /**
     * 设置业务员
     *
     * @param operator 业务员.ID
     */
	public void setOperator(String operator) {
		set("operator", operator);
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
     * 获取是否红冲
     *
     * @return 是否红冲
     */
	public Boolean getRedflag() {
	    return getBoolean("redflag");
	}

    /**
     * 设置是否红冲
     *
     * @param redflag 是否红冲
     */
	public void setRedflag(Boolean redflag) {
		set("redflag", redflag);
	}

    /**
     * 获取期初标志
     *
     * @return 期初标志
     */
	public Boolean getInitflag() {
	    return getBoolean("initflag");
	}

    /**
     * 设置期初标志
     *
     * @param initflag 期初标志
     */
	public void setInitflag(Boolean initflag) {
		set("initflag", initflag);
	}

    /**
     * 获取是否传现金标志
     *
     * @return 是否传现金标志
     */
	public Boolean getCmpflag() {
	    return getBoolean("cmpflag");
	}

    /**
     * 设置是否传现金标志
     *
     * @param cmpflag 是否传现金标志
     */
	public void setCmpflag(Boolean cmpflag) {
		set("cmpflag", cmpflag);
	}

    /**
     * 获取余额
     *
     * @return 余额
     */
	public java.math.BigDecimal getBalance() {
		return get("balance");
	}

    /**
     * 设置余额
     *
     * @param balance 余额
     */
	public void setBalance(java.math.BigDecimal balance) {
		set("balance", balance);
	}

    /**
     * 获取本币余额
     *
     * @return 本币余额
     */
	public java.math.BigDecimal getLocalbalance() {
		return get("localbalance");
	}

    /**
     * 设置本币余额
     *
     * @param localbalance 本币余额
     */
	public void setLocalbalance(java.math.BigDecimal localbalance) {
		set("localbalance", localbalance);
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
     * 获取企业银行账户
     *
     * @return 企业银行账户.ID
     */
	public String getEnterprisebankaccount() {
		return get("enterprisebankaccount");
	}

    /**
     * 设置企业银行账户
     *
     * @param enterprisebankaccount 企业银行账户.ID
     */
	public void setEnterprisebankaccount(String enterprisebankaccount) {
		set("enterprisebankaccount", enterprisebankaccount);
	}

    /**
     * 获取客户银行账户
     *
     * @return 客户银行账户.ID
     */
	public Long getCustomerbankaccount() {
		return get("customerbankaccount");
	}

    /**
     * 设置客户银行账户
     *
     * @param customerbankaccount 客户银行账户.ID
     */
	public void setCustomerbankaccount(Long customerbankaccount) {
		set("customerbankaccount", customerbankaccount);
	}

    /**
     * 获取客户银行名称
     *
     * @return 客户银行名称
     */
	public String getCustomerbankname() {
		return get("customerbankname");
	}

    /**
     * 设置客户银行名称
     *
     * @param customerbankname 客户银行名称
     */
	public void setCustomerbankname(String customerbankname) {
		set("customerbankname", customerbankname);
	}

    /**
     * 获取供应商银行账户
     *
     * @return 供应商银行账户.ID
     */
	public Long getSupplierbankaccount() {
		return get("supplierbankaccount");
	}

    /**
     * 设置供应商银行账户
     *
     * @param supplierbankaccount 供应商银行账户.ID
     */
	public void setSupplierbankaccount(Long supplierbankaccount) {
		set("supplierbankaccount", supplierbankaccount);
	}

    /**
     * 获取供应商银行名称
     *
     * @return 供应商银行名称
     */
	public String getSupplierbankname() {
		return get("supplierbankname");
	}

    /**
     * 设置供应商银行名称
     *
     * @param supplierbankname 供应商银行名称
     */
	public void setSupplierbankname(String supplierbankname) {
		set("supplierbankname", supplierbankname);
	}

    /**
     * 获取企业现金账户
     *
     * @return 企业现金账户.ID
     */
	public String getCashaccount() {
		return get("cashaccount");
	}

    /**
     * 设置企业现金账户
     *
     * @param cashaccount 企业现金账户.ID
     */
	public void setCashaccount(String cashaccount) {
		set("cashaccount", cashaccount);
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
     * 获取散户
     *
     * @return 散户
     */
	public String getRetailer() {
		return get("retailer");
	}

    /**
     * 设置散户
     *
     * @param retailer 散户
     */
	public void setRetailer(String retailer) {
		set("retailer", retailer);
	}

    /**
     * 获取订单编号
     *
     * @return 订单编号
     */
	public String getOrderno() {
		return get("orderno");
	}

    /**
     * 设置订单编号
     *
     * @param orderno 订单编号
     */
	public void setOrderno(String orderno) {
		set("orderno", orderno);
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
     * 获取核销状态
     *
     * @return 核销状态
     */
	public WriteOffStatus getWriteoffstatus() {
		Number v = get("writeoffstatus");
		return WriteOffStatus.find(v);
	}

    /**
     * 设置核销状态
     *
     * @param writeoffstatus 核销状态
     */
	public void setWriteoffstatus(WriteOffStatus writeoffstatus) {
		if (writeoffstatus != null) {
			set("writeoffstatus", writeoffstatus.getValue());
		} else {
			set("writeoffstatus", null);
		}
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
     * 获取源头单据号
     *
     * @return 源头单据号
     */
	public String getTopsrcbillno() {
		return get("topsrcbillno");
	}

    /**
     * 设置源头单据号
     *
     * @param topsrcbillno 源头单据号
     */
	public void setTopsrcbillno(String topsrcbillno) {
		set("topsrcbillno", topsrcbillno);
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
     * @param settleuser 结算人
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
     * 获取员工银行账户
     *
     * @return 员工银行账户.ID
     */
	public String getStaffBankAccount() {
		return get("staffBankAccount");
	}

    /**
     * 设置员工银行账户
     *
     * @param staffBankAccount 员工银行账户.ID
     */
	public void setStaffBankAccount(String staffBankAccount) {
		set("staffBankAccount", staffBankAccount);
	}

    /**
     * 获取散户账户名称
     *
     * @return 散户账户名称
     */
	public String getRetailerAccountName() {
		return get("retailerAccountName");
	}

    /**
     * 设置散户账户名称
     *
     * @param retailerAccountName 散户账户名称
     */
	public void setRetailerAccountName(String retailerAccountName) {
		set("retailerAccountName", retailerAccountName);
	}

    /**
     * 获取散户账号
     *
     * @return 散户账号
     */
	public String getRetailerAccountNo() {
		return get("retailerAccountNo");
	}

    /**
     * 设置散户账号
     *
     * @param retailerAccountNo 散户账号
     */
	public void setRetailerAccountNo(String retailerAccountNo) {
		set("retailerAccountNo", retailerAccountNo);
	}

    /**
     * 获取散户收款类型
     *
     * @return 散户收款类型
     */
	public RetailerAccountType getRetailerAccountType() {
		Number v = get("retailerAccountType");
		return RetailerAccountType.find(v);
	}

    /**
     * 设置散户收款类型
     *
     * @param retailerAccountType 散户收款类型
     */
	public void setRetailerAccountType(RetailerAccountType retailerAccountType) {
		if (retailerAccountType != null) {
			set("retailerAccountType", retailerAccountType.getValue());
		} else {
			set("retailerAccountType", null);
		}
	}

    /**
     * 获取散户账户银行类别
     *
     * @return 散户账户银行类别.ID
     */
	public String getRetailerBankType() {
		return get("retailerBankType");
	}

    /**
     * 设置散户账户银行类别
     *
     * @param retailerBankType 散户账户银行类别.ID
     */
	public void setRetailerBankType(String retailerBankType) {
		set("retailerBankType", retailerBankType);
	}

    /**
     * 获取散户账户联行号
     *
     * @return 散户账户联行号
     */
	public String getRetailerLineNumber() {
		return get("retailerLineNumber");
	}

    /**
     * 设置散户账户联行号
     *
     * @param retailerLineNumber 散户账户联行号
     */
	public void setRetailerLineNumber(String retailerLineNumber) {
		set("retailerLineNumber", retailerLineNumber);
	}

	/**
	 * 获取散户账号银行网点
	 *
	 * @return 散户账号银行网点.ID
	 */
	public String getRetailerBankDot() {
		return get("retailerBankDot");
	}

	/**
	 * 设置散户账号银行网点
	 *
	 * @param retailerBankDot 散户账号银行网点.ID
	 */
	public void setRetailerBankDot(String retailerBankDot) {
		set("retailerBankDot", retailerBankDot);
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
     * 获取流程防串改加签
     *
     * @return 流程防串改加签
     */
	public String getSignature() {
		return get("signature");
	}

    /**
     * 设置流程防串改加签
     *
     * @param signature 流程防串改加签
     */
	public void setSignature(String signature) {
		set("signature", signature);
	}

    /**
     * 获取回调地址
     *
     * @return 回调地址
     */
	public String getCallback() {
		return get("callback");
	}

    /**
     * 设置回调地址
     *
     * @param callback 回调地址
     */
	public void setCallback(String callback) {
		set("callback", callback);
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
     * 获取登账日期
     *
     * @return 登账日期
     */
	public java.util.Date getDzdate() {
		return get("dzdate");
	}

    /**
     * 设置登账日期
     *
     * @param dzdate 登账日期
     */
	public void setDzdate(java.util.Date dzdate) {
		set("dzdate", dzdate);
	}

    /**
     * 获取汇兑计提标志
     *
     * @return 汇兑计提标志
     */
	public Boolean getLossearnflag() {
	    return getBoolean("lossearnflag");
	}

    /**
     * 设置汇兑计提标志
     *
     * @param lossearnflag 汇兑计提标志
     */
	public void setLossearnflag(Boolean lossearnflag) {
		set("lossearnflag", lossearnflag);
	}

    /**
     * 获取汇兑计提日期
     *
     * @return 汇兑计提日期
     */
	public java.util.Date getLossearnprovisiondate() {
		return get("lossearnprovisiondate");
	}

    /**
     * 设置汇兑计提日期
     *
     * @param lossearnprovisiondate 汇兑计提日期
     */
	public void setLossearnprovisiondate(java.util.Date lossearnprovisiondate) {
		set("lossearnprovisiondate", lossearnprovisiondate);
	}

    /**
     * 获取来源类型标识
     *
     * @return 来源类型标识
     */
	public String getSrctypeflag() {
		return get("srctypeflag");
	}

    /**
     * 设置来源类型标识
     *
     * @param srctypeflag 来源类型标识
     */
	public void setSrctypeflag(String srctypeflag) {
		set("srctypeflag", srctypeflag);
	}

    /**
     * 获取协同单据确认状态
     *
     * @return 协同单据确认状态
     */
	public SynergyStatus getSynergystatus() {
		Number v = get("synergystatus");
		return SynergyStatus.find(v);
	}

    /**
     * 设置协同单据确认状态
     *
     * @param synergystatus 协同单据确认状态
     */
	public void setSynergystatus(SynergyStatus synergystatus) {
		if (synergystatus != null) {
			set("synergystatus", synergystatus.getValue());
		} else {
			set("synergystatus", null);
		}
	}

    /**
     * 获取预占用金额
     *
     * @return 预占用金额
     */
	public java.math.BigDecimal getBookAmount() {
		return get("bookAmount");
	}

    /**
     * 设置预占用金额
     *
     * @param bookAmount 预占用金额
     */
	public void setBookAmount(java.math.BigDecimal bookAmount) {
		set("bookAmount", bookAmount);
	}

    /**
     * 获取散户银行开户行
     *
     * @return 散户银行开户行
     */
	public String getRetailerBankName() {
		return get("retailerBankName");
	}

    /**
     * 设置散户银行开户行
     *
     * @param retailerBankName 散户银行开户行
     */
	public void setRetailerBankName(String retailerBankName) {
		set("retailerBankName", retailerBankName);
	}

    /**
     * 获取合同编号
     *
     * @return 合同编号
     */
	public String getContractno() {
		return get("contractno");
	}

    /**
     * 设置合同编号
     *
     * @param contractno 合同编号
     */
	public void setContractno(String contractno) {
		set("contractno", contractno);
	}

    /**
     * 获取是否批量支付
     *
     * @return 是否批量支付
     */
	public Boolean getIsBatchPay() {
	    return getBoolean("isBatchPay");
	}

    /**
     * 设置是否批量支付
     *
     * @param isBatchPay 是否批量支付
     */
	public void setIsBatchPay(Boolean isBatchPay) {
		set("isBatchPay", isBatchPay);
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
     * 获取自定义项1
     *
     * @return 自定义项1
     */
	public String getDefine1() {
		return get("define1");
	}

    /**
     * 设置自定义项1
     *
     * @param define1 自定义项1
     */
	public void setDefine1(String define1) {
		set("define1", define1);
	}

    /**
     * 获取自定义项2
     *
     * @return 自定义项2
     */
	public String getDefine2() {
		return get("define2");
	}

    /**
     * 设置自定义项2
     *
     * @param define2 自定义项2
     */
	public void setDefine2(String define2) {
		set("define2", define2);
	}

    /**
     * 获取自定义项3
     *
     * @return 自定义项3
     */
	public String getDefine3() {
		return get("define3");
	}

    /**
     * 设置自定义项3
     *
     * @param define3 自定义项3
     */
	public void setDefine3(String define3) {
		set("define3", define3);
	}

    /**
     * 获取自定义项4
     *
     * @return 自定义项4
     */
	public String getDefine4() {
		return get("define4");
	}

    /**
     * 设置自定义项4
     *
     * @param define4 自定义项4
     */
	public void setDefine4(String define4) {
		set("define4", define4);
	}

    /**
     * 获取自定义项5
     *
     * @return 自定义项5
     */
	public String getDefine5() {
		return get("define5");
	}

    /**
     * 设置自定义项5
     *
     * @param define5 自定义项5
     */
	public void setDefine5(String define5) {
		set("define5", define5);
	}

    /**
     * 获取自定义项6
     *
     * @return 自定义项6
     */
	public String getDefine6() {
		return get("define6");
	}

    /**
     * 设置自定义项6
     *
     * @param define6 自定义项6
     */
	public void setDefine6(String define6) {
		set("define6", define6);
	}

    /**
     * 获取自定义项7
     *
     * @return 自定义项7
     */
	public String getDefine7() {
		return get("define7");
	}

    /**
     * 设置自定义项7
     *
     * @param define7 自定义项7
     */
	public void setDefine7(String define7) {
		set("define7", define7);
	}

    /**
     * 获取自定义项8
     *
     * @return 自定义项8
     */
	public String getDefine8() {
		return get("define8");
	}

    /**
     * 设置自定义项8
     *
     * @param define8 自定义项8
     */
	public void setDefine8(String define8) {
		set("define8", define8);
	}

    /**
     * 获取自定义项9
     *
     * @return 自定义项9
     */
	public String getDefine9() {
		return get("define9");
	}

    /**
     * 设置自定义项9
     *
     * @param define9 自定义项9
     */
	public void setDefine9(String define9) {
		set("define9", define9);
	}

    /**
     * 获取自定义项10
     *
     * @return 自定义项10
     */
	public String getDefine10() {
		return get("define10");
	}

    /**
     * 设置自定义项10
     *
     * @param define10 自定义项10
     */
	public void setDefine10(String define10) {
		set("define10", define10);
	}

	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public BizObject getPayBillCharacterDef() {
		return get("payBillCharacterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param payBillCharacterDef 自定义项特征属性组.ID
	 */
	public void setPayBillCharacterDef(BizObject payBillCharacterDef) {
		set("payBillCharacterDef", payBillCharacterDef);
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
	public void setStatus(Short status) {
		set("status", status);
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
     * 获取付款单子表集合
     *
     * @return 付款单子表集合
     */
	public java.util.List<PayBillb> PayBillb() {
		return getBizObjects("PayBillb", PayBillb.class);
	}

    /**
     * 设置付款单子表集合
     *
     * @param PayBillb 付款单子表集合
     */
	public void setPayBillb(java.util.List<PayBillb> PayBillb) {
		setBizObjects("PayBillb", PayBillb);
	}

	/**
	 * 获取上送表单编号
	 *
	 * @return 上送表单编号
	 */
	public String getUpsrcbillno() {
		return get("upsrcbillno");
	}

	/**
	 * 设置上送表单编号
	 *
	 * @param upsrcbillno 上送表单编号
	 */
	public void setUpsrcbillno(String upsrcbillno) {
		set("upsrcbillno", upsrcbillno);
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
	 * 获取关联状态
	 *
	 * @return 关联状态
	 */
	public Short getAssociationStatus() {
		return getShort("associationStatus");
	}

	/**
	 * 设置关联状态
	 *
	 * @param associationStatus 关联状态
	 */
	public void setAssociationStatus(Short associationStatus) {
		set("associationStatus", associationStatus);
	}

	/**
	 * 获取银行对账单ID
	 *
	 * @return 银行对账单ID
	 */
	public String getBankReconciliationId() {
		return get("bankReconciliationId");
	}

	/**
	 * 设置银行对账单ID
	 *
	 * @param bankReconciliationId 银行对账单ID
	 */
	public void setBankReconciliationId(String bankReconciliationId) {
		set("bankReconciliationId", bankReconciliationId);
	}

	/**
	 * 获取认领单ID
	 *
	 * @return 认领单ID
	 */
	public String getBillClaimId() {
		return get("billClaimId");
	}

	/**
	 * 设置认领单ID
	 *
	 * @param billClaimId 认领单ID
	 */
	public void setBillClaimId(String billClaimId) {
		set("billClaimId", billClaimId);
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
	 * 获取租户id
	 *
	 * @return 租户id
	 */
	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	/**
	 * 设置租户id
	 *
	 * @param ytenant 租户id
	 */
	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

	/**
	 * 获取是否已迁移
	 *
	 * @return 是否已迁移
	 */
	public Boolean getMigrateflag() {
		return getBoolean("migrateflag");
	}

	/**
	 * 设置是否已迁移
	 *
	 * @param migrateflag 是否已迁移
	 */
	public void setMigrateflag(Boolean migrateflag) {
		set("migrateflag", migrateflag);
	}

}
