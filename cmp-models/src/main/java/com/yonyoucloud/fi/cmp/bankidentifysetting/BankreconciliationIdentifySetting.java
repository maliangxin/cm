package com.yonyoucloud.fi.cmp.bankidentifysetting;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.LogicDelete;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 银行流水辨识匹配规则实体
 *
 * @author u
 * @version 1.0
 */
public class BankreconciliationIdentifySetting extends BizObject implements IYTenant, ITenant, LogicDelete, IAuditInfo {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankidentifysetting.BankreconciliationIdentifySetting";

    /**
     * 获取适用组织
     *
     * @return 适用组织.ID
     */
	public String getAccentity() {
		return get("accentity");
	}

    /**
     * 设置适用组织
     *
     * @param accentity 适用组织.ID
     */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

    /**
     * 获取适用对象单据编码
     *
     * @return 适用对象单据编码
     */
	public String getApplybillnum() {
		return get("applybillnum");
	}

    /**
     * 设置适用对象单据编码
     *
     * @param applybillnum 适用对象单据编码
     */
	public void setApplybillnum(String applybillnum) {
		set("applybillnum", applybillnum);
	}


	/**
	 * 获取适用对象单据编码
	 *
	 * @return 适用对象单据编码
	 */
	public String getMulticaserule() {
		return get("multicaserule");
	}

	/**
	 * 设置适用对象单据编码
	 *
	 * @param applybillnum 适用对象单据编码
	 */
	public void setMulticaserule(String applybillnum) {
		set("multicaserule", applybillnum);
	}

    /**
     * 获取适用对象类型
     *
     * @return 适用对象类型
     */
	public Short getApplybilltype() {
	    return getShort("applybilltype");
	}

    /**
     * 设置适用对象类型
     *
     * @param applybilltype 适用对象类型
     */
	public void setApplybilltype(Short applybilltype) {
		set("applybilltype", applybilltype);
	}

    /**
     * 获取适用对象业务编码
     *
     * @return 适用对象业务编码
     */
	public String getApplybusicode() {
		return get("applybusicode");
	}

    /**
     * 设置适用对象业务编码
     *
     * @param applybusicode 适用对象业务编码
     */
	public void setApplybusicode(String applybusicode) {
		set("applybusicode", applybusicode);
	}

    /**
     * 获取适用对象描述
     *
     * @return 适用对象描述
     */
	public String getApplydes() {
		return get("applydes");
	}

    /**
     * 设置适用对象描述
     *
     * @param applydes 适用对象描述
     */
	public void setApplydes(String applydes) {
		set("applydes", applydes);
	}

	/**
	 * 获取适用对象单据领域
	 *
	 * @return 适用对象单据领域
	 */
	public String getApplydomain() {
		return get("applydomain");
	}

	/**
	 * 设置适用对象单据领域
	 *
	 * @param applydomain 适用对象单据领域
	 */
	public void setApplydomain(String applydomain) {
		set("applydomain", applydomain);
	}

    /**
     * 获取适用对象
     *
     * @return 适用对象
     */
	public Short getApplyobject() {
	    return getShort("applyobject");
	}

    /**
     * 设置适用对象
     *
     * @param applyobject 适用对象
     */
	public void setApplyobject(Short applyobject) {
		set("applyobject", applyobject);
	}

    /**
     * 获取银行类别
     *
     * @return 银行类别.ID
     */
	public String getBanktype() {
		return get("banktype");
	}

    /**
     * 设置银行类别
     *
     * @param banktype 银行类别.ID
     */
	public void setBanktype(String banktype) {
		set("banktype", banktype);
	}

	/**
	 * 获取资金数据池对应数据源id
	 *
	 * @return 资金数据池对应数据源id
	 */
	public String getCdp_id() {
		return get("cdp_id");
	}

	/**
	 * 设置资金数据池对应数据源id
	 *
	 * @param cdp_id 资金数据池对应数据源id
	 */
	public void setCdp_id(String cdp_id) {
		set("cdp_id", cdp_id);
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
     * 获取创建人
     *
     * @return 创建人.ID
     */
	public String getCreator() {
		return get("creator");
	}

    /**
     * 设置创建人
     *
     * @param creator 创建人.ID
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
     * 获取收付方向
     *
     * @return 收付方向
     */
	public Short getDc_flag() {
	    return getShort("dc_flag");
	}

    /**
     * 设置收付方向
     *
     * @param dc_flag 收付方向
     */
	public void setDc_flag(Short dc_flag) {
		set("dc_flag", dc_flag);
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
     * 获取逻辑删除标记
     *
     * @return 逻辑删除标记
     */
	public Short getDr() {
	    return getShort("dr");
	}

    /**
     * 设置逻辑删除标记
     *
     * @param dr 逻辑删除标记
     */
	public void setDr(Short dr) {
		set("dr", dr);
	}

    /**
     * 获取启用时间
     *
     * @return 启用时间
     */
	public java.util.Date getEnabledate() {
		return get("enabledate");
	}

    /**
     * 设置启用时间
     *
     * @param enabledate 启用时间
     */
	public void setEnabledate(java.util.Date enabledate) {
		set("enabledate", enabledate);
	}

    /**
     * 获取启用状态
     *
     * @return 启用状态
     */
	public Short getEnablestatus() {
	    return getShort("enablestatus");
	}

    /**
     * 设置启用状态
     *
     * @param enablestatus 启用状态
     */
	public void setEnablestatus(Short enablestatus) {
		set("enablestatus", enablestatus);
	}

    /**
     * 获取执行优先级
     *
     * @return 执行优先级
     */
	public Integer getExcutelevel() {
		return get("excutelevel");
	}

    /**
     * 设置执行优先级
     *
     * @param excutelevel 执行优先级
     */
	public void setExcutelevel(Integer excutelevel) {
		set("excutelevel", excutelevel);
	}

    /**
     * 获取规则大类
     *
     * @return 规则大类
     */
	public Short getIdentifytype() {
	    return getShort("identifytype");
	}

    /**
     * 设置规则大类
     *
     * @param identifytype 规则大类
     */
	public void setIdentifytype(Short identifytype) {
		set("identifytype", identifytype);
	}

    /**
     * 获取是否启用三方协议
     *
     * @return 是否启用三方协议
     */
	public Boolean getIsenabletriple() {
	    return getBoolean("isenabletriple");
	}

    /**
     * 设置是否启用三方协议
     *
     * @param isenabletriple 是否启用三方协议
     */
	public void setIsenabletriple(Boolean isenabletriple) {
		set("isenabletriple", isenabletriple);
	}

    /**
     * 获取是否预置
     *
     * @return 是否预置
     */
	public Boolean getIssystem() {
	    return getBoolean("issystem");
	}

    /**
     * 设置是否预置
     *
     * @param issystem 是否预置
     */
	public void setIssystem(Boolean issystem) {
		set("issystem", issystem);
	}

    /**
     * 获取匹配对象单据编码
     *
     * @return 匹配对象单据编码
     */
	public String getMatchbillnum() {
		return get("matchbillnum");
	}

    /**
     * 设置匹配对象单据编码
     *
     * @param matchbillnum 匹配对象单据编码
     */
	public void setMatchbillnum(String matchbillnum) {
		set("matchbillnum", matchbillnum);
	}

    /**
     * 获取匹配对象类型
     *
     * @return 匹配对象类型
     */
	public Short getMatchbilltype() {
	    return getShort("matchbilltype");
	}

    /**
     * 设置匹配对象类型
     *
     * @param matchbilltype 匹配对象类型
     */
	public void setMatchbilltype(Short matchbilltype) {
		set("matchbilltype", matchbilltype);
	}

    /**
     * 获取匹配业务对象编码
     *
     * @return 匹配业务对象编码
     */
	public String getMatchbusicode() {
		return get("matchbusicode");
	}

    /**
     * 设置匹配业务对象编码
     *
     * @param matchbusicode 匹配业务对象编码
     */
	public void setMatchbusicode(String matchbusicode) {
		set("matchbusicode", matchbusicode);
	}

    /**
     * 获取匹配对象描述
     *
     * @return 匹配对象描述
     */
	public String getMatchdes() {
		return get("matchdes");
	}

    /**
     * 设置匹配对象描述
     *
     * @param matchdes 匹配对象描述
     */
	public void setMatchdes(String matchdes) {
		set("matchdes", matchdes);
	}

	/**
	 * 获取匹配对象单据领域
	 *
	 * @return 匹配对象单据领域
	 */
	public String getMatchdomain() {
		return get("matchdomain");
	}

	/**
	 * 设置匹配对象单据领域
	 *
	 * @param matchdomain 匹配对象单据领域
	 */
	public void setMatchdomain(String matchdomain) {
		set("matchdomain", matchdomain);
	}

    /**
     * 获取匹配对象
     *
     * @return 匹配对象
     */
	public String getMatchobject() {
		return get("matchobject");
	}

    /**
     * 设置匹配对象
     *
     * @param matchobject 匹配对象
     */
	public void setMatchobject(String matchobject) {
		set("matchobject", matchobject);
	}

	/**
	 * 获取匹配对象id
	 *
	 * @return 匹配对象id
	 */
	public String getMatchobjectid() {
		return get("matchobjectid");
	}

	/**
	 * 设置匹配对象id
	 *
	 * @param matchobjectid 匹配对象id
	 */
	public void setMatchobjectid(String matchobjectid) {
		set("matchobjectid", matchobjectid);
	}

    /**
     * 获取修改人
     *
     * @return 修改人.ID
     */
	public String getModifier() {
		return get("modifier");
	}

    /**
     * 设置修改人
     *
     * @param modifier 修改人.ID
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
     * 获取规则名称
     *
     * @return 规则名称
     */
	public String getName() {
		return get("name");
	}

    /**
     * 设置规则名称
     *
     * @param name 规则名称
     */
	public void setName(String name) {
		set("name", name);
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
     * 获取规则属性
     *
     * @return 规则属性
     */
	public Short getRuleattribute() {
	    return getShort("ruleattribute");
	}

    /**
     * 设置规则属性
     *
     * @param ruleattribute 规则属性
     */
	public void setRuleattribute(Short ruleattribute) {
		set("ruleattribute", ruleattribute);
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
     * 获取停用时间
     *
     * @return 停用时间
     */
	public java.util.Date getStopdate() {
		return get("stopdate");
	}

    /**
     * 设置停用时间
     *
     * @param stopdate 停用时间
     */
	public void setStopdate(java.util.Date stopdate) {
		set("stopdate", stopdate);
	}

    /**
     * 获取是否完结流程
     *
     * @return 是否完结流程
     */
	public Boolean getStoptag() {
	    return getBoolean("stoptag");
	}

    /**
     * 设置是否完结流程
     *
     * @param stoptag 是否完结流程
     */
	public void setStoptag(Boolean stoptag) {
		set("stoptag", stoptag);
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
     * 获取银行流水辨识匹配规则子表集合
     *
     * @return 银行流水辨识匹配规则子表集合
     */
	public java.util.List<BankreconciliationIdentifySetting_b> BankreconciliationIdentifySetting_b() {
		return getBizObjects("BankreconciliationIdentifySetting_b", BankreconciliationIdentifySetting_b.class);
	}

    /**
     * 设置银行流水辨识匹配规则子表集合
     *
     * @param BankreconciliationIdentifySetting_b 银行流水辨识匹配规则子表集合
     */
	public void setBankreconciliationIdentifySetting_b(java.util.List<BankreconciliationIdentifySetting_b> BankreconciliationIdentifySetting_b) {
		setBizObjects("BankreconciliationIdentifySetting_b", BankreconciliationIdentifySetting_b);
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
	 * 获取流水处理规则id
	 *
	 * @return 流水处理规则id.ID
	 */
	public Long getFlow_id() {
		return get("flow_id");
	}

	/**
	 * 设置流水处理规则id
	 *
	 * @param flow_id 流水处理规则id.ID
	 */
	public void setFlow_id(Long flow_id) {
		set("flow_id", flow_id);
	}


	/**
	 * 启用用户
	 *
	 * @return 启用用户
	 */
	public String getEnable_user() {
		return get("enable_user");
	}

	/**
	 * 启用用户
	 *
	 * @param enable_user 启用用户
	 */
	public void setEnable_user(String enable_user) {
		set("enable_user", enable_user);
	}

	/**
	 * 启用用户
	 *
	 * @return 启用用户
	 */
	public String getStop_user() {
		return get("stop_user");
	}

	/**
	 * 启用用户
	 *
	 * @param stop_user 启用用户
	 */
	public void setStop_user(String stop_user) {
		set("stop_user", stop_user);
	}

}
