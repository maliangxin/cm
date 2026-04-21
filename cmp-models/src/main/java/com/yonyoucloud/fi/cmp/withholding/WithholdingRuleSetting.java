package com.yonyoucloud.fi.cmp.withholding;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 预提规则设置实体
 *
 * @author u
 * @version 1.0
 */
public class WithholdingRuleSetting extends BizObject implements IAuditInfo, ITenant, IYTenant, AccentityRawInterface {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.withholding.WithholdingRuleSetting";

    /**
     * 获取核算会计
     *
     * @return 核算会计.ID
     */
    @Override
    public String getAccentityRaw() {
        return get("accentityRaw");
    }

    /**
     * 设置核算会计
     *
     * @param accentityRaw 核算会计.ID
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
     * 获取规则状态
     *
     * @return 规则状态
     */
    public Short getRuleStatus() {
        return getShort("ruleStatus");
    }

    /**
     * 设置规则状态
     *
     * @param ruleStatus 规则状态
     */
    public void setRuleStatus(Short ruleStatus) {
        set("ruleStatus", ruleStatus);
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
     * 获取银行账户利率设置变更历史表集合
     *
     * @return 银行账户利率设置变更历史表集合
     */
    public java.util.List<InterestRateSettingHistory> InterestRateSettingHistory() {
        return getBizObjects("InterestRateSettingHistory", InterestRateSettingHistory.class);
    }

    /**
     * 设置银行账户利率设置变更历史表集合
     *
     * @param InterestRateSettingHistory 银行账户利率设置变更历史表集合
     */
    public void setInterestRateSettingHistory(java.util.List<InterestRateSettingHistory> InterestRateSettingHistory) {
        setBizObjects("InterestRateSettingHistory", InterestRateSettingHistory);
    }

    /**
     * 获取利息测算表集合
     *
     * @return 利息测算表集合
     */
    public java.util.List<InterestCalculation> InterestCalculation() {
        return getBizObjects("InterestCalculation", InterestCalculation.class);
    }

    /**
     * 设置利息测算表集合
     *
     * @param InterestCalculation 利息测算表集合
     */
    public void setInterestCalculation(java.util.List<InterestCalculation> InterestCalculation) {
        setBizObjects("InterestCalculation", InterestCalculation);
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
     * 获取设置预提规则设置表集合
     *
     * @return 预提规则设置表集合
     */
    public java.util.List<BizObject> getWithholdingRuleSettingList() {
        return get("withholdingrulesettinglist");
    }

    /**
     * 设置预提规则设置表集合
     *
     * @param withholdingrulesettinglist 预提规则设置表集合
     */
    public void setWithholdingRuleSettingList(java.util.List<BizObject> withholdingrulesettinglist) {
        set("withholdingrulesettinglist", withholdingrulesettinglist);
    }


    /**
     * 获取计息天数
     *
     * @return 计息天数
     */
    public Short getInterestDays() {
        return getShort("interestDays");
    }

    /**
     * 设置计息天数
     *
     * @param interestDays 计息天数
     */
    public void setInterestDays(Short interestDays) {
        set("interestDays", interestDays);
    }

    /**
     * 获取存款利率
     *
     * @return 存款利率
     */
    public java.math.BigDecimal getInterestRate() {
        return get("interestRate");
    }

    /**
     * 设置存款利率
     *
     * @param interestRate 存款利率
     */
    public void setInterestRate(java.math.BigDecimal interestRate) {
        set("interestRate", interestRate);
    }

    /**
     * 获取透支利率
     *
     * @return 透支利率
     */
    public java.math.BigDecimal getOverdraftRate() {
        return get("overdraftRate");
    }

    /**
     * 设置透支利率
     *
     * @param overdraftRate 透支利率
     */
    public void setOverdraftRate(java.math.BigDecimal overdraftRate) {
        set("overdraftRate", overdraftRate);
    }

    /**
     * 获取版本号
     *
     * @return 版本号
     */
    public Long getVersion() {
        return get("version");
    }

    /**
     * 设置版本号
     *
     * @param version 版本号
     */
    public void setVersion(Long version) {
        set("version", version);
    }


    /**
     * 获取签约协定存款
     *
     * @return 签约协定存款
     */
    public Short getIssignagree() {
        return getShort("issignagree");
    }

    /**
     * 设置签约协定存款
     *
     * @param issignagree 签约协定存款
     */
    public void setIssignagree(Short issignagree) {
        set("issignagree", issignagree);
    }

    /**
     * 获取协定存款计息天数
     *
     * @return 协定存款计息天数
     */
    public String getAgreeinterestdays() {
        return get("agreeinterestdays");
    }

    /**
     * 设置协定存款计息天数
     *
     * @param agreeinterestdays 协定存款计息天数
     */
    public void setAgreeinterestdays(String agreeinterestdays) {
        set("agreeinterestdays", agreeinterestdays);
    }

    /**
     * 获取协定存款计息方式
     *
     * @return 协定存款计息方式
     */
    public Short getAgreeinterestmethod() {
        return getShort("agreeinterestmethod");
    }

    /**
     * 设置协定存款计息方式
     *
     * @param agreeinterestmethod 协定存款计息方式
     */
    public void setAgreeinterestmethod(Short agreeinterestmethod) {
        set("agreeinterestmethod", agreeinterestmethod);
    }

    /**
     * 获取协定存款靠档方式
     *
     * @return 协定存款靠档方式
     */
    public Short getAgreerelymethod() {
        return getShort("agreerelymethod");
    }

    /**
     * 设置协定存款靠档方式
     *
     * @param agreerelymethod 协定存款靠档方式
     */
    public void setAgreerelymethod(Short agreerelymethod) {
        set("agreerelymethod", agreerelymethod);
    }

    /**
     * 获取协定存款生效日期
     *
     * @return 协定存款生效日期
     */
    public java.util.Date getAgreestartdate() {
        return getDate("agreestartdate");
    }

    /**
     * 设置协定存款生效日期
     *
     * @param agreestartdate 协定存款生效日期
     */
    public void setAgreestartdate(java.util.Date agreestartdate) {
        set("agreestartdate", agreestartdate);
    }

    /**
     * 获取协定存款结束日期
     *
     * @return 协定存款结束日期
     */
    public java.util.Date getAgreeenddate() {
        return getDate("agreeenddate");
    }

    /**
     * 设置协定存款结束日期
     *
     * @param agreeenddate 协定存款结束日期
     */
    public void setAgreeenddate(java.util.Date agreeenddate) {
        set("agreeenddate", agreeenddate);
    }

    /**
     * 获取协定存款分档子表集合
     *
     * @return 协定存款分档子表集合
     */
    public java.util.List<AgreeIRGrade> agreeIRGrade() {
        return getBizObjects("agreeIRGrade", AgreeIRGrade.class);
    }

    /**
     * 设置协定存款分档子表集合
     *
     * @param agreeIRGrade 协定存款分档子表集合
     */
    public void setAgreeIRGrade(java.util.List<AgreeIRGrade> agreeIRGrade) {
        setBizObjects("agreeIRGrade", agreeIRGrade);
    }

    /**
     * 获取协定存款利率变更历史记录子表集合
     *
     * @return 协定存款利率变更历史记录子表集合
     */
    public java.util.List<AgreeIRSettingHistory> agreeIRSettingHistory() {
        return getBizObjects("agreeIRSettingHistory", AgreeIRSettingHistory.class);
    }

    /**
     * 设置协定存款利率变更历史记录子表集合
     *
     * @param agreeIRSettingHistory 协定存款利率变更历史记录子表集合
     */
    public void setAgreeIRSettingHistory(java.util.List<AgreeIRSettingHistory> agreeIRSettingHistory) {
        setBizObjects("agreeIRSettingHistory", agreeIRSettingHistory);
    }
}
