package com.yonyoucloud.fi.cmp.event.listerEvent;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.event.enums.EventCenterEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * <h1>事件中心过账成功后生成凭证号监听</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-05-17 16:10
 */
@Service
@Slf4j
public class CmpBankReconciliationChangeListerServiceImpl implements IEventReceiveService {

    @Override
    public String onEvent(BusinessEvent businessEvent, String queueName) throws BusinessException {
        // 判断是否为过账状态事件监听
        if (isEventStatusBackwrite(businessEvent)) {
            log.error("CmpBankReconciliationChangeListerServiceImpl, dataMsg= {}", CtmJSONObject.toJSONString(businessEvent));
            // 过账状态逻辑的回写
            return JsonUtils.toJsonString(EventResponseVO.success());
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }

    private boolean isEventStatusBackwrite(BusinessEvent businessEvent) {
        return EventCenterEnum.BANK_CLAIM.getSourceId().equals(businessEvent.getSourceID()) &&
                EventCenterEnum.BANK_CLAIM.getEventType().equals(businessEvent.getEventType());
    }
}
