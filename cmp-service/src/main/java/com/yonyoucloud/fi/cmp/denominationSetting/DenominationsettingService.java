package com.yonyoucloud.fi.cmp.denominationSetting;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

public interface DenominationsettingService {

    public CtmJSONObject checkDenominationsetting(Long id) throws Exception;

    public CtmJSONObject checkDenominationsetting_b(Long id) throws Exception;
}
