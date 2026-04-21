package com.yonyoucloud.fi.cmp.weekday;

import com.yonyou.ctp.ctm.finance.rule.domain.workingcalendar.HolidayResponseVO;
import com.yonyou.ctp.ctm.finance.rule.domain.workingcalendar.WorkingCalendar;
import com.yonyou.ctp.ctm.finance.support.calendar.IFinanceCalendarService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author guoyangy
 * @Date 2024/8/14 15:02
 * @Description 没有这个类起不来服务，WorkingCalendarManager里面需要
 * @Version 1.0
 */
@Service
public class CmpFinanceCalendarService implements IFinanceCalendarService {
    @Override
    public List<WorkingCalendar> getWorkingCalendar(String tenantId, String orgId) {
        return null;
    }

    @Override
    public HolidayResponseVO.VoData getWorkingCalendarWithHoliday(String attendCalendarId, int year) {
        return null;
    }
}
