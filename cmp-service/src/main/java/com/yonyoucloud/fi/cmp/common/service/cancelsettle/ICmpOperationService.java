package com.yonyoucloud.fi.cmp.common.service.cancelsettle;

import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillBaseNode;
import com.yonyoucloud.ctm.stwb.cancelsettle.vo.BillDetailNode;

import java.util.List;

public interface ICmpOperationService {
    /**
     * 实际处理取消结算、流程入口
     * @return
     */
    boolean handleCancelSettle(CancelSettlementServiceEnum serviceEnum, BillBaseNode billBaseNode, String reason) throws Exception;

    /**
     *
     * 结算平台 可以取消的单据上下游关系、 校验目前返回成功就可以
     * @param billTypeId
     * @param billDetailIds
     * @return
     */
    BillDetailNode buildCancelSettleNode(String billTypeId, List<String> billDetailIds);
}
