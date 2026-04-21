package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;

import java.util.Date;
import java.util.List;

public class EntityTool {
	public static void setUpdateTS(BizObject bizObject) {
		bizObject.setPubts(new Date());
	}

	public static <T extends BizObject> void setUpdateTS(List<T> bizObjects) {
		for (BizObject biz : bizObjects)
			biz.setPubts(new Date());
	}

	public static void setUpdateStatus(BizObject bizObject) {
		bizObject.setEntityStatus(EntityStatus.Update);
	}

	public static <T extends BizObject> void setUpdateStatus(List<T> bizObjects) {
		for (BizObject biz : bizObjects)
			biz.setEntityStatus(EntityStatus.Update);
	}

	public static void setTenant(BizObject bizObject) {
		setTenant(bizObject, AppContext.getTenantId());
	}

	public static <T extends BizObject> void setTenant(List<T> bizObjects) {
		setTenant(bizObjects, AppContext.getTenantId());
	}

	public static void setTenant(BizObject bizObject, long tenant) {
		bizObject.set("tenant", tenant);
	}

	public static <T extends BizObject> void setTenant(List<T> bizObjects, long tenant) {
		for (BizObject biz : bizObjects)
			biz.set("tenant", tenant);
	}
}
