package com.yonyoucloud.fi.cmp.foreignpayment.rule.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.stwb.StwbBillService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 外汇付款保存后规则*
 *
 * @author xuxbo
 * @date 2024/1/3 10:09
 */

@Slf4j
@Component
public class ForeignPaymentAfterSaveRule extends AbstractCommonRule {

    @Autowired
    @Qualifier("stwbForeignPaymentBillServiceImpl")
    private StwbBillService stwbBillService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        for (BizObject bizObject : bills) {

            try {
                ForeignPayment currentBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizObject.getId(), 1);
                //推送资金结算
                List<BizObject> currentBillList = new ArrayList<>();
                currentBillList.add(currentBill);
                stwbBillService.pushBill(currentBillList, true);// 推送资金结算 进行校验
            } catch (Exception e) {
                log.error("exception when pushing bill to settle: ", e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102546"),e.getMessage());
            }
        }
        return new RuleExecuteResult();
    }


}
