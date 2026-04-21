package com.yonyoucloud.fi.cmp.event.sendEvent;


import com.yonyou.iuap.event.model.EventBody;
import com.yonyou.iuap.event.rpc.IEventFetchService;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.log.model.BusinessObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.log.util.BusiObjectBuildUtil;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.enums.FiEventActionEnum;
import com.yonyoucloud.fi.cmp.event.utils.DetermineUtils;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocol;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.EventMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.FULL_NAME;

/**
 * <h1>SimpleMessageCallbackEvents</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022-05-18 10:47
 */
@Service("simpleMessageCallbackEventsImpl")
@Slf4j
@RequiredArgsConstructor()
public class SimpleMessageCallbackEventsImpl implements IEventFetchService{


    private final ICmpSendEventService cmpSendEventService;

    private final CmCommonService commonService;

    @SneakyThrows
    @Override
    public EventBody fetchData(String fullName, String billClue, long version, String tenantId, String sourceId, String eventTypeCode) {
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(billClue)).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005FC", "简单消息回调逻辑，线索字段不能为空！") /* "简单消息回调逻辑，线索字段不能为空！" */);
        EventBody eventBody= null;
        try {
            List<EventMessageDTO> eventMessages = new ArrayList<>();
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(billClue);
            String srcBusiId = jsonObject.getString("srcBusiId");
            BizObject bizObject = MetaDaoHelper.findById(fullName, srcBusiId, 2);
            bizObject.put(FULL_NAME, fullName);
            // 设置单据附件数: Sun GuoCai 2023/7/3
            commonService.getFilesCount(bizObject);
            List<EventMessageDTO> userObject = cmpSendEventService.createUserObject(jsonObject, bizObject, FiEventActionEnum.ADD, version);
            if (!ValueUtils.isNotEmptyObj(userObject)){
                return eventBody;
            }
            EventMessageDTO eventMessageDTO = userObject.get(0);
            eventMessages.add(eventMessageDTO);
            eventBody = new EventBody();
            eventBody.setBillVersion(eventMessageDTO.getEventInfo().getSrcBillVersion());
            eventBody.setBody(CtmJSONObject.toJSONString(eventMessages));
        } catch (Exception e) {
            log.error("SimpleMessageCallbackEventsImpl, query data fail,fullName ={}, billClue = {}, e = {}", fullName, billClue, e.getMessage(),e);
        }
        return eventBody;
    }

    /**
     * 校验单据状态，是否可以进行过账，如果在过账中做了逆操作，则不能再继续进行过账
     * @return
     */
    private boolean checkBillStatus(){
        //todo 如果不能过账，返回的版本号需要根据事项的逻辑来返回版本号

        return true;
    }


}
