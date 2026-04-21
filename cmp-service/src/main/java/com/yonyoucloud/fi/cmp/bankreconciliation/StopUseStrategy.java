package com.yonyoucloud.fi.cmp.bankreconciliation;

import org.imeta.orm.base.BizObject;

/**
 * 停用策略
 *
 * @author xuwei
 * @date 2024/01/24
 */
public interface StopUseStrategy {

    /**
     * 停用
     *
     * @param bizObject 主子表数据
     * @param mode      0-启用，1-取消启用，2-主表停用，3-子表停用
     * @return msg 操作信息
     */
    String stopUse(BizObject bizObject, Short mode) throws Exception;

}
