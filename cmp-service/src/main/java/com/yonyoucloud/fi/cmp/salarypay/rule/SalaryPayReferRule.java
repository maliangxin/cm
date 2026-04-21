package com.yonyoucloud.fi.cmp.salarypay.rule;


import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 薪资支付参照过滤
 * @author majfd
 *
 */
@Component
public class SalaryPayReferRule extends AbstractCommonRule {

    @Autowired
    CmCommonService commonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        BizObject bizObject = null;
        List<BizObject> bills = getBills(billContext, map);
        if (bills.size() > 0) {
            bizObject = bills.get(0);
        }
        boolean iscard= !"filter".equals(billDataDto.getExternalData());
        if ("transtype.bd_billtyperef".equals(billDataDto.getrefCode())) {
            FilterVO conditon = new FilterVO();
            if(billDataDto.getCondition() == null) {
                conditon.appendCondition(ConditionOperator.and,new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, "FICA6"));
                if(iscard) {
                    conditon.appendCondition(ConditionOperator.and,new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
                }
                conditon.appendCondition(ConditionOperator.and,new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
                billDataDto.setCondition(conditon);
            } else {
                billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, "FICA6"));
                if(iscard) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
                }
                billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
            }
        } else if ("ucfbasedoc.bd_enterprisebankacct".equals(billDataDto.getrefCode()) && bizObject != null) {
            if (bizObject.get("currency") != null) {
                billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, bizObject.get("currency")));
            }
            billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO(ICmpConstant.CURRENCY_ENABLE_REf, ICmpConstant.QUERY_EQ, "1"));
            if ("payBankAccount_name".equals(billDataDto.getKey()) && bizObject.get("payBankAccount") != null) {
                billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("payBankAccount")));
            }
            // 增加内部账户过滤
            billDataDto.getCondition().appendCondition(ConditionOperator.and,new SimpleFilterVO("acctopentype", ICmpConstant.QUERY_EQ, "0"));
        } else if ("productcenter.aa_settlemethodref".equals(billDataDto.getrefCode())) {
            FilterVO conditon = new FilterVO();
            if (billDataDto.getTreeCondition() == null){
                conditon.appendCondition(ConditionOperator.and,new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                billDataDto.setTreeCondition(conditon);
            } else {
                billDataDto.setTreeCondition((FilterVO) MddBaseUtils.appendCondition(billDataDto.getTreeCondition(), "serviceAttr", ICmpConstant.QUERY_EQ, 0));
            }
        } else if (IRefCodeConstant.REF_CSPL_FUND_PLAN.equals(billDataDto.getrefCode())){
            CmpCommonUtil.csplPlanReferFilter(billContext,billDataDto, bizObject);
        }
        return new RuleExecuteResult();
    }
}
