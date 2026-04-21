package com.yonyoucloud.fi.cmp.enums;

/**
 * 发布对象：组织、部门、角色、人员、用户
 */
public enum PublishedType {
    ORG((short) 1, "组织" /* "银行流水认领" */),
    DEPT((short) 2, "部门" /* "银行流水认领" */),
    ROLE((short) 3, "角色" /* "银行流水认领" */),
    EMPLOYEE((short) 4, "人员" /* "银行流水认领" */),
    USER((short) 5, "用户" /* "到账认领" */),
    ASSIGN_ORG((short) 6, "指定组织" /* "到账认领" */),
    ;

    private short code;
    private String name;

    PublishedType(short  code, String name) {
        this.code = code;
        this.name = name;
    }

    public short getCode() {
        return code;
    }

    public void setCode(short code) {
        this.code = code;
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

    public void setName(String name) {
        this.name = name;
    }

}
