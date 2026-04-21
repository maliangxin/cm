package com.yonyoucloud.fi.cmp.reconciliate;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.journal.Journal;

import java.util.List;
import java.util.Map;

/**
 * @description: 银企对账后台操作接口
 * 自动对账，手工对账，单边对账，取消对账等接口
 * Created by zhanghlr on 2019/5/8
 */


public interface ReconciliateService {
    /**
     * 自动对账
     * @param jsonObject
     * @return
     * @throws Exception
     */
    CtmJSONObject automateTick(CtmJSONObject jsonObject) throws Exception;

    /**
     * 手工对账
     * @param jsonObject
     * @return
     * @throws JsonProcessingException
     */
    CtmJSONObject handTick(CtmJSONObject jsonObject) throws Exception;

    /**
     * 取消对账
     * @param jsonObject
     * @return
     * @throws JsonProcessingException
     */
    CtmJSONObject cancelTick(CtmJSONObject jsonObject) throws Exception;

    /**
     * 单边对账
     * @param jsonObject
     * @return
     */
    CtmJSONObject onesideTick(CtmJSONObject jsonObject);

    String checkDzEndDate(CtmJSONObject jsonObject) throws Exception;

    /**
     * 对账封存
     * @param jsonObject
     * @return
     */
    CtmJSONObject seal(CtmJSONObject jsonObject) throws JsonProcessingException;


    /**
     * 取消封存
     * @param jsonObject
     * @return
     */
    CtmJSONObject cancelSeal(CtmJSONObject jsonObject) throws JsonProcessingException;

    /**
     * 净额对账
     * @param jsonObject
     * @return
     * @throws Exception
     */
    CtmJSONObject netamountTick(CtmJSONObject jsonObject) throws Exception;

}
