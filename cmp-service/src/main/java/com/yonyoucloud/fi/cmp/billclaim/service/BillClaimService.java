package com.yonyoucloud.fi.cmp.billclaim.service;


import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.ypd.bizflow.dto.ConvertResult;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItemVO;
import org.imeta.orm.base.BizObject;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @description: 到账认领service
 * @author: wanxbo@yonyou.com
 * @date: 2022/4/21 10:02
 */

public interface BillClaimService {

    /**
     * 取消认领
     *
     * @param id 认领单ID
     * @return 取消结果
     */
    CtmJSONObject cancelClaim(Long id) throws Exception;

    /**
     * 根据银行对账单ID获取认领详情
     *
     * @param bankbillid 对账单ID
     * @return 认领单详情列表
     */
    List<BillClaimItemVO> queryBillClaimInfo(Long bankbillid) throws Exception;

    /**
     * 校验是否是认领的同一个交易日期
     *
     * @param id 对账单id
     * @return 校验结果
     */
    CtmJSONObject checkIsSameTransDate(Long id) throws Exception;

    /**
     * 我的认领 复核*
     *
     * @param billClaimResultes
     * @return
     * @throws Exception
     */
    CtmJSONObject recheck(List<BillClaim> billClaimResultes) throws Exception;

    /**
     * 我的认领 取消复核*
     *
     * @param billClaimResultes
     * @return
     * @throws Exception
     */
    CtmJSONObject unRecheck(List<BillClaim> billClaimResultes) throws Exception;

    /**
     * 结算成功 认领单进行资金切块
     *
     * @param dataSettledDetail
     * @throws Exception
     */
    void billclaimFundSegmentation(DataSettledDetail dataSettledDetail) throws Exception;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    void billclaimFundSegmentationSimple(String claimId);

    /**
     * 项目编辑后赋值
     *
     * @param param
     * @param
     */
    CtmJSONObject billClaimProjectEditAfter(CtmJSONObject param) throws Exception;

    /**
     * 到账认领中心 使用组织确认
     *
     * @param bankReconciliations
     * @return
     * @throws Exception
     */
    CtmJSONObject confirm(List<BankReconciliation> bankReconciliations, String accentity) throws Exception;

    /**
     * 银行流水处理 使用组织确认
     *
     * @param bankReconciliations
     * @return
     * @throws Exception
     */
    CtmJSONObject confirmFromBank(List<BankReconciliation> bankReconciliations, String accentity) throws Exception;

    /**
     * 银行流水处理 使用组织确认
     *
     * @param uid
     * @return
     * @throws Exception
     */
    CtmJSONObject asyncConfirmFromBank(String uid, CtmJSONArray row, String accentity) throws Exception;


    /**
     * 银行流水处理 取消确认
     *
     * @param uid
     * @return
     * @throws Exception
     */
    CtmJSONObject asyncCancelConfirmFormBank(String uid, CtmJSONArray row) throws Exception;

    /**
     * 到账认领中心 取消确认
     *
     * @param bankReconciliations
     * @return
     * @throws Exception
     */
    CtmJSONObject cancelConfirm(List<BankReconciliation> bankReconciliations) throws Exception;


    /**
     * 银行流水处理 取消确认
     *
     * @param bankReconciliations
     * @return
     * @throws Exception
     */
    CtmJSONObject cancelConfirmFormBank(List<BankReconciliation> bankReconciliations) throws Exception;

    //取消单据关联
    CtmJSONObject cancelCorrelate(Long id) throws Exception;

    void massClaim(BillContext billContext, BillDataDto billDataDto, String optType) throws Exception;

    Map<String,Object> supportTransferRuleAfterAddRule(List<BizObject> bills) throws Exception;

    /**
     * 认领关联其他领域单据后生收款单 单据转换规则后处理转单后数据
     * @param convertResult
     * @param paramMap
     * @param param
     * @param bustype
     * @throws Exception
     */
    void handResult(ConvertResult convertResult, Map<String, Object> paramMap, Map<String, String> param, String bustype) throws Exception;
}
