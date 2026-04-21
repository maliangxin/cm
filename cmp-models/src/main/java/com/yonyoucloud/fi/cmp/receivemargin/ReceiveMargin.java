package com.yonyoucloud.fi.cmp.receivemargin;


import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.*;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 收到保证金台账管理实体
 *
 * @author u
 * @version 1.0
 */
public class ReceiveMargin extends BizObject implements IAuditInfo, IApprovalInfo, ICurrency, IApprovalFlow, IPrintCount, ITenant, IYTenant, AccentityRawInterface {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.receivemargin.ReceiveMargin";
	// 业务对象编码
	public static final String BUSI_OBJ_CODE = "ctm-cmp.cmp_receivemargin";
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
     * 获取是否期初
     *
     * @return 是否期初
     */
	public Short getInitflag() {
	    return getShort("initflag");
	}

    /**
     * 设置是否期初
     *
     * @param initflag 是否期初
     */
	public void setInitflag(Short initflag) {
		set("initflag", initflag);
	}

    /**
     * 获取保证金虚拟户
     *
     * @return 保证金虚拟户.ID
     */
	public Long getMarginvirtualaccount() {
		return get("marginvirtualaccount");
	}

    /**
     * 设置保证金虚拟户
     *
     * @param marginvirtualaccount 保证金虚拟户.ID
     */
	public void setMarginvirtualaccount(Long marginvirtualaccount) {
		set("marginvirtualaccount", marginvirtualaccount);
	}

    /**
     * 获取保证金原始业务号
     *
     * @return 保证金原始业务号
     */
	public String getMarginbusinessno() {
		return get("marginbusinessno");
	}

    /**
     * 设置保证金原始业务号
     *
     * @param marginbusinessno 保证金原始业务号
     */
	public void setMarginbusinessno(String marginbusinessno) {
		set("marginbusinessno", marginbusinessno);
	}

    /**
     * 获取保证金类型
     *
     * @return 保证金类型.ID
     */
	public Long getMargintype() {
		return get("margintype");
	}

    /**
     * 设置保证金类型
     *
     * @param margintype 保证金类型.ID
     */
	public void setMargintype(Long margintype) {
		set("margintype", margintype);
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
     * 获取保证金余额
     *
     * @return 保证金余额
     */
	public java.math.BigDecimal getMarginbalance() {
		return get("marginbalance");
	}

    /**
     * 设置保证金余额
     *
     * @param marginbalance 保证金余额
     */
	public void setMarginbalance(java.math.BigDecimal marginbalance) {
		set("marginbalance", marginbalance);
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
     * 获取最迟退还日期
     *
     * @return 最迟退还日期
     */
	public java.util.Date getLatestreturndate() {
		return get("latestreturndate");
	}

    /**
     * 设置最迟退还日期
     *
     * @param latestreturndate 最迟退还日期
     */
	public void setLatestreturndate(java.util.Date latestreturndate) {
		set("latestreturndate", latestreturndate);
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
	public String getExchangeratetype() {
		return get("exchangeratetype");
	}

    /**
     * 设置汇率类型
     *
     * @param exchangeratetype 汇率类型.ID
     */
	public void setExchangeratetype(String exchangeratetype) {
		set("exchangeratetype", exchangeratetype);
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
     * 获取事项来源
     *
     * @return 汇率折算方式
     */
    public Short getExchRateOps() {
        return getShort("exchRateOps");
    }

    /**
     * 设置事项来源
     *
     * @param exchRateOps 汇率折算方式
     */
    public void setExchRateOps(Short exchRateOps) {
        set("exchRateOps", exchRateOps);
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
     * 获取来源业务单据id
     *
     * @return 来源业务单据id
     */
	public String getSrcbillid() {
		return get("srcbillid");
	}

    /**
     * 设置来源业务单据id
     *
     * @param srcbillid 来源业务单据id
     */
	public void setSrcbillid(String srcbillid) {
		set("srcbillid", srcbillid);
	}

    /**
     * 获取来源业务单据编号
     *
     * @return 来源业务单据编号
     */
	public String getSrcbillno() {
		return get("srcbillno");
	}

    /**
     * 设置来源业务单据编号
     *
     * @param srcbillno 来源业务单据编号
     */
	public void setSrcbillno(String srcbillno) {
		set("srcbillno", srcbillno);
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
     * 获取凭证状态说明
     *
     * @return 凭证状态说明
     */
	public String getVoucherstatusdescription() {
		return get("voucherstatusdescription");
	}

    /**
     * 设置凭证状态说明
     *
     * @param voucherstatusdescription 凭证状态说明
     */
	public void setVoucherstatusdescription(String voucherstatusdescription) {
		set("voucherstatusdescription", voucherstatusdescription);
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
     * 获取对方类型
     *
     * @return 对方类型
     */
	public Short getOppositetype() {
	    return getShort("oppositetype");
	}

    /**
     * 设置对方类型
     *
     * @param oppositetype 对方类型
     */
	public void setOppositetype(Short oppositetype) {
		set("oppositetype", oppositetype);
	}

    /**
     * 获取保证金金额
     *
     * @return 保证金金额
     */
	public java.math.BigDecimal getMarginamount() {
		return get("marginamount");
	}

    /**
     * 设置保证金金额
     *
     * @param marginamount 保证金金额
     */
	public void setMarginamount(java.math.BigDecimal marginamount) {
		set("marginamount", marginamount);
	}

    /**
     * 获取本币保证金金额
     *
     * @return 本币保证金金额
     */
	public java.math.BigDecimal getNatmarginamount() {
		return get("natmarginamount");
	}

    /**
     * 设置本币保证金金额
     *
     * @param natmarginamount 本币保证金金额
     */
	public void setNatmarginamount(java.math.BigDecimal natmarginamount) {
		set("natmarginamount", natmarginamount);
	}

    /**
     * 获取是否结算
     *
     * @return 是否结算
     */
	public Short getSettleflag() {
	    return getShort("settleflag");
	}

    /**
     * 设置是否结算
     *
     * @param settleflag 是否结算
     */
	public void setSettleflag(Short settleflag) {
		set("settleflag", settleflag);
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
     * 获取收付类型
     *
     * @return 收付类型
     */
	public Short getPaymenttype() {
	    return getShort("paymenttype");
	}

    /**
     * 设置收付类型
     *
     * @param paymenttype 收付类型
     */
	public void setPaymenttype(Short paymenttype) {
		set("paymenttype", paymenttype);
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
     * 获取本方银行账户
     *
     * @return 本方银行账户.ID
     */
	public String getEnterprisebankaccount() {
		return get("enterprisebankaccount");
	}

    /**
     * 设置本方银行账户
     *
     * @param enterprisebankaccount 本方银行账户.ID
     */
	public void setEnterprisebankaccount(String enterprisebankaccount) {
		set("enterprisebankaccount", enterprisebankaccount);
	}

	/**
	 * 获取对方银行类别
	 *
	 * @return 对方银行类别
	 */
	public String getOppositebankType() {
		return get("oppositebankType");
	}

	/**
	 * 设置对方银行类别
	 *
	 * @param oppositebankType 对方银行类别
	 */
	public void setOppositebankType(String oppositebankType) {
		set("oppositebankType", oppositebankType);
	}
    /**
     * 获取对方开户网点
     *
     * @return 对方开户网点.ID
     */
	public String getOppositebankNumber() {
		return get("oppositebankNumber");
	}

    /**
     * 设置对方开户网点
     *
     * @param oppositebankNumber 对方开户网点.ID
     */
	public void setOppositebankNumber(String oppositebankNumber) {
		set("oppositebankNumber", oppositebankNumber);
	}

    /**
     * 获取对方名称
     *
     * @return 对方名称
     */
	public String getOppositename() {
		return get("oppositename");
	}

    /**
     * 设置对方名称
     *
     * @param oppositename 对方名称
     */
	public void setOppositename(String oppositename) {
		set("oppositename", oppositename);
	}

    /**
     * 获取对方银行账户名称
     *
     * @return 对方银行账户名称
     */
	public String getOppositebankaccountname() {
		return get("oppositebankaccountname");
	}

    /**
     * 设置对方银行账户名称
     *
     * @param oppositebankaccountname 对方银行账户名称
     */
	public void setOppositebankaccountname(String oppositebankaccountname) {
		set("oppositebankaccountname", oppositebankaccountname);
	}

    /**
     * 获取对方银行账号
     *
     * @return 对方银行账号
     */
	public String getOppositebankaccount() {
		return get("oppositebankaccount");
	}

    /**
     * 设置对方银行账号
     *
     * @param oppositebankaccount 对方银行账号
     */
	public void setOppositebankaccount(String oppositebankaccount) {
		set("oppositebankaccount", oppositebankaccount);
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
     * 获取是否自动退还
     *
     * @return 是否自动退还
     */
	public Short getAutorefundflag() {
	    return getShort("autorefundflag");
	}

    /**
     * 设置是否自动退还
     *
     * @param autorefundflag 是否自动退还
     */
	public void setAutorefundflag(Short autorefundflag) {
		set("autorefundflag", autorefundflag);
	}

    /**
     * 获取退还是否结算
     *
     * @return 退还是否结算
     */
	public Short getRefundsettleflag() {
	    return getShort("refundsettleflag");
	}

    /**
     * 设置退还是否结算
     *
     * @param refundsettleflag 退还是否结算
     */
	public void setRefundsettleflag(Short refundsettleflag) {
		set("refundsettleflag", refundsettleflag);
	}

    /**
     * 获取退还日期
     *
     * @return 退还日期
     */
	public java.util.Date getRefunddate() {
		return get("refunddate");
	}

    /**
     * 设置退还日期
     *
     * @param refunddate 退还日期
     */
	public void setRefunddate(java.util.Date refunddate) {
		set("refunddate", refunddate);
	}

    /**
     * 获取转换保证金
     *
     * @return 转换保证金
     */
	public Short getConversionmarginflag() {
	    return getShort("conversionmarginflag");
	}

    /**
     * 设置转换保证金
     *
     * @param conversionmarginflag 转换保证金
     */
	public void setConversionmarginflag(Short conversionmarginflag) {
		set("conversionmarginflag", conversionmarginflag);
	}

    /**
     * 获取转换金额
     *
     * @return 转换金额
     */
	public java.math.BigDecimal getConversionamount() {
		return get("conversionamount");
	}

    /**
     * 设置转换金额
     *
     * @param conversionamount 转换金额
     */
	public void setConversionamount(java.math.BigDecimal conversionamount) {
		set("conversionamount", conversionamount);
	}

    /**
     * 获取本币转换金额
     *
     * @return 本币转换金额
     */
	public java.math.BigDecimal getNatconversionamount() {
		return get("natconversionamount");
	}

    /**
     * 设置本币转换金额
     *
     * @param natconversionamount 本币转换金额
     */
	public void setNatconversionamount(java.math.BigDecimal natconversionamount) {
		set("natconversionamount", natconversionamount);
	}

    /**
     * 获取新保证金原始业务号
     *
     * @return 新保证金原始业务号
     */
	public String getNewmarginbusinessno() {
		return get("newmarginbusinessno");
	}

    /**
     * 设置新保证金原始业务号
     *
     * @param newmarginbusinessno 新保证金原始业务号
     */
	public void setNewmarginbusinessno(String newmarginbusinessno) {
		set("newmarginbusinessno", newmarginbusinessno);
	}

    /**
     * 获取新保证金类型
     *
     * @return 新保证金类型.ID
     */
	public Long getNewmargintype() {
		return get("newmargintype");
	}

    /**
     * 设置新保证金类型
     *
     * @param newmargintype 新保证金类型.ID
     */
	public void setNewmargintype(Long newmargintype) {
		set("newmargintype", newmargintype);
	}

    /**
     * 获取新部门
     *
     * @return 新部门.ID
     */
	public String getNewdept() {
		return get("newdept");
	}

    /**
     * 设置新部门
     *
     * @param newdept 新部门.ID
     */
	public void setNewdept(String newdept) {
		set("newdept", newdept);
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
     * 获取收到保证金台账管理特征
     *
     * @return 收到保证金台账管理特征.ID
     */
	public String getCharacterDef() {
		return get("characterDef");
	}

    /**
     * 设置收到保证金台账管理特征
     *
     * @param characterDef 收到保证金台账管理特征.ID
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
	 * 获取新项目
	 *
	 * @return 新项目.ID
	 */
	public String getNewproject() {
		return get("newproject");
	}

	/**
	 * 设置新项目
	 *
	 * @param newproject 新项目.ID
	 */
	public void setNewproject(String newproject) {
		set("newproject", newproject);
	}

	/**
	 * 获取退还保证金单据id
	 *
	 * @return 退还保证金单据id
	 */
	public String getRefundmarginid() {
		return get("refundmarginid");
	}

	/**
	 * 设置退还保证金单据id
	 *
	 * @param refundmarginid 退还保证金单据id
	 */
	public void setRefundmarginid(String refundmarginid) {
		set("refundmarginid", refundmarginid);
	}

	/**
	 * 获取退还保证金单据编号
	 *
	 * @return 退还保证金单据编号
	 */
	public String getRefundmargincode() {
		return get("refundmargincode");
	}

	/**
	 * 设置退还保证金单据编号
	 *
	 * @param refundmargincode 退还保证金单据编号
	 */
	public void setRefundmargincode(String refundmargincode) {
		set("refundmargincode", refundmargincode);
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
	 * 获取关联状态
	 *
	 * @return 关联状态
	 */
	public Short getAssociationstatus() {
		return getShort("associationstatus");
	}

	/**
	 * 设置关联状态
	 *
	 * @param associationstatus 关联状态
	 */
	public void setAssociationstatus(Short associationstatus) {
		set("associationstatus", associationstatus);
	}

	/**
	 * 获取银行对账单id
	 *
	 * @return 银行对账单id
	 */
	public String getBankbillid() {
		return get("bankbillid");
	}

	/**
	 * 设置银行对账单id
	 *
	 * @param bankbillid 银行对账单id
	 */
	public void setBankbillid(String bankbillid) {
		set("bankbillid", bankbillid);
	}

	/**
	 * 获取认领单id
	 *
	 * @return 认领单id
	 */
	public String getBillclaimid() {
		return get("billclaimid");
	}

	/**
	 * 设置认领单id
	 *
	 * @param billclaimid 认领单id
	 */
	public void setBillclaimid(String billclaimid) {
		set("billclaimid", billclaimid);
	}

	/**
	 * 获取勾兑号
	 *
	 * @return 勾兑号
	 */
	public String getCheckno() {
		return get("checkno");
	}

	/**
	 * 设置勾兑号
	 *
	 * @param checkno 勾兑号
	 */
	public void setCheckno(String checkno) {
		set("checkno", checkno);
	}

	/**
	 * 获取转换保证金单据id
	 *
	 * @return 转换保证金单据id
	 */
	public String getConversionmarginid() {
		return get("conversionmarginid");
	}

	/**
	 * 设置转换保证金单据id
	 *
	 * @param conversionmarginid 转换保证金单据id
	 */
	public void setConversionmarginid(String conversionmarginid) {
		set("conversionmarginid", conversionmarginid);
	}

	/**
	 * 获取转换保证金单据编号
	 *
	 * @return 转换保证金单据编号
	 */
	public String getConversionmargincode() {
		return get("conversionmargincode");
	}

	/**
	 * 设置转换保证金单据编号
	 *
	 * @param conversionmargincode 转换保证金单据编号
	 */
	public void setConversionmargincode(String conversionmargincode) {
		set("conversionmargincode", conversionmargincode);
	}

	/**
	 * 获取新最迟退还日期
	 *
	 * @return 新最迟退还日期
	 */
	public java.util.Date getNewlatestreturndate() {
		return get("newlatestreturndate");
	}

	/**
	 * 设置新最迟退还日期
	 *
	 * @param newlatestreturndate 新最迟退还日期
	 */
	public void setNewlatestreturndate(java.util.Date newlatestreturndate) {
		set("newlatestreturndate", newlatestreturndate);
	}

	/**
	 * 获取交易类型标识
	 *
	 * @return 交易类型标识
	 */
	public Short getTradetypeflag() {
		return getShort("tradetypeflag");
	}

	/**
	 * 设置交易类型标识
	 *
	 * @param tradetypeflag 交易类型标识
	 */
	public void setTradetypeflag(Short tradetypeflag) {
		set("tradetypeflag", tradetypeflag);
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
	/**
	 * 获取本方关联状态
	 *
	 * @return 本方关联状态
	 */
	public Short getOurassociationstatus() {
		return getShort("ourassociationstatus");
	}

	/**
	 * 设置本方关联状态
	 *
	 * @param ourassociationstatus 本方关联状态
	 */
	public void setOurassociationstatus(Short ourassociationstatus) {
		set("ourassociationstatus", ourassociationstatus);
	}

	/**
	 * 获取本方财资统一对账码
	 *
	 * @return 本方财资统一对账码
	 */
	public String getOurcheckno() {
		return get("ourcheckno");
	}

	/**
	 * 设置本方财资统一对账码
	 *
	 * @param ourcheckno 本方财资统一对账码
	 */
	public void setOurcheckno(String ourcheckno) {
		set("ourcheckno", ourcheckno);
	}

	/**
	 * 获取本方流水ID
	 *
	 * @return 本方流水ID
	 */
	public String getOurbankbillid() {
		return get("ourbankbillid");
	}

	/**
	 * 设置本方流水ID
	 *
	 * @param ourbankbillid 本方流水ID
	 */
	public void setOurbankbillid(String ourbankbillid) {
		set("ourbankbillid", ourbankbillid);
	}

	/**
	 * 获取本方认领单ID
	 *
	 * @return 本方认领单ID
	 */
	public String getOurbillclaimid() {
		return get("ourbillclaimid");
	}

	/**
	 * 设置本方认领单ID
	 *
	 * @param ourbillclaimid 本方认领单ID
	 */
	public void setOurbillclaimid(String ourbillclaimid) {
		set("ourbillclaimid", ourbillclaimid);
	}

}
