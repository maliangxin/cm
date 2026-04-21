package com.yonyoucloud.fi.cmp.vo.migrade;

import lombok.Data;

import java.io.Serializable;

@Data
public class CmpPreCheckDetailVO implements Serializable {
    private String tenant_name; //租户名称
    private String ytenant_id; //租户id
    private String serviceName; //服务节点
    private String checkType; //校验类型
    private String billid; //单据主键id
    private String billCode; //单据编号
    private String checkResult; //校验结果
    private String repairPlan; //建议修复数据方案

    //平台工具返回的相关字段(预检回调)
    private String billType;//单据类型
    private String billDate;//单据日期
    private String accEntityName;//资金组织名称
    private String srcAppName;//来源应用名称
    private String srcBillNo;//来源单据号
    private String auditStatus;//审核状态名称
    private String accountId;//账簿 id
    private String accountName;//账簿名称


}
