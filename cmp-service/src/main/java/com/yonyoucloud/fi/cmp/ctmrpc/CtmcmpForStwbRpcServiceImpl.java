package com.yonyoucloud.fi.cmp.ctmrpc;


import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.common.CtmcmpForStwbRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.journal.CtmCmpJournalCommonService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonResponseDataVo;
import com.yonyoucloud.fi.cmp.vo.common.CommonQueryDataVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class CtmcmpForStwbRpcServiceImpl implements CtmcmpForStwbRpcService {

    @Autowired
    CtmCmpJournalCommonService ctmCmpJournalCommonService;

    @Override
    public CommonResponseDataVo queryJournalBalanceForStwb(List<CommonRequestDataVo> queryDataVoList) throws Exception {
        List<String> keyList = new ArrayList<>();
        List<String> bankaccountList = new ArrayList<>();
        for(CommonRequestDataVo dataVo : queryDataVoList){
            keyList.add(dataVo.getAccentity()+dataVo.getAccount()+dataVo.getAccount());
            bankaccountList.add(dataVo.getAccount());
        }
        CommonResponseDataVo resultVo = ctmCmpJournalCommonService.queryJournalBalanceSettled(bankaccountList,keyList);
        return resultVo;
    }
}
