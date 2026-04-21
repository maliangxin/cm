package com.yonyoucloud.fi.cmp.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * <h1>新架构对接业务事项相关监听枚举值</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2022/5/17 7:32
 */
public enum EventCenterEnum {
    /*
     * <h2>发送业务事项过账消息事件源</h2>
     */
    EVENT_ENTRIES("EEAC", "eeac_busievent_msg"),
    /*
     * <h2>发送业务事项过账回退地址</h2>
     */
    EVENT_VOUCHER_DELETE("EEAC", "yonbip-fi-eeac"),
    /*
     * <h2>监听业务事项过账处理结果消息事件源</h2>
     */
    EVENT_STATUS_BACK_WRITE("EEAC", "eeac_busievent_rep"),
    /*
     * <h2>监听业务事项过账处理凭证号回写消息事件源</h2>
     */
    EVENT_VOUCHER_NO_BACK_WRITE("EAAI", "eaai_voucher_writeback_msg"),
    /*
     * <h2>发送会计事项过账消息事件源</h2>
     */
    EVENT_VOUCHER_ENTRIES("EAAI", "eaai_fievent_msg"),

    /*
     * 会计平台回写凭证状态的事件源
     */
    EVENT_VOUCHER_STATUS_CHANNEL_CMP("otp", "voucher-status-channel-cmp"),

    /*
     * 结算明细办结事件源
     */
    EVENT_SETTLEDETAIL_FINISHEVENT("STWB", "settleDetailFinishEvent");

    private String sourceId;
    private String eventType;
    private static HashMap<String, EventCenterEnum> map = null;

    private EventCenterEnum(String sourceId, String eventType) {
        this.sourceId = sourceId;
        this.eventType = eventType;
    }

    public String getSourceId() {
        return this.sourceId;
    }

    public String getEventType() {
        return this.eventType;
    }

    private static synchronized void initMap() {
        if (map == null) {
            map = new HashMap();
            EventCenterEnum[] items = values();
            EventCenterEnum[] var1 = items;
            int var2 = items.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                EventCenterEnum item = var1[var3];
                map.put(item.getEventType(), item);
            }

        }
    }

    public static EventCenterEnum find(String eventType) {
        if (StringUtils.isBlank(eventType)) {
            return null;
        } else {
            if (map == null) {
                initMap();
            }

            return map.get(eventType);
        }
    }
}
