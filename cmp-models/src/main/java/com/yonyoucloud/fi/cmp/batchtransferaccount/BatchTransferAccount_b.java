package com.yonyoucloud.fi.cmp.batchtransferaccount;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IBackWrite;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

import com.yonyoucloud.fi.cmp.common.annotation.ClassDescription;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 批量同名账户划转子表实体
 *
 * @author u
 * @version 1.0
 */
@ClassDescription(message = "批量同名账户划转子表", tableNames = {"stwb_apply_detail","stwb_settleapply_ta_b_assistant"})//@notranslate
public class BatchTransferAccount_b extends BizObject implements IBackWrite, ITenant, IYTenant {
	// 实体全称
	public static final String ENTITY_NAME = "cmp.batchtransferaccount.BatchTransferAccount_b";

	private Long mainid; // 主表id
	private String lineno; // 行号
	private String billDate; // 单据日期
	private String bizObjType; // 业务对象类型
	private String serialNumber; // 业务流水号
	private String sourceChildId; // 上游业务单据明细
	private Short supplementary; // 是否结算补单
	private String directionType; // 收付类型
	private String paySettlementMode; // 付款结算模式
	private String paySettleStatus; // 付款结算状态
	private Date paySettleSuccessDate; // 付款结算成功日期
	private Date paySettleSuccessDateTime; // 付款结算成功时间
	private String purpose; // 用途
	private String postScript; // 附言
	private Long paySettlemode; // 付款结算方式
	private String settleMethodProps; // 结算方式业务属性
	private Short settleMethodDirectLink; // 结算方式是否直联
	private String expectSettleDate; // 期望结算日期
	private String payBankAccountId; // 付款银行账号id/付款虚拟账号id
	private String payBankAccountNo; // 付款银行账号/付款虚拟账号
	private String payBankAccountName; // 付款方户名
	private String payBankId; // 付款方开户行id
	private String payBankName; // 付款方开户行名
	private String payBankTypeId; // 付款方银行类别
	private String payLineNumber; // 付款方开户联行号
	private String payCashAccount; // 付款现金账号
	private String noteDirect; // 票证方向 - 支票
	private Long noteId; // 票证号id
	private String noteno; // 票证号
	private Short directPay; // 是否直联
	private Long linkPayStrategyId; // 联动下拨策略
	private String oppType; // 对方类型
	private String oppId; // 对方Id
	private String oppFundPartnerTypeId; // 资金业务伙伴类型
	private String oppName; // 对方名称
	private String publicOrPrivate; // 对公对私
	private String oppAcctType; // 对方账户类型
	private String recBankAccountId; // 收款银行账户id/收款虚拟账号id
	private String recBankAccountNo; // 收款银行账号/收款虚拟账号
	private String recBankAccountName; // 收款方户名
	private String recBankId; // 收款方开户行Id
	private String recBankName; // 收款方开户行名
	private String recBankTypeId; // 收款方银行类别Id
	private String recLineNumber; // 收款方开户联行号
	private String currencyId; // 币种id
	private BigDecimal oriSum; // 转账金额
	private BigDecimal oriRemainAmount; // 待结算余额
	private BigDecimal oriTransitAmount; // 结算中金额
	private BigDecimal settleSucAmount; // 结算成功金额
	private BigDecimal settleStopPayAmount; // 结算止付金额
	private Short exchangePayment; // 是否换汇
	private String settleCurrencyId; // 结算币种
	private String settleRateTypeId; // 结算汇率类型
	private BigDecimal settleRate; // 预估结算汇率
    private Short settleRateOps; // 预估结算汇率折算方式
	private BigDecimal settleAmount; // 预估结算金额
	private String natCurrencyId; // 本币币种
	private String natRateTypeId; // 本币汇率类型
	private BigDecimal natRate; // 本币汇率
    private Short natRateOps; // 本币汇率折算方式
	private BigDecimal natSum; // 转账金额本币
	private Short associationStatusPay; // 付款是否关联
	private Long payBankBillId; // 付款银行流水ID/付款认领单ID
	private String paySmartCheckNo; // 付款财资统一对账码
	private Short reFund; // 是否退票
	private Date refundDateTime; // 退票时间
	private BigDecimal refundAmount; // 退票金额
	private String entryType; // 入账类型
	private Short pushStct; // 是否传结算中心
	private Short merageEnable; // 是否可合并
	private Short splitEnable; // 是否可拆分
	private Short settleMethodEditable; // 是否可修改结算方式
	private Short settleInfoEditable; // 是否可修改结算信息
	private Short journalRegisteable; // 是否需登日记账
	private Short vouchGenerateable; // 是否需生成凭证
	private Short csplEnable; // 是否需占资金计划
	private String riskType; // 风险类型
	private Short isFirstHandler; // 首笔已处理
	private Short isSettleAgain; // 再次结算
	private Short isOccupyBudget; // 占用预算
	private String voucherId; // 凭证ID
	private String voucherNo; // 凭证号
	private String voucherPeriod; // 凭证期间
	private String voucherstatus; // 凭证状态
	private String voucherMessage; // 过账失败信息
	private Long fiEventDataVersion; // 单据过账版本号
	private Long voucherVersion; // 事项会计过账版本号
	private String recSmartCheckNo; // 收款财资统一对账码
	private BigDecimal outBrokerage; // 转出手续费
	private BigDecimal natOutBrokerage; // 转出手续费本币
	private Short checkPurpose; // 支票用途
	private Long recSettlemode; // 收款结算方式
	private String recCashAccount; // 收款现金账户
	private String swiftCode; // 收款银行SWIFT码
	private BigDecimal inBrokerage; // 转入手续费
	private BigDecimal natInBrokerage; // 转入手续费本币
	private Long recBankBillId; // 收款银行流水ID/收款认领单ID
	private String deptId; // 部门
	private String projectId; // 项目
	private String recSettleStatus; // 收款结算状态
	private Date recSettleSuccessDate; // 收款结算成功日期
	private Date recSettleSuccessDateTime; // 收款结算成功时间
	private String failmessage; // 结算状态回写失败原因
	private String accentity;//资金组织

	// Getters and Setters
	public Long getMainid() {
        if (get("mainid") == null) {
            return 0L;
        }
		return Long.valueOf(get("mainid"));
	}

	public void setMainid(Long mainid) {
		set("mainid", mainid);
	}

	public BigDecimal getLineno() {
		Object lineno = get("lineno");
		if (lineno instanceof String) {
			return new BigDecimal(lineno.toString());
		}
		return get("lineno");
	}

	public void setLineno(BigDecimal lineno) {
		set("lineno", lineno);
	}

	public String getBillDate() {
		return get("billDate");
	}

	public void setBillDate(String billDate) {
		set("billDate", billDate);
	}

	public String getBizObjType() {
		return get("bizObjType");
	}

	public void setBizObjType(String bizObjType) {
		set("bizObjType", bizObjType);
	}

	public String getSerialNumber() {
		return get("serialNumber");
	}

	public void setSerialNumber(String serialNumber) {
		set("serialNumber", serialNumber);
	}

	public String getSourceChildId() {
		return get("sourceChildId");
	}

	public void setSourceChildId(String sourceChildId) {
		set("sourceChildId", sourceChildId);
	}

	public Short getSupplementary() {
		return getShort("supplementary");
	}

	public void setSupplementary(Short supplementary) {
		set("supplementary", supplementary);
	}

	public String getDirectionType() {
		return get("directionType");
	}

	public void setDirectionType(String directionType) {
		set("directionType", directionType);
	}

	public String getPaySettlementMode() {
		return get("paySettlementMode");
	}

	public void setPaySettlementMode(String paySettlementMode) {
		set("paySettlementMode", paySettlementMode);
	}

	public String getPaySettleStatus() {
		return get("paySettleStatus");
	}

	public void setPaySettleStatus(String paySettleStatus) {
		set("paySettleStatus", paySettleStatus);
	}

	public Date getPaySettleSuccessDate() {
		return get("paySettleSuccessDate");
	}

	public void setPaySettleSuccessDate(Date paySettleSuccessDate) {
		set("paySettleSuccessDate", paySettleSuccessDate);
	}

	public Date getPaySettleSuccessDateTime() {
		return get("paySettleSuccessDateTime");
	}

	public void setPaySettleSuccessDateTime(Date paySettleSuccessDateTime) {
		set("paySettleSuccessDateTime", paySettleSuccessDateTime);
	}

	public String getPurpose() {
		return get("purpose");
	}

	public void setPurpose(String purpose) {
		set("purpose", purpose);
	}

	public String getPostScript() {
		return get("postScript");
	}

	public void setPostScript(String postScript) {
		set("postScript", postScript);
	}

	public Long getPaySettlemode() {
		return get("paySettlemode");
	}

	public void setPaySettlemode(Long paySettlemode) {
		set("paySettlemode", paySettlemode);
	}

	public String getSettleMethodProps() {
		return get("settleMethodProps");
	}

	public void setSettleMethodProps(String settleMethodProps) {
		set("settleMethodProps", settleMethodProps);
	}

	public Short getSettleMethodDirectLink() {
		return getShort("settleMethodDirectLink");
	}

	public void setSettleMethodDirectLink(Short settleMethodDirectLink) {
		set("settleMethodDirectLink", settleMethodDirectLink);
	}

	public String getExpectSettleDate() {
		return get("expectSettleDate");
	}

	public void setExpectSettleDate(String expectSettleDate) {
		set("expectSettleDate", expectSettleDate);
	}

	public String getPayBankAccountId() {
		return get("payBankAccountId");
	}

	public void setPayBankAccountId(String payBankAccountId) {
		set("payBankAccountId", payBankAccountId);
	}

	public String getPayBankAccountNo() {
		return get("payBankAccountNo");
	}

	public void setPayBankAccountNo(String payBankAccountNo) {
		set("payBankAccountNo", payBankAccountNo);
	}

	public String getPayBankAccountName() {
		return get("payBankAccountName");
	}

	public void setPayBankAccountName(String payBankAccountName) {
		set("payBankAccountName", payBankAccountName);
	}

	public String getPayBankId() {
		return get("payBankId");
	}

	public void setPayBankId(String payBankId) {
		set("payBankId", payBankId);
	}

	public String getPayBankName() {
		return get("payBankName");
	}

	public void setPayBankName(String payBankName) {
		set("payBankName", payBankName);
	}

	public String getPayBankTypeId() {
		return get("payBankTypeId");
	}

	public void setPayBankTypeId(String payBankTypeId) {
		set("payBankTypeId", payBankTypeId);
	}

	public String getPayLineNumber() {
		return get("payLineNumber");
	}

	public void setPayLineNumber(String payLineNumber) {
		set("payLineNumber", payLineNumber);
	}

	public String getPayCashAccount() {
		return get("payCashAccount");
	}

	public void setPayCashAccount(String payCashAccount) {
		set("payCashAccount", payCashAccount);
	}

	public String getNoteDirect() {
		return get("noteDirect");
	}

	public void setNoteDirect(String noteDirect) {
		set("noteDirect", noteDirect);
	}

	public Long getNoteId() {
		return get("noteId");
	}

	public void setNoteId(Long noteId) {
		set("noteId", noteId);
	}

	public String getNoteno() {
		return get("noteno");
	}

	public void setNoteno(String noteno) {
		set("noteno", noteno);
	}

	public Short getDirectPay() {
		return getShort("directPay");
	}

	public void setDirectPay(Short directPay) {
		set("directPay", directPay);
	}

	public Long getLinkPayStrategyId() {
		return get("linkPayStrategyId");
	}

	public void setLinkPayStrategyId(Long linkPayStrategyId) {
		set("linkPayStrategyId", linkPayStrategyId);
	}

	public String getOppType() {
		return get("oppType");
	}

	public void setOppType(String oppType) {
		set("oppType", oppType);
	}

	public String getOppId() {
		return get("oppId");
	}

	public void setOppId(String oppId) {
		set("oppId", oppId);
	}

	public String getOppFundPartnerTypeId() {
		return get("oppFundPartnerTypeId");
	}

	public void setOppFundPartnerTypeId(String oppFundPartnerTypeId) {
		set("oppFundPartnerTypeId", oppFundPartnerTypeId);
	}

	public String getOppName() {
		return get("oppName");
	}

	public void setOppName(String oppName) {
		set("oppName", oppName);
	}

	public String getPublicOrPrivate() {
		return get("publicOrPrivate");
	}

	public void setPublicOrPrivate(String publicOrPrivate) {
		set("publicOrPrivate", publicOrPrivate);
	}

	public String getOppAcctType() {
		return get("oppAcctType");
	}

	public void setOppAcctType(String oppAcctType) {
		set("oppAcctType", oppAcctType);
	}

	public String getRecBankAccountId() {
		return get("recBankAccountId");
	}

	public void setRecBankAccountId(String recBankAccountId) {
		set("recBankAccountId", recBankAccountId);
	}

	public String getRecBankAccountNo() {
		return get("recBankAccountNo");
	}

	public void setRecBankAccountNo(String recBankAccountNo) {
		set("recBankAccountNo", recBankAccountNo);
	}

	public String getRecBankAccountName() {
		return get("recBankAccountName");
	}

	public void setRecBankAccountName(String recBankAccountName) {
		set("recBankAccountName", recBankAccountName);
	}

	public String getRecBankId() {
		return get("recBankId");
	}

	public void setRecBankId(String recBankId) {
		set("recBankId", recBankId);
	}

	public String getRecBankName() {
		return get("recBankName");
	}

	public void setRecBankName(String recBankName) {
		set("recBankName", recBankName);
	}

	public String getRecBankTypeId() {
		return get("recBankTypeId");
	}

	public void setRecBankTypeId(String recBankTypeId) {
		set("recBankTypeId", recBankTypeId);
	}

	public String getRecLineNumber() {
		return get("recLineNumber");
	}

	public void setRecLineNumber(String recLineNumber) {
		set("recLineNumber", recLineNumber);
	}

	public String getCurrencyId() {
		return get("currencyId");
	}

	public void setCurrencyId(String currencyId) {
		set("currencyId", currencyId);
	}

	public BigDecimal getOriSum() {
		return get("oriSum");
	}

	public void setOriSum(BigDecimal oriSum) {
		set("oriSum", oriSum);
	}

	public BigDecimal getOriRemainAmount() {
		return get("oriRemainAmount");
	}

	public void setOriRemainAmount(BigDecimal oriRemainAmount) {
		set("oriRemainAmount", oriRemainAmount);
	}

	public BigDecimal getOriTransitAmount() {
		return get("oriTransitAmount");
	}

	public void setOriTransitAmount(BigDecimal oriTransitAmount) {
		set("oriTransitAmount", oriTransitAmount);
	}

	public BigDecimal getSettleSucAmount() {
		return get("settleSucAmount");
	}

	public void setSettleSucAmount(BigDecimal settleSucAmount) {
		set("settleSucAmount", settleSucAmount);
	}

	public BigDecimal getSettleStopPayAmount() {
		return get("settleStopPayAmount");
	}

	public void setSettleStopPayAmount(BigDecimal settleStopPayAmount) {
		set("settleStopPayAmount", settleStopPayAmount);
	}

	public Short getExchangePayment() {
		return getShort("exchangePayment");
	}

	public void setExchangePayment(Short exchangePayment) {
		set("exchangePayment", exchangePayment);
	}

	public String getSettleCurrencyId() {
		return get("settleCurrencyId");
	}

	public void setSettleCurrencyId(String settleCurrencyId) {
		set("settleCurrencyId", settleCurrencyId);
	}

	public String getSettleRateTypeId() {
		return get("settleRateTypeId");
	}

	public void setSettleRateTypeId(String settleRateTypeId) {
		set("settleRateTypeId", settleRateTypeId);
	}

	public BigDecimal getSettleRate() {
		return get("settleRate");
	}

	public void setSettleRate(BigDecimal settleRate) {
		set("settleRate", settleRate);
	}

    /**
     * 获取预估结算汇率折算方式
     *
     * @return 汇率折算方式
     */
    public Short getSettleRateOps() {
        return getShort("settleRateOps");
    }

    /**
     * 设置预估结算汇率折算方式
     *
     * @param settleRateOps 汇率折算方式
     */
    public void setSettleRateOps(Short settleRateOps) {
        set("settleRateOps", settleRateOps);
    }

	public BigDecimal getSettleAmount() {
		return get("settleAmount");
	}

	public void setSettleAmount(BigDecimal settleAmount) {
		set("settleAmount", settleAmount);
	}

	public String getNatCurrencyId() {
		return get("natCurrencyId");
	}

	public void setNatCurrencyId(String natCurrencyId) {
		set("natCurrencyId", natCurrencyId);
	}

	public String getNatRateTypeId() {
		return get("natRateTypeId");
	}

	public void setNatRateTypeId(String natRateTypeId) {
		set("natRateTypeId", natRateTypeId);
	}

	public BigDecimal getNatRate() {
		return get("natRate");
	}

	public void setNatRate(BigDecimal natRate) {
		set("natRate", natRate);
	}

    /**
     * 获取汇率折算方式
     *
     * @return 汇率折算方式
     */
    public Short getNatRateOps() {
        return getShort("natRateOps");
    }

    /**
     * 设置汇率折算方式
     *
     * @param natRateOps 汇率折算方式
     */
    public void setNatRateOps(Short natRateOps) {
        set("natRateOps", natRateOps);
    }

	public BigDecimal getNatSum() {
		return get("natSum");
	}

	public void setNatSum(BigDecimal natSum) {
		set("natSum", natSum);
	}

	public Short getAssociationStatusPay() {
		return getShort("associationStatusPay");
	}

	public void setAssociationStatusPay(Short associationStatusPay) {
		set("associationStatusPay", associationStatusPay);
	}

	public Long getPayBankBillId() {
		return get("payBankBillId");
	}

	public void setPayBankBillId(Long payBankBillId) {
		set("payBankBillId", payBankBillId);
	}

	public String getPaySmartCheckNo() {
		return get("paySmartCheckNo");
	}

	public void setPaySmartCheckNo(String paySmartCheckNo) {
		set("paySmartCheckNo", paySmartCheckNo);
	}

	public Short getReFund() {
		return getShort("reFund");
	}

	public void setReFund(Short reFund) {
		set("reFund", reFund);
	}

	public Date getRefundDateTime() {
		return get("refundDateTime");
	}

	public void setRefundDateTime(Date refundDateTime) {
		set("refundDateTime", refundDateTime);
	}

	public BigDecimal getRefundAmount() {
		return get("refundAmount");
	}

	public void setRefundAmount(BigDecimal refundAmount) {
		set("refundAmount", refundAmount);
	}

	public String getEntryType() {
		return get("entryType");
	}

	public void setEntryType(String entryType) {
		set("entryType", entryType);
	}

	public Short getPushStct() {
		return getShort("pushStct");
	}

	public void setPushStct(Short pushStct) {
		set("pushStct", pushStct);
	}

	public Short getMerageEnable() {
		return getShort("merageEnable");
	}

	public void setMerageEnable(Short merageEnable) {
		set("merageEnable", merageEnable);
	}

	public Short getSplitEnable() {
		return getShort("splitEnable");
	}

	public void setSplitEnable(Short splitEnable) {
		set("splitEnable", splitEnable);
	}

	public Short getSettleMethodEditable() {
		return getShort("settleMethodEditable");
	}

	public void setSettleMethodEditable(Short settleMethodEditable) {
		set("settleMethodEditable", settleMethodEditable);
	}

	public Short getSettleInfoEditable() {
		return getShort("settleInfoEditable");
	}

	public void setSettleInfoEditable(Short settleInfoEditable) {
		set("settleInfoEditable", settleInfoEditable);
	}

	public Short getJournalRegisteable() {
		return getShort("journalRegisteable");
	}

	public void setJournalRegisteable(Short journalRegisteable) {
		set("journalRegisteable", journalRegisteable);
	}

	public Short getVouchGenerateable() {
		return getShort("vouchGenerateable");
	}

	public void setVouchGenerateable(Short vouchGenerateable) {
		set("vouchGenerateable", vouchGenerateable);
	}

	public Short getCsplEnable() {
		return getShort("csplEnable");
	}

	public void setCsplEnable(Short csplEnable) {
		set("csplEnable", csplEnable);
	}

	public String getRiskType() {
		return get("riskType");
	}

	public void setRiskType(String riskType) {
		set("riskType", riskType);
	}

	public Short getIsFirstHandler() {
		return getShort("isFirstHandler");
	}

	public void setIsFirstHandler(Short isFirstHandler) {
		set("isFirstHandler", isFirstHandler);
	}

	public Short getIsSettleAgain() {
		return getShort("isSettleAgain");
	}

	public void setIsSettleAgain(Short isSettleAgain) {
		set("isSettleAgain", isSettleAgain);
	}

	public Short getIsOccupyBudget() {
		return getShort("isOccupyBudget");
	}

	public void setIsOccupyBudget(Short isOccupyBudget) {
		set("isOccupyBudget", isOccupyBudget);
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

	public String getRecSmartCheckNo() {
		return get("recSmartCheckNo");
	}

	public void setRecSmartCheckNo(String recSmartCheckNo) {
		set("recSmartCheckNo", recSmartCheckNo);
	}

	public BigDecimal getOutBrokerage() {
		return get("outBrokerage");
	}

	public void setOutBrokerage(BigDecimal outBrokerage) {
		set("outBrokerage", outBrokerage);
	}

	public BigDecimal getNatOutBrokerage() {
		return get("natOutBrokerage");
	}

	public void setNatOutBrokerage(BigDecimal natOutBrokerage) {
		set("natOutBrokerage", natOutBrokerage);
	}

	public Short getCheckPurpose() {
		return getShort("checkPurpose");
	}

	public void setCheckPurpose(Short checkPurpose) {
		set("checkPurpose", checkPurpose);
	}

	public Long getRecSettlemode() {
		return get("recSettlemode");
	}

	public void setRecSettlemode(Long recSettlemode) {
		set("recSettlemode", recSettlemode);
	}

	public String getRecCashAccount() {
		return get("recCashAccount");
	}

	public void setRecCashAccount(String recCashAccount) {
		set("recCashAccount", recCashAccount);
	}

	public String getSwiftCode() {
		return get("swiftCode");
	}

	public void setSwiftCode(String swiftCode) {
		set("swiftCode", swiftCode);
	}

	public BigDecimal getInBrokerage() {
		return get("inBrokerage");
	}

	public void setInBrokerage(BigDecimal inBrokerage) {
		set("inBrokerage", inBrokerage);
	}

	public BigDecimal getNatInBrokerage() {
		return get("natInBrokerage");
	}

	public void setNatInBrokerage(BigDecimal natInBrokerage) {
		set("natInBrokerage", natInBrokerage);
	}

	public Long getRecBankBillId() {
		return get("recBankBillId");
	}

	public void setRecBankBillId(Long recBankBillId) {
		set("recBankBillId", recBankBillId);
	}

	public String getDeptId() {
		return get("deptId");
	}

	public void setDeptId(String deptId) {
		set("deptId", deptId);
	}

	public String getProjectId() {
		return get("projectId");
	}

	public void setProjectId(String projectId) {
		set("projectId", projectId);
	}

	public String getRecSettleStatus() {
		return get("recSettleStatus");
	}

	public void setRecSettleStatus(String recSettleStatus) {
		set("recSettleStatus", recSettleStatus);
	}

	public Date getRecSettleSuccessDate() {
		return get("recSettleSuccessDate");
	}

	public void setRecSettleSuccessDate(Date recSettleSuccessDate) {
		set("recSettleSuccessDate", recSettleSuccessDate);
	}

	public Date getRecSettleSuccessDateTime() {
		return get("recSettleSuccessDateTime");
	}

	public void setRecSettleSuccessDateTime(Date recSettleSuccessDateTime) {
		set("recSettleSuccessDateTime", recSettleSuccessDateTime);
	}

	public String getFailmessage() {
		return get("failmessage");
	}

	public void setFailmessage(String failmessage) {
		set("failmessage", failmessage);
	}

	public Short getIsCashBusiness() {
		return getShort("isCashBusiness");
	}

	public void setIsCashBusiness(Short isCashBusiness) {
		set("isCashBusiness", isCashBusiness);
	}


	@Override
	public String getYTenant() {
		return get("ytenant");
	}

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
	public Long getSourceid() {
		return get("sourceid");
	}

	@Override
	public void setSourceid(Long sourceid) {
		set("sourceid", sourceid);
	}

	@Override
	public Long getSourceautoid() {
		return get("sourceautoid");
	}

	@Override
	public void setSourceautoid(Long sourceautoid) {
		set("sourceautoid", sourceautoid);
	}

	@Override
	public String getSource() {
		return get("source");
	}

	@Override
	public void setSource(String source) {
		set("source", source);
	}

	@Override
	public String getUpcode() {
		return get("upcode");
	}

	@Override
	public void setUpcode(String upcode) {
		set("upcode", upcode);
	}

	@Override
	public String getMakeRuleCode() {
		return get("makeRuleCode");
	}

	@Override
	public void setMakeRuleCode(String makeRuleCode) {
		set("makeRuleCode", makeRuleCode);
	}

	@Override
	public java.util.Date getSourceMainPubts() {
		return get("sourceMainPubts");
	}

	@Override
	public void setSourceMainPubts(java.util.Date sourceMainPubts) {
		set("sourceMainPubts", sourceMainPubts);
	}

	@Override
	public String getGroupTaskKey() {
		return get("groupTaskKey");
	}

	@Override
	public void setGroupTaskKey(String groupTaskKey) {
		set("groupTaskKey", groupTaskKey);
	}

	public String getAccentity() {
		return get("accentity");
	}

	public void setAccentity(String accentity) {
		set("accentity", accentity);
	}
}
