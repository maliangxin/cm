package com.yonyoucloud.fi.cmp.billclaim;

import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import org.imeta.orm.base.BizObject;

/**
 * 业务凭据关联信息实体
 *
 * @author u
 * @version 1.0
 */
public class BillClaimBusVoucherInfo extends BizObject implements ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.billclaim.BillClaimBusVoucherInfo";

    /**
     * 获取业务单元
     *
     * @return 业务单元.ID
     */
	public String getAccentity() {
		return get("accentity");
	}

    /**
     * 设置业务单元
     *
     * @param accentity 业务单元.ID
     */
	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}

    /**
     * 获取银行对账单
     *
     * @return 银行对账单.ID
     */
	public Long getBankbill() {
		return get("bankbill");
	}

    /**
     * 设置银行对账单
     *
     * @param bankbill 银行对账单.ID
     */
	public void setBankbill(Long bankbill) {
		set("bankbill", bankbill);
	}

    /**
     * 获取单据编号
     *
     * @return 单据编号
     */
	public String getBillcode() {
		return get("billcode");
	}

    /**
     * 设置单据编号
     *
     * @param billcode 单据编号
     */
	public void setBillcode(String billcode) {
		set("billcode", billcode);
	}

    /**
     * 获取单据日期
     *
     * @return 单据日期
     */
	public java.util.Date getBilldate() {
		return get("billdate");
	}

    /**
     * 设置单据日期
     *
     * @param billdate 单据日期
     */
	public void setBilldate(java.util.Date billdate) {
		set("billdate", billdate);
	}

    /**
     * 获取明细编码
     *
     * @return 明细编码
     */
	public String getBillitmecode() {
		return get("billitmecode");
	}

    /**
     * 设置明细编码
     *
     * @param billitmecode 明细编码
     */
	public void setBillitmecode(String billitmecode) {
		set("billitmecode", billitmecode);
	}

    /**
     * 获取凭据明细id
     *
     * @return 凭据明细id
     */
	public String getBillitmeid() {
		return get("billitmeid");
	}

    /**
     * 设置凭据明细id
     *
     * @param billitmeid 凭据明细id
     */
	public void setBillitmeid(String billitmeid) {
		set("billitmeid", billitmeid);
	}

    /**
     * 获取凭据主表id
     *
     * @return 凭据主表id
     */
	public String getBillmainid() {
		return get("billmainid");
	}

    /**
     * 设置凭据主表id
     *
     * @param billmainid 凭据主表id
     */
	public void setBillmainid(String billmainid) {
		set("billmainid", billmainid);
	}

    /**
     * 获取凭据类型
     *
     * @return 凭据类型
     */
	public Short getBilltype() {
	    return getShort("billtype");
	}

    /**
     * 设置凭据类型
     *
     * @param billtype 凭据类型
     */
	public void setBilltype(Short billtype) {
		set("billtype", billtype);
	}

	/**
	 * 获取收款模式
	 *
	 * @return 收款模式
	 */
	public Short getPaymentModel() {
		return getShort("paymentModel");
	}

	/**
	 * 设置收款模式
	 *
	 * @param paymentModel 收款模式
	 */
	public void setPaymentModel(Short paymentModel) {
		set("paymentModel", paymentModel);
	}
    /**
     * 获取本次认领金额
     *
     * @return 本次认领金额
     */
	public java.math.BigDecimal getClaimamount() {
		return get("claimamount");
	}

    /**
     * 设置本次认领金额
     *
     * @param claimamount 本次认领金额
     */
	public void setClaimamount(java.math.BigDecimal claimamount) {
		set("claimamount", claimamount);
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
     * 获取应收金额
     *
     * @return 应收金额
     */
	public java.math.BigDecimal getNeedamount() {
		return get("needamount");
	}

    /**
     * 设置应收金额
     *
     * @param needamount 应收金额
     */
	public void setNeedamount(java.math.BigDecimal needamount) {
		set("needamount", needamount);
	}

    /**
     * 获取期间名称
     *
     * @return 期间名称
     */
	public String getPeridname() {
		return get("peridname");
	}

    /**
     * 设置期间名称
     *
     * @param peridname 期间名称
     */
	public void setPeridname(String peridname) {
		set("peridname", peridname);
	}

    /**
     * 获取计划日期
     *
     * @return 计划日期
     */
	public java.util.Date getPlandate() {
		return get("plandate");
	}

    /**
     * 设置计划日期
     *
     * @param plandate 计划日期
     */
	public void setPlandate(java.util.Date plandate) {
		set("plandate", plandate);
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
     * 获取已收金额
     *
     * @return 已收金额
     */
	public java.math.BigDecimal getReceivedamount() {
		return get("receivedamount");
	}

    /**
     * 设置已收金额
     *
     * @param receivedamount 已收金额
     */
	public void setReceivedamount(java.math.BigDecimal receivedamount) {
		set("receivedamount", receivedamount);
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
     * 获取总金额
     *
     * @return 总金额
     */
	public java.math.BigDecimal getTotalamount() {
		return get("totalamount");
	}

    /**
     * 设置总金额
     *
     * @param totalamount 总金额
     */
	public void setTotalamount(java.math.BigDecimal totalamount) {
		set("totalamount", totalamount);
	}

    /**
     * 获取未收金额
     *
     * @return 未收金额
     */
	public java.math.BigDecimal getUncollectedamount() {
		return get("uncollectedamount");
	}

    /**
     * 设置未收金额
     *
     * @param uncollectedamount 未收金额
     */
	public void setUncollectedamount(java.math.BigDecimal uncollectedamount) {
		set("uncollectedamount", uncollectedamount);
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

	@Override
	public String getYTenant() {
		return get("ytenant");
	}

	@Override
	public void setYTenant(String ytenant) {
		set("ytenant", ytenant);
	}
}
