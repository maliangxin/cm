package com.yonyoucloud.fi.cmp.bankdealdetail.bankdealdetailconfig;

import com.yonyoucloud.fi.cmp.bankdealdetail.SwitchDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Bankdealdetailconfig {

    @Bean
    public SwitchDTO switchDTO() {
        SwitchDTO switchDTO = new SwitchDTO();
        switchDTO.setMultiThreads(false);
        return switchDTO;
    }
}
