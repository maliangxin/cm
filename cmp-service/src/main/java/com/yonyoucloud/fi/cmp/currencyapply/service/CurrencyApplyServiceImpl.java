package com.yonyoucloud.fi.cmp.currencyapply.service;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.cmpentity.DeliveryStatus;
import com.yonyoucloud.fi.cmp.currencyapply.CurrencyApply;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @description: 外币兑换申请service
 * @author: wanxbo@yonyou.com
 * @date: 2023/8/25 16:14
 */

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class CurrencyApplyServiceImpl implements CurrencyApplyService {

    @Override
    public void updateDeliveryStatus(Long currencyApplyId, short deliveryStatus, Date fixtureDate) throws Exception {
        //手工交割完成/已交割/交割失败/已逾期;逆向取消手工交割待处理
        if (deliveryStatus != DeliveryStatus.completeDelivery.getValue() && deliveryStatus != DeliveryStatus.todoDelivery.getValue() && deliveryStatus != DeliveryStatus.alreadyDelivery.getValue()
                && deliveryStatus != DeliveryStatus.beOverdueDelivery.getValue() && deliveryStatus != DeliveryStatus.failDelivery.getValue() ){
            return;
        }

        //更新交割状态
        CurrencyApply currencyApply = MetaDaoHelper.findById(CurrencyApply.ENTITY_NAME,currencyApplyId);
        if (currencyApply == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102115"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180069","单据不存在 id:") /* "单据不存在 id:" */ + currencyApplyId);
        }
        currencyApply.setDeliverystatus(deliveryStatus);
        if (deliveryStatus != DeliveryStatus.todoDelivery.getValue()){ //逆向取消手工交割时
            currencyApply.setFixtureDate(fixtureDate == null ? new Date():fixtureDate);
        }else {
            currencyApply.setFixtureDate(null);
        }
        EntityTool.setUpdateStatus(currencyApply);
        MetaDaoHelper.update(CurrencyApply.ENTITY_NAME,currencyApply);

    }
}
