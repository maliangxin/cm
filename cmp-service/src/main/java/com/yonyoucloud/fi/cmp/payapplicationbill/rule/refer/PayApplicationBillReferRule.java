package com.yonyoucloud.fi.cmp.payapplicationbill.rule.refer;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceSystem;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>description</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-12-08 19:11
 */
@Component("payApplicationBillReferRule")
public class PayApplicationBillReferRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        List<BizObject> billContexts = getBills(billContext, map);
        BizObject bizObject = null;
        if (ValueUtils.isNotEmptyObj(billContexts)) {
            bizObject = billContexts.get(0);
        }
        String refCode = billDataDto.getrefCode();
        FilterVO condition = ValueUtils.isNotEmptyObj(billDataDto.getCondition()) ? billDataDto.getCondition() : new FilterVO();
        if ("transtype.bd_billtyperef".equals(refCode) || "transtype.bd_billtyperef".equals(refCode)) {
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, "FICM3"));
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
            if (!"filter".equals(billDataDto.getExternalData())) {
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
            }
            if (ValueUtils.isNotEmptyObj(bizObject)
                    && ValueUtils.isNotEmptyObj(bizObject.get("sourceSystem"))
                    && Short.parseShort(bizObject.get("sourceSystem").toString()) == SourceSystem.Cash.getValue()) {
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NEQ, "order-apply"));
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NEQ, "contract-apply"));
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NEQ, "oap-apply"));
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NEQ, "oap-init-apply"));
                condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NEQ, "outsourcing-order-apply"));
            }
        }
        if ("ucf-org-center.aa_org".equals(refCode)) {
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("stopstatus", ICmpConstant.QUERY_EQ, "0"));
        }
        // 付款申请单付款部门参照，列表页时，过滤显示所有部门
        if ("ucf-org-center.bd_adminorgsharetreeref".equals(refCode) && IBillNumConstant.PAYAPPLICATIONBILLLIST.equals(billDataDto.getParameter("sourceBillnum"))) {
            billDataDto.setExternalData("filter");
        }
        billDataDto.setCondition(condition);
        return new RuleExecuteResult();
    }
}
