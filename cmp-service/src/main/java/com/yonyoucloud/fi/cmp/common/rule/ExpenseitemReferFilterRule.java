package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本类用于费用项目的过滤
 * 费用项目档案增加启用领域，现金管理仅允许选择财资业务的费用项目*
 * @author xuxbo
 * @date 2022/12/1 13:58
 */

@Component
public class ExpenseitemReferFilterRule extends AbstractCommonRule {

    /**
     *  需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public ExpenseitemReferFilterRule() {
        //付款申请工作台
        BILLNUM_MAP.add(IBillNumConstant.PAYAPPLICATIONBILL);
        //收款工作台
        BILLNUM_MAP.add(IBillNumConstant.RECEIVE_BILL);
        //付款工作台
        BILLNUM_MAP.add(IBillNumConstant.PAYMENT);
        //资金收款单
        BILLNUM_MAP.add(IBillNumConstant.FUND_COLLECTION);
        //资金付款单
        BILLNUM_MAP.add(IBillNumConstant.FUND_PAYMENT);
        //薪资支付
        BILLNUM_MAP.add(IBillNumConstant.SALARYPAY);
        //外汇付款
        BILLNUM_MAP.add(IBillNumConstant.CMP_FOREIGNPAYMENT);
    }

    /**
     * 对费用项目进行过滤，过滤费用项目为财资服务的费用项目
     * @param billContext
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)) {
            RefEntity refentity = billDataDto.getRefEntity();
            if ("bd_expenseitemref".equals(refentity.code)) {
                if("tree".equalsIgnoreCase(billDataDto.getDataType())){
                    if(billDataDto.getTreeCondition() == null){
                        FilterVO conditon = new FilterVO();
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("propertybusiness", "eq", "1"));
                        billDataDto.setTreeCondition(conditon);
                    }else{
                        billDataDto.getTreeCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("propertybusiness", "eq", "1"));
                    }
                }else{
                    if(billDataDto.getCondition() == null){
                        FilterVO conditon = new FilterVO();
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("propertybusiness", "eq", "1"));
                        billDataDto.setCondition(conditon);
                    }else{
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("propertybusiness", "eq", "1"));
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
