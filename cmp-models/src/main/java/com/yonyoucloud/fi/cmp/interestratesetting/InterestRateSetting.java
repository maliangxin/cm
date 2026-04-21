package com.yonyoucloud.fi.cmp.interestratesetting;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 银行账户利率设置实体
 *
 * @author u
 * @version 1.0
 */
public class InterestRateSetting extends BizObject implements IAuditInfo, ITenant, IYTenant, AccentityRawInterface {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.interestratesetting.InterestRateSetting";

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
     * 获取预提规则设置id
     *
     * @return 预提规则设置id
     */
    public Long getAccountNumberId() {
        return get("accountNumberId");
    }

    /**
     * 设置预提规则设置id
     *
     * @param accountNumberId 预提规则设置id
     */
    public void setAccountNumberId(Long accountNumberId) {
        set("accountNumberId", accountNumberId);
    }

    /**
     * 获取银行账户
     *
     * @return 银行账户.ID
     */
    public String getBankAccount() {
        return get("bankAccount");
    }

    /**
     * 设置银行账户
     *
     * @param bankAccount 银行账户.ID
     */
    public void setBankAccount(String bankAccount) {
        set("bankAccount", bankAccount);
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
     * 获取存款利率变更方式:1.按比例增减%,2.增加/减少固定值,3.调整为固定值
     *
     * @return 存款利率变更方式:1.按比例增减%,2.增加/减少固定值,3.调整为固定值
     */
    public Short getDepositChangeType() {
        return getShort("depositChangeType");
    }

    /**
     * 设置存款利率变更方式:1.按比例增减%,2.增加/减少固定值,3.调整为固定值
     *
     * @param depositChangeType 存款利率变更方式:1.按比例增减%,2.增加/减少固定值,3.调整为固定值
     */
    public void setDepositChangeType(Short depositChangeType) {
        set("depositChangeType", depositChangeType);
    }

    /**
     * 获取存款利率变更值
     *
     * @return 存款利率变更值
     */
    public java.math.BigDecimal getDepositChangeValue() {
        return get("depositChangeValue");
    }

    /**
     * 设置存款利率变更值
     *
     * @param depositChangeValue 存款利率变更值
     */
    public void setDepositChangeValue(java.math.BigDecimal depositChangeValue) {
        set("depositChangeValue", depositChangeValue);
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
     * 获取透支利率变更方式:1.按比例增减%,2.增加/减少固定值,3.调整为固定值
     *
     * @return 透支利率变更方式:1.按比例增减%,2.增加/减少固定值,3.调整为固定值
     */
    public Short getOverdraftChangeType() {
        return getShort("overdraftChangeType");
    }

    /**
     * 设置透支利率变更方式:1.按比例增减%,2.增加/减少固定值,3.调整为固定值
     *
     * @param overdraftChangeType 透支利率变更方式:1.按比例增减%,2.增加/减少固定值,3.调整为固定值
     */
    public void setOverdraftChangeType(Short overdraftChangeType) {
        set("overdraftChangeType", overdraftChangeType);
    }

    /**
     * 获取透支利率变更值
     *
     * @return 透支利率变更值
     */
    public java.math.BigDecimal getOverdraftChangeValue() {
        return get("overdraftChangeValue");
    }

    /**
     * 设置透支利率变更值
     *
     * @param overdraftChangeValue 透支利率变更值
     */
    public void setOverdraftChangeValue(java.math.BigDecimal overdraftChangeValue) {
        set("overdraftChangeValue", overdraftChangeValue);
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
     * 获取利率生效日期
     *
     * @return 利率生效日期
     */
    public java.util.Date getStartDate() {
        return get("startDate");
    }

    /**
     * 设置利率生效日期
     *
     * @param startDate 利率生效日期
     */
    public void setStartDate(java.util.Date startDate) {
        set("startDate", startDate);
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
     * 获取银行利率设置表集合
     *
     * @return 预提规则设置表集合
     */
    public java.util.List<BizObject> getInterestRateSettingList() {
        return get("interestratesettinglist");
    }

    /**
     * 设置银行利率设置表集合
     *
     * @param interestratesettinglist 银行利率设置表集合
     */
    public void setInterestRateSettingList(java.util.List<BizObject> interestratesettinglist) {
        set("interestratesettinglist", interestratesettinglist);
    }

    /**
     * 获取结束日期
     *
     * @return 结束日期
     */
    public java.util.Date getAgreeenddate() {
        return get("agreeenddate");
    }

    /**
     * 设置结束日期
     *
     * @param agreeenddate 结束日期
     */
    public void setAgreeenddate(java.util.Date agreeenddate) {
        set("agreeenddate", agreeenddate);
    }

    /**
     * 获取计息天数
     *
     * @return 计息天数
     */
    public String getAgreeinterestdays() {
        return get("agreeinterestdays");
    }

    /**
     * 设置计息天数
     *
     * @param agreeinterestdays 计息天数
     */
    public void setAgreeinterestdays(String agreeinterestdays) {
        set("agreeinterestdays", agreeinterestdays);
    }

    /**
     * 获取计息方式
     *
     * @return 计息方式
     */
    public Short getAgreeinterestmethod() {
        return getShort("agreeinterestmethod");
    }

    /**
     * 设置计息方式
     *
     * @param agreeinterestmethod 计息方式
     */
    public void setAgreeinterestmethod(Short agreeinterestmethod) {
        set("agreeinterestmethod", agreeinterestmethod);
    }

    /**
     * 获取靠档方式
     *
     * @return 靠档方式
     */
    public Short getAgreerelymethod() {
        return getShort("agreerelymethod");
    }

    /**
     * 设置靠档方式
     *
     * @param agreerelymethod 靠档方式
     */
    public void setAgreerelymethod(Short agreerelymethod) {
        set("agreerelymethod", agreerelymethod);
    }

    /**
     * 获取生效日期
     *
     * @return 生效日期
     */
    public java.util.Date getAgreestartdate() {
        return get("agreestartdate");
    }

    /**
     * 设置生效日期
     *
     * @param agreestartdate 生效日期
     */
    public void setAgreestartdate(java.util.Date agreestartdate) {
        set("agreestartdate", agreestartdate);
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
     * 获取协定存款分档子表集合
     *
     * @return 协定存款分档子表集合
     */
    public java.util.List<AgreeIRSettingGrade> agreeIRSettingGrade() {
        return getBizObjects("agreeIRSettingGrade", AgreeIRSettingGrade.class);
    }

    /**
     * 设置协定存款分档子表集合
     *
     * @param agreeIRSettingGrade 协定存款分档子表集合
     */
    public void setAgreeIRSettingGrade(java.util.List<AgreeIRSettingGrade> agreeIRSettingGrade) {
        setBizObjects("agreeIRSettingGrade", agreeIRSettingGrade);
    }

}
