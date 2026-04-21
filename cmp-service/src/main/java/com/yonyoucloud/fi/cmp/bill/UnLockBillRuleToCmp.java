package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 释放Openapi幂等性锁业务规则
 */
@Slf4j
@Component
public class UnLockBillRuleToCmp extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {
            boolean formApi = ((bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true))|| billDataDto.getFromApi()) && bizObject.getEntityStatus().name().equals("Insert");
            boolean systemOut = bizObject.get("srcitem").equals(EventSource.SystemOut.getValue()) && bizObject.getEntityStatus().name().equals("Insert");
            if (formApi || systemOut){
                // 释放Openapi幂等性锁
                if (bizObject.getEntityName().equals(PayApplicationBill.ENTITY_NAME)){
                    JedisLockUtils.unlockRuleWithOutTrace(map);
                } else {
                    if (!StringUtils.isEmpty(bizObject.get(ICmpConstant.SRCBILLID))) {
                        JedisLockUtils.unlockRuleWithOutTrace(map);
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }

}
