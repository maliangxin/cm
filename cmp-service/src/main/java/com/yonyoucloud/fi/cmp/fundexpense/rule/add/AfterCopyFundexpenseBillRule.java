package com.yonyoucloud.fi.cmp.fundexpense.rule.add;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.tmsp.openapi.ITmspSystemRespRpcService;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemReq;
import com.yonyoucloud.fi.tmsp.vo.TmspSystemResp;
import org.imeta.biz.base.BizContext;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.JsonFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component("afterCopyFundexpenseBillRule")
public class AfterCopyFundexpenseBillRule extends AbstractCommonRule {

    @Autowired
    private ITmspSystemRespRpcService iTmspSystemRespRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String fullname = billContext.getFullname();
        if (bills != null && bills.size() > 0) {
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            // 补充单据日期
            fundexpense.setVouchdate(BillInfoUtils.getBusinessDate());
            // 补充费用参数，用于计算已结算金额等
            TmspSystemReq tmspSystemReq = new TmspSystemReq();
            tmspSystemReq.setApplyname("6");
            tmspSystemReq.setServicename("162");
            List<TmspSystemResp> tmspSystemResp = iTmspSystemRespRpcService.querySystemParameters(tmspSystemReq);
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(tmspSystemResp)) {
                TmspSystemResp collectionParam = tmspSystemResp.get(0);
                if (collectionParam.getSettlementprocessingmode() != null && !"".equals(collectionParam.getSettlementprocessingmode())) {
                    fundexpense.setExpenseparam(Short.valueOf(collectionParam.getSettlementprocessingmode()));
                } else {
                    fundexpense.setExpenseparam((short) 2);
                }
            }
        }
        JsonFormatter formatter = new JsonFormatter(BizContext.getMetaRepository());
        String json = formatter.toJson(bills, fullname, true).toString();
        this.putParam(paramMap, "return", json);
        return new RuleExecuteResult();
    }


}
