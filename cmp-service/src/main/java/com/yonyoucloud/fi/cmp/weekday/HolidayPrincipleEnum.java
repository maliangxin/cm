package com.yonyoucloud.fi.cmp.weekday;

import java.time.LocalDate;
import java.util.HashMap;

/**
 * @author shiqhs
 * @date 2021/11/19
 * @description RL001-节假日处理规则
 */
public enum HolidayPrincipleEnum {

    /**
     * 正常
     */
    NORMAL("normal") {
        @Override
        public LocalDate dateTick(LocalDate date) {
            return date;
        }

        @Override
        public LocalDate weekendHandle(LocalDate date) {
            return date;
        }

        @Override
        public DoubleNode<RuleWeek> pointerTick(DoubleNode<RuleWeek> pointer) {
            return pointer;
        }
    },

    /**
     * 提前
     */
    ADVANCE("advance") {
        @Override
        public LocalDate dateTick(LocalDate date) {
            return date.minusDays(1);
        }

        @Override
        public LocalDate weekendHandle(LocalDate date) {
            switch (date.getDayOfWeek()) {
                case SATURDAY:
                    return date.minusDays(1);
                case SUNDAY:
                    return date.minusDays(2);
                default:
                    return date;
            }
        }

        @Override
        public DoubleNode<RuleWeek> pointerTick(DoubleNode<RuleWeek> pointer) {
            pointer = pointer.prev;
            // 头节点
            if (pointer.data == null) {
                pointer = pointer.prev;
            }
            return pointer;
        }
    },

    /**
     * 延后
     */
    DELAY("delayed") {
        @Override
        public LocalDate dateTick(LocalDate date) {
            return date.plusDays(1);
        }

        @Override
        public LocalDate weekendHandle(LocalDate date) {
            switch (date.getDayOfWeek()) {
                case SATURDAY:
                    return date.plusDays(2);
                case SUNDAY:
                    return date.plusDays(1);
                default:
                    return date;
            }
        }

        @Override
        public DoubleNode<RuleWeek> pointerTick(DoubleNode<RuleWeek> pointer) {
            pointer = pointer.next;
            // 头节点
            if (pointer.data == null) {
                pointer = pointer.next;
            }
            return pointer;
        }
    };

    /**
     * 日期移动，提前或延后1天
     * @param date 日期
     * @return 日期
     */
    public abstract LocalDate dateTick(LocalDate date);

    /**
     * 周六日日期处理，提前或延后
     * @param date 日期
     * @return 日期
     */
    public abstract LocalDate weekendHandle(LocalDate date);

    /**
     * 指针移动,向上或向下移动
     * @param pointer 指针元素
     * @return 指针元素
     */
    public abstract DoubleNode<RuleWeek> pointerTick(DoubleNode<RuleWeek> pointer);

    private final String code;

    public String getCode(){
        return code;
    }

    private static HashMap<String, HolidayPrincipleEnum> map = null;

    private synchronized static void initMap() {
        if (map != null) {
            return;
        }
        map = new HashMap<>();
        HolidayPrincipleEnum[] items = HolidayPrincipleEnum.values();
        for (HolidayPrincipleEnum item : items) {
            map.put(item.getCode(), item);
        }
    }

    HolidayPrincipleEnum(String code){
        this.code = code;
    }

    public static HolidayPrincipleEnum find(String code) {
        if (code == null) {
            return null;
        }
        if (map == null) {
            initMap();
        }
        return map.get(code);
    }

}
