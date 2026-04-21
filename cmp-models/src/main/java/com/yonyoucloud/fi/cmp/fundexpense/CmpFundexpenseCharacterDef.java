package com.yonyoucloud.fi.cmp.fundexpense;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import org.imeta.orm.base.BizObject;

/**
 * 资金费用单特征实体
 *
 * @author u
 * @version 1.0
 */
public class CmpFundexpenseCharacterDef extends BizObject implements IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.fundexpense.CmpFundexpenseCharacterDef";

    /**
     * 获取租户id
     *
     * @return 租户id.ID
     */
	public String getYtenant() {
		return get("ytenant");
	}

    /**
     * 设置租户id
     *
     * @param ytenant 租户id.ID
     */
	public void setYtenant(String ytenant) {
		set("ytenant", ytenant);
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
