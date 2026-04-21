package com.yonyoucloud.fi.cmp.fundpayment;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 资金付款单子表与预提单关联关系表实体
 *
 * @author u
 * @version 1.0
 */
/**
 * 资金付款单子表与预提单关联关系表实体
 *
 * @author u
 * @version 1.0
 */
public class FundPaymentSubWithholdingRelation extends BizObject implements ITenant, IYTenant, AccentityRawInterface {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.fundpayment.FundPaymentSubWithholdingRelation";

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
     * 获取预提单号
     *
     * @return 预提单号
     */
    public String getCode() {
        return get("code");
    }

    /**
     * 设置预提单号
     *
     * @param code 预提单号
     */
    public void setCode(String code) {
        set("code", code);
    }

    /**
     * 获取币种id
     *
     * @return 币种id.ID
     */
    public String getCurrency() {
        return get("currency");
    }

    /**
     * 设置币种id
     *
     * @param currency 币种id.ID
     */
    public void setCurrency(String currency) {
        set("currency", currency);
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
     * 获取资金付款单子表id
     *
     * @return 资金付款单子表id.ID
     */
    public Long getFundpaymentsubid() {
        return getLong("fundpaymentsubid");
    }

    /**
     * 设置资金付款单子表id
     *
     * @param fundpaymentsubid 资金付款单子表id.ID
     */
    public void setFundpaymentsubid(Long fundpaymentsubid) {
        set("fundpaymentsubid", fundpaymentsubid);
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
     * 获取预提单id
     *
     * @return 预提单id
     */
    public String getWithholdingid() {
        return get("withholdingid");
    }

    /**
     * 设置预提单id
     *
     * @param withholdingid 预提单id
     */
    public void setWithholdingid(String withholdingid) {
        set("withholdingid", withholdingid);
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
