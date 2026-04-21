package com.yonyou.yonbip.ctm.cmp;

import com.yonyou.iuap.yms.lock.YMSLockFactoryManager;
import com.yonyou.iuap.yms.lock.YmsLockFactory;
import com.yonyou.iuap.yms.lock.YmsScopeLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
@RequiredArgsConstructor(onConstructor=@__(@Autowired))
public class CmpScopeLockConfig {

    @Bean(name = "ymsGlobalScopeLockManager")
    @ConditionalOnClass(value = YmsScopeLockManager.class)
    @ConditionalOnBean(YmsLockFactory.class)
    public YmsScopeLockManager ymsScopeLockManager(@Qualifier("ymsGlobalYmsLockFactory") YmsLockFactory ymsLockFactory) {
        return YMSLockFactoryManager.getScopeLockManager(ymsLockFactory);
    }
}
