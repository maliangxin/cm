package com.yonyoucloud.fi.cmp.util;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.model.BusinessEventBuilder;
import com.yonyou.iuap.event.service.EventService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;

/**
 * <h1>发送可靠性事件消息</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022/9/5 18:05
 */
@Slf4j
public class SendEventMessageUtils {

    /**
     * <h2>发送可靠性事件</h2>
     *
     * @param bizObject: 消息体
     * @param sourceId: 事件源
     * @param eventType: 事件类型
     * @author Sun GuoCai
     * @since 2021/11/11 9:47
     */
    public static void sendEventMessageEos(BizObject bizObject, String sourceId, String eventType) throws Exception {
        BusinessEventBuilder businessEventBuilder = new BusinessEventBuilder();
        businessEventBuilder.setSourceId(sourceId);
        // 设置事件查询ID
        businessEventBuilder.setBillId(bizObject.getString("bankreconciliationId"));
        businessEventBuilder.setBillCode(bizObject.getString("bankSeqNo"));
        businessEventBuilder.setEventType(eventType);
        businessEventBuilder.setUserObject(bizObject);
        businessEventBuilder.setTenantCode(InvocationInfoProxy.getTenantid());
        BusinessEvent businessEvent = businessEventBuilder.build();
        EventService eventService = AppContext.getBean(EventService.class);
        eventService.fireLocalEvent(businessEvent);
    }

    /**
     * <h2>发送可靠性事件</h2>
     *
     * @param businessEventBuilder : 事件参数
     * @author Sun GuoCai
     * @since 2022/11/29 17:30
     */
    public static void sendEventMessageEosByParams(BusinessEventBuilder businessEventBuilder) throws Exception {
        businessEventBuilder.setTenantCode(InvocationInfoProxy.getTenantid());
        BusinessEvent businessEvent = businessEventBuilder.build();
        EventService eventService = AppContext.getBean(EventService.class);
        eventService.fireLocalEvent(businessEvent);
    }
}
