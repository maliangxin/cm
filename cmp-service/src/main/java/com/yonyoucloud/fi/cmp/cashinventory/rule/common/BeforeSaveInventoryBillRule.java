package com.yonyoucloud.fi.cmp.cashinventory.rule.common;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.util.BigDecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 现金盘点保存前规则
 */
@Slf4j
@Component
public class BeforeSaveInventoryBillRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto dataDto = (BillDataDto) getParam(paramMap);
//        CtmJSONObject item = CtmJSONObject.parseObject(dataDto.getItem());
        BizObject bill = getBills(billContext, paramMap).get(0);
        Integer moneydigit = Integer.parseInt(bill.get("currency_moneyDigit"));
        //长款
        BigDecimal longstyle = null != bill.get("longstyle") ? bill.get("longstyle"): BigDecimal.ZERO;
        //短款
        BigDecimal shortie = null != bill.get("shortie") ? bill.get("shortie"): BigDecimal.ZERO;
        BigDecimal exchRate = null != bill.get("exchRate") ? bill.get("exchRate"): BigDecimal.ZERO;
        bill.set("longlocalamount", BigDecimalUtils.safeMultiply(longstyle,exchRate,moneydigit));
        bill.set("shortielocalamount", BigDecimalUtils.safeMultiply(shortie,exchRate,moneydigit));
        return new RuleExecuteResult();
    }

}
