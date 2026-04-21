package com.yonyoucloud.fi.cmp.common.digitalwallet;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import org.imeta.orm.base.BizObject;

/**
 * @Description
 * @Author hanll
 * @Date 2025/8/30-16:24
 */
public interface DigitalWalletService {

    /**
     * 保存校验
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject checkSave(CtmJSONObject param) throws Exception;
}
