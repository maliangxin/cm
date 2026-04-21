package com.yonyoucloud.fi.cmp.controller.common;


import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.realtimebalance.CtmCmpAccountRealtimeBalanceRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * rpc接口测试类
 */
@RestController
@Slf4j
@RequestMapping("/rpc")
public class RpcTestController {

    @Autowired
    CtmCmpAccountRealtimeBalanceRpcService ctmCmpAccountRealtimeBalanceRpcService;

    /**
     * 查询银行账户实时余额
     * @param params
     * @throws Exception
     */
    @RequestMapping("/queryAccountRealtimeBalance")
    public void queryAccountRealtimeBalance(CommonRequestDataVo params) throws Exception{
        ctmCmpAccountRealtimeBalanceRpcService.queryAccountRealtimeBalance(params);
    }


}
