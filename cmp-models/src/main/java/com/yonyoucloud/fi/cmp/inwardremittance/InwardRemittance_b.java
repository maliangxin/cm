package com.yonyoucloud.fi.cmp.inwardremittance;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 汇入汇款子表实体
 *
 * @author u
 * @version 1.0
 */
public class InwardRemittance_b extends BizObject implements ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.inwardremittance.InwardRemittance_b";

    /**
     * 获取交易金额
     *
     * @return 交易金额
     */
	public java.math.BigDecimal getAmount1() {
		return get("amount1");
	}

    /**
     * 设置交易金额
     *
     * @param amount1 交易金额
     */
	public void setAmount1(java.math.BigDecimal amount1) {
		set("amount1", amount1);
	}

    /**
     * 获取交易金额2
     *
     * @return 交易金额2
     */
	public java.math.BigDecimal getAmount2() {
		return get("amount2");
	}

    /**
     * 设置交易金额2
     *
     * @param amount2 交易金额2
     */
	public void setAmount2(java.math.BigDecimal amount2) {
		set("amount2", amount2);
	}

    /**
     * 获取外汇局批件号/备案号/业务编号
     *
     * @return 外汇局批件号/备案号/业务编号
     */
	public String getApprovalno() {
		return get("approvalno");
	}

    /**
     * 设置外汇局批件号/备案号/业务编号
     *
     * @param approvalno 外汇局批件号/备案号/业务编号
     */
	public void setApprovalno(String approvalno) {
		set("approvalno", approvalno);
	}

    /**
     * 获取是否本行收款人
     *
     * @return 是否本行收款人
     */
	public Short getBankpayeeflag() {
	    return getShort("bankpayeeflag");
	}

    /**
     * 设置是否本行收款人
     *
     * @param bankpayeeflag 是否本行收款人
     */
	public void setBankpayeeflag(Short bankpayeeflag) {
		set("bankpayeeflag", bankpayeeflag);
	}

    /**
     * 获取是否为保税货物项下收汇
     *
     * @return 是否为保税货物项下收汇
     */
	public String getBodflag() {
		return get("bodflag");
	}

    /**
     * 设置是否为保税货物项下收汇
     *
     * @param bodflag 是否为保税货物项下收汇
     */
	public void setBodflag(String bodflag) {
		set("bodflag", bodflag);
	}

    /**
     * 获取收款性质
     *
     * @return 收款性质
     */
	public Short getCollectionproperties() {
	    return getShort("collectionproperties");
	}

    /**
     * 设置收款性质
     *
     * @param collectionproperties 收款性质
     */
	public void setCollectionproperties(Short collectionproperties) {
		set("collectionproperties", collectionproperties);
	}

    /**
     * 获取申报日期
     *
     * @return 申报日期
     */
	public java.util.Date getDeclarationdate() {
		return get("declarationdate");
	}

    /**
     * 设置申报日期
     *
     * @param declarationdate 申报日期
     */
	public void setDeclarationdate(java.util.Date declarationdate) {
		set("declarationdate", declarationdate);
	}

    /**
     * 获取申报标识
     *
     * @return 申报标识
     */
	public Short getDeclarationmark() {
	    return getShort("declarationmark");
	}

    /**
     * 设置申报标识
     *
     * @param declarationmark 申报标识
     */
	public void setDeclarationmark(Short declarationmark) {
		set("declarationmark", declarationmark);
	}

    /**
     * 获取申报人
     *
     * @return 申报人
     */
	public String getDeclarer() {
		return get("declarer");
	}

    /**
     * 设置申报人
     *
     * @param declarer 申报人
     */
	public void setDeclarer(String declarer) {
		set("declarer", declarer);
	}

    /**
     * 获取申报人电话
     *
     * @return 申报人电话
     */
	public String getDeclarertel() {
		return get("declarertel");
	}

    /**
     * 设置申报人电话
     *
     * @param declarertel 申报人电话
     */
	public void setDeclarertel(String declarertel) {
		set("declarertel", declarertel);
	}

    /**
     * 获取境内收入类型
     *
     * @return 境内收入类型
     */
	public String getIncometype() {
		return get("incometype");
	}

    /**
     * 设置境内收入类型
     *
     * @param incometype 境内收入类型
     */
	public void setIncometype(String incometype) {
		set("incometype", incometype);
	}

    /**
     * 获取汇入来源
     *
     * @return 汇入来源
     */
	public Short getInwardsource() {
	    return getShort("inwardsource");
	}

    /**
     * 设置汇入来源
     *
     * @param inwardsource 汇入来源
     */
	public void setInwardsource(Short inwardsource) {
		set("inwardsource", inwardsource);
	}

    /**
     * 获取主表id
     *
     * @return 主表id.ID
     */
	public Long getMainid() {
		return get("mainid");
	}

    /**
     * 设置主表id
     *
     * @param mainid 主表id.ID
     */
	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

    /**
     * 获取款项性质
     *
     * @return 款项性质
     */
	public String getNatureofpayment() {
		return get("natureofpayment");
	}

    /**
     * 设置款项性质
     *
     * @param natureofpayment 款项性质
     */
	public void setNatureofpayment(String natureofpayment) {
		set("natureofpayment", natureofpayment);
	}

    /**
     * 获取境外收入类型
     *
     * @return 境外收入类型
     */
	public String getOverseaincometype() {
		return get("overseaincometype");
	}

    /**
     * 设置境外收入类型
     *
     * @param overseaincometype 境外收入类型
     */
	public void setOverseaincometype(String overseaincometype) {
		set("overseaincometype", overseaincometype);
	}

    /**
     * 获取收款行行名
     *
     * @return 收款行行名
     */
	public String getPayeeaccountbankname() {
		return get("payeeaccountbankname");
	}

    /**
     * 设置收款行行名
     *
     * @param payeeaccountbankname 收款行行名
     */
	public void setPayeeaccountbankname(String payeeaccountbankname) {
		set("payeeaccountbankname", payeeaccountbankname);
	}

    /**
     * 获取收款行行号
     *
     * @return 收款行行号
     */
	public String getPayeeaccountbankno() {
		return get("payeeaccountbankno");
	}

    /**
     * 设置收款行行号
     *
     * @param payeeaccountbankno 收款行行号
     */
	public void setPayeeaccountbankno(String payeeaccountbankno) {
		set("payeeaccountbankno", payeeaccountbankno);
	}

    /**
     * 获取收款人名称
     *
     * @return 收款人名称
     */
	public String getPayeeaccountname() {
		return get("payeeaccountname");
	}

    /**
     * 设置收款人名称
     *
     * @param payeeaccountname 收款人名称
     */
	public void setPayeeaccountname(String payeeaccountname) {
		set("payeeaccountname", payeeaccountname);
	}

    /**
     * 获取收款人账号
     *
     * @return 收款人账号
     */
	public String getPayeeaccountno() {
		return get("payeeaccountno");
	}

    /**
     * 设置收款人账号
     *
     * @param payeeaccountno 收款人账号
     */
	public void setPayeeaccountno(String payeeaccountno) {
		set("payeeaccountno", payeeaccountno);
	}

    /**
     * 获取收款人常驻国家
     *
     * @return 收款人常驻国家.ID
     */
	public String getPayernation_code() {
		return get("payernation_code");
	}

    /**
     * 设置收款人常驻国家
     *
     * @param payernation_code 收款人常驻国家.ID
     */
	public void setPayernation_code(String payernation_code) {
		set("payernation_code", payernation_code);
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
     * 获取用途
     *
     * @return 用途
     */
	public String getPurpose() {
		return get("purpose");
	}

    /**
     * 设置用途
     *
     * @param purpose 用途
     */
	public void setPurpose(String purpose) {
		set("purpose", purpose);
	}

    /**
     * 获取是否为退汇
     *
     * @return 是否为退汇
     */
	public String getRefundflag() {
		return get("refundflag");
	}

    /**
     * 设置是否为退汇
     *
     * @param refundflag 是否为退汇
     */
	public void setRefundflag(String refundflag) {
		set("refundflag", refundflag);
	}

    /**
     * 获取退汇原因
     *
     * @return 退汇原因
     */
	public String getRefundreason() {
		return get("refundreason");
	}

    /**
     * 设置退汇原因
     *
     * @param refundreason 退汇原因
     */
	public void setRefundreason(String refundreason) {
		set("refundreason", refundreason);
	}

    /**
     * 获取序号
     *
     * @return 序号
     */
	public Integer getRowno() {
		return get("rowno");
	}

    /**
     * 设置序号
     *
     * @param rowno 序号
     */
	public void setRowno(Integer rowno) {
		set("rowno", rowno);
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
     * 获取交易编码
     *
     * @return 交易编码.ID
     */
	public Long getTransactioncode1() {
		return get("transactioncode1");
	}

    /**
     * 设置交易编码
     *
     * @param transactioncode1 交易编码.ID
     */
	public void setTransactioncode1(Long transactioncode1) {
		set("transactioncode1", transactioncode1);
	}

    /**
     * 获取交易编码2
     *
     * @return 交易编码2.ID
     */
	public Long getTransactioncode2() {
		return get("transactioncode2");
	}

    /**
     * 设置交易编码2
     *
     * @param transactioncode2 交易编码2.ID
     */
	public void setTransactioncode2(Long transactioncode2) {
		set("transactioncode2", transactioncode2);
	}

    /**
     * 获取交易附言
     *
     * @return 交易附言
     */
	public String getTransactionpostscript1() {
		return get("transactionpostscript1");
	}

    /**
     * 设置交易附言
     *
     * @param transactionpostscript1 交易附言
     */
	public void setTransactionpostscript1(String transactionpostscript1) {
		set("transactionpostscript1", transactionpostscript1);
	}

    /**
     * 获取交易附言2
     *
     * @return 交易附言2
     */
	public String getTransactionpostscript2() {
		return get("transactionpostscript2");
	}

    /**
     * 设置交易附言2
     *
     * @param transactionpostscript2 交易附言2
     */
	public void setTransactionpostscript2(String transactionpostscript2) {
		set("transactionpostscript2", transactionpostscript2);
	}

    /**
     * 获取交易类型
     *
     * @return 交易类型
     */
	public Short getTrantype() {
	    return getShort("trantype");
	}

    /**
     * 设置交易类型
     *
     * @param trantype 交易类型
     */
	public void setTrantype(Short trantype) {
		set("trantype", trantype);
	}

    /**
     * 获取租户id
     *
     * @return 租户id
     */
	@Override
	public String getYTenant() {
		return get("ytenant");
	}

    /**
     * 设置租户id
     *
     * @param ytenant 租户id
     */
	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

}
