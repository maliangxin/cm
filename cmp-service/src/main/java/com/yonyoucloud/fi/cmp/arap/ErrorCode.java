package com.yonyoucloud.fi.cmp.arap;

public enum ErrorCode {
    PARAM_CHECK_FALSE("20220000", "参数校验问题", "UID:P_ARAP-FE_180721A604C8007E"),
    BUSINESS_ERROR("20220001", "业务校验异常", "UID:P_ARAP-FE_180721A604C8007F"),
    UNCONTROLLABLE_DEL_ERROR("20220002", "反向不可控异常", "UID:P_ARAP-FE_180721A604C80080"),
    DEAL_SUCCESS("20220003", "单据处理成功请勿重复操作!", "UID:P_ARAP-FE_180721A604C80081"),
    PURCHASE_SETTLE_DETAILS_NULL("20220004", "采购结算单表体为空!", "UID:P_ARAP-FE_180721A604C80082"),
    PURINRECORD_REPEATE_REQUEST("20220005", "采购入库已开始业务处理，无法重复保存!", "UID:P_ARAP-FE_180721A604C80083"),
    TRANSFER_SETTLE_ORDER_CHECK("20220006", "内部交易非暂估不立应收应付!", "UID:P_ARAP-FE_180721A604C80084"),
    INVALID_ORDER_CHECK("20220007", "单据作废删除操作!", "UID:P_ARAP-FE_180721A604C80085"),
    TRANSLATE_ORDER_CHECK("20220008", "立账方式为空或开票确认应收应付，当前单据不立!", "UID:P_ARAP-FE_180721A604C80086"),
    PATCH_BALANCE_CHECK("20220009", "补差后金额为0，当前单据不立!", "UID:P_ARAP-FE_180721A604C80087"),
    INVOICE_CHILD_CHECK("20220010", "发票标题行为空流程校验结束!", "UID:P_ARAP-FE_180721A604C80088"),
    ORDER_INVOICE_CHECK("20220011", "订单直接下推或自制发票流程校验结束!", "UID:P_ARAP-FE_180721A604C80089"),
    NOT_SALE_OUT_INVOICE_CHECK("20220012", "销售发票不是销售出库下推校验流程结束!", "UID:P_ARAP-FE_180721A604C8008A"),
    MANUAL_VERIFY_CHECK("20220013", "已经手工核销，不能进行操作", "UID:P_ARAP-FE_180721A604C8008B"),
    TRANSFER_DEBT_COUNT_CHECK("20220014", "当前单据已经做过债务转移，不能进行操作", "UID:P_ARAP-FE_180721A604C8008C"),
    ORDER_LOSS_EARN_CHECK("20220015", "当前单据已进行汇兑损益，不能进行操作", "UID:P_ARAP-FE_180721A604C8008D"),
    PAY_BILL_CHECK("20220016", "当前单据已生成付款单不能进行操作", "UID:P_ARAP-FE_180721A604C8008E"),
    PAY_BILL_APPLY_CHECK("20220017", "当前单据已生成付款申请单不能进行操作", "UID:P_ARAP-FE_180721A604C8008F"),
    RECEIVE_BILL_CHECK("20220018", "当前单据已生成收款单不能进行操作", "UID:P_ARAP-FE_180721A604C80090"),
    SETTLE_ACCOUNTS_CHECK("20220019", "收付已经结账，不能进行操作", "UID:P_ARAP-FE_180721A604C80091"),
    CONSUMEORDER_REPEATE_REQUEST("20220020", "消耗汇总单已开始业务处理，无法重复保存!", "UID:P_ARAP-FE_180721A604C80092"),
    REQUEST_PARAM_CHECK_ERROR("20220021", "单据传收付立账失败：上游参数存在格式错误或者字段缺失!", "UID:P_ARAP-FE_180721A604C80093"),
    VOUCH_DATE_ENABLE_CHECK_ERROR("20220022", "单据日期或立账日期不能早于模块启用日期！", "UID:P_ARAP-FE_180721A604C80094"),
    DUPLICATE_SUBMIT("10210000", "单据重复提交", "UID:P_ARAP-FE_180721A604C80095"),
    UNCONTROLLABLE_ERROR("10210001", "不可控异常", "UID:P_ARAP-FE_180721A604C80096"),
    PURCHASE_INVOICE_ERROR("10210002", "发票未查询到上游关联单据", "UID:P_ARAP-FE_180721A604C80097"),
    PURCHASE_INVOICE_CHECK_ERROR("10210003", "发票表体行upsrcbillid为空", "UID:P_ARAP-FE_180721A604C80098"),
    PURCHASE_SETTLE_QUERY_ERROR("10210004", "采购结算单关联单据Event_Bill中未查询到： ", "UID:P_ARAP-FE_180721A604C80099"),
    PURCHASE_SETTLE_CHECK_ERROR("10210005", "采购结算单关联采购入库和采购发票未全部立应付!", "UID:P_ARAP-FE_180721A604C8009A"),
    VOUCHER_ERROR("10210006", "生成凭证失败", "UID:P_ARAP-FE_180721A604C8009B"),
    VOUCHER_DEL_ERROR("10210007", "删除凭证异常(请检查是否已生成审核记账凭证)!", "UID:P_ARAP-FE_180721A604C8009C"),
    VOUCHER_DEL_BATCH_ERROR("10210008", "删除凭证异常(调用删除凭证接口异常)!", "UID:P_ARAP-FE_180721A604C8009D"),
    EVENT_DB_ERROR("10210009", "事项库单据转换异常!", "UID:P_ARAP-FE_180721A604C8009E"),
    DEFAULT_CODE("0", "采购结算单关联采购入库和采购发票未全部立应付!", "UID:P_ARAP-FE_180721A604C8009A");

    private String code;
    private String message;
    private String messageCode;

    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public String getMessageCode() {
        return this.messageCode;
    }

    private ErrorCode(final String code, final String message, final String messageCode) {
        this.code = code;
        this.message = message;
        this.messageCode = messageCode;
    }
}