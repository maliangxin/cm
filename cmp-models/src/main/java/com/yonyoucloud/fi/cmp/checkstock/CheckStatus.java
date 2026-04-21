package com.yonyoucloud.fi.cmp.checkstock;

import com.yonyou.ucf.mdd.ext.base.itf.IAuditInfo;
import com.yonyou.ucf.mdd.ext.base.itf.ITenant;
import org.imeta.orm.base.BizObject;

/**
 * 支票状态记录实体
 *
 * @author u
 * @version 1.0
 */
public class CheckStatus extends BizObject implements IAuditInfo, ITenant {
    // 实体全称
    public static final String ENTITY_NAME = "cmp.checkstock.CheckStatus";

    /**
     * 获取支票库存id
     *
     * @return 支票库存id
     */
    public Long getCheckId() {
        return get("checkId");
    }

    /**
     * 设置支票库存id
     *
     * @param checkId 支票库存id
     */
    public void setCheckId(Long checkId) {
        set("checkId", checkId);
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
     * 获取支票编号
     *
     * @return 支票编号
     */
    public String getCheckBillNo() {
        return get("checkBillNo");
    }

    /**
     * 设置支票编号
     *
     * @param checkBillNo 支票编号
     */
    public void setCheckBillNo(String checkBillNo) {
        set("checkBillNo", checkBillNo);
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
     * 获取更新前支票状态
     *
     * @return 更新前支票状态
     */
    public String getBeforeCheckBillStatus() {
        return get("beforeCheckBillStatus");
    }

    /**
     * 设置更新前支票状态
     *
     * @param beforeCheckBillStatus 更新前支票状态
     */
    public void setBeforeCheckBillStatus(String beforeCheckBillStatus) {
        set("beforeCheckBillStatus", beforeCheckBillStatus);
    }

    /**
     * 获取更新后支票状态
     *
     * @return 更新后支票状态
     */
    public String getAfterCheckBillStatus() {
        return get("afterCheckBillStatus");
    }

    /**
     * 设置更新后支票状态
     *
     * @param afterCheckBillStatus 更新后支票状态
     */
    public void setAfterCheckBillStatus(String afterCheckBillStatus) {
        set("afterCheckBillStatus", afterCheckBillStatus);
    }

    /**
     * 获取支票方向
     *
     * @return 支票方向
     */
    public String getCheckBillDir() {
        return get("checkBillDir");
    }

    /**
     * 设置支票方向
     *
     * @param checkBillDir 支票方向
     */
    public void setCheckBillDir(String checkBillDir) {
        set("checkBillDir", checkBillDir);
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
     * 获取操作日期
     *
     * @return 操作日期
     */
    public java.util.Date getOperatorDate() {
        return get("operatorDate");
    }

    /**
     * 设置操作日期
     *
     * @param operatorDate 操作日期
     */
    public void setOperatorDate(java.util.Date operatorDate) {
        set("operatorDate", operatorDate);
    }

    /**
     * 获取操作人
     *
     * @return 操作人
     */
    public String getOperator() {
        return get("operator");
    }

    /**
     * 设置操作人
     *
     * @param operator 操作人
     */
    public void setOperator(String operator) {
        set("operator", operator);
    }

    /**
     * 获取操作人部门
     *
     * @return 操作人部门
     */
    public String getOperatorDept() {
        return get("operatorDept");
    }

    /**
     * 设置操作人部门
     *
     * @param operatorDept 操作人部门
     */
    public void setOperatorDept(String operatorDept) {
        set("operatorDept", operatorDept);
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

}
