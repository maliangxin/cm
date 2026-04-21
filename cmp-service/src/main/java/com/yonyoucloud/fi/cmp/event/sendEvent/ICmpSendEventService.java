package com.yonyoucloud.fi.cmp.event.sendEvent;


import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.enums.FiEventActionEnum;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.BusiQueryStatusItemDTO;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.BusiStatusRespDTO;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.EventMessageDTO;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QuerySchema;

import java.util.List;

/**
 * <h1>推送事务值事件中心</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022/5/17 7:35
 */
public interface ICmpSendEventService {

    /**
     * 发送事件
     *
     */
    void sendEvent(BizObject msgBizObject) throws Exception;

    /**
     * 发送精简消息事件
     *
     */
    void sendSimpleEvent(BizObject msgBizObject, CtmJSONObject billClue) throws Exception;

    /**
     * 反向操作
     *
     */
    void deleteEvent(BizObject msgBizObject) throws Exception;

    /**
     * 反向操作，无异常版
     *
     */
    CtmJSONObject deleteEventWithoutException(BizObject msgBizObject) throws Exception;

    /**
     * <h2>组装参数公共方法</h2>
     * @author Sun GuoCai
     * @date 2022/5/18 11:39
     * @param : 实体信息
     * @param actionEnum: 单据持久状态
     * @return java.util.List<com.yonyoucloud.fi.fieaai.busievent.dto.v1.EventMessageDTO>
     */
    List<EventMessageDTO> createUserObject(CtmJSONObject jsonObject, BizObject msgBizObject,  FiEventActionEnum actionEnum, long billVersion) throws Exception;

    void translateUserId(FiEventActionEnum actionEnum, BizObject bizObject);

    List<Short> getVoucherStatusId(BusiQueryStatusItemDTO busiQueryStatusItemDTO);

    BusiStatusRespDTO getBusiStatusRespDTO(BusiQueryStatusItemDTO busiQueryStatusItemDTO, List<Short> list, QuerySchema querySchema,boolean isDataFlatten) throws Exception;

    void dataVerify(BusiQueryStatusItemDTO busiQueryStatusItemDTO, boolean isDataFlatten);

    /**
     * <h2>银行流水支持发送事件消息</h2>
     * @author Sun GuoCai
     * @date 2024/4/29 10:11
     * @param msgBizObject: 消息体
     */
    void sendEventByBankClaim(BizObject msgBizObject, String action) throws Exception;

    void sendEventByBankClaimBatch(List<BankReconciliation>  msgBizObject, String action) throws Exception;
}
