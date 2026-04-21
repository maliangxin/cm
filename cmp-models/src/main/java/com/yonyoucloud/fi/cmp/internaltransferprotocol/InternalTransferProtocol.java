package com.yonyoucloud.fi.cmp.internaltransferprotocol;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IAutoCode;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import org.imeta.orm.base.BizObject;

/**
 * 内转协议主表实体
 *
 * @author u
 * @version 1.0
 */
public class InternalTransferProtocol extends BizObject implements IAutoCode, IAuditInfo, ITenant, IYTenant, ICurrency {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.internaltransferprotocol.InternalTransferProtocol";
	private static final long serialVersionUID = -8223098840673430419L;

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
     * 获取单据类型
     *
     * @return 单据类型
     */
	public EventType getBilltype() {
		Number v = get("billtype");
		return EventType.find(v);
	}

    /**
     * 设置单据类型
     *
     * @param billtype 单据类型
     */
	public void setBilltype(EventType billtype) {
		if (billtype != null) {
			set("billtype", billtype.getValue());
		} else {
			set("billtype", null);
		}
	}

    /**
     * 获取内转协议编号
     *
     * @return 内转协议编号
     */
	public String getInternalTransferProtocolCode() {
		return get("internalTransferProtocolCode");
	}

    /**
     * 设置内转协议编号
     *
     * @param internalTransferProtocolCode 内转协议编号
     */
	public void setInternalTransferProtocolCode(String internalTransferProtocolCode) {
		set("internalTransferProtocolCode", internalTransferProtocolCode);
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
     * 获取合同号
     *
     * @return 合同号
     */
	public String getContractNo() {
		return get("contractNo");
	}

    /**
     * 设置合同号
     *
     * @param contractNo 合同号
     */
	public void setContractNo(String contractNo) {
		set("contractNo", contractNo);
	}

    /**
     * 获取合同名称
     *
     * @return 合同名称
     */
	public String getContractName() {
		return get("contractName");
	}

    /**
     * 设置合同名称
     *
     * @param contractName 合同名称
     */
	public void setContractName(String contractName) {
		set("contractName", contractName);
	}

    /**
     * 获取启停用状态
     *
     * @return 启停用状态
     */
	public Short getIsEnabledType() {
	    return getShort("isEnabledType");
	}

    /**
     * 设置启停用状态
     *
     * @param isEnabledType 启停用状态
     */
	public void setIsEnabledType(Short isEnabledType) {
		set("isEnabledType", isEnabledType);
	}

    /**
     * 获取责任主体
     *
     * @return 责任主体.ID
     */
	public String getOrg() {
		return get("org");
	}

    /**
     * 设置责任主体
     *
     * @param org 责任主体.ID
     */
	public void setOrg(String org) {
		set("org", org);
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
     * 获取转出账户分配
     *
     * @return 转出账户分配
     */
	public Short getTransferOutAccountAllocation() {
	    return getShort("transferOutAccountAllocation");
	}

    /**
     * 设置转出账户分配
     *
     * @param transferOutAccountAllocation 转出账户分配
     */
	public void setTransferOutAccountAllocation(Short transferOutAccountAllocation) {
		set("transferOutAccountAllocation", transferOutAccountAllocation);
	}

    /**
     * 获取银行账户
     *
     * @return 银行账户.ID
     */
	public String getEnterprisebankaccount() {
		return get("enterprisebankaccount");
	}

    /**
     * 设置银行账户
     *
     * @param enterprisebankaccount 银行账户.ID
     */
	public void setEnterprisebankaccount(String enterprisebankaccount) {
		set("enterprisebankaccount", enterprisebankaccount);
	}

    /**
     * 获取开户类型
     *
     * @return 开户类型
     */
	public Short getAcctOpenType() {
	    return getShort("acctOpenType");
	}

    /**
     * 设置开户类型
     *
     * @param acctOpenType 开户类型
     */
	public void setAcctOpenType(Short acctOpenType) {
		set("acctOpenType", acctOpenType);
	}

    /**
     * 获取版本号
     *
     * @return 版本号
     */
	public String getVersionNo() {
		return get("versionNo");
	}

    /**
     * 设置版本号
     *
     * @param versionNo 版本号
     */
	public void setVersionNo(String versionNo) {
		set("versionNo", versionNo);
	}

    /**
     * 获取版本Id
     *
     * @return 版本Id
     */
	public Long getVersionId() {
		return get("versionId");
	}

    /**
     * 设置版本Id
     *
     * @param versionId 版本Id
     */
	public void setVersionId(Long versionId) {
		set("versionId", versionId);
	}

    /**
     * 获取父单据Id
     *
     * @return 父单据Id
     */
	public Long getParentId() {
		return get("parentId");
	}

    /**
     * 设置父单据Id
     *
     * @param parentId 父单据Id
     */
	public void setParentId(Long parentId) {
		set("parentId", parentId);
	}

	/**
	 * 获取是否为父级
	 *
	 * @return 是否为父级
	 */
	public Long getIsParent() {
		return get("isParent");
	}

	/**
	 * 设置是否为父级
	 *
	 * @param isParent 是否为父级
	 */
	public void setIsParent(Long isParent) {
		set("isParent", isParent);
	}

    /**
     * 获取内转协议特征
     *
     * @return 内转协议特征.ID
     */
	public String getInternalTransferProtocolCharacterDef() {
		return get("internalTransferProtocolCharacterDef");
	}

    /**
     * 设置内转协议特征
     *
     * @param internalTransferProtocolCharacterDef 内转协议特征.ID
     */
	public void setInternalTransferProtocolCharacterDef(String internalTransferProtocolCharacterDef) {
		set("internalTransferProtocolCharacterDef", internalTransferProtocolCharacterDef);
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
     * 获取转出方信息集合
     *
     * @return 转出方信息集合
     */
	public java.util.List<TransfereeInformation> TransfereeInformation() {
		return getBizObjects("TransfereeInformation", TransfereeInformation.class);
	}

    /**
     * 设置转出方信息集合
     *
     * @param TransfereeInformation 转出方信息集合
     */
	public void setTransfereeInformation(java.util.List<TransfereeInformation> TransfereeInformation) {
		setBizObjects("TransfereeInformation", TransfereeInformation);
	}

    /**
     * 获取协议调用日志集合
     *
     * @return 协议调用日志集合
     */
	public java.util.List<ProtocolCallLogs> ProtocolCallLogs() {
		return getBizObjects("ProtocolCallLogs", ProtocolCallLogs.class);
	}

    /**
     * 设置协议调用日志集合
     *
     * @param ProtocolCallLogs 协议调用日志集合
     */
	public void setProtocolCallLogs(java.util.List<ProtocolCallLogs> ProtocolCallLogs) {
		setBizObjects("ProtocolCallLogs", ProtocolCallLogs);
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

}
