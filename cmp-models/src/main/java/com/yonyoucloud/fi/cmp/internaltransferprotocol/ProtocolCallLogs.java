package com.yonyoucloud.fi.cmp.internaltransferprotocol;


import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import org.imeta.orm.base.BizObject;

/**
 * 协议调用日志实体
 *
 * @author u
 * @version 1.0
 */
public class ProtocolCallLogs extends BizObject implements IAuditInfo, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.internaltransferprotocol.ProtocolCallLogs";
	private static final long serialVersionUID = 6141883730547120538L;

	/**
     * 获取调用日期
     *
     * @return 调用日期
     */
	public java.util.Date getCallDate() {
		return get("callDate");
	}

    /**
     * 设置调用日期
     *
     * @param callDate 调用日期
     */
	public void setCallDate(java.util.Date callDate) {
		set("callDate", callDate);
	}

    /**
     * 获取调用方单据编码
     *
     * @return 调用方单据编码
     */
	public String getCallerDocumentCode() {
		return get("callerDocumentCode");
	}

    /**
     * 设置调用方单据编码
     *
     * @param callerDocumentCode 调用方单据编码
     */
	public void setCallerDocumentCode(String callerDocumentCode) {
		set("callerDocumentCode", callerDocumentCode);
	}

    /**
     * 获取调用协议版本
     *
     * @return 调用协议版本
     */
	public String getCallerProtocolVersion() {
		return get("callerProtocolVersion");
	}

    /**
     * 设置调用协议版本
     *
     * @param callerProtocolVersion 调用协议版本
     */
	public void setCallerProtocolVersion(String callerProtocolVersion) {
		set("callerProtocolVersion", callerProtocolVersion);
	}

    /**
     * 获取调用方系统名称
     *
     * @return 调用方系统名称
     */
	public String getCallerSystemName() {
		return get("callerSystemName");
	}

    /**
     * 设置调用方系统名称
     *
     * @param callerSystemName 调用方系统名称
     */
	public void setCallerSystemName(String callerSystemName) {
		set("callerSystemName", callerSystemName);
	}

    /**
     * 获取调用方交易类型
     *
     * @return 调用方交易类型
     */
	public String getCallerTransactionType() {
		return get("callerTransactionType");
	}

    /**
     * 设置调用方交易类型
     *
     * @param callerTransactionType 调用方交易类型
     */
	public void setCallerTransactionType(String callerTransactionType) {
		set("callerTransactionType", callerTransactionType);
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
     * 获取生成单据编码
     *
     * @return 生成单据编码
     */
	public String getGeneratedDocumentCode() {
		return get("generatedDocumentCode");
	}

    /**
     * 设置生成单据编码
     *
     * @param generatedDocumentCode 生成单据编码
     */
	public void setGeneratedDocumentCode(String generatedDocumentCode) {
		set("generatedDocumentCode", generatedDocumentCode);
	}
   /**
     * 获取生成单据ID
     *
     * @return 生成单据ID
     */
	public Long getGeneratedDocumentId() {
		return get("generatedDocumentId");
	}

    /**
     * 设置生成单据ID
     *
     * @param generatedDocumentId 生成单据ID
     */
	public void setGeneratedDocumentId(Long generatedDocumentId) {
		set("generatedDocumentId", generatedDocumentId);
	}

    /**
     * 获取生成单据状态
     *
     * @return 生成单据状态
     */
	public String getGeneratedDocumentStatus() {
		return get("generatedDocumentStatus");
	}

    /**
     * 设置生成单据状态
     *
     * @param generatedDocumentStatus 生成单据状态
     */
	public void setGeneratedDocumentStatus(String generatedDocumentStatus) {
		set("generatedDocumentStatus", generatedDocumentStatus);
	}

    /**
     * 获取主表id
     *
     * @return 主表id.ID
     */
	public String getMainid() {
		return get("mainid");
	}

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
	public void setMainid(String mainid) {
		set("mainid", mainid);
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
     * 获取协议Id
     *
     * @return 协议Id
     */
	public Long getProtocolId() {
		return get("protocolId");
	}

    /**
     * 设置协议Id
     *
     * @param protocolId 协议Id
     */
	public void setProtocolId(Long protocolId) {
		set("protocolId", protocolId);
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
