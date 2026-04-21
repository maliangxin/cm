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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本类用于结售汇来源代码参照过滤
 */

@Component
public class ExchangeSettlementSourceCodeFilterRule extends AbstractCommonRule {

    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public ExchangeSettlementSourceCodeFilterRule() {
        //外币兑换
        BILLNUM_MAP.add(IBillNumConstant.CURRENCYEXCHANGE);
        //外币申请
        BILLNUM_MAP.add(IBillNumConstant.CURRENCYAPPLY);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        String locale = InvocationInfoProxy.getLocale();
        //多语类型
        int multilangtype;
        switch (locale) {
            case "en_US":
                multilangtype = 2;
                break;
            case "zh_TW":
                multilangtype = 3;
                break;
            default:
                // "zh_CN"或其他
                multilangtype = 1;
        }
        if (BILLNUM_MAP.contains(billnum)) {
            RefEntity refentity = billDataDto.getRefEntity();
            FilterVO filterVO = new FilterVO();
            if ("cmp_sourcecodeRef".equals(refentity.code)) {
                List<BizObject> bills = getBills(billContext, map);
                if (CollectionUtils.isNotEmpty(bills)) {
                    String projectclassification = bills.get(0).get("projectclassification");
                    if (!StringUtils.isEmpty(projectclassification)) {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("projectclassifycode", "eq", projectclassification));
                    }
                    // 根据多语类型进行过滤
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("multilangtype", "eq", multilangtype));
                    billDataDto.setCondition(filterVO);
                }
            }
        }
        return new RuleExecuteResult();
    }
}
