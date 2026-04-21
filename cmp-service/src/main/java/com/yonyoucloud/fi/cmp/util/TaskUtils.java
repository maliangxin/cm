package com.yonyoucloud.fi.cmp.util;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.yonyou.iuap.basedoc.social.util.JacksonUtils;
import com.yonyou.iuap.iuap.dispatch.sdk.bo.UpdateTaskLogResult;
import com.yonyou.iuap.iuap.dispatch.sdk.dto.DispatchTaskSendMsgDTO;
import com.yonyou.iuap.iuap.dispatch.sdk.rpc.IDispatchUpdateLogService;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JSONObject;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.CtmExceptionConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadResult;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TaskUtils {

    /**
     * 异步通知
     */
    public static String UPDATE_TASK_LOG_URL;


    static {
        UPDATE_TASK_LOG_URL = AppContext.getEnvConfig("UPDATE_TASK_LOG_URL");
    }

    public static final int TASK_BACK_SUCCESS = 1;//定时任务执行成功
    public static final int TASK_BACK_FAILURE = 0;//定时任务执行失败


    public static final String TASK_NO_DATA = "task_no_data";//定时任务不返回数据
    public static final String TASK_START_DATE = "startdate";//定时任务开始时间
    public static final String TASK_END_DATE = "enddate";//定时任务结束时间
    public static final String TASK_DAY_IN_ADV = "daysinadvance";//定时任务结束时间
    public static final String CONTAIN_CUR_DATE = "containCurDate";//包含当日

    public static final String BALANCE_CHECK_START_DATE = "startDate";//定时任务开始时间
    public static final String BALANCE_CHECK_END_DATE = "endDate";//定时任务结束时间
    public static final String BALANCE_CHECK_CHECKRANGE = "CheckRange";//定时任务结束时间

    public static final String BANKTYPE = "banktype";//银行类别
    public static final String ACCENTITY = "accentity";//账户使用组织
    public static EnterpriseBankQueryService enterpriseBankQueryService =  AppContext.getBean(EnterpriseBankQueryService.class);

    /**
     * 异步通知
     *
     * @param status 任务执行结果 1：成功；0：失败；
     * @param logId 日志ID，请求时header中的logId
     * @param content 错误信息
     * @param url
     * @throws Exception
     */
    public static void updateTaskLog(Map<String,String> ipaParams, int status, String logId,String content,String url){
        //调度任务和其他接口共用接口时，没有logId，直接返回
        /*if (StringUtils.isEmpty(logId)) {
            return;
        }*/
        /*Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", logId);
        map.put("status", status);
        map.put("content", content);
        CtmJSONObject json = (CtmJSONObject) CtmJSONObject.toJSON(map);*/
        DispatchTaskSendMsgDTO dispatchTaskSendMsgDTO = new DispatchTaskSendMsgDTO();
        dispatchTaskSendMsgDTO.setContent(content);
        dispatchTaskSendMsgDTO.setId(logId);
        dispatchTaskSendMsgDTO.setStatus(status);
        dispatchTaskSendMsgDTO.setUrl(url);
        dispatchTaskSendMsgDTO.setTenantId(AppContext.getYhtTenantId());
        try {
            log.error("异步通知请求 " + JacksonUtils.toJSONString(dispatchTaskSendMsgDTO));
            if (ipaParams != null) {
                callbackIpa(ipaParams.get("recordId"),ipaParams.get("nodeId"),status,"",content);
            }
            UpdateTaskLogResult updateTaskLogResult = AppContext.getBean(IDispatchUpdateLogService.class).updateTaskLog(dispatchTaskSendMsgDTO);
            log.error("异步通知响应 " + JacksonUtils.toJSONString(updateTaskLogResult));
        }catch (Exception e){
            log.error("异步通知响应异常 " + e.getMessage());
        }
    }

    /**
     * 添加新方法 封存message错误信息
     *
     * @param status
     * @param logId
     * @param content
     * @param message
     * @param url
     */
  /*  public static void updateTaskLog(int status, String logId, String content, String message, String url) {
        CtmJSONObject map = new CtmJSONObject();
        map.put("id", logId);
        map.put("status", status);
        map.put("content", content);
        map.put("message", message);
        map.put("title", "调度任务");//@notranslate
//        map.put("url", "url为空");
        try {
            log.error("异步通知请求 " + map.toString());
            CtmJSONObject jsonObject = RestTemplateUtils.doPostByJSON(url, map);
            log.error("异步通知响应 " + jsonObject.toString());
        } catch (Exception e) {
            log.error("异步通知响应异常 " + e.getMessage());
        }

    }*/

    /**
     * 传入开始日期、结束日期、日期范围
     * 返回开始日期、结束日期
     *
     * @param param
     */
    public static List<Date> queryStartAndEndDateByParam(JsonNode param, String dateRangeWord) throws Exception {
        CtmJSONObject paramForFays = new CtmJSONObject();
        JsonNode startDateNode = param.get(TaskUtils.TASK_START_DATE);
        JsonNode endDateNode = param.get(TaskUtils.TASK_END_DATE);
        //接口配置参数day，用来判断交易日期范围
        paramForFays.put(TaskUtils.TASK_START_DATE,null == startDateNode ?null:startDateNode.asText());
        paramForFays.put(TaskUtils.TASK_END_DATE,null == endDateNode ?null:endDateNode.asText());
        paramForFays.put("daysinadvance",param.get(dateRangeWord).asText());
        HashMap<String, String> queryDate = TaskUtils.queryDateProcess(paramForFays, "yyyy-MM-dd");
        Date startDate = DateUtils.dateParse(queryDate.get(TaskUtils.TASK_START_DATE), DateUtils.DATE_PATTERN);
        Date endDate = DateUtils.dateParse(queryDate.get(TaskUtils.TASK_END_DATE), DateUtils.DATE_PATTERN);
        if (startDate == null || endDate == null) {
            throw new com.yonyou.yonbip.ctm.error.CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B9A43A05D80083", "执行的调度任务配置的日期获取不对，请检查！") /* "执行的调度任务配置的日期获取不对，请检查！" */);
        }
        return Arrays.asList(startDate, endDate);
    }


    /**
     * 任务时间统一处理逻辑
     *
     * @param queryParamMap
     */
    public static HashMap<String, String> queryDateProcess(Map<String, Object> queryParamMap, String dateFormat) throws Exception {
        if (StringUtils.isEmpty(dateFormat)) {
            dateFormat = DateUtils.YYYYMMDD;
        }
        //新逻辑
        //        1:开始日期（非空）     结束日期（空）        提前日期（空）：报错，开始日期与结束日期要不同时有值，要不同时为空。
//        2:开始日期（非空）    结束日期（空）         提前日期（非空）：报错，开始日期与结束日期要不同时有值，要不同时为空。
//        3:开始日期（空）        结束日期（非空）     提前日期（空）：报错，开始日期与结束日期要不同时有值，要不同时为空。
//        4:开始日期（空）        结束日期（非空）     提前日期（非空）： 报错，开始日期与结束日期要不同时有值，要不同时为空。

//        5:开始日期（空）        结束日期（空）        提前日期（空）:   默认取当前日期+前一天数据；
//        如有'包含当日'参数时，则默认取前一天，仅”包含当日“=是时，在取当前日期+前一天数据
//        6:开始日期（空）        结束日期（空）        提前日期（非空）：提前日期~当前日期
//        7:开始日期（非空）    结束日期（非空）     提前日期（空）：   开始日期~结束日期
//        8:开始日期（非空）    结束日期（非空）     提前日期（非空）：报错，开始日期、结束日期不允许与日期范围同时有值


//        1:开始日期（空）	    结束日期（空）	    提前日期（空）:原逻辑[只有'包含当日'参数时，默认取当前日期；没有时，返回空]
//        ->默认取当前日期+前一天数据；如有'包含当日'参数时，则默认取前一天，仅”包含当日“=是时，在取当前日期+前一天数据
//        2:开始日期（非空）	结束日期（空）		提前日期（空）：获取前一天的数据；如开始日期大于当前日期，则获取数据为空->报错，开始日期与结束日期要不同时有值，要不同时为空。
//        3:开始日期（空）		结束日期（非空）	提前日期（空）：以结束日期为基准，往前推一个月->报错，开始日期与结束日期要不同时有值，要不同时为空。
//        4:开始日期（空）		结束日期（空）		提前日期（非空）：提前日期~当前日期->提前日期~当前日期
//
//        5:开始日期（非空）	结束日期（非空）	提前日期（空）：开始日期~结束日期-> 开始日期~结束日期
//        * 开始日期（空）		结束日期（非空）	提前日期（非空）：提前校验抛错，不处理 ->报错，开始日期与结束日期要不同时有值，要不同时为空。
//        6:开始日期（非空）	结束日期（空）		提前日期（非空）：提前日期~当前日期 或 开始日期~ 当前日期->报错，开始日期与结束日期要不同时有值，要不同时为空。
//
//        * 开始日期（非空）	结束日期（非空）	提前日期（非空）：提前校验抛错，不处理->报错，开始日期、结束日期不允许与日期范围同时有值
        String today = DateUtils.dateFormat(DateUtils.getNow(), dateFormat);
        String yesTody = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1), dateFormat);
        //兼容日期的不同名称
        String startdate = getStartdate(queryParamMap);
        String enddate = getEnddate(queryParamMap);

        // 检查开始日期和结束日期必须同时为空或同时存在
        if (!(StringUtils.isEmpty(startdate) && StringUtils.isEmpty(enddate) ||
                StringUtils.isNotEmpty(startdate) && StringUtils.isNotEmpty(enddate))) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400762", "开始日期与结束日期要不同时有值，要不同时为空") /* "开始日期与结束日期要不同时有值，要不同时为空" */);
        }

        //兼容日期范围的不同名称
        String intervalday = getIntString(queryParamMap, "daysinadvance");
        if (StringUtils.isEmpty(intervalday)) {
            intervalday = getIntString(queryParamMap, "RelatedDays");
        }
        if (StringUtils.isEmpty(intervalday)) {
            intervalday = getIntString(queryParamMap, "advanceDate");
        }
        // 包含当日-仅针对于提前天数不为空的时候，
        String containCurDate = (String) (Optional.ofNullable(queryParamMap.get(CONTAIN_CUR_DATE)).orElse(""));
        if (!StringUtils.isEmpty(startdate) && !DateUtils.pattern.equals(dateFormat)) {
            startdate = DateUtils.convertToStr(DateUtils.convertToDate(startdate, DateUtils.pattern), dateFormat);
        }
        if (!StringUtils.isEmpty(enddate) && !DateUtils.pattern.equals(dateFormat)) {
            enddate = DateUtils.convertToStr(DateUtils.convertToDate(enddate, DateUtils.pattern), dateFormat);
        }
        if (StringUtils.isNotEmpty(startdate) && StringUtils.isNotEmpty(enddate)) {
            int days = DateUtils.dateBetween(startdate, enddate);
            //调度任务使用他们的前端限制，不在代码中限制
            if (days > 31) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100090"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC93920448000C", "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!") /* "请注意，查询日期范围不允许超出31天，请调小日期范围后进行查询!" */);
            }
        }
        boolean startdateFlag = StringUtils.isNotEmpty(startdate);
        boolean enddateFlag = StringUtils.isNotEmpty(enddate);
        boolean intervaldayFlag = StringUtils.isNotEmpty(intervalday);
        //默认包含当日，公共方法，修改需全面修改
        boolean containCurDateFlag = true;
        if (StringUtils.isNotEmpty(containCurDate)){
            containCurDateFlag = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540075E", "是") /* "是" */.equals(containCurDate);
        }


        HashMap<String, String> retMap = new HashMap<String, String>();

        if (startdateFlag && enddateFlag && !intervaldayFlag) {
            retMap.put(TASK_START_DATE, startdate);
            retMap.put(TASK_END_DATE, enddate);
        } else if (!startdateFlag && !enddateFlag && intervaldayFlag) {
            String intervaldate = DateUtils.dateFormat(DateUtils.dateAddDays(DateUtils.getNow(), -1 * Integer.valueOf(intervalday)), dateFormat);
            retMap.put(TASK_START_DATE, intervaldate);
            if (containCurDateFlag) {
                retMap.put(TASK_END_DATE, today);
            } else {
                retMap.put(TASK_END_DATE, yesTody);
            }
        //       开始日期（空）        结束日期（空）        提前日期（空）:   有'包含当日'参数且为是时，取昨天；否则取今天和昨天
        } else if (!startdateFlag && !enddateFlag && !intervaldayFlag) {
            if (!containCurDateFlag) {
                retMap.put(TASK_START_DATE, yesTody);
                retMap.put(TASK_END_DATE, yesTody);
            } else {
                retMap.put(TASK_START_DATE, yesTody);
                retMap.put(TASK_END_DATE, today);
            }
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400761", "开始日期、结束日期不允许与日期范围同时有值") /* "开始日期、结束日期不允许与日期范围同时有值" */);
        }
        return retMap;
    }

    //调度任务原来传的是string，现在传int，兼容两种情况
    private static  String getIntString(Map<String, Object> queryParamMap, String fieldName) {
        return Objects.toString(queryParamMap.get(fieldName), "");
    }

    private static String getEnddate(Map<String, Object> queryParamMap) {
        String enddate = (String) (Optional.ofNullable(queryParamMap.get(TASK_END_DATE)).orElse(""));
        if (StringUtils.isEmpty(enddate)) {
            enddate = (String) (Optional.ofNullable(queryParamMap.get(BALANCE_CHECK_END_DATE)).orElse(""));
        }
        return enddate;
    }

    private static String getStartdate(Map<String, Object> queryParamMap) {
        String startdate = (String) (Optional.ofNullable(queryParamMap.get(TASK_START_DATE)).orElse(""));
        if (StringUtils.isEmpty(startdate)) {
            startdate = (String) (Optional.ofNullable(queryParamMap.get(BALANCE_CHECK_START_DATE)).orElse(""));
        }
        return startdate;
    }

    public static String[] getBanktypes(CtmJSONObject paramMap) {
        Object banktypeObj = paramMap.get(TaskUtils.BANKTYPE);
        String banktypes;

        if (banktypeObj instanceof String) {
            banktypes = (String) banktypeObj;
        } else {
            banktypes = "";
        }

        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(banktypes)) {
            return new String[0]; // 返回空数组而不是未初始化的引用
        }

        String[] banktypeArr = banktypes.split(";");
        return banktypeArr;
    }

    //通过使用组织查找对应的账户
    public static List<String> getAccountsByAccEntitys(CtmJSONObject paramMap) throws Exception {
        String accEntity = paramMap.getString(IBussinessConstant.ACCENTITY);
        List<String> accentitys = new ArrayList<>();
        List<String> accounts = new ArrayList<>();
        if (!org.apache.commons.lang3.StringUtils.isEmpty(accEntity)) {
            if (accEntity.contains(",")) {
                accentitys = Arrays.asList(accEntity.split(","));
            } else if (accEntity.contains(";")) {//调度任务 传进来的信息是分号区分
                accentitys = Arrays.asList(accEntity.split(";"));
            } else {
                //说明参数只选了一个
                accentitys.add(accEntity);
            }
            if (accentitys != null && !accentitys.isEmpty()) {
                // 根据所选组织查询，有使用组织权限的账户
                EnterpriseParams newEnterpriseParams = new EnterpriseParams();
                newEnterpriseParams.setOrgidList(accentitys);
                List<EnterpriseBankAcctVOWithRange> enterpriseBankAcctVOS = enterpriseBankQueryService.queryAllWithRange(newEnterpriseParams);
                for (EnterpriseBankAcctVOWithRange enterpriseBankAcctVO : enterpriseBankAcctVOS) {
                    accounts.add(enterpriseBankAcctVO.getId());
                }
            }
        }
        return accounts;
    }


    public static void dateCheck(Map<String, Object> paramMap) throws Exception {
        //兼容日期的不同名称
        String startdate = getStartdate(paramMap);
        String enddate = getEnddate(paramMap);
        String daysinadvance = (String) (Optional.ofNullable(paramMap.get(TASK_DAY_IN_ADV)).orElse(""));
        // 检查开始日期和结束日期必须同时为空或同时存在
        if (!(StringUtils.isEmpty(startdate) && StringUtils.isEmpty(enddate) ||
                StringUtils.isNotEmpty(startdate) && StringUtils.isNotEmpty(enddate))) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400762", "开始日期与结束日期要不同时有值，要不同时为空") /* "开始日期与结束日期要不同时有值，要不同时为空" */);
        }
        //日期间隔不能超31天
        if (!StringUtils.isEmpty(startdate) && !StringUtils.isEmpty(enddate)) {
            if (DateUtils.dateBetween(startdate, enddate) > 31) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100884"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1862FE3C05D0001F", "开始日期与结束日期不允许超出31天，请检查！") /* "开始日期与结束日期不允许超出31天，请检查！" */);
            }
        }
    }

    /**
     * 历史余额检查调度任务日期校验
     * @param paramMap
     */
    public static int dateCheckForBalanceCheck(Map<String, Object> paramMap) throws ParseException {
        String startdate = (String) (Optional.ofNullable(paramMap.get(BALANCE_CHECK_START_DATE)).orElse(""));
        String enddate = (String) (Optional.ofNullable(paramMap.get(BALANCE_CHECK_END_DATE)).orElse(""));
        String dateRange = (String) (Optional.ofNullable(paramMap.get(BALANCE_CHECK_CHECKRANGE)).orElse(""));
        boolean startdateFlag = StringUtils.isNotEmpty(startdate);
        boolean enddateFlag = StringUtils.isNotEmpty(enddate);
        boolean intervaldayFlag = StringUtils.isNotEmpty(dateRange);
        //[开始日期、结束日期]不允许与日期范围同时有值
        if((startdateFlag || enddateFlag) && intervaldayFlag){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400761", "开始日期、结束日期不允许与日期范围同时有值") /* "开始日期、结束日期不允许与日期范围同时有值" */);
        }
        Date startDate = DateUtils.strToDate(startdate);
        Date endDay = DateUtils.strToDate(enddate);
        Date nowDate = DateUtils.formatBalanceDate(new Date());
        if(StringUtils.isEmpty(dateRange) && (StringUtils.isEmpty(enddate) && StringUtils.isEmpty(startdate))){
            // 开始日期 结束日期 检查天数都为空，走检查天数默认天数
            return 1;
        }else {
            if(StringUtils.isEmpty(dateRange)){
                // 检查天数为空，校验开始日期 结束日期
                if ((StringUtils.isEmpty(enddate) && StringUtils.isEmpty(startdate))
                        || (!StringUtils.isEmpty(enddate) && !StringUtils.isEmpty(startdate))) {
                    //日期间隔不能超31天
                    if (!StringUtils.isEmpty(startdate) && !StringUtils.isEmpty(enddate)) {
                        if (DateUtils.dateBetween(startdate, enddate) > 60) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100885"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00071", "开始日期与结束日期不允许超出60天，请检查！") /* "开始日期与结束日期不允许超出60天，请检查！" */);
                        }else if(startDate.after(endDay)){
                            // 开始日期不能晚于结束日期
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100886"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00072", "开始日期不能晚于结束日期，请检查！") /* "开始日期不能晚于结束日期，请检查！" */);
                        }else if(endDay.after(nowDate)){
                            // 开始日期不能晚于昨天
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100887"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C9", "结束日期不能晚于今日，请检查！") /* "结束日期不能晚于今日，请检查！" */);
                        }
                    }
                }else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100888"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B14FEF605F00070", "开始日期、结束日期要不同时为空，要不同时有值，请检查！") /* "开始日期、结束日期要不同时为空，要不同时有值，请检查！" */);
                }
            }else {
                return Integer.valueOf(dateRange);
            }
        }
        // 使用开始日期 结束日期
        return 0;
    }


    public static void updateTaskLogbyThreadResult(Map<String,String> ipaParams, String logId, String accountCountMsg, ThreadResult threadResult) {
        if (threadResult.getErrorReturnList().size() > 0) {
            String content = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B001FE", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400760", "执行失败") /* "执行失败" */) /* "执行失败" */
                    + "[Failure Reason]"+ "\n"
                    + threadResult.getErrorReturnList().stream().collect(Collectors.joining("\n")) + "\n"
                    + threadResult.getSucessReturnList().stream().collect(Collectors.joining("\n"));
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_FAILURE, logId,content, TaskUtils.UPDATE_TASK_LOG_URL);
            if (ipaParams != null) {
                callbackIpa(ipaParams.get("recordId"),ipaParams.get("nodeId"),0,"",content);
            }
        }else{
            String content = MessageUtils.getMessage("P_YS_OA_app_xtyyjm_0000035989") /* "执行成功" */ + ":" + accountCountMsg.toString()+ "\n" + threadResult.getSucessReturnList().stream().collect(Collectors.joining("\n"));
            TaskUtils.updateTaskLog(null,TaskUtils.TASK_BACK_SUCCESS, logId,content , TaskUtils.UPDATE_TASK_LOG_URL);
            if (ipaParams != null) {
                callbackIpa(ipaParams.get("recordId"),ipaParams.get("nodeId"),1,"",content);
            }
        }
    }

    private static void callbackIpa(String recordId, String nodeId, int status, String msg, String content) {
        // 回调数智员工
        JSONObject param = new JSONObject();
        param.put("status", status);
        param.put("recordId", recordId);
        param.put("nodeId", nodeId);
        param.put("msg", msg);
        param.put("content", content);
        try {
            // 数智员工回调地址
            String url = AppContext.getEnvConfig("domain.iuap-aip-ipa") + "/ipaserver/portal/skill/async/callback/run";
            CTMAuthHttpClientUtils.execPost(url, null, null, param.toString());
        } catch (Exception e) {
            log.error("Callback to IPA failed, reason for failure:{} ", e.getMessage());
        }
    }

    public static boolean getBooleanFromString(String cotainFreezeAccount, boolean defaultValue) {
        if (StringUtils.isEmpty(cotainFreezeAccount)) {
            return defaultValue;
        } else if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540075E", "是") /* "是" */.equals(cotainFreezeAccount)) {
            return true;
        } else if (com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540075F", "否") /* "否" */.equals(cotainFreezeAccount)) {
            return false;
        } else {
            log.error("UnclaimedWarningServiceImpl getBooleanFromString error, unsupport parameter cotainFreezeAccount = {}", cotainFreezeAccount);
            return defaultValue;
        }
    }

    public static boolean changeStartDateByEnableDateAndCheckIfSkip(EnterpriseBankAcctVO bankAccount, StringBuilder startDate, String endDate) throws Exception {
        // 校验银行账户账号是否为空
        if (bankAccount == null || bankAccount.getAccount() == null || bankAccount.getAccount().isEmpty()) {
            throw new CtmException(CtmExceptionConstant.ACCOUNT_CANNOT_BE_EMPTY);
        }
        String acct = bankAccount.getAccount();
        // 获取启用日期
        Date enableDate = BankAccountUtil.getEnableDate(acct);
        if (enableDate == null) {
            return false; // 启用日期为空时直接返回 false
        }

        // 解析起止日期
        Date parsedStartDate = DateUtil.parse(startDate);
        Date parsedEndDate = DateUtil.parse(endDate);

        // 比较启用日期与起止日期的关系
        if (enableDate.after(parsedEndDate)) {
            log.error("账户[{}]启用日期[{}]晚于结束日期[{}]，跳过处理", acct, enableDate, parsedEndDate);
            return true; // 启用日期晚于结束日期，跳过处理
        } else if (enableDate.before(parsedStartDate)) {
            log.info("账户[{}]启用日期[{}]早于开始日期[{}]，不跳过处理", acct, enableDate, parsedStartDate);
            return false; // 启用日期早于开始日期，不跳过处理
        } else {
            log.info("账户[{}]启用日期[{}]在范围内，更新开始日期从[{}]到[{}]", acct, enableDate, parsedStartDate, DateUtil.format(enableDate, DateUtils.YQL_REQUEST_DATE_FORMAT));
            // 启用日期在范围内，更新开始日期
            startDate.setLength(0);
            startDate.append(DateUtil.format(enableDate, DateUtils.YQL_REQUEST_DATE_FORMAT));
            return false;
        }

    }
}
