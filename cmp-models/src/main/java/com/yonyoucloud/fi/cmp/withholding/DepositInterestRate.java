package com.yonyoucloud.fi.cmp.withholding;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 存款利率管理实体
 *
 * @author u
 * @version 1.0
 */
public class DepositInterestRate extends BizObject implements IAuditInfo, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "tlm.depositinterestrate.DepositInterestRate";

    /**
     * 获取银行类别id
     *
     * @return 银行类别id.ID
     */
	public String getBank() {
		return get("bank");
	}

    /**
     * 设置银行类别id
     *
     * @param bank 银行类别id.ID
     */
	public void setBank(String bank) {
		set("bank", bank);
	}

    /**
     * 获取利率类型id
     *
     * @return 利率类型id.ID
     */
	public Long getRateType() {
		return get("rateType");
	}

    /**
     * 设置利率类型id
     *
     * @param rateType 利率类型id.ID
     */
	public void setRateType(Long rateType) {
		set("rateType", rateType);
	}

    /**
     * 获取最近一次利率（%）
     *
     * @return 最近一次利率（%）
     */
	public String getLatestRate() {
		return get("latestRate");
	}

    /**
     * 设置最近一次利率（%）
     *
     * @param latestRate 最近一次利率（%）
     */
	public void setLatestRate(String latestRate) {
		set("latestRate", latestRate);
	}

    /**
     * 获取利率(%)
     *
     * @return 利率(%)
     */
	public java.math.BigDecimal getRate() {
		return get("rate");
	}

    /**
     * 设置利率(%)
     *
     * @param rate 利率(%)
     */
	public void setRate(java.math.BigDecimal rate) {
		set("rate", rate);
	}

    /**
     * 获取利率日期
     *
     * @return 利率日期
     */
	public java.util.Date getRateDate() {
		return get("rateDate");
	}

    /**
     * 设置利率日期
     *
     * @param rateDate 利率日期
     */
	public void setRateDate(java.util.Date rateDate) {
		set("rateDate", rateDate);
	}

    /**
     * 获取更新时间
     *
     * @return 更新时间
     */
	public java.util.Date getUpdateDate() {
		return get("updateDate");
	}

    /**
     * 设置更新时间
     *
     * @param updateDate 更新时间
     */
	public void setUpdateDate(java.util.Date updateDate) {
		set("updateDate", updateDate);
	}

    /**
     * 获取更新人
     *
     * @return 更新人
     */
	public String getUpdateUser() {
		return get("updateUser");
	}

    /**
     * 设置更新人
     *
     * @param updateUser 更新人
     */
	public void setUpdateUser(String updateUser) {
		set("updateUser", updateUser);
	}

    /**
     * 获取创建人
     *
     * @return 创建人
     */
	public String getCreator() {
		return get("creator");
	}

    /**
     * 设置创建人
     *
     * @param creator 创建人
     */
	public void setCreator(String creator) {
		set("creator", creator);
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
     * 获取修改人
     *
     * @return 修改人
     */
	public String getModifier() {
		return get("modifier");
	}

    /**
     * 设置修改人
     *
     * @param modifier 修改人
     */
	public void setModifier(String modifier) {
		set("modifier", modifier);
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
     * 获取名称
     *
     * @return 名称
     */
	public String getName() {
		return get("name");
	}

    /**
     * 设置名称
     *
     * @param name 名称
     */
	public void setName(String name) {
		set("name", name);
	}

    /**
     * 获取存款利率管理自定义项
     *
     * @return 存款利率管理自定义项.ID
     */
	public String getDepositInterestRateCharacterDef() {
		return get("depositInterestRateCharacterDef");
	}

    /**
     * 设置存款利率管理自定义项
     *
     * @param depositInterestRateCharacterDef 存款利率管理自定义项.ID
     */
	public void setDepositInterestRateCharacterDef(String depositInterestRateCharacterDef) {
		set("depositInterestRateCharacterDef", depositInterestRateCharacterDef);
	}

    /**
     * 获取生成方式
     *
     * @return 生成方式
     */
	public String getCreateMethod() {
		return get("createMethod");
	}

    /**
     * 设置生成方式
     *
     * @param createMethod 生成方式
     */
	public void setCreateMethod(String createMethod) {
		set("createMethod", createMethod);
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
     * 获取包含历史数据
     *
     * @return 包含历史数据
     */
	public String getIncludeHistory() {
		return get("includeHistory");
	}

    /**
     * 设置包含历史数据
     *
     * @param includeHistory 包含历史数据
     */
	public void setIncludeHistory(String includeHistory) {
		set("includeHistory", includeHistory);
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
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

}
