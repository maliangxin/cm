package com.yonyoucloud.fi.cmp.stwb;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.journal.Journal;

import java.util.List;

/**
 * @Date 2021/5/26 11:31
 * @Author wangshbv
 * @Description 日记账公用工具类
 */
public interface JournalCommonService {

    /**
     * 登日记账的公用方法
     * @author wangshbv
     * @date 10:59
     */
    public void journalRegisterForStwb(CtmJSONObject param) throws Exception;

    /**
     * 日记账审核，弃审，驳回，止付，结算成功的方法
     * @author wangshbv
     * @date 11:00
     */
    public void journalApproveForStwb(CtmJSONObject param) throws Exception ;

    /**
     * 根据资金组织，结算明细id查询日记账数据的公用方法
     * @author wangshbv
     * @date 11:01
     */
    public List<Journal> getJournalsByItemBodyIdList(String accentity, List<String> srcItemBodyIdList) throws Exception;

    /**
     * type :1 正向更新期初余额，2：回滚期初余额并且删除日记账
     * 回滚期初余额 并删除日记账
     * @param journalList
     * @throws Exception
     */
    public void rollbackInitDataAndJournalSecond(List<Journal> journalList, Integer type) throws Exception ;

    /**
     * 设置日记账信息的公用方法
     * @author wangshbv
     * @date 13:51
     */
    public List<Journal> setJournalInfoByType(List<Journal> journalList, AuditStatus incomplete, AuditStatus complete);

    /**
     * 结算成功，更新日记账数据
     * @author wangshbv
     * @date 11:01
     */
    public List<Journal> settleSuccess(List<Journal> journalList);
    /**
     * 结算失败，回滚日记账数据
     * @author wangshbv
     * @date 11:01
     */
    public List<Journal> settleSuccessCancel(List<Journal> journalList);

    /**
     * 从redis中获取日记账数据，并回滚的方法
     * @author wangshbv
     * @date 11:02
     */
    public void rollbackJournalDataFromRedis(List<String> srcBillItemIdList, String requestId) throws Exception;
}
