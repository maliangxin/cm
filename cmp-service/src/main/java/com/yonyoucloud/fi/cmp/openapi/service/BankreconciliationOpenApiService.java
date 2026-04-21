package com.yonyoucloud.fi.cmp.openapi.service;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.vo.Result;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author qihaoc
 * @Description: 银行对账单单据对外OpenAPI接口
 * @date 2022/10/5 14:19
 */
public interface BankreconciliationOpenApiService {
    /**
     * 新增
     * @param param
     * @return
     * @throws Exception
     */
    Result<Object> insert(CtmJSONObject param) throws Exception;


    /**
     * 删除
     * @param param
     * @return
     * @throws Exception
     */
    Result<Object> delete(CtmJSONObject param) throws Exception;

    /**
     * 执行辨识规则
     * @param param
     * @return
     * @throws Exception
     */
    HashMap<String,Object> executeRule(CtmJSONObject param) throws Exception;

    /**
     * 银行对账单查询
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject querylist(CtmJSONObject param) throws Exception;

    /**
     * 查询认领中心数据
     * @param param
     * @return
     * @throws Exception
     */
    CtmJSONObject queryClaimCenterList(CtmJSONObject param) throws Exception;

    /**
     * 新增关联关系
     *
     * @param param
     * @return
     */
    ArrayList<HashMap<String, String>> insertrelation(CtmJSONArray param);

    /**
     * 银行对账单认领并生成来款记录单
     * @param param
     * @return
     * @throws Exception
     */
    HashMap<String,Object> claimToReceipt(CtmJSONObject param) throws Exception;


    /**
     * 认领单生成来款记录单
     * @param claim
     * @return
     * @throws Exception
     */
    String claimPullAndPushToReceipt(BillClaim claim, BankReconciliation bankReconciliation) throws Exception;

    /**
     * 银行对账单OpenApi批量更新接口
     * @param jsonArray 批量更新的数据信息
     * @return
     * @throws Exception
     */
    CtmJSONObject batchUpdate(CtmJSONArray jsonArray) throws Exception;

    /**
     * 银行对账单OpenApi批量认领接口
     * @param jsonObj 批量认领的数据信息
     * @return
     * @throws Exception
     */
    CtmJSONObject batchClaim(CtmJSONObject jsonObj) throws Exception;
}
