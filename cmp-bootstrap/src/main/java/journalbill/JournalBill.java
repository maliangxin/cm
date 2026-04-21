package journalbill;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IAutoCode;
import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.*;
import com.yonyou.ucf.mdd.ext.voucher.base.ICurrency;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 日记账录入主体表
 */
public class JournalBill extends BizObject implements IAuditInfo, ITenant, ICurrency, IApprovalFlow, IPrintCount, IYTenant, AccentityRawInterface, IAutoCode, IApprovalInfo {
    // 业务对象编码
    public static final String BUSI_OBJ_CODE = "ctm-cmp.cmp_journalbill";
    private static final long serialVersionUID = 1L;

    /**
     * 实体全称
     */
    public static final String ENTITY_NAME = "cmp.journalbill.JournalBill";

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
     * 获取单据日期
     */
    public java.util.Date getBillDate() {
        return get("billDate");
    }

    /**
     * 获取单据日期
     * @param billDate
     */
    public void setBillDate(java.util.Date billDate) {
        set("billDate", billDate);
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
     * 获取部门
     *
     * @return 部门.ID
     */
    public String getDept() {
        return get("dept");
    }

    /**
     * 设置部门
     *
     * @param dept 部门.ID
     */
    public void setDept(String dept) {
        set("dept", dept);
    }

    /**
     * 获取生成方式
     * @return
     */
    public Short getGenerateType() {
        return getShort("generateType");
    }

    /**
     * 设置生成方式
     * @param generateType
     */
    public void setGenerateType(Short generateType) {
        set("generateType", generateType);
    }

    /**
     * 获取租户id
     *
     * @return 租户id
     */
    public String getYTenant() {
        return get("ytenant");
    }

    /**
     * 设置租户id
     *
     * @param ytenant 租户id
     */
    public void setYTenant(String ytenant) {
        set("ytenant", ytenant);
    }

    /**
     * 获取Y租户Id
     *
     * @return Y租户Id
     */
    public String getYtenantId() {
        return get("ytenantId");
    }

    /**
     * 设置Y租户Id
     *
     * @param ytenantId Y租户Id
     */
    public void setYtenantId(String ytenantId) {
        set("ytenantId", ytenantId);
    }

    /**
     * 获取审批流控制
     *
     * @return 审批流控制
     */
    public Boolean getIsWfControlled() {
        return getBoolean("isWfControlled");
    }

    /**
     * 设置审批流控制
     *
     * @param isWfControlled 审批流控制
     */
    public void setIsWfControlled(Boolean isWfControlled) {
        set("isWfControlled", isWfControlled);
    }

    /**
     * 获取审批流状态
     *
     * @return 审批流状态
     */
    public Short getVerifystate() {
        return getShort("verifystate");
    }

    /**
     * 设置审批流状态
     *
     * @param verifystate 审批流状态
     */
    public void setVerifystate(Short verifystate) {
        set("verifystate", verifystate);
    }

    /**
     * 获取返回总数
     *
     * @return 返回总数
     */
    public Short getReturncount() {
        return getShort("returncount");
    }

    /**
     * 设置返回总数
     *
     * @param returncount 返回总数
     */
    public void setReturncount(Short returncount) {
        set("returncount", returncount);
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
     * 获取创建人
     *
     * @return 创建人.ID
     */
    public Long getCreatorId() {
        return getLong("creatorId");
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

    @Override
    public Integer getPrintCount() {
        return get("printCount");
    }

    @Override
    public void setPrintCount(Integer printCount) {
        set("printCount", printCount);
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
     * 获取日记账录入子表集合
     *
     * @return 日记账录入子表集合
     */
    public java.util.List<JournalBill_b> JournalBill_b() {
        return getBizObjects("JournalBill_b", JournalBill_b.class);
    }

    /**
     * 设置日记账录入子表集合
     *
     * @param JournalBill_b 日记账录入子表集合
     */
    public void setJournalBill_b(java.util.List<JournalBill_b> JournalBill_b) {
        setBizObjects("JournalBill_b", JournalBill_b);
    }

}
