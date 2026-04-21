package com.yonyoucloud.fi.cmp.reconciliate.vo;

import com.yonyoucloud.fi.cmp.bankautocheckconfig.BankAutoCheckConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.journal.Journal;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @description: 记录凭证/银行日记账和银行流水勾对的关系
 * @author: wanxbo@yonyou.com
 * @date: 2025/11/14 15:50
 */
@Data
public class ReconciliationInfoVO {
    /**
     * 对账组织
     */
    private String accentity;
    /**
     * 银行账户
     */
    private String bankaccount;
    /**
     * 币种
     */
    private String currency;
    /**
     * 对账方案
     */
    private String reconciliationScheme;
    /**
     * 对账数据源 1凭证；2银行日记账
     */
    private Short reconciliationDataSource;
    /**
     * 勾对号；唯一不重复
     */
    private String checkno;
    /**
     * 勾对日期
     */
    private Date checkDate;
    /**
     * 勾对时间
     */
    private Date checkTime;
    /**
     * 对账依据。1财资统一对账码勾对；2关键要素匹配；3手工对账；4净额对账；5单边对账
     */
    private Short reconciliationBasis;
    /**
     * 勾对人
     */
    private Long checkOperator;
    /**
     * 对账依据=2关键要素匹配时，依据自动对账设置匹配
     */
    private BankAutoCheckConfig bankAutoCheckConfig;
    /**
     * 凭证、银行日记账
     */
    private List<Journal> journalList = new ArrayList<>();
    /**
     * 银行流水
     */
    private List<BankReconciliation> bankReconciliationList = new ArrayList<>();
}
