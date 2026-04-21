package com.yonyoucloud.fi.cmp.cashinventory.check;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.cashinventory.InventoryCheckService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @version 1.0
 * @since 2022-01-17
 */
@Slf4j
@Component
public class InventoryVouchdateCheckRule extends AbstractCommonRule {

    public final static String  VOUCHDATE = "vouchdate";

    @Autowired
    private InventoryCheckService inventoryCheckService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        if (!VOUCHDATE.equals(item.get("key"))) {
            return new RuleExecuteResult();
        }
        BizObject bill = getBills(billContext, paramMap).get(0);
        if(!inventoryCheckService.ruleCheck(bill,VOUCHDATE)){
            return new RuleExecuteResult();
        }
        //现金实点金额
        BigDecimal actualamount = null == bill.get("actualamount") ? BigDecimal.ZERO : bill.get("actualamount");
        inventoryCheckService.amountCalculation(bill);
        this.putParam(paramMap, "return", bill);
        return new RuleExecuteResult();
    }

}
