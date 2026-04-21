package com.yonyoucloud.fi.cmp.billclaim;

import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 认领单主表实体
 *
 * @author u
 * @version 1.0
 */
public class BillClaim extends BizObject implements IAuditInfo, ITenant, ICurrency, IPrintCount, IApprovalFlow, IApprovalInfo, AccentityRawInterface {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.billclaim.BillClaim";


	//认领单主表和明细子表子表集合关联键
	public static final String ITEMS_KEY = "items";

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
	 * 获取业务单元
	 *
	 * @return 业务单元.ID
	 */
	public String getOrg() {
		return get("org");
	}

	/**
	 * 设置业务单元
	 *
	 * @param org 业务单元.ID
	 */
	public void setOrg(String org) {
		set("org", org);
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
	 * 获取借贷方向
	 *
	 * @return 借贷方向
	 */
	public Short getDirection() {
		return getShort("direction");
	}

	/**
	 * 设置借贷方向
	 *
	 * @param direction 借贷方向
	 */
	public void setDirection(Short direction) {
		set("direction", direction);
	}

	/**
	 * 获取对方账号
	 *
	 * @return 对方账号
	 */
	public String getToaccountno() {
		return get("toaccountno");
	}

	/**
	 * 设置对方账号
	 *
	 * @param toaccountno 对方账号
	 */
	public void setToaccountno(String toaccountno) {
		set("toaccountno", toaccountno);
	}

	/**
	 * 获取对方户名
	 *
	 * @return 对方户名
	 */
	public String getToaccountname() {
		return get("toaccountname");
	}

	/**
	 * 设置对方户名
	 *
	 * @param toaccountname 对方户名
	 */
	public void setToaccountname(String toaccountname) {
		set("toaccountname", toaccountname);
	}

	/**
	 * 获取对方开户行
	 *
	 * @return 对方开户行
	 */
	public String getToaccountbank() {
		return get("toaccountbank");
	}

	/**
	 * 设置对方开户行
	 *
	 * @param toaccountbank 对方开户行
	 */
	public void setToaccountbank(String toaccountbank) {
		set("toaccountbank", toaccountbank);
	}

	/**
	 * 获取对方开户行名
	 *
	 * @return 对方开户行名
	 */
	public String getToaccountbankname() {
		return get("toaccountbankname");
	}

	/**
	 * 设置对方开户行名
	 *
	 * @param toaccountbankname 对方开户行名
	 */
	public void setToaccountbankname(String toaccountbankname) {
		set("toaccountbankname", toaccountbankname);
	}

	/**
	 * 获取认领日期
	 *
	 * @return 认领日期
	 */
	public java.util.Date getClaimdate() {
		return get("claimdate");
	}

	/**
	 * 设置认领日期
	 *
	 * @param claimdate 认领日期
	 */
	public void setClaimdate(java.util.Date claimdate) {
		set("claimdate", claimdate);
	}

	/**
	 * 获取认领类型
	 *
	 * @return 认领类型
	 */
	public Short getClaimtype() {
		return getShort("claimtype");
	}

	/**
	 * 设置认领类型
	 *
	 * @param claimtype 认领类型
	 */
	public void setClaimtype(Short claimtype) {
		set("claimtype", claimtype);
	}

	/**
	 * 获取认领总金额
	 *
	 * @return 认领总金额
	 */
	public java.math.BigDecimal getTotalamount() {
		return get("totalamount");
	}

	/**
	 * 设置认领总金额
	 *
	 * @param totalamount 认领总金额
	 */
	public void setTotalamount(java.math.BigDecimal totalamount) {
		set("totalamount", totalamount);
	}

	/**
	 * 获取认领人
	 *
	 * @return 认领人
	 */
	public String getClaimstaff() {
		return get("claimstaff");
	}

	/**
	 * 设置认领人
	 *
	 * @param claimstaff 认领人
	 */
	public void setClaimstaff(String claimstaff) {
		set("claimstaff", claimstaff);
	}

	/**
	 * 获取认领说明
	 *
	 * @return 认领说明
	 */
	public String getRemark() {
		return get("remark");
	}

	/**
	 * 设置认领说明
	 *
	 * @param remark 认领说明
	 */
	public void setRemark(String remark) {
		set("remark", remark);
	}

	/**
	 * 获取业务关联状态
	 *
	 * @return 业务关联状态
	 */
	public Short getAssociationstatus() {
		return getShort("associationstatus");
	}

	/**
	 * 设置业务关联状态
	 *
	 * @param associationstatus 业务关联状态
	 */
	public void setAssociationstatus(Short associationstatus) {
		set("associationstatus", associationstatus);
	}

	/**
	 * 获取Y租户Id
	 *
	 * @return Y租户Id
	 */
	public String getYtenantId() {
		return get("ytenantId");
	}

	/**
	 * 设置Y租户Id
	 *
	 * @param ytenantId Y租户Id
	 */
	public void setYtenantId(String ytenantId) {
		set("ytenantId", ytenantId);
	}


	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getBillClaimCharacterDef() {
		return get("billClaimCharacterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param billClaimCharacterDef 自定义项特征属性组.ID
	 */
	public void setBillClaimCharacterDef(String billClaimCharacterDef) {
		set("billClaimCharacterDef", billClaimCharacterDef);
	}

	/**
	 * 获取智能对账勾兑码
	 *
	 * @return 智能对账勾兑码
	 */
	public String getSmartcheckno() {
		return get("smartcheckno");
	}

	/**
	 * 设置智能对账勾兑码
	 *
	 * @param smartcheckno 智能对账勾兑码
	 */
	public void setSmartcheckno(String smartcheckno) {
		set("smartcheckno", smartcheckno);
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
	 * 获取实际认领单位
	 *
	 * @return 实际认领单位
	 */
	public String getActualclaimaccentiry() {
		return get("actualclaimaccentiry");
	}

	/**
	 * 设置实际认领单位
	 *
	 * @param actualclaimaccentiry 实际认领单位
	 */
	public void setActualclaimaccentiry(String actualclaimaccentiry) {
		set("actualclaimaccentiry", actualclaimaccentiry);
	}

	/**
	 * 获取实际认领单位
	 *
	 * @return 实际认领单位
	 */
	public String getactualclaimaccentiryRaw() {
		return get("actualclaimaccentiryRaw");
	}

	/**
	 * 设置实际认领单位
	 *
	 * @param actualclaimaccentiryRaw 实际认领单位
	 */
	public void setactualclaimaccentiryRaw(String actualclaimaccentiryRaw) {
		set("actualclaimaccentiryRaw", actualclaimaccentiryRaw);
	}

	/**
	 * 获取实际结算主体
	 *
	 * @return 实际结算主体
	 */
	public String getActualsettleaccentity() {
		return get("actualsettleaccentity");
	}

	/**
	 * 设置实际结算主体
	 *
	 * @param actualsettleaccentity 实际结算主体
	 */
	public void setActualsettleaccentity(String actualsettleaccentity) {
		set("actualsettleaccentity", actualsettleaccentity);
	}

	/**
	 * 获取业务模式
	 *
	 * @return 业务模式
	 */
	public Short getBusinessmodel() {
		return getShort("businessmodel");
	}

	/**
	 * 设置业务模式
	 *
	 * @param businessmodel 业务模式
	 */
	public void setBusinessmodel(Short businessmodel) {
		set("businessmodel", businessmodel);
	}

	/**
	 * 获取统收统支标识
	 *
	 * @return 统收统支标识
	 */
	public Boolean getIsincomeandexpenditure() {
		return getBoolean("isincomeandexpenditure");
	}

	/**
	 * 设置统收统支标识
	 *
	 * @param isincomeandexpenditure 统收统支标识
	 */
	public void setIsincomeandexpenditure(Boolean isincomeandexpenditure) {
		set("isincomeandexpenditure", isincomeandexpenditure);
	}

	/**
	 * 设置统收统支关系组
	 *
	 * @param incomeAndExpendRelationGroup 统收统支关系组
	 */
	public void setIncomeAndExpendRelationGroup(Long incomeAndExpendRelationGroup) {
		set("incomeAndExpendRelationGroup", incomeAndExpendRelationGroup);
	}

	/**
	 * 获取统收统支关系组
	 *
	 * @return 统收统支关系组
	 */
	public Long getIncomeAndExpendRelationGroup() {
		return get("incomeAndExpendRelationGroup");
	}


	/**
	 * 获取认领账户
	 *
	 * @return 认领账户
	 */
	public String getClaimaccount() {
		return get("claimaccount");
	}

	/**
	 * 设置认领账户
	 *
	 * @param claimaccount 认领账户
	 */
	public void setClaimaccount(String claimaccount) {
		set("claimaccount", claimaccount);
	}

	/**
	 * 获取款项类型
	 *
	 * @return 款项类型
	 */
	public Long getQuicktype() {
		return get("quicktype");
	}

	/**
	 * 设置款项类型
	 *
	 * @param quicktype 款项类型
	 */
	public void setQuicktype(Long quicktype) {
		set("quicktype", quicktype);
	}

	/**
	 * 获取提前入账
	 *
	 * @return 提前入账
	 */
	public Boolean getEarlyentry() {
		return getBoolean("earlyentry");
	}

	/**
	 * 设置提前入账
	 *
	 * @param earlyentry 提前入账
	 */
	public void setEarlyentry(Boolean earlyentry) {
		set("earlyentry", earlyentry);
	}

	/**
	 * 获取人员
	 *
	 * @return 人员
	 */
	public String getClaimperson() {
		return get("claimperson");
	}

	/**
	 * 设置人员
	 *
	 * @param claimperson 人员
	 */
	public void setClaimperson(String claimperson) {
		set("claimperson", claimperson);
	}

	/**
	 * 获取内转协议编号
	 *
	 * @return 内转协议编号
	 */
	public String getIntransagreementnumber() {
		return get("intransagreementnumber");
	}

	/**
	 * 设置内转协议编号
	 *
	 * @param intransagreementnumber 内转协议编号
	 */
	public void setIntransagreementnumber(String intransagreementnumber) {
		set("intransagreementnumber", intransagreementnumber);
	}

	/**
	 * 获取内转协议版本
	 *
	 * @return 内转协议版本
	 */
	public String getIntransagreementversion() {
		return get("intransagreementversion");
	}

	/**
	 * 设置内转协议版本
	 *
	 * @param intransagreementversion 内转协议版本
	 */
	public void setIntransagreementversion(String intransagreementversion) {
		set("intransagreementversion", intransagreementversion);
	}

	/**
	 * 获取资金切分方式
	 *
	 * @return 资金切分方式
	 */
	public Short getFundsplitmethod() {
		return getShort("fundsplitmethod");
	}

	/**
	 * 设置资金切分方式
	 *
	 * @param fundsplitmethod 资金切分方式
	 */
	public void setFundsplitmethod(Short fundsplitmethod) {
		set("fundsplitmethod", fundsplitmethod);
	}

	/**
	 * 获取是否资金切分
	 *
	 * @return 是否资金切分
	 */
	public Boolean getIsfundsplit() {
		return getBoolean("isfundsplit");
	}

	/**
	 * 设置是否资金切分
	 *
	 * @param isfundsplit 是否资金切分
	 */
	public void setIsfundsplit(Boolean isfundsplit) {
		set("isfundsplit", isfundsplit);
	}

	/**
	 * 获取内部账户是否记账
	 *
	 * @return 内部账户是否记账
	 */
	public Boolean getIsinneraccounting() {
		return getBoolean("isinneraccounting");
	}

	/**
	 * 设置内部账户是否记账
	 *
	 * @param isinneraccounting 内部账户是否记账
	 */
	public void setIsinneraccounting(Boolean isinneraccounting) {
		set("isinneraccounting", isinneraccounting);
	}

	/**
	 * 获取归集内部账户
	 *
	 * @return 归集内部账户
	 */
	public String getImpinneraccount() {
		return get("impinneraccount");
	}

	/**
	 * 设置归集内部账户
	 *
	 * @param impinneraccount 归集内部账户
	 */
	public void setImpinneraccount(String impinneraccount) {
		set("impinneraccount", impinneraccount);
	}

	/**
	 * 获取参照单据
	 *
	 * @return 参照单据
	 */
	public String getRefbill() {
		return get("refbill");
	}

	/**
	 * 设置参照单据
	 *
	 * @param refbill 参照单据
	 */
	public void setRefbill(String refbill) {
		set("refbill", refbill);
	}

	/**
	 * 获取参照关联状态-实际认领单位认领完结状态
	 *
	 * @return 参照关联状态
	 */
	public Short getRefassociationstatus() {
		return getShort("refassociationstatus");
	}

	/**
	 * 设置参照关联状态
	 *
	 * @param refassociationstatus 参照关联状态
	 */
	public void setRefassociationstatus(Short refassociationstatus) {
		set("refassociationstatus", refassociationstatus);
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
     * 获取认领单明细子表子表集合
     *
     * @return 认领单明细子表子表集合
     */
	public java.util.List<BillClaimItem> items() {
		return getBizObjects("items", BillClaimItem.class);
	}

    /**
     * 设置认领单明细子表子表集合
     *
     * @param items 认领单明细子表子表集合
     */
	public void setItems(java.util.List<BillClaimItem> items) {
		setBizObjects("items", items);
	}

	/**
	 * 获取认领状态
	 *
	 * @return 认领状态
	 */
	public Short getRecheckstatus() {
		return getShort("recheckstatus");
	}

	/**
	 * 设置复核状态
	 *
	 * @param recheckstatus 认领状态
	 */
	public void setRecheckstatus(Short recheckstatus) {
		set("recheckstatus", recheckstatus);
	}

	/**
	 * 获取复核人
	 *
	 * @return 复核人
	 */
	public String getRecheckstaff() {
		return get("recheckstaff");
	}

	/**
	 * 设置复核人
	 *
	 * @param recheckstaff 复核人
	 */
	public void setRecheckstaff(String recheckstaff) {
		set("recheckstaff", recheckstaff);
	}

	/**
	 * 获取复核时间
	 *
	 * @return 复核时间
	 */
	public java.util.Date getRecheckdate() {
		return get("recheckdate");
	}

	/**
	 * 设置复核时间
	 *
	 * @param recheckdate 复核时间
	 */
	public void setRecheckdate(java.util.Date recheckdate) {
		set("recheckdate", recheckdate);
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
	 * 获取审批流状态
	 *
	 * @return 审批流状态
	 */
	public Short getVerifystate() {
		return getShort("verifystate");
	}

	/**
	 * 设置审批流状态
	 *
	 * @param verifystate 审批流状态
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
	public AuditStatus getAuditstatus() {
		return get("auditstatus");
	}

	/**
	 * 设置审批状态
	 *
	 * @param auditstatus 审批状态
	 */
	public void setAuditstatus(AuditStatus auditstatus) {
		set("auditstatus", auditstatus);
	}
	/**
	 * 获取认领完结方式
	 *
	 * @return 认领完结方式
	 */
	public Short getClaimcompletetype() {
		return getShort("claimcompletetype");
	}

	/**
	 * 设置认领完结方式
	 *
	 * @param claimcompletetype 认领完结方式
	 */
	public void setClaimcompletetype(Short claimcompletetype) {
		set("claimcompletetype", claimcompletetype);
	}

	/**
	 * 获取业务凭据关联信息集合
	 *
	 * @return 业务凭据关联信息集合
	 */
	public java.util.List<BillClaimBusVoucherInfo> busVoucherInfoList() {
		return getBizObjects("busVoucherInfoList", BillClaimBusVoucherInfo.class);
	}

	/**
	 * 设置业务凭据关联信息集合
	 *
	 * @param busVoucherInfoList 业务凭据关联信息集合
	 */
	public void setBusVoucherInfoList(java.util.List<BillClaimBusVoucherInfo> busVoucherInfoList) {
		setBizObjects("busVoucherInfoList", busVoucherInfoList);
	}

	/**
	 * 获取收付单据关联时间
	 *
	 * @return 收付单据关联时间
	 */
	public java.util.Date getAssociateddate() {
		return get("associateddate");
	}

	/**
	 * 设置收付单据关联时间
	 *
	 * @param associateddate 收付单据关联时间
	 */
	public void setAssociateddate(java.util.Date associateddate) {
		set("associateddate", associateddate);
	}

	/**
	 * 获取收付单据关联操作人
	 *
	 * @return 收付单据关联操作人
	 */
	public String getAssociatedoperator() {
		return get("associatedoperator");
	}

	/**
	 * 设置收付单据关联操作人
	 *
	 * @param associatedoperator 收付单据关联操作人
	 */
	public void setAssociatedoperator(String associatedoperator) {
		set("associatedoperator", associatedoperator);
	}

	/**
	 * 获取业务凭据类型
	 *
	 * @return 业务凭据类型
	 */
	public Short getBusvouchercorr_billtype() {
		return getShort("busvouchercorr_billtype");
	}

	/**
	 * 设置业务凭据类型
	 *
	 * @param busvouchercorr_billtype 业务凭据类型
	 */
	public void setBusvouchercorr_billtype(Short busvouchercorr_billtype) {
		set("busvouchercorr_billtype", busvouchercorr_billtype);
	}

	/**
	 * 获取业务凭据关联时间
	 *
	 * @return 业务凭据关联时间
	 */
	public java.util.Date getBusvouchercorr_date() {
		return get("busvouchercorr_date");
	}

	/**
	 * 设置业务凭据关联时间
	 *
	 * @param busvouchercorr_date 业务凭据关联时间
	 */
	public void setBusvouchercorr_date(java.util.Date busvouchercorr_date) {
		set("busvouchercorr_date", busvouchercorr_date);
	}

	/**
	 * 获取业务凭据自动关联
	 *
	 * @return 业务凭据自动关联
	 */
	public Boolean getBusvouchercorr_isautocorr() {
		return getBoolean("busvouchercorr_isautocorr");
	}

	/**
	 * 设置业务凭据自动关联
	 *
	 * @param busvouchercorr_isautocorr 业务凭据自动关联
	 */
	public void setBusvouchercorr_isautocorr(Boolean busvouchercorr_isautocorr) {
		set("busvouchercorr_isautocorr", busvouchercorr_isautocorr);
	}

	/**
	 * 获取业务凭据关联状态
	 *
	 * @return 业务凭据关联状态
	 */
	public Boolean getBusvouchercorr_iscorr() {
		return getBoolean("busvouchercorr_iscorr");
	}

	/**
	 * 设置业务凭据关联状态
	 *
	 * @param busvouchercorr_iscorr 业务凭据关联状态
	 */
	public void setBusvouchercorr_iscorr(Boolean busvouchercorr_iscorr) {
		set("busvouchercorr_iscorr", busvouchercorr_iscorr);
	}

	/**
	 * 获取业务凭据关联操作人
	 *
	 * @return 业务凭据关联操作人
	 */
	public String getBusvouchercorr_operator() {
		return get("busvouchercorr_operator");
	}

	/**
	 * 设置业务凭据关联操作人
	 *
	 * @param busvouchercorr_operator 业务凭据关联操作人
	 */
	public void setBusvouchercorr_operator(String busvouchercorr_operator) {
		set("busvouchercorr_operator", busvouchercorr_operator);
	}

	/**
	 * 获取关联业务凭据金额合计
	 *
	 * @return 关联业务凭据金额合计
	 */
	public java.math.BigDecimal getBusvouchercorr_totalamount() {
		return get("busvouchercorr_totalamount");
	}

	/**
	 * 设置关联业务凭据金额合计
	 *
	 * @param busvouchercorr_totalamount 关联业务凭据金额合计
	 */
	public void setBusvouchercorr_totalamount(java.math.BigDecimal busvouchercorr_totalamount) {
		set("busvouchercorr_totalamount", busvouchercorr_totalamount);
	}

}
