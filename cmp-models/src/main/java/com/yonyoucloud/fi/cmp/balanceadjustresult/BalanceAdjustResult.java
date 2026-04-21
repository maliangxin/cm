package com.yonyoucloud.fi.cmp.balanceadjustresult;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IAutoCode;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.*;
import com.yonyoucloud.fi.cmp.bankreconciliation.BalanceadjustBankreconciliation;
import com.yonyoucloud.fi.cmp.journal.BalanceadjustJournal;
import org.imeta.orm.base.BizObject;

/**
 * 余额调节表列表实体
 *
 * @author u
 * @version 1.0
 */
public class BalanceAdjustResult extends BizObject implements IAutoCode, IApprovalInfo, IAuditInfo, ITenant, IApprovalFlow, IPrintCount, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.balanceadjustresult.BalanceAdjustResult";

    /**
     * 获取资金组织
     *
     * @return 资金组织.ID
     */
	public String getAccentity() {
		return get("accentity");
	}

    /**
     * 设置资金组织
     *
     * @param accentity 资金组织.ID
     */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}
	/**
	 * 获取对账方案
	 *
	 * @return 对账方案.ID
	 */
	public Long getBankreconciliationscheme() {
		return get("bankreconciliationscheme");
	}

	/**
	 * 设置对账方案
	 *
	 * @param bankreconciliationscheme 对账方案.ID
	 */
	public void setBankreconciliationscheme(Long bankreconciliationscheme) {
		set("bankreconciliationscheme", bankreconciliationscheme);
	}
    /**
     * 获取企业银行账户
     *
     * @return 企业银行账户.ID
     */
	public String getBankaccount() {
		return get("bankaccount");
	}

    /**
     * 设置企业银行账户
     *
     * @param bankaccount 企业银行账户.ID
     */
	public void setBankaccount(String bankaccount) {
		set("bankaccount", bankaccount);
	}

    /**
     * 获取币种
     *
     * @return 币种.ID
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置币种
     *
     * @param currency 币种.ID
     */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

    /**
     * 获取对账状态
     *
     * @return 对账状态
     */
	public Long getBalenceState() {
		return get("balenceState");
	}

    /**
     * 设置对账状态
     *
     * @param balenceState 对账状态
     */
	public void setBalenceState(Long balenceState) {
		set("balenceState", balenceState);
	}
	/**
	 * 获取记账截止日期
	 *
	 * @return 记账截止日期
	 */
	public java.util.Date getDzdate() {
		return get("dzdate");
	}

	/**
	 * 设置记账截止日期
	 *
	 * @param dzdate 记账截止日期
	 */
	public void setDzdate(java.util.Date dzdate) {
		set("dzdate", dzdate);
	}
    /**
     * 获取企业日记账截止日期
     *
     * @return 企业日记账截止日期
     */
	public java.util.Date getJournaldate() {
		return get("journaldate");
	}

    /**
     * 设置企业日记账截止日期
     *
     * @param journaldate 企业日记账截止日期
     */
	public void setJournaldate(java.util.Date journaldate) {
		set("journaldate", journaldate);
	}

    /**
     * 获取企业日记账余额
     *
     * @return 企业日记账余额
     */
	public java.math.BigDecimal getJournalye() {
		return get("journalye");
	}

    /**
     * 设置企业日记账余额
     *
     * @param journalye 企业日记账余额
     */
	public void setJournalye(java.math.BigDecimal journalye) {
		set("journalye", journalye);
	}

	/**
	 * 获取企业日记账余额详情
	 *
	 * @return 企业日记账余额详情
	 */
	public String getJournalyedetailinfo() {
		return get("journalyedetailinfo");
	}

	/**
	 * 设置企业日记账余额详情
	 *
	 * @param journalyedetailinfo 企业日记账余额详情
	 */
	public void setJournalyedetailinfo(String journalyedetailinfo) {
		set("journalyedetailinfo", journalyedetailinfo);
	}

    /**
     * 获取银收企未收
     *
     * @return 银收企未收
     */
	public java.math.BigDecimal getJournalyhys() {
		return get("journalyhys");
	}

    /**
     * 设置银收企未收
     *
     * @param journalyhys 银收企未收
     */
	public void setJournalyhys(java.math.BigDecimal journalyhys) {
		set("journalyhys", journalyhys);
	}

    /**
     * 获取银付企未付
     *
     * @return 银付企未付
     */
	public java.math.BigDecimal getJournalyhyf() {
		return get("journalyhyf");
	}

    /**
     * 设置银付企未付
     *
     * @param journalyhyf 银付企未付
     */
	public void setJournalyhyf(java.math.BigDecimal journalyhyf) {
		set("journalyhyf", journalyhyf);
	}

    /**
     * 获取调整后余额
     *
     * @return 调整后余额
     */
	public java.math.BigDecimal getJournaltzye() {
		return get("journaltzye");
	}

    /**
     * 设置调整后余额
     *
     * @param journaltzye 调整后余额
     */
	public void setJournaltzye(java.math.BigDecimal journaltzye) {
		set("journaltzye", journaltzye);
	}

    /**
     * 获取银行对账单截止日期
     *
     * @return 银行对账单截止日期
     */
	public java.util.Date getBankdate() {
		return get("bankdate");
	}

    /**
     * 设置银行对账单截止日期
     *
     * @param bankdate 银行对账单截止日期
     */
	public void setBankdate(java.util.Date bankdate) {
		set("bankdate", bankdate);
	}

    /**
     * 获取银行对账单余额
     *
     * @return 银行对账单余额
     */
	public java.math.BigDecimal getBankye() {
		return get("bankye");
	}

    /**
     * 设置银行对账单余额
     *
     * @param bankye 银行对账单余额
     */
	public void setBankye(java.math.BigDecimal bankye) {
		set("bankye", bankye);
	}

    /**
     * 获取企收银未收
     *
     * @return 企收银未收
     */
	public java.math.BigDecimal getBankqyys() {
		return get("bankqyys");
	}

    /**
     * 设置企收银未收
     *
     * @param bankqyys 企收银未收
     */
	public void setBankqyys(java.math.BigDecimal bankqyys) {
		set("bankqyys", bankqyys);
	}

    /**
     * 获取企付银付
     *
     * @return 企付银付
     */
	public java.math.BigDecimal getBankqyyf() {
		return get("bankqyyf");
	}

    /**
     * 设置企付银付
     *
     * @param bankqyyf 企付银付
     */
	public void setBankqyyf(java.math.BigDecimal bankqyyf) {
		set("bankqyyf", bankqyyf);
	}

    /**
     * 获取调整后余额
     *
     * @return 调整后余额
     */
	public java.math.BigDecimal getBanktzye() {
		return get("banktzye");
	}

    /**
     * 设置调整后余额
     *
     * @param banktzye 调整后余额
     */
	public void setBanktzye(java.math.BigDecimal banktzye) {
		set("banktzye", banktzye);
	}

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
	public java.util.Date getCreateTime() {
		return get("createTime");
	}

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
	public void setCreateTime(java.util.Date createTime) {
		set("createTime", createTime);
	}

    /**
     * 获取创建日期
     *
     * @return 创建日期
     */
	public java.util.Date getCreateDate() {
		return get("createDate");
	}

    /**
     * 设置创建日期
     *
     * @param createDate 创建日期
     */
	public void setCreateDate(java.util.Date createDate) {
		set("createDate", createDate);
	}

    /**
     * 获取修改时间
     *
     * @return 修改时间
     */
	public java.util.Date getModifyTime() {
		return get("modifyTime");
	}

    /**
     * 设置修改时间
     *
     * @param modifyTime 修改时间
     */
	public void setModifyTime(java.util.Date modifyTime) {
		set("modifyTime", modifyTime);
	}

    /**
     * 获取修改日期
     *
     * @return 修改日期
     */
	public java.util.Date getModifyDate() {
		return get("modifyDate");
	}

    /**
     * 设置修改日期
     *
     * @param modifyDate 修改日期
     */
	public void setModifyDate(java.util.Date modifyDate) {
		set("modifyDate", modifyDate);
	}

    /**
     * 获取创建人名称
     *
     * @return 创建人名称
     */
	public String getCreator() {
		return get("creator");
	}

    /**
     * 设置创建人名称
     *
     * @param creator 创建人名称
     */
	public void setCreator(String creator) {
		set("creator", creator);
	}

    /**
     * 获取修改人名称
     *
     * @return 修改人名称
     */
	public String getModifier() {
		return get("modifier");
	}

    /**
     * 设置修改人名称
     *
     * @param modifier 修改人名称
     */
	public void setModifier(String modifier) {
		set("modifier", modifier);
	}

    /**
     * 获取创建人
     *
     * @return 创建人.ID
     */
	public Long getCreatorId() {
		return get("creatorId");
	}

    /**
     * 设置创建人
     *
     * @param creatorId 创建人.ID
     */
	public void setCreatorId(Long creatorId) {
		set("creatorId", creatorId);
	}

    /**
     * 获取修改人
     *
     * @return 修改人.ID
     */
	public Long getModifierId() {
		return get("modifierId");
	}

    /**
     * 设置修改人
     *
     * @param modifierId 修改人.ID
     */
	public void setModifierId(Long modifierId) {
		set("modifierId", modifierId);
	}

    /**
     * 获取租户
     *
     * @return 租户.ID
     */
	public Long getTenant() {
		return get("tenant");
	}

    /**
     * 设置租户
     *
     * @param tenant 租户.ID
     */
	public void setTenant(Long tenant) {
		set("tenant", tenant);
	}

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
	public java.util.Date getPubts() {
		return get("pubts");
	}

    /**
     * 设置时间戳
     *
     * @param pubts 时间戳
     */
	public void setPubts(java.util.Date pubts) {
		set("pubts", pubts);
	}

	/**
	 * 获取日记账明细子表集合
	 *
	 * @return 日记账明细子表集合
	 */
	public java.util.List<BalanceadjustJournal> balanceadjustJournal() {
		return getBizObjects("balanceadjustJournal", BalanceadjustJournal.class);
	}

	/**
	 * 设置日记账明细子表集合
	 *
	 * @param balanceadjustJournal 日记账明细子表集合
	 */
	public void setBalanceadjustJournal(java.util.List<BalanceadjustJournal> balanceadjustJournal) {
		setBizObjects("balanceadjustJournal", balanceadjustJournal);
	}

	/**
	 * 获取对账单明细子表集合
	 *
	 * @return 对账单明细子表集合
	 */
	public java.util.List<BalanceadjustBankreconciliation> balanceadjustBankreconciliation() {
		return getBizObjects("balanceadjustBankreconciliation", BalanceadjustBankreconciliation.class);
	}

	/**
	 * 设置对账单明细子表集合
	 *
	 * @param balanceadjustBankreconciliation 对账单明细子表集合
	 */
	public void setBalanceadjustBankreconciliation(java.util.List<BalanceadjustBankreconciliation> balanceadjustBankreconciliation) {
		setBizObjects("balanceadjustBankreconciliation", balanceadjustBankreconciliation);
	}

	/**
	 * 获取打印次数
	 *
	 * @return 打印次数
	 */
	public Integer getPrintCount() {
		return get("printCount");
	}

	/**
	 * 设置打印次数
	 *
	 * @param printCount 打印次数
	 */
	public void setPrintCount(Integer printCount) {
		set("printCount", printCount);
	}

    /**
     * 获取审批状态
     *
     * @return 审批状态
     */
    public Short getAuditstatus() {
        return getShort("auditstatus");
    }

    /**
     * 设置审批状态
     *
     * @param auditstatus 审批状态
     */
    public void setAuditstatus(Short auditstatus) {
        set("auditstatus", auditstatus);
    }

	/**
	 * 获取归档状态
	 *
	 * @return 归档状态
	 */
	public Boolean getArchivingstatus() {
		return getBoolean("archivingstatus");
	}

	/**
	 * 设置归档状态
	 *
	 * @param archivingstatus 归档状态
	 */
	public void setArchivingstatus(Boolean archivingstatus) {
		set("archivingstatus", archivingstatus);
	}
	/**
	 * 获取是否存在未勾对
	 *
	 * @return 是否存在未勾对
	 */
	public Boolean getUncheckflag() {
		return getBoolean("uncheckflag");
	}

	/**
	 * 设置是否存在未勾对
	 *
	 * @param uncheckflag 是否存在未勾对
	 */
	public void setUncheckflag(Boolean uncheckflag) {
		set("uncheckflag", uncheckflag);
	}

	/**
	 * 获取审核人
	 *
	 * @return 审核人
	 */
	public String getOperator() {
		return getString("operator");
	}

	/**
	 * 设置审核人
	 *
	 * @param operator 审核人
	 */
	public void setOperator(String operator) {
		set("operator", operator);
	}

	/**
	 * 获取审核时间
	 *
	 * @return 审核时间
	 */
	public java.util.Date getOperatorTime() {
		return get("operateTime");
	}

	/**
	 * 设置审核时间
	 *
	 * @param operateTime 审核时间
	 */
	public void setOperatorTime(java.util.Date operateTime) {
		set("operateTime", operateTime);
	}

	/**
	 * 获取审核人名称
	 *
	 * @return 审核人名称
	 */
	public String getOperatorName() {
		return getString("operatorName");
	}

	/**
	 * 设置审核人名称
	 *
	 * @param operatorName 审核人名称
	 */
	public void setOperatorName(String operatorName) {
		set("operatorName", operatorName);
	}


	/**
	 * 获取交易类型
	 *
	 * @return 交易类型.ID
	 */
	public String getTradetype() {
		return get("tradetype");
	}

	/**
	 * 设置交易类型
	 *
	 * @param tradetype 交易类型.ID
	 */
	public void setTradetype(String tradetype) {
		set("tradetype", tradetype);
	}

	/**
	 * 获取审批流状态
	 *
	 * @return 审批流状态
	 */
	public Short getVerifystate() {
		return getShort("verifystate");
	}

	/**
	 * 设置审批流状态
	 *
	 * @param verifystate 审批流状态
	 */
	public void setVerifystate(Short verifystate) {
		set("verifystate", verifystate);
	}

	/**
	 * 获取退回次数
	 *
	 * @return 退回次数
	 */
	public Short getReturncount() {
		return getShort("returncount");
	}

	/**
	 * 设置退回次数
	 *
	 * @param returncount 退回次数
	 */
	public void setReturncount(Short returncount) {
		set("returncount", returncount);
	}
	/**
	 * 获取是否审批流控制
	 *
	 * @return 是否审批流控制
	 */
	public Boolean getIsWfControlled() {
		return getBoolean("isWfControlled");
	}

	/**
	 * 设置是否审批流控制
	 *
	 * @param isWfControlled 是否审批流控制
	 */
	public void setIsWfControlled(Boolean isWfControlled) {
		set("isWfControlled", isWfControlled);
	}

	/**
	 * 获取审批日期
	 *
	 * @return 审批日期
	 */
	public java.util.Date getAuditDate() {
		return get("auditDate");
	}

	/**
	 * 设置审批日期
	 *
	 * @param auditDate 审批日期
	 */
	public void setAuditDate(java.util.Date auditDate) {
		set("auditDate", auditDate);
	}

	/**
	 * 获取审批时间
	 *
	 * @return 审批时间
	 */
	public java.util.Date getAuditTime() {
		return get("auditTime");
	}

	/**
	 * 设置审批时间
	 *
	 * @param auditTime 审批时间
	 */
	public void setAuditTime(java.util.Date auditTime) {
		set("auditTime", auditTime);
	}

	/**
	 * 获取审批人名称
	 *
	 * @return 审批人名称
	 */
	public String getAuditor() {
		return get("auditor");
	}

	/**
	 * 设置审批人名称
	 *
	 * @param auditor 审批人名称
	 */
	public void setAuditor(String auditor) {
		set("auditor", auditor);
	}

	/**
	 * 获取审批人
	 *
	 * @return 审批人.ID
	 */
	public Long getAuditorId() {
		return get("auditorId");
	}

	/**
	 * 设置审批人
	 *
	 * @param auditorId 审批人.ID
	 */
	public void setAuditorId(Long auditorId) {
		set("auditorId", auditorId);
	}


	@Override
	public String getYTenant() {
		return null;
	}

	@Override
	public void setYTenant(String s) {

	}

	/**
	 * 获取单据编号
	 *
	 * @return 单据编号
	 */
	public String getCode() {
		return get("code");
	}

	/**
	 * 设置单据编号
	 *
	 * @param code 单据编号
	 */
	public void setCode(String code) {
		set("code", code);
	}
}
