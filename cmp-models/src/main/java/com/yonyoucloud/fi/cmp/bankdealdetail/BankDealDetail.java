package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.cmp.cmpentity.MerchantFlag;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import org.imeta.orm.base.BizObject;

/**
 * 银行交易明细实体
 *
 * @author u
 * @version 1.0
 */
public class BankDealDetail extends BizObject implements IAuditInfo, ITenant {
	private static final long serialVersionUID = 1L;
	// 实体全称
	public static final String ENTITY_NAME = "cmp.bankdealdetail.BankDealDetail";


	public static final String UNIQUE_NO = "unique_no";
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
     * 获取企业银行账户
     *
     * @return 企业银行账户.ID
     */
	public String getEnterpriseBankAccount() {
		return get("enterpriseBankAccount");
	}

	/**
	 * 8要素组合
	 * @param concat_info
	 */
	public void fillConcatInfo(String concat_info){
		set("concat_info", concat_info);
	}
    /**
     * 设置企业银行账户
     *
     * @param enterpriseBankAccount 企业银行账户.ID
     */
	public void setEnterpriseBankAccount(String enterpriseBankAccount) {
		set("enterpriseBankAccount", enterpriseBankAccount);
	}

    /**
     * 获取交易日期
     *
     * @return 交易日期
     */
	public java.util.Date getTranDate() {
		return get("tranDate");
	}

    /**
     * 设置交易日期
     *
     * @param tranDate 交易日期
     */
	public void setTranDate(java.util.Date tranDate) {
		set("tranDate", tranDate);
	}

    /**
     * 获取交易时间
     *
     * @return 交易时间
     */
	public java.util.Date getTranTime() {
		return get("tranTime");
	}

    /**
     * 设置交易时间
     *
     * @param tranTime 交易时间
     */
	public void setTranTime(java.util.Date tranTime) {
		set("tranTime", tranTime);
	}

    /**
     * 获取借/贷
     *
     * @return 借/贷
     */
	public Direction getDc_flag() {
		Number v = get("dc_flag");
		return Direction.find(v);
	}

    /**
     * 设置借/贷
     *
     * @param dc_flag 借/贷
     */
	public void setDc_flag(Direction dc_flag) {
		if (dc_flag != null) {
			set("dc_flag", dc_flag.getValue());
		} else {
			set("dc_flag", null);
		}
	}

    /**
     * 获取银行交易流水号
     *
     * @return 银行交易流水号
     */
	public String getBankseqno() {
		return get("bankseqno");
	}

    /**
     * 设置银行交易流水号
     *
     * @param bankseqno 银行交易流水号
     */
	public void setBankseqno(String bankseqno) {
		set("bankseqno", bankseqno);
	}

    /**
     * 获取对号方账
     *
     * @return 对号方账
     */
	public String getTo_acct_no() {
		return get("to_acct_no");
	}

    /**
     * 设置对号方账
     *
     * @param to_acct_no 对号方账
     */
	public void setTo_acct_no(String to_acct_no) {
		set("to_acct_no", to_acct_no);
	}

    /**
     * 获取对方户名
     *
     * @return 对方户名
     */
	public String getTo_acct_name() {
		return get("to_acct_name");
	}

    /**
     * 设置对方户名
     *
     * @param to_acct_name 对方户名
     */
	public void setTo_acct_name(String to_acct_name) {
		set("to_acct_name", to_acct_name);
	}

    /**
     * 获取对方开户行
     *
     * @return 对方开户行
     */
	public String getTo_acct_bank() {
		return get("to_acct_bank");
	}

    /**
     * 设置对方开户行
     *
     * @param to_acct_bank 对方开户行
     */
	public void setTo_acct_bank(String to_acct_bank) {
		set("to_acct_bank", to_acct_bank);
	}

    /**
     * 获取对方开户行名
     *
     * @return 对方开户行名
     */
	public String getTo_acct_bank_name() {
		return get("to_acct_bank_name");
	}

    /**
     * 设置对方开户行名
     *
     * @param to_acct_bank_name 对方开户行名
     */
	public void setTo_acct_bank_name(String to_acct_bank_name) {
		set("to_acct_bank_name", to_acct_bank_name);
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
     * 获取钞汇标志
     *
     * @return 钞汇标志
     */
	public String getCashflag() {
		return get("cashflag");
	}

    /**
     * 设置钞汇标志
     *
     * @param cashflag 钞汇标志
     */
	public void setCashflag(String cashflag) {
		set("cashflag", cashflag);
	}

    /**
     * 获取余额
     *
     * @return 余额
     */
	public java.math.BigDecimal getAcctbal() {
		return get("acctbal");
	}

    /**
     * 设置余额
     *
     * @param acctbal 余额
     */
	public void setAcctbal(java.math.BigDecimal acctbal) {
		set("acctbal", acctbal);
	}

    /**
     * 获取交易金额
     *
     * @return 交易金额
     */
	public java.math.BigDecimal getTran_amt() {
		return get("tran_amt");
	}

    /**
     * 设置交易金额
     *
     * @param tran_amt 交易金额
     */
	public void setTran_amt(java.math.BigDecimal tran_amt) {
		set("tran_amt", tran_amt);
	}

    /**
     * 获取操作员
     *
     * @return 操作员
     */
	public String getOper() {
		return get("oper");
	}

    /**
     * 设置操作员
     *
     * @param oper 操作员
     */
	public void setOper(String oper) {
		set("oper", oper);
	}

    /**
     * 获取起息日
     *
     * @return 起息日
     */
	public java.util.Date getValue_date() {
		return get("value_date");
	}

    /**
     * 设置起息日
     *
     * @param value_date 起息日
     */
	public void setValue_date(java.util.Date value_date) {
		set("value_date", value_date);
	}

    /**
     * 获取用途
     *
     * @return 用途
     */
	public String getUse_name() {
		return get("use_name");
	}

    /**
     * 设置用途
     *
     * @param use_name 用途
     */
	public void setUse_name(String use_name) {
		set("use_name", use_name);
	}

    /**
     * 获取摘要
     *
     * @return 摘要
     */
	public String getRemark() {
		return get("remark");
	}

    /**
     * 设置摘要
     *
     * @param remark 摘要
     */
	public void setRemark(String remark) {
		set("remark", remark);
	}

    /**
     * 获取是否存在客商档案
     *
     * @return 是否存在客商档案
     */
	public MerchantFlag getMerchant_flag() {
		Number v = get("merchant_flag");
		return MerchantFlag.find(v);
	}

    /**
     * 设置是否存在客商档案
     *
     * @param merchant_flag 是否存在客商档案
     */
	public void setMerchant_flag(MerchantFlag merchant_flag) {
		if (merchant_flag != null) {
			set("merchant_flag", merchant_flag.getValue());
		} else {
			set("merchant_flag", null);
		}
	}

    /**
     * 获取客户编码
     *
     * @return 客户编码
     */
	public String getMerchant_code() {
		return get("merchant_code");
	}

    /**
     * 设置客户编码
     *
     * @param merchant_code 客户编码
     */
	public void setMerchant_code(String merchant_code) {
		set("merchant_code", merchant_code);
	}

    /**
     * 获取客户分类
     *
     * @return 客户分类.ID
     */
	public Long getMerchantCustomerClass() {
		return get("merchantCustomerClass");
	}

    /**
     * 设置客户分类
     *
     * @param merchantCustomerClass 客户分类.ID
     */
	public void setMerchantCustomerClass(Long merchantCustomerClass) {
		set("merchantCustomerClass", merchantCustomerClass);
	}

    /**
     * 获取客商账户类型
     *
     * @return 客商账户类型.ID
     */
	public Long getMerchantAccountType() {
		return get("merchantAccountType");
	}

    /**
     * 设置客商账户类型
     *
     * @param merchantAccountType 客商账户类型.ID
     */
	public void setMerchantAccountType(Long merchantAccountType) {
		set("merchantAccountType", merchantAccountType);
	}

    /**
     * 获取客商国家id
     *
     * @return 客商国家id.ID
     */
	public String getMerchantCountry() {
		return get("merchantCountry");
	}

    /**
     * 设置客商国家id
     *
     * @param merchantCountry 客商国家id.ID
     */
	public void setMerchantCountry(String merchantCountry) {
		set("merchantCountry", merchantCountry);
	}

    /**
     * 获取客商银行网点
     *
     * @return 客商银行网点.ID
     */
	public String getMerchantOpenBank() {
		return get("merchantOpenBank");
	}

    /**
     * 设置客商银行网点
     *
     * @param merchantOpenBank 客商银行网点.ID
     */
	public void setMerchantOpenBank(String merchantOpenBank) {
		set("merchantOpenBank", merchantOpenBank);
	}

    /**
     * 获取客户名称
     *
     * @return 客户名称
     */
	public String getMerchantName() {
		return get("merchantName");
	}

    /**
     * 设置客户名称
     *
     * @param merchantName 客户名称
     */
	public void setMerchantName(String merchantName) {
		set("merchantName", merchantName);
	}

    /**
     * 获取银行明细对账编号
     *
     * @return 银行明细对账编号
     */
	public String getBankdetailno() {
		return get("bankdetailno");
	}

    /**
     * 设置银行明细对账编号
     *
     * @param bankdetailno 银行明细对账编号
     */
	public void setBankdetailno(String bankdetailno) {
		set("bankdetailno", bankdetailno);
	}

	/**
	 * 获取银行账户银行类别
	 *
	 * @return 银行账户银行类别.ID
	 */
	public String getBanktype() {
		return get("banktype");
	}

	/**
	 * 设置银行账户银行类别
	 *
	 * @param banktype 银行账户银行类别.ID
	 */
	public void setBanktype(String banktype) {
		set("banktype", banktype);
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
	 * 内部账户交易明细标志
	 *
	 * @return 内部账户交易明细标志
	 */
	public Boolean getIsaccountflag() {
		return getBoolean("isaccountflag");
	}

	/**
	 * 内部账户交易明细标志
	 *
	 * @param isaccountflag 坏账收回标志
	 */
	public void setIsaccountflag(Boolean isaccountflag) {
		set("isaccountflag", isaccountflag);
	}

	/**
	 * 获取银行对账编号
	 *
	 * @return 银行对账编号
	 */
	public String getBankcheckno() {
		return get("bankcheckno");
	}

	/**
	 * 设置银行对账编号
	 *
	 * @param bankcheckno 银行对账编号
	 */
	public void setBankcheckno(String bankcheckno) {
		set("bankcheckno", bankcheckno);
	}


	/**
	 * 获取预算来源
	 *
	 * @return 预算来源
	 */
	public String getBudget_source() {
		return get("budget_source");
	}

	/**
	 * 设置预算来源
	 *
	 * @param budget_source 预算来源
	 */
	public void setBudget_source(String budget_source) {
		set("budget_source", budget_source);
	}

	/**
	 * 获取代理手续费
	 *
	 * @return 代理手续费
	 */
	public java.math.BigDecimal getCorr_fee_amt() {
		return get("corr_fee_amt");
	}

	/**
	 * 设置代理手续费
	 *
	 * @param corr_fee_amt 代理手续费
	 */
	public void setCorr_fee_amt(java.math.BigDecimal corr_fee_amt) {
		set("corr_fee_amt", corr_fee_amt);
	}

	/**
	 * 获取代理手续费币种
	 *
	 * @return 代理手续费币种.ID
	 */
	public String getCorr_fee_amt_cur() {
		return get("corr_fee_amt_cur");
	}

	/**
	 * 设置代理手续费币种
	 *
	 * @param corr_fee_amt_cur 代理手续费币种.ID
	 */
	public void setCorr_fee_amt_cur(String corr_fee_amt_cur) {
		set("corr_fee_amt_cur", corr_fee_amt_cur);
	}

	/**
	 * 获取手续费金额
	 *
	 * @return 手续费金额
	 */
	public java.math.BigDecimal getFee_amt() {
		return get("fee_amt");
	}

	/**
	 * 设置手续费金额
	 *
	 * @param fee_amt 手续费金额
	 */
	public void setFee_amt(java.math.BigDecimal fee_amt) {
		set("fee_amt", fee_amt);
	}

	/**
	 * 获取手续费币种
	 *
	 * @return 手续费币种.ID
	 */
	public String getFee_amt_cur() {
		return get("fee_amt_cur");
	}

	/**
	 * 设置手续费币种
	 *
	 * @param fee_amt_cur 手续费币种.ID
	 */
	public void setFee_amt_cur(String fee_amt_cur) {
		set("fee_amt_cur", fee_amt_cur);
	}

	/**
	 * 获取汇款用途
	 *
	 * @return 汇款用途
	 */
	public String getPay_use_desc() {
		return get("pay_use_desc");
	}

	/**
	 * 设置汇款用途
	 *
	 * @param pay_use_desc 汇款用途
	 */
	public void setPay_use_desc(String pay_use_desc) {
		set("pay_use_desc", pay_use_desc);
	}

	/**
	 * 获取项目名称
	 *
	 * @return 项目名称
	 */
	public String getProj_name() {
		return get("proj_name");
	}

	/**
	 * 设置项目名称
	 *
	 * @param proj_name 项目名称
	 */
	public void setProj_name(String proj_name) {
		set("proj_name", proj_name);
	}

	/**
	 * 获取汇率
	 *
	 * @return 汇率
	 */
	public java.math.BigDecimal getRate() {
		return get("rate");
	}

	/**
	 * 设置汇率
	 *
	 * @param rate 汇率
	 */
	public void setRate(java.math.BigDecimal rate) {
		set("rate", rate);
	}

	/**
	 * 获取新增附言
	 *
	 * @return 新增附言
	 */
	public String getRemark01() {
		return get("remark01");
	}

	/**
	 * 设置新增附言
	 *
	 * @param remark01 新增附言
	 */
	public void setRemark01(String remark01) {
		set("remark01", remark01);
	}

	/**
	 * 获取科目名称
	 *
	 * @return 科目名称
	 */
	public String getSub_name() {
		return get("sub_name");
	}

	/**
	 * 设置科目名称
	 *
	 * @param sub_name 科目名称
	 */
	public void setSub_name(String sub_name) {
		set("sub_name", sub_name);
	}

	/**
	 * 获取凭证号
	 *
	 * @return 凭证号
	 */
	public String getVoucher_no() {
		return get("voucher_no");
	}

	/**
	 * 设置凭证号
	 *
	 * @param voucher_no 凭证号
	 */
	public void setVoucher_no(String voucher_no) {
		set("voucher_no", voucher_no);
	}

	/**
	 * 获取凭证类型
	 *
	 * @return 凭证类型
	 */
	public String getVoucher_type() {
		return get("voucher_type");
	}

    /**
     * 设置凭证类型
     *
     * @param voucher_type 凭证类型
     */
    public void setVoucher_type(String voucher_type) {
        set("voucher_type", voucher_type);
    }

    /**
     * 获取项目
     *
     * @return 项目
     */
    public String getProject() {
        return get("project");
    }

    /**
     * 设置项目
     *
     * @param project 项目
     */
    public void setProject(String project) {
        set("project", project);
    }

	/**
	 * 获取费用项目
	 *
	 * @return 费用项目
	 */
	public Long getExpenseItem() {
		return get("expenseItem");
	}

	/**
	 * 设置费用项目
	 *
	 * @param expenseItem 费用项目
	 */
	public void setExpenseItem(Long expenseItem) {
		set("expenseItem", expenseItem);
	}

	/**
	 * 唯一标识码
	 *
	 * @return 唯一标识码
	 */
	public String getUnique_no() {
		return get("unique_no");
	}

	/**
	 * 唯一标识码
	 *
	 * @param unique_no 唯一标识码
	 */
	public void setUnique_no(String unique_no) {
		set("unique_no", unique_no);
	}

	/**
	 * 字段唯一标识码
	 *
	 * @return 字段唯一标识码
	 */
	public String getConcat_info() {
		return get("concat_info");
	}

	/**
	 * 字段唯一标识码
	 *
	 * @param concat_info 字段唯一标识码
	 */
	private void setConcat_info(String concat_info) {
		set("concat_info", concat_info);
	}

	/**
	 * 字段四要素标识码
	 *
	 * @return 字段四要素标识码
	 */
	public String getConcat_info_4() {
		return get("concat_info_4");
	}

	/**
	 * 字段四要素标识码
	 *
	 * @param concat_info_4 字段四要素标识码
	 */
	public void setConcat_info_4(String concat_info_4) {
		set("concat_info_4", concat_info_4);
	}

	/**
	 * 分发状态
	 *
	 * @return 分发状态
	 */
	public Boolean getDistribution_status() {
		return getBoolean("distribution_status");
	}

	/**
	 * 分发状态
	 *
	 * @param distribution_status 分发状态
	 */
	public void setDistribution_status(Boolean distribution_status) {
		set("distribution_status", distribution_status);
	}

	/**
	 * 疑重状态
	 *
	 * @return 疑重状态
	 */
	public Integer getRepetition_status() {
		return getInteger("repetition_status");
	}

	/**
	 * 疑重状态
	 *
	 * @param repetition_status 疑重状态
	 */
	public void setRepetition_status(Integer repetition_status) {
		set("repetition_status", repetition_status);
	}


	/**
	 * 利息
	 *
	 * @return 利息
	 */
	public java.math.BigDecimal getInterest() {
		return get("interest");
	}

	/**
	 * 利息
	 *
	 * @param interest 利息
	 */
	public void setInterest(java.math.BigDecimal interest) {
		set("interest", interest);
	}

	/**
	 * 获取代理银行账户
	 *
	 * @return 代理银行账户.ID
	 */
	public String getAgentBankAccount() {
		return get("agentBankAccount");
	}

	/**
	 * 设置代理银行账户
	 *
	 * @param agentBankAccount 代理银行账户.ID
	 */
	public void setAgentBankAccount(String agentBankAccount) {
		set("agentBankAccount", agentBankAccount);
	}

	/**
	 * 获取退票
	 *
	 * @return 退票
	 */
	public Boolean getRefundFlag() {
		return get("refundflag");
	}

	/**
	 * 设置退票
	 *
	 * @param refundFlag 退票
	 */
	public void setRefundFlag(Boolean refundFlag) {
		set("refundflag",refundFlag);
	}

	/**
	 * 获取原交易流水号
	 *
	 * @return 原交易流水号
	 */
	public String getOriginBankseqno() {
		return get("originbankseqno");
	}

	/**
	 * 原交易流水
	 *
	 * @param orignBankseqno 原交易流水
	 */
	public void setOrignBankseqno(String orignBankseqno) {
		set("originbankseqno",orignBankseqno);
	}


	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

	/**
	 * 设置自定义项特征属性组
	 *
	 * @param characterDef 自定义项特征属性组.ID
	 */
	public void setCharacterDef(String characterDef) {
		set("characterDef", characterDef);
	}
}
