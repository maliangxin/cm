
package com.yonyoucloud.fi.cmp.reconciliation;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

import java.util.Date;

/**
 * 银企对账勾对关系记录表实体
 *
 * @author wxb 20251113
 * @version 1.0
 */
public class ReconciliationMatchRecord extends BizObject implements IYTenant, ITenant, IAuditInfo {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.reconciliation.ReconciliationMatchRecord";

    /**
     * 获取对账组织
     *
     * @return 对账组织.ID
     */
    public String getAccentity() {
        return get("accentity");
    }

    /**
     * 设置对账组织
     *
     * @param accentity 对账组织.ID
     */
    public void setAccentity(String accentity) {
        set("accentity", accentity);
    }

    /**
     * 获取对账方案
     *
     * @return 对账方案.ID
     */
    public String getReconciliationScheme() {
        return get("reconciliationScheme");
    }

    /**
     * 设置对账方案
     *
     * @param reconciliationScheme 对账方案.ID
     */
    public void setReconciliationScheme(String reconciliationScheme) {
        set("reconciliationScheme", reconciliationScheme);
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
     * 获取勾对号
     *
     * @return 勾对号
     */
    public String getCheckno() {
        return get("checkno");
    }

    /**
     * 设置勾对号
     *
     * @param checkno 勾对号
     */
    public void setCheckno(String checkno) {
        set("checkno", checkno);
    }

    /**
     * 获取数据来源：1凭证;2银行日记账;3银行流水
     *
     * @return 数据来源
     */
    public Short getDataSource() {
        return getShort("dataSource");
    }

    /**
     * 设置数据来源：1凭证;2银行日记账;3银行流水
     *
     * @param dataSource 数据来源
     */
    public void setDataSource(Short dataSource) {
        set("dataSource", dataSource);
    }

    /**
     * 获取凭证分录ID
     *
     * @return 凭证分录ID
     */
    public String getVoucherId() {
        return get("voucherId");
    }

    /**
     * 设置凭证分录ID
     *
     * @param voucherId 凭证分录ID
     */
    public void setVoucherId(String voucherId) {
        set("voucherId", voucherId);
    }

    /**
     * 获取日记账ID
     *
     * @return 日记账ID
     */
    public String getJournalId() {
        return get("journalId");
    }

    /**
     * 设置日记账ID
     *
     * @param journalId 日记账ID
     */
    public void setJournalId(String journalId) {
        set("journalId", journalId);
    }

    /**
     * 获取记账日期
     *
     * @return 记账日期
     */
    public Date getAccountingDate() {
        return get("accountingDate");
    }

    /**
     * 设置记账日期
     *
     * @param accountingDate 记账日期
     */
    public void setAccountingDate(Date accountingDate) {
        set("accountingDate", accountingDate);
    }

    /**
     * 获取银行流水ID
     *
     * @return 银行流水ID
     */
    public String getBankreconciliationId() {
        return get("bankreconciliationId");
    }

    /**
     * 设置银行流水ID
     *
     * @param bankreconciliationId 银行流水ID
     */
    public void setBankreconciliationId(String bankreconciliationId) {
        set("bankreconciliationId", bankreconciliationId);
    }

    /**
     * 获取交易日期
     *
     * @return 交易日期
     */
    public Date getTranDate() {
        return get("tranDate");
    }

    /**
     * 设置交易日期
     *
     * @param tranDate 交易日期
     */
    public void setTranDate(Date tranDate) {
        set("tranDate", tranDate);
    }

    /**
     * 获取勾对日期
     *
     * @return 勾对日期
     */
    public Date getCheckDate() {
        return get("checkDate");
    }

    /**
     * 设置勾对日期
     *
     * @param checkDate 勾对日期
     */
    public void setCheckDate(Date checkDate) {
        set("checkDate", checkDate);
    }

    /**
     * 获取勾对时间
     *
     * @return 勾对时间
     */
    public Date getCheckTime() {
        return get("checkTime");
    }

    /**
     * 设置勾对时间
     *
     * @param checkTime 勾对时间
     */
    public void setCheckTime(Date checkTime) {
        set("checkTime", checkTime);
    }

    /**
     * 获取对账依据
     *
     * @return 对账依据
     */
    public Short getReconciliationBasis() {
        return getShort("reconciliationBasis");
    }

    /**
     * 设置对账依据
     *
     * @param reconciliationBasis 对账依据
     */
    public void setReconciliationBasis(Short reconciliationBasis) {
        set("reconciliationBasis", reconciliationBasis);
    }

    /**
     * 获取关键要素-日期浮动
     *
     * @return 关键要素-日期浮动
     */
    public Integer getDateFloat() {
        return get("dateFloat");
    }

    /**
     * 设置关键要素-日期浮动
     *
     * @param dateFloat 关键要素-日期浮动
     */
    public void setDateFloat(Integer dateFloat) {
        set("dateFloat", dateFloat);
    }

    /**
     * 获取关键要素-摘要匹配方式
     *
     * @return 关键要素-摘要匹配方式
     */
    public Short getRemarkMatchMethod() {
        return getShort("remarkMatchMethod");
    }

    /**
     * 设置关键要素-摘要匹配方式
     *
     * @param remarkMatchMethod 关键要素-摘要匹配方式
     */
    public void setRemarkMatchMethod(Short remarkMatchMethod) {
        set("remarkMatchMethod", remarkMatchMethod);
    }

    /**
     * 获取关键要素-票据号匹配方式
     *
     * @return 关键要素-票据号匹配方式
     */
    public Short getNotenoMatchMethod() {
        return getShort("notenoMatchMethod");
    }

    /**
     * 设置关键要素-票据号匹配方式
     *
     * @param notenoMatchMethod 关键要素-票据号匹配方式
     */
    public void setNotenoMatchMethod(Short notenoMatchMethod) {
        set("notenoMatchMethod", notenoMatchMethod);
    }

    /**
     * 获取关键要素-对方名称匹配方式
     *
     * @return 关键要素-对方名称匹配方式
     */
    public Short getOthernameMatchMethod() {
        return getShort("othernameMatchMethod");
    }

    /**
     * 设置关键要素-对方名称匹配方式
     *
     * @param othernameMatchMethod 关键要素-对方名称匹配方式
     */
    public void setOthernameMatchMethod(Short othernameMatchMethod) {
        set("othernameMatchMethod", othernameMatchMethod);
    }

    /**
     * 获取关键要素-相同数据匹配方式
     *
     * @return 关键要素-相同数据匹配方式
     */
    public Short getSamedataMatchMethod() {
        return getShort("samedataMatchMethod");
    }

    /**
     * 设置关键要素-相同数据匹配方式
     *
     * @param samedataMatchMethod 关键要素-相同数据匹配方式
     */
    public void setSamedataMatchMethod(Short samedataMatchMethod) {
        set("samedataMatchMethod", samedataMatchMethod);
    }

    /**
     * 获取封存标识
     *
     * @return 封存标识
     */
    public Boolean getSealFlag() {
        return getBoolean("sealFlag");
    }

    /**
     * 设置封存标识
     *
     * @param sealFlag 封存标识
     */
    public void setSealFlag(Boolean sealFlag) {
        set("sealFlag", sealFlag);
    }

    /**
     * 获取勾对人
     *
     * @return 勾对人.ID
     */
    public Long getCheckOperator() {
        return get("checkOperator");
    }

    /**
     * 设置勾对人
     *
     * @param checkOperator 勾对人.ID
     */
    public void setCheckOperator(Long checkOperator) {
        set("checkOperator", checkOperator);
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public Date getCreateTime() {
        return get("createTime");
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(Date createTime) {
        set("createTime", createTime);
    }

    @Override
    public Date getCreateDate() {
        return get("createDate");
    }

    @Override
    public void setCreateDate(Date createDate) {
        set("createDate", createDate);
    }

    /**
     * 获取创建人
     *
     * @return 创建人.ID
     */
    public String getCreator() {
        return get("creator");
    }

    /**
     * 设置创建人
     *
     * @param creator 创建人.ID
     */
    public void setCreator(String creator) {
        set("creator", creator);
    }

    /**
     * 获取修改人
     *
     * @return 修改人.ID
     */
    public String getModifier() {
        return get("modifier");
    }

    /**
     * 设置修改人
     *
     * @param modifier 修改人.ID
     */
    public void setModifier(String modifier) {
        set("modifier", modifier);
    }

    /**
     * 获取修改时间
     *
     * @return 修改时间
     */
    public Date getModifyTime() {
        return get("modifyTime");
    }

    /**
     * 设置修改时间
     *
     * @param modifyTime 修改时间
     */
    public void setModifyTime(Date modifyTime) {
        set("modifyTime", modifyTime);
    }

    @Override
    public Date getModifyDate() {
        return get("modifyDate");
    }

    @Override
    public void setModifyDate(Date modifyDate) {
        set("modifyDate", modifyDate);
    }

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
    public Date getPubts() {
        return get("pubts");
    }

    /**
     * 设置时间戳
     *
     * @param pubts 时间戳
     */
    public void setPubts(Date pubts) {
        set("pubts", pubts);
    }

    /**
     * 获取ID
     *
     * @return ID
     */
    public Long getId() {
        return get("id");
    }

    /**
     * 设置ID
     *
     * @param id ID
     */
    public void setId(Long id) {
        set("id", id);
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
}