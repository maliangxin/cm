package com.yonyoucloud.fi.cmp.initdata;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import org.imeta.orm.base.BizObject;

/**
 * 期初未达项子表实体
 *
 * @author u
 * @version 1.0
 */
public class OpeningOutstanding_b extends BizObject implements IAuditInfo, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.openingOutstanding.OpeningOutstanding_b";

    /**
     * 获取银行方余额方向
     *
     * @return 银行方余额方向
     */
	public Direction getBankdirection() {
		return get("bankdirection");
	}

    /**
     * 设置银行方余额方向
     *
     * @param bankdirection 银行方余额方向
     */
	public void setBankdirection(Direction bankdirection) {
		set("bankdirection", bankdirection);
	}

    /**
     * 获取银行方期初原币余额
     *
     * @return 银行方期初原币余额
     */
	public java.math.BigDecimal getBankinitoribalance() {
		return get("bankinitoribalance");
	}

    /**
     * 设置银行方期初原币余额
     *
     * @param bankinitoribalance 银行方期初原币余额
     */
	public void setBankinitoribalance(java.math.BigDecimal bankinitoribalance) {
		set("bankinitoribalance", bankinitoribalance);
	}

    /**
     * 获取企业方期初原币余额
     *
     * @return 企业方期初原币余额
     */
	public java.math.BigDecimal getCoinitloribalance() {
		return get("coinitloribalance");
	}

    /**
     * 设置企业方期初原币余额
     *
     * @param coinitloribalance 企业方期初原币余额
     */
	public void setCoinitloribalance(java.math.BigDecimal coinitloribalance) {
		set("coinitloribalance", coinitloribalance);
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
     * 获取企业方余额方向
     *
     * @return 企业方余额方向
     */
	public Direction getDirection() {
		return get("direction");
	}

    /**
     * 设置企业方余额方向
     *
     * @param direction 企业方余额方向
     */
	public void setDirection(Direction direction) {
		set("direction", direction);
	}

    /**
     * 获取期初未达项id
     *
     * @return 期初未达项id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置期初未达项id
     *
     * @param mainid 期初未达项id.ID
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
     * 获取授权使用组织
     *
     * @return 授权使用组织.ID
     */
	public String getUseOrg() {
		return get("useOrg");
	}

    /**
     * 设置授权使用组织
     *
     * @param useOrg 授权使用组织.ID
     */
	public void setUseOrg(String useOrg) {
		set("useOrg", useOrg);
	}

	/**
	 * 获取核算账簿
	 *
	 * @return 核算账簿
	 */
	public String getAccountBook() {
		return get("accountBook");
	}

	/**
	 * 设置核算账簿
	 *
	 * @param accountBook 核算账簿
	 */
	public void setAccountBook(String accountBook) {
		set("accountBook", accountBook);
	}

}
