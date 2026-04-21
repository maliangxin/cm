package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.event.vo.CmpBudgetEventBill;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import org.imeta.orm.base.BizObject;

import java.util.List;

public interface CmpBudgetReceivemarginManagerService {
    /**
     * * 查询资金付款单
     *
     * @return
     */
    CtmJSONArray queryReceivemargin(CmpBudgetEventBill budgetEventBill) throws Exception;

    /**
     * * 更新
     *
     * @param receiveMargins
     * @return
     * @throws Exception
     */
    void updateReceivemargin(List<ReceiveMargin> receiveMargins, Short isOccupyBudget) throws Exception;

    /**
     * * 联查控制明细查询
     *
     * @param param
     * @return
     */

    String queryBudgetDetail(CtmJSONObject param);

    /**
     * 预算检查
     *
     * @param bizObjects
     * @param billnum
     * @param submit
     * @return
     */
    String budgetCheckNew(List<BizObject> bizObjects, String billnum, String submit);

    /**
     * 删除预占，执行实占
     *
     * @param bizObject
     * @param receiveMargin
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget gcExecuteTrueAudit(BizObject bizObject, ReceiveMargin receiveMargin, String billCode, String billAction, boolean deleteBudget, boolean checkResult) throws Exception;

    /**
     * 撤回实占
     *
     * @param bizObject
     * @param receiveMargin
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget gcExecuteTrueUnAudit(BizObject bizObject, ReceiveMargin receiveMargin, String billCode, String billAction) throws Exception;

    /**
     * 预占
     *
     * @param bizObject
     * @param receiveMargin
     * @param billCode
     * @param billAction
     * @return
     * @throws Exception
     */
    ResultBudget budget(BizObject bizObject, ReceiveMargin receiveMargin, String billCode, String billAction) throws Exception;

    /**
     * 删除预占
     *
     * @param bizObject 单据信息
     * @param billCode  单据编码
     * @return
     */
    ResultBudget releaseBudget(BizObject bizObject, ReceiveMargin receiveMargin, String billCode, String billAction) throws Exception;


    /**
     * 删除预占且更新单据状态
     *
     * @param receiveMargin
     * @return
     */
    void executeSubmitDelete(ReceiveMargin receiveMargin) throws Exception;


    /**
     * 占用预算且更新单据状态
     *
     * @param receiveMargin
     * @throws Exception
     */
    void executeSubmit(ReceiveMargin receiveMargin) throws Exception;

    /**
     * 带判断的实占
     *
     * @param receiveMargin
     * @param deleteBudget
     * @return
     * @throws Exception
     */
    boolean budgetSuccess(ReceiveMargin receiveMargin, boolean deleteBudget, boolean checkResult) throws Exception;

    /**
     * 删除旧的预算，匹配新的预算
     *
     * @param newBill
     * @throws Exception
     */
    void reMatchBudget(ReceiveMargin newBill) throws Exception;

    /**
     * 未启用审批流，点击提交
     *
     * @param receiveMargin
     * @throws Exception
     */
    void executeAudit(ReceiveMargin receiveMargin) throws Exception;
}
