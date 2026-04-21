package com.yonyoucloud.fi.cmp.bankautocheckconfig;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 自动对账设置实体
 *
 * @author u
 * @version 1.0
 */
public class BankAutoCheckConfig extends BizObject implements IYTenant,IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankautocheckconfig.BankAutoCheckConfig";

    /**
     * 获取是否本方账号相同
     *
     * @return 是否本方账号相同
     */
	public Boolean getAccountflag() {
	    return getBoolean("accountflag");
	}

    /**
     * 设置是否本方账号相同
     *
     * @param accountflag 是否本方账号相同
     */
	public void setAccountflag(Boolean accountflag) {
		set("accountflag", accountflag);
	}

    /**
     * 获取是否币种相同
     *
     * @return 是否币种相同
     */
	public Boolean getCurrencyflag() {
	    return getBoolean("currencyflag");
	}

    /**
     * 设置是否币种相同
     *
     * @param currencyflag 是否币种相同
     */
	public void setCurrencyflag(Boolean currencyflag) {
		set("currencyflag", currencyflag);
	}

    /**
     * 获取是否金额相同
     *
     * @return 是否金额相同
     */
	public Boolean getAmountflag() {
	    return getBoolean("amountflag");
	}

    /**
     * 设置是否金额相同
     *
     * @param amountflag 是否金额相同
     */
	public void setAmountflag(Boolean amountflag) {
		set("amountflag", amountflag);
	}

    /**
     * 获取是否借贷方向相反
     *
     * @return 是否借贷方向相反
     */
	public Boolean getDirectionflag() {
	    return getBoolean("directionflag");
	}

    /**
     * 设置是否借贷方向相反
     *
     * @param directionflag 是否借贷方向相反
     */
	public void setDirectionflag(Boolean directionflag) {
		set("directionflag", directionflag);
	}

    /**
     * 获取浮动天数
     *
     * @return 浮动天数
     */
	public Integer getChangedays() {
		return get("changedays");
	}

    /**
     * 设置浮动天数
     *
     * @param changedays 浮动天数
     */
	public void setChangedays(Integer changedays) {
		set("changedays", changedays);
	}

    /**
     * 获取是否对方账号相同
     *
     * @return 是否对方账号相同
     */
	public Boolean getToaccountflag() {
	    return getBoolean("toaccountflag");
	}

    /**
     * 设置是否对方账号相同
     *
     * @param toaccountflag 是否对方账号相同
     */
	public void setToaccountflag(Boolean toaccountflag) {
		set("toaccountflag", toaccountflag);
	}

    /**
     * 获取摘要匹配方式
     *
     * @return 摘要匹配方式
     */
	public Short getRemarkmatch() {
	    return getShort("remarkmatch");
	}

    /**
     * 设置摘要匹配方式
     *
     * @param remarkmatch 摘要匹配方式
     */
	public void setRemarkmatch(Short remarkmatch) {
		set("remarkmatch", remarkmatch);
	}

    /**
     * 获取是否银行对账码相同
     *
     * @return 是否银行对账码相同
     */
	public Boolean getBankchecknoflag() {
	    return getBoolean("bankchecknoflag");
	}

    /**
     * 设置是否银行对账码相同
     *
     * @param bankchecknoflag 是否银行对账码相同
     */
	public void setBankchecknoflag(Boolean bankchecknoflag) {
		set("bankchecknoflag", bankchecknoflag);
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
	 * 获取用户的id
	 *
	 * @return 用户的id
	 */
	public String getUserId() {
		return get("userId");
	}

	/**
	 * 设置用户的id
	 *
	 * @param userId 用户的id
	 */
	public void setUserId(String userId) {
		set("userId", userId);
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
	 * 获取是否按关键要素对账
	 *
	 * @return 是否按关键要素对账
	 */
	public Short getKeyElementMatchFlag() {
		return getShort("keyElementMatchFlag");
	}

	/**
	 * 设置是否按关键要素对账
	 *
	 * @param keyElementMatchFlag 关键要素-票据号匹配方式
	 */
	public void setKeyElementMatchFlag(Short keyElementMatchFlag) {
		set("keyElementMatchFlag", keyElementMatchFlag);
	}

	/**
	 * 获取关键要素-票据号匹配方式
	 *
	 * @return 关键要素-票据号匹配方式
	 */
	public Short getNotenoMatchMethod() {
		return getShort("notenoMatchMethod");
	}

	/**
	 * 设置关键要素-票据号匹配方式
	 *
	 * @param notenoMatchMethod 关键要素-票据号匹配方式
	 */
	public void setNotenoMatchMethod(Short notenoMatchMethod) {
		set("notenoMatchMethod", notenoMatchMethod);
	}

	/**
	 * 获取关键要素-对方名称匹配方式
	 *
	 * @return 关键要素-对方名称匹配方式
	 */
	public Short getOthernameMatchMethod() {
		return getShort("othernameMatchMethod");
	}

	/**
	 * 设置关键要素-对方名称匹配方式
	 *
	 * @param othernameMatchMethod 关键要素-对方名称匹配方式
	 */
	public void setOthernameMatchMethod(Short othernameMatchMethod) {
		set("othernameMatchMethod", othernameMatchMethod);
	}

	/**
	 * 获取关键要素-相同数据匹配方式
	 *
	 * @return 关键要素-相同数据匹配方式
	 */
	public Short getSamedataMatchMethod() {
		return getShort("samedataMatchMethod");
	}

	/**
	 * 设置关键要素-相同数据匹配方式
	 *
	 * @param samedataMatchMethod 关键要素-相同数据匹配方式
	 */
	public void setSamedataMatchMethod(Short samedataMatchMethod) {
		set("samedataMatchMethod", samedataMatchMethod);
	}

}
