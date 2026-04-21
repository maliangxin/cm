package com.yonyoucloud.fi.cmp.marginworkbench;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 保证金工作台实体
 *
 * @author u
 * @version 1.0
 */
public class MarginWorkbench extends BizObject implements IAuditInfo, ICurrency, ITenant, IYTenant, AccentityRawInterface {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.marginworkbench.MarginWorkbench";

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
     * 获取单据类型
     *
     * @return 单据类型.ID
     */
	public String getBillType() {
		return get("billType");
	}

    /**
     * 设置单据类型
     *
     * @param billType 单据类型.ID
     */
	public void setBillType(String billType) {
		set("billType", billType);
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
	public String getCapBizObjBankAccount() {
		return get("capBizObjBankAccount");
	}

    /**
     * 设置资金业务对象银行账户
     *
     * @param capBizObjBankAccount 资金业务对象银行账户.ID
     */
	public void setCapBizObjBankAccount(String capBizObjBankAccount) {
		set("capBizObjBankAccount", capBizObjBankAccount);
	}

    /**
     * 获取保证金虚拟户
     *
     * @return 保证金虚拟户
     */
	public String getCode() {
		return get("code");
	}

    /**
     * 设置保证金虚拟户
     *
     * @param code 保证金虚拟户
     */
	public void setCode(String code) {
		set("code", code);
	}

    /**
     * 获取转换金额
     *
     * @return 转换金额
     */
	public java.math.BigDecimal getConversionAmount() {
		return get("conversionAmount");
	}

    /**
     * 设置转换金额
     *
     * @param conversionAmount 转换金额
     */
	public void setConversionAmount(java.math.BigDecimal conversionAmount) {
		set("conversionAmount", conversionAmount);
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
     * 获取客户银行账户
     *
     * @return 客户银行账户.ID
     */
	public Long getCustomerBankAccount() {
		return get("customerBankAccount");
	}

    /**
     * 设置客户银行账户
     *
     * @param customerBankAccount 客户银行账户.ID
     */
	public void setCustomerBankAccount(Long customerBankAccount) {
		set("customerBankAccount", customerBankAccount);
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
     * 获取本方银行账户
     *
     * @return 本方银行账户.ID
     */
	public String getEnterpriseBankAccount() {
		return get("enterpriseBankAccount");
	}

    /**
     * 设置本方银行账户
     *
     * @param enterpriseBankAccount 本方银行账户.ID
     */
	public void setEnterpriseBankAccount(String enterpriseBankAccount) {
		set("enterpriseBankAccount", enterpriseBankAccount);
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
     * 获取预计取回日期
     *
     * @return 预计取回日期
     */
	public java.util.Date getExpectedDate() {
		return get("expectedDate");
	}

    /**
     * 设置预计取回日期
     *
     * @param expectedDate 预计取回日期
     */
	public void setExpectedDate(java.util.Date expectedDate) {
		set("expectedDate", expectedDate);
	}

    /**
     * 获取首次支付日期
     *
     * @return 首次支付日期
     */
	public java.util.Date getFirstPayDate() {
		return get("firstPayDate");
	}

    /**
     * 设置首次支付日期
     *
     * @param firstPayDate 首次支付日期
     */
	public void setFirstPayDate(java.util.Date firstPayDate) {
		set("firstPayDate", firstPayDate);
	}

    /**
     * 获取首次收到日期
     *
     * @return 首次收到日期
     */
	public java.util.Date getFirstReceivedDate() {
		return get("firstReceivedDate");
	}

    /**
     * 设置首次收到日期
     *
     * @param firstReceivedDate 首次收到日期
     */
	public void setFirstReceivedDate(java.util.Date firstReceivedDate) {
		set("firstReceivedDate", firstReceivedDate);
	}

    /**
     * 获取最迟退还日期
     *
     * @return 最迟退还日期
     */
	public java.util.Date getLatestReturnDate() {
		return get("latestReturnDate");
	}

    /**
     * 设置最迟退还日期
     *
     * @param latestReturnDate 最迟退还日期
     */
	public void setLatestReturnDate(java.util.Date latestReturnDate) {
		set("latestReturnDate", latestReturnDate);
	}

    /**
     * 获取保证金可用余额
     *
     * @return 保证金可用余额
     */
	public java.math.BigDecimal getMarginAvailableBalance() {
		return get("marginAvailableBalance");
	}

    /**
     * 设置保证金可用余额
     *
     * @param marginAvailableBalance 保证金可用余额
     */
	public void setMarginAvailableBalance(java.math.BigDecimal marginAvailableBalance) {
		set("marginAvailableBalance", marginAvailableBalance);
	}

    /**
     * 获取保证金余额
     *
     * @return 保证金余额
     */
	public java.math.BigDecimal getMarginBalance() {
		return get("marginBalance");
	}

    /**
     * 设置保证金余额
     *
     * @param marginBalance 保证金余额
     */
	public void setMarginBalance(java.math.BigDecimal marginBalance) {
		set("marginBalance", marginBalance);
	}

    /**
     * 获取保证金原始业务号
     *
     * @return 保证金原始业务号
     */
	public String getMarginBusinessNo() {
		return get("marginBusinessNo");
	}

    /**
     * 设置保证金原始业务号
     *
     * @param marginBusinessNo 保证金原始业务号
     */
	public void setMarginBusinessNo(String marginBusinessNo) {
		set("marginBusinessNo", marginBusinessNo);
	}

    /**
     * 获取保证金标识
     *
     * @return 保证金标识
     */
	public Short getMarginFlag() {
	    return getShort("marginFlag");
	}

    /**
     * 设置保证金标识
     *
     * @param marginFlag 保证金标识
     */
	public void setMarginFlag(Short marginFlag) {
		set("marginFlag", marginFlag);
	}

    /**
     * 获取保证金类型
     *
     * @return 保证金类型.ID
     */
	public Long getMarginType() {
		return get("marginType");
	}

    /**
     * 设置保证金类型
     *
     * @param marginType 保证金类型.ID
     */
	public void setMarginType(Long marginType) {
		set("marginType", marginType);
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
     * 获取本币转换金额
     *
     * @return 本币转换金额
     */
	public java.math.BigDecimal getNatConversionAmount() {
		return get("natConversionAmount");
	}

    /**
     * 设置本币转换金额
     *
     * @param natConversionAmount 本币转换金额
     */
	public void setNatConversionAmount(java.math.BigDecimal natConversionAmount) {
		set("natConversionAmount", natConversionAmount);
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
     * 获取本币保证金可用余额
     *
     * @return 本币保证金可用余额
     */
	public java.math.BigDecimal getNatMarginAvailableBalance() {
		return get("natMarginAvailableBalance");
	}

    /**
     * 设置本币保证金可用余额
     *
     * @param natMarginAvailableBalance 本币保证金可用余额
     */
	public void setNatMarginAvailableBalance(java.math.BigDecimal natMarginAvailableBalance) {
		set("natMarginAvailableBalance", natMarginAvailableBalance);
	}

    /**
     * 获取本币保证金余额
     *
     * @return 本币保证金余额
     */
	public java.math.BigDecimal getNatMarginBalance() {
		return get("natMarginBalance");
	}

    /**
     * 设置本币保证金余额
     *
     * @param natMarginBalance 本币保证金余额
     */
	public void setNatMarginBalance(java.math.BigDecimal natMarginBalance) {
		set("natMarginBalance", natMarginBalance);
	}

    /**
     * 获取本币支付金额
     *
     * @return 本币支付金额
     */
	public java.math.BigDecimal getNatPayAmount() {
		return get("natPayAmount");
	}

    /**
     * 设置本币支付金额
     *
     * @param natPayAmount 本币支付金额
     */
	public void setNatPayAmount(java.math.BigDecimal natPayAmount) {
		set("natPayAmount", natPayAmount);
	}

    /**
     * 获取本币收到金额
     *
     * @return 本币收到金额
     */
	public java.math.BigDecimal getNatReceivedAmount() {
		return get("natReceivedAmount");
	}

    /**
     * 设置本币收到金额
     *
     * @param natReceivedAmount 本币收到金额
     */
	public void setNatReceivedAmount(java.math.BigDecimal natReceivedAmount) {
		set("natReceivedAmount", natReceivedAmount);
	}

    /**
     * 获取本币取回金额
     *
     * @return 本币取回金额
     */
	public java.math.BigDecimal getNatRetrieveAmount() {
		return get("natRetrieveAmount");
	}

    /**
     * 设置本币取回金额
     *
     * @param natRetrieveAmount 本币取回金额
     */
	public void setNatRetrieveAmount(java.math.BigDecimal natRetrieveAmount) {
		set("natRetrieveAmount", natRetrieveAmount);
	}

    /**
     * 获取本币退还金额
     *
     * @return 本币退还金额
     */
	public java.math.BigDecimal getNatReturnAmount() {
		return get("natReturnAmount");
	}

    /**
     * 设置本币退还金额
     *
     * @param natReturnAmount 本币退还金额
     */
	public void setNatReturnAmount(java.math.BigDecimal natReturnAmount) {
		set("natReturnAmount", natReturnAmount);
	}

    /**
     * 获取对方银行账号
     *
     * @return 对方银行账号
     */
	public String getOppositeBankAccount() {
		return get("oppositeBankAccount");
	}

    /**
     * 设置对方银行账号
     *
     * @param oppositeBankAccount 对方银行账号
     */
	public void setOppositeBankAccount(String oppositeBankAccount) {
		set("oppositeBankAccount", oppositeBankAccount);
	}

    /**
     * 获取对方银行账户名称
     *
     * @return 对方银行账户名称
     */
	public String getOppositeBankAccountName() {
		return get("oppositeBankAccountName");
	}

    /**
     * 设置对方银行账户名称
     *
     * @param oppositeBankAccountName 对方银行账户名称
     */
	public void setOppositeBankAccountName(String oppositeBankAccountName) {
		set("oppositeBankAccountName", oppositeBankAccountName);
	}

    /**
     * 获取对方开户网点
     *
     * @return 对方开户网点.ID
     */
	public String getOppositeBankNumber() {
		return get("oppositeBankNumber");
	}

    /**
     * 设置对方开户网点
     *
     * @param oppositeBankNumber 对方开户网点.ID
     */
	public void setOppositeBankNumber(String oppositeBankNumber) {
		set("oppositeBankNumber", oppositeBankNumber);
	}

    /**
     * 获取对方银行类别
     *
     * @return 对方银行类别.ID
     */
	public String getOppositeBankType() {
		return get("oppositeBankType");
	}

    /**
     * 设置对方银行类别
     *
     * @param oppositeBankType 对方银行类别.ID
     */
	public void setOppositeBankType(String oppositeBankType) {
		set("oppositeBankType", oppositeBankType);
	}

    /**
     * 获取对方名称
     *
     * @return 对方名称
     */
	public String getOppositeName() {
		return get("oppositeName");
	}

    /**
     * 设置对方名称
     *
     * @param oppositeName 对方名称
     */
	public void setOppositeName(String oppositeName) {
		set("oppositeName", oppositeName);
	}

    /**
     * 获取对方类型
     *
     * @return 对方类型
     */
	public Short getOppositeType() {
	    return getShort("oppositeType");
	}

    /**
     * 设置对方类型
     *
     * @param oppositeType 对方类型
     */
	public void setOppositeType(Short oppositeType) {
		set("oppositeType", oppositeType);
	}

    /**
     * 获取内部单位银行账户
     *
     * @return 内部单位银行账户.ID
     */
	public String getOurBankAccount() {
		return get("ourBankAccount");
	}

    /**
     * 设置内部单位银行账户
     *
     * @param ourBankAccount 内部单位银行账户.ID
     */
	public void setOurBankAccount(String ourBankAccount) {
		set("ourBankAccount", ourBankAccount);
	}

	/**
	 * 获取内部单位名称
	 *
	 * @return 内部单位名称.ID
	 */
	public String getOurName() {
		return get("ourName");
	}

	/**
	 * 设置内部单位名称
	 *
	 * @param ourName 内部单位名称.ID
	 */
	public void setOurName(String ourName) {
		set("ourName", ourName);
	}

    /**
     * 获取支付金额
     *
     * @return 支付金额
     */
	public java.math.BigDecimal getPayAmount() {
		return get("payAmount");
	}

    /**
     * 设置支付金额
     *
     * @param payAmount 支付金额
     */
	public void setPayAmount(java.math.BigDecimal payAmount) {
		set("payAmount", payAmount);
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
     * 获取收到金额
     *
     * @return 收到金额
     */
	public java.math.BigDecimal getReceivedAmount() {
		return get("receivedAmount");
	}

    /**
     * 设置收到金额
     *
     * @param receivedAmount 收到金额
     */
	public void setReceivedAmount(java.math.BigDecimal receivedAmount) {
		set("receivedAmount", receivedAmount);
	}

    /**
     * 获取取回金额
     *
     * @return 取回金额
     */
	public java.math.BigDecimal getRetrieveAmount() {
		return get("retrieveAmount");
	}

    /**
     * 设置取回金额
     *
     * @param retrieveAmount 取回金额
     */
	public void setRetrieveAmount(java.math.BigDecimal retrieveAmount) {
		set("retrieveAmount", retrieveAmount);
	}

    /**
     * 获取退还金额
     *
     * @return 退还金额
     */
	public java.math.BigDecimal getReturnAmount() {
		return get("returnAmount");
	}

    /**
     * 设置退还金额
     *
     * @param returnAmount 退还金额
     */
	public void setReturnAmount(java.math.BigDecimal returnAmount) {
		set("returnAmount", returnAmount);
	}

    /**
     * 获取事项来源
     *
     * @return 事项来源
     */
	public Short getSrcItem() {
	    return getShort("srcItem");
	}

    /**
     * 设置事项来源
     *
     * @param srcItem 事项来源
     */
	public void setSrcItem(Short srcItem) {
		set("srcItem", srcItem);
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
	public Long getSupplierBankAccount() {
		return get("supplierBankAccount");
	}

    /**
     * 设置供应商银行账户
     *
     * @param supplierBankAccount 供应商银行账户.ID
     */
	public void setSupplierBankAccount(Long supplierBankAccount) {
		set("supplierBankAccount", supplierBankAccount);
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

	@Override
	public String getYTenant() {
		return null;
	}

	@Override
	public void setYTenant(String s) {

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
}
