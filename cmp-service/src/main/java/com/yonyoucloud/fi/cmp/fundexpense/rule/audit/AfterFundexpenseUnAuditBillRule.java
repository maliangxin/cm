package com.yonyoucloud.fi.cmp.fundexpense.rule.audit;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.QuerySettledDetailModel;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("afterDischargeUnAuditBillRule")
public class AfterFundexpenseUnAuditBillRule extends AbstractCommonRule {


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            //获取前端传过来的值对象
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            Short expenseparam = fundexpense.getExpenseparam();
            //资金结算
            if(expenseparam == 0){
                QuerySettledDetailModel querySettledDetailModel = new QuerySettledDetailModel();
                querySettledDetailModel.setBusinessId(fundexpense.getId().toString());
                querySettledDetailModel.setWdataorigin(8);// 来源业务系统，现金管理
                List<String> ids = new ArrayList<>();
                ids.add(fundexpense.getId().toString());
                querySettledDetailModel.setBusinessDetailsIds(ids);
                RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).datasettledDelete(querySettledDetailModel);
            }
            //应收应付
            if(expenseparam == 1){
            }
        }
        return new RuleExecuteResult();
    }

}
