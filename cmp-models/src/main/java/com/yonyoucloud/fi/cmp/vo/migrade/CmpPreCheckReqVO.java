package com.yonyoucloud.fi.cmp.vo.migrade;

import lombok.Data;

import java.util.Map;

@Data
public class CmpPreCheckReqVO {
    private Map<String, String> tenantMap; //租户id
    private String fullName; //服务节点
    private String checkType; //校验类型
}
