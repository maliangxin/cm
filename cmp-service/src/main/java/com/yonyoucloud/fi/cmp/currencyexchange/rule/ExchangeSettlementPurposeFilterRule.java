package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
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
 * 本类用于结售汇用途过滤
 * 根据多语环境进行过滤*
 * @author xuxbo
 * @date 2023/1/13
 */

@Component
public class ExchangeSettlementPurposeFilterRule extends AbstractCommonRule {

    /**
     *  需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public ExchangeSettlementPurposeFilterRule() {
        //外币兑换
        BILLNUM_MAP.add(IBillNumConstant.CURRENCYEXCHANGE);
        BILLNUM_MAP.add(IBillNumConstant.CURRENCYAPPLY);
    }


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        String locale = InvocationInfoProxy.getLocale();
        //多语类型
        Integer multilangtype;
        switch (locale) {
            case "zh_CN":
                multilangtype = 1;
                break;
            case "en_US":
                multilangtype = 2;
                break;
            case "zh_TW":
                multilangtype = 3;
                break;
            default:
                multilangtype = 1;
        }

        if (BILLNUM_MAP.contains(billnum)) {
            RefEntity refentity = billDataDto.getRefEntity();
            FilterVO filterVO = new FilterVO();
            if ("cmp_exchangesettlement_purpose_ref".equals(refentity.code)) {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("multilangtype", "eq", multilangtype));
                billDataDto.setCondition(filterVO);
            }
        }
        return new RuleExecuteResult();
    }
}
