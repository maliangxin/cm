package com.yonyoucloud.fi.cmp.settlement;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 日结实体
 *
 * @author u
 * @version 1.0
 */
public class Settlement extends BizObject implements IAuditInfo, ITenant, AccentityRawInterface {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.settlement.Settlement";

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
     * 获取会计期间
     *
     * @return 会计期间.ID
     */
	public Long getPeriod() {
		return get("period");
	}

    /**
     * 设置会计期间
     *
     * @param period 会计期间.ID
     */
	public void setPeriod(Long period) {
		set("period", period);
	}

    /**
     * 获取会计期间编码
     *
     * @return 会计期间编码
     */
	public String getPeriodcode() {
		return get("periodcode");
	}

    /**
     * 设置会计期间编码
     *
     * @param periodcode 会计期间编码
     */
	public void setPeriodcode(String periodcode) {
		set("periodcode", periodcode);
	}

    /**
     * 获取日期
     *
     * @return 日期
     */
	public java.util.Date getSettlementdate() {
		return get("settlementdate");
	}

    /**
     * 设置日期
     *
     * @param settlementdate 日期
     */
	public void setSettlementdate(java.util.Date settlementdate) {
		set("settlementdate", settlementdate);
	}

    /**
     * 获取日结标志
     *
     * @return 日结标志
     */
	public Boolean getSettleflag() {
	    return getBoolean("settleflag");
	}

    /**
     * 设置日结标志
     *
     * @param settleflag 日结标志
     */
	public void setSettleflag(Boolean settleflag) {
		set("settleflag", settleflag);
	}

    /**
     * 获取结账人
     *
     * @return 结账人.ID
     */
	public Long getSettleman() {
		return get("settleman");
	}

    /**
     * 设置结账人
     *
     * @param settleman 结账人.ID
     */
	public void setSettleman(Long settleman) {
		set("settleman", settleman);
	}

    /**
     * 获取结账时间
     *
     * @return 结账时间
     */
	public java.util.Date getSettledate() {
		return get("settledate");
	}

    /**
     * 设置结账时间
     *
     * @param settledate 结账时间
     */
	public void setSettledate(java.util.Date settledate) {
		set("settledate", settledate);
	}

    /**
     * 获取取消结账人
     *
     * @return 取消结账人.ID
     */
	public Long getUnsettleman() {
		return get("unsettleman");
	}

    /**
     * 设置取消结账人
     *
     * @param unsettleman 取消结账人.ID
     */
	public void setUnsettleman(Long unsettleman) {
		set("unsettleman", unsettleman);
	}

    /**
     * 获取取消结账时间
     *
     * @return 取消结账时间
     */
	public java.util.Date getUnsettledate() {
		return get("unsettledate");
	}

    /**
     * 设置取消结账时间
     *
     * @param unsettledate 取消结账时间
     */
	public void setUnsettledate(java.util.Date unsettledate) {
		set("unsettledate", unsettledate);
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

}
