package com.yonyoucloud.fi.cmp.util.message;

import com.yonyou.iuap.event.mp.api.model.PushMessage;
import com.yonyou.iuap.event.mp.common.model.Result;
import com.yonyou.iuap.message.platform.client.MessagePlatformClient;
import com.yonyou.iuap.message.platform.entity.MessageInfoEntity;
import com.yonyou.ucf.mdd.ext.core.AppContext;

import java.util.List;

/**
 * 新的消息平台sdk适配类
 *
 * @author maliangn
 * @version 1.0
 * @since 2026-03-15
 */
public class CMMessageInfoClient {
    static final String dsCode = "yonbip-fi-ctmcmp_dataSource";

    /**
     * 使用默认数据源发送消息
     * @param entity
     * @return
     */
    public static Result<List<PushMessage>> sendMessageByDefaultDs(MessageInfoEntity entity){
       return AppContext.getBean(MessagePlatformClient.class).storage(entity,dsCode);
    }


}
