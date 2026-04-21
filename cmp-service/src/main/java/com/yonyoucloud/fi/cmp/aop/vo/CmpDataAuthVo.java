package com.yonyoucloud.fi.cmp.aop.vo;

import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * @description:账户管理-数据权限-vo
 * @author: zhanghjr
 * @time: 2024/3/21
 */
@Data
@Builder
public class CmpDataAuthVo {
    /**
     * 业务对象编码
     */
    private String bizObjCode;
    /**
     * 目标实体的enrityuri
     */
    private String fullName;
    /**
     * 服务编码
     */
    private String serviceCode;
    /**
     * 业务单据实体id集合
     */
    private Set<Object> ids;
    /**
     * 场景编码
     */
    private String sceneCode;

    public CmpDataAuthVo() {

    }

    public CmpDataAuthVo(String bizObjCode, String fullName, String serviceCode, Set<Object> ids, String sceneCode) {
        this.bizObjCode = bizObjCode;
        this.fullName = fullName;
        this.serviceCode = serviceCode;
        this.ids = ids;
        this.sceneCode = sceneCode;
    }

    public static CmpDataAuthVo find(String fullName, String sceneCode) {
        if (fullName.equals(BillClaim.ENTITY_NAME)) {
            CmpDataAuthVo vo = new CmpDataAuthVo("ctm-cmp.cmp_billclaimcard", fullName, "ficmp0034", null, sceneCode);
            return vo;
        }

        return null;
    }
}
