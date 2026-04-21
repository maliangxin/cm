package com.yonyoucloud.fi.cmp.fundexpense;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IBackWrite;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 税费信息子表实体
 *
 * @author u
 * @version 1.0
 */
public class Taxation_detail_b extends BizObject implements IBackWrite, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.fundexpense.Taxation_detail_b";

	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}

    /**
     * 获取分组任务KEY
     *
     * @return 分组任务KEY
     */
	public String getGroupTaskKey() {
		return get("groupTaskKey");
	}

    /**
     * 设置分组任务KEY
     *
     * @param groupTaskKey 分组任务KEY
     */
	public void setGroupTaskKey(String groupTaskKey) {
		set("groupTaskKey", groupTaskKey);
	}

    /**
     * 获取发票校验码
     *
     * @return 发票校验码
     */
	public String getInvoicecheckcode() {
		return get("invoicecheckcode");
	}

    /**
     * 设置发票校验码
     *
     * @param invoicecheckcode 发票校验码
     */
	public void setInvoicecheckcode(String invoicecheckcode) {
		set("invoicecheckcode", invoicecheckcode);
	}

    /**
     * 获取发票号码
     *
     * @return 发票号码
     */
	public String getInvoicecode() {
		return get("invoicecode");
	}

    /**
     * 设置发票号码
     *
     * @param invoicecode 发票号码
     */
	public void setInvoicecode(String invoicecode) {
		set("invoicecode", invoicecode);
	}

    /**
     * 获取附件
     *
     * @return 附件
     */
	public String getInvoicefile() {
		return get("invoicefile");
	}

    /**
     * 设置附件
     *
     * @param invoicefile 附件
     */
	public void setInvoicefile(String invoicefile) {
		set("invoicefile", invoicefile);
	}

    /**
     * 获取发票代码
     *
     * @return 发票代码
     */
	public String getInvoicenumber() {
		return get("invoicenumber");
	}

    /**
     * 设置发票代码
     *
     * @param invoicenumber 发票代码
     */
	public void setInvoicenumber(String invoicenumber) {
		set("invoicenumber", invoicenumber);
	}

    /**
     * 获取开票日期
     *
     * @return 开票日期
     */
	public java.util.Date getInvoiceopendate() {
		return get("invoiceopendate");
	}

    /**
     * 设置开票日期
     *
     * @param invoiceopendate 开票日期
     */
	public void setInvoiceopendate(java.util.Date invoiceopendate) {
		set("invoiceopendate", invoiceopendate);
	}

    /**
     * 获取发票状态
     *
     * @return 发票状态
     */
	public String getInvoicestatus() {
		return get("invoicestatus");
	}

    /**
     * 设置发票状态
     *
     * @param invoicestatus 发票状态
     */
	public void setInvoicestatus(String invoicestatus) {
		set("invoicestatus", invoicestatus);
	}

    /**
     * 获取发票抬头
     *
     * @return 发票抬头
     */
	public String getInvoicetitle() {
		return get("invoicetitle");
	}

    /**
     * 设置发票抬头
     *
     * @param invoicetitle 发票抬头
     */
	public void setInvoicetitle(String invoicetitle) {
		set("invoicetitle", invoicetitle);
	}

    /**
     * 获取发票类型
     *
     * @return 发票类型
     */
	public String getInvoicetype() {
		return get("invoicetype");
	}

    /**
     * 设置发票类型
     *
     * @param invoicetype 发票类型
     */
	public void setInvoicetype(String invoicetype) {
		set("invoicetype", invoicetype);
	}

    /**
     * 获取行号
     *
     * @return 行号
     */
	public java.math.BigDecimal getLineno() {
		return get("lineno");
	}

    /**
     * 设置行号
     *
     * @param lineno 行号
     */
	public void setLineno(java.math.BigDecimal lineno) {
		set("lineno", lineno);
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
     * 获取生单规则编号
     *
     * @return 生单规则编号
     */
	public String getMakeRuleCode() {
		return get("makeRuleCode");
	}

    /**
     * 设置生单规则编号
     *
     * @param makeRuleCode 生单规则编号
     */
	public void setMakeRuleCode(String makeRuleCode) {
		set("makeRuleCode", makeRuleCode);
	}

    /**
     * 获取价税合计
     *
     * @return 价税合计
     */
	public java.math.BigDecimal getNatmoney() {
		return get("natmoney");
	}

    /**
     * 设置价税合计
     *
     * @param natmoney 价税合计
     */
	public void setNatmoney(java.math.BigDecimal natmoney) {
		set("natmoney", natmoney);
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
     * 获取销售方名称
     *
     * @return 销售方名称
     */
	public String getSalename() {
		return get("salename");
	}

    /**
     * 设置销售方名称
     *
     * @param salename 销售方名称
     */
	public void setSalename(String salename) {
		set("salename", salename);
	}

    /**
     * 获取上游单据类型
     *
     * @return 上游单据类型
     */
	public String getSource() {
		return get("source");
	}

    /**
     * 设置上游单据类型
     *
     * @param source 上游单据类型
     */
	public void setSource(String source) {
		set("source", source);
	}

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
	public java.util.Date getSourceMainPubts() {
		return get("sourceMainPubts");
	}

    /**
     * 设置时间戳
     *
     * @param sourceMainPubts 时间戳
     */
	public void setSourceMainPubts(java.util.Date sourceMainPubts) {
		set("sourceMainPubts", sourceMainPubts);
	}

    /**
     * 获取上游单据子表id
     *
     * @return 上游单据子表id
     */
	public Long getSourceautoid() {
		return get("sourceautoid");
	}

    /**
     * 设置上游单据子表id
     *
     * @param sourceautoid 上游单据子表id
     */
	public void setSourceautoid(Long sourceautoid) {
		set("sourceautoid", sourceautoid);
	}

    /**
     * 获取上游单据主表id
     *
     * @return 上游单据主表id
     */
	public Long getSourceid() {
		return get("sourceid");
	}

    /**
     * 设置上游单据主表id
     *
     * @param sourceid 上游单据主表id
     */
	public void setSourceid(Long sourceid) {
		set("sourceid", sourceid);
	}

    /**
     * 获取税额
     *
     * @return 税额
     */
	public java.math.BigDecimal getTaxmoney() {
		return get("taxmoney");
	}

    /**
     * 设置税额
     *
     * @param taxmoney 税额
     */
	public void setTaxmoney(java.math.BigDecimal taxmoney) {
		set("taxmoney", taxmoney);
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
     * 获取累计报账价税合计
     *
     * @return 累计报账价税合计
     */
	public java.math.BigDecimal getTotalpostnatmoney() {
		return get("totalpostnatmoney");
	}

    /**
     * 设置累计报账价税合计
     *
     * @param totalpostnatmoney 累计报账价税合计
     */
	public void setTotalpostnatmoney(java.math.BigDecimal totalpostnatmoney) {
		set("totalpostnatmoney", totalpostnatmoney);
	}

    /**
     * 获取上游单据号
     *
     * @return 上游单据号
     */
	public String getUpcode() {
		return get("upcode");
	}

    /**
     * 设置上游单据号
     *
     * @param upcode 上游单据号
     */
	public void setUpcode(String upcode) {
		set("upcode", upcode);
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

}
