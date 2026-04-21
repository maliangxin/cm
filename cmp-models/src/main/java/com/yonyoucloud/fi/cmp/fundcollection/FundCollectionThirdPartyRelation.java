package com.yonyoucloud.fi.cmp.fundcollection;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 资金收款单与第三方关联关系表实体
 *
 * @author u
 * @version 1.0
 */
public class FundCollectionThirdPartyRelation extends BizObject implements ITenant, IYTenant{
	// 实体全称
	public static final String ENTITY_NAME = "cmp.fundcollection.FundCollectionThirdPartyRelation";

    /**
     * 获取子表id
     *
     * @return 子表id.ID
     */
	public Long getChildid() {
		return get("childid");
	}

    /**
     * 设置子表id
     *
     * @param childid 子表id.ID
     */
	public void setChildid(Long childid) {
		set("childid", childid);
	}

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
     * 获取序号
     *
     * @return 序号
     */
	public Integer getRowno() {
		return get("rowno");
	}

    /**
     * 设置序号
     *
     * @param rowno 序号
     */
	public void setRowno(Integer rowno) {
		set("rowno", rowno);
	}

    /**
     * 获取来源单据id
     *
     * @return 来源单据id
     */
	public String getSrcbillid() {
		return get("srcbillid");
	}

    /**
     * 设置来源单据id
     *
     * @param srcbillid 来源单据id
     */
	public void setSrcbillid(String srcbillid) {
		set("srcbillid", srcbillid);
	}

    /**
     * 获取来源单据明细id
     *
     * @return 来源单据明细id
     */
	public String getSrcbillitemid() {
		return get("srcbillitemid");
	}

    /**
     * 设置来源单据明细id
     *
     * @param srcbillitemid 来源单据明细id
     */
	public void setSrcbillitemid(String srcbillitemid) {
		set("srcbillitemid", srcbillitemid);
	}

    /**
     * 获取来源单据号
     *
     * @return 来源单据号
     */
	public String getSrcbillno() {
		return get("srcbillno");
	}

    /**
     * 设置来源单据号
     *
     * @param srcbillno 来源单据号
     */
	public void setSrcbillno(String srcbillno) {
		set("srcbillno", srcbillno);
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
