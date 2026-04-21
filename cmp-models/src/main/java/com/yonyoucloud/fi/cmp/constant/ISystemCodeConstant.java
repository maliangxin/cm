package com.yonyoucloud.fi.cmp.constant;

/**
 * 系统编码常量
 * @author maliang
 * @version V1.0
 * @date 2021/6/22 14:56
 * @Copyright yonyou
 */
public interface ISystemCodeConstant {

    /**
     * 以下用于业务单据生成凭证，在凭证预制的来源系统数据
     */
    String CMP = "cmp"; // 现金管理

    String FIER = "fier";  // 费用管理

    String FIAR ="fiar"; // 应收管理

    String FIAP = "fiap"; // 应付管理


    /**
     * 以下用于业务单元期初设置模块预制数据类型
     */
    String ORG_MODULE_CM = "cashManagement"; //现金管理

    String ORG_MODULE_GL = "generalLedger"; //总账

    String ORG_MODULE_AR = "receivables"; //应收管理

    String ORG_MODULE_AP = "pay"; //应付管理

    String ORG_MODULE_DRFT = "BSD"; //商业汇票

    String ORG_MODULE_STCT = "settlement"; //结算中心

    String ORG_MODULE_PC = "purchase"; //采购


    /**
     * 其他
     */
    String THIRD_PARTY = "THIRD_PARTY"; //第三方


    String ORG_MODULE_CSPL = "CSPL"; // 资金计划


}
