package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.match;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author guoyangy
 * @Date 2024/6/18 16:32
 * @Description 流水处理过滤器链
 * @Version 1.0
 */
@Slf4j
@Service
public class SignalBankDealDetailMatchChainImplNew extends BankDealDetailMatchChainImpl{
    public static SignalBankDealDetailMatchChainImplNew get(){
        return new SignalBankDealDetailMatchChainImplNew();
    }

}