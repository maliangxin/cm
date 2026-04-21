package com.yonyoucloud.fi.cmp.common;

import lombok.Data;

/**
 *
 * 本类用于常用查询rpc接口实体类
 * @author mal
 * 请使用api包下类进行替换
 * @deprecated (use com.yonyoucloud.fi.cmp.vo.common.CommonRequestDataVo instead)
 */
  @Deprecated
@Data
public class CommonQueryData {

    private String accentity;//资金组织
    private String bankaccount;//银行账号
    private String currency; //币种
    private Short dcflag;//借贷方向
    private String remark; //摘要
    private String remarkMatchType ;// 摘要匹配方式
    private String matchType ;// 匹配方式
    private String startDate;
    private String endDate;
    private String ytenantId;
    private String tenantId;
    private String id;
    private String srcbillno;//来源单据号
    private String srcitmeid;//来源单据id
    private short srcbilltype;//来源单据类型
    private String checkno;//勾兑号
    private String voucherNo;//凭证号
    private String voucherPeriod;//凭证期间
    private String operateType;//操作类型


}
