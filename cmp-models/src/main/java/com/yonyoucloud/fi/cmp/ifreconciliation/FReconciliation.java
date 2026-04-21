/**
 * Copyright (c) 2019 ucsmy.com, All rights reserved.
 */
package com.yonyoucloud.fi.cmp.ifreconciliation;

import com.yonyoucloud.fi.cmp.cmpentity.CurrencyClassification;
import com.yonyoucloud.fi.cmp.cmpentity.DirectionJD;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description: 业财对账实体
 * @Author: Administrator
 * @Created Date: 2019年12月31日
 * @Version:
 */
@Data
public class FReconciliation {

    private String  accbody;
    private String  accbook;
    private String  checkcurrency;
    private String  checktype;
    private String  startperiod;
    private String  endperiod;
    private String  groupcode;
    private String  mattertype;
    private String  transactiontype;
    private List<Dimension>  dimlist;
    private List<BSParam>  bslist;
    private String  tally;
    private String  tempvoucher;
    private String  startdate;
    private String  enddate;
    private String  mattertypecode;
    //2022 12 06 总账对账新增需求字段
    private String  id;
    private String busi_queryschema;
    private String fixed_cond;
    private String ytenant_id;
    private String recon_dbschema;
    private String recon_table;
    private String currency;
    //贷方原币
    private BigDecimal creditoriSum;
    //借方原币
    private BigDecimal debitoriSum;
    //贷方本币
    BigDecimal creditnatSum;
    //借方本币
    BigDecimal debitnatSum;
    //对账方案id
    String reconcplan_id;
    //任务id
    String task_id;
    //对账规则ID
    String reconcrule_id;
    //会计期间id
    String period_id;
    //资金组织id
    String accentity_id;
    //账簿id
    String accbook_id;
    //期间开始日期
    String period_start_date;
    //期间结束日期
    String period_end_date;
    //对账日期类型，为settleDate时，按照登账日期返回，为billDate时，按照制单日期返回
    private String billDataType;
    //借贷方向
    private DirectionJD directionJD;
    //币种分类 (原币、本币)
    private CurrencyClassification currencyClassification;
    //是否查询余额 期初期末一起查询
    private Boolean queryBalance;
    // 期初原币金额
    private BigDecimal period_begin_ori_amount;
    // 期初本币金额
    private BigDecimal period_begin_amount;
    // 期末原币本币
    private BigDecimal period_end_ori_amount;
    // 期末本币金额
    private BigDecimal period_end_amount;
    // 维度1
    private String define1;
    private String define2;
    private String define3;
    private String define4;
    private String define5;
    private String define6;
    private String define7;
    private String define8;
    private String define9;
    private String define10;
    private String define11;
    private String define12;
    private String define13;
    private String define14;
    private String define15;
    private String define16;
    private String define17;
    private String define18;
    private String define19;
    private String define20;
    private String define21;
    private String define22;
    private String define23;
    private String define24;
    private String define25;
    private String define26;
    private String define27;
    private String define28;
    private String define29;
    private String define30;

    String subject_chart_id;

    String subject_id;

    String projectCode;
}
