package com.yonyoucloud.fi.cmp.migrade;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * 财务老架构升迁新架构，现金管理迁移数据接口
 *
 */
public interface CmpNewFiMigradeService {

    /**
     * 数据迁移 从付款 至 资金付款
     * @return
     * @throws Exception
     */
    public CtmJSONObject migradePayToFunPayMent(CtmJSONObject params) throws Exception;

    /**
     * 返回迁移结果数据信息
     * @return
     * @throws Exception
     */
    public CtmJSONObject migradePayResult()throws Exception;

    /**
     * 数据迁移 从收款款 至 资金收款
     * @return
     * @throws Exception
     */
    public CtmJSONObject migradeReToFundCollection(CtmJSONObject params) throws Exception;

    /**
     * 返回迁移结果数据信息
     * @return
     * @throws Exception
     */
    public CtmJSONObject migradeReResult()throws Exception;


    /**
     * 升级交易类型
     * @throws Exception
     */
    public CtmJSONObject migradeUpdateTradetype(CtmJSONObject params)throws Exception;

    public void migradeUpdateTradetypeExcute(String ytenantid)throws Exception;

    /**
     * 升级特征
     * @return
     * @throws Exception
     */
    public CtmJSONObject migradeUpdateCharacterDef(CtmJSONObject params)throws Exception;

    public void migradeUpdateCharacterDefExcute(String uid)throws Exception;

    void migradePayToFunPayMentExcuteForSystem() throws Exception;

    void migradeReToFundCollectionExcuteForSystem() throws Exception;
}
