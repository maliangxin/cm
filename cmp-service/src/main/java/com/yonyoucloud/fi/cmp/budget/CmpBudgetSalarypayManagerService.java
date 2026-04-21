package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay;
import com.yonyoucloud.fi.cmp.salarypay.Salarypay_b;
import org.imeta.orm.base.BizObject;

import java.util.List;

public interface CmpBudgetSalarypayManagerService {
    /**
     * * 查询资金付款单
     *
     * @return
     */
    CtmJSONArray querySalarypay(CmpBudgetEventBill budgetEventBill) throws Exception;

    void updateSalaryPay(List<Salarypay> salarypays, Short isOccupyBudget) throws Exception;
    ResultBudget gcExecuteSubmit(BizObject bizObject, Salarypay_b salarypay_b, String billCode, String billAction) throws Exception;

    ResultBudget gcExecuteBatchSubmit(BizObject bizObject, List<Salarypay_b> salarypay_bs, String billCode, String billAction) throws Exception;

    /**
     * 撤回实占
     *
     * @param bizObject
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget gcExecuteTrueUnAudit(BizObject bizObject,  String billCode, String billAction) throws Exception;

    /**
     * 删除预占
     *
     * @param bizObject 单据信息
     * @param billCode  单据编码
     * @return
     */
    ResultBudget gcExecuteDelete(BizObject bizObject, Salarypay_b salarypay_b, String billCode, String billAction) throws Exception;

    ResultBudget gcExecuteBatchUnSubmit(BizObject bizObject, String billCode, String billAction) throws Exception;

    /**
     * 预算实占
     *
     * @param bizObject
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget gcExecuteTrueAudit(BizObject bizObject, String billCode, String billAction,boolean deleteBudget) throws Exception;

    /**
     * 删除预占
     *
     * @param payBill
     * @return
     */
    void executeBudgetDelete(Salarypay payBill) throws Exception;

    /**
     * * 联查控制明细查询
     * @param param
     * @return
     */

    String queryBudgetDetail(CtmJSONObject param);

    String budgetCheckNew(List<BizObject> bizObjects, String billnum, String submit);
    /**
     * 删除旧的预算，匹配新的预算
     *
     * @param newBill
     * @throws Exception
     */
    void reMatchBudget(Salarypay newBill,boolean reMatch) throws Exception;

    /**
     * 删除预占
     *
     * @param bizObject 单据信息
     * @param billCode  单据编码
     * @return
     */
    ResultBudget releaseBudget(BizObject bizObject, Salarypay salarypay, String billCode, String billAction) throws Exception;
    /**
     * 预占
     *
     * @param bizObject
     * @param salarypay
     * @param billCode
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget budget(BizObject bizObject, Salarypay salarypay, String billCode, String billAction) throws Exception;

    /**
     * 占用预算且更新单据状态
     *
     * @param salarypay
     * @throws Exception
     */
    void executeSubmit(Salarypay salarypay) throws Exception;

    void executeAuditDelete(Salarypay salarypay) throws Exception;
    void executeAuditDeleteReBudget(Salarypay salarypay) throws Exception;
}
