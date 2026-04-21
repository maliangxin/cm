package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.config;

import com.yonyou.iuap.context.YmsContextExecutor;
import com.yonyou.iuap.context.YmsContextWrappers;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.queue.OdsConsumerArrayBlockingQueue;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author guoyangy
 * @Date 2024/6/26 14:53
 * @Description todo
 * @Version 1.0
 */
@Configuration
@Slf4j
public class ThreadPoolExecutorConfig {

    @Resource
    private OdsConsumerArrayBlockingQueue odsConsumerArrayBlockingQueue;
    @Bean(name = {"odsConsumerExecutorService"})
    public ExecutorService odsConsumerExecutorService(@Value("${ods.consumer.parallel.core-size:0}") int corePoolSize,
                                                      @Value("${ods.consumer.parallel.max-size:0}") int maximumPoolSize) {
        int coreSize = Runtime.getRuntime().availableProcessors();
        if (corePoolSize == 0) {
            corePoolSize = coreSize;
        }
        corePoolSize = Math.max(corePoolSize, DealDetailEnumConst.MAXPOOL);
        if (maximumPoolSize == 0) {
            maximumPoolSize = coreSize;
        }
        if (maximumPoolSize < corePoolSize) {
            maximumPoolSize = corePoolSize;
        }
        ThreadFactory threadFactory = ThreadPoolUtil.createThreadFactory("ods-consumer-pool-", true);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                120L, TimeUnit.SECONDS,
                odsConsumerArrayBlockingQueue.getOdsConsumerBlockingQueue(), threadFactory, new ThreadPoolExecutor.DiscardPolicy());
        YmsContextExecutor taskExecutor = (YmsContextExecutor)YmsContextWrappers.wrap(executor);
        //todo 保证简强架构下线程池可用，有可能线程池起不来
        return taskExecutor;
    }
}
