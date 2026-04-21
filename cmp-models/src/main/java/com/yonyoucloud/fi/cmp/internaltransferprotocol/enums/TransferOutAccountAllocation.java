package com.yonyoucloud.fi.cmp.internaltransferprotocol.enums;

import lombok.Getter;

import java.util.HashMap;

/**
 * <h1>转出账户分配</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-08 13:58
 */
public enum TransferOutAccountAllocation {

    WITH_FRONT_END_BUSINESS("UID:P_CM-BE_1A033EB805B80019", (short) 0),
    SETUP_ACCOUNT_MANUALLY("UID:P_CM-BE_1A033EB805B8001A", (short) 1);

    private static HashMap<Short, TransferOutAccountAllocation> map = null;
    private final String name;
    @Getter
    private final short value;

    TransferOutAccountAllocation(String name, short value) {
        this.name = name;
        this.value = value;
    }

    private static synchronized void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        TransferOutAccountAllocation[] items = TransferOutAccountAllocation.values();
        for (TransferOutAccountAllocation item : items) {
            map.put(item.getValue(), item);
        }
    }

    public static TransferOutAccountAllocation find(Number value) {
        if (value == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(value.shortValue());
    }

    public String getName() {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(name,name) /* name */;
    }

}
