package com.yonyoucloud.fi.cmp.accrualsWithholding;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.*;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 存款利息预提实体
 *
 * @author u
 * @version 1.0
 */
public class AccrualsWithholding extends BizObject implements IAuditInfo, ITenant, IYTenant, IApprovalInfo, ICurrency, IApprovalFlow, IPrintCount, AccentityRawInterface {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.accruals.AccrualsWithholding";
    private static final long serialVersionUID = 1586469094382351646L;
    // 业务对象编码
    public static final String BUSI_OBJ_CODE = "ctm-cmp.cmp_accrualsWithholdingquery";

    /**
     * 获取核算会计 主体
     *
     * @return 核算会计 主体.ID
     */
    @Override
    public String getAccentityRaw() {
        return get("accentityRaw");
    }

    /**
     * 设置核算会计 主体
     *
     * @param accentityRaw 核算会计 主体.ID
     */
    @Override
    public void setAccentityRaw(String accentityRaw) {
        set("accentityRaw", accentityRaw);
    }

    /**
     * 获取资金组织
     *
     * @return 资金组织.ID
     */
    @Override
    public String getAccentity() {
        return get("accentity");
    }

    /**
     * 设置资金组织
     *
     * @param accentity 资金组织.ID
     */
    @Override
    public void setAccentity(String accentity) {
        set("accentity", accentity);
    }

    /**
     * 获取预提结束日
     *
     * @return 预提结束日
     */
    public java.util.Date getAccruedEndDate() {
        return get("accruedEndDate");
    }

    /**
     * 设置预提结束日
     *
     * @param accruedEndDate 预提结束日
     */
    public void setAccruedEndDate(java.util.Date accruedEndDate) {
        set("accruedEndDate", accruedEndDate);
    }

    /**
     * 获取预提开始日
     *
     * @return 预提开始日
     */
    public java.util.Date getAccruedStartDate() {
        return get("accruedStartDate");
    }

    /**
     * 设置预提开始日
     *
     * @param accruedStartDate 预提开始日
     */
    public void setAccruedStartDate(java.util.Date accruedStartDate) {
        set("accruedStartDate", accruedStartDate);
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

    /**
     * 获取银行类别
     *
     * @return 银行类别.ID
     */
    public String getBankType() {
        return get("bankType");
    }

    /**
     * 设置银行类别
     *
     * @param bankType 银行类别.ID
     */
    public void setBankType(String bankType) {
        set("bankType", bankType);
    }

    /**
     * 获取银行账户
     *
     * @return 银行账户.ID
     */
    public String getBankaccount() {
        return get("bankaccount");
    }

    /**
     * 设置银行账户
     *
     * @param bankaccount 银行账户.ID
     */
    public void setBankaccount(String bankaccount) {
        set("bankaccount", bankaccount);
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
     * 获取原币
     *
     * @return 原币.ID
     */
    public String getCurrency() {
        return get("currency");
    }

    /**
     * 设置原币
     *
     * @param currency 原币.ID
     */
    public void setCurrency(String currency) {
        set("currency", currency);
    }

    /**
     * 获取活期存款利息测算(本币)
     *
     * @return 活期存款利息测算(本币)
     */
    public java.math.BigDecimal getCurrencyDepositInterestCalculate() {
        return get("currencyDepositInterestCalculate");
    }

    /**
     * 设置活期存款利息测算(本币)
     *
     * @param currencyDepositInterestCalculate 活期存款利息测算(本币)
     */
    public void setCurrencyDepositInterestCalculate(java.math.BigDecimal currencyDepositInterestCalculate) {
        set("currencyDepositInterestCalculate", currencyDepositInterestCalculate);
    }

    /**
     * 获取活期透支利息测算(本币)
     *
     * @return 活期透支利息测算(本币)
     */
    public java.math.BigDecimal getCurrencyOverdraftInterestCalculate() {
        return get("currencyOverdraftInterestCalculate");
    }

    /**
     * 设置活期透支利息测算(本币)
     *
     * @param currencyOverdraftInterestCalculate 活期透支利息测算(本币)
     */
    public void setCurrencyOverdraftInterestCalculate(java.math.BigDecimal currencyOverdraftInterestCalculate) {
        set("currencyOverdraftInterestCalculate", currencyOverdraftInterestCalculate);
    }

    /**
     * 获取合计利息(本币)
     *
     * @return 合计利息(本币)
     */
    public java.math.BigDecimal getCurrencyTotalInterest() {
        return get("currencyTotalInterest");
    }

    /**
     * 设置合计利息(本币)
     *
     * @param currencyTotalInterest 合计利息(本币)
     */
    public void setCurrencyTotalInterest(java.math.BigDecimal currencyTotalInterest) {
        set("currencyTotalInterest", currencyTotalInterest);
    }

    /**
     * 获取当前存款利率
     *
     * @return 当前存款利率
     */
    public java.math.BigDecimal getCurrentDepositRate() {
        return get("currentDepositRate");
    }

    /**
     * 设置当前存款利率
     *
     * @param currentDepositRate 当前存款利率
     */
    public void setCurrentDepositRate(java.math.BigDecimal currentDepositRate) {
        set("currentDepositRate", currentDepositRate);
    }

    /**
     * 获取当前透支利率
     *
     * @return 当前透支利率
     */
    public java.math.BigDecimal getCurrentOverdraftRate() {
        return get("currentOverdraftRate");
    }

    /**
     * 设置当前透支利率
     *
     * @param currentOverdraftRate 当前透支利率
     */
    public void setCurrentOverdraftRate(java.math.BigDecimal currentOverdraftRate) {
        set("currentOverdraftRate", currentOverdraftRate);
    }
    /**
     * 获取存款利息测算
     *
     * @return 存款利息测算
     */
    public java.math.BigDecimal getDepositInterestCalculate() {
        return get("depositInterestCalculate");
    }

    /**
     * 设置存款利息测算
     *
     * @param depositInterestCalculate 存款利息测算
     */
    public void setDepositInterestCalculate(java.math.BigDecimal depositInterestCalculate) {
        set("depositInterestCalculate", depositInterestCalculate);
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
     * 获取汇率类型
     *
     * @return 汇率类型.ID
     */
    public String getExchangeRateType() {
        return get("exchangeRateType");
    }

    /**
     * 设置汇率类型
     *
     * @param exchangeRateType 汇率类型.ID
     */
    public void setExchangeRateType(String exchangeRateType) {
        set("exchangeRateType", exchangeRateType);
    }

    /**
     * 获取汇率
     *
     * @return 汇率
     */
    public java.math.BigDecimal getExchangerate() {
        return get("exchangerate");
    }

    /**
     * 设置汇率
     *
     * @param exchangerate 汇率
     */
    public void setExchangerate(java.math.BigDecimal exchangerate) {
        set("exchangerate", exchangerate);
    }

    /**
     * 获取汇率折算方式
     *
     * @return 汇率折算方式
     */
    public Short getExchangerateOps() {
        return getShort("exchangerateOps");
    }

    /**
     * 设置汇率折算方式
     *
     * @param exchangerateOps 汇率折算方式
     */
    public void setExchangerateOps(Short exchangerateOps) {
        set("exchangerateOps", exchangerateOps);
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
     * 获取上次结息/预提结束日
     *
     * @return 上次结息/预提结束日
     */
    public java.util.Date getLastInterestSettlementOrAccruedDate() {
        return get("lastInterestSettlementOrAccruedDate");
    }

    /**
     * 设置上次结息/预提结束日
     *
     * @param lastInterestSettlementOrAccruedDate 上次结息/预提结束日
     */
    public void setLastInterestSettlementOrAccruedDate(java.util.Date lastInterestSettlementOrAccruedDate) {
        set("lastInterestSettlementOrAccruedDate", lastInterestSettlementOrAccruedDate);
    }


    /**
     * 获取利息计算逻辑
     *
     * @return 利息计算逻辑
     */
    public String getLogicMsg() {
        return get("logicMsg");
    }

    /**
     * 设置利息计算逻辑
     *
     * @param logicMsg 利息计算逻辑
     */
    public void setLogicMsg(String logicMsg) {
        set("logicMsg", logicMsg);
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
     * 获取是否透支
     *
     * @return 是否透支
     */
    public Boolean getOverdraftFlag() {
        return getBoolean("overdraftFlag");
    }

    /**
     * 设置是否透支
     *
     * @param overdraftFlag 是否透支
     */
    public void setOverdraftFlag(Boolean overdraftFlag) {
        set("overdraftFlag", overdraftFlag);
    }

    /**
     * 获取透支利息测算
     *
     * @return 透支利息测算
     */
    public java.math.BigDecimal getOverdraftInterestCalculate() {
        return get("overdraftInterestCalculate");
    }

    /**
     * 设置透支利息测算
     *
     * @param overdraftInterestCalculate 透支利息测算
     */
    public void setOverdraftInterestCalculate(java.math.BigDecimal overdraftInterestCalculate) {
        set("overdraftInterestCalculate", overdraftInterestCalculate);
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
     * 获取关联结息单
     *
     * @return 关联结息单
     */
    public Short getRelatedinterest() {
        return getShort("relatedinterest");
    }

    /**
     * 设置关联结息单
     *
     * @param relatedinterest 关联结息单
     */
    public void setRelatedinterest(Short relatedinterest) {
        set("relatedinterest", relatedinterest);
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
     * 获取版本号
     *
     * @return 版本号
     */
    public String getSettingVersion() {
        return get("settingVersion");
    }

    /**
     * 设置版本号
     *
     * @param settingVersion 版本号
     */
    public void setSettingVersion(String settingVersion) {
        set("settingVersion", settingVersion);
    }

    /**
     * 获取结算状态
     *
     * @return 结算状态
     */
    public FundSettleStatus getSettlestatus() {
        return get("settlestatus");
    }

    /**
     * 设置结算状态
     *
     * @param settlestatus 结算状态
     */
    public void setSettlestatus(FundSettleStatus settlestatus) {
        set("settlestatus", settlestatus);
    }

    /**
     * 获取结息单id
     *
     * @return 结息单id
     */
    public Long getSrcbillid() {
        return get("srcbillid");
    }

    /**
     * 设置结息单id
     *
     * @param srcbillid 结息单id
     */
    public void setSrcbillid(Long srcbillid) {
        set("srcbillid", srcbillid);
    }

    /**
     * 获取结息单单据号
     *
     * @return 结息单单据号
     */
    public String getSrcbillno() {
        return get("srcbillno");
    }

    /**
     * 设置结息单单据号
     *
     * @param srcbillno 结息单单据号
     */
    public void setSrcbillno(String srcbillno) {
        set("srcbillno", srcbillno);
    }

    /**
     * 获取账户用途
     *
     * @return 账户用途.ID
     */
    public String getAccountPurpose() {
        return get("accountPurpose");
    }

    /**
     * 设置账户用途
     *
     * @param accountPurpose 账户用途.ID
     */
    public void setAccountPurpose(String accountPurpose) {
        set("accountPurpose", accountPurpose);
    }

    /**
     * 获取开户网点
     *
     * @return 开户网点.ID
     */
    public String getBankNumber() {
        return get("bankNumber");
    }

    /**
     * 设置开户网点
     *
     * @param bankNumber 开户网点.ID
     */
    public void setBankNumber(String bankNumber) {
        set("bankNumber", bankNumber);
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
     * 获取合计利息
     *
     * @return 合计利息
     */
    public java.math.BigDecimal getTotalInterest() {
        return get("totalInterest");
    }

    /**
     * 设置合计利息
     *
     * @param totalInterest 合计利息
     */
    public void setTotalInterest(java.math.BigDecimal totalInterest) {
        set("totalInterest", totalInterest);
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
     * 获取审批状态
     *
     * @return 审批状态
     */
    @Override
    public Short getVerifystate() {
        return getShort("verifystate");
    }

    /**
     * 设置审批状态
     *
     * @param verifystate 审批状态
     */
    @Override
    public void setVerifystate(Short verifystate) {
        set("verifystate", verifystate);
    }

    /**
     * 获取事项分录ID|凭证ID
     *
     * @return 事项分录ID|凭证ID
     */
    public String getVoucherId() {
        return get("voucherId");
    }

    /**
     * 设置事项分录ID|凭证ID
     *
     * @param voucherId 事项分录ID|凭证ID
     */
    public void setVoucherId(String voucherId) {
        set("voucherId", voucherId);
    }

    /**
     * 获取凭证号
     *
     * @return 凭证号
     */
    public String getVoucherNo() {
        return get("voucherNo");
    }

    /**
     * 设置凭证号
     *
     * @param voucherNo 凭证号
     */
    public void setVoucherNo(String voucherNo) {
        set("voucherNo", voucherNo);
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
     * 获取凭证期间
     *
     * @return 凭证期间
     */
    public String getVoucherPeriod() {
        return get("voucherPeriod");
    }

    /**
     * 设置凭证期间
     *
     * @param voucherPeriod 凭证期间
     */
    public void setVoucherPeriod(String voucherPeriod) {
        set("voucherPeriod", voucherPeriod);
    }

    /**
     * 获取凭证状态
     *
     * @return 凭证状态
     */
    public Short getVoucherstatus() {
        return getShort("voucherstatus");
    }

    /**
     * 设置凭证状态
     *
     * @param voucherstatus 凭证状态
     */
    public void setVoucherstatus(Short voucherstatus) {
        set("voucherstatus", voucherstatus);
    }

    /**
     * 获取预提规则设置id
     *
     * @return 预提规则设置id
     */
    public Long getWithholdingRuleId() {
        return get("withholdingRuleId");
    }

    /**
     * 设置预提规则设置id
     *
     * @param withholdingRuleId 预提规则设置id
     */
    public void setWithholdingRuleId(Long withholdingRuleId) {
        set("withholdingRuleId", withholdingRuleId);
    }
    /**
     * 获取结息单billnum
     *
     * @return 结息单billnum
     */
    public String getSrcbillnum() {
        return get("srcbillnum");
    }

    /**
     * 设置结息单billnum
     *
     * @param srcbillnum 结息单billnum
     */
    public void setSrcbillnum(String srcbillnum) {
        set("srcbillnum", srcbillnum);
    }

    /**
     * 获取结息单卡片类型
     *
     * @return 结息单卡片类型
     */
    public String getSrcbilltype() {
        return get("srcbilltype");
    }

    /**
     * 设置结息单卡片类型
     *
     * @param srcbilltype 结息单卡片类型
     */
    public void setSrcbilltype(String srcbilltype) {
        set("srcbilltype", srcbilltype);
    }
    /**
     * 获取结息单主表id
     *
     * @return 结息单主表id
     */
    public Long getSrcbillmainid() {
        return get("srcbillmainid");
    }

    /**
     * 设置结息单主表id
     *
     * @param srcbillmainid 结息单主表id
     */
    public void setSrcbillmainid(Long srcbillmainid) {
        set("srcbillmainid", srcbillmainid);
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

    /**
     * 获取上次预提结束日
     *
     * @return 上次预提结束日
     */
    public java.util.Date getLastInterestAccruedDate() {
        return get("lastInterestAccruedDate");
    }

    /**
     * 设置上次预提结束日
     *
     * @param lastInterestAccruedDate 上次预提结束日
     */
    public void setLastInterestAccruedDate(java.util.Date lastInterestAccruedDate) {
        set("lastInterestAccruedDate", lastInterestAccruedDate);
    }

    /**
     * 获取上次结息结束日
     *
     * @return 上次结息结束日
     */
    public java.util.Date getLastInterestSettlementDate() {
        return get("lastInterestSettlementDate");
    }

    /**
     * 设置上次结息结束日
     *
     * @param lastInterestSettlementDate 上次结息结束日
     */
    public void setLastInterestSettlementDate(java.util.Date lastInterestSettlementDate) {
        set("lastInterestSettlementDate", lastInterestSettlementDate);
    }

    /**
     * 获取日结控制
     *
     * @return 日结控制
     */
    public Short getDailySettlementControl() {
        return getShort("dailySettlementControl");
    }

    /**
     * 设置日结控制
     *
     * @param dailySettlementControl 日结控制
     */
    public void setDailySettlementControl(Short dailySettlementControl) {
        set("dailySettlementControl", dailySettlementControl);
    }
    /**
     * 获取协定存款利息测算(本币)
     *
     * @return 协定存款利息测算(本币)
     */
    public java.math.BigDecimal getCurrencyDepositAgreementCalculate() {
        return get("currencyDepositAgreementCalculate");
    }

    /**
     * 设置协定存款利息测算(本币)
     *
     * @param currencyDepositAgreementCalculate 协定存款利息测算(本币)
     */
    public void setCurrencyDepositAgreementCalculate(java.math.BigDecimal currencyDepositAgreementCalculate) {
        set("currencyDepositAgreementCalculate", currencyDepositAgreementCalculate);
    }

    /**
     * 获取协定存款利息测算
     *
     * @return 协定存款利息测算
     */
    public java.math.BigDecimal getDepositAgreementCalculate() {
        return get("depositAgreementCalculate");
    }

    /**
     * 设置协定存款利息测算
     *
     * @param depositAgreementCalculate 协定存款利息测算
     */
    public void setDepositAgreementCalculate(java.math.BigDecimal depositAgreementCalculate) {
        set("depositAgreementCalculate", depositAgreementCalculate);
    }
}
