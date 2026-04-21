package com.yonyoucloud.fi.cmp.weekday;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * @author shiqhs
 * @date 2021/11/24
 * @description RL001-节假日处理规则 服务类
 * 规则: 1.无工作日历及节假日信息，按周六日非工作日处理，返回工作日
 *      2.日期是调休日，日期即工作日，返回工作日
 *      3.日期在某个节假日区间，工作日原则是延后、提前时, 1-处理节假日; 2-周六日处理,处理和的日期和其他情况，根据工作日历判断是否工作日，返回工作日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeekdayServiceImpl implements RuleService {

    private final CmpRl001DelayManager rl001DelayManager;
    private final CmpWorkingCalendarManager workingCalendarManager;

    @Override
    public RuleBaseResp executeRule(RuleBaseReq request) throws CtmException {
        Rl001Req rl001Req = (Rl001Req) request;
        WorkdayReq workdayReq = rl001Req.getWorkdayReq();
        List<WorkingCalendar> list = workingCalendarManager.getWorkingCalendar(workdayReq.getTenantId(), workdayReq.getOrgId());
        // 1. 无工作日历及节假日信息，按周六日非工作日处理，返回工作日
        if (CollectionUtils.isEmpty(list)) {
            return new Rl001CommonManager().emptyHandle(HolidayPrincipleEnum.DELAY, rl001Req.getNaturalDay());
        }
        Optional<WorkingCalendar> workingCalendarOptional = list.stream().filter(item -> workdayReq.getOrgId() != null && workdayReq.getOrgId().equals(item.getBusiorg())).findFirst();
        // 会计主体不存在工作日历，取默认工作日历
        WorkingCalendar workingCalendar = workingCalendarOptional.orElse(list.get(0));
        List<HolidayResponseVO.Holiday> holidayList = workingCalendarManager.getHolidayList(workingCalendar.getId(), rl001Req.getNaturalDay());
        // 2. 日期是调休日，日期即工作日，返回工作日
        if (workingCalendarManager.isRepTime(holidayList, rl001Req.getNaturalDay())) {
            return new Rl001Resp() {{
                setWorkday(rl001Req.getNaturalDay());
            }};
        }
        Optional<HolidayResponseVO.Holiday> optional = workingCalendarManager.holidayFilter(holidayList, rl001Req.getNaturalDay());
        return rl001DelayManager.executeRule(workingCalendar, holidayList, optional, rl001Req.getNaturalDay());
    }

    /**
     * 获取工作日
     * @param workdayReq 工作日参数
     * @param naturalDay 自然日
     * @return 工作日
     */
    public LocalDate getWorkday(WorkdayReq workdayReq, LocalDate naturalDay) {
        Rl001Req rl001Req = new Rl001Req() {{
            setWorkdayReq(workdayReq);
            setNaturalDay(naturalDay);
        }};
        Rl001Resp rl001Resp = (Rl001Resp) executeRule(rl001Req);
        return rl001Resp.getWorkday();
    }

    public CtmJSONObject getWorkday(CtmJSONObject param) {
        WorkdayReq workdayReq = new WorkdayReq();
        workdayReq.setTenantId(AppContext.getTenantId().toString());
        workdayReq.setOrgId(param.get("accentity").toString());
        long addDays = Long.parseLong(param.get("deliverytime").toString());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 取系统日期作为当前日期进行后续计算
        LocalDate naturalDay = LocalDate.now();
        if (addDays == 1L) {
            // T+1计算
            LocalDate resultDate = getWorkday(workdayReq, naturalDay.plusDays(1L));
            String resultDateStr = resultDate.format(formatter);
            CtmJSONObject ctmJSONObject = new CtmJSONObject();
            ctmJSONObject.put("delayedDate", resultDateStr);
            return ctmJSONObject;
        } else {
            /**
             * T+2计算
             * 此处不能直接日期加2后跳过节假日处理，应该按照工作日计算，先取到T+1后，再T+1的基础上再+1计算出T+2才准确
             * 例如：系统日期为周五（只考虑周六、日），T+1应为下周一，T+2应为下周二；
             * 如果先对日期加减后再跳过节假日，则T+2为本周日的后面第一个工作日，还是下周一，与需求不符
             */
            LocalDate resultDateT1 = getWorkday(workdayReq, naturalDay.plusDays(1L));
            LocalDate resultDateT2 = getWorkday(workdayReq, resultDateT1.plusDays(1L));
            String resultDateStr = resultDateT2.format(formatter);
            CtmJSONObject ctmJSONObject = new CtmJSONObject();
            ctmJSONObject.put("delayedDate", resultDateStr);
            return ctmJSONObject;
        }
    }

}
