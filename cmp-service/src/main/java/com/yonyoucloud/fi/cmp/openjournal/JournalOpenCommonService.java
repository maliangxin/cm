package com.yonyoucloud.fi.cmp.openjournal;

import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyoucloud.fi.cmp.journal.JournalVo;

/**
 * @Date 2021/6/10 11:31
 * @Author shangxd
 * @Description 日记账公用工具类
 */
public interface JournalOpenCommonService {



    /**
     * 登日记账的公用方法
     * @author shangxd
     * @date 10:59
     */
    public void journalRegister(JournalVo journalVo) throws Exception;

    /**
     * 从redis中获取日记账数据，并回滚的方法
     * @author shangxd
     * @date 11:02
     */
    public void rollbackJournalRegister(JournalVo journalVo) throws Exception;

    /**
     * 更新日记账
     * @param journalVo
     * @throws Exception
     */
    public void journalUpdate(JournalVo journalVo) throws Exception;

    /**
     * 更新日记账回滚逻辑
     * @param journalVo
     * @throws Exception
     */
    public void rollbackJournalUpdate(JournalVo journalVo) throws Exception;

    /**
     * 删除日记账
     * @param journalVo
     * @throws Exception
     */
    public void journalDelete(JournalVo journalVo) throws CtmException;

    /**
     * 删除日记账回滚逻辑
     * @param journalVo
     * @throws Exception
     */
    public void rollbackJournalDelete(JournalVo journalVo) throws Exception;

}
