package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.impl;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.param.ResultSetProcessor;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.ODSBasicInfoObject;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BankDealDetailClearProcessImpl extends ODSBasicInfoObject {
    public Object clearBankDealDetail(String pubtsTime) throws Exception {
        //查询所有的业务单据
        if(StringUtils.isEmpty(pubtsTime)){
            CtmJSONArray result = new CtmJSONArray();
            CtmJSONObject object = new CtmJSONObject();
            object.put("result",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400734", "没有时间戳不执行！") /* "没有时间戳不执行！" */);
            result.add(object);
            return result;
        }
        LocalDate date = LocalDate.parse(pubtsTime, DateTimeFormatter.ISO_LOCAL_DATE);

        // 转换为 2024-09-19 00:00:00
        LocalDateTime startOfDay = date.atStartOfDay();
        String startOfDayString = startOfDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // 转换为 2024-09-19 23:59:59
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        String endOfDayString = endOfDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String endOfDayStringODS = LocalDate.now().atTime(23, 59, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //组装sql
        String UPDATE_BANK_BYPROCESSSTATUSANDTRACEID="select  id  from cmp_BankReconciliation where ytenant_id=? and dataOrigin=?  and tran_time between  ? and ?";
        SQLParameter sqlParameter = new SQLParameter();
        sqlParameter.addParamWithType(InvocationInfoProxy.getTenantid(), Types.VARCHAR);
        sqlParameter.addParamWithType(1, Types.VARCHAR);
        sqlParameter.addParamWithType(startOfDayString, Types.VARCHAR);
        sqlParameter.addParamWithType(endOfDayString, Types.VARCHAR);
        List<BankReconciliation> list = ymsJdbcApi.queryForList(UPDATE_BANK_BYPROCESSSTATUSANDTRACEID, sqlParameter, new ResultSetProcessor() {
            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<BankReconciliation> list = new ArrayList<>();
                while (rs.next()) {
                    Long id = rs.getLong(1);
                    BankReconciliation bankReconciliation = new BankReconciliation();
                    bankReconciliation.setId(id);
                    list.add(bankReconciliation);
                }
                return list;
            }
            @Override
            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                return null;
            }
        });
        String paramsQuery = "";
        if (!CollectionUtils.isEmpty(list) && list.size()>1){
            for (int i=0;i<list.size()-1;i++) {
                paramsQuery+="'"+list.get(i).getId()+"',";
            }
        }
        if (!CollectionUtils.isEmpty(list)){
            paramsQuery+="'"+list.get(list.size()-1).getId()+"'";
        }

        String DELETE_BANK_DELETE_A="delete from cmp_BankReconciliation where id in (" +
                paramsQuery+")";
        List<BankReconciliationbusrelation_b> list_b =null;
        if (StringUtils.isNotEmpty(paramsQuery)){
            String UPDATE_BANK_DELETE_A="select  id  from cmp_bankreconciliation_bus_relation_b where bankreconciliation in (" +
                    paramsQuery+")";
            list_b = ymsJdbcApi.queryForList(UPDATE_BANK_DELETE_A, new ResultSetProcessor() {
                @Override
                public Object handleResultSet(ResultSet rs) throws SQLException {
                    List<BankReconciliationbusrelation_b> list = new ArrayList<>();
                    while (rs.next()) {
                        Long id = rs.getLong(1);
                        BankReconciliationbusrelation_b bankReconciliation_b = new BankReconciliationbusrelation_b();
                        bankReconciliation_b.setId(id);
                        list.add(bankReconciliation_b);
                    }
                    return list;
                }
                @Override
                public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                    return null;
                }
            });
        }
        String UPDATE_BANK_DELETE_DEATAIL="select  id  from cmp_bankdealdetail where ytenant_id=? and trantime between ? and ?";
        SQLParameter sqlParameter_detail = new SQLParameter();
        sqlParameter_detail.addParamWithType(InvocationInfoProxy.getTenantid(), Types.VARCHAR);
        sqlParameter_detail.addParamWithType(startOfDayString,Types.VARCHAR);
        sqlParameter_detail.addParamWithType(endOfDayString,Types.VARCHAR);
        List<BankDealDetail> list_detail = ymsJdbcApi.queryForList(UPDATE_BANK_DELETE_DEATAIL,sqlParameter_detail, new ResultSetProcessor() {
            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<BankDealDetail> list = new ArrayList<>();
                while (rs.next()) {
                    Long id = rs.getLong(1);
                    BankDealDetail bankReconciliation_b = new BankDealDetail();
                    bankReconciliation_b.setId(id);
                    list.add(bankReconciliation_b);
                }
                return list;
            }
            @Override
            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                return null;
            }
        });


        String UPDATE_ODS_BYPROCESSSTATUSANDTRACEID="select id from cmp_bankdealdetail_ods where ytenant_id=? and tran_date = ?";
        SQLParameter sqlParameter_ods = new SQLParameter();
        sqlParameter_ods.addParamWithType(InvocationInfoProxy.getTenantid(), Types.VARCHAR);
        sqlParameter_ods.addParamWithType(pubtsTime.replace("-",""),Types.VARCHAR);
        List<BankDealDetailODSModel> list_ods = ymsJdbcApi.queryForList(UPDATE_ODS_BYPROCESSSTATUSANDTRACEID, sqlParameter_ods, new ResultSetProcessor() {
            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<BankDealDetailODSModel> list = new ArrayList<>();
                while (rs.next()) {
                    String id = rs.getString(1);
                    BankDealDetailODSModel bankDealDetailODSModel = new BankDealDetailODSModel();
                    bankDealDetailODSModel.setId(id);
                    list.add(bankDealDetailODSModel);
                }
                return list;
            }
            @Override
            public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                return null;
            }
        });
        if (!CollectionUtils.isEmpty(list_b)){
            MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME,list_b);
        }
        if (!CollectionUtils.isEmpty(list)){
            MetaDaoHelper.delete(BankReconciliation.ENTITY_NAME,list);
        }
        if (!CollectionUtils.isEmpty(list_detail)){
            MetaDaoHelper.delete(BankDealDetail.ENTITY_NAME,list_detail);
        }
        if (!CollectionUtils.isEmpty(list_ods)){
            ymsJdbcApi.remove(list_ods);
        }
        CtmJSONArray result = new CtmJSONArray();
        CtmJSONObject object = new CtmJSONObject();
        object.put("result",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400735", "删除成功！") /* "删除成功！" */);
        result.add(object);
        return result;
    }
}
