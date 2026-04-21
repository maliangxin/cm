package com.yonyoucloud.fi.cmp.workbench.flowbench.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流水工作台-我的待办
 * @author guoxh
 */
@NoArgsConstructor
@Data
public class FlowTodoVO {
    /**
     * 银行流水处理-待确认使用组织
     */
    private Long orgConfirmNum;
    /**
     * 银行流水处理-疑似退票待确认 取退单
     */
    private Long refundConfirmNm;
    /**
     * 银行流水处理-自动处理待确认 废弃
     */
    private Long autoHandleConfirmNum;
    /**
     * 银行流水处理-待处理
     */
    private Long todoProcessingNum;

    /**
     * 自动关联待确认
     */
    private Long autorelationConfirming;

    /**
     * 自动生单待确认 取生单
     */
    private Long autocreatebillConfirming;

    /**
     * 银行流水处理-发布处理中
     */
    private Long processingNum;
    /**
     * 到账认领-我的待认领
     */
    private Long myTodoClaimNum;
    /**
     * 到账认领-公共待认领
     */
    private Long commonTodoClaimNum;
    /**
     * 到账认领-公共待确认使用组织
     */
    private Long commonOrgConfirmNum;
    /**
     * 我的认领-我的待提交认领
     */
    private Long myPendSubmitClaimNum;
    /**
     * 我的认领-我的待审批认领
     */
    private Long myPendApprovalClaimNum;
    /**
     * 我的认领-我的待处理认领
     */
    private Long myPendDoClaimNum;
    /**
     * 我的认领-我的待审认领
     */
    private Long myPendAuditClaim;
}
