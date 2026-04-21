package com.yonyoucloud.fi.cmp.bankreconciliation;

import com.yonyou.ucf.mdd.biz.ucfbase.character.ICharacteristcs;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import org.imeta.orm.base.BizObject;

/**
 * 关联信息特征实体
 *
 * @author u
 * @version 1.0
 */
public class BankRecRelationCharacterDef extends BizObject implements ICharacteristcs, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankreconciliation.BankRecRelationCharacterDef";

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
	public void setYTenant(String s) {
		set("ytenant", s);
	}
}
