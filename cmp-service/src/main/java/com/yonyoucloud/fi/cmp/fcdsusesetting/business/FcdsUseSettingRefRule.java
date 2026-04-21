package com.yonyoucloud.fi.cmp.fcdsusesetting.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author guoxh
 */
@Component("fcdsUseSettingRefRule")
@Slf4j
public class FcdsUseSettingRefRule extends AbstractCommonRule {
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billnum = billContext.getBillnum();
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        BizObject bizObject = null;
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills.size() > 0) {
            bizObject = bills.get(0);
        }
        //交易类型参照根据选择的业务对象的busiobjCode进行过滤
        if ("transtype.bd_billtyperef".equals(billDataDto.getrefCode())) {
            if(bizObject.containsKey("fcDsUseSettingList")){
                List<BizObject> fcDsUseSettingList =  bizObject.get("fcDsUseSettingList");
                BizObject fcDsUseSetting = fcDsUseSettingList.get(0);
                if(fcDsUseSetting.containsKey("bizObjectCode") && fcDsUseSetting.get("bizObjectCode") != null){
                    QuerySchema schema = QuerySchema.create();
                    schema.addSelect("id");
                    QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                    conditionGroup.appendCondition(QueryCondition.name("busiobjCode").eq(fcDsUseSetting.get("bizObjectCode")));
                    schema.addCondition(conditionGroup);
                    List<Map<String, Object>> list = MetaDaoHelper.query("bd.bill.BillTypeVO", schema,IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
                    Set<String> billTypeIds = list.stream().map(item -> (String)(item.get("id"))).collect(Collectors.toSet());
                    if(CollectionUtils.isNotEmpty(billTypeIds)) {
                        billDataDto.appendCondition("billtype_id", ICmpConstant.QUERY_IN, billTypeIds);
                    }else{
                        billDataDto.appendCondition("1", ICmpConstant.QUERY_EQ, "2");
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
