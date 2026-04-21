package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception;
/**
 * @Author guoyangy
 * @Date 2024/7/12 14:12
 * @Description 异常码
 * @Version 1.0
 */
public enum BankDealDetailExceptionCodeEnum{
    /**
     * 流水接入
     * */
    BANKDEALDETAIL_ACCESS_PARSE_ERROR(1,"流水解析失败"),
    BANKDEALDETAIL_ACCESS_ODSCHECKREPEAT_ERROR(2,"ODS去重业务异常"),
    BANKDEALDETAIL_ACCESS_ODSSAVE_ERROR(3,"流水入库异常"),
    BANKDEALDETAIL_ACCESS_NOTIFY_ERROR(4,"通知消费者消费流水异常"),
    BANKDEALDETAIL_ACCESS_SIGNATURE_ERROR(5,"流水生成签名异常"),

    BANKDEALDETAIL_ACCESS_NOTICE_PARSE_ERROR(6,"流水解析失败,数量异常"),
    /**
     * 流水消费
     * */
    BANKDEALDETAIL_CONSUMER_READCACHE_ERROR(51,"消费端读缓存异常"),
    BANKDEALDETAIL_CONSUMER_UPDATE_ERROR(52,"消费端更新流水状态异常"),
    BANKDEALDETAIL_CONSUMER_REPEAT_ERROR(53,"消费端流水业务去重异常"),
    BANKDEALDETAIL_CONSUMER_WRAPPER_ERROR(54,"封装流水包装类异常"),
    BANKDEALDETAIL_CHECK_BANKRECONCILIATIONID_ERROR(55,"校验流水id为空"),
    BANKDEALDETAIL_MATCH_ERROR(56,"调辨识匹配规则异常"),
    /**
     * 流水管理
     * */
    BANKRECONCILIATION_SAVE_ERROR(101,"流水新增异常"),
    BANKRECONCILIATION_UPDATE_ERROR(102,"流水更新异常"),
    BANKRECONCILIATION_ODS_UPDATE_ERROR(103,"ODS更新异常"),
    BANKRECONCILIATION_PROCESSING_UPDATE_ERROR(104,"过程表更新异常"),
    BANKRECONCILIATION_PROCESSING_ADD_ERROR(105,"过程表新增异常"),
    BANKRECONCILIATION_TRANSACTION_SUBMIT_ERROR(106,"流水事务提交异常"),
    BANKRECONCILIATION_CALLBACK_ERROR(107,"流水管理器执行业务回调异常"),
    BANKRECONCILIATION_BUSINESSRETURN_COUNT_ERROR(108,"业务规则返回流水数量与传入流水数量不一致"),

    /**
     * 辨识匹配规则
     * */
    BANKRECONCILIATION_CALL_RULE_ERROR(201,"调用规则异常"),


    /**
     * 规则大类具体业务
     * */
    UPDATE_BANKRECONCILIATIONRELATIONBUS_ERROR(300,"修改关联明细状态异常"),
    ;
    private int errCode;
    private String msg;
    BankDealDetailExceptionCodeEnum(int errCode,String msg){
        this.errCode=errCode;
        this.msg=msg;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}