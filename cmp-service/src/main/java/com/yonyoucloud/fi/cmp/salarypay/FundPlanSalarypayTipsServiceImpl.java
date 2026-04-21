package com.yonyoucloud.fi.cmp.salarypay;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.cspl.vo.response.CapitalPlanExecuteResp;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcommon.service.FundPlanOccupancyTipsService;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
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
public class FundPlanSalarypayTipsServiceImpl implements FundPlanOccupancyTipsService {

    @Resource
    private CmCommonService<Object> commonService;

    @Override
    public CtmJSONObject createFundPlanOccupancyTips(CtmJSONObject param) throws Exception {
        String billnum = param.getString("billnum");
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        if (IBillNumConstant.SALARYPAY.equals(billnum)){
            //获取薪资支付信息
            Salarypay salarypay = MetaDaoHelper.findById(Salarypay.ENTITY_NAME, param.get("id"));
            List<BizObject> checkFundBillForFundPlanProjectList = new ArrayList<>();

            if (salarypay.get(ICmpConstant.FUND_PLAN_PROJECT) != null) {
                checkFundBillForFundPlanProjectList.add(salarypay);
            }
            List<CapitalPlanExecuteModel> checkObject = null;
            if (CollectionUtils.isNotEmpty(checkFundBillForFundPlanProjectList)) {
                Map<String, Object> map = new HashMap<>();
                map.put(ICmpConstant.ACCENTITY, salarypay.get(ICmpConstant.ACCENTITY));
                map.put(ICmpConstant.VOUCHDATE, salarypay.get(ICmpConstant.VOUCHDATE));
                map.put(ICmpConstant.CODE, salarypay.get(ICmpConstant.CODE));
                checkObject = commonService.putCheckParameterSalarypay(checkFundBillForFundPlanProjectList, IStwbConstant.CHENK, IBillNumConstant.SALARYPAY, map);
            }
            if (CollectionUtils.isNotEmpty(checkObject)) {
                CapitalPlanExecuteResp capitalPlanExecuteResp = AppContext.getBean(CapitalPlanExecuteService.class).checkNew(checkObject);
                if (!Objects.isNull(capitalPlanExecuteResp) && capitalPlanExecuteResp.getSuccessCount()>0) {
                    CtmJSONObject failMsg =  capitalPlanExecuteResp.getFailedRecords();
                    CtmJSONObject successMsg =  capitalPlanExecuteResp.getSucessRecords();
                    List<String> msg = capitalPlanExecuteResp.getMessage();
                    ctmJSONObject.put("msg",msg);
                    ctmJSONObject.put("successMsg",successMsg);
                }
            }

        }
        return ctmJSONObject;
    }
}
