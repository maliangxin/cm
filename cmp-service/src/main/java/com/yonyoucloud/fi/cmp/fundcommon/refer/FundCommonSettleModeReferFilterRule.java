package com.yonyoucloud.fi.cmp.fundcommon.refer;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryOrderby;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <h1>资金付款单结算方式过滤</h1>
 *
 * 结算方式的业务属性为信用证业务时，service_attr值为9
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2021-12-14 9:59
 */
@Component
public class FundCommonSettleModeReferFilterRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        FilterVO condition = ValueUtils.isNotEmptyObj(billDataDto.getTreeCondition()) ? billDataDto.getTreeCondition() : new FilterVO();
        if ("productcenter.aa_settlemethodref".equals(billDataDto.getrefCode())) {
            List<BizObject> bills = getBills(billContext, map);
            if(bills != null && bills.size() >0){
                String billnum = billContext.getBillnum();
                if (bills.get(0) != null && bills.get(0).get("billtype") != null) {
                    short billtype = bills.get(0).getShort("billtype");
                    boolean iscard = !"filter".equals(billDataDto.getExternalData());
                    Short associationStatus = null;
                   if (IBillNumConstant.FUND_PAYMENT.equals(billnum)){
                       List<FundPayment_b> fundPaymentSubList = bills.get(0).getBizObjects("FundPayment_b", FundPayment_b.class);
                       if (CollectionUtils.isNotEmpty(fundPaymentSubList)){
                           associationStatus = fundPaymentSubList.get(0).getAssociationStatus();
                       }
                   } else if(IBillNumConstant.FUND_COLLECTION.equals(billnum)){
                       List<FundCollection_b> fundCollectionSubList = bills.get(0).getBizObjects("FundCollection_b", FundCollection_b.class);
                       if (CollectionUtils.isNotEmpty(fundCollectionSubList)){
                           associationStatus = fundCollectionSubList.get(0).getAssociationStatus();
                       }
                   }
                    //如果是单据类型银行对账单，并且是卡片，且是资金收付款单时，则过滤时只能选择银行转账和支票结算
                    if((billtype == EventType.CashMark.getValue()
                            || billtype == EventType.BillClaim.getValue()
                            || (ValueUtils.isNotEmptyObj(associationStatus)
                            && associationStatus== AssociationStatus.Associated.getValue()))
                            && iscard
                            && (IBillNumConstant.FUND_PAYMENT.equals(billnum)
                            || IBillNumConstant.FUND_COLLECTION.equals(billnum))){
                        condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0,8}));
                        billDataDto.setTreeCondition(condition);
                        return new RuleExecuteResult();
                    }
                }
            }
            condition.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0,1,2,8,10}));
            List<QueryOrderby> queryOrderlyList = new ArrayList<>();
            QueryOrderby orders = new QueryOrderby("order",ICmpConstant.ORDER_ASC);
            queryOrderlyList.add(orders);
            billDataDto.setQueryOrders(queryOrderlyList);
            billDataDto.setTreeCondition(condition);
        }
        return new RuleExecuteResult();
    }


}
