package com.yonyoucloud.fi.cmp.journal.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

public interface JournalRepairService {

    public void addJournalForStwbCode(CtmJSONObject params) throws Exception;

    public void deleteJournalByStwbCode(CtmJSONObject params) throws Exception;

    public void deleteJournalById(CtmJSONObject params) throws Exception;

    public void modifyInitBalanceById(CtmJSONObject params) throws Exception;

    //前端屏蔽 此方法不会调用
    public void updateCmpDataByQueryCondition(CtmJSONObject params) throws Exception;

}
