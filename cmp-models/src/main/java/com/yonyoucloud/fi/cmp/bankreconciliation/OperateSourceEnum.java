package com.yonyoucloud.fi.cmp.bankreconciliation;

/**
 * 流水处理修改的来源
 *
 * @author ml
 */
public enum OperateSourceEnum {


    MANUAL_REFUND("手工退票", "UID:P_CM-UI_198CBF7A04F80003"),//@notranslate
    CANCEL_REFUND("取消退票", ""),//@notranslate

    PUBLISH("发布", ""),//@notranslate
    CANCEL_PUBLISH("取消发布", ""),//@notranslate
    NO_PROCESS("无需处理", ""),//@notranslate
    CANCEL_NO_PROCESS("取消无需处理", ""),//@notranslate

    DEFAULT_UPDATE("默认修改", ""),//@notranslate
    API_UPDATE("Api更新", ""),//@notranslate

    EXCLUSION("账户收支明细剔除", ""),//@notranslate


    REWRITE_UPDATE("关联关系新增回写", ""),//@notranslate
    REWRITE_UPDATE_ROLLBACK("关联关系新增回写回滚", ""),//@notranslate
    REWRITE_DELETE("关联关系删除回写", ""),//@notranslate
    REWRITE_DELETE_ROLLBACK("关联关系删除回写回滚", ""),//@notranslate

    CLEAR_REFUND("运营工具-清除银行流水退票状态", "");//@notranslate

    OperateSourceEnum(String name, String nameResid) {
        this.name = name;
        this.nameResid = nameResid;
    }

    private String name;
    private String nameResid;

    public String getName() {
        return name;
    }

    public String getNameResid() {
        return nameResid;
    }

}


