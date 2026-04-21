package com.yonyoucloud.fi.cmp.balanceadjustresult;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;

public interface BalanceAdjustResultSerevice  {
    /**
     * 保存余额调节表列表的数据
     */
    public CtmJSONObject add(BalanceAdjustResult balanceAdjustResult,String filterArgs,CtmJSONObject ctmJson) throws Exception;

    /**
     * 校验最早的数据之前是否存在未审批数据
     * @param balanceAdjustResult
     * @throws Exception
     */
    void beforeSubmitCheck(BalanceAdjustResult balanceAdjustResult) throws Exception;

    BalanceAdjustResult queryExistsByCond(BalanceAdjustResult balanceAdjustResult) throws Exception;

    //删除余额调节表
    public CtmJSONObject delete(Long id) throws Exception;


    CtmJSONObject balanceAudit(List<BalanceAdjustResult> balanceAdjustResultes) throws Exception;

    CtmJSONObject balanceUnAudit(List<BalanceAdjustResult> balanceAdjustResultes) throws Exception;


    BalanceAdjustResult getEarlyUnauditData(BalanceAdjustResult balanceAdjustResult) throws Exception;

    BalanceAdjustResult  getAfterAuditData(BalanceAdjustResult balanceAdjustResult) throws Exception;

    BalanceAdjustResult getBalanceAdjustResultById(Long id) throws Exception;
}
