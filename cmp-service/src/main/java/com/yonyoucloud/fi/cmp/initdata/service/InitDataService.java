package com.yonyoucloud.fi.cmp.initdata.service;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.bd.period.Period;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;

public interface InitDataService {
    void batchSave(List<InitData> settlementList) throws Exception;

    CtmJSONObject queryHvEditState(String accentity, String currency) throws Exception;

    CtmJSONObject importData(String billNumber);

    CtmJSONObject upgradeInitData() throws Exception;

    void scheduledUpgradeInitData(CtmJSONObject params) throws Exception;

    void checkSettleflag(CtmJSONObject params) throws Exception;

    void initAccountDate(Map<String, Period> periodMap, String orgId, InitData initData, EnterpriseBankAcctVOWithRange bankAcctVO);

    Map<String, Period> queryListFinanceOrg(List<String> orgIdList) throws Exception;

    void updateNewInitDataForOldData(CtmJSONObject params) throws Exception;

    String changeAccountDate(CtmJSONObject params) throws Exception;

    /**
     * 同步期初余额
     * @param bizobject 银行账户期初
     * @return
     */
    CtmJSONObject syncInitBalance(BizObject bizobject) throws Exception;

    /**
     * 初始化现金账户的期初日期
     * @param periodMap
     * @param orgId
     * @param initData
     * @param enterpriseCashMap
     */
    void initCashAccountDate(Map<String, Period> periodMap, String orgId, InitData initData, Map<String, Object> enterpriseCashMap);
}
