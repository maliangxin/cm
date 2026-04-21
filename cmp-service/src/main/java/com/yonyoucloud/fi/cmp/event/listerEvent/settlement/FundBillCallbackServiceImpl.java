package com.yonyoucloud.fi.cmp.event.listerEvent.settlement;


import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资金结算单结算后回调接口
 * 待结算数据办结事件、dSettleDoneEvent
 */
@Slf4j
@Service
@Transactional
public class FundBillCallbackServiceImpl implements IEventReceiveService {

    @Autowired
    private FundBillCallbackProxyServiceImpl fundBillCallbackProxyService;

    @Override
    public String onEvent(BusinessEvent businessEvent, String s) throws CtmException{
        // 资金付款单，结算成功后，需要占用预算，调用接口
        String callInfo = businessEvent.getUserObject();
        try {
            if (!ICmpConstant.CMDOMAIN.equals(s)) { //非现金管理的单据不进行处理
                return JsonUtils.toJsonString(EventResponseVO.success());
            }
            fundBillCallbackProxyService.proxyOnEvent(businessEvent, s);
        } catch (Exception e) {
            log.error("FundBillCallbackServiceImpl.error:userObject = {}, e = {}", callInfo, e);
            return JsonUtils.toJsonString(EventResponseVO.fail(e.getMessage()));
        }
        return JsonUtils.toJsonString(EventResponseVO.success());
    }

}
