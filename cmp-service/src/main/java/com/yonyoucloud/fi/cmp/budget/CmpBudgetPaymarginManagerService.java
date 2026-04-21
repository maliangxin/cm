package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import org.imeta.orm.base.BizObject;

import java.util.List;

public interface CmpBudgetPaymarginManagerService {
    /**
     * * 查询资金付款单
     *
     * @return
     */
    CtmJSONArray queryPaymargin(CmpBudgetEventBill budgetEventBill) throws Exception;

    /**
     * * 更新
     *
     * @param Paymargins
     * @return
     * @throws Exception
     */
    void updatePaymargin(List<PayMargin> Paymargins, Short isOccupyBudget) throws Exception;


    /**
     * 预算实占
     *
     * @param bizObject
     * @param payMargin
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget gcExecuteTrueAudit(BizObject bizObject, PayMargin payMargin, String billCode, String billAction, boolean deleteBudget,boolean checkResult) throws Exception;

    /**
     * 撤回实占
     *
     * @param bizObject
     * @param payMargin
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget gcExecuteTrueUnAudit(BizObject bizObject, PayMargin payMargin, String billCode, String billAction) throws Exception;


    ResultBudget budget(BizObject bizObject, PayMargin payMargin, String billCode, String billAction) throws Exception;

    /**
     * 删除预占
     *
     * @param bizObject 单据信息
     * @param billCode  单据编码
     * @return
     */
    ResultBudget releaseBudget(BizObject bizObject, PayMargin payMargin, String billCode, String billAction) throws Exception;


    /**
     * 删除预占且更新
     *
     * @param payMargin
     * @return
     */
    void executeSubmitDelete(PayMargin payMargin) throws Exception;

    boolean budgetSuccess(PayMargin bill_b, boolean deleteBudget, boolean checkResult) throws Exception;

    /**
     * * 联查控制明细查询
     *
     * @param param
     * @return
     */

    String queryBudgetDetail(CtmJSONObject param);

    String budgetCheckNew(List<BizObject> bizObjects, String billnum, String submit);

    void executeSubmit(PayMargin payMargin) throws Exception;

    void executeAudit(PayMargin payMargin) throws Exception;
    /**
     * 删除旧的预算，匹配新的预算
     *
     * @param newBill
     * @throws Exception
     */
    void reMatchBudget(PayMargin newBill) throws Exception;
}
