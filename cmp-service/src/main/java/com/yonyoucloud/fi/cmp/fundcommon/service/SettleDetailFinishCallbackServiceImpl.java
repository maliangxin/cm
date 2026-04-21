package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventCallBackService;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.iuap.event.vo.EventAsyncResultVO;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.ctm.stwb.settleapply.vo.PushOrder;
import com.yonyoucloud.ctm.stwb.unifiedsettle.event.SettleDetailFinishEvent;
import com.yonyoucloud.ctm.stwb.unifiedsettle.vo.UnifiedSettleDetail;
import com.yonyoucloud.fi.cmp.batchtransferaccount.service.BatchtransferaccountService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.EventCenterEnum;
import com.yonyoucloud.fi.cmp.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @Description 监听结算明细办结事件
 * @Date 2025/6/14-16:24
 */
@Service
@Slf4j
@Transactional
public class SettleDetailFinishCallbackServiceImpl implements IEventReceiveService {

    @Autowired
    CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Autowired
    private BatchtransferaccountService batchtransferaccountService;

    @Autowired
    private IEventCallBackService eventCallBackService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String onEvent(BusinessEvent businessEvent, String nodeCode) throws CtmException {
        if (!ICmpConstant.CMDOMAIN.equals(nodeCode)) {
            return JsonUtils.toJsonString(EventResponseVO.success());
        }
        if (!isSettleDetailFinishEvent(businessEvent)) {
            return JsonUtils.toJsonString(EventResponseVO.success());
        }
        log.error("监听到结算明细办结的事件:{},nodeCode:{}", CtmJSONObject.toJSONString(businessEvent), nodeCode);
        SettleDetailFinishEvent settleDetailFinishEvent = CtmJSONObject.parseObject(businessEvent.getUserObject(), SettleDetailFinishEvent.class);
        UnifiedSettleDetail unifiedSettleDetail = settleDetailFinishEvent.getUnifiedSettleDetail();
        if (Objects.isNull(unifiedSettleDetail)) {
            throw new CtmException("SettleDetailFinishCallbackServiceImpl.onEvent error, unifiedSettleDetail is null");
        }

        if (StringUtils.isEmpty(settleDetailFinishEvent.getSourceCode()) || settleDetailFinishEvent.getPushOrder() == null) {
            log.error("事件id:{}的sourceCode或pushOrder为空，跳过", businessEvent.getUuid());
            return JsonUtils.toJsonString(EventResponseVO.success());
        }

        CtmJSONObject logData = new CtmJSONObject();
        logData.put("settleDetailFinishEvent", settleDetailFinishEvent);
        ctmcmpBusinessLogService.saveBusinessLog(logData, settleDetailFinishEvent.getSourceCode(), "SettleDetailFinishCallbackServiceImpl", IServicecodeConstant.BANKJOURNAL, IMsgConstant.SETTLE, IMsgConstant.SETTLE);

        // 批量同名账户划转
        EventAsyncResultVO eventAsyncResult = new EventAsyncResultVO();
        try {
            switch (settleDetailFinishEvent.getSourceCode()) {
                // 批量同名账户划转
                case ICmpConstant.BATCH_TRANSFER_ACCOUNT:
                    try {
                        eventAsyncResult = batchtransferaccountService.updateSettledInfoOfBatchtransferaccount(businessEvent, unifiedSettleDetail, settleDetailFinishEvent);
                    } catch (Exception e) {
                        log.error("SettleDetailFinishCallbackServiceImpl.batchtransferaccountService error", e);
                        eventAsyncResult.setSuccess(false);
                        eventAsyncResult.setNeedRetry(true);
                        eventAsyncResult.setMsg(e.getMessage());
                        return JsonUtils.toJsonString(EventResponseVO.fail(e.getMessage()));
                    }
                    break;
                // 统收统支
                default:
                    break;
            }
            // 任务出来完后执行回调，通知事件中心已完成，这里不可靠，事件中心如果30分钟未收到会重试
            log.error("监听到结算明细办结的事件异步回调开始,事件id:{},:{}", businessEvent.getUuid(), CtmJSONObject.toJSONString(eventAsyncResult));
            eventCallBackService.callBack(eventAsyncResult);
            log.error("监听到结算明细办结的事件异步回调完成,事件id:{}", businessEvent.getUuid());
        } catch (Exception e) {
            // 回调失败
            log.error("监听到结算明细办结的事件失败，需要重新处理上报,事件id:{}", businessEvent.getUuid(), e);
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }

    private boolean isSettleDetailFinishEvent(BusinessEvent businessEvent) {
        return EventCenterEnum.EVENT_SETTLEDETAIL_FINISHEVENT.getSourceId().equals(businessEvent.getSourceID()) &&
                com.yonyoucloud.fi.cmp.enums.EventCenterEnum.EVENT_SETTLEDETAIL_FINISHEVENT.getEventType().equals(businessEvent.getEventType());
    }
}
