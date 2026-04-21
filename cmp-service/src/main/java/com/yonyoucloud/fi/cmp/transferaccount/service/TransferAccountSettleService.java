package com.yonyoucloud.fi.cmp.transferaccount.service;


import com.yonyou.yonbip.ctm.json.CtmJSONObject;

/**
 * @InterfaceName TransferAccountService
 * @Desc 转账单服务
 * @Author tongyd
 * @Date 2019/9/9
 * @Version 1.0
 */
public interface TransferAccountSettleService {

    void OfflinePay(CtmJSONObject param) throws Exception;

    void cancelOfflinePay(CtmJSONObject param) throws Exception;
}
