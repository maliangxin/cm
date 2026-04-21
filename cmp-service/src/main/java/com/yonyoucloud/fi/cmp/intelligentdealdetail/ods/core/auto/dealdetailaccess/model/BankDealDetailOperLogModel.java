package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model;

import com.yonyou.iuap.yms.annotation.*;
import com.yonyou.iuap.yms.param.BaseEntity;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.sqls.ODSSql;
import lombok.Data;

import java.util.Date;

/**
 * @Author guoyangy
 * @Date 2024/6/22 9:55
 * @Description todo
 * @Version 1.0
 */
@Data
@YMSEntity
@YMSTable(tableName = ODSSql.cmp_bankdealdetail_operlog)
public class BankDealDetailOperLogModel extends BaseEntity {
    @YMSId
    @YMSColumn(name = "id")
    @YMSGeneratedValue(domain = "ctm-cmp")
    private String id;
    @YMSColumn(name = "opertype")
    private String operType; //1:用户点击页面 2:调度任务触发
    @YMSColumn(name = "tenant_id")
    private long tenantId;
    @YMSColumn(name = "ytenant_id")
    private String yTenantId;
    @YMSColumn(name = "traceid")
    private String traceId;
    @YMSColumn(name = "pubts", defaultValue = DefaultValueType.CURRENT_TIMESTAMP)
    private Date pubts;
    @YMSColumn(name = "usedtime")
    private String usedTime;
    @YMSColumn(name = "trancode")
    private String tranCode;
    @YMSColumn(name = "request_seq_no")
    private String requestSeqNo;
    @YMSColumn(name = "beg_date")
    private String begDate;
    @YMSColumn(name = "end_date")
    private String endDate;
    @YMSColumn(name = "acct_no")
    private String acctNo;
    @YMSColumn(name = "acct_name")
    private String acctName;
    @YMSColumn(name = "curr_code")
    private String currCode;
    @YMSColumn(name = "tran_status")
    private String tranStatus;
    @YMSColumn(name = "beg_num")
    private Integer begNum;
    @YMSColumn(name = "requestbegintime")
    private Date requestbegintime;//请求开始时间
    @YMSColumn(name = "requestendtime")
    private Date requestEndtime;//请求结束时间
    @YMSColumn(name = "response_code")
    private Integer code;
    @YMSColumn(name = "message")
    private String message;
    @YMSColumn(name = "resp_service_code")
    private String respServiceCode;
    @YMSColumn(name = "resp_service_desc")
    private String respServiceDesc;
    @YMSColumn(name = "resp_service_seq_no")
    private String respServiceSeqno;
    @YMSColumn(name = "resp_service_status")
    private String respServiceStatus;
    @YMSColumn(name = "tot_num")
    private Integer totNum;  //总数量
    @YMSColumn(name = "back_num")
    private Integer backNum;  //返回条数
    @YMSColumn(name = "actual_num")
    private Integer actualNum;
    @YMSColumn(name = "succ_num")
    private Integer succNum;//去重复落入ods数据
    @YMSColumn(name = "fail_num")
    private Integer failNum;//落入异常库数量
    @YMSColumn(name = "create_time")
    private Date create_time;//创建时间,
    @YMSColumn(name = "create_date")
    private Date create_date;//创建日期,
    @YMSColumn(name = "modify_time")
    private Date modify_time;//修改时间,
    @YMSColumn(name = "modify_date")
    private Date modify_date;//修改日期,
    @YMSColumn(name = "creator")
    private String creator;//创建人名称,
    @YMSColumn(name = "modifier")
    private String modifier;//修改人名称,
    @YMSColumn(name = "creatorId")
    private long creatorId;//创建人
    @YMSColumn(name = "modifierId")
    private long modifierId;//修改人
}
