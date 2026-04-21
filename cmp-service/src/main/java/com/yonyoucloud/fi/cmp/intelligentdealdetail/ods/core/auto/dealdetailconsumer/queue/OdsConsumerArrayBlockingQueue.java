package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.concurrent.ArrayBlockingQueue;
/**
 * @Author guoyangy
 * @Date 2024/6/26 15:07
 * @Description todo
 * @Version 1.0
 */
@Component
public class OdsConsumerArrayBlockingQueue {
    private ArrayBlockingQueue odsConsumerBlockingQueue = null;
    @Value("${ods.consumer.queue-length:2000}")
    private int queueLength;
    @PostConstruct
    public void init(){
        this.odsConsumerBlockingQueue = new ArrayBlockingQueue(queueLength);
    }
    public ArrayBlockingQueue getOdsConsumerBlockingQueue() {
        return this.odsConsumerBlockingQueue;
    }
}