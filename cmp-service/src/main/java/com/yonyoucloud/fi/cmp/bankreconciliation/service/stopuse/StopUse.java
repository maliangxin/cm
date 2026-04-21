package com.yonyoucloud.fi.cmp.bankreconciliation.service.stopuse;

import com.yonyoucloud.fi.cmp.bankreconciliation.StopUseStrategy;
import org.imeta.orm.base.BizObject;

/**
 * 停用
 *
 * @author xuwei
 * @date 2024/01/24
 */
public class StopUse {

    private StopUseStrategy stopUseStrategy;

    public StopUse(StopUseStrategy strategy) {
        this.stopUseStrategy = strategy;
    }

    public String stopUse(BizObject bizObject, Short mode) throws Exception {
        return stopUseStrategy.stopUse(bizObject, mode);
    }

}
