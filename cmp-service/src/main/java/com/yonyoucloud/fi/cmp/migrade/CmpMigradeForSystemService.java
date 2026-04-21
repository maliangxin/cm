package com.yonyoucloud.fi.cmp.migrade;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.vo.migrade.CmpMigradeForSystemRequest;

public interface CmpMigradeForSystemService {

    public CtmJSONObject cmpMigradeForSystemCheck(CmpMigradeForSystemRequest params) throws Exception;

    public CtmJSONObject updateconfig(CtmJSONObject params) throws Exception;

    public CtmJSONObject update(CtmJSONObject params) throws Exception;
}
