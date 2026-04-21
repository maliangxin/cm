package com.yonyoucloud.fi.cmp.withholding;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 利息测算表实体
 *
 * @author u
 * @version 1.0
 */
public class InterestCalculation extends BizObject implements IAuditInfo, ITenant, IYTenant {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.withholding.InterestCalculation";
    private static final long serialVersionUID = -8084268203594321967L;

    /**
     * 获取存款利率(年)
     *
     * @return 存款利率(年)
     */
    public java.math.BigDecimal getAnnualInterestRate() {
        return get("annualInterestRate");
    }

    /**
     * 设置存款利率(年)
     *
     * @param annualInterestRate 存款利率(年)
     */
    public void setAnnualInterestRate(java.math.BigDecimal annualInterestRate) {
        set("annualInterestRate", annualInterestRate);
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
     * 获取历史余额id
     *
     * @return 历史余额id
     */
    public Long getBalanceid() {
        return get("balanceid");
    }

    /**
     * 设置历史余额id
     *
     * @param balanceid 历史余额id
     */
    public void setBalanceid(Long balanceid) {
        set("balanceid", balanceid);
    }
    /**
     * 获取存款利息
     *
     * @return 存款利息
     */
    public java.math.BigDecimal getDepositInterest() {
        return get("depositInterest");
    }

    /**
     * 设置存款利息
     *
     * @param depositInterest 存款利息
     */
    public void setDepositInterest(java.math.BigDecimal depositInterest) {
        set("depositInterest", depositInterest);
    }

    /**
     * 获取当日存款余额
     *
     * @return 当日存款余额
     */
    public java.math.BigDecimal getDepositbal() {
        return get("depositbal");
    }

    /**
     * 设置当日存款余额
     *
     * @param depositbal 当日存款余额
     */
    public void setDepositbal(java.math.BigDecimal depositbal) {
        set("depositbal", depositbal);
    }

    /**
     * 获取日期
     *
     * @return 日期
     */
    public java.util.Date getInterestDate() {
        return get("interestDate");
    }

    /**
     * 设置日期
     *
     * @param interestDate 日期
     */
    public void setInterestDate(java.util.Date interestDate) {
        set("interestDate", interestDate);
    }

    /**
     * 获取计息天数
     *
     * @return 计息天数
     */
    public String getInterestDays() {
        return get("interestDays");
    }

    /**
     * 设置计息天数
     *
     * @param interestDays 计息天数
     */
    public void setInterestDays(String interestDays) {
        set("interestDays", interestDays);
    }

    /**
     * 获取存款计息积数
     *
     * @return 存款计息积数
     */
    public java.math.BigDecimal getInterestacc() {
        return get("interestacc");
    }

    /**
     * 设置存款计息积数
     *
     * @param interestacc 存款计息积数
     */
    public void setInterestacc(java.math.BigDecimal interestacc) {
        set("interestacc", interestacc);
    }

    /**
     * 获取是否已预提
     *
     * @return 是否已预提
     */
    public Boolean getIsWithholding() {
        return getBoolean("isWithholding");
    }

    /**
     * 设置是否已预提
     *
     * @param isWithholding 是否已预提
     */
    public void setIsWithholding(Boolean isWithholding) {
        set("isWithholding", isWithholding);
    }

    /**
     * 获取预提规则设置id
     *
     * @return 预提规则设置id.ID
     */
    public Long getMainid() {
        return get("mainid");
    }

    /**
     * 设置预提规则设置id
     *
     * @param mainid 预提规则设置id.ID
     */
    public void setMainid(Long mainid) {
        set("mainid", mainid);
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
     * 获取透支利息
     *
     * @return 透支利息
     */
    public java.math.BigDecimal getOverdraftInterest() {
        return get("overdraftInterest");
    }

    /**
     * 设置透支利息
     *
     * @param overdraftInterest 透支利息
     */
    public void setOverdraftInterest(java.math.BigDecimal overdraftInterest) {
        set("overdraftInterest", overdraftInterest);
    }

    /**
     * 获取透支利率(年)
     *
     * @return 透支利率(年)
     */
    public java.math.BigDecimal getOverdraftRate() {
        return get("overdraftRate");
    }

    /**
     * 设置透支利率(年)
     *
     * @param overdraftRate 透支利率(年)
     */
    public void setOverdraftRate(java.math.BigDecimal overdraftRate) {
        set("overdraftRate", overdraftRate);
    }

    /**
     * 获取透支计息积数
     *
     * @return 透支计息积数
     */
    public java.math.BigDecimal getOverdraftacc() {
        return get("overdraftacc");
    }

    /**
     * 设置透支计息积数
     *
     * @param overdraftacc 透支计息积数
     */
    public void setOverdraftacc(java.math.BigDecimal overdraftacc) {
        set("overdraftacc", overdraftacc);
    }

    /**
     * 获取当日透支余额
     *
     * @return 当日透支余额
     */
    public java.math.BigDecimal getOverdraftbal() {
        return get("overdraftbal");
    }

    /**
     * 设置当日透支余额
     *
     * @param overdraftbal 当日透支余额
     */
    public void setOverdraftbal(java.math.BigDecimal overdraftbal) {
        set("overdraftbal", overdraftbal);
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
    public java.math.BigDecimal getTotalInterestAmount() {
        return get("totalInterestAmount");
    }

    /**
     * 设置合计利息
     *
     * @param totalInterestAmount 合计利息
     */
    public void setTotalInterestAmount(java.math.BigDecimal totalInterestAmount) {
        set("totalInterestAmount", totalInterestAmount);
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
     * 获取是否金融中台计算
     *
     * @return 是否金融中台计算
     */
    public Boolean getIsCaculate() {
        return getBoolean("isCaculate");
    }

    /**
     * 设置是否金融中台计算
     *
     * @param isCaculate 是否金融中台计算
     */
    public void setIsCaculate(Boolean isCaculate) {
        set("isCaculate", isCaculate);
    }

    /**
     * 获取利息类型
     *
     * @return 利息类型
     */
    public String getDeposittype() {
        return get("deposittype");
    }

    /**
     * 设置利息类型
     *
     * @param deposittype 利息类型
     */
    public void setDeposittype(String deposittype) {
        set("deposittype", deposittype);
    }

    /**
     * 获取天数
     *
     * @return 天数
     */
    public String getDays() {
        return get("days");
    }

    /**
     * 设置天数
     *
     * @param days 天数
     */
    public void setDays(String days) {
        set("days", days);
    }

    /**
     * 获取截止日期
     *
     * @return 截止日期
     */
    public java.util.Date getEndDate() {
        return get("endDate");
    }

    /**
     * 设置截止日期
     *
     * @param endDate 截止日期
     */
    public void setEndDate(java.util.Date endDate) {
        set("endDate", endDate);
    }
    /**
     * 获取 计息方式
     *
     * @return  计息方式
     */
    public Short getAgreeinterestmethod() {
        return getShort("agreeinterestmethod");
    }

    /**
     * 设置 计息方式
     *
     * @param agreeinterestmethod  计息方式
     */
    public void setAgreeinterestmethod(Short agreeinterestmethod) {
        set("agreeinterestmethod", agreeinterestmethod);
    }

    /**
     * 获取存款日均余额
     *
     * @return 存款日均余额
     */
    public java.math.BigDecimal getDepositbalavg() {
        return get("depositbalavg");
    }

    /**
     * 设置存款日均余额
     *
     * @param depositbalavg 存款日均余额
     */
    public void setDepositbalavg(java.math.BigDecimal depositbalavg) {
        set("depositbalavg", depositbalavg);
    }

    /**
     * 获取存款分档编号
     *
     * @return 存款分档编号
     */
    public Long getGradenum() {
        return get("gradenum");
    }

    /**
     * 设置存款分档编号
     *
     * @param gradenum 存款分档编号
     */
    public void setGradenum(Long gradenum) {
        set("gradenum", gradenum);
    }

    /**
     * 获取存款本金金额
     *
     * @return 存款本金金额
     */
    public java.math.BigDecimal getPrincipal() {
        return get("principal");
    }

    /**
     * 设置存款本金金额
     *
     * @param principal 存款本金金额
     */
    public void setPrincipal(java.math.BigDecimal principal) {
        set("principal", principal);
    }
    /**
     * 获取存款基准利率
     *
     * @return 存款基准利率
     */
    public java.math.BigDecimal getBaseir() {
        return get("baseir");
    }

    /**
     * 设置存款基准利率
     *
     * @param baseir 存款基准利率
     */
    public void setBaseir(java.math.BigDecimal baseir) {
        set("baseir", baseir);
    }


    /**
     * 获取存款利率浮动值(加减点)
     *
     * @return 存款利率浮动值(加减点)
     */
    public java.math.BigDecimal getFloatvalue() {
        return get("floatvalue");
    }

    /**
     * 设置存款利率浮动值(加减点)
     *
     * @param floatvalue 存款利率浮动值(加减点)
     */
    public void setFloatvalue(java.math.BigDecimal floatvalue) {
        set("floatvalue", floatvalue);
    }
}
