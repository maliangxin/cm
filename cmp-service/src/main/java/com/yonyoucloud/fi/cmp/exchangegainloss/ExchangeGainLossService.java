package com.yonyoucloud.fi.cmp.exchangegainloss;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.journal.Journal;

/**
 * Created  by xudy on 2019/9/26.
 */
public interface ExchangeGainLossService {

    /**
     * @param params
     * 汇率损益界面初始化
     */
    CtmJSONObject  initData(CtmJSONObject params) throws  Exception;

    /**
     * 创建日记账-现金汇兑损益新增
     * @param exchangeGainLoss 现金汇兑损益主表
     * @param exchangeGainLoss_b 现金汇兑损益子表
     * @return
     * @throws Exception
     */
    Journal createJournalForAdd(ExchangeGainLoss exchangeGainLoss, ExchangeGainLoss_b exchangeGainLoss_b, String billnum) throws Exception;

    /**
     * 创建日记账-现金汇兑损益冲销
     * @param exchangeGainLoss   现金汇兑损益主表
     * @param exchangeGainLoss_b 现金汇兑损益主表
     * @param billnum
     * @return
     * @throws Exception
     */
    Journal createJournalForWriteOff(ExchangeGainLoss exchangeGainLoss, ExchangeGainLoss_b exchangeGainLoss_b, String billnum) throws Exception;
}
