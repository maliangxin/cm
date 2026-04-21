package com.yonyoucloud.fi.cmp.api.openapi;

import com.yonyou.cloud.middleware.rpc.RemoteCall;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;
import java.util.Map;

/**
 * Created by sz on 2019/4/20 0020.
 */
public interface OpenApiService {
    /**
     * 外部服务，生成单据，通过调用规则链生成，支持新增和修改
     * @param bill
     * @return
     * @throws Exception
     */
    CtmJSONObject saveBillByRules(BillDataDto bill) throws Exception;

    /**
     * 外部服务，生成单据
     * @param bill
     * @return
     * @throws Exception
     */
    CtmJSONObject deleteBillByRules(BillDataDto bill) throws Exception;


    /**
     * 外部服务，生成单据
     * @param bill
     * @return
     * @throws Exception
     */
    CtmJSONObject queryBillByCondition(BillDataDto bill) throws Exception;



}
