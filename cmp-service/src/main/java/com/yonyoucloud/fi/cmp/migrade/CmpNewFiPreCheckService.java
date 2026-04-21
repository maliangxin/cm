package com.yonyoucloud.fi.cmp.migrade;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.vo.migrade.CmpPreCheckDetailVO;
import com.yonyoucloud.fi.cmp.vo.migrade.CmpPreCheckReqVO;

import java.util.List;

/**
 * 财务老架构升迁新架构，现金管理迁移前数据预检接口
 *
 */
public interface CmpNewFiPreCheckService {

    String newFiPreCheck(CtmJSONObject params) throws Exception;
    /**
     * 已审核未结算数据
     */
    List<CmpPreCheckDetailVO> getAuditAndUnSettleData(CmpPreCheckReqVO preCheckReqVO) throws Exception;

    /**
     * 启用审批流 且 审批流状态为审批中的单据
     */
    List<CmpPreCheckDetailVO> getIsWfAndApprovalData(CmpPreCheckReqVO preCheckReqVO) throws Exception;

    /**
     * 已审核 结算中数据
     */
    List<CmpPreCheckDetailVO> getAuditAndSettleIngData(CmpPreCheckReqVO preCheckReqVO) throws Exception;

    /**
     * 已审核已结算未生成凭证--往期
     */
    List<CmpPreCheckDetailVO> getVoucherStatusReceivedData(CmpPreCheckReqVO preCheckReqVO) throws Exception;

    /**
     * 已审核已结算未生成凭证--当期
     */
    List<CmpPreCheckDetailVO> getVoucherStatusReceivedDataThis(CmpPreCheckReqVO preCheckReqVO) throws Exception;

    /**
     * 存在商业汇票推送数据
     */
    List<CmpPreCheckDetailVO> getSourceFromDrftBill(CmpPreCheckReqVO preCheckReqVO) throws Exception;

    /**
     * 来源应收应付未审核单据
     */
    List<CmpPreCheckDetailVO> getSourceFromArapBill(CmpPreCheckReqVO preCheckReqVO) throws Exception;

    /**
     * 付款申请单增加校验：需要校验老架构审批过程中的单据需要在老架构审批通过（verifystate 不为 1 ）
     */
    List<CmpPreCheckDetailVO> getPayApplicationBill(CmpPreCheckReqVO preCheckReqVO) throws Exception;


    /**
     * 老架构是否有资金收付款单的校验
     */
    List<CmpPreCheckDetailVO> getFundPayBillIsExist(CmpPreCheckReqVO preCheckReqVO) throws Exception;

    List<CmpPreCheckDetailVO> getFundCollBillIsExist(CmpPreCheckReqVO preCheckReqVO) throws Exception;
    /**
     *老架构收付款工作台升级数据 如果来源单据已经结算完成 则资金收付不能逆操作
     * @param fullname
     */
    public void checkUpgradeDataBack(String fullname,String billId) throws Exception;



}
