package com.yonyoucloud.fi.cmp.journal;

/**
 * 对JournalVo添加othertitle，caobject（收付款对象）
 */
public interface SetOtherForJournalService {

    void setOtherInfo(Journal journal) throws Exception;

}
