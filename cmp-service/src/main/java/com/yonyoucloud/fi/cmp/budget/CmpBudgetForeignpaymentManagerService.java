package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import org.imeta.orm.base.BizObject;

import java.util.List;

public interface CmpBudgetForeignpaymentManagerService {

    /**
     * 查询符合规则设置的数据
     *
     * @param budgetEventBill
     * @return
     * @throws Exception
     */
    CtmJSONArray queryBillByRule(CmpBudgetEventBill budgetEventBill) throws Exception;

    void updateBillList(List<ForeignPayment> foreignPayments, Short isOccupyBudget) throws Exception;

    boolean budget(ForeignPayment foreignPayment) throws Exception;

    boolean releaseBudget(ForeignPayment foreignPayment) throws Exception;

    /**
     * 带判断的实占
     *
     * @param foreignPayment
     * @return
     * @throws Exception
     */
    boolean implement(ForeignPayment foreignPayment) throws Exception;

    /**
     * 释放实占
     *
     * @param foreignPayment
     * @return
     * @throws Exception
     */
    boolean releaseImplement(ForeignPayment foreignPayment) throws Exception;

    void updateOccupyBudget(ForeignPayment foreignPayment, Short isOccupyBudget) throws Exception;


}
