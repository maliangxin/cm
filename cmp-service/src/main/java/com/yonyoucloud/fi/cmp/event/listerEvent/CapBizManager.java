package com.yonyoucloud.fi.cmp.event.listerEvent;

import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.model.BusinessEventBuilder;
import com.yonyou.iuap.event.rpc.IEventSendService;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 资金业务对象类别删除校验
 * @author xuxbo
 * @date 2022/11/1 13:37
 */
@Slf4j
@Service
public class CapBizManager {

    public void sendToTmsp(String sourceId, String eventType, CtmJSONObject message) {

        BusinessEventBuilder businessEventBuilder = new BusinessEventBuilder();
        businessEventBuilder.setSourceId(sourceId);
        businessEventBuilder.setEventType(eventType);
        businessEventBuilder.setUserObject(message);
        BusinessEvent businessEvent = businessEventBuilder.build();

        RemoteDubbo.get(IEventSendService.class, "iuap-event-server").sendEvent(businessEvent);
    }
}
