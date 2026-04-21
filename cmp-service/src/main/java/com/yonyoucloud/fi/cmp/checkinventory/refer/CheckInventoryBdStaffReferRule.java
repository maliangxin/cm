package com.yonyoucloud.fi.cmp.checkinventory.refer;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 支票盘点员工参照过滤
 */
@Slf4j
@Component("checkInventoryBdStaffReferRule")
public class CheckInventoryBdStaffReferRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if (!IBillNumConstant.CHECKINVENTORY.equals(billDataDto.getBillnum()) && !IBillNumConstant.CHECKINVENTORY_LIST.equals(billContext.getBillnum())) {
            return new RuleExecuteResult();
        }
        List<BizObject> bills = getBills(billContext, paramMap);

        BizObject bill = null;
        if (bills.size() > 0) {
            bill = bills.get(0);
        }
        if (IRefCodeConstant.BD_STAFF_REF.equals(billDataDto.getrefCode())) {
            String accentity = bill.get("accentity").toString();
            billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("org_id", ICmpConstant.QUERY_EQ, accentity));
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
            billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
        }
        return new RuleExecuteResult();
    }
}
