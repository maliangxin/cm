package com.yonyoucloud.fi.cmp.electronicstatementconfirm;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 电子对账单确认实体
 *
 * @author u
 * @version 1.0
 */
public class ElectronicStatementConfirm extends BizObject implements IAuditInfo, ITenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.electronicstatementconfirm.ElectronicStatementConfirm";

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
     * 获取对账单标识
     *
     * @return 对账单标识
     */
	public String getStatement_id() {
		return get("statement_id");
	}

    /**
     * 设置对账单标识
     *
     * @param statement_id 对账单标识
     */
	public void setStatement_id(String statement_id) {
		set("statement_id", statement_id);
	}

    /**
     * 获取银行账号
     *
     * @return 银行账号
     */
	public String getBankaccount() {
		return get("bankaccount");
	}

    /**
     * 设置银行账号
     *
     * @param bankaccount 银行账号
     */
	public void setBankaccount(String bankaccount) {
		set("bankaccount", bankaccount);
	}

    /**
     * 获取银行账户名称
     *
     * @return 银行账户名称
     */
	public String getBank_name() {
		return get("bank_name");
	}

    /**
     * 设置银行账户名称
     *
     * @param bank_name 银行账户名称
     */
	public void setBank_name(String bank_name) {
		set("bank_name", bank_name);
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
     * 获取账单开始日期
     *
     * @return 账单开始日期
     */
	public java.util.Date getStartdate() {
		return get("startdate");
	}

    /**
     * 设置账单开始日期
     *
     * @param startdate 账单开始日期
     */
	public void setStartdate(java.util.Date startdate) {
		set("startdate", startdate);
	}

    /**
     * 获取账单结束日期
     *
     * @return 账单结束日期
     */
	public java.util.Date getEnddate() {
		return get("enddate");
	}

    /**
     * 设置账单结束日期
     *
     * @param enddate 账单结束日期
     */
	public void setEnddate(java.util.Date enddate) {
		set("enddate", enddate);
	}

    /**
     * 获取账单生成日期
     *
     * @return 账单生成日期
     */
	public java.util.Date getCreatedate() {
		return get("createdate");
	}

    /**
     * 设置账单生成日期
     *
     * @param createdate 账单生成日期
     */
	public void setCreatedate(java.util.Date createdate) {
		set("createdate", createdate);
	}

    /**
     * 获取已下载对账单
     *
     * @return 已下载对账单
     */
	public Boolean getStatement_download() {
	    return getBoolean("statement_download");
	}

    /**
     * 设置已下载对账单
     *
     * @param statement_download 已下载对账单
     */
	public void setStatement_download(Boolean statement_download) {
		set("statement_download", statement_download);
	}

    /**
     * 获取对账单文件名
     *
     * @return 对账单文件名
     */
	public String getStatement_name() {
		return get("statement_name");
	}

    /**
     * 设置对账单文件名
     *
     * @param statement_name 对账单文件名
     */
	public void setStatement_name(String statement_name) {
		set("statement_name", statement_name);
	}

    /**
     * 获取对账状态
     *
     * @return 对账状态
     */
	public Short getReconciliation_status() {
	    return getShort("reconciliation_status");
	}

    /**
     * 设置对账状态
     *
     * @param reconciliation_status 对账状态
     */
	public void setReconciliation_status(Short reconciliation_status) {
		set("reconciliation_status", reconciliation_status);
	}

    /**
     * 获取对账日期
     *
     * @return 对账日期
     */
	public java.util.Date getReconciliation_date() {
		return get("reconciliation_date");
	}

    /**
     * 设置对账日期
     *
     * @param reconciliation_date 对账日期
     */
	public void setReconciliation_date(java.util.Date reconciliation_date) {
		set("reconciliation_date", reconciliation_date);
	}

    /**
     * 获取对账结果
     *
     * @return 对账结果
     */
	public Short getReconciliation_result() {
	    return getShort("reconciliation_result");
	}

    /**
     * 设置对账结果
     *
     * @param reconciliation_result 对账结果
     */
	public void setReconciliation_result(Short reconciliation_result) {
		set("reconciliation_result", reconciliation_result);
	}

    /**
     * 获取银行余额
     *
     * @return 银行余额
     */
	public java.math.BigDecimal getBank_balance() {
		return get("bank_balance");
	}

    /**
     * 设置银行余额
     *
     * @param bank_balance 银行余额
     */
	public void setBank_balance(java.math.BigDecimal bank_balance) {
		set("bank_balance", bank_balance);
	}

	/**
	 * 获取账单支出总金额
	 *
	 * @return 账单支出总金额
	 */
	public java.math.BigDecimal getTotal_amount() {
		return get("total_amount");
	}

	/**
	 * 设置账单支出总金额
	 *
	 * @param total_amount 账单支出总金额
	 */
	public void setTotal_amount(java.math.BigDecimal total_amount) {
		set("total_amount", total_amount);
	}

    /**
     * 获取企业余额
     *
     * @return 企业余额
     */
	public java.math.BigDecimal getBanenterprise_balance() {
		return get("banenterprise_balance");
	}

    /**
     * 设置企业余额
     *
     * @param banenterprise_balance 企业余额
     */
	public void setBanenterprise_balance(java.math.BigDecimal banenterprise_balance) {
		set("banenterprise_balance", banenterprise_balance);
	}

    /**
     * 获取调整后银行余额
     *
     * @return 调整后银行余额
     */
	public java.math.BigDecimal getAdjust_bank_balance() {
		return get("adjust_bank_balance");
	}

    /**
     * 设置调整后银行余额
     *
     * @param adjust_bank_balance 调整后银行余额
     */
	public void setAdjust_bank_balance(java.math.BigDecimal adjust_bank_balance) {
		set("adjust_bank_balance", adjust_bank_balance);
	}

    /**
     * 获取调整后企业余额
     *
     * @return 调整后企业余额
     */
	public java.math.BigDecimal getAdjust_enterprise_balance() {
		return get("adjust_enterprise_balance");
	}

    /**
     * 设置调整后企业余额
     *
     * @param adjust_enterprise_balance 调整后企业余额
     */
	public void setAdjust_enterprise_balance(java.math.BigDecimal adjust_enterprise_balance) {
		set("adjust_enterprise_balance", adjust_enterprise_balance);
	}

    /**
     * 获取联系电话
     *
     * @return 联系电话
     */
	public String getTel() {
		return get("tel");
	}

    /**
     * 设置联系电话
     *
     * @param tel 联系电话
     */
	public void setTel(String tel) {
		set("tel", tel);
	}

    /**
     * 获取相符明细数
     *
     * @return 相符明细数
     */
	public Integer getMatch_detail() {
		return get("match_detail");
	}

    /**
     * 设置相符明细数
     *
     * @param match_detail 相符明细数
     */
	public void setMatch_detail(Integer match_detail) {
		set("match_detail", match_detail);
	}

    /**
     * 获取不符明细数
     *
     * @return 不符明细数
     */
	public Integer getUnmatch_detail() {
		return get("unmatch_detail");
	}

    /**
     * 设置不符明细数
     *
     * @param unmatch_detail 不符明细数
     */
	public void setUnmatch_detail(Integer unmatch_detail) {
		set("unmatch_detail", unmatch_detail);
	}

    /**
     * 获取归档状态
     *
     * @return 归档状态
     */
	public Boolean getArchiving_status() {
	    return getBoolean("archiving_status");
	}

    /**
     * 设置归档状态
     *
     * @param archiving_status 归档状态
     */
	public void setArchiving_status(Boolean archiving_status) {
		set("archiving_status", archiving_status);
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
	 * 获取租户id
	 *
	 * @return 租户id.ID
	 */
	public String getYtenant() {
		return get("ytenant");
	}

	/**
	 * 设置租户id
	 *
	 * @param ytenant 租户id.ID
	 */
	public void setYtenant(String ytenant) {
		set("ytenant", ytenant);
	}

	/**
	 * 获取电子归档服务器地址
	 *
	 * @return 电子归档服务器地址
	 */
	public String getRemotepath() {
		return get("remotepath");
	}

	/**
	 * 设置电子归档服务器地址
	 *
	 * @param remotepath 电子归档服务器地址
	 */
	public void setRemotepath(String remotepath) {
		set("remotepath", remotepath);
	}

	/**
	 * 获取电子回单文件大小
	 *
	 * @return 电子回单文件大小
	 */
	public Integer getFilesize() {
		return get("filesize");
	}

	/**
	 * 设置电子回单文件大小
	 *
	 * @param filesize 电子回单文件大小
	 */
	public void setFilesize(Integer filesize) {
		set("filesize", filesize);
	}

	/**
	 * 获取回单下载信息
	 *
	 * @return 回单匹配关联id
	 */
	public String getBill_extend() {
		return get("bill_extend");
	}

	/**
	 * 设置回单下载信息
	 *
	 * @param bill_extend 回单匹配关联id
	 */
	public void setBill_extend(String bill_extend) {
		set("bill_extend", bill_extend);
	}

	/**
	 * 获取对账单文件id
	 *
	 * @return 对账单文件id
	 */
	public String getStatementfileid() {
		return get("statementfileid");
	}

	/**
	 * 设置对账单文件id
	 *
	 * @param statementfileid 对账单文件id
	 */
	public void setStatementfileid(String statementfileid) {
		set("statementfileid", statementfileid);
	}

	/**
	 * 获取授权使用组织
	 *
	 * @return 授权使用组织.ID
	 */
	public String getAuthoruseaccentity() {
		return get("authoruseaccentity");
	}

	/**
	 * 设置授权使用组织
	 *
	 * @param authoruseaccentity 授权使用组织.ID
	 */
	public void setAuthoruseaccentity(String authoruseaccentity) {
		set("authoruseaccentity", authoruseaccentity);
	}

	/**
	 * 获取银行类别
	 *
	 * @return 银行类别.ID
	 */
	public String getBanktype() {
		return get("banktype");
	}

	/**
	 * 设置银行类别
	 *
	 * @param banktype 银行类别.ID
	 */
	public void setBanktype(String banktype) {
		set("banktype", banktype);
	}

	/**
	 * 获取电子对账单编号
	 *
	 * @return 电子对账单编号
	 */
	public String getStatementno() {
		return get("statementno");
	}

	/**
	 * 设置电子对账单编号
	 *
	 * @param statementno 电子对账单编号
	 */
	public void setStatementno(String statementno) {
		set("statementno", statementno);
	}

	/**
	 * 获取账单收入总金额
	 *
	 * @return 账单收入总金额
	 */
	public java.math.BigDecimal getTotalrecamount() {
		return get("totalrecamount");
	}

	/**
	 * 设置账单收入总金额
	 *
	 * @param totalrecamount 账单收入总金额
	 */
	public void setTotalrecamount(java.math.BigDecimal totalrecamount) {
		set("totalrecamount", totalrecamount);
	}

	/**
	 * 获取数据来源
	 *
	 * @return 数据来源
	 */
	public Short getInputtype() {
		return getShort("inputtype");
	}

	/**
	 * 设置数据来源
	 *
	 * @param inputtype 数据来源
	 */
	public void setInputtype(Short inputtype) {
		set("inputtype", inputtype);
	}

	/**
	 * 获取备注
	 *
	 * @return 备注
	 */
	public String getRemark() {
		return get("remark");
	}

	/**
	 * 设置备注
	 *
	 * @param remark 备注
	 */
	public void setRemark(String remark) {
		set("remark", remark);
	}

	/**
	 * 获取全局对账单标识码
	 *
	 * @return 全局对账单标识码
	 */
	public String getUniqueno() {
		return get("uniqueno");
	}

	/**
	 * 设置全局对账单标识码
	 *
	 * @param uniqueno 全局对账单标识码
	 */
	public void setUniqueno(String uniqueno) {
		set("uniqueno", uniqueno);
	}

}
