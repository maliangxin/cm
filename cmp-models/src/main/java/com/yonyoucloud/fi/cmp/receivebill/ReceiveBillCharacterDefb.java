package com.yonyoucloud.fi.cmp.receivebill;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import org.imeta.orm.base.BizObject;

/**
 * 收款单子表自定义项特征属性组实体
 *
 * @author u
 * @version 1.0
 */
public class ReceiveBillCharacterDefb extends BizObject implements IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.receivebill.ReceiveBillCharacterDefb";

    /**
     * 获取租户id
     *
     * @return 租户id
     */
	public String getYtenant() {
		return get("ytenant");
	}

    /**
     * 设置租户id
     *
     * @param ytenant 租户id
     */
	public void setYtenant(String ytenant) {
		set("ytenant", ytenant);
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

	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

}
