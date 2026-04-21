package com.yonyoucloud.fi.cmp.constant;

/**
 * 手工日结常量类
 */
public interface ISettlementConstant {
    //最终检查结果
    String CHECKRRESULT = "checkrResult";

    //检查状态 warning pass error
    String CHECKRESULT = "checkResult";
    //检查状态 warning pass error
    String CHECKRESULT_WARNING = "warning";
    String CHECKRESULT_PASS = "pass";
    String CHECKRESULT_ERROR = "error";

    //检查项名称 单据审核检查， 单据结算检查，月末汇兑损益计算检查
    String CHECKNAME = "checkName";

    //月末汇兑损益计算检查->调整信息
    String MESSAGEADJUSTMENT = "messageAdjustment";

    //多语key
    String MESSAGELOCALE = "messageLocale";

    //检查项说明
    String MESSAGE = "message";

    //检查项状态
    String CHECKRULE = "checkRule";

    //检查明细日期
    String DATE = "date";
    //检查明细单据号
    String ORDERNO = "orderno";
    //检查明细说明
    String ERRORMESSAGE = "errorMessage";
    //检查明细单据类型
    String TYPE = "type";


}