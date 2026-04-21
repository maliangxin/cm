package com.yonyoucloud.fi.cmp.openingoutstanding;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import org.imeta.orm.base.BizObject;

import java.util.List;
import java.util.Map;

public interface OpeningOutstandingService {

    void updateOpeningOutstanding(CtmJSONObject params) throws Exception;

    /**
     * 查询期初余额子表数据
     * @param mainId 主表id
     * @return
     */
    List<Map<String, Object>> queryItemsByMainId(String mainId) throws Exception;

    /**
     * 同步期初余额
     * @param bizobject 期初未达项
     * @return
     */
    CtmJSONObject syncOpeningBalance(BizObject bizobject) throws Exception;
}
