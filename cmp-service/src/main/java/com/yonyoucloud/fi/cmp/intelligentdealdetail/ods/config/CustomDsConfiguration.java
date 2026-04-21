package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.config;

import com.yonyou.iuap.yms.dao.BaseDAO;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * @Author guoyangy
 * @Date 2024/6/26 21:15
 * @Description todo
 * @Version 1.0
 */
@Configuration
@EnableTransactionManagement
public class CustomDsConfiguration {

    @Bean("jdbcTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource master) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(master);
        return jdbcTemplate;
    }
    /*@Bean(DealDetailEnumConst.JDBCAPIDAO)
    public BaseDAO baseDAO(JdbcTemplate jdbcTemplate) {
        BaseDAO baseDAO = new BaseDAO();
        baseDAO.setJdbcTemplate(jdbcTemplate);
        return baseDAO;
    }*/
}
