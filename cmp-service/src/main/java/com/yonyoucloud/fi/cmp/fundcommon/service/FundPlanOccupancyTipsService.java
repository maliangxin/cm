package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;

/**
 * 用于获取不同业务模块的校验数据
 */
public interface FundPlanOccupancyTipsService {

    /**
     * 各业务单据获取进行资金计划占用时提示信息check的数据，不是自己模块则直接返回null
     * @param param
     * @return
     */
    CtmJSONObject  createFundPlanOccupancyTips(CtmJSONObject param) throws Exception;
}
