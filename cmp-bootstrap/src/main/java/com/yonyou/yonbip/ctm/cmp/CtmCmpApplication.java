package com.yonyou.yonbip.ctm.cmp;

import com.yonyou.cloud.bean.RemoteCallInfo;
import com.yonyou.cloud.mw.MwLocator;
import com.yonyou.cloud.reqservice.IRemoteCallInfoManagerService;
import com.yonyou.iuap.event.service.EventBodyFetchService;
import com.yonyou.iuap.yms.id.config.EnableYmsOid;
import com.yonyoucloud.fi.cmp.util.cuckoo.CmpCuckooFilters;
import com.yonyoucloud.fi.cmp.util.cuckoo.CmpCuckooFiltersNothing;
import com.yonyoucloud.uretail.sys.itf.custom.ICustomVersionRuleAuthReferService;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
/*import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;*/
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * spring-boot 入口类
 *
 * @author LIUHAO
 */
@EnableAsync
@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, /*DataSourceTransactionManagerAutoConfiguration.class,*/ RabbitAutoConfiguration.class},scanBasePackages = "com.yonyoucloud.fi")
@ImportResource(locations = {"classpath*:/spring-com/applicationContext.xml"})
@MapperScan(basePackages = {"com.yonyoucloud.fi","com.yonyou.ctp"},
        annotationClass = Repository.class,
        sqlSessionFactoryRef = "sqlSessionFactory")
@EnableYmsOid
@EnableScheduling
@EnableRetry
@EnableTransactionManagement
public class CtmCmpApplication extends SpringBootServletInitializer {
    @Bean
    public RemoteCallInfo initStaffCenterSelfAll(){
        RemoteCallInfo rci = new RemoteCallInfo();
        rci.setAppCode("yonbip-fi-ctmcmp");
        rci.setNameSpace("c87e2267-1001-4c70-bb2a-ab41f3b81aa3");
        rci.setClassName(ICustomVersionRuleAuthReferService.class.getName());
        rci.setAlias("staffCenterSelfAll");
        rci.setReadTimeout("600000");
        rci.setReadTimeoutValue(600000);
        ((IRemoteCallInfoManagerService) MwLocator.lookup(IRemoteCallInfoManagerService.class)).regRemoteCall(rci);
        return rci;
    }

    public CmpCuckooFilters cmpCuckooFilters;
    @Bean("cmpCuckooFilters")
    public CmpCuckooFilters createCuckooFilters(@Value("${ctmcmp.bankDetail.cuckooFiltersEnable:true}") boolean enable) {
        if (enable) {
            this.cmpCuckooFilters =  CmpCuckooFilters.singleton();
        } else {
            this.cmpCuckooFilters =  CmpCuckooFiltersNothing.singleton();
        }
        return this.cmpCuckooFilters;
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(CtmCmpApplication.class).web(WebApplicationType.SERVLET).run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // 注意这里要指向原先用main方法执行的Application启动类
        return builder.sources(CtmCmpApplication.class);
    }

    /**
     * 事件中心精简消息配置类
     */
    @Bean
    public EventBodyFetchService eventBodyFetchService(JdbcTemplate jt) {
        EventBodyFetchService bodyFetchService = new EventBodyFetchService();
        bodyFetchService.setJt(jt);
        return bodyFetchService;
    }
}
