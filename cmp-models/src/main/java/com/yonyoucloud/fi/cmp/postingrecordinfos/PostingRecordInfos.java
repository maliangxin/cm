package com.yonyoucloud.fi.cmp.postingrecordinfos;


import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 过账记录信息实体
 *
 * @author u
 * @version 1.0
 */
public class PostingRecordInfos extends BizObject implements ITenant, IAuditInfo {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.postingrecordinfos.PostingRecordInfos";

	/**
	 * 获取单据id
	 *
	 * @return 单据id
	 */
	public String getBillId() {
		return get("billId");
	}

	/**
	 * 设置单据id
	 *
	 * @param billId 单据id
	 */
	public void setBillId(String billId) {
		set("billId", billId);
	}

	/**
	 * 获取单据编码
	 *
	 * @return 单据编码
	 */
	public String getBillCode() {
		return get("billCode");
	}

	/**
	 * 设置单据编码
	 *
	 * @param billCode 单据编码
	 */
	public void setBillCode(String billCode) {
		set("billCode", billCode);
	}

	/**
	 * 获取单据名称
	 *
	 * @return 单据名称
	 */
	public String getBillName() {
		return get("billName");
	}

	/**
	 * 设置单据名称
	 *
	 * @param billName 单据名称
	 */
	public void setBillName(String billName) {
		set("billName", billName);
	}

	/**
	 * 获取单据编号
	 *
	 * @return 单据编号
	 */
	public String getBillNum() {
		return get("billNum");
	}

	/**
	 * 设置单据编号
	 *
	 * @param billNum 单据编号
	 */
	public void setBillNum(String billNum) {
		set("billNum", billNum);
	}

	/**
	 * 获取单据实体名称
	 *
	 * @return 单据实体名称
	 */
	public String getBillEntityName() {
		return get("billEntityName");
	}

	/**
	 * 设置单据实体名称
	 *
	 * @param billEntityName 单据实体名称
	 */
	public void setBillEntityName(String billEntityName) {
		set("billEntityName", billEntityName);
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
	 * 获取过账状态码
	 *
	 * @return 过账状态码
	 */
	public String getPostingCode() {
		return get("postingCode");
	}

	/**
	 * 设置过账状态码
	 *
	 * @param postingCode 过账状态码
	 */
	public void setPostingCode(String postingCode) {
		set("postingCode", postingCode);
	}

	/**
	 * 获取过账返回信息
	 *
	 * @return 过账返回信息
	 */
	public String getPostingMsg() {
		return get("postingMsg");
	}

	/**
	 * 设置过账返回信息
	 *
	 * @param postingMsg 过账返回信息
	 */
	public void setPostingMsg(String postingMsg) {
		set("postingMsg", postingMsg);
	}

	/**
	 * 获取过账记录信息
	 *
	 * @return 过账记录信息
	 */
	public String getPostingArray() {
		return get("postingArray");
	}

	/**
	 * 设置过账记录信息
	 *
	 * @param postingArray 过账记录信息
	 */
	public void setPostingArray(String postingArray) {
		set("postingArray", postingArray);
	}

	/**
	 * 获取单据详情
	 *
	 * @return 单据详情
	 */
	public String getBillDetail() {
		return get("billDetail");
	}

	/**
	 * 设置单据详情
	 *
	 * @param billDetail 单据详情
	 */
	public void setBillDetail(String billDetail) {
		set("billDetail", billDetail);
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

}
