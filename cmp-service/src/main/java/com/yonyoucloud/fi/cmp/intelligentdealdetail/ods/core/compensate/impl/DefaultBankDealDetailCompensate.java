package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate.impl;

import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.BankDealDetailManageFacade;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.compensate.IBankDealDetailCompensate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
/**
 * @Author guoyangy
 * @Date 2024/10/12 13:49
 * @Description todo
 * @Version 1.0
 */
@Slf4j
public abstract class DefaultBankDealDetailCompensate implements IBankDealDetailCompensate {
    @Value("${dealdetail.timeout:30}")
    protected int dealDetailTimeout;

    //补偿的最大期限
    @Value("${dealdetail.expire:30}")
    protected int dealDetailExpire;
    @Resource
    protected IBankDealDetailAccessDao bankDealDetailAccessDao;
    @Resource
    protected BankDealDetailManageFacade bankDealDetailManageFacade;
}
