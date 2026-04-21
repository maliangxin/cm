package com.yonyoucloud.fi.cmp.enums;

import java.util.HashMap;

import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * <h1>FiEventActionEnum</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022/5/17 8:06
 */
public enum FiEventActionEnum {
    ADD(MessageUtils.getMessage("P_YS_FED_FW_0000021020") /* "新增" */, "add"),
    DELETE(MessageUtils.getMessage("P_YS_FED_EXAMP_0000019871") /* "删除" */, "delete");

    private String name;
    private String value;
    private static HashMap<String, FiEventActionEnum> map = null;

    private FiEventActionEnum(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    private static synchronized void initMap() {
        if (map == null) {
            map = new HashMap();
            FiEventActionEnum[] items = values();
            FiEventActionEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                FiEventActionEnum item = var1[var3];
                map.put(item.getValue(), item);
            }

        }
    }

    public static FiEventActionEnum find(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        } else {
            if (map == null) {
                initMap();
            }

            return (FiEventActionEnum)map.get(value);
        }
    }
}
