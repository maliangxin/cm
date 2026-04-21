package com.yonyoucloud.fi.cmp.fundcommon.service;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.cspl.capitalplanexecute.CapitalPlanExecuteService;
import com.yonyou.yonbip.ctm.cspl.vo.request.CapitalPlanExecuteModel;
import com.yonyou.yonbip.ctm.cspl.vo.response.CapitalPlanExecuteResp;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.constant.IStwbConstant;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class FundPlanOccupancyDataServiceImpl implements FundPlanOccupancyDataService{

    @Autowired
    private List<FundPlanOccupancyTipsService> fundPlanOccupancyTipsServiceList;

    @Override
    public List<CtmJSONObject> createFundPlanOccupancyData(CtmJSONObject param) {
        List<CtmJSONObject> funPlanOccupancyTips = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fundPlanOccupancyTipsServiceList)){
            funPlanOccupancyTips = fundPlanOccupancyTipsServiceList.stream().map(p->{
                try {
                    return p.createFundPlanOccupancyTips(param);
                } catch (Exception e) {
                    throw new CtmException(e.getMessage());
                }
            }).filter(Objects::nonNull).filter(p->p.size()>0).collect(Collectors.toList());
        }
        return funPlanOccupancyTips;
    }
}
