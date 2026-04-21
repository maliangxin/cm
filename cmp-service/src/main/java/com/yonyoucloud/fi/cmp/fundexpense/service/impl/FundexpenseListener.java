package com.yonyoucloud.fi.cmp.fundexpense.service.impl;

import com.yonyou.diwork.exception.BusinessException;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.rpc.IEventReceiveService;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.option.model.vo.EventResponseVO;
import com.yonyou.workbench.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.openapi.DataSettledDetail;
import com.yonyoucloud.ctm.stwb.stwbentity.BusinessBillType;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.listerEvent.CmpEventListerServiceImpl;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.tmsp.openapi.ITmspSystemRespRpcService;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemReq;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 转账工作台-结算工作台_监听事件
 */
@Slf4j
@Service
public class FundexpenseListener implements IEventReceiveService {

    @Autowired
    private ITmspSystemRespRpcService iTmspSystemRespRpcService;

    @Override
    public String onEvent(BusinessEvent businessEvent, String s) throws BusinessException {
        String callInfo = businessEvent.getUserObject();
        try{
            TmspSystemReq tmspSystemReq = new TmspSystemReq();
            tmspSystemReq.setApplyname("6");
            tmspSystemReq.setServicename("162");
            List<TmspSystemResp> tmspSystemResp = iTmspSystemRespRpcService.querySystemParameters(tmspSystemReq);
            if (CollectionUtils.isNotEmpty(tmspSystemResp)) {
                TmspSystemResp collectionParam = tmspSystemResp.get(0);
                //集成参数中资金结算的时候触发更新
                if (ObjectUtils.isNotEmpty(collectionParam.getSettlementprocessingmode()) && "0".equals(collectionParam.getSettlementprocessingmode())) {
                    CtmJSONArray array = CtmJSONArray.parseArray(callInfo);
                    // 事件消息体为空直接返回
                    if (arrayIsEmpty(array)) {
                        return JsonUtils.toJsonString(EventResponseVO.success());
                    }
                    for (int i = 0; i < array.size(); i++) {
                        // 构造EventInfo类对象
                        CtmJSONObject jsonObject = array.getJSONObject(i);
                        String srcBusiId = jsonObject.getString("srcBusiId");
                        String code = jsonObject.get(ICmpConstant.CODE).toString();
                        //等于200更新成功
                        if("200".equals(code)) {
                            //更新结算成功
                            BizObject bizObject = new BizObject();
                            bizObject.put("settlestate",3);
                            bizObject.put("id",srcBusiId);
                            bizObject.put("_status",EntityStatus.Update);
                            MetaDaoHelper.update(Fundexpense.ENTITY_NAME,bizObject);
                        }else{
                            //更新结算止付
                            BizObject bizObject = new BizObject();
                            bizObject.put("settlestate",4);
                            bizObject.put("id",srcBusiId);
                            bizObject.put("_status",EntityStatus.Update);
                            MetaDaoHelper.update(Fundexpense.ENTITY_NAME,bizObject);
                        }

                    }

                }
            }
        } catch (Exception e) {
            log.error("BindingTransferaccountListener.error:userObject = {}, e = {}", callInfo, e);
            return JsonUtils.toJsonString(EventResponseVO.fail(e.getMessage()));
        }

        return JsonUtils.toJsonString(EventResponseVO.success());
    }
    private boolean arrayIsEmpty(CtmJSONArray array) {
        return array == null || array.isEmpty();
    }

}
