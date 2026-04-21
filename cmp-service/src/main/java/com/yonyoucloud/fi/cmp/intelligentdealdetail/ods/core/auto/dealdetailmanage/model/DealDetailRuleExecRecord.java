package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model;

import com.yonyou.iuap.yms.annotation.*;
import com.yonyou.iuap.yms.param.BaseEntity;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.sqls.ODSSql;
import lombok.Data;

import java.util.Date;
/**
 * @Author guoyangy
 * @Date 2024/7/2 13:48
 * @Description todo
 * @Version 1.0
 */
@Data
@YMSEntity
@YMSTable(tableName = ODSSql.CMP_BANKDEALDETAIL_PROCESSING)
public class DealDetailRuleExecRecord   extends BaseEntity{
    @YMSId
    @YMSColumn(name = "id")
    @YMSGeneratedValue(domain = "ctm-cmp")
    private String id;
    @YMSColumn(name = "mainid")
    private Long mainid;
    @YMSColumn(name = "osdid")
    private String osdid;
    @YMSColumn(name = "pubts", defaultValue = DefaultValueType.CURRENT_TIMESTAMP)
    private Date pubts;
    @YMSColumn(name = "create_time")
    private Date create_time;// 创建时间,
    @YMSColumn(name = "tenant_id")
    private Long tenant_id;// 租户,
    @YMSColumn(name = "ytenant_id")
    private String ytenant_id;// 租户id,
    @YMSColumn(name = "exerules")
    private String exerules;// 流水已执行规则记录Json格式,
    @YMSColumn(name = "fullrules")
    private String fullrules;//全量规则
    @YMSColumn(name = "exeversion")
    private int exeversion;
    @YMSColumn(name = "failrule")
    private String failrule;//全量规则
    @YMSColumn(name = "failreason")
    private String failreason;//全量规则
    @YMSColumn(name = "traceid")
    private String traceid;
    @YMSColumn(name = "request_seq_no")
    private String requestseqno;
    @YMSColumn(name = "exceptionmsg")
    private String exceptionmsg;
}