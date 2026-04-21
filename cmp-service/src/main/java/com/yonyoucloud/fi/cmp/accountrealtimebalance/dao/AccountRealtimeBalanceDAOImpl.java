package com.yonyoucloud.fi.cmp.accountrealtimebalance.dao;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.param.ResultSetProcessor;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyoucloud.fi.cmp.accountrealtimebalance.AccountRealtimeBalance;
import com.yonyoucloud.fi.cmp.constant.ICsplConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import groovy.util.logging.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class AccountRealtimeBalanceDAOImpl implements AccountRealtimeBalanceDAO {

    @Autowired
    @Qualifier("busiBaseDAO")
    private IYmsJdbcApi ymsJdbcApi;

    /**
     * select t1.* from cmp_bankaccount_realtimebalance t1 inner join (SELECT max(balancedate) as balancedate ,enterprisebankaccount,currency,ytenant_id FROM cmp_bankaccount_realtimebalance where ytenant_id= '0000LPS4DGO24AR11H0000' and balancedate <= DATE_FORMAT('2024-11-28','%Y-%m-%d')
     * AND enterpriseBankAccount in ('1877554806278062089','1878161693757931520','2056301234790334467')
     * AND currency in ('1877480589267304482','1877480589267304485')
     * GROUP BY enterprisebankaccount,currency,ytenant_id ) t2 on t1.enterprisebankaccount = t2.enterprisebankaccount and t1.currency = t2.currency and t1.balancedate = t2.balancedate and t1.ytenant_id = t2.ytenant_id
     * WHERE t1.first_flag = '0'
     * @param enterpriseBankAccounts
     * @param accentitys
     * @param currency
     * @param currencyList
     * @param startDate
     * @param endDate
     * @return
     */
   @Override
public List<AccountRealtimeBalance> queryTraceabilityBalance(List<String> enterpriseBankAccounts, List<String> accentitys, String currency, List<String> currencyList, String startDate, String endDate) {
    // 第一步：查询符合条件的最大余额日期记录
    SQLParameter subQueryParameter = new SQLParameter(true);
    StringBuilder subQuerySql = new StringBuilder("SELECT max(balancedate) as balancedate ,enterprisebankaccount,currency,ytenant_id FROM cmp_bankaccount_realtimebalance where ytenant_id= ? and balancedate <= str_to_date(?,'%Y-%m-%d')");
    subQueryParameter.addParam(InvocationInfoProxy.getTenantid());
    subQueryParameter.addParam(startDate);

    String separator0 = " , ";
    if (CollectionUtils.isEmpty(enterpriseBankAccounts) && !CollectionUtils.isEmpty(accentitys)) {
        subQuerySql.append(" AND accentity in ( ");
        for (int i = 0; i < accentitys.size(); i++) {
            String item = accentitys.get(i);
            subQuerySql.append(" ? ");
            subQueryParameter.addParam(item);
            if (i < accentitys.size() - 1) {
                subQuerySql.append(separator0);
            }
        }
        subQuerySql.append(" ) ");
    }
    if (!CollectionUtils.isEmpty(enterpriseBankAccounts)) {
        subQuerySql.append(" AND enterpriseBankAccount in ( ");
        for (int i = 0; i < enterpriseBankAccounts.size(); i++) {
            String item = enterpriseBankAccounts.get(i);
            subQuerySql.append(" ? ");
            subQueryParameter.addParam(item);
            if (i < enterpriseBankAccounts.size() - 1) {
                subQuerySql.append(separator0);
            }
        }
        subQuerySql.append(" ) ");
    }
    if (currency != null) {
        subQuerySql.append(" AND currency = ? ");
        subQueryParameter.addParam(currency);
    }
    if(CollectionUtils.isNotEmpty(currencyList)){
        subQuerySql.append(" AND currency in ( ");
        for (int i = 0; i < currencyList.size(); i++) {
            String item = currencyList.get(i);
            subQuerySql.append(" ? ");
            subQueryParameter.addParam(item);
            if (i < currencyList.size() - 1) {
                subQuerySql.append(separator0);
            }
        }
        subQuerySql.append(" ) ");
    }
    subQuerySql.append(" GROUP BY enterprisebankaccount,currency,ytenant_id");

    // 执行子查询获取符合条件的最新余额记录
    List<BalanceMaxRecord> maxBalanceRecords = ymsJdbcApi.queryForList(subQuerySql.toString(), subQueryParameter, new ResultSetProcessor() {
        @Override
        public Object handleResultSet(ResultSet rs) throws SQLException {
            List<BalanceMaxRecord> list = new ArrayList<>();
            while (rs.next()) {
                BalanceMaxRecord record = new BalanceMaxRecord();
                record.setBalanceDate(rs.getDate("balancedate"));
                record.setEnterpriseBankAccount(rs.getString("enterprisebankaccount"));
                record.setCurrency(rs.getString("currency"));
                record.setYTenantId(rs.getString("ytenant_id"));
                list.add(record);
            }
            return list;
        }

        @Override
        public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
            return null;
        }
    });

    if (maxBalanceRecords.isEmpty()) {
        return new ArrayList<>();
    }

    // 第二步：根据第一步的结果查询具体的余额记录
    SQLParameter mainQueryParameter = new SQLParameter(true);
    StringBuilder mainQuerySql = new StringBuilder("select ")
            .append(ICsplConstant.SELECT_TOTAL_Field)
            .append(" from cmp_bankaccount_realtimebalance where first_flag = '0' AND (");

    // 构建查询条件，使用 (enterprisebankaccount, currency, balancedate, ytenant_id) 组合条件
    for (int i = 0; i < maxBalanceRecords.size(); i++) {
        BalanceMaxRecord record = maxBalanceRecords.get(i);
        if (i > 0) {
            mainQuerySql.append(" OR ");
        }
        mainQuerySql.append("(enterprisebankaccount = ? AND currency = ? AND balancedate = ? AND ytenant_id = ?)");
        mainQueryParameter.addParam(record.getEnterpriseBankAccount());
        mainQueryParameter.addParam(record.getCurrency());
        mainQueryParameter.addParam(record.getBalanceDate());
        mainQueryParameter.addParam(record.getYTenantId());
    }
    mainQuerySql.append(")");

    return ymsJdbcApi.queryForList(mainQuerySql.toString(), mainQueryParameter, new ResultSetProcessor() {
        @Override
        public Object handleResultSet(ResultSet rs) throws SQLException {
            List<AccountRealtimeBalance> list1 = new ArrayList<>();
            while (rs.next()) {
                AccountRealtimeBalance accountRealtimeBalance = new AccountRealtimeBalance();
                accountRealtimeBalance.setId(rs.getLong("id"));
                accountRealtimeBalance.setAccentity(rs.getString("accentity"));
                accountRealtimeBalance.setAcctbal(rs.getBigDecimal("acctbal"));
                accountRealtimeBalance.setAvlbal(rs.getBigDecimal("avlbal"));
                accountRealtimeBalance.setBanktype(rs.getString("banktype"));
                accountRealtimeBalance.setCashflag(rs.getString("cashflag"));
                accountRealtimeBalance.setCurrency(rs.getString("currency"));
                accountRealtimeBalance.setEnterpriseBankAccount(rs.getString("enterprisebankaccount"));
                accountRealtimeBalance.setFrzbal(rs.getBigDecimal("frzbal"));
                accountRealtimeBalance.setNatCurrency(rs.getString("iNatCurrencyID"));
                accountRealtimeBalance.setYesterbal(rs.getBigDecimal("yesterbal"));
                accountRealtimeBalance.setBalancedate(rs.getDate("balancedate"));
                accountRealtimeBalance.setFlag(rs.getString("flag"));
                accountRealtimeBalance.setProj_name(rs.getString("proj_name"));
                accountRealtimeBalance.setSub_name(rs.getString("sub_name"));
                accountRealtimeBalance.setBudget_source(rs.getString("budget_source"));
                accountRealtimeBalance.setAcctbalcount(rs.getBigDecimal("id"));
                accountRealtimeBalance.setBalancecontrast(rs.getShort("balancecontrast"));
                accountRealtimeBalance.setDepositbalance(rs.getBigDecimal("depositbalance"));
                accountRealtimeBalance.setOverdraftbalance(rs.getBigDecimal("overdraftbalance"));
                accountRealtimeBalance.setIsconfirm(rs.getBoolean("isconfirm"));
                accountRealtimeBalance.setDatasource(rs.getShort("datasource"));
                accountRealtimeBalance.setBalancecheckinstruction(rs.getString("balancecheckinstruction"));
                accountRealtimeBalance.setBalanceconfirmerid(rs.getLong("balanceconfirmerid"));
                accountRealtimeBalance.setBalanceconfirmtime(rs.getDate("balanceconfirmtime"));
                accountRealtimeBalance.setFirst_flag(rs.getString("first_flag"));
                accountRealtimeBalance.setRegular_amt(rs.getBigDecimal("regular_amt"));
                accountRealtimeBalance.setTotal_amt(rs.getBigDecimal("total_amt"));
                accountRealtimeBalance.set("ytenantId", rs.getString("ytenant_id"));
                list1.add(accountRealtimeBalance);
            }
            return list1;
        }

        @Override
        public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
            return null;
        }
    });
}

    // 辅助类：用于存储最大余额日期记录
    private static class BalanceMaxRecord {
        private Date balanceDate;
        private String enterpriseBankAccount;
        private String currency;
        private String yTenantId;

        // getter和setter方法
        public Date getBalanceDate() {
            return balanceDate;
        }

        public void setBalanceDate(Date balanceDate) {
            this.balanceDate = balanceDate;
        }

        public String getEnterpriseBankAccount() {
            return enterpriseBankAccount;
        }

        public void setEnterpriseBankAccount(String enterpriseBankAccount) {
            this.enterpriseBankAccount = enterpriseBankAccount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getYTenantId() {
            return yTenantId;
        }

        public void setYTenantId(String yTenantId) {
            this.yTenantId = yTenantId;
        }
    }
}
