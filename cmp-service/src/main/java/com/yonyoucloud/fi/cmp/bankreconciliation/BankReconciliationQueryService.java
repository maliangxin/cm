package com.yonyoucloud.fi.cmp.bankreconciliation;

import org.imeta.orm.schema.QuerySchema;

import java.util.List;

/**
 * 银行对账单查询接口
 *
 * @author xuwei
 * @date 2024/3/19
 */
public interface BankReconciliationQueryService {

    /**
     * 通过ids查询银行对账单的id
     *
     * @param schema 查询方案
     * @return 银行对账单
     * @throws Exception 查询异常
     */
    List<BankReconciliation> queryBySchema(QuerySchema schema) throws Exception;


}
