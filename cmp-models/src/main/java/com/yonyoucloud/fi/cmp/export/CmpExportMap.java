package com.yonyoucloud.fi.cmp.export;

import java.io.Serializable;


public class CmpExportMap implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 0:sap导出：1：excel导出 2：pdf导出
     */
    private Integer type;

    /**
     * 来源实体类名
     */
    private String soureceEntityname;

    /**
     * 表单
     */
    private String billno;

    /**
     * 计算因子逗号切割
     */
    private String sourceEntityattrFactors;

    /**
     * 处理类
     */
    private String evalHandler;

    /**
     * 目标名（转化的目标名）
     */
    private String targetName;

    /**
     * target_type
     */
    private String targetType;

    /**
     * 是否开启映射此字段
     */
    private boolean isOpen;

    /**
     * 精度，保留小数位
     */
    private Integer numpoint;

    /**
     * 是否四舍五入 0 不入 1 四舍五入
     */
    private boolean isRounding;


    /**
     * 扩展字段: 放固定值，计算规则 {0}=0?{1}||{0}>0?50，正则，日期格式化，默认值，或者json等你要的数据，自由发挥
     */
    private String extra1;

    /**
     * extra2
     */
    private String extra2;

    /**
     * 扩展
     */
    private String extra3;

    /**
     * 租户
     */
    private Long tenantId;

    /**
     * sort
     */
    private Integer sort;


    public CmpExportMap() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getSoureceEntityname() {
        return soureceEntityname;
    }

    public void setSoureceEntityname(String soureceEntityname) {
        this.soureceEntityname = soureceEntityname;
    }

    public String getBillno() {
        return billno;
    }

    public void setBillno(String billno) {
        this.billno = billno;
    }

    public String getSourceEntityattrFactors() {
        return sourceEntityattrFactors;
    }

    public void setSourceEntityattrFactors(String sourceEntityattrFactors) {
        this.sourceEntityattrFactors = sourceEntityattrFactors;
    }

    public String getEvalHandler() {
        return evalHandler;
    }

    public void setEvalHandler(String evalHandler) {
        this.evalHandler = evalHandler;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public Integer getNumpoint() {
        return numpoint;
    }

    public void setNumpoint(Integer numpoint) {
        this.numpoint = numpoint;
    }

    public Boolean getIsRounding() {
        return isRounding;
    }

    public void setIsRounding(Boolean isRounding) {
        this.isRounding = isRounding;
    }


    public String getExtra1() {
        return extra1;
    }

    public void setExtra1(String extra1) {
        this.extra1 = extra1;
    }

    public String getExtra2() {
        return extra2;
    }

    public void setExtra2(String extra2) {
        this.extra2 = extra2;
    }

    public String getExtra3() {
        return extra3;
    }

    public void setExtra3(String extra3) {
        this.extra3 = extra3;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
