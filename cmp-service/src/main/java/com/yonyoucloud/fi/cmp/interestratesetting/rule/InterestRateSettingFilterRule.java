package com.yonyoucloud.fi.cmp.interestratesetting.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import org.imeta.core.base.ConditionOperator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 银行账户利率设置列表 过滤 只显示规则状态启用的数据*
 *
 * @author xuxbo
 * @date 2023/5/5 15:13
 */

@Component("interestRateSettingFilterRule")
public class InterestRateSettingFilterRule extends AbstractCommonRule {

    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public InterestRateSettingFilterRule() {
        //银行账户利率设置
        BILLNUM_MAP.add(IBillNumConstant.INTERESTRATESETTINGLIST);
    }


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)) {
            if(billDataDto.getCondition() != null){
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("ruleStatus", ICmpConstant.QUERY_EQ, 1));
            }else {
                FilterVO filterVO = new FilterVO();
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("ruleStatus", ICmpConstant.QUERY_EQ, 1));
                billDataDto.setCondition(filterVO);
            }
        }
        return new RuleExecuteResult();
    }
}
