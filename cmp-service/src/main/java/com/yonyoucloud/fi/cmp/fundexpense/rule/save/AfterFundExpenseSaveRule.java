package com.yonyoucloud.fi.cmp.fundexpense.rule.save;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense_b;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("afterFundExpenseSaveRule")
public class AfterFundExpenseSaveRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            //获取前端传过来的值对象
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            // 费用明细编码赋值：主表code_i+1
            if (fundexpense.getCode() != null && fundexpense.detail() != null) {
                String code = fundexpense.getCode();
                List<Fundexpense_b> fundexpense_bs = fundexpense.detail();
                if (CollectionUtils.isNotEmpty(fundexpense_bs)) {
                    for (int i = 0; i < fundexpense_bs.size(); i++) {
                        fundexpense_bs.get(i).setExpense_detail_code(code + "_" + (i + 1));
                        fundexpense_bs.get(i).setPubts(null);
                    }
                    EntityTool.setUpdateStatus(fundexpense_bs);
                    MetaDaoHelper.update(Fundexpense_b.ENTITY_NAME, fundexpense_bs);
                }
            }
        }
        return new RuleExecuteResult();
    }

}
