package com.yonyoucloud.fi.cmp.flowhandlesetting.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author guoxh
 */
@Data
public class FlowHandleSettingSub implements Serializable {
    /**
     * 条件
     */
    private String ruleEngineConfig;
    /**
     * 可关联单据
     */
    private String assoBill;
    /**
     * 关联凭据类型
     */
    private String credentialType;
    /**
     * 是否业务凭据匹配关联即完结
     */
    private Integer isFinishOver;
    /**
     * 业务凭据匹配关联后流程
     */
    private Integer finishAfterFlow;
    /**
     * 收付单据辨识生单类型
     */
    private String createBill;
    /**
     * 发布对象类型
     */
    private String publishObjectType;
}
