package com.yonyoucloud.fi.cmp.util;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @Author: liuymeng
 * @Date: 2022/8/2
 * @Description: 发号器
 */
@Component
public class IdCreator implements ApplicationContextAware {

    @Autowired
    private YmsOidGenerator ymsOidGenerator;

    private static ApplicationContext applicationContext;

    public static IdCreator getInstance() {
        return applicationContext.getBean(IdCreator.class);
    }

    public long nextId() {
        return ymsOidGenerator.nextId();
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        IdCreator.applicationContext  = ac;
    }
}
