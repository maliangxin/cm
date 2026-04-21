package com.yonyoucloud.fi.cmp.weekday;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.utils.RestTemplateUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.*;

import static com.yonyoucloud.fi.cmp.weekday.WorkingCalendarConsts.*;

/**
 * @author shiqhs
 * @date 2022/04/27
 * @description 工作日历服务
 */
@Service
public class CmpWorkingCalendarManager {

    @Value("${calendar-url}")
    private String calendarUrl;
    @Value("${holiday-url}")
    private String holidayUrl;


    /**
     * 是否调休日
     * @param holidayList 节假日列表
     * @param date 日期
     * @return 是否调休日
     */
    public boolean isRepTime(List<HolidayResponseVO.Holiday> holidayList, LocalDate date) {
        String splitCode = ",";
        Optional<HolidayResponseVO.Holiday> optional = holidayList.stream().filter(holiday -> {
            if (StringUtils.hasText(holiday.getReptime())) {
                for (String repTimeItem: holiday.getReptime().split(splitCode)) {
                    LocalDate repTime = DateUtils.milliTimestamp2LocalDateTime(Long.parseLong(repTimeItem)).toLocalDate();
                    return date.equals(repTime);
                }

            }
            return false;
        }).findFirst();
        return optional.isPresent();
    }

    /**
     * 节假日过滤, 获取第日期所属节假日
     * @param holidayList 节假日集合
     * @param date 日期
     * @return 工作日
     */
    public Optional<HolidayResponseVO.Holiday> holidayFilter(List<HolidayResponseVO.Holiday> holidayList, LocalDate date) {
        return holidayList.stream().filter(holiday -> {
            LocalDate beginDate = DateUtils.milliTimestamp2LocalDateTime(holiday.getBegintime()).toLocalDate();
            LocalDate endDate = DateUtils.milliTimestamp2LocalDateTime(holiday.getEndtime()).toLocalDate();
            boolean isBetweenRange = date.isAfter(beginDate) && date.isBefore(endDate);
            return isBetweenRange  || date.equals(beginDate) || date.equals(endDate);
        }).findFirst();
    }

    /**
     * 获取工作日历
     * @param tenantId 租户ID
     * @param orgId 组织ID（会计主体）
     * @return 工作日历
     */
    @TlmCacheable(keyELs = {"#tenantId", "#orgId"}, cacheType = TlmCacheType.THREAD)
    public List<WorkingCalendar> getWorkingCalendar(String tenantId, String orgId) {
        Map<String, String> uriVariablesMap = new HashMap<>(2);
        uriVariablesMap.putIfAbsent(TENANT_ID, tenantId);
        uriVariablesMap.putIfAbsent(ORG_ID, orgId);
        String json = RestTemplateUtils.doExchange(calendarUrl, HttpMethod.GET, null, null, String.class, uriVariablesMap);
        WorkingCalendarVO holidayResponseVO = CtmJSONObject.parseObject(json, WorkingCalendarVO.class);
        return holidayResponseVO.getData();
    }

    /**
     * 获取节假日集合
     * @param attendCalendarId 工作日历ID
     * @param date 日期
     * @return 节假日集合
     */
    public List<HolidayResponseVO.Holiday> getHolidayList(String attendCalendarId, LocalDate date) {
        List<HolidayResponseVO.Holiday> all = new LinkedList<>();
        HolidayResponseVO.VoData voData = AppContext.getBean(CmpWorkingCalendarManager.class).getWorkingCalendarWithHoliday(attendCalendarId, date.getYear());
        if (CollectionUtils.isNotEmpty(voData.getHoliday())) {
            all.addAll(voData.getHoliday());
        }
        if (date.getMonthValue() == 12) {
            HolidayResponseVO.VoData nextData = AppContext.getBean(CmpWorkingCalendarManager.class).getWorkingCalendarWithHoliday(attendCalendarId, date.getYear() + 1);
            if (CollectionUtils.isNotEmpty(nextData.getHoliday())) {
                all.addAll(nextData.getHoliday());
            }
        }
        return all;
    }

    /**
     * 获取一年中的节假日
     * @param attendCalendarId 工作日历ID
     * @param year 年
     * @return 工作日历和节假日
     */
    @TlmCacheable(keyELs = {"#attendCalendarId", "#year"}, cacheType = TlmCacheType.THREAD)
    public HolidayResponseVO.VoData getWorkingCalendarWithHoliday(String attendCalendarId, int year) throws RestClientException {
        Map<String, String> uriVariablesMap = new HashMap<>(2);
        uriVariablesMap.putIfAbsent(ATTEND_CALENDAR_ID, attendCalendarId);
        uriVariablesMap.putIfAbsent(YEAR, year+"");
//        HolidayResponseVO holidayResponseVO = RestTemplateUtils.doGet(holidayUrl, HolidayResponseVO.class, uriVariablesMap);
        String json = RestTemplateUtils.doGet(holidayUrl, String.class, uriVariablesMap);
        HolidayResponseVO holidayResponseVO = CtmJSONObject.parseObject(json, HolidayResponseVO.class);
        return holidayResponseVO.getData();
    }

}
