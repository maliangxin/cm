package com.yonyoucloud.fi.cmp.weekday;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * @author shiqhs
 * @date 2022/04/27
 * @description rl001公共服务
 */
public class Rl001CommonManager {

    /**
     * 计算工作日
     * @param holidayPrinciple 节假日原则
     * @param date 日期
     * @param workingCalendar 工作日历
     * @return 工作日
     */
    public LocalDate calcWorkDate(HolidayPrincipleEnum holidayPrinciple, LocalDate date, WorkingCalendar workingCalendar) {
        CirDoublyList<RuleWeek> cirDoublyList = addWeekData(workingCalendar);
        DoubleNode<RuleWeek> pointer = searchPointer(date, cirDoublyList);
        return workingCalendarHandle(holidayPrinciple, date, pointer);
    }

    /**
     * 工作日历为空时，根据周六日计算工作日
     * @param holidayPrinciple 节假日原则
     * @param date 日期
     * @return 工作日返回值
     */
    public Rl001Resp emptyHandle(HolidayPrincipleEnum holidayPrinciple, LocalDate date) {
        LocalDate workday = holidayPrinciple.weekendHandle(date);
        return new Rl001Resp() {{
            setWorkday(workday);
        }};
    }

    /**
     * 工作日历处理
     * @param holidayPrincipleEnum rl001枚举
     * @param date 日期
     * @param pointer 指针元素
     * @return 工作日
     */
    private LocalDate workingCalendarHandle(HolidayPrincipleEnum holidayPrincipleEnum, LocalDate date, DoubleNode<RuleWeek> pointer) {
        for (int i = 0; i < DayOfWeek.values().length; i++) {
            if (Boolean.TRUE.equals(pointer.data.getIsWorkDate())) {
                return date;
            }
            date = holidayPrincipleEnum.dateTick(date);
            pointer = holidayPrincipleEnum.pointerTick(pointer);
        }
        return date;
    }

    /**
     * 搜索日期对应星期在链表中的指针元素
     * @param date 日期
     * @param cirDoublyList 循环双向链表
     * @return 指针元素
     */
    private DoubleNode<RuleWeek> searchPointer(LocalDate date, CirDoublyList<RuleWeek> cirDoublyList) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        DoubleNode<RuleWeek> pointer = cirDoublyList.head.next;
        while (pointer.data.getDayOfWeek() != null) {
            if (dayOfWeek == pointer.data.getDayOfWeek()) {
                break;
            }
            pointer = pointer.next;
        }
        return pointer;
    }

    /**
     * 添加星期信息
     * @param workingCalendar 工作日历
     * @return 星期双链表
     */
    private CirDoublyList<RuleWeek> addWeekData(WorkingCalendar workingCalendar) {
        CirDoublyList<RuleWeek> cirDoublyList = new CirDoublyList<>();
        cirDoublyList.insert(new RuleWeek() {{
            setDayOfWeek(DayOfWeek.MONDAY);
            setIsWorkDate(workingCalendar.getIsmonday());
        }});
        cirDoublyList.insert(new RuleWeek() {{
            setDayOfWeek(DayOfWeek.TUESDAY);
            setIsWorkDate(workingCalendar.getIstuesday());
        }});
        cirDoublyList.insert(new RuleWeek() {{
            setDayOfWeek(DayOfWeek.WEDNESDAY);
            setIsWorkDate(workingCalendar.getIswednesday());
        }});
        cirDoublyList.insert(new RuleWeek() {{
            setDayOfWeek(DayOfWeek.THURSDAY);
            setIsWorkDate(workingCalendar.getIsthursday());
        }});
        cirDoublyList.insert(new RuleWeek() {{
            setDayOfWeek(DayOfWeek.FRIDAY);
            setIsWorkDate(workingCalendar.getIsfriday());
        }});
        cirDoublyList.insert(new RuleWeek() {{
            setDayOfWeek(DayOfWeek.SATURDAY);
            setIsWorkDate(workingCalendar.getIssaturday());
        }});
        cirDoublyList.insert(new RuleWeek() {{
            setDayOfWeek(DayOfWeek.SUNDAY);
            setIsWorkDate(workingCalendar.getIssunday());
        }});
        return cirDoublyList;
    }
}
