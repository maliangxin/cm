package com.yonyoucloud.fi.cmp.inwardremittance;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import org.imeta.orm.base.BizObject;

/**
 * 汇入汇款实体
 *
 * @author u
 * @version 1.0
 */
public class InwardRemittance extends BizObject implements ICurrency, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.inwardremittance.InwardRemittance";



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
     * 获取交易金额
     *
     * @return 交易金额
     */
	public java.math.BigDecimal getAmount() {
		return get("amount");
	}

    /**
     * 设置交易金额
     *
     * @param amount 交易金额
     */
	public void setAmount(java.math.BigDecimal amount) {
		set("amount", amount);
	}

    /**
     * 获取收款银行账号
     *
     * @return 收款银行账号.ID
     */
	public String getBankaccount() {
		return get("bankaccount");
	}

    /**
     * 设置收款银行账号
     *
     * @param bankaccount 收款银行账号.ID
     */
	public void setBankaccount(String bankaccount) {
		set("bankaccount", bankaccount);
	}

    /**
     * 获取开户行名称
     *
     * @return 开户行名称
     */
	public String getBankname() {
		return get("bankname");
	}

    /**
     * 设置开户行名称
     *
     * @param bankname 开户行名称
     */
	public void setBankname(String bankname) {
		set("bankname", bankname);
	}

    /**
     * 获取编码
     *
     * @return 编码
     */
	public String getCode() {
		return get("code");
	}

    /**
     * 设置编码
     *
     * @param code 编码
     */
	public void setCode(String code) {
		set("code", code);
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
     * 获取交易币种
     *
     * @return 交易币种.ID
     */
	public String getCurrency() {
		return get("currency");
	}

    /**
     * 设置交易币种
     *
     * @param currency 交易币种.ID
     */
	public void setCurrency(String currency) {
		set("currency", currency);
	}

    /**
     * 获取汇率
     *
     * @return 汇率
     */
	public java.math.BigDecimal getExchRate() {
		return get("exchRate");
	}

    /**
     * 设置汇率
     *
     * @param exchRate 汇率
     */
	public void setExchRate(java.math.BigDecimal exchRate) {
		set("exchRate", exchRate);
	}

    /**
     * 获取汇率折算方式
     *
     * @return 汇率折算方式
     */
    public Short getExchRateOps() {
        return getShort("exchRateOps");
    }

    /**
     * 设置汇率折算方式
     *
     * @param exchRateOps 汇率折算方式
     */
    public void setExchRateOps(Short exchRateOps) {
        set("exchRateOps", exchRateOps);
    }

    /**
     * 获取国外扣手续费金额
     *
     * @return 国外扣手续费金额
     */
	public java.math.BigDecimal getForeignfeeamount() {
		return get("foreignfeeamount");
	}

    /**
     * 设置国外扣手续费金额
     *
     * @param foreignfeeamount 国外扣手续费金额
     */
	public void setForeignfeeamount(java.math.BigDecimal foreignfeeamount) {
		set("foreignfeeamount", foreignfeeamount);
	}

    /**
     * 获取国外扣手续费币种
     *
     * @return 国外扣手续费币种.ID
     */
	public String getForeignfeecurrency() {
		return get("foreignfeecurrency");
	}

    /**
     * 设置国外扣手续费币种
     *
     * @param foreignfeecurrency 国外扣手续费币种.ID
     */
	public void setForeignfeecurrency(String foreignfeecurrency) {
		set("foreignfeecurrency", foreignfeecurrency);
	}

    /**
     * 获取国外扣电报费金额
     *
     * @return 国外扣电报费金额
     */
	public java.math.BigDecimal getForeigntelegramfee() {
		return get("foreigntelegramfee");
	}

    /**
     * 设置国外扣电报费金额
     *
     * @param foreigntelegramfee 国外扣电报费金额
     */
	public void setForeigntelegramfee(java.math.BigDecimal foreigntelegramfee) {
		set("foreigntelegramfee", foreigntelegramfee);
	}

    /**
     * 获取国外扣电报费币种
     *
     * @return 国外扣电报费币种.ID
     */
	public String getForeigntelegramfeecurrency() {
		return get("foreigntelegramfeecurrency");
	}

    /**
     * 设置国外扣电报费币种
     *
     * @param foreigntelegramfeecurrency 国外扣电报费币种.ID
     */
	public void setForeigntelegramfeecurrency(String foreigntelegramfeecurrency) {
		set("foreigntelegramfeecurrency", foreigntelegramfeecurrency);
	}

    /**
     * 获取汇入汇款编号
     *
     * @return 汇入汇款编号
     */
	public String getInwardremittancecode() {
		return get("inwardremittancecode");
	}

    /**
     * 设置汇入汇款编号
     *
     * @param inwardremittancecode 汇入汇款编号
     */
	public void setInwardremittancecode(String inwardremittancecode) {
		set("inwardremittancecode", inwardremittancecode);
	}

    /**
     * 获取汇入状态
     *
     * @return 汇入状态
     */
	public Short getInwardstatus() {
	    return getShort("inwardstatus");
	}

    /**
     * 设置汇入状态
     *
     * @param inwardstatus 汇入状态
     */
	public void setInwardstatus(Short inwardstatus) {
		set("inwardstatus", inwardstatus);
	}

    /**
     * 获取国内银行扣费金额
     *
     * @return 国内银行扣费金额
     */
	public java.math.BigDecimal getLocalfeeamount() {
		return get("localfeeamount");
	}

    /**
     * 设置国内银行扣费金额
     *
     * @param localfeeamount 国内银行扣费金额
     */
	public void setLocalfeeamount(java.math.BigDecimal localfeeamount) {
		set("localfeeamount", localfeeamount);
	}

    /**
     * 获取国内银行扣费币种
     *
     * @return 国内银行扣费币种.ID
     */
	public String getLocalfeecurrency() {
		return get("localfeecurrency");
	}

    /**
     * 设置国内银行扣费币种
     *
     * @param localfeecurrency 国内银行扣费币种.ID
     */
	public void setLocalfeecurrency(String localfeecurrency) {
		set("localfeecurrency", localfeecurrency);
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
     * 获取本币
     *
     * @return 本币.ID
     */
	public String getNatCurrency() {
		return get("natCurrency");
	}

    /**
     * 设置本币
     *
     * @param natCurrency 本币.ID
     */
	public void setNatCurrency(String natCurrency) {
		set("natCurrency", natCurrency);
	}

    /**
     * 获取附言
     *
     * @return 附言
     */
	public String getPostscript() {
		return get("postscript");
	}

    /**
     * 设置附言
     *
     * @param postscript 附言
     */
	public void setPostscript(String postscript) {
		set("postscript", postscript);
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
     * 获取实际入账账号
     *
     * @return 实际入账账号
     */
	public String getRealaccount() {
		return get("realaccount");
	}

    /**
     * 设置实际入账账号
     *
     * @param realaccount 实际入账账号
     */
	public void setRealaccount(String realaccount) {
		set("realaccount", realaccount);
	}

    /**
     * 获取实际入账金额
     *
     * @return 实际入账金额
     */
	public java.math.BigDecimal getRealamount() {
		return get("realamount");
	}

    /**
     * 设置实际入账金额
     *
     * @param realamount 实际入账金额
     */
	public void setRealamount(java.math.BigDecimal realamount) {
		set("realamount", realamount);
	}

    /**
     * 获取实际入账日期
     *
     * @return 实际入账日期
     */
	public java.util.Date getRealdate() {
		return get("realdate");
	}

    /**
     * 设置实际入账日期
     *
     * @param realdate 实际入账日期
     */
	public void setRealdate(java.util.Date realdate) {
		set("realdate", realdate);
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
     * 获取汇款人名称
     *
     * @return 汇款人名称
     */
	public String getRemitter() {
		return get("remitter");
	}

    /**
     * 设置汇款人名称
     *
     * @param remitter 汇款人名称
     */
	public void setRemitter(String remitter) {
		set("remitter", remitter);
	}

    /**
     * 获取汇款人账号
     *
     * @return 汇款人账号
     */
	public String getRemitteraccount() {
		return get("remitteraccount");
	}

    /**
     * 设置汇款人账号
     *
     * @param remitteraccount 汇款人账号
     */
	public void setRemitteraccount(String remitteraccount) {
		set("remitteraccount", remitteraccount);
	}

    /**
     * 获取汇款人地址
     *
     * @return 汇款人地址
     */
	public String getRemitteraddr() {
		return get("remitteraddr");
	}

    /**
     * 设置汇款人地址
     *
     * @param remitteraddr 汇款人地址
     */
	public void setRemitteraddr(String remitteraddr) {
		set("remitteraddr", remitteraddr);
	}

    /**
     * 获取汇出行行名
     *
     * @return 汇出行行名
     */
	public String getRemittingbank() {
		return get("remittingbank");
	}

    /**
     * 设置汇出行行名
     *
     * @param remittingbank 汇出行行名
     */
	public void setRemittingbank(String remittingbank) {
		set("remittingbank", remittingbank);
	}

    /**
     * 获取汇出行地址
     *
     * @return 汇出行地址
     */
	public String getRemittingbankaddr() {
		return get("remittingbankaddr");
	}

    /**
     * 设置汇出行地址
     *
     * @param remittingbankaddr 汇出行地址
     */
	public void setRemittingbankaddr(String remittingbankaddr) {
		set("remittingbankaddr", remittingbankaddr);
	}

    /**
     * 获取单据状态
     *
     * @return 单据状态
     */
	public Short getStatus() {
	    return getShort("status");
	}

    /**
     * 设置单据状态
     *
     * @param status 单据状态
     */
	public void setStatus(Short status) {
		set("status", status);
	}

    /**
     * 获取汇出行SWIFT码
     *
     * @return 汇出行SWIFT码
     */
	public String getSwiftcode() {
		return get("swiftcode");
	}

    /**
     * 设置汇出行SWIFT码
     *
     * @param swiftcode 汇出行SWIFT码
     */
	public void setSwiftcode(String swiftcode) {
		set("swiftcode", swiftcode);
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
     * 获取模板id
     *
     * @return 模板id
     */
	public Long getTplid() {
		return get("tplid");
	}

    /**
     * 设置模板id
     *
     * @param tplid 模板id
     */
	public void setTplid(Long tplid) {
		set("tplid", tplid);
	}

    /**
     * 获取交易日期
     *
     * @return 交易日期
     */
	public java.util.Date getTransferdate() {
		return get("transferdate");
	}

    /**
     * 设置交易日期
     *
     * @param transferdate 交易日期
     */
	public void setTransferdate(java.util.Date transferdate) {
		set("transferdate", transferdate);
	}

    /**
     * 获取单据日期
     *
     * @return 单据日期
     */
	public java.util.Date getVouchdate() {
		return get("vouchdate");
	}

    /**
     * 设置单据日期
     *
     * @param vouchdate 单据日期
     */
	public void setVouchdate(java.util.Date vouchdate) {
		set("vouchdate", vouchdate);
	}

    /**
     * 获取租户id
     *
     * @return 租户id
     */
	public String getYtenant() {
		return get("ytenant");
	}

    /**
     * 设置租户id
     *
     * @param ytenant 租户id
     */
	public void setYtenant(String ytenant) {
		set("ytenant", ytenant);
	}

    /**
     * 获取汇入汇款子表
     *
     * @return 汇入汇款子表
     */
	public InwardRemittance_b InwardRemittance_b() {
		return getBizObject("InwardRemittance_b", InwardRemittance_b.class);
	}

    /**
     * 设置汇入汇款子表
     *
     * @param InwardRemittance_b 汇入汇款子表
     */
	public void setInwardRemittance_b(InwardRemittance_b InwardRemittance_b) {
		setBizObject("InwardRemittance_b", InwardRemittance_b);
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
