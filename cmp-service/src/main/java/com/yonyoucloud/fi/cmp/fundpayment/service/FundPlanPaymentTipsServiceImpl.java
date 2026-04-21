package com.yonyoucloud.fi.cmp.fundpayment.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.cspl.vo.response.CapitalPlanExecuteResp;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundPlanOccupancyTipsService;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
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
public class FundPlanPaymentTipsServiceImpl implements FundPlanOccupancyTipsService {

    @Resource
    private CmCommonService<Object> commonService;
    @Resource
    private IFundCommonService fundCommonService;

    @Override
    public CtmJSONObject createFundPlanOccupancyTips(CtmJSONObject param) throws Exception {
        String billnum = param.getString("billnum");
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        boolean checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundPayment.getValue());
        if (!IBillNumConstant.FUND_PAYMENT.equals(billnum) || !checkFundPlanIsEnabled){
            return ctmJSONObject;
        }
        //获取资金付款单信息
        FundPayment fundPayment = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, param.get("id"), 2);
        if (Objects.isNull(fundPayment)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100700"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180385", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        List<BizObject> checkFundBillForFundPlanProjectList = new ArrayList<>();
        List<BizObject> fundPaymentBList = fundPayment.get("FundPayment_b");
        for (BizObject biz : fundPaymentBList) {
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
            map.put(ICmpConstant.ACCENTITY, fundPayment.get(ICmpConstant.ACCENTITY));
            map.put(ICmpConstant.VOUCHDATE, fundPayment.get(ICmpConstant.VOUCHDATE));
            map.put(ICmpConstant.CODE, fundPayment.get(ICmpConstant.CODE));
            checkObject = commonService.putCheckParameter(checkFundBillForFundPlanProjectList, IStwbConstant.CHENK, IBillNumConstant.FUND_PAYMENT, map);
        }
        if (CollectionUtils.isNotEmpty(checkObject)) {
            CapitalPlanExecuteResp capitalPlanExecuteResp = AppContext.getBean(CapitalPlanExecuteService.class).checkNew(checkObject);
            if (!Objects.isNull(capitalPlanExecuteResp) && capitalPlanExecuteResp.getMessage() != null && capitalPlanExecuteResp.getMessage().size() > 0) {
//                CtmJSONObject failMsg =  capitalPlanExecuteResp.getFailedRecords();
//                CtmJSONObject successMsg =  capitalPlanExecuteResp.getSucessRecords();
                ctmJSONObject.put("msg",capitalPlanExecuteResp.getMessage());
            }
        }
        return ctmJSONObject;
    }
}
