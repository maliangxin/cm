package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.common.CtmSignatureRpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CtmSignatureRpcServiceImpl implements CtmSignatureRpcService {
    @Autowired
    BankConnectionAdapterContext bankConnectionAdapterContext;

    @Override
    public String ctmPaySignMessage(String originalMsg) throws Exception {
        return bankConnectionAdapterContext.chanPaySignMessage(originalMsg);
    }

    @Override
    public Boolean ctmPayVerifySignature(String originalMsg, String signature) throws Exception {
        return bankConnectionAdapterContext.chanPayVerifySignature(originalMsg, signature);
    }
}
