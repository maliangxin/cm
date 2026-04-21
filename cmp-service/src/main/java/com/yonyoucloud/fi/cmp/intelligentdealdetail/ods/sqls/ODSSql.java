package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.sqls;

import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;

/**
 * @Author guoyangy
 * @Date 2024/6/27 21:06
 * @Description 智能流水有关sql语句汇总
 * @Version 1.0
 */
public class ODSSql {

    public static final String cmp_bankdealdetail_operlog="cmp_bankdealdetail_operlog";
    public static final String CMP_BANKDEALDETAIL_ODS="cmp_bankdealdetail_ods";
    public static final String CMP_BANKDEALDETAIL_ODS_FAIL="cmp_bankdealdetail_ods_fail";
    public static final String CMP_BANKDEALDETAIL_PROCESSING="cmp_bankdealdetail_processing";

    public static final String ODS_ALL_FIELDS="id,mainid,pubts,traceid,create_time,tenant_id,ytenant_id,acct_no,acct_name,resp_service_seq_no,request_seq_no,processstatus,retrycount,tran_date,tran_time,dc_flag,bank_seq_no,to_acct_no,to_acct_name,to_acct_bank,to_acct_bank_name,curr_code,cash_flag,acct_bal,tran_amt,oper,value_date,use_name,remark,bank_reconciliation_code,unique_no,detail_check_id,bank_check_code,rate,fee_amt,fee_amt_cur,remark01,pay_use_desc,corr_fee_amt,corr_fee_amt_cur,sub_name,sub_code,proj_name,budget_source,voucher_type,voucher_no,mdcard_no,mdcard_name,payment_manage_type,eco_class,budget_relevance_no,add_amt,balance,is_refund,refund_original_transaction,bankaccount,currency,orgid,contentsignature,accesschannel ";

    public static final String BANKDEALDETAIL_CONDITION="(processstatus in ("+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus()+","+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus()+","+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_ERROR.getStatus()+","+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_RECEIVEPAY_SUCC.getStatus()+","+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_GENERATE_SUCC.getStatus()+","+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus()+","+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_FAIL.getStatus()+","+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_FAIL.getStatus()+","+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_FAIL.getStatus()+","+
            DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_FAIL.getStatus()+"))" +
            " or (processstatus="+DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_START.getStatus()+"  and create_time between ? and ? ) " +
            " or (processstatus="+DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_RELATED_START.getStatus()+"  and create_time between ? and ? ) "+
            " or (processstatus="+DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_CREDENTIALT_START.getStatus()+"  and create_time between ? and ? ) "+
            " or (processstatus="+DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_GENERATEBILL_START.getStatus()+"  and create_time between ? and ? ) "+
            " or (processstatus="+DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_PUBLISH_START.getStatus()+"  and create_time between ? and ? ) ";

    //36.修改关联明细表确认状态
    public static final String UPDATE_BANKRECONCILIATION_BUS_RELATIONSTATUS = "UPDATE cmp_bankreconciliation_bus_relation_b SET relationstatus=? ,relationtype=? where id=? and ytenant_id=?";

    //35.查询关联明细
    public static final String QUERY_BANKRECONCILIATION_BUS_RELATION_B_BYBILLIDANDBANKRECONCILITIONID = "select id from cmp_bankreconciliation_bus_relation_b where ytenant_id=? and billid=? and bankreconciliation=?";

    //34.查询流水处理规则
    public static final String QUERY_FLOWHANDLESETTING = "select is_arti_confirm,is_random_auto_confirm from cmp_flow_handle_setting  where ytenant_id=? and flow_type=? and enable=? and object=? and association_mode=? ";


    //33.查询关联明细
    public static final String QUERY_BANKRECONCILIATION_BUS_RELATION_B = "select id,billid from cmp_bankreconciliation_bus_relation_b ";

    //32.分页查询导入流水
    public static final String queryOdsConsumerSqlByPage = "select "+ ICmpConstant.SELECT_TOTAL_PARAM +" from cmp_bankdealdetail_ods where traceid=? and request_seq_no=? and processstatus=? and  mod(CAST(id AS SIGNED),?)=?  and ytenant_id=? order by pubts desc limit "+ DealDetailEnumConst.PAGESIZE;

    //31.ods导入流水数量
    public static final String queryOdsConsumerSqlToalcount = "select count(*) as "+BankReconciliationSql.TOTALCOUNT+" from cmp_bankdealdetail_ods where traceid=? and request_seq_no=? and processstatus=? and  mod(CAST(id AS SIGNED),?)=? and ytenant_id=?";

    public static final String UPDATE_ODS_BYPROCESSSTATUSANDTRACEID="update cmp_bankdealdetail_ods set processstatus=?, traceid=?,request_seq_no=? ";
    //30.
    public static final String QUERY_CONTENTSIGN_FROM_ODS = "select contentsignature,id,mainid from cmp_bankdealdetail_ods ";
    //29.查询流水表需要补偿的租户
    public static final String QUERY_NEED_TENANT="SELECT DISTINCT ytenant_id FROM cmp_bankreconciliation where "+ BANKDEALDETAIL_CONDITION;
    public static final String QUERY_TOTALCOUNT="SELECT count(*) as totalCount FROM cmp_bankreconciliation where "+ BANKDEALDETAIL_CONDITION +" and ytenant_id=?";


    public static final String QUERY_NEED_ODS_TENANT="SELECT DISTINCT ytenant_id FROM cmp_bankdealdetail_ods where (processstatus in ("+
            DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NO.getProcessstatus()+","+
            DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_REPEAT_ERROR.getProcessstatus()+","+
            DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_REPEAT_FINISH_WAIT_MATCH.getProcessstatus()+")) or (processstatus="+DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_START.getProcessstatus()+" and create_time between ? and ? ) ";
    public static final String QUERY_NEED_ODS_BYREQUESTSEQNO="SELECT distinct traceid,request_seq_no,ytenant_id,accesschannel FROM cmp_bankdealdetail_ods where (processstatus in ("+
            DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NO.getProcessstatus()+","+
            DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_REPEAT_ERROR.getProcessstatus()+","+
            DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_REPEAT_FINISH_WAIT_MATCH.getProcessstatus()+") or (processstatus="+DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_START.getProcessstatus()+" and create_time between ? and ? )) and ytenant_id=? ";




    //28.批量修改流水表的processstatus状态
    public static final String UPDATE_DEALDETAIL_PROCESSSTATUS_BY_IDS_WITHOUT_PUBTS = "UPDATE cmp_bankreconciliation SET processstatus=? where id=? and ytenant_id=?  AND processstatus!="+DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus();

    //28.批量修改流水表的processstatus状态
    public static final String UPDATE_DEALDETAIL_PROCESSSTATUS_BY_IDS = "UPDATE cmp_bankreconciliation SET processstatus=?  where id=? and ytenant_id=?";
    //27.批量修改ods表消费中的processstatus状态
    public static final String UPDATE_ODS_PROCESSSTATUS_BY_IDS = "UPDATE cmp_bankdealdetail_ods SET processstatus=? ";

    //26.流水补偿-针对流水辨识成功的，需要取流水过程表这条流水最新执行记录确定流程处理是哪个
    public static final String QUERY_DEALDETAIL_PROCESSING_BY_MAINID="select processing.id,processing.mainid,processing.osdid,processing.pubts,processing.exerules,processing.fullrules,processing.traceid,processing.request_seq_no from cmp_bankdealdetail_processing processing\n" +
            "    inner join\n" +
            "            (select mainid,max(id)as mid from cmp_bankdealdetail_processing group by mainid) tmp\n" +
            "    on processing.id = tmp.mid and processing.mainid = tmp.mainid";
   //18.流水表拉取processstatus=25的流水
    //24.ods未消费流水补偿
    public static final String ODS_COMPENSATE_COSSUMER_SQL = "select distinct request_seq_no,traceid,ytenant_id from cmp_bankdealdetail_ods where processstatus="+DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NO.getProcessstatus()+" and create_time between ? and ? ";
    //22.流水表拉取processstatus=2的流水
    public static final String  QUERY_DEALDETAIL_BY_PROCESSSTATUS= "SELECT id,create_time,processstatus FROM cmp_bankreconciliation WHERE processstatus="+DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_START.getStatus()+" and pubts BETWEEN ? AND ?";
   //18.流水表拉取processstatus=25的流水
    public static final String  QUERY_DEALDETAIL_BY_FINISH_PROCESSSTATUS= "SELECT id,create_time,processstatus,bank_seq_no FROM cmp_bankreconciliation ";

   //5.更新流水状态processstauts
   //5.更新流水状态processstauts
    public static final String UPDATE_PROCESS_STATUS_BANK_RECONCILIATION_BY_ID ="update cmp_bankreconciliation set processstatus=? where id=?";
    //4.根据ods主键id更新ods的状态字段、mainid
    public static final String UPDATE_PROCESS_STATUS_ODS_BY_ID ="update cmp_bankdealdetail_ods set processstatus=? where id=?";
    //3.查询规则大类
    public static final String QUERY_RULETYPE="SELECT id, code,excuteorder,identifytype from cmp_bankreconciliation_identify_type where ytenant_id=? and enablestatus=1";
    //2.根据ods主键id更新ods的状态字段、mainid
    public static final String UPDATE_PROCESS_STATUS_AND_MAIN_ID_ODS_BY_ID ="update cmp_bankdealdetail_ods set processstatus=?,mainid=? where id=?";
    //1.根据traceid+requestSeqNo+odsstatus查询ods
    public static final String QUERY_ODS_BY_TRACE_ID_AND_REQUEST_SEQ_NO_AND_ODSSTATUS = "select "+ODS_ALL_FIELDS+" from cmp_bankdealdetail_ods where traceid=? and request_seq_no=? and processstatus=?";
    //1.根据traceid+requestSeqNo查询ods
    public static final String QUERY_ODS_BY_TRACE_ID_AND_REQUEST_SEQ_NO = "select "+ODS_ALL_FIELDS+" from cmp_bankdealdetail_ods where traceid=? and request_seq_no=?";

}