package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.impl;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JSONObject;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model.DealDetailConsumerDTO;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.*;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailExceptionCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.MD5Utils;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * @Author guoyangy
 * @Date 2024/7/23 16:19
 * @Description todo
 * @Version 1.0
 */
@Service("yQLDealDetailAccessImpl")
@Slf4j
public class YQLDealDetailAccessImpl<T> extends DefaultDealDetailAccessImpl<T>{
    public static final Cache<DealDetailConsumerDTO, List<BankDealDetailODSModel>> odsCache = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).concurrencyLevel(4).maximumSize(1000).softValues().build();
    public static Cache<DealDetailConsumerDTO, List<BankDealDetailODSModel>> getOdsCache(){
        return odsCache;
    }
    @Override
    public BankDealDetailModel getBankDealDetailModelList(T t)  throws BankDealDetailException{
        YQLDataAccessModel yqlDataAccessModel = null;
        if(t instanceof  YQLDataAccessModel){
            yqlDataAccessModel = (YQLDataAccessModel)t;
        }
        if(yqlDataAccessModel == null){
            log.error("【流水接入】入参为空，流水接入结束！");
            return null;
        }
        //解析银企联入参反参
        BankDealDetailModel streamModel = this.parseRequestAndResponseParam(yqlDataAccessModel);
        if(null == streamModel){
            log.error("【流水接入】流水参数解析为空,流水处理流程结束");
            return null;
        }
        yqlDataAccessModel.setRequestSeqNo(streamModel.getRequestSeqNo());
        return streamModel;
    }
    private BankDealDetailModel parseRequestAndResponseParam(YQLDataAccessModel yqlDataAccessModel) {
        String requestParam = yqlDataAccessModel.getRequestParam();
        String response = yqlDataAccessModel.getResponse();
        String usedTime = yqlDataAccessModel.getUsedTime();
        String operType = yqlDataAccessModel.getOperType();
        String bankaccountId = yqlDataAccessModel.getBankaccountId();
        String currencyId = yqlDataAccessModel.getCurrencyId();
        String orgId = yqlDataAccessModel.getOrgId();
        /**
         * step1: 关键参数非空校验
         * */
        if(StringUtils.isEmpty(requestParam)){
            return null;
        }
        CtmJSONObject requestObj = CtmJSONObject.parseObject(requestParam);
        if(ObjectUtils.isEmpty(requestObj)){
            return null;
        }
        CtmJSONObject requestHeadObj = requestObj.getJSONObject("request_head");
        if(ObjectUtils.isEmpty(requestHeadObj)){
            return null;
        }
        CtmJSONObject requestBodyObj = requestObj.getJSONObject("request_body");
        if(ObjectUtils.isEmpty(requestBodyObj)){
            return null;
        }
        /**
         * step2: 解析请求参数
         * */
        BankDealDetailModel streamLogModel = new BankDealDetailModel();
        BankDealDetailOperLogModel streamOperLogModel = new BankDealDetailOperLogModel();
        streamLogModel.setDetailOperLogModel(streamOperLogModel);

        long tenantId = Long.parseLong(InvocationInfoProxy.getYxyTenantid());
        String ytenantId = InvocationInfoProxy.getTenantid();
        String traceId = DealDetailUtils.getTraceId();
        streamOperLogModel.setTenantId(tenantId);
        streamOperLogModel.setYTenantId(ytenantId);
        streamOperLogModel.setTraceId(traceId);
        streamOperLogModel.setUsedTime(usedTime);
        String tranCode = requestHeadObj.getString("tran_code");
        String requestSeqNo = requestHeadObj.getString("request_seq_no");
        String beg_dateStr = requestBodyObj.getString("beg_date");
        String end_dateStr = requestBodyObj.getString("end_date");
        String acct_no = requestBodyObj.getString("acct_no");
        String acct_name = requestBodyObj.getString("acct_name");
        String curr_code = requestBodyObj.getString("curr_code");
        String tran_status = requestBodyObj.getString("tran_status");
        Integer begNum = requestBodyObj.getInteger("beg_num");
        streamOperLogModel.setTranCode(tranCode);
        streamOperLogModel.setRequestSeqNo(requestSeqNo);
        streamLogModel.setRequestSeqNo(requestSeqNo);
        streamOperLogModel.setBegDate(beg_dateStr);
        streamOperLogModel.setEndDate(end_dateStr);
        streamOperLogModel.setAcctNo(acct_no);
        streamOperLogModel.setAcctName(acct_name);
        streamOperLogModel.setCurrCode(curr_code);
        streamOperLogModel.setTranStatus(tran_status);
        streamOperLogModel.setBegNum(begNum);
        streamOperLogModel.setOperType(operType);
        if(StringUtils.isEmpty(response)){
            return streamLogModel;
        }
        /**
         * step3:解析响应参数,response_head
         * */
        CtmJSONObject responseData = CtmJSONObject.parseObject(response);
        if(ObjectUtils.isEmpty(responseData)){
            return streamLogModel;
        }
        Integer code = (Integer) responseData.get("code");
        streamOperLogModel.setCode(code);
        streamOperLogModel.setMessage((String) responseData.get("message"));
        if(BankDealDetailConst.CODE_SUCC!=code){
            return streamLogModel;
        }
        CtmJSONObject data = responseData.getJSONObject("data");
        if(ObjectUtils.isEmpty(data)){
            return streamLogModel;
        }
        CtmJSONObject responseHead = data.getJSONObject("response_head");
        if(responseHead == null){
            return streamLogModel;
        }
        String serviceRespCode = responseHead.getString("service_resp_code");
        String serviceRespDesc = responseHead.getString("service_resp_desc");
        String serviceStatus = responseHead.getString("service_status");
        String service_seq_no = responseHead.getString("service_seq_no");
        streamOperLogModel.setRespServiceCode(serviceRespCode);
        streamOperLogModel.setRespServiceDesc(serviceRespDesc);
        streamOperLogModel.setRespServiceSeqno(service_seq_no);
        streamOperLogModel.setRespServiceStatus(serviceStatus);

        CtmJSONObject responseBody = data.getJSONObject("response_body");
        if(ObjectUtils.isEmpty(responseBody)){
            return streamLogModel;
        }
        String back_num =  responseBody.getString("back_num");
        if(!StringUtils.isEmpty(back_num)){
            streamOperLogModel.setBackNum(Integer.parseInt(back_num));
        }
        Integer backNum = streamOperLogModel.getBackNum();
        CtmJSONArray recordArray = null;
        if(backNum!=null&&backNum>0){
            if(backNum > 1){
                recordArray = responseBody.getJSONArray("record");
            }else {
                CtmJSONObject ctmJSONObject = responseBody.getJSONObject("record");
                recordArray = new CtmJSONArray();
                recordArray.add(ctmJSONObject);
            }
        }
        if(recordArray==null||recordArray.size()==0){
            streamOperLogModel.setActualNum(0);
            return streamLogModel;
        }
        streamOperLogModel.setActualNum(recordArray.size());
        /**
         * step4:解析流水内容 response_body中record参数
         * */
        List<BankDealDetailODSModel> streamResponseRecordList = new ArrayList<>();
        for(int i=0;i<recordArray.size();i++){
            CtmJSONObject recordObj = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(recordArray.get(i)));
            log.error("【解析流水】1.流水【{}】解析结果：{}", recordObj.get("request_seq_no"),recordObj);
            // 收付方向
            String dc_flag = recordObj.get("dc_flag")+"";
            if ("d".equalsIgnoreCase(dc_flag)) {
                recordObj.put("dc_flag",Direction.Debit.getValue());
            } else if ("c".equalsIgnoreCase(dc_flag)) {
                recordObj.put("dc_flag",Direction.Credit.getValue());
            }
            String value_date = recordObj.get("value_date") == null ? null : recordObj.get("value_date")+"";
            try {
                if(!StringUtils.isEmpty(value_date) && !value_date.contains("-") && !"null".equals(value_date)){
                    Date valueDate = DateUtils.dateParse(value_date, DateUtils.YYYYMMDD);
                    String s = DateUtils.convertToStr(valueDate, DateUtils.DATE_PATTERN);
                    SimpleDateFormat formatter = new SimpleDateFormat(DateUtils.DATE_PATTERN);
                    recordObj.put("value_date",formatter.parse(s));
                }
            } catch (ParseException e) {
                log.error("日期转换异常！，时间："+value_date);
            }
            String recordObjStr = JSONObject.toJSONString(recordObj);
            String signature = this.contentSign(recordObjStr);
            BankDealDetailODSModel streamResponseRecord = CtmJSONObject.parseObject(recordObjStr, BankDealDetailODSModel.class);
            if(null==streamResponseRecord){
                continue;
            }
            streamResponseRecord.setContentsignature(signature);
            streamResponseRecord.setBankaccount(bankaccountId);
            streamResponseRecord.setCurrency(currencyId);
            streamResponseRecord.setOrgid(orgId);
            streamResponseRecord.setTenant_id(tenantId);
            streamResponseRecord.setYtenant_id(ytenantId);
            streamResponseRecord.setCreate_time(new Date());
            streamResponseRecord.setTraceid(traceId);
            streamResponseRecord.setRequest_seq_no(requestSeqNo);
            streamResponseRecord.setResp_service_seq_no(service_seq_no);
            streamResponseRecord.setAcct_no(responseBody.getString("acct_no"));
            streamResponseRecord.setAcct_name(responseBody.getString("acct_name"));
            streamResponseRecord.setMainid(0L);
            streamResponseRecord.setProcessstatus(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NO.getProcessstatus());
            streamResponseRecord.setAccesschannel(DealDetailEnumConst.AccessChannelEnum.YQL.getKey());
            streamResponseRecordList.add(streamResponseRecord);
        }
        streamLogModel.setStreamResponseRecordList(streamResponseRecordList);
        return streamLogModel;
    }
    /**
     * 银企联流水生成唯一签名用于验重，生成唯一签名算法如下：
     * 1）银企联反参key与本地存储的银企联反参key做交集，防止银企联私自增加反参字段，影响生成签名的结果
     * 2）第一步获取的key做排序
     * 3）取key对应value以追加方式生成字符串
     * 4）第三步获取的字符串做MD5生成加密后的唯一标识
     *
     * [{key1:""},{}]
     * */
    @Override
    public String contentSign(String content) {
        if(StringUtils.isEmpty(content)){
            return null;
        }
        CtmJSONObject ctmJSONObject = CtmJSONObject.parseObject(content);
        Set<String> keys = ctmJSONObject.keySet();
        //step1:获取流水反参key集合
        List<String> fields = new ArrayList<>(keys);
        // todo step2:求交集[null值，key不同、value相同]
        List<String> orderfields = fields.stream().filter(BankDealDetailConst.YQL_ALL_FIELDS::contains).collect(Collectors.toList());
        //step3:排序
        Collections.sort(orderfields);
        StringBuilder builder = new StringBuilder();
        //step4:获取排序后Key所对应的value追加到stringbuilder
        for(int i=0;i<orderfields.size();i++){
            String key = orderfields.get(i);
            builder.append(ctmJSONObject.get(key));
        }
        //银企联数据+租户进行MD5加密
        builder.append(InvocationInfoProxy.getTenantid());
        //step5:最后将生成的stringbuilder做md5生成唯一签名
        String signContent = MD5Utils.MD5Encode(builder.toString());
        return signContent;
    }
    /**
     * 按照流水是否在ods存在分成两类
     * 1.当前批次流水根据唯一标识先在内存中分类
     * 2.取第一步不重复的流水根据唯一标识拿到ods库看是否存在，不存在的流水后续可以入ods库,存在的流水后续入库ods_fail
     * */
    @Override
    public Map<String, List<BankDealDetailODSModel>> checkRepeatFromODS(BankDealDetailModel dealDetailModel){
        List<BankDealDetailODSModel> streamResponseRecordList = dealDetailModel.getStreamResponseRecordList();
        if(CollectionUtils.isEmpty(streamResponseRecordList)){
            return null;
        }
        List<BankDealDetailODSModel> repeatStreamCurrentBatch = new ArrayList<>();
        List<BankDealDetailODSModel> normalStreamCurrentBatch = new ArrayList<>();
        //step1:流水先在内存中区分重复流水
        this.checkRepeatInCurrentBatch(repeatStreamCurrentBatch,normalStreamCurrentBatch,streamResponseRecordList);
        //step2:流水基于DB区分重复流水
        this.checkRepeatInODSDB(repeatStreamCurrentBatch,normalStreamCurrentBatch,streamResponseRecordList);
        //step3:构建反参，重复流水入异常库，不重复流水入ods库
        Map<String, List<BankDealDetailODSModel>> resultMap= new HashMap<>();
        resultMap.put(BankDealDetailConst.NORMALSTREAM,normalStreamCurrentBatch);
        resultMap.put(BankDealDetailConst.REPEATSTREAM,repeatStreamCurrentBatch);
        //step4:计算本批次流水成功、失败条数
        BankDealDetailOperLogModel detailOperLogModel = dealDetailModel.getDetailOperLogModel();
        if(null!=detailOperLogModel){
            detailOperLogModel.setSuccNum(normalStreamCurrentBatch.size());
            detailOperLogModel.setFailNum(repeatStreamCurrentBatch.size());
        }

        //step5:处理结果数量校验
        if(normalStreamCurrentBatch.size()+repeatStreamCurrentBatch.size()!=streamResponseRecordList.size()){
            log.error("【流水接入】3.流水执行验重逻辑后结果,入ods库{}条，入异常库{}条", normalStreamCurrentBatch.size(),repeatStreamCurrentBatch.size());
            throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400447", "执行验重逻辑获取的ods入库数据量和入异常库数据量之和与传入数据不等!") /* "执行验重逻辑获取的ods入库数据量和入异常库数据量之和与传入数据不等!" */);
        }
        log.error("【流水接入】3.流水执行验重逻辑后结果,入ods库{}条，入异常库{}条", normalStreamCurrentBatch.size(),repeatStreamCurrentBatch.size());
        return resultMap;
    }

    private void checkRepeatInODSDB(List<BankDealDetailODSModel> repeatStreamCurrentBatch, List<BankDealDetailODSModel> normalStreamCurrentBatch, List<BankDealDetailODSModel> streamResponseRecordList) {
        Map<String, BankDealDetailODSModel> stringStreamResponseRecordMap = normalStreamCurrentBatch.stream().collect(Collectors.toMap(BankDealDetailODSModel::getContentsignature, Function.identity(),(oldValue, newValue)->newValue));
        //提取签名信息
        Set<String> contentSignatureList = stringStreamResponseRecordMap.keySet();
        //去ods查询
        List<BankDealDetailODSModel> existsContentSignatureList = bankDealDetailAccessDao.batchQueryODSByContentSign(contentSignatureList);
        if(!CollectionUtils.isEmpty(existsContentSignatureList)){
            //ods已存在流水
            for(BankDealDetailODSModel bankDealDetailODSModel:existsContentSignatureList){
                BankDealDetailODSModel existsStreamResponseRecord = stringStreamResponseRecordMap.get(bankDealDetailODSModel.getContentsignature());
                repeatStreamCurrentBatch.add(existsStreamResponseRecord);
                normalStreamCurrentBatch.remove(existsStreamResponseRecord);
            }
        }
    }
    private void checkRepeatInCurrentBatch(List<BankDealDetailODSModel> repeatStreamCurrentBatch, List<BankDealDetailODSModel> normalStreamCurrentBatch, List<BankDealDetailODSModel> streamResponseRecordList) {
        for(BankDealDetailODSModel streamResponseRecord: streamResponseRecordList){
            if(!normalStreamCurrentBatch.contains(streamResponseRecord)){
                normalStreamCurrentBatch.add(streamResponseRecord);
            }else{
                repeatStreamCurrentBatch.add(streamResponseRecord);
            }
        }
    }
    @Override
    protected void processDealDetailTOODS(BankDealDetailModel streamModel, Map<String, List<BankDealDetailODSModel>> resultMap, List<BankDealDetailODSModel> bankDealDetailODSFailModelList) {
        long start = System.currentTimeMillis();
        CtmJSONArray ctmJSONArray = CtmJSONArray.parseArray(JSONObject.toJSONString(bankDealDetailODSFailModelList));
        List<BankDealDetailODSFailModel> bankDealDetailODSFailModels = new ArrayList<>();
        for(int i=0;i<ctmJSONArray.size();i++){
            Object recordObj = ctmJSONArray.get(i);
            BankDealDetailODSFailModel streamResponseRecord = CtmJSONObject.parseObject(JSONObject.toJSONString(recordObj), BankDealDetailODSFailModel.class);
            long tenantId = Long.parseLong(InvocationInfoProxy.getYxyTenantid());
            String ytenantId = InvocationInfoProxy.getTenantid();
            String traceId = DealDetailUtils.getTraceId();
            streamResponseRecord.setTenant_id(tenantId);
            streamResponseRecord.setYtenant_id(ytenantId);
            streamResponseRecord.setTraceId(traceId);
            streamResponseRecord.setCreate_time(new Date());
            bankDealDetailODSFailModels.add(streamResponseRecord);
        }
        bankDealDetailAccessDao.dealDetailInsert(streamModel.getDetailOperLogModel(),resultMap.get(BankDealDetailConst.NORMALSTREAM),bankDealDetailODSFailModels);
        log.error("【流水接入】6.流水入ods库成功,耗时{}ms",(System.currentTimeMillis()-start));
    }
    @Override
    public void notifyConsumer(BankDealDetailModel bankDealDetailModel,Map<String, List<BankDealDetailODSModel>> mapByRepeatStatus){
        try{
            //step1:ods流水缓存起来
            List<BankDealDetailODSModel> odsDealDetails = mapByRepeatStatus.get(BankDealDetailConst.NORMALSTREAM);
            BankDealDetailOperLogModel dealDetailOperLogModel = bankDealDetailModel.getDetailOperLogModel();
            if(!CollectionUtils.isEmpty(odsDealDetails)){
                String traceId = dealDetailOperLogModel.getTraceId();
                String requestSeqNo = dealDetailOperLogModel.getRequestSeqNo();
                DealDetailConsumerDTO eventBusModel = new DealDetailConsumerDTO(traceId,requestSeqNo, DealDetailConsumerDTO.TYPE_YQL);
                odsCache.put(eventBusModel,odsDealDetails);
                //step2:调用guava eventbus 通知消费者消费
                EventBus eventBus = AppContext.getBean(DealDetailEnumConst.ODSEVENTBUS,EventBus.class);
                eventBus.post(eventBusModel);
                log.error("【流水接入】5.已发送guava事件,traceId={},requestSeqNo={}",traceId,requestSeqNo);
            }
        }catch (Exception e){
            log.error("【流水接入】通知消费者消息消费异常",e);
        }
    }
}
