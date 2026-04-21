package com.yonyoucloud.fi.cmp.checkmanage.service;

import com.yonyou.cloud.yts.annotation.YtsTransactional;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;

/**
 * <h1>支票处置生成接口</h1>
 *
 * @author yanxiaokai
 * @version 1.0
 * @since 2023-06-28 15:07
 */
public interface CheckManageDBService {

    /**
     * 支票处置生成统一接口
     *
     * @param checkManage
     * @return RuleExecuteResult
     * @throws Exception Exception
     */
    @YtsTransactional(cancel = "checkSaveRollBack")
    RuleExecuteResult checkSave(CheckManage checkManage) throws Exception ;

    /**
     * 支票处置生成统一回滚接口
     *
     * @param checkManage
     * @return Object
     * @throws Exception Exception
     */
    Object checkSaveRollBack(CheckManage checkManage) throws Exception ;
}
