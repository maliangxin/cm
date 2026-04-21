package com.config;

import com.yonyou.ucf.mdd.ext.util.file.oss.CooperationServer;
import com.yonyou.ucf.mdd.ext.util.file.oss.Object;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @Author guoyangy
 * @Date 2024/4/20 16:06
 * @Description todo
 * @Version 1.0
 */
@Configuration
public class CooperationConfig {
    @ConditionalOnProperty(prefix = "mdd.cooperation.business", name = "line", matchIfMissing = false)
    @ConditionalOnMissingBean({Object.IObject.class})
    @Bean("cooperation")
    @DependsOn("mainAppContext")
    public Object.IObject cooperation() {
        return new CooperationServer();
    }
}
