package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.config;

import com.google.common.eventbus.EventBus;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl.YQLDealDetailConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author guoyangy
 * @Date 2024/6/26 14:10
 * @Description todo
 * @Version 1.0
 */
@Configuration
@Slf4j
public class GuavaEventBusConfig {

    @Bean(DealDetailEnumConst.ODSEVENTBUS)
    public EventBus eventBus(){
        EventBus eventBus = new EventBus();
        eventBus.register(bankDealDetailEventBusListener());
        return eventBus;
    }

    @Bean("ods_eventbuslistener")
    public YQLDealDetailConsumer bankDealDetailEventBusListener(){
        return new YQLDealDetailConsumer();
    }



//    @Bean("bankreconciliation_eventBus")
//    public EventBus bankreconciliationEventBus(){
//        EventBus eventBus = new EventBus();
//        eventBus.register(bankreconciliationEventBusListener());
//        return eventBus;
//    }
//
//    @Bean("bankreconciliation_eventbuslistener")
//    public BankreconciliationEventBusListener bankreconciliationEventBusListener(){
//        return new BankreconciliationEventBusListener();
//    }
}
