package com.yonyoucloud.fi.cmp.weekday;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author shiqhs
 * @date 2022/04/28
 * @description RL001 假日原则延后服务类
 */
@Service
@RequiredArgsConstructor
public class CmpRl001DelayManager {

    private final CmpWorkingCalendarManager   workingCalendarManager;

    /**
     * 延后
     * @param workingCalendar 工作日历
     * @param holidayList 节假日列表
     * @param inHolidayOptional 在节假日区间optional
     * @param date 日期
     * @return 工作日
     */
    public Rl001Resp executeRule(WorkingCalendar workingCalendar,
                                 List<HolidayResponseVO.Holiday> holidayList,
                                 Optional<HolidayResponseVO.Holiday> inHolidayOptional,
                                 LocalDate date) {
        while (inHolidayOptional.isPresent()) {
            date = DateUtils.milliTimestamp2LocalDateTime(inHolidayOptional.get().getEndtime()).toLocalDate().plusDays(1L);
            // 2. 日期是调休日，日期即工作日，返回工作日
            if (workingCalendarManager.isRepTime(holidayList, date)) {
                LocalDate workday = date;
                return new Rl001Resp() {{
                    setWorkday(workday);
                }};
            }
            inHolidayOptional = workingCalendarManager.holidayFilter(holidayList, date);
        }
        LocalDate workday = new Rl001CommonManager().calcWorkDate(HolidayPrincipleEnum.DELAY, date, workingCalendar);
        // 3. 日期在某个节假日区间，其他情况，根据工作日历判断是否工作日，返回工作日
        return new Rl001Resp() {{
            setWorkday(workday);
        }};
    }
}
