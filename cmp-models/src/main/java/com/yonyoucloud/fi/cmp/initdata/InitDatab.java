package com.yonyoucloud.fi.cmp.initdata;

import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

import java.math.BigDecimal;

/**
 * 资金付款子表实体
 *
 * @author u
 * @version 1.0
 */
public class InitDatab extends BizObject implements  ITenant, AccentityRawInterface {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.initdata.InitDatab";

	private static final long serialVersionUID = -3448627345333873327L;

	/**
     * 获取主表id
     *
     * @return 主表id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}


	/**
	 * 获取核算会计 主体
	 *
	 * @return 核算会计 主体.ID
	 */
	@Override
	public String getAccentityRaw() {
		return get("accentityRaw");
	}

	/**
	 * 设置核算会计 主体
	 *
	 * @param accentityRaw 核算会计 主体.ID
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
	 * 获取银行方期初本币余额
	 *
	 * @return 银行方期初本币余额
	 */
	public BigDecimal getBankinitlocalbalance() {
		return getBigDecimal("bankinitlocalbalance");
	}

	/**
	 * 设置银行方期初本币余额
	 *
	 * @param bankinitlocalbalance 银行方期初本币余额
	 */
	public void setBankinitlocalbalance(BigDecimal bankinitlocalbalance) {
		set("bankinitlocalbalance", bankinitlocalbalance);
	}

	/**
	 * 获取企业方期初本币余额
	 *
	 * @return 企业方期初本币余额
	 */
	public BigDecimal getCoinitlocalbalance() {
		return getBigDecimal("coinitlocalbalance");
	}

	/**
	 * 设置企业方期初本币余额
	 *
	 * @param coinitlocalbalance 企业方期初本币余额
	 */
	public void setCoinitlocalbalance(BigDecimal coinitlocalbalance) {
		set("coinitlocalbalance", coinitlocalbalance);
	}

	/**
	 * 获取企业方账面本币余额
	 *
	 * @return 企业方账面本币余额
	 */
	public BigDecimal getCobooklocalbalance() {
		return getBigDecimal("cobooklocalbalance");
	}

	/**
	 * 设置企业方账面本币余额
	 *
	 * @param cobooklocalbalance 企业方账面本币余额
	 */
	public void setCobooklocalbalance(BigDecimal cobooklocalbalance) {
		set("cobooklocalbalance", cobooklocalbalance);
	}

	/**
	 * 获取银行方期初原币余额
	 *
	 * @return 银行方期初原币余额
	 */
	public BigDecimal getBankinitoribalance() {
		return getBigDecimal("bankinitoribalance");
	}

	/**
	 * 设置银行方期初原币余额
	 *
	 * @param bankinitoribalance 银行方期初原币余额
	 */
	public void setBankinitoribalance(BigDecimal bankinitoribalance) {
		set("bankinitoribalance", bankinitoribalance);
	}

	/**
	 * 获取企业方期初原币余额
	 *
	 * @return 企业方期初原币余额
	 */
	public BigDecimal getCoinitloribalance() {
		return getBigDecimal("coinitloribalance");
	}

	/**
	 * 设置企业方期初原币余额
	 *
	 * @param coinitloribalance 企业方期初原币余额
	 */
	public void setCoinitloribalance(BigDecimal coinitloribalance) {
		set("coinitloribalance", coinitloribalance);
	}

	/**
	 * 获取企业方账面原币余额
	 *
	 * @return 企业方账面原币余额
	 */
	public BigDecimal getCobookoribalance() {
		return getBigDecimal("cobookoribalance");
	}

	/**
	 * 设置企业方账面原币余额
	 *
	 * @param cobookoribalance 企业方账面原币余额
	 */
	public void setCobookoribalance(BigDecimal cobookoribalance) {
		set("cobookoribalance", cobookoribalance);
	}

	/**
	 * 获取企业方余额方向
	 *
	 * @return 企业方余额方向
	 */
	public Direction getDirection() {
		String direction = get("direction").toString();
		Number v = Integer.parseInt(direction);;
		return Direction.find(v);
	}

	/**
	 * 设置企业方余额方向
	 *
	 * @param direction 企业方余额方向
	 */
	public void setDirection(Direction direction) {
		if (direction != null) {
			set("direction", direction.getValue());
		} else {
			set("direction", null);
		}
	}

	/**
	 * 获取银行方余额方向
	 *
	 * @return 银行方余额方向
	 */
	public Direction getBankdirection() {
		String bankdirection = get("bankdirection").toString();
		Number v = Integer.parseInt(bankdirection);
		return Direction.find(v);
	}

	/**
	 * 设置银行方余额方向
	 *
	 * @param bankdirection 银行方余额方向
	 */
	public void setBankdirection(Direction bankdirection) {
		if (bankdirection != null) {
			set("bankdirection", bankdirection.getValue());
		} else {
			set("bankdirection", null);
		}
	}

	/**
	 * 获取业务描述
	 *
	 * @return 业务描述
	 */
	public String getDescription() {
		return get("description");
	}

	/**
	 * 设置业务描述
	 *
	 * @param description 业务描述
	 */
	public void setDescription(String description) {
		set("description", description);
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


	/**
	 * 获取行号
	 *
	 * @return 行号
	 */
	public java.math.BigDecimal getLineno() {
		return get("lineno");
	}

	/**
	 * 设置行号
	 *
	 * @param lineno 行号
	 */
	public void setLineno(java.math.BigDecimal lineno) {
		set("lineno", lineno);
	}

}
