package com.yonyoucloud.fi.cmp.util;

/**
 * * 具体单据动作(仅单据明细展示用，不参与占预算逻辑)
 */
public class BillAction {
    public static final String SAVE="SAVE";//保存
    public static final String SUBMIT="SUBMIT";//提交
    public static final String CLOSE="CLOSE";//关闭
    public static final String CANCEL_SUBMIT="CANCEL_SUBMIT";//撤回
    public static final String DELETE="DELETE";//删除
    public static final String AUDIT="AUDIT";//审核
    public static final String CANCEL_AUDIT="CANCEL_AUDIT";//弃审
    public static final String APPROVE_UNSUBMIT="APPROVE_UNSUBMIT";//审批中撤回
    public static final String APPROVE_UNPASS="APPROVE_UNPASS";//审批退回
    public static final String APPROVE_EDIT="APPROVE_EDIT";//审批中修改
    public static final String APPROVE_PASS="APPROVE_PASS";//生效（审批通过）
    public static final String PAYMENT="PAYMENT";//生效（结算完成）
    public static final String CANCEL_APPROVE_BACK="CANCEL_APPROVE_BACK";//取消生效（撤回审批）
    public static final String DEFER="DEFER";//摊销
    public static final String CANCEL_DEFER="CANCEL_DEFER";//取消摊销
    public static final String CLOSE_APPLY="CLOSE_APPLY";//关闭申请
    public static final String OPEN_APPLY="OPEN_APPLY";//打开申请
    public static final String LOAN_REFER="LOAN_REFER";//借款参照
    public static final String PUBPREPAY_REFER="PUBPREPAY_REFER";//预付参照
    public static final String REFUND="REFUND";//退款
    public static final String RETURN="RETURN";//还款
    public static final String EXPENSE_VERIFY="EXPENSE_VERIFY";//报销核销
    public static final String EXPENSE_REFER="EXPENSE_REFER";//报销参照
    public static final String EXPENSE_DEFER="EXPENSE_DEFER";//报销摊销
    public static final String DEFEREXPENSE_REFER="DEFEREXPENSE_REFER";//摊销参照
    public static final String IMPORT="IMPORT";//导入
    public static final String AUDIT_REPLY="AUDIT_REPLY";//批复
    public static final String TERMINATE="TERMINATE";//终止
    public static final String CHANGE="CHANGE";//变更
    public static final String AUDIT_APPROVE="AUDIT_APPROVE";//审批通过
    public static final String AUDIT_PAYMENT="AUDIT_PAYMENT";//结算完成
    public static final String EDIT="EDIT";//编辑
    public static final String PURCHASEORDERCLOSE="PURCHASEORDERCLOSE";//发票关闭
    public static final String OPEN="OPEN";//打开
    public static final String PURCHASEORDEROPEN="PURCHASEORDEROPEN";//发票打开
    public static final String QUERY="QUERY";//查询
    public static final String CANCEL_SETTLE="CANCEL_SETTLE";//取消结算
    public static final String STOP_SETTLE="STOP_SETTLE";//结算止付
    public static final String SETTLE_SUCCESS="SETTLE_SUCCESS";//结算成功
}
