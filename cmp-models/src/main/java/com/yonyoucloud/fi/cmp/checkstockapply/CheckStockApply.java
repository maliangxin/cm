package com.yonyoucloud.fi.cmp.checkstockapply;

import com.yonyou.ucf.mdd.biz.ucfbase.ucfbaseItf.IYTenant;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalFlow;
import com.yonyou.ucf.mdd.ext.base.itf.IApprovalInfo;
import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import com.yonyoucloud.fi.expansion.AccentityRawInterface;
import org.imeta.orm.base.BizObject;

/**
 * 支票入库实体
 *
 * @author u
 * @version 1.0
 */
public class CheckStockApply extends BizObject implements IAuditInfo, IApprovalInfo, ITenant, IApprovalFlow, IYTenant {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.checkstockapply.CheckStockApply";


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
     * 获取使用组织
     *
     * @return 使用组织.ID
     */
    public String getOrg() {
        return get("org");
    }

    /**
     * 设置使用组织
     *
     * @param org 使用组织.ID
     */
    public void setOrg(String org) {
        set("org", org);
    }

    /**
     * 获取单据类型
     *
     * @return 单据类型
     */
    public String getBillType() {
        return get("billType");
    }

    /**
     * 设置单据类型
     *
     * @param billType 单据类型
     */
    public void setBillType(String billType) {
        set("billType", billType);
    }

    /**
     * 获取交易类型
     *
     * @return 交易类型
     */
    public String getTradetype() {
        return get("tradetype");
    }

    /**
     * 设置交易类型
     *
     * @param tradetype 交易类型
     */
    public void setTradetype(String tradetype) {
        set("tradetype", tradetype);
    }

    /**
     * 获取审批状态
     *
     * @return 审批状态
     */
    public Short getAuditstatus() {
        return getShort("auditstatus");
    }

    /**
     * 设置审批状态
     *
     * @param auditstatus 审批状态
     */
    public void setAuditstatus(Short auditstatus) {
        set("auditstatus", auditstatus);
    }

    /**
     * 获取银行账户
     *
     * @return 银行账户.ID
     */
    public String getAccount() {
        return get("account");
    }

    /**
     * 设置银行账户
     *
     * @param account 银行账户.ID
     */
    public void setAccount(String account) {
        set("account", account);
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
     * 获取支票类型
     *
     * @return 支票类型
     */
    public Short getCheckBillType() {
        return getShort("checkBillType");
    }

    /**
     * 设置支票类型
     *
     * @param checkBillType 支票类型
     */
    public void setCheckBillType(Short checkBillType) {
        set("checkBillType", checkBillType);
    }

    /**
     * 获取起始编号
     *
     * @return 起始编号
     */
    public String getStartNo() {
        return get("startNo");
    }

    /**
     * 设置起始编号
     *
     * @param startNo 起始编号
     */
    public void setStartNo(String startNo) {
        set("startNo", startNo);
    }

    /**
     * 获取终止编号
     *
     * @return 终止编号
     */
    public String getEndNo() {
        return get("endNo");
    }

    /**
     * 设置终止编号
     *
     * @param endNo 终止编号
     */
    public void setEndNo(String endNo) {
        set("endNo", endNo);
    }

    /**
     * 获取入库张数
     *
     * @return 入库张数
     */
    public Integer getStockNum() {
        return get("stockNum");
    }

    /**
     * 设置入库张数
     *
     * @param stockNum 入库张数
     */
    public void setStockNum(Integer stockNum) {
        set("stockNum", stockNum);
    }

    /**
     * 获取支票簿编号
     *
     * @return 支票簿编号
     */
    public String getCheckBookNo() {
        return get("checkBookNo");
    }

    /**
     * 设置支票簿编号
     *
     * @param checkBookNo 支票簿编号
     */
    public void setCheckBookNo(String checkBookNo) {
        set("checkBookNo", checkBookNo);
    }

    /**
     * 获取备注
     *
     * @return 备注
     */
    public String getDescription() {
        return get("description");
    }

    /**
     * 设置备注
     *
     * @param description 备注
     */
    public void setDescription(String description) {
        set("description", description);
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
     * 获取单据状态
     *
     * @return 单据状态
     */
    public Short getStatus() {
        return getShort("status");
    }

    /**
     * 设置单据状态
     *
     * @param status 单据状态
     */
    public void setStatus(Short status) {
        set("status", status);
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
     * 获取支票收票临时表集合
     *
     * @return 支票收票临时表集合
     */
    public java.util.List<RecCheckTemp> RecCheckTemp() {
        return getBizObjects("RecCheckTemp", RecCheckTemp.class);
    }

    /**
     * 设置支票收票临时表集合
     *
     * @param RecCheckTemp 支票收票临时表集合
     */
    public void setRecCheckTemp(java.util.List<RecCheckTemp> RecCheckTemp) {
        setBizObjects("RecCheckTemp", RecCheckTemp);
    }

	/**
	 * 获取自定义项特征属性组
	 *
	 * @return 自定义项特征属性组.ID
	 */
	public String getCharacterDef() {
		return get("characterDef");
	}

    /**
     * 设置自定义项特征属性组
     *
     * @param characterDef 自定义项特征属性组.ID
     */
    public void setCharacterDef(String characterDef) {
        set("characterDef", characterDef);
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
     * 获取审批状态
     *
     * @return 审批状态
     */
    public Short getVerifystate() {
        return getShort("verifystate");
    }

    /**
     * 设置审批状态
     *
     * @param verifystate 审批状态
     */
    public void setVerifystate(Short verifystate) {
        set("verifystate", verifystate);
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
     * 获取支票类型标识
     *
     * @return 支票类型标识
     */
    public Short getChequeType() {
        return getShort("chequeType");
    }

    /**
     * 设置支票类型标识
     *
     * @param chequeType 支票类型标识
     */
    public void setChequeType(Short chequeType) {
        set("chequeType", chequeType);
    }


    @Override
    public String getYTenant() {
        return get("ytenant");
    }

    @Override
    public void setYTenant(String ytenant) {
        set("ytenant", ytenant);
    }

    /**
     * 获取生成方式
     *
     * @return 生成方式
     */
    public Short getGenerationType() {
        return getShort("generationType");
    }

    /**
     * 设置生成方式
     *
     * @param generationType 生成方式
     */
    public void setGenerationType(Short generationType) {
        set("generationType", generationType);
    }
}
