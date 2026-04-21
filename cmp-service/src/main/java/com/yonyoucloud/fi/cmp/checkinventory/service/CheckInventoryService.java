package com.yonyoucloud.fi.cmp.checkinventory.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory_b;

import java.util.List;
import java.util.Map;

/**
 * @author zhaorui
 * @version V1.0
 * @Copyright yonyou
 */
public interface CheckInventoryService {

    List<Map<String, Object>> getCheckInventoryInfo(CtmJSONObject params) throws Exception;

    void afterSaveBillToCmp(List<CheckInventory_b> billbs,String accentity) throws Exception;
}
