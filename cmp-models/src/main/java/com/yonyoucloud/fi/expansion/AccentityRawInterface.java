package com.yonyoucloud.fi.expansion;

import java.util.Map;

public interface AccentityRawInterface {
    /**
     * 获取核算会计 主体
     *
     * @return 核算会计 主体.ID
     */
     String getAccentityRaw();
    /**
     * 设置核算会计 主体
     *
     * @param accentityRaw 核算会计 主体.ID
     */
     void setAccentityRaw(String accentityRaw);

    /**
     * 获取资金组织
     *
     * @return 资金组织.ID
     */
     String getAccentity();

    /**
     * 设置资金组织
     *
     * @param accentity 资金组织.ID
     */
     void setAccentity(String accentity);

}
