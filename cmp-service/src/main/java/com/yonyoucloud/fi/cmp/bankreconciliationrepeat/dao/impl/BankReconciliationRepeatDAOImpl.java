package com.yonyoucloud.fi.cmp.bankreconciliationrepeat.dao.impl;

import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.param.ResultSetProcessor;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyou.iuap.yms.processor.IntegerProcessor;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliationrepeat.dao.BankReconciliationRepeatDAO;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class BankReconciliationRepeatDAOImpl implements BankReconciliationRepeatDAO {
    @Autowired
    @Qualifier("busiBaseDAO")
    private IYmsJdbcApi ymsJdbcApi;

    @Override
    public Integer selectBankReconciliationRepeatCount(String ytenantId) {
        SQLParameter parameter = new SQLParameter(true);
        String sql = "select count(1) from cmp_bankreconciliation where isrepeat is null and ytenant_id = ?";
        parameter.addParam(ytenantId);
        IntegerProcessor processor = new IntegerProcessor();
        return (Integer) ymsJdbcApi.queryForObject(sql,parameter,processor);
    }

    @Override
    public List<BankReconciliation> selectBankReconciliationRepeatData(String ytenantId) {
        SQLParameter parameter = new SQLParameter(true);
        String sql = "select id from cmp_bankreconciliation where isrepeat is null and ytenant_id = ? limit 1000000";
        parameter.addParam(ytenantId);
        List<BankReconciliation> list = ymsJdbcApi.queryForList(sql,parameter,new ResultSetProcessor(){

            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<BankReconciliation> list = new ArrayList<>();
                while(rs.next()){
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
        return list;
    }

    @Override
    public void updateBankReconciliationRepeat(List<BankReconciliation> bankReconciliations, String ytenantId) {
        SQLParameter parameter = new SQLParameter(true);
        StringBuilder sqlBuilder = new StringBuilder("update cmp_bankreconciliation set isrepeat=0 where isrepeat is null and ytenant_id = ?");
        parameter.addParam(ytenantId);
        if (CollectionUtils.isNotEmpty(bankReconciliations)) {
            sqlBuilder.append(" AND id in ");
            sqlBuilder.append(" ( ");
            String separator0 = " , ";
            for (int i = 0; i < bankReconciliations.size(); i++) {
                sqlBuilder.append(" ? ");
                parameter.addParam(bankReconciliations.get(i).getLong("id"));
                if (i < bankReconciliations.size() - 1) {
                    sqlBuilder.append(separator0);
                }
            }
            sqlBuilder.append(" ) ");
        }
        ymsJdbcApi.update(sqlBuilder.toString(), parameter);
    }

    /**
     * SELECT distinct id FROM cmp_bankreconciliation br JOIN (SELECT ytenant_id,bankaccount,tran_date,tran_amt,dc_flag,to_acct,to_acct_no
     *   FROM cmp_bankreconciliation WHERE isrepeat IN (1, 2, 3) and ytenant_id='wdze9x4t') subquery on
     * 	(br.bankaccount = subquery.bankaccount or (br.bankaccount is null and subquery.bankaccount is null))
     * 	and (br.tran_date = subquery.tran_date or (br.tran_date is null and subquery.tran_date is null))
     * 	and (br.to_acct_no = subquery.to_acct_no or (br.to_acct_no is null and  subquery.to_acct_no is null))
     * WHERE br.ytenant_id = 'wdze9x4t'
     * @param repeatFactors
     * @param ytenantId
     * @return
     */
    @Override
    public List<BankReconciliation> selectRepeatDataWithNormal(String repeatFactors, String ytenantId) {
        StringBuilder joinStr = new StringBuilder("br.ytenant_id = subquery.ytenant_id ");
        for(String repeatFactor: repeatFactors.split(",")){
            joinStr.append(" and (br.").append(repeatFactor).append(" = ").append("subquery.").append(repeatFactor)
                    .append(" or (br.").append(repeatFactor).append(" is null and subquery. ").append(repeatFactor).append(" is null))");
        }
        SQLParameter parameter = new SQLParameter(true);
        StringBuilder sqlBuilder = new StringBuilder("SELECT distinct br.id FROM cmp_bankreconciliation br JOIN (SELECT ytenant_id,")
                .append(repeatFactors)
                .append(" FROM cmp_bankreconciliation WHERE isrepeat IN (1, 2, 3) and ytenant_id=?) subquery on ")
                .append(joinStr)
                .append(" WHERE br.ytenant_id = ?");
        parameter.addParam(ytenantId);
        parameter.addParam(ytenantId);
        List<BankReconciliation> list = ymsJdbcApi.queryForList(sqlBuilder.toString(),parameter,new ResultSetProcessor(){

            @Override
            public Object handleResultSet(ResultSet rs) throws SQLException {
                List<BankReconciliation> list = new ArrayList<>();
                while(rs.next()){
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
        return list;
    }
}
