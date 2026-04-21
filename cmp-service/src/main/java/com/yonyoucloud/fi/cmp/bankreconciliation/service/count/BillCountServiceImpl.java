package com.yonyoucloud.fi.cmp.bankreconciliation.service.count;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.reqvo.IncomeAndExpenditureReqVO;
import com.yonyoucloud.ctm.stwb.respvo.IncomeAndExpenditureResVO;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.stct.api.openapi.IBusinessDelegationApiService;
import com.yonyoucloud.fi.stct.api.openapi.common.dto.Result;
import com.yonyoucloud.fi.stct.api.openapi.request.QueryDelegationReqVo;
import com.yonyoucloud.fi.stct.api.openapi.vo.businessDelegation.BusinessDelegationVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class BillCountServiceImpl implements IBillCountService {


    @Autowired
    BankreconciliationCountServiceImpl bankreconciliationCountServiceImpl;
    @Autowired
    ClaimCenterCountServiceImpl claimCenterCountServiceImpl;
    @Autowired
    MybillclaimCountServiceImpl mybillclaimCountServiceImpl;

    @Override
    public HashMap<String, Object> getCount(CtmJSONObject params) throws Exception {

        String billNo = params.getString("billNo");//页面编码

        switch (billNo) {
            case IBillNumConstant.BANKRECONCILIATIONLIST:
                return bankreconciliationCountServiceImpl.getCount(params);
            case IBillNumConstant.CMP_MYBILLCLAIM_LIST:
                return mybillclaimCountServiceImpl.getCount(params);
            case IBillNumConstant.CMP_BILLCLAIMCENTER_LIST:
                return claimCenterCountServiceImpl.getCount(params);
            default:
                return null;
        }
    }


}
