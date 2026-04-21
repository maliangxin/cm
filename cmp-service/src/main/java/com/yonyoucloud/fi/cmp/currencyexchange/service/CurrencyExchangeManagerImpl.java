package com.yonyoucloud.fi.cmp.currencyexchange.service;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.cmpentity.DeliveryStatus;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 外币兑换实现类
 *
 * @author dongjch 2019年9月10日
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class CurrencyExchangeManagerImpl implements CurrencyExchangeManager {

    /**
     * 修改单据结算状态为处理中;
     * 新建事务，外部报错不影响此操作
     * @param currencyExchange
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class, propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(CurrencyExchange currencyExchange) throws Exception {
        currencyExchange.setSettlestatus(DeliveryStatus.doingDelivery);
        currencyExchange.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
    }
}
