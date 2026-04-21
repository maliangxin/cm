package com.yonyoucloud.fi.cmp.foreignpayment;

import com.yonyou.ucf.mdd.biz.ucfbase.character.ICharacteristcs;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import org.imeta.orm.base.BizObject;

/**
 * 外汇付款特征实体
 *
 * @author u
 * @version 1.0
 */
public class ForeignPaymentCharacterDef extends BizObject implements ICharacteristcs, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.foreignpayment.ForeignPaymentCharacterDef";

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
