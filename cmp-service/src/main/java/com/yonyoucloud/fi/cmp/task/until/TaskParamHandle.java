package com.yonyoucloud.fi.cmp.task.until;

import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.YQLUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskParamHandle {

    public static AtomicInteger cardinalNumber = new AtomicInteger(1);

    @Autowired
    public BankConnectionAdapterContext bankConnectionAdapterContext;// 签名验签

    /**
     * 调度任务使用 处理前端传递的参数信息 构建查询银行账户的参数
     * @param param
     * @return
     */
    public static CtmJSONObject buildQueryBankAccountVosParams(CtmJSONObject param){

        //根据调度任务参数条件查询对应的账户信息
        String accentitys = (String) (Optional.ofNullable(param.get("accentity")).orElse(""));
        String banktypes = (String) (Optional.ofNullable(param.get("banktype")).orElse(""));
        String currencys = (String) (Optional.ofNullable(param.get("currency")).orElse(""));
        String bankaccounts = (String) (Optional.ofNullable(param.get("bankaccount")).orElse(""));
        //线程并发数量
        Integer corepoolsize = param.get("corepoolsize")!=null&&param.get("corepoolsize")!=""?Integer.valueOf(param.get("corepoolsize").toString()):0;

        String[] accentityArr = null;
        if (!StringUtils.isEmpty(accentitys)) {
            accentityArr = accentitys.split(";");
        }
        String[] banktypeArr = null;
        if (!StringUtils.isEmpty(banktypes)) {
            banktypeArr = banktypes.split(";");
        }
        String[] currencyArr = null;
        if (!StringUtils.isEmpty(currencys)) {
            currencyArr = currencys.split(";");
        }
        String[] bankaccountArr = null;
        if (!StringUtils.isEmpty(bankaccounts)) {
            bankaccountArr = bankaccounts.split(";");
        }

        CtmJSONObject queryBankAccountVosParams = new CtmJSONObject();
        queryBankAccountVosParams.put("accEntity",accentityArr);
        queryBankAccountVosParams.put("bankType",banktypeArr);
        queryBankAccountVosParams.put("accountId", bankaccountArr);
        queryBankAccountVosParams.put("currency",currencyArr);
        queryBankAccountVosParams.put("corepoolsize",corepoolsize);
        return queryBankAccountVosParams;
    }

    /**
     * 获取传入日期的 期间内的所有日期集合
     * @param param
     * @param isHistory
     * @return
     * @throws Exception
     */
    public static List<String> getBetweenDate(CtmJSONObject param, Boolean isHistory) throws Exception {
        String startDate = "";
        String endDate = "";
        Calendar calyseToday = Calendar.getInstance();
        Date dateToday = calyseToday.getTime();
        HashMap<String, String> queryData = TaskUtils.queryDateProcess(param,"yyyy-MM-dd");
        startDate = queryData.get(TaskUtils.TASK_START_DATE);
        endDate = queryData.get(TaskUtils.TASK_END_DATE);
        Date startDateDateType = DateUtils.dateParse(startDate, null);
        Date endDateDateType = DateUtils.dateParse(endDate, null);
        //回单传null，不使用isHistory参数;流水使用isHistory参数区分两个调度任务
        if (isHistory != null) {
            //历史的，不能查询当天的数据
            if (isHistory) {
                if (startDateDateType != null) {
                    if (DateUtils.isSameDay(startDateDateType, dateToday)) {
                        startDateDateType = DateUtils.beforeDay(startDateDateType);
                    }
                }
                if (endDateDateType != null) {
                    if (DateUtils.isSameDay(endDateDateType, dateToday)) {
                        endDateDateType = DateUtils.beforeDay(endDateDateType);
                    }
                }
            } else {
                startDateDateType = dateToday;
                endDateDateType = dateToday;
            }
        }
        //修改按每天返回 现在只返回起始 和 结束日期
        List<String> retrunDate = new ArrayList<>();
        retrunDate.add(startDate);
        retrunDate.add(endDate);
        return DateUtils.getBetweenDateForStrEnd(startDateDateType, endDateDateType);
    }

    public static String buildRequestSeqNo(String customNo) {
        StringBuilder tranSeqNo = new StringBuilder("R");
        tranSeqNo.append(customNo);
        tranSeqNo.append("0000");
        tranSeqNo.append(DateTimeFormatter.ofPattern(DateUtils.MILLISECOND_PATTERN).format(LocalDateTime.now()));
        tranSeqNo.append(YQLUtils.getSerialNumberNoCAS(cardinalNumber));
        return tranSeqNo.toString();
    }

}
