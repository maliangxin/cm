package com.yonyoucloud.fi.cmp.foreignpayment;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.*;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 外汇付款实体
 *
 * @author u
 * @version 1.0
 */
public class ForeignPayment extends BizObject implements IAuditInfo, IApprovalInfo, ICurrency, IApprovalFlow, IPrintCount, ITenant, IYTenant, AccentityRawInterface {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.foreignpayment.ForeignPayment";

	public static final String BUSI_OBJ_CODE = "ctm-cmp.cmp_foreignpayment";
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
	 * 获取单据编号
	 *
	 * @return 单据编号
	 */
	public String getCode() {
		return get("code");
	}

	/**
	 * 设置单据编号
	 *
	 * @param code 单据编号
	 */
	public void setCode(String code) {
		set("code", code);
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
	 * 获取事项来源
	 *
	 * @return 事项来源
	 */
	public Short getSrcitem() {
		return getShort("srcitem");
	}

	/**
	 * 设置事项来源
	 *
	 * @param srcitem 事项来源
	 */
	public void setSrcitem(Short srcitem) {
		set("srcitem", srcitem);
	}

	/**
	 * 获取单据类型
	 *
	 * @return 单据类型.ID
	 */
	public String getBilltype() {
		return get("billtype");
	}

	/**
	 * 设置单据类型
	 *
	 * @param billtype 单据类型.ID
	 */
	public void setBilltype(String billtype) {
		set("billtype", billtype);
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
	 * 获取单据状态
	 *
	 * @return 单据状态
	 */
	public Short getVerifystate() {
		return getShort("verifystate");
	}

	/**
	 * 设置单据状态
	 *
	 * @param verifystate 单据状态
	 */
	public void setVerifystate(Short verifystate) {
		set("verifystate", verifystate);
	}

	/**
	 * 获取凭证状态
	 *
	 * @return 凭证状态
	 */
	public Short getVoucherstatus() {
		return getShort("voucherstatus");
	}

	/**
	 * 设置凭证状态
	 *
	 * @param voucherstatus 凭证状态
	 */
	public void setVoucherstatus(Short voucherstatus) {
		set("voucherstatus", voucherstatus);
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
	 * 获取是否跨境
	 *
	 * @return 是否跨境
	 */
	public Short getIscrossborder() {
		return getShort("iscrossborder");
	}

	/**
	 * 设置是否跨境
	 *
	 * @param iscrossborder 是否跨境
	 */
	public void setIscrossborder(Short iscrossborder) {
		set("iscrossborder", iscrossborder);
	}

	/**
	 * 获取是否加急
	 *
	 * @return 是否加急
	 */
	public Short getIsurgent() {
		return getShort("isurgent");
	}

	/**
	 * 设置是否加急
	 *
	 * @param isurgent 是否加急
	 */
	public void setIsurgent(Short isurgent) {
		set("isurgent", isurgent);
	}

	/**
	 * 获取原币币种
	 *
	 * @return 原币币种.ID
	 */
	public String getCurrency() {
		return get("currency");
	}

	/**
	 * 设置原币币种
	 *
	 * @param currency 原币币种.ID
	 */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

	/**
	 * 获取原币金额
	 *
	 * @return 原币金额
	 */
	public java.math.BigDecimal getAmount() {
		return get("amount");
	}

	/**
	 * 设置原币金额
	 *
	 * @param amount 原币金额
	 */
	public void setAmount(java.math.BigDecimal amount) {
		set("amount", amount);
	}

	/**
	 * 获取金额合计大写
	 *
	 * @return 金额合计大写
	 */
	public String getAmountuppercase() {
		return get("amountuppercase");
	}

	/**
	 * 设置金额合计大写
	 *
	 * @param amountuppercase 金额合计大写
	 */
	public void setAmountuppercase(String amountuppercase) {
		set("amountuppercase", amountuppercase);
	}

	/**
	 * 获取期望结算日期
	 *
	 * @return 期望结算日期
	 */
	public java.util.Date getExpectedsettlementdate() {
		return get("expectedsettlementdate");
	}

	/**
	 * 设置期望结算日期
	 *
	 * @param expectedsettlementdate 期望结算日期
	 */
	public void setExpectedsettlementdate(java.util.Date expectedsettlementdate) {
		set("expectedsettlementdate", expectedsettlementdate);
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
	 * 获取付款方银行账户
	 *
	 * @return 付款方银行账户.ID
	 */
	public String getPaymenterprisebankaccount() {
		return get("paymenterprisebankaccount");
	}

	/**
	 * 设置付款方银行账户
	 *
	 * @param paymenterprisebankaccount 付款方银行账户.ID
	 */
	public void setPaymenterprisebankaccount(String paymenterprisebankaccount) {
		set("paymenterprisebankaccount", paymenterprisebankaccount);
	}

	/**
	 * 获取是否直联
	 *
	 * @return 是否直联
	 */
	public Short getIsdirectlyconnected() {
		return getShort("isdirectlyconnected");
	}

	/**
	 * 设置是否直联
	 *
	 * @param isdirectlyconnected 是否直联
	 */
	public void setIsdirectlyconnected(Short isdirectlyconnected) {
		set("isdirectlyconnected", isdirectlyconnected);
	}

	/**
	 * 获取付款方常驻国家地区
	 *
	 * @return 付款方常驻国家地区.ID
	 */
	public String getPaycountry() {
		return get("paycountry");
	}

	/**
	 * 设置付款方常驻国家地区
	 *
	 * @param paycountry 付款方常驻国家地区.ID
	 */
	public void setPaycountry(String paycountry) {
		set("paycountry", paycountry);
	}

	/**
	 * 获取汇款人地址
	 *
	 * @return 汇款人地址
	 */
	public String getAddress() {
		return get("address");
	}

	/**
	 * 设置汇款人地址
	 *
	 * @param address 汇款人地址
	 */
	public void setAddress(String address) {
		set("address", address);
	}

	/**
	 * 获取外汇支付方式
	 *
	 * @return 外汇支付方式
	 */
	public Short getForeignpaymenttype() {
		return getShort("foreignpaymenttype");
	}

	/**
	 * 设置外汇支付方式
	 *
	 * @param foreignpaymenttype 外汇支付方式
	 */
	public void setForeignpaymenttype(Short foreignpaymenttype) {
		set("foreignpaymenttype", foreignpaymenttype);
	}

	/**
	 * 获取汇款附言
	 *
	 * @return 汇款附言
	 */
	public String getPostscript() {
		return get("postscript");
	}

	/**
	 * 设置汇款附言
	 *
	 * @param postscript 汇款附言
	 */
	public void setPostscript(String postscript) {
		set("postscript", postscript);
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
	 * 获取对公/对私
	 *
	 * @return 对公/对私
	 */
	public Short getPublicorprivate() {
		return getShort("publicorprivate");
	}

	/**
	 * 设置对公/对私
	 *
	 * @param publicorprivate 对公/对私
	 */
	public void setPublicorprivate(Short publicorprivate) {
		set("publicorprivate", publicorprivate);
	}

	/**
	 * 获取收款方类型
	 *
	 * @return 收款方类型
	 */
	public Short getReceivetype() {
		return getShort("receivetype");
	}

	/**
	 * 设置收款方类型
	 *
	 * @param receivetype 收款方类型
	 */
	public void setReceivetype(Short receivetype) {
		set("receivetype", receivetype);
	}

	/**
	 * 获取收款方非中文名称
	 *
	 * @return 收款方非中文名称
	 */
	public String getReceivenameother() {
		return get("receivenameother");
	}

	/**
	 * 设置收款方非中文名称
	 *
	 * @param receivenameother 收款方非中文名称
	 */
	public void setReceivenameother(String receivenameother) {
		set("receivenameother", receivenameother);
	}

	/**
	 * 获取收款方常驻国家地区
	 *
	 * @return 收款方常驻国家地区.ID
	 */
	public String getReceivecountry() {
		return get("receivecountry");
	}

	/**
	 * 设置收款方常驻国家地区
	 *
	 * @param receivecountry 收款方常驻国家地区.ID
	 */
	public void setReceivecountry(String receivecountry) {
		set("receivecountry", receivecountry);
	}

	/**
	 * 获取收款方开户行名称
	 *
	 * @return 收款方开户行名称.ID
	 */
	public String getReceivebankaddr() {
		return get("receivebankaddr");
	}

	/**
	 * 设置收款方开户行名称
	 *
	 * @param receivebankaddr 收款方开户行名称.ID
	 */
	public void setReceivebankaddr(String receivebankaddr) {
		set("receivebankaddr", receivebankaddr);
	}

	/**
	 * 获取收款方银行类别
	 *
	 * @return 收款方银行类别.ID
	 */
	public String getReceivebanktype() {
		return get("receivebanktype");
	}

	/**
	 * 设置收款方银行类别
	 *
	 * @param receivebanktype 收款方银行类别.ID
	 */
	public void setReceivebanktype(String receivebanktype) {
		set("receivebanktype", receivebanktype);
	}

	/**
	 * 获取其他名称
	 *
	 * @return 其他名称
	 */
	public String getOthername() {
		return get("othername");
	}

	/**
	 * 设置其他名称
	 *
	 * @param othername 其他名称
	 */
	public void setOthername(String othername) {
		set("othername", othername);
	}

	/**
	 * 获取其他账户名称
	 *
	 * @return 其他账户名称
	 */
	public String getOtherbankaccountname() {
		return get("otherbankaccountname");
	}

	/**
	 * 设置其他账户名称
	 *
	 * @param otherbankaccountname 其他账户名称
	 */
	public void setOtherbankaccountname(String otherbankaccountname) {
		set("otherbankaccountname", otherbankaccountname);
	}

	/**
	 * 获取其他账号
	 *
	 * @return 其他账号
	 */
	public String getOtherbankaccount() {
		return get("otherbankaccount");
	}

	/**
	 * 设置其他账号
	 *
	 * @param otherbankaccount 其他账号
	 */
	public void setOtherbankaccount(String otherbankaccount) {
		set("otherbankaccount", otherbankaccount);
	}

	/**
	 * 获取内部单位名称
	 *
	 * @return 内部单位名称.ID
	 */
	public String getOurname() {
		return get("ourname");
	}

	/**
	 * 设置内部单位名称
	 *
	 * @param ourname 内部单位名称.ID
	 */
	public void setOurname(String ourname) {
		set("ourname", ourname);
	}

	/**
	 * 获取内部单位银行账户
	 *
	 * @return 内部单位银行账户.ID
	 */
	public String getOurbankaccount() {
		return get("ourbankaccount");
	}

	/**
	 * 设置内部单位银行账户
	 *
	 * @param ourbankaccount 内部单位银行账户.ID
	 */
	public void setOurbankaccount(String ourbankaccount) {
		set("ourbankaccount", ourbankaccount);
	}

	/**
	 * 获取客户名称
	 *
	 * @return 客户名称.ID
	 */
	public Long getCustomer() {
		return get("customer");
	}

	/**
	 * 设置客户名称
	 *
	 * @param customer 客户名称.ID
	 */
	public void setCustomer(Long customer) {
		set("customer", customer);
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
	 * 获取资金业务对象
	 *
	 * @return 资金业务对象.ID
	 */
	public String getCapBizObj() {
		return get("capBizObj");
	}

	/**
	 * 设置资金业务对象
	 *
	 * @param capBizObj 资金业务对象.ID
	 */
	public void setCapBizObj(String capBizObj) {
		set("capBizObj", capBizObj);
	}

	/**
	 * 获取资金业务对象银行账户
	 *
	 * @return 资金业务对象银行账户.ID
	 */
	public String getCapBizObjbankaccount() {
		return get("capBizObjbankaccount");
	}

	/**
	 * 设置资金业务对象银行账户
	 *
	 * @param capBizObjbankaccount 资金业务对象银行账户.ID
	 */
	public void setCapBizObjbankaccount(String capBizObjbankaccount) {
		set("capBizObjbankaccount", capBizObjbankaccount);
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
	 * 获取收款方名称
	 *
	 * @return 收款方名称
	 */
	public String getReceivename() {
		return get("receivename");
	}

	/**
	 * 设置收款方名称
	 *
	 * @param receivename 收款方名称
	 */
	public void setReceivename(String receivename) {
		set("receivename", receivename);
	}

	/**
	 * 获取收款方账号
	 *
	 * @return 收款方账号
	 */
	public String getReceivebankaccount() {
		return get("receivebankaccount");
	}

	/**
	 * 设置收款方账号
	 *
	 * @param receivebankaccount 收款方账号
	 */
	public void setReceivebankaccount(String receivebankaccount) {
		set("receivebankaccount", receivebankaccount);
	}

	/**
	 * 获取收款方账户名称
	 *
	 * @return 收款方账户名称
	 */
	public String getReceivebankaccountname() {
		return get("receivebankaccountname");
	}

	/**
	 * 设置收款方账户名称
	 *
	 * @param receivebankaccountname 收款方账户名称
	 */
	public void setReceivebankaccountname(String receivebankaccountname) {
		set("receivebankaccountname", receivebankaccountname);
	}

	/**
	 * 获取收款方地址
	 *
	 * @return 收款方地址
	 */
	public String getReceiveaddress() {
		return get("receiveaddress");
	}

	/**
	 * 设置收款方地址
	 *
	 * @param receiveaddress 收款方地址
	 */
	public void setReceiveaddress(String receiveaddress) {
		set("receiveaddress", receiveaddress);
	}

	/**
	 * 获取是否通过代理行
	 *
	 * @return 是否通过代理行
	 */
	public Short getIsagencybank() {
		return getShort("isagencybank");
	}

	/**
	 * 设置是否通过代理行
	 *
	 * @param isagencybank 是否通过代理行
	 */
	public void setIsagencybank(Short isagencybank) {
		set("isagencybank", isagencybank);
	}

	/**
	 * 获取收款方开户行在其代理行账号
	 *
	 * @return 收款方开户行在其代理行账号
	 */
	public String getAgencybankaccount() {
		return get("agencybankaccount");
	}

	/**
	 * 设置收款方开户行在其代理行账号
	 *
	 * @param agencybankaccount 收款方开户行在其代理行账号
	 */
	public void setAgencybankaccount(String agencybankaccount) {
		set("agencybankaccount", agencybankaccount);
	}

	/**
	 * 获取代理行名称
	 *
	 * @return 代理行名称
	 */
	public String getAgencybankname() {
		return get("agencybankname");
	}

	/**
	 * 设置代理行名称
	 *
	 * @param agencybankname 代理行名称
	 */
	public void setAgencybankname(String agencybankname) {
		set("agencybankname", agencybankname);
	}

	/**
	 * 获取代理行地址
	 *
	 * @return 代理行地址
	 */
	public String getAgencybankaddress() {
		return get("agencybankaddress");
	}

	/**
	 * 设置代理行地址
	 *
	 * @param agencybankaddress 代理行地址
	 */
	public void setAgencybankaddress(String agencybankaddress) {
		set("agencybankaddress", agencybankaddress);
	}

	/**
	 * 获取代理行SWIFT
	 *
	 * @return 代理行SWIFT
	 */
	public String getAgencybankswift() {
		return get("agencybankswift");
	}

	/**
	 * 设置代理行SWIFT
	 *
	 * @param agencybankswift 代理行SWIFT
	 */
	public void setAgencybankswift(String agencybankswift) {
		set("agencybankswift", agencybankswift);
	}

	/**
	 * 获取国内外费用承担方
	 *
	 * @return 国内外费用承担方
	 */
	public String getCostbearers() {
		return get("costbearers");
	}

	/**
	 * 设置国内外费用承担方
	 *
	 * @param costbearers 国内外费用承担方
	 */
	public void setCostbearers(String costbearers) {
		set("costbearers", costbearers);
	}

	/**
	 * 获取全额到账
	 *
	 * @return 全额到账
	 */
	public Short getIsfullpayment() {
		return getShort("isfullpayment");
	}

	/**
	 * 设置全额到账
	 *
	 * @param isfullpayment 全额到账
	 */
	public void setIsfullpayment(Short isfullpayment) {
		set("isfullpayment", isfullpayment);
	}

	/**
	 * 获取费用支付账号
	 *
	 * @return 费用支付账号.ID
	 */
	public String getPaymentaccount() {
		return get("paymentaccount");
	}

	/**
	 * 设置费用支付账号
	 *
	 * @param paymentaccount 费用支付账号.ID
	 */
	public void setPaymentaccount(String paymentaccount) {
		set("paymentaccount", paymentaccount);
	}

	/**
	 * 获取费用支付账号(外币)
	 *
	 * @return 费用支付账号(外币).ID
	 */
	public String getForeignpaymentaccount() {
		return get("foreignpaymentaccount");
	}

	/**
	 * 设置费用支付账号(外币)
	 *
	 * @param foreignpaymentaccount 费用支付账号(外币).ID
	 */
	public void setForeignpaymentaccount(String foreignpaymentaccount) {
		set("foreignpaymentaccount", foreignpaymentaccount);
	}

	/**
	 * 获取交易编码A
	 *
	 * @return 交易编码A.ID
	 */
	public Long getTransactioncodeA() {
		return get("transactioncodeA");
	}

	/**
	 * 设置交易编码A
	 *
	 * @param transactioncodeA 交易编码A.ID
	 */
	public void setTransactioncodeA(Long transactioncodeA) {
		set("transactioncodeA", transactioncodeA);
	}

	/**
	 * 获取交易币种A
	 *
	 * @return 交易币种A.ID
	 */
	public String getTransactioncurrencyA() {
		return get("transactioncurrencyA");
	}

	/**
	 * 设置交易币种A
	 *
	 * @param transactioncurrencyA 交易币种A.ID
	 */
	public void setTransactioncurrencyA(String transactioncurrencyA) {
		set("transactioncurrencyA", transactioncurrencyA);
	}

	/**
	 * 获取交易金额A
	 *
	 * @return 交易金额A
	 */
	public java.math.BigDecimal getTransactionamountA() {
		return get("transactionamountA");
	}

	/**
	 * 设置交易金额A
	 *
	 * @param transactionamountA 交易金额A
	 */
	public void setTransactionamountA(java.math.BigDecimal transactionamountA) {
		set("transactionamountA", transactionamountA);
	}

	/**
	 * 获取交易附言A
	 *
	 * @return 交易附言A
	 */
	public String getTradepostscriptA() {
		return get("tradepostscriptA");
	}

	/**
	 * 设置交易附言A
	 *
	 * @param tradepostscriptA 交易附言A
	 */
	public void setTradepostscriptA(String tradepostscriptA) {
		set("tradepostscriptA", tradepostscriptA);
	}

	/**
	 * 获取交易编码B
	 *
	 * @return 交易编码B.ID
	 */
	public Long getTransactioncodeB() {
		return get("transactioncodeB");
	}

	/**
	 * 设置交易编码B
	 *
	 * @param transactioncodeB 交易编码B.ID
	 */
	public void setTransactioncodeB(Long transactioncodeB) {
		set("transactioncodeB", transactioncodeB);
	}

	/**
	 * 获取交易币种B
	 *
	 * @return 交易币种B.ID
	 */
	public String getTransactioncurrencyB() {
		return get("transactioncurrencyB");
	}

	/**
	 * 设置交易币种B
	 *
	 * @param transactioncurrencyB 交易币种B.ID
	 */
	public void setTransactioncurrencyB(String transactioncurrencyB) {
		set("transactioncurrencyB", transactioncurrencyB);
	}

	/**
	 * 获取交易金额A
	 *
	 * @return 交易金额A
	 */
	public java.math.BigDecimal getTransactionamountB() {
		return get("transactionamountB");
	}

	/**
	 * 设置交易金额A
	 *
	 * @param transactionamountB 交易金额A
	 */
	public void setTransactionamountB(java.math.BigDecimal transactionamountB) {
		set("transactionamountB", transactionamountB);
	}

	/**
	 * 获取交易附言B
	 *
	 * @return 交易附言B
	 */
	public String getTradepostscriptB() {
		return get("tradepostscriptB");
	}

	/**
	 * 设置交易附言B
	 *
	 * @param tradepostscriptB 交易附言B
	 */
	public void setTradepostscriptB(String tradepostscriptB) {
		set("tradepostscriptB", tradepostscriptB);
	}

	/**
	 * 获取资金用途
	 *
	 * @return 资金用途
	 */
	public String getFundpurpose() {
		return get("fundpurpose");
	}

	/**
	 * 设置资金用途
	 *
	 * @param fundpurpose 资金用途
	 */
	public void setFundpurpose(String fundpurpose) {
		set("fundpurpose", fundpurpose);
	}

	/**
	 * 获取付款性质
	 *
	 * @return 付款性质
	 */
	public String getPaymentnature() {
		return get("paymentnature");
	}

	/**
	 * 设置付款性质
	 *
	 * @param paymentnature 付款性质
	 */
	public void setPaymentnature(String paymentnature) {
		set("paymentnature", paymentnature);
	}

	/**
	 * 获取是否为保税货物项下付款
	 *
	 * @return 是否为保税货物项下付款
	 */
	public Short getIsbondedgoodspay() {
		return getShort("isbondedgoodspay");
	}

	/**
	 * 设置是否为保税货物项下付款
	 *
	 * @param isbondedgoodspay 是否为保税货物项下付款
	 */
	public void setIsbondedgoodspay(Short isbondedgoodspay) {
		set("isbondedgoodspay", isbondedgoodspay);
	}

	/**
	 * 获取合同号
	 *
	 * @return 合同号
	 */
	public String getContractnumber() {
		return get("contractnumber");
	}

	/**
	 * 设置合同号
	 *
	 * @param contractnumber 合同号
	 */
	public void setContractnumber(String contractnumber) {
		set("contractnumber", contractnumber);
	}

	/**
	 * 获取发票号
	 *
	 * @return 发票号
	 */
	public String getInvoicenumber() {
		return get("invoicenumber");
	}

	/**
	 * 设置发票号
	 *
	 * @param invoicenumber 发票号
	 */
	public void setInvoicenumber(String invoicenumber) {
		set("invoicenumber", invoicenumber);
	}

	/**
	 * 获取外汇局批件号/备案表号/业务编号
	 *
	 * @return 外汇局批件号/备案表号/业务编号
	 */
	public String getFilingnumber() {
		return get("filingnumber");
	}

	/**
	 * 设置外汇局批件号/备案表号/业务编号
	 *
	 * @param filingnumber 外汇局批件号/备案表号/业务编号
	 */
	public void setFilingnumber(String filingnumber) {
		set("filingnumber", filingnumber);
	}

	/**
	 * 获取填报人姓名
	 *
	 * @return 填报人姓名
	 */
	public String getApplicantname() {
		return get("applicantname");
	}

	/**
	 * 设置填报人姓名
	 *
	 * @param applicantname 填报人姓名
	 */
	public void setApplicantname(String applicantname) {
		set("applicantname", applicantname);
	}

	/**
	 * 获取填报人电话
	 *
	 * @return 填报人电话
	 */
	public String getApplicantphonenumber() {
		return get("applicantphonenumber");
	}

	/**
	 * 设置填报人电话
	 *
	 * @param applicantphonenumber 填报人电话
	 */
	public void setApplicantphonenumber(String applicantphonenumber) {
		set("applicantphonenumber", applicantphonenumber);
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
	 * 获取款项类型
	 *
	 * @return 款项类型.ID
	 */
	public Long getQuickType() {
		return get("quickType");
	}

	/**
	 * 设置款项类型
	 *
	 * @param quickType 款项类型.ID
	 */
	public void setQuickType(Long quickType) {
		set("quickType", quickType);
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
	 * 获取折本币汇率类型
	 *
	 * @return 折本币汇率类型.ID
	 */
	public String getCurrencyexchangeratetype() {
		return get("currencyexchangeratetype");
	}

	/**
	 * 设置折本币汇率类型
	 *
	 * @param currencyexchangeratetype 折本币汇率类型.ID
	 */
	public void setCurrencyexchangeratetype(String currencyexchangeratetype) {
		set("currencyexchangeratetype", currencyexchangeratetype);
	}

	/**
	 * 获取折本币汇率
	 *
	 * @return 折本币汇率
	 */
	public java.math.BigDecimal getCurrencyexchRate() {
		return get("currencyexchRate");
	}

	/**
	 * 设置折本币汇率
	 *
	 * @param currencyexchRate 折本币汇率
	 */
	public void setCurrencyexchRate(java.math.BigDecimal currencyexchRate) {
		set("currencyexchRate", currencyexchRate);
	}

    /**
     * 获取折本币汇率折算方式
     *
     * @return 折本币汇率折算方式
     */
    public Short getCurrencyexchRateOps() {
        return getShort("currencyexchRateOps");
    }

    /**
     * 设置折本币汇率折算方式
     *
     * @param currencyexchRateOps 折本币汇率折算方式
     */
    public void setCurrencyexchRateOps(Short currencyexchRateOps) {
        set("currencyexchRateOps", currencyexchRateOps);
    }

	/**
	 * 获取折本币金额
	 *
	 * @return 折本币金额
	 */
	public java.math.BigDecimal getCurrencyamount() {
		return get("currencyamount");
	}

	/**
	 * 设置折本币金额
	 *
	 * @param currencyamount 折本币金额
	 */
	public void setCurrencyamount(java.math.BigDecimal currencyamount) {
		set("currencyamount", currencyamount);
	}

	/**
	 * 获取换出汇率类型
	 *
	 * @return 换出汇率类型.ID
	 */
	public String getSwapOutExchangeRateType() {
		return get("swapOutExchangeRateType");
	}

	/**
	 * 设置换出汇率类型
	 *
	 * @param swapOutExchangeRateType 换出汇率类型.ID
	 */
	public void setSwapOutExchangeRateType(String swapOutExchangeRateType) {
		set("swapOutExchangeRateType", swapOutExchangeRateType);
	}

	/**
	 * 获取换出汇率预估
	 *
	 * @return 换出汇率预估
	 */
	public java.math.BigDecimal getSwapOutExchangeRateEstimate() {
		return get("swapOutExchangeRateEstimate");
	}

	/**
	 * 设置换出汇率预估
	 *
	 * @param swapOutExchangeRateEstimate 换出汇率预估
	 */
	public void setSwapOutExchangeRateEstimate(java.math.BigDecimal swapOutExchangeRateEstimate) {
		set("swapOutExchangeRateEstimate", swapOutExchangeRateEstimate);
	}

    /**
     * 获取换出汇率预估折算方式
     *
     * @return 换出汇率预估折算方式
     */
    public Short getSwapOutExchangeRateEstimateOps() {
        return getShort("swapOutExchangeRateEstimateOps");
    }

    /**
     * 设置换出汇率预估折算方式
     *
     * @param swapOutExchangeRateEstimateOps 换出汇率预估折算方式
     */
    public void setSwapOutExchangeRateEstimateOps(Short swapOutExchangeRateEstimateOps) {
        set("swapOutExchangeRateEstimateOps", swapOutExchangeRateEstimateOps);
    }

	/**
	 * 获取换出金额预估
	 *
	 * @return 换出金额预估
	 */
	public java.math.BigDecimal getSwapOutAmountEstimate() {
		return get("swapOutAmountEstimate");
	}

	/**
	 * 设置换出金额预估
	 *
	 * @param swapOutAmountEstimate 换出金额预估
	 */
	public void setSwapOutAmountEstimate(java.math.BigDecimal swapOutAmountEstimate) {
		set("swapOutAmountEstimate", swapOutAmountEstimate);
	}

	/**
	 * 获取实际结算汇率类型
	 *
	 * @return 实际结算汇率类型.ID
	 */
	public String getSettleExchangeRateType() {
		return get("settleExchangeRateType");
	}

	/**
	 * 设置实际结算汇率类型
	 *
	 * @param settleExchangeRateType 实际结算汇率类型.ID
	 */
	public void setSettleExchangeRateType(String settleExchangeRateType) {
		set("settleExchangeRateType", settleExchangeRateType);
	}

	/**
	 * 获取实际结算汇率
	 *
	 * @return 实际结算汇率
	 */
	public java.math.BigDecimal getSettleExchangeRate() {
		return get("settleExchangeRate");
	}

	/**
	 * 设置实际结算汇率
	 *
	 * @param settleExchangeRate 实际结算汇率
	 */
	public void setSettleExchangeRate(java.math.BigDecimal settleExchangeRate) {
		set("settleExchangeRate", settleExchangeRate);
	}

    /**
     * 获取实际结算汇率折算方式
     *
     * @return 实际结算汇率折算方式
     */
    public Short getSettleExchangeRateOps() {
        return getShort("settleExchangeRateOps");
    }

    /**
     * 设置实际结算汇率折算方式
     *
     * @param settleExchangeRateOps 实际结算汇率折算方式
     */
    public void setSettleExchangeRateOps(Short settleExchangeRateOps) {
        set("settleExchangeRateOps", settleExchangeRateOps);
    }

	/**
	 * 获取实际结算金额
	 *
	 * @return 实际结算金额
	 */
	public java.math.BigDecimal getSettleAmount() {
		return get("settleAmount");
	}

	/**
	 * 设置实际结算金额
	 *
	 * @param settleAmount 实际结算金额
	 */
	public void setSettleAmount(java.math.BigDecimal settleAmount) {
		set("settleAmount", settleAmount);
	}

	/**
	 * 获取结算状态
	 *
	 * @return 结算状态
	 */
	public Short getSettlestatus() {
		return getShort("settlestatus");
	}

	/**
	 * 设置结算状态
	 *
	 * @param settlestatus 结算状态
	 */
	public void setSettlestatus(Short settlestatus) {
		set("settlestatus", settlestatus);
	}

	/**
	 * 获取结算成功金额
	 *
	 * @return 结算成功金额
	 */
	public java.math.BigDecimal getSettlesuccessSum() {
		return get("settlesuccessSum");
	}

	/**
	 * 设置结算成功金额
	 *
	 * @param settlesuccessSum 结算成功金额
	 */
	public void setSettlesuccessSum(java.math.BigDecimal settlesuccessSum) {
		set("settlesuccessSum", settlesuccessSum);
	}

	/**
	 * 获取结算止付金额
	 *
	 * @return 结算止付金额
	 */
	public java.math.BigDecimal getSettleerrorSum() {
		return get("settleerrorSum");
	}

	/**
	 * 设置结算止付金额
	 *
	 * @param settleerrorSum 结算止付金额
	 */
	public void setSettleerrorSum(java.math.BigDecimal settleerrorSum) {
		set("settleerrorSum", settleerrorSum);
	}

	/**
	 * 获取入账类型
	 *
	 * @return 入账类型
	 */
	public Short getEntrytype() {
		return getShort("entrytype");
	}

	/**
	 * 设置入账类型
	 *
	 * @param entrytype 入账类型
	 */
	public void setEntrytype(Short entrytype) {
		set("entrytype", entrytype);
	}

	/**
	 * 获取是否统收统支
	 *
	 * @return 是否统收统支
	 */
	public Short getIsIncomeAndExpenditure() {
		return getShort("isIncomeAndExpenditure");
	}

	/**
	 * 设置是否统收统支
	 *
	 * @param isIncomeAndExpenditure 是否统收统支
	 */
	public void setIsIncomeAndExpenditure(Short isIncomeAndExpenditure) {
		set("isIncomeAndExpenditure", isIncomeAndExpenditure);
	}

	/**
	 * 获取资金计划项目
	 *
	 * @return 资金计划项目.ID
	 */
	public String getFundPlanProject() {
		return get("fundPlanProject");
	}

	/**
	 * 设置资金计划项目
	 *
	 * @param fundPlanProject 资金计划项目.ID
	 */
	public void setFundPlanProject(String fundPlanProject) {
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
	 * 获取是否关联对账单
	 *
	 * @return 是否关联对账单
	 */
	public Short getIsassociationbankbill() {
		return getShort("isassociationbankbill");
	}

	/**
	 * 设置是否关联对账单
	 *
	 * @param isassociationbankbill 是否关联对账单
	 */
	public void setIsassociationbankbill(Short isassociationbankbill) {
		set("isassociationbankbill", isassociationbankbill);
	}

	/**
	 * 获取关联银行对账单ID
	 *
	 * @return 关联银行对账单ID
	 */
	public String getAssociationbankbillid() {
		return get("associationbankbillid");
	}

	/**
	 * 设置关联银行对账单ID
	 *
	 * @param associationbankbillid 关联银行对账单ID
	 */
	public void setAssociationbankbillid(String associationbankbillid) {
		set("associationbankbillid", associationbankbillid);
	}

	/**
	 * 获取关联认领单ID
	 *
	 * @return 关联认领单ID
	 */
	public String getAssociationbillclaimid() {
		return get("associationbillclaimid");
	}

	/**
	 * 设置关联认领单ID
	 *
	 * @param associationbillclaimid 关联认领单ID
	 */
	public void setAssociationbillclaimid(String associationbillclaimid) {
		set("associationbillclaimid", associationbillclaimid);
	}

	/**
	 * 获取勾兑号
	 *
	 * @return 勾兑号
	 */
	public String getSmartcheckno() {
		return get("smartcheckno");
	}

	/**
	 * 设置勾兑号
	 *
	 * @param smartcheckno 勾兑号
	 */
	public void setSmartcheckno(String smartcheckno) {
		set("smartcheckno", smartcheckno);
	}

	/**
	 * 获取是否退票
	 *
	 * @return 是否退票
	 */
	public Short getIsrefund() {
		return getShort("isrefund");
	}

	/**
	 * 设置是否退票
	 *
	 * @param isrefund 是否退票
	 */
	public void setIsrefund(Short isrefund) {
		set("isrefund", isrefund);
	}

	/**
	 * 获取退票金额
	 *
	 * @return 退票金额
	 */
	public java.math.BigDecimal getRefundSum() {
		return get("refundSum");
	}

	/**
	 * 设置退票金额
	 *
	 * @param refundSum 退票金额
	 */
	public void setRefundSum(java.math.BigDecimal refundSum) {
		set("refundSum", refundSum);
	}

	/**
	 * 获取成本中心
	 *
	 * @return 成本中心.ID
	 */
	public Long getCostcenter() {
		return get("costcenter");
	}

	/**
	 * 设置成本中心
	 *
	 * @param costcenter 成本中心.ID
	 */
	public void setCostcenter(Long costcenter) {
		set("costcenter", costcenter);
	}

	/**
	 * 获取利润中心
	 *
	 * @return 利润中心.ID
	 */
	public String getProfitcenter() {
		return get("profitcenter");
	}

	/**
	 * 设置利润中心
	 *
	 * @param profitcenter 利润中心.ID
	 */
	public void setProfitcenter(String profitcenter) {
		set("profitcenter", profitcenter);
	}

	/**
	 * 获取是否要结算占资金计划
	 *
	 * @return 是否要结算占资金计划
	 */
	public Short getIsSettleToPushCspl() {
		return getShort("isSettleToPushCspl");
	}

	/**
	 * 设置是否要结算占资金计划
	 *
	 * @param isSettleToPushCspl 是否要结算占资金计划
	 */
	public void setIsSettleToPushCspl(Short isSettleToPushCspl) {
		set("isSettleToPushCspl", isSettleToPushCspl);
	}

	/**
	 * 获取是否结算登日记账
	 *
	 * @return 是否结算登日记账
	 */
	public Short getIsSettleToJournal() {
		return getShort("isSettleToJournal");
	}

	/**
	 * 设置是否结算登日记账
	 *
	 * @param isSettleToJournal 是否结算登日记账
	 */
	public void setIsSettleToJournal(Short isSettleToJournal) {
		set("isSettleToJournal", isSettleToJournal);
	}

	/**
	 * 获取是否结算生成凭证
	 *
	 * @return 是否结算生成凭证
	 */
	public Short getIsSettleTovoucher() {
		return getShort("isSettleTovoucher");
	}

	/**
	 * 设置是否结算生成凭证
	 *
	 * @param isSettleTovoucher 是否结算生成凭证
	 */
	public void setIsSettleTovoucher(Short isSettleTovoucher) {
		set("isSettleTovoucher", isSettleTovoucher);
	}

	/**
	 * 获取是否已关联对账单
	 *
	 * @return 是否已关联对账单
	 */
	public Short getIsassociatedbankbill() {
		return getShort("isassociatedbankbill");
	}

	/**
	 * 设置是否已关联对账单
	 *
	 * @param isassociatedbankbill 是否已关联对账单
	 */
	public void setIsassociatedbankbill(Short isassociatedbankbill) {
		set("isassociatedbankbill", isassociatedbankbill);
	}

	/**
	 * 获取是否传结算
	 *
	 * @return 是否传结算
	 */
	public Short getSettleflag() {
		return getShort("settleflag");
	}

	/**
	 * 设置是否传结算
	 *
	 * @param settleflag 是否传结算
	 */
	public void setSettleflag(Short settleflag) {
		set("settleflag", settleflag);
	}

	/**
	 * 获取实际结算主体
	 *
	 * @return 实际结算主体.ID
	 */
	public String getSettleaccentity() {
		return get("settleaccentity");
	}

	/**
	 * 设置实际结算主体
	 *
	 * @param settleaccentity 实际结算主体.ID
	 */
	public void setSettleaccentity(String settleaccentity) {
		set("settleaccentity", settleaccentity);
	}

	/**
	 * 获取实际结算账号
	 *
	 * @return 实际结算账号.ID
	 */
	public String getSettleaccount() {
		return get("settleaccount");
	}

	/**
	 * 设置实际结算账号
	 *
	 * @param settleaccount 实际结算账号.ID
	 */
	public void setSettleaccount(String settleaccount) {
		set("settleaccount", settleaccount);
	}

	/**
	 * 获取付款结算模式
	 *
	 * @return 付款结算模式
	 */
	public Short getPaymentsettlemode() {
		return getShort("paymentsettlemode");
	}

	/**
	 * 设置付款结算模式
	 *
	 * @param paymentsettlemode 付款结算模式
	 */
	public void setPaymentsettlemode(Short paymentsettlemode) {
		set("paymentsettlemode", paymentsettlemode);
	}

	/**
	 * 获取外汇付款特征
	 *
	 * @return 外汇付款特征.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置外汇付款特征
	 *
	 * @param characterDef 外汇付款特征.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
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
	public Short getStatus() {
		return getShort("status");
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

	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

	/**
	 * 获取待结算数据流水号
	 *
	 * @return 待结算数据流水号
	 */
	public String getTransNumber() {
		return get("transNumber");
	}

	/**
	 * 设置待结算数据流水号
	 *
	 * @param transNumber 待结算数据流水号
	 */
	public void setTransNumber(String transNumber) {
		set("transNumber", transNumber);
	}

	/**
	 * 获取结算成功时间
	 *
	 * @return 结算成功时间
	 */
	public java.util.Date getSettlesuccesstime() {
		return get("settlesuccesstime");
	}

	/**
	 * 设置结算成功时间
	 *
	 * @param settlesuccesstime 结算成功时间
	 */
	public void setSettlesuccesstime(java.util.Date settlesuccesstime) {
		set("settlesuccesstime", settlesuccesstime);
	}

	/**
	 * 获取止付类型
	 *
	 * @return 止付类型
	 */
	public String getRejecttype() {
		return get("rejecttype");
	}

	/**
	 * 设置止付类型
	 *
	 * @param rejecttype 止付类型
	 */
	public void setRejecttype(String rejecttype) {
		set("rejecttype", rejecttype);
	}

	/**
	 * 获取支付说明
	 *
	 * @return 支付说明
	 */
	public String getRejectremark() {
		return get("rejectremark");
	}

	/**
	 * 设置支付说明
	 *
	 * @param rejectremark 支付说明
	 */
	public void setRejectremark(String rejectremark) {
		set("rejectremark", rejectremark);
	}

	/**
	 * 获取结算单号
	 *
	 * @return 结算单号
	 */
	public String getRelatedsettlementBillno() {
		return get("relatedsettlementBillno");
	}

	/**
	 * 设置结算单号
	 *
	 * @param relatedsettlementBillno 结算单号
	 */
	public void setRelatedsettlementBillno(String relatedsettlementBillno) {
		set("relatedsettlementBillno", relatedsettlementBillno);
	}

	/**
	 * 获取结算单ID
	 *
	 * @return 结算单ID
	 */
	public Long getSettlementId() {
		return get("settlementId");
	}

	/**
	 * 设置结算单ID
	 *
	 * @param settlementId 结算单ID
	 */
	public void setSettlementId(Long settlementId) {
		set("settlementId", settlementId);
	}

	/**
	 * 获取资金结算明细id
	 *
	 * @return 资金结算明细id
	 */
	public String getSettledId() {
		return get("settledId");
	}

	/**
	 * 设置资金结算明细id
	 *
	 * @param settledId 资金结算明细id
	 */
	public void setSettledId(String settledId) {
		set("settledId", settledId);
	}

	/**
	 * 获取结算成功业务时间
	 *
	 * @return 结算成功业务时间
	 */
	public java.util.Date getSettleSuccBizTime() {
		return get("settleSuccBizTime");
	}

	/**
	 * 设置结算成功业务时间
	 *
	 * @param settleSuccBizTime 结算成功业务时间
	 */
	public void setSettleSuccBizTime(java.util.Date settleSuccBizTime) {
		set("settleSuccBizTime", settleSuccBizTime);
	}

	/**
	 * 获取结算成功系统时间
	 *
	 * @return 结算成功系统时间
	 */
	public java.util.Date getSettleSuccSysTime() {
		return get("settleSuccSysTime");
	}

	/**
	 * 设置结算成功系统时间
	 *
	 * @param settleSuccSysTime 结算成功系统时间
	 */
	public void setSettleSuccSysTime(java.util.Date settleSuccSysTime) {
		set("settleSuccSysTime", settleSuccSysTime);
	}

	/**
	 * 获取结算方式code
	 *
	 * @return 结算方式code
	 */
	public String getExpectsettlemethodCode() {
		return get("expectsettlemethodCode");
	}

	/**
	 * 设置结算方式code
	 *
	 * @param expectsettlemethodCode 结算方式code
	 */
	public void setExpectsettlemethodCode(String expectsettlemethodCode) {
		set("expectsettlemethodCode", expectsettlemethodCode);
	}

	/**
	 * 获取结算方式名字
	 *
	 * @return 结算方式名字
	 */
	public String getExpectsettlemethodName() {
		return get("expectsettlemethodName");
	}

	/**
	 * 设置结算方式名字
	 *
	 * @param expectsettlemethodName 结算方式名字
	 */
	public void setExpectsettlemethodName(String expectsettlemethodName) {
		set("expectsettlemethodName", expectsettlemethodName);
	}

	/**
	 * 获取本方银行账户id
	 *
	 * @return 本方银行账户id
	 */
	public String getSettlemetBankAccountId() {
		return get("settlemetBankAccountId");
	}

	/**
	 * 设置本方银行账户id
	 *
	 * @param settlemetBankAccountId 本方银行账户id
	 */
	public void setSettlemetBankAccountId(String settlemetBankAccountId) {
		set("settlemetBankAccountId", settlemetBankAccountId);
	}

	/**
	 * 获取本方银行账户
	 *
	 * @return 本方银行账户
	 */
	public String getSettlemetBankAccount() {
		return get("settlemetBankAccount");
	}

	/**
	 * 设置本方银行账户
	 *
	 * @param settlemetBankAccount 本方银行账户
	 */
	public void setSettlemetBankAccount(String settlemetBankAccount) {
		set("settlemetBankAccount", settlemetBankAccount);
	}

	/**
	 * 获取是否换汇支付
	 *
	 * @return 是否换汇支付
	 */
	public Short getIsExchangePayment() {
		return getShort("isExchangePayment");
	}

	/**
	 * 设置是否换汇支付
	 *
	 * @param isExchangePayment 是否换汇支付
	 */
	public void setIsExchangePayment(Short isExchangePayment) {
		set("isExchangePayment", isExchangePayment);
	}

	/**
	 * 获取对方银行账号id
	 *
	 * @return 对方银行账号id
	 */
	public String getCounterpartybankaccount() {
		return get("counterpartybankaccount");
	}

	/**
	 * 设置对方银行账号id
	 *
	 * @param counterpartybankaccount 对方银行账号id
	 */
	public void setCounterpartybankaccount(String counterpartybankaccount) {
		set("counterpartybankaccount", counterpartybankaccount);
	}

	/**
	 * 获取对方银行账号
	 *
	 * @return 对方银行账号
	 */
	public String getShowoppositebankaccount() {
		return get("showoppositebankaccount");
	}

	/**
	 * 设置对方银行账号
	 *
	 * @param showoppositebankaccount 对方银行账号
	 */
	public void setShowoppositebankaccount(String showoppositebankaccount) {
		set("showoppositebankaccount", showoppositebankaccount);
	}

	/**
	 * 获取收款方名称id
	 *
	 * @return 收款方名称id
	 */
	public String getReceivenameid() {
		return get("receivenameid");
	}

	/**
	 * 设置收款方名称id
	 *
	 * @param receivenameid 收款方名称id
	 */
	public void setReceivenameid(String receivenameid) {
		set("receivenameid", receivenameid);
	}


	/**
	 * 获取收款方账户id
	 *
	 * @return 收款方账户id
	 */
	public String getReceivebankaccountid() {
		return get("receivebankaccountid");
	}

	/**
	 * 设置收款方账户id
	 *
	 * @param receivebankaccountid 收款方账户id
	 */
	public void setReceivebankaccountid(String receivebankaccountid) {
		set("receivebankaccountid", receivebankaccountid);
	}


	/**
	 * 获取账户币种
	 *
	 * @return 账户币种
	 */
	public String getAccountcurrency() {
		return get("accountcurrency");
	}

	/**
	 * 设置账户币种
	 *
	 * @param accountcurrency 账户币种
	 */
	public void setAccountcurrency(String accountcurrency) {
		set("accountcurrency", accountcurrency);
	}

	/**
	 * 获取收款方开户行地址
	 *
	 * @return 收款方开户行地址
	 */
	public String getReceivebankaddress() {
		return get("receivebankaddress");
	}

	/**
	 * 设置收款方开户行地址
	 *
	 * @param receivebankaddress 收款方开户行地址
	 */
	public void setReceivebankaddress(String receivebankaddress) {
		set("receivebankaddress", receivebankaddress);
	}

	/**
	 * 获取收款方开户行SWIFT
	 *
	 * @return 收款方开户行SWIFT
	 */
	public String getReceivebankswift() {
		return get("receivebankswift");
	}

	/**
	 * 设置收款方开户行SWIFT
	 *
	 * @param receivebankswift 收款方开户行SWIFT
	 */
	public void setReceivebankswift(String receivebankswift) {
		set("receivebankswift", receivebankswift);
	}

	/**
	 * 获取账户币种名称
	 *
	 * @return 账户币种名称
	 */
	public String getAccountcurrency_name() {
		return get("accountcurrency_name");
	}

	/**
	 * 设置账户币种名称
	 *
	 * @param accountcurrency_name 账户币种名称
	 */
	public void setAccountcurrency_name(String accountcurrency_name) {
		set("accountcurrency_name", accountcurrency_name);
	}

	/**
	 * 获取是否存在支付风险
	 *
	 * @return 是否存在支付风险
	 */
	public Short getRiskPayFlag() {
		return getShort("riskPayFlag");
	}

	/**
	 * 设置是否存在支付风险
	 *
	 * @param riskPayFlag 是否存在支付风险
	 */
	public void setRiskPayFlag(Short riskPayFlag) {
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
	 * 获取签名串
	 *
	 * @return 签名串
	 */
	public String getSignature() {
		return get("signature");
	}

	/**
	 * 设置签名串
	 *
	 * @param signature 签名串
	 */
	public void setSignature(String signature) {
		set("signature", signature);
	}


	/**
	 * 获取收款方常驻国家地区代码
	 *
	 * @return 收款方常驻国家地区代码
	 */
	public String getReceivecountrycode() {
		return get("receivecountrycode");
	}

	/**
	 * 设置收款方常驻国家地区代码
	 *
	 * @param receivecountrycode 收款方常驻国家地区代码
	 */
	public void setReceivecountrycode(String receivecountrycode) {
		set("receivecountrycode", receivecountrycode);
	}

	/**
	 * 获取原申报号
	 *
	 * @return 原申报号
	 */
	public String getDeclarationnumber() {
		return get("declarationnumber");
	}

	/**
	 * 设置原申报号
	 *
	 * @param declarationnumber 原申报号
	 */
	public void setDeclarationnumber(String declarationnumber) {
		set("declarationnumber", declarationnumber);
	}


	/**
	 * 获取付款类型
	 *
	 * @return 付款类型
	 */
	public String getFundtype() {
		return get("fundtype");
	}

	/**
	 * 设置付款类型
	 *
	 * @param fundtype 付款类型
	 */
	public void setFundtype(String fundtype) {
		set("fundtype", fundtype);
	}

	/**
	 * 获取付款方名称（非中文）
	 *
	 * @return 付款方名称（非中文）
	 */
	public String getPayernamenocn() {
		return get("payernamenocn");
	}

	/**
	 * 设置付款方名称（非中文）
	 *
	 * @param payernamenocn 付款方名称（非中文）
	 */
	public void setPayernamenocn(String payernamenocn) {
		set("payernamenocn", payernamenocn);
	}

	/**
	 * 获取是否跨行
	 *
	 * @return 是否跨行
	 */
	public Short getBankflag() {
		return getShort("bankflag");
	}

	/**
	 * 设置是否跨行
	 *
	 * @param bankflag 是否跨行
	 */
	public void setBankflag(Short bankflag) {
		set("bankflag", bankflag);
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
}
