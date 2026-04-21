package com.yonyoucloud.fi.cmp.inwardremittance;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.Map;

/**
 * 汇入汇款调度任务
 */
public interface InwardRemittanceTaskService {

    /**
     * 汇入汇款待确认业务列表查询SSFE3005 - 调度任务
     * @param params
     * @return
     */
    Map inwardRemittanceListQueryTask(CtmJSONObject params);

    /**
     * 汇入汇款确认交易结果查询SSFE3004 - 调度任务
     * @param params
     * @return
     */
    Map inwardRemittanceResultQueryTask(CtmJSONObject params);

    /**
     * 汇入汇款业务明细查询SSFE3006 - 调度任务
     * @param param
     * @return
     */
    Map inwardRemittanceDetailQueryTask(Map param);
}
