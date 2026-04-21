package com.yonyoucloud.fi.cmp.bankreceipt.dto;

import lombok.Data;


/**
 * @Description
 * @Author hanll
 * @Date 2024/6/11-15:06
 */
@Data
public class TenantDTO  {

    /**
     * 租户Id
     */
    private Long tenantId;
    /**
     * 友互通租户Id
     */
    private String ytenantId;

    public TenantDTO(String ytenantId) {
        this.ytenantId = ytenantId;
    }
}
