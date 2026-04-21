package com.yonyoucloud.fi.cmp.internaltransferprotocol.service;

import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocolVO;
import com.yonyoucloud.fi.cmp.vo.ResultMessageVO;
import com.yonyoucloud.fi.cmp.common.CtmException;

import java.util.Map;

/**
 * <h1>内转协议接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-08 16:18
 */
public interface InternalTransferProtocolService {

    /**
     * <h2>更新内转协议单据的启停用状态</h2>
     *
     * @param ids           :
     * @param isEnabledType :
     * @return ResultMessageVO<java.lang.String>
     * @author Sun GuoCai
     * @since 2023/9/10 9:54
     */
    ResultMessageVO<String> updateEnabledStatusOfTransferProtocolByIds(String ids, String isEnabledType) throws CtmException;


    /**
     * <h2>认领单结合内转协议单生成资金付款单</h2>
     *
     * @param internalTransferProtocolVO : 认领单入参
     * @return CtmJSONObject
     * @author Sun GuoCai
     * @since 2023/9/11 14:55
     */
    ResultMessageVO<String> internalTransferBillGeneratesFundPaymentBill(InternalTransferProtocolVO internalTransferProtocolVO) throws CtmException;


    /**
     * <h2>OpenAPI:根据id删除内转协议单</h2>
     *
     * @param param :
     * @return ResultMessageVO<java.lang.Object>
     * @author Sun GuoCai
     * @since 2023/9/19 11:56
     */
    ResultMessageVO<Object> deleteInternalTransferBillByIds(BillDataDto param) throws CtmException;

    /**
     * <h2>查询有效补充协议</h2>
     *
     * @param internalTransferProtocolVO :
     * @return ResultMessageVO
     * @author maxx4
     * @since 2024/4/8
     */
    ResultMessageVO<Map<String, Object>> queryValidInternalTransferBill(InternalTransferProtocolVO internalTransferProtocolVO) throws CtmException;

}
