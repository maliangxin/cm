package com.yonyoucloud.fi.cmp.fundcollection.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.cspl.vo.response.CapitalPlanExecuteResp;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundPlanOccupancyTipsService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
@Transactional
public class FundPlanCollectionTipsServiceImpl implements FundPlanOccupancyTipsService {

    @Resource
    private CmCommonService<Object> commonService;

    @Override
    public CtmJSONObject createFundPlanOccupancyTips(CtmJSONObject param) throws Exception {
        String billnum = param.getString("billnum");
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        if (!IBillNumConstant.FUND_COLLECTION.equals(billnum)){
            return ctmJSONObject;
        }
        //获取资金付款单信息
        FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, param.get("id"), 2);
        List<BizObject> checkFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> fundCollectionBList = fundCollection.get("FundCollection_b");
        for (BizObject biz : fundCollectionBList) {
            if (biz.get("entrustReject") != null && biz.getInteger("entrustReject") == 1) {
                continue;
            }
            Short settleStatus = biz.getShort("settlestatus");
            if (settleStatus == null || settleStatus == FundSettleStatus.Refund.getValue()) {
                continue;
            }
            if (biz.get(ICmpConstant.FUND_PLAN_PROJECT) != null) {
                biz.set(ICmpConstant.IS_TO_PUSH_CSPL, 1);
                checkFundBillForFundPlanProjectList.add(biz);
            }
        }
        List<CapitalPlanExecuteModel> checkObject = null;
        if (CollectionUtils.isNotEmpty(checkFundBillForFundPlanProjectList)) {
            Map<String, Object> map = new HashMap<>();
            map.put(ICmpConstant.ACCENTITY, fundCollection.get(ICmpConstant.ACCENTITY));
            map.put(ICmpConstant.VOUCHDATE, fundCollection.get(ICmpConstant.VOUCHDATE));
            map.put(ICmpConstant.CODE, fundCollection.get(ICmpConstant.CODE));
            checkObject = commonService.putCheckParameter(checkFundBillForFundPlanProjectList, IStwbConstant.CHENK, IBillNumConstant.FUND_COLLECTION, map);
        }
        if (CollectionUtils.isNotEmpty(checkObject)) {
            CapitalPlanExecuteResp capitalPlanExecuteResp = AppContext.getBean(CapitalPlanExecuteService.class).checkNew(checkObject);
            if (!Objects.isNull(capitalPlanExecuteResp) && capitalPlanExecuteResp.getSuccessCount()>0) {
                CtmJSONObject failMsg =  capitalPlanExecuteResp.getFailedRecords();
                CtmJSONObject successMsg =  capitalPlanExecuteResp.getSucessRecords();
                ctmJSONObject.put("msg",capitalPlanExecuteResp.getMessage());
            }
        }
        return ctmJSONObject;
    }
}
