package com.yonyou.yonbip.ctm.cmp;

import com.yonyou.iuap.ml.provider.IMultiLangProvider;
import com.yonyou.ucf.mdd.common.i18n.MddMultiLangProvider;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @Author guoyangy
 * @Date 2024/4/19 3:15
 * @Description todo
 * @Version 1.0
 */
@Configuration
@AutoConfigureOrder
public class MultiLangConfig {

    @Bean("iMultiLangProvider")
    public IMultiLangProvider iMultiLangProvider(){
        return new MddMultiLangProvider();
    }

}
