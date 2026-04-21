package com.yonyoucloud.fi.cmp.fcdsusesetting.dto;

import lombok.Data;

/**
 * @author guoxh
 */
@Data
public class FcdsUseSettingVO {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 业务对象id
     */
    private String bizObject;
    /**
     * 业务对象编码
     */
    private String bizObjectCode;
    /**
     * 业务对象名称
     */
    private String bizObjectName;
    /**
     * 使用对象名称
     */
    private String object;
    /**
     * 交易类型id
     */
    private String transType;
    /**
     * 资金数据源id
     */
    private String cdpId;
    /**
     * 资金数据池注册id
     */
    private String cdpName;
}
