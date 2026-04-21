package com.yonyoucloud.fi.cmp.budget;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.event.vo.ResultBudget;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import org.imeta.orm.base.BizObject;

import java.util.List;

public interface CmpBudgetManagerService {

    /**
     * * 是否购买预算
     * @return
     */
    boolean isCanUseBudget();
    /**
     * * 是否购买预算
     * @return
     */
    boolean isPushHistory()throws Exception;
    /**
     * * 是否财资公共配置开启
     * @return
     */
    boolean isStartBudget(String billNum);

    /**
     * * 是否开启预算
     * @param billNum
     * @return
     */
    boolean isCanStart(String billNum);

    boolean isCanUseSpecialBudget();

    /**
     * 执行预占
     * @param bizObject 单据信息
     * @param billCode 单据编码
     * @return
     */
    ResultBudget gcExecuteSubmit(BizObject bizObject, FundPayment_b fundPayment_b, String billCode,String billAction) throws Exception;

    ResultBudget gcExecuteBatchSubmit(BizObject bizObject, List<FundPayment_b> fundPayment_b, String billCode, String billAction) throws Exception;
    /**
     * 预占撤回
     * @param bizObject 单据信息
     * @param billCode 单据编码
     * @return
     */
    ResultBudget gcExecuteUnSubmit(BizObject bizObject, FundPayment_b fundPayment_b, String billCode,String billAction) throws Exception;

    ResultBudget gcExecuteBatchUnSubmit(BizObject bizObject, List<FundPayment_b> fundPayment_b, String billCode,String billAction) throws Exception;

    /**
     * 预算实占
     * @param bizObject
     * @param fundPayment_b
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget gcExecuteTrueAudit(BizObject bizObject, FundPayment_b fundPayment_b, String billCode, String billAction) throws Exception;

    /**
     * 预算实占
     * @param bizObject
     * @param fundCollection_b
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget fundCollectionEmployActualOccupySuccessAudit(BizObject bizObject, FundCollection_b fundCollection_b, String billCode, String billAction) throws Exception;

    /**
     * 撤回实占
     * @param bizObject
     * @param fundPayment_b
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget gcExecuteTrueUnAudit(BizObject bizObject, List<FundPayment_b> fundPayment_b, String billCode,String billAction) throws Exception;

    /**
     * 撤回实占
     * @param bizObject
     * @param fundCollection_b
     * @param billCode
     * @param billAction
     * @return
     */
    ResultBudget fundCollectionReleaseActualOccupySuccessUnAudit(BizObject bizObject, List<FundCollection_b> fundCollection_b, String billCode, String billAction) throws Exception;

    /**
     * * 联查控制明细查询
     * @param param
     * @return
     */

    String queryBudgetDetail(CtmJSONObject param);

    String budgetCheckNew(List<BizObject> bizObjects, String billnum, String submit);

    void fundPaymentExecuteAuditDeleteReleasePre(BizObject fundPayment) throws Exception;

    void fundPaymentExecuteAuditDeleteReleaseActual(FundPayment fundPayment) throws Exception;

    ResultBudget doResult(CtmJSONObject result);

    String budgetCheckBatchTransferAccount(List<BizObject> bizObjects, String billnum, String submit);
}
