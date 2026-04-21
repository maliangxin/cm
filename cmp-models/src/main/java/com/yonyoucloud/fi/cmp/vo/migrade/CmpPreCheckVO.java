package com.yonyoucloud.fi.cmp.vo.migrade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CmpPreCheckVO implements Serializable {
    private String tenant_name; //租户名称
    private String ytenant_id; //租户id
    private String checkResult; //校验结果
    private List<CmpPreCheckDetailVO> preCheckDetailList;
}
