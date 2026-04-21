package com.yonyoucloud.fi.cmp.journal;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 日记账实体
 *
 * @author u
 * @version 1.0
 */
public class BalanceadjustJournal extends BizObject implements IAuditInfo, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.balanceadjustresult.BalanceadjustJournal";


	/**
	 * 获取资金组织
	 *
	 * @return 资金组织.ID
	 */
	public String getAccentity() {
		return get("accentity");
	}

	/**
	 * 设置资金组织
	 *
	 * @param accentity 资金组织.ID
	 */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
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
     * 获取日记账日期
     *
     * @return 日记账日期
     */
	public java.util.Date getDzdate() {
		return get("dzdate");
	}

    /**
     * 设置日记账日期
     *
     * @param dzdate 日记账日期
     */
	public void setDzdate(java.util.Date dzdate) {
		set("dzdate", dzdate);
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
     * 获取摘要
     *
     * @return 摘要
     */
	public String getDescription() {
		return get("description");
	}

    /**
     * 设置摘要
     *
     * @param description 摘要
     */
	public void setDescription(String description) {
		set("description", description);
	}

    /**
     * 获取借方原币金额
     *
     * @return 借方原币金额
     */
	public java.math.BigDecimal getDebitoriSum() {
		return get("debitoriSum");
	}

    /**
     * 设置借方原币金额
     *
     * @param debitoriSum 借方原币金额
     */
	public void setDebitoriSum(java.math.BigDecimal debitoriSum) {
		set("debitoriSum", debitoriSum);
	}

    /**
     * 获取贷方原币金额
     *
     * @return 贷方原币金额
     */
	public java.math.BigDecimal getCreditoriSum() {
		return get("creditoriSum");
	}

    /**
     * 设置贷方原币金额
     *
     * @param creditoriSum 贷方原币金额
     */
	public void setCreditoriSum(java.math.BigDecimal creditoriSum) {
		set("creditoriSum", creditoriSum);
	}

    /**
     * 获取是否已勾对
     *
     * @return 是否已勾对
     */
	public Boolean getCheckflag() {
	    return getBoolean("checkflag");
	}

    /**
     * 设置是否已勾对
     *
     * @param checkflag 是否已勾对
     */
	public void setCheckflag(Boolean checkflag) {
		set("checkflag", checkflag);
	}

    /**
     * 获取勾对人
     *
     * @return 勾对人.ID
     */
	public Long getCheckman() {
		return get("checkman");
	}

    /**
     * 设置勾对人
     *
     * @param checkman 勾对人.ID
     */
	public void setCheckman(Long checkman) {
		set("checkman", checkman);
	}

    /**
     * 获取勾对日期
     *
     * @return 勾对日期
     */
	public java.util.Date getCheckdate() {
		return get("checkdate");
	}

    /**
     * 设置勾对日期
     *
     * @param checkdate 勾对日期
     */
	public void setCheckdate(java.util.Date checkdate) {
		set("checkdate", checkdate);
	}

    /**
     * 获取勾对号
     *
     * @return 勾对号
     */
	public String getCheckno() {
		return get("checkno");
	}

    /**
     * 设置勾对号
     *
     * @param checkno 勾对号
     */
	public void setCheckno(String checkno) {
		set("checkno", checkno);
	}

	/**
	 * 获取事项来源
	 *
	 * @return 事项来源
	 */
	public EventSource getSrcitem() {
		Number v = get("srcitem");
		return EventSource.find(v);
	}

	/**
	 * 设置事项来源
	 *
	 * @param srcitem 事项来源
	 */
	public void setSrcitem(EventSource srcitem) {
		if (srcitem != null) {
			set("srcitem", srcitem.getValue());
		} else {
			set("srcitem", null);
		}
	}

	/**
	 * 获取单据编号
	 *
	 * @return 单据编号
	 */
	public String getBillnum() {
		return get("billnum");
	}

	/**
	 * 设置单据编号
	 *
	 * @param billnum 单据编号
	 */
	public void setBillnum(String billnum) {
		set("billnum", billnum);
	}

	/**
	 * 获取余额调节表id
	 *
	 * @return 余额调节表.ID
	 */
	public Long getBalanceadjustresultid() {
		return get("balanceadjustresultid");
	}

	/**
	 * 设置余额调节表id
	 *
	 * @param balanceadjustresultid 余额调节表..ID
	 */
	public void setBalanceadjustresultid(Long balanceadjustresultid) {
		set("balanceadjustresultid", balanceadjustresultid);
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
}
