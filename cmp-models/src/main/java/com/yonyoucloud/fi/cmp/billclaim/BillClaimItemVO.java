package com.yonyoucloud.fi.cmp.billclaim;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @description: 认领单详情维度VO
 * @author: wanxbo@yonyou.com
 * @date: 2022/4/26 13:51
 */
@Data
public class BillClaimItemVO {
    /**
     * 认领日期
     */
    private String vouchdate;
    /**
     * 认领单编号
     */
    private String code;
    /**
     * 认领金额
     */
    private BigDecimal claimamount;
    /**
     * 认领单合计金额
     */
    private BigDecimal totalamount;
    /**
     * 认领类型
     */
    private String claimtype;
    /**
     * 认领单位
     */
    private String accentity_name;
    /**
     * 部门
     */
    private String dept_name;
    /**
     * 项目
     */
    private String project_name;
    /**
     * 认领人
     */
    private String claimstaff;
    /**
     * 认领说明
     */
    private String remark;
}
