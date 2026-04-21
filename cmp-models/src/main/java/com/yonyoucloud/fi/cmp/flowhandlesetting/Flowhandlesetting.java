package com.yonyoucloud.fi.cmp.flowhandlesetting;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IAutoCode;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 流水处理流程设置表实体
 * table cmp_flow_handle_setting
 * @author guoxh
 * @version 1.0
 */
public class Flowhandlesetting extends BizObject implements IYTenant, ITenant, IAutoCode, IAuditInfo {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.flowhandlesetting.Flowhandlesetting";

    /**
     * 获取关联单据范围
     *
     * @return 关联单据范围
     */
	public Short getAssociationBillRange() {
	    return getShort("associationBillRange");
	}

    /**
     * 设置关联单据范围
     *
     * @param associationBillRange 关联单据范围
     */
	public void setAssociationBillRange(Short associationBillRange) {
		set("associationBillRange", associationBillRange);
	}

    /**
     * 获取业务凭据关联方式
     *
     * @return 业务凭据关联方式
     */
	public Short getAssociationCredentialMode() {
	    return getShort("associationCredentialMode");
	}

    /**
     * 设置业务凭据关联方式
     *
     * @param associationCredentialMode 业务凭据关联方式
     */
	public void setAssociationCredentialMode(Short associationCredentialMode) {
		set("associationCredentialMode", associationCredentialMode);
	}

    /**
     * 获取关联单据范围
     *
     * @return 关联单据范围
     */
	public Short getAssociationCredentialRange() {
	    return getShort("associationCredentialRange");
	}

    /**
     * 设置关联单据范围
     *
     * @param associationCredentialRange 关联单据范围
     */
	public void setAssociationCredentialRange(Short associationCredentialRange) {
		set("associationCredentialRange", associationCredentialRange);
	}

    /**
     * 获取收付单据关联方式
     *
     * @return 收付单据关联方式
     */
	public Short getAssociationMode() {
	    return getShort("associationMode");
	}

    /**
     * 设置收付单据关联方式
     *
     * @param associationMode 收付单据关联方式
     */
	public void setAssociationMode(Short associationMode) {
		set("associationMode", associationMode);
	}

	/**
	 * 获取业务凭据匹配关联后是否要处理
	 *
	 * @return 业务凭据匹配关联后是否要处理
	 */
	public Short getReceiptRelIshandle() {
		return getShort("receiptRelIshandle");
	}

	/**
	 * 设置业务凭据匹配关联后是否要处理
	 *
	 * @param receiptRelIshandle 业务凭据匹配关联后是否要处理
	 */
	public void setReceiptRelIshandle(Short receiptRelIshandle) {
		set("receiptRelIshandle", receiptRelIshandle);
	}


	/**
     * 获取流程编码
     *
     * @return 流程编码
     */
	public String getCode() {
		return get("code");
	}

    /**
     * 设置流程编码
     *
     * @param code 流程编码
     */
	public void setCode(String code) {
		set("code", code);
	}

    /**
     * 获取生单方式
     *
     * @return 生单方式
     */
	public Short getCreateBillMode() {
	    return getShort("createBillMode");
	}

    /**
     * 设置生单方式
     *
     * @param createBillMode 生单方式
     */
	public void setCreateBillMode(Short createBillMode) {
		set("createBillMode", createBillMode);
	}

    /**
     * 获取生单单据范围
     *
     * @return 生单单据范围
     */
	public Short getCreateBillRange() {
	    return getShort("createBillRange");
	}

    /**
     * 设置生单单据范围
     *
     * @param createBillRange 生单单据范围
     */
	public void setCreateBillRange(Short createBillRange) {
		set("createBillRange", createBillRange);
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
     * 获取停用时间
     *
     * @return 停用时间
     */
	public java.util.Date getDisablets() {
		return get("disablets");
	}

    /**
     * 设置停用时间
     *
     * @param disablets 停用时间
     */
	public void setDisablets(java.util.Date disablets) {
		set("disablets", disablets);
	}


    /**
     * 获取启用
     *
     * @return 启用
     */
	public String getEnable() {
		return get("enable");
	}

    /**
     * 设置启用
     *
     * @param enable 启用
     */
	public void setEnable(String enable) {
		set("enable", enable);
	}

    /**
     * 获取启用时间
     *
     * @return 启用时间
     */
	public java.util.Date getEnablets() {
		return get("enablets");
	}

    /**
     * 设置启用时间
     *
     * @param enablets 启用时间
     */
	public void setEnablets(java.util.Date enablets) {
		set("enablets", enablets);
	}

    /**
     * 获取流程大类
     *
     * @return 流程大类
     */
	public Short getFlowType() {
	    return getShort("flowType");
	}

    /**
     * 设置流程大类
     *
     * @param flowType 流程大类
     */
	public void setFlowType(Short flowType) {
		set("flowType", flowType);
	}

    /**
     * 获取是否人工确认
     *
     * @return 是否人工确认
     */
	public Short getIsArtiConfirm() {
	    return getShort("isArtiConfirm");
	}

    /**
     * 设置是否人工确认
     *
     * @param isArtiConfirm 是否人工确认
     */
	public void setIsArtiConfirm(Short isArtiConfirm) {
		set("isArtiConfirm", isArtiConfirm);
	}

    /**
     * 获取多条是否自动确认
     *
     * @return 多条是否自动确认
     */
	public Short getIsRandomAutoConfirm() {
	    return getShort("isRandomAutoConfirm");
	}

    /**
     * 设置多条是否自动确认
     *
     * @param isRandomAutoConfirm 多条是否自动确认
     */
	public void setIsRandomAutoConfirm(Short isRandomAutoConfirm) {
		set("isRandomAutoConfirm", isRandomAutoConfirm);
	}

    /**
     * 获取是否预制 
     *
     * @return 是否预制 
     */
	public Short getIsSystem() {
	    return getShort("isSystem");
	}

    /**
     * 设置是否预制 
     *
     * @param isSystem 是否预制 
     */
	public void setIsSystem(Short isSystem) {
		set("isSystem", isSystem);
	}

    /**
     * 获取是否完结流程
     *
     * @return 是否完结流程
     */
	public Short getIsTerminationFlow() {
	    return getShort("isTerminationFlow");
	}

    /**
     * 设置是否完结流程
     *
     * @param isTerminationFlow 是否完结流程
     */
	public void setIsTerminationFlow(Short isTerminationFlow) {
		set("isTerminationFlow", isTerminationFlow);
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
     * 获取通知方式
     *
     * @return 通知方式
     */
	public String getMsgType() {
		return get("msgType");
	}

    /**
     * 设置通知方式
     *
     * @param msgType 通知方式
     */
	public void setMsgType(String msgType) {
		set("msgType", msgType);
	}

    /**
     * 获取流程名称
     *
     * @return 流程名称
     */
	public String getName() {
		return get("name");
	}

    /**
     * 设置流程名称
     *
     * @param name 流程名称
     */
	public void setName(String name) {
		set("name", name);
	}

    /**
     * 获取适用对象
     *
     * @return 适用对象
     */
	public Short getObject() {
	    return getShort("object");
	}

    /**
     * 设置适用对象
     *
     * @param object 适用对象
     */
	public void setObject(Short object) {
		set("object", object);
	}

	/**
	 * 获取适用组织
	 *
	 * @return 适用组织 .ID
	 */
	public String getAccentity() {
		return get("accentity");
	}

	/**
	 * 设置适用组织
	 *
	 * @param accentity 适用组织 .ID
	 */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

    /**
     * 获取发布认领方式
     *
     * @return 发布认领方式
     */
	public Short getPublishMode() {
	    return getShort("publishMode");
	}

    /**
     * 设置发布认领方式
     *
     * @param publishMode 发布认领方式
     */
	public void setPublishMode(Short publishMode) {
		set("publishMode", publishMode);
	}

    /**
     * 获取发布范围
     *
     * @return 发布范围
     */
	public Short getPublishRange() {
	    return getShort("publishRange");
	}

    /**
     * 设置发布范围
     *
     * @param publishRange 发布范围
     */
	public void setPublishRange(Short publishRange) {
		set("publishRange", publishRange);
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
     * 获取备注
     *
     * @return 备注
     */
	public String getRemark() {
		return get("remark");
	}

    /**
     * 设置备注
     *
     * @param remark 备注
     */
	public void setRemark(String remark) {
		set("remark", remark);
	}

    /**
     * 获取是否消息通知
     *
     * @return 是否消息通知
     */
	public Short getSendMsg() {
	    return getShort("sendMsg");
	}

    /**
     * 设置是否消息通知
     *
     * @param sendMsg 是否消息通知
     */
	public void setSendMsg(Short sendMsg) {
		set("sendMsg", sendMsg);
	}

    /**
     * 获取执行优先级
     *
     * @return 执行优先级
     */
	public Integer getSortNum() {
		return get("sortNum");
	}

    /**
     * 设置执行优先级
     *
     * @param sortNum 执行优先级
     */
	public void setSortNum(Integer sortNum) {
		set("sortNum", sortNum);
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
     * 获取部分无需确认范围
     *
     * @return 部分无需确认范围
     */
	public String getUnconfirmIds() {
		return get("unconfirmIds");
	}

    /**
     * 设置部分无需确认范围
     *
     * @param unconfirmIds 部分无需确认范围
     */
	public void setUnconfirmIds(String unconfirmIds) {
		set("unconfirmIds", unconfirmIds);
	}

    /**
     * 获取租户id
     *
     * @return 租户id.ID
     */
	public String getYTenant() {
		return get("ytenant");
	}

    /**
     * 设置租户id
     *
     * @param ytenant 租户id.ID
     */
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

    /**
     * 获取子表_关联单据范围集合
     *
     * @return 子表_关联单据范围集合
     */
	public java.util.List<FhsAssociationRange> fhsAssociationRangeList() {
		return getBizObjects("fhsAssociationRangeList", FhsAssociationRange.class);
	}

    /**
     * 设置子表_关联单据范围集合
     *
     * @param fhsAssociationRangeList 子表_关联单据范围集合
     */
	public void setFhsAssociationRangeList(java.util.List<FhsAssociationRange> fhsAssociationRangeList) {
		setBizObjects("fhsAssociationRangeList", fhsAssociationRangeList);
	}

    /**
     * 获取子表_关联业务凭据范围集合
     *
     * @return 子表_关联业务凭据范围集合
     */
	public java.util.List<FhsBillRange> fhsBillRangeList() {
		return getBizObjects("fhsBillRangeList", FhsBillRange.class);
	}

    /**
     * 设置子表_关联业务凭据范围集合
     *
     * @param fhsBillRangeList 子表_关联业务凭据范围集合
     */
	public void setFhsBillRangeList(java.util.List<FhsBillRange> fhsBillRangeList) {
		setBizObjects("fhsBillRangeList", fhsBillRangeList);
	}

    /**
     * 获取子表_手工生单范围集合
     *
     * @return 子表_手工生单范围集合
     */
	public java.util.List<FhsCreateBillRange> fhsCreateBillRangeList() {
		return getBizObjects("fhsCreateBillRangeList", FhsCreateBillRange.class);
	}

    /**
     * 设置子表_手工生单范围集合
     *
     * @param fhsCreateBillRangeList 子表_手工生单范围集合
     */
	public void setFhsCreateBillRangeList(java.util.List<FhsCreateBillRange> fhsCreateBillRangeList) {
		setBizObjects("fhsCreateBillRangeList", fhsCreateBillRangeList);
	}

    /**
     * 获取子表_手工发布范围集合
     *
     * @return 子表_手工发布范围集合
     */
	public java.util.List<FhsPublishRange> fhsPublishRangeList() {
		return getBizObjects("fhsPublishRangeList", FhsPublishRange.class);
	}

    /**
     * 设置子表_手工发布范围集合
     *
     * @param fhsPublishRangeList 子表_手工发布范围集合
     */
	public void setFhsPublishRangeList(java.util.List<FhsPublishRange> fhsPublishRangeList) {
		setBizObjects("fhsPublishRangeList", fhsPublishRangeList);
	}

}
