package com.yonyoucloud.fi.cmp.apicorr;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;

import java.util.List;

/**
 * @author qihaoc
 * @Description: api接口操作银行对账单关联信息服务
 * @date 2023/7/8 10:01
 */
public interface APICorrService {

    void insertRelation4BankBill(BankReconciliation bankReconciliation, boolean delRelation, List<BankReconciliationbusrelation_b> busrelationbList, CtmJSONObject busrelations) throws Exception;
}