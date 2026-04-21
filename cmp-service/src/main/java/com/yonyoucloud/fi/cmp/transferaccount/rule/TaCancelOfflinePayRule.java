package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.transferaccount.service.TransferAccountSettleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("taCancelOfflinePayRule")
@Slf4j
public class TaCancelOfflinePayRule extends AbstractCommonRule {

    @Autowired
    TransferAccountSettleService transferAccountSettleService;

    /**
     * 转账单，取消线下支付
     * @param billContext
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        CtmJSONArray jsonArray = new CtmJSONArray();
        CtmJSONObject jsonObject = CtmJSONObject.parseObject((String) map.get("requestData"));
        jsonArray.add(jsonObject);
        CtmJSONObject requestData = new CtmJSONObject();
        requestData.put("row", jsonArray);
        transferAccountSettleService.cancelOfflinePay(requestData);
        return new RuleExecuteResult();
    }
}
