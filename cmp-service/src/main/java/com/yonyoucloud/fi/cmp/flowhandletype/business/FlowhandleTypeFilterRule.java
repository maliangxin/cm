package com.yonyoucloud.fi.cmp.flowhandletype.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.flowhandletype.service.IFlowHandleTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("flowhandleTypeFilterRule")
@Slf4j
public class FlowhandleTypeFilterRule extends AbstractCommonRule {
    @Autowired
    private IFlowHandleTypeService flowHandleTypeService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(AppContext.getTenantId());
        tenant.setYTenantId(AppContext.getYTenantId());

        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if ("cmp_flowhandleTypeList".equals(billDataDto.getBillnum())) {
            flowHandleTypeService.initIdentifyTypeData(tenant, billDataDto.getBillnum());
            billDataDto.appendCondition("type", "eq", 2);
        } else if ("cmp_fcdsUsesetTypeList".equals(billDataDto.getBillnum())) {
            flowHandleTypeService.initIdentifyTypeData(tenant, billDataDto.getBillnum());
            billDataDto.appendCondition("type", "eq", 1);
        }
        return new RuleExecuteResult();
    }
}
