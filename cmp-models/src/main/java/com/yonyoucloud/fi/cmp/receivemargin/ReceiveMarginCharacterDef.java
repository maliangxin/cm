package com.yonyoucloud.fi.cmp.receivemargin;


import com.yonyou.ucf.mdd.biz.ucfbase.character.ICharacteristcs;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import org.imeta.orm.base.BizObject;

/**
 * 收到保证金台账管理特征实体
 *
 * @author u
 * @version 1.0
 */
public class ReceiveMarginCharacterDef extends BizObject implements ICharacteristcs, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.receivemargin.ReceiveMarginCharacterDef";

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
		return null;
	}

	@Override
	public void setYTenant(String ytenant) {

	}
}
