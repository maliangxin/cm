package com.yonyoucloud.fi.cmp.withholding;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 协定存款分档子表实体
 *
 * @author u
 * @version 1.0
 */
public class AgreeIRGrade extends BizObject implements IAuditInfo, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.withholding.AgreeIRGrade";

    /**
     * 获取金额
     *
     * @return 金额
     */
	public java.math.BigDecimal getAmount() {
		return getBigDecimal("amount");
	}

    /**
     * 设置金额
     *
     * @param amount 金额
     */
	public void setAmount(java.math.BigDecimal amount) {
		set("amount", amount);
	}

    /**
     * 获取基准利率%
     *
     * @return 基准利率%
     */
	public java.math.BigDecimal getBaseir() {
		return getBigDecimal("baseir");
	}

    /**
     * 设置基准利率%
     *
     * @param baseir 基准利率%
     */
	public void setBaseir(java.math.BigDecimal baseir) {
		set("baseir", baseir);
	}

    /**
     * 获取基准利率类型
     *
     * @return 基准利率类型.ID
     */
	public Long getBaseirtype() {
		return getLong("baseirtype");
	}

    /**
     * 设置基准利率类型
     *
     * @param baseirtype 基准利率类型.ID
     */
	public void setBaseirtype(Long baseirtype) {
		set("baseirtype", baseirtype);
	}

    /**
     * 获取基准利率类型币种
     *
     * @return 基准利率类型币种.ID
     */
	public String getBaseirtypecurrency() {
		return getString("baseirtypecurrency");
	}

    /**
     * 设置基准利率类型币种
     *
     * @param baseirtypecurrency 基准利率类型币种.ID
     */
	public void setBaseirtypecurrency(String baseirtypecurrency) {
		set("baseirtypecurrency", baseirtypecurrency);
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
     * 获取浮动值(加减点)%
     *
     * @return 浮动值(加减点)%
     */
	public java.math.BigDecimal getFloatvalue() {
		return getBigDecimal("floatvalue");
	}

    /**
     * 设置浮动值(加减点)%
     *
     * @param floatvalue 浮动值(加减点)%
     */
	public void setFloatvalue(java.math.BigDecimal floatvalue) {
		set("floatvalue", floatvalue);
	}

    /**
     * 获取分档编号
     *
     * @return 分档编号
     */
	public Long getGradenum() {
		return getLong("gradenum");
	}

    /**
     * 设置分档编号
     *
     * @param gradenum 分档编号
     */
	public void setGradenum(Long gradenum) {
		set("gradenum", gradenum);
	}

    /**
     * 获取分档选项
     *
     * @return 分档选项
     */
	public Short getGradeoption() {
	    return getShort("gradeoption");
	}

    /**
     * 设置分档选项
     *
     * @param gradeoption 分档选项
     */
	public void setGradeoption(Short gradeoption) {
		set("gradeoption", gradeoption);
	}

    /**
     * 获取利率%
     *
     * @return 利率%
     */
	public java.math.BigDecimal getInterestrate() {
		return getBigDecimal("interestrate");
	}

    /**
     * 设置利率%
     *
     * @param interestrate 利率%
     */
	public void setInterestrate(java.math.BigDecimal interestrate) {
		set("interestrate", interestrate);
	}

    /**
     * 获取利率类型
     *
     * @return 利率类型
     */
	public Short getIrtype() {
	    return getShort("irtype");
	}

    /**
     * 设置利率类型
     *
     * @param irtype 利率类型
     */
	public void setIrtype(Short irtype) {
		set("irtype", irtype);
	}

    /**
     * 获取预提规则设置id
     *
     * @return 预提规则设置id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置预提规则设置id
     *
     * @param mainid 预提规则设置id.ID
     */
	public void setMainid(Long mainid) {
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
