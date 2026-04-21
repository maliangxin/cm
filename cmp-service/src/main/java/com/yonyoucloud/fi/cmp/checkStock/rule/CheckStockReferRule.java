package com.yonyoucloud.fi.cmp.checkStock.rule;

import com.yonyou.iuap.org.service.itf.core.IFundsOrgQueryService;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * 支票入库，参照过滤规则
 */
@Slf4j
@Component
public class CheckStockReferRule extends AbstractCommonRule {
    @Autowired
    IFundsOrgQueryService orgService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billnum = billContext.getBillnum();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        BizObject bizObject = null;
        List<BizObject> bills = getBills(billContext, paramMap);
        String custNo = null;
        if (bills.size() > 0) {
            bizObject = bills.get(0);
            custNo = bizObject.get("custNo");
        }
        if (billDataDto.getrefCode() != null && IRefCodeConstant.FUNDS_ORG_ADN_FINANCE_ORG_LIST.contains(billDataDto.getrefCode()) && "cmp_checktablelist".equals(billnum) && ICmpConstant.VIRTUAL.equals(billDataDto.getFullname()) && StringUtils.isNotEmpty(custNo)) {
            List<String> childrenOrgIdsByIds = orgService.getChildrenOrgIdsByIds(Collections.singletonList(custNo), AppContext.getYTenantId(), Boolean.TRUE, Collections.singletonList(1));
            log.error("childrenOrgIdsByIds--------"+childrenOrgIdsByIds);
            billDataDto.getTreeCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", childrenOrgIdsByIds));
        }

        return new RuleExecuteResult();
    }

}
