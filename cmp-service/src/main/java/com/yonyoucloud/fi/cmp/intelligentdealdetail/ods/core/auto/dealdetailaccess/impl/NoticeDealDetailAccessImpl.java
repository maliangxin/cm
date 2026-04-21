package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.impl;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionRequestBodyRecord;
import com.yonyoucloud.fi.cmp.bankunion.BankUnionResponseRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.NoticeBankDealDetailModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.impl.NoticeDealDetailConsumer;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailconsumer.model.DealDetailConsumerDTO;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailException;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.exception.BankDealDetailExceptionCodeEnum;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.util.*;
/**
 * @Author guoyangy
 * @Date 2024/7/23 16:19
 * @Description 到账通知流水接入
 * @Version 1.0
 */
@Service
@Slf4j
public class NoticeDealDetailAccessImpl<T> extends YQLDealDetailAccessImpl<T>{

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Resource
    private NoticeDealDetailConsumer noticeDealDetailConsumer;

    @Override
    public BankDealDetailModel getBankDealDetailModelList(T t) throws BankDealDetailException {
        if(t instanceof NoticeBankDealDetailModel){
            NoticeBankDealDetailModel bankDealDetailModel = (NoticeBankDealDetailModel) t;
            String requestSeqNo = bankDealDetailModel.getRequestSeqNo();
            List<BankUnionRequestBodyRecord> bankUnionRequestBodyRecords = bankDealDetailModel.getStreamResponseRecordList();
            List<BankUnionResponseRecord> responseParseFailRecords = new ArrayList<>();
            List<BankDealDetailODSModel> bankReconciliationList = this.dealWithBankUnion(bankUnionRequestBodyRecords,responseParseFailRecords,requestSeqNo);
            if(responseParseFailRecords.size()+bankReconciliationList.size() != bankUnionRequestBodyRecords.size()){
                throw new BankDealDetailException(BankDealDetailExceptionCodeEnum.BANKDEALDETAIL_ACCESS_NOTICE_PARSE_ERROR.getErrCode(),BankDealDetailExceptionCodeEnum.BANKDEALDETAIL_ACCESS_NOTICE_PARSE_ERROR.getMsg());
            }
            if(!CollectionUtils.isEmpty(responseParseFailRecords)){
                bankDealDetailModel.setResponseParseFailRecords(responseParseFailRecords);
            }
            if(CollectionUtils.isEmpty(bankReconciliationList)){
                throw new BankDealDetailException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540058F", "入参解析成流水ODS实体异常") /* "入参解析成流水ODS实体异常" */);
            }
            bankDealDetailModel.setStreamResponseRecordList(bankReconciliationList);
            return bankDealDetailModel;
        }
        return null;
    }

    @Override
    public void processDealDetailTOODS(BankDealDetailModel streamModel, Map<String, List<BankDealDetailODSModel>> resultMap) {
        super.processDealDetailTOODS(streamModel, resultMap);
        //技术去重流水
        List<BankDealDetailODSModel> bankDealDetailODSFailModelList = resultMap.get(BankDealDetailConst.REPEATSTREAM);
        if(!CollectionUtils.isEmpty(bankDealDetailODSFailModelList)){
            if(streamModel instanceof  NoticeBankDealDetailModel){
                NoticeBankDealDetailModel noticeBankDealDetailModel = (NoticeBankDealDetailModel) streamModel;
                 for(BankDealDetailODSModel odsModel : bankDealDetailODSFailModelList){
                    BankUnionResponseRecord responseRecord = new BankUnionResponseRecord();
                    responseRecord.setUnique_no(odsModel.getUnique_no());
                    responseRecord.setCode("010100");
                    responseRecord.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540058D", "【ODS技术去重】当前交易明细为重复数据") /* "【ODS技术去重】当前交易明细为重复数据" */);
                    noticeBankDealDetailModel.addBankUnionResponseRecord(responseRecord);
                }

            }
        }
    }

    @Override
    public void notifyConsumer(BankDealDetailModel bankDealDetailModel, Map<String, List<BankDealDetailODSModel>> mapByRepeatStatus) {
        try{
            //step1:ods流水缓存起来
            List<BankDealDetailODSModel> odsDealDetails = mapByRepeatStatus.get(BankDealDetailConst.NORMALSTREAM);
            if(!CollectionUtils.isEmpty(odsDealDetails)){
                String traceId = odsDealDetails.get(0).getTraceid();
                String requestSeqNo = odsDealDetails.get(0).getRequest_seq_no();
                DealDetailConsumerDTO eventBusModel = new DealDetailConsumerDTO(traceId,requestSeqNo, DealDetailConsumerDTO.TYPE_YQL);
                noticeDealDetailConsumer.asyncBankDealDetailConsumer(eventBusModel);
                log.info("【流水接入】5.已发送guava事件,traceId={},requestSeqNo={}",traceId,requestSeqNo);
            }
        }catch (Exception e){
            log.error("【流水接入】通知消费者消息消费异常",e);
        }
    }

    /**
     * 处理推过来的交易明细转化为bip交易明细并入库
     * 同时在事物结束后开启线程异步插入银行对账单
     * @throws Exception
     */
    public  List<BankDealDetailODSModel> dealWithBankUnion( List<BankUnionRequestBodyRecord> bankUnionRequestBodyRecords,List<BankUnionResponseRecord> responseParseFailRecords,String requestSeqNo){
        List<BankDealDetailODSModel> bankDealDetailODSModelList = new ArrayList<>();
        for(BankUnionRequestBodyRecord bankUnionRequest:bankUnionRequestBodyRecords){
            BankUnionResponseRecord responseRecord = new BankUnionResponseRecord();
            try {
                BankDealDetailODSModel bankDealDetailODSModel = new BankDealDetailODSModel();
                bankDealDetailODSModel.setRequest_seq_no(requestSeqNo);
                bankDealDetailODSModel.setTraceid(DealDetailUtils.getTraceId());
                String id = ymsOidGenerator.nextStrId();
                bankDealDetailODSModel.setId(id);
                bankDealDetailODSModel.setMainid(0L);
                bankDealDetailODSModel.setCreate_time(new Date());
                bankDealDetailODSModel.setTenant_id(AppContext.getTenantId());
                bankDealDetailODSModel.setYtenant_id(AppContext.getYTenantId());
                bankDealDetailODSModel.setProcessstatus(DealDetailEnumConst.ODS_processstatusEnum.PROCESSSTATUS_NO.getProcessstatus());
                // 币种
                String currCode = bankUnionRequest.getCurr_code();
                CurrencyBdParams currencyBdParams = new CurrencyBdParams();
                if (StringUtils.isNotEmpty(currCode)) {
                    currencyBdParams.setCode(currCode);
                }
                List<CurrencyTenantDTO> currencylist = baseRefRpcService.queryCurrencyByParams(currencyBdParams);
                if (currencylist == null || currencylist.size() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100566"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508009E", "币种编码不存在！") /* "币种编码不存在！" */);
                }
                String currency =currencylist.get(0).getId();
                bankDealDetailODSModel.setCurrency(currency);
                log.error("=========币种查询===unique_no:=="+bankUnionRequest.getUnique_no()+"==currCode:"+currCode+"==currency:"+currency);
                // 交易流水号
                bankDealDetailODSModel.setBank_seq_no(bankUnionRequest.getBank_seq_no());
                // 交易日期
                String tran_date = bankUnionRequest.getTran_date();
                if(StringUtils.isNotEmpty(tran_date)){
                    Date tranDate = DateUtils.dateParse(tran_date, DateUtils.YYYYMMDD);
                    bankDealDetailODSModel.setTran_date(tran_date);
                }
                // 交易时间
                String timeStr = bankUnionRequest.getTran_time();
                if (StringUtils.isNotEmpty(tran_date) && StringUtils.isNotEmpty(timeStr)) {
                    bankDealDetailODSModel.setTran_time(timeStr);
                }
                // 收付方向
                String dc_flag = bankUnionRequest.getDc_flag();
                if ("d".equalsIgnoreCase(dc_flag)) {
                    bankDealDetailODSModel.setDc_flag(Direction.Debit.getValue());
                } else if ("c".equalsIgnoreCase(dc_flag)) {
                    bankDealDetailODSModel.setDc_flag(Direction.Credit.getValue());
                }
                // 交易金额
                bankDealDetailODSModel.setTran_amt(bankUnionRequest.getTran_amt());
                // 余额
                bankDealDetailODSModel.setAcct_bal(bankUnionRequest.getAcct_bal());
                bankDealDetailODSModel.setAcct_no(bankUnionRequest.getAcct_no());
                bankDealDetailODSModel.setAcct_name(bankUnionRequest.getAcct_name());
                // 对方账号
                bankDealDetailODSModel.setTo_acct_no(bankUnionRequest.getTo_acct_no());
                // 对方户名
                bankDealDetailODSModel.setTo_acct_name(bankUnionRequest.getTo_acct_name());
                // 对方账户开户行
                bankDealDetailODSModel.setTo_acct_bank(bankUnionRequest.getTo_acct_bank());
                // 对方开户行名
                bankDealDetailODSModel.setTo_acct_bank_name(bankUnionRequest.getTo_acct_bank_name());
                // 钞汇标志
                bankDealDetailODSModel.setCash_flag(bankUnionRequest.getCash_flag());
                // 操作员
                bankDealDetailODSModel.setOper(bankUnionRequest.getOper());
                // 起息日
                String value_date = bankUnionRequest.getValue_date();
                if(StringUtils.isNotEmpty(value_date)){
                    Date valueDate = DateUtils.dateParse(value_date, DateUtils.YYYYMMDD);
                    bankDealDetailODSModel.setValue_date(valueDate);
                }
                // 用途
                bankDealDetailODSModel.setUse_name(bankUnionRequest.getUse_name());
                // 摘要
                bankDealDetailODSModel.setRemark(bankUnionRequest.getRemark());
                // 附言
                bankDealDetailODSModel.setRemark01(bankUnionRequest.getRemark01());
                // 银行对账编号
                bankDealDetailODSModel.setBank_check_code(bankUnionRequest.getBank_check_code());
                // 唯一标识码
                bankDealDetailODSModel.setUnique_no(bankUnionRequest.getUnique_no());
                // 数据来源
                bankDealDetailODSModel.setAccesschannel(DealDetailEnumConst.AccessChannelEnum.NOTICE.getKey());
                //生成唯一签名
                String signature = this.contentSign(CtmJSONObject.toJSONString(bankUnionRequest));
                bankDealDetailODSModel.setContentsignature(signature);
                bankDealDetailODSModelList.add(bankDealDetailODSModel);
            } catch (Exception e) {
                responseRecord.setUnique_no(bankUnionRequest.getUnique_no());
                responseRecord.setCode("010012");
                responseRecord.setMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540058E", "检查数据准确性：") /* "检查数据准确性：" */+e.getMessage());
                responseParseFailRecords.add(responseRecord);
                log.error("到账通知，解析入参异常",e);
            }
        }
        return bankDealDetailODSModelList;
    }


}
