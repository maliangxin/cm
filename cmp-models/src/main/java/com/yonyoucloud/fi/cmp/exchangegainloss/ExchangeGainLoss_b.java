package com.yonyoucloud.fi.cmp.exchangegainloss;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.voucher.base.VouchLine;

/**
 * 汇兑损益子表实体
 *
 * @author u
 * @version 1.0
 */
public class ExchangeGainLoss_b extends VouchLine implements IAuditInfo {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.exchangegainloss.ExchangeGainLoss_b";

    /**
     * 获取汇兑损益id
     *
     * @return 汇兑损益id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置汇兑损益id
     *
     * @param mainid 汇兑损益id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

    /**
     * 获取银行账户
     *
     * @return 银行账户.ID
     */
	public String getBankaccount() {
		return get("bankaccount");
	}

    /**
     * 设置银行账户
     *
     * @param bankaccount 银行账户.ID
     */
	public void setBankaccount(String bankaccount) {
		set("bankaccount", bankaccount);
	}

    /**
     * 获取现金账户
     *
     * @return 现金账户.ID
     */
	public String getCashaccount() {
		return get("cashaccount");
	}

    /**
     * 设置现金账户
     *
     * @param cashaccount 现金账户.ID
     */
	public void setCashaccount(String cashaccount) {
		set("cashaccount", cashaccount);
	}

    /**
     * 获取币种
     *
     * @return 币种.ID
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置币种
     *
     * @param currency 币种.ID
     */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

    /**
     * 获取原币余额
     *
     * @return 原币余额
     */
	public java.math.BigDecimal getOribalance() {
		return get("oribalance");
	}

    /**
     * 设置原币余额
     *
     * @param oribalance 原币余额
     */
	public void setOribalance(java.math.BigDecimal oribalance) {
		set("oribalance", oribalance);
	}

    /**
     * 获取本币余额
     *
     * @return 本币余额
     */
	public java.math.BigDecimal getLocalbalance() {
		return get("localbalance");
	}

    /**
     * 设置本币余额
     *
     * @param localbalance 本币余额
     */
	public void setLocalbalance(java.math.BigDecimal localbalance) {
		set("localbalance", localbalance);
	}

    /**
     * 获取调整后汇率
     *
     * @return 调整后汇率
     */
	public java.math.BigDecimal getExchangerate() {
		return get("exchangerate");
	}

    /**
     * 设置调整后汇率
     *
     * @param exchangerate 调整后汇率
     */
	public void setExchangerate(java.math.BigDecimal exchangerate) {
		set("exchangerate", exchangerate);
	}

    /**
     * 获取调整后汇率折算方式
     *
     * @return 调整后汇率折算方式
     */
    public Short getExchangerateOps() {
        return getShort("exchangerateOps");
    }

    /**
     * 设置调整后汇率折算方式
     *
     * @param exchangerateOps 调整后汇率折算方式
     */
    public void setExchangerateOps(Short exchangerateOps) {
        set("exchangerateOps", exchangerateOps);
    }

    /**
     * 获取调整后本币余额
     *
     * @return 调整后本币余额
     */
	public java.math.BigDecimal getAdjustlocalbalance() {
		return get("adjustlocalbalance");
	}

    /**
     * 设置调整后本币余额
     *
     * @param adjustlocalbalance 调整后本币余额
     */
	public void setAdjustlocalbalance(java.math.BigDecimal adjustlocalbalance) {
		set("adjustlocalbalance", adjustlocalbalance);
	}

    /**
     * 获取调整后差额
     *
     * @return 调整后差额
     */
	public java.math.BigDecimal getAdjustbalance() {
		return get("adjustbalance");
	}

    /**
     * 设置调整后差额
     *
     * @param adjustbalance 调整后差额
     */
	public void setAdjustbalance(java.math.BigDecimal adjustbalance) {
		set("adjustbalance", adjustbalance);
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
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getCharacterDefb() {
		return get("characterDefb");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDefb 自定义项特征属性组.ID
	 */
	public void setCharacterDefb(String characterDefb) {
		set("characterDefb", characterDefb);
	}
}
