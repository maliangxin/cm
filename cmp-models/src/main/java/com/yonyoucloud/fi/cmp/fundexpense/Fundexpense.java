package com.yonyoucloud.fi.cmp.fundexpense;

import com.yonyou.ucf.mdd.ext.base.itf.*;
import com.yonyou.ucf.mdd.ext.voucher.base.Vouch;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import org.springframework.util.ObjectUtils;

/**
 * 资金费用主表实体
 *
 * @author u
 * @version 1.0
 */
public class Fundexpense extends Vouch implements IAuditInfo, ITenant, ICurrency, IApprovalFlow, IPrintCount, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.fundexpense.Fundexpense";

	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
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
	 * 获取活动
	 *
	 * @return 活动.ID
	 */
	public Long getActivity() {
		return get("activity");
	}

	/**
	 * 设置活动
	 *
	 * @param activity 活动.ID
	 */
	public void setActivity(Long activity) {
		set("activity", activity);
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
	 * 获取客户银行账号
	 *
	 * @return 客户银行账号.ID
	 */
	public Long getDfcustomerbankaccount() {
		return get("dfcustomerbankaccount");
	}

	/**
	 * 设置客户银行账号
	 *
	 * @param dfcustomerbankaccount 客户银行账号.ID
	 */
	public void setDfcustomerbankaccount(Long dfcustomerbankaccount) {
		set("dfcustomerbankaccount", dfcustomerbankaccount);
	}

	/**
	 * 获取客户银行账号
	 *
	 * @return 客户银行账号
	 */
	public String getDfcustomerbankaccount_account() {
		return get("dfcustomerbankaccount_account");
	}

	/**
	 * 设置客户银行账号
	 *
	 * @param dfcustomerbankaccount_account 客户银行账号
	 */
	public void setDfcustomerbankaccount_account(String dfcustomerbankaccount_account) {
		set("dfcustomerbankaccount_account", dfcustomerbankaccount_account);
	}
	/**
	 * 获取资金伙伴银行账号
	 *
	 * @return 资金伙伴银行账号.ID
	 */
	public String getDffunbusobjbankaccount() {
		return get("dffunbusobjbankaccount");
	}

	/**
	 * 设置资金伙伴银行账号
	 *
	 * @param dffunbusobjbankaccount 资金伙伴银行账号.ID
	 */
	public void setDffunbusobjbankaccount(String dffunbusobjbankaccount) {
		set("dffunbusobjbankaccount", dffunbusobjbankaccount);
	}
	/**
	 * 获取资金伙伴银行账号
	 *
	 * @return 资金伙伴银行账号
	 */
	public String getDffunbusobjbankaccount_account() {
		return get("dffunbusobjbankaccount_account");
	}

	/**
	 * 设置资金伙伴银行账号
	 *
	 * @param dffunbusobjbankaccount_account 资金伙伴银行账号
	 */
	public void setDffunbusobjbankaccount_account(String dffunbusobjbankaccount_account) {
		set("dffunbusobjbankaccount_account", dffunbusobjbankaccount_account);
	}

	/**
	 * 获取供应商银行账号
	 *
	 * @return 供应商银行账号.ID
	 */
	public Long getDfsupplierbankaccount() {
		return get("dfsupplierbankaccount");
	}

	/**
	 * 设置供应商银行账号
	 *
	 * @param dfsupplierbankaccount 供应商银行账号.ID
	 */
	public void setDfsupplierbankaccount(Long dfsupplierbankaccount) {
		set("dfsupplierbankaccount", dfsupplierbankaccount);
	}
	/**
	 * 获取供应商银行账号
	 *
	 * @return 供应商银行账号
	 */
	public String getDfsupplierbankaccount_account() {
		return get("dfsupplierbankaccount_account");
	}

	/**
	 * 设置供应商银行账号
	 *
	 * @param dfsupplierbankaccount_account 供应商银行账号
	 */
	public void setDfsupplierbankaccount_account(String dfsupplierbankaccount_account) {
		set("dfsupplierbankaccount_account", dfsupplierbankaccount_account);
	}


	/**
	 * 获取客户
	 *
	 * @return 客户.ID
	 */
	public String getDfenterprise_customer() {
		return get("dfenterprise_customer")==null?null:get("dfenterprise_customer")+"";
	}

	/**
	 * 设置客户
	 *
	 * @param dfenterprise_customer 客户.ID
	 */
	public void setDfenterprise_customer(String dfenterprise_customer) {
		set("dfenterprise_customer", dfenterprise_customer);
	}

	/**
	 * 获取客户
	 *
	 * @return 客户.
	 */
	public String getDfenterprise_customer_name() {
		return get("dfenterprise_customer_name");
	}

	/**
	 * 设置客户
	 *
	 * @param dfenterprise_customer_name 客户
	 */
	public void setDfenterprise_customer_name(String dfenterprise_customer_name) {
		set("dfenterprise_customer_name", dfenterprise_customer_name);
	}
	/**
	 * 获取资金伙伴
	 *
	 * @return 资金伙伴.ID
	 */
	public String getDfenterprise_funbusobj() {
		return get("dfenterprise_funbusobj");
	}

	/**
	 * 设置资金伙伴
	 *
	 * @param dfenterprise_funbusobj 资金伙伴.ID
	 */
	public void setDfenterprise_funbusobj(String dfenterprise_funbusobj) {
		set("dfenterprise_funbusobj", dfenterprise_funbusobj);
	}

	/**
	 * 获取资金伙伴
	 *
	 * @return 资金伙伴
	 */
	public String getDfenterprise_funbusobj_name() {
		return get("dfenterprise_funbusobj_name");
	}

	/**
	 * 设置资金伙伴
	 *
	 * @param dfenterprise_funbusobj_name 资金伙伴
	 */
	public void setDfenterprise_funbusobj_name(String dfenterprise_funbusobj_name) {
		set("dfenterprise_funbusobj_name", dfenterprise_funbusobj_name);
	}

	/**
	 * 获取供应商
	 *
	 * @return 供应商.ID
	 */
	public Long getDfenterprise_supplier() {
		return get("dfenterprise_supplier");
	}

	/**
	 * 设置供应商
	 *
	 * @param dfenterprise_supplier 供应商.ID
	 */
	public void setDfenterprise_supplier(Long dfenterprise_supplier) {
		set("dfenterprise_supplier", dfenterprise_supplier);
	}
	/**
	 * 获取供应商
	 *
	 * @return 供应商
	 */
	public Long getDfenterprise_supplier_name() {
		return get("dfenterprise_supplier_name");
	}

	/**
	 * 设置供应商
	 *
	 * @param dfenterprise_supplier_name 供应商
	 */
	public void setDfenterprise_supplier_name(String dfenterprise_supplier_name) {
		set("dfenterprise_supplier_name", dfenterprise_supplier_name);
	}


	/**
	 * 获取对方账户开户行id
	 *
	 * @return 对方账户开户行id
	 */
	public String getDfbankcountopenbank() {
		return get("dfbankcountopenbank");
	}

	/**
	 * 设置对方账户开户行id
	 *
	 * @param dfbankcountopenbank 对方账户开户行id
	 */
	public void setDfbankcountopenbank(String dfbankcountopenbank) {
		set("dfbankcountopenbank", dfbankcountopenbank);
	}

	/**
	 * 获取对方账户开户行
	 *
	 * @return 对方账户开户行
	 */
	public String getDfbankcountopenbankname() {
		return get("dfbankcountopenbankname");
	}

	/**
	 * 设置对方账户开户行
	 *
	 * @param dfbankcountopenbankname 对方账户开户行
	 */
	public void setDfbankcountopenbankname(String dfbankcountopenbankname) {
		set("dfbankcountopenbankname", dfbankcountopenbankname);
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
	 * 获取审批状态
	 *
	 * @return 审批状态
	 */
	public Short getAuditstatus() {
		return getShort("auditstatus");
	}

	/**
	 * 设置审批状态
	 *
	 * @param auditstatus 审批状态
	 */
	public void setAuditstatus(Short auditstatus) {
		set("auditstatus", auditstatus);
	}

	/**
	 * 获取本方账户
	 *
	 * @return 本方账户.ID
	 */
	public String getBankAccount() {
		return get("bankAccount");
	}

	/**
	 * 设置本方账户
	 *
	 * @param bankAccount 本方账户.ID
	 */
	public void setBankAccount(String bankAccount) {
		set("bankAccount", bankAccount);
	}

	/**
	 * 获取交易类型
	 *
	 * @return 交易类型.ID
	 */
	public String getBustype() {
		return get("bustype");
	}

	/**
	 * 设置交易类型
	 *
	 * @param bustype 交易类型.ID
	 */
	public void setBustype(String bustype) {
		set("bustype", bustype);
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
	 * 获取成本中心
	 *
	 * @return 成本中心.ID
	 */
	public Long getCost_center() {
		return get("cost_center");
	}

	/**
	 * 设置成本中心
	 *
	 * @param cost_center 成本中心.ID
	 */
	public void setCost_center(Long cost_center) {
		set("cost_center", cost_center);
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
	 * 获取待分配金额
	 *
	 * @return 待分配金额
	 */
	public java.math.BigDecimal getDaifenpMoney() {
		return get("daifenpMoney");
	}

	/**
	 * 设置待分配金额
	 *
	 * @param daifenpMoney 待分配金额
	 */
	public void setDaifenpMoney(java.math.BigDecimal daifenpMoney) {
		set("daifenpMoney", daifenpMoney);
	}

	/**
	 * 获取待摊金额
	 *
	 * @return 待摊金额
	 */
	public java.math.BigDecimal getDaitanMoney() {
		return get("daitanMoney");
	}

	/**
	 * 设置待摊金额
	 *
	 * @param daitanMoney 待摊金额
	 */
	public void setDaitanMoney(java.math.BigDecimal daitanMoney) {
		set("daitanMoney", daitanMoney);
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
	 * 获取对方账户联行号
	 *
	 * @return 对方账户联行号
	 */
	public String getDfaccountlinenumber() {
		return get("dfaccountlinenumber");
	}

	/**
	 * 设置对方账户联行号
	 *
	 * @param dfaccountlinenumber 对方账户联行号
	 */
	public void setDfaccountlinenumber(String dfaccountlinenumber) {
		set("dfaccountlinenumber", dfaccountlinenumber);
	}

	/**
	 * 获取对方账户银行类别
	 *
	 * @return 对方账户银行类别
	 */
	public String getDfbankacounttype() {
		return get("dfbankacounttype");
	}

	/**
	 * 设置对方账户银行类别
	 *
	 * @param dfbankacounttype 对方账户银行类别
	 */
	public void setDfbankacounttype(String dfbankacounttype) {
		set("dfbankacounttype", dfbankacounttype);
	}

	/**
	 * 获取对方账户名称
	 *
	 * @return 对方账户名称
	 */
	public String getDfenterprisecountbankname() {
		return get("dfenterprisecountbankname");
	}

	/**
	 * 设置对方账户名称
	 *
	 * @param dfenterprisecountbankname 对方账户名称
	 */
	public void setDfenterprisecountbankname(String dfenterprisecountbankname) {
		set("dfenterprisecountbankname", dfenterprisecountbankname);
	}

	/**
	 * 获取对方类型
	 *
	 * @return 对方类型
	 */
	public Short getDftype() {
		return getShort("dftype");
	}

	/**
	 * 设置对方类型
	 *
	 * @param dftype 对方类型
	 */
	public void setDftype(Short dftype) {
		set("dftype", dftype);
	}

	/**
	 * 获取事项来源
	 *
	 * @return 事项来源
	 */
	public EventSource getEventsrcitem() {
		return get("eventsrcitem");
	}

	/**
	 * 设置事项来源
	 *
	 * @param eventsrcitem 事项来源
	 */
	public void setEventsrcitem(EventSource eventsrcitem) {
		set("eventsrcitem", eventsrcitem);
	}

	/**
	 * 获取事项类型
	 *
	 * @return 事项类型
	 */
	public EventType getEventtype() {
		return get("eventtype");
	}

	/**
	 * 设置事项类型
	 *
	 * @param eventtype 事项类型
	 */
	public void setEventtype(EventType eventtype) {
		set("eventtype", eventtype);
	}

	/**
	 * 获取本币币种汇率
	 *
	 * @return 本币币种汇率
	 */
	public java.math.BigDecimal getExchRate() {
		return get("exchRate");
	}

	/**
	 * 设置本币币种汇率
	 *
	 * @param exchRate 本币币种汇率
	 */
	public void setExchRate(java.math.BigDecimal exchRate) {
		set("exchRate", exchRate);
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
	 * 获取费用方向
	 *
	 * @return 费用方向
	 */
	public Short getExpenseDirect() {
		return getShort("expenseDirect");
	}

	/**
	 * 设置费用方向
	 *
	 * @param expenseDirect 费用方向
	 */
	public void setExpenseDirect(Short expenseDirect) {
		set("expenseDirect", expenseDirect);
	}

	/**
	 * 获取费用支付模式
	 *
	 * @return 费用支付模式
	 */
	public Short getExpensePayMode() {
		return getShort("expensePayMode");
	}

	/**
	 * 设置费用支付模式
	 *
	 * @param expensePayMode 费用支付模式
	 */
	public void setExpensePayMode(Short expensePayMode) {
		set("expensePayMode", expensePayMode);
	}

	/**
	 * 获取费用金额(本币币种)
	 *
	 * @return 费用金额(本币币种)
	 */
	public java.math.BigDecimal getExpenseSumBenbi() {
		return get("expenseSumBenbi");
	}

	/**
	 * 设置费用金额(本币币种)
	 *
	 * @param expenseSumBenbi 费用金额(本币币种)
	 */
	public void setExpenseSumBenbi(java.math.BigDecimal expenseSumBenbi) {
		set("expenseSumBenbi", expenseSumBenbi);
	}

	/**
	 * 获取费用金额(费用币种)
	 *
	 * @return 费用金额(费用币种)
	 */
	public java.math.BigDecimal getExpenseSum_fy() {
		return get("expenseSum_fy");
	}

	/**
	 * 设置费用金额(费用币种)
	 *
	 * @param expenseSum_fy 费用金额(费用币种)
	 */
	public void setExpenseSum_fy(java.math.BigDecimal expenseSum_fy) {
		set("expenseSum_fy", expenseSum_fy);
	}

	/**
	 * 获取费用日期
	 *
	 * @return 费用日期
	 */
	public java.util.Date getExpensedate() {
		return get("expensedate");
	}

	/**
	 * 设置费用日期
	 *
	 * @param expensedate 费用日期
	 */
	public void setExpensedate(java.util.Date expensedate) {
		set("expensedate", expensedate);
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
	 * 获取费用币种id
	 *
	 * @return 费用币种id.ID
	 */
	public String getExpensenatCurrency() {
		return get("expensenatCurrency");
	}

	/**
	 * 设置费用币种id
	 *
	 * @param expensenatCurrency 费用币种id.ID
	 */
	public void setExpensenatCurrency(String expensenatCurrency) {
		set("expensenatCurrency", expensenatCurrency);
	}

	/**
	 * 获取费用参数
	 *
	 * @return 费用参数
	 */
	public Short getExpenseparam() {
		return getShort("expenseparam");
	}

	/**
	 * 设置费用参数
	 *
	 * @param expenseparam 费用参数
	 */
	public void setExpenseparam(Short expenseparam) {
		set("expenseparam", expenseparam);
	}

	/**
	 * 获取集成税务
	 *
	 * @return 集成税务
	 */
	public Short getIntegrated_taxation() {
		return getShort("integrated_taxation");
	}

	/**
	 * 设置集成税务
	 *
	 * @param integrated_taxation 集成税务
	 */
	public void setIntegrated_taxation(Short integrated_taxation) {
		set("integrated_taxation", integrated_taxation);
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
	 * 获取期初标识
	 *
	 * @return 期初标识
	 */
	public Short getIsinit() {
		return getShort("isinit");
	}

	/**
	 * 设置期初标识
	 *
	 * @param isinit 期初标识
	 */
	public void setIsinit(Short isinit) {
		set("isinit", isinit);
	}

	/**
	 * 获取利润中心
	 *
	 * @return 利润中心.ID
	 */
	public String getLirun_center() {
		return get("lirun_center");
	}

	/**
	 * 设置利润中心
	 *
	 * @param lirun_center 利润中心.ID
	 */
	public void setLirun_center(String lirun_center) {
		set("lirun_center", lirun_center);
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
	 * 获取本币币种id
	 *
	 * @return 本币币种id.ID
	 */
	public String getNatCurrency() {
		return get("natCurrency");
	}

	/**
	 * 设置本币币种id
	 *
	 * @param natCurrency 本币币种id.ID
	 */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
	}

	/**
	 * 获取轧差码
	 *
	 * @return 轧差码
	 */
	public String getNettingcode() {
		return get("nettingcode");
	}

	/**
	 * 设置轧差码
	 *
	 * @param nettingcode 轧差码
	 */
	public void setNettingcode(String nettingcode) {
		set("nettingcode", nettingcode);
	}

	/**
	 * 获取轧差后金额
	 *
	 * @return 轧差后金额
	 */
	public java.math.BigDecimal getNettingmoney() {
		return get("nettingmoney");
	}

	/**
	 * 设置轧差后金额
	 *
	 * @param nettingmoney 轧差后金额
	 */
	public void setNettingmoney(java.math.BigDecimal nettingmoney) {
		set("nettingmoney", nettingmoney);
	}

	/**
	 * 获取轧差笔数
	 *
	 * @return 轧差笔数
	 */
	public Integer getNettingnum() {
		return get("nettingnum");
	}

	/**
	 * 设置轧差笔数
	 *
	 * @param nettingnum 轧差笔数
	 */
	public void setNettingnum(Integer nettingnum) {
		set("nettingnum", nettingnum);
	}

	/**
	 * 获取资金组织
	 *
	 * @return 资金组织.ID
	 */
	public String getOrg() {
		return get("org");
	}

	/**
	 * 设置资金组织
	 *
	 * @param org 资金组织.ID
	 */
	public void setOrg(String org) {
		set("org", org);
	}

	/**
	 * 获取过账状态
	 *
	 * @return 过账状态
	 */
	public String getPoststatus() {
		return get("poststatus");
	}

	/**
	 * 设置过账状态
	 *
	 * @param poststatus 过账状态
	 */
	public void setPoststatus(String poststatus) {
		set("poststatus", poststatus);
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
	 * 获取结算成功日期
	 *
	 * @return 结算成功日期
	 */
	public java.util.Date getSettledate() {
		return get("settledate");
	}

	/**
	 * 设置结算成功日期
	 *
	 * @param settledate 结算成功日期
	 */
	public void setSettledate(java.util.Date settledate) {
		set("settledate", settledate);
	}

	/**
	 * 获取结算方式
	 *
	 * @return 结算方式.ID
	 */
	public Long getSettlemodesec() {
		return get("settlemodesec");
	}

	/**
	 * 设置结算方式
	 *
	 * @param settlemodesec 结算方式.ID
	 */
	public void setSettlemodesec(Long settlemodesec) {
		set("settlemodesec", settlemodesec);
	}

	/**
	 * 获取结算状态
	 *
	 * @return 结算状态
	 */
	public Short getSettlestate() {
		return getShort("settlestate");
	}

	/**
	 * 设置结算状态
	 *
	 * @param settlestate 结算状态
	 */
	public void setSettlestate(Short settlestate) {
		set("settlestate", settlestate);
	}

	/**
	 * 获取结算状态
	 *
	 * @return 结算状态
	 */
	public FundSettleStatus getSettlestatus() {
		return get("settlestatus");
	}

	/**
	 * 设置结算状态
	 *
	 * @param settlestatus 结算状态
	 */
	public void setSettlestatus(FundSettleStatus settlestatus) {
		set("settlestatus", settlestatus);
	}

	/**
	 * 获取分摊类型
	 *
	 * @return 分摊类型
	 */
	public Short getSharetype() {
		return getShort("sharetype");
	}

	/**
	 * 设置分摊类型
	 *
	 * @param sharetype 分摊类型
	 */
	public void setSharetype(Short sharetype) {
		set("sharetype", sharetype);
	}

	/**
	 * 获取事项来源编号
	 *
	 * @return 事项来源编号
	 */
	public String getSrccode() {
		return get("srccode");
	}

	/**
	 * 设置事项来源编号
	 *
	 * @param srccode 事项来源编号
	 */
	public void setSrccode(String srccode) {
		set("srccode", srccode);
	}

	/**
	 * 获取关联来源单据id
	 *
	 * @return 关联来源单据id
	 */
	public String getSrcid() {
		return get("srcid");
	}

	/**
	 * 设置关联来源单据id
	 *
	 * @param srcid 关联来源单据id
	 */
	public void setSrcid(String srcid) {
		set("srcid", srcid);
	}

//	/**
//	 * 获取单据状态
//	 *
//	 * @return 单据状态
//	 */
//	public Short getStatus() {
//		return getShort("status");
//	}
//
//	/**
//	 * 设置单据状态
//	 *
//	 * @param status 单据状态
//	 */
//	public void setStatus(Short status) {
//		set("status", status);
//	}

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
	 * 获取资金费用子表集合
	 *
	 * @return 资金费用子表集合
	 */
	public java.util.List<Fundexpense_b> detail() {
		return getBizObjects("detail", Fundexpense_b.class);
	}

	/**
	 * 设置资金费用子表集合
	 *
	 * @param detail 资金费用子表集合
	 */
	public void setDetail(java.util.List<Fundexpense_b> detail) {
		setBizObjects("detail", detail);
	}

	/**
	 * 获取税费信息子表集合
	 *
	 * @return 税费信息子表集合
	 */
	public java.util.List<Taxation_detail_b> taxdetail() {
		return getBizObjects("taxdetail", Taxation_detail_b.class);
	}

	/**
	 * 设置税费信息子表集合
	 *
	 * @param taxdetail 税费信息子表集合
	 */
	public void setTaxdetail(java.util.List<Taxation_detail_b> taxdetail) {
		setBizObjects("taxdetail", taxdetail);
	}

}
