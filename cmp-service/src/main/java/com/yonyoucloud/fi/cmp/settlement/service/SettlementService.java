package com.yonyoucloud.fi.cmp.settlement.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import org.imeta.orm.base.EntityStatus;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2019/4/20 0020.
 */
public interface SettlementService {

    /**
     * 获取选中会计主体下的会计期间
     *
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    CtmJSONObject getSettlementPeriod(String accentity, String period) throws Exception;


    /**
     * 获取日结检查项
     *
     * @return checkItemList
     * @throws Exception
     */
    List<Map<String, Object>> getCheckItem() throws Exception;


    /**
     * 获取日结检查结果
     *
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    CtmJSONObject settleCheck(String accentity, String period,boolean isAuto, String settleFlag) throws Exception;

    /**
     * 查询手工日结检查结果
     *
     * @param accentity 会计主体
     * @param period    期间
     * @throws Exception
     */
    CtmJSONObject queryCheckResult(String accentity, String period) throws Exception;
    /**
     * 获取默认会计主体
     *
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    Map<String, Object> getDefaultAccentity() throws Exception;


    /**
     * 日结
     *
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    CtmJSONObject settle(String accentity, String period,boolean isAuto) throws Exception;


    /**
     * 取消日结
     *
     * @return CtmJSONObject 0:失败；1:成功；message：错误信息
     * @throws Exception
     */
    CtmJSONObject unsettle(String accentity, String period) throws Exception;


    /**
     * 总账是否日结检查
     */
    CtmJSONObject getCheckResult(String accentity, String period);


    /*总账判断是否引用会计账簿*/
    List<String> hasRefAccbook(List<String> accbookIds) throws Exception;

    /**
     * 获取最大结账日期
     * @param accEntity
     * @return
     * @throws Exception
     */
    Date getMaxSettleDate(String accEntity) throws Exception;

    /**
     * 获取最小未结账日期
     * @param accEntity
     * @return
     * @throws Exception
     */
    Date getMinUnSettleDate(String accEntity) throws Exception;


    /**
     * 检查日结
     * @param accEntity
     * @param vouchDate
     * @return
     * @throws Exception
     */
    Boolean checkDailySettlement(String accEntity, Date vouchDate) throws Exception;

    /**
     * 定时任务执行日结
     * @param params
     * @return
     */
    String autoDailySettle(CtmJSONObject params) throws JsonProcessingException;

    /**
     * 获取会计期间启用日期
     * @param accentity
     * @return
     */
    Map<String,Object>  getPeriodByAccentity(String accentity) throws Exception;

    void checkSettleForAcctParentAccentity(String accentity,String bankacctId, Date date, EntityStatus entityStatus) throws Exception;


    CtmJSONObject upgradeDailySettleCurrent() throws Exception;

    /**
     * 期初改造日结升级
     * @throws Exception
     */
    void upgradeDailySettle(CtmJSONObject param) throws Exception;

    CtmJSONObject getSettlementlistRefData() throws Exception;
}
