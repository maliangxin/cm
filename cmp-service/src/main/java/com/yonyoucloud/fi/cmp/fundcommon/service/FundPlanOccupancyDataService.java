package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;

public interface FundPlanOccupancyDataService {
    List<CtmJSONObject> createFundPlanOccupancyData(CtmJSONObject param);
}
