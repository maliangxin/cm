package com.yonyoucloud.fi.cmp.batchtransferaccount;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IPrintCount;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 批量同名账户划转实体
 *
 * @author u
 * @version 1.0
 */
public class BatchTransferAccount extends BizObject implements IAuditInfo, ICurrency, IApprovalFlow, IPrintCount, IYTenant, AccentityRawInterface {
	/**
	 * 实体全称
	 */
	public static final String ENTITY_NAME = "cmp.batchtransferaccount.BatchTransferAccount";
	// 业务对象编码
	public static final String BUSI_OBJ_CODE = "ctm-cmp.cmp_batchtransferaccount";
	private String code; // 单据编号
	private String accentity; // 资金组织
	private String accentityRaw; // 核算会计主体
	private String billDate; // 单据日期
	private String tradeType; // 交易类型
	private String mark; // 备注
	private String srcitem; // 事项来源
	private String settlestatus; // 结算状态
	private String directionType; // 收付类型
	private String generateMethod; // 单据生成方式
	private String bizObjType; // 业务对象类型
	private String billtypeId; // 单据类型
	private Short isSupplementary; // 是否结算补单
	private String sourceSystemId; // 上游业务系统
	private String sourceSystemType; // 上游业务系统枚举
	private String sourceBillTypeId; // 上游业务单据类型
	private String sourceBillType; // 上游业务单据类型枚举
	private String sourceTradeTypeId; // 上游交易类型
	private String sourceId; // 上游业务单据id
	private String srcBillCode; // 上游业务单据编号
	private Date settleDate; // 结算日期
	private Short transferCount; // 转账笔数
	private BigDecimal transferSumAmount; // 转账金额合计
	private BigDecimal transferSumNamount; // 转账金额合计本币
	private BigDecimal transferSucSumAmount; // 转账成功金额合计
	private BigDecimal transferSucSumNamount; // 转账成功金额合计本币
	private BigDecimal sumOutBrokerage; // 转出手续费合计
	private BigDecimal sumNatOutBrokerage; // 转出手续费合计本币
	private BigDecimal sumInBrokerage; // 转入手续费合计
	private BigDecimal sumNatInBrokerage; // 转入手续费合计本币
	private String currency; // 币种
	private String natCurrency; // 本币币种
	private BigDecimal exchRate; // 汇率
    private Short exchRateOps; // 汇率折算方式
	private String exchangeRateType; // 汇率类型
	private Short virtualBank; // 三方转账类型
	private String characterDef; // 批量同名账户划转特征
	private String voucherId; // 凭证ID
	private String voucherNo; // 凭证号
	private String voucherPeriod; // 凭证期间
	private String voucherstatus; // 凭证状态
	private String voucherMessage; // 过账失败信息
	private Long fiEventDataVersion; // 单据过账版本号
	private Long voucherVersion; // 事项会计过账版本号
    private Long creatorId; // 创建人id




	public java.util.List<BatchTransferAccount_b> BatchTransferAccount_b() {
		return getBizObjects("BatchTransferAccount_b", BatchTransferAccount_b.class);
	}


	public void setBatchTransferAccount_b(java.util.List<BatchTransferAccount_b> BatchTransferAccount_b) {
		setBizObjects("BatchTransferAccount_b", BatchTransferAccount_b);
	}

	// Getters and Setters
	public String getCode() {
		return get("code");
	}

	public void setCode(String code) {
		set("code", code);
	}

	public String getAccentity() {
		return get("accentity");
	}

	public void setAccentity(String accentity) {
		set("accentity" , accentity);
	}

	public String getAccentityRaw() {
		return get("accentityRaw");
	}

	public void setAccentityRaw(String accentityRaw) {
		set("accentityRaw" , accentityRaw);
	}

	public String getBillDate() {
		return get("billDate");
	}

	public void setBillDate(String billDate) {
		set("billDate" , billDate);
	}

	public String getTradeType() {
		return get("tradeType");
	}

	public void setTradeType(String tradeType) {
		set("tradeType" , tradeType);
	}

	public String getMark() {
		return get("mark");
	}

	public void setMark(String memo) {
		set("mark" , mark);
	}

	public String getSrcitem() {
		return get("srcitem");
	}

	public void setSrcitem(String srcitem) {
		set("srcitem" , srcitem);
	}

	public String getSettlestatus() {
		return get("settlestatus");
	}

	public void setSettlestatus(String settlestatus) {
		set("settlestatus" , settlestatus);
	}

	public String getDirectionType() {
		return get("directionType");
	}

	public void setDirectionType(String directionType) {
		set("directionType" , directionType);
	}

	public String getGenerateMethod() {
		return get("generateMethod");
	}

	public void setGenerateMethod(String generateMethod) {
		set("generateMethod" , generateMethod);
	}

	public String getBizObjType() {
		return get("bizObjType");
	}

	public void setBizObjType(String bizObjType) {
		set("bizObjType" , bizObjType);
	}

	public String getBillTypeId() {
		return get("billTypeId");
	}

	public void setBillTypeId(String billTypeId) {
		set("billTypeId" , billTypeId);
	}

	public Short getIsSupplementary() {
		return getShort("isSupplementary");
	}

	public void setIsSupplementary(Short isSupplementary) {
		set("isSupplementary" , isSupplementary);
	}

	public String getSourceSystemId() {
		return get("sourceSystemId");
	}

	public void setSourceSystemId(String sourceSystemId) {
		set("sourceSystemId" , sourceSystemId);
	}

	public String getSourceSystemType() {
		return get("sourceSystemType");
	}

	public void setSourceSystemType(String sourceSystemType) {
		set("sourceSystemType" , sourceSystemType);
	}

	public String getSourceBillTypeId() {
		return get("sourceBillTypeId");
	}

	public void setSourceBillTypeId(String sourceBillTypeId) {
		set("sourceBillTypeId" , sourceBillTypeId);
	}

	public String getSourceBillType() {
		return get("sourceBillType");
	}

	public void setSourceBillType(String sourceBillType) {
		set("sourceBillType" , sourceBillType);
	}

	public String getSourceTradeTypeId() {
		return get("sourceTradeTypeId");
	}

	public void setSourceTradeTypeId(String sourceTradeTypeId) {
		set("sourceTradeTypeId" , sourceTradeTypeId);
	}

	public String getSourceId() {
		return get("sourceId");
	}

	public void setSourceId(String sourceId) {
		set("sourceId" , sourceId);
	}

	public String getSrcBillCode() {
		return get("srcBillCode");
	}

	public void setSrcBillCode(String srcBillCode) {
		set("srcBillCode" , srcBillCode);
	}


	public Date getSettleDate() {
		return get("settleDate");
	}

	public void setSettleDate(Date settleDate) {
		set("settleDate" , settleDate);
	}

	public Short getTransferCount() {
		return getShort("transferCount");
	}

	public void setTransferCount(Short transferCount) {
		set("transferCount" , transferCount);
	}

	public BigDecimal getTransferSumAmount() {
		return get("transferSumAmount");
	}

	public void setTransferSumAmount(BigDecimal transferSumAmount) {
		set("transferSumAmount" , transferSumAmount);
	}

	public BigDecimal getTransferSumNamount() {
		return get("transferSumNamount");
	}

	public void setTransferSumNamount(BigDecimal transferSumNamount) {
		set("transferSumNamount" , transferSumNamount);
	}

	public BigDecimal getTransferSucSumAmount() {
		return get("transferSucSumAmount");
	}

	public void setTransferSucSumAmount(BigDecimal transferSucSumAmount) {
		set("transferSucSumAmount" , transferSucSumAmount);
	}

	public BigDecimal getTransferSucSumNamount() {
		return get("transferSucSumNamount");
	}

	public void setTransferSucSumNamount(BigDecimal transferSucSumNamount) {
		set("transferSucSumNamount", transferSucSumNamount);
	}

	public BigDecimal getSumOutBrokerage() {
		return get("sumOutBrokerage");
	}

	public void setSumOutBrokerage(BigDecimal sumOutBrokerage) {
		set("sumOutBrokerage" , sumOutBrokerage);
	}

	public BigDecimal getSumNatOutBrokerage() {
		return get("sumNatOutBrokerage");
	}

	public void setSumNatOutBrokerage(BigDecimal sumNatOutBrokerage) {
		set("sumNatOutBrokerage" , sumNatOutBrokerage);
	}

	public BigDecimal getSumInBrokerage() {
		return get("sumInBrokerage");
	}

	public void setSumInBrokerage(BigDecimal sumInBrokerage) {
		set("sumInBrokerage" , sumInBrokerage);
	}

	public BigDecimal getSumNatInBrokerage() {
		return get("sumNatInBrokerage");
	}

	public void setSumNatInBrokerage(BigDecimal sumNatInBrokerage) {
		set("sumNatInBrokerage" , sumNatInBrokerage);
	}

	public String getCurrency() {
		return get("currency");
	}

	public void setCurrency(String currency) {
		set("currency" , currency);
	}

	@Override
	public String getNatCurrency() {
		return get("natCurrency");
	}

	@Override
	public void setNatCurrency(String natCurrency) {
		set("natCurrency" , natCurrency);
	}


	public BigDecimal getExchRate() {
		return get("exchRate");
	}

	public void setExchRate(BigDecimal exchRate) {
		set("exchRate" , exchRate);
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

	public String getExchangeRateType() {
		return get("exchangeRateType");
	}

	public void setExchangeRateType(String exchangeRateType) {
		set("exchangeRateType" , exchangeRateType);
	}

	public Short getVirtualBank() {
		return getShort("virtualBank");
	}

	public void setVirtualBank(Short virtualBank) {
		set("virtualBank" , virtualBank);
	}

	public String getCharacterDef() {
		return get("characterDef");
	}

	public void setCharacterDef(String characterDef) {
		set("characterDef" , characterDef);
	}


	/**
	 * 获取租户id
	 *
	 * @return get(" 租户id
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

	@Override
	public Boolean getIsWfControlled() {
		return getBoolean("isWfControlled");
	}

	@Override
	public void setIsWfControlled(Boolean isWfControlled) {
		set("isWfControlled", isWfControlled);
	}

	@Override
	public Short getVerifystate() {
		return getShort("verifystate");
	}

	@Override
	public void setVerifystate(Short verifystate) {
		set("verifystate", verifystate);
	}

	@Override
	public Short getReturncount() {
		return getShort("returncount");
	}

	@Override
	public void setReturncount(Short returncount) {
		set("returncount", returncount);
	}


	@Override
	public java.util.Date getCreateTime() {
		return get("createTime");
	}
	@Override
	public void setCreateTime(java.util.Date createTime) {
		set("createTime", createTime);
	}

	@Override
	public java.util.Date getCreateDate() {
		return get("createDate");
	}

	@Override
	public void setCreateDate(java.util.Date createDate) {
		set("createDate", createDate);
	}

	@Override
	public java.util.Date getModifyTime() {
		return get("modifyTime");
	}

	@Override
	public void setModifyTime(java.util.Date modifyTime) {
		set("modifyTime", modifyTime);
	}

	@Override
	public java.util.Date getModifyDate() {
		return get("modifyDate");
	}

	@Override
	public void setModifyDate(java.util.Date modifyDate) {
		set("modifyDate", modifyDate);
	}

	@Override
	public String getCreator() {
		return get("creator");
	}

	@Override
	public void setCreator(String creator) {
		set("creator", creator);
	}

    /**
     * 获取创建人
     *
     * @return 创建人
     */
    public Long getCreatorId() {
        return get("creatorId");
    }

    /**
     * 设置创建人
     *
     * @param creatorId 创建人
     */
    public void setCreatorId(Long creatorId) {
        set("creatorId", creatorId);
    }

	@Override
	public String getModifier() {
		return get("modifier");
	}

	@Override
	public void setModifier(String modifier) {
		set("modifier", modifier);
	}

	@Override
	public Integer getPrintCount() {
		return get("printCount");
	}

	@Override
	public void setPrintCount(Integer printCount) {
		set("printCount", printCount);
	}

	public String getVoucherId() {
		return get("voucherId");
	}

	public void setVoucherId(String voucherId) {
		set("voucherId", voucherId);
	}

	public String getVoucherNo() {
		return get("voucherNo");
	}

	public void setVoucherNo(String voucherNo) {
		set("voucherNo", voucherNo);
	}

	public String getVoucherPeriod() {
		return get("voucherPeriod");
	}

	public void setVoucherPeriod(String voucherPeriod) {
		set("voucherPeriod", voucherPeriod);
	}

	public Short getVoucherstatus() {
		return getShort("voucherstatus");
	}

	public void setVoucherstatus(Short voucherstatus) {
		set("voucherstatus", voucherstatus);
	}

	public String getVoucherMessage() {
		return get("voucherMessage");
	}

	public void setVoucherMessage(String voucherMessage) {
		set("voucherMessage", voucherMessage);
	}

	public Long getFiEventDataVersion() {
		return get("fiEventDataVersion");
	}

	public void setFiEventDataVersion(Long fiEventDataVersion) {
		set("fiEventDataVersion", fiEventDataVersion);
	}

	public Long getVoucherVersion() {
		return get("voucherVersion");
	}

	public void setVoucherVersion(Long voucherVersion) {
		set("voucherVersion", voucherVersion);
	}
}
