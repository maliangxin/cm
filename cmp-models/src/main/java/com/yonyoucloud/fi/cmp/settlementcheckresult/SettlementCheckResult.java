package com.yonyoucloud.fi.cmp.settlementcheckresult;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 日结检查结果主表实体
 *
 * @author u
 * @version 1.0
 */
public class SettlementCheckResult extends BizObject implements IYTenant, IAuditInfo, ITenant, AccentityRawInterface {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.settlementcheckresult.SettlementCheckResult";

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
     * 获取检查名称
     *
     * @return 检查名称
     */
    public String getCheckName() {
        return get("checkName");
    }

    /**
     * 设置检查名称
     *
     * @param checkName 检查名称
     */
    public void setCheckName(String checkName) {
        set("checkName", checkName);
    }

    /**
     * 获取检查结果
     *
     * @return 检查结果
     */
    public String getCheckResult() {
        return get("checkResult");
    }

    /**
     * 设置检查结果
     *
     * @param checkResult 检查结果
     */
    public void setCheckResult(String checkResult) {
        set("checkResult", checkResult);
    }

    /**
     * 获取明细检查结果
     *
     * @return 明细检查结果
     */
    public String getCheckResultDetail() {
        return get("checkResultDetail");
    }

    /**
     * 设置明细检查结果
     *
     * @param checkResultDetail 明细检查结果
     */
    public void setCheckResultDetail(String checkResultDetail) {
        set("checkResultDetail", checkResultDetail);
    }

    /**
     * 获取审核状态
     *
     * @return 审核状态
     */
    public String getCheckRule() {
        return get("checkRule");
    }

    /**
     * 设置审核状态
     *
     * @param checkRule 审核状态
     */
    public void setCheckRule(String checkRule) {
        set("checkRule", checkRule);
    }

    /**
     * 获取最终检查结果
     *
     * @return 最终检查结果
     */
    public String getCheckrResult() {
        return get("checkrResult");
    }

    /**
     * 设置最终检查结果
     *
     * @param checkrResult 最终检查结果
     */
    public void setCheckrResult(String checkrResult) {
        set("checkrResult", checkrResult);
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
     * 获取提示信息
     *
     * @return 提示信息
     */
    public String getMessage() {
        return get("message");
    }

    /**
     * 设置提示信息
     *
     * @param message 提示信息
     */
    public void setMessage(String message) {
        set("message", message);
    }

    /**
     * 获取月末汇兑损益计算检查信息
     *
     * @return 月末汇兑损益计算检查信息
     */
    public String getMessageAdjustment() {
        return get("messageAdjustment");
    }

    /**
     * 设置月末汇兑损益计算检查信息
     *
     * @param messageAdjustment 月末汇兑损益计算检查信息
     */
    public void setMessageAdjustment(String messageAdjustment) {
        set("messageAdjustment", messageAdjustment);
    }

    /**
     * 获取多语语言
     *
     * @return 多语语言
     */
    public String getMessageLocale() {
        return get("messageLocale");
    }

    /**
     * 设置多语语言
     *
     * @param messageLocale 多语语言
     */
    public void setMessageLocale(String messageLocale) {
        set("messageLocale", messageLocale);
    }

    /**
     * 获取最小异常日期
     *
     * @return 最小异常日期
     */
    public String getMinErrorDate() {
        return get("minErrorDate");
    }

    /**
     * 设置最小异常日期
     *
     * @param minErrorDate 最小异常日期
     */
    public void setMinErrorDate(String minErrorDate) {
        set("minErrorDate", minErrorDate);
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
     * 获取期间
     *
     * @return 期间
     */
    public String getPeriod() {
        return get("period");
    }

    /**
     * 设置期间
     *
     * @param period 期间
     */
    public void setPeriod(String period) {
        set("period", period);
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
     * 获取是否结账
     *
     * @return 是否结账
     */
    public Boolean getSettleFlag() {
        return getBoolean("settleFlag");
    }

    /**
     * 设置是否结账
     *
     * @param settleFlag 是否结账
     */
    public void setSettleFlag(Boolean settleFlag) {
        set("settleFlag", settleFlag);
    }
    /**
     * 设置是否结账
     *
     * @param settleFlag 是否结账
     */
    public void setSettleFlag(String settleFlag) {
        set("settleFlag", "Y".equals(settleFlag));
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
     * 获取日结检查结果子表集合
     *
     * @return 日结检查结果子表集合
     */
    public java.util.List<SettlementCheckResultb> SettlementCheckResultb() {
        return getBizObjects("SettlementCheckResultb", SettlementCheckResultb.class);
    }

    /**
     * 设置日结检查结果子表集合
     *
     * @param SettlementCheckResultb 日结检查结果子表集合
     */
    public void setSettlementCheckResultb(java.util.List<SettlementCheckResultb> SettlementCheckResultb) {
        setBizObjects("SettlementCheckResultb", SettlementCheckResultb);
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
