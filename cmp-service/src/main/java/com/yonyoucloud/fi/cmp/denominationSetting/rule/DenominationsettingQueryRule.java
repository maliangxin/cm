package com.yonyoucloud.fi.cmp.denominationSetting.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("denominationsettingQueryRule")
public class DenominationsettingQueryRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto)getParam(paramMap);
        FilterVO filterVO = new FilterVO();
        if(billDataDto.getCondition() != null) {
            filterVO = billDataDto.getCondition();
        }

        List<Map<String,Object>> list = QueryBaseDocUtils.queryCurrencyByCondition(new HashMap<>());

        Set<String> currencys = new HashSet<>();

        if(CollectionUtils.isNotEmpty(list)){
            for (Map<String,Object> map:list){
                if(ObjectUtils.isNotEmpty(map.get("id"))){
                    currencys.add(String.valueOf(map.get("id")));
                }
            }
        }

        billDataDto.setCondition((FilterVO) MddBaseUtils.appendCondition(filterVO, "currency", "in", currencys));

        return new RuleExecuteResult();
    }

}
