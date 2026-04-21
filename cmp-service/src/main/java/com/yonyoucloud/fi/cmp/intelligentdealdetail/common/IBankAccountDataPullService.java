package com.yonyoucloud.fi.cmp.intelligentdealdetail.common;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

public interface IBankAccountDataPullService {


    void pullData(CtmJSONObject param);

    void checkParam(CtmJSONObject param) throws Exception;
}
