package com.yonyoucloud.fi.cmp.aop;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;

import java.util.HashMap;

/**
 * @description:数据权限-操作动作
 */
public enum DataAuthActionEnum {

    /**
     * 维护
     **/
    ACTION_BILL_MAINTAIN("bill_maintain", "维护", "维护"),
    /**
     * 编辑
     **/
    ACTION_BILL_EDIT("bill_edit", "编辑", "编辑"),
    /**
     * 增加
     **/

    ACTION_BILL_ADD("bill_add", "增加", "增加"),
    /**
     * 审计
     **/

    ACTION_BILL_AUDIT("bill_audit", "审计", "审计"),
    /**
     * 取消审计
     **/

    ACTION_BILL_CANCLE_AUDIT("bill_cancle_audit", "取消审计", "取消审计"),
    /**
     * 删除
     **/

    ACTION_BILL_DELETE("bill_delete", "删除", "删除");

    private String code;
    private String name;
    private String defaultname;

    DataAuthActionEnum(String code, String name, String defaultname) {
        this.code = code;
        this.name = name;
        this.defaultname = defaultname;

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return MessageUtils.getMessageWithDefault(this.name, this.defaultname);
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }


    private static HashMap<String, DataAuthActionEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        DataAuthActionEnum[] items = DataAuthActionEnum.values();
        for (DataAuthActionEnum item : items) {
            map.put(item.getCode(), item);
        }
    }

    public static DataAuthActionEnum getDataAuthActionEnumByCode(String code) {
        if (map == null) {
            initMap();
        }
        return map.get(code) == null ? null : map.get(code);
    }
}
