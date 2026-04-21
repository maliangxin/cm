package com.yonyoucloud.fi.cmp.bill;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.arap.IBillNum;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.constant.ISystemCodeConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <h1>款项类型参照对现金公共过滤</h1>
 *
 * @author yangjn
 * @version 1.0
 * @since 2022-2-24 19:11
 */
@Component
public class QuickTypeForCmFilterReferRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        //判断是卡片还是查询区域
        boolean iscard = !"filter".equals(billDataDto.getExternalData());
        if (IRefCodeConstant.FINBD_BD_PAYMENTTYPEREF.equals(billDataDto.getrefCode()) && iscard) {
            List<BizObject> bills = getBills(billContext, paramMap);
            if(bills == null || bills.size() == 0){//可能是由UI模板过来的，需要判空
                return new RuleExecuteResult();
            }
            BizObject bill = bills.get(0);
            //区分收款、付款
            switch (billDataDto.getBillnum()) {
                case IBillNum.CMP_RECEIVEBILL:
                    //应收未启用
                    if(QueryBaseDocUtils.queryOrgPeriodBeginDate(bill.get("accentity"), ISystemCodeConstant.ORG_MODULE_AR)==null){
                        //由于ICmpConstant.QUERY_NEQ经测试不可用，故这里使用ICmpConstant.QUERY_IN 过滤
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NOTIN,
                                new Integer[]{5,6}));
                    }else
                        billDataDto.getCondition().appendCondition(ConditionOperator.and,
                                new SimpleFilterVO("code", ICmpConstant.QUERY_NOTIN, new Integer[]{1,2,5,6}));
                    break;
                case IBillNum.CMP_PAYMENT:
                    //应付未启用
                    if(QueryBaseDocUtils.queryOrgPeriodBeginDate(bill.get("accentity"), ISystemCodeConstant.ORG_MODULE_AP)==null){
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NOTIN,
                                new Integer[]{1,2}));
                    }else
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NOTIN,
                                new Integer[]{1,2,5,6}));
                    break;
                default:
                    break;
            }

        }
        return new RuleExecuteResult();
    }
}
