package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.payPullApply;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.CloseStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayBillStatus;
import com.yonyoucloud.fi.cmp.payapplicationbill.SourceMatters;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryOrderby;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_ONE;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CONSTANT_ZERO;

/**
 * <h1>款单拉取付款申请单时的数据过滤</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2020-11-16 9:37
 */
@Component("paymentPullPayApplyBillFilterQueryRule")
public class PaymentPullPayApplyBillFilterQueryRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        FilterVO filterVO = new FilterVO();
        if (billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
        }
        List<String> orgs = AuthUtil.getAccentitys(billContext,null,"cmp_payment",false);
        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "unpaidAmountSum", ICmpConstant.QUERY_NEQ, BigDecimal.ZERO));
        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "srcitem", ICmpConstant.QUERY_EQ, SourceMatters.ManualInput.getValue()));
        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "closeStatus", ICmpConstant.QUERY_EQ, CloseStatus.Normal.getValue()));
        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "payBillStatus", ICmpConstant.QUERY_IN,
                new Short[]{PayBillStatus.PendingApproval.getValue(),PayBillStatus.PendingPayment.getValue(),PayBillStatus.PartialPayment.getValue()}));
        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "status", ICmpConstant.QUERY_EQ, CONSTANT_ONE));
        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "preemptAmountFull", ICmpConstant.QUERY_EQ, CONSTANT_ZERO));
        List<QueryOrderby> queryOrderlyList = new ArrayList<>(1);
        if(CollectionUtils.isNotEmpty(orgs)) {
            billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, IBussinessConstant.ACCENTITY, ICmpConstant.QUERY_IN, orgs));
        }
        QueryOrderby orders = new QueryOrderby("distanceProposePaymentDateDays",ICmpConstant.ORDER_ASC);
        queryOrderlyList.add(orders);
        billDataDto.setQueryOrders(queryOrderlyList);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }
}
