package com.yonyoucloud.fi.cmp.initdata;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import org.imeta.orm.base.BizObject;

/**
 * 期初未达项实体
 *
 * @author u
 * @version 1.0
 */
public class OpeningOutstanding extends BizObject implements IAuditInfo, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.openingOutstanding.OpeningOutstanding";

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
     * 获取对账方案
     *
     * @return 对账方案.ID
     */
	public Long getBankreconciliationscheme() {
		return get("bankreconciliationscheme");
	}

    /**
     * 设置对账方案
     *
     * @param bankreconciliationscheme 对账方案.ID
     */
	public void setBankreconciliationscheme(Long bankreconciliationscheme) {
		set("bankreconciliationscheme", bankreconciliationscheme);
	}

	/**
	 * 获取对账方案明细id
	 *
	 * @return 对账方案明细id
	 */
	public String getBankreconciliationscheme_b() {
		return get("bankreconciliationscheme_b");
	}

	/**
	 * 设置对账方案明细id
	 *
	 * @param bankreconciliationscheme_b 对账方案明细id
	 */
	public void setBankreconciliationscheme_b(String bankreconciliationscheme_b) {
		set("bankreconciliationscheme_b", bankreconciliationscheme_b);
	}

    /**
     * 获取对账数据源
     *
     * @return 对账数据源
     */
	public ReconciliationDataSource getReconciliationdatasource() {
		Number v = get("reconciliationdatasource");
		return ReconciliationDataSource.find(v);
	}

    /**
     * 设置对账数据源
     *
     * @param reconciliationdatasource 对账数据源
     */
	public void setReconciliationdatasource(ReconciliationDataSource reconciliationdatasource) {
		if (reconciliationdatasource != null) {
			set("reconciliationdatasource", reconciliationdatasource.getValue());
		} else {
			set("reconciliationdatasource", null);
		}
	}

    /**
     * 获取启用日期
     *
     * @return 启用日期
     */
	public java.util.Date getEnableDate() {
		return get("enableDate");
	}

    /**
     * 设置启用日期
     *
     * @param enableDate 启用日期
     */
	public void setEnableDate(java.util.Date enableDate) {
		set("enableDate", enableDate);
	}

    /**
     * 获取银行账号
     *
     * @return 银行账号
     */
	public String getBankaccountno() {
		return get("bankaccountno");
	}

    /**
     * 设置银行账号
     *
     * @param bankaccountno 银行账号
     */
	public void setBankaccountno(String bankaccountno) {
		set("bankaccountno", bankaccountno);
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
     * 获取企业方余额方向
     *
     * @return 企业方余额方向
     */
	public Direction getDirection() {
		Number v = get("direction");
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
		Number v = get("bankdirection");
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
	 * 获取对账财务账簿
	 *
	 * @return 对账财务账簿.ID
	 */
	public String getAccbook() {
		return get("accbook");
	}

	/**
	 * 设置对账财务账簿
	 *
	 * @param accbook 对账财务账簿.ID
	 */
	public void setAccbook(String accbook) {
		set("accbook", accbook);
	}

    /**
     * 获取启用状态
     *
     * @return 启用状态
     */
    public EnableStatus getEnableStatus(){
        Number v = get("enableStatus");
        return EnableStatus.find(v);
    }

    /**
     * 设置启用状态
     *
     * @param enableStatus 是否启用状态
     */
    public void setEnableStatus(EnableStatus enableStatus){
        if (enableStatus != null) {
            set("enableStatus", enableStatus.getValue());
        } else {
            set("enableStatus", null);
        }
    }
	/**
	 * 获取停用日期
	 *
	 * @return 停用日期
	 */
	public java.util.Date getStopDate() {
		return get("stopDate");
	}

	/**
	 * 设置停用日期
	 *
	 * @param stopDate 停用日期
	 */
	public void setStopDate(java.util.Date stopDate) {
		set("stopDate", stopDate);
	}
	/**
	 * 获取期初未达项子表集合
	 *
	 * @return 期初未达项子表集合
	 */
	public java.util.List<OpeningOutstanding_b> openingOutstanding_b() {
		return getBizObjects("openingOutstanding_b", OpeningOutstanding_b.class);
	}

	/**
	 * 设置期初未达项子表集合
	 *
	 * @param openingOutstanding_b 期初未达项子表集合
	 */
	public void setOpeningOutstanding_b(java.util.List<OpeningOutstanding_b> openingOutstanding_b) {
		setBizObjects("openingOutstanding_b", openingOutstanding_b);
	}
}
